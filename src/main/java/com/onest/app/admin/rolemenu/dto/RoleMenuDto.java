package com.onest.app.admin.rolemenu.dto;

/** Fila del checklist de /admin/roles/{id}/menus: un modulo del catalogo + si esta asignado al rol. */
public record RoleMenuDto(
        Long menuId,
        String code,
        String title,
        String icon,
        boolean asignado
) {
}
