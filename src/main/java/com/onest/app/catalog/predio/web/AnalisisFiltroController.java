package com.onest.app.catalog.predio.web;

import com.onest.app.catalog.dashboard.service.DashboardPredioFiltro;
import com.onest.app.catalog.predio.dto.CuentaPredioDto;
import com.onest.app.catalog.predio.service.PredioService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Opciones de los selects Predio/Cuenta del Dashboard Ejecutivo (rol MEDICO_ANALISTA,
 * la ruta /api/analisis/** ya esta gateada en SecurityConfiguration).
 *
 * <p>Solo lectura y sobre datos que PredioService ya tiene cacheados (TTL 2 min) - no hay
 * WS nuevo. Se expone aparte de PredioAdminController a proposito: esa pantalla es /admin/**
 * (hasRole ADMIN) y el analista no la puede llamar.
 */
@RestController
@RequestMapping("/api/analisis")
public class AnalisisFiltroController {

    private final PredioService predioService;

    public AnalisisFiltroController(PredioService predioService) {
        this.predioService = predioService;
    }

    /**
     * Predios del catalogo + cuentas reales con el predio al que pertenecen, para poder
     * encadenar los dos selects sin ida y vuelta extra al servidor.
     *
     * <p>Las cuentas sin predio asignado se devuelven con el bucket "Sin asignar" (mismo
     * texto que agrupa DashboardConsultaService) en vez del "0" crudo que manda el WS.
     */
    @GetMapping("/filtros")
    public FiltrosDto filtros() {
        List<String> predios = new ArrayList<>(predioService.listarPredios().stream()
                .map(p -> p.nombre())
                .filter(n -> n != null && !n.isBlank())
                .sorted(String::compareToIgnoreCase)
                .toList());

        List<CuentaOpcionDto> cuentas = predioService.listarCuentaPredio().stream()
                .map(c -> new CuentaOpcionDto(
                        c.cuentaNombre(),
                        c.tienePredioAsignado() ? c.predioNombre() : DashboardPredioFiltro.SIN_ASIGNAR))
                .toList();

        // "Sin asignar" es filtrable a proposito: con 190+ cuentas por mapear es el corte que
        // deja ver cuanto falta por capturar en /admin/predios.
        boolean hayPendientes = cuentas.stream()
                .anyMatch(c -> DashboardPredioFiltro.SIN_ASIGNAR.equals(c.predio()));
        if (hayPendientes) {
            predios.add(DashboardPredioFiltro.SIN_ASIGNAR);
        }

        return new FiltrosDto(predios, cuentas);
    }

    public record FiltrosDto(List<String> predios, List<CuentaOpcionDto> cuentas) {
    }

    public record CuentaOpcionDto(String cuenta, String predio) {
    }
}
