package com.onest.app.catalog.examen.repository;

import java.io.StringReader;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Acceso JDBC a SERV_MED_EXAMEN_HISTORICO (base del portal): una fila por examen finalizado,
 * desde el modulo general o el inicial. Ver db/sql/app_domain/examen-historico.sql.
 */
@Repository
public class ExamenHistoricoRepository {

    public static final String ORIGEN_INICIAL = "INICIAL";
    public static final String ORIGEN_GENERAL = "GENERAL";

    /** Resumen de un examen del historico (sin el JSON). */
    public record Registro(long id, String nss, String origen, String tipoExamen, String dictamen,
                           LocalDateTime fechaExamen, String usuario) {
    }

    private final JdbcTemplate jdbc;

    public ExamenHistoricoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long registrar(String nss, String origen, String tipoExamen, String dictamen, String usuario, String datosJson) {
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO SERV_MED_EXAMEN_HISTORICO (NSS, ORIGEN, TIPO_EXAMEN, DICTAMEN, USUARIO, DATOS) VALUES (?,?,?,?,?,?)");
            ps.setString(1, nss);
            ps.setString(2, origen);
            ps.setString(3, tipoExamen);
            ps.setString(4, dictamen);
            ps.setString(5, usuario);
            ps.setCharacterStream(6, new StringReader(datosJson == null ? "{}" : datosJson));
            return ps;
        });
        return 0;
    }

    /** Ultimo examen registrado de la persona, de cualquier origen. */
    public Optional<Registro> ultimo(String nss) {
        return listar(nss, 1).stream().findFirst();
    }

    /** Foto del ultimo examen inicial finalizado (C-075, 11-nov-2026): fecha y DATOS (JSON {clave: valor}). */
    public record Foto(LocalDateTime fecha, String datosJson) {
    }

    /**
     * C-075: el ultimo examen INICIAL con su foto, para complementar los demas examenes (Examen medico).
     * Vacio si la persona no tiene examen inicial en el portal.
     */
    public Optional<Foto> ultimoInicial(String nss) {
        List<Foto> rows = jdbc.query(
                "SELECT FECHA_EXAMEN, DATOS FROM SERV_MED_EXAMEN_HISTORICO WHERE NSS = ? AND ORIGEN = ?"
                        + " ORDER BY FECHA_EXAMEN DESC, ID DESC FETCH FIRST 1 ROWS ONLY",
                (rs, i) -> new Foto(rs.getTimestamp("FECHA_EXAMEN").toLocalDateTime(), rs.getString("DATOS")),
                nss, ORIGEN_INICIAL);
        return rows.stream().findFirst();
    }

    public boolean existeInicial(String nss) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM SERV_MED_EXAMEN_HISTORICO WHERE NSS = ? AND ORIGEN = ?", Integer.class, nss, ORIGEN_INICIAL);
        return n != null && n > 0;
    }

    public List<Registro> listar(String nss, int limite) {
        return jdbc.query(
                "SELECT ID, NSS, ORIGEN, TIPO_EXAMEN, DICTAMEN, FECHA_EXAMEN, USUARIO FROM SERV_MED_EXAMEN_HISTORICO"
                        + " WHERE NSS = ? ORDER BY FECHA_EXAMEN DESC, ID DESC FETCH FIRST ? ROWS ONLY",
                (rs, i) -> new Registro(rs.getLong("ID"), rs.getString("NSS"), rs.getString("ORIGEN"), rs.getString("TIPO_EXAMEN"),
                        rs.getString("DICTAMEN"), rs.getTimestamp("FECHA_EXAMEN").toLocalDateTime(), rs.getString("USUARIO")),
                nss, limite);
    }
}
