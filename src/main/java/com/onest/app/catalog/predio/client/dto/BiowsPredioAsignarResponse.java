package com.onest.app.catalog.predio.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Respuesta de .../Servcio/cuenta_predio_asignar. Ver docs/ords-predio-cuenta.sql BLOQUE 5.
 * Clave "proceso" en minuscula (no reusa BiowsProcesoResponse - ese usa "Proceso" con mayuscula,
 * distinto convenio de mayusculas del handler ya aplicado aqui).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BiowsPredioAsignarResponse(
        @JsonProperty("Datos") List<Dato> datos
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Dato(
            @JsonProperty("proceso") String proceso
    ) {
    }
}
