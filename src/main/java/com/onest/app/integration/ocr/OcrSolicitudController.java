package com.onest.app.integration.ocr;

import com.onest.app.catalog.examen.service.ExamenDocumentoService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * API que consume OCR (docs/integracion-ocr-examen-inicial.md, seccion 1). Autenticada por
 * OcrApiKeyFilter. Los codigos y cuerpos son los del contrato: 201/200/400/401/409/503.
 */
@Controller
@RequestMapping("/api/integracion/ocr/examen-inicial")
public class OcrSolicitudController {

    private static final Logger log = LoggerFactory.getLogger(OcrSolicitudController.class);

    private final SolicitudExamenService service;
    private final OcrProperties props;
    private final ExamenDocumentoService documentos;

    public OcrSolicitudController(SolicitudExamenService service, OcrProperties props, ExamenDocumentoService documentos) {
        this.service = service;
        this.props = props;
        this.documentos = documentos;
    }

    @PostMapping(path = "/solicitudes", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> crear(@RequestBody SolicitudExamenService.SolicitudRequest body, HttpServletRequest request) {
        SolicitudExamenService.Creada c = service.crear(body, request);
        return ResponseEntity.status(c.existia() ? HttpStatus.OK : HttpStatus.CREATED).body(cuerpo(c.solicitud()));
    }

    @GetMapping(path = "/solicitudes/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> porId(@PathVariable("id") long id) {
        return service.porId(id).map(s -> ResponseEntity.ok(cuerpo(s)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("NO_EXISTE", null)));
    }

    @GetMapping(path = "/solicitudes", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> porReferencia(@RequestParam("referenciaOcr") String referencia) {
        return service.porReferencia(referencia.trim()).map(s -> ResponseEntity.ok(cuerpo(s)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("NO_EXISTE", null)));
    }

    @PostMapping(path = "/solicitudes/{id}/cancelar", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cancelar(@PathVariable("id") long id,
                                                        @RequestBody(required = false) Map<String, String> body,
                                                        HttpServletRequest request) {
        Optional<SolicitudExamen> s = service.porId(id);
        if (s.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("NO_EXISTE", null));
        }
        String motivo = body == null ? null : body.get("motivo");
        if (!service.cancelar(s.get(), motivo, request)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error("YA_FINALIZADO", null));
        }
        SolicitudExamen c = service.porId(id).orElseThrow();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("solicitudId", c.id());
        m.put("estatus", c.estatus());
        m.put("motivo", c.motivoCancel());
        m.put("cancelado", iso(c.canceladoAt()));
        return ResponseEntity.ok(m);
    }

    /** Expediente impreso del examen (mismo documento que "Imprimir expediente"), para el documentoUrl del webhook. */
    @GetMapping(path = "/solicitudes/{id}/documento", produces = MediaType.TEXT_HTML_VALUE)
    public String documento(@PathVariable("id") long id, Model model) {
        SolicitudExamen s = service.porId(id).orElseThrow(() -> new IllegalArgumentException("Solicitud no existe"));
        if (!SolicitudExamen.FINALIZADO.equals(s.estatus())) {
            throw new IllegalStateException("La solicitud no esta finalizada");
        }
        model.addAttribute("d", documentos.armar(s.nss()));
        model.addAttribute("auto", false);
        return "pages/examen-documento";
    }

    // ---- cuerpos del contrato ------------------------------------------------------

    private Map<String, Object> cuerpo(SolicitudExamen s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("solicitudId", s.id());
        m.put("referenciaOcr", s.referencia());
        m.put("nss", s.nss());
        m.put("estatus", s.estatus());
        m.put("recibido", iso(s.createdAt()));
        m.put("caduca", s.caduca() == null ? null : s.caduca().toString());
        if (s.fechaCita() != null) {
            m.put("fechaCita", s.fechaCita().toString());
        }
        if (SolicitudExamen.FINALIZADO.equals(s.estatus())) {
            m.put("fechaExamen", iso(s.fechaExamen()));
            m.put("dictamen", OcrWebhookService.codigo(s.dictamen()));
            m.put("notificado", s.notificadoAt() != null);
        }
        if (SolicitudExamen.CANCELADO.equals(s.estatus())) {
            m.put("motivo", s.motivoCancel());
            m.put("cancelado", iso(s.canceladoAt()));
        }
        String base = props.publicBaseUrl();
        if (base != null && !base.isBlank()) {
            m.put("consulta", base.replaceAll("/$", "") + "/api/integracion/ocr/examen-inicial/solicitudes/" + s.id());
        }
        return m;
    }

    private static Map<String, Object> error(String codigo, String mensaje) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("error", codigo);
        if (mensaje != null) {
            m.put("mensaje", mensaje);
        }
        return m;
    }

    private static String iso(LocalDateTime t) {
        return t == null ? null : t.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    // ---- errores -> codigos del contrato ---------------------------------------------

    @ExceptionHandler(SolicitudExamenService.SolicitudInvalida.class)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> invalida(SolicitudExamenService.SolicitudInvalida ex) {
        Map<String, Object> m = error("SOLICITUD_INVALIDA", null);
        m.put("detalles", ex.detalles());
        return ResponseEntity.badRequest().body(m);
    }

    @ExceptionHandler(SolicitudExamenService.ExamenVigente.class)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> vigente(SolicitudExamenService.ExamenVigente ex) {
        Map<String, Object> m = error("EXAMEN_VIGENTE", ex.getMessage());
        m.put("dictamenVigente", OcrWebhookService.codigo(ex.dictamen()));
        m.put("fechaExamen", ex.fecha());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(m);
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> jsonMalFormado(Exception ex) {
        Map<String, Object> m = error("SOLICITUD_INVALIDA", null);
        m.put("detalles", List.of("cuerpo: JSON mal formado"));
        return ResponseEntity.badRequest().body(m);
    }

    @ExceptionHandler(DataAccessException.class)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> baseCaida(DataAccessException ex) {
        log.error("[ocr] base del portal no disponible: {}", ex.getMostSpecificCause().getMessage());
        Map<String, Object> m = error("NO_DISPONIBLE", null);
        m.put("reintentarEn", 300);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(m);
    }
}
