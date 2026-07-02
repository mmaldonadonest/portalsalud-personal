package com.onest.app.catalog.module.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Respuesta de .../info/consulta_app_rol_usuario. php-old lee
 * $data['Resultado']['Datos'][0]['id_rol'].
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BiowsRolUsuarioResponse(
        @JsonProperty("Resultado") Resultado resultado
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Resultado(
            @JsonProperty("Datos") List<Dato> datos
    ) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Dato(
                @JsonProperty("id_rol") String idRol
        ) {
        }
    }
}
