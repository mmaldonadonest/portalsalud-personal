package com.onest.app.catalog.expediente.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Body del POST a .../Servcio/conuslta_medica_usuario.
 * Replica el $postData de app::showConsults() en php-old/app/app.php: {NSS}.
 */
public record BiowsConsultasRequest(
        @JsonProperty("NSS") String nss
) {
}
