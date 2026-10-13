package com.onest.app.catalog.dashboard.repository;

import static com.onest.app.catalog.dashboard.repository.VistaFiltro.MES;
import static com.onest.app.catalog.dashboard.repository.VistaFiltro.PREDIO;
import static com.onest.app.catalog.dashboard.repository.VistaFiltro.SIN_DATO;

import com.onest.app.catalog.dashboard.dto.ConteoCruzadoDto;
import com.onest.app.catalog.dashboard.dto.ConteoSimpleDto;
import com.onest.app.catalog.dashboard.dto.PuntoMensualDto;
import java.time.LocalDate;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Antidoping COMPLETO (portal + Excel por NSS + carga vieja mientras el Excel nuevo este vacio)
 * leido de la vista SERV_MED_V_ANTIDOPING (v4 de excel-nss-05-vistas.sql). Con este repositorio,
 * DashboardAntidopingService ya no llama al WS de ORDS para graficar.
 *
 * <p>Solo existe con {@code portal.historico.fuente=VISTA}.
 *
 * <p>Diferencia con el camino viejo: la sustancia se cuenta por prueba (una por renglon); la carga
 * vieja contaba hallazgos y un positivo podia traer dos sustancias. Y el estatus junta conclusion y
 * estatus laboral como lo hacia el portal (STATUS_CONCLUSION), con el texto del layout.
 */
@Repository
@ConditionalOnProperty(name = "portal.historico.fuente", havingValue = "VISTA")
public class VistaAntidopingRepository {

    private static final String VISTA = " FROM SERV_MED_V_ANTIDOPING e";
    private static final String TIPO = "NVL(e.TIPO_PRUEBA, '" + SIN_DATO + "')";

    private final JdbcTemplate jdbc;

    public VistaAntidopingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** {pruebas, positivos}. */
    public long[] totales(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        VistaFiltro f = new VistaFiltro("SELECT COUNT(*), SUM(CASE WHEN e.RESULTADO = 'POSITIVO' THEN 1 ELSE 0 END)"
                + VISTA).filtros(desde, hasta, predio, cuenta);
        return jdbc.queryForObject(f.sql(), (rs, i) -> new long[]{rs.getLong(1), rs.getLong(2)}, f.params());
    }

    /**
     * Pruebas por una dimension. Sustancia y estatus solo cuentan los renglones que los traen (como
     * el historico hasta hoy): la sustancia solo existe en los positivos.
     */
    public List<ConteoSimpleDto> porDimension(String dimension, LocalDate desde, LocalDate hasta,
                                              String predio, String cuenta) {
        String expr;
        String condicion = null;
        switch (dimension) {
            case "TIPO" -> expr = TIPO;
            case "RESULTADO" -> expr = "NVL(e.RESULTADO, '" + SIN_DATO + "')";
            case "SUSTANCIA" -> { expr = "e.SUSTANCIA"; condicion = "e.SUSTANCIA IS NOT NULL"; }
            case "STATUS" -> { expr = "NVL(e.CONCLUSION, e.STATUS)"; condicion = "NVL(e.CONCLUSION, e.STATUS) IS NOT NULL"; }
            case "PREDIO" -> expr = PREDIO;
            default -> throw new IllegalArgumentException("Dimension no permitida en antidoping: " + dimension);
        }
        VistaFiltro f = new VistaFiltro("SELECT " + expr + " clave, COUNT(*) cantidad" + VISTA);
        if (condicion != null) {
            f.and(condicion);
        }
        f.filtros(desde, hasta, predio, cuenta);
        return jdbc.query(f.sql() + " GROUP BY " + expr + " ORDER BY COUNT(*) DESC",
                (rs, i) -> new ConteoSimpleDto(rs.getString("clave"), rs.getLong("cantidad")), f.params());
    }

    /** Predio x tipo de prueba (sin costo: el antidoping no lo tiene). */
    public List<ConteoCruzadoDto> porPredioYTipo(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        VistaFiltro f = new VistaFiltro("SELECT " + PREDIO + " fila, " + TIPO + " columna, COUNT(*) cantidad" + VISTA)
                .filtros(desde, hasta, predio, cuenta);
        return jdbc.query(f.sql() + " GROUP BY " + PREDIO + ", " + TIPO + " ORDER BY COUNT(*) DESC",
                (rs, i) -> new ConteoCruzadoDto(rs.getString("fila"), rs.getString("columna"), rs.getLong("cantidad"), 0),
                f.params());
    }

    public List<PuntoMensualDto> tendenciaMensual(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        VistaFiltro f = new VistaFiltro("SELECT " + MES + " mes, COUNT(*) cantidad" + VISTA)
                .filtros(desde, hasta, predio, cuenta);
        return jdbc.query(f.sql() + " GROUP BY " + MES + " ORDER BY 1",
                (rs, i) -> new PuntoMensualDto(rs.getString("mes"), rs.getLong("cantidad")), f.params());
    }
}
