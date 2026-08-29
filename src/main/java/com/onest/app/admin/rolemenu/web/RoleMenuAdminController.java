package com.onest.app.admin.rolemenu.web;

import com.onest.app.admin.rolemenu.dto.RoleMenusResponse;
import com.onest.app.admin.rolemenu.service.RoleMenuAdminService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Checklist de menus por rol para /admin/roles/{id}/menus (protegido con
 * hasRole("ADMIN") en SecurityConfiguration). Ver docs/plan-rbac-local.md.
 */
@RestController
@RequestMapping("/api/admin/roles/{roleId}/menus")
public class RoleMenuAdminController {

    private final RoleMenuAdminService roleMenuAdminService;

    public RoleMenuAdminController(RoleMenuAdminService roleMenuAdminService) {
        this.roleMenuAdminService = roleMenuAdminService;
    }

    @GetMapping
    public RoleMenusResponse listar(@PathVariable Long roleId) {
        try {
            return roleMenuAdminService.listar(roleId);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }
    }

    @PostMapping("/{menuId}")
    public void asignar(@PathVariable Long roleId, @PathVariable Long menuId) {
        try {
            roleMenuAdminService.asignar(roleId, menuId);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @DeleteMapping("/{menuId}")
    public void quitar(@PathVariable Long roleId, @PathVariable Long menuId) {
        roleMenuAdminService.quitar(roleId, menuId);
    }
}
