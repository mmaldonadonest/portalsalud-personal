package com.onest.app.catalog.dashboard.repository;

import static com.onest.app.catalog.dashboard.repository.VistaFiltro.MES;
import static com.onest.app.catalog.dashboard.repository.VistaFiltro.PREDIO;
import static com.onest.app.catalog.dashboard.repository.VistaFiltro.SIN_DATO;

import com.onest.app.catalog.dashboard.dto.ConteoCostoDto;
import com.onest.app.catalog.dashboard.dto.ConteoCruzadoDto;
import com.onest.app.catalog.dashboard.dto.PuntoMensualDto;
import java.time.LocalDate;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Accidentes COMPLETOS (portal + Excel) leidos de la vista SERV_MED_V_ACCIDENTE: fase 2 de las
 * vistas. Con este repositorio, DashboardAccidentesService ya no llama al WS de ORDS para graficar.
 *
 * <p>Solo existe con {@code portal.historico.fuente=VISTA}.
 *
 * <p>Diferencias con el camino viejo, a favor: la causa RT ya trae su costo en las dos fuentes (en
 * la carga vieja la causa era el nombre de un atributo y su costo se dejaba en cero), y la fecha
 * es la del accidente (la de registro solo si no se capturo).
 */
@Repository
@ConditionalOnProperty(name = "portal.historico.fuente", havingValue = "VISTA")
public class VistaAccidenteRepository {

    private static final String VISTA = " FROM SERV_MED_V_ACCIDENTE e";
    private static final String TIPO = "NVL(e.TIPO_RIESGO, '" + SIN_DATO + "')";

    private final JdbcTemplate jdbc;

    public VistaAccidenteRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Accidentes y costo total. */
    public ConteoCostoDto totales(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        VistaFiltro f = new VistaFiltro("SELECT COUNT(*), NVL(SUM(e.COSTO), 0)" + VISTA)
                .filtros(desde, hasta, predio, cuenta);
        return jdbc.queryForObject(f.sql(), (rs, i) -> new ConteoCostoDto("TOTAL", rs.getLong(1), rs.getDouble(2)),
                f.params());
    }

    /**
     * Cantidad y costo por una dimension. La causa RT solo cuenta los accidentes que la traen,
     * como hasta hoy el historico: el layout la deja opcional y una barra "Sin dato" del tamano de
     * todo el Excel taparia las causas reales.
     */
    public List<ConteoCostoDto> porDimension(String dimension, LocalDate desde, LocalDate hasta,
                                             String predio, String cuenta) {
        String expr;
        String condicion = null;
        switch (dimension) {
            case "TIPO_RIESGO" -> expr = TIPO;
            case "CAUSA_RT" -> { expr = "e.CAUSA_RT"; condicion = "e.CAUSA_RT IS NOT NULL"; }
            case "STATUS" -> expr = "NVL(e.STATUS_CALIFICACION, '" + SIN_DATO + "')";
            case "PREDIO" -> expr = PREDIO;
            default -> throw new IllegalArgumentException("Dimension no permitida en accidentes: " + dimension);
        }
        VistaFiltro f = new VistaFiltro("SELECT " + expr + " clave, COUNT(*) cantidad, NVL(SUM(e.COSTO), 0) costo"
                + VISTA);
        if (condicion != null) {
            f.and(condicion);
        }
        f.filtros(desde, hasta, predio, cuenta);
        return jdbc.query(f.sql() + " GROUP BY " + expr + " ORDER BY COUNT(*) DESC",
                (rs, i) -> new ConteoCostoDto(rs.getString("clave"), rs.getLong("cantidad"), rs.getDouble("costo")),
                f.params());
    }

    public List<PuntoMensualDto> tendenciaMensual(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        VistaFiltro f = new VistaFiltro("SELECT " + MES + " mes, COUNT(*) cantidad" + VISTA)
                .filtros(desde, hasta, predio, cuenta);
        return jdbc.query(f.sql() + " GROUP BY " + MES + " ORDER BY 1",
                (rs, i) -> new PuntoMensualDto(rs.getString("mes"), rs.getLong("cantidad")), f.params());
    }

    /** Predio x tipo de riesgo, con costo. */
    public List<ConteoCruzadoDto> porPredioYTipo(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        VistaFiltro f = new VistaFiltro("SELECT " + PREDIO + " fila, " + TIPO + " columna, COUNT(*) cantidad, "
                + "NVL(SUM(e.COSTO), 0) costo" + VISTA).filtros(desde, hasta, predio, cuenta);
        return jdbc.query(f.sql() + " GROUP BY " + PREDIO + ", " + TIPO + " ORDER BY COUNT(*) DESC",
                (rs, i) -> new ConteoCruzadoDto(rs.getString("fila"), rs.getString("columna"),
                        rs.getLong("cantidad"), rs.getDouble("costo")), f.params());
    }

    // ---------------------------------------------------------------------------------------
    //  Expediente por NSS (paso 10 del plan)
    // ---------------------------------------------------------------------------------------

    /** Un accidente del Excel, tal cual quedo en el layout. Solo lectura. */
    public record Historico(LocalDate fecha, String tipoRiesgo, String causaRt, String dxImss,
                            java.math.BigDecimal sdi, String statusCalificacion, String status,
                            java.math.BigDecimal costo, String observaciones, String predio, String area) {
    }

    /**
     * Los accidentes del Excel de un NSS, del mas reciente al mas viejo. Solo FUENTE = 'EXCEL':
     * lo del portal ya lo muestra el modulo desde su WS, y la vista ya dejo fuera lo del Excel
     * que tambien esta en el portal ("manda el portal").
     */
    public List<Historico> historicoPorNss(String nss) {
        return jdbc.query("SELECT e.FECHA, e.TIPO_RIESGO, e.CAUSA_RT, e.DX_IMSS, e.SDI, e.STATUS_CALIFICACION, "
                        + "e.STATUS, e.COSTO, e.OBSERVACIONES, e.PREDIO, e.AREA" + VISTA
                        + " WHERE e.NSS = ? AND e.FUENTE = 'EXCEL' ORDER BY e.FECHA DESC",
                (rs, i) -> new Historico(
                        rs.getDate(1) == null ? null : rs.getDate(1).toLocalDate(),
                        rs.getString(2), rs.getString(3), rs.getString(4), rs.getBigDecimal(5),
                        rs.getString(6), rs.getString(7), rs.getBigDecimal(8), rs.getString(9),
                        rs.getString(10), rs.getString(11)),
                nss);
    }

    /** {tipo de riesgo, mes, cantidad}. */
    public List<Object[]> porTipoYMes(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        VistaFiltro f = new VistaFiltro("SELECT " + TIPO + " tipo, " + MES + " mes, COUNT(*) cantidad" + VISTA)
                .filtros(desde, hasta, predio, cuenta);
        return jdbc.query(f.sql() + " GROUP BY " + TIPO + ", " + MES + " ORDER BY 1, 2",
                (rs, i) -> new Object[]{rs.getString(1), rs.getString(2), rs.getLong(3)}, f.params());
    }
}
