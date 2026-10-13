package com.onest.app.catalog.examen.client;

/**
 * ORDS contesto el guardado con HTTP 200 pero con un error dentro (Estado distinto de 0) o con
 * algo que no es JSON. Se distingue de un fallo de red para que el usuario vea el motivo real y
 * no un 500 generico.
 */
public class BiowsRechazoException extends RuntimeException {

    public BiowsRechazoException(String message) {
        super(message);
    }
}
