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

    /**
     * Estado/Mensaje/Data se agregaron para el guardado del examen. OJO: el significado de
     * Estado NO es igual en todos los WS (Servcio/Medico: 0 = aceptado; otros usan 100 como
     * exito), asi que cada cliente decide como interpretarlo.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Dato(
            @JsonProperty("Proceso") String proceso,
            @JsonProperty("Estado") Integer estado,
            @JsonProperty("Mensaje") String mensaje,
            @JsonProperty("Data") String data
    ) {
    }
}
