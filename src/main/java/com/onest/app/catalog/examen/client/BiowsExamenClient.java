package com.onest.app.catalog.examen.client;

import com.onest.app.catalog.examen.client.dto.BiowsExamenRequest;
import com.onest.app.catalog.examen.client.dto.BiowsExamenResponse;
import com.onest.app.catalog.expediente.client.dto.BiowsProcesoResponse;
import com.onest.app.config.BiowsProperties;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Implementacion del gateway del examen contra el WS ORDS. Aplana la estructura
 * anidada Data.<SECCION>.<CAMPO> a claves punteadas para consumo simple desde las plantillas.
 */
@Component
public class BiowsExamenClient implements ExamenClient {

    private static final Logger log = LoggerFactory.getLogger(BiowsExamenClient.class);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm:ss");
    private static final String PATH = "/Servcio/consulta_examen";
    private static final String PATH_SAVE = "/Servcio/Medico";

    private final RestClient biowsRestClient;
    private final BiowsProperties properties;

    public BiowsExamenClient(RestClient biowsRestClient, BiowsProperties properties) {
        this.biowsRestClient = biowsRestClient;
        this.properties = properties;
    }

    @Override
    public Map<String, String> getExamenData(String nss) {
        log.info("[biows] POST {}{} NSS={}", properties.baseUrl(), PATH, nss);
        BiowsExamenResponse response = biowsRestClient.post()
                .uri(PATH)
                .body(new BiowsExamenRequest(nss, LocalDateTime.now().format(FECHA), usuarioActual()))
                .retrieve()
                .body(BiowsExamenResponse.class);

        Map<String, String> flat = new LinkedHashMap<>();
        if (response == null || response.datos() == null || response.datos().isEmpty()) {
            return flat;
        }
        Map<String, Object> data = response.datos().get(0).data();
        if (data != null) {
            flatten(null, data, flat);
        }
        return flat;
    }

    @Override
    public String guardar(String nss, Map<String, String> campos, String firma) {
        // Reconstruye el payload anidado: agrupa por seccion (parte antes del primer '.').
        Map<String, Map<String, Object>> secciones = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : campos.entrySet()) {
            String key = e.getKey();
            int dot = key.indexOf('.');
            if (dot < 0) {
                continue;
            }
            String seccion = key.substring(0, dot);
            String field = key.substring(dot + 1);
            Map<String, Object> obj = secciones.computeIfAbsent(seccion, k -> new LinkedHashMap<>());

            // Dictamen (apto/no apto/...) -> 4 campos booleanos que espera el WS.
            if ("SERV_MED_RESULTADO_EXAMEN".equals(seccion) && "DICTAMEN".equals(field)) {
                String v = e.getValue();
                obj.put("APTO", "apto".equals(v) ? "1" : "");
                obj.put("NO_APTO", "no_apto".equals(v) ? "1" : "");
                obj.put("APTO_CONDICIONADO", "apto_condicionado".equals(v) ? "1" : "");
                obj.put("APTO_RESTRINGIDO", "apto_restringido".equals(v) ? "1" : "");
                continue;
            }
            obj.put(writeKey(field), e.getValue());
        }

        if (firma != null && !firma.isBlank()) {
            secciones.computeIfAbsent("SERV_MED_RESULTADO_EXAMEN", k -> new LinkedHashMap<>())
                    .put("FIRMA_DIGITAL", firma);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("NNS", nss);
        payload.put("FECHA", LocalDateTime.now().format(FECHA));
        payload.put("TIPO_REGISTROS", "CAMBIO");
        payload.put("USUARIO_CAMBIO", usuarioActual());
        payload.putAll(secciones);

        log.info("[biows] POST {}{} NSS={} secciones={}", properties.baseUrl(), PATH_SAVE, nss, secciones.size());
        BiowsProcesoResponse response = biowsRestClient.post()
                .uri(PATH_SAVE)
                .body(payload)
                .retrieve()
                .body(BiowsProcesoResponse.class);

        if (response == null || response.datos() == null || response.datos().isEmpty()) {
            return "";
        }
        return response.datos().get(0).proceso();
    }

    /**
     * Ajuste de clave de lectura -> escritura para los typos/inconsistencias del WS legacy.
     * Casos claros (typo). Los de espacio final (SABIN/LUMBALGIA vs INFARTOS/ALT_CARGA) son
     * inconsistentes en el legacy -> se dejan tal cual; VERIFICAR contra el WS real.
     */
    private static String writeKey(String field) {
        return switch (field) {
            case "BAÑO" -> "BANO";
            case "TABIQUE_NASA" -> "TABIQUE_NASAL";
            case "CERVICA" -> "CERVICAL";
            default -> field;
        };
    }

    @SuppressWarnings("unchecked")
    private static void flatten(String prefix, Map<String, Object> node, Map<String, String> out) {
        for (Map.Entry<String, Object> e : node.entrySet()) {
            String key = prefix == null ? e.getKey() : prefix + "." + e.getKey();
            Object value = e.getValue();
            if (value instanceof Map<?, ?> m) {
                flatten(key, (Map<String, Object>) m, out);
            } else if (value != null && !(value instanceof Iterable)) {
                out.put(key, String.valueOf(value));
            }
            // listas (p.ej. antecedentes laborales) se omiten en esta fase
        }
    }

    private String usuarioActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return "SISTEMA";
        }
        return authentication.getName();
    }
}
