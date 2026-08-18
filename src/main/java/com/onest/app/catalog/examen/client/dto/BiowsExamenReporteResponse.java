package com.onest.app.catalog.examen.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Respuesta de .../Servcio/consulta_examen_fecha. Array de dictamenes de examen
 * (historial, no retroactivo desde 2026-08-17) de TODAS las NSS en el rango de
 * fechas. Ver docs/ords-examen-dashboard.sql.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BiowsExamenReporteResponse(
        @JsonProperty("Datos") List<Dato> datos
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Dato(
            @JsonProperty("id_registro") String idRegistro,
            @JsonProperty("fecha_registro") String fechaRegistro,
            @JsonProperty("nss") String nss,
            @JsonProperty("nombre") String nombre,
            @JsonProperty("rfc") String rfc,
            @JsonProperty("curp") String curp,
            @JsonProperty("apto") String apto,
            @JsonProperty("no_apto") String noApto,
            @JsonProperty("apto_condicionado") String aptoCondicionado,
            @JsonProperty("apto_restringido") String aptoRestringido
    ) {
    }
}
