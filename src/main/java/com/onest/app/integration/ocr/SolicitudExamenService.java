package com.onest.app.integration.ocr;

import com.onest.app.audit.service.AuditoriaService;
import com.onest.app.catalog.examen.repository.ExamenHistoricoRepository;
import com.onest.app.catalog.exameninicial.service.ExamenInicialService;
import com.onest.app.notification.NotificacionService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Solicitudes de examen medico inicial que manda OCR (docs/integracion-ocr-examen-inicial.md).
 * Crea la solicitud (validacion, idempotencia, candado), la muestra como pendiente, la marca
 * en captura cuando el medico abre el examen y, al finalizar, la cierra y programa el webhook.
 *
 * <p>El cierre llega por {@link ExamenInicialService.ExamenInicialFinalizado} (evento) y no
 * por llamada directa, para no crear un ciclo: este servicio usa ExamenInicialService.candado().
 */
@Service
public class SolicitudExamenService {

    private static final Logger log = LoggerFactory.getLogger(SolicitudExamenService.class);
    public static final String ORIGEN_OCR = "OCR";

    /** Cuerpo del POST de OCR. Campos opcionales pueden venir null. */
    public record SolicitudRequest(String referenciaOcr, String nss, String curp, String rfc,
                                   String nombre, String apellidoPaterno, String apellidoMaterno,
                                   String fechaNacimiento, String sexo, String estadoCivil, String escolaridad, String profesionOficio,
                                   String domicilio, String telefono, String empresa, String cuenta, String agencia,
                                   String puestoSolicitado, String fechaCita, String callbackUrl) {
    }

    /** Resultado de crear: la solicitud y si ya existia (200) o es nueva (201). */
    public record Creada(SolicitudExamen solicitud, boolean existia) {
    }

    public static class SolicitudInvalida extends RuntimeException {
        private final List<String> detalles;

        public SolicitudInvalida(List<String> detalles) {
            super("Solicitud inválida: " + String.join("; ", detalles));
            this.detalles = detalles;
        }

        public List<String> detalles() {
            return detalles;
        }
    }

    /** El NSS ya tiene examen vigente como empleado (candado del examen inicial). */
    public static class ExamenVigente extends RuntimeException {
        private final String dictamen;
        private final String fecha;

        public ExamenVigente(String mensaje, String dictamen, String fecha) {
            super(mensaje);
            this.dictamen = dictamen;
            this.fecha = fecha;
        }

        public String dictamen() {
            return dictamen;
        }

        public String fecha() {
            return fecha;
        }
    }

    private final SolicitudExamenRepository repo;
    private final ExamenInicialService examenInicialService;
    private final ExamenHistoricoRepository historico;
    private final NotificacionService notificaciones;
    private final OcrWebhookService webhook;
    private final AuditoriaService auditoria;
    private final OcrProperties props;

    public SolicitudExamenService(SolicitudExamenRepository repo, ExamenInicialService examenInicialService,
                                  ExamenHistoricoRepository historico, NotificacionService notificaciones,
                                  OcrWebhookService webhook, AuditoriaService auditoria, OcrProperties props) {
        this.repo = repo;
        this.examenInicialService = examenInicialService;
        this.historico = historico;
        this.notificaciones = notificaciones;
        this.webhook = webhook;
        this.auditoria = auditoria;
        this.props = props;
    }

