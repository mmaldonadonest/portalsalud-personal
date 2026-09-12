package com.onest.app.audit.service;

import com.onest.app.audit.dto.AuditoriaDto;
import com.onest.app.audit.dto.AuditoriaDto.Evento;
import com.onest.app.audit.dto.AuditoriaDto.Filtros;
import com.onest.app.audit.model.AudEvent;
import com.onest.app.audit.repository.AudEventRepository;
import com.onest.app.security.service.PortalUserPrincipal;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bitacora funcional del portal (modulo Auditoria). Escribe en APP_AUD_EVENT y la consulta
 * con filtros. Registrar NUNCA lanza: una bitacora caida no debe tumbar la operacion que
 * audita (se loguea y sigue). Usuario, IP y trace-id se toman del contexto del request.
 */
@Service
public class AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);
    private static final int MAX_DETALLE = 4000;
    private static final int MAX_POR_PAGINA = 200;

    private final AudEventRepository repository;

    public AuditoriaService(AudEventRepository repository) {
        this.repository = repository;
    }

    /**
     * @param modulo   Consultas, Incapacidades, Accidentes, Examenes, Antidoping, Maternidad,
     *                 Administracion, Sistema...
     * @param accion   create | update | delete | export | login
     * @param entidad  tipo de registro (Consulta, Rol, Archivo...)
     * @param registro clave del registro (NSS, id) - puede ser null
     * @param detalle  texto legible para el analista (se recorta a 4000)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(String modulo, String accion, String entidad, String registro, String detalle,
                          HttpServletRequest request) {
        try {
            AudEvent e = new AudEvent();
            e.setEventType(recorta(modulo, 100));
            e.setAction(recorta(accion, 80));
            e.setEntityName(recorta(entidad, 120));
            e.setEntityId(recorta(registro, 120));
            e.setDetailJson(recorta(detalle, MAX_DETALLE));
            e.setEventTs(LocalDateTime.now());
            e.setUsername(recorta(usuarioActual(), 120));
            e.setTraceId(recorta(MDC.get("traceId"), 120));
            e.setIpAddress(recorta(ipDe(request), 80));
            repository.save(e);
        } catch (RuntimeException ex) {
            log.warn("[auditoria] no se pudo registrar {}/{} {}: {}", modulo, accion, registro, ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public AuditoriaDto buscar(String usuario, String modulo, String accion, String desde, String hasta,
                               String texto, int pagina, int porPagina) {
        int size = Math.max(1, Math.min(porPagina, MAX_POR_PAGINA));
        int page = Math.max(0, pagina);
        Specification<AudEvent> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (lleno(usuario)) {
                p.add(cb.equal(root.get("username"), usuario.trim()));
            }
            if (lleno(modulo)) {
                p.add(cb.equal(root.get("eventType"), modulo.trim()));
            }
            if (lleno(accion)) {
                p.add(cb.equal(root.get("action"), accion.trim()));
            }
            if (lleno(desde)) {
                p.add(cb.greaterThanOrEqualTo(root.get("eventTs"), LocalDate.parse(desde.trim()).atStartOfDay()));
            }
            if (lleno(hasta)) {
                p.add(cb.lessThan(root.get("eventTs"), LocalDate.parse(hasta.trim()).plusDays(1).atStartOfDay()));
            }
            if (lleno(texto)) {
                String like = "%" + texto.trim().toLowerCase() + "%";
                p.add(cb.or(
                        cb.like(cb.lower(root.get("entityId")), like),
                        cb.like(cb.lower(root.get("entityName")), like),
                        cb.like(cb.lower(root.get("detailJson")), like)));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
        var res = repository.findAll(spec, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "eventTs", "id")));
        List<Evento> eventos = res.getContent().stream().map(e -> new Evento(
                e.getId(),
                e.getEventTs() == null ? null : e.getEventTs().withNano(0).toString(),
                e.getUsername(), e.getAction(), e.getEventType(), e.getEntityName(), e.getEntityId(),
                e.getDetailJson(), e.getIpAddress())).toList();
        return new AuditoriaDto(res.getTotalElements(), page, size, eventos);
    }

    @Transactional(readOnly = true)
    public Filtros filtros() {
        return new Filtros(repository.usuarios(), repository.modulos(), repository.acciones());
    }

    /** "NSS · Nombre" cuando el principal trae nombre (PortalUserPrincipal); solo el NSS si no. */
    private static String usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return null;
        }
        if (auth.getPrincipal() instanceof PortalUserPrincipal p && p.getDisplayName() != null
                && !p.getDisplayName().isBlank() && !p.getDisplayName().equals(auth.getName())) {
            return auth.getName() + " · " + p.getDisplayName().trim();
        }
        return auth.getName();
    }

    /** Detras del nginx de QA la IP real viene en X-Forwarded-For (primer valor). */
    private static String ipDe(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static boolean lleno(String v) {
        return v != null && !v.isBlank();
    }

    private static String recorta(String v, int max) {
        if (v == null) {
            return null;
        }
        String t = v.trim();
        return t.length() <= max ? t : t.substring(0, max);
    }
}
