package com.onest.app.security.legacy.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Respuesta del WS .../login/registro_app.
 * Fiel a php-old/rest/login/checklogApi.php: la respuesta trae un arreglo
 * {@code Datos} de NIVEL SUPERIOR y el login es correcto si el ultimo
 * {@code Datos[].Mensaje == "Login correcto"} (no hay envoltorio Resultado aqui,
 * a diferencia de otros WS del ORDS).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BiowsLoginResponse(
        @JsonProperty("Datos") List<Dato> datos
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Dato(
            @JsonProperty("Mensaje") String mensaje
    ) {
    }
}
