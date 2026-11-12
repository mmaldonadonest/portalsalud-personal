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
    // E-032 (11-nov-2026): clon de consulta_examen que llama a PR_SERVICIO_MED_CONSULTA_V2: regresa ademas
    // NEUROLOGIA.AVC_OBS, HEREDOFAMILIARES.NEFROPATIAS_ID y SERV_MED_EXPLORACION_FISICA.NOLMAL_FASCIES (se
    // guardaban y nunca se veian) y ASMA con su propia columna (el original repetia TUBERCULOSIS).
    // docs/script-prod/e032-clon-consulta-examen-v2.sql. Mientras no exista (404) se lee el original.
    private static final String PATH_V2 = "/Servcio/consulta_examen_v2";
    private volatile boolean sinV2 = false;
    // E-024 (9-nov-2026): clon de Servcio/Medico que llama a PR_SERVICIO_MED_EXAMEN1/2_V2, iguales al
    // original pero anotan el error real de Oracle en SERV_MED_LOG_PROC (el original solo responde
    // "Problema en el procedimiento"). El original sigue publicado e intacto.
    // docs/script-prod/e024-bitacora-examen-medico-v2.sql DEBE correrse antes de subir este WAR.
    private static final String PATH_SAVE = "/Servcio/Medico_v2";
    // _cta = clon del WS original que ademas devuelve CUENTA por registro (10-sep-2026,
    // docs/ords-cuenta-en-reportes.sql). El original sigue publicado e intacto.
    private static final String PATH_REPORTE_FECHA = "/Servcio/consulta_examen_fecha_cta";
    private static final Pattern TRABAJO_KEY = Pattern.compile("^trabajos\\[(\\d+)]\\.(.+)$");

    private final RestClient biowsRestClient;
    /** Mismo ORDS, pero espera hasta 120 s: solo para guardar (ver BiowsClientConfig). */
    private final RestClient restClientGuardado;
    private final BiowsProperties properties;

    public BiowsExamenClient(RestClient biowsRestClient,
                             @org.springframework.beans.factory.annotation.Qualifier("biowsRestClientGuardadoLargo")
                             RestClient restClientGuardado,
                             BiowsProperties properties) {
        this.biowsRestClient = biowsRestClient;
        this.restClientGuardado = restClientGuardado;
        this.properties = properties;
    }

    @Override
    public Map<String, String> getExamenData(String nss) {
        BiowsExamenResponse response = null;
        if (!sinV2) {
            try {
                response = consultar(PATH_V2, nss);
            } catch (org.springframework.web.client.HttpClientErrorException.NotFound ex) {
                // el clon aun no esta publicado en ORDS: se usa el original y no se vuelve a intentar
                sinV2 = true;
                log.warn("[biows] {} no existe (404): se lee {} (sin AVC_OBS, NEFROPATIAS_ID ni NOLMAL_FASCIES)",
                        PATH_V2, PATH);
            }
        }
        if (sinV2) {
            response = consultar(PATH, nss);
        }

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

    private BiowsExamenResponse consultar(String path, String nss) {
        log.info("[biows] POST {}{} NSS={}", properties.baseUrl(), path, nss);
        return biowsRestClient.post()
                .uri(path)
                .body(new BiowsExamenRequest(nss, LocalDateTime.now().format(FECHA), usuarioActual()))
                .retrieve()
                .body(BiowsExamenResponse.class);
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

        // PR_SERVICIO_MED_EXAMEN1/2 (docs/contextoWS.txt) hacen UPDATE de TODAS las tablas del
        // examen en cada guardado, con lo que venga en el JSON: un campo ausente llega NULL y
        // pisa el valor guardado, y una seccion ausente pisa la tabla completa. Confirmado
        // 13-sep-2026: guardar solo "Estudios realizados" borro la FIRMA_DIGITAL de
        // SERV_MED_RESULTADO_EXAMEN. El PHP no lo sufria porque siempre mandaba el examen
        // completo. Por eso aqui se manda SIEMPRE el estado completo: lo que el formulario no
        // trae se rellena con el valor vigente del WS (los textos de relleno del coalesce
        // - "sin obs...", "sin datos", "sin firma" - se mandan vacios para no persistirlos).
        Map<String, String> vigente;
        try {
            vigente = getExamenData(nss);
        } catch (RuntimeException ex) {
            log.warn("[biows] no se pudo leer el examen vigente de {} antes de guardar: {}", nss, ex.getMessage());
            vigente = Map.of();
        }
        java.util.Set<String> clavesEscritura = com.onest.app.catalog.examen.service.ExamenService.clavesEscritura();
        for (Map.Entry<String, String> e : vigente.entrySet()) {
            if (e.getKey().indexOf('.') < 0 || e.getKey().startsWith("SERV_ANTECEDENTESLAB.trabajos")) {
                continue;
            }
            // la lectura trae claves con espacios/typos distintos a los de escritura: se
            // reconcilian contra el catalogo para que el proc reciba el nombre que espera
            String key = com.onest.app.catalog.examen.service.ExamenClaves.aEscritura(e.getKey(), clavesEscritura);
            int dot = key.indexOf('.');
            String seccion = key.substring(0, dot);
            String field = writeKey(key.substring(dot + 1));
            Map<String, Object> obj = secciones.computeIfAbsent(seccion, k -> new LinkedHashMap<>());
            if (!obj.containsKey(field)) {
                obj.put(field, esRellenoWs(e.getValue()) ? "" : e.getValue());
            }
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
        // Se lee como texto y se interpreta aqui, en vez de dejarlo al convertidor: asi, si ORDS
        // responde algo que no es JSON (pagina de error, etiqueta rara), queda en el log lo que
        // respondio de verdad. El 9-oct-2026 solo quedo "Error while extracting response".
        long inicio = System.currentTimeMillis();
        // Se leen los bytes crudos con exchange() y no con body(String.class): con la etiqueta
        // application/json el convertidor de Jackson (primero de la lista) intentaba convertir el
        // objeto JSON en un String y fallaba (500 del 10-oct 17:39, con ORDS ya respondiendo bien).
        String crudo = restClientGuardado.post()
                .uri(PATH_SAVE)
                .body(payload)
                .exchange((req, res) -> {
                    byte[] bytes = res.getBody().readAllBytes();
                    String texto = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                    if (!res.getStatusCode().is2xxSuccessful()) {
                        log.error("[biows] guardado de examen NSS={} HTTP {}: {}", nss, res.getStatusCode().value(), recortar(texto));
                        throw new BiowsRechazoException("El servicio de exámenes respondió HTTP "
                                + res.getStatusCode().value() + ". Revise si el examen se guardó antes de reintentar.");
                    }
                    return texto;
                });
        // Cuanto tarda ORDS en guardar: sirve para medir si el ajuste de la base lo bajo.
        log.info("[biows] guardado de examen NSS={} respondio en {} ms", nss, System.currentTimeMillis() - inicio);
        if (crudo == null || crudo.isBlank()) {
            return "";
        }
        BiowsProcesoResponse response;
        try {
            response = LECTOR.readValue(crudo, BiowsProcesoResponse.class);
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            log.error("[biows] respuesta no JSON de {} NSS={}: {}", PATH_SAVE, nss, recortar(crudo));
            throw new BiowsRechazoException(
                    "El servicio de examenes respondio algo inesperado. Revise si el examen se guardo antes de reintentar.");
        }
        if (response.datos() == null || response.datos().isEmpty()) {
            return "";
        }
        BiowsProcesoResponse.Dato d = response.datos().get(0);
        // En Servcio/Medico, Estado 0 = aceptado. Los errores (-100 del procedimiento, -2 de la
        // excepcion, 100 de rechazo) llegan con HTTP 200: sin esta revision el portal los
        // mostraba como guardado exitoso y ademas registraba el historico del dictamen.
        if (d.estado() != null && d.estado() != 0) {
            log.error("[biows] guardado rechazado por ORDS NSS={} estado={} proceso={} data={}",
                    nss, d.estado(), d.proceso(), d.data());
            String detalle = d.data() != null && !d.data().isBlank() ? d.data() : d.mensaje();
            throw new BiowsRechazoException("No se guardó el examen: " + d.proceso()
                    + (detalle == null || detalle.isBlank() ? "" : " (" + detalle + ")"));
        }
        return d.proceso();
    }

    private static final com.fasterxml.jackson.databind.ObjectMapper LECTOR =
            new com.fasterxml.jackson.databind.ObjectMapper();

    private static String recortar(String s) {
        return s.length() <= 2000 ? s : s.substring(0, 2000) + "...(" + s.length() + " caracteres)";
    }

    /**
     * Ajuste de clave de lectura -> escritura para los typos/inconsistencias del WS legacy.
     * Casos claros (typo). Los de espacio final (SABIN/LUMBALGIA vs INFARTOS/ALT_CARGA) son
     * inconsistentes en el legacy -> se dejan tal cual; VERIFICAR contra el WS real.
     */
    private static String writeKey(String field) {
        // E-032 (11-nov-2026): el catalogo copia las claves del WS de LECTURA, que traen espacios
        // ('LUMBALGIA ', 'INFARTOS ', 'ALT_CARGA ', 'SABIN ' -> 'LUMBALGIA _OBS'...). El WS de ESCRITURA
        // las lee sin espacio (PR_SERVICIO_MED_EXAMEN1/2): con el espacio, Si/No y observacion de esos
        // renglones nunca se guardaban. Ninguna clave de escritura lleva espacios.
        field = field.replace(" ", "");
        return switch (field) {
            case "BAÑO" -> "BANO";
            case "TABIQUE_NASA" -> "TABIQUE_NASAL";
            case "CERVICA" -> "CERVICAL";
            default -> field;
        };
    }

    @SuppressWarnings("unchecked")
    /** Textos de relleno que el WS devuelve en lugar de null (coalesce en el PL/SQL). */
    private static boolean esRellenoWs(String v) {
        if (v == null) {
            return true;
        }
        String t = v.trim().toLowerCase();
        return t.isEmpty() || t.equals("sin firma") || t.equals("sin datos") || t.startsWith("sin obs");
    }

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
                d.idRegistro(), d.fechaRegistro(), d.nss(), d.nombre(), d.rfc(), d.curp(), d.cuenta(),
                d.apto(), d.noApto(), d.aptoCondicionado(), d.aptoRestringido());
    }
}
