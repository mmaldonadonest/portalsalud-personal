package com.onest.app.catalog.dashboard.dto;

/**
 * Celda de un cruce de dos dimensiones (p.ej. predio x tipo de riesgo). Lista plana: la UI
 * pivota como necesite. Mas simple que un mapa anidado y se serializa sin sorpresas.
 */
public record ConteoCruzadoDto(String fila, String columna, long cantidad, double costo) {
}
