package com.onest.app.catalog.antidoping.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Body de .../Servcio/consulta_antidoping_fecha. Fechas en formato "dd/MM/yy" (mismo
 * criterio que BiowsIncapacidadReporteRequest/BiowsAccidenteReporteRequest/
 * BiowsConsultaReporteRequest).
 */
public record BiowsAntidopingReporteRequest(
        @JsonProperty("fecha_inicial") String fechaInicial,
        @JsonProperty("fecha_final") String fechaFinal
) {
}
