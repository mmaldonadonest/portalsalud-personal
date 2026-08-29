package com.onest.app.catalog.nss.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Body del POST a .../Servcio/usuario_email - WS nuevo y aislado, solo Nss. */
public record BiowsEmailLookupRequest(
        @JsonProperty("Nss") String nss
) {
}