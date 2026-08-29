package com.onest.app.admin.role.dto;

import com.onest.app.security.model.AppSecRole;

/** Fila de la pantalla /admin/roles. */
public record RoleAdminDto(
        Long id,
        String code,
        String name,
        String description,
        boolean active
) {
    public static RoleAdminDto from(AppSecRole role) {
        return new RoleAdminDto(role.getId(), role.getCode(), role.getName(), role.getDescription(),
                "Y".equals(role.getActive()));
    }
}
