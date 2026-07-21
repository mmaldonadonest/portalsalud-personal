package com.onest.app.catalog.incapacidad.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Body del POST a .../Servcio/consulta_incapacidad.
 * Replica el $postData de app::showIncapacidades() en php-old/app/app.php: {NSS}.
 */
public record BiowsIncapacidadesRequest(
        @JsonProperty("NSS") String nss
) {
}
