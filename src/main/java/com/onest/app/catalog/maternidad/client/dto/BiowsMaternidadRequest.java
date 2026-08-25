package com.onest.app.catalog.maternidad.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Body de .../Servcio/consulta_maternidad: {NSS}. Ver docs/ords-maternidad.sql BLOQUE 3.
 */
public record BiowsMaternidadRequest(
        @JsonProperty("NSS") String nss
) {
}
