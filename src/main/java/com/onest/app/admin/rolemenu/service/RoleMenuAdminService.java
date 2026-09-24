package com.onest.app.admin.rolemenu.service;

import com.onest.app.admin.role.dto.RoleAdminDto;
import com.onest.app.admin.rolemenu.dto.RoleMenuDto;
import com.onest.app.admin.rolemenu.dto.RoleMenusResponse;
import com.onest.app.security.model.AppMenu;
import com.onest.app.security.permission.PermissionService;
import com.onest.app.security.model.AppMenuRole;
import com.onest.app.security.model.AppSecRole;
import com.onest.app.security.repository.AppMenuRepository;
import com.onest.app.security.repository.AppMenuRoleRepository;
import com.onest.app.security.repository.AppSecRoleRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Checklist de menus por rol (/admin/roles/{id}/menus) - la granularidad rol->menu
 * que motivo todo el RBAC local (ver docs/plan-rbac-local.md). Reemplaza el
 * INSERT/DELETE manual en SERV_MED_MENU_ROLE que se veni haciendo por SQL Developer.
 */
@Service
public class RoleMenuAdminService {

    private final AppSecRoleRepository roleRepository;
    private final AppMenuRepository menuRepository;
    private final AppMenuRoleRepository menuRoleRepository;
    private final PermissionService permissionService;

    public RoleMenuAdminService(
            AppSecRoleRepository roleRepository, AppMenuRepository menuRepository,
            AppMenuRoleRepository menuRoleRepository, PermissionService permissionService) {
        this.roleRepository = roleRepository;
        this.menuRepository = menuRepository;
        this.menuRoleRepository = menuRoleRepository;
        this.permissionService = permissionService;
    }

    @Transactional(readOnly = true)
    public RoleMenusResponse listar(Long roleId) {
        AppSecRole role = requireRole(roleId);
        Set<Long> asignados = menuRoleRepository.findActiveByRoleId(roleId).stream()
                .map(mr -> mr.getMenu().getId())
                .collect(Collectors.toSet());
        List<RoleMenuDto> menus = menuRepository.findByActiveOrderByOrderNo("Y").stream()
                .map(m -> new RoleMenuDto(m.getId(), m.getCode(), m.getTitle(), m.getIcon(), asignados.contains(m.getId())))
                .toList();
        return new RoleMenusResponse(RoleAdminDto.from(role), menus);
    }

    @Transactional
    public void asignar(Long roleId, Long menuId) {
        if (menuRoleRepository.findByRoleIdAndMenuId(roleId, menuId).isPresent()) {
            return;
        }
        AppSecRole role = requireRole(roleId);
        AppMenu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new IllegalArgumentException("Menu no encontrado (id=" + menuId + ")."));
        menuRoleRepository.save(new AppMenuRole(menu, role));
        permissionService.invalidar();
    }

    @Transactional
    public void quitar(Long roleId, Long menuId) {
        menuRoleRepository.findByRoleIdAndMenuId(roleId, menuId).ifPresent(menuRoleRepository::delete);
        permissionService.invalidar();
    }

    private AppSecRole requireRole(Long roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado (id=" + roleId + ")."));
    }
}
