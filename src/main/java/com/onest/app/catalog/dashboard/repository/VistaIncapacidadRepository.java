package com.onest.app.catalog.dashboard.repository;

import static com.onest.app.catalog.dashboard.repository.VistaFiltro.MES;
import static com.onest.app.catalog.dashboard.repository.VistaFiltro.PREDIO;
import static com.onest.app.catalog.dashboard.repository.VistaFiltro.SIN_DATO;

import com.onest.app.catalog.dashboard.dto.ConteoDto;
import com.onest.app.catalog.dashboard.dto.ConteoSimpleDto;
import java.time.LocalDate;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Incapacidades COMPLETAS (portal + Excel) leidas de la vista SERV_MED_V_INCAPACIDAD: fase 2 de
 * las vistas. DashboardIncapacidadesService ya no llama al WS de ORDS para graficar.
 *
 * <p>Solo existe con {@code portal.historico.fuente=VISTA}.
 *
 * <p>Mismos criterios que el WS {@code consulta_incapacidades_fecha_cta}: el periodo se corta por
 * la FECHA DE INICIO del certificado (si no se puede leer, la de registro), y dias y costo se
 * suman tal cual. Lo que cambia, a favor: el ramo del portal (enfermedad_general, rt1...) sale con
 * el codigo del layout (EG, RT1...) y suma en la misma barra que el Excel, y las personas se
 * cuentan una vez aunque esten en las dos fuentes.
 */
@Repository
@ConditionalOnProperty(name = "portal.historico.fuente", havingValue = "VISTA")
public class VistaIncapacidadRepository {

    private static final String VISTA = " FROM SERV_MED_V_INCAPACIDAD e";
    private static final String RAMO = "NVL(e.RAMO, '" + SIN_DATO + "')";
    /** La pantalla agrupa el rubro buscando "imss" / "interna" en el texto; lo demas va a "sin rubro". */
    private static final String RUBRO = "CASE e.RUBRO WHEN 'INTERNA' THEN 'Interna' WHEN 'IMSS' THEN 'IMSS' "
            + "ELSE '" + SIN_DATO + "' END";
    private static final String AGREGADOS = "COUNT(*) cantidad, NVL(SUM(e.DIAS), 0) dias, NVL(SUM(e.COSTO), 0) costo";

    private final JdbcTemplate jdbc;

    public VistaIncapacidadRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** {incapacidades, personas distintas, dias} y el costo aparte (double). */
    public Totales totales(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        VistaFiltro f = new VistaFiltro("SELECT COUNT(*), COUNT(DISTINCT e.NSS), NVL(SUM(e.DIAS), 0), "
                + "NVL(SUM(e.COSTO), 0)" + VISTA).filtros(desde, hasta, predio, cuenta);
        return jdbc.queryForObject(f.sql(), (rs, i) -> new Totales(rs.getLong(1), rs.getLong(2), rs.getLong(3),
                rs.getDouble(4)), f.params());
    }

    public record Totales(long incapacidades, long personas, long dias, double costo) {
    }

    /**
     * Incapacidades, dias y costo por una dimension. El estado del dictamen solo existe en el
     * portal (el layout no lo trae): ahi se cuentan solo los renglones del portal, como hasta hoy,
     * para no meter todo el Excel en una barra "Sin dato".
     */
    public List<ConteoDto> porDimension(String dimension, LocalDate desde, LocalDate hasta,
                                        String predio, String cuenta) {
        String expr;
        String condicion = null;
        switch (dimension) {
            case "RAMO" -> expr = RAMO;
            case "RUBRO" -> expr = RUBRO;
            case "PREDIO" -> expr = PREDIO;
            case "ESTADO" -> { expr = "NVL(e.ESTADO_DICTAMEN, '" + SIN_DATO + "')"; condicion = "e.FUENTE = 'PORTAL'"; }
            default -> throw new IllegalArgumentException("Dimension no permitida en incapacidades: " + dimension);
        }
        VistaFiltro f = new VistaFiltro("SELECT " + expr + " clave, " + AGREGADOS + VISTA);
        if (condicion != null) {
            f.and(condicion);
        }
        f.filtros(desde, hasta, predio, cuenta);
        return jdbc.query(f.sql() + " GROUP BY " + expr + " ORDER BY COUNT(*) DESC",
                (rs, i) -> new ConteoDto(rs.getString("clave"), rs.getLong("cantidad"), rs.getLong("dias"),
                        rs.getDouble("costo")), f.params());
    }

