package com.onest.app.catalog.accidente.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Body de .../Servcio/consulta_accidente: {NSS}. Ver docs/ords-accidentes.sql BLOQUE 3.
 */
public record BiowsAccidenteRequest(
        @JsonProperty("NSS") String nss
) {
}
