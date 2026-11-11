package com.onest.app.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

/**
 * Provee un RestClient dedicado al web service ORDS legacy (biows).
 * Replica los headers fijos que arma cada metodo curl_* de php-old/app/app.php:
 * x-api-key y Content-Type: application/json.
 */
@Configuration
@EnableConfigurationProperties(BiowsProperties.class)
public class BiowsClientConfig {

    // SIN @Primary a proposito: los clientes eligen su RestClient por nombre de parametro
    // (biowsRestClient, intranetRestClient) o con @Qualifier. Un @Primary aqui desvio las
    // llamadas de la intranet a ORDS (10-oct-2026).
    /**
     * Spring Boot 4 (11-nov-2026) ya no publica un ObjectMapper de Jackson 2 (usa Jackson 3). Para ORDS se arma uno
     * igual al que daba Spring Boot 3: ignora campos desconocidos, fechas java.time como texto. Asi las respuestas
     * de ORDS (Content-Type cambiante, campos extra) se leen exactamente como antes.
     */
    private static final ObjectMapper ORDS_JSON = JsonMapper.builder()
            .findAndAddModules()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    @Bean
    public RestClient biowsRestClient(RestClient.Builder builder, BiowsProperties properties) {
        // Timeouts explicitos: un ORDS inalcanzable no debe colgar el request
        // (critico para el login HYBRID/LEGACY_PHP que pasa por el WS).
        return construir(builder, properties, ORDS_JSON, Duration.ofSeconds(20));
    }

    /**
     * Solo para el guardado del examen (POST Servcio/Medico). 10-oct-2026: ORDS tarda mas de 20 s
     * en guardar el examen completo (47 secciones, ~100 sentencias en los procedimientos) y el
     * cliente cortaba con "Read timed out" mientras ORDS seguia guardando: el usuario veia un 500
     * de un examen que si quedaba guardado. El resto de las llamadas sigue con 20 s.
     */
    @Bean
    public RestClient biowsRestClientGuardadoLargo(RestClient.Builder builder, BiowsProperties properties) {
        return construir(builder, properties, ORDS_JSON, Duration.ofSeconds(120));
    }

    private static RestClient construir(RestClient.Builder builder, BiowsProperties properties,
                                        ObjectMapper objectMapper, Duration lectura) {
        // Spring Boot 4 retiro ClientHttpRequestFactories: misma conexion simple (HttpURLConnection) y mismos tiempos
        SimpleClientHttpRequestFactory timeouts = new SimpleClientHttpRequestFactory();
        timeouts.setConnectTimeout(Duration.ofSeconds(5));
        timeouts.setReadTimeout(lectura);
        // clone(): el Builder lo comparte Spring; sin copia, el segundo bean heredaria la
        // configuracion acumulada del primero (convertidores duplicados).
        return builder.clone()
                .baseUrl(properties.baseUrl())
                .requestFactory(timeouts)
                .defaultHeader("x-api-key", properties.apiKey())
                .defaultHeader("Content-Type", "application/json; charset=utf-8")
                // Sin keep-alive: ORDS cierra las conexiones ociosas por su lado y HttpURLConnection
                // reutilizaba la conexion muerta -> "Unexpected end of file from server" (HTTP 500
                // intermitente en la primera llamada tras un rato sin uso; visto en el Dashboard
                // Ejecutivo 2024 y al abrir el examen, 11-13 sep 2026). Un POST no se reintenta
                // solo, asi que cada llamada abre su propia conexion (ORDS es red local, costo nulo).
                .defaultHeader("Connection", "close")
                // El ORDS responde con Content-Type text/html aunque el cuerpo es JSON
                // (en php-old se hacia json_decode ignorando el content-type). Forzamos que
                // Jackson tambien deserialice text/html y text/plain.
                .messageConverters(converters -> converters.add(0, biowsJacksonConverter(objectMapper)))
                .build();
    }

    private static MappingJackson2HttpMessageConverter biowsJacksonConverter(ObjectMapper objectMapper) {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);
        converter.setSupportedMediaTypes(List.of(
                MediaType.APPLICATION_JSON,
                MediaType.valueOf("application/*+json"),
                MediaType.TEXT_HTML,
                MediaType.TEXT_PLAIN,
                // Los handlers no fijan el Content-Type (no llaman owa_util.mime_header) y ORDS
                // lo decide solo: el 9-oct-2026 11:45 el guardado del examen llego etiquetado
                // como octet-stream con el JSON adentro, y el usuario vio un 500 aunque el
                // examen si se guardo.
                MediaType.APPLICATION_OCTET_STREAM));
        return converter;
    }
}
