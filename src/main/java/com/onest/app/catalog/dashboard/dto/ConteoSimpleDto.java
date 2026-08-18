package com.onest.app.catalog.dashboard.dto;

/** Agregado por categoria, solo conteo (sin costo ni dias - ej. consultas medicas). */
public record ConteoSimpleDto(String clave, long cantidad) {
}
