package com.onest.app.catalog.accidente.service;

import com.onest.app.catalog.accidente.client.AccidenteClient;
import com.onest.app.catalog.accidente.client.dto.BiowsAccidenteAltaRequest;
import com.onest.app.catalog.accidente.client.dto.BiowsAccidenteSeguimientoAltaRequest;
import com.onest.app.catalog.accidente.dto.AccidenteDto;
import com.onest.app.catalog.accidente.dto.AccidenteReporteDto;
import com.onest.app.catalog.accidente.dto.AccidenteSeguimientoDto;
import com.onest.app.catalog.accidente.web.AccidenteAltaForm;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Accidentes de trabajo por NSS (lista + alta). Backend aplicado y verificado
 * 2026-08-14 contra el WS ORDS real (docs/ords-accidentes.sql).
 */
@Service
public class AccidenteService {

    private static final String USUARIO_FIJO = "747849849";
    private static final Set<String> TIPOS_SEGUIMIENTO = Set.of("SEGUIMIENTO", "CIERRE");
    // Formato confirmado en vivo el 2026-08-14 contra Servcio/accidente (ver docs/ords-accidentes.sql NOTA-2).
    private static final DateTimeFormatter FECHA_ACCIDENTE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    // Mismo formato de 2 digitos de anio que usa Incapacidad para su reporte por fecha
    // (ver IncapacidadService.FECHA_REPORTE) - confirmado en vivo 2026-08-17.
    private static final DateTimeFormatter FECHA_REPORTE = DateTimeFormatter.ofPattern("dd/MM/yy");

    private final AccidenteClient client;

    public AccidenteService(AccidenteClient client) {
        this.client = client;
    }

    public List<AccidenteDto> byNss(String nss) {
        return client.findAccidentes(normalizeNss(nss));
    }

    /** Igual que {@link #byNss(String)} pero con el estado de seguimiento calculado por caso. */
    public List<AccidenteDto> byNssConEstado(String nss) {
        return byNss(nss).stream()
                .map(a -> a.withEstado(estadoDe(seguimientos(a.idRegistro()))))
                .toList();
    }

    /** Historial de seguimiento de un caso especifico, mas reciente primero. */
    public List<AccidenteSeguimientoDto> seguimientos(String accidenteRegId) {
        return client.findSeguimientos(parseAccidenteRegId(accidenteRegId));
    }

    /** "CERRADO" si existe al menos un seguimiento TIPO=CIERRE, "ABIERTO" si no. */
    public String estadoDe(List<AccidenteSeguimientoDto> historial) {
        boolean cerrado = historial.stream().anyMatch(s -> "CIERRE".equals(s.tipo()));
        return cerrado ? "CERRADO" : "ABIERTO";
    }

    /** Alta de un seguimiento sobre un caso ya registrado (docs/ords-accidentes-seguimiento.sql). */
    public String crearSeguimiento(String accidenteRegId, String tipo, String observaciones) {
        long regId = parseAccidenteRegId(accidenteRegId);
        if (tipo == null || !TIPOS_SEGUIMIENTO.contains(tipo.trim().toUpperCase())) {
            throw new IllegalArgumentException("El tipo de seguimiento debe ser SEGUIMIENTO o CIERRE");
        }
        if (observaciones == null || observaciones.isBlank()) {
            throw new IllegalArgumentException("Las observaciones son obligatorias");
        }
        if (estadoDe(seguimientos(accidenteRegId)).equals("CERRADO")) {
            throw new IllegalArgumentException("El caso ya está cerrado, no admite más seguimiento");
        }
        BiowsAccidenteSeguimientoAltaRequest request = new BiowsAccidenteSeguimientoAltaRequest(
                regId, tipo.trim().toUpperCase(), observaciones.trim(), USUARIO_FIJO, usuarioActual());
        return client.registrarSeguimiento(request);
    }

