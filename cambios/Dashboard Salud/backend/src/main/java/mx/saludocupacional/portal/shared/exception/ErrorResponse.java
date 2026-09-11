package mx.saludocupacional.portal.shared.exception;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Forma única de todo error devuelto por la API.
 *
 * @param timestamp momento en que ocurrió
 * @param status    código HTTP
 * @param error     nombre corto del error
 * @param message   descripción legible para el usuario
 * @param path      ruta que originó el error
 * @param campos    errores por campo, solo en fallos de validación
 */
public record ErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, List<String>> campos
) {

    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(OffsetDateTime.now(), status, error, message, path, null);
    }

    public static ErrorResponse validacion(String path, Map<String, List<String>> campos) {
        return new ErrorResponse(
                OffsetDateTime.now(), 400, "Validación fallida",
                "Revisa los campos marcados", path, campos);
    }
}
