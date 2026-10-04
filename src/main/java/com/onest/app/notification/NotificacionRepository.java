package com.onest.app.notification;

import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/** Acceso JDBC a SERV_MED_NOTIFICACION (base del portal). Ver db/sql/app_domain/notificaciones.sql. */
@Repository
public class NotificacionRepository {

    public record Notificacion(long id, String tipo, String titulo, String mensaje, String nss, String enlace,
                               String destinoMenuCode, String destinoUsuario, String origen, String referencia,
                               LocalDateTime createdAt) {
    }

    private final JdbcTemplate jdbc;

    public NotificacionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** No leidas, mas recientes primero. El filtro por destino se hace en el servicio (necesita los permisos del usuario). */
    public List<Notificacion> pendientes(int limite) {
        return jdbc.query(
                "SELECT ID, TIPO, TITULO, MENSAJE, NSS, ENLACE, DESTINO_MENU_CODE, DESTINO_USUARIO, ORIGEN, REFERENCIA, CREATED_AT"
                        + " FROM SERV_MED_NOTIFICACION WHERE LEIDA_AT IS NULL ORDER BY ID DESC FETCH FIRST ? ROWS ONLY",
                (rs, i) -> new Notificacion(rs.getLong("ID"), rs.getString("TIPO"), rs.getString("TITULO"), rs.getString("MENSAJE"),
                        rs.getString("NSS"), rs.getString("ENLACE"), rs.getString("DESTINO_MENU_CODE"), rs.getString("DESTINO_USUARIO"),
                        rs.getString("ORIGEN"), rs.getString("REFERENCIA"), rs.getTimestamp("CREATED_AT").toLocalDateTime()),
                limite);
    }

    public int marcarLeida(long id, String usuario) {
        return jdbc.update("UPDATE SERV_MED_NOTIFICACION SET LEIDA_AT = SYSTIMESTAMP, LEIDA_POR = ? WHERE ID = ? AND LEIDA_AT IS NULL",
                usuario, id);
    }

    public int marcarLeidas(List<Long> ids, String usuario) {
        int n = 0;
        for (Long id : ids) {
            n += marcarLeida(id, usuario);
        }
        return n;
    }

    /** Crea una notificacion y devuelve su ID. */
    public long crear(String tipo, String titulo, String mensaje, String nss, String enlace, String destinoMenuCode,
                      String destinoUsuario, String origen, String referencia, String creadoPor) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO SERV_MED_NOTIFICACION (TIPO, TITULO, MENSAJE, NSS, ENLACE, DESTINO_MENU_CODE, DESTINO_USUARIO,"
                            + " ORIGEN, REFERENCIA, CREATED_BY) VALUES (?,?,?,?,?,?,?,?,?,?)",
                    new String[] {"ID"});
            ps.setString(1, tipo);
            ps.setString(2, titulo);
            ps.setString(3, mensaje);
            ps.setString(4, nss);
            ps.setString(5, enlace);
            ps.setString(6, destinoMenuCode);
            ps.setString(7, destinoUsuario);
            ps.setString(8, origen);
            ps.setString(9, referencia);
            ps.setString(10, creadoPor);
            return ps;
        }, keys);
        Number key = keys.getKey();
        return key == null ? -1 : key.longValue();
    }

    /** true si existe una notificacion (leida o no) con ese origen+referencia: evita duplicar avisos repetidos. */
    public boolean existe(String origen, String referencia) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM SERV_MED_NOTIFICACION WHERE ORIGEN = ? AND REFERENCIA = ?",
                Integer.class, origen, referencia);
        return n != null && n > 0;
    }
}
