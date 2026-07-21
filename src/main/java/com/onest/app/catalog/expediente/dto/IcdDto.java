package com.onest.app.catalog.expediente.dto;

/**
 * Clave ICD (catalogo de diagnosticos). Equivale a cada elemento de showIcd.php.
 */
public record IcdDto(
        String claveId,
        String nombreClave
) {
}
