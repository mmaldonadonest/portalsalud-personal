package mx.saludocupacional.portal.analytics.dto;

import java.math.BigDecimal;
import java.util.List;

/** Estructuras que entrega la capa analítica al dashboard y a los reportes. */
public final class AnalyticsDtos {

    private AnalyticsDtos() {
    }

    /**
     * Indicador con su comparación contra el periodo anterior.
     *
     * @param variacion diferencia porcentual, nula cuando no hay base de comparación
     */
    public record Kpi(
            String clave,
            String etiqueta,
            long valor,
            BigDecimal variacion,
            String detalle
    ) {
    }

    /** Resumen ejecutivo del periodo consultado. */
    public record ResumenEjecutivo(
            int anio,
            Integer mes,
            String periodo,
            long atenciones,
            long personasIncapacitadas,
            long diasIncapacidad,
            long horasNoTrabajadas,
            long accidentes,
            long accidentesLaborales,
            long accidentesTrayecto,
            long examenes,
            long examenesAptos,
            long examenesNoAptos,
            long examenesCondicionados,
            long pruebasAntidoping,
            long casosMaternidad,
            BigDecimal costoIncapacidades,
            List<Kpi> kpis
    ) {
    }

    /** Serie mensual de un indicador. */
    public record SerieMensual(
            String indicador,
            List<String> etiquetas,
            List<Long> valores
    ) {
    }

    /** Fila del ranking de predios con su nivel de riesgo. */
    public record RankingPredio(
            Long predioId,
            String predio,
            long atenciones,
            long incapacidades,
            long diasIncapacidad,
            long accidentes,
            long examenes,
            BigDecimal costo,
            String riesgo
    ) {
    }

    /** Distribución de un total entre categorías. */
    public record Distribucion(
            String categoria,
            long valor,
            BigDecimal porcentaje
    ) {
    }

    /**
     * Hallazgo derivado de los datos.
     *
     * @param severidad CRITICA, ADVERTENCIA, INFORMATIVA o POSITIVA
     */
    public record Insight(
            String severidad,
            String texto
    ) {
    }
}
