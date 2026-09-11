package com.onest.app.security.service;

import com.onest.app.catalog.nss.client.EmailLookupClient;
import com.onest.app.catalog.nss.client.NssSearchClient;
import com.onest.app.catalog.nss.dto.EmpleadoDto;
import com.onest.app.integration.intranet.IntranetUserClient;
import com.onest.app.security.model.AppSecUser;
import com.onest.app.security.repository.AppSecUserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * Arma el {@link PortalUserPrincipal} (autoridades locales + datos de empleado + avatar) para
 * un usuario ya autenticado, sin importar el metodo de autenticacion. Extraido de
 * {@code LegacyPhpAuthenticationProvider} para que el login SSO del launcher (que valida
 * identidad via JWT, no password) reuse exactamente el mismo armado de sesion en vez de
 * duplicarlo - roles/autoridades SIEMPRE salen de APP_SEC_USER/APP_SEC_USER_ROLE local
 * (BD = autorizacion), nunca del proveedor de autenticacion (ORDS o JWT del IdP).
 */
@Component
public class PortalPrincipalResolver {

    private static final Logger log = LoggerFactory.getLogger(PortalPrincipalResolver.class);

    private final AppSecUserRepository userRepository;
    private final NssSearchClient nssSearchClient;
    private final EmailLookupClient emailLookupClient;
    private final IntranetUserClient intranetUserClient;

    public PortalPrincipalResolver(
            AppSecUserRepository userRepository, NssSearchClient nssSearchClient,
            EmailLookupClient emailLookupClient, IntranetUserClient intranetUserClient) {
        this.userRepository = userRepository;
        this.nssSearchClient = nssSearchClient;
        this.emailLookupClient = emailLookupClient;
        this.intranetUserClient = intranetUserClient;
    }

    /** Login por NSS (password legacy o admin local) - el email para el avatar se resuelve via
     *  Servcio/usuario_email (NSS -> email), porque "usuario" aqui SI es un NSS. */
    public PortalUserPrincipal resolve(String usuario) {
        return resolve(usuario, emailLookupClient.emailPorNss(usuario).orElse(null));
    }

    /**
     * Login SSO (ver SsoLoginController) - "usuario" aqui es el email que ya vino del claim
     * del JWT, NO un NSS, asi que NO se debe llamar emailLookupClient.emailPorNss (espera un
     * NSS como llave; con un email nunca encuentra nada y el avatar se queda en el default).
     * El avatar de Intranet SI acepta email directo (IntranetUserClient.avatarUrlPorEmail) -
     * mismo WS que ya se usaba, solo sin el paso intermedio de "NSS -> email" que aqui sobra.
     */
    public PortalUserPrincipal resolveConEmailConocido(String usuario, String emailConocido) {
        return resolve(usuario, emailConocido);
    }

    private PortalUserPrincipal resolve(String usuario, String email) {
        Optional<AppSecUser> localUser = userRepository.findActiveByIdentifier(usuario);
        List<GrantedAuthority> authorities = resolveAuthorities(localUser);
        Long localId = localUser.map(AppSecUser::getId).orElse(null);
        Optional<EmpleadoDto> empleado = resolveEmpleado(usuario);
        String displayName = empleado.map(this::nombreCompleto)
                .filter(nombre -> nombre != null && !nombre.isBlank())
                .orElse(usuario);
        String avatar = Optional.ofNullable(email)
                .flatMap(intranetUserClient::avatarUrlPorEmail)
                .orElse(AvatarDefaults.DEFAULT_AVATAR);
        return new PortalUserPrincipal(localId, usuario, displayName, avatar, null, authorities);
    }

    /**
     * Ver comentario equivalente que tenia LegacyPhpAuthenticationProvider: una sola llamada a
     * Catalogo/usuario, reusada para nombre y para el email que alimenta el avatar. Cualquier
     * fallo de red cae a Optional.empty() sin romper el login.
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

    private List<GrantedAuthority> resolveAuthorities(Optional<AppSecUser> localUser) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        localUser.ifPresent(user -> user.getRoles()
                .forEach(role -> authorities.add(new SimpleGrantedAuthority(role.getCode()))));
        return authorities;
    }
}
