package com.onest.app.integration.intranet;

import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * RestClient dedicado a la intranet Onest (foto de perfil). Timeouts cortos a
 * proposito: esto se llama durante el login (PortalUserDetailsService) y NUNCA
 * debe colgar la autenticacion si la intranet esta lenta o caida.
 */
@Configuration
@EnableConfigurationProperties(IntranetProperties.class)
public class IntranetClientConfig {

    @Bean
    public RestClient intranetRestClient(RestClient.Builder builder, IntranetProperties properties) {
        // Spring Boot 4 retiro ClientHttpRequestFactories: conexion simple con los mismos tiempos (3 s / 5 s)
        SimpleClientHttpRequestFactory timeouts = new SimpleClientHttpRequestFactory();
        timeouts.setConnectTimeout(Duration.ofSeconds(3));
        timeouts.setReadTimeout(Duration.ofSeconds(5));
        // clone(): el Builder lo comparte Spring con los clientes de ORDS
        return builder.clone()
                .baseUrl(properties.baseUrl())
                .requestFactory(timeouts)
                .defaultHeader("Authorization", "Bearer " + properties.token())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
