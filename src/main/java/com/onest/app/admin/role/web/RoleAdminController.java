package com.onest.app.admin.role.web;

import com.onest.app.admin.role.dto.RoleAdminDto;
import com.onest.app.admin.role.service.RoleAdminService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * CRUD de roles locales para /admin/roles (protegido con hasRole("ADMIN") en
 * SecurityConfiguration). Ver docs/plan-rbac-local.md.
 */
@RestController
@RequestMapping("/api/admin/roles")
public class RoleAdminController {

    private final RoleAdminService roleAdminService;

    public RoleAdminController(RoleAdminService roleAdminService) {
        this.roleAdminService = roleAdminService;
    }

    @GetMapping
    public List<RoleAdminDto> listar() {
        return roleAdminService.listar();
    }

    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public RoleAdminDto crear(
            @RequestParam("code") String code,
            @RequestParam("name") String name,
            @RequestParam(name = "description", required = false) String description) {
        try {
            return roleAdminService.crear(code, name, description);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PostMapping(path = "/{id}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public RoleAdminDto renombrar(
            @PathVariable Long id,
            @RequestParam("name") String name,
            @RequestParam(name = "description", required = false) String description) {
        try {
            return roleAdminService.renombrar(id, name, description);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PostMapping(path = "/{id}/estado", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public RoleAdminDto cambiarEstado(@PathVariable Long id, @RequestParam("activo") boolean activo) {
        try {
            return roleAdminService.cambiarEstado(id, activo);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }
}
