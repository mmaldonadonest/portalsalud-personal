package com.onest.app.catalog.pretest.repository;

import java.io.StringReader;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

/**
 * Acceso JDBC a SERV_MED_TAG (EAV). Replica el patron del legacy:
 * lectura = ultimo por (nss,type) [ORDER BY id DESC]; escritura = DELETE+INSERT
 * por campo (insertDatsExpFis). SERV_MED_TAG.CONTENT es CLOB (soporta la firma base64).
 */
@Repository
public class MedTagRepository {

    private final JdbcTemplate jdbc;

    public MedTagRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Ultimo CONTENT por TYPE para un NSS, filtrando TYPE LIKE %suffix. */
    public Map<String, String> latestByNssAndTypeSuffix(String nss, String suffix) {
        Map<String, String> map = new LinkedHashMap<>();
        jdbc.query(
                "SELECT TYPE, CONTENT FROM ("
                        + "  SELECT TYPE, CONTENT, ROW_NUMBER() OVER (PARTITION BY TYPE ORDER BY ID DESC) rn"
                        + "  FROM SERV_MED_TAG WHERE NSS = ? AND TYPE LIKE ?"
                        + ") WHERE rn = 1",
                (RowCallbackHandler) rs -> map.put(rs.getString("TYPE"), rs.getString("CONTENT")),
                nss, "%" + suffix);
        return map;
    }

    /** Ultimo CONTENT por TYPE para un NSS, filtrando TYPE LIKE prefix%. */
    public Map<String, String> latestByNssAndTypePrefix(String nss, String prefix) {
        Map<String, String> map = new LinkedHashMap<>();
        jdbc.query(
                "SELECT TYPE, CONTENT FROM ("
                        + "  SELECT TYPE, CONTENT, ROW_NUMBER() OVER (PARTITION BY TYPE ORDER BY ID DESC) rn"
                        + "  FROM SERV_MED_TAG WHERE NSS = ? AND TYPE LIKE ?"
                        + ") WHERE rn = 1",
                (RowCallbackHandler) rs -> map.put(rs.getString("TYPE"), rs.getString("CONTENT")),
                nss, prefix + "%");
        return map;
    }

    /** Un renglon de SERV_MED_TAG con su fecha (para historiales: no se queda solo con el ultimo por TYPE). */
    public record Tag(String type, String content, java.time.LocalDateTime creado, String creadoPor) {
    }

    /** Todos los TYPE LIKE prefix% del NSS, el mas reciente primero. */
    public java.util.List<Tag> todosPorPrefijo(String nss, String prefix) {
        return jdbc.query(
                "SELECT TYPE, CONTENT, CREATED_AT, CREATED_BY FROM SERV_MED_TAG WHERE NSS = ? AND TYPE LIKE ? ORDER BY ID DESC",
                (rs, i) -> new Tag(rs.getString(1), rs.getString(2),
                        rs.getTimestamp(3) == null ? null : rs.getTimestamp(3).toLocalDateTime(), rs.getString(4)),
                nss, prefix + "%");
    }

    /** DELETE + INSERT del valor de un campo (como insertDatsExpFis). */
    /** Fecha del ultimo registro guardado para el NSS con TYPE LIKE %suffix (null si no hay ninguno). */
    public java.time.LocalDateTime ultimoGuardado(String nss, String suffix) {
        return jdbc.query("SELECT MAX(CREATED_AT) FROM SERV_MED_TAG WHERE NSS = ? AND TYPE LIKE ?",
                rs -> rs.next() && rs.getTimestamp(1) != null ? rs.getTimestamp(1).toLocalDateTime() : null,
                nss, "%" + suffix);
    }

    public void upsert(String nss, String type, String content, String tagGroup, String createdBy) {
        jdbc.update("DELETE FROM SERV_MED_TAG WHERE NSS = ? AND TYPE = ?", nss, type);
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO SERV_MED_TAG (NSS, TYPE, CONTENT, TAG_GROUP, CREATED_BY) VALUES (?,?,?,?,?)");
            ps.setString(1, nss);
            ps.setString(2, type);
            if (content == null) {
                ps.setNull(3, Types.CLOB);
            } else {
                ps.setCharacterStream(3, new StringReader(content));
            }
            ps.setString(4, tagGroup);
            ps.setString(5, createdBy);
            return ps;
        });
    }
}
