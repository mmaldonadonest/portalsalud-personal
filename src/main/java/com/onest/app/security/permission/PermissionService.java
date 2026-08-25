package com.onest.app.security.permission;

import com.onest.app.catalog.module.client.ModulePermissionClient;
import com.onest.app.catalog.module.dto.ModuleDto;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Resuelve que id_menu tiene permitidos el usuario autenticado, reusando el MISMO WS
 * que ya arma el menu lateral (info/consulta_app_rol_usuario + consulta_app_rol_menu,
 * ver ModulePermissionService) - sin inventar ninguna fuente de permisos nueva ni
 * tocar ningun WS existente. Cache corta por usuario para no llamar a ORDS en cada
 * peticion (lo consume {@link ClinicalAccessFilter} en cada request clinico).
 */
@Service
public class PermissionService {

    private static final long TTL_MILLIS = 120_000L;

    private final ModulePermissionClient client;
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public PermissionService(ModulePermissionClient client) {
        this.client = client;
    }

    /** id_menu permitidos para el usuario autenticado actual (mismos que ve en el menu lateral). */
    public Set<Integer> idsMenuPermitidos() {
        String usuario = usuarioActual();
        CacheEntry entry = cache.get(usuario);
        long now = System.currentTimeMillis();
        if (entry != null && entry.expiresAt() > now) {
            return entry.ids();
        }
        Set<Integer> ids = client.findRoleId(usuario)
                .map(idRol -> client.findMenusByRole(idRol).stream()
                        .map(ModuleDto::idMenu)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toUnmodifiableSet()))
                .orElseGet(Set::of);
        cache.put(usuario, new CacheEntry(ids, now + TTL_MILLIS));
        return ids;
    }

    public boolean tieneAcceso(int idMenuRequerido) {
        return idsMenuPermitidos().contains(idMenuRequerido);
    }

    private String usuarioActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return "SISTEMA";
        }
        return authentication.getName();
    }

    private record CacheEntry(Set<Integer> ids, long expiresAt) {
    }
}
