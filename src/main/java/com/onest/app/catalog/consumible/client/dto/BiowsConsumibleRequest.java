package com.onest.app.catalog.consumible.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Body de .../Servcio/consulta_consumibles: {PREDIO}. PREDIO es opcional -
 * null/vacio regresa todos los registros. Ver docs/ords-antidoping-consumibles.sql BLOQUE 2.
 */
public record BiowsConsumibleRequest(
        @JsonProperty("PREDIO") String predio
) {
}
