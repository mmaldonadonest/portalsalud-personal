package com.onest.app.catalog.maternidad.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Respuesta de .../Servcio/consulta_maternidad_fecha (docs/ords-maternidad-dashboard.sql):
 * todos los seguimientos de TODAS las NSS en un rango, con CUENTA por registro. Las fechas
 * vienen en YYYY-MM-DD (a diferencia de consulta_maternidad por NSS, que usa DD/MM/YYYY).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BiowsMaternidadReporteResponse(
        @JsonProperty("Datos") List<Dato> datos
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Dato(
            @JsonProperty("id_registro") String idRegistro,
            @JsonProperty("fecha_registro") String fechaRegistro,
            @JsonProperty("nss") String nss,
            @JsonProperty("nombre") String nombre,
            @JsonProperty("cuenta") String cuenta,
            @JsonProperty("semanas_gestacion") String semanasGestacion,
            @JsonProperty("fecha_probable_parto") String fechaProbableParto,
            @JsonProperty("restricciones_laborales") String restriccionesLaborales,
            @JsonProperty("proxima_revision") String proximaRevision,
            @JsonProperty("estatus") String estatus,
            @JsonProperty("incapacidad") String incapacidad,
            @JsonProperty("reincorporacion") String reincorporacion,
            @JsonProperty("usuario") String usuario
    ) {
    }
}
