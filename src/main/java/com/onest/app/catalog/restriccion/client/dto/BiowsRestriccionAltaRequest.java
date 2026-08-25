package com.onest.app.catalog.restriccion.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Body del POST a .../Servcio/restriccion (alta). Ver docs/ords-restriccion.sql BLOQUE 2.
 */
public record BiowsRestriccionAltaRequest(
        @JsonProperty("NSS") String nss,
        @JsonProperty("CODIGO_RESTRICCION") String codigoRestriccion,
        @JsonProperty("DESCRIPCION") String descripcion,
        @JsonProperty("VALOR_LIMITE") java.math.BigDecimal valorLimite,
        @JsonProperty("UNIDAD") String unidad,
        @JsonProperty("FECHA_INICIO") String fechaInicio,
        @JsonProperty("FECHA_FIN") String fechaFin,
        @JsonProperty("FECHA_REVALORACION") String fechaRevaloracion,
        @JsonProperty("TEMPORALIDAD") String temporalidad,
        @JsonProperty("OBSERVACIONES") String observaciones,
        @JsonProperty("MEDICO_RESPONSABLE") String medicoResponsable,
        @JsonProperty("ESTATUS") String estatus,
        @JsonProperty("USUARIO_ID") String usuarioId,
        @JsonProperty("NOMBRE_USUARIO") String nombreUsuario
) {
}
