package com.onest.app.catalog.consumible.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Body del POST a .../Servcio/consumibles (alta). Ver
 * docs/ords-antidoping-consumibles.sql BLOQUE 1.
 */
public record BiowsConsumibleAltaRequest(
        @JsonProperty("PREDIO") String predio,
        @JsonProperty("ANIO") Integer anio,
        @JsonProperty("MES") Integer mes,
        @JsonProperty("CANTIDAD_INICIAL") Integer cantidadInicial,
        @JsonProperty("ENTREGA_MENSUAL") Integer entregaMensual,
        @JsonProperty("CONSUMO_MENSUAL") Integer consumoMensual,
        @JsonProperty("OBSERVACIONES") String observaciones
) {
}
