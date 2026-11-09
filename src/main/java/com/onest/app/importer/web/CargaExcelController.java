package com.onest.app.importer.web;

import com.onest.app.audit.web.Auditado;
import com.onest.app.catalog.file.dto.StoredFileLocation;
import com.onest.app.importer.dto.CargaExcelDto.Trabajo;
import com.onest.app.importer.nss.XlsNssRepository.LoteResumen;
import com.onest.app.importer.service.CargaExcelService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * API de Carga de Excel. Acceso por el menu CARGA_EXCEL (ClinicalAccessFilter), asignable a
 * MEDICO_ANALISTA y a los roles de Servicio Medico en Administracion > Roles > Menus.
 * Multipart: CSRF por header, igual que el resto de las cargas del portal.
 */
@RestController
@RequestMapping("/api/carga-excel")
public class CargaExcelController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final CargaExcelService service;

    public CargaExcelController(CargaExcelService service) {
        this.service = service;
    }

    /** Sube y manda validar (no escribe en la base). Regresa el trabajo para consultar su avance. */
    @PostMapping
    public Trabajo validar(@RequestParam("file") MultipartFile file,
                           @RequestParam(name = "correcciones", required = false) MultipartFile correcciones,
                           @RequestParam(name = "tipo", required = false) String tipo,
                           Authentication auth) {
        return service.validar(file, correcciones, tipo, auth.getName());
    }

    /** Los 10 tipos de archivo, para la lista de la pantalla. */
    @GetMapping("/tipos")
    public java.util.List<java.util.Map<String, String>> tipos() {
        return CargaExcelService.tipos();
    }

    @GetMapping("/trabajos/{id}")
    public Trabajo consultar(@PathVariable String id, Authentication auth) {
        return service.consultar(id, auth.getName());
    }

    /** Escribe en SERV_MED_XLS_*; el resultado (lote) se consulta igual que la validacion. */
    @PostMapping("/trabajos/{id}/confirmar")
    @Auditado(modulo = "Carga de Excel", accion = "create", entidad = "Lote de Excel", registro = "id", detalle = {"forzar"})
    public Trabajo confirmar(@PathVariable String id, @RequestParam(name = "forzar", defaultValue = "false") boolean forzar,
                             Authentication auth) {
        return service.confirmar(id, auth.getName(), forzar);
    }

    @PostMapping("/trabajos/{id}/descartar")
    public ResponseEntity<Void> descartar(@PathVariable String id, Authentication auth) {
        service.descartar(id, auth.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/trabajos/{id}/errores")
    @Auditado(modulo = "Carga de Excel", accion = "export", entidad = "Excel de errores", registro = "id")
    public ResponseEntity<byte[]> errores(@PathVariable String id, Authentication auth) {
        return descarga(service.excelErrores(id, auth.getName()), service.nombreExcelErrores(id, auth.getName()), XLSX);
    }

    @GetMapping("/lotes")
    public List<LoteResumen> historial() {
        return service.historial();
    }

    @GetMapping("/archivos/{archivoId}")
    @Auditado(modulo = "Carga de Excel", accion = "export", entidad = "Excel original", registro = "archivoId")
    public ResponseEntity<byte[]> original(@PathVariable long archivoId) {
        StoredFileLocation f = service.original(archivoId);
        return descarga(service.leer(f), f.name(), XLSX);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> invalido(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .body(ex.getMessage());
    }

    private static ResponseEntity<byte[]> descarga(byte[] bytes, String nombre, MediaType tipo) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(nombre, StandardCharsets.UTF_8).build().toString())
                .contentType(tipo)
                .body(bytes);
    }
}
