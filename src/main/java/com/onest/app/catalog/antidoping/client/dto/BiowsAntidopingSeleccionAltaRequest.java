package com.onest.app.catalog.antidoping.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Body del POST a .../Servcio/antidoping_seleccion (alta, una vez por NSS elegido en la ronda).
 * Ver docs/ords-antidoping-seleccion.sql BLOQUE 2.
 */
public record BiowsAntidopingSeleccionAltaRequest(
        @JsonProperty("NSS") String nss,
        @JsonProperty("TAMANO_POOL") Integer tamanoPool,
        @JsonProperty("USUARIO_ID") String usuarioId,
        @JsonProperty("NOMBRE_USUARIO") String nombreUsuario
) {
}
