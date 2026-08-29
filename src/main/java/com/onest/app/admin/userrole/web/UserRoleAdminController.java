package com.onest.app.admin.userrole.web;

import com.onest.app.admin.userrole.dto.UserRoleDto;
import com.onest.app.admin.userrole.service.UserRoleAdminService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Asignacion de rol local por NSS para /admin/usuarios (protegido con
 * hasRole("ADMIN") en SecurityConfiguration). Ver docs/plan-rbac-local.md.
 */
@RestController
@RequestMapping("/api/admin/usuarios")
public class UserRoleAdminController {

    private final UserRoleAdminService userRoleAdminService;

    public UserRoleAdminController(UserRoleAdminService userRoleAdminService) {
        this.userRoleAdminService = userRoleAdminService;
    }

    @GetMapping("/{nss}")
    public UserRoleDto buscar(@PathVariable String nss) {
        try {
            return userRoleAdminService.buscar(nss);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }
    }

    @PostMapping("/{nss}/roles/{roleId}")
    public void asignar(@PathVariable String nss, @PathVariable Long roleId) {
        try {
            userRoleAdminService.asignar(nss, roleId);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @DeleteMapping("/{nss}/roles/{roleId}")
    public void quitar(@PathVariable String nss, @PathVariable Long roleId) {
        userRoleAdminService.quitar(nss, roleId);
    }
}
