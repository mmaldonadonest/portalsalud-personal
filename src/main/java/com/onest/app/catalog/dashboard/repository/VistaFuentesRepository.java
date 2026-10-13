package com.onest.app.catalog.dashboard.repository;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Cuantos registros de cada familia vienen del portal y cuantos del Excel, para el desglose de las
 * tarjetas del home. Responde la pregunta de "las graficas exceden los Excel" (P8 del plan): cada
 * cifra dice de donde salio.
 *
 * <p>Solo existe con {@code portal.historico.fuente=VISTA}: sin las vistas no hay columna FUENTE.
 */
@Repository
@ConditionalOnProperty(name = "portal.historico.fuente", havingValue = "VISTA")
public class VistaFuentesRepository {

    private final JdbcTemplate jdbc;

    public VistaFuentesRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** familia (CONSULTAS, EXAMENES, INCAPACIDADES, ACCIDENTES) -> fuente (PORTAL, EXCEL, CARGA_VIEJA) -> registros. */
    public Map<String, Map<String, Long>> porFamiliaYFuente(LocalDate desde, LocalDate hasta) {
        String rango = " WHERE e.FECHA >= ? AND e.FECHA <= ? GROUP BY e.FUENTE";
        String sql = "SELECT 'CONSULTAS' familia, e.FUENTE, COUNT(*) FROM SERV_MED_V_ATENCION e" + rango
                + " UNION ALL SELECT 'EXAMENES', e.FUENTE, COUNT(*) FROM SERV_MED_V_EXAMEN e" + rango
                + " UNION ALL SELECT 'INCAPACIDADES', e.FUENTE, COUNT(*) FROM SERV_MED_V_INCAPACIDAD e" + rango
                + " UNION ALL SELECT 'ACCIDENTES', e.FUENTE, COUNT(*) FROM SERV_MED_V_ACCIDENTE e" + rango;
        java.sql.Date d = java.sql.Date.valueOf(desde);
        java.sql.Date h = java.sql.Date.valueOf(hasta);
        Map<String, Map<String, Long>> resultado = new LinkedHashMap<>();
        jdbc.query(sql, rs -> {
            resultado.computeIfAbsent(rs.getString(1), k -> new TreeMap<>()).put(rs.getString(2), rs.getLong(3));
        }, d, h, d, h, d, h, d, h);
        return resultado;
    }
}
