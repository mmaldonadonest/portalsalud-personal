package com.onest.app.catalog.examen.client;

import com.onest.app.catalog.examen.client.dto.BiowsExamenReporteRequest;
import com.onest.app.catalog.examen.client.dto.BiowsExamenReporteResponse;
import com.onest.app.catalog.examen.client.dto.BiowsExamenRequest;
import com.onest.app.catalog.examen.client.dto.BiowsExamenResponse;
import com.onest.app.catalog.examen.dto.ExamenReporteDto;
import com.onest.app.catalog.expediente.client.dto.BiowsProcesoResponse;
import com.onest.app.config.BiowsProperties;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private static final String PATH_REPORTE_FECHA = "/Servcio/consulta_examen_fecha";
    private static final Pattern TRABAJO_KEY = Pattern.compile("^trabajos\\[(\\d+)]\\.(.+)$");

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
        // trabajos[N].campo (antecedentes laborales - lista de empleos previos) se arma aparte,
        // no es una "seccion" con nombre fijo sino un arreglo de filas. Ver comentario en guardar().
        Map<Integer, Map<String, Object>> trabajosPorIndice = new TreeMap<>();
        for (Map.Entry<String, String> e : campos.entrySet()) {
            String key = e.getKey();
            Matcher trabajo = TRABAJO_KEY.matcher(key);
            if (trabajo.matches()) {
                int idx = Integer.parseInt(trabajo.group(1));
                trabajosPorIndice.computeIfAbsent(idx, k -> new LinkedHashMap<>())
                        .put(trabajo.group(2), e.getValue());
                continue;
            }
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

        // CONFIRMADO 2026-08-13 leyendo el PL/SQL real (docs/contextoWS.txt:4891-4900): el WS
        // cuenta las filas con apex_json.get_count(p_path=>'SERV_ANTECEDENTESLAB.trabajos', ...)
        // (arreglo ANIDADO dentro de la seccion) pero lee cada fila con 'trabajos[%d].campo' SIN
        // ese prefijo (arreglo en la RAIZ). Son dos ubicaciones JSON distintas: con "trabajos"
        // solo en la raiz (como se enviaba antes), el conteo siempre da 0 y el bloque completo
        // se salta - por eso SERV_MED_DET_ANT_LABORALES tiene 51,730 filas historicas, todas
        // NULL (ni el legacy PHP logro nunca guardar esto). Fix: mandar el arreglo en AMBOS
        // lugares para satisfacer el conteo y la lectura.
        List<Map<String, Object>> trabajos = new ArrayList<>();
        for (Map<String, Object> fila : trabajosPorIndice.values()) {
            boolean vacia = fila.values().stream().allMatch(v -> v == null || String.valueOf(v).isBlank());
            if (!vacia) {
                trabajos.add(fila);
            }
        }
        if (!trabajos.isEmpty()) {
            payload.put("trabajos", trabajos);
            // secciones ya se copio a payload arriba (putAll); "SERV_ANTECEDENTESLAB" puede no
            // haber existido todavia en ese momento, asi que se re-inserta explicitamente en
            // payload (no basta con mutar el mapa de "secciones").
            Map<String, Object> antecedentesLab =
                    secciones.computeIfAbsent("SERV_ANTECEDENTESLAB", k -> new LinkedHashMap<>());
            antecedentesLab.put("trabajos", trabajos);
            payload.put("SERV_ANTECEDENTESLAB", antecedentesLab);
        }

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

    @Override
    public List<ExamenReporteDto> reportePorFecha(String fechaInicial, String fechaFinal) {
        log.info("[biows] POST {}{} {} - {}", properties.baseUrl(), PATH_REPORTE_FECHA, fechaInicial, fechaFinal);
        BiowsExamenReporteResponse response = biowsRestClient.post()
                .uri(PATH_REPORTE_FECHA)
                .body(new BiowsExamenReporteRequest(fechaInicial, fechaFinal))
                .retrieve()
                .body(BiowsExamenReporteResponse.class);

        if (response == null || response.datos() == null) {
            return List.of();
        }
        return response.datos().stream().map(BiowsExamenClient::toReporte).toList();
    }

    private static ExamenReporteDto toReporte(BiowsExamenReporteResponse.Dato d) {
        return new ExamenReporteDto(
                d.idRegistro(), d.fechaRegistro(), d.nss(), d.nombre(), d.rfc(), d.curp(),
                d.apto(), d.noApto(), d.aptoCondicionado(), d.aptoRestringido());
    }
}
