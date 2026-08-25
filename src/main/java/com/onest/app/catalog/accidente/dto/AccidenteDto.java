package com.onest.app.catalog.accidente.dto;

/**
 * Accidente de trabajo (fila de la lista). Equivale a Datos de consulta_accidente
 * (docs/ords-accidentes.sql BLOQUE 3).
 *
 * <p>{@code estado} NO viene del WS de accidentes - se calcula en {@code AccidenteService}
 * a partir del historial de seguimiento (docs/ords-accidentes-seguimiento.sql): "CERRADO"
 * si existe al menos un seguimiento con TIPO=CIERRE, "ABIERTO" si no.
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
        String usuario,
        String estado
) {
    public AccidenteDto withEstado(String nuevoEstado) {
        return new AccidenteDto(idRegistro, fechaRegistro, fechaAccidente, tipoRiesgo, causaRt, diagnostico, sdi,
                statusCalificacion, costo, observaciones, usuario, nuevoEstado);
    }
}
