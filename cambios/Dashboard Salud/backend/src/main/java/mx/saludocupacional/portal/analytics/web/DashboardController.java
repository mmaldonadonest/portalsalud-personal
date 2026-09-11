package mx.saludocupacional.portal.analytics.web;

import lombok.RequiredArgsConstructor;
import mx.saludocupacional.portal.analytics.dto.AnalyticsDtos.Distribucion;
import mx.saludocupacional.portal.analytics.dto.AnalyticsDtos.Insight;
import mx.saludocupacional.portal.analytics.dto.AnalyticsDtos.RankingPredio;
import mx.saludocupacional.portal.analytics.dto.AnalyticsDtos.ResumenEjecutivo;
import mx.saludocupacional.portal.analytics.dto.AnalyticsDtos.SerieMensual;
import mx.saludocupacional.portal.analytics.service.AnalyticsService;
import mx.saludocupacional.portal.analytics.service.InsightEngine;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Datos del dashboard ejecutivo.
 *
 * <p>Todas las cifras provienen de la capa analítica; este controlador no
 * calcula nada por su cuenta.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final AnalyticsService analytics;
    private final InsightEngine insights;

    @GetMapping("/resumen")
    @PreAuthorize("hasAuthority('dashboard.executive')")
    public ResumenEjecutivo resumen(
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Long predioId) {
        return analytics.resumen(anioOActual(anio), mes, predioId);
    }

    /**
     * Serie mensual de un indicador.
     *
     * @param indicador atenciones, dias, accidentes, examenes o pruebas
     */
    @GetMapping("/tendencia")
    @PreAuthorize("hasAuthority('dashboard.executive')")
    public SerieMensual tendencia(
            @RequestParam(defaultValue = "atenciones") String indicador,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Long predioId) {
        return analytics.serie(indicador, anioOActual(anio), predioId);
    }

    @GetMapping("/predios")
    @PreAuthorize("hasAuthority('dashboard.executive')")
    public List<RankingPredio> ranking(
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer mes) {
        return analytics.ranking(anioOActual(anio), mes);
    }

    @GetMapping("/causas")
    @PreAuthorize("hasAuthority('dashboard.executive')")
    public List<Distribucion> causas(
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Long predioId,
            @RequestParam(defaultValue = "10") int limite) {
        return analytics.topCausas(anioOActual(anio), mes, predioId, limite);
    }

    @GetMapping("/incapacidades-por-tipo")
    @PreAuthorize("hasAuthority('dashboard.executive')")
    public List<Distribucion> incapacidadesPorTipo(
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Long predioId) {
        return analytics.diasPorTipoIncapacidad(anioOActual(anio), mes, predioId);
    }

    @GetMapping("/causas-accidente")
    @PreAuthorize("hasAuthority('dashboard.executive')")
    public List<Distribucion> causasDeAccidente(
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Long predioId) {
        return analytics.causasDeAccidente(anioOActual(anio), mes, predioId);
    }

    @GetMapping("/insights")
    @PreAuthorize("hasAuthority('dashboard.executive')")
    public List<Insight> insights(
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Long predioId) {
        return insights.generar(anioOActual(anio), mes, predioId);
    }

    private int anioOActual(Integer anio) {
        return anio != null ? anio : LocalDate.now().getYear();
    }
}
