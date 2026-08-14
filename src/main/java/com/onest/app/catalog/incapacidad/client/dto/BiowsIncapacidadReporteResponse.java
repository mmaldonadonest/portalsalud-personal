package com.onest.app.catalog.incapacidad.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Respuesta de .../Servcio/consulta_incapacidades_fecha. Array de incapacidades de TODAS
 * las NSS en el rango de fechas, con nombre/RFC/CURP del empleado ya resueltos (join contra
 * bio_empleado). Ver docs/contextoWS.txt:2317-2389.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BiowsIncapacidadReporteResponse(
        @JsonProperty("Datos") List<Dato> datos
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Dato(
            @JsonProperty("fecha_consulta") String fechaConsulta,
            @JsonProperty("folio_incapacidad") String folioIncapacidad,
            @JsonProperty("nss") String nss,
            @JsonProperty("nombre") String nombre,
            @JsonProperty("rfc") String rfc,
            @JsonProperty("curp") String curp,
            @JsonProperty("rubro") String rubro,
            @JsonProperty("ramo") String ramo,
            @JsonProperty("tipo_incapacidad") String tipoIncapacidad,
            @JsonProperty("fecha_inicio") String fechaInicio,
            @JsonProperty("fecha_termino") String fechaTermino,
            @JsonProperty("dias_autorizados") String diasAutorizados,
            @JsonProperty("salario_integrado") String salarioIntegrado,
            @JsonProperty("costo") String costo,
            @JsonProperty("estado_dictamen") String estadoDictamen
    ) {
    }
}
