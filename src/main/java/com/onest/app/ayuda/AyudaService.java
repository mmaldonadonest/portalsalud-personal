package com.onest.app.ayuda;

import com.onest.app.security.permission.PermissionService;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Chat de ayuda sin IA: la pregunta del usuario se busca en los articulos (Meilisearch si esta,
 * LIKE en Oracle si no), filtrada por los modulos que su rol puede ver, y se registra para saber
 * que falta documentar. Nunca manda ni guarda datos de pacientes: los NSS/CURP se tachan antes.
 */
@Service
public class AyudaService {

    private static final Logger log = LoggerFactory.getLogger(AyudaService.class);
    private static final Pattern NSS = Pattern.compile("\\b\\d{10,11}\\b");
    private static final Pattern CURP = Pattern.compile("\\b[A-Za-z]{4}\\d{6}[HMhm][A-Za-z]{5}[A-Za-z0-9]\\d\\b");
    private static final int LIMITE = 4;

    /** Sinonimos del dominio: lo que la gente escribe -> como esta escrito en los articulos. */
    static final Map<String, List<String>> SINONIMOS = new LinkedHashMap<>();
    static {
        SINONIMOS.put("imss", List.of("st2", "st-2", "incapacidad"));
        SINONIMOS.put("inicial", List.of("ingreso", "nuevo ingreso", "candidato", "admision"));
        SINONIMOS.put("apto inclusion", List.of("apto restringido"));
        SINONIMOS.put("apto restringido", List.of("apto inclusion"));
        SINONIMOS.put("condicionado", List.of("apto condicionado"));
        SINONIMOS.put("pdf", List.of("archivo", "documento", "adjunto"));
        SINONIMOS.put("archivo", List.of("pdf", "adjunto"));
        SINONIMOS.put("campanita", List.of("notificacion", "aviso", "campana"));
        SINONIMOS.put("notificacion", List.of("campanita", "aviso"));
        SINONIMOS.put("bandeja", List.of("pendientes", "examenes iniciales"));
        SINONIMOS.put("icd", List.of("cie", "diagnostico", "catalogo"));
        SINONIMOS.put("diagnostico", List.of("icd", "cie"));
        SINONIMOS.put("borrador", List.of("autoguardado", "guardar solo"));
        SINONIMOS.put("doping", List.of("antidoping"));
        SINONIMOS.put("alcohol", List.of("alcoholemia"));
        SINONIMOS.put("embarazo", List.of("maternidad"));
        SINONIMOS.put("accidente", List.of("riesgo de trabajo", "rt"));
        SINONIMOS.put("login", List.of("entrar", "acceso", "iniciar sesion", "contrasena"));
        SINONIMOS.put("contrasena", List.of("password", "clave"));
        SINONIMOS.put("permiso", List.of("rol", "acceso", "menu"));
        SINONIMOS.put("trabajador", List.of("empleado", "nss", "persona"));
    }

    public record Resultado(long id, String clave, String modulo, String titulo, String tituloResaltado, String fragmento, String enlace) {
    }

    public record Respuesta(long preguntaId, String motor, List<Resultado> resultados) {
    }

    private final AyudaArticuloRepository articulos;
    private final AyudaPreguntaRepository preguntas;
    private final MeilisearchClient meili;
    private final PermissionService permisos;
    private final boolean permisosLocales;
    private volatile boolean meiliListo;

    public AyudaService(AyudaArticuloRepository articulos, AyudaPreguntaRepository preguntas, MeilisearchClient meili,
                        PermissionService permisos, @Value("${portal.permissions.source:ORDS}") String permissionsSource) {
        this.articulos = articulos;
        this.preguntas = preguntas;
        this.meili = meili;
        this.permisos = permisos;
        this.permisosLocales = "LOCAL".equalsIgnoreCase(permissionsSource);
    }

    /** Al arrancar: si Meilisearch responde (2 s de espera maximo), se configura y se vuelca todo el contenido. */
    @EventListener(ApplicationReadyEvent.class)
    public void alArrancar() {
        try {
            reindexar();
        } catch (RuntimeException ex) {
            log.warn("[ayuda] no se pudo reindexar al arrancar: {}", ex.getMessage());
        }
    }

