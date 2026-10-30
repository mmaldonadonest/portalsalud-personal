package com.onest.app.audit.web;

import com.onest.app.audit.service.UsuariosEnLinea;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Anota cada peticion autenticada en {@link UsuariosEnLinea}. Solo controladores: los estaticos no cuentan. */
@Component
public class UsuariosEnLineaInterceptor implements HandlerInterceptor, WebMvcConfigurer {

    private final UsuariosEnLinea usuariosEnLinea;

    public UsuariosEnLineaInterceptor(UsuariosEnLinea usuariosEnLinea) {
        this.usuariosEnLinea = usuariosEnLinea;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod) {
            usuariosEnLinea.registrar(request);
        }
        return true;
    }
}
