package com.onest.app.catalog.dashboard.dto;

/** Punto de la tendencia mensual (mes en formato "yyyy-MM", o "Sin fecha" para filas sin fecha parseable). */
public record PuntoMensualDto(String mes, long cantidad) {
}
