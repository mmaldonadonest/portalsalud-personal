package com.onest.app.catalog.dashboard.service;

import com.onest.app.catalog.dashboard.dto.ConteoSimpleDto;
import com.onest.app.catalog.dashboard.dto.DashboardMaternidadDto;
import com.onest.app.catalog.dashboard.dto.PuntoMensualDto;
import com.onest.app.catalog.dashboard.repository.BitacoraHistoricoRepository;
import com.onest.app.catalog.maternidad.client.MaternidadClient;
import com.onest.app.catalog.maternidad.dto.MaternidadReporteDto;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.springframework.stereotype.Service;

/**
 * Dashboard de seguimiento de maternidad. Se apoya en el seguimiento propio del portal
 * (SERV_MED_MATERNIDAD_SEGUIMIENTO), NO en incapacidades con ramo "maternidad": ese ramo no
 * existe en los datos (0 de 291 incapacidades del historico al 11-sep-2026).
 *
 * <p><b>Desde el 30-sep-2026 suma el historico de los Excel.</b> Con una diferencia de fondo
 * respecto de las otras familias: <b>un caso de maternidad no tiene fecha del evento</b>. Es un
 * expediente que se sigue durante meses, y sus fechas &mdash;FUM, parto probable, incapacidad,
 * lactancia&mdash; son todas de otra cosa. Del lado del Excel la fecha es <b>el mes en que se
 * reporta el caso</b>, que es como el propio archivo lo plantea: su primer bloque es
 * {@code MES REPORTADO} y su primera casilla es {@code ANTES 2025}.
 *
 * <p>Por eso {@code revisionesProximas} se queda <b>solo con lo del portal</b>: los Excel no traen
 * fecha de proxima revision. La traen en una hoja aparte, {@code REVISIONES}, que es una matriz de
 * persona por mes y todavia no se carga. Contarla como cero seria decir que no hay revisiones
 * proximas, cuando lo cierto es que ese dato aun no esta.
 */
@Service
public class DashboardMaternidadService {

    private static final int ANIO_MINIMO = 2000;
    private static final int ANIO_MAXIMO = LocalDate.now().getYear() + 2;
    private static final int DIAS_REVISION_PROXIMA = 30;

    /** Atributos del historico, como los nombra el cargador. */
    private static final String ATR_REINICIO = "REINICIO_REAL";
    private static final String ATR_PARTO = "FECHA_PROBABLE_PARTO";

    private final MaternidadClient client;
    private final DashboardPredioFiltro predioFiltro;
    private final BitacoraHistoricoRepository historico;

    public DashboardMaternidadService(MaternidadClient client, DashboardPredioFiltro predioFiltro,
                                      BitacoraHistoricoRepository historico) {
        this.client = client;
        this.predioFiltro = predioFiltro;
        this.historico = historico;
    }

