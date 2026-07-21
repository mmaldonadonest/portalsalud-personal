package com.onest.app.catalog.examen.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Respuesta de .../Servcio/consulta_examen. php lee $decoded->Datos[].Data.<SECCION>.<CAMPO>
 * (estructura profundamente anidada del examen medico).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BiowsExamenResponse(
        @JsonProperty("Datos") List<Dato> datos
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Dato(
            @JsonProperty("Data") Map<String, Object> data
    ) {
    }
}
