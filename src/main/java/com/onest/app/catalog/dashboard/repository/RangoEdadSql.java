package com.onest.app.catalog.dashboard.repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Expresion SQL del rango de edad de las atenciones (12-nov-2026).
 *
 * <p>El formato 2026 de atenciones trae el RANGO, no la edad, y se guarda en RANGO_EDAD
 * (docs/script-prod/excel-nss-18-atencion-rango-edad.sql). Si el WAR se instala antes que ese
 * script, leer una columna inexistente tumbaba el Home entero (ORA-00904, HTTP 500). Por eso se
 * pregunta si la columna existe: sin ella se calcula con la edad, como antes.
 *
 * <p>Solo se recuerda el "si existe": mientras falte se vuelve a preguntar, asi el rango aparece
 * en cuanto se corre el script, sin reiniciar.
 */
final class RangoEdadSql {

    private static final String CORTES = "CASE WHEN e.EDAD IS NULL OR e.EDAD <= 0 THEN '"
            + VistaFiltro.SIN_DATO + "' "
            + "WHEN e.EDAD <= 25 THEN '18-25' WHEN e.EDAD <= 35 THEN '26-35' "
            + "WHEN e.EDAD <= 45 THEN '36-45' WHEN e.EDAD <= 55 THEN '46-55' ELSE '55+' END";

    private static final Map<String, Boolean> CON_COLUMNA = new ConcurrentHashMap<>();

    private RangoEdadSql() {
    }

    /**
     * Rango del Excel si el objeto ya tiene RANGO_EDAD; si no, los mismos cortes que
     * DashboardConsultaService.rangoEdad sobre la edad.
     *
     * @param objeto tabla o vista que se lee con alias {@code e}
     */
    static String expresion(JdbcTemplate jdbc, String objeto) {
        return tieneColumna(jdbc, objeto) ? "COALESCE(e.RANGO_EDAD, " + CORTES + ")" : CORTES;
    }

    private static boolean tieneColumna(JdbcTemplate jdbc, String objeto) {
        if (CON_COLUMNA.containsKey(objeto)) {
            return true;
        }
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM ALL_TAB_COLUMNS "
                + "WHERE TABLE_NAME = ? AND COLUMN_NAME = 'RANGO_EDAD'", Integer.class, objeto);
        boolean hay = n != null && n > 0;
        if (hay) {
            CON_COLUMNA.put(objeto, Boolean.TRUE);
        }
        return hay;
    }
}
