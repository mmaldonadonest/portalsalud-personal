package com.onest.app.catalog.module.dto;

/**
 * Modulo de menu permitido para el rol del usuario.
 * Equivale a cada elemento de Resultado.Datos de validateModules.php.
 */
public record ModuleDto(
        Integer idMenu,
        String nombreMenu
) {
}
