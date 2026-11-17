package com.onest.app.catalog.dashboard.web;

import com.onest.app.audit.web.Auditado;
import com.onest.app.catalog.dashboard.service.TotalesIncapacidadService;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Excel de totales y horas perdidas de Analisis > Incapacidades (C-056 y C-048, 17-nov-2026). Bajo
 * /api/analisis/**: mismo rol (MEDICO_ANALISTA) que la pantalla que lo descarga.
 */
@RestController
@RequestMapping("/api/analisis/incapacidades")
public class TotalesIncapacidadController {

    private final TotalesIncapacidadService servicio;

    public TotalesIncapacidadController(TotalesIncapacidadService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/excel")
    @Auditado(modulo = "Incapacidades", accion = "export", entidad = "Totales de incapacidades (Excel)", registro = "fechaInicial")
    public ResponseEntity<byte[]> excel(@RequestParam(name = "fechaInicial", required = false) String fechaInicial,
                                        @RequestParam(name = "fechaFinal", required = false) String fechaFinal,
                                        @RequestParam(name = "predio", required = false) String predio) {
        byte[] xlsx;
        try {
            xlsx = servicio.excel(fechaInicial, fechaFinal, predio);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), ex);
        }
        String archivo = ("incapacidades " + nvl(fechaInicial) + " a " + nvl(fechaFinal)
                + (predio == null || predio.isBlank() ? "" : " " + predio.trim()))
                .replaceAll("[^A-Za-z0-9ÁÉÍÓÚÑáéíóúñ ._-]", "_").trim() + ".xlsx";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(archivo, StandardCharsets.UTF_8).build().toString())
                .body(xlsx);
    }

    private static String nvl(String v) {
        return v == null ? "" : v.trim();
    }
}
