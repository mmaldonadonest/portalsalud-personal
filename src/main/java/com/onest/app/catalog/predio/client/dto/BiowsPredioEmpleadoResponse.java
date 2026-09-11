package com.onest.app.catalog.predio.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Respuesta de .../Servcio/predio_empleado. Ver docs/ords-predio-empleado.sql. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BiowsPredioEmpleadoResponse(
        @JsonProperty("Datos") List<Dato> datos
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Dato(
            @JsonProperty("nss") String nss,
            @JsonProperty("predioId") Long predioId,
            @JsonProperty("predioDesc") String predioDesc
    ) {
    }
}
