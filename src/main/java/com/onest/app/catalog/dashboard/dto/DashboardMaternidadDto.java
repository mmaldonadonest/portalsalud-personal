package com.onest.app.catalog.dashboard.dto;

import java.util.List;

/**
 * KPIs de seguimiento de maternidad en el rango [fechaInicial,fechaFinal], a partir de
 * Servcio/consulta_maternidad_fecha (docs/ords-maternidad-dashboard.sql).
 */
public record DashboardMaternidadDto(
        String fechaInicial,
        String fechaFinal,
        /** Seguimientos (registros) en el rango. */
        long totalSeguimientos,
        /** Personas (NSS distintos): una misma persona lleva varios seguimientos. */
        long totalPersonas,
        /** Casos con reincorporacion registrada. */
        long totalReincorporadas,
        /** Casos con proxima revision dentro de los siguientes 30 dias desde hoy. */
        long revisionesProximas,
        double promedioSemanasGestacion,
        List<ConteoSimpleDto> porEstatus,
        List<ConteoSimpleDto> porPredio,
        /** Semanas de gestacion agrupadas por trimestre (1: 1-13, 2: 14-27, 3: 28+). */
        List<ConteoSimpleDto> porTrimestre,
        List<PuntoMensualDto> tendenciaMensual,
        /** Partos probables por mes (fecha_probable_parto), para anticipar ausencias. */
        List<PuntoMensualDto> partosPorMes
) {
}
