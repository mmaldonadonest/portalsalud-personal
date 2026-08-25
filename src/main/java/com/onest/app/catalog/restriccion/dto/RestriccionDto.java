package com.onest.app.catalog.restriccion.dto;

/**
 * Una restricción médica asignada a un NSS (fila del historial, insert-only).
 * Ver docs/ords-restriccion.sql.
 */
public record RestriccionDto(
        String idRegistro,
        String codigoRestriccion,
        String descripcion,
        String valorLimite,
        String unidad,
        String fechaInicio,
        String fechaFin,
        String fechaRevaloracion,
        String temporalidad,
        String observaciones,
        String medicoResponsable,
        String estatus,
        String fechaRegistro,
        String usuario
) {
}
