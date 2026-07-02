package com.onest.app.catalog.nss.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Respuesta de .../Servcio/consulta_examen. En php-old se lee $data->Datos[].Estado:
 * Estado == 0 => ya registrado (no hace nada); Estado != 0 => dispara ALTA en Servcio/Medico.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BiowsConsultaExamenResponse(
        @JsonProperty("Datos") List<Dato> datos
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Dato(
            @JsonProperty("Estado") String estado
    ) {
    }
}
