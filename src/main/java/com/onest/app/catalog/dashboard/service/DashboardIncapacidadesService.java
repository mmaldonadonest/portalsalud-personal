package com.onest.app.catalog.dashboard.service;

import com.onest.app.catalog.dashboard.dto.ConteoDto;
import com.onest.app.catalog.dashboard.dto.ConteoSimpleDto;
import com.onest.app.catalog.dashboard.dto.SerieMensualDto;
import com.onest.app.catalog.dashboard.dto.DashboardIncapacidadesDto;
import com.onest.app.catalog.dashboard.dto.PuntoMensualDiasDto;
import com.onest.app.catalog.dashboard.dto.PuntoMensualDto;
import com.onest.app.catalog.incapacidad.dto.IncapacidadReporteDto;
import com.onest.app.catalog.incapacidad.service.IncapacidadService;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import com.onest.app.catalog.dashboard.repository.BitacoraHistoricoRepository;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
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
    private final DashboardPredioFiltro predioFiltro;
    private final BitacoraHistoricoRepository historico;

    public DashboardIncapacidadesService(IncapacidadService incapacidadService,
                                         DashboardPredioFiltro predioFiltro,
                                         BitacoraHistoricoRepository historico) {
        this.incapacidadService = incapacidadService;
        this.predioFiltro = predioFiltro;
        this.historico = historico;
    }


    /** Sin corte por predio/cuenta - el que usa /home. */
    public DashboardIncapacidadesDto resumen(String fechaInicial, String fechaFinal) {
        return resumen(fechaInicial, fechaFinal, null, null);
    }

    /**
     * Con corte opcional por predio y/o cuenta. Posible desde el 10-sep-2026: el WS
     * _cta devuelve CUENTA por registro (docs/ords-cuenta-en-reportes.sql). Filtro
     * vacio = sin corte, identico al comportamiento anterior.
     */
    public DashboardIncapacidadesDto resumen(String fechaInicial, String fechaFinal, String predio, String cuenta) {
        List<IncapacidadReporteDto> filas = incapacidadService.reportePorFecha(fechaInicial, fechaFinal).stream()
                .filter(f -> predioFiltro.coincide(f.cuenta(), predio, cuenta))
                .toList();

        long totalDias = 0;
        double totalCosto = 0;
        Map<String, Acumulador> porRamo = new LinkedHashMap<>();
        Map<String, Acumulador> porRubro = new LinkedHashMap<>();
        Map<String, Acumulador> porEstado = new LinkedHashMap<>();
        Map<String, Acumulador> porPredio = new LinkedHashMap<>();
        Map<String, Long> porMes = new TreeMap<>();
        Map<String, Long> porMesDias = new TreeMap<>();
        // ramo -> mes -> dias; rubro -> NSS distintos; rubro -> mes -> NSS distintos
        Map<String, Map<String, Long>> diasRamoMes = new LinkedHashMap<>();
        Map<String, Set<String>> nssPorRubro = new LinkedHashMap<>();
        Map<String, Map<String, Set<String>>> nssRubroMes = new LinkedHashMap<>();
        long sinFecha = 0;

        for (IncapacidadReporteDto fila : filas) {
            long dias = parseLong(fila.diasAutorizados());
            double costo = parseDouble(fila.costo());
            totalDias += dias;
            totalCosto += costo;

            acumular(porRamo, etiqueta(fila.ramo()), dias, costo);
            acumular(porRubro, etiqueta(fila.rubro()), dias, costo);
            acumular(porEstado, etiqueta(fila.estadoDictamen()), dias, costo);
            // Predio resuelto desde la cuenta que ahora trae el WS _cta. Sin llamadas
            // extra: PredioService cachea el mapeo 2 minutos.
            acumular(porPredio, predioFiltro.predioDe(fila.cuenta()), dias, costo);

            String rubro = etiqueta(fila.rubro());
            String nss = fila.nss() == null ? "" : fila.nss().trim();
            if (!nss.isEmpty()) {
                nssPorRubro.computeIfAbsent(rubro, k -> new HashSet<>()).add(nss);
            }

            // Criterio de negocio (11-sep-2026): manda la FECHA DE INICIO del certificado; si
            // no hay, la de registro. Es exactamente el coalesce que aplica el WS _cta al
            // filtrar el periodo (docs/ords-incapacidades-criterio-fecha-inicio.sql), asi las
            // tarjetas y la tendencia mensual cuentan las mismas filas en los mismos meses.
            Optional<YearMonth> mes = mesDe(fila.fechaInicio()).or(() -> mesDe(fila.fechaConsulta()));
            if (mes.isPresent()) {
                String clave = mes.get().toString();
                porMes.merge(clave, 1L, Long::sum);
                porMesDias.merge(clave, dias, Long::sum);
                diasRamoMes.computeIfAbsent(etiqueta(fila.ramo()), k -> new TreeMap<>()).merge(clave, dias, Long::sum);
                if (!nss.isEmpty()) {
                    nssRubroMes.computeIfAbsent(rubro, k -> new TreeMap<>())
                            .computeIfAbsent(clave, k -> new HashSet<>()).add(nss);
                }
            } else {
                sinFecha++;
            }
        }

        // Personas != registros: una misma persona puede tener varias incapacidades en el rango.
        // La tarjeta del dashboard dice "Personas incapacitadas" y venia mostrando filas.size().
        long totalPersonas = filas.stream()
                .map(IncapacidadReporteDto::nss)
                .filter(nss -> nss != null && !nss.isBlank())
                .distinct()
                .count();

        // --- Historico cargado de los Excel del servicio medico -------------------------------
        // La familia con mas volumen del proyecto: ~1,929 episodios de 2026, de las hojas del IMSS
        // y de las internas que paga la empresa. Servicio medico confirmo el 29-sep-2026 que las
        // internas SI cuentan y su costo SI entra.
        long episodiosHistorico = 0;
        long personasHistorico = 0;
        if (historico.hayIncapacidades()) {
            LocalDate desde = FechaFiltro.aFecha(fechaInicial).orElse(null);
            LocalDate hasta = FechaFiltro.aFecha(fechaFinal).orElse(null);

            var totales = historico.totalesIncapacidad(desde, hasta, predio, cuenta);
            episodiosHistorico = totales.episodios();
            personasHistorico = totales.personas();
            totalDias += (long) historico.diasIncapacidades(desde, hasta, predio, cuenta);
            totalCosto += historico.costoIncapacidades(desde, hasta, predio, cuenta);

            acumularHistorico(porRamo, historico.incapacidadPorRamo(desde, hasta, predio, cuenta));
            acumularHistorico(porRubro,
                    historico.incapacidadPorDimension("RUBRO", desde, hasta, predio, cuenta));
            acumularHistorico(porPredio,
                    historico.incapacidadPorDimension("PREDIO", desde, hasta, predio, cuenta));

            var mensualHistorico = historico.incapacidadTendencia(desde, hasta, predio, cuenta);
            mensualHistorico.forEach(p -> porMes.merge(p.mes(), p.cantidad(), Long::sum));

            // Los episodios sin mes quedan fuera de la tendencia pero si cuentan en el total.
            sinFecha += Math.max(0, episodiosHistorico
                    - mensualHistorico.stream().mapToLong(PuntoMensualDto::cantidad).sum());

            historico.incapacidadDiasPorMes(desde, hasta, predio, cuenta)
                    .forEach(p -> porMesDias.merge(p.mes(), p.cantidad(), Long::sum));
            historico.incapacidadDiasPorMesYRamo(desde, hasta, predio, cuenta).forEach(x ->
                    diasRamoMes.computeIfAbsent(etiqueta(x.valor()), k -> new TreeMap<>())
                            .merge(x.clave(), x.cantidad(), Long::sum));

            // porEstado NO se alimenta del historico a proposito: el Excel no trae estado de
            // dictamen. Lo mas parecido es TIPO DE INCAP (I - S - MAT), que es otra cosa -si la
            // incapacidad es inicial, subsecuente o de maternidad- y meterla ahi haria que la
            // grafica de dictamenes mezclara dos preguntas distintas. Se queda con lo del portal.
        }

        // Las dos series se arman DESPUES del bloque del historico, no antes. Ver la nota larga
        // en DashboardConsultaService: son copias de porMes y porMesDias, asi que armarlas arriba
        // dejaba fuera todo lo del Excel y las graficas salian vacias junto a KPIs con datos.
        List<PuntoMensualDto> tendencia = new ArrayList<>();
        porMes.forEach((mes, cantidad) -> tendencia.add(new PuntoMensualDto(mes, cantidad)));
        if (sinFecha > 0) {
            tendencia.add(new PuntoMensualDto("Sin fecha", sinFecha));
        }

        // Serie separada de dias (no conteo de casos) para el chart "Evolucion mensual de
        // indicadores" del Dashboard Ejecutivo - mismo dato ya calculado arriba por
        // ramo/rubro/estado, solo re-agrupado por mes.
        List<PuntoMensualDiasDto> tendenciaDias = new ArrayList<>();
        porMesDias.forEach((mes, dias) -> tendenciaDias.add(new PuntoMensualDiasDto(mes, dias)));

        return new DashboardIncapacidadesDto(
                fechaInicial, fechaFinal,
                filas.size() + episodiosHistorico,
                // Se suman sin deduplicar: el portal identifica por NSS y el Excel por nombre, y
                // no hay forma de cruzarlos. Misma limitacion conocida que en consultas.
                totalPersonas + personasHistorico,
                totalDias, totalCosto,
                aConteo(porRamo), aConteo(porRubro), aConteo(porEstado), aConteo(porPredio),
                tendencia, tendenciaDias,
                aSeries(diasRamoMes),
                nssPorRubro.entrySet().stream()
                        .map(e -> new ConteoSimpleDto(e.getKey(), e.getValue().size()))
                        .sorted(Comparator.comparingLong(ConteoSimpleDto::cantidad).reversed())
                        .toList(),
                nssRubroMes.entrySet().stream()
                        .map(e -> new SerieMensualDto(e.getKey(), e.getValue().entrySet().stream()
                                .map(m -> new PuntoMensualDto(m.getKey(), m.getValue().size()))
                                .toList()))
                        .toList());
    }

    /** Series por clave ordenadas por total descendente (la mas pesada primero en la pila). */
    private static List<SerieMensualDto> aSeries(Map<String, Map<String, Long>> porClaveMes) {
        return porClaveMes.entrySet().stream()
                .sorted(Comparator.comparingLong(
                        (Map.Entry<String, Map<String, Long>> e) -> e.getValue().values().stream()
                                .mapToLong(Long::longValue).sum()).reversed())
                .map(e -> new SerieMensualDto(e.getKey(), e.getValue().entrySet().stream()
                        .map(m -> new PuntoMensualDto(m.getKey(), m.getValue()))
                        .toList()))
                .toList();
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

        /** Para el historico, que llega ya agrupado por la base. */
        void sumarVarios(long episodios, long dias, double costo) {
            this.cantidad += episodios;
            this.dias += dias;
            this.costo += costo;
        }
    }

    private static void acumular(Map<String, Acumulador> mapa, String clave, long dias, double costo) {
        mapa.computeIfAbsent(clave, k -> new Acumulador()).sumar(dias, costo);
    }

    /**
     * Mete los grupos del historico en los mismos acumuladores que llena el WS.
     *
     * <p>El historico llega <b>ya agrupado</b> por la base, con episodios, dias y costo por clave,
     * asi que se suma de golpe en lugar de renglon por renglon. Se suma por clave y no se
     * concatenan listas: {@code EG} existe en los dos origenes y tiene que salir <b>una</b> barra
     * con el total, no dos barras.
     */
    private static void acumularHistorico(
            Map<String, Acumulador> mapa,
            List<BitacoraHistoricoRepository.AgrupadoIncapacidad> grupos) {
        for (var g : grupos) {
            mapa.computeIfAbsent(etiqueta(g.clave()), k -> new Acumulador())
                    .sumarVarios(g.episodios(), g.dias(), g.costo());
        }
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
     * Formatos reales de fecha_inicio en el WS (verificado 11-sep-2026 sobre las 291 filas del
     * historico): 'yyyy-MM-dd' (263), 'dd/MM/yyyy HH:mm:ss' (11, carga vieja) y '0' por null (17).
     * fecha_consulta (= fecha_registro) viene ISO con hora. Cualquier formato no reconocido, o
     * con un anio fuera de rango plausible, cae en el bucket "Sin fecha" en vez de tronar o
     * ensuciar la tendencia.
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
            } else if (valor.matches("^\\d{1,2}/\\d{1,2}/\\d{4}( .*)?$")) {
                // con o sin hora ("20/06/2023 10:23:32"): solo importa el dia
                String dia = valor.contains(" ") ? valor.substring(0, valor.indexOf(' ')) : valor;
                mes = YearMonth.from(LocalDate.parse(dia, DateTimeFormatter.ofPattern("d/M/yyyy")));
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
