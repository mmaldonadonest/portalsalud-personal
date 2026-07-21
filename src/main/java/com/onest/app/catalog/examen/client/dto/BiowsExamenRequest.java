package com.onest.app.catalog.examen.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Body del POST a .../Servcio/consulta_examen (app::getDatsNss). {NNS, FECHA, USUARIO}.
 */
public record BiowsExamenRequest(
        @JsonProperty("NNS") String nns,
        @JsonProperty("FECHA") String fecha,
        @JsonProperty("USUARIO") String usuario
) {
}
