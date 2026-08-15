package com.onest.app.catalog.accidente.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * Body del POST a .../Servcio/accidente (alta). Ver docs/ords-accidentes.sql BLOQUE 2.
 * FECHA_ACCIDENTE viaja como texto "dd/MM/yyyy" (confirmado contra el WS real, ver
 * docs/ords-accidentes.sql NOTA-2). SDI/COSTO van como numero JSON, no string: el handler
 * PL/SQL los lee con apex_json.get_number (confirmado en la prueba en vivo del 2026-08-14).
 */
public record BiowsAccidenteAltaRequest(
        @JsonProperty("NSS") String nss,
        @JsonProperty("FECHA_ACCIDENTE") String fechaAccidente,
        @JsonProperty("TIPO_RIESGO") String tipoRiesgo,
        @JsonProperty("CAUSA_RT") String causaRt,
        @JsonProperty("DIAGNOSTICO") String diagnostico,
        @JsonProperty("SDI") BigDecimal sdi,
        @JsonProperty("STATUS_CALIFICACION") String statusCalificacion,
        @JsonProperty("COSTO") BigDecimal costo,
        @JsonProperty("OBSERVACIONES") String observaciones,
        @JsonProperty("USUARIO_ID") String usuarioId,
        @JsonProperty("NOMBRE_USUARIO") String nombreUsuario
) {
}
