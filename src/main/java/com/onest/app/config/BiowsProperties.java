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
        String appId,
        // 25-nov-2026: base SOLO del login del formulario (.../login/registro_app). Se migra a los servicios de la
        // base de SSO para que cada usuario tenga una sola contrasena. Vacio = la misma base-url de siempre.
        String loginBaseUrl,
        // 26-nov-2026: ruta del WS de login. Vacio = /login/registro_app (el de siempre). En SSO, el clon
        // /login/registro_app_v2 acepta la app 13 y la 27 (PORTAL SALUD del launcher) sin tocar el original.
        String loginPath
) {

    /** Base que usa el login: la de SSO si esta configurada; si no, la de biows de siempre. */
    public String loginBaseUrlEfectiva() {
        return loginBaseUrl == null || loginBaseUrl.isBlank() ? baseUrl : loginBaseUrl.trim();
    }

    /** Ruta del WS de login: la configurada o /login/registro_app. */
    public String loginPathEfectivo() {
        return loginPath == null || loginPath.isBlank() ? "/login/registro_app" : loginPath.trim();
    }
}
