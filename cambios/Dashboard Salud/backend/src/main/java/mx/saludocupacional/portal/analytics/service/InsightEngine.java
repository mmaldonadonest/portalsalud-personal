package mx.saludocupacional.portal.analytics.service;

import lombok.RequiredArgsConstructor;
import mx.saludocupacional.portal.analytics.dto.AnalyticsDtos.Distribucion;
import mx.saludocupacional.portal.analytics.dto.AnalyticsDtos.Insight;
import mx.saludocupacional.portal.analytics.dto.AnalyticsDtos.RankingPredio;
import mx.saludocupacional.portal.analytics.dto.AnalyticsDtos.ResumenEjecutivo;
import mx.saludocupacional.portal.shared.service.ThresholdService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Genera los hallazgos que acompañan al dashboard.
 *
 * <p>Cada frase se deriva de una cifra concreta del periodo consultado; ninguna
 * se redacta de antemano. Si los datos no sustentan un hallazgo, no se muestra.
 */
@Service
@RequiredArgsConstructor
public class InsightEngine {

    private static final String CRITICA = "CRITICA";
    private static final String ADVERTENCIA = "ADVERTENCIA";
    private static final String INFORMATIVA = "INFORMATIVA";
    private static final String POSITIVA = "POSITIVA";

    private final AnalyticsService analytics;
    private final ThresholdService umbrales;

    /** Hallazgos del periodo, ordenados de mayor a menor relevancia. */
    @Transactional(readOnly = true)
    public List<Insight> generar(int anio, Integer mes, Long predioId) {
        List<Insight> hallazgos = new ArrayList<>();
        ResumenEjecutivo resumen = analytics.resumen(anio, mes, predioId);

        if (resumen.atenciones() == 0) {
            hallazgos.add(new Insight(INFORMATIVA,
                    "Aún no hay atenciones capturadas para %s.".formatted(resumen.periodo())));
            return hallazgos;
        }

        agregarVariaciones(hallazgos, resumen);
        agregarConcentracionDePredio(hallazgos, anio, mes, resumen);
        agregarCausaPrincipal(hallazgos, anio, mes, predioId, resumen);
        agregarComparativoDeAccidentes(hallazgos, anio, mes, predioId);
        agregarAptitud(hallazgos, resumen);

        return hallazgos;
    }

    private void agregarVariaciones(List<Insight> hallazgos, ResumenEjecutivo resumen) {
        BigDecimal relevante = umbrales.valor(ThresholdService.VARIACION_RELEVANTE);

        resumen.kpis().stream()
                .filter(kpi -> kpi.variacion() != null)
                .filter(kpi -> kpi.variacion().abs().compareTo(relevante) >= 0)
                .forEach(kpi -> {
                    boolean subio = kpi.variacion().signum() > 0;
                    hallazgos.add(new Insight(
                            subio ? ADVERTENCIA : POSITIVA,
                            "%s %s %s%% respecto al mes anterior.".formatted(
                                    kpi.etiqueta(),
                                    subio ? "aumentaron" : "disminuyeron",
                                    kpi.variacion().abs())));
                });
    }

    private void agregarConcentracionDePredio(List<Insight> hallazgos, int anio, Integer mes,
                                              ResumenEjecutivo resumen) {
        List<RankingPredio> ranking = analytics.ranking(anio, mes);
        if (ranking.isEmpty() || resumen.atenciones() == 0) {
            return;
        }
        RankingPredio primero = ranking.get(0);
        if (primero.atenciones() == 0) {
            return;
        }
        double proporcion = primero.atenciones() * 100.0 / resumen.atenciones();
        if (proporcion >= 20) {
            hallazgos.add(new Insight(
                    "CRITICO".equals(primero.riesgo()) ? CRITICA : ADVERTENCIA,
                    "%s concentra el %.1f%% de las atenciones del periodo.".formatted(
                            primero.predio(), proporcion)));
        }

        ranking.stream()
                .filter(p -> "CRITICO".equals(p.riesgo()))
                .findFirst()
                .ifPresent(p -> hallazgos.add(new Insight(CRITICA,
                        "%s acumula %,d días de incapacidad y está clasificado en riesgo crítico."
                                .formatted(p.predio(), p.diasIncapacidad()))));
    }

    private void agregarCausaPrincipal(List<Insight> hallazgos, int anio, Integer mes,
                                       Long predioId, ResumenEjecutivo resumen) {
        List<Distribucion> causas = analytics.topCausas(anio, mes, predioId, 3);
        if (causas.isEmpty()) {
            return;
        }
        Distribucion principal = causas.get(0);
        hallazgos.add(new Insight(INFORMATIVA,
                "%s es la principal causa de atención, con %,d casos (%s%% del total)."
                        .formatted(principal.categoria(), principal.valor(), principal.porcentaje())));

        if (causas.size() >= 3) {
            BigDecimal suma = causas.stream()
                    .map(Distribucion::porcentaje)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            hallazgos.add(new Insight(INFORMATIVA,
                    "Las tres principales causas concentran el %s%% de las atenciones.".formatted(suma)));
        }
    }

    private void agregarComparativoDeAccidentes(List<Insight> hallazgos, int anio, Integer mes, Long predioId) {
        List<Distribucion> causas = analytics.causasDeAccidente(anio, mes, predioId);
        if (causas.isEmpty()) {
            return;
        }
        Distribucion principal = causas.get(0);
        hallazgos.add(new Insight(ADVERTENCIA,
                "%s es el mecanismo más frecuente de accidente, con %,d casos."
                        .formatted(principal.categoria(), principal.valor())));
    }

    private void agregarAptitud(List<Insight> hallazgos, ResumenEjecutivo resumen) {
        if (resumen.examenes() == 0) {
            return;
        }
        double noAptos = resumen.examenesNoAptos() * 100.0 / resumen.examenes();
        if (noAptos >= 15) {
            hallazgos.add(new Insight(ADVERTENCIA,
                    "El %.1f%% de los exámenes resultó no apto; conviene revisar el proceso de selección."
                            .formatted(noAptos)));
        } else if (noAptos <= 5) {
            hallazgos.add(new Insight(POSITIVA,
                    "Solo el %.1f%% de los exámenes resultó no apto.".formatted(noAptos)));
        }
    }
}
