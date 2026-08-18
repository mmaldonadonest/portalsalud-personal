package com.onest.app.catalog.accidente.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Respuesta de .../Servcio/consulta_accidentes_fecha. Array de accidentes de TODAS las NSS
 * en el rango de fechas, con nombre/RFC/CURP ya resueltos (join contra bio_empleado).
 * Ver docs/ords-accidentes-dashboard.sql.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BiowsAccidenteReporteResponse(
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
