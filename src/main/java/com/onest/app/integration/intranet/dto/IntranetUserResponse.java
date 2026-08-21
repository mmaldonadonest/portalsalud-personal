package com.onest.app.integration.intranet.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Respuesta de GET /api/v1/user/mail/{email}. Solo se declara "guid" (usado para
 * construir la URL de la foto de perfil) - se ignoran el resto de campos que
 * traiga la intranet, forma completa de la respuesta no confirmada.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IntranetUserResponse(
        @JsonProperty("guid") String guid
) {
}
