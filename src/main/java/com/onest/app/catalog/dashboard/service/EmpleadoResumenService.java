package com.onest.app.catalog.dashboard.service;

import com.onest.app.catalog.accidente.service.AccidenteService;
import com.onest.app.catalog.antidoping.service.AntidopingService;
import com.onest.app.catalog.dashboard.dto.EmpleadoResumenDto;
import com.onest.app.catalog.dashboard.dto.EmpleadoResumenDto.Contadores;
import com.onest.app.catalog.dashboard.dto.EmpleadoResumenDto.Evento;
import com.onest.app.catalog.dashboard.dto.EmpleadoResumenDto.Ficha;
import com.onest.app.catalog.examen.service.ExamenService;
import com.onest.app.catalog.expediente.service.ExpedienteService;
import com.onest.app.catalog.incapacidad.service.IncapacidadService;
import com.onest.app.catalog.maternidad.service.MaternidadService;
import com.onest.app.catalog.nss.dto.EmpleadoDto;
import com.onest.app.catalog.nss.service.NssSearchService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Expediente unificado de una persona (modulo Empleados del dashboard analitico): cruza los
 * servicios por NSS que ya existen y arma un timeline unico. Cada dominio que falle se omite
 * del timeline en vez de tumbar la ficha completa - el analista prefiere ver 5 de 6 fuentes
 * que un error.
 */
@Service
public class EmpleadoResumenService {

    private static final String RANGO_EXAMEN_DESDE = "2020-01-01";

    private final NssSearchService nssSearchService;
    private final ExpedienteService expedienteService;
    private final IncapacidadService incapacidadService;
    private final AccidenteService accidenteService;
    private final AntidopingService antidopingService;
    private final MaternidadService maternidadService;
    private final ExamenService examenService;
    private final DashboardPredioFiltro predioFiltro;

    public EmpleadoResumenService(NssSearchService nssSearchService, ExpedienteService expedienteService,
                                  IncapacidadService incapacidadService, AccidenteService accidenteService,
                                  AntidopingService antidopingService, MaternidadService maternidadService,
                                  ExamenService examenService, DashboardPredioFiltro predioFiltro) {
        this.nssSearchService = nssSearchService;
        this.expedienteService = expedienteService;
        this.incapacidadService = incapacidadService;
        this.accidenteService = accidenteService;
        this.antidopingService = antidopingService;
        this.maternidadService = maternidadService;
        this.examenService = examenService;
        this.predioFiltro = predioFiltro;
    }

    public Optional<EmpleadoResumenDto> resumen(String nss) {
        if (nss == null || nss.isBlank()) {
            return Optional.empty();
        }
        String nssLimpio = nss.trim();
        var busqueda = nssSearchService.findByNss(nssLimpio);
        if (busqueda.isEmpty() || busqueda.get().empleado() == null) {
            return Optional.empty();
        }
        EmpleadoDto emp = busqueda.get().empleado();

        List<Evento> eventos = new ArrayList<>();
        long atenciones = 0, incapacidades = 0, dias = 0, accidentes = 0, examenes = 0, antidoping = 0, maternidad = 0;

        try {
            // El WS mete un objeto "no hay datos" DENTRO de Datos cuando la persona no tiene
            // consultas: llega como ConsultaDto sin id y con "sin datos" en los textos.
            var lista = expedienteService.consultasByNss(nssLimpio).stream()
                    .filter(c -> !limpio(c.idConsulta()).isEmpty())
                    .toList();
            atenciones = lista.size();
            lista.forEach(c -> eventos.add(new Evento(iso(c.fechaConsulta()), "atencion",
                    "Atención médica", junta(c.tipoConsulta(), c.causa()))));
        } catch (RuntimeException ex) {
            // dominio caido: se omite del timeline
        }
        try {
            var lista = incapacidadService.byNss(nssLimpio);
            incapacidades = lista.size();
            for (var i : lista) {
                long d = parseLong(i.diasAutorizados());
                dias += d;
                eventos.add(new Evento(iso(i.fechaInicio() != null && !"0".equals(i.fechaInicio()) ? i.fechaInicio() : i.fechaConsulta()),
                        "incapacidad", "Incapacidad" + (d > 0 ? " de " + d + " días" : ""), junta(i.ramo(), i.tipoIncapacidad())));
            }
        } catch (RuntimeException ex) {
            // idem
        }
        try {
            var lista = accidenteService.byNss(nssLimpio);
            accidentes = lista.size();
            lista.forEach(a -> eventos.add(new Evento(iso(a.fechaAccidente()), "accidente",
                    "Accidente " + limpio(a.tipoRiesgo()).toLowerCase(), junta(a.causaRt(), a.estado()))));
        } catch (RuntimeException ex) {
            // idem
        }
        try {
            var lista = antidopingService.byNss(nssLimpio);
            antidoping = lista.size();
            lista.forEach(p -> eventos.add(new Evento(iso(p.fechaRegistro()), "antidoping",
                    "Prueba " + limpio(p.tipoPrueba()).toLowerCase(), limpio(p.resultado()))));
        } catch (RuntimeException ex) {
            // idem
        }
        try {
            var lista = maternidadService.byNss(nssLimpio);
            maternidad = lista.size();
            lista.forEach(m -> eventos.add(new Evento(iso(m.fechaRegistro()), "maternidad",
                    "Seguimiento de maternidad", junta(limpio(m.estatus()),
                            esFecha(m.fechaProbableParto()) ? "parto probable " + m.fechaProbableParto() : ""))));
        } catch (RuntimeException ex) {
            // idem
        }
        try {
            // No hay historial de examenes por NSS: se filtra el reporte por fecha. El
            // historial arranco vacio el 17-ago-2026, asi que el rango amplio es barato.
            String hoy = LocalDate.now().toString();
            var lista = examenService.reportePorFecha(RANGO_EXAMEN_DESDE, hoy).stream()
                    .filter(e -> nssLimpio.equals(e.nss() == null ? "" : e.nss().trim()))
                    .toList();
            examenes = lista.size();
            lista.forEach(e -> eventos.add(new Evento(iso(e.fechaRegistro()), "examen",
                    "Examen médico", dictamen(e.apto(), e.noApto(), e.aptoCondicionado(), e.aptoRestringido()))));
        } catch (RuntimeException ex) {
            // idem
        }

        eventos.sort(Comparator.comparing(Evento::fecha, Comparator.nullsLast(Comparator.reverseOrder())));

        Ficha ficha = new Ficha(
                emp.nss(),
                emp.completo() != null && !emp.completo().isBlank() ? emp.completo()
                        : junta(emp.nombre(), junta(emp.apellidoPaterno(), emp.apellidoMaterno())),
                limpio(emp.cuenta()),
                predioFiltro.predioDe(emp.cuenta()),
                limpio(emp.nombrePuesto()),
                limpio(emp.nombreEmpresa()),
                sexo(emp.sexo()),
                limpio(emp.fechaNacimiento()),
                emp.edad(),
                limpio(emp.estadoCivil()),
                limpio(emp.rfc()),
                limpio(emp.turno()),
                limpio(emp.celular()),
                limpio(emp.telFijo()),
                limpio(emp.direccion()));

        return Optional.of(new EmpleadoResumenDto(ficha,
                new Contadores(atenciones, incapacidades, dias, accidentes, examenes, antidoping, maternidad),
                eventos));
    }

