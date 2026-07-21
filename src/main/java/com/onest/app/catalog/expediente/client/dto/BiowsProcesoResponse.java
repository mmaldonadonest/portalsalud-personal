package com.onest.app.catalog.expediente.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Respuesta generica de los WS de escritura (addConsultM, etc.): Datos[].Proceso.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BiowsProcesoResponse(
        @JsonProperty("Datos") List<Dato> datos
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Dato(
            @JsonProperty("Proceso") String proceso
    ) {
    }
}
