package com.onest.app.catalog.dashboard.web;

import com.onest.app.catalog.dashboard.dto.DashboardAccidentesDto;
import com.onest.app.catalog.dashboard.dto.DashboardAntidopingDto;
import com.onest.app.catalog.dashboard.dto.DashboardConsultaDto;
import com.onest.app.catalog.dashboard.dto.DashboardExamenDto;
import com.onest.app.catalog.dashboard.dto.DashboardIncapacidadesDto;
import com.onest.app.catalog.dashboard.dto.DashboardResumenGeneralDto;
import com.onest.app.catalog.dashboard.service.DashboardAccidentesService;
import com.onest.app.catalog.dashboard.service.DashboardAntidopingService;
import com.onest.app.catalog.dashboard.service.DashboardConsultaService;
import com.onest.app.catalog.dashboard.service.DashboardExamenService;
import com.onest.app.catalog.dashboard.service.DashboardIncapacidadesService;
import com.onest.app.catalog.dashboard.service.DashboardResumenGeneralService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Dashboard ligero de KPIs (backend). Devuelve JSON puro a proposito: la UI (graficas) se
 * conecta despues de decidir la libreria (recharts vs Chart.js/ApexCharts, ver
 * docs/plan-tareas-concretas.html) - el contrato JSON no depende de esa decision.
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardIncapacidadesService incapacidadesService;
    private final DashboardAccidentesService accidentesService;
    private final DashboardConsultaService consultaService;
    private final DashboardAntidopingService antidopingService;
    private final DashboardExamenService examenService;
    private final DashboardResumenGeneralService resumenGeneralService;

    public DashboardController(
            DashboardIncapacidadesService incapacidadesService,
            DashboardAccidentesService accidentesService,
            DashboardConsultaService consultaService,
            DashboardAntidopingService antidopingService,
            DashboardExamenService examenService,
            DashboardResumenGeneralService resumenGeneralService) {
        this.incapacidadesService = incapacidadesService;
        this.accidentesService = accidentesService;
        this.consultaService = consultaService;
        this.antidopingService = antidopingService;
        this.examenService = examenService;
        this.resumenGeneralService = resumenGeneralService;
    }

    /** KPIs de incapacidades en un rango de fechas (ISO yyyy-MM-dd, igual que el reporte existente). */
    @GetMapping("/incapacidades")
    public DashboardIncapacidadesDto incapacidades(
            @RequestParam("fechaInicial") String fechaInicial,
            @RequestParam("fechaFinal") String fechaFinal) {
        try {
            return incapacidadesService.resumen(fechaInicial, fechaFinal);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    /** KPIs de accidentes de trabajo en un rango de fechas (ISO yyyy-MM-dd). */
    @GetMapping("/accidentes")
    public DashboardAccidentesDto accidentes(
            @RequestParam("fechaInicial") String fechaInicial,
            @RequestParam("fechaFinal") String fechaFinal) {
        try {
            return accidentesService.resumen(fechaInicial, fechaFinal);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    /** KPIs de consultas medicas / Morbilidad en un rango de fechas (ISO yyyy-MM-dd). */
    @GetMapping("/consultas")
    public DashboardConsultaDto consultas(
            @RequestParam("fechaInicial") String fechaInicial,
            @RequestParam("fechaFinal") String fechaFinal) {
        try {
            return consultaService.resumen(fechaInicial, fechaFinal);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    /** KPIs de antidoping/alcoholimetria en un rango de fechas (ISO yyyy-MM-dd). */
    @GetMapping("/antidoping")
    public DashboardAntidopingDto antidoping(
            @RequestParam("fechaInicial") String fechaInicial,
            @RequestParam("fechaFinal") String fechaFinal) {
        try {
            return antidopingService.resumen(fechaInicial, fechaFinal);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    /**
     * KPIs de dictamenes de examen medico en un rango de fechas (ISO yyyy-MM-dd).
     * SIN datos retroactivos - el historial arranco vacio el 2026-08-17.
     */
    @GetMapping("/examen")
    public DashboardExamenDto examen(
            @RequestParam("fechaInicial") String fechaInicial,
            @RequestParam("fechaFinal") String fechaFinal) {
        try {
            return examenService.resumen(fechaInicial, fechaFinal);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    /**
     * Resumen General: combina los 4 dashboards anteriores (Incapacidades, Accidentes,
     * Consultas, Examen) en un solo total y una tendencia mensual unificada.
     */
    @GetMapping("/resumen-general")
    public DashboardResumenGeneralDto resumenGeneral(
            @RequestParam("fechaInicial") String fechaInicial,
            @RequestParam("fechaFinal") String fechaFinal) {
        try {
            return resumenGeneralService.resumen(fechaInicial, fechaFinal);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }
}
