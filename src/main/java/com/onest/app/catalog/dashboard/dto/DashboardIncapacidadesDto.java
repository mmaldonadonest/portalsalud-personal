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
        /** NSS distintos en el rango. La tarjeta dice "Personas incapacitadas", y
         *  totalIncapacidades son REGISTROS: una misma persona puede tener varios. */
        long totalPersonas,
        long totalDiasAutorizados,
        double totalCosto,
        List<ConteoDto> porRamo,
        List<ConteoDto> porRubro,
        List<ConteoDto> porEstadoDictamen,
        /** Desglose por predio (via el mapeo cuenta->predio). Alimenta el ranking del
         *  Dashboard Ejecutivo y el modulo Vista por Predio. */
        List<ConteoDto> porPredio,
        List<PuntoMensualDto> tendenciaMensual,
        List<PuntoMensualDiasDto> tendenciaDiasMensual,
        /** Dias por mes y por ramo (cantidad = dias), para la barra apilada del modulo Incapacidades. */
        List<SerieMensualDto> diasRamoMensual,
        /** Personas (NSS distintos) por rubro IMSS / Interna / sin rubro. */
        List<ConteoSimpleDto> personasPorRubro,
        /** Personas (NSS distintos) por mes y por rubro. */
        List<SerieMensualDto> personasRubroMensual
) {
}
