package mx.saludocupacional.portal.shared.exception;

/** El recurso solicitado no existe o fue borrado lógicamente. Se traduce a HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String recurso, Object identificador) {
        super("No se encontró %s con identificador %s".formatted(recurso, identificador));
    }

    public ResourceNotFoundException(String mensaje) {
        super(mensaje);
    }
}
