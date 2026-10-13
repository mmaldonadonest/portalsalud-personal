package com.onest.app.integration.ocr;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/** Acceso JDBC a SERV_MED_EXAMEN_SOLICITUD (base del portal). Ver db/sql/app_domain/integracion-ocr.sql. */
@Repository
public class SolicitudExamenRepository {

    private static final String COLS = "ID, ORIGEN, REFERENCIA, NSS, CURP, RFC, NOMBRE, APELLIDO_PATERNO, APELLIDO_MATERNO,"
            + " FECHA_NACIMIENTO, SEXO, ESTADO_CIVIL, ESCOLARIDAD, PROFESION_OFICIO, DOMICILIO, TELEFONO, EMPRESA, CUENTA, AGENCIA,"
            + " PUESTO_SOLICITADO, FECHA_CITA, CALLBACK_URL, ESTATUS, CADUCA, MOTIVO_CANCEL, CANCELADO_AT, DICTAMEN, FECHA_EXAMEN,"
            + " HISTORICO_ID, NOTIFICADO_AT, CREATED_AT, UPDATED_AT, ATENDIDO_POR, ATENDIDO_AT";

    private static final RowMapper<SolicitudExamen> MAPPER = (rs, i) -> new SolicitudExamen(
            rs.getLong("ID"), rs.getString("ORIGEN"), rs.getString("REFERENCIA"), rs.getString("NSS"), rs.getString("CURP"),
            rs.getString("RFC"), rs.getString("NOMBRE"), rs.getString("APELLIDO_PATERNO"), rs.getString("APELLIDO_MATERNO"),
            fecha(rs, "FECHA_NACIMIENTO"), rs.getString("SEXO"), rs.getString("ESTADO_CIVIL"), rs.getString("ESCOLARIDAD"),
            rs.getString("PROFESION_OFICIO"), rs.getString("DOMICILIO"), rs.getString("TELEFONO"), rs.getString("EMPRESA"),
            rs.getString("CUENTA"), rs.getString("AGENCIA"), rs.getString("PUESTO_SOLICITADO"), fecha(rs, "FECHA_CITA"),
            rs.getString("CALLBACK_URL"), rs.getString("ESTATUS"), fecha(rs, "CADUCA"), rs.getString("MOTIVO_CANCEL"),
            ts(rs, "CANCELADO_AT"), rs.getString("DICTAMEN"), ts(rs, "FECHA_EXAMEN"),
            rs.getObject("HISTORICO_ID") == null ? null : rs.getLong("HISTORICO_ID"),
            ts(rs, "NOTIFICADO_AT"), ts(rs, "CREATED_AT"), ts(rs, "UPDATED_AT"), rs.getString("ATENDIDO_POR"), ts(rs, "ATENDIDO_AT"));

    private static LocalDate fecha(ResultSet rs, String col) throws SQLException {
        Date d = rs.getDate(col);
        return d == null ? null : d.toLocalDate();
    }

    private static LocalDateTime ts(ResultSet rs, String col) throws SQLException {
        Timestamp t = rs.getTimestamp(col);
        return t == null ? null : t.toLocalDateTime();
    }

    private final JdbcTemplate jdbc;

    public SolicitudExamenRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<SolicitudExamen> porId(long id) {
        return jdbc.query("SELECT " + COLS + " FROM SERV_MED_EXAMEN_SOLICITUD WHERE ID = ?", MAPPER, id).stream().findFirst();
    }

    public Optional<SolicitudExamen> porReferencia(String origen, String referencia) {
        return jdbc.query("SELECT " + COLS + " FROM SERV_MED_EXAMEN_SOLICITUD WHERE ORIGEN = ? AND REFERENCIA = ?", MAPPER,
                origen, referencia).stream().findFirst();
    }

    /** Solicitud viva (PENDIENTE o EN_CAPTURA) del NSS, la mas reciente. */
    public Optional<SolicitudExamen> activaPorNss(String nss) {
        return jdbc.query("SELECT " + COLS + " FROM SERV_MED_EXAMEN_SOLICITUD WHERE NSS = ? AND ESTATUS IN ('PENDIENTE','EN_CAPTURA')"
                + " ORDER BY ID DESC FETCH FIRST 1 ROWS ONLY", MAPPER, nss).stream().findFirst();
    }

