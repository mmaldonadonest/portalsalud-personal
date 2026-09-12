package com.onest.app.catalog.dashboard.service;

import com.onest.app.catalog.dashboard.dto.ConteoSimpleDto;
import com.onest.app.catalog.dashboard.dto.DashboardMaternidadDto;
import com.onest.app.catalog.dashboard.dto.PuntoMensualDto;
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
 */
@Service
public class DashboardMaternidadService {

    private static final int ANIO_MINIMO = 2000;
    private static final int ANIO_MAXIMO = LocalDate.now().getYear() + 2;
    private static final int DIAS_REVISION_PROXIMA = 30;

    private final MaternidadClient client;
    private final DashboardPredioFiltro predioFiltro;

    public DashboardMaternidadService(MaternidadClient client, DashboardPredioFiltro predioFiltro) {
        this.client = client;
        this.predioFiltro = predioFiltro;
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

        return new DashboardMaternidadDto(
                fechaInicial, fechaFinal,
                filas.size(), personas, reincorporadas.size(), revisionesProximas,
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
