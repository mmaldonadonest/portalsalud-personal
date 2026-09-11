package com.onest.app.catalog.predio.dto;

/**
 * Cuenta real (biometrico_cuenta_SAP) con su predio vigente si ya fue asignado.
 * predioId=null / predioNombre="0" significa pendiente de asignar - nunca se descarta,
 * el llamador decide como mostrar "Sin asignar". Ver docs/ords-predio-cuenta.sql BLOQUE 4.
 */
public record CuentaPredioDto(String cuentaId, String cuentaNombre, Long predioId, String predioNombre) {

    public boolean tienePredioAsignado() {
        return predioId != null;
    }
}
