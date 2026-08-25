package com.onest.app.catalog.maternidad.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Respuesta de .../Servcio/consulta_maternidad. Ver docs/ords-maternidad.sql BLOQUE 3.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BiowsMaternidadResponse(
        @JsonProperty("Datos") List<Dato> datos
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Dato(
            @JsonProperty("id_registro") String idRegistro,
            @JsonProperty("fecha_registro") String fechaRegistro,
            @JsonProperty("semanas_gestacion") String semanasGestacion,
            @JsonProperty("fecha_probable_parto") String fechaProbableParto,
            @JsonProperty("restricciones_laborales") String restriccionesLaborales,
            @JsonProperty("proxima_revision") String proximaRevision,
            @JsonProperty("observaciones") String observaciones,
            @JsonProperty("estatus") String estatus,
            @JsonProperty("incapacidad") String incapacidad,
            @JsonProperty("reincorporacion") String reincorporacion,
            @JsonProperty("usuario") String usuario
    ) {
    }
}
