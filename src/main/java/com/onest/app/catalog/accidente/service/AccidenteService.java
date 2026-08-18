package com.onest.app.catalog.accidente.service;

import com.onest.app.catalog.accidente.client.AccidenteClient;
import com.onest.app.catalog.accidente.client.dto.BiowsAccidenteAltaRequest;
import com.onest.app.catalog.accidente.dto.AccidenteDto;
import com.onest.app.catalog.accidente.dto.AccidenteReporteDto;
import com.onest.app.catalog.accidente.web.AccidenteAltaForm;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
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
                usuarioActual());
        return client.crearAccidente(request);
    }

    private static String formatFechaAccidente(String isoDate) {
        if (isoDate == null || isoDate.isBlank()) {
            throw new IllegalArgumentException("La fecha del accidente es obligatoria");
        }
        try {
            return LocalDate.parse(isoDate.trim()).format(FECHA_ACCIDENTE);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("La fecha del accidente no es valida", ex);
        }
    }

    private static BigDecimal parseDecimal(String value, String etiqueta) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("El valor de " + etiqueta + " no es valido", ex);
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
