package com.onest.app.catalog.maternidad.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * Body del POST a .../Servcio/maternidad (alta de un chequeo). Ver docs/ords-maternidad.sql
 * BLOQUE 2. Las 3 fechas viajan como texto "dd/MM/yyyy" solo si vienen no-nulas (son opcionales -
 * un chequeo temprano puede no tener fecha probable de parto o proxima revision definidas aun).
 * SEMANAS_GESTACION va como numero JSON (el handler PL/SQL lo lee con apex_json.get_number,
 * mismo criterio que SDI/COSTO en Accidentes).
 */
public record BiowsMaternidadAltaRequest(
        @JsonProperty("NSS") String nss,
        @JsonProperty("SEMANAS_GESTACION") BigDecimal semanasGestacion,
        @JsonProperty("FECHA_PROBABLE_PARTO") String fechaProbableParto,
        @JsonProperty("RESTRICCIONES_LABORALES") String restriccionesLaborales,
        @JsonProperty("PROXIMA_REVISION") String proximaRevision,
        @JsonProperty("OBSERVACIONES") String observaciones,
        @JsonProperty("ESTATUS") String estatus,
        @JsonProperty("INCAPACIDAD") String incapacidad,
        @JsonProperty("REINCORPORACION") String reincorporacion,
        @JsonProperty("USUARIO_ID") String usuarioId,
        @JsonProperty("NOMBRE_USUARIO") String nombreUsuario
) {
}
