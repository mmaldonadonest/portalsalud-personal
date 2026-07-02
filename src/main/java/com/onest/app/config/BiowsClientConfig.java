package com.onest.app.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
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

    @Bean
    public RestClient biowsRestClient(RestClient.Builder builder, BiowsProperties properties, ObjectMapper objectMapper) {
        return builder
                .baseUrl(properties.baseUrl())
                .defaultHeader("x-api-key", properties.apiKey())
                .defaultHeader("Content-Type", "application/json; charset=utf-8")
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
                MediaType.TEXT_PLAIN));
        return converter;
    }
}
