package mx.saludocupacional.portal.security.service;

import lombok.RequiredArgsConstructor;
import mx.saludocupacional.portal.security.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Carga el usuario y sus permisos para el proceso de autenticación. */
@Service
@RequiredArgsConstructor
public class PortalUserDetailsService implements org.springframework.security.core.userdetails.UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public PortalUserDetails loadUserByUsername(String email) {
        return userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(email)
                .map(PortalUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas"));
    }

    @Transactional(readOnly = true)
    public PortalUserDetails cargarPorId(Long id) {
        return userRepository.findByIdAndDeletedAtIsNull(id)
                .map(PortalUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("Sesión inválida"));
    }
}
