package com.onest.app.catalog.accidente.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Body de .../Servcio/consulta_accidente_seguimiento: {ACCIDENTE_REG_ID}. A diferencia de
 * consulta_antidoping_seleccion, aqui el id es OBLIGATORIO - el seguimiento siempre se
 * consulta para UN caso especifico. Ver docs/ords-accidentes-seguimiento.sql BLOQUE 3.
 */
public record BiowsAccidenteSeguimientoRequest(
        @JsonProperty("ACCIDENTE_REG_ID") Long accidenteRegId
) {
}
