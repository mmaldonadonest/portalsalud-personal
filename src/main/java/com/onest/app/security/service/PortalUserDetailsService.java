package com.onest.app.security.service;

import com.onest.app.catalog.nss.client.NssSearchClient;
import com.onest.app.catalog.nss.dto.EmpleadoDto;
import com.onest.app.integration.intranet.IntranetUserClient;
import com.onest.app.security.model.AppSecUser;
import com.onest.app.security.repository.AppSecUserRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class PortalUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(PortalUserDetailsService.class);
    private static final String DEFAULT_AVATAR = "/theme/assets/img/img1.jpg";

    private final AppSecUserRepository userRepository;
    private final NssSearchClient nssSearchClient;
    private final IntranetUserClient intranetUserClient;

    public PortalUserDetailsService(
            AppSecUserRepository userRepository,
            NssSearchClient nssSearchClient,
            IntranetUserClient intranetUserClient) {
        this.userRepository = userRepository;
        this.nssSearchClient = nssSearchClient;
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
     * El email real vive en bio_empleado (WS Catalogo/usuario, ver
     * docs/ords-catalogo-usuario-email.sql) - se prefiere sobre APP_SEC_USER.EMAIL
     * (que puede ser un placeholder local, ej. el seed de pruebas). Se usa
     * NssSearchClient.findUsuario() directo (NO NssSearchService.findByNss(), que
     * tiene el efecto colateral de poder disparar una ALTA en Servcio/Medico - no
     * se quiere eso corriendo en cada login). Cualquier fallo de red cae al email
     * local sin romper el login.
     */
    private String resolveEmail(AppSecUser user) {
        try {
            return nssSearchClient.findUsuario(user.getUsername(), "LOGIN")
                    .map(EmpleadoDto::email)
                    .filter(email -> email != null && !email.isBlank())
                    .orElseGet(user::getEmail);
        } catch (Exception ex) {
            log.warn("[login] no se pudo resolver el email via Catalogo/usuario para {}: {}", user.getUsername(), ex.getMessage());
            return user.getEmail();
        }
    }
}
