package com.onest.app.catalog.causaconsulta.web;

import com.onest.app.audit.web.Auditado;
import com.onest.app.catalog.causaconsulta.dto.CausaConsultaDto;
import com.onest.app.catalog.causaconsulta.service.CausaConsultaService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Catálogo administrable de causas de consulta (JSON puro, mismo criterio que
 * ConsumibleController) - NO está asociado al flujo de búsqueda por NSS.
 * Backend en docs/ords-causa-consulta.sql, aplicado 2026-08-24.
 */
@RestController
@RequestMapping("/api/causas-consulta")
public class CausaConsultaController {

    private final CausaConsultaService causaConsultaService;

    public CausaConsultaController(CausaConsultaService causaConsultaService) {
        this.causaConsultaService = causaConsultaService;
    }

    /** soloActivos=true (default) para el select de captura; false para la pantalla admin. */
    @GetMapping
    public List<CausaConsultaDto> listar(@RequestParam(name = "soloActivos", defaultValue = "true") boolean soloActivos) {
        return causaConsultaService.listar(soloActivos);
    }

    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = "text/plain;charset=UTF-8")
    @Auditado(modulo = "Catalogos", accion = "create", entidad = "Causa de consulta", registro = "nombre")
    public String crear(@RequestParam("nombre") String nombre) {
        try {
            String proceso = causaConsultaService.crear(nombre);
            return (proceso == null || proceso.isBlank()) ? "Causa registrada." : proceso;
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PostMapping(
            path = "/{id}/estado",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = "text/plain;charset=UTF-8")
    @Auditado(modulo = "Catalogos", accion = "update", entidad = "Causa de consulta", registro = "id", detalle = {"activo"})
    public String cambiarEstado(@PathVariable String id, @RequestParam("activo") boolean activo) {
        try {
            String proceso = causaConsultaService.cambiarEstado(id, activo);
            return (proceso == null || proceso.isBlank()) ? "Estado actualizado." : proceso;
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }
}
