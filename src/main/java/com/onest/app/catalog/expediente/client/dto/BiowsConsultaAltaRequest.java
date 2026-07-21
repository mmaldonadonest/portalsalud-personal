package com.onest.app.catalog.expediente.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Body del POST a .../Servcio/consulta (alta de consulta medica).
 * Replica el $postData de app::addConsultM() en php-old/app/app.php.
 */
public record BiowsConsultaAltaRequest(
        @JsonProperty("FECHA") String fecha,
        @JsonProperty("NSS") String nss,
        @JsonProperty("TIPO_CONSULTA") String tipoConsulta,
        @JsonProperty("AREA_ACCIDENTE") String areaAccidente,
        @JsonProperty("AREA_ANATOMICA") String areaAnatomica,
        @JsonProperty("CAUSA") String causa,
        @JsonProperty("PESO") String peso,
        @JsonProperty("TALLA") String talla,
        @JsonProperty("IMC") String imc,
        @JsonProperty("FC") String fc,
        @JsonProperty("FR") String fr,
        @JsonProperty("TA") String ta,
        @JsonProperty("TEMPERATURA") String temperatura,
        @JsonProperty("MOTIVO_CONSULTA") String motivoConsulta,
        @JsonProperty("EXPLORACION_FISICA") String exploracionFisica,
        @JsonProperty("DIAGNOSTICO") String diagnostico,
        @JsonProperty("TRATAMIENTO") String tratamiento,
        @JsonProperty("CONSULTA_RELACIONADA") String consultaRelacionada,
        @JsonProperty("ID_ARCHIVOS") String idArchivos,
        @JsonProperty("USUARIO") String usuario,
        @JsonProperty("NOMBRE_USUARIO") String nombreUsuario,
        @JsonProperty("FIRMA_DIGITAL") String firmaDigital
) {
}