    public synchronized String reindexar() {
        if (!meili.disponible()) {
            meiliListo = false;
            log.info("[ayuda] Meilisearch no disponible: el chat de ayuda busca en Oracle (LIKE)");
            return "Meilisearch no responde; se usa la búsqueda en Oracle.";
        }
        List<AyudaArticulo> todos = articulos.listar();
        meili.configurar(SINONIMOS);
        meili.reindexar(todos);
        meiliListo = true;
        log.info("[ayuda] {} artículos indexados en Meilisearch", todos.size());
        return todos.size() + " artículos indexados en Meilisearch.";
    }

    public boolean buscadorActivo() {
        return meiliListo;
    }

    /** La busqueda del chat. {@code modulos} null = sin filtro (fuente ORDS). */
    public Respuesta preguntar(String texto, String usuario) {
        String limpio = tachar(texto == null ? "" : texto.strip());
        if (limpio.length() < 2) {
            return new Respuesta(0, "", List.of());
        }
        Set<String> visibles = modulosVisibles();
        List<String> filtro = visibles == null ? null : conGeneral(visibles);
        List<Resultado> out = new ArrayList<>();
        String motor = "ORACLE";
        if (meiliListo) {
            try {
                for (MeilisearchClient.Hit h : meili.buscar(limpio, filtro, LIMITE)) {
                    articulos.porId(h.id()).filter(AyudaArticulo::activo).ifPresent(a ->
                            out.add(new Resultado(a.id(), a.clave(), a.modulo(), a.titulo(), h.tituloResaltado(), h.fragmento(), a.enlace())));
                }
                motor = "MEILI";
            } catch (RuntimeException ex) {
                log.warn("[ayuda] Meilisearch fallo, se usa Oracle: {}", ex.getMessage());
                meiliListo = false;
            }
        }
        if (!"MEILI".equals(motor)) {
            try {
                for (AyudaArticulo a : articulos.buscarLike(palabras(limpio), LIMITE)) {
                    if (filtro == null || filtro.contains(a.modulo())) {
                        out.add(new Resultado(a.id(), a.clave(), a.modulo(), a.titulo(), a.titulo(), resumen(a), a.enlace()));
                    }
                }
            } catch (org.springframework.dao.DataAccessException ex) {
                // Falta ayuda.sql en la base del portal: se responde vacio en vez de 500
                log.warn("[ayuda] sin tablas de ayuda: {}", ex.getMostSpecificCause().getMessage());
                return new Respuesta(0, "SIN_TABLAS", List.of());
            }
        }
        long id = 0;
        try {
            id = preguntas.registrar(usuario, limpio, out.size(), motor);
        } catch (RuntimeException ex) {
            log.debug("[ayuda] pregunta no registrada: {}", ex.getMessage());
        }
        return new Respuesta(id, motor, out);
    }

    public void calificar(long preguntaId, Long articuloId, boolean util) {
        preguntas.calificar(preguntaId, articuloId, util);
    }

    /** Articulo completo (en HTML) si esta activo y el usuario puede ver su modulo. */
    public Optional<AyudaArticulo> articuloVisible(long id) {
        Set<String> visibles = modulosVisibles();
        return articulos.porId(id).filter(AyudaArticulo::activo)
                .filter(a -> visibles == null || AyudaArticulo.GENERAL.equals(a.modulo()) || visibles.contains(a.modulo()));
    }

    /** Centro de ayuda: activos que el usuario puede ver, agrupados por modulo en el orden del contenido. */
    public Map<String, List<AyudaArticulo>> porModuloVisibles() {
        Set<String> visibles = modulosVisibles();
        Map<String, List<AyudaArticulo>> m = new LinkedHashMap<>();
        List<AyudaArticulo> activos;
        try {
            activos = articulos.activos();
        } catch (org.springframework.dao.DataAccessException ex) {
            log.warn("[ayuda] sin tablas de ayuda: {}", ex.getMostSpecificCause().getMessage());
            return m;
        }
        for (AyudaArticulo a : activos) {
            if (visibles == null || AyudaArticulo.GENERAL.equals(a.modulo()) || visibles.contains(a.modulo())) {
                m.computeIfAbsent(a.modulo(), k -> new ArrayList<>()).add(a);
            }
        }
        return m;
    }

    // ---- admin ---------------------------------------------------------------

