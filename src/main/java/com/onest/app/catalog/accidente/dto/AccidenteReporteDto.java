package com.onest.app.catalog.accidente.dto;

/**
 * Fila del reporte de accidentes por rango de fechas (todas las NSS, no una sola).
 * Equivale a cada elemento de Datos de consulta_accidentes_fecha, pensado para el
 * dashboard - no para el medico tratante. Ver docs/ords-accidentes-dashboard.sql.
 */
public record AccidenteReporteDto(
        String idRegistro,
        String fechaRegistro,
        String nss,
        String nombre,
        String rfc,
        String curp,
        String fechaAccidente,
        String tipoRiesgo,
        String causaRt,
        String diagnostico,
        String sdi,
        String statusCalificacion,
        String costo,
        String observaciones,
        String usuario
) {
}
