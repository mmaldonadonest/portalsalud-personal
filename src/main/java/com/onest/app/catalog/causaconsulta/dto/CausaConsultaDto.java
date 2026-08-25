package com.onest.app.catalog.causaconsulta.dto;

/**
 * Fila del catálogo administrable de causas de consulta (23 predefinidas + "Otro").
 * Ver docs/ords-causa-consulta.sql.
 */
public record CausaConsultaDto(
        String idRegistro,
        String nombre,
        String activo,
        String fechaAlta,
        String fechaBaja,
        String usuario
) {
}
