package com.onest.app.admin.predio.web;

import com.onest.app.audit.web.Auditado;
import com.onest.app.catalog.predio.dto.CuentaPredioDto;
import com.onest.app.catalog.predio.dto.PredioDto;
import com.onest.app.catalog.predio.service.PredioService;
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
 * Mapeo cuenta-&gt;predio para /admin/predios (protegido con hasRole("ADMIN") en
 * SecurityConfiguration, mismo gate que roles/usuarios). Alimenta el dashboard
 * analitico del rol MEDICO_ANALISTA - ver docs/ords-predio-cuenta.sql.
 */
@RestController
@RequestMapping("/api/admin/predios")
public class PredioAdminController {

    private final PredioService predioService;

    public PredioAdminController(PredioService predioService) {
        this.predioService = predioService;
    }

    /** Catalogo de los 17 sitios, para el &lt;select&gt; de asignacion. */
    @GetMapping
    public List<PredioDto> listarPredios() {
        return predioService.listarPredios();
    }

    /** Todas las cuentas reales con su predio vigente o pendiente. */
    @GetMapping("/cuentas")
    public List<CuentaPredioDto> listarCuentas() {
        return predioService.listarCuentaPredio();
    }

    @PostMapping(path = "/asignar", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @Auditado(modulo = "Administracion", accion = "update", entidad = "Cuenta-predio", registro = "cuentaNombre", detalle = {"predioId"})
    public String asignar(
            @RequestParam("cuentaNombre") String cuentaNombre,
            @RequestParam("predioId") Long predioId) {
        if (cuentaNombre == null || cuentaNombre.isBlank() || predioId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cuentaNombre y predioId son obligatorios");
        }
        return predioService.asignar(cuentaNombre, predioId);
    }
}
