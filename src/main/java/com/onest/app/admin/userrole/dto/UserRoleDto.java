package com.onest.app.admin.userrole.dto;

import java.util.List;

/** Respuesta de GET /api/admin/usuarios/{nss}: datos del empleado (ORDS) + checklist de roles. */
public record UserRoleDto(
        String nss,
        String nombre,
        List<RoleAssignmentDto> roles
) {
    public record RoleAssignmentDto(Long roleId, String code, String name, boolean asignado) {
    }
}
