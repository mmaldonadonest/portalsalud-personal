package com.onest.app.catalog.dashboard.dto;

import java.util.List;

/**
 * Resumen General: compuesto de los 4 dashboards ya existentes (Incapacidades,
 * Accidentabilidad, Consulta/Morbilidad, Examen). No tiene WS propio - combina
 * lo que ya devuelven los otros 4 endpoints. Ver docs/plan-tareas-concretas.html.
 */
public record DashboardResumenGeneralDto(
        String fechaInicial,
        String fechaFinal,
        long totalIncapacidades,
        long totalAccidentes,
        long totalConsultas,
        long totalExamenes,
        long totalGeneral,
        List<PuntoMensualCombinadoDto> tendenciaMensual
) {
}
