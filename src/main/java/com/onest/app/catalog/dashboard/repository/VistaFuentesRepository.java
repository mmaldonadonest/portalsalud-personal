package com.onest.app.catalog.dashboard.repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
        return porFamiliaYFuente(desde, hasta, null);
    }

    /**
     * Con predio (14-oct-2026, filtro del Inicio): mismo corte que las tarjetas (VistaFiltro), si no
     * la linea "Portal · Excel" sumaria todos los predios debajo de una tarjeta filtrada.
     */
    public Map<String, Map<String, Long>> porFamiliaYFuente(LocalDate desde, LocalDate hasta, String predio) {
        Map<String, Map<String, Long>> resultado = new LinkedHashMap<>();
        Map<String, String> vistas = new LinkedHashMap<>();
        vistas.put("CONSULTAS", "SERV_MED_V_ATENCION");
        vistas.put("EXAMENES", "SERV_MED_V_EXAMEN");
        vistas.put("INCAPACIDADES", "SERV_MED_V_INCAPACIDAD");
        vistas.put("ACCIDENTES", "SERV_MED_V_ACCIDENTE");
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        vistas.forEach((familia, vista) -> {
            VistaFiltro f = new VistaFiltro("SELECT '" + familia + "' familia, e.FUENTE, COUNT(*) FROM " + vista + " e")
                    .filtros(desde, hasta, predio, null);
            if (sql.length() > 0) {
                sql.append(" UNION ALL ");
            }
            sql.append(f.sql()).append(" GROUP BY e.FUENTE");
            params.addAll(List.of(f.params()));
        });
        jdbc.query(sql.toString(), rs -> {
            resultado.computeIfAbsent(rs.getString(1), k -> new TreeMap<>()).put(rs.getString(2), rs.getLong(3));
        }, params.toArray());
        return resultado;
    }
}
