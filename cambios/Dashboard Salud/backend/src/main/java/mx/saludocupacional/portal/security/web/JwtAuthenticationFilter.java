package mx.saludocupacional.portal.security.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import mx.saludocupacional.portal.security.service.JwtService;
import mx.saludocupacional.portal.security.service.PortalUserDetails;
import mx.saludocupacional.portal.security.service.PortalUserDetailsService;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

/**
 * Autentica cada petición a partir del token de sesión.
 *
 * <p>Acepta el token en la cabecera {@code Authorization} para las llamadas a la
 * API y en una cookie {@code portal_session} para la navegación con Thymeleaf,
 * de modo que ambas superficies compartan el mismo mecanismo.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String COOKIE_SESION = "portal_session";
    private static final String CABECERA = "Authorization";
    private static final String PREFIJO = "Bearer ";

    private final JwtService jwtService;
    private final PortalUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            extraerToken(request)
                    .flatMap(jwtService::extraerUsuarioId)
                    .ifPresent(usuarioId -> autenticar(usuarioId, request));
        }
        filterChain.doFilter(request, response);
    }

    private void autenticar(Long usuarioId, HttpServletRequest request) {
        try {
            PortalUserDetails detalles = userDetailsService.cargarPorId(usuarioId);
            if (!detalles.isEnabled()) {
                return;
            }
            var auth = new UsernamePasswordAuthenticationToken(
                    detalles, null, detalles.getAuthorities());
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (UsernameNotFoundException ex) {
            // Token válido de un usuario que ya no existe: la petición sigue sin autenticar.
            SecurityContextHolder.clearContext();
        }
    }

    private Optional<String> extraerToken(HttpServletRequest request) {
        String cabecera = request.getHeader(CABECERA);
        if (cabecera != null && cabecera.startsWith(PREFIJO)) {
            return Optional.of(cabecera.substring(PREFIJO.length()));
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(c -> COOKIE_SESION.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }
}
