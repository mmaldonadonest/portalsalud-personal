package com.onest.app.catalog.dashboard.web;

import com.onest.app.audit.web.Auditado;
import com.onest.app.catalog.dashboard.service.DetalleCausaService;
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
 * Detalle de una causa (Analisis > Causas > clic en la barra), 13-nov-2026. Bajo /api/analisis/**:
 * mismo rol (MEDICO_ANALISTA) que la pantalla de Causas que lo abre.
 */
@RestController
@RequestMapping("/api/analisis/causas")
public class DetalleCausaController {

    private final DetalleCausaService servicio;

    public DetalleCausaController(DetalleCausaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/detalle")
    public DetalleCausaService.Detalle detalle(@RequestParam("causa") String causa, @RequestParam("anio") int anio,
                                               @RequestParam(name = "predio", required = false) String predio) {
        return armar(causa, anio, predio);
    }

    @GetMapping("/detalle/excel")
    @Auditado(modulo = "Causas", accion = "export", entidad = "Detalle de causa (Excel)", registro = "causa")
    public ResponseEntity<byte[]> excel(@RequestParam("causa") String causa, @RequestParam("anio") int anio,
                                        @RequestParam(name = "predio", required = false) String predio) {
        DetalleCausaService.Detalle d = armar(causa, anio, predio);
        String archivo = ("detalle " + d.causa() + " " + anio + (d.predio() == null ? "" : " " + d.predio()))
                .replaceAll("[^A-Za-z0-9ÁÉÍÓÚÑáéíóúñ ._-]", "_") + ".xlsx";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(archivo, StandardCharsets.UTF_8).build().toString())
                .body(servicio.excel(d));
    }

    private DetalleCausaService.Detalle armar(String causa, int anio, String predio) {
        try {
            return servicio.detalle(causa, anio, predio);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), ex);
        }
    }
}
