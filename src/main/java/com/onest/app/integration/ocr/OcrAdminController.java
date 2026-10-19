package com.onest.app.integration.ocr;

import com.onest.app.audit.web.Auditado;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Seguimiento de la integracion OCR para el administrador (/admin/**, rol ADMIN): solicitudes
 * por estatus, intentos del webhook y reenvio manual cuando OCR no contesto.
 */
@Controller
public class OcrAdminController {

    private final SolicitudExamenService solicitudes;
    private final EnvioWebhookRepository envios;
    private final OcrWebhookService webhook;
    private final OcrProperties props;

    public OcrAdminController(SolicitudExamenService solicitudes, EnvioWebhookRepository envios, OcrWebhookService webhook,
                              OcrProperties props) {
        this.solicitudes = solicitudes;
        this.envios = envios;
        this.webhook = webhook;
        this.props = props;
    }

    @GetMapping("/admin/integracion-ocr")
    public String pagina(@RequestParam(name = "estatus", required = false) String estatus, Model model) {
        List<SolicitudExamen> lista;
        boolean disponible = true;
        try {
            lista = solicitudes.listar(estatus);
        } catch (org.springframework.dao.DataAccessException ex) {
            lista = List.of();
            disponible = false;
        }
        model.addAttribute("solicitudes", lista);
        model.addAttribute("estatus", estatus == null ? "" : estatus);
        model.addAttribute("disponible", disponible);
        model.addAttribute("habilitada", props.habilitada());
        model.addAttribute("callbackConfigurado", props.callbackUrl() != null && !props.callbackUrl().isBlank());
        return "pages/admin-integracion-ocr";
    }

    @GetMapping(path = "/api/admin/integracion-ocr/solicitudes/{id}/envios", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<Map<String, Object>> enviosDe(@PathVariable("id") long id) {
        return envios.porSolicitud(id).stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", e.id());
            m.put("intento", e.intento());
            m.put("estatus", e.estatus());
            m.put("programado", e.programadoAt().toString());
            m.put("enviado", e.enviadoAt() == null ? null : e.enviadoAt().toString());
            m.put("httpCode", e.httpCode());
            m.put("respuesta", e.respuesta());
            m.put("url", e.url());
            return m;
        }).toList();
    }

    @PostMapping(path = "/api/admin/integracion-ocr/solicitudes/{id}/reenviar", produces = "text/plain;charset=UTF-8")
    @ResponseBody
    @Auditado(modulo = "Integracion OCR", accion = "update", entidad = "Reenvio webhook", registro = "id")
    public String reenviar(@PathVariable("id") long id) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        webhook.reenviar(id, auth == null ? "ADMIN" : auth.getName());
        return "Reenvío programado: se intenta en el próximo minuto.";
    }
}
