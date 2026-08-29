package com.onest.app.security.legacy;

import com.onest.app.catalog.nss.client.EmailLookupClient;
import com.onest.app.catalog.nss.client.NssSearchClient;
import com.onest.app.catalog.nss.dto.EmpleadoDto;
import com.onest.app.integration.intranet.IntranetUserClient;
import com.onest.app.security.model.AppSecUser;
import com.onest.app.security.service.AvatarDefaults;
import com.onest.app.security.repository.AppSecUserRepository;
import com.onest.app.security.service.PortalUserPrincipal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * (BiowsLoginClient) - unico proveedor bajo portal.auth.strategy=LEGACY_PHP
 * (WS = fuente de verdad del password para todos los usuarios, incluido el
 * admin del panel /admin, ver docs/plan-rbac-local.md).
 *
 * <p>El password SIEMPRE se valida contra ORDS. Los roles/autoridades de Spring
 * (ej. ROLE_ADMIN) se resuelven aparte consultando APP_SEC_USER/APP_SEC_USER_ROLE -
 * mismo criterio ya usado para menus (BD local = autorizacion, ORDS = autenticacion).
 * Un usuario sin fila local (la gran mayoria) queda sin ninguna autoridad - no bloquea
 * el login, solo significa que no cae en ningun gate basado en rol (ej. /admin/**).
 */
@Component
public class LegacyPhpAuthenticationProvider implements AuthenticationProvider {

    private static final Logger log = LoggerFactory.getLogger(LegacyPhpAuthenticationProvider.class);
    private static final String DEFAULT_AVATAR = AvatarDefaults.DEFAULT_AVATAR;

    private final BiowsLoginClient loginClient;
    private final AppSecUserRepository userRepository;
    private final NssSearchClient nssSearchClient;
    private final EmailLookupClient emailLookupClient;
    private final IntranetUserClient intranetUserClient;

    public LegacyPhpAuthenticationProvider(
            BiowsLoginClient loginClient, AppSecUserRepository userRepository, NssSearchClient nssSearchClient,
            EmailLookupClient emailLookupClient, IntranetUserClient intranetUserClient) {
        this.loginClient = loginClient;
        this.userRepository = userRepository;
        this.nssSearchClient = nssSearchClient;
        this.emailLookupClient = emailLookupClient;
        this.intranetUserClient = intranetUserClient;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String usuario = authentication.getName();
        String password = authentication.getCredentials() == null ? null : authentication.getCredentials().toString();

        if (usuario == null || password == null || !loginClient.autenticar(usuario, password)) {
            throw new BadCredentialsException("Credenciales invalidas");
        }

        Optional<AppSecUser> localUser = userRepository.findActiveByIdentifier(usuario);
        List<GrantedAuthority> authorities = resolveAuthorities(localUser);
        Long localId = localUser.map(AppSecUser::getId).orElse(null);
        Optional<EmpleadoDto> empleado = resolveEmpleado(usuario);
        String displayName = empleado.map(this::nombreCompleto)
                .filter(nombre -> nombre != null && !nombre.isBlank())
                .orElse(usuario);
        String email = emailLookupClient.emailPorNss(usuario).orElse(null);
        String avatar = Optional.ofNullable(email)
                .flatMap(intranetUserClient::avatarUrlPorEmail)
                .orElse(DEFAULT_AVATAR);
        log.info("[login] usuario={} email={} avatar={}", usuario, email, avatar);
        PortalUserPrincipal principal = new PortalUserPrincipal(
                localId, usuario, displayName, avatar, null, authorities);
        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    /**
     * Datos del empleado via Catalogo/usuario (WS ya conectado, mismo patron que
     * PortalUserDetailsService.resolveEmail: NssSearchClient.findUsuario() directo,
     * NO NssSearchService.findByNss(), que puede disparar una ALTA en Servcio/Medico
     * como efecto colateral - no se quiere eso corriendo en cada login). Una sola
     * llamada, reusada para nombre y para el email que alimenta el avatar (evita
     * pegarle dos veces al WS por login). Cualquier fallo de red cae a Optional.empty()
     * sin romper el login.
     */
    private Optional<EmpleadoDto> resolveEmpleado(String usuario) {
        try {
            return nssSearchClient.findUsuario(usuario, "LOGIN");
        } catch (Exception ex) {
            log.warn("[login] no se pudo resolver datos via Catalogo/usuario para {}: {}", usuario, ex.getMessage());
            return Optional.empty();
        }
    }

    private String nombreCompleto(EmpleadoDto empleado) {
        return Stream.of(empleado.nombre(), empleado.apellidoPaterno(), empleado.apellidoMaterno())
                .filter(parte -> parte != null && !parte.isBlank())
                .reduce((a, b) -> a + " " + b)
                .orElse(null);
    }

    /**
     * Solo los roles locales del usuario, si tiene fila en APP_SEC_USER (ej.
     * ROLE_ADMIN para el gate de /admin). La gran mayoria no tiene ninguno todavia
     * y queda con lista vacia - .anyRequest().authenticated() no exige ninguna
     * autoridad especifica, solo estar autenticado, asi que esto no bloquea nada.
     */
    private List<GrantedAuthority> resolveAuthorities(Optional<AppSecUser> localUser) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        localUser.ifPresent(user -> user.getRoles()
                .forEach(role -> authorities.add(new SimpleGrantedAuthority(role.getCode()))));
        return authorities;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
