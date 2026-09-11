package com.onest.app.catalog.dashboard.service;

import com.onest.app.catalog.dashboard.dto.ConteoSimpleDto;
import com.onest.app.catalog.dashboard.dto.DashboardConsultaDto;
import com.onest.app.catalog.dashboard.dto.PuntoMensualDto;
import com.onest.app.catalog.dashboard.dto.SeriePredioDto;
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

    // Mismo criterio que MAX_CAUSAS: CUENTA puede tener muchos valores distintos
    // (18 en la prueba real) - top N + "Otras" para que la grafica sea legible.
    // salud-ocupacional-v2 hace lo mismo (ATTENTIONS_BY_ACCOUNT: top 4 + "Otras cuentas").
    private static final int MAX_CUENTAS = 8;

    // Predios que se superponen en la grafica comparada del modulo Atenciones. Mas de 6
    // lineas en un mismo eje deja de leerse (mismo criterio que el prototipo).
    private static final int MAX_PREDIOS_SERIE = 6;

    private static final int ANIO_MINIMO = 2000;
    private static final int ANIO_MAXIMO = LocalDate.now().getYear() + 1;

    private final ExpedienteService expedienteService;
    private final DashboardPredioFiltro predioFiltro;

    public DashboardConsultaService(ExpedienteService expedienteService, DashboardPredioFiltro predioFiltro) {
        this.expedienteService = expedienteService;
        this.predioFiltro = predioFiltro;
    }

    /** Sin corte por predio/cuenta - el que ya usa /home desde el 17-ago. */
    public DashboardConsultaDto resumen(String fechaInicial, String fechaFinal) {
        return resumen(fechaInicial, fechaFinal, null, null);
    }

    /**
     * Con corte opcional por predio y/o cuenta (filtro del Dashboard Ejecutivo). Desde el
     * 10-sep-2026 los 4 dashboards aceptan el mismo corte: los WS {@code _cta} ya devuelven
     * CUENTA por registro tambien en Incapacidades/Accidentes/Examenes.
     *
     * <p>El predio se compara contra el nombre YA resuelto (incluido el bucket "Sin asignar",
     * que es seleccionable a proposito: deja ver cuanto falta por mapear en /admin/predios).
     */
    public DashboardConsultaDto resumen(String fechaInicial, String fechaFinal, String predio, String cuenta) {
        return resumen(fechaInicial, fechaFinal, predio, cuenta, MAX_CAUSAS);
    }

    /**
     * {@code topCausas} controla cuantas causas se devuelven antes de agrupar el resto en
     * "Otras". El default (15) mantiene ligero el payload de /home y del Dashboard Ejecutivo,
     * que solo pintan un top-N; el modulo Causas pide todas, porque su razon de ser es el
     * ranking completo y un bucket "Otras" ahi seria justo lo que se quiere abrir.
     */
    public DashboardConsultaDto resumen(String fechaInicial, String fechaFinal, String predio, String cuenta,
                                        int topCausas) {
        List<ConsultaReporteDto> filas = expedienteService.reportePorFecha(fechaInicial, fechaFinal).stream()
                // Mismo bug de "fila fantasma" ya visto en Accidentes: sin datos en el rango,
                // el WS regresa un objeto con todos los campos null en vez de array vacio.
                .filter(f -> f.nss() != null && !f.nss().isBlank())
                .filter(f -> predioFiltro.coincide(f.cuenta(), predio, cuenta))
                .toList();

        long totalAccidentesEmergencias = 0;
        Map<String, Long> porTipoConsulta = new LinkedHashMap<>();
        Map<String, Long> porAreaAccidente = new LinkedHashMap<>();
        Map<String, Long> porCausa = new LinkedHashMap<>();
        Map<String, Long> porGenero = new LinkedHashMap<>();
        Map<String, Long> porEdad = new LinkedHashMap<>();
        Map<String, Long> porCuenta = new LinkedHashMap<>();
        Map<String, Long> porPredio = new LinkedHashMap<>();
        Map<String, Long> porMes = new TreeMap<>();
        Map<String, Map<String, Long>> porPredioMes = new LinkedHashMap<>();
        long sinFecha = 0;

        for (ConsultaReporteDto fila : filas) {
            if (fila.esAccidente()) {
                totalAccidentesEmergencias++;
            }
            incrementar(porTipoConsulta, etiqueta(fila.tipoConsulta()));
            incrementar(porAreaAccidente, etiqueta(fila.areaAccidente()));
            incrementar(porCausa, etiqueta(fila.causa()));
            incrementar(porGenero, etiqueta(fila.genero()));
            incrementar(porEdad, rangoEdad(fila.edad()));
            incrementar(porCuenta, etiqueta(fila.cuenta()));
            // Predio "fino" (17 sitios) via el mapeo cuenta->predio administrable
            // (docs/ords-predio-cuenta.sql) - CERO llamadas WS extra, la cuenta ya viene en
            // cada fila. Cuenta sin mapear todavia (o "Sin dato") cae en "Sin asignar", nunca
            // se descarta - el analista ve cuanto le falta por mapear, no un dato inventado.
            String predioFila = predioFiltro.predioDe(fila.cuenta());
            incrementar(porPredio, predioFila);

            Optional<YearMonth> mes = mesDe(fila.fechaConsulta());
            if (mes.isPresent()) {
                porMes.merge(mes.get().toString(), 1L, Long::sum);
                porPredioMes.computeIfAbsent(predioFila, k -> new TreeMap<>())
                        .merge(mes.get().toString(), 1L, Long::sum);
            } else {
                sinFecha++;
            }
        }

        List<PuntoMensualDto> tendencia = new ArrayList<>();
        porMes.forEach((mes, cantidad) -> tendencia.add(new PuntoMensualDto(mes, cantidad)));
        if (sinFecha > 0) {
            tendencia.add(new PuntoMensualDto("Sin fecha", sinFecha));
        }

        // Personas != atenciones: la misma persona puede consultar varias veces en el rango.
        long totalPersonas = filas.stream()
                .map(ConsultaReporteDto::nss)
                .filter(nss -> nss != null && !nss.isBlank())
                .distinct()
                .count();

        // Solo los predios con mas atenciones, y "Sin asignar" fuera: no es un predio real y
        // hoy concentraria casi todo, aplastando la escala de los demas.
        List<SeriePredioDto> tendenciaPorPredio = porPredioMes.entrySet().stream()
                .filter(e -> !DashboardPredioFiltro.SIN_ASIGNAR.equals(e.getKey()))
                .sorted(Comparator.comparingLong(
                        (Map.Entry<String, Map<String, Long>> e) -> e.getValue().values().stream()
                                .mapToLong(Long::longValue).sum()).reversed())
                .limit(MAX_PREDIOS_SERIE)
                .map(e -> new SeriePredioDto(e.getKey(), e.getValue().entrySet().stream()
                        .map(m -> new PuntoMensualDto(m.getKey(), m.getValue()))
                        .toList()))
                .toList();

        return new DashboardConsultaDto(
                fechaInicial, fechaFinal,
                filas.size(), totalAccidentesEmergencias,
                aConteo(porTipoConsulta, Integer.MAX_VALUE),
                aConteo(porAreaAccidente, Integer.MAX_VALUE),
                aConteo(porCausa, topCausas > 0 ? topCausas : MAX_CAUSAS),
                aConteo(porGenero, Integer.MAX_VALUE),
                aConteoPorRangoEdad(porEdad),
                aConteo(porCuenta, MAX_CUENTAS),
                aConteo(porPredio, Integer.MAX_VALUE),
                totalPersonas, tendenciaPorPredio,
                tendencia);
    }

    /**
     * Mismos rangos que salud-ocupacional-v2 (dashboard/Morbilidad.jsx, fuente hoja
     * ACUMULADO "RANGO DE EDAD"). Edad calculada contra SYSDATE en el WS (edad actual,
     * no edad al momento de la consulta), ver docs/ords-consulta-dashboard-genero-edad-cuenta.sql.
     */
    private static String rangoEdad(int edad) {
        if (edad <= 0) {
            return "Sin dato";
        }
        if (edad <= 25) {
            return "18-25";
        }
        if (edad <= 35) {
            return "26-35";
        }
        if (edad <= 45) {
            return "36-45";
        }
        if (edad <= 55) {
            return "46-55";
        }
        return "55+";
    }

    private static final List<String> ORDEN_RANGOS_EDAD = List.of("18-25", "26-35", "36-45", "46-55", "55+", "Sin dato");

    /** A diferencia de aConteo() (ordenado por cantidad), los rangos de edad se muestran en orden cronologico. */
    private static List<ConteoSimpleDto> aConteoPorRangoEdad(Map<String, Long> porEdad) {
        List<ConteoSimpleDto> resultado = new ArrayList<>();
        for (String rango : ORDEN_RANGOS_EDAD) {
            Long cantidad = porEdad.get(rango);
            if (cantidad != null) {
                resultado.add(new ConteoSimpleDto(rango, cantidad));
            }
        }
        return resultado;
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
