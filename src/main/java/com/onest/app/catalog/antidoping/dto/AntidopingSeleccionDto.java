package com.onest.app.catalog.antidoping.dto;

/**
 * Fila del historial de selecciones aleatorias de Antidoping (quien fue elegido y cuando).
 * NO es el resultado de la prueba (ver AntidopingDto) - es la traza de la ronda de seleccion
 * en si, ver docs/ords-antidoping-seleccion.sql.
 */
public record AntidopingSeleccionDto(
        String idRegistro,
        String fechaSeleccion,
        String nss,
        String tamanoPool,
        String usuario
) {
}
