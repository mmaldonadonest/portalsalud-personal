package com.onest.app.catalog.dashboard.service;

import com.onest.app.catalog.accidente.dto.AccidenteReporteDto;
import com.onest.app.catalog.accidente.service.AccidenteService;
import com.onest.app.catalog.dashboard.dto.ConteoCostoDto;
import com.onest.app.catalog.dashboard.dto.DashboardAccidentesDto;
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
 * Dashboard ligero de KPIs de Accidentes de trabajo. Reutiliza
 * AccidenteService.reportePorFecha() (Servcio/consulta_accidentes_fecha, aplicado y
 * verificado 2026-08-17) - mismo criterio que DashboardIncapacidadesService.
 */
@Service
public class DashboardAccidentesService {

    private static final int ANIO_MINIMO = 2000;
    private static final int ANIO_MAXIMO = LocalDate.now().getYear() + 1;

    private final AccidenteService accidenteService;

    public DashboardAccidentesService(AccidenteService accidenteService) {
        this.accidenteService = accidenteService;
    }

    public DashboardAccidentesDto resumen(String fechaInicial, String fechaFinal) {
        List<AccidenteReporteDto> filas = accidenteService.reportePorFecha(fechaInicial, fechaFinal).stream()
                // El WS devuelve una fila "fantasma" (todos los campos null) cuando no hay
                // datos en el rango, en vez de un array vacio - se descarta aqui.
                .filter(f -> f.nss() != null && !f.nss().isBlank())
                .toList();

        double totalCosto = 0;
        Map<String, Acumulador> porTipoRiesgo = new LinkedHashMap<>();
        Map<String, Acumulador> porCausaRt = new LinkedHashMap<>();
        Map<String, Acumulador> porStatus = new LinkedHashMap<>();
        Map<String, Long> porMes = new TreeMap<>();
        long sinFecha = 0;

        for (AccidenteReporteDto fila : filas) {
            double costo = parseDouble(fila.costo());
            totalCosto += costo;

            acumular(porTipoRiesgo, etiqueta(fila.tipoRiesgo()), costo);
            acumular(porCausaRt, etiqueta(fila.causaRt()), costo);
            acumular(porStatus, etiqueta(fila.statusCalificacion()), costo);

            Optional<YearMonth> mes = mesDe(fila.fechaAccidente());
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

        return new DashboardAccidentesDto(
                fechaInicial, fechaFinal,
                filas.size(), totalCosto,
                aConteo(porTipoRiesgo), aConteo(porCausaRt), aConteo(porStatus),
                tendencia);
    }

    private static final class Acumulador {
        long cantidad;
        double costo;

        void sumar(double costo) {
            this.cantidad++;
            this.costo += costo;
        }
    }

    private static void acumular(Map<String, Acumulador> mapa, String clave, double costo) {
        mapa.computeIfAbsent(clave, k -> new Acumulador()).sumar(costo);
    }

    private static List<ConteoCostoDto> aConteo(Map<String, Acumulador> mapa) {
        return mapa.entrySet().stream()
                .map(e -> new ConteoCostoDto(e.getKey(), e.getValue().cantidad, e.getValue().costo))
                .sorted(Comparator.comparingLong(ConteoCostoDto::cantidad).reversed())
                .toList();
    }

    private static String etiqueta(String valor) {
        return (valor == null || valor.isBlank()) ? "Sin dato" : valor.trim();
    }

    private static double parseDouble(String valor) {
        if (valor == null || valor.isBlank()) {
            return 0;
        }
        try {
            return Double.parseDouble(valor.trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
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
