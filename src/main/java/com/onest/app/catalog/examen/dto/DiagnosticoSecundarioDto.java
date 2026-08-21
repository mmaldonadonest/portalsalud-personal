package com.onest.app.catalog.examen.dto;

/**
 * Diagnostico secundario del Examen (hasta 3 por persona). EAV via MED_TAG,
 * TAG_GROUP='DIAGNOSTICO_SECUNDARIO', TYPE=diagnosticoSecundario{indice}.
 * Mismo formato "codigo - descripcion" que ya usa addIcd() en el diagnostico principal.
 */
public record DiagnosticoSecundarioDto(int indice, String valor) {
}
