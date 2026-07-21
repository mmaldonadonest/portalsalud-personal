package com.onest.app.catalog.file.web;

import com.onest.app.catalog.file.service.FileStoreService;
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
 * Modelo acordado: metadatos en BD (APP_FS_FILE) + binario en filesystem
 * (portal.files.root); el binario NO se guarda en base de datos.
 */
@Controller
public class FileStoreController {

    private final FileStoreService fileStoreService;

    public FileStoreController(FileStoreService fileStoreService) {
        this.fileStoreService = fileStoreService;
    }

    /** Sube uno o mas PDF asociados a una consulta (relacion = idArchivoRel). */
    @PostMapping(path = "/api/nss/consulta/file", produces = "text/plain;charset=UTF-8")
    @ResponseBody
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

    /** Descarga/visualiza un adjunto por id. */
    @GetMapping("/api/nss/consulta/file/{id}")
    public ResponseEntity<byte[]> download(@PathVariable long id) {
        return fileStoreService.get(id)
                .map(f -> ResponseEntity.ok()
                        .contentType(parseMime(f.mimeType()))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + safe(f.name()) + "\"")
                        .body(f.content()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Tipos de archivo de expediente por NSS (Laboratorio U03 + Historico/Notas U06).
    private static final java.util.Map<String, String> TIPOS = java.util.Map.of(
            "laboratorio", "Estudios de laboratorio",
            "examen_medico", "Historico de examen medico",
            "nota_medica", "Notas medicas",
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
        return "fragments/archivos :: view";
    }

    /** Borra un adjunto por id (erasedocksLAB del legacy). */
    @PostMapping(path = "/api/nss/consulta/file/{id}/delete", produces = "text/plain;charset=UTF-8")
    @ResponseBody
    public String delete(@PathVariable long id) {
        return fileStoreService.delete(id) ? "eliminado" : "no encontrado";
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
