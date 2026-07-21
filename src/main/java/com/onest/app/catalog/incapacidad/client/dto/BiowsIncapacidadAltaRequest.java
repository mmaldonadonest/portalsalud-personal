package com.onest.app.catalog.incapacidad.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Body del POST a .../Servcio/incapacidades (alta de incapacidad).
 * Replica el $postData de app::addIncapacidad() en php-old/app/app.php
 * (incluido el typo GOCE_SUEDO tal cual el legacy).
 */
public record BiowsIncapacidadAltaRequest(
        @JsonProperty("FECHA") String fecha,
        @JsonProperty("NSS") String nss,
        @JsonProperty("FOLIO_INCAPACIDAD") String folioIncapacidad,
        @JsonProperty("RAMO") String ramo,
        @JsonProperty("TIPO_INCAPACIDAD") String tipoIncapacidad,
        @JsonProperty("FECHA_INICIO") String fechaInicio,
        @JsonProperty("FECHA_TERMINO") String fechaTermino,
        @JsonProperty("DIAS_AUTORIZADOS") String diasAutorizados,
        @JsonProperty("SALARIO_INTEGRADO") String salarioIntegrado,
        @JsonProperty("COSTO") String costo,
        @JsonProperty("IMPUTABLE") String imputable,
        @JsonProperty("ESTADO_DICTAMEN") String estadoDictamen,
        @JsonProperty("GOCE_SUEDO") String goceSueldo,
        @JsonProperty("COMPLEMENTO_SALARIAL") String complementoSalarial,
        @JsonProperty("URL_ARCHIVOS") String urlArchivos,
        @JsonProperty("USUARIO") String usuario,
        @JsonProperty("NOMBRE_USUARIO") String nombreUsuario,
        @JsonProperty("FIRMA_DIGITAL") String firmaDigital,
        @JsonProperty("RUBRO") String rubro,
        @JsonProperty("FECHA_ALTA") String fechaAlta,
        @JsonProperty("FECHA_PRIMERA_INCAPACIDAD") String fechaPrimeraIncapacidad,
        @JsonProperty("ST2") String st2,
        @JsonProperty("SALARIO_ACUMULADO") String salarioAcumulado,
        @JsonProperty("ALTA") String alta
) {
}
