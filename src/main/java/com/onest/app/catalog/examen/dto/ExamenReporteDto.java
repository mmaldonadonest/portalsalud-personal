package com.onest.app.catalog.examen.dto;

/**
 * Fila del reporte de dictamenes de examen por rango de fechas (todas las NSS).
 * Equivale a cada elemento de Datos de consulta_examen_fecha, pensado para el
 * dashboard. Ver docs/ords-examen-dashboard.sql.
 */
public record ExamenReporteDto(
        String idRegistro,
        String fechaRegistro,
        String nss,
        String nombre,
        String rfc,
        String curp,
        String apto,
        String noApto,
        String aptoCondicionado,
        String aptoRestringido
) {
}
