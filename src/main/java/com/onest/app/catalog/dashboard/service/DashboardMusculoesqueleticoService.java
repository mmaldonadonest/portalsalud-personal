package com.onest.app.catalog.dashboard.service;

import com.onest.app.catalog.dashboard.dto.ConteoSimpleDto;
import com.onest.app.catalog.dashboard.dto.DashboardMusculoesqueleticoDto;
import com.onest.app.catalog.dashboard.dto.PuntoMensualDto;
import com.onest.app.catalog.dashboard.service.LesionMusculoesqueletica.Clasificacion;
import com.onest.app.catalog.dashboard.service.LesionMusculoesqueletica.Grupo;
import com.onest.app.catalog.expediente.dto.ConsultaReporteDto;
import com.onest.app.catalog.expediente.service.ExpedienteService;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import org.springframework.stereotype.Service;

/**
 * Modulo Musculoesqueleticas del dashboard analitico. Reutiliza el mismo reporte de
 * consultas que DashboardConsultaService (Servcio/consulta_medica_fecha, que desde el
 * 11-sep-2026 devuelve DIAGNOSTICO - docs/ords-diagnostico-en-consultas.sql) y clasifica
 * cada fila con LesionMusculoesqueletica. Cero WS nuevo.
 *
 * <p>Hoy (sep-2026) ninguna consulta historica trae clave CIE-10: el diagnostico se captura
 * en el portal PHP productivo y sera obligatorio al lanzar este portal. El modulo arranca
 * vacio a proposito y se llena solo conforme se capture; por eso expone
 * {@code sinDiagnostico}, para que el vacio se explique en pantalla y no parezca falla.
 */
@Service
public class DashboardMusculoesqueleticoService {

    private static final int MAX_DIAGNOSTICOS = 10;
    private static final int ANIO_MINIMO = 2000;
    private static final int ANIO_MAXIMO = LocalDate.now().getYear() + 1;

    private final ExpedienteService expedienteService;
    private final DashboardPredioFiltro predioFiltro;

    public DashboardMusculoesqueleticoService(ExpedienteService expedienteService, DashboardPredioFiltro predioFiltro) {
        this.expedienteService = expedienteService;
        this.predioFiltro = predioFiltro;
    }

    public DashboardMusculoesqueleticoDto resumen(String fechaInicial, String fechaFinal, String predio, String cuenta) {
        List<ConsultaReporteDto> filas = expedienteService.reportePorFecha(fechaInicial, fechaFinal).stream()
                // fila fantasma del WS cuando no hay datos en el rango
                .filter(f -> f.nss() != null && !f.nss().isBlank())
                .filter(f -> predioFiltro.coincide(f.cuenta(), predio, cuenta))
                .toList();

        long sinDiagnostico = 0;
        long lesiones = 0;
        long algias = 0, columna = 0, traumatismos = 0, fracturas = 0;
        Set<String> personas = new HashSet<>();
        Map<String, Long> porTipo = new LinkedHashMap<>();
        Map<String, Long> porRegion = new LinkedHashMap<>();
        Map<String, Long> porPredio = new LinkedHashMap<>();
        Map<String, Long> porMes = new TreeMap<>();
        Map<String, Long> porDiagnostico = new LinkedHashMap<>();
        long sinFecha = 0;

        for (ConsultaReporteDto fila : filas) {
            if (!tieneClave(fila.diagnostico())) {
                sinDiagnostico++;
                continue;
            }
            Optional<Clasificacion> c = LesionMusculoesqueletica.clasificar(fila.diagnostico(), fila.areaInvolucrada());
            if (c.isEmpty()) {
                continue; // con clave CIE-10 pero de otro capitulo: no es lesion musculoesqueletica
            }
            lesiones++;
            personas.add(fila.nss().trim());
            Grupo g = c.get().grupo();
            if (g == Grupo.ALGIA) {
                algias++;
            } else if (g == Grupo.COLUMNA) {
                columna++;
            } else if (g == Grupo.TRAUMATISMO) {
                traumatismos++;
            } else {
                fracturas++;
            }
            porTipo.merge(c.get().tipo(), 1L, Long::sum);
            porRegion.merge(c.get().region(), 1L, Long::sum);
            porPredio.merge(predioFiltro.predioDe(fila.cuenta()), 1L, Long::sum);
            porDiagnostico.merge(claveCorta(fila.diagnostico()), 1L, Long::sum);
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

        return new DashboardMusculoesqueleticoDto(fechaInicial, fechaFinal,
                filas.size(), sinDiagnostico, lesiones, personas.size(),
                algias, columna, traumatismos, fracturas,
                aConteo(porTipo, Integer.MAX_VALUE), aConteo(porRegion, Integer.MAX_VALUE),
                aConteo(porPredio, Integer.MAX_VALUE), tendencia,
                aConteo(porDiagnostico, MAX_DIAGNOSTICOS));
    }

    private static boolean tieneClave(String diagnostico) {
        return diagnostico != null && diagnostico.trim().matches("^[A-Za-z]\\d{2}.*");
    }

    /** "M54.5 - Lumbago" -> "M54 - Lumbago": agrupa por categoria de 3 caracteres, conserva el texto capturado. */
    private static String claveCorta(String diagnostico) {
        String d = diagnostico.trim();
        String clave = d.substring(0, 3).toUpperCase();
        int sep = d.indexOf('-');
        String texto = sep > 0 ? d.substring(sep + 1).trim() : "";
        return texto.isEmpty() ? clave : clave + " - " + texto;
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
        top.add(new ConteoSimpleDto("Otros", resto));
        return top;
    }

    /** Mismo criterio que DashboardConsultaService.mesDe(). */
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
