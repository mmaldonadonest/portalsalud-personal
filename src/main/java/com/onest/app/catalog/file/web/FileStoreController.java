package com.onest.app.catalog.file.web;

import com.onest.app.audit.web.Auditado;
import com.onest.app.catalog.dashboard.repository.VistaExamenRepository;
import com.onest.app.catalog.examen.repository.ExamenHistoricoRepository;
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
    // 4-nov-2026: los examenes guardados en el portal tambien van en Historico E.M. (antes no se veian)
    private final ExamenHistoricoRepository examenesPortal;

    public FileStoreController(FileStoreService fileStoreService,
                               ObjectProvider<VistaExamenRepository> vistaExamenes,
                               ExamenHistoricoRepository examenesPortal) {
        this.fileStoreService = fileStoreService;
        this.vistaExamenes = vistaExamenes;
        this.examenesPortal = examenesPortal;
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
        java.util.List<com.onest.app.catalog.file.dto.MedFileMeta> files = fileStoreService.listByNssAndType(nss, tipo);
        model.addAttribute("files", files);
        if ("examen_medico".equals(tipo)) {
            model.addAttribute("historial", historialExamenes(nss, files, model));
        }
        return "fragments/archivos :: view";
    }

    // Origen de cada renglon del historial de Historico E.M. (data-origen en la tabla)
    static final String ORIGEN_MANO = "MANO";
    static final String ORIGEN_PORTAL = "PORTAL";
    static final String ORIGEN_EXCEL = "EXCEL";

    private static final java.util.Map<String, String> TIPO_PORTAL = java.util.Map.of(
            "1", "Admisión", "2", "Periódico", "3", "Cambio de puesto", "4", "Post incapacidad", "5", "Especial",
            "6", "Egreso", "7", "Inicial mantenimiento", "8", "Periódico mantenimiento");
    private static final java.util.Map<String, String> TIPO_EXCEL = java.util.Map.of(
            "NUEVO_INGRESO", "Nuevo ingreso", "PERIODICO", "Periódico", "POS_INCAPACIDAD", "Post incapacidad");
    private static final java.util.Map<String, String> DICTAMEN = java.util.Map.of(
            "APTO", "Apto", "APTO_CONDICIONADO", "Apto condicionado", "APTO_RESTRINGIDO", "Apto restringido",
            "NO_APTO", "No apto");

    /**
     * Un renglon del historial. archivoId solo en los subidos a mano (descargar/borrar); pdfPortal solo en
     * el examen del portal mas reciente. fecha puede ser null (Excel sin fecha): esos van al final.
     */
    public record HistorialItem(java.time.LocalDateTime fecha, boolean conHora, String origen, String examen,
                                String dictamen, String detalle, Long archivoId, String archivoNombre,
                                boolean pdfPortal) {
    }

    /**
     * 4-nov-2026: Historico E.M. junta en UNA lista, por fecha, lo que antes eran tres tarjetas: PDF subidos a
     * mano, examenes guardados en el portal (SERV_MED_EXAMEN_HISTORICO) y examenes del Excel (vista, solo con
     * portal.historico.fuente=VISTA). Si una fuente falla, las otras se ven igual y se avisa cual falto.
     *
     * <p>Portal: uno por dia (el ultimo): cada guardado con dictamen deja una fila y el mismo examen se guarda
     * varias veces el mismo dia (mismo criterio que la vista SERV_MED_V_EXAMEN). Solo el mas reciente tiene PDF:
     * el impreso se arma con los datos vigentes, que son los del ultimo guardado. Cuando Servicio Medico pase la
     * nomenclatura, el PDF de cada examen se guardara como archivo.
     */
    private java.util.List<HistorialItem> historialExamenes(String nss, java.util.List<com.onest.app.catalog.file.dto.MedFileMeta> files,
                                                            Model model) {
        java.util.List<HistorialItem> items = new java.util.ArrayList<>();
        for (com.onest.app.catalog.file.dto.MedFileMeta f : files) {
            String tam = f.sizeBytes() == null ? null : String.format(java.util.Locale.US, "%,d KB", f.sizeBytes() / 1024);
            items.add(new HistorialItem(f.dateUpload(), true, ORIGEN_MANO, f.name(), null, tam, f.id(), f.name(), false));
        }
        if (nss.isEmpty()) {
            return items;
        }
        java.util.List<String> faltan = new java.util.ArrayList<>();
        try {
            java.util.Map<java.time.LocalDate, ExamenHistoricoRepository.Registro> porDia = new java.util.LinkedHashMap<>();
            for (ExamenHistoricoRepository.Registro r : examenesPortal.listar(nss, 500)) {
                porDia.putIfAbsent(r.fechaExamen().toLocalDate(), r); // viene ordenado: el primero del dia es el ultimo guardado
            }
            boolean primero = true;
            for (ExamenHistoricoRepository.Registro r : porDia.values()) {
                String tipoEx = r.tipoExamen() == null ? null : TIPO_PORTAL.getOrDefault(r.tipoExamen().trim(), r.tipoExamen());
                String examen = (tipoEx == null ? "Examen médico" : tipoEx)
                        + (ExamenHistoricoRepository.ORIGEN_INICIAL.equals(r.origen()) ? " (examen inicial)" : "");
                String detalle = r.usuario() == null || r.usuario().isBlank() ? null : "Capturó " + r.usuario();
                items.add(new HistorialItem(r.fechaExamen(), true, ORIGEN_PORTAL, examen, dictamen(r.dictamen()), detalle,
                        null, null, primero));
                primero = false;
            }
        } catch (DataAccessException ex) {
            log.warn("[expediente] examenes del portal no disponibles para el NSS: {}", ex.getMessage());
            faltan.add("los exámenes capturados en el portal");
        }
        VistaExamenRepository vista = vistaExamenes.getIfAvailable();
        if (vista != null) {
            try {
                for (VistaExamenRepository.Historico h : vista.historicoPorNss(nss)) {
                    String examen = h.tipo() == null ? "Examen" : TIPO_EXCEL.getOrDefault(h.tipo(), h.tipo());
                    if (h.tipoExamen() != null && !h.tipoExamen().isBlank()) {
                        examen += " (" + h.tipoExamen().trim() + ")";
                    }
                    String detalle = java.util.stream.Stream.of(h.diagnostico(), h.predio())
                            .filter(s -> s != null && !s.isBlank()).map(String::trim)
                            .collect(java.util.stream.Collectors.joining(" · "));
                    items.add(new HistorialItem(h.fecha() == null ? null : h.fecha().atStartOfDay(), false, ORIGEN_EXCEL,
                            examen, dictamen(h.dictamen()), detalle.isEmpty() ? null : detalle, null, null, false));
                }
            } catch (DataAccessException ex) {
                log.warn("[expediente] historico de examenes no disponible para el NSS: {}", ex.getMessage());
                faltan.add("el histórico de Excel");
            }
        }
        model.addAttribute("historialFaltan", faltan);
        model.addAttribute("conExcel", vista != null);
        items.sort(java.util.Comparator.comparing(HistorialItem::fecha,
                java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())));
        return items;
    }

    private static String dictamen(String d) {
        if (d == null || d.isBlank()) {
            return null;
        }
        return DICTAMEN.getOrDefault(d.trim().toUpperCase(java.util.Locale.ROOT), d.trim());
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
