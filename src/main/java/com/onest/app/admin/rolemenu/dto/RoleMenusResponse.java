package com.onest.app.admin.rolemenu.dto;

import com.onest.app.admin.role.dto.RoleAdminDto;
import java.util.List;

/** Respuesta de GET /api/admin/roles/{roleId}/menus: el rol + el checklist completo. */
public record RoleMenusResponse(
        RoleAdminDto role,
        List<RoleMenuDto> menus
) {
}
