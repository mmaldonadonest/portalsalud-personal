package com.onest.app.catalog.nss.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Body del POST a .../Servcio/Medico para el caso 'ALTA'.
 * Replica el $postData del case 'ALTA' de app::sendHeredoFamDats() en php-old/app/app.php.
 */
public record BiowsAltaMedicoRequest(
        @JsonProperty("NNS") String nns,
        @JsonProperty("FECHA") String fecha,
        @JsonProperty("TIPO_REGISTROS") String tipoRegistros,
        @JsonProperty("TIPO_EMPLEADO") String tipoEmpleado,
        @JsonProperty("USUARIO_CAMBIO") String usuarioCambio,
        @JsonProperty("OTRAS") Otras otras
) {

    public record Otras(
            @JsonProperty("OTRAS") String otras,
            @JsonProperty("OTRAS_OBS") String otrasObs
    ) {
    }
}
