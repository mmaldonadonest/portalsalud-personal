package com.onest.app.security.service;

import com.onest.app.catalog.nss.client.EmailLookupClient;
import com.onest.app.integration.intranet.IntranetUserClient;
import com.onest.app.security.model.AppSecUser;
import com.onest.app.security.repository.AppSecUserRepository;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class PortalUserDetailsService implements UserDetailsService {

    private static final String DEFAULT_AVATAR = AvatarDefaults.DEFAULT_AVATAR;

    private final AppSecUserRepository userRepository;
    private final EmailLookupClient emailLookupClient;
    private final IntranetUserClient intranetUserClient;

    public PortalUserDetailsService(
            AppSecUserRepository userRepository,
            EmailLookupClient emailLookupClient,
            IntranetUserClient intranetUserClient) {
        this.userRepository = userRepository;
        this.emailLookupClient = emailLookupClient;
        this.intranetUserClient = intranetUserClient;
    }

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        AppSecUser user = userRepository.findActiveByIdentifier(identifier)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getCode()))
                .toList();

        String displayName = user.getDisplayName();
        if (displayName == null || displayName.isBlank()) {
            displayName = user.getUsername();
        }

        // Se resuelve una sola vez aqui (login), no en cada request - avatar y
        // email quedan cacheados en el principal durante toda la sesion.
        String email = resolveEmail(user);
        String avatar = intranetUserClient.avatarUrlPorEmail(email).orElse(DEFAULT_AVATAR);

        return new PortalUserPrincipal(
                user.getId(),
                user.getUsername(),
                displayName,
                avatar,
                user.getPasswordHash(),
                authorities
        );
    }

    /**
     * El email real vive en bio_empleado, resuelto via el WS aislado
     * Servcio/usuario_email (ver docs/ords-usuario-email-nuevo.sql) - se prefiere
     * sobre SERV_MED_SEC_USER.EMAIL (que puede ser un placeholder local, ej. el seed de
     * pruebas). Cualquier fallo de red cae al email local sin romper el login.
     */
    private String resolveEmail(AppSecUser user) {
        return emailLookupClient.emailPorNss(user.getUsername()).orElseGet(user::getEmail);
    }
}
