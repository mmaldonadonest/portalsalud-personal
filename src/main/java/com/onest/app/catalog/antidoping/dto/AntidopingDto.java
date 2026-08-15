package com.onest.app.catalog.antidoping.dto;

/**
 * Antidoping/alcoholimetria (fila de la lista). Equivale a Datos de
 * consulta_antidoping (docs/ords-antidoping.sql BLOQUE 4).
 */
public record AntidopingDto(
        String idRegistro,
        String fechaRegistro,
        String folio,
        String tipoPrueba,
        String sustancia,
        String resultado,
        String statusConclusion,
        String observaciones,
        String usuario
) {
}
