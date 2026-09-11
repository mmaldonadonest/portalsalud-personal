package com.onest.app.catalog.dashboard.dto;

import java.util.List;

/**
 * Serie mensual de UNA clave (un ramo, un rubro...), para graficas apiladas o comparadas
 * donde varias claves comparten el eje de tiempo. Generalizacion de SeriePredioDto, que se
 * conserva tal cual porque el modulo Atenciones ya lee su campo {@code predio}.
 */
public record SerieMensualDto(String clave, List<PuntoMensualDto> puntos) {
}
