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
        List<PuntoMensualDto> tendenciaMensual
) {
}
