package com.onest.app.security.sso;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Decoder del JWT que manda el launcher SSO (ver SsoLoginController). Si portal.sso.jwks-uri o
 * portal.sso.issuer-uri estan en blanco (default hasta que se configuren por ambiente), el
 * metodo devuelve null a proposito - Spring simplemente NO registra el bean en ese caso (no
 * lanza excepcion), asi que el resto de la app arranca normal y /sso/login responde
 * "no configurado" via ObjectProvider.getIfAvailable() en vez de tronar el arranque completo
 * con NimbusJwtDecoder.withJwkSetUri("") (que si lanza IllegalArgumentException).
 *
 * <p>No se activa oauth2ResourceServer() en SecurityConfiguration a proposito: el resto de
 * la app sigue siendo form-login/sesion, este decoder es solo una utilidad que usa
 * SsoLoginController para validar un token puntual, no un filtro de seguridad global.
 */
@Configuration
public class SsoJwtConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SsoJwtConfiguration.class);

    @Bean
    JwtDecoder ssoJwtDecoder(
            @Value("${portal.sso.jwks-uri:}") String jwksUri,
            @Value("${portal.sso.issuer-uri:}") String issuerUri) {
        if (jwksUri.isBlank() || issuerUri.isBlank()) {
            log.info("[sso] portal.sso.jwks-uri/issuer-uri sin configurar en este ambiente - /sso/login queda inactivo");
            return null;
        }
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuerUri));
        return decoder;
    }
}
