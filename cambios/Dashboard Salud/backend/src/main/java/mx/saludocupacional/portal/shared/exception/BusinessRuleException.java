package mx.saludocupacional.portal.shared.exception;

/**
 * Una regla de negocio impide completar la operación. Se traduce a HTTP 409.
 *
 * <p>Ejemplos: intentar capturar en un periodo cerrado, crear una incapacidad
 * de origen accidente desde el módulo de incapacidades, o consumir un lote de
 * antidoping sin existencia disponible.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String mensaje) {
        super(mensaje);
    }
}
