package com.onest.app.catalog.module.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Respuesta de .../info/consulta_app_rol_menu. La lista de modulos viene en
 * Resultado.Datos[] (id_app, id_rol, id_menu, nombre_menu).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BiowsRolMenuResponse(
        @JsonProperty("Resultado") Resultado resultado
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Resultado(
            @JsonProperty("Estado") Integer estado,
            @JsonProperty("Mensaje") String mensaje,
            @JsonProperty("Datos") List<Dato> datos
    ) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Dato(
                @JsonProperty("id_app") Integer idApp,
                @JsonProperty("id_rol") String idRol,
                @JsonProperty("id_menu") Integer idMenu,
                @JsonProperty("nombre_menu") String nombreMenu
        ) {
        }
    }
}
