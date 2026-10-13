package com.onest.app.ayuda;

import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** Acceso JDBC a SERV_MED_AYUDA_ARTICULO (base del portal). Ver db/sql/app_domain/ayuda.sql. */
@Repository
public class AyudaArticuloRepository {

    private static final String COLS = "ID, CLAVE, MODULO, TITULO, PREGUNTA, PALABRAS_CLAVE, CUERPO, ENLACE, ORDEN, ACTIVO, UPDATED_AT, UPDATED_BY";

    private static final RowMapper<AyudaArticulo> MAPPER = (rs, i) -> new AyudaArticulo(
            rs.getLong("ID"), rs.getString("CLAVE"), rs.getString("MODULO"), rs.getString("TITULO"), rs.getString("PREGUNTA"),
            rs.getString("PALABRAS_CLAVE"), clob(rs, "CUERPO"), rs.getString("ENLACE"), rs.getInt("ORDEN"),
            "S".equals(rs.getString("ACTIVO")), ts(rs.getTimestamp("UPDATED_AT")), rs.getString("UPDATED_BY"));

    private final JdbcTemplate jdbc;

    public AyudaArticuloRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Todos (activos e inactivos), por modulo y orden: para el admin y para reindexar. */
    public List<AyudaArticulo> listar() {
        return jdbc.query("SELECT " + COLS + " FROM SERV_MED_AYUDA_ARTICULO ORDER BY MODULO, ORDEN, TITULO", MAPPER);
    }

    public List<AyudaArticulo> activos() {
        return jdbc.query("SELECT " + COLS + " FROM SERV_MED_AYUDA_ARTICULO WHERE ACTIVO = 'S' ORDER BY MODULO, ORDEN, TITULO", MAPPER);
    }

    public Optional<AyudaArticulo> porId(long id) {
        return jdbc.query("SELECT " + COLS + " FROM SERV_MED_AYUDA_ARTICULO WHERE ID = ?", MAPPER, id).stream().findFirst();
    }

    public Optional<AyudaArticulo> porClave(String clave) {
        return jdbc.query("SELECT " + COLS + " FROM SERV_MED_AYUDA_ARTICULO WHERE CLAVE = ?", MAPPER, clave).stream().findFirst();
    }

    /**
     * Busqueda de respaldo cuando Meilisearch no esta: cada palabra (>= 3 letras) cuenta un punto si
     * aparece en titulo, pregunta o palabras clave, dos si en el titulo. Sin acentos ni mayusculas.
     * Es tonta a proposito: la busqueda buena la hace Meilisearch; esta solo evita responder vacio.
     */
    public List<AyudaArticulo> buscarLike(List<String> palabras, int limite) {
        if (palabras.isEmpty()) {
            return List.of();
        }
        StringBuilder puntos = new StringBuilder();
        List<Object> args = new ArrayList<>();
        for (String p : palabras) {
            String like = "%" + p + "%";
            if (puntos.length() > 0) {
                puntos.append(" + ");
            }
            puntos.append("(CASE WHEN ").append(sinAcentos("TITULO")).append(" LIKE ? THEN 2 ELSE 0 END)")
                  .append(" + (CASE WHEN ").append(sinAcentos("PREGUNTA")).append(" LIKE ? THEN 1 ELSE 0 END)")
                  .append(" + (CASE WHEN ").append(sinAcentos("PALABRAS_CLAVE")).append(" LIKE ? THEN 1 ELSE 0 END)");
            args.add(like);
            args.add(like);
            args.add(like);
        }
        String sql = "SELECT * FROM (SELECT " + COLS + ", (" + puntos + ") PUNTOS FROM SERV_MED_AYUDA_ARTICULO WHERE ACTIVO = 'S')"
                + " WHERE PUNTOS > 0 ORDER BY PUNTOS DESC, ORDEN FETCH FIRST ? ROWS ONLY";
        args.add(limite);
        return jdbc.query(sql, MAPPER, args.toArray());
    }

    /** Alta o edicion. Devuelve el id. La CLAVE no cambia una vez creada (es el slug del enlace). */
    public long guardar(AyudaArticulo a, String usuario) {
        if (a.id() > 0) {
            jdbc.update("UPDATE SERV_MED_AYUDA_ARTICULO SET MODULO = ?, TITULO = ?, PREGUNTA = ?, PALABRAS_CLAVE = ?, CUERPO = ?,"
                    + " ENLACE = ?, ORDEN = ?, ACTIVO = ?, UPDATED_AT = SYSTIMESTAMP, UPDATED_BY = ? WHERE ID = ?",
                    a.modulo(), a.titulo(), a.pregunta(), a.palabrasClave(), a.cuerpo(), a.enlace(), a.orden(),
                    a.activo() ? "S" : "N", usuario, a.id());
            return a.id();
        }
        jdbc.update("INSERT INTO SERV_MED_AYUDA_ARTICULO (CLAVE, MODULO, TITULO, PREGUNTA, PALABRAS_CLAVE, CUERPO, ENLACE, ORDEN, ACTIVO, CREATED_BY, UPDATED_BY)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                a.clave(), a.modulo(), a.titulo(), a.pregunta(), a.palabrasClave(), a.cuerpo(), a.enlace(), a.orden(),
                a.activo() ? "S" : "N", usuario, usuario);
        return porClave(a.clave()).map(AyudaArticulo::id).orElse(0L);
    }

    public int activar(long id, boolean activo, String usuario) {
        return jdbc.update("UPDATE SERV_MED_AYUDA_ARTICULO SET ACTIVO = ?, UPDATED_AT = SYSTIMESTAMP, UPDATED_BY = ? WHERE ID = ?",
                activo ? "S" : "N", usuario, id);
    }

    // Oracle: quitar acentos y mayusculas para comparar; la palabra buscada ya viene normalizada
    private static String sinAcentos(String col) {
        return "TRANSLATE(UPPER(" + col + "), 'ÁÉÍÓÚÜÑ', 'AEIOUUN')";
    }

    private static String clob(ResultSet rs, String col) throws SQLException {
        Clob c = rs.getClob(col);
        if (c == null) {
            return "";
        }
        try {
            return c.getSubString(1, (int) c.length());
        } finally {
            c.free();
        }
    }

    private static java.time.LocalDateTime ts(Timestamp t) {
        return t == null ? null : t.toLocalDateTime();
    }
}