    private static String dictamen(String apto, String noApto, String cond, String restr) {
        if (marcado(apto)) {
            return "Apto";
        }
        if (marcado(noApto)) {
            return "No apto";
        }
        if (marcado(cond)) {
            return "Apto condicionado";
        }
        if (marcado(restr)) {
            return "Apto restringido";
        }
        return "Sin dictamen";
    }

    private static boolean marcado(String v) {
        return v != null && !v.isBlank() && !"0".equals(v.trim());
    }

    private static String sexo(String v) {
        if (v == null) {
            return "";
        }
        return switch (v.trim()) {
            case "1" -> "Hombre";
            case "2" -> "Mujer";
            default -> v.trim();
        };
    }

    /** Vacio para null, "0" y los placeholders que los WS usan en lugar de null. */
    private static String limpio(String v) {
        if (v == null || v.isBlank()) {
            return "";
        }
        String t = v.trim();
        if ("0".equals(t) || t.equalsIgnoreCase("sin datos") || t.toLowerCase().contains("no existe en base de datos")) {
            return "";
        }
        return t;
    }

    private static String junta(String a, String b) {
        String x = limpio(a), y = limpio(b);
        if (x.isEmpty()) {
            return y;
        }
        return y.isEmpty() ? x : x + " · " + y;
    }

    private static long parseLong(String v) {
        try {
            return v == null ? 0 : Math.round(Double.parseDouble(v.trim()));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static boolean esFecha(String v) {
        return v != null && v.length() >= 8 && !"0".equals(v.trim());
    }

    /**
     * Normaliza a ISO yyyy-MM-dd. Los WS mezclan formatos: ISO con hora (consultas,
     * accidentes), dd/MM/yyyy (maternidad, incapacidades por NSS), y "0" para null.
     */
    private static String iso(String fecha) {
        if (!esFecha(fecha)) {
            return null;
        }
        String v = fecha.trim();
        try {
            if (v.matches("^\\d{4}-\\d{2}-\\d{2}.*")) {
                return v.substring(0, 10);
            }
            if (v.matches("^\\d{1,2}/\\d{1,2}/\\d{4}$")) {
                return LocalDate.parse(v, DateTimeFormatter.ofPattern("d/M/yyyy")).toString();
            }
            if (v.matches("^\\d{1,2}/\\d{1,2}/\\d{2}$")) {
                return LocalDate.parse(v, DateTimeFormatter.ofPattern("d/M/yy")).toString();
            }
        } catch (DateTimeParseException ignored) {
            // formato inesperado: sin fecha, va al final del timeline
        }
        return null;
    }
}
