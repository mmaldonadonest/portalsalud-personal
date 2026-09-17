package com.onest.app.admin.userrole.service;

import com.onest.app.admin.userrole.dto.UserRoleDto;
import com.onest.app.admin.userrole.dto.UserRoleDto.RoleAssignmentDto;
import com.onest.app.catalog.nss.client.NssSearchClient;
import com.onest.app.catalog.nss.dto.EmpleadoDto;
import com.onest.app.security.model.AppSecRole;
import com.onest.app.security.permission.PermissionService;
import com.onest.app.security.model.AppSecUser;
import com.onest.app.security.repository.AppSecRoleRepository;
import com.onest.app.security.repository.AppSecUserRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Asignacion de rol local por NSS (/admin/usuarios) - reemplaza el INSERT/UPDATE
 * manual en APP_SEC_USER/APP_SEC_USER_ROLE que se venia haciendo por SQL Developer.
 * Ver docs/plan-rbac-local.md.
 *
 * <p>Busqueda SIEMPRE via NssSearchClient.findUsuario() directo (NO
 * NssSearchService.findByNss(), que puede disparar una ALTA en Servcio/Medico
 * como efecto colateral - no se quiere eso corriendo desde una pantalla admin).
 */
@Service
public class UserRoleAdminService {

    private final NssSearchClient nssSearchClient;
    private final AppSecUserRepository userRepository;
    private final AppSecRoleRepository roleRepository;
    private final PermissionService permissionService;

    public UserRoleAdminService(
            NssSearchClient nssSearchClient, AppSecUserRepository userRepository, AppSecRoleRepository roleRepository,
            PermissionService permissionService) {
        this.nssSearchClient = nssSearchClient;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionService = permissionService;
    }

    @Transactional(readOnly = true)
    public UserRoleDto buscar(String nss) {
        EmpleadoDto empleado = nssSearchClient.findUsuario(nss, "ADMIN")
                .orElseThrow(() -> new IllegalArgumentException("NSS no encontrado en ORDS."));
        Set<Long> asignados = userRepository.findByUsernameIgnoreCase(nss)
                .map(u -> u.getRoles().stream().map(AppSecRole::getId).collect(Collectors.toSet()))
                .orElseGet(Set::of);
        List<RoleAssignmentDto> roles = roleRepository.findByActiveOrderByName("Y").stream()
                .map(r -> new RoleAssignmentDto(r.getId(), r.getCode(), r.getName(), asignados.contains(r.getId())))
                .toList();
        return new UserRoleDto(nss, nombreCompleto(empleado), roles);
    }

    @Transactional
    public void asignar(String nss, Long roleId) {
        AppSecUser user = requireOrCreateUser(nss);
        AppSecRole role = requireRole(roleId);
        user.getRoles().add(role);
        userRepository.save(user);
        permissionService.invalidar();
    }

    @Transactional
    public void quitar(String nss, Long roleId) {
        Optional<AppSecUser> user = userRepository.findByUsernameIgnoreCase(nss);
        if (user.isEmpty()) {
            return;
        }
        user.get().getRoles().removeIf(r -> r.getId().equals(roleId));
        userRepository.save(user.get());
        permissionService.invalidar();
    }

    /** Crea el usuario local en la primera asignacion (migracion incremental, sin password local). */
    private AppSecUser requireOrCreateUser(String nss) {
        return userRepository.findByUsernameIgnoreCase(nss).orElseGet(() -> {
            AppSecUser user = new AppSecUser();
            user.setUsername(nss);
            user.setDisplayName(nombreCompletoODefault(nss));
            user.setActive("Y");
            user.setAccountStatus("ACTIVE");
            return user;
        });
    }

    private AppSecRole requireRole(Long roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado (id=" + roleId + ")."));
    }

    private String nombreCompletoODefault(String nss) {
        return nssSearchClient.findUsuario(nss, "ADMIN").map(this::nombreCompleto).orElse(nss);
    }

    private String nombreCompleto(EmpleadoDto empleado) {
        return Stream.of(empleado.nombre(), empleado.apellidoPaterno(), empleado.apellidoMaterno())
                .filter(parte -> parte != null && !parte.isBlank())
                .reduce((a, b) -> a + " " + b)
                .orElse(empleado.nss());
    }
}
