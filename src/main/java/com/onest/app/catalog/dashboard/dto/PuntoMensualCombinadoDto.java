package com.onest.app.catalog.dashboard.dto;

/** Tendencia mensual combinada de los 4 dominios, para graficar varias series a la vez. */
public record PuntoMensualCombinadoDto(
        String mes,
        long incapacidades,
        long accidentes,
        long consultas,
        long examenes
) {
}
