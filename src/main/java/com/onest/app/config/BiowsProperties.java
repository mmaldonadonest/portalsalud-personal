package com.onest.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracion del web service ORDS legacy (biows).
 * Referencia: php-old/app/app.php (metodos curl_* que llaman a http://10.249.249.3/biows/ords/security/...).
 */
@ConfigurationProperties(prefix = "portal.biows")
public record BiowsProperties(
        String baseUrl,
        String apiKey,
        String aplicacionId,
        String idAplicacion,
        // id_app del portal (login / permisos de menu). En php-old es '13',
        // distinto de idAplicacion='4' (Biometrico) que usa la busqueda NSS.
        String appId
) {
}
