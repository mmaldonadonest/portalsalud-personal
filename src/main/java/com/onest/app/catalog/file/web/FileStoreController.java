package com.onest.app.catalog.file.web;

import com.onest.app.audit.web.Auditado;
import com.onest.app.catalog.dashboard.repository.VistaExamenRepository;
import com.onest.app.catalog.file.service.FileStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * Upload/download de adjuntos del expediente.
 * Reemplaza uploadFile.php (subida) y downloadPDFCons (descarga base64) del legacy.
 * Modelo acordado: metadatos en BD (SERV_MED_FS_FILE) + binario en filesystem
 * (portal.files.root); el binario NO se guarda en base de datos.
 */
@Controller
public class FileStoreController {

    private static final Logger log = LoggerFactory.getLogger(FileStoreController.class);

    private final FileStoreService fileStoreService;
    // Solo existe con portal.historico.fuente=VISTA: Historico E.M. muestra tambien los examenes del Excel
    private final ObjectProvider<VistaExamenRepository> vistaExamenes;

    public FileStoreController(FileStoreService fileStoreService,
                               ObjectProvider<VistaExamenRepository> vistaExamenes) {
        this.fileStoreService = fileStoreService;
        this.vistaExamenes = vistaExamenes;
    }

    /** Sube uno o mas PDF asociados a una consulta (relacion = idArchivoRel). */
    @PostMapping(path = "/api/nss/consulta/file", produces = "text/plain;charset=UTF-8")
    @ResponseBody
    // "Archivos del expediente" y no "Consultas" (16-oct-2026): este alta la usan tambien Laboratorio,
    // Historico E.M. y las notas; la relacion (tipo o id de consulta) va en el detalle
    @Auditado(modulo = "Archivos del expediente", accion = "create", entidad = "Archivo adjunto", registro = "nss", detalle = {"relacion"})
    public String upload(
            @RequestParam("file") MultipartFile[] file,
            @RequestParam("nss") String nss,
            @RequestParam("relacion") String relacion) {
        try {
            int n = fileStoreService.store(file, nss, relacion);
            return n + " archivo(s) adjuntado(s).";
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    /**
     * Descarga/visualiza un adjunto por id. El NSS del expediente abierto es obligatorio y debe ser
     * el dueño del archivo; si no lo es se responde 404, igual que si no existiera (no revela ids).
     */
    @GetMapping("/api/nss/consulta/file/{id}")
    public ResponseEntity<byte[]> download(@PathVariable long id, @RequestParam("nss") String nss) {
        return fileStoreService.get(id, nss)
                .map(f -> ResponseEntity.ok()
                        .contentType(parseMime(f.mimeType()))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + safe(f.name()) + "\"")
                        .body(f.content()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Tipos de archivo de expediente por NSS (Laboratorio U03 + Historico/Notas U06).
    private static final java.util.Map<String, String> TIPOS = java.util.Map.of(
            "laboratorio", "Estudios de laboratorio",
            "examen_medico", "Histórico de examen médico",
            "nota_medica", "Notas médicas",
            "nota_incapacidad", "Notas de incapacidad");

    /**
     * Vista generica de archivos por NSS y tipo (subir + listar + borrar).
     * Cubre Laboratorio (id 2), Historico E.M (id 3), Nota medica (id 4), Nota incapacidad (id 5).
     */
    @PostMapping(
            path = "/api/nss/archivos",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_HTML_VALUE)
    public String archivos(@RequestParam("data") String data, @RequestParam("tipo") String tipo, Model model) {
        if (!TIPOS.containsKey(tipo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de archivo no permitido: " + tipo);
        }
        String nss = data == null ? "" : data.trim();
        model.addAttribute("nss", nss);
        model.addAttribute("tipo", tipo);
        model.addAttribute("titulo", TIPOS.get(tipo));
        model.addAttribute("files", fileStoreService.listByNssAndType(nss, tipo));
        if ("examen_medico".equals(tipo)) {
            agregarHistoricoExamenes(nss, model);
        }
        return "fragments/archivos :: view";
    }

    /**
     * En Historico E.M., ademas de los PDF, los examenes del Excel (layouts 2, 3 y 4 por NSS) como
     * tarjeta "Histórico Excel". Sin el interruptor VISTA no se agrega nada. Si la vista falla, los
     * archivos se ven igual y la tarjeta avisa: el historico es un complemento.
     */
    private void agregarHistoricoExamenes(String nss, Model model) {
        VistaExamenRepository vista = vistaExamenes.getIfAvailable();
        if (vista == null || nss.isEmpty()) {
            return;
        }
        model.addAttribute("conHistorico", true);
        try {
            model.addAttribute("historico", vista.historicoPorNss(nss));
        } catch (DataAccessException ex) {
            log.warn("[expediente] historico de examenes no disponible para el NSS: {}", ex.getMessage());
            model.addAttribute("historico", java.util.List.of());
            model.addAttribute("historicoError", true);
        }
    }

    /** Borra un adjunto por id (erasedocksLAB del legacy). */
    @PostMapping(path = "/api/nss/consulta/file/{id}/delete", produces = "text/plain;charset=UTF-8")
    @ResponseBody
    @Auditado(modulo = "Archivos del expediente", accion = "delete", entidad = "Archivo adjunto", registro = "id")
    public String delete(@PathVariable long id, @RequestParam("nss") String nss) {
        if (!fileStoreService.delete(id, nss)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "no encontrado");
        }
        return "eliminado";
    }

    private static MediaType parseMime(String mime) {
        try {
            return mime == null || mime.isBlank() ? MediaType.APPLICATION_PDF : MediaType.parseMediaType(mime);
        } catch (RuntimeException ex) {
            return MediaType.APPLICATION_PDF;
        }
    }

    private static String safe(String name) {
        return name == null ? "archivo.pdf" : name.replace("\"", "");
    }
}
