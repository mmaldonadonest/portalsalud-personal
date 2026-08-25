package com.onest.app.web;

import org.springframework.http.MediaType;
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
    public ResponseEntity<String> handleResponseStatus(ResponseStatusException ex) {
        String mensaje = ex.getReason();
        return ResponseEntity.status(ex.getStatusCode())
                .headers(headers -> headers.setContentType(MediaType.valueOf("text/plain;charset=UTF-8")))
                .body(mensaje == null || mensaje.isBlank() ? ex.getMessage() : mensaje);
    }
}
