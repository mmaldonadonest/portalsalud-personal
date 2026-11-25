package com.onest.app.catalog.dashboard.repository;

import static com.onest.app.catalog.dashboard.repository.VistaFiltro.MES;
import static com.onest.app.catalog.dashboard.repository.VistaFiltro.PREDIO;
import static com.onest.app.catalog.dashboard.repository.VistaFiltro.SIN_DATO;

import com.onest.app.catalog.dashboard.dto.ConteoSimpleDto;
import com.onest.app.catalog.dashboard.dto.PuntoMensualDto;
import java.time.LocalDate;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Maternidad COMPLETA (chequeos del portal + reportes mensuales del Excel + carga vieja mientras el
 * Excel nuevo este vacio) leida de la vista SERV_MED_V_MATERNIDAD (v4 de excel-nss-05-vistas.sql).
 * Con este repositorio, DashboardMaternidadService ya no llama al WS de ORDS para graficar.
 *
 * <p>Solo existe con {@code portal.historico.fuente=VISTA}.
 *
 * <p>Es seguimiento con historial (un renglon por caso por mes): por eso personas, reincorporadas
 * y partos se cuentan por persona / embarazo, no por renglon. Persona = NSS; la carga vieja no tiene
 * NSS y usa su CLAVE_EMBARAZO (el nombre).
 */
@Repository
@ConditionalOnProperty(name = "portal.historico.fuente", havingValue = "VISTA")
public class VistaMaternidadRepository {

    private static final String VISTA = " FROM SERV_MED_V_MATERNIDAD e";
    private static final String PERSONA = "NVL(e.NSS, e.CLAVE_EMBARAZO)";
    /** El WS del portal mandaba "0" cuando no habia estatus: no es un estatus. */
    private static final String ESTATUS = "CASE WHEN e.STATUS_LABORAL IS NULL OR TRIM(e.STATUS_LABORAL) = '0' THEN '"
            + SIN_DATO + "' ELSE TRIM(e.STATUS_LABORAL) END";

    private final JdbcTemplate jdbc;

    public VistaMaternidadRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** {seguimientos, personas, reincorporadas, revisiones en los proximos 30 dias}. */
    public long[] totales(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        VistaFiltro f = new VistaFiltro("SELECT COUNT(*), COUNT(DISTINCT " + PERSONA + "), "
                + "COUNT(DISTINCT CASE WHEN e.FECHA_REAL_REINICIO IS NOT NULL THEN " + PERSONA + " END), "
                + "SUM(CASE WHEN e.PROXIMA_REVISION BETWEEN TRUNC(SYSDATE) AND TRUNC(SYSDATE) + 30 THEN 1 ELSE 0 END)"
                + VISTA).filtros(desde, hasta, predio, cuenta);
        return jdbc.queryForObject(f.sql(),
                (rs, i) -> new long[]{rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getLong(4)}, f.params());
    }

    /**
     * C-037 / P-028 (18-nov-2026): cuantas hay HOY {laborando embarazadas, en incapacidad, en lactancia},
     * segun el estatus laboral del ultimo seguimiento de cada embarazo (SERV_MED_V_MATERNIDAD_ACTUAL).
     * Es una foto del presente: respeta predio y cuenta pero no el periodo de la pantalla.
     */
    public long[] estadoActual(String predio, String cuenta) {
        String st = "UPPER(TRIM(e.STATUS_LABORAL))";
        VistaFiltro f = new VistaFiltro("SELECT "
                + "SUM(CASE WHEN " + st + " = 'LABORANDO EMBARAZADA' THEN 1 ELSE 0 END), "
                + "SUM(CASE WHEN " + st + " = 'INCAPACIDAD' THEN 1 ELSE 0 END), "
                + "SUM(CASE WHEN " + st + " = 'LACTANCIA' THEN 1 ELSE 0 END)"
                + " FROM SERV_MED_V_MATERNIDAD_ACTUAL e").filtros(null, null, predio, cuenta);
        return jdbc.queryForObject(f.sql(),
                (rs, i) -> new long[]{rs.getLong(1), rs.getLong(2), rs.getLong(3)}, f.params());
    }

    /**
     * Semanas de gestacion de cada renglon (el promedio y el trimestre los calcula el servicio, con
     * el mismo corte de siempre). De 1 a 45: fuera de ahi es una formula sobre una FUM vacia.
     */
    public List<Integer> semanas(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        VistaFiltro f = new VistaFiltro("SELECT e.SEMANAS" + VISTA).and("e.SEMANAS BETWEEN 1 AND 45")
                .filtros(desde, hasta, predio, cuenta);
        return jdbc.query(f.sql(), (rs, i) -> rs.getInt(1), f.params());
    }

