package com.onest.app.catalog.antidoping.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Respuesta de .../Servcio/consulta_antidoping_seleccion. Ver docs/ords-antidoping-seleccion.sql BLOQUE 3.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BiowsAntidopingSeleccionResponse(
        @JsonProperty("Datos") List<Dato> datos
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Dato(
            @JsonProperty("id_registro") String idRegistro,
            @JsonProperty("fecha_seleccion") String fechaSeleccion,
            @JsonProperty("nss") String nss,
            @JsonProperty("tamano_pool") String tamanoPool,
            @JsonProperty("usuario") String usuario
    ) {
    }
}
