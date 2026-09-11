package mx.saludocupacional.portal.analytics;

import mx.saludocupacional.portal.IntegrationTestBase;
import mx.saludocupacional.portal.analytics.dto.AnalyticsDtos.ResumenEjecutivo;
import mx.saludocupacional.portal.analytics.service.AnalyticsService;
import mx.saludocupacional.portal.analytics.service.InsightEngine;
import mx.saludocupacional.portal.catalog.repository.AttentionCauseRepository;
import mx.saludocupacional.portal.catalog.repository.PredioRepository;
import mx.saludocupacional.portal.employee.domain.Employee;
import mx.saludocupacional.portal.employee.repository.EmployeeRepository;
import mx.saludocupacional.portal.morbidity.service.AttentionService;
import mx.saludocupacional.portal.morbidity.web.dto.AttentionDtos.AttentionRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprueba que la capa analítica resuma correctamente lo capturado.
 *
 * <p>Como el dashboard y los reportes leen de aquí, cualquier desviación en
 * estas cifras se propagaría a toda la plataforma.
 */
class AnalyticsTest extends IntegrationTestBase {

    private static final Long USUARIO = 1L;
    private static final int ANIO = 2026;

    @Autowired AnalyticsService analytics;
    @Autowired InsightEngine insights;
    @Autowired AttentionService atenciones;
    @Autowired EmployeeRepository empleados;
    @Autowired PredioRepository predios;
    @Autowired AttentionCauseRepository causas;

    @Test
    @DisplayName("Las atenciones capturadas se reflejan en el resumen del periodo")
    void resumenCuentaLasAtenciones() {
        Long predioId = predios.findByNombreIgnoreCase("TOLUCA").orElseThrow().getId();
        Long causaId = causas.findByNombreIgnoreCase("RESPIRATORIO").orElseThrow().getId();

        long antes = analytics.resumen(ANIO, 7, predioId).atenciones();

        atenciones.crear(new AttentionRequest(
                null, predioId, LocalDate.of(ANIO, 7, 15),
                causaId, null, null, false, null), USUARIO);

        ResumenEjecutivo despues = analytics.resumen(ANIO, 7, predioId);
        assertThat(despues.atenciones()).isEqualTo(antes + 1);
        assertThat(despues.periodo()).isEqualTo("JUL " + ANIO);
    }

    @Test
    @DisplayName("Las horas no trabajadas equivalen a los días por la jornada")
    void horasDerivanDeLosDias() {
        ResumenEjecutivo resumen = analytics.resumen(ANIO, null, null);
        assertThat(resumen.horasNoTrabajadas()).isEqualTo(resumen.diasIncapacidad() * 8);
    }

    @Test
    @DisplayName("La serie mensual siempre trae los doce meses")
    void serieCubreElAnioCompleto() {
        var serie = analytics.serie("atenciones", ANIO, null);

        assertThat(serie.etiquetas()).hasSize(12).startsWith("ENE").endsWith("DIC");
        assertThat(serie.valores()).hasSize(12);
    }

    @Test
    @DisplayName("El ranking incluye a todos los predios activos con su nivel de riesgo")
    void rankingClasificaLosPredios() {
        var ranking = analytics.ranking(ANIO, null);

        assertThat(ranking).isNotEmpty();
        assertThat(ranking).allSatisfy(p ->
                assertThat(p.riesgo()).isIn("CRITICO", "ALTO", "MEDIO", "BAJO"));
    }

    @Test
    @DisplayName("Sin datos del periodo, los hallazgos lo dicen en lugar de inventar")
    void sinDatosLoIndica() {
        var hallazgos = insights.generar(2099, 1, null);

        assertThat(hallazgos).hasSize(1);
        assertThat(hallazgos.get(0).texto()).contains("Aún no hay atenciones capturadas");
    }

    @Test
    @DisplayName("Los porcentajes de una distribución suman cien")
    void distribucionSumaCien() {
        Long predioId = predios.findByNombreIgnoreCase("AIFA").orElseThrow().getId();
        Long causaId = causas.findByNombreIgnoreCase("DIGESTIVO").orElseThrow().getId();

        atenciones.crear(new AttentionRequest(
                null, predioId, LocalDate.of(ANIO, 8, 3),
                causaId, null, null, false, null), USUARIO);

        var distribucion = analytics.topCausas(ANIO, 8, predioId, 10);

        assertThat(distribucion).isNotEmpty();
        double suma = distribucion.stream()
                .mapToDouble(d -> d.porcentaje().doubleValue())
                .sum();
        assertThat(suma).isCloseTo(100.0, org.assertj.core.data.Offset.offset(0.5));
    }
}
