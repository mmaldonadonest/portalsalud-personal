package com.onest.app.web;

import com.onest.app.security.permission.PermissionService;
import com.onest.app.security.service.AvatarDefaults;
import com.onest.app.security.service.PortalUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class ViewModelAdvice {

    private static final String DEFAULT_AVATAR = AvatarDefaults.DEFAULT_AVATAR;

    private final PermissionService permissionService;
    private final String permissionsSource;
    private final com.onest.app.integration.intranet.IntranetUserClient intranetUserClient;
    private final com.onest.app.catalog.firma.repository.FirmaRepository firmas;

    public ViewModelAdvice(PermissionService permissionService,
                           @Value("${portal.permissions.source:ORDS}") String permissionsSource,
                           com.onest.app.integration.intranet.IntranetUserClient intranetUserClient,
                           com.onest.app.catalog.firma.repository.FirmaRepository firmas) {
        this.firmas = firmas;
        this.permissionService = permissionService;
        this.permissionsSource = permissionsSource;
        this.intranetUserClient = intranetUserClient;
    }

    @ModelAttribute
    public void populateCommonAttributes(Model model, HttpServletRequest request) {
        Map<String, Object> attributes = model.asMap();
        attributes.put("appName", "Portal Salud Personal");
        // getServletPath() (no getRequestURI()) - este ultimo incluye el context path
        // del despliegue en Tomcat externo (ej. /portal-salud/consumibles), y menu.html
        // compara currentPath contra literales sin ese prefijo (ej. '/consumibles'),
        // asi que el resaltado 'active' nunca aplicaba en un despliegue con context path.
        attributes.put("currentPath", request.getServletPath());
        attributes.put("currentYear", Year.now().getValue());
        attributes.putIfAbsent("notificationCount", 0);
        attributes.putIfAbsent("notifications", List.of());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            setDefaultUserAttributes(attributes);
            return;
        }

        // Menus por rol para el sidebar (grupo "Examenes"): con fuente LOCAL, los CODE de
        // SERV_MED_MENU_ROLE del usuario; con ORDS no hay codes -> null = el menu no filtra
        // (comportamiento anterior). Nunca lanza: un fallo aqui no debe tumbar la vista.
        attributes.put("menusPermitidos", menusPermitidos());
        // Firma digitalizada que un administrador cargo a nombre de este usuario y espera su aceptacion
        // (20-oct-2026): el encabezado lo avisa. Solo en paginas completas (no en /api/, que son
        // fragmentos e impresos) para no consultar en cada peticion. Nunca lanza.
        if (!request.getServletPath().startsWith("/api/")) {
            attributes.put("firmaPendiente", firmaPendiente(authentication.getName()));
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof PortalUserPrincipal userPrincipal) {
            attributes.put("currentUserName", userPrincipal.getDisplayName());
            attributes.put("currentUserRole", extractRoleName(authentication));
            // Un reintento por sesion si la foto fallo al entrar (ver PortalUserPrincipal)
            userPrincipal.reintentarAvatarSiFallo(intranetUserClient::avatarUrlPorEmail);
            attributes.put("currentUserAvatar", userPrincipal.getAvatar());
            return;
        }

        if (principal instanceof UserDetails userDetails) {
            attributes.put("currentUserName", userDetails.getUsername());
            attributes.put("currentUserRole", extractRoleName(authentication));
            attributes.put("currentUserAvatar", DEFAULT_AVATAR);
            return;
        }

        setDefaultUserAttributes(attributes);
    }

    private boolean firmaPendiente(String usuario) {
        try {
            return firmas.enEstado(usuario, "PENDIENTE").isPresent();
        } catch (RuntimeException ex) {
            return false; // sin tablas del modulo de firmas
        }
    }

    private Set<String> menusPermitidos() {
        if (!"LOCAL".equalsIgnoreCase(permissionsSource)) {
            return null;
        }
        try {
            return permissionService.modulosPermitidos().stream()
                    .map(m -> m.code())
                    .filter(c -> c != null && !c.isBlank())
                    .collect(Collectors.toSet());
        } catch (RuntimeException ex) {
            return Set.of();
        }
    }

    private void setDefaultUserAttributes(Map<String, Object> attributes) {
        attributes.put("currentUserName", "Usuario del portal");
        attributes.put("currentUserRole", "Perfil pendiente");
        attributes.put("currentUserAvatar", DEFAULT_AVATAR);
    }

    private String extractRoleName(Authentication authentication) {
        // distinct(): el mismo rol puede venir dos veces (p.ej. asignado en BD local y en ORDS)
        // y se veia "MEDICO_ANALISTA, MEDICO_ANALISTA, ADMIN" en el pie del sidebar.
        String roles = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority().replace("ROLE_", ""))
                .distinct()
                .collect(Collectors.joining(", "));
        return roles.isBlank() ? "Usuario" : roles;
    }
}
