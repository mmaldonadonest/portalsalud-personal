package com.onest.app.catalog.accidente.dto;

/**
 * Una entrada del historial de seguimiento de un accidente (TIPO=SEGUIMIENTO o CIERRE).
 * Insert-only - cada entrada es un registro nuevo, nunca se actualiza una existente.
 * Ver docs/ords-accidentes-seguimiento.sql.
 */
public record AccidenteSeguimientoDto(
        String idRegistro,
        String tipo,
        String fechaSeguimiento,
        String observaciones,
        String usuario
) {
}
