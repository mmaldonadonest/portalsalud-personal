package com.onest.app.audit.web;

import com.onest.app.audit.service.AuditoriaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.StringJoiner;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registra en la bitacora los handlers marcados con {@link Auditado} cuando terminan con
 * exito (2xx/3xx y sin excepcion). Los valores del registro y del detalle salen de los
 * request params o de las path variables, por nombre - sin tocar los servicios.
 */
@Component
public class AuditoriaInterceptor implements HandlerInterceptor, WebMvcConfigurer {

    private static final int MAX_VALOR = 200;

    private final AuditoriaService auditoriaService;

    public AuditoriaInterceptor(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (ex != null || response.getStatus() >= 400 || !(handler instanceof HandlerMethod hm)) {
            return;
        }
        Auditado a = hm.getMethodAnnotation(Auditado.class);
        if (a == null) {
            return;
        }
        String registro = a.registro().isEmpty() ? null : valor(request, a.registro());
        StringJoiner detalle = new StringJoiner(" · ");
        for (String nombre : a.detalle()) {
            String v = valor(request, nombre);
            if (v != null && !v.isBlank()) {
                detalle.add(nombre + "=" + v);
            }
        }
        auditoriaService.registrar(a.modulo(), a.accion(), a.entidad(), registro,
                detalle.length() == 0 ? null : detalle.toString(), request);
    }

    @SuppressWarnings("unchecked")
    private static String valor(HttpServletRequest request, String nombre) {
        String v = request.getParameter(nombre);
        if (v == null) {
            Object vars = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
            if (vars instanceof Map<?, ?> m && m.get(nombre) != null) {
                v = String.valueOf(((Map<String, ?>) m).get(nombre));
            }
        }
        if (v == null) {
            return null;
        }
        String t = v.trim().replaceAll("\\s+", " ");
        return t.length() <= MAX_VALOR ? t : t.substring(0, MAX_VALOR) + "…";
    }
}
