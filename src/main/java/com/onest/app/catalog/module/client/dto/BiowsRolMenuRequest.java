package com.onest.app.catalog.module.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Body del POST a .../info/consulta_app_rol_menu.
 * Replica el $postData de loadMenus() en php-old/rest/validateModules.php: {id_app, id_rol}.
 */
public record BiowsRolMenuRequest(
        @JsonProperty("id_app") String idApp,
        @JsonProperty("id_rol") String idRol
) {
}
