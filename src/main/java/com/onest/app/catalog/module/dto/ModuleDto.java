package com.onest.app.catalog.module.dto;

/**
 * Modulo de menu permitido para el rol del usuario.
 * Equivale a cada elemento de Resultado.Datos de validateModules.php.
 *
 * <p>{@code code} es null cuando la fuente es ORDS (BiowsModulePermissionClient) -
 * ese esquema no tiene una llave estable de menu, solo el id_menu numerico.
 * Cuando la fuente es local (LocalModulePermissionClient) viene poblado con el
 * CODE de SERV_MED_MENU; fragments/nss-modules.html se indexa por code cuando esta
 * presente. Ver docs/plan-rbac-local.md.
 *
 * <p>{@code icon} es null para ORDS (ese WS no tiene columna de icono); viene
 * de SERV_MED_MENU.ICON para la fuente local, editable en BD sin redeploy.
 */
public record ModuleDto(
        Integer idMenu,
        String nombreMenu,
        String code,
        String icon
) {
}
