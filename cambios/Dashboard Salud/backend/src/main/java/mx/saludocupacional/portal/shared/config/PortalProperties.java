package mx.saludocupacional.portal.shared.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración propia del portal, definida bajo el prefijo {@code portal} en
 * los archivos de configuración.
 *
 * <p>Los valores sensibles llegan por variable de entorno; el repositorio no
 * contiene secretos reales.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "portal")
public class PortalProperties {

    private Jwt jwt = new Jwt();
    private Cors cors = new Cors();

    @Getter
    @Setter
    public static class Jwt {
        /** Clave de firma; debe superar los treinta y dos caracteres. */
        private String secret;
        private long expirationMinutes = 480;
        private String issuer = "portal-salud-ocupacional";
    }

    @Getter
    @Setter
    public static class Cors {
        private String allowedOrigins = "http://localhost:8080";
    }
}
