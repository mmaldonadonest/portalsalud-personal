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
 * Atenciones COMPLETAS (portal + Excel) leidas de la vista SERV_MED_V_ATENCION: fase 2 de las
 * vistas. Con este repositorio, DashboardConsultaService ya no llama al WS de ORDS para graficar;
 * todo sale de una sola consulta agregada, con la regla "manda el portal" aplicada en la vista.
 *
 * <p>Solo existe con {@code portal.historico.fuente=VISTA}. Requiere la v2 de
 * docs/script-prod/excel-nss-05-vistas.sql (columnas AREA_ACCIDENTE y ES_ACCIDENTE).
 *
 * <p>Diferencias con el camino viejo (ORDS + historico sumados en Java), todas a favor:
 * <ul>
 *   <li>Personas se cuentan una vez aunque esten en las dos fuentes (antes se sumaban).</li>
 *   <li>Genero normalizado a MASC/FEM en las dos fuentes (el WS mandaba el 1/2 del biometrico).</li>
 *   <li>Una consulta del portal no se duplica si el empleado tiene dos renglones laborales.</li>
 *   <li>Edad a la fecha de la atencion, no la edad de hoy.</li>
 * </ul>
 */
@Repository
@ConditionalOnProperty(name = "portal.historico.fuente", havingValue = "VISTA")
public class VistaAtencionRepository {

    private static final String VISTA = " FROM SERV_MED_V_ATENCION e";

    private final JdbcTemplate jdbc;

    public VistaAtencionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Atenciones, personas distintas y atenciones por accidente/emergencia, en una sola pasada. */
    public long[] totales(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        VistaFiltro f = new VistaFiltro("SELECT COUNT(*), COUNT(DISTINCT e.NSS), NVL(SUM(e.ES_ACCIDENTE), 0)" + VISTA)
                .filtros(desde, hasta, predio, cuenta);
        return jdbc.queryForObject(f.sql(), (rs, i) -> new long[]{rs.getLong(1), rs.getLong(2), rs.getLong(3)},
                f.params());
    }

    /**
     * Conteo por una dimension de la pantalla. Causa, tipo y area de accidente solo cuentan los
     * renglones que traen el dato (igual que el historico hasta hoy): sin eso la grafica de causas
     * gana una barra "Sin dato" del tamano de todas las atenciones del Excel sin causa.
     */
    public List<ConteoSimpleDto> porDimension(String dimension, LocalDate desde, LocalDate hasta,
                                              String predio, String cuenta) {
        String expr;
        String condicion = null;
        switch (dimension) {
            case "TIPO" -> { expr = "e.RAMO"; condicion = "e.RAMO IS NOT NULL"; }
            case "CAUSA" -> { expr = "e.CAUSA"; condicion = "e.CAUSA IS NOT NULL"; }
            // El WS escribia '0' cuando no habia area: no es un area
            case "AREA_ACCIDENTE" -> { expr = "e.AREA_ACCIDENTE"; condicion = "NVL(e.AREA_ACCIDENTE, '0') <> '0'"; }
            case "GENERO" -> expr = "NVL(e.GENERO, '" + SIN_DATO + "')";
            case "CUENTA" -> expr = "NVL(e.CUENTA, '" + SIN_DATO + "')";
            case "PREDIO" -> expr = PREDIO;
            // Mismos cortes que DashboardConsultaService.rangoEdad
            case "EDAD" -> expr = "CASE WHEN e.EDAD IS NULL OR e.EDAD <= 0 THEN '" + SIN_DATO + "' "
                    + "WHEN e.EDAD <= 25 THEN '18-25' WHEN e.EDAD <= 35 THEN '26-35' "
                    + "WHEN e.EDAD <= 45 THEN '36-45' WHEN e.EDAD <= 55 THEN '46-55' ELSE '55+' END";
            default -> throw new IllegalArgumentException("Dimension no permitida en atenciones: " + dimension);
        }
        VistaFiltro f = new VistaFiltro("SELECT " + expr + " clave, COUNT(*) cantidad" + VISTA);
        if (condicion != null) {
            f.and(condicion);
        }
        f.filtros(desde, hasta, predio, cuenta);
        return jdbc.query(f.sql() + " GROUP BY " + expr + " ORDER BY COUNT(*) DESC",
                (rs, i) -> new ConteoSimpleDto(rs.getString("clave"), rs.getLong("cantidad")), f.params());
    }

    public List<PuntoMensualDto> tendenciaMensual(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        VistaFiltro f = new VistaFiltro("SELECT " + MES + " mes, COUNT(*) cantidad" + VISTA)
                .filtros(desde, hasta, predio, cuenta);
        return jdbc.query(f.sql() + " GROUP BY " + MES + " ORDER BY 1",
                (rs, i) -> new PuntoMensualDto(rs.getString("mes"), rs.getLong("cantidad")), f.params());
    }