    public List<AyudaArticulo> todos() {
        try {
            return articulos.listar();
        } catch (org.springframework.dao.DataAccessException ex) {
            log.warn("[ayuda] sin tablas de ayuda: {}", ex.getMostSpecificCause().getMessage());
            return List.of();
        }
    }

    public Optional<AyudaArticulo> porId(long id) {
        return articulos.porId(id);
    }

    public long guardar(AyudaArticulo a, String usuario) {
        if (a.titulo() == null || a.titulo().isBlank() || a.cuerpo() == null || a.cuerpo().isBlank()) {
            throw new IllegalArgumentException("Título y contenido son obligatorios");
        }
        String clave = a.id() > 0 ? a.clave() : slug(a.clave() == null || a.clave().isBlank() ? a.titulo() : a.clave());
        AyudaArticulo listo = new AyudaArticulo(a.id(), clave, a.modulo() == null || a.modulo().isBlank() ? AyudaArticulo.GENERAL : a.modulo().trim(),
                a.titulo().strip(), vacioANull(a.pregunta()), vacioANull(a.palabrasClave()), a.cuerpo(), vacioANull(a.enlace()),
                a.orden() <= 0 ? 100 : a.orden(), a.activo(), null, null);
        long id = articulos.guardar(listo, usuario);
        if (meiliListo) {
            try {
                articulos.porId(id).ifPresent(meili::indexar);
            } catch (RuntimeException ex) {
                log.warn("[ayuda] articulo {} guardado en Oracle pero no reindexado: {}", id, ex.getMessage());
            }
        }
        return id;
    }

    public void activar(long id, boolean activo, String usuario) {
        articulos.activar(id, activo, usuario);
        if (meiliListo) {
            try {
                articulos.porId(id).ifPresent(meili::indexar);
            } catch (RuntimeException ex) {
                log.warn("[ayuda] articulo {} no reindexado: {}", id, ex.getMessage());
            }
        }
    }

    public List<AyudaPreguntaRepository.Pregunta> preguntasSinRespuesta() {
        try {
            return preguntas.sinRespuesta(100);
        } catch (org.springframework.dao.DataAccessException ex) {
            return List.of();
        }
    }

    // ---- utilerias -----------------------------------------------------------

    /** NSS y CURP nunca salen del navegador hacia el buscador ni al registro de preguntas. */
    static String tachar(String s) {
        return CURP.matcher(NSS.matcher(s).replaceAll("[nss]")).replaceAll("[curp]");
    }

    static List<String> palabras(String s) {
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toUpperCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String p : n.split("[^A-Z0-9]+")) {
            if (p.length() >= 3 && !out.contains(p)) {
                out.add(p);
            }
        }
        return out;
    }

    static String slug(String s) {
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        n = n.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return n.length() > 60 ? n.substring(0, 60) : n;
    }

    private static String resumen(AyudaArticulo a) {
        String t = Markdown.textoPlano(a.cuerpo());
        return t.length() > 180 ? t.substring(0, 180) + "…" : t;
    }

    private Set<String> modulosVisibles() {
        if (!permisosLocales) {
            return null;
        }
        try {
            Set<String> codigos = permisos.modulosPermitidos().stream().map(m -> m.code()).filter(c -> c != null && !c.isBlank())
                    .collect(Collectors.toCollection(java.util.HashSet::new));
            // Los articulos agrupan por tema, no por menu: "EXPEDIENTE" (consultas) e "INCAPACIDAD" no son CODE de
            // SERV_MED_MENU. Sin esto, con permisos locales nadie veia esos articulos (corregido 22-oct-2026).
            if (codigos.contains("CONSULTA_MEDICA") || codigos.contains("ARCHIVO_CONSULTAS")) {
                codigos.add("EXPEDIENTE");
            }
            if (codigos.contains("INCAPACIDADES") || codigos.contains("ARCHIVO_INCAPACIDADES")) {
                codigos.add("INCAPACIDAD");
            }
            return codigos;
        } catch (RuntimeException ex) {
            return Set.of();
        }
    }

    private static List<String> conGeneral(Set<String> visibles) {
        List<String> l = new ArrayList<>(visibles);
        l.add(AyudaArticulo.GENERAL);
        return l;
    }

    private static String vacioANull(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }

    static String usuarioActual() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a == null || a.getName() == null ? "anonimo" : a.getName();
    }
}
