package com.onest.app.integration.ocr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onest.app.catalog.restriccion.dto.RestriccionDto;
import com.onest.app.catalog.restriccion.service.RestriccionService;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Webhook Portal -> OCR al finalizar un examen inicial (docs/integracion-ocr-examen-inicial.md).
 * El cuerpo se arma UNA vez al programar (queda en SERV_MED_INTEGRACION_ENVIO) y se firma en cada
 * intento con HMAC-SHA256(secreto, tiempo + "." + cuerpo). Reintentos 1, 5, 15, 60 min y 6 h;
 * despues queda AGOTADO y se puede reenviar a mano desde /admin/integracion-ocr.
 */
@Service
public class OcrWebhookService {

    private static final Logger log = LoggerFactory.getLogger(OcrWebhookService.class);
    static final String EVENTO = "EXAMEN_INICIAL_FINALIZADO";
    /** Minutos de espera antes del intento n+1 (indice = intento fallido - 1). */
    private static final int[] ESPERA_MIN = {1, 5, 15, 60, 360};

    private final EnvioWebhookRepository envios;
    private final SolicitudExamenRepository solicitudes;
    private final RestriccionService restricciones;
    private final OcrProperties props;
    private final ObjectMapper json = new ObjectMapper();
    private final RestClient http;

    public OcrWebhookService(EnvioWebhookRepository envios, SolicitudExamenRepository solicitudes,
                             RestriccionService restricciones, OcrProperties props) {
        this.envios = envios;
        this.solicitudes = solicitudes;
        this.restricciones = restricciones;
        this.props = props;
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(Duration.ofSeconds(5));
        f.setReadTimeout(Duration.ofSeconds(10));
        this.http = RestClient.builder().requestFactory(f).build();
    }

    /** Arma el cuerpo y deja el primer intento programado para ahora. */
    public void programar(SolicitudExamen s) {
        String url = s.callbackUrl() != null && !s.callbackUrl().isBlank() ? s.callbackUrl() : props.callbackUrl();
        if (url == null || url.isBlank()) {
            log.warn("[ocr] solicitud {} finalizada sin URL de webhook (ni en la solicitud ni configurada): no se avisa", s.id());
            return;
        }
        String cuerpo = cuerpo(s);
        envios.programar(s.id(), EVENTO, url, cuerpo, 1, LocalDateTime.now(), "SISTEMA");
    }

