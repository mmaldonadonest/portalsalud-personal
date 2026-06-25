package com.onest.app.web;

import com.onest.app.security.service.PortalUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Year;
import java.util.List;
import java.util.Map;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class ViewModelAdvice {

    private static final String DEFAULT_AVATAR = "/theme/assets/img/img1.jpg";

    @ModelAttribute
    public void populateCommonAttributes(Model model, HttpServletRequest request) {
        Map<String, Object> attributes = model.asMap();
        attributes.put("appName", "Portal Salud Personal");
        attributes.put("currentPath", request.getRequestURI());
        attributes.put("currentYear", Year.now().getValue());
        attributes.putIfAbsent("notificationCount", 0);
        attributes.putIfAbsent("notifications", List.of());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            attributes.put("currentUserName", "Usuario del portal");
            attributes.put("currentUserRole", "Perfil pendiente");
            attributes.put("currentUserAvatar", DEFAULT_AVATAR);
            return;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof PortalUserPrincipal userPrincipal) {
            attributes.put("currentUserName", userPrincipal.getDisplayName());
            attributes.put("currentUserRole", authentication.getAuthorities().stream()
                    .findFirst()
                    .map(authority -> authority.getAuthority().replace("ROLE_", ""))
                    .orElse("Usuario"));
            attributes.put("currentUserAvatar", userPrincipal.getAvatar());
            return;
        }

        if (principal instanceof UserDetails userDetails) {
            attributes.put("currentUserName", userDetails.getUsername());
            attributes.put("currentUserRole", authentication.getAuthorities().stream()
                    .findFirst()
                    .map(authority -> authority.getAuthority().replace("ROLE_", ""))
                    .orElse("Usuario"));
            attributes.put("currentUserAvatar", DEFAULT_AVATAR);
            return;
        }

        attributes.put("currentUserName", "Usuario del portal");
        attributes.put("currentUserRole", "Perfil pendiente");
        attributes.put("currentUserAvatar", DEFAULT_AVATAR);
    }
}
