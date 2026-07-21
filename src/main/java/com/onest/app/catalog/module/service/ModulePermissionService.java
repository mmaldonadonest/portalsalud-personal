package com.onest.app.catalog.module.service;

import com.onest.app.catalog.module.client.ModulePermissionClient;
import com.onest.app.catalog.module.dto.ModuleDto;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Modulos de menu permitidos para el usuario autenticado.
 * Replica validateModules.php: rol del usuario -> menus del rol.
 */
@Service
public class ModulePermissionService {

    private static final Logger log = LoggerFactory.getLogger(ModulePermissionService.class);

    private final ModulePermissionClient client;

    public ModulePermissionService(ModulePermissionClient client) {
        this.client = client;
    }

    public List<ModuleDto> allowedModulesForCurrentUser() {
        String idUsuario = usuarioActual();
        return client.findRoleId(idUsuario)
                .map(idRol -> {
                    List<ModuleDto> modulos = client.findMenusByRole(idRol);
                    log.info("[modulos] usuario={} rol={} -> {} modulo(s)", idUsuario, idRol, modulos.size());
                    return modulos;
                })
                .orElseGet(() -> {
                    log.warn("[modulos] usuario={} SIN rol en ORDS (consulta_app_rol_usuario, id_app=13);"
                            + " el submenu lateral quedara vacio", idUsuario);
                    return List.of();
                });
    }

    /**
     * id_usuario. En php-old era $_SESSION['nombre']; aqui el principal autenticado.
     */
    private String usuarioActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return "SISTEMA";
        }
        return authentication.getName();
    }
}
