package com.onest.app.catalog.nss.client;

import com.onest.app.catalog.nss.client.dto.BiowsEmailLookupRequest;
import com.onest.app.catalog.nss.client.dto.BiowsEmailLookupResponse;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Implementacion contra POST .../Servcio/usuario_email - ver docs/ords-usuario-email-nuevo.sql. */
@Component
public class BiowsEmailLookupClient implements EmailLookupClient {

    private static final Logger log = LoggerFactory.getLogger(BiowsEmailLookupClient.class);
    private static final String PATH_USUARIO_EMAIL = "/Servcio/usuario_email";

    private final RestClient biowsRestClient;

    public BiowsEmailLookupClient(RestClient biowsRestClient) {
        this.biowsRestClient = biowsRestClient;
    }

    @Override
    public Optional<String> emailPorNss(String nss) {
        if (nss == null || nss.isBlank()) {
            return Optional.empty();
        }
        try {
            BiowsEmailLookupResponse response = biowsRestClient.post()
                    .uri(PATH_USUARIO_EMAIL)
                    .body(new BiowsEmailLookupRequest(nss))
                    .retrieve()
                    .body(BiowsEmailLookupResponse.class);

            List<BiowsEmailLookupResponse.Dato> datos = response == null ? null : response.datos();
            if (datos == null || datos.isEmpty()) {
                return Optional.empty();
            }
            return normalizarEmail(datos.get(0).email());
        } catch (Exception ex) {
            log.warn("[biows] no se pudo resolver email via Servcio/usuario_email para {}: {}", nss, ex.getMessage());
            return Optional.empty();
        }
    }

    /** Mismo convenio "0"=sin dato ya usado en Examen/Antidoping/Consumibles. */
    private static Optional<String> normalizarEmail(String email) {
        if (email == null || email.isBlank() || "0".equals(email.trim())) {
            return Optional.empty();
        }
        return Optional.of(email.trim());
    }
}