package com.onest.app.ayuda;

import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/** SERV_MED_AYUDA_PREGUNTA: lo que la gente pregunta y si le sirvio. Base para mejorar la ayuda. */
@Repository
public class AyudaPreguntaRepository {

    public record Pregunta(long id, String usuario, String pregunta, int resultados, String motor, Long articuloId,
                           String util, LocalDateTime createdAt) {
    }

    private final JdbcTemplate jdbc;

    public AyudaPreguntaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long registrar(String usuario, String pregunta, int resultados, String motor) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO SERV_MED_AYUDA_PREGUNTA (USUARIO, PREGUNTA, RESULTADOS, MOTOR) VALUES (?, ?, ?, ?)", new String[] {"ID"});
            ps.setString(1, usuario);
            ps.setString(2, pregunta.length() > 500 ? pregunta.substring(0, 500) : pregunta);
            ps.setInt(3, resultados);
            ps.setString(4, motor);
            return ps;
        }, kh);
        return kh.getKey() == null ? 0 : kh.getKey().longValue();
    }

    public int calificar(long id, Long articuloId, boolean util) {
        return jdbc.update("UPDATE SERV_MED_AYUDA_PREGUNTA SET UTIL = ?, ARTICULO_ID = COALESCE(?, ARTICULO_ID) WHERE ID = ?",
                util ? "S" : "N", articuloId, id);
    }

    /** Sin resultados o marcadas como no utiles, mas recientes primero: la lista de "que falta documentar". */
    public List<Pregunta> sinRespuesta(int limite) {
        return jdbc.query("SELECT ID, USUARIO, PREGUNTA, RESULTADOS, MOTOR, ARTICULO_ID, UTIL, CREATED_AT FROM SERV_MED_AYUDA_PREGUNTA"
                        + " WHERE RESULTADOS = 0 OR UTIL = 'N' ORDER BY ID DESC FETCH FIRST ? ROWS ONLY",
                (rs, i) -> new Pregunta(rs.getLong("ID"), rs.getString("USUARIO"), rs.getString("PREGUNTA"), rs.getInt("RESULTADOS"),
                        rs.getString("MOTOR"), rs.getObject("ARTICULO_ID") == null ? null : rs.getLong("ARTICULO_ID"), rs.getString("UTIL"),
                        rs.getTimestamp("CREATED_AT").toLocalDateTime()),
                limite);
    }
}
