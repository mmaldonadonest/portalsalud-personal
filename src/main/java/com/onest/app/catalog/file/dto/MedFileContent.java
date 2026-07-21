package com.onest.app.catalog.file.dto;

/**
 * Contenido binario de un archivo adjunto (MED_FILE) para descarga.
 */
public record MedFileContent(
        String name,
        String mimeType,
        byte[] content
) {
}
