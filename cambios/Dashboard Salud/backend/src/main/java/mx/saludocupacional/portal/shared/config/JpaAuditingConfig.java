package mx.saludocupacional.portal.shared.config;

import mx.saludocupacional.portal.security.service.PortalUserDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Identifica al usuario responsable de cada escritura.
 *
 * <p>Spring Data consulta este componente al persistir cualquier entidad y
 * completa {@code created_by} y {@code updated_by} sin que los servicios tengan
 * que asignarlos. Durante migraciones y tareas automáticas no hay usuario
 * autenticado y los campos quedan vacíos.
 */
@Configuration
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<Long> auditorProvider() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()
                    || !(auth.getPrincipal() instanceof PortalUserDetails detalles)) {
                return Optional.empty();
            }
            return Optional.ofNullable(detalles.getUsuarioId());
        };
    }
}
