package com.onest.app.security.legacy;

import com.onest.app.security.service.PortalUserPrincipal;
import java.util.List;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * AuthenticationProvider que valida credenciales contra el login legacy ORDS
 * (BiowsLoginClient). Se usa en las estrategias LEGACY_PHP e HYBRID.
 * Los roles/menus se resuelven aparte en runtime (validateModules), asi que al
 * autenticar por legacy se concede un rol base ROLE_USER.
 */
@Component
public class LegacyPhpAuthenticationProvider implements AuthenticationProvider {

    private static final String DEFAULT_AVATAR = "/theme/assets/img/img1.jpg";

    private final BiowsLoginClient loginClient;

    public LegacyPhpAuthenticationProvider(BiowsLoginClient loginClient) {
        this.loginClient = loginClient;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String usuario = authentication.getName();
        String password = authentication.getCredentials() == null ? null : authentication.getCredentials().toString();

        if (usuario == null || password == null || !loginClient.autenticar(usuario, password)) {
            throw new BadCredentialsException("Credenciales invalidas");
        }

        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        PortalUserPrincipal principal = new PortalUserPrincipal(
                null, usuario, usuario, DEFAULT_AVATAR, null, authorities);
        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
