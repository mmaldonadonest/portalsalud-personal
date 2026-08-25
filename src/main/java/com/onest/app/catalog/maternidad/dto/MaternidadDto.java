package com.onest.app.catalog.maternidad.dto;

/**
 * Registro de seguimiento de maternidad (fila de la lista/historial). Equivale a Datos de
 * consulta_maternidad (docs/ords-maternidad.sql BLOQUE 3).
 */
public record MaternidadDto(
        String idRegistro,
        String fechaRegistro,
        String semanasGestacion,
        String fechaProbableParto,
        String restriccionesLaborales,
        String proximaRevision,
        String observaciones,
        String estatus,
        String incapacidad,
        String reincorporacion,
        String usuario
) {
}
