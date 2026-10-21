package com.onest.app.catalog.firma.repository;

import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/**
 * JDBC de las firmas digitalizadas (docs/portal-firmas-digitalizadas.sql), del aviso de privacidad
 * (docs/portal-aviso-privacidad-firma.sql) y de la vigencia del empleado en el biometrico.
 * Los estados se cambian con UPDATE; ninguna fila se borra.
 */
@Repository
public class FirmaRepository {

    public static final String TIPO_AVISO = "FIRMA_DIGITALIZADA";

    public record Firma(long id, String usuario, String nombre, long fileId, String origen, String estado,
                        Integer avisoVersion, LocalDateTime aceptadoAt, LocalDateTime createdAt, String createdBy,
                        LocalDateTime updatedAt, String updatedBy, String motivo) {
    }

    public record Aviso(int version, String titulo, String texto, boolean borrador) {
    }

    private static final String COLS = "ID, USUARIO, NOMBRE, FILE_ID, ORIGEN, ESTADO, AVISO_VERSION, ACEPTADO_AT, "
            + "CREATED_AT, CREATED_BY, UPDATED_AT, UPDATED_BY, MOTIVO";

    private static final RowMapper<Firma> MAPPER = (rs, i) -> new Firma(
            rs.getLong("ID"), rs.getString("USUARIO"), rs.getString("NOMBRE"), rs.getLong("FILE_ID"),
            rs.getString("ORIGEN"), rs.getString("ESTADO"),
            rs.getObject("AVISO_VERSION") == null ? null : rs.getInt("AVISO_VERSION"),
            rs.getTimestamp("ACEPTADO_AT") == null ? null : rs.getTimestamp("ACEPTADO_AT").toLocalDateTime(),
            rs.getTimestamp("CREATED_AT") == null ? null : rs.getTimestamp("CREATED_AT").toLocalDateTime(),
            rs.getString("CREATED_BY"),
            rs.getTimestamp("UPDATED_AT") == null ? null : rs.getTimestamp("UPDATED_AT").toLocalDateTime(),
            rs.getString("UPDATED_BY"), rs.getString("MOTIVO"));

    private final JdbcTemplate jdbc;

    public FirmaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ---- aviso de privacidad (solo lectura: se publica por base de datos) ----

    public Optional<Aviso> avisoVigente() {
        return jdbc.query("SELECT VERSION, TITULO, TEXTO, BORRADOR FROM SERV_MED_AVISO_PRIVACIDAD WHERE TIPO = ? "
                        + "ORDER BY VERSION DESC FETCH FIRST 1 ROW ONLY",
                (rs, i) -> new Aviso(rs.getInt("VERSION"), rs.getString("TITULO"), rs.getString("TEXTO"),
                        "S".equals(rs.getString("BORRADOR"))),
                TIPO_AVISO).stream().findFirst();
    }

    // ---- firmas ----

    public long insertar(String usuario, String nombre, long fileId, String origen, String estado,
                         Integer avisoVersion, String ip, String creadoPor) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO SERV_MED_FIRMA (USUARIO, NOMBRE, FILE_ID, ORIGEN, ESTADO, AVISO_VERSION, ACEPTADO_AT, "
                            + "ACEPTADO_IP, CREATED_BY) VALUES (?,?,?,?,?,?,"
                            + (avisoVersion == null ? "NULL" : "SYSTIMESTAMP") + ",?,?)",
                    new String[]{"ID"});
            int i = 1;
            ps.setString(i++, usuario);
            ps.setString(i++, nombre);
            ps.setLong(i++, fileId);
            ps.setString(i++, origen);
            ps.setString(i++, estado);
            if (avisoVersion == null) {
                ps.setNull(i++, java.sql.Types.INTEGER);
            } else {
                ps.setInt(i++, avisoVersion);
            }
            ps.setString(i++, avisoVersion == null ? null : ip);
            ps.setString(i, creadoPor);
            return ps;
        }, kh);
        Number k = kh.getKey();
        return k == null ? -1L : k.longValue();
    }

    /** La firma en ese estado (VIGENTE o PENDIENTE) del usuario, la mas reciente. */
    public Optional<Firma> enEstado(String usuario, String estado) {
        return jdbc.query("SELECT " + COLS + " FROM SERV_MED_FIRMA WHERE USUARIO = ? AND ESTADO = ? ORDER BY ID DESC "
                + "FETCH FIRST 1 ROW ONLY", MAPPER, usuario, estado).stream().findFirst();
    }

    public Optional<Firma> porId(long id) {
        return jdbc.query("SELECT " + COLS + " FROM SERV_MED_FIRMA WHERE ID = ?", MAPPER, id).stream().findFirst();
    }

    /** Todas las firmas (administracion), las vigentes y pendientes primero. */
    public List<Firma> todas() {
        return jdbc.query("SELECT " + COLS + " FROM SERV_MED_FIRMA ORDER BY CASE ESTADO WHEN 'VIGENTE' THEN 0 "
                + "WHEN 'PENDIENTE' THEN 1 ELSE 2 END, USUARIO, ID DESC", MAPPER);
    }

    /** Usuarios con firma VIGENTE (para la revision diaria de vigencia). */
    public List<Firma> vigentes() {
        return jdbc.query("SELECT " + COLS + " FROM SERV_MED_FIRMA WHERE ESTADO = 'VIGENTE'", MAPPER);
    }

    /** Cambia el estado solo si sigue en el estado esperado (evita pisar un cambio simultaneo). */
    public int cambiarEstado(long id, String de, String a, String quien, String motivo) {
        return jdbc.update("UPDATE SERV_MED_FIRMA SET ESTADO = ?, UPDATED_AT = SYSTIMESTAMP, UPDATED_BY = ?, MOTIVO = ? "
                + "WHERE ID = ? AND ESTADO = ?", a, quien, recorta(motivo, 400), id, de);
    }

    /** El dueno acepta (o vuelve a aceptar) el aviso: guarda la version, el momento y la IP. */
    public int aceptar(long id, int avisoVersion, String ip, String quien, String nuevoEstado) {
        return jdbc.update("UPDATE SERV_MED_FIRMA SET AVISO_VERSION = ?, ACEPTADO_AT = SYSTIMESTAMP, ACEPTADO_IP = ?, "
                        + "ESTADO = ?, UPDATED_AT = SYSTIMESTAMP, UPDATED_BY = ? WHERE ID = ?",
                avisoVersion, recorta(ip, 80), nuevoEstado, quien, id);
    }

    // ---- vigencia del empleado (biometrico, solo lectura) ----

    /**
     * true si el NSS tiene un registro con EMP_STATUS = 0 en BIO_EMPLEADO: 0 = ACTIVO para el personal
     * (confirmado por el usuario el 20-oct-2026; el sistema anterior usa 1 para operacion).
     */
    public boolean empleadoActivo(String nss) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM BIO_EMPLEADO WHERE EMP_NSS = ? AND EMP_STATUS = 0",
                Integer.class, nss);
        return n != null && n > 0;
    }

    private static String recorta(String s, int max) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max);
    }
}