    // ---------------------------------------------------------------------------------------
    //  Expediente por NSS (paso 10 del plan)
    // ---------------------------------------------------------------------------------------

    /** Una atencion del Excel, tal cual quedo en el layout. Solo lectura. */
    public record Historica(LocalDate fecha, String ramo, String causa, String lesionMe, String diagnostico,
                            String tratamiento, String predio, String area) {
    }

    /**
     * Las atenciones del Excel de un NSS, de la mas reciente a la mas vieja. Solo FUENTE = 'EXCEL':
     * lo del portal ya lo muestra el modulo desde su WS, y la vista ya dejo fuera lo del Excel
     * que tambien esta en el portal ("manda el portal").
     */
    public List<Historica> historicoPorNss(String nss) {
        return jdbc.query("SELECT e.FECHA, e.RAMO, e.CAUSA, e.LESION_ME, e.DIAGNOSTICO, e.TRATAMIENTO, e.PREDIO, e.AREA"
                        + VISTA + " WHERE e.NSS = ? AND e.FUENTE = 'EXCEL' ORDER BY e.FECHA DESC",
                (rs, i) -> new Historica(rs.getDate(1) == null ? null : rs.getDate(1).toLocalDate(),
                        rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6),
                        rs.getString(7), rs.getString(8)),
                nss);
    }

    // ---------------------------------------------------------------------------------------
    //  Musculoesqueletico
    // ---------------------------------------------------------------------------------------

    /** Renglon del portal para clasificar en Java (LesionMusculoesqueletica): diagnostico + area. */
    public record FilaPortal(String nss, LocalDate fecha, String predio, String diagnostico, String areaInvolucrada) {
    }

    /**
     * Las atenciones capturadas en el portal, renglon por renglon. Son pocas (cientos) y la lesion
     * se deduce en Java de la clave CIE-10 y el area anatomica; repetir esa regla en SQL dejaria dos
     * versiones del mismo criterio.
     */
    public List<FilaPortal> filasPortal(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        VistaFiltro f = new VistaFiltro("SELECT e.NSS, e.FECHA, " + PREDIO + " predio, e.DIAGNOSTICO, "
                + "e.AREA_INVOLUCRADA" + VISTA).and("e.FUENTE = 'PORTAL'").filtros(desde, hasta, predio, cuenta);
        return jdbc.query(f.sql(), (rs, i) -> new FilaPortal(rs.getString(1),
                rs.getDate(2) == null ? null : rs.getDate(2).toLocalDate(), rs.getString(3), rs.getString(4),
                rs.getString(5)), f.params());
    }

    /**
     * Lesiones del Excel, que ya llegan clasificadas (la enfermera marca el tipo en el layout).
     * LESION_ME solo existe del lado del Excel, asi que no hace falta filtrar la fuente.
     * dimension: TIPO (la etiqueta del layout), PREDIO o MES.
     */
    public List<ConteoSimpleDto> lesionesExcel(String dimension, LocalDate desde, LocalDate hasta,
                                               String predio, String cuenta) {
        String expr = switch (dimension) {
            case "TIPO" -> "e.LESION_ME";
            case "PREDIO" -> PREDIO;
            case "MES" -> MES;
            default -> throw new IllegalArgumentException("Dimension no permitida en lesiones: " + dimension);
        };
        VistaFiltro f = new VistaFiltro("SELECT " + expr + " clave, COUNT(*) cantidad" + VISTA)
                .and("e.LESION_ME IS NOT NULL").filtros(desde, hasta, predio, cuenta);
        return jdbc.query(f.sql() + " GROUP BY " + expr + " ORDER BY " + ("MES".equals(dimension) ? "1" : "2 DESC"),
                (rs, i) -> new ConteoSimpleDto(rs.getString("clave"), rs.getLong("cantidad")), f.params());
    }

    /** NSS distintos con lesion en el Excel, para juntarlos con los del portal (personas = union). */
    public List<String> nssConLesionExcel(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        VistaFiltro f = new VistaFiltro("SELECT DISTINCT e.NSS" + VISTA)
                .and("e.LESION_ME IS NOT NULL").and("e.NSS IS NOT NULL").filtros(desde, hasta, predio, cuenta);
        return jdbc.queryForList(f.sql(), String.class, f.params());
    }

    /** {predio, mes, cantidad}. */
    public List<Object[]> tendenciaPorPredio(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        VistaFiltro f = new VistaFiltro("SELECT " + PREDIO + " predio, " + MES + " mes, COUNT(*) cantidad" + VISTA)
                .filtros(desde, hasta, predio, cuenta);
        return jdbc.query(f.sql() + " GROUP BY " + PREDIO + ", " + MES + " ORDER BY 1, 2",
                (rs, i) -> new Object[]{rs.getString(1), rs.getString(2), rs.getLong(3)}, f.params());
    }
}
