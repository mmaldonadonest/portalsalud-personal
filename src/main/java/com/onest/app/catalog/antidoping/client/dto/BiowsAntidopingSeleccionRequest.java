package com.onest.app.catalog.antidoping.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Body de .../Servcio/consulta_antidoping_seleccion: {NSS}. NSS es opcional - si viene
 * null, el WS regresa TODO el historial (no esta atado a un NSS especifico). Ver
 * docs/ords-antidoping-seleccion.sql BLOQUE 3.
 */
public record BiowsAntidopingSeleccionRequest(
        @JsonProperty("NSS") String nss
) {
}
