package com.onest.app.catalog.antidoping.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Body del POST a .../Servcio/antidoping (alta). Ver docs/ords-antidoping.sql BLOQUE 3.
 */
public record BiowsAntidopingAltaRequest(
        @JsonProperty("NSS") String nss,
        @JsonProperty("FOLIO") String folio,
        @JsonProperty("TIPO_PRUEBA") String tipoPrueba,
        @JsonProperty("SUSTANCIA") String sustancia,
        @JsonProperty("RESULTADO") String resultado,
        @JsonProperty("STATUS_CONCLUSION") String statusConclusion,
        @JsonProperty("OBSERVACIONES") String observaciones,
        @JsonProperty("USUARIO_ID") String usuarioId,
        @JsonProperty("NOMBRE_USUARIO") String nombreUsuario
) {
}
