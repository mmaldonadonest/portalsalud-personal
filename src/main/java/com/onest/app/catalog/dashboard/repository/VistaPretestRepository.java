package com.onest.app.catalog.dashboard.repository;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * C-006 (18-nov-2026): comparativo por predio de examenes medicos vs Pretest. Solo lectura, base del portal.
 *
 * <p>Examenes: la vista SERV_MED_V_EXAMEN (portal + Excel), la misma de las tarjetas de Analisis > Examenes.
 * Pretest: no hay vista; se arma aqui con alias "e" (NSS, FECHA, PREDIO, CUENTA) para que {@link VistaFiltro}
 * corte igual que en los examenes:
 * <ul>
 *   <li>Desde C-007 cada guardado deja una copia PRETESTHIST.&lt;sello&gt;: un Pretest por copia.</li>
 *   <li>Antes solo se conservaban las respuestas vigentes (cada guardado borraba el anterior): de esos NSS se
 *       cuenta UNO, con la fecha del ultimo guardado. Por eso lo anterior al 18-nov-2026 sale por debajo.</li>
 * </ul>
 * Predio y cuenta del Pretest salen del empleado (SERV_MED_V_EMPLEADO), como lo capturado en el portal.
 */
@Repository
@ConditionalOnProperty(name = "portal.historico.fuente", havingValue = "VISTA")
public class VistaPretestRepository {

    private static final String PRETEST = " FROM (SELECT p.NSS, p.FECHA, emp.PREDIO, emp.CUENTA FROM ("
            + "  SELECT t.NSS, TRUNC(t.CREATED_AT) FECHA FROM SERV_MED_TAG t WHERE t.TYPE LIKE 'PRETESTHIST.%'"
            + "  UNION ALL"
            + "  SELECT t.NSS, TRUNC(MAX(t.CREATED_AT)) FROM SERV_MED_TAG t"
            + "   WHERE t.TAG_GROUP = 'PRETEST' AND t.TYPE LIKE '%PRETEST'"
            + "     AND NOT EXISTS (SELECT 1 FROM SERV_MED_TAG h WHERE h.NSS = t.NSS AND h.TYPE LIKE 'PRETESTHIST.%')"
            + "   GROUP BY t.NSS"
            + " ) p LEFT JOIN SERV_MED_V_EMPLEADO emp ON emp.NSS = p.NSS) e";

    private static final String EXAMEN = " FROM SERV_MED_V_EXAMEN e";

    private final JdbcTemplate jdbc;

    public VistaPretestRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Examenes y Pretest por predio: {predio -> [examenes, pretest]}. */
    public Map<String, long[]> porPredio(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        return cruzar(VistaFiltro.PREDIO, desde, hasta, predio, cuenta);
    }

    /** Examenes y Pretest por mes (YYYY-MM): {mes -> [examenes, pretest]}, en orden cronologico. */
    public Map<String, long[]> porMes(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        Map<String, long[]> m = cruzar(VistaFiltro.MES, desde, hasta, predio, cuenta);
        return new java.util.TreeMap<>(m);
    }

    private Map<String, long[]> cruzar(String clave, LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        Map<String, long[]> out = new LinkedHashMap<>();
        contar(EXAMEN, clave, desde, hasta, predio, cuenta, out, 0);
        contar(PRETEST, clave, desde, hasta, predio, cuenta, out, 1);
        return out;
    }

    private void contar(String from, String clave, LocalDate desde, LocalDate hasta, String predio, String cuenta,
                        Map<String, long[]> out, int col) {
        VistaFiltro f = new VistaFiltro("SELECT " + clave + " clave, COUNT(*) n" + from).filtros(desde, hasta, predio, cuenta);
        jdbc.query(f.sql() + " GROUP BY " + clave + " ORDER BY COUNT(*) DESC",
                (org.springframework.jdbc.core.RowCallbackHandler) rs ->
                        out.computeIfAbsent(rs.getString(1), k -> new long[2])[col] += rs.getLong(2),
                f.params());
    }
}
