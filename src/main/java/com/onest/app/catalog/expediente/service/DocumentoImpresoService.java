package com.onest.app.catalog.expediente.service;

import com.onest.app.catalog.nss.dto.EmpleadoDto;
import com.onest.app.catalog.nss.service.NssSearchService;
import com.onest.app.security.service.PortalUserPrincipal;
import java.lang.reflect.RecordComponent;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Piezas comunes de los documentos imprimibles del expediente (Pre-Test, Consulta medica,
 * Incapacidad): ficha del trabajador, fecha de impresion y limpieza de los textos de relleno
 * que el PL/SQL devuelve en lugar de null.
 *
 * <p>El expediente del EXAMEN no usa esto: es un port 1:1 del PHP con su propio servicio
 * ({@link com.onest.app.catalog.examen.service.ExamenDocumentoService}), y se deja aparte para
 * poder regenerarlo desde el PHP sin arrastrar cambios.
 *
 * <p>El PDF lo genera el navegador (window.print), como en el legacy: sin libreria de PDF.
 */
public final class DocumentoImpresoService {

    private static final DateTimeFormatter IMPRESO =
            DateTimeFormatter.ofPattern("dd/MM/yy, HH:mm", new Locale("es", "MX"));

    private DocumentoImpresoService() {
    }

    /** Fecha y hora de impresion para el encabezado de cada hoja. */
    public static String ahora() {
        return LocalDateTime.now().format(IMPRESO);
    }

    public static EmpleadoDto empleado(NssSearchService nssSearchService, String nss) {
        try {
            return nssSearchService.findByNss(nss).map(r -> r.empleado()).orElse(null);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    public static String nombreCompleto(EmpleadoDto e, String nss) {
        if (e == null) {
            return nss;
        }
        if (e.completo() != null && !e.completo().isBlank()) {
            return e.completo().trim();
        }
        String armado = (texto(e.nombre()) + " " + texto(e.apellidoPaterno()) + " " + texto(e.apellidoMaterno())).trim();
        return armado.isBlank() ? nss : armado;
    }

    /**
     * Vacia los textos de relleno del PL/SQL ("sin datos", "sin observaciones", "sin firma",
     * "no existe en base de datos") para que no se impriman como si fueran captura. El "0" se
     * respeta: en costo, dias o peso es un dato (el examen, que si lo trata como relleno, tiene
     * su propio servicio).
     */
    public static String texto(String v) {
        if (v == null) {
            return "";
        }
        String t = v.trim();
        String u = t.toLowerCase(Locale.ROOT);
        if (u.isEmpty() || u.equals("null") || u.equals("sin datos") || u.equals("sin firma")
                || u.startsWith("sin obs") || u.contains("no existe en base de datos")) {
            return "";
        }
        return t;
    }

    /** Si/No para los campos que el WS guarda como 1/0 (o true/false). Aqui "0" es No, no relleno. */
    public static String siNo(String v) {
        if (v != null && v.trim().equals("0")) {
            return "No";
        }
        String t = texto(v).toLowerCase(Locale.ROOT);
        if (t.isEmpty()) {
            return "";
        }
        return t.equals("1") || t.equals("si") || t.equals("sí") || t.equals("true") ? "Sí" : "No";
    }

    /**
     * Copia de los componentes String de un record con {@link #texto} aplicado, para pasar el
     * detalle del WS a la vista sin los textos de relleno. SpEL resuelve {@code d.campo} sobre el
     * Map igual que sobre el record.
     */
    public static Map<String, String> limpio(Record r) {
        Map<String, String> m = new LinkedHashMap<>();
        if (r == null) {
            return m;
        }
        for (RecordComponent c : r.getClass().getRecordComponents()) {
            try {
                Object v = c.getAccessor().invoke(r);
                m.put(c.getName(), v == null ? "" : fecha(texto(String.valueOf(v))));
            } catch (ReflectiveOperationException ex) {
                m.put(c.getName(), "");
            }
        }
        return m;
    }

    /**
     * Las fechas ISO del WS (2026-08-27T21:36:32Z) se imprimen como dd/MM/yyyy HH:mm; lo demas se
     * deja igual. La hora se toma LITERAL (sin convertir la "Z"): ORDS marca Z sobre la hora local
     * de la BD, y el resto del portal (listas, dashboard) tambien la lee tal cual.
     */
    public static String fecha(String v) {
        if (v == null || !ISO.matcher(v).matches()) {
            return v;
        }
        try {
            if (v.length() == 10) {
                return LocalDate.parse(v).format(DIA);
            }
            String sinZona = v.replaceFirst("(Z|[+-]\\d{2}:\\d{2})$", "").replaceFirst("\\.\\d+$", "");
            return LocalDateTime.parse(sinZona).format(DIA_HORA);
        } catch (DateTimeException ex) {
            return v;
        }
    }

    private static final Pattern ISO = Pattern.compile("\\d{4}-\\d{2}-\\d{2}(T\\d{2}:\\d{2}(:\\d{2}(\\.\\d+)?)?(Z|[+-]\\d{2}:\\d{2})?)?");
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DIA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Usuario logueado (nombre visible si lo trae el principal), para "capturo". */
    public static String usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return "";
        }
        if (auth.getPrincipal() instanceof PortalUserPrincipal p && p.getDisplayName() != null
                && !p.getDisplayName().isBlank()) {
            return p.getDisplayName().trim();
        }
        return "anonymousUser".equals(auth.getName()) ? "" : auth.getName();
    }

    /**
     * Busca, entre los ids de un NSS (del mas reciente hacia atras, tope {@code MAX_BUSQUEDA}),
     * el primero cuyo detalle tenga la relacion de adjuntos {@code rel} (el idArchivoRel que
     * genera el formulario de alta). Sirve para abrir el documento justo despues de guardar,
     * cuando el WS todavia no nos dijo que ID_CONSULTA asigno.
     */
    public static <D> Optional<D> porRelacion(List<String> ids, String rel, Function<String, Optional<D>> detalle,
                                              Function<D, String> relacionDe) {
        if (rel == null || rel.isBlank()) {
            return Optional.empty();
        }
        List<String> orden = new ArrayList<>(ids);
        orden.sort(Comparator.comparingLong(DocumentoImpresoService::numero).reversed());
        int vistos = 0;
        for (String id : orden) {
            if (vistos++ >= MAX_BUSQUEDA) {
                break;
            }
            Optional<D> d = detalle.apply(id);
            if (d.isPresent() && rel.trim().equalsIgnoreCase(texto(relacionDe.apply(d.get())))) {
                return d;
            }
        }
        return Optional.empty();
    }

    private static final int MAX_BUSQUEDA = 10;

    private static long numero(String id) {
        try {
            return Long.parseLong(id.trim());
        } catch (RuntimeException ex) {
            return Long.MIN_VALUE;
        }
    }

    /** Data URL de la firma capturada, o null si el registro no trae firma. */
    public static String firma(String v) {
        String t = texto(v);
        return t.startsWith("data:image") ? t : null;
    }
}
