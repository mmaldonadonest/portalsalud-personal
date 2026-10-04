package com.onest.app.catalog.dashboard.service;

import com.onest.app.catalog.dashboard.dto.ConteoDictamenDto;
import com.onest.app.catalog.dashboard.dto.ConteoSimpleDto;
import com.onest.app.catalog.dashboard.dto.DashboardExamenDto;
import com.onest.app.catalog.dashboard.dto.PuntoMensualDto;
import com.onest.app.catalog.dashboard.repository.BitacoraHistoricoRepository;
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
 *
 * <p><b>Desde el 30-sep-2026 suma tambien el historico de los Excel.</b> Eso cambia la premisa de
 * arriba: la pantalla ya no arranca vacia. El WS sigue trayendo lo que se captura en el portal
 * desde agosto de 2026, y {@link BitacoraHistoricoRepository} trae los 8,616 examenes de 2026 que
 * el servicio medico venia llevando en Excel. Los dos origenes se pintan en la misma barra
 * apilada, con la equivalencia de dictamenes en {@link DictamenExamen}.
 *
 * <p>Una diferencia entre los dos vale tenerla presente: el WS permite que <b>un examen traiga
 * varios dictamenes marcados</b> &mdash;por eso la nota al pie de la pantalla advierte que la suma
 * de los cuatro puede superar el total&mdash;, mientras que en el Excel el bloque es "marca con 1"
 * y el cargador se queda con el primero, dejando aviso. Del lado del historico, entonces, la suma
 * de los cuatro dictamenes si cuadra con el total.
 */
@Service
public class DashboardExamenService {

    private static final int ANIO_MINIMO = 2000;
    private static final int ANIO_MAXIMO = LocalDate.now().getYear() + 1;

    private final ExamenService examenService;
    private final DashboardPredioFiltro predioFiltro;
    private final BitacoraHistoricoRepository historico;

    public DashboardExamenService(ExamenService examenService, DashboardPredioFiltro predioFiltro,
                                  BitacoraHistoricoRepository historico) {
        this.examenService = examenService;
        this.predioFiltro = predioFiltro;
        this.historico = historico;
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

        // --- Historico cargado de los Excel del servicio medico -------------------------------
        // Los 8,616 examenes de 2026 de las tres familias: ingreso, periodico y pos incapacidad.
        // Se suman a lo del WS en los mismos acumuladores, para que la barra apilada no tenga que
        // saber de donde vino cada renglon.
        long totalHistorico = 0;
        if (historico.hayExamenes()) {
            LocalDate desde = FechaFiltro.aFecha(fechaInicial).orElse(null);
            LocalDate hasta = FechaFiltro.aFecha(fechaFinal).orElse(null);

            totalHistorico = historico.totalExamenes(desde, hasta, predio, cuenta);

            for (ConteoSimpleDto d : historico.examenesPorDictamen(desde, hasta, predio, cuenta)) {
                Optional<DictamenExamen> dic = DictamenExamen.deEtiquetaExcel(d.clave());
                if (dic.isEmpty()) {
                    // Etiqueta sin codigo reconocible: cuenta en el total pero no en ningun
                    // dictamen. Es preferible que la suma de los cuatro quede corta y se note,
                    // a meterla al bucket equivocado.
                    continue;
                }
                switch (dic.get()) {
                    case APTO -> apto += d.cantidad();
                    case NO_APTO -> noApto += d.cantidad();
                    case CONDICIONADO -> aptoCondicionado += d.cantidad();
                    case RESTRINGIDO -> aptoRestringido += d.cantidad();
                }
            }

            sumarCruce(porPredio, historico.examenesPorPredioYDictamen(desde, hasta, predio, cuenta));

            // El cruce por mes alimenta dos cosas -la barra apilada y la serie simple- y se
            // consulta UNA vez. Pedirlo dos veces eran dos viajes a la base por cada carga de
            // pantalla para traer exactamente lo mismo.
            var porMesHistorico = historico.examenesPorMesYDictamen(desde, hasta, predio, cuenta);
            sumarCruce(porMesDictamen, porMesHistorico);
            porMesHistorico.forEach(x -> porMes.merge(x.clave(), x.cantidad(), Long::sum));
        }

        List<PuntoMensualDto> tendencia = new ArrayList<>();
        porMes.forEach((mes, cantidad) -> tendencia.add(new PuntoMensualDto(mes, cantidad)));
        if (sinFecha > 0) {
            tendencia.add(new PuntoMensualDto("Sin fecha", sinFecha));
        }

        return new DashboardExamenDto(
                fechaInicial, fechaFinal,
                filas.size() + (int) totalHistorico,
                apto, noApto, aptoCondicionado, aptoRestringido,
                aConteo(porPredio, true),
                tendencia,
                aConteo(porMesDictamen, false));
    }

    /**
     * Mete los cruces del historico en los mismos acumuladores que llena el WS.
     *
     * <p>Cada renglon del cruce es (clave, dictamen, cantidad), asi que se suma {@code cantidad}
     * veces al bucket que toca. La etiqueta que no se reconoce suma al total del grupo pero a
     * ningun dictamen, igual que en los KPIs: el hueco se ve en la barra y eso es lo que se busca.
     */
    private static void sumarCruce(Map<String, Acumulador> destino,
                                   List<BitacoraHistoricoRepository.Cruce> cruces) {
        for (BitacoraHistoricoRepository.Cruce x : cruces) {
            Acumulador a = destino.computeIfAbsent(x.clave(), k -> new Acumulador());
            Optional<DictamenExamen> dic = DictamenExamen.deEtiquetaExcel(x.valor());
            a.sumarVarios(x.cantidad(), dic.orElse(null));
        }
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

        /**
         * Suma varios examenes que comparten el mismo dictamen, que es como llega el historico:
         * ya agrupado por la base.
         *
         * @param dictamen {@code null} cuando la etiqueta del Excel no se reconocio. En ese caso
         *                 suma al total del grupo y a ningun dictamen, a proposito.
         */
        void sumarVarios(long cuantos, DictamenExamen dictamen) {
            cantidad += cuantos;
            if (dictamen == null) {
                return;
            }
            switch (dictamen) {
                case APTO -> apto += cuantos;
                case NO_APTO -> noApto += cuantos;
                case CONDICIONADO -> cond += cuantos;
                case RESTRINGIDO -> restr += cuantos;
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
