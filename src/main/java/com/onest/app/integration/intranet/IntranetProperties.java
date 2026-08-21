package com.onest.app.integration.intranet;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracion de la intranet Onest (foto de perfil por email).
 * GET {baseUrl}/api/v1/user/mail/{email} con header Authorization: Bearer {token}.
 */
@ConfigurationProperties(prefix = "portal.intranet")
public record IntranetProperties(
        String baseUrl,
        String token
) {
}
