package com.onest.app.catalog.maternidad.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Body del POST a .../Servcio/consulta_maternidad_fecha (fechas dd/mm/yy, igual que los demas *_fecha). */
public record BiowsMaternidadReporteRequest(
        @JsonProperty("fecha_inicial") String fechaInicial,
        @JsonProperty("fecha_final") String fechaFinal
) {
}
