package com.onest.app.catalog.accidente.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Respuesta de .../Servcio/consulta_accidente. Ver docs/ords-accidentes.sql BLOQUE 3.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BiowsAccidenteResponse(
        @JsonProperty("Datos") List<Dato> datos
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Dato(
            @JsonProperty("id_registro") String idRegistro,
            @JsonProperty("fecha_registro") String fechaRegistro,
            @JsonProperty("fecha_accidente") String fechaAccidente,
            @JsonProperty("tipo_riesgo") String tipoRiesgo,
            @JsonProperty("causa_rt") String causaRt,
            @JsonProperty("diagnostico") String diagnostico,
            @JsonProperty("sdi") String sdi,
            @JsonProperty("status_calificacion") String statusCalificacion,
            @JsonProperty("costo") String costo,
            @JsonProperty("observaciones") String observaciones,
            @JsonProperty("usuario") String usuario
    ) {
    }
}
