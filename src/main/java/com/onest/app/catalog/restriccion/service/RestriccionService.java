package com.onest.app.catalog.restriccion.service;

import com.onest.app.catalog.restriccion.client.RestriccionClient;
import com.onest.app.catalog.restriccion.client.dto.BiowsRestriccionAltaRequest;
import com.onest.app.catalog.restriccion.dto.RestriccionCatalogoDto;
import com.onest.app.catalog.restriccion.dto.RestriccionDto;
import com.onest.app.catalog.restriccion.web.RestriccionAltaForm;
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
 * Restricciones medicas asignadas por NSS (insert-only, conserva historico de
 * revaloraciones). Backend aplicado 2026-08-24 (docs/ords-restriccion.sql).
 * Alcance: docs/checklist-bloqueadores-negocio.html #2 (Medico Jefe).
 */
@Service
public class RestriccionService {

    private static final String USUARIO_FIJO = "747849849";
    private static final DateTimeFormatter FECHA_WS = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** RES-01..RES-16, catalogo fijo (no administrable) definido en la propuesta formal. */
    private static final List<RestriccionCatalogoDto> CATALOGO = List.of(
            new RestriccionCatalogoDto("RES-01", "No cargar peso"),
            new RestriccionCatalogoDto("RES-02", "Límite de carga"),
            new RestriccionCatalogoDto("RES-03", "No realizar trabajo en alturas"),
            new RestriccionCatalogoDto("RES-04", "No operar maquinaria"),
            new RestriccionCatalogoDto("RES-05", "No conducir vehículos"),
            new RestriccionCatalogoDto("RES-06", "Evitar movimientos repetitivos"),
            new RestriccionCatalogoDto("RES-07", "Evitar exposición a ruido"),
            new RestriccionCatalogoDto("RES-08", "Evitar exposición a sustancias"),
            new RestriccionCatalogoDto("RES-09", "Evitar temperaturas extremas"),
            new RestriccionCatalogoDto("RES-10", "Evitar esfuerzo físico intenso"),
            new RestriccionCatalogoDto("RES-11", "Trabajo administrativo"),
            new RestriccionCatalogoDto("RES-12", "Requiere pausas periódicas"),
            new RestriccionCatalogoDto("RES-13", "Restricción de horario"),
            new RestriccionCatalogoDto("RES-14", "Restricción temporal"),
            new RestriccionCatalogoDto("RES-15", "Uso obligatorio de equipo específico"),
            new RestriccionCatalogoDto("RES-16", "Otra restricción")
    );
    private static final Set<String> CODIGOS_VALIDOS = CATALOGO.stream()
            .map(RestriccionCatalogoDto::codigo).collect(java.util.stream.Collectors.toUnmodifiableSet());

    private final RestriccionClient client;

    public RestriccionService(RestriccionClient client) {
        this.client = client;
    }

    public List<RestriccionCatalogoDto> catalogo() {
        return CATALOGO;
    }

    public List<RestriccionDto> byNss(String nss) {
        return client.findRestricciones(normalizeNss(nss));
    }

    public String crear(RestriccionAltaForm form) {
        String nss = normalizeNss(form.getNss());
        if (form.getCodigoRestriccion() == null || !CODIGOS_VALIDOS.contains(form.getCodigoRestriccion().trim())) {
            throw new IllegalArgumentException("El código de restricción no es válido");
        }
        if (form.getDescripcion() == null || form.getDescripcion().isBlank()) {
            throw new IllegalArgumentException("La descripción es obligatoria");
        }

        BiowsRestriccionAltaRequest request = new BiowsRestriccionAltaRequest(
                nss,
                form.getCodigoRestriccion().trim(),
                form.getDescripcion().trim(),
                parseDecimal(form.getValorLimite()),
                form.getUnidad(),
                formatFechaOpcional(form.getFechaInicio()),
                formatFechaOpcional(form.getFechaFin()),
                formatFechaOpcional(form.getFechaRevaloracion()),
                form.getTemporalidad(),
                form.getObservaciones(),
                form.getMedicoResponsable(),
                form.getEstatus(),
                USUARIO_FIJO,
                usuarioActual());
        return client.crearRestriccion(request);
    }

    private static String formatFechaOpcional(String isoDate) {
        if (isoDate == null || isoDate.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(isoDate.trim()).format(FECHA_WS);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Una de las fechas no es valida", ex);
        }
    }

    private static BigDecimal parseDecimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("El valor limite no es valido", ex);
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
