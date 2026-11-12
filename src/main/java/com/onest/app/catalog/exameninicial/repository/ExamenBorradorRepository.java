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

    /** Origen de los renglones de la bandeja que salen de aqui (los de SERV_MED_EXAMEN_SOLICITUD son "OCR"). */
    public static final String ORIGEN_DIRECTO = "DIRECTO";

    /**
     * Examenes iniciales capturados DIRECTO (desde Busqueda NSS, sin solicitud de OCR) para la bandeja
     * (9-nov-2026: SM no veia ahi el examen que acababa de finalizar). Mismo registro que la bandeja de OCR:
     * borrador abierto = EN_CAPTURA, cerrado = FINALIZADO. Los NSS con solicitud de OCR no se repiten (esa
     * solicitud ya sale). El id va en negativo para que nunca se confunda con una solicitud ("Tomar" no aplica).
     * Nombre, puesto y cuenta salen de SERV_MED_V_EMPLEADO; si la vista no existe (base local) van vacios.
     */
    public List<com.onest.app.integration.ocr.SolicitudExamen> directos(String estatus, String nss, String nombre,
            java.time.LocalDate desde, java.time.LocalDate hasta, int limite) {
        try {
            return consultarDirectos(true, estatus, nss, nombre, desde, hasta, limite);
        } catch (org.springframework.dao.DataAccessException ex) {
            return consultarDirectos(false, estatus, nss, null, desde, hasta, limite);
        }
    }

    private List<com.onest.app.integration.ocr.SolicitudExamen> consultarDirectos(boolean conEmpleado, String estatus,
            String nss, String nombre, java.time.LocalDate desde, java.time.LocalDate hasta, int limite) {
        StringBuilder sql = new StringBuilder("SELECT b.ID, b.NSS, b.ESTATUS, b.CREATED_AT, b.UPDATED_AT, b.UPDATED_BY,"
                + " b.FINALIZADO_AT, b.FINALIZADO_BY,"
                + " JSON_VALUE(b.DATOS, '$.\"SERV_MED_RESULTADO_EXAMEN.DICTAMEN\"' RETURNING VARCHAR2(40)) DICTAMEN,"
                + (conEmpleado ? " e.NOMBRE, e.PUESTO, e.CUENTA" : " NULL NOMBRE, NULL PUESTO, NULL CUENTA")
                + " FROM SERV_MED_EXAMEN_BORRADOR b"
                // un renglon por NSS aunque tenga varias cuentas/predios en la vista (multipredio)
                + (conEmpleado ? " LEFT JOIN (SELECT NSS, MAX(NOMBRE) NOMBRE, MAX(PUESTO) PUESTO, MAX(CUENTA) CUENTA"
                        + " FROM SERV_MED_V_EMPLEADO WHERE NSS IN (SELECT NSS FROM SERV_MED_EXAMEN_BORRADOR)"
                        + " GROUP BY NSS) e ON e.NSS = b.NSS" : "")
                + " WHERE b.TIPO_EXAMEN = 'INICIAL'"
                + " AND NOT EXISTS (SELECT 1 FROM SERV_MED_EXAMEN_SOLICITUD s WHERE s.NSS = b.NSS)");
        List<Object> args = new java.util.ArrayList<>();
        if (estatus != null && !estatus.isBlank()) {
            String e = estatus.trim();
            if ("EN_CAPTURA".equals(e)) {
                sql.append(" AND b.ESTATUS = 'BORRADOR'");
            } else if ("FINALIZADO".equals(e)) {
                sql.append(" AND b.ESTATUS = 'FINALIZADO'");
            } else {
                return List.of();   // PENDIENTE y CANCELADO solo existen en las solicitudes de OCR
            }
        }
        if (nss != null && !nss.isBlank()) {
            sql.append(" AND b.NSS LIKE ?");
            args.add(nss.trim() + "%");
        }
        if (nombre != null && !nombre.isBlank()) {
            sql.append(" AND UPPER(e.NOMBRE) LIKE ?");
            args.add("%" + nombre.trim().toUpperCase() + "%");
        }
        if (desde != null) {
            sql.append(" AND TRUNC(b.CREATED_AT) >= ?");
            args.add(java.sql.Date.valueOf(desde));
        }
        if (hasta != null) {
            sql.append(" AND TRUNC(b.CREATED_AT) <= ?");
            args.add(java.sql.Date.valueOf(hasta));
        }
        sql.append(" ORDER BY b.CREATED_AT DESC FETCH FIRST ? ROWS ONLY");
        args.add(limite);
        return jdbc.query(sql.toString(), (rs, i) -> {
            boolean fin = "FINALIZADO".equals(rs.getString("ESTATUS"));
            String nom = rs.getString("NOMBRE");
            java.sql.Timestamp creado = rs.getTimestamp("CREATED_AT");
            java.sql.Timestamp cuando = rs.getTimestamp(fin ? "FINALIZADO_AT" : "UPDATED_AT");
            return new com.onest.app.integration.ocr.SolicitudExamen(
                    -rs.getLong("ID"), ORIGEN_DIRECTO, "Captura directa", rs.getString("NSS"), null, null,
                    nom == null ? "" : nom, "", "", null, null, null, null, null, null, null, null,
                    rs.getString("CUENTA"), null, rs.getString("PUESTO"), null, null,
                    fin ? com.onest.app.integration.ocr.SolicitudExamen.FINALIZADO : com.onest.app.integration.ocr.SolicitudExamen.EN_CAPTURA,
                    null, null, null, rs.getString("DICTAMEN"), null, null, null,
                    creado == null ? null : creado.toLocalDateTime(), null,
                    rs.getString(fin ? "FINALIZADO_BY" : "UPDATED_BY"), cuando == null ? null : cuando.toLocalDateTime());
        }, args.toArray());
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

    /**
     * true si el NSS se finalizo hace menos de 2 minutos. Compara con la hora de la BASE (SYSTIMESTAMP),
     * la misma que puso FINALIZADO_AT: la del servidor de aplicaciones puede estar en otra zona horaria.
     */
    public boolean finalizadoHaceMenosDe2Min(String nss) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM SERV_MED_EXAMEN_BORRADOR"
                        + " WHERE NSS = ? AND ESTATUS = 'FINALIZADO' AND FINALIZADO_AT > SYSTIMESTAMP - INTERVAL '2' MINUTE",
                Integer.class, nss);
        return n != null && n > 0;
    }

    public Optional<Finalizado> ultimoFinalizado(String nss) {
        List<Finalizado> rows = jdbc.query(
                "SELECT FINALIZADO_AT, FINALIZADO_BY FROM SERV_MED_EXAMEN_BORRADOR"
                        + " WHERE NSS = ? AND ESTATUS = 'FINALIZADO' ORDER BY FINALIZADO_AT DESC FETCH FIRST 1 ROWS ONLY",
                (rs, i) -> new Finalizado(rs.getTimestamp("FINALIZADO_AT").toLocalDateTime(), rs.getString("FINALIZADO_BY")),
                nss);
        return rows.stream().findFirst();
    }

    /**
     * Datos del ultimo borrador FINALIZADO (lo que el medico capturo en ese examen), o vacio. E-032 (11-nov):
     * de aqui sale el Si/No elegido en exploracion al reabrir un examen ya finalizado (solo vive en el borrador).
     */
    public Map<String, String> datosUltimoFinalizado(String nss) {
        List<Map<String, String>> rows = jdbc.query(
                "SELECT DATOS FROM SERV_MED_EXAMEN_BORRADOR"
                        + " WHERE NSS = ? AND ESTATUS = 'FINALIZADO' ORDER BY FINALIZADO_AT DESC FETCH FIRST 1 ROWS ONLY",
                (rs, i) -> leerDatos(rs), nss);
        return rows.isEmpty() ? Map.of() : rows.get(0);
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
