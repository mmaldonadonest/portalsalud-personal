package com.onest.app.catalog.file.etl;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Escritura en {@code MED_TAG} (Oracle, destino del ETL de {@code servicioMedico.tags}).
 * Landing EAV fiel: sin normalizar (ver tags-salud.sql). {@code SOURCE_ID} guarda el
 * id original de MariaDB para idempotencia/reconciliacion (no hay unique constraint
 * en la tabla - se verifica por consulta antes de insertar, igual que el ETL de files).
 */
@Repository
@Profile("etl")
public class MedTagWriter {

    private final JdbcTemplate jdbc;

    public MedTagWriter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean existsBySourceId(long sourceId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM MED_TAG WHERE SOURCE_ID = ?", Integer.class, sourceId);
        return count != null && count > 0;
    }

    public void insert(String nss, String type, String content, long sourceId, String createdBy) {
        jdbc.update(
                "INSERT INTO MED_TAG (NSS, TYPE, CONTENT, TAG_GROUP, SOURCE_ID, MIGRATED_AT, CREATED_AT, CREATED_BY) "
                        + "VALUES (?, ?, ?, FN_MED_TAG_GROUP(?), ?, SYSTIMESTAMP, SYSTIMESTAMP, ?)",
                nss, type, content, type, sourceId, createdBy);
    }
}
