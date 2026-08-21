package com.onest.app.integration.intranet;

import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
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
        ClientHttpRequestFactorySettings timeouts = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(3))
                .withReadTimeout(Duration.ofSeconds(5));
        return builder
                .baseUrl(properties.baseUrl())
                .requestFactory(ClientHttpRequestFactories.get(timeouts))
                .defaultHeader("Authorization", "Bearer " + properties.token())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
