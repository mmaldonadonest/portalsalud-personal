package com.onest.app.catalog.module.client;

import com.onest.app.catalog.module.client.dto.BiowsRolMenuRequest;
import com.onest.app.catalog.module.client.dto.BiowsRolMenuResponse;
import com.onest.app.catalog.module.client.dto.BiowsRolUsuarioRequest;
import com.onest.app.catalog.module.client.dto.BiowsRolUsuarioResponse;
import com.onest.app.catalog.module.dto.ModuleDto;
import com.onest.app.config.BiowsProperties;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Implementacion de los gateways ORDS de permisos de menu.
 * Replica loadParms() + loadMenus() de php-old/rest/validateModules.php,
 * conservando URLs y nombres de campo. Reusa el RestClient de biows.
 */
@Component
public class BiowsModulePermissionClient implements ModulePermissionClient {

    private static final Logger log = LoggerFactory.getLogger(BiowsModulePermissionClient.class);

    private static final String PATH_ROL_USUARIO = "/info/consulta_app_rol_usuario";
    private static final String PATH_ROL_MENU = "/info/consulta_app_rol_menu";

    private final RestClient biowsRestClient;
    private final BiowsProperties properties;

    public BiowsModulePermissionClient(RestClient biowsRestClient, BiowsProperties properties) {
        this.biowsRestClient = biowsRestClient;
        this.properties = properties;
    }

    @Override
    public Optional<String> findRoleId(String idUsuario) {
        log.info("[biows] POST {}{} id_usuario={}", properties.baseUrl(), PATH_ROL_USUARIO, idUsuario);
        BiowsRolUsuarioResponse response = biowsRestClient.post()
                .uri(PATH_ROL_USUARIO)
                .body(new BiowsRolUsuarioRequest(idUsuario, properties.appId()))
                .retrieve()
                .body(BiowsRolUsuarioResponse.class);

        if (response == null || response.resultado() == null || response.resultado().datos() == null
                || response.resultado().datos().isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(response.resultado().datos().get(0).idRol());
    }

    @Override
    public List<ModuleDto> findMenusByRole(String idRol) {
        log.info("[biows] POST {}{} id_app={} id_rol={}", properties.baseUrl(), PATH_ROL_MENU, properties.appId(), idRol);
        BiowsRolMenuResponse response = biowsRestClient.post()
                .uri(PATH_ROL_MENU)
                .body(new BiowsRolMenuRequest(properties.appId(), idRol))
                .retrieve()
                .body(BiowsRolMenuResponse.class);

        if (response == null || response.resultado() == null || response.resultado().datos() == null) {
            return List.of();
        }
        return response.resultado().datos().stream()
                .map(d -> new ModuleDto(d.idMenu(), d.nombreMenu()))
                .toList();
    }
}
