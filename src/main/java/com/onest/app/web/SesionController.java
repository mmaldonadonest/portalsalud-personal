package com.onest.app.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Estado de la sesion para static/js/sesion.js (13-nov-2026). Lo usan el aviso de «¿Necesitas mas
 * tiempo?» (al contestar Si, esta llamada misma extiende la sesion) y el reintento de un guardado
 * rechazado por token CSRF viejo: tras un reinicio del Tomcat o un nuevo login en otra pestana la
 * pagina conserva el token de la sesion anterior y el servidor respondia 403, que las pantallas
 * mostraban como «No tienes permiso». Si la sesion ya no existe, Spring Security redirige al
 * login antes de llegar aqui: eso es justo lo que detecta el JS.
 */
@RestController
public class SesionController {

    @GetMapping(value = "/api/sesion", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> sesion(HttpServletRequest request, CsrfToken csrf) {
        HttpSession sesion = request.getSession(false);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("segundos", sesion == null ? 0 : sesion.getMaxInactiveInterval());
        r.put("csrfHeader", csrf.getHeaderName());
        r.put("csrfParam", csrf.getParameterName());
        r.put("csrfToken", csrf.getToken());
        return r;
    }
}