    @Transactional
    public Creada crear(SolicitudRequest r, HttpServletRequest request) {
        List<String> errores = validar(r);
        if (!errores.isEmpty()) {
            throw new SolicitudInvalida(errores);
        }
        String nss = r.nss().trim();
        String referencia = limpio(r.referenciaOcr()).isEmpty() ? "NSS-" + nss : r.referenciaOcr().trim();

        Optional<SolicitudExamen> existente = repo.porReferencia(ORIGEN_OCR, referencia);
        if (existente.isPresent()) {
            return new Creada(existente.get(), true);
        }

        // Candado: el inicial es de nuevo ingreso. Si ya tiene examen vigente como empleado, 409.
        ExamenInicialService.Candado candado = examenInicialService.candado(nss, false);
        if (candado.bloqueado()) {
            String dictamen = examenInicialService.dictamenVigenteCodigo(nss);
            throw new ExamenVigente(candado.motivo(), dictamen, examenInicialService.fechaExamenVigente(nss));
        }

        LocalDate nacimiento = fecha(r.fechaNacimiento());
        String sexo = limpio(r.sexo()).toUpperCase();
        // La CURP trae fecha de nacimiento (pos 4-9, aammdd) y sexo (pos 10: H/M)
        String curp = r.curp().trim().toUpperCase();
        if (nacimiento == null && curp.length() >= 10) {
            nacimiento = nacimientoDesdeCurp(curp);
        }
        if (sexo.isEmpty() && curp.length() >= 11) {
            char c = curp.charAt(10);
            sexo = c == 'H' ? "M" : (c == 'M' ? "F" : "");
        }
        LocalDate cita = fecha(r.fechaCita());
        LocalDate caduca = (cita != null ? cita : LocalDate.now()).plusDays(props.caducidad());

        SolicitudExamen nueva = new SolicitudExamen(0, ORIGEN_OCR, referencia, nss, curp, vacioANull(r.rfc()),
                r.nombre().trim(), r.apellidoPaterno().trim(), r.apellidoMaterno().trim(), nacimiento, vacioANull(sexo),
                vacioANull(r.estadoCivil()), vacioANull(r.escolaridad()), vacioANull(r.profesionOficio()), vacioANull(r.domicilio()),
                vacioANull(r.telefono()), vacioANull(r.empresa()), r.cuenta().trim(), vacioANull(r.agencia()), r.puestoSolicitado().trim(),
                cita, vacioANull(r.callbackUrl()), SolicitudExamen.PENDIENTE, caduca, null, null, null, null, null, null, null, null);
        long id = repo.crear(nueva, request == null ? null : request.getRemoteAddr());
        SolicitudExamen creada = repo.porId(id).orElseThrow();

        String citaTxt = cita == null ? "" : " (cita " + cita.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ")";
        notificaciones.crear("EXAMEN_INICIAL_SOLICITUD", "Examen médico inicial pendiente",
                "OCR solicita examen inicial de " + creada.nombreCompleto() + citaTxt + " · " + creada.puestoSolicitado(),
                nss, "nss?nss=" + nss + "&abrir=examen-inicial", "EXAMEN_INICIAL", null, ORIGEN_OCR, referencia);
        auditar("create", "Solicitud examen inicial", String.valueOf(id), "referencia=" + referencia + " nss=" + nss, request);
        log.info("[ocr] solicitud {} creada para NSS {} (ref {})", id, nss, referencia);
        return new Creada(creada, false);
    }

    public Optional<SolicitudExamen> porId(long id) {
        return repo.porId(id);
    }

    public Optional<SolicitudExamen> porReferencia(String referencia) {
        return repo.porReferencia(ORIGEN_OCR, referencia);
    }

