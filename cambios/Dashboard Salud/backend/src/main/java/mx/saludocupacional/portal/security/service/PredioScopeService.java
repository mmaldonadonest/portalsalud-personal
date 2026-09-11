package mx.saludocupacional.portal.security.service;

import mx.saludocupacional.portal.shared.exception.BusinessRuleException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * Resuelve qué predios puede consultar el usuario de la petición en curso.
 *
 * <p>Los roles con alcance limitado, como gerencia de predio, solo ven los
 * predios que tienen autorizados. Concentrar esta decisión aquí evita que cada
 * consulta reinvente el filtro y deje una fuga por descuido.
 */
@Service
public class PredioScopeService {

    /**
     * Predios por los que debe filtrarse la consulta.
     *
     * @return {@code null} cuando el usuario tiene alcance global, de modo que
     *         las consultas con {@code :prediosPermitidos IS NULL} omitan el filtro
     */
    public Collection<Long> filtroDePredios() {
        return usuarioActual()
                .filter(u -> !u.tieneAlcanceGlobal())
                .map(PortalUserDetails::getPrediosPermitidos)
                .filter(predios -> !predios.isEmpty())
                .map(predios -> (Collection<Long>) predios)
                .orElse(null);
    }

    /**
     * Comprueba que el usuario pueda operar sobre el predio indicado.
     *
     * @throws BusinessRuleException si el predio queda fuera de su alcance
     */
    public void verificarAcceso(Long predioId) {
        Optional<PortalUserDetails> usuario = usuarioActual();
        if (usuario.isEmpty() || usuario.get().tieneAlcanceGlobal()) {
            return;
        }
        Set<Long> permitidos = usuario.get().getPrediosPermitidos();
        if (!permitidos.isEmpty() && !permitidos.contains(predioId)) {
            throw new BusinessRuleException("No tienes acceso al predio solicitado");
        }
    }

    public Optional<Long> usuarioId() {
        return usuarioActual().map(PortalUserDetails::getUsuarioId);
    }

    private Optional<PortalUserDetails> usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof PortalUserDetails detalles)) {
            return Optional.empty();
        }
        return Optional.of(detalles);
    }
}