    private static long parseAccidenteRegId(String accidenteRegId) {
        if (accidenteRegId == null || accidenteRegId.isBlank()) {
            throw new IllegalArgumentException("El caso de accidente es obligatorio");
        }
        try {
            return Long.parseLong(accidenteRegId.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("El identificador del caso no es válido", ex);
        }
    }

    /**
     * Reporte administrativo por rango de fechas (todas las NSS), para el dashboard.
     * fechaInicial/fechaFinal llegan en formato ISO (yyyy-MM-dd) y se reformatean a "dd/MM/yy".
     */
    public List<AccidenteReporteDto> reportePorFecha(String fechaInicial, String fechaFinal) {
        LocalDate desde = parseFechaIso(fechaInicial, "inicial");
        LocalDate hasta = parseFechaIso(fechaFinal, "final");
        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la fecha final");
        }
        return client.reportePorFecha(desde.format(FECHA_REPORTE), hasta.format(FECHA_REPORTE));
    }

    private static LocalDate parseFechaIso(String valor, String etiqueta) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("La fecha " + etiqueta + " es obligatoria");
        }
        try {
            return LocalDate.parse(valor.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("La fecha " + etiqueta + " no es valida", ex);
        }
    }

    /** Alta de accidente (POST /Servcio/accidente). Devuelve el mensaje Proceso. */
    public String crearAccidente(AccidenteAltaForm form) {
        String nss = normalizeNss(form.getNss());
        if (form.getTipoRiesgo() == null || form.getTipoRiesgo().isBlank()) {
            throw new IllegalArgumentException("El tipo de riesgo es obligatorio");
        }

        BiowsAccidenteAltaRequest request = new BiowsAccidenteAltaRequest(
                nss,
                formatFechaAccidente(form.getFechaAccidente()),
                form.getTipoRiesgo(),
                form.getCausaRt(),
                form.getDiagnostico(),
                parseDecimal(form.getSdi(), "SDI"),
                form.getStatusCalificacion(),
                parseDecimal(form.getCosto(), "costo"),
                form.getObservaciones(),
                USUARIO_FIJO,
                usuarioActual(),
                parseDias(form.getDias()));
        return client.crearAccidente(request);
    }

    /** Dias de incapacidad: opcional, entero de 0 a 99999 (NUMBER(5) en SERV_MED_ACCIDENTE). */
    private static Integer parseDias(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            int dias = Integer.parseInt(value.trim());
            if (dias < 0 || dias > 99_999) {
                throw new IllegalArgumentException("Los días deben estar entre 0 y 99999");
            }
            return dias;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Los días deben ser un número entero", ex);
        }
    }

    private static String formatFechaAccidente(String isoDate) {
        if (isoDate == null || isoDate.isBlank()) {
            throw new IllegalArgumentException("La fecha del accidente es obligatoria");
        }
        LocalDate fecha;
        try {
            fecha = LocalDate.parse(isoDate.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("La fecha del accidente no es valida", ex);
        }
        // C-041 / B2 (9-nov): un accidente no puede ser futuro. Hora de Mexico, no la del servidor (puede estar en
        // UTC y a partir de las 18:00 "hoy" ya seria manana). El limite de anios atras espera a SM (E-019).
        if (fecha.isAfter(LocalDate.now(java.time.ZoneId.of("America/Mexico_City")))) {
            throw new IllegalArgumentException("La fecha del accidente no puede ser futura");
        }
        return fecha.format(FECHA_ACCIDENTE);
    }

    private static BigDecimal parseDecimal(String value, String etiqueta) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("El valor de " + etiqueta + " no es válido", ex);
        }
    }

    private String usuarioActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return "SISTEMA";
        }
        return authentication.getName();
    }

    private String normalizeNss(String nss) {
        if (nss == null || nss.isBlank()) {
            throw new IllegalArgumentException("El NSS es obligatorio");
        }
        String value = nss.trim();
        if (value.length() > 50) {
            throw new IllegalArgumentException("El NSS excede la longitud permitida");
        }
        return value;
    }
}
