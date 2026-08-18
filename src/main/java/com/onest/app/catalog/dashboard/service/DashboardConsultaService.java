package com.onest.app.catalog.dashboard.service;

import com.onest.app.catalog.dashboard.dto.ConteoSimpleDto;
import com.onest.app.catalog.dashboard.dto.DashboardConsultaDto;
import com.onest.app.catalog.dashboard.dto.PuntoMensualDto;
import com.onest.app.catalog.expediente.dto.ConsultaReporteDto;
import com.onest.app.catalog.expediente.service.ExpedienteService;
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
 * Dashboard ligero de KPIs de Consulta Medica (Morbilidad). Reutiliza
 * ExpedienteService.reportePorFecha() (Servcio/consulta_medica_fecha, aplicado y
 * verificado 2026-08-17) - mismo criterio que DashboardIncapacidadesService/
 * DashboardAccidentesService.
 */
@Service
public class DashboardConsultaService {

    // CAUSA es texto clinico libre (ver hallazgo "Para validar con Product Owner") -
    // puede tener decenas/cientos de valores distintos. Se limita a los N mas
    // frecuentes para no devolver un payload gigante; el resto se agrupa en "Otras".
    private static final int MAX_CAUSAS = 15;

    private static final int ANIO_MINIMO = 2000;
    private static final int ANIO_MAXIMO = LocalDate.now().getYear() + 1;

    private final ExpedienteService expedienteService;

    public DashboardConsultaService(ExpedienteService expedienteService) {
        this.expedienteService = expedienteService;
    }

    public DashboardConsultaDto resumen(String fechaInicial, String fechaFinal) {
        List<ConsultaReporteDto> filas = expedienteService.reportePorFecha(fechaInicial, fechaFinal).stream()
                // Mismo bug de "fila fantasma" ya visto en Accidentes: sin datos en el rango,
                // el WS regresa un objeto con todos los campos null en vez de array vacio.
                .filter(f -> f.nss() != null && !f.nss().isBlank())
                .toList();

        long totalAccidentesEmergencias = 0;
        Map<String, Long> porTipoConsulta = new LinkedHashMap<>();
        Map<String, Long> porAreaAccidente = new LinkedHashMap<>();
        Map<String, Long> porCausa = new LinkedHashMap<>();
        Map<String, Long> porMes = new TreeMap<>();
        long sinFecha = 0;

        for (ConsultaReporteDto fila : filas) {
            if (fila.esAccidente()) {
                totalAccidentesEmergencias++;
            }
            incrementar(porTipoConsulta, etiqueta(fila.tipoConsulta()));
            incrementar(porAreaAccidente, etiqueta(fila.areaAccidente()));
            incrementar(porCausa, etiqueta(fila.causa()));

            Optional<YearMonth> mes = mesDe(fila.fechaConsulta());
            if (mes.isPresent()) {
                porMes.merge(mes.get().toString(), 1L, Long::sum);
            } else {
                sinFecha++;
            }
        }

        List<PuntoMensualDto> tendencia = new ArrayList<>();
        porMes.forEach((mes, cantidad) -> tendencia.add(new PuntoMensualDto(mes, cantidad)));
        if (sinFecha > 0) {
            tendencia.add(new PuntoMensualDto("Sin fecha", sinFecha));
        }

        return new DashboardConsultaDto(
                fechaInicial, fechaFinal,
                filas.size(), totalAccidentesEmergencias,
                aConteo(porTipoConsulta, Integer.MAX_VALUE),
                aConteo(porAreaAccidente, Integer.MAX_VALUE),
                aConteo(porCausa, MAX_CAUSAS),
                tendencia);
    }

    private static void incrementar(Map<String, Long> mapa, String clave) {
        mapa.merge(clave, 1L, Long::sum);
    }

    private static List<ConteoSimpleDto> aConteo(Map<String, Long> mapa, int limite) {
        List<ConteoSimpleDto> ordenado = mapa.entrySet().stream()
                .map(e -> new ConteoSimpleDto(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(ConteoSimpleDto::cantidad).reversed())
                .toList();
        if (ordenado.size() <= limite) {
            return ordenado;
        }
        List<ConteoSimpleDto> top = new ArrayList<>(ordenado.subList(0, limite));
        long resto = ordenado.subList(limite, ordenado.size()).stream().mapToLong(ConteoSimpleDto::cantidad).sum();
        top.add(new ConteoSimpleDto("Otras", resto));
        return top;
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
