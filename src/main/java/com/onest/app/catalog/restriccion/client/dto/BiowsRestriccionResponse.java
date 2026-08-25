package com.onest.app.catalog.restriccion.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Respuesta de .../Servcio/consulta_restriccion. Ver docs/ords-restriccion.sql BLOQUE 3.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BiowsRestriccionResponse(
        @JsonProperty("Datos") List<Dato> datos
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Dato(
            @JsonProperty("id_registro") String idRegistro,
            @JsonProperty("codigo_restriccion") String codigoRestriccion,
            @JsonProperty("descripcion") String descripcion,
            @JsonProperty("valor_limite") String valorLimite,
            @JsonProperty("unidad") String unidad,
            @JsonProperty("fecha_inicio") String fechaInicio,
            @JsonProperty("fecha_fin") String fechaFin,
            @JsonProperty("fecha_revaloracion") String fechaRevaloracion,
            @JsonProperty("temporalidad") String temporalidad,
            @JsonProperty("observaciones") String observaciones,
            @JsonProperty("medico_responsable") String medicoResponsable,
            @JsonProperty("estatus") String estatus,
            @JsonProperty("fecha_registro") String fechaRegistro,
            @JsonProperty("usuario") String usuario
    ) {
    }
}
