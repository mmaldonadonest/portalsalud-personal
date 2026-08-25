package com.onest.app.catalog.causaconsulta.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Body del POST a .../Servcio/causa_consulta_estado (activar/desactivar).
 * Ver docs/ords-causa-consulta.sql BLOQUE 4.
 */
public record BiowsCausaConsultaEstadoRequest(
        @JsonProperty("REG_ID") Long regId,
        @JsonProperty("ACTIVO") String activo
) {
}
