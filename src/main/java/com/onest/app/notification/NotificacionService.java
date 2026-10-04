package com.onest.app.notification;

import com.onest.app.notification.NotificacionRepository.Notificacion;
import com.onest.app.security.permission.PermissionService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Notificaciones de la campanita. Una notificacion la ve el usuario si (a) va dirigida a su
 * usuario, (b) va dirigida a un menu al que tiene acceso (DESTINO_MENU_CODE, resuelto con
 * {@link PermissionService#tieneAccesoPorCodigo}) o (c) no tiene destino (todos).
 *
 * <p>Nunca lanza hacia la vista: sin la tabla (notificaciones.sql sin aplicar) devuelve vacio
 * y lo deja en el log, para que el encabezado siga pintando.
 */
@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);
    private static final int MAX_PENDIENTES = 50;
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    /** Lo que ve el navegador. */
    public record Item(long id, String tipo, String titulo, String mensaje, String nss, String enlace,
                       String fecha, String hace, String inicial) {
    }

    private final NotificacionRepository repository;
    private final PermissionService permissionService;

    public NotificacionService(NotificacionRepository repository, PermissionService permissionService) {
        this.repository = repository;
        this.permissionService = permissionService;
    }

    public List<Item> pendientesDelUsuario() {
        List<Notificacion> todas;
        try {
            todas = repository.pendientes(MAX_PENDIENTES);
        } catch (DataAccessException ex) {
            log.warn("[notificaciones] no disponibles: {}", ex.getMostSpecificCause().getMessage());
            return List.of();
        }
        String usuario = usuarioActual();
        List<Item> out = new ArrayList<>();
        for (Notificacion n : todas) {
            if (!meCorresponde(n, usuario)) {
                continue;
            }
            out.add(new Item(n.id(), n.tipo(), n.titulo(), n.mensaje() == null ? "" : n.mensaje(), n.nss(),
                    n.enlace() == null ? "" : n.enlace(), n.createdAt().format(FECHA), hace(n.createdAt()), inicial(n.tipo())));
        }
        return out;
    }

    public void marcarLeida(long id) {
        repository.marcarLeida(id, usuarioActual());
    }

    public void marcarLeidas(List<Long> ids) {
        repository.marcarLeidas(ids, usuarioActual());
    }

    /**
     * Crea una notificacion. {@code origen}+{@code referencia} evitan duplicados (p.ej. OCR
     * reenviando la misma solicitud): si ya existe, no se crea otra y devuelve -1.
     */
    public long crear(String tipo, String titulo, String mensaje, String nss, String enlace, String destinoMenuCode,
                      String destinoUsuario, String origen, String referencia) {
        if (origen != null && referencia != null && repository.existe(origen, referencia)) {
            return -1;
        }
        return repository.crear(tipo, titulo, mensaje, nss, enlace, destinoMenuCode, destinoUsuario, origen, referencia, usuarioActual());
    }

    private boolean meCorresponde(Notificacion n, String usuario) {
        if (n.destinoUsuario() != null && !n.destinoUsuario().isBlank()) {
            return n.destinoUsuario().trim().equalsIgnoreCase(usuario);
        }
        if (n.destinoMenuCode() != null && !n.destinoMenuCode().isBlank()) {
            try {
                return permissionService.tieneAccesoPorCodigo(n.destinoMenuCode().trim());
            } catch (RuntimeException ex) {
                return false;
            }
        }
        return true;
    }

    private static String hace(LocalDateTime t) {
        Duration d = Duration.between(t, LocalDateTime.now());
        long m = d.toMinutes();
        if (m < 1) {
            return "ahora";
        }
        if (m < 60) {
            return "hace " + m + " min";
        }
        long h = d.toHours();
        if (h < 24) {
            return "hace " + h + " h";
        }
        return "hace " + d.toDays() + " d";
    }

    private static final Map<String, String> INICIALES = Map.of("EXAMEN_INICIAL_SOLICITUD", "EX");

    private static String inicial(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return "N";
        }
        return INICIALES.getOrDefault(tipo, tipo.substring(0, 1).toUpperCase());
    }

    private static String usuarioActual() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a == null || a.getName() == null ? "SISTEMA" : a.getName();
    }
}