    /** Por mes (YYYY-MM): clave = mes, cantidad = incapacidades, diasAutorizados = dias. */
    public List<ConteoDto> porMes(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        VistaFiltro f = new VistaFiltro("SELECT " + MES + " clave, " + AGREGADOS + VISTA)
                .filtros(desde, hasta, predio, cuenta);
        return jdbc.query(f.sql() + " GROUP BY " + MES + " ORDER BY 1",
                (rs, i) -> new ConteoDto(rs.getString("clave"), rs.getLong("cantidad"), rs.getLong("dias"),
                        rs.getDouble("costo")), f.params());
    }

    /** {ramo, mes, dias}. */
    public List<Object[]> diasPorRamoYMes(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        VistaFiltro f = new VistaFiltro("SELECT " + RAMO + " ramo, " + MES + " mes, NVL(SUM(e.DIAS), 0) dias" + VISTA)
                .filtros(desde, hasta, predio, cuenta);
        return jdbc.query(f.sql() + " GROUP BY " + RAMO + ", " + MES + " ORDER BY 1, 2",
                (rs, i) -> new Object[]{rs.getString(1), rs.getString(2), rs.getLong(3)}, f.params());
    }

    /** Personas (NSS distintos) por rubro, de mayor a menor. */
    public List<ConteoSimpleDto> personasPorRubro(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        VistaFiltro f = new VistaFiltro("SELECT " + RUBRO + " clave, COUNT(DISTINCT e.NSS) cantidad" + VISTA)
                .and("e.NSS IS NOT NULL").filtros(desde, hasta, predio, cuenta);
        return jdbc.query(f.sql() + " GROUP BY " + RUBRO + " ORDER BY 2 DESC",
                (rs, i) -> new ConteoSimpleDto(rs.getString("clave"), rs.getLong("cantidad")), f.params());
    }

    // ---------------------------------------------------------------------------------------
    //  Expediente por NSS (paso 10 del plan)
    // ---------------------------------------------------------------------------------------

    /** Una incapacidad del Excel, tal cual quedo en el layout. Solo lectura. */
    public record Historica(LocalDate inicio, LocalDate termino, String folio, String rubro, String ramo,
                            String tipo, Long dias, java.math.BigDecimal costo, String predio, String area,
                            String diagnostico) {
    }

    /**
     * Las incapacidades del Excel de un NSS, de la mas reciente a la mas vieja. Solo FUENTE =
     * 'EXCEL': lo del portal ya lo muestra el modulo desde su WS, y la vista ya dejo fuera lo
     * del Excel que tambien esta en el portal ("manda el portal").
     */
    public List<Historica> historicoPorNss(String nss) {
        return jdbc.query("SELECT e.FECHA, e.FECHA_TERMINO, e.FOLIO, e.RUBRO, e.RAMO, e.TIPO, e.DIAS, e.COSTO, "
                        + "e.PREDIO, e.AREA, e.DIAGNOSTICO" + VISTA + " WHERE e.NSS = ? AND e.FUENTE = 'EXCEL' "
                        + "ORDER BY e.FECHA DESC",
                (rs, i) -> new Historica(
                        rs.getDate(1) == null ? null : rs.getDate(1).toLocalDate(),
                        rs.getDate(2) == null ? null : rs.getDate(2).toLocalDate(),
                        rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6),
                        rs.getObject(7) == null ? null : rs.getLong(7), rs.getBigDecimal(8),
                        rs.getString(9), rs.getString(10), rs.getString(11)),
                nss);
    }

    /** {rubro, mes, personas distintas}. */
    public List<Object[]> personasPorRubroYMes(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        VistaFiltro f = new VistaFiltro("SELECT " + RUBRO + " rubro, " + MES + " mes, COUNT(DISTINCT e.NSS) personas"
                + VISTA).and("e.NSS IS NOT NULL").filtros(desde, hasta, predio, cuenta);
        return jdbc.query(f.sql() + " GROUP BY " + RUBRO + ", " + MES + " ORDER BY 1, 2",
                (rs, i) -> new Object[]{rs.getString(1), rs.getString(2), rs.getLong(3)}, f.params());
    }
}
