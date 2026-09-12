package com.onest.app.catalog.maternidad.dto;

/**
 * Fila del reporte de seguimientos de maternidad por rango de fechas (todas las NSS),
 * pensado para el dashboard. Ver docs/ords-maternidad-dashboard.sql.
 */
public record MaternidadReporteDto(
        String idRegistro,
        String fechaRegistro,
        String nss,
        String nombre,
        /** Cuenta del empleado; de aqui sale el predio via PredioService.predioFinoPorCuenta(). */
        String cuenta,
        String semanasGestacion,
        String fechaProbableParto,
        String restriccionesLaborales,
        String proximaRevision,
        String estatus,
        String incapacidad,
        String reincorporacion,
        String usuario
) {
}
