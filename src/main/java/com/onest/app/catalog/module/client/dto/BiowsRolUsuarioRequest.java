package com.onest.app.catalog.module.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Body del POST a .../info/consulta_app_rol_usuario.
 * Replica el $postData de loadParms() en php-old/rest/validateModules.php: {id_usuario, id_app}.
 */
public record BiowsRolUsuarioRequest(
        @JsonProperty("id_usuario") String idUsuario,
        @JsonProperty("id_app") String idApp
) {
}
