package com.onest.app.catalog.antidoping.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Respuesta de .../Servcio/consulta_antidoping. Ver docs/ords-antidoping.sql BLOQUE 4.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BiowsAntidopingResponse(
        @JsonProperty("Datos") List<Dato> datos
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Dato(
            @JsonProperty("id_registro") String idRegistro,
            @JsonProperty("fecha_registro") String fechaRegistro,
            @JsonProperty("folio") String folio,
            @JsonProperty("tipo_prueba") String tipoPrueba,
            @JsonProperty("sustancia") String sustancia,
            @JsonProperty("resultado") String resultado,
            @JsonProperty("status_conclusion") String statusConclusion,
            @JsonProperty("observaciones") String observaciones,
            @JsonProperty("usuario") String usuario
    ) {
    }
}
