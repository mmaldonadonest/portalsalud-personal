package com.onest.app.catalog.dashboard.web;

import com.onest.app.audit.web.Auditado;
import com.onest.app.catalog.dashboard.dto.ConteoDictamenDto;
import com.onest.app.catalog.dashboard.service.ComparativoPretestService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
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
 * C-006 (18-nov-2026): Excel examenes medicos vs Pretest de Analisis > Examenes. Bajo /api/analisis/**: mismo
 * rol que la pantalla que lo descarga (igual que el Excel de totales de incapacidades).
 */
@RestController
@RequestMapping("/api/analisis/examenes")
public class ComparativoPretestController {

    private final ComparativoPretestService servicio;

    public ComparativoPretestController(ComparativoPretestService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/pretest/excel")
    @Auditado(modulo = "Examenes", accion = "export", entidad = "Examenes vs Pretest (Excel)", registro = "fechaInicial")
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
        String archivo = ("examenes vs pretest " + nvl(fechaInicial) + " a " + nvl(fechaFinal)
                + (predio == null || predio.isBlank() ? "" : " " + predio.trim()))
                .replaceAll("[^A-Za-z0-9ÁÉÍÓÚÑáéíóúñ ._-]", "_").trim() + ".xlsx";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(archivo, StandardCharsets.UTF_8).build().toString())
                .body(xlsx);
    }

    /** 19-nov-2026: grafica examen medico vs Pretest (mismos numeros que el Excel). */
    @GetMapping("/pretest")
    public Map<String, List<ComparativoPretestService.Comparativo>> grafica(
            @RequestParam(name = "fechaInicial", required = false) String fechaInicial,
            @RequestParam(name = "fechaFinal", required = false) String fechaFinal,
            @RequestParam(name = "predio", required = false) String predio) {
        try {
            return servicio.grafica(fechaInicial, fechaFinal, predio);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), ex);
        }
    }

    /** 19-nov-2026: examenes por genero con su dictamen. */
    @GetMapping("/genero")
    public List<ConteoDictamenDto> genero(@RequestParam(name = "fechaInicial", required = false) String fechaInicial,
                                          @RequestParam(name = "fechaFinal", required = false) String fechaFinal,
                                          @RequestParam(name = "predio", required = false) String predio,
                                          @RequestParam(name = "tipo", required = false) String tipo) {
        try {
            return servicio.porGenero(fechaInicial, fechaFinal, predio, tipo);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), ex);
        }
    }

    private static String nvl(String v) {
        return v == null ? "" : v.trim();
    }
}
