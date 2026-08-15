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
 * Acceso JDBC a MED_TAG (EAV). Replica el patron del legacy:
 * lectura = ultimo por (nss,type) [ORDER BY id DESC]; escritura = DELETE+INSERT
 * por campo (insertDatsExpFis). MED_TAG.CONTENT es CLOB (soporta la firma base64).
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
                        + "  FROM MED_TAG WHERE NSS = ? AND TYPE LIKE ?"
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
                        + "  FROM MED_TAG WHERE NSS = ? AND TYPE LIKE ?"
                        + ") WHERE rn = 1",
                (RowCallbackHandler) rs -> map.put(rs.getString("TYPE"), rs.getString("CONTENT")),
                nss, prefix + "%");
        return map;
    }

    /** DELETE + INSERT del valor de un campo (como insertDatsExpFis). */
    public void upsert(String nss, String type, String content, String tagGroup, String createdBy) {
        jdbc.update("DELETE FROM MED_TAG WHERE NSS = ? AND TYPE = ?", nss, type);
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO MED_TAG (NSS, TYPE, CONTENT, TAG_GROUP, CREATED_BY) VALUES (?,?,?,?,?)");
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
