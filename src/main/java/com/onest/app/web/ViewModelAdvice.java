package com.onest.app.web;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Year;
import java.util.List;
import java.util.Map;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class ViewModelAdvice {

    @ModelAttribute
    public void populateCommonAttributes(Model model, HttpServletRequest request) {
        Map<String, Object> attributes = model.asMap();
        attributes.put("appName", "Portal Salud Personal");
        attributes.put("currentPath", request.getRequestURI());
        attributes.put("currentYear", Year.now().getValue());
        attributes.putIfAbsent("currentUserName", "Usuario del portal");
        attributes.putIfAbsent("currentUserRole", "Perfil pendiente");
        attributes.putIfAbsent("currentUserAvatar", "/theme/assets/img/img1.jpg");
        attributes.putIfAbsent("notificationCount", 0);
        attributes.putIfAbsent("notifications", List.of());
    }
}
