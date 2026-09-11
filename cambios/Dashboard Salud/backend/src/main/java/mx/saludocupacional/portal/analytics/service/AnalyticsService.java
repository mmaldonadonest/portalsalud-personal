package mx.saludocupacional.portal.analytics.service;

import lombok.RequiredArgsConstructor;
import mx.saludocupacional.portal.analytics.dto.AnalyticsDtos.Distribucion;
import mx.saludocupacional.portal.analytics.dto.AnalyticsDtos.Kpi;
import mx.saludocupacional.portal.analytics.dto.AnalyticsDtos.RankingPredio;
import mx.saludocupacional.portal.analytics.dto.AnalyticsDtos.ResumenEjecutivo;
import mx.saludocupacional.portal.analytics.dto.AnalyticsDtos.SerieMensual;
import mx.saludocupacional.portal.analytics.repository.AnalyticsRepository;
import mx.saludocupacional.portal.shared.domain.Period;
import mx.saludocupacional.portal.shared.service.ThresholdService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Capa analítica única del portal.
 *
 * <p>Dashboard, reportes y cualquier consumidor futuro obtienen sus cifras de
 * aquí. Concentrarlas en un solo lugar garantiza que la pantalla y el PDF nunca
 * muestren números distintos por haber duplicado una fórmula.
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final String[] MESES = {
            "ENE", "FEB", "MAR", "ABR", "MAY", "JUN", "JUL", "AGO", "SEP", "OCT", "NOV", "DIC"
    };
    private static final int HORAS_POR_JORNADA = 8;

    private final AnalyticsRepository repository;
    private final ThresholdService umbrales;

    /**
     * Resumen ejecutivo del periodo.
     *
     * @param mes nulo para obtener el acumulado del año
     */
    @Transactional(readOnly = true)
    public ResumenEjecutivo resumen(int anio, Integer mes, Long predioId) {
        long atenciones = repository.contarAtenciones(anio, mes, predioId);
        long personas = repository.contarPersonasIncapacitadas(anio, mes, predioId);
        long dias = repository.sumarDiasIncapacidad(anio, mes, predioId);
        long accidentes = repository.contarAccidentes(anio, mes, predioId, null);
        long laborales = repository.contarAccidentes(anio, mes, predioId, "LABORAL");
        long trayecto = repository.contarAccidentes(anio, mes, predioId, "TRAYECTO");
        long examenes = repository.contarExamenes(anio, mes, predioId, null);
        long aptos = repository.contarExamenes(anio, mes, predioId, "APTO");
        long noAptos = repository.contarExamenes(anio, mes, predioId, "NO_APTO");
        long condicionados = repository.contarExamenes(anio, mes, predioId, "CONDICIONADO");
        long pruebas = repository.contarPruebas(anio, mes, predioId, "ANTIDOPING");
        long maternidad = repository.contarCasosMaternidad(anio, mes, predioId);
        BigDecimal costo = repository.sumarCostoIncapacidades(anio, mes, predioId);

        List<Kpi> kpis = List.of(
                kpiCon("atenciones", "Atenciones médicas", atenciones,
                        variacionMensual("atenciones", anio, mes, predioId, atenciones), null),
                kpiCon("personasIncapacitadas", "Personas incapacitadas", personas,
                        variacionMensual("personas", anio, mes, predioId, personas), null),
                kpiCon("diasIncapacidad", "Días de incapacidad", dias,
                        variacionMensual("dias", anio, mes, predioId, dias),
                        "%,d horas no trabajadas".formatted(dias * HORAS_POR_JORNADA)),
                kpiCon("accidentes", "Accidentes", accidentes, null,
                        "%d laboral · %d trayecto".formatted(laborales, trayecto)),
                kpiCon("examenes", "Exámenes médicos", examenes, null,
                        examenes == 0 ? null : "%s aptos".formatted(porcentaje(aptos, examenes))),
                kpiCon("costo", "Costo de incapacidades", costo.longValue(), null, null));

        return new ResumenEjecutivo(
                anio, mes, etiquetaPeriodo(anio, mes),
                atenciones, personas, dias, dias * HORAS_POR_JORNADA,
                accidentes, laborales, trayecto,
                examenes, aptos, noAptos, condicionados,
                pruebas, maternidad, costo, kpis);
    }

    /** Serie de doce meses del indicador solicitado. */
    @Transactional(readOnly = true)
    public SerieMensual serie(String indicador, int anio, Long predioId) {
        List<Long> valores = repository.serieMensual(indicador, anio, predioId);
        return new SerieMensual(indicador, List.of(MESES), valores);
    }

    /** Ranking de predios con su nivel de riesgo calculado sobre los umbrales vigentes. */
    @Transactional(readOnly = true)
    public List<RankingPredio> ranking(int anio, Integer mes) {
        int critico = umbrales.valorEntero(ThresholdService.MORBILIDAD_CRITICO);
        int alto = umbrales.valorEntero(ThresholdService.MORBILIDAD_ALTO);
        int medio = umbrales.valorEntero(ThresholdService.MORBILIDAD_MEDIO);

        List<Map<String, Object>> filas = repository.rankingPredios(anio, mes);
        long maximoAtenciones = filas.stream()
                .mapToLong(f -> numero(f.get("atenciones")))
                .max().orElse(0);

        List<RankingPredio> ranking = new ArrayList<>(filas.size());
        for (Map<String, Object> fila : filas) {
            long atenciones = numero(fila.get("atenciones"));
            long dias = numero(fila.get("dias"));

            ranking.add(new RankingPredio(
                    ((Number) fila.get("predio_id")).longValue(),
                    (String) fila.get("predio"),
                    atenciones,
                    numero(fila.get("incapacidades")),
                    dias,
                    numero(fila.get("accidentes")),
                    numero(fila.get("examenes")),
                    decimal(fila.get("costo")),
                    nivelDeRiesgo(atenciones, maximoAtenciones, dias, critico, alto, medio)));
        }
        return ranking;
    }

    @Transactional(readOnly = true)
    public List<Distribucion> topCausas(int anio, Integer mes, Long predioId, int limite) {
        return distribucion(repository.distribucionPorCausa(anio, mes, predioId, limite));
    }

    @Transactional(readOnly = true)
    public List<Distribucion> diasPorTipoIncapacidad(int anio, Integer mes, Long predioId) {
        return distribucion(repository.distribucionPorTipoIncapacidad(anio, mes, predioId));
    }

    @Transactional(readOnly = true)
    public List<Distribucion> causasDeAccidente(int anio, Integer mes, Long predioId) {
        return distribucion(repository.distribucionPorCausaAccidente(anio, mes, predioId));
    }

    /**
     * Nivel de riesgo de un predio.
     *
     * <p>Combina su peso relativo en atenciones con los días de incapacidad
     * acumulados; los cortes provienen de los umbrales administrables.
     */
    private String nivelDeRiesgo(long atenciones, long maximo, long dias,
                                 int critico, int alto, int medio) {
        double proporcion = maximo == 0 ? 0 : atenciones * 100.0 / maximo;
        if (proporcion >= 70 || dias >= critico) {
            return "CRITICO";
        }
        if (proporcion >= 40 || dias >= alto) {
            return "ALTO";
        }
        if (proporcion >= 12 || dias >= medio) {
            return "MEDIO";
        }
        return "BAJO";
    }

    /** Variación porcentual frente al mes anterior; nula en vistas acumuladas. */
    private BigDecimal variacionMensual(String indicador, int anio, Integer mes,
                                        Long predioId, long valorActual) {
        if (mes == null || mes == 1) {
            return null;
        }
        long anterior = switch (indicador) {
            case "atenciones" -> repository.contarAtenciones(anio, mes - 1, predioId);
            case "personas" -> repository.contarPersonasIncapacitadas(anio, mes - 1, predioId);
            case "dias" -> repository.sumarDiasIncapacidad(anio, mes - 1, predioId);
            default -> 0;
        };
        if (anterior == 0) {
            return null;
        }
        return BigDecimal.valueOf((valorActual - anterior) * 100.0 / anterior)
                .setScale(1, RoundingMode.HALF_UP);
    }

    private List<Distribucion> distribucion(List<Map<String, Object>> filas) {
        long total = filas.stream().mapToLong(f -> numero(f.get("total"))).sum();
        return filas.stream()
                .map(f -> {
                    long valor = numero(f.get("total"));
                    BigDecimal porcentaje = total == 0 ? BigDecimal.ZERO
                            : BigDecimal.valueOf(valor * 100.0 / total).setScale(1, RoundingMode.HALF_UP);
                    return new Distribucion((String) f.get("categoria"), valor, porcentaje);
                })
                .toList();
    }

    private Kpi kpiCon(String clave, String etiqueta, long valor, BigDecimal variacion, String detalle) {
        return new Kpi(clave, etiqueta, valor, variacion, detalle);
    }

    private String etiquetaPeriodo(int anio, Integer mes) {
        return mes == null ? "Acumulado " + anio : MESES[mes - 1] + " " + anio;
    }

    private String porcentaje(long parte, long total) {
        if (total == 0) {
            return "0%";
        }
        return BigDecimal.valueOf(parte * 100.0 / total)
                .setScale(1, RoundingMode.HALF_UP) + "%";
    }

    private long numero(Object valor) {
        return valor == null ? 0 : ((Number) valor).longValue();
    }

    private BigDecimal decimal(Object valor) {
        if (valor == null) {
            return BigDecimal.ZERO;
        }
        return valor instanceof BigDecimal bd ? bd : BigDecimal.valueOf(((Number) valor).doubleValue());
    }
}
