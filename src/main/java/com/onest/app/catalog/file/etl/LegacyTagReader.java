package com.onest.app.catalog.file.etl;

import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * Lectura de la tabla legacy {@code servicioMedico.tags} en MariaDB (EAV:
 * nss + type=nombre de campo + content=valor). Solo lectura.
 */
@Repository
@Profile("etl")
public class LegacyTagReader {

    /**
     * Muestra representativa: primeras filas, ultimas filas, contenido vacio,
     * el contenido mas largo (stress del CLOB), grupo PRETEST (el mas comun) y
     * firma digital base64 (drawdataUrlPRETEST). Ver docs/plan-etl-migracion-files.md
     * seccion 9 (tags EAV, corregido 2026-08-13) y tags-salud.sql.
     */
    private static final String SAMPLE_IDS_SQL = """
            (SELECT id FROM tags ORDER BY id LIMIT 20)
            UNION (SELECT id FROM tags ORDER BY id DESC LIMIT 10)
            UNION (SELECT id FROM tags WHERE content IS NULL OR content = '' LIMIT 10)
            UNION (SELECT id FROM tags WHERE LENGTH(content) = (SELECT MAX(LENGTH(content)) FROM tags) LIMIT 3)
            UNION (SELECT id FROM tags WHERE type LIKE '%PRETEST%' ORDER BY id LIMIT 10)
            UNION (SELECT id FROM tags WHERE type = 'drawdataUrlPRETEST' ORDER BY id LIMIT 5)
            """;

    private static final RowMapper<LegacyTagRow> ROW_MAPPER = (rs, i) -> new LegacyTagRow(
            rs.getLong("id"),
            rs.getString("nss"),
            rs.getString("type"),
            rs.getString("content"));

    private final JdbcTemplate legacyJdbc;

    public LegacyTagReader(@Qualifier("legacy") JdbcTemplate legacyJdbc) {
        this.legacyJdbc = legacyJdbc;
    }

    public List<Long> sampleIds() {
        return legacyJdbc.queryForList(SAMPLE_IDS_SQL, Long.class);
    }

    public List<LegacyTagRow> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", ids.stream().map(id -> "?").toList());
        return legacyJdbc.query(
                "SELECT id, nss, type, content FROM tags WHERE id IN (" + placeholders + ") ORDER BY id",
                ROW_MAPPER,
                ids.toArray());
    }

    public long minId() {
        Long v = legacyJdbc.queryForObject("SELECT MIN(id) FROM tags", Long.class);
        return v == null ? 0 : v;
    }

    public long maxId() {
        Long v = legacyJdbc.queryForObject("SELECT MAX(id) FROM tags", Long.class);
        return v == null ? 0 : v;
    }

    public List<LegacyTagRow> findByIdRange(long fromIdInclusive, long toIdInclusive) {
        return legacyJdbc.query(
                "SELECT id, nss, type, content FROM tags WHERE id BETWEEN ? AND ? ORDER BY id",
                ROW_MAPPER,
                fromIdInclusive, toIdInclusive);
    }
}
