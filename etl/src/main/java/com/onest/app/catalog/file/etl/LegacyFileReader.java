package com.onest.app.catalog.file.etl;

import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * Lectura de la tabla legacy {@code servicioMedico.files} en MariaDB. Solo
 * lectura: el ETL nunca escribe en el origen.
 */
@Repository
@Profile("etl")
public class LegacyFileReader {

    /**
     * Muestra representativa (~60 filas) que ejercita todos los caminos del ETL:
     * las 4 categorias funcionales, adjuntos de consulta (type = hash MD5),
     * los 3 nombres sin extension y los duplicados de contenido reales.
     * Ver docs/plan-etl-migracion-files.md seccion 6.
     */
    private static final String SAMPLE_IDS_SQL = """
            (SELECT id FROM files WHERE type='examen_medico'    ORDER BY id LIMIT 15)
            UNION (SELECT id FROM files WHERE type='laboratorio'      ORDER BY id LIMIT 10)
            UNION (SELECT id FROM files WHERE type='nota_medica'      ORDER BY id LIMIT 10)
            UNION (SELECT id FROM files WHERE type='nota_incapacidad' ORDER BY id LIMIT 10)
            UNION (SELECT id FROM files WHERE LENGTH(type)=32         ORDER BY id LIMIT 5)
            UNION (SELECT id FROM files WHERE name NOT LIKE '%.%')
            UNION (SELECT id FROM files GROUP BY MD5(url) HAVING COUNT(*)>1 ORDER BY id LIMIT 4)
            """;

    private static final RowMapper<LegacyFileRow> ROW_MAPPER = (rs, i) -> new LegacyFileRow(
            rs.getLong("id"),
            rs.getString("nss"),
            rs.getString("name"),
            rs.getTimestamp("date_upload") != null ? rs.getTimestamp("date_upload").toLocalDateTime() : null,
            rs.getString("url"),
            rs.getString("type"));

    private final JdbcTemplate legacyJdbc;

    public LegacyFileReader(@Qualifier("legacy") JdbcTemplate legacyJdbc) {
        this.legacyJdbc = legacyJdbc;
    }

    public List<Long> sampleIds() {
        return legacyJdbc.queryForList(SAMPLE_IDS_SQL, Long.class);
    }

    public List<LegacyFileRow> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", ids.stream().map(id -> "?").toList());
        return legacyJdbc.query(
                "SELECT id, nss, name, date_upload, url, type FROM files WHERE id IN (" + placeholders + ") ORDER BY id",
                ROW_MAPPER,
                ids.toArray());
    }

    public long minId() {
        Long v = legacyJdbc.queryForObject("SELECT MIN(id) FROM files", Long.class);
        return v == null ? 0 : v;
    }

    public long maxId() {
        Long v = legacyJdbc.queryForObject("SELECT MAX(id) FROM files", Long.class);
        return v == null ? 0 : v;
    }

    /** Trae un rango de ids CON su {@code url} (unico punto donde se lee el base64 completo, por lote). */
    public List<LegacyFileRow> findByIdRange(long fromIdInclusive, long toIdInclusive) {
        return legacyJdbc.query(
                "SELECT id, nss, name, date_upload, url, type FROM files WHERE id BETWEEN ? AND ? ORDER BY id",
                ROW_MAPPER,
                fromIdInclusive, toIdInclusive);
    }
}
