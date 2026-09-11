package com.onest.app.catalog.predio.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Respuesta de .../Servcio/cuenta_predio_lista. Ver docs/ords-predio-cuenta.sql BLOQUE 4. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BiowsCuentaPredioListResponse(
        @JsonProperty("Datos") List<Dato> datos
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Dato(
            @JsonProperty("cuentaId") String cuentaId,
            @JsonProperty("cuentaNombre") String cuentaNombre,
            @JsonProperty("predioId") Long predioId,
            @JsonProperty("predioNombre") String predioNombre
    ) {
    }
}
