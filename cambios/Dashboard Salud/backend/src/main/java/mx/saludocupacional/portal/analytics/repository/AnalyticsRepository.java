package mx.saludocupacional.portal.analytics.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Consultas agregadas del portal.
 *
 * <p>Usa SQL directo en lugar de JPA porque estas consultas resumen decenas de
 * miles de filas y conviene que la base haga el trabajo. Todas comparten la
 * misma forma de filtrar por año, mes y predio, de modo que las cifras del
 * dashboard y de los reportes provienen siempre del mismo lugar.
 */
@Repository
@RequiredArgsConstructor
public class AnalyticsRepository {

    private static final String FILTRO_PERIODO = """
            AND p.anio = :anio
            AND (CAST(:mes AS INTEGER) IS NULL OR p.mes = :mes)
            AND (CAST(:predioId AS BIGINT) IS NULL OR t.predio_id = :predioId)
            """;

    private final NamedParameterJdbcTemplate jdbc;

    /** Total de atenciones médicas del periodo. */
    public long contarAtenciones(int anio, Integer mes, Long predioId) {
        String sql = """
                SELECT COUNT(*) FROM medical_attentions t
                JOIN periods p ON p.id = t.period_id
                WHERE t.deleted_at IS NULL
                """ + FILTRO_PERIODO;
        return escalar(sql, anio, mes, predioId);
    }

    /** Personas distintas con al menos una incapacidad en el periodo. */
    public long contarPersonasIncapacitadas(int anio, Integer mes, Long predioId) {
        String sql = """
                SELECT COUNT(DISTINCT t.employee_id) FROM disabilities t
                JOIN periods p ON p.id = t.period_id
                WHERE t.deleted_at IS NULL
                """ + FILTRO_PERIODO;
        return escalar(sql, anio, mes, predioId);
    }

    /** Días de incapacidad acumulados, cualquiera que sea su origen. */
    public long sumarDiasIncapacidad(int anio, Integer mes, Long predioId) {
        String sql = """
                SELECT COALESCE(SUM(t.dias_incapacidad), 0) FROM disabilities t
                JOIN periods p ON p.id = t.period_id
                WHERE t.deleted_at IS NULL
                """ + FILTRO_PERIODO;
        return escalar(sql, anio, mes, predioId);
    }

    public long contarAccidentes(int anio, Integer mes, Long predioId, String codigoTipo) {
        String sql = """
                SELECT COUNT(*) FROM accidents t
                JOIN periods p ON p.id = t.period_id
                JOIN accident_types at ON at.id = t.accident_type_id
                WHERE t.deleted_at IS NULL
                  AND (CAST(:codigoTipo AS VARCHAR) IS NULL OR at.codigo = :codigoTipo)
                """ + FILTRO_PERIODO;

        MapSqlParameterSource params = parametros(anio, mes, predioId)
                .addValue("codigoTipo", codigoTipo);
        Long total = jdbc.queryForObject(sql, params, Long.class);
        return total == null ? 0 : total;
    }

    public long contarExamenes(int anio, Integer mes, Long predioId, String codigoResultado) {
        String sql = """
                SELECT COUNT(*) FROM medical_exams t
                JOIN periods p ON p.id = t.period_id
                JOIN medical_exam_results r ON r.id = t.medical_exam_result_id
                WHERE t.deleted_at IS NULL
                  AND (CAST(:codigoResultado AS VARCHAR) IS NULL OR r.codigo = :codigoResultado)
                """ + FILTRO_PERIODO;

        MapSqlParameterSource params = parametros(anio, mes, predioId)
                .addValue("codigoResultado", codigoResultado);
        Long total = jdbc.queryForObject(sql, params, Long.class);
        return total == null ? 0 : total;
    }

    public long contarPruebas(int anio, Integer mes, Long predioId, String codigoTipo) {
        String sql = """
                SELECT COUNT(*) FROM drug_tests t
                JOIN periods p ON p.id = t.period_id
                JOIN drug_test_types tt ON tt.id = t.drug_test_type_id
                WHERE t.deleted_at IS NULL
                  AND (CAST(:codigoTipo AS VARCHAR) IS NULL OR tt.codigo = :codigoTipo)
                """ + FILTRO_PERIODO;

        MapSqlParameterSource params = parametros(anio, mes, predioId)
                .addValue("codigoTipo", codigoTipo);
        Long total = jdbc.queryForObject(sql, params, Long.class);
        return total == null ? 0 : total;
    }

