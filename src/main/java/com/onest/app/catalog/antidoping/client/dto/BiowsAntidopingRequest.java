package com.onest.app.catalog.antidoping.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Body de .../Servcio/consulta_antidoping: {NSS}. Ver docs/ords-antidoping.sql BLOQUE 4.
 */
public record BiowsAntidopingRequest(
        @JsonProperty("NSS") String nss
) {
}
