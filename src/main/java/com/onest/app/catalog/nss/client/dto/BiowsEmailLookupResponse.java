package com.onest.app.catalog.nss.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Respuesta del WS .../Servcio/usuario_email - ver docs/ords-usuario-email-nuevo.sql. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BiowsEmailLookupResponse(
        @JsonProperty("Datos") List<Dato> datos
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Dato(
            @JsonProperty("nss") String nss,
            @JsonProperty("email") String email
    ) {
    }
}