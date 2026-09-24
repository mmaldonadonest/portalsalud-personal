package com.onest.app.admin.role.service;

import com.onest.app.admin.role.dto.RoleAdminDto;
import com.onest.app.security.model.AppSecRole;
import com.onest.app.security.repository.AppSecRoleRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD de roles locales (SERV_MED_SEC_ROLE) para /admin/roles - ver docs/plan-rbac-local.md.
 * CODE es inmutable una vez creado: es la llave usada como GrantedAuthority de
 * Spring (ej. ROLE_ADMIN, ver LegacyPhpAuthenticationProvider/PortalUserDetailsService)
 * y como referencia en SERV_MED_MENU_ROLE/SERV_MED_SEC_USER_ROLE - renombrarlo en caliente
 * podria dejar esas referencias apuntando a un rol "distinto" en la practica.
 * Preferir ACTIVE=N sobre DELETE (a diferencia de ORDS, que borra en duro sin
 * validar huerfanos en TBL_APPS_ROL_MENU/TBL_APP_ROL_USUARIO).
 */
@Service
public class RoleAdminService {

    private final AppSecRoleRepository roleRepository;

    public RoleAdminService(AppSecRoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public List<RoleAdminDto> listar() {
        return roleRepository.findAllByOrderByNameAsc().stream().map(RoleAdminDto::from).toList();
    }

    @Transactional
    public RoleAdminDto crear(String code, String name, String description) {
        String codeNorm = requireNonBlank(code, "El codigo es obligatorio.").trim();
        String nameNorm = requireNonBlank(name, "El nombre es obligatorio.").trim();
        if (roleRepository.existsByCode(codeNorm)) {
            throw new IllegalArgumentException("Ya existe un rol con el codigo '" + codeNorm + "'.");
        }
        AppSecRole role = new AppSecRole();
        role.setCode(codeNorm);
        role.setName(nameNorm);
        role.setDescription(blankToNull(description));
        // CREATED_AT/CREATED_BY son NOT NULL con default en Oracle (SYSTIMESTAMP/USER), pero
        // ese default SOLO aplica si la columna se omite del INSERT - Hibernate siempre manda
        // todas las columnas mapeadas (NULL incluido si el campo Java esta vacio), lo que pisa
        // el default y viola el NOT NULL (ORA-01400 -> 500 sin capturar). Setearlos aqui explicito.
        role.setCreatedAt(LocalDateTime.now());
        role.setCreatedBy(usuarioActual());
        return RoleAdminDto.from(roleRepository.save(role));
    }

    @Transactional
    public RoleAdminDto renombrar(Long id, String name, String description) {
        AppSecRole role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado (id=" + id + ")."));
        role.setName(requireNonBlank(name, "El nombre es obligatorio.").trim());
        role.setDescription(blankToNull(description));
        role.setUpdatedAt(LocalDateTime.now());
        role.setUpdatedBy(usuarioActual());
        return RoleAdminDto.from(roleRepository.save(role));
    }

    @Transactional
    public RoleAdminDto cambiarEstado(Long id, boolean activo) {
        AppSecRole role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado (id=" + id + ")."));
        role.setActive(activo ? "Y" : "N");
        role.setUpdatedAt(LocalDateTime.now());
        role.setUpdatedBy(usuarioActual());
        return RoleAdminDto.from(roleRepository.save(role));
    }

    private String usuarioActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return "SISTEMA";
        }
        return authentication.getName();
    }

    private static String requireNonBlank(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensaje);
        }
        return valor;
    }

    private static String blankToNull(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }
}
