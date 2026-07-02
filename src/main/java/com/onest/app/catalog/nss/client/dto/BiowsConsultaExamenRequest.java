package com.onest.app.catalog.nss.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Body del POST a .../Servcio/consulta_examen.
 * Replica el $postData de app::CheckEmploye()/getDatsNss() en php-old/app/app.php.
 */
public record BiowsConsultaExamenRequest(
        @JsonProperty("NNS") String nns,
        @JsonProperty("FECHA") String fecha,
        @JsonProperty("USUARIO") String usuario
) {
}
