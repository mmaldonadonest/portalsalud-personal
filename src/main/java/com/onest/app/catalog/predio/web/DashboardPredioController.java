package com.onest.app.catalog.predio.web;

import com.onest.app.catalog.predio.service.PredioService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lista de predios para el filtro del Inicio (14-oct-2026). El Inicio lo ve cualquier usuario
 * autenticado y /api/analisis/filtros es solo del rol MEDICO_ANALISTA, por eso va aparte bajo
 * /api/dashboard. Solo nombres de predio (sin cuentas): el Inicio ya mostraba a todos el total
 * de todos los predios; esto solo permite partirlo.
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardPredioController {

    private final PredioService predioService;

    public DashboardPredioController(PredioService predioService) {
        this.predioService = predioService;
    }

    @GetMapping("/predios")
    public List<String> predios() {
        return AnalisisFiltroController.predios(predioService, AnalisisFiltroController.cuentas(predioService));
    }
}
