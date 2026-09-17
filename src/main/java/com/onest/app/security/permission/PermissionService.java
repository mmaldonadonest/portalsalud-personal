package com.onest.app.security.permission;

import com.onest.app.catalog.module.client.ModulePermissionClient;
import com.onest.app.catalog.module.dto.ModuleDto;
import java.util.List;
import java.util.Objects;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resuelve que modulos tiene permitidos el usuario autenticado, reusando el MISMO
 * gateway que ya arma el menu lateral (ModulePermissionClient - ORDS o local segun
 * portal.permissions.source, ver ModulePermissionService) - sin inventar ninguna
 * fuente de permisos nueva. Cache corta por usuario para no golpear la fuente en
 * cada peticion (lo consume {@link ClinicalAccessFilter} en cada request clinico).
 *
 * <p>Se cachea la lista completa de {@link ModuleDto} (no solo los id_menu) porque
 * la llave que de verdad identifica un modulo depende de la fuente: ORDS solo trae
 * id_menu (numeracion fija 1-14, code siempre null); el esquema local solo trae un
 * id_menu util como PK tecnica (numeracion arbitraria segun el IDENTITY de APP_MENU,
 * NO comparable con la de ORDS) pero SI trae un code estable. Ver docs/plan-rbac-local.md.
 */
@Service
public class PermissionService {

    private static final long TTL_MILLIS = 120_000L;

    private final ModulePermissionClient client;
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public PermissionService(ModulePermissionClient client) {
        this.client = client;
    }

    /** Modulos permitidos para el usuario autenticado actual (mismos que ve en el menu lateral). */
    public List<ModuleDto> modulosPermitidos() {
        String usuario = usuarioActual();
        CacheEntry entry = cache.get(usuario);
        long now = System.currentTimeMillis();
        if (entry != null && entry.expiresAt() > now) {
            return entry.modulos();
        }
        List<ModuleDto> modulos = client.findRoleId(usuario)
                .map(client::findMenusByRole)
                .orElseGet(List::of);
        cache.put(usuario, new CacheEntry(modulos, now + TTL_MILLIS));
        return modulos;
    }

    /** Acceso por id_menu - valido SOLO mientras la fuente activa sea ORDS. */
    public boolean tieneAcceso(int idMenuRequerido) {
        return modulosPermitidos().stream().anyMatch(m -> Objects.equals(m.idMenu(), idMenuRequerido));
    }

    /**
     * Olvida los menus cacheados de TODOS los usuarios. Lo llama Administracion al asignar o
     * quitar menus a un rol o roles a un usuario, para que el cambio se vea al recargar y no
     * hasta que venza el TTL (2 min) - antes "asigne el menu y no me sale" era solo la cache.
     */
    public void invalidar() {
        cache.clear();
    }

    /** Acceso por code - valido SOLO mientras la fuente activa sea LOCAL (ORDS nunca trae code). */
    public boolean tieneAccesoPorCodigo(String codeRequerido) {
        return modulosPermitidos().stream().anyMatch(m -> codeRequerido.equals(m.code()));
    }

    private String usuarioActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return "SISTEMA";
        }
        return authentication.getName();
    }

    private record CacheEntry(List<ModuleDto> modulos, long expiresAt) {
    }
}
