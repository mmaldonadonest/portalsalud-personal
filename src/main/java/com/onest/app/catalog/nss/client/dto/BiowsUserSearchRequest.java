package com.onest.app.catalog.nss.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Body del POST a .../Catalogo/usuario.
 * Replica exactamente el $postData de app::searchUser() en php-old/app/app.php:
 * Nss, Fecha_consulta, Usuario_consulta, Aplicacion_id, Id_aplicacion.
 * La busqueda por NSS NO incluye ningun campo "type".
 */
public record BiowsUserSearchRequest(
        @JsonProperty("Nss") String nss,
        @JsonProperty("Fecha_consulta") String fechaConsulta,
        @JsonProperty("Usuario_consulta") String usuarioConsulta,
        @JsonProperty("Aplicacion_id") String aplicacionId,
        @JsonProperty("Id_aplicacion") String idAplicacion
) {
}
