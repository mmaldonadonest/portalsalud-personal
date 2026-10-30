package com.onest.app.admin.userrole.web;

import com.onest.app.audit.web.Auditado;
import com.onest.app.admin.userrole.dto.UserRoleDto;
import com.onest.app.admin.userrole.service.UserRoleAdminService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
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
    @Auditado(modulo = "Administracion", accion = "create", entidad = "Rol de usuario", registro = "nss", detalle = {"roleId"})
    public void asignar(@PathVariable String nss, @PathVariable Long roleId) {
        try {
            userRoleAdminService.asignar(nss, roleId);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    /** Bloqueo solo de este portal: no entra por password ni SSO y se le cierran las sesiones abiertas. */
    @PostMapping("/{nss}/bloqueo")
    @Auditado(modulo = "Administracion", accion = "update", entidad = "Bloqueo de usuario", registro = "nss")
    public Map<String, Integer> bloquear(@PathVariable String nss, Authentication auth) {
        try {
            return Map.of("sesionesCerradas", userRoleAdminService.bloquear(nss, auth == null ? null : auth.getName()));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @DeleteMapping("/{nss}/bloqueo")
    @Auditado(modulo = "Administracion", accion = "update", entidad = "Desbloqueo de usuario", registro = "nss")
    public void desbloquear(@PathVariable String nss) {
        userRoleAdminService.desbloquear(nss);
    }

    @DeleteMapping("/{nss}/roles/{roleId}")
    @Auditado(modulo = "Administracion", accion = "delete", entidad = "Rol de usuario", registro = "nss", detalle = {"roleId"})
    public void quitar(@PathVariable String nss, @PathVariable Long roleId) {
        userRoleAdminService.quitar(nss, roleId);
    }
}
