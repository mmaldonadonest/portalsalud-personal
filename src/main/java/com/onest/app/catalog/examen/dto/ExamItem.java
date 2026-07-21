package com.onest.app.catalog.examen.dto;

/**
 * Campo de una seccion del examen. type = SINO (select Si/No, con OBS opcional) |
 * TEXT (texto libre) | DATE (fecha). name = clave punteada (p.ej. NEUROLOGIA.AVC).
 */
public record ExamItem(
        String type,
        String label,
        String fieldName,
        String value,
        String obsName,
        String obsValue
) {
}
