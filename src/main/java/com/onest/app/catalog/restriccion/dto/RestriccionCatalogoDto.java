package com.onest.app.catalog.restriccion.dto;

/**
 * Una entrada del catálogo fijo RES-01..RES-16 (no es una tabla, vive como
 * constante en RestriccionService - ver docs/ords-restriccion.sql NOTA-1).
 */
public record RestriccionCatalogoDto(String codigo, String etiqueta) {
}
