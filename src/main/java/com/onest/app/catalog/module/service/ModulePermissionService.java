package com.onest.app.catalog.module.service;

import com.onest.app.catalog.module.client.ModulePermissionClient;
import com.onest.app.catalog.module.dto.ModuleDto;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Modulos de menu permitidos para el usuario autenticado.
 * Replica validateModules.php: rol del usuario -> menus del rol.
 */
@Service
public class ModulePermissionService {

    private final ModulePermissionClient client;

    public ModulePermissionService(ModulePermissionClient client) {
        this.client = client;
    }

    public List<ModuleDto> allowedModulesForCurrentUser() {
        String idUsuario = usuarioActual();
        return client.findRoleId(idUsuario)
                .map(client::findMenusByRole)
                .orElseGet(List::of);
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
