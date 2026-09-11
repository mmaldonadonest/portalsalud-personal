package mx.saludocupacional.portal.security.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.saludocupacional.portal.security.service.AuthService;
import mx.saludocupacional.portal.security.service.PortalUserDetails;
import mx.saludocupacional.portal.security.web.dto.AuthDtos.CambioPasswordRequest;
import mx.saludocupacional.portal.security.web.dto.AuthDtos.LoginRequest;
import mx.saludocupacional.portal.security.web.dto.AuthDtos.LoginResponse;
import mx.saludocupacional.portal.security.web.dto.AuthDtos.UsuarioResponse;
import mx.saludocupacional.portal.shared.config.PortalProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Puntos de entrada de autenticación.
 *
 * <p>Devuelve el token en el cuerpo, para las llamadas desde JavaScript, y
 * también en una cookie de solo servidor, para que la navegación con Thymeleaf
 * funcione sin manipular el almacenamiento del navegador.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PortalProperties properties;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest servletRequest) {
        LoginResponse respuesta = authService.login(request, ipDe(servletRequest));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieSesion(respuesta.token()).toString())
                .body(respuesta);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal PortalUserDetails usuario,
                                       HttpServletRequest servletRequest) {
        if (usuario != null) {
            authService.registrarSalida(usuario.getUsuarioId(), ipDe(servletRequest));
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieVacia().toString())
                .build();
    }

    @GetMapping("/perfil")
    public ResponseEntity<UsuarioResponse> perfil(@AuthenticationPrincipal PortalUserDetails usuario) {
        return ResponseEntity.ok(authService.perfilActual(usuario.getUsuarioId()));
    }

    @PostMapping("/password")
    public ResponseEntity<Void> cambiarPassword(@AuthenticationPrincipal PortalUserDetails usuario,
                                                @Valid @RequestBody CambioPasswordRequest request) {
        authService.cambiarPassword(usuario.getUsuarioId(), request);
        return ResponseEntity.noContent().build();
    }

    private ResponseCookie cookieSesion(String token) {
        return ResponseCookie.from(JwtAuthenticationFilter.COOKIE_SESION, token)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofMinutes(properties.getJwt().getExpirationMinutes()))
                .build();
    }

    private ResponseCookie cookieVacia() {
        return ResponseCookie.from(JwtAuthenticationFilter.COOKIE_SESION, "")
                .httpOnly(true).secure(true).sameSite("Lax").path("/").maxAge(0).build();
    }

    /** Prefiere la dirección original cuando la petición pasó por un proxy. */
    private String ipDe(HttpServletRequest request) {
        String reenviada = request.getHeader("X-Forwarded-For");
        if (reenviada != null && !reenviada.isBlank()) {
            return reenviada.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
