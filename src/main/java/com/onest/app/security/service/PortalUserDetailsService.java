package com.onest.app.security.service;

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

    private static final String DEFAULT_AVATAR = "/theme/assets/img/img1.jpg";

    private final AppSecUserRepository userRepository;

    public PortalUserDetailsService(AppSecUserRepository userRepository) {
        this.userRepository = userRepository;
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

        return new PortalUserPrincipal(
                user.getId(),
                user.getUsername(),
                displayName,
                DEFAULT_AVATAR,
                user.getPasswordHash(),
                authorities
        );
    }
}
