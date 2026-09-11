package com.onest.app.catalog.predio.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Body del POST a .../Servcio/cuenta_predio_asignar. Ver docs/ords-predio-cuenta.sql BLOQUE 5. */
public record BiowsCuentaPredioAsignarRequest(
        @JsonProperty("CuentaNombre") String cuentaNombre,
        @JsonProperty("PredioId") Long predioId,
        @JsonProperty("IdUsuario") String idUsuario,
        @JsonProperty("NombreUsuario") String nombreUsuario
) {
}
