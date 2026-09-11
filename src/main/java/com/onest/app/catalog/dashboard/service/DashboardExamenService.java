package com.onest.app.catalog.dashboard.service;

import com.onest.app.catalog.dashboard.dto.ConteoDictamenDto;
import com.onest.app.catalog.dashboard.dto.DashboardExamenDto;
import com.onest.app.catalog.dashboard.dto.PuntoMensualDto;
import com.onest.app.catalog.examen.dto.ExamenReporteDto;
import com.onest.app.catalog.examen.service.ExamenService;
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
 * Dashboard ligero de KPIs de Exámenes Médicos. Reutiliza
 * ExamenService.reportePorFecha() (Servcio/consulta_examen_fecha, aplicado y
 * verificado 2026-08-17 sobre SERV_MED_RESULTADO_EXAMEN_HIST) - SIN datos
 * retroactivos, el historial arranco vacio ese dia.
 */
@Service
public class DashboardExamenService {

    private static final int ANIO_MINIMO = 2000;
    private static final int ANIO_MAXIMO = LocalDate.now().getYear() + 1;

    private final ExamenService examenService;
    private final DashboardPredioFiltro predioFiltro;

    public DashboardExamenService(ExamenService examenService, DashboardPredioFiltro predioFiltro) {
        this.examenService = examenService;
        this.predioFiltro = predioFiltro;
    }

    /** Sin corte por predio/cuenta - el que usa /home. */
    public DashboardExamenDto resumen(String fechaInicial, String fechaFinal) {
        return resumen(fechaInicial, fechaFinal, null, null);
    }

    /**
     * Con corte opcional por predio y/o cuenta. Posible desde el 10-sep-2026: el WS
     * _cta devuelve CUENTA por registro (docs/ords-cuenta-en-reportes.sql). Filtro
     * vacio = sin corte, identico al comportamiento anterior.
     */
    public DashboardExamenDto resumen(String fechaInicial, String fechaFinal, String predio, String cuenta) {
        List<ExamenReporteDto> filas = examenService.reportePorFecha(fechaInicial, fechaFinal).stream()
                // Mismo bug de "fila fantasma" ya visto en los otros 3 dashboards: sin datos
                // en el rango, el WS regresa un objeto con todos los campos null.
                .filter(f -> f.nss() != null && !f.nss().isBlank())
                .filter(f -> predioFiltro.coincide(f.cuenta(), predio, cuenta))
                .toList();

        long apto = 0;
        long noApto = 0;
        long aptoCondicionado = 0;
        long aptoRestringido = 0;
        Map<String, Acumulador> porPredio = new LinkedHashMap<>();
        Map<String, Acumulador> porMesDictamen = new TreeMap<>();
        TreeMap<String, Long> porMes = new TreeMap<>();
        long sinFecha = 0;

        for (ExamenReporteDto fila : filas) {
            boolean esApto = marcado(fila.apto());
            boolean esNoApto = marcado(fila.noApto());
            boolean esCond = marcado(fila.aptoCondicionado());
            boolean esRestr = marcado(fila.aptoRestringido());
            if (esApto) {
                apto++;
            }
            if (esNoApto) {
                noApto++;
            }
            if (esCond) {
                aptoCondicionado++;
            }
            if (esRestr) {
                aptoRestringido++;
            }

            // Predio resuelto desde la cuenta que ahora trae el WS _cta (sin llamadas extra:
            // PredioService cachea el mapeo 2 minutos). Con desglose por dictamen: el modulo
            // Examenes pinta "resultado por predio" como barra apilada.
            porPredio.computeIfAbsent(predioFiltro.predioDe(fila.cuenta()), k -> new Acumulador())
                    .sumar(esApto, esNoApto, esCond, esRestr);

            Optional<YearMonth> mes = mesDe(fila.fechaRegistro());
            if (mes.isPresent()) {
                porMes.merge(mes.get().toString(), 1L, Long::sum);
                porMesDictamen.computeIfAbsent(mes.get().toString(), k -> new Acumulador())
                        .sumar(esApto, esNoApto, esCond, esRestr);
            } else {
                sinFecha++;
            }
        }

        List<PuntoMensualDto> tendencia = new ArrayList<>();
        porMes.forEach((mes, cantidad) -> tendencia.add(new PuntoMensualDto(mes, cantidad)));
        if (sinFecha > 0) {
            tendencia.add(new PuntoMensualDto("Sin fecha", sinFecha));
        }

        return new DashboardExamenDto(
                fechaInicial, fechaFinal,
                filas.size(), apto, noApto, aptoCondicionado, aptoRestringido,
                aConteo(porPredio, true),
                tendencia,
                aConteo(porMesDictamen, false));
    }

    /** Acumula un examen por cada dictamen marcado. Un examen puede traer mas de uno. */
    private static final class Acumulador {
        long cantidad;
        long apto;
        long noApto;
        long cond;
        long restr;

        void sumar(boolean esApto, boolean esNoApto, boolean esCond, boolean esRestr) {
            cantidad++;
            if (esApto) {
                apto++;
            }
            if (esNoApto) {
                noApto++;
            }
            if (esCond) {
                cond++;
            }
            if (esRestr) {
                restr++;
            }
        }
    }

    /** ordenarPorCantidad=true para predios (ranking); false para meses (ya vienen en orden cronologico del TreeMap). */
    private static List<ConteoDictamenDto> aConteo(Map<String, Acumulador> mapa, boolean ordenarPorCantidad) {
        var stream = mapa.entrySet().stream()
                .map(e -> new ConteoDictamenDto(e.getKey(), e.getValue().cantidad, e.getValue().apto,
                        e.getValue().noApto, e.getValue().cond, e.getValue().restr));
        if (ordenarPorCantidad) {
            stream = stream.sorted(Comparator.comparingLong(ConteoDictamenDto::cantidad).reversed());
        }
        return stream.toList();
    }

    /**
     * El WS usa "0" como placeholder de "vacio" (via coalesce), no cadena vacia -
     * confirmado en vivo 2026-08-17. Una columna cuenta como "marcada" solo si no
     * es null/blank NI "0".
     */
    private static boolean marcado(String valor) {
        return valor != null && !valor.isBlank() && !"0".equals(valor.trim());
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
