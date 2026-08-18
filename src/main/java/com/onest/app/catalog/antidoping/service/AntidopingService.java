package com.onest.app.catalog.antidoping.service;

import com.onest.app.catalog.antidoping.client.AntidopingClient;
import com.onest.app.catalog.antidoping.client.dto.BiowsAntidopingAltaRequest;
import com.onest.app.catalog.antidoping.dto.AntidopingDto;
import com.onest.app.catalog.antidoping.dto.AntidopingReporteDto;
import com.onest.app.catalog.antidoping.web.AntidopingAltaForm;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Antidoping/alcoholimetria por NSS (lista + alta). Backend aplicado y verificado
 * 2026-08-14 contra el WS ORDS real (docs/ords-antidoping.sql).
 */
@Service
public class AntidopingService {

    private static final String USUARIO_FIJO = "747849849";
    // Mismo formato de 2 digitos de anio que Incapacidad/Accidente/Consulta para su
    // reporte por fecha.
    private static final DateTimeFormatter FECHA_REPORTE = DateTimeFormatter.ofPattern("dd/MM/yy");

    private final AntidopingClient client;

    public AntidopingService(AntidopingClient client) {
        this.client = client;
    }

    public List<AntidopingDto> byNss(String nss) {
        return client.findAntidopings(normalizeNss(nss));
    }

    /**
     * Reporte administrativo por rango de fechas (todas las NSS), para el dashboard.
     * fechaInicial/fechaFinal llegan en formato ISO (yyyy-MM-dd) y se reformatean a "dd/MM/yy".
     */
    public List<AntidopingReporteDto> reportePorFecha(String fechaInicial, String fechaFinal) {
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

    /** Alta de antidoping (POST /Servcio/antidoping). Devuelve el mensaje Proceso. */
    public String crearAntidoping(AntidopingAltaForm form) {
        String nss = normalizeNss(form.getNss());
        if (form.getTipoPrueba() == null || form.getTipoPrueba().isBlank()) {
            throw new IllegalArgumentException("El tipo de prueba es obligatorio");
        }
        if (form.getResultado() == null || form.getResultado().isBlank()) {
            throw new IllegalArgumentException("El resultado es obligatorio");
        }

        BiowsAntidopingAltaRequest request = new BiowsAntidopingAltaRequest(
                nss,
                form.getFolio(),
                form.getTipoPrueba(),
                form.getSustancia(),
                form.getResultado(),
                form.getStatusConclusion(),
                form.getObservaciones(),
                USUARIO_FIJO,
                usuarioActual());
        return client.crearAntidoping(request);
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
