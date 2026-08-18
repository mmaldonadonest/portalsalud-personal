package com.onest.app.catalog.dashboard.dto;

import java.util.List;

/**
 * KPIs de antidoping/alcoholimetria para el dashboard ligero, calculados en el rango
 * [fechaInicial,fechaFinal] a partir de Servcio/consulta_antidoping_fecha (ver
 * docs/ords-antidoping-dashboard.sql).
 */
public record DashboardAntidopingDto(
        String fechaInicial,
        String fechaFinal,
        long totalPruebas,
        long totalPositivos,
        List<ConteoSimpleDto> porTipoPrueba,
        List<ConteoSimpleDto> porSustancia,
        List<ConteoSimpleDto> porResultado,
        List<ConteoSimpleDto> porStatusConclusion,
        List<PuntoMensualDto> tendenciaMensual
) {
}
