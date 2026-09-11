package mx.saludocupacional.portal.shared.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Habilita las capacidades transversales de la aplicación.
 *
 * <p>La caché se usa únicamente para datos de lectura muy frecuente y cambio
 * poco habitual, como los umbrales de riesgo y los catálogos. Toda escritura
 * sobre esos datos invalida su entrada correspondiente.
 */
@Configuration
@EnableCaching
@EnableConfigurationProperties(PortalProperties.class)
public class ApplicationConfig {
}