    /** Ultima solicitud del NSS de cualquier estatus (para la ficha de un candidato ya examinado/cancelado). */
    public Optional<SolicitudExamen> ultimaPorNss(String nss) {
        return jdbc.query("SELECT " + COLS + " FROM SERV_MED_EXAMEN_SOLICITUD WHERE NSS = ? ORDER BY ID DESC FETCH FIRST 1 ROWS ONLY",
                MAPPER, nss).stream().findFirst();
    }

    public List<SolicitudExamen> listar(String estatus, int limite) {
        if (estatus == null || estatus.isBlank()) {
            return jdbc.query("SELECT " + COLS + " FROM SERV_MED_EXAMEN_SOLICITUD ORDER BY ID DESC FETCH FIRST ? ROWS ONLY", MAPPER, limite);
        }
        return jdbc.query("SELECT " + COLS + " FROM SERV_MED_EXAMEN_SOLICITUD WHERE ESTATUS = ? ORDER BY ID DESC FETCH FIRST ? ROWS ONLY",
                MAPPER, estatus, limite);
    }

    public long crear(SolicitudExamen s, String ip) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO SERV_MED_EXAMEN_SOLICITUD (ORIGEN, REFERENCIA, NSS, CURP, RFC, NOMBRE, APELLIDO_PATERNO, APELLIDO_MATERNO,"
                            + " FECHA_NACIMIENTO, SEXO, ESTADO_CIVIL, ESCOLARIDAD, PROFESION_OFICIO, DOMICILIO, TELEFONO, EMPRESA, CUENTA, AGENCIA,"
                            + " PUESTO_SOLICITADO, FECHA_CITA, CALLBACK_URL, ESTATUS, CADUCA, CREATED_IP)"
                            + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'PENDIENTE',?,?)",
                    new String[] {"ID"});
            int i = 1;
            ps.setString(i++, s.origen());
            ps.setString(i++, s.referencia());
            ps.setString(i++, s.nss());
            ps.setString(i++, s.curp());
            ps.setString(i++, s.rfc());
            ps.setString(i++, s.nombre());
            ps.setString(i++, s.apellidoPaterno());
            ps.setString(i++, s.apellidoMaterno());
            ps.setDate(i++, s.fechaNacimiento() == null ? null : Date.valueOf(s.fechaNacimiento()));
            ps.setString(i++, s.sexo());
            ps.setString(i++, s.estadoCivil());
            ps.setString(i++, s.escolaridad());
            ps.setString(i++, s.profesionOficio());
            ps.setString(i++, s.domicilio());
            ps.setString(i++, s.telefono());
            ps.setString(i++, s.empresa());
            ps.setString(i++, s.cuenta());
            ps.setString(i++, s.agencia());
            ps.setString(i++, s.puestoSolicitado());
            ps.setDate(i++, s.fechaCita() == null ? null : Date.valueOf(s.fechaCita()));
            ps.setString(i++, s.callbackUrl());
            ps.setDate(i++, Date.valueOf(s.caduca()));
            ps.setString(i, ip);
            return ps;
        }, keys);
        Number k = keys.getKey();
        return k == null ? -1 : k.longValue();
    }

    /** Asigna al primero que abre (solo si no tiene a nadie). Devuelve filas tocadas. */
    public int asignarSiLibre(long id, String usuario) {
        return jdbc.update("UPDATE SERV_MED_EXAMEN_SOLICITUD SET ATENDIDO_POR = ?, ATENDIDO_AT = SYSTIMESTAMP, UPDATED_AT = SYSTIMESTAMP"
                + " WHERE ID = ? AND ATENDIDO_POR IS NULL AND ESTATUS IN ('PENDIENTE','EN_CAPTURA')", usuario, id);
    }

    /** "Tomar": reasigna explicitamente a otro usuario. */
    public int reasignar(long id, String usuario) {
        return jdbc.update("UPDATE SERV_MED_EXAMEN_SOLICITUD SET ATENDIDO_POR = ?, ATENDIDO_AT = SYSTIMESTAMP, UPDATED_AT = SYSTIMESTAMP"
                + " WHERE ID = ? AND ESTATUS IN ('PENDIENTE','EN_CAPTURA')", usuario, id);
    }

    /** Bandeja: filtros opcionales (null/vacio = sin filtro). Fechas sobre FECHA_CITA si hay, si no CREATED_AT. */
    public List<SolicitudExamen> bandeja(String estatus, String nss, String nombre, LocalDate desde, LocalDate hasta, int limite) {
        StringBuilder sql = new StringBuilder("SELECT " + COLS + " FROM SERV_MED_EXAMEN_SOLICITUD WHERE 1 = 1");
        List<Object> args = new java.util.ArrayList<>();
        if (estatus != null && !estatus.isBlank()) {
            sql.append(" AND ESTATUS = ?");
            args.add(estatus.trim());
        }
        if (nss != null && !nss.isBlank()) {
            sql.append(" AND NSS LIKE ?");
            args.add(nss.trim() + "%");
        }
        if (nombre != null && !nombre.isBlank()) {
            sql.append(" AND UPPER(NOMBRE || ' ' || APELLIDO_PATERNO || ' ' || APELLIDO_MATERNO) LIKE ?");
            args.add("%" + nombre.trim().toUpperCase() + "%");
        }
        if (desde != null) {
            sql.append(" AND COALESCE(FECHA_CITA, TRUNC(CREATED_AT)) >= ?");
            args.add(Date.valueOf(desde));
        }
        if (hasta != null) {
            sql.append(" AND COALESCE(FECHA_CITA, TRUNC(CREATED_AT)) <= ?");
            args.add(Date.valueOf(hasta));
        }
        sql.append(" ORDER BY CASE ESTATUS WHEN 'PENDIENTE' THEN 0 WHEN 'EN_CAPTURA' THEN 1 WHEN 'FINALIZADO' THEN 2 ELSE 3 END,")
           .append(" COALESCE(FECHA_CITA, TRUNC(CREATED_AT)), ID DESC FETCH FIRST ? ROWS ONLY");
        args.add(limite);
        return jdbc.query(sql.toString(), MAPPER, args.toArray());
    }

    public int marcarEnCaptura(long id) {
        return jdbc.update("UPDATE SERV_MED_EXAMEN_SOLICITUD SET ESTATUS = 'EN_CAPTURA', UPDATED_AT = SYSTIMESTAMP"
                + " WHERE ID = ? AND ESTATUS = 'PENDIENTE'", id);
    }

    public int finalizar(long id, String dictamen, Long historicoId) {
        return jdbc.update("UPDATE SERV_MED_EXAMEN_SOLICITUD SET ESTATUS = 'FINALIZADO', DICTAMEN = ?, FECHA_EXAMEN = SYSTIMESTAMP,"
                + " HISTORICO_ID = ?, UPDATED_AT = SYSTIMESTAMP WHERE ID = ? AND ESTATUS IN ('PENDIENTE','EN_CAPTURA')",
                dictamen, historicoId, id);
    }

    public int cancelar(long id, String motivo) {
        return jdbc.update("UPDATE SERV_MED_EXAMEN_SOLICITUD SET ESTATUS = 'CANCELADO', MOTIVO_CANCEL = ?, CANCELADO_AT = SYSTIMESTAMP,"
                + " UPDATED_AT = SYSTIMESTAMP WHERE ID = ? AND ESTATUS IN ('PENDIENTE','EN_CAPTURA')", motivo, id);
    }

    /** Caduca las pendientes cuya fecha CADUCA ya paso. Devuelve cuantas. */
    public int caducarVencidas() {
        return jdbc.update("UPDATE SERV_MED_EXAMEN_SOLICITUD SET ESTATUS = 'CANCELADO', MOTIVO_CANCEL = 'CADUCIDAD',"
                + " CANCELADO_AT = SYSTIMESTAMP, UPDATED_AT = SYSTIMESTAMP WHERE ESTATUS IN ('PENDIENTE','EN_CAPTURA') AND CADUCA < TRUNC(SYSDATE)");
    }

    public int marcarNotificada(long id) {
        return jdbc.update("UPDATE SERV_MED_EXAMEN_SOLICITUD SET NOTIFICADO_AT = SYSTIMESTAMP, UPDATED_AT = SYSTIMESTAMP WHERE ID = ?", id);
    }
}
