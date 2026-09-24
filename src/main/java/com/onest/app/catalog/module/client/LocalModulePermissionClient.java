package com.onest.app.catalog.module.client;

import com.onest.app.catalog.module.dto.ModuleDto;
import com.onest.app.security.model.AppSecRole;
import com.onest.app.security.repository.AppMenuRoleRepository;
import com.onest.app.security.repository.AppSecUserRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Implementacion RBAC local de ModulePermissionClient - reemplaza al WS ORDS
 * compartido con el portal PHP legacy (BiowsModulePermissionClient) cuando
 * {@code portal.permissions.source=LOCAL}. Ver docs/plan-rbac-local.md.
 *
 * <p>Bean SIEMPRE presente (no condicional) a proposito: aunque ORDS siga siendo
 * la fuente activa para PermissionService/ModulePermissionService (ver @Primary
 * en BiowsModulePermissionClient), ClinicalAccessFilter necesita poder inyectar
 * esta implementacion directo (tipo concreto, no la interfaz) para el modo sombra
 * ({@code portal.permissions.shadow=true}) sin depender de cual esta activa.
 *
 * <p>Un usuario puede tener mas de un rol local a la vez (ej. ROLE_ADMIN, el gate
 * del panel /admin, sembrado aparte de este RBAC) - findRoleId prefiere el rol
 * que de verdad tenga menus asignados en SERV_MED_MENU_ROLE (el "rol clinico"), no
 * "el primero que se encuentre".
 */
@Component
public class LocalModulePermissionClient implements ModulePermissionClient {

    private final AppSecUserRepository userRepository;
    private final AppMenuRoleRepository menuRoleRepository;

    public LocalModulePermissionClient(AppSecUserRepository userRepository, AppMenuRoleRepository menuRoleRepository) {
        this.userRepository = userRepository;
        this.menuRoleRepository = menuRoleRepository;
    }

    @Override
    public Optional<String> findRoleId(String idUsuario) {
        return userRepository.findActiveByIdentifier(idUsuario)
                .flatMap(user -> {
                    List<AppSecRole> activos = user.getRoles().stream()
                            .filter(role -> "Y".equals(role.getActive()))
                            .toList();
                    // Un usuario puede tener a la vez ROLE_ADMIN (gate del panel /admin,
                    // sin relacion con menus clinicos) y un rol clinico (USER/ADM/ENFERMERO,
                    // el que SI tiene filas en SERV_MED_MENU_ROLE) - preferir el que de verdad
                    // tenga menus asignados, no "el primero que se encuentre" (el orden de
                    // un Set no esta garantizado y con ROLE_ADMIN primero el submenu salia
                    // vacio aunque el usuario si tuviera rol clinico).
                    return activos.stream()
                            .filter(role -> menuRoleRepository.existsByRoleId(role.getId()))
                            .findFirst()
                            .or(() -> activos.stream().findFirst());
                })
                .map(role -> role.getId().toString());
    }

    @Override
    public List<ModuleDto> findMenusByRole(String idRol) {
        Long roleId;
        try {
            roleId = Long.parseLong(idRol);
        } catch (NumberFormatException ex) {
            return List.of();
        }
        return menuRoleRepository.findActiveByRoleId(roleId).stream()
                .map(mr -> new ModuleDto(mr.getMenu().getId().intValue(), mr.getMenu().getTitle(),
                        mr.getMenu().getCode(), mr.getMenu().getIcon()))
                .toList();
    }
}
