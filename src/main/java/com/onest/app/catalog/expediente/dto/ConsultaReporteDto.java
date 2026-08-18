package com.onest.app.catalog.expediente.dto;

/**
 * Fila del reporte de consultas medicas por rango de fechas (todas las NSS, no una sola).
 * Equivale a cada elemento de Datos de consulta_medica_fecha, pensado para el dashboard.
 * Ver docs/ords-consulta-dashboard.sql.
 */
public record ConsultaReporteDto(
        String idConsulta,
        String fechaConsulta,
        String nss,
        String nombre,
        String rfc,
        String curp,
        String tipoConsulta,
        String areaAccidente,
        String areaInvolucrada,
        String causa
) {
    /** Mismo criterio que ConsultaDto.esAccidente() - ver ese comentario. */
    public boolean esAccidente() {
        if (tipoConsulta == null) {
            return false;
        }
        String t = tipoConsulta.toLowerCase();
        return t.contains("accidente") || t.contains("emergencia");
    }
}
