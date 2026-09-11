package com.onest.app.security.legacy;

import com.onest.app.security.service.PortalPrincipalResolver;
import com.onest.app.security.service.PortalUserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

/**
 * AuthenticationProvider que valida credenciales contra el login legacy ORDS
 * (BiowsLoginClient) - unico proveedor bajo portal.auth.strategy=LEGACY_PHP
 * (WS = fuente de verdad del password para todos los usuarios, incluido el
 * admin del panel /admin, ver docs/plan-rbac-local.md).
 *
 * <p>El password SIEMPRE se valida contra ORDS. El armado del principal (roles
 * locales + datos de empleado + avatar) vive en {@link PortalPrincipalResolver},
 * compartido con el login SSO del launcher (ver SsoLoginController) - la unica
 * diferencia entre ambos flujos es COMO se prueba la identidad, no como se arma
 * la sesion despues.
 */
@Component
public class LegacyPhpAuthenticationProvider implements AuthenticationProvider {

    private static final Logger log = LoggerFactory.getLogger(LegacyPhpAuthenticationProvider.class);

    private final BiowsLoginClient loginClient;
    private final PortalPrincipalResolver principalResolver;

    public LegacyPhpAuthenticationProvider(BiowsLoginClient loginClient, PortalPrincipalResolver principalResolver) {
        this.loginClient = loginClient;
        this.principalResolver = principalResolver;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String usuario = authentication.getName();
        String password = authentication.getCredentials() == null ? null : authentication.getCredentials().toString();

        if (usuario == null || password == null || !loginClient.autenticar(usuario, password)) {
            throw new BadCredentialsException("Credenciales invalidas");
        }

        PortalUserPrincipal principal = principalResolver.resolve(usuario);
        log.info("[login] usuario={} autoridades={}", usuario, principal.getAuthorities());
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
