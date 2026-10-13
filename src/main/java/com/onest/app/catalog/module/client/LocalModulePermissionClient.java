package com.onest.app.catalog.module.client;

import com.onest.app.catalog.module.dto.ModuleDto;
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
 * del panel /admin, mas un rol clinico) - se usan los menus de TODOS sus roles activos.
 */
@Component
public class LocalModulePermissionClient implements ModulePermissionClient {

    private final AppSecUserRepository userRepository;
    private final AppMenuRoleRepository menuRoleRepository;

    public LocalModulePermissionClient(AppSecUserRepository userRepository, AppMenuRoleRepository menuRoleRepository) {
        this.userRepository = userRepository;
        this.menuRoleRepository = menuRoleRepository;
    }

    /**
     * Devuelve TODOS los roles activos del usuario, separados por coma ("3,7,9"), no uno solo.
     *
     * <p>10-oct-2026: antes se tomaba el primer rol con menus. El orden de los roles (un Set)
     * no es fijo, asi que con ADMIN o MEDICO_ANALISTA primero el submenu del NSS salia vacio
     * aunque el usuario tuviera ENFERMERO/USER/ADM, y al reiniciar podia volver a verse. La firma
     * de la interfaz se conserva: quien llama siempre encadena findRoleId -> findMenusByRole, y
     * findMenusByRole acepta la lista.
     */
    @Override
    public Optional<String> findRoleId(String idUsuario) {
        return userRepository.findActiveByIdentifier(idUsuario)
                .map(user -> user.getRoles().stream()
                        .filter(role -> "Y".equals(role.getActive()))
                        .map(role -> role.getId().toString())
                        .sorted()
                        .collect(java.util.stream.Collectors.joining(",")))
                .filter(ids -> !ids.isBlank());
    }

    /** Une los menus de todos los roles recibidos, sin repetir y en el orden de SERV_MED_MENU. */
    @Override
    public List<ModuleDto> findMenusByRole(String idRol) {
        if (idRol == null || idRol.isBlank()) {
            return List.of();
        }
        java.util.Map<Long, com.onest.app.security.model.AppMenu> menus = new java.util.LinkedHashMap<>();
        for (String parte : idRol.split(",")) {
            Long roleId;
            try {
                roleId = Long.parseLong(parte.trim());
            } catch (NumberFormatException ex) {
                continue;
            }
            menuRoleRepository.findActiveByRoleId(roleId)
                    .forEach(mr -> menus.putIfAbsent(mr.getMenu().getId(), mr.getMenu()));
        }
        return menus.values().stream()
                .sorted(java.util.Comparator.comparing(com.onest.app.security.model.AppMenu::getOrderNo,
                        java.util.Comparator.nullsLast(Integer::compareTo)))
                .map(m -> new ModuleDto(m.getId().intValue(), m.getTitle(), m.getCode(), m.getIcon()))
                .toList();
    }
}