    /** Seguimientos por estatus laboral o por predio. */
    public List<ConteoSimpleDto> porDimension(String dimension, LocalDate desde, LocalDate hasta,
                                              String predio, String cuenta) {
        String expr = switch (dimension) {
            case "ESTATUS" -> ESTATUS;
            case "PREDIO" -> PREDIO;
            default -> throw new IllegalArgumentException("Dimension no permitida en maternidad: " + dimension);
        };
        VistaFiltro f = new VistaFiltro("SELECT " + expr + " clave, COUNT(*) cantidad" + VISTA)
                .filtros(desde, hasta, predio, cuenta);
        return jdbc.query(f.sql() + " GROUP BY " + expr + " ORDER BY COUNT(*) DESC",
                (rs, i) -> new ConteoSimpleDto(rs.getString("clave"), rs.getLong("cantidad")), f.params());
    }

    /**
     * SM 24-nov-2026 (preguntas del 23-nov, P-064): numero de PERSONAS por cuenta. Por persona y no por renglon:
     * cada embarazo lleva un seguimiento por mes.
     */
    public List<ConteoSimpleDto> personasPorCuenta(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        String expr = "NVL(TRIM(e.CUENTA), '" + SIN_DATO + "')";
        VistaFiltro f = new VistaFiltro("SELECT " + expr + " clave, COUNT(DISTINCT " + PERSONA + ") cantidad" + VISTA)
                .filtros(desde, hasta, predio, cuenta);
        return jdbc.query(f.sql() + " GROUP BY " + expr + " ORDER BY 2 DESC, 1",
                (rs, i) -> new ConteoSimpleDto(rs.getString("clave"), rs.getLong("cantidad")), f.params());
    }

    /**
     * SM 24-nov-2026 (P-064): personas por rango de edad, con los mismos rangos que el resto del portal. La edad
     * de cada persona es la mayor de sus seguimientos del periodo (si cumple anios a la mitad no se cuenta dos
     * veces). Sin fecha de nacimiento, o con una edad imposible, cae en «Sin dato».
     */
    public List<ConteoSimpleDto> personasPorEdad(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        VistaFiltro f = new VistaFiltro("SELECT " + PERSONA + " persona, MAX(e.EDAD) edad" + VISTA)
                .filtros(desde, hasta, predio, cuenta);
        String rango = "CASE WHEN edad IS NULL OR edad < 15 OR edad > 100 THEN '" + SIN_DATO + "'"
                + " WHEN edad <= 25 THEN '18-25' WHEN edad <= 35 THEN '26-35' WHEN edad <= 45 THEN '36-45'"
                + " WHEN edad <= 55 THEN '46-55' ELSE '55+' END";
        return jdbc.query("SELECT " + rango + " clave, COUNT(*) cantidad FROM (" + f.sql() + " GROUP BY " + PERSONA
                        + ") GROUP BY " + rango + " ORDER BY 1",
                (rs, i) -> new ConteoSimpleDto(rs.getString("clave"), rs.getLong("cantidad")), f.params());
    }

    /** Seguimientos por mes de corte. */
    public List<PuntoMensualDto> tendenciaMensual(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        VistaFiltro f = new VistaFiltro("SELECT " + MES + " mes, COUNT(*) cantidad" + VISTA)
                .filtros(desde, hasta, predio, cuenta);
        return jdbc.query(f.sql() + " GROUP BY " + MES + " ORDER BY 1",
                (rs, i) -> new PuntoMensualDto(rs.getString("mes"), rs.getLong("cantidad")), f.params());
    }

    /**
     * Partos probables por mes, por EMBARAZO: con historial, el mismo parto aparece en cada mes
     * reportado y contar renglones lo multiplicaria.
     */
    public List<PuntoMensualDto> partosPorMes(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        String mesParto = "TO_CHAR(e.FECHA_PROB_PARTO, 'YYYY-MM')";
        VistaFiltro f = new VistaFiltro("SELECT " + mesParto + " mes, COUNT(DISTINCT e.CLAVE_EMBARAZO) cantidad" + VISTA)
                .and("e.FECHA_PROB_PARTO IS NOT NULL").filtros(desde, hasta, predio, cuenta);
        return jdbc.query(f.sql() + " GROUP BY " + mesParto + " ORDER BY 1",
                (rs, i) -> new PuntoMensualDto(rs.getString("mes"), rs.getLong("cantidad")), f.params());
    }
}