    public long contarCasosMaternidad(int anio, Integer mes, Long predioId) {
        String sql = """
                SELECT COUNT(*) FROM maternity_cases t
                JOIN periods p ON p.id = t.period_id
                WHERE t.deleted_at IS NULL
                """ + FILTRO_PERIODO;
        return escalar(sql, anio, mes, predioId);
    }

    /** Costo total de las incapacidades del periodo. */
    public BigDecimal sumarCostoIncapacidades(int anio, Integer mes, Long predioId) {
        String sql = """
                SELECT COALESCE(SUM(c.monto), 0) FROM disability_costs c
                JOIN disabilities t ON t.id = c.disability_id
                JOIN periods p ON p.id = t.period_id
                WHERE t.deleted_at IS NULL
                """ + FILTRO_PERIODO;
        BigDecimal total = jdbc.queryForObject(sql, parametros(anio, mes, predioId), BigDecimal.class);
        return total == null ? BigDecimal.ZERO : total;
    }

    /** Serie de doce meses para el indicador solicitado. */
    public List<Long> serieMensual(String indicador, int anio, Long predioId) {
        String tabla = switch (indicador) {
            case "atenciones" -> "medical_attentions";
            case "accidentes" -> "accidents";
            case "examenes" -> "medical_exams";
            case "pruebas" -> "drug_tests";
            default -> "disabilities";
        };
        boolean sumarDias = "dias".equals(indicador);

        String sql = """
                SELECT p.mes, %s AS total
                FROM %s t
                JOIN periods p ON p.id = t.period_id
                WHERE t.deleted_at IS NULL
                  AND p.anio = :anio
                  AND (CAST(:predioId AS BIGINT) IS NULL OR t.predio_id = :predioId)
                GROUP BY p.mes ORDER BY p.mes
                """.formatted(sumarDias ? "COALESCE(SUM(t.dias_incapacidad), 0)" : "COUNT(*)", tabla);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("anio", anio)
                .addValue("predioId", predioId);

        Map<Integer, Long> porMes = new HashMap<>();
        jdbc.query(sql, params, rs -> {
            porMes.put(rs.getInt("mes"), rs.getLong("total"));
        });

        List<Long> serie = new ArrayList<>(12);
        for (int mes = 1; mes <= 12; mes++) {
            serie.add(porMes.getOrDefault(mes, 0L));
        }
        return serie;
    }

