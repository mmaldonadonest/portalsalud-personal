package mx.saludocupacional.portal.shared.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Único punto donde las excepciones se convierten en respuestas HTTP.
 *
 * <p>Ningún controlador captura excepciones por su cuenta: todas llegan aquí,
 * garantizando que el formato del error sea idéntico en toda la API.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> noEncontrado(ResourceNotFoundException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "No encontrado", ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> reglaDeNegocio(BusinessRuleException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Regla de negocio", ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, List<String>> campos = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            campos.computeIfAbsent(error.getField(), k -> new java.util.ArrayList<>())
                  .add(error.getDefaultMessage());
        }
        return ResponseEntity.badRequest()
                .body(ErrorResponse.validacion(req.getRequestURI(), campos));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> accesoDenegado(AccessDeniedException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(403, "Acceso denegado",
                        "No cuentas con el permiso necesario para esta operación", req.getRequestURI()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> noAutenticado(AuthenticationException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(401, "No autenticado",
                        "Credenciales inválidas o sesión expirada", req.getRequestURI()));
    }

    /** Red de seguridad: cualquier fallo no previsto se registra y se oculta al cliente. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> inesperado(Exception ex, HttpServletRequest req) {
        log.error("Error no controlado en {}", req.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(500, "Error interno",
                        "Ocurrió un error inesperado. Consulta al administrador.", req.getRequestURI()));
    }
}
