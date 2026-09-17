package com.onest.app.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Date;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * Todos los controllers de catalog/* (Consulta/Incapacidad/Antidoping/Accidente/Maternidad/
 * Examen/...) siguen el mismo patron: capturan IllegalArgumentException y la relanzan como
 * ResponseStatusException(BAD_REQUEST, ex.getMessage()). Sin este advice, Spring resuelve esa
 * excepcion via su /error por defecto, que para peticiones con Accept: text/plain devuelve
 * el body VACIO (confirmado en vivo 2026-08-21, ver docs/plan-tareas-concretas.html) - el
 * mensaje de validacion nunca llegaba al frontend pese a que el fetch leia response.text().
 * Esto intercepta la excepcion antes de que llegue al /error generico y devuelve el mensaje
 * real como texto plano, con el status code correcto.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public Object handleResponseStatus(ResponseStatusException ex, HttpServletRequest request) {
        String mensaje = ex.getReason();
        String texto = mensaje == null || mensaje.isBlank() ? ex.getMessage() : mensaje;
        // Navegacion a una PAGINA (no /api/**): pagina de error del portal (templates/error.html)
        // con el status y el mensaje, en vez de texto plano en blanco y negro. Los fetch de la
        // app pegan a /api/** y siguen recibiendo el mensaje como texto plano.
        if (!request.getServletPath().startsWith("/api/")) {
            ModelAndView mv = new ModelAndView("error", HttpStatus.valueOf(ex.getStatusCode().value()));
            mv.addObject("status", ex.getStatusCode().value());
            mv.addObject("message", texto);
            mv.addObject("path", request.getServletPath());
            mv.addObject("timestamp", new Date());
            mv.addObject("traceId", MDC.get("traceId"));
            return mv;
        }
        return ResponseEntity.status(ex.getStatusCode())
                .headers(headers -> headers.setContentType(MediaType.valueOf("text/plain;charset=UTF-8")))
                .body(texto);
    }
}
