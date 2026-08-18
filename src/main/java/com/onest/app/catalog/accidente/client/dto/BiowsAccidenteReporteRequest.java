package com.onest.app.catalog.accidente.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Body de .../Servcio/consulta_accidentes_fecha. Fechas en formato "dd/MM/yy" (mismo
 * criterio que BiowsIncapacidadReporteRequest, confirmado contra el WS real).
 */
public record BiowsAccidenteReporteRequest(
        @JsonProperty("fecha_inicial") String fechaInicial,
        @JsonProperty("fecha_final") String fechaFinal
) {
}
