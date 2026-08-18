package com.onest.app.catalog.antidoping.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Respuesta de .../Servcio/consulta_antidoping_fecha. Array de resultados de antidoping
 * de TODAS las NSS en el rango de fechas, con nombre/RFC/CURP resueltos (join contra
 * bio_empleado). Ver docs/ords-antidoping-dashboard.sql.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BiowsAntidopingReporteResponse(
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
            @JsonProperty("folio") String folio,
            @JsonProperty("tipo_prueba") String tipoPrueba,
            @JsonProperty("sustancia") String sustancia,
            @JsonProperty("resultado") String resultado,
            @JsonProperty("status_conclusion") String statusConclusion,
            @JsonProperty("usuario") String usuario
    ) {
    }
}
