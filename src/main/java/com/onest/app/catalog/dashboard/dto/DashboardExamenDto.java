package com.onest.app.catalog.dashboard.dto;

import java.util.List;

/**
 * KPIs de dictamenes de examen para el dashboard ligero, calculados en el rango
 * [fechaInicial,fechaFinal] a partir de Servcio/consulta_examen_fecha (ver
 * docs/ords-examen-dashboard.sql). SIN datos retroactivos - el historial arranco
 * vacio el 17 de agosto de 2026.
 */
public record DashboardExamenDto(
        String fechaInicial,
        String fechaFinal,
        long totalExamenes,
        long totalApto,
        long totalNoApto,
        long totalAptoCondicionado,
        long totalAptoRestringido,
        List<PuntoMensualDto> tendenciaMensual
) {
}
