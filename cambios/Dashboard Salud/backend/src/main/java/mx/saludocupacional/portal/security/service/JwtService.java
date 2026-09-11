package mx.saludocupacional.portal.security.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.saludocupacional.portal.security.domain.User;
import mx.saludocupacional.portal.shared.config.PortalProperties;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Emisión y verificación de los tokens de sesión.
 *
 * <p>El token incluye el identificador del usuario, su correo y su rol. Los
 * permisos no viajan en el token: se consultan en cada petición, de modo que
 * revocar un permiso surte efecto de inmediato sin esperar a que expire la
 * sesión.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROL = "rol";
    private static final String CLAIM_NOMBRE = "nombre";

    private final PortalProperties properties;

    public String generar(User usuario) {
        Instant ahora = Instant.now();
        Instant expiracion = ahora.plusSeconds(properties.getJwt().getExpirationMinutes() * 60);

        return Jwts.builder()
                .subject(String.valueOf(usuario.getId()))
                .claim(CLAIM_EMAIL, usuario.getEmail())
                .claim(CLAIM_ROL, usuario.getRole().getNombre())
                .claim(CLAIM_NOMBRE, usuario.getNombre())
                .issuer(properties.getJwt().getIssuer())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(expiracion))
                .signWith(clave())
                .compact();
    }

    /** Devuelve el identificador del usuario si el token es válido y no ha expirado. */
    public Optional<Long> extraerUsuarioId(String token) {
        return parsear(token).map(claims -> Long.valueOf(claims.getSubject()));
    }

    public Optional<String> extraerEmail(String token) {
        return parsear(token).map(claims -> claims.get(CLAIM_EMAIL, String.class));
    }

    public boolean esValido(String token) {
        return parsear(token).isPresent();
    }

    private Optional<Claims> parsear(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(clave())
                    .requireIssuer(properties.getJwt().getIssuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Token rechazado: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private SecretKey clave() {
        String secreto = properties.getJwt().getSecret();
        if (secreto == null || secreto.length() < 32) {
            throw new IllegalStateException(
                    "La propiedad portal.jwt.secret debe tener al menos 32 caracteres");
        }
        return Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
    }
}
