package com.onest.app.catalog.dashboard.dto;

/**
 * Conteo de examenes con su desglose por dictamen. Sirve tanto agrupado por predio como por
 * mes. Conserva {@code cantidad} con el mismo nombre que ConteoSimpleDto a proposito: el
 * ranking del Dashboard Ejecutivo y el radar de Vista por Predio ya leen ese campo de
 * {@code porPredio} y no tienen por que enterarse de que ahora trae mas columnas.
 */
public record ConteoDictamenDto(
        String clave,
        long cantidad,
        long apto,
        long noApto,
        long aptoCondicionado,
        long aptoRestringido
) {
}
