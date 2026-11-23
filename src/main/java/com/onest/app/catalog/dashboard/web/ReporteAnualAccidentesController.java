package com.onest.app.catalog.dashboard.web;

import com.onest.app.audit.web.Auditado;
import com.onest.app.catalog.dashboard.service.ReporteAnualAccidentesService;
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
 * C-044: Excel del reporte anual de accidentes (hoja GENERAL de SM). Bajo /api/analisis/**: mismo rol
 * (MEDICO_ANALISTA) que la pantalla de Accidentabilidad que lo descarga.
 */
@RestController
@RequestMapping("/api/analisis/accidentes")
public class ReporteAnualAccidentesController {

    private final ReporteAnualAccidentesService servicio;

    public ReporteAnualAccidentesController(ReporteAnualAccidentesService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/reporte-anual")
    @Auditado(modulo = "Accidentabilidad", accion = "export", entidad = "Reporte anual de accidentes (Excel)", registro = "anio")
    public ResponseEntity<byte[]> reporteAnual(@RequestParam(name = "anio") int anio,
                                               @RequestParam(name = "predio", required = false) String predio) {
        byte[] xlsx;
        try {
            xlsx = servicio.excel(anio, predio);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), ex);
        }
        String archivo = ("reporte anual accidentes " + anio
                + (predio == null || predio.isBlank() ? "" : " " + predio.trim()))
                .replaceAll("[^A-Za-z0-9ÁÉÍÓÚÑáéíóúñ ._-]", "_").trim() + ".xlsx";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(archivo, StandardCharsets.UTF_8).build().toString())
                .body(xlsx);
    }
}
