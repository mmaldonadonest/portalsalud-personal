package com.onest.app.catalog.causaconsulta.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Body del POST a .../Servcio/causa_consulta (alta). Ver docs/ords-causa-consulta.sql BLOQUE 2.
 */
public record BiowsCausaConsultaAltaRequest(
        @JsonProperty("NOMBRE") String nombre,
        @JsonProperty("USUARIO_ID") String usuarioId,
        @JsonProperty("NOMBRE_USUARIO") String nombreUsuario
) {
}
