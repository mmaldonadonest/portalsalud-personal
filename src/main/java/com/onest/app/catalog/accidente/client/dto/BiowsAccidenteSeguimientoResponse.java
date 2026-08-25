package com.onest.app.catalog.accidente.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Respuesta de .../Servcio/consulta_accidente_seguimiento. Ver docs/ords-accidentes-seguimiento.sql BLOQUE 3.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BiowsAccidenteSeguimientoResponse(
        @JsonProperty("Datos") List<Dato> datos
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Dato(
            @JsonProperty("id_registro") String idRegistro,
            @JsonProperty("tipo") String tipo,
            @JsonProperty("fecha_seguimiento") String fechaSeguimiento,
            @JsonProperty("observaciones") String observaciones,
            @JsonProperty("usuario") String usuario
    ) {
    }
}
