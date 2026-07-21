package com.onest.app.catalog.file.dto;

import java.time.LocalDateTime;

/**
 * Metadatos de un archivo adjunto (MED_FILE) para listados.
 */
public record MedFileMeta(
        Long id,
        String name,
        Long sizeBytes,
        LocalDateTime dateUpload,
        String mimeType
) {
}
