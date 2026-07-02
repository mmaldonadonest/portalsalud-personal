package com.onest.app.catalog.nss.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Body del POST a .../Catalogo/Candidatos.
 * Replica el $postData de app::searchProspecto() en php-old/app/app.php (solo 'curp').
 */
public record BiowsProspectoRequest(
        @JsonProperty("curp") String curp
) {
}
