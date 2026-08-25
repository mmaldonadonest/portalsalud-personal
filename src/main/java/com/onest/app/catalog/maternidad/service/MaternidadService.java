package com.onest.app.catalog.maternidad.service;

import com.onest.app.catalog.maternidad.client.MaternidadClient;
import com.onest.app.catalog.maternidad.client.dto.BiowsMaternidadAltaRequest;
import com.onest.app.catalog.maternidad.dto.MaternidadDto;
import com.onest.app.catalog.maternidad.web.MaternidadAltaForm;
import com.onest.app.catalog.nss.client.NssSearchClient;
import com.onest.app.catalog.nss.dto.EmpleadoDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Seguimiento de maternidad por NSS (historial + alta de un chequeo). Backend en
 * docs/ords-maternidad.sql, aplicado 2026-08-21. Solo el NSS es obligatorio - las 3 fechas
 * (probable parto, proxima revision, reincorporacion) son opcionales: un chequeo temprano
 * puede no tenerlas definidas aun. Cada alta es un registro NUEVO, nunca un update - preserva
 * el historial completo del seguimiento.
 */
@Service
public class MaternidadService {

    private static final Logger log = LoggerFactory.getLogger(MaternidadService.class);

    // Mismo formato confirmado en vivo para Accidentes (ver docs/ords-accidentes.sql NOTA-2).
    private static final DateTimeFormatter FECHA_WS = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    // Mismo valor fijo que usa AccidenteService.USUARIO_FIJO para USUARIO_ID - no documentado
    // que represente, se replica igual por consistencia con el resto del sistema.
    private static final String USUARIO_FIJO = "747849849";

    // emp_sexo NO tiene catalogo consistente (ver docs/cuestionario-bloqueadores-negocio.md
    // pregunta #8 - 12 variantes distintas confirmadas: M/m/F/f/H/HOMBRE/MUJER/O/X/1/2/vacio).
    // Solo se bloquean los valores CONFIRMADOS como Hombre (1=Hombre, 2=Mujer confirmado por el
    // usuario 2026-08-21, mas las variantes de texto obvias). "O"/"X"/vacio/desconocido NO se
    // bloquean - no hay forma de saber si significan Hombre, bloquear eso seria inventar.
    private static final Set<String> VALORES_HOMBRE = Set.of("1", "M", "H", "HOMBRE");

    private final MaternidadClient client;
    private final NssSearchClient nssSearchClient;

    public MaternidadService(MaternidadClient client, NssSearchClient nssSearchClient) {
        this.client = client;
        this.nssSearchClient = nssSearchClient;
    }

    public List<MaternidadDto> byNss(String nss) {
        return client.findSeguimientos(normalizeNss(nss));
    }

    /**
     * true si el sexo del NSS esta CONFIRMADO como Hombre - para mostrar un aviso en la UI
     * antes de que el usuario intente guardar. Mismas reglas/limites que validarSexoNoHombre():
     * no bloquea/marca nada si la consulta falla o el valor no es una de las 4 variantes
     * confirmadas.
     */
    public boolean esSexoHombre(String nss) {
        if (nss == null || nss.isBlank()) {
            return false;
        }
        try {
            return nssSearchClient.findUsuario(nss.trim(), "MATERNIDAD")
                    .map(EmpleadoDto::sexo)
                    .filter(sexo -> sexo != null && !sexo.isBlank())
                    .map(sexo -> VALORES_HOMBRE.contains(sexo.trim().toUpperCase()))
                    .orElse(false);
        } catch (Exception ex) {
            log.warn("[maternidad] No se pudo verificar el sexo del NSS {}", nss, ex);
            return false;
        }
    }

    /** Alta de un chequeo de seguimiento (POST /Servcio/maternidad). Devuelve el mensaje Proceso. */
    public String crearSeguimiento(MaternidadAltaForm form) {
        String nss = normalizeNss(form.getNss());
        validarSexoNoHombre(nss);

        BiowsMaternidadAltaRequest request = new BiowsMaternidadAltaRequest(
                nss,
                parseDecimal(form.getSemanasGestacion(), "semanas de gestación"),
                formatFechaOpcional(form.getFechaProbableParto(), "fecha probable de parto"),
                form.getRestriccionesLaborales(),
                formatFechaOpcional(form.getProximaRevision(), "próxima revisión"),
                form.getObservaciones(),
                form.getEstatus(),
                form.getIncapacidad(),
                formatFechaOpcional(form.getReincorporacion(), "reincorporación"),
                USUARIO_FIJO,
                usuarioActual());
        return client.crearSeguimiento(request);
    }

    /**
     * Bloquea el alta si el sexo del NSS esta CONFIRMADO como Hombre (ver esSexoHombre()).
     * Usa NssSearchClient.findUsuario() directo (NO NssSearchService.findByNss(), que puede
     * disparar una ALTA en Servcio/Medico como efecto colateral - mismo criterio que
     * PortalUserDetailsService.resolveEmail()).
     */
    private void validarSexoNoHombre(String nss) {
        if (esSexoHombre(nss)) {
            throw new IllegalArgumentException("No se le puede asociar un seguimiento de maternidad");
        }
    }

    private static String formatFechaOpcional(String isoDate, String etiqueta) {
        if (isoDate == null || isoDate.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(isoDate.trim()).format(FECHA_WS);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("La " + etiqueta + " no es valida", ex);
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
