package com.onest.app.catalog.predio.dto;

/** Fila del catálogo de predios (sitios físicos) para el dashboard analítico. Ver docs/ords-predio-cuenta.sql. */
public record PredioDto(Long predioId, String nombre) {
}
