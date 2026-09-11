package com.onest.app.catalog.predio.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Respuesta de .../Servcio/predios (catálogo). Ver docs/ords-predio-cuenta.sql BLOQUE 3. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BiowsPredioListResponse(
        @JsonProperty("Datos") List<Dato> datos
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Dato(
            @JsonProperty("predioId") Long predioId,
            @JsonProperty("nombre") String nombre
    ) {
    }
}