    /** Ranking de predios con sus cifras principales. */
    public List<Map<String, Object>> rankingPredios(int anio, Integer mes) {
        String sql = """
                SELECT pr.id AS predio_id, pr.nombre AS predio,
                       COALESCE(a.total, 0) AS atenciones,
                       COALESCE(d.casos, 0) AS incapacidades,
                       COALESCE(d.dias, 0) AS dias,
                       COALESCE(ac.total, 0) AS accidentes,
                       COALESCE(e.total, 0) AS examenes,
                       COALESCE(d.costo, 0) AS costo
                FROM predios pr
                LEFT JOIN (
                    SELECT t.predio_id, COUNT(*) AS total FROM medical_attentions t
                    JOIN periods p ON p.id = t.period_id
                    WHERE t.deleted_at IS NULL AND p.anio = :anio AND (CAST(:mes AS INTEGER) IS NULL OR p.mes = :mes)
                    GROUP BY t.predio_id
                ) a ON a.predio_id = pr.id
                LEFT JOIN (
                    SELECT t.predio_id, COUNT(*) AS casos,
                           SUM(t.dias_incapacidad) AS dias,
                           SUM(COALESCE(c.monto, 0)) AS costo
                    FROM disabilities t
                    JOIN periods p ON p.id = t.period_id
                    LEFT JOIN disability_costs c ON c.disability_id = t.id
                    WHERE t.deleted_at IS NULL AND p.anio = :anio AND (CAST(:mes AS INTEGER) IS NULL OR p.mes = :mes)
                    GROUP BY t.predio_id
                ) d ON d.predio_id = pr.id
                LEFT JOIN (
                    SELECT t.predio_id, COUNT(*) AS total FROM accidents t
                    JOIN periods p ON p.id = t.period_id
                    WHERE t.deleted_at IS NULL AND p.anio = :anio AND (CAST(:mes AS INTEGER) IS NULL OR p.mes = :mes)
                    GROUP BY t.predio_id
                ) ac ON ac.predio_id = pr.id
                LEFT JOIN (
                    SELECT t.predio_id, COUNT(*) AS total FROM medical_exams t
                    JOIN periods p ON p.id = t.period_id
                    WHERE t.deleted_at IS NULL AND p.anio = :anio AND (CAST(:mes AS INTEGER) IS NULL OR p.mes = :mes)
                    GROUP BY t.predio_id
                ) e ON e.predio_id = pr.id
                WHERE pr.activo = true
                ORDER BY atenciones DESC, dias DESC
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("anio", anio)
                .addValue("mes", mes);
        return jdbc.queryForList(sql, params);
    }

    /** Atenciones agrupadas por causa médica, de mayor a menor. */
    public List<Map<String, Object>> distribucionPorCausa(int anio, Integer mes, Long predioId, int limite) {
        String sql = """
                SELECT c.nombre AS categoria, COUNT(*) AS total
                FROM medical_attentions t
                JOIN periods p ON p.id = t.period_id
                JOIN attention_causes c ON c.id = t.attention_cause_id
                WHERE t.deleted_at IS NULL
                  AND p.anio = :anio
                  AND (CAST(:mes AS INTEGER) IS NULL OR p.mes = :mes)
                  AND (CAST(:predioId AS BIGINT) IS NULL OR t.predio_id = :predioId)
                GROUP BY c.nombre
                ORDER BY total DESC
                LIMIT :limite
                """;

        MapSqlParameterSource params = parametros(anio, mes, predioId).addValue("limite", limite);
        return jdbc.queryForList(sql, params);
    }

    /** Días de incapacidad agrupados por tipo. */
    public List<Map<String, Object>> distribucionPorTipoIncapacidad(int anio, Integer mes, Long predioId) {
        String sql = """
                SELECT dt.nombre AS categoria, COALESCE(SUM(t.dias_incapacidad), 0) AS total
                FROM disabilities t
                JOIN periods p ON p.id = t.period_id
                JOIN disability_types dt ON dt.id = t.disability_type_id
                WHERE t.deleted_at IS NULL
                  AND p.anio = :anio
                  AND (CAST(:mes AS INTEGER) IS NULL OR p.mes = :mes)
                  AND (CAST(:predioId AS BIGINT) IS NULL OR t.predio_id = :predioId)
                GROUP BY dt.nombre
                ORDER BY total DESC
                """;
        return jdbc.queryForList(sql, parametros(anio, mes, predioId));
    }

    /** Accidentes agrupados por su mecanismo. */
    public List<Map<String, Object>> distribucionPorCausaAccidente(int anio, Integer mes, Long predioId) {
        String sql = """
                SELECT ac.nombre AS categoria, COUNT(*) AS total
                FROM accidents t
                JOIN periods p ON p.id = t.period_id
                JOIN accident_causes ac ON ac.id = t.accident_cause_id
                WHERE t.deleted_at IS NULL
                  AND p.anio = :anio
                  AND (CAST(:mes AS INTEGER) IS NULL OR p.mes = :mes)
                  AND (CAST(:predioId AS BIGINT) IS NULL OR t.predio_id = :predioId)
                GROUP BY ac.nombre
                ORDER BY total DESC
                """;
        return jdbc.queryForList(sql, parametros(anio, mes, predioId));
    }

    private long escalar(String sql, int anio, Integer mes, Long predioId) {
        Long total = jdbc.queryForObject(sql, parametros(anio, mes, predioId), Long.class);
        return total == null ? 0 : total;
    }

    private MapSqlParameterSource parametros(int anio, Integer mes, Long predioId) {
        return new MapSqlParameterSource()
                .addValue("anio", anio)
                .addValue("mes", mes)
                .addValue("predioId", predioId);
    }
}
