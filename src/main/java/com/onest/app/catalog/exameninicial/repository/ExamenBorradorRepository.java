package com.onest.app.catalog.exameninicial.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.StringReader;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Acceso JDBC a SERV_MED_EXAMEN_BORRADOR (base del portal). Un solo borrador vivo por NSS:
 * el ultimo con ESTATUS='BORRADOR'. Los finalizados se conservan como constancia de que el
 * examen se capturo desde el modulo inicial (fecha y usuario), no como copia del examen.
 */
@Repository
public class ExamenBorradorRepository {

    /** Borrador tal como esta en la tabla. {@code datos} = clave -> valor (JSON del CLOB). */
    public record Borrador(long id, int hojaActual, Map<String, String> datos,
                           LocalDateTime updatedAt, String updatedBy) {
    }

    /** Constancia de un examen finalizado desde este modulo. */
    public record Finalizado(LocalDateTime fecha, String usuario) {
    }

    private static final TypeReference<Map<String, String>> MAPA = new TypeReference<>() { };

    private final JdbcTemplate jdbc;
    private final ObjectMapper json = new ObjectMapper();

    public ExamenBorradorRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Borrador> buscar(String nss) {
        List<Borrador> rows = jdbc.query(
                "SELECT ID, HOJA_ACTUAL, DATOS, UPDATED_AT, UPDATED_BY FROM SERV_MED_EXAMEN_BORRADOR"
                        + " WHERE NSS = ? AND ESTATUS = 'BORRADOR' ORDER BY ID DESC FETCH FIRST 1 ROWS ONLY",
                (rs, i) -> new Borrador(rs.getLong("ID"), rs.getInt("HOJA_ACTUAL"), leerDatos(rs),
                        rs.getTimestamp("UPDATED_AT") == null ? null : rs.getTimestamp("UPDATED_AT").toLocalDateTime(),
                        rs.getString("UPDATED_BY")),
                nss);
        return rows.stream().findFirst();
    }

    public Optional<Finalizado> ultimoFinalizado(String nss) {
        List<Finalizado> rows = jdbc.query(
                "SELECT FINALIZADO_AT, FINALIZADO_BY FROM SERV_MED_EXAMEN_BORRADOR"
                        + " WHERE NSS = ? AND ESTATUS = 'FINALIZADO' ORDER BY FINALIZADO_AT DESC FETCH FIRST 1 ROWS ONLY",
                (rs, i) -> new Finalizado(rs.getTimestamp("FINALIZADO_AT").toLocalDateTime(), rs.getString("FINALIZADO_BY")),
                nss);
        return rows.stream().findFirst();
    }

    /** Crea o actualiza el borrador vivo del NSS con el mapa completo de datos. */
    public void guardar(String nss, int hojaActual, Map<String, String> datos, String usuario) {
        String cuerpo;
        try {
            cuerpo = json.writeValueAsString(datos);
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo serializar el borrador", ex);
        }
        Optional<Borrador> actual = buscar(nss);
        if (actual.isPresent()) {
            jdbc.update(con -> {
                PreparedStatement ps = con.prepareStatement(
                        "UPDATE SERV_MED_EXAMEN_BORRADOR SET HOJA_ACTUAL = ?, DATOS = ?, UPDATED_AT = SYSTIMESTAMP, UPDATED_BY = ?"
                                + " WHERE ID = ?");
                ps.setInt(1, hojaActual);
                ps.setCharacterStream(2, new StringReader(cuerpo));
                ps.setString(3, usuario);
                ps.setLong(4, actual.get().id());
                return ps;
            });
            return;
        }
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO SERV_MED_EXAMEN_BORRADOR (NSS, TIPO_EXAMEN, ESTATUS, HOJA_ACTUAL, DATOS, CREATED_BY, UPDATED_BY)"
                            + " VALUES (?, 'INICIAL', 'BORRADOR', ?, ?, ?, ?)");
            ps.setString(1, nss);
            ps.setInt(2, hojaActual);
            ps.setCharacterStream(3, new StringReader(cuerpo));
            ps.setString(4, usuario);
            ps.setString(5, usuario);
            return ps;
        });
    }

    /** Cierra el borrador vivo: queda como constancia FINALIZADO. */
    public void finalizar(String nss, String usuario) {
        jdbc.update("UPDATE SERV_MED_EXAMEN_BORRADOR SET ESTATUS = 'FINALIZADO', FINALIZADO_AT = SYSTIMESTAMP,"
                + " FINALIZADO_BY = ?, UPDATED_AT = SYSTIMESTAMP, UPDATED_BY = ? WHERE NSS = ? AND ESTATUS = 'BORRADOR'",
                usuario, usuario, nss);
    }

    private Map<String, String> leerDatos(ResultSet rs) throws SQLException {
        Clob clob = rs.getClob("DATOS");
        if (clob == null) {
            return new LinkedHashMap<>();
        }
        try {
            String s = clob.getSubString(1, (int) clob.length());
            if (s == null || s.isBlank()) {
                return new LinkedHashMap<>();
            }
            return json.readValue(s, MAPA);
        } catch (java.io.IOException ex) {
            throw new SQLException("Borrador con JSON ilegible", ex);
        }
    }
}
