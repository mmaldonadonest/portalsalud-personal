package com.onest.app.catalog.dashboard.service;

import com.onest.app.catalog.antidoping.dto.AntidopingReporteDto;
import com.onest.app.catalog.antidoping.service.AntidopingService;
import com.onest.app.catalog.dashboard.dto.ConteoSimpleDto;
import com.onest.app.catalog.dashboard.dto.DashboardAntidopingDto;
import com.onest.app.catalog.dashboard.dto.PuntoMensualDto;
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
 */
@Service
public class DashboardAntidopingService {

    private static final int ANIO_MINIMO = 2000;
    private static final int ANIO_MAXIMO = LocalDate.now().getYear() + 1;

    private final AntidopingService antidopingService;

    public DashboardAntidopingService(AntidopingService antidopingService) {
        this.antidopingService = antidopingService;
    }

    public DashboardAntidopingDto resumen(String fechaInicial, String fechaFinal) {
        List<AntidopingReporteDto> filas = antidopingService.reportePorFecha(fechaInicial, fechaFinal).stream()
                // Mismo bug de "fila fantasma" ya visto en Accidentes/Consulta: sin datos en
                // el rango, el WS regresa un objeto con todos los campos null.
                .filter(f -> f.nss() != null && !f.nss().isBlank())
                .toList();

        long totalPositivos = 0;
        Map<String, Long> porTipoPrueba = new LinkedHashMap<>();
        Map<String, Long> porSustancia = new LinkedHashMap<>();
        Map<String, Long> porResultado = new LinkedHashMap<>();
        Map<String, Long> porStatus = new LinkedHashMap<>();
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

            Optional<YearMonth> mes = mesDe(fila.fechaRegistro());
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

        return new DashboardAntidopingDto(
                fechaInicial, fechaFinal,
                filas.size(), totalPositivos,
                aConteo(porTipoPrueba), aConteo(porSustancia), aConteo(porResultado), aConteo(porStatus),
                tendencia);
    }

    private static void incrementar(Map<String, Long> mapa, String clave) {
        mapa.merge(clave, 1L, Long::sum);
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
