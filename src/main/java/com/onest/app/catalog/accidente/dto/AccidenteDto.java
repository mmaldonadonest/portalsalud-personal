package com.onest.app.catalog.accidente.dto;

/**
 * Accidente de trabajo (fila de la lista). Equivale a Datos de consulta_accidente
 * (docs/ords-accidentes.sql BLOQUE 3).
 */
public record AccidenteDto(
        String idRegistro,
        String fechaRegistro,
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
