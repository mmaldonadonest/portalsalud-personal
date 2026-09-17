package com.onest.app.security.permission;

import com.onest.app.catalog.module.client.LocalModulePermissionClient;
import com.onest.app.catalog.module.dto.ModuleDto;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Bloquea (403 real, no solo oculta el boton) el acceso a datos CLINICOS si el rol
 * del usuario no tiene el id_menu correspondiente. Usa el mismo id_menu que ya
 * decide que se dibuja en fragments/nss-modules.html - "Rol + Tipo de informacion"
 * del diagrama de la Matriz de permisos (docs/checklist-bloqueadores-negocio.html #7).
 *
 * <p>NO se registra como {@code @Component} a proposito: si Spring Security lo
 * agrega a la cadena via {@code addFilterAfter} Y ademas Spring Boot lo autoregistra
 * como filtro de servlet generico (por ser bean de tipo Filter), corre dos veces y
 * antes de que exista Authentication. Se instancia a mano en SecurityConfiguration.
 */
public class ClinicalAccessFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ClinicalAccessFilter.class);

    // Prefijo de path -> id_menu(s) aceptados (cualquiera de ellos da acceso). Mismos
    // ids que ya usa fragments/nss-modules.html para dibujar el menu lateral. Es la
    // decision REAL mientras portal.permissions.source=ORDS (default).
    private static final Map<String, Set<Integer>> RUTAS_CLINICAS = new LinkedHashMap<>();
    static {
        RUTAS_CLINICAS.put("/api/nss/expediente", Set.of(6, 7));
        RUTAS_CLINICAS.put("/api/nss/consulta", Set.of(6, 7));
        RUTAS_CLINICAS.put("/api/nss/incapacidades", Set.of(8, 9));
        RUTAS_CLINICAS.put("/api/nss/examen", Set.of(10));
        RUTAS_CLINICAS.put("/api/nss/restricciones", Set.of(10)); // embebido en el dictamen de Examen
        RUTAS_CLINICAS.put("/api/nss/pretest", Set.of(11));
        RUTAS_CLINICAS.put("/api/nss/antidoping", Set.of(12));
        RUTAS_CLINICAS.put("/api/nss/accidentes", Set.of(13));
        RUTAS_CLINICAS.put("/api/nss/maternidad", Set.of(14));
    }

    // Mismas 9 rutas, en CODE local (ver docs/plan-rbac-local.md) - solo se usa para
    // el modo sombra (portal.permissions.shadow=true), nunca para bloquear de verdad
    // mientras la fuente activa siga siendo ORDS.
    private static final Map<String, Set<String>> RUTAS_CLINICAS_LOCAL = new LinkedHashMap<>();
    static {
        RUTAS_CLINICAS_LOCAL.put("/api/nss/expediente", Set.of("ARCHIVO_CONSULTAS", "CONSULTA_MEDICA"));
        RUTAS_CLINICAS_LOCAL.put("/api/nss/consulta", Set.of("ARCHIVO_CONSULTAS", "CONSULTA_MEDICA"));
        RUTAS_CLINICAS_LOCAL.put("/api/nss/incapacidades", Set.of("INCAPACIDADES", "ARCHIVO_INCAPACIDADES"));
        RUTAS_CLINICAS_LOCAL.put("/api/nss/examen", Set.of("EXAMEN_MEDICO"));
        RUTAS_CLINICAS_LOCAL.put("/api/nss/restricciones", Set.of("EXAMEN_MEDICO"));
        RUTAS_CLINICAS_LOCAL.put("/api/nss/pretest", Set.of("PRETEST"));
        RUTAS_CLINICAS_LOCAL.put("/api/nss/antidoping", Set.of("ANTIDOPING"));
        RUTAS_CLINICAS_LOCAL.put("/api/nss/accidentes", Set.of("ACCIDENTES"));
        RUTAS_CLINICAS_LOCAL.put("/api/nss/maternidad", Set.of("MATERNIDAD"));
    }

    // Pantallas del grupo "Examenes" del sidebar (fuera del flujo NSS). Solo existen como
    // menu en el esquema LOCAL (db/sql/03_menu_examenes.sql), ORDS no las conoce: con
    // fuente ORDS siguen abiertas a cualquier autenticado, como antes del 17-sep-2026.
    // /api/consumibles GET queda fuera a proposito: lo consumen Analisis > Antidoping e
    // Inventario (rol MEDICO_ANALISTA); solo se protegen las escrituras.
    private static final Map<String, String> RUTAS_EXAMENES_LOCAL = new LinkedHashMap<>();
    static {
        RUTAS_EXAMENES_LOCAL.put("/antidoping-seleccion", "ANTIDOPING_SELECCION");
        RUTAS_EXAMENES_LOCAL.put("/api/antidoping-seleccion", "ANTIDOPING_SELECCION");
        RUTAS_EXAMENES_LOCAL.put("/consumibles", "CONSUMIBLES");
        RUTAS_EXAMENES_LOCAL.put("/api/consumibles", "CONSUMIBLES");
        RUTAS_EXAMENES_LOCAL.put("/causas-consulta", "CAUSAS_CONSULTA");
        RUTAS_EXAMENES_LOCAL.put("/api/causas-consulta", "CAUSAS_CONSULTA");
    }

    private final PermissionService permissionService;
    private final LocalModulePermissionClient localClient;
    private final boolean shadowEnabled;
    // true cuando portal.permissions.source=LOCAL: la decision real debe comparar por
    // CODE (permissionService.tieneAccesoPorCodigo), no por id_menu - con la fuente
    // local, PermissionService.modulosPermitidos() ya no trae los ids fijos 1-14 de
    // ORDS (vienen del IDENTITY de APP_MENU, numeracion arbitraria) asi que comparar
    // por id ahi seria comparar numeros que no significan lo mismo. Ver docs/plan-rbac-local.md.
    private final boolean useLocalKeys;

    public ClinicalAccessFilter(PermissionService permissionService) {
        this(permissionService, null, false, false);
    }

    public ClinicalAccessFilter(
            PermissionService permissionService, LocalModulePermissionClient localClient, boolean shadowEnabled,
            boolean useLocalKeys) {
        this.permissionService = permissionService;
        this.localClient = localClient;
        this.shadowEnabled = shadowEnabled;
        this.useLocalKeys = useLocalKeys;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // getServletPath() excluye el context path (a diferencia de getRequestURI()) -
        // necesario porque el WAR se despliega con context path /portal-salud (ver
        // META-INF/context.xml); con getRequestURI() los prefijos de RUTAS_CLINICAS
        // nunca hacian match en ese deployment y el filtro no bloqueaba nada.
        String path = request.getServletPath();
        // Adjuntos (subir/descargar/borrar): los consumen varios modulos clinicos a la
        // vez y el path no distingue cual - quedan fuera de este primer alcance.
        if (path.startsWith("/api/nss/consulta/file")) {
            chain.doFilter(request, response);
            return;
        }
        Set<Integer> idsRequeridos = idsMenuRequeridos(path);
        Set<String> codesRequeridos = codesRequeridos(path);
        boolean esRutaClinica = idsRequeridos != null && !idsRequeridos.isEmpty();
        if (useLocalKeys && !esRutaClinica) {
            String codeExamenes = codeExamenesRequerido(path, request.getMethod());
            if (codeExamenes != null) {
                codesRequeridos = Set.of(codeExamenes);
                esRutaClinica = true;
            }
        }
        boolean permitido = !esRutaClinica || (useLocalKeys
                ? codesRequeridos.stream().anyMatch(permissionService::tieneAccesoPorCodigo)
                : idsRequeridos.stream().anyMatch(permissionService::tieneAcceso));

        // El modo sombra compara "que decidiria local" contra la decision real - solo
        // tiene sentido mientras la decision real siga siendo ORDS (useLocalKeys=false).
        // Si ya se volteo a LOCAL, la decision real YA es local - compararla contra si
        // misma via localClient seria ruido, no informacion.
        if (shadowEnabled && !useLocalKeys && esRutaClinica) {
            registrarComparacionSombra(path, permitido);
        }

        if (!permitido) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            log.warn("[permisos] usuario={} SIN acceso clinico a {} (fuente={}, requiere {})",
                    auth == null ? "?" : auth.getName(), path, useLocalKeys ? "LOCAL" : "ORDS",
                    useLocalKeys ? codesRequeridos : idsRequeridos);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("No tienes permiso para ver esta información clínica.");
            return;
        }
        chain.doFilter(request, response);
    }

    /** CODE de menu que exige una pagina del grupo Examenes (o null si la ruta no es de ese grupo). */
    private static String codeExamenesRequerido(String path, String method) {
        for (Map.Entry<String, String> e : RUTAS_EXAMENES_LOCAL.entrySet()) {
            String prefijo = e.getKey();
            if (path.equals(prefijo) || path.startsWith(prefijo + "/")) {
                if ("/api/consumibles".equals(prefijo) && "GET".equalsIgnoreCase(method)) {
                    return null;
                }
                return e.getValue();
            }
        }
        return null;
    }

    /**
     * Calcula EN PARALELO lo que decidiria el esquema local (sin afectar la
     * respuesta real, que siempre sigue siendo la de ORDS mientras
     * portal.permissions.source=ORDS) y lo loguea junto al resultado real, para
     * comparar N dias de trafico antes de voltear la fuente de verdad. Cualquier
     * fallo aqui (usuario no migrado, excepcion de BD) se traga y se loguea como
     * discrepancia, nunca rompe el request real.
     */
    private void registrarComparacionSombra(String path, boolean permitidoOrds) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String usuario = auth == null ? null : auth.getName();
        Set<String> codesRequeridos = codesRequeridos(path);
        boolean permitidoLocal;
        try {
            permitidoLocal = usuario != null
                    && localClient.findRoleId(usuario)
                            .map(localClient::findMenusByRole)
                            .map(modulos -> modulos.stream().map(ModuleDto::code)
                                    .anyMatch(codesRequeridos::contains))
                            .orElse(false);
        } catch (Exception ex) {
            log.warn("[permisos-sombra] usuario={} ruta={} error calculando decision local: {}", usuario, path, ex.getMessage());
            return;
        }
        log.info("[permisos-sombra] usuario={} ruta={} ords={} local={} coincide={}",
                usuario, path, permitidoOrds ? "permitido" : "403", permitidoLocal ? "permitido" : "403",
                permitidoOrds == permitidoLocal);
    }

    private static Set<Integer> idsMenuRequeridos(String path) {
        for (Map.Entry<String, Set<Integer>> entry : RUTAS_CLINICAS.entrySet()) {
            if (path.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static Set<String> codesRequeridos(String path) {
        for (Map.Entry<String, Set<String>> entry : RUTAS_CLINICAS_LOCAL.entrySet()) {
            if (path.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return Set.of();
    }
}
