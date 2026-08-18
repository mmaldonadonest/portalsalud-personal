package com.onest.app.catalog.dashboard.service;

import com.onest.app.catalog.dashboard.dto.ConteoDto;
import com.onest.app.catalog.dashboard.dto.DashboardIncapacidadesDto;
import com.onest.app.catalog.dashboard.dto.PuntoMensualDto;
import com.onest.app.catalog.incapacidad.dto.IncapacidadReporteDto;
import com.onest.app.catalog.incapacidad.service.IncapacidadService;
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
 * Dashboard ligero de KPIs de Incapacidades. Reutiliza IncapacidadService.reportePorFecha()
 * (Servcio/consulta_incapacidades_fecha), el unico WS que ya trae "todas las NSS en un rango" -
 * sin WS ni vistas SQL nuevas, ver docs/plan-tareas-concretas.html. Consulta/Examen quedan
 * fuera de alcance de este dashboard: esos WS exigen NSS individual, no existe equivalente
 * agregado (confirmado 2026-08-17, iterar NSS por NSS no es viable a escala).
 */
@Service
public class DashboardIncapacidadesService {

    private final IncapacidadService incapacidadService;

    public DashboardIncapacidadesService(IncapacidadService incapacidadService) {
        this.incapacidadService = incapacidadService;
    }

    public DashboardIncapacidadesDto resumen(String fechaInicial, String fechaFinal) {
        List<IncapacidadReporteDto> filas = incapacidadService.reportePorFecha(fechaInicial, fechaFinal);

        long totalDias = 0;
        double totalCosto = 0;
        Map<String, Acumulador> porRamo = new LinkedHashMap<>();
        Map<String, Acumulador> porRubro = new LinkedHashMap<>();
        Map<String, Acumulador> porEstado = new LinkedHashMap<>();
        Map<String, Long> porMes = new TreeMap<>();
        long sinFecha = 0;

        for (IncapacidadReporteDto fila : filas) {
            long dias = parseLong(fila.diasAutorizados());
            double costo = parseDouble(fila.costo());
            totalDias += dias;
            totalCosto += costo;

            acumular(porRamo, etiqueta(fila.ramo()), dias, costo);
            acumular(porRubro, etiqueta(fila.rubro()), dias, costo);
            acumular(porEstado, etiqueta(fila.estadoDictamen()), dias, costo);

            Optional<YearMonth> mes = mesDe(fila.fechaInicio());
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

        return new DashboardIncapacidadesDto(
                fechaInicial, fechaFinal,
                filas.size(), totalDias, totalCosto,
                aConteo(porRamo), aConteo(porRubro), aConteo(porEstado),
                tendencia);
    }

    private static final class Acumulador {
        long cantidad;
        long dias;
        double costo;

        void sumar(long dias, double costo) {
            this.cantidad++;
            this.dias += dias;
            this.costo += costo;
        }
    }

    private static void acumular(Map<String, Acumulador> mapa, String clave, long dias, double costo) {
        mapa.computeIfAbsent(clave, k -> new Acumulador()).sumar(dias, costo);
    }

    private static List<ConteoDto> aConteo(Map<String, Acumulador> mapa) {
        return mapa.entrySet().stream()
                .map(e -> new ConteoDto(e.getKey(), e.getValue().cantidad, e.getValue().dias, e.getValue().costo))
                .sorted(Comparator.comparingLong(ConteoDto::cantidad).reversed())
                .toList();
    }

    private static String etiqueta(String valor) {
        return (valor == null || valor.isBlank()) ? "Sin dato" : valor.trim();
    }

    /**
     * Parseo defensivo: el sistema real ya demostro traer valores no numericos en campos
     * numericos (datos de prueba/sucios, ver memoria de Incapacidad/Rubro) - nunca lanzar,
     * solo contar como 0 si no se puede interpretar.
     */
    private static long parseLong(String valor) {
        if (valor == null || valor.isBlank()) {
            return 0;
        }
        try {
            return Long.parseLong(valor.trim());
        } catch (NumberFormatException ex) {
            try {
                return Math.round(Double.parseDouble(valor.trim()));
            } catch (NumberFormatException ex2) {
                return 0;
            }
        }
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

    // Rango de anios plausible para una fecha de incapacidad real - fuera de esto se trata como
    // dato sucio (confirmado en produccion: existe al menos un caso "2923-09", typo de "2023-09"
    // que de otra forma dejaria un punto absurdo en la tendencia mensual).
    private static final int ANIO_MINIMO = 2000;
    private static final int ANIO_MAXIMO = LocalDate.now().getYear() + 1;

    /**
     * Formato real de fecha_inicio en la respuesta del WS NO confirmado (distinto del formato
     * dd/MM/yy que se manda en el REQUEST, ver BiowsIncapacidadReporteRequest) - se probaron
     * formatos ISO (con o sin hora) y dd/MM/yyyy o dd/MM/yy por si acaso. Cualquier formato no
     * reconocido, o con un anio fuera de rango plausible, cae en el bucket "Sin fecha" en vez
     * de tronar o ensuciar la tendencia.
     */
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
