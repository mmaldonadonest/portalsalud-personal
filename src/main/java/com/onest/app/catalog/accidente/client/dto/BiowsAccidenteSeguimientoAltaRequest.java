package com.onest.app.catalog.accidente.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Body del POST a .../Servcio/accidente_seguimiento (alta de un seguimiento sobre un
 * caso ya registrado en SERV_MED_ACCIDENTE). Ver docs/ords-accidentes-seguimiento.sql BLOQUE 2.
 */
public record BiowsAccidenteSeguimientoAltaRequest(
        @JsonProperty("ACCIDENTE_REG_ID") Long accidenteRegId,
        @JsonProperty("TIPO") String tipo,
        @JsonProperty("OBSERVACIONES") String observaciones,
        @JsonProperty("USUARIO_ID") String usuarioId,
        @JsonProperty("NOMBRE_USUARIO") String nombreUsuario
) {
}
