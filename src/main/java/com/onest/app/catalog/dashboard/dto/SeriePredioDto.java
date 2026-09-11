package com.onest.app.catalog.dashboard.dto;

import java.util.List;

/**
 * Serie mensual de UN predio. Existe para la grafica "Evolucion comparada" del modulo
 * Atenciones, que superpone varios predios en el mismo eje de tiempo: con solo
 * {@code porPredio} (totales) y {@code tendenciaMensual} (global) no se puede dibujar,
 * y resolverlo desde el navegador obligaria a una llamada por predio.
 */
public record SeriePredioDto(String predio, List<PuntoMensualDto> puntos) {
}
