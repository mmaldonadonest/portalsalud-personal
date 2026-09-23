package com.onest.app.catalog.file.etl;

/** Fila cruda de la tabla legacy {@code servicioMedico.tags} (MariaDB, patron EAV). */
public record LegacyTagRow(
        long id,
        String nss,
        String type,
        String content
) {
}
