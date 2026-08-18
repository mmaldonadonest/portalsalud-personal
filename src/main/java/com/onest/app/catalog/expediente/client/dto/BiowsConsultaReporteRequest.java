package com.onest.app.catalog.expediente.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Body de .../Servcio/consulta_medica_fecha. Fechas en formato "dd/MM/yy" (mismo
 * criterio que BiowsIncapacidadReporteRequest/BiowsAccidenteReporteRequest).
 */
public record BiowsConsultaReporteRequest(
        @JsonProperty("fecha_inicial") String fechaInicial,
        @JsonProperty("fecha_final") String fechaFinal
) {
}
