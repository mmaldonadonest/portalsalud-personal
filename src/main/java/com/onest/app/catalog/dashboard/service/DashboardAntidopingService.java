package com.onest.app.catalog.dashboard.service;

import com.onest.app.catalog.antidoping.dto.AntidopingReporteDto;
import com.onest.app.catalog.antidoping.service.AntidopingService;
import com.onest.app.catalog.dashboard.dto.ConteoCruzadoDto;
import com.onest.app.catalog.dashboard.dto.ConteoSimpleDto;
import com.onest.app.catalog.dashboard.dto.DashboardAntidopingDto;
import com.onest.app.catalog.dashboard.dto.PuntoMensualDto;
import com.onest.app.catalog.dashboard.repository.BitacoraHistoricoRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.springframework.stereotype.Service;

/**
 * Dashboard ligero de KPIs de Antidoping/Alcoholimetria. Reutiliza
 * AntidopingService.reportePorFecha() (Servcio/consulta_antidoping_fecha, aplicado y
 * verificado 2026-08-17) - mismo criterio que los otros 3 dashboards.
 *
 * <p><b>Desde el 30-sep-2026 suma el historico de los Excel</b>, primera familia de la fase 2:
 * 826 pruebas de 2026 en 16 hojas. El DTO no cambio ni una linea, y eso dice algo: cuando se
 * diseno contra el WS de ORDS ya pedia {@code porResultado}, {@code porSustancia},
 * {@code porTipoPrueba} y {@code porStatusConclusion}, que es exactamente como el cargador separa
 * el bloque {@code RESULTADO} del Excel. Los dos origenes describen el mismo proceso.
 */
@Service
public class DashboardAntidopingService {

    private static final int ANIO_MINIMO = 2000;
    private static final int ANIO_MAXIMO = LocalDate.now().getYear() + 1;

    /** El valor con el que el Excel marca una prueba positiva. */
    private static final String POSITIVO = "POSITIVO";

    private final AntidopingService antidopingService;
    private final DashboardPredioFiltro predioFiltro;
    private final BitacoraHistoricoRepository historico;

    public DashboardAntidopingService(AntidopingService antidopingService,
                                      DashboardPredioFiltro predioFiltro,
                                      BitacoraHistoricoRepository historico) {
        this.antidopingService = antidopingService;
        this.predioFiltro = predioFiltro;
        this.historico = historico;
    }

    /** Sin corte por predio/cuenta - el que usa /home. */
    public DashboardAntidopingDto resumen(String fechaInicial, String fechaFinal) {
        return resumen(fechaInicial, fechaFinal, null, null);
    }

