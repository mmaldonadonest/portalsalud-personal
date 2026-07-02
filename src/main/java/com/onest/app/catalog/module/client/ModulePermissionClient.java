package com.onest.app.catalog.module.client;

import com.onest.app.catalog.module.dto.ModuleDto;
import java.util.List;
import java.util.Optional;

/**
 * Gateway hacia los WS ORDS de permisos de menu (validateModules.php).
 * Dos llamadas encadenadas: rol del usuario y menus permitidos por rol.
 */
public interface ModulePermissionClient {

    /** loadParms -> POST .../info/consulta_app_rol_usuario. Devuelve el id_rol. */
    Optional<String> findRoleId(String idUsuario);

    /** loadMenus -> POST .../info/consulta_app_rol_menu. Devuelve los modulos del rol. */
    List<ModuleDto> findMenusByRole(String idRol);
}
