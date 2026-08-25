package com.onest.app.security.permission;

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
    // ids que ya usa fragments/nss-modules.html para dibujar el menu lateral.
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

    private final PermissionService permissionService;

    public ClinicalAccessFilter(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        // Adjuntos (subir/descargar/borrar): los consumen varios modulos clinicos a la
        // vez y el path no distingue cual - quedan fuera de este primer alcance.
        if (path.startsWith("/api/nss/consulta/file")) {
            chain.doFilter(request, response);
            return;
        }
        Set<Integer> requeridos = idsMenuRequeridos(path);
        if (requeridos != null && !requeridos.isEmpty() && requeridos.stream().noneMatch(permissionService::tieneAcceso)) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            log.warn("[permisos] usuario={} SIN acceso clinico a {} (requiere id_menu {})",
                    auth == null ? "?" : auth.getName(), path, requeridos);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("No tienes permiso para ver esta información clínica.");
            return;
        }
        chain.doFilter(request, response);
    }

    private static Set<Integer> idsMenuRequeridos(String path) {
        for (Map.Entry<String, Set<Integer>> entry : RUTAS_CLINICAS.entrySet()) {
            if (path.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }
}
