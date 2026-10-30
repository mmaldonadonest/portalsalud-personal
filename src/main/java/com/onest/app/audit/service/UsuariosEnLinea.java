package com.onest.app.audit.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Usuarios en linea (29-oct-2026, modulo Auditoria): quien esta conectado y quien se desconecto en las ultimas
 * horas, en memoria. No va a la base: es una foto del momento, no bitacora, y se pierde al reiniciar Tomcat (las
 * sesiones tambien). Vale mientras el portal corra en UN solo Tomcat, como hoy; con varios, cada uno veria solo
 * sus sesiones.
 *
 * En linea = sesion abierta con alguna peticion en los ultimos {@link #EN_LINEA}. La campanita consulta cada 45 s,
 * asi que una pestana abierta cuenta como en linea aunque no se toque; por eso se guarda aparte la ultima accion
 * real (peticion que no es un sondeo). Al cerrar sesion la sesion queda como desconectada, no se borra, para que
 * la pantalla muestre tambien a quien ya salio; se olvida a las {@link #RETENCION}.
 */
@Component
public class UsuariosEnLinea implements HttpSessionListener {

    static final Duration EN_LINEA = Duration.ofMinutes(5);
    static final Duration RETENCION = Duration.ofHours(12);

    /** Sondeos automaticos del navegador: mantienen viva la sesion pero no son trabajo del usuario. */
    private static final List<String> SONDEOS = List.of("/api/notificaciones", "/api/auditoria/en-linea");

    /** login = el NSS con que entro (para cerrarle la sesion al bloquearlo); usuario = NSS + nombre, para mostrar. */
    private record Sesion(String login, String usuario, String roles, String ip, Instant inicio, Instant ultimaPeticion,
                          Instant ultimaAccion, String ultimaRuta, boolean cerrada) {
    }

    /** Un renglon por usuario (junta sus sesiones). enLinea es el 1/0 que cuenta la estadistica. */
    public record Usuario(String usuario, String roles, boolean enLinea, int sesionesAbiertas, Instant inicio,
                          Instant ultimaPeticion, Instant ultimaAccion, String ultimaRuta, String ip) {
    }

    private final Map<String, Sesion> sesiones = new ConcurrentHashMap<>();
    /** Las sesiones vivas, para poder invalidarlas al bloquear a un usuario (Admin > Usuarios). */
    private final Map<String, HttpSession> abiertas = new ConcurrentHashMap<>();

    /** Una peticion autenticada (la llama UsuariosEnLineaInterceptor). Nunca lanza. */
    public void registrar(HttpServletRequest request) {
        try {
            HttpSession s = request.getSession(false);
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (s == null || auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                return;
            }
            Instant ahora = Instant.now();
            String ruta = request.getRequestURI().substring(request.getContextPath().length());
            boolean sondeo = SONDEOS.stream().anyMatch(ruta::startsWith);
            abiertas.put(s.getId(), s);
            sesiones.compute(s.getId(), (id, prev) -> new Sesion(
                    auth.getName(),
                    AuditoriaService.usuarioActual(),
                    roles(auth),
                    AuditoriaService.ipDe(request),
                    prev == null ? ahora : prev.inicio(),
                    ahora,
                    sondeo && prev != null ? prev.ultimaAccion() : (sondeo ? null : ahora),
                    sondeo && prev != null ? prev.ultimaRuta() : (sondeo ? null : ruta),
                    false));
        } catch (RuntimeException ignorada) {
            // la lista es informativa: nunca debe tumbar una peticion
        }
    }

    /** Usuarios vistos en las ultimas {@link #RETENCION}: primero los en linea, luego por ultima actividad. */
    public List<Usuario> usuarios() {
        Instant ahora = Instant.now();
        sesiones.values().removeIf(x -> x.ultimaPeticion().isBefore(ahora.minus(RETENCION)));
        Instant limite = ahora.minus(EN_LINEA);
        return sesiones.values().stream()
                .collect(Collectors.groupingBy(Sesion::usuario))
                .values().stream()
                .map(lista -> {
                    List<Sesion> abiertas = lista.stream()
                            .filter(x -> !x.cerrada() && x.ultimaPeticion().isAfter(limite)).toList();
                    Sesion ultima = lista.stream().max(Comparator.comparing(Sesion::ultimaPeticion)).orElseThrow();
                    Sesion conAccion = lista.stream().filter(x -> x.ultimaAccion() != null)
                            .max(Comparator.comparing(Sesion::ultimaAccion)).orElse(ultima);
                    return new Usuario(ultima.usuario(), ultima.roles(), !abiertas.isEmpty(), abiertas.size(),
                            lista.stream().map(Sesion::inicio).min(Comparator.naturalOrder()).orElse(null),
                            ultima.ultimaPeticion(), conAccion.ultimaAccion(), conAccion.ultimaRuta(), ultima.ip());
                })
                .sorted(Comparator.comparing(Usuario::enLinea).reversed()
                        .thenComparing(Usuario::ultimaPeticion, Comparator.reverseOrder()))
                .toList();
    }

    /**
     * Cierra en el acto todas las sesiones abiertas de {@code login} (bloqueo en Admin > Usuarios): en su siguiente
     * clic el portal lo manda al login, donde ya no puede entrar. Regresa cuantas sesiones cerro.
     */
    public int cerrarSesionesDe(String login) {
        List<String> ids = sesiones.entrySet().stream()
                .filter(e -> !e.getValue().cerrada() && e.getValue().login() != null && e.getValue().login().equalsIgnoreCase(login))
                .map(Map.Entry::getKey)
                .toList();
        int cerradas = 0;
        for (String id : ids) {
            HttpSession s = abiertas.remove(id);
            if (s == null) {
                continue;
            }
            try {
                s.invalidate();   // dispara sessionDestroyed: queda como desconectado
                cerradas++;
            } catch (IllegalStateException yaInvalida) {
                // vencio entre la lectura y el invalidate: ya esta fuera
            }
        }
        return cerradas;
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        // cerrar sesion o vencer: queda como desconectado con su ultima actividad
        abiertas.remove(se.getSession().getId());
        sesiones.computeIfPresent(se.getSession().getId(), (id, x) -> new Sesion(x.login(), x.usuario(), x.roles(), x.ip(),
                x.inicio(), x.ultimaPeticion(), x.ultimaAccion(), x.ultimaRuta(), true));
    }

    private static String roles(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .collect(Collectors.joining(", "));
    }
}
