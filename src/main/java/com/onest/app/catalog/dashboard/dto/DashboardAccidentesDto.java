package com.onest.app.catalog.dashboard.dto;

import java.util.List;

/**
 * KPIs de accidentes de trabajo para el dashboard ligero, calculados en el rango
 * [fechaInicial,fechaFinal] a partir de Servcio/consulta_accidentes_fecha (ver
 * docs/ords-accidentes-dashboard.sql).
 */
public record DashboardAccidentesDto(
        String fechaInicial,
        String fechaFinal,
        long totalAccidentes,
        double totalCosto,
        List<ConteoCostoDto> porTipoRiesgo,
        List<ConteoCostoDto> porCausaRt,
        List<ConteoCostoDto> porStatusCalificacion,
        /** Desglose por predio (via el mapeo cuenta->predio). Alimenta el ranking del
         *  Dashboard Ejecutivo y el modulo Vista por Predio. */
        List<ConteoCostoDto> porPredio,
        List<PuntoMensualDto> tendenciaMensual,
        /** Predio x tipo de riesgo, para la barra apilada del modulo Accidentabilidad. */
        List<ConteoCruzadoDto> porPredioTipo,
        /** Accidentes por mes y tipo de riesgo (cantidad = accidentes). */
        List<SerieMensualDto> tipoMensual
) {
}
