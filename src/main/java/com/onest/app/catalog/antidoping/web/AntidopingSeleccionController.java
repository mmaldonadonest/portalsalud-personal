package com.onest.app.catalog.antidoping.web;

import com.onest.app.audit.web.Auditado;
import com.onest.app.catalog.antidoping.dto.AntidopingSeleccionDto;
import com.onest.app.catalog.antidoping.service.AntidopingService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Historial de selecciones aleatorias de Antidoping (quien fue elegido y cuando) - separado
 * del resultado real de la prueba (AntidopingController). Backend en
 * docs/ords-antidoping-seleccion.sql, aplicado 2026-08-22. JSON puro (igual criterio que
 * ConsumibleController) - NO esta asociado al flujo de busqueda por NSS, lo consume
 * directo /antidoping-seleccion.html.
 */
@RestController
@RequestMapping("/api/antidoping-seleccion")
public class AntidopingSeleccionController {

    private final AntidopingService antidopingService;

    public AntidopingSeleccionController(AntidopingService antidopingService) {
        this.antidopingService = antidopingService;
    }

    /** Todo el historial de selecciones (no filtrado por NSS). */
    @GetMapping
    public List<AntidopingSeleccionDto> historial() {
        return antidopingService.historialSelecciones();
    }

    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = "text/plain;charset=UTF-8")
    @Auditado(modulo = "Antidoping", accion = "create", entidad = "Seleccion aleatoria", registro = "nss", detalle = {"tamanoPool"})
    public String registrar(
            @RequestParam("nss") String nss,
            @RequestParam(name = "tamanoPool", required = false) Integer tamanoPool) {
        try {
            String proceso = antidopingService.registrarSeleccion(nss, tamanoPool);
            return (proceso == null || proceso.isBlank()) ? "Seleccion registrada." : proceso;
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }
}
