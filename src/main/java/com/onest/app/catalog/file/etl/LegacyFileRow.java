package com.onest.app.catalog.file.etl;

import java.time.LocalDateTime;

/** Fila cruda de la tabla legacy {@code servicioMedico.files} (MariaDB). */
public record LegacyFileRow(
        long id,
        String nss,
        String name,
        LocalDateTime dateUpload,
        String urlBase64,
        String type
) {
}
