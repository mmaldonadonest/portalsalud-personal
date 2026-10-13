package com.onest.app.ayuda;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Cliente minimo de Meilisearch (API HTTP, sin SDK): salud, ajustes del indice, carga de
 * documentos y busqueda. Todo lo que falla aqui lo atrapa AyudaService y cae a Oracle.
 */
@Component
public class MeilisearchClient {

    private static final Logger log = LoggerFactory.getLogger(MeilisearchClient.class);

    /** Un resultado: el articulo y el fragmento resaltado que explica por que coincidio. */
    public record Hit(long id, String tituloResaltado, String fragmento) {
    }

    private final AyudaProperties props;
    private final RestClient http;

    public MeilisearchClient(AyudaProperties props) {
        this.props = props;
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(Duration.ofSeconds(2));
        f.setReadTimeout(Duration.ofSeconds(5));
        RestClient.Builder b = RestClient.builder().requestFactory(f);
        if (props.meiliConfigurado()) {
            b.baseUrl(props.meili().url().replaceAll("/$", ""));
            if (props.meili().key() != null && !props.meili().key().isBlank()) {
                b.defaultHeader("Authorization", "Bearer " + props.meili().key().trim());
            }
        }
        this.http = b.build();
    }

    public boolean disponible() {
        if (!props.meiliConfigurado()) {
            return false;
        }
        try {
            JsonNode n = http.get().uri("/health").retrieve().body(JsonNode.class);
            return n != null && "available".equals(n.path("status").asText());
        } catch (RuntimeException ex) {
            return false;
        }
    }

    /** Ajustes del indice: que se busca, por que se filtra, sinonimos y palabras vacias en espanol. */
    public void configurar(Map<String, List<String>> sinonimos) {
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("searchableAttributes", List.of("titulo", "pregunta", "palabrasClave", "cuerpo"));
        settings.put("filterableAttributes", List.of("modulo", "activo"));
        settings.put("sortableAttributes", List.of("orden"));
        settings.put("synonyms", sinonimos);
        settings.put("stopWords", List.of("el", "la", "los", "las", "un", "una", "unos", "unas", "de", "del", "al", "a", "en", "y", "o",
                "que", "como", "donde", "cual", "cuando", "por", "para", "con", "sin", "se", "me", "mi", "es", "son", "hay", "puedo",
                "hago", "le", "lo", "su", "sus", "ya", "no", "si"));
        // Acentos y mayusculas los normaliza Meilisearch solo; la tolerancia a errores viene por defecto.
        http.patch().uri("/indexes/{i}/settings", indice()).contentType(MediaType.APPLICATION_JSON).body(settings).retrieve().toBodilessEntity();
    }

    /** Reemplaza el indice completo con estos articulos (borra lo que ya no exista). */
    public void reindexar(List<AyudaArticulo> articulos) {
        try {
            http.delete().uri("/indexes/{i}/documents", indice()).retrieve().toBodilessEntity();
        } catch (RuntimeException ex) {
            log.debug("[ayuda] indice aun no existe: {}", ex.getMessage());
        }
        List<Map<String, Object>> docs = new ArrayList<>();
        for (AyudaArticulo a : articulos) {
            docs.add(documento(a));
        }
        http.put().uri("/indexes/{i}/documents?primaryKey=id", indice()).contentType(MediaType.APPLICATION_JSON).body(docs)
                .retrieve().toBodilessEntity();
    }

    public void indexar(AyudaArticulo a) {
        http.put().uri("/indexes/{i}/documents?primaryKey=id", indice()).contentType(MediaType.APPLICATION_JSON)
                .body(List.of(documento(a))).retrieve().toBodilessEntity();
    }

    /** Busca entre los activos de los modulos indicados (null = sin filtro de modulo). */
    public List<Hit> buscar(String q, List<String> modulos, int limite) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("q", q);
        body.put("limit", limite);
        body.put("attributesToRetrieve", List.of("id", "titulo"));
        body.put("attributesToHighlight", List.of("titulo", "cuerpo"));
        body.put("attributesToCrop", List.of("cuerpo"));
        body.put("cropLength", 28);
        body.put("highlightPreTag", "<mark>");
        body.put("highlightPostTag", "</mark>");
        StringBuilder filtro = new StringBuilder("activo = true");
        if (modulos != null) {
            StringBuilder in = new StringBuilder();
            for (String m : modulos) {
                if (in.length() > 0) {
                    in.append(", ");
                }
                in.append('"').append(m.replace("\"", "")).append('"');
            }
            filtro.append(" AND modulo IN [").append(in).append("]");
        }
        body.put("filter", filtro.toString());
        JsonNode r = http.post().uri("/indexes/{i}/search", indice()).contentType(MediaType.APPLICATION_JSON).body(body)
                .retrieve().body(JsonNode.class);
        List<Hit> hits = new ArrayList<>();
        if (r != null) {
            for (JsonNode h : r.path("hits")) {
                JsonNode f = h.path("_formatted");
                hits.add(new Hit(h.path("id").asLong(), f.path("titulo").asText(h.path("titulo").asText()), f.path("cuerpo").asText("")));
            }
        }
        return hits;
    }

    private Map<String, Object> documento(AyudaArticulo a) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("id", a.id());
        d.put("clave", a.clave());
        d.put("modulo", a.modulo());
        d.put("titulo", a.titulo());
        d.put("pregunta", a.pregunta() == null ? "" : a.pregunta());
        d.put("palabrasClave", a.palabrasClave() == null ? "" : a.palabrasClave());
        d.put("cuerpo", Markdown.textoPlano(a.cuerpo()));
        d.put("enlace", a.enlace() == null ? "" : a.enlace());
        d.put("orden", a.orden());
        d.put("activo", a.activo());
        return d;
    }

    private String indice() {
        return props.meili().indiceONombre();
    }

    @SuppressWarnings("unused")
    private static URI uri(String s) {
        return URI.create(s);
    }
}
