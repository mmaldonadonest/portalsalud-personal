package com.onest.app.catalog.dashboard.dto;

/** Agregado por categoria sin dias autorizados (ej. accidentes: no aplica "dias"). */
public record ConteoCostoDto(String clave, long cantidad, double costo) {
}
