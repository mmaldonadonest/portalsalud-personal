package com.onest.app.catalog.maternidad.service;

import com.onest.app.catalog.maternidad.client.MaternidadClient;
import com.onest.app.catalog.maternidad.client.dto.BiowsMaternidadAltaRequest;
import com.onest.app.catalog.maternidad.dto.MaternidadDto;
import com.onest.app.catalog.maternidad.web.MaternidadAltaForm;
import com.onest.app.catalog.nss.client.NssSearchClient;
import com.onest.app.catalog.nss.dto.EmpleadoDto;
import com.onest.app.catalog.pretest.repository.MedTagRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
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

    // C-037: el WS no tiene columnas para la FUM ni el inicio de la incapacidad. Son datos del embarazo
    // EN CURSO (no de cada chequeo), asi que van a SERV_MED_TAG por NSS, el ultimo valor manda: un
    // embarazo nuevo se registra cambiando la FUM. Mismo patron que la cadena de incapacidades.
    static final String TAG_FUM = "MATER.FUM";
    static final String TAG_INICIO = "MATER.INICIO_INCAP";
    private static final String TAG_GRUPO = "MATERNIDAD";
    // Prod corre en UTC; "hoy" es el de Mexico (mismo criterio que IncapacidadService).
    private static final ZoneId ZONA = ZoneId.of("America/Mexico_City");

    private final MaternidadClient client;
    private final NssSearchClient nssSearchClient;
    private final MedTagRepository tags;

    public MaternidadService(MaternidadClient client, NssSearchClient nssSearchClient, MedTagRepository tags) {
        this.client = client;
        this.nssSearchClient = nssSearchClient;
        this.tags = tags;
    }

    /**
     * FUM e inicio de la incapacidad guardados del embarazo en curso, con sus calculos (C-037).
     * Null si no hay ninguno o si la base no responde: la pantalla sigue sin el recuadro.
     */
    public CalculoMaternidad.Resultado calculo(String nss) {
        try {
            Map<String, String> t = tags.latestByNssAndTypePrefix(normalizeNss(nss), "MATER.");
            CalculoMaternidad.Resultado r = CalculoMaternidad.calcular(
                    fechaTag(t.get(TAG_FUM)), fechaTag(t.get(TAG_INICIO)), LocalDate.now(ZONA));
            return r.vacio() ? null : r;
        } catch (DataAccessException ex) {
            log.warn("[maternidad] No se pudo leer la FUM del NSS {}", nss, ex);
            return null;
        }
    }

    private static LocalDate fechaTag(String valor) {
        try {
            return valor == null || valor.isBlank() ? null : LocalDate.parse(valor.trim());
        } catch (DateTimeParseException ex) {
            return null;
        }
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

        // C-037: con FUM, semanas y fecha probable de parto salen de la formula de SM (el formulario
        // ya las muestra asi y de solo lectura; aqui se recalculan para que nadie las desvie). Con
        // inicio de incapacidad, la reincorporacion = termino + 1 si no se capturo otra.
        LocalDate hoy = LocalDate.now(ZONA);
        LocalDate fum = parseFecha(form.getFum(), "FUM (fecha de última menstruación)");
        LocalDate inicio = parseFecha(form.getInicioIncapacidad(), "fecha de inicio de la incapacidad");
        if (fum != null && fum.isAfter(hoy)) {
            throw new IllegalArgumentException("La FUM no puede ser posterior a hoy.");
        }
        if (fum != null && inicio != null && inicio.isBefore(fum)) {
            throw new IllegalArgumentException("El inicio de la incapacidad es anterior a la FUM. "
                    + "Si es de otro embarazo, bórralo.");
        }
        CalculoMaternidad.Resultado calc = CalculoMaternidad.calcular(fum, inicio, hoy);
        BigDecimal semanas = fum != null
                ? (calc.semanas() == null ? null : BigDecimal.valueOf(calc.semanas()))
                : parseDecimal(form.getSemanasGestacion(), "semanas de gestación");
        String fpp = fum != null ? calc.fpp().format(FECHA_WS)
                : formatFechaOpcional(form.getFechaProbableParto(), "fecha probable de parto");
        String reincorporacion = formatFechaOpcional(form.getReincorporacion(), "reincorporación");
        if (reincorporacion == null && calc.reinicio() != null) {
            reincorporacion = calc.reinicio().format(FECHA_WS);
        }

        BiowsMaternidadAltaRequest request = new BiowsMaternidadAltaRequest(
                nss,
                semanas,
                fpp,
                form.getRestriccionesLaborales(),
                formatFechaOpcional(form.getProximaRevision(), "próxima revisión"),
                form.getObservaciones(),
                form.getEstatus(),
                form.getIncapacidad(),
                reincorporacion,
                USUARIO_FIJO,
                usuarioActual());
        String proceso = client.crearSeguimiento(request);
        guardarEmbarazo(nss, fum, inicio);
        return proceso;
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

    /**
     * Despues del alta: si falla la base del portal el chequeo ya quedo guardado en el WS, asi que
     * no se revierte; solo se avisa en el log (mismo criterio que guardarExtras de incapacidades).
     * Una fecha vacia se guarda vacia: es como se borra la de otro embarazo.
     */
    private void guardarEmbarazo(String nss, LocalDate fum, LocalDate inicio) {
        try {
            tags.upsert(nss, TAG_FUM, fum == null ? null : fum.toString(), TAG_GRUPO, usuarioActual());
            tags.upsert(nss, TAG_INICIO, inicio == null ? null : inicio.toString(), TAG_GRUPO, usuarioActual());
        } catch (DataAccessException ex) {
            log.warn("[maternidad] Chequeo guardado pero no la FUM / inicio del NSS {}", nss, ex);
        }
    }

    private static LocalDate parseFecha(String isoDate, String etiqueta) {
        if (isoDate == null || isoDate.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(isoDate.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("La " + etiqueta + " no es válida", ex);
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
