package com.onest.app.catalog.examen.dto;

/**
 * Contacto de emergencia del Examen (hasta 3 por persona). EAV via MED_TAG,
 * TAG_GROUP='CONTACTO_EMERGENCIA', TYPE=contactoEmer{campo}{indice} (confirmado
 * contra los datos migrados via ETL, ver docs/normalizacion-encoding-med-tag-detalle.csv).
 */
public record ContactoEmergenciaDto(
        int indice,
        String nombre,
        String apellidoPaterno,
        String apellidoMaterno,
        String parentesco,
        String telefono
) {
}
