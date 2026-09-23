package com.onest.app.catalog.file.dto;

/**
 * Datos minimos para servir/leer un archivo: nombre para descarga, mime y la
 * ruta relativa en el filesystem (STORAGE_PATH).
 */
public record StoredFileLocation(
        String name,
        String mimeType,
        String storagePath
) {
}
