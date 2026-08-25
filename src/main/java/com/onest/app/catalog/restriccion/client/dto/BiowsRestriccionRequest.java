package com.onest.app.catalog.restriccion.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Body de .../Servcio/consulta_restriccion: {NSS}. Ver docs/ords-restriccion.sql BLOQUE 3.
 */
public record BiowsRestriccionRequest(
        @JsonProperty("NSS") String nss
) {
}
