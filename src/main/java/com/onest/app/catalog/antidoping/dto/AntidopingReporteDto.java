package com.onest.app.catalog.antidoping.dto;

/**
 * Fila del reporte de antidoping por rango de fechas (todas las NSS, no una sola).
 * Equivale a cada elemento de Datos de consulta_antidoping_fecha, pensado para el
 * dashboard. Ver docs/ords-antidoping-dashboard.sql.
 */
public record AntidopingReporteDto(
        String idRegistro,
        String fechaRegistro,
        String nss,
        String nombre,
        String rfc,
        String curp,
        String folio,
        String tipoPrueba,
        String sustancia,
        String resultado,
        String statusConclusion,
        String usuario
) {
}