    /** Con corte opcional por predio y/o cuenta. Posible desde el 11-sep-2026 (WS _cta). */
    public DashboardAntidopingDto resumen(String fechaInicial, String fechaFinal, String predio, String cuenta) {
        List<AntidopingReporteDto> filas = antidopingService.reportePorFecha(fechaInicial, fechaFinal).stream()
                // Mismo bug de "fila fantasma" ya visto en Accidentes/Consulta: sin datos en
                // el rango, el WS regresa un objeto con todos los campos null.
                .filter(f -> f.nss() != null && !f.nss().isBlank())
                .filter(f -> predioFiltro.coincide(f.cuenta(), predio, cuenta))
                .toList();

        long totalPositivos = 0;
        Map<String, Long> porTipoPrueba = new LinkedHashMap<>();
        Map<String, Long> porSustancia = new LinkedHashMap<>();
        Map<String, Long> porResultado = new LinkedHashMap<>();
        Map<String, Long> porStatus = new LinkedHashMap<>();
        Map<String, Long> porPredio = new LinkedHashMap<>();
        Map<String, Map<String, Long>> predioTipo = new LinkedHashMap<>();
        Map<String, Long> porMes = new TreeMap<>();
        long sinFecha = 0;

        for (AntidopingReporteDto fila : filas) {
            if (fila.resultado() != null && fila.resultado().equalsIgnoreCase("POSITIVO")) {
                totalPositivos++;
            }
            incrementar(porTipoPrueba, etiqueta(fila.tipoPrueba()));
            incrementar(porSustancia, etiqueta(fila.sustancia()));
            incrementar(porResultado, etiqueta(fila.resultado()));
            incrementar(porStatus, etiqueta(fila.statusConclusion()));
            // Predio resuelto desde la cuenta que trae el WS _cta (PredioService cachea 2 min).
            String predioFila = predioFiltro.predioDe(fila.cuenta());
            incrementar(porPredio, predioFila);
            predioTipo.computeIfAbsent(predioFila, k -> new LinkedHashMap<>())
                    .merge(etiqueta(fila.tipoPrueba()), 1L, Long::sum);

            Optional<YearMonth> mes = mesDe(fila.fechaRegistro());
            if (mes.isPresent()) {
                porMes.merge(mes.get().toString(), 1L, Long::sum);
            } else {
                sinFecha++;
            }
        }

        // --- Historico cargado de los Excel del servicio medico -------------------------------
        long totalHistorico = 0;
        if (historico.hayAntidoping()) {
            LocalDate desde = FechaFiltro.aFecha(fechaInicial).orElse(null);
            LocalDate hasta = FechaFiltro.aFecha(fechaFinal).orElse(null);

            totalHistorico = historico.totalAntidoping(desde, hasta, predio, cuenta);

            sumar(porTipoPrueba, historico.antidopingPorAtributo("TIPO_PRUEBA", desde, hasta, predio, cuenta));
            sumar(porSustancia, historico.antidopingPorAtributo("SUSTANCIA", desde, hasta, predio, cuenta));
            sumar(porStatus, historico.antidopingPorAtributo("CONCLUSION", desde, hasta, predio, cuenta));
            sumar(porPredio, historico.antidopingPorPredio(desde, hasta, predio, cuenta));

            // El veredicto alimenta dos cosas -la grafica por resultado y el KPI de positivos-
            // y se consulta una vez.
            var resultados = historico.antidopingPorAtributo("RESULTADO", desde, hasta, predio, cuenta);
            sumar(porResultado, resultados);
            for (ConteoSimpleDto r : resultados) {
                if (POSITIVO.equalsIgnoreCase(r.clave())) {
                    totalPositivos += r.cantidad();
                }
            }

            historico.antidopingPorPredioYTipo(desde, hasta, predio, cuenta).forEach(x ->
                    predioTipo.computeIfAbsent(x.clave(), k -> new LinkedHashMap<>())
                            .merge(x.valor(), x.cantidad(), Long::sum));
            historico.antidopingTendencia(desde, hasta, predio, cuenta)
                    .forEach(p -> porMes.merge(p.mes(), p.cantidad(), Long::sum));
        }

        List<PuntoMensualDto> tendencia = new ArrayList<>();
        porMes.forEach((mes, cantidad) -> tendencia.add(new PuntoMensualDto(mes, cantidad)));
        if (sinFecha > 0) {
            tendencia.add(new PuntoMensualDto("Sin fecha", sinFecha));
        }

        return new DashboardAntidopingDto(
                fechaInicial, fechaFinal,
                filas.size() + (int) totalHistorico, totalPositivos,
                aConteo(porTipoPrueba), aConteo(porSustancia), aConteo(porResultado), aConteo(porStatus),
                aConteo(porPredio),
                predioTipo.entrySet().stream()
                        .flatMap(p -> p.getValue().entrySet().stream()
                                .map(t -> new ConteoCruzadoDto(p.getKey(), t.getKey(), t.getValue(), 0)))
                        .toList(),
                tendencia);
    }

    private static void incrementar(Map<String, Long> mapa, String clave) {
        mapa.merge(clave, 1L, Long::sum);
    }

    /**
     * Suma los conteos del historico al acumulador que ya trae lo del WS.
     *
     * <p>Se suma por clave y no se concatenan listas: cuando el mismo valor existe en los dos
     * origenes &mdash;{@code NEGATIVO} lo hay en ORDS y en el Excel&mdash; tiene que salir una
     * barra con el total, no dos barras iguales.
     */
    private static void sumar(Map<String, Long> destino, List<ConteoSimpleDto> conteos) {
        conteos.forEach(c -> destino.merge(etiqueta(c.clave()), c.cantidad(), Long::sum));
    }

    private static List<ConteoSimpleDto> aConteo(Map<String, Long> mapa) {
        return mapa.entrySet().stream()
                .map(e -> new ConteoSimpleDto(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(ConteoSimpleDto::cantidad).reversed())
                .toList();
    }

    private static String etiqueta(String valor) {
        return (valor == null || valor.isBlank()) ? "Sin dato" : valor.trim();
    }

    /** Mismo criterio que DashboardIncapacidadesService.mesDe() - ver ese comentario. */
    private static Optional<YearMonth> mesDe(String fecha) {
        if (fecha == null || fecha.isBlank()) {
            return Optional.empty();
        }
        String valor = fecha.trim();
        try {
            YearMonth mes = null;
            if (valor.matches("^\\d{4}-\\d{2}-\\d{2}.*")) {
                mes = YearMonth.parse(valor.substring(0, 7));
            } else if (valor.matches("^\\d{1,2}/\\d{1,2}/\\d{4}$")) {
                mes = YearMonth.from(LocalDate.parse(valor, DateTimeFormatter.ofPattern("d/M/yyyy")));
            } else if (valor.matches("^\\d{1,2}/\\d{1,2}/\\d{2}$")) {
                mes = YearMonth.from(LocalDate.parse(valor, DateTimeFormatter.ofPattern("d/M/yy")));
            }
            if (mes != null && mes.getYear() >= ANIO_MINIMO && mes.getYear() <= ANIO_MAXIMO) {
                return Optional.of(mes);
            }
        } catch (DateTimeParseException ignored) {
            // formato inesperado - cae al bucket "Sin fecha"
        }
        return Optional.empty();
    }
}
