package com.onest.app.catalog.expediente.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Respuesta de .../Servcio/consulta_medica_fecha. Array de consultas medicas de TODAS
 * las NSS en el rango de fechas, con nombre/RFC/CURP resueltos (join contra bio_empleado).
 * Payload deliberadamente liviano (sin campos clinicos sensibles), ver
 * docs/ords-consulta-dashboard.sql.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BiowsConsultaReporteResponse(
        @JsonProperty("Datos") List<Dato> datos
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Dato(
            @JsonProperty("id_consulta") String idConsulta,
            @JsonProperty("fecha_consulta") String fechaConsulta,
            @JsonProperty("nss") String nss,
            @JsonProperty("nombre") String nombre,
            @JsonProperty("rfc") String rfc,
            @JsonProperty("curp") String curp,
            @JsonProperty("genero") String genero,
            @JsonProperty("edad") int edad,
            @JsonProperty("cuenta") String cuenta,
            @JsonProperty("tipo_consulta") String tipoConsulta,
            @JsonProperty("area_accidente") String areaAccidente,
            @JsonProperty("area_involucrada") String areaInvolucrada,
            @JsonProperty("causa") String causa
    ) {
    }
}
