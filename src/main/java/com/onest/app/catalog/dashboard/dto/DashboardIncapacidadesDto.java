package com.onest.app.catalog.dashboard.dto;

import java.util.List;

/**
 * KPIs de incapacidades para el dashboard ligero, calculados en el rango [fechaInicial,fechaFinal]
 * a partir de Servcio/consulta_incapacidades_fecha (el unico WS agregado real de "todas las NSS
 * en un rango" disponible hoy - ver docs/plan-tareas-concretas.html).
 */
public record DashboardIncapacidadesDto(
        String fechaInicial,
        String fechaFinal,
        long totalIncapacidades,
        long totalDiasAutorizados,
        double totalCosto,
        List<ConteoDto> porRamo,
        List<ConteoDto> porRubro,
        List<ConteoDto> porEstadoDictamen,
        List<PuntoMensualDto> tendenciaMensual
) {
}
