package com.onest.app.catalog.examen.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Body de .../Servcio/consulta_examen_fecha. Fechas en formato "dd/MM/yy" (mismo
 * criterio que los otros 4 reportes por fecha).
 */
public record BiowsExamenReporteRequest(
        @JsonProperty("fecha_inicial") String fechaInicial,
        @JsonProperty("fecha_final") String fechaFinal
) {
}
