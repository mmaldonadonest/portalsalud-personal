package mx.saludocupacional.portal.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.saludocupacional.portal.security.domain.User;
import mx.saludocupacional.portal.security.repository.UserRepository;
import mx.saludocupacional.portal.security.web.dto.AuthDtos.CambioPasswordRequest;
import mx.saludocupacional.portal.security.web.dto.AuthDtos.LoginRequest;
import mx.saludocupacional.portal.security.web.dto.AuthDtos.LoginResponse;
import mx.saludocupacional.portal.security.web.dto.AuthDtos.UsuarioResponse;
import mx.saludocupacional.portal.shared.audit.AuditAction;
import mx.saludocupacional.portal.shared.audit.AuditService;
import mx.saludocupacional.portal.shared.config.PortalProperties;
import mx.saludocupacional.portal.shared.exception.BusinessRuleException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Inicio de sesión y gestión de la contraseña propia.
 *
 * <p>Un intento fallido devuelve siempre el mismo mensaje, sin revelar si el
 * correo existe. Cada inicio de sesión queda registrado en la bitácora.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final PortalProperties properties;

    @Transactional
    public LoginResponse login(LoginRequest request, String ipOrigen) {
        User usuario = userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(request.email())
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));

        if (!usuario.isActivo()) {
            throw new BadCredentialsException("Credenciales inválidas");
        }
        if (!passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            log.warn("Intento de acceso fallido para {}", request.email());
            throw new BadCredentialsException("Credenciales inválidas");
        }

        usuario.setUltimoAcceso(OffsetDateTime.now());
        userRepository.save(usuario);

        auditService.registrarAcceso(AuditAction.LOGIN, usuario.getId(), ipOrigen);

        return new LoginResponse(
                jwtService.generar(usuario),
                properties.getJwt().getExpirationMinutes(),
                perfil(usuario));
    }

    @Transactional
    public void cambiarPassword(Long usuarioId, CambioPasswordRequest request) {
        User usuario = userRepository.findByIdAndDeletedAtIsNull(usuarioId)
                .orElseThrow(() -> new BadCredentialsException("Sesión inválida"));

        if (!passwordEncoder.matches(request.passwordActual(), usuario.getPasswordHash())) {
            throw new BusinessRuleException("La contraseña actual no es correcta");
        }
        if (passwordEncoder.matches(request.passwordNueva(), usuario.getPasswordHash())) {
            throw new BusinessRuleException("La nueva contraseña debe ser distinta de la actual");
        }

        usuario.setPasswordHash(passwordEncoder.encode(request.passwordNueva()));
        userRepository.save(usuario);

        // Se registra el hecho, nunca la contraseña.
        auditService.registrar(AuditAction.UPDATE, "seguridad", "User", usuario.getId(),
                null, null, usuarioId, false);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse perfilActual(Long usuarioId) {
        User usuario = userRepository.findByIdAndDeletedAtIsNull(usuarioId)
                .orElseThrow(() -> new BadCredentialsException("Sesión inválida"));
        return perfil(usuario);
    }

    public void registrarSalida(Long usuarioId, String ipOrigen) {
        auditService.registrarAcceso(AuditAction.LOGOUT, usuarioId, ipOrigen);
    }

    private UsuarioResponse perfil(User usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getNombre(),
                usuario.getRole().getNombre(),
                usuario.getPermisos(),
                usuario.getPrediosPermitidos(),
                usuario.tieneAlcanceGlobal());
    }
}
