package com.onest.app.integration.ocr;

import java.io.StringReader;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/** Acceso JDBC a SERV_MED_INTEGRACION_ENVIO: intentos del webhook hacia OCR. */
@Repository
public class EnvioWebhookRepository {

    public record Envio(long id, long solicitudId, String evento, String url, int intento, LocalDateTime programadoAt,
                        LocalDateTime enviadoAt, Integer httpCode, String respuesta, String estatus) {
    }

    private static final RowMapper<Envio> MAPPER = (rs, i) -> new Envio(rs.getLong("ID"), rs.getLong("SOLICITUD_ID"),
            rs.getString("EVENTO"), rs.getString("URL"), rs.getInt("INTENTO"), rs.getTimestamp("PROGRAMADO_AT").toLocalDateTime(),
            rs.getTimestamp("ENVIADO_AT") == null ? null : rs.getTimestamp("ENVIADO_AT").toLocalDateTime(),
            rs.getObject("HTTP_CODE") == null ? null : rs.getInt("HTTP_CODE"), rs.getString("RESPUESTA"), rs.getString("ESTATUS"));

    private final JdbcTemplate jdbc;

    public EnvioWebhookRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long programar(long solicitudId, String evento, String url, String cuerpo, int intento, LocalDateTime cuando, String usuario) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO SERV_MED_INTEGRACION_ENVIO (SOLICITUD_ID, EVENTO, URL, CUERPO, INTENTO, PROGRAMADO_AT, CREATED_BY)"
                            + " VALUES (?,?,?,?,?,?,?)", new String[] {"ID"});
            ps.setLong(1, solicitudId);
            ps.setString(2, evento);
            ps.setString(3, url);
            ps.setCharacterStream(4, new StringReader(cuerpo));
            ps.setInt(5, intento);
            ps.setTimestamp(6, Timestamp.valueOf(cuando));
            ps.setString(7, usuario);
            return ps;
        }, keys);
        Number k = keys.getKey();
        return k == null ? -1 : k.longValue();
    }

    /**
     * Pendientes cuya hora programada ya llego. Se compara contra el reloj del PORTAL (el mismo
     * que escribio PROGRAMADO_AT), no contra SYSTIMESTAMP: si la base esta en otra zona horaria
     * los reintentos se adelantaban (visto en QA el 5-oct-2026: 4 intentos en 4 minutos).
     */
    public List<Envio> pendientesVencidos(int limite) {
        return jdbc.query("SELECT ID, SOLICITUD_ID, EVENTO, URL, INTENTO, PROGRAMADO_AT, ENVIADO_AT, HTTP_CODE, RESPUESTA, ESTATUS"
                + " FROM SERV_MED_INTEGRACION_ENVIO WHERE ESTATUS = 'PENDIENTE' AND PROGRAMADO_AT <= ?"
                + " ORDER BY PROGRAMADO_AT FETCH FIRST ? ROWS ONLY", MAPPER, Timestamp.valueOf(LocalDateTime.now()), limite);
    }

    public String cuerpo(long id) {
        return jdbc.query("SELECT CUERPO FROM SERV_MED_INTEGRACION_ENVIO WHERE ID = ?",
                rs -> rs.next() ? rs.getString(1) : null, id);
    }

    public List<Envio> porSolicitud(long solicitudId) {
        return jdbc.query("SELECT ID, SOLICITUD_ID, EVENTO, URL, INTENTO, PROGRAMADO_AT, ENVIADO_AT, HTTP_CODE, RESPUESTA, ESTATUS"
                + " FROM SERV_MED_INTEGRACION_ENVIO WHERE SOLICITUD_ID = ? ORDER BY ID", MAPPER, solicitudId);
    }

    public void resultado(long id, String estatus, Integer httpCode, String respuesta) {
        String r = respuesta == null ? null : respuesta.substring(0, Math.min(2000, respuesta.length()));
        jdbc.update("UPDATE SERV_MED_INTEGRACION_ENVIO SET ESTATUS = ?, HTTP_CODE = ?, RESPUESTA = ?, ENVIADO_AT = SYSTIMESTAMP WHERE ID = ?",
                estatus, httpCode, r, id);
    }
}
