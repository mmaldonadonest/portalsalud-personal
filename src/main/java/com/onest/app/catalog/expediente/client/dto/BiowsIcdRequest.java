package com.onest.app.catalog.expediente.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Body del POST a .../Servcio/indice.
 * Replica el $postData de app::showICD() en php-old/app/app.php: {variable}.
 */
public record BiowsIcdRequest(
        @JsonProperty("variable") String variable
) {
}