    public DashboardMaternidadDto resumen(String fechaInicial, String fechaFinal, String predio, String cuenta) {
        List<MaternidadReporteDto> filas = client.reportePorFecha(fechaInicial, fechaFinal).stream()
                .filter(f -> predioFiltro.coincide(f.cuenta(), predio, cuenta))
                .toList();

        Map<String, Long> porEstatus = new LinkedHashMap<>();
        Map<String, Long> porPredio = new LinkedHashMap<>();
        Map<String, Long> porTrimestre = new LinkedHashMap<>();
        Map<String, Long> porMes = new TreeMap<>();
        Map<String, Long> partosMes = new TreeMap<>();
        Set<String> reincorporadas = new HashSet<>();
        long revisionesProximas = 0;
        long sumaSemanas = 0;
        long conSemanas = 0;
        LocalDate hoy = LocalDate.now();
        LocalDate limite = hoy.plusDays(DIAS_REVISION_PROXIMA);

        for (MaternidadReporteDto fila : filas) {
            porEstatus.merge(etiqueta(fila.estatus()), 1L, Long::sum);
            porPredio.merge(predioFiltro.predioDe(fila.cuenta()), 1L, Long::sum);

            int semanas = parseInt(fila.semanasGestacion());
            if (semanas > 0) {
                sumaSemanas += semanas;
                conSemanas++;
                porTrimestre.merge(trimestre(semanas), 1L, Long::sum);
            }
            // PERSONAS reincorporadas, no seguimientos: una persona lleva varios y todos
            // heredan la misma fecha de reincorporacion - contar filas duplicaria.
            if (fechaDe(fila.reincorporacion()).isPresent() && fila.nss() != null && !fila.nss().isBlank()) {
                reincorporadas.add(fila.nss().trim());
            }
            Optional<LocalDate> revision = fechaDe(fila.proximaRevision());
            if (revision.isPresent() && !revision.get().isBefore(hoy) && !revision.get().isAfter(limite)) {
                revisionesProximas++;
            }
            mesDe(fila.fechaRegistro()).ifPresent(m -> porMes.merge(m.toString(), 1L, Long::sum));
            mesDe(fila.fechaProbableParto()).ifPresent(m -> partosMes.merge(m.toString(), 1L, Long::sum));
        }

        long personas = filas.stream().map(MaternidadReporteDto::nss)
                .filter(n -> n != null && !n.isBlank()).distinct().count();

        // --- Historico cargado de los Excel del servicio medico -------------------------------
        long seguimientos = filas.size();
        long personasHistorico = 0;
        long reincorporadasHistorico = 0;
        if (historico.hayMaternidad()) {
            LocalDate hasta2 = FechaFiltro.aFecha(fechaFinal).orElse(null);
            LocalDate desde2 = FechaFiltro.aFecha(fechaInicial).orElse(null);

            var totales = historico.totalesMaternidad(desde2, hasta2, predio, cuenta, ATR_REINICIO);
            seguimientos += totales.seguimientos();
            personasHistorico = totales.personas();
            reincorporadasHistorico = totales.reincorporadas();

            // El promedio se recalcula sobre la union, no se promedian dos promedios: eso daria
            // el mismo peso a 3 seguimientos del portal que a 70 del historico.
            for (int s : historico.semanasGestacion(desde2, hasta2, predio, cuenta)) {
                sumaSemanas += s;
                conSemanas++;
                porTrimestre.merge(trimestre(s), 1L, Long::sum);
            }

            historico.maternidadPorColumna("ESTATUS", desde2, hasta2, predio, cuenta)
                    .forEach(x -> porEstatus.merge(etiqueta(x.clave()), x.cantidad(), Long::sum));
            historico.maternidadPorColumna("PREDIO", desde2, hasta2, predio, cuenta)
                    .forEach(x -> porPredio.merge(x.clave(), x.cantidad(), Long::sum));
            historico.maternidadTendencia(desde2, hasta2, predio, cuenta)
                    .forEach(p -> porMes.merge(p.mes(), p.cantidad(), Long::sum));
            historico.partosPorMes(desde2, hasta2, predio, cuenta, ATR_PARTO)
                    .forEach(p -> partosMes.merge(p.mes(), p.cantidad(), Long::sum));
        }

        return new DashboardMaternidadDto(
                fechaInicial, fechaFinal,
                seguimientos,
                // Las personas de los dos origenes se SUMAN sin deduplicar: el portal identifica
                // por NSS y el Excel por nombre, y no hay forma de cruzarlos. Es el mismo criterio
                // -y la misma limitacion conocida- que en el modulo de consultas.
                personas + personasHistorico,
                reincorporadas.size() + reincorporadasHistorico,
                revisionesProximas,
                conSemanas > 0 ? (double) sumaSemanas / conSemanas : 0,
                aConteo(porEstatus), aConteo(porPredio), aConteoOrdenClave(porTrimestre),
                aTendencia(porMes), aTendencia(partosMes));
    }

    private static String trimestre(int semanas) {
        if (semanas <= 13) {
            return "1er trimestre";
        }
        if (semanas <= 27) {
            return "2do trimestre";
        }
        return "3er trimestre";
    }

    private static List<PuntoMensualDto> aTendencia(Map<String, Long> porMes) {
        List<PuntoMensualDto> lista = new ArrayList<>();
        porMes.forEach((mes, n) -> lista.add(new PuntoMensualDto(mes, n)));
        return lista;
    }

    private static List<ConteoSimpleDto> aConteo(Map<String, Long> mapa) {
        return mapa.entrySet().stream()
                .map(e -> new ConteoSimpleDto(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(ConteoSimpleDto::cantidad).reversed())
                .toList();
    }

    private static List<ConteoSimpleDto> aConteoOrdenClave(Map<String, Long> mapa) {
        return mapa.entrySet().stream()
                .map(e -> new ConteoSimpleDto(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(ConteoSimpleDto::clave))
                .toList();
    }

    private static String etiqueta(String valor) {
        return (valor == null || valor.isBlank() || "0".equals(valor.trim())) ? "Sin dato" : valor.trim();
    }

    private static int parseInt(String valor) {
        try {
            return valor == null ? 0 : Integer.parseInt(valor.trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    /** El WS manda "0" cuando la fecha es null (coalesce); eso NO es una fecha. */
    private static Optional<LocalDate> fechaDe(String iso) {
        if (iso == null || iso.isBlank() || "0".equals(iso.trim()) || iso.length() < 10) {
            return Optional.empty();
        }
        try {
            LocalDate d = LocalDate.parse(iso.substring(0, 10));
            return d.getYear() >= ANIO_MINIMO && d.getYear() <= ANIO_MAXIMO ? Optional.of(d) : Optional.empty();
        } catch (DateTimeParseException ex) {
            return Optional.empty();
        }
    }

    private static Optional<YearMonth> mesDe(String iso) {
        return fechaDe(iso).map(YearMonth::from);
    }
}