    /** Reenvio manual: nuevo intento inmediato con el cuerpo actual de la solicitud. */
    public void reenviar(long solicitudId, String usuario) {
        SolicitudExamen s = solicitudes.porId(solicitudId).orElseThrow(() -> new IllegalArgumentException("Solicitud no existe"));
        String url = s.callbackUrl() != null && !s.callbackUrl().isBlank() ? s.callbackUrl() : props.callbackUrl();
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("La solicitud no tiene URL de webhook y no hay una configurada");
        }
        int intento = envios.porSolicitud(solicitudId).size() + 1;
        envios.programar(solicitudId, EVENTO, url, cuerpo(s), intento, LocalDateTime.now(), usuario);
    }

    /** Cada minuto: manda los envios cuya hora llego. */
    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void procesarPendientes() {
        List<EnvioWebhookRepository.Envio> lote;
        try {
            lote = envios.pendientesVencidos(20);
        } catch (RuntimeException ex) {
            return; // sin tabla o sin base: se reintenta en el siguiente ciclo
        }
        for (EnvioWebhookRepository.Envio e : lote) {
            enviar(e);
        }
    }

    private void enviar(EnvioWebhookRepository.Envio e) {
        String cuerpo = envios.cuerpo(e.id());
        if (cuerpo == null) {
            envios.resultado(e.id(), "FALLO", null, "sin cuerpo");
            return;
        }
        String tiempo = String.valueOf(Instant.now().getEpochSecond());
        String firma = "sha256=" + hmac(tiempo + "." + cuerpo);
        try {
            var resp = http.post().uri(URI.create(e.url()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Firma-Tiempo", tiempo)
                    .header("X-Firma", firma)
                    .header("X-Evento", e.evento())
                    .body(cuerpo)
                    .retrieve()
                    .toEntity(String.class);
            envios.resultado(e.id(), "OK", resp.getStatusCode().value(), resp.getBody());
            solicitudes.marcarNotificada(e.solicitudId());
            log.info("[ocr] webhook solicitud {} intento {} -> {}", e.solicitudId(), e.intento(), resp.getStatusCode().value());
        } catch (RestClientResponseException ex) {
            fallo(e, cuerpo, ex.getStatusCode().value(), ex.getResponseBodyAsString());
        } catch (RuntimeException ex) {
            fallo(e, cuerpo, null, ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    private void fallo(EnvioWebhookRepository.Envio e, String cuerpo, Integer code, String respuesta) {
        boolean agotado = e.intento() > ESPERA_MIN.length;
        envios.resultado(e.id(), agotado ? "AGOTADO" : "FALLO", code, respuesta);
        log.warn("[ocr] webhook solicitud {} intento {} fallo ({}): {}", e.solicitudId(), e.intento(), code, respuesta);
        if (!agotado) {
            int minutos = ESPERA_MIN[Math.min(e.intento(), ESPERA_MIN.length) - 1];
            envios.programar(e.solicitudId(), e.evento(), e.url(), cuerpo, e.intento() + 1, LocalDateTime.now().plusMinutes(minutos), "SISTEMA");
        }
    }

    /** Cuerpo del contrato: dictamen + restricciones (el diagnostico ICD espera a Juridico). */
    String cuerpo(SolicitudExamen s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("evento", EVENTO);
        m.put("solicitudId", s.id());
        m.put("referenciaOcr", s.referencia());
        m.put("nss", s.nss());
        m.put("curp", s.curp());
        m.put("fechaExamen", s.fechaExamen() == null ? null
                : s.fechaExamen().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        m.put("dictamen", codigo(s.dictamen()));
        m.put("restricciones", restriccionesDe(s));
        m.put("documentoUrl", props.publicBaseUrl() == null || props.publicBaseUrl().isBlank() ? null
                : props.publicBaseUrl().replaceAll("/$", "") + "/api/integracion/ocr/examen-inicial/solicitudes/" + s.id() + "/documento");
        try {
            return json.writeValueAsString(m);
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new IllegalStateException(ex);
        }
    }

    static String codigo(String dictamen) {
        return switch (dictamen == null ? "" : dictamen) {
            case "apto" -> "APTO";
            case "no_apto" -> "NO_APTO";
            case "apto_condicionado" -> "APTO_CONDICIONADO";
            case "apto_restringido" -> "APTO_RESTRINGIDO";
            default -> dictamen == null ? "" : dictamen.toUpperCase();
        };
    }

    private List<Map<String, String>> restriccionesDe(SolicitudExamen s) {
        List<Map<String, String>> out = new ArrayList<>();
        if (!"apto_restringido".equals(s.dictamen())) {
            return out;
        }
        try {
            for (RestriccionDto r : restricciones.byNss(s.nss())) {
                if (!"ACTIVA".equalsIgnoreCase(r.estatus())) {
                    continue;
                }
                Map<String, String> x = new LinkedHashMap<>();
                x.put("codigo", r.codigoRestriccion());
                x.put("descripcion", r.descripcion());
                pon(x, "valorLimite", r.valorLimite());
                pon(x, "unidad", r.unidad());
                pon(x, "temporalidad", r.temporalidad());
                pon(x, "fechaFin", r.fechaFin());
                out.add(x);
            }
        } catch (RuntimeException ex) {
            log.warn("[ocr] sin restricciones para {}: {}", s.nss(), ex.getMessage());
        }
        return out;
    }

    private static void pon(Map<String, String> m, String k, String v) {
        if (v != null && !v.isBlank()) {
            m.put(k, v);
        }
    }

    String hmac(String texto) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(props.hmacSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException ex) {
            throw new IllegalStateException("No se pudo firmar el webhook", ex);
        }
    }
}