    /** Solicitud viva del NSS, para la ficha y el aviso "pendiente". Nunca lanza. */
    public Optional<SolicitudExamen> activaPorNss(String nss) {
        try {
            return repo.activaPorNss(nss.trim());
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    public Optional<SolicitudExamen> ultimaPorNss(String nss) {
        try {
            return repo.ultimaPorNss(nss.trim());
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    public List<SolicitudExamen> listar(String estatus) {
        return repo.listar(estatus, 200);
    }

    /** Cancelacion desde OCR. false si ya estaba FINALIZADO (409). */
    @Transactional
    public boolean cancelar(SolicitudExamen s, String motivo, HttpServletRequest request) {
        if (SolicitudExamen.FINALIZADO.equals(s.estatus())) {
            return false;
        }
        repo.cancelar(s.id(), limpio(motivo).isEmpty() ? "Cancelada por OCR" : motivo.trim());
        notificaciones.marcarLeidasPorReferencia(ORIGEN_OCR, s.referencia());
        auditar("update", "Solicitud examen inicial", String.valueOf(s.id()), "cancelar motivo=" + motivo, request);
        return true;
    }

    /** El medico abrio el examen inicial de ese NSS: PENDIENTE -> EN_CAPTURA. Nunca lanza. */
    public void enCaptura(String nss) {
        try {
            repo.activaPorNss(nss).ifPresent(s -> repo.marcarEnCaptura(s.id()));
        } catch (RuntimeException ex) {
            log.debug("[ocr] sin solicitud en captura para {}: {}", nss, ex.getMessage());
        }
    }

    /** Examen inicial finalizado: cierra la solicitud viva del NSS y programa el webhook. */
    @EventListener
    public void alFinalizar(ExamenInicialService.ExamenInicialFinalizado ev) {
        try {
            Optional<SolicitudExamen> activa = repo.activaPorNss(ev.nss());
            if (activa.isEmpty()) {
                return;
            }
            Long historicoId = historico.ultimo(ev.nss()).map(ExamenHistoricoRepository.Registro::id).orElse(null);
            repo.finalizar(activa.get().id(), ev.dictamen(), historicoId);
            notificaciones.marcarLeidasPorReferencia(ORIGEN_OCR, activa.get().referencia());
            SolicitudExamen cerrada = repo.porId(activa.get().id()).orElseThrow();
            webhook.programar(cerrada);
        } catch (RuntimeException ex) {
            // el examen ya quedo guardado; esto solo afecta el aviso a OCR, que se puede reenviar a mano
            log.warn("[ocr] no se pudo cerrar/avisar la solicitud de {}: {}", ev.nss(), ex.getMessage());
        }
    }

    /** Tarea diaria: solicitudes vencidas -> CANCELADO (CADUCIDAD). */
    public int caducarVencidas() {
        int n = repo.caducarVencidas();
        if (n > 0) {
            log.info("[ocr] {} solicitudes caducadas", n);
        }
        return n;
    }

    // ---- validacion -----------------------------------------------------------------

    private static final Map<String, String> OBLIGATORIOS = Map.of(
            "nss", "nss", "curp", "curp", "nombre", "nombre", "apellidoPaterno", "apellidoPaterno",
            "apellidoMaterno", "apellidoMaterno", "puestoSolicitado", "puestoSolicitado", "cuenta", "cuenta");

    private static List<String> validar(SolicitudRequest r) {
        List<String> e = new ArrayList<>();
        if (r == null) {
            e.add("cuerpo: vacío");
            return e;
        }
        if (limpio(r.nss()).isEmpty()) {
            e.add("nss: obligatorio");
        } else if (!r.nss().trim().matches("\\d{10,11}")) {
            e.add("nss: deben ser 10 u 11 dígitos");
        }
        if (limpio(r.curp()).isEmpty()) {
            e.add("curp: obligatorio");
        } else if (!r.curp().trim().matches("(?i)[A-Z]{4}\\d{6}[HM][A-Z]{5}[A-Z0-9]\\d")) {
            e.add("curp: formato inválido (18 caracteres)");
        }
        for (String campo : List.of("nombre", "apellidoPaterno", "apellidoMaterno", "puestoSolicitado", "cuenta")) {
            String v = switch (campo) {
                case "nombre" -> r.nombre();
                case "apellidoPaterno" -> r.apellidoPaterno();
                case "apellidoMaterno" -> r.apellidoMaterno();
                case "puestoSolicitado" -> r.puestoSolicitado();
                default -> r.cuenta();
            };
            if (limpio(v).isEmpty()) {
                e.add(campo + ": obligatorio");
            }
        }
        if (!limpio(r.fechaNacimiento()).isEmpty() && fecha(r.fechaNacimiento()) == null) {
            e.add("fechaNacimiento: formato esperado yyyy-MM-dd");
        }
        if (!limpio(r.fechaCita()).isEmpty() && fecha(r.fechaCita()) == null) {
            e.add("fechaCita: formato esperado yyyy-MM-dd");
        }
        String sexo = limpio(r.sexo()).toUpperCase();
        if (!sexo.isEmpty() && !sexo.equals("M") && !sexo.equals("F")) {
            e.add("sexo: valores permitidos M o F");
        }
        if (!limpio(r.callbackUrl()).isEmpty() && !r.callbackUrl().trim().toLowerCase().startsWith("https://")
                && !r.callbackUrl().trim().toLowerCase().startsWith("http://")) {
            e.add("callbackUrl: debe ser una URL http(s)");
        }
        return e;
    }

    private static LocalDate fecha(String v) {
        if (v == null || v.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(v.trim().substring(0, Math.min(10, v.trim().length())));
        } catch (DateTimeParseException | StringIndexOutOfBoundsException ex) {
            return null;
        }
    }

    static LocalDate nacimientoDesdeCurp(String curp) {
        try {
            int aa = Integer.parseInt(curp.substring(4, 6));
            int mm = Integer.parseInt(curp.substring(6, 8));
            int dd = Integer.parseInt(curp.substring(8, 10));
            // el caracter 17 es digito para nacidos antes de 2000 y letra a partir de 2000
            int siglo = Character.isDigit(curp.charAt(16)) ? 1900 : 2000;
            return LocalDate.of(siglo + aa, mm, dd);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static String limpio(String v) {
        return v == null ? "" : v.trim();
    }

    private static String vacioANull(String v) {
        String t = limpio(v);
        return t.isEmpty() ? null : t;
    }

    private void auditar(String accion, String entidad, String registro, String detalle, HttpServletRequest request) {
        try {
            auditoria.registrar("Integracion OCR", accion, entidad, registro, detalle, request);
        } catch (RuntimeException ex) {
            log.debug("[ocr] auditoria no registrada: {}", ex.getMessage());
        }
    }
}
