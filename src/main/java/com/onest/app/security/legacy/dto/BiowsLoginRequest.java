package com.onest.app.security.legacy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Body del POST a .../login/registro_app.
 * Replica el $postData de app::loginApiRest() en php-old/app/app.php.
 */
public record BiowsLoginRequest(
        @JsonProperty("id_usuario") String idUsuario,
        @JsonProperty("id_password") String idPassword,
        @JsonProperty("id_app") String idApp
) {
}
