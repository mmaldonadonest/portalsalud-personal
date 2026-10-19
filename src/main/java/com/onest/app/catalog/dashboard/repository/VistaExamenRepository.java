package com.onest.app.catalog.dashboard.repository;

import static com.onest.app.catalog.dashboard.repository.VistaFiltro.MES;
import static com.onest.app.catalog.dashboard.repository.VistaFiltro.PREDIO;

import com.onest.app.catalog.dashboard.dto.ConteoDictamenDto;
import java.time.LocalDate;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Examenes COMPLETOS leidos de la vista SERV_MED_V_EXAMEN: lo capturado en el portal, nuevo
 * ingreso y periodico del Excel por NSS, y pos incapacidad de la carga vieja (su layout sigue en
 * revision). Fase 2 de las vistas: DashboardExamenService ya no llama al WS de ORDS para graficar.
 *
 * <p>Solo existe con {@code portal.historico.fuente=VISTA}.
 *
 * <p>Diferencia con el camino viejo: en la vista cada examen tiene UN dictamen (si el portal marco
 * varios, gana el mas severo), asi que la suma de los cuatro siempre cuadra con el total menos los
 * examenes sin dictamen. Con los datos de produccion al 11-oct no hay examenes con dos marcas, asi
 * que las cifras no cambian.
 */
@Repository
@ConditionalOnProperty(name = "portal.historico.fuente", havingValue = "VISTA")
public class VistaExamenRepository {

    private static final String VISTA = " FROM SERV_MED_V_EXAMEN e";
    private static final String DICTAMENES = "COUNT(*) cantidad, "
            + "SUM(CASE WHEN e.DICTAMEN = 'APTO' THEN 1 ELSE 0 END) apto, "
            + "SUM(CASE WHEN e.DICTAMEN = 'NO_APTO' THEN 1 ELSE 0 END) no_apto, "
            + "SUM(CASE WHEN e.DICTAMEN = 'APTO_CONDICIONADO' THEN 1 ELSE 0 END) cond, "
            + "SUM(CASE WHEN e.DICTAMEN = 'APTO_RESTRINGIDO' THEN 1 ELSE 0 END) restr";

    private final JdbcTemplate jdbc;

    public VistaExamenRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Total del periodo con su desglose por dictamen. */
    public ConteoDictamenDto totales(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        VistaFiltro f = new VistaFiltro("SELECT 'TOTAL' clave, " + DICTAMENES + VISTA)
                .filtros(desde, hasta, predio, cuenta);
        return jdbc.queryForObject(f.sql(), (rs, i) -> conteo(rs), f.params());
    }

    /** Por predio, de mayor a menor. */
    public List<ConteoDictamenDto> porPredio(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        VistaFiltro f = new VistaFiltro("SELECT " + PREDIO + " clave, " + DICTAMENES + VISTA)
                .filtros(desde, hasta, predio, cuenta);
        return jdbc.query(f.sql() + " GROUP BY " + PREDIO + " ORDER BY COUNT(*) DESC", (rs, i) -> conteo(rs),
                f.params());
    }

    /** Por mes (YYYY-MM), en orden cronologico. */
    public List<ConteoDictamenDto> porMes(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        VistaFiltro f = new VistaFiltro("SELECT " + MES + " clave, " + DICTAMENES + VISTA)
                .filtros(desde, hasta, predio, cuenta);
        return jdbc.query(f.sql() + " GROUP BY " + MES + " ORDER BY 1", (rs, i) -> conteo(rs), f.params());
    }

    // ---------------------------------------------------------------------------------------
    //  Expediente por NSS (paso 10 del plan)
    // ---------------------------------------------------------------------------------------

    /** Un examen del Excel, tal cual quedo en el layout. Solo lectura. */
    public record Historico(LocalDate fecha, String tipo, String tipoExamen, String dictamen, String diagnostico,
                            String predio, String area) {
    }

    /**
     * Los examenes del Excel de un NSS (nuevo ingreso, periodico y pos incapacidad), del mas reciente
     * al mas viejo. Solo FUENTE = 'EXCEL': lo del portal ya tiene su propia pantalla, y la vista ya
     * dejo fuera lo del Excel que tambien esta en el portal ("manda el portal").
     */
    public List<Historico> historicoPorNss(String nss) {
        return jdbc.query("SELECT e.FECHA, e.TIPO, e.TIPO_EXAMEN, e.DICTAMEN, e.DIAGNOSTICO, e.PREDIO, e.AREA" + VISTA
                        + " WHERE e.NSS = ? AND e.FUENTE = 'EXCEL' ORDER BY e.FECHA DESC",
                (rs, i) -> new Historico(rs.getDate(1) == null ? null : rs.getDate(1).toLocalDate(),
                        rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6),
                        rs.getString(7)),
                nss);
    }

    private static ConteoDictamenDto conteo(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new ConteoDictamenDto(rs.getString("clave"), rs.getLong("cantidad"), rs.getLong("apto"),
                rs.getLong("no_apto"), rs.getLong("cond"), rs.getLong("restr"));
    }
}
