package com.onest.app.catalog.dashboard.dto;

/** Agregado por categoria (ramo, rubro, estado de dictamen, etc.). */
public record ConteoDto(String clave, long cantidad, long diasAutorizados, double costo) {
}
