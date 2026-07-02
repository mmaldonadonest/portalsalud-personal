package com.onest.app.catalog.nss.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Respuesta del WS .../Catalogo/usuario. En php-old/app/app.php se recorre
 * $data->Datos como una lista de usuarios/empleados.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BiowsUserResponse(
        @JsonProperty("Datos") List<Dato> datos
) {

    /**
     * Campos usados por searchEmploye.php (mezcla de camelCase y snake_case tal como los devuelve el ORDS).
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Dato(
            @JsonProperty("nombre") String nombre,
            @JsonProperty("apellidoPaterno") String apellidoPaterno,
            @JsonProperty("apellidoMaterno") String apellidoMaterno,
            @JsonProperty("rfc") String rfc,
            @JsonProperty("nss") String nss,
            @JsonProperty("cuenta") String cuenta,
            @JsonProperty("fecha_nacimiento") String fechaNacimiento,
            @JsonProperty("estado_civil") String estadoCivil,
            @JsonProperty("sexo") String sexo,
            @JsonProperty("direccion") String direccion,
            @JsonProperty("celular") String celular,
            @JsonProperty("turno") String turno,
            @JsonProperty("tel_fijo") String telFijo,
            @JsonProperty("completo") String completo,
            @JsonProperty("nombre_puesto") String nombrePuesto,
            @JsonProperty("nombre_empresa") String nombreEmpresa
    ) {
    }
}
