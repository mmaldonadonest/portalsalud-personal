package com.onest.app.catalog.pretest.service;

import com.onest.app.catalog.pretest.repository.MedTagRepository;
import java.util.HashMap;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pre-Test (U03): cuestionario EAV en SERV_MED_TAG. Cada campo se guarda con TYPE = <base>PRETEST
 * y TAG_GROUP='PRETEST'. Equivale a pretest.php (load) + savePretest.php (save).
 */
@Service
public class PretestService {

    private static final String SUFFIX = "PRETEST";
    private static final String GROUP = "PRETEST";

    /** Una pregunta Si/No del cuestionario, con su codigo y su etiqueta. */
    public record Pregunta(String codigo, String etiqueta) {
    }

    /**
     * Las 15 preguntas del cuestionario, en el mismo orden y con los mismos codigos que el
     * formulario (y que el PHP: verificado 14-sep-2026, los 15 codigos coinciden). Vive aqui
     * para que el formulario y el documento impreso no repitan las etiquetas.
     */
    public static final List<Pregunta> SALUD = List.of(
            new Pregunta("LHOP", "¿Lo han operado en menos de 6 meses?"),
            new Pregunta("TALMED", "¿Toma algún medicamento en específico?"),
            new Pregunta("SUAA", "¿Sufre de alguna alergia?"),
            new Pregunta("SAFOPH", "¿Se ha fracturado o padece hernias?"),
            new Pregunta("ESEMB", "¿Está embarazada (solo mujeres)?"),
            new Pregunta("PALENF", "¿Padece alguna enfermedad(es)?"),
            new Pregunta("CONDROG", "¿Actualmente consume drogas?"));

    public static final List<Pregunta> SINTOMAS = List.of(
            new Pregunta("TOSIN", "Tos"),
            new Pregunta("FIEBREIN", "Fiebre"),
            new Pregunta("DOLOCABINP", "Dolor de cabeza"),
            new Pregunta("DIFRESP", "Dificultad para respirar"),
            new Pregunta("DOLARDGARG", "Dolor o ardor en la garganta"),
            new Pregunta("ESCURRINAZ", "Escurrimiento nasal"),
            new Pregunta("OJOSROJOS", "Ojos rojos"),
            new Pregunta("DOLMUSART", "Dolor muscular o articular"));

    private final MedTagRepository repository;

    public PretestService(MedTagRepository repository) {
        this.repository = repository;
    }

    /** Carga los campos del pretest, mapeados por su nombre base (sin sufijo PRETEST). */
    public Map<String, String> load(String nss) {
        Map<String, String> byType = repository.latestByNssAndTypeSuffix(normalizeNss(nss), SUFFIX);
        Map<String, String> byBase = new HashMap<>();
        byType.forEach((type, content) -> {
            String base = type.endsWith(SUFFIX) ? type.substring(0, type.length() - SUFFIX.length()) : type;
            byBase.put(base, content);
        });
        return byBase;
    }

    /** Fecha del ultimo guardado del Pre-Test de ese NSS, o null si nunca se ha guardado. */
    public LocalDateTime ultimoGuardado(String nss) {
        return repository.ultimoGuardado(normalizeNss(nss), SUFFIX);
    }

    /** Guarda cada campo (base -> base+PRETEST) con DELETE+INSERT. */
    @Transactional
    public void save(String nss, Map<String, String> fields) {
        String normalized = normalizeNss(nss);
        String usuario = usuarioActual();
        fields.forEach((base, value) ->
                repository.upsert(normalized, base + SUFFIX, value, GROUP, usuario));
        // C-007 (18-nov-2026): los campos de arriba se sobrescriben en cada guardado; esta copia fechada es la
        // que deja historia para Historico E.M. y el comparativo C-006. No termina en PRETEST, asi que load() y
        // ultimoGuardado() (TYPE LIKE %PRETEST) no la ven.
        repository.upsert(normalized, HIST + LocalDateTime.now(ZONA).format(SELLO), copia(fields), GRUPO_HIST, usuario);
    }

    static final String HIST = "PRETESTHIST.";
    private static final String GRUPO_HIST = "PRETEST_HIST";
    private static final java.time.ZoneId ZONA = java.time.ZoneId.of("America/Mexico_City");
    private static final java.time.format.DateTimeFormatter SELLO = java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final List<String> DATOS_COPIA = List.of("cuenta", "puesto", "agencia");

    /** Respuestas Si/No (y datos de cabecera) como CLAVE=valor por renglon; sin la firma ni observaciones. */
    static String copia(Map<String, String> fields) {
        StringBuilder sb = new StringBuilder();
        java.util.stream.Stream.concat(java.util.stream.Stream.concat(SALUD.stream(), SINTOMAS.stream()).map(Pregunta::codigo),
                DATOS_COPIA.stream()).forEach(k -> {
            String v = fields.get(k);
            if (v != null && !v.isBlank()) {
                sb.append(k).append('=').append(v.trim().replaceAll("[\\r\\n=]+", " ")).append('\n');
            }
        });
        return sb.toString();
    }

    /** Un Pretest del historial. legado = el unico que se conoce de antes de C-007 (sus respuestas vigentes). */
    public record Historial(LocalDateTime fecha, String usuario, Map<String, String> respuestas, boolean legado) {

        /** «Síntomas: Tos, Fiebre» o «Sin síntomas»; mas las preguntas de salud contestadas Si. */
        public String resumen() {
            List<String> sint = SINTOMAS.stream().filter(p -> "1".equals(respuestas.get(p.codigo()))).map(Pregunta::etiqueta).toList();
            List<String> salud = SALUD.stream().filter(p -> "1".equals(respuestas.get(p.codigo())))
                    .map(p -> p.etiqueta().replace("¿", "").replace("?", "")).toList();
            String r = sint.isEmpty() ? "Sin síntomas" : "Síntomas: " + String.join(", ", sint);
            return salud.isEmpty() ? r : r + " · Sí: " + String.join("; ", salud);
        }
    }

    /** Pretest del NSS, el mas reciente primero. */
    public List<Historial> historial(String nss) {
        String n = normalizeNss(nss);
        List<Historial> out = new java.util.ArrayList<>();
        for (MedTagRepository.Tag t : repository.todosPorPrefijo(n, HIST)) {
            Map<String, String> r = new HashMap<>();
            for (String linea : (t.content() == null ? "" : t.content()).split("\n")) {
                int i = linea.indexOf('=');
                if (i > 0) {
                    r.put(linea.substring(0, i), linea.substring(i + 1));
                }
            }
            out.add(new Historial(fecha(t), t.creadoPor(), r, false));
        }
        // de antes de C-007 solo existen las respuestas vigentes: se muestran como un Pretest con su fecha
        if (out.isEmpty()) {
            LocalDateTime ultimo = repository.ultimoGuardado(n, SUFFIX);
            if (ultimo != null) {
                out.add(new Historial(ultimo, null, load(n), true));
            }
        }
        return out;
    }

    /** La fecha va en el TYPE (hora de la Ciudad de Mexico); CREATED_AT solo si el sello no se puede leer. */
    private static LocalDateTime fecha(MedTagRepository.Tag t) {
        try {
            return LocalDateTime.parse(t.type().substring(HIST.length()), SELLO);
        } catch (RuntimeException ex) {
            return t.creado();
        }
    }

    private String normalizeNss(String nss) {
        if (nss == null || nss.isBlank()) {
            throw new IllegalArgumentException("El NSS es obligatorio");
        }
        String value = nss.trim();
        if (value.length() > 50) {
            throw new IllegalArgumentException("El NSS excede la longitud permitida");
        }
        return value;
    }

    private String usuarioActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return "SISTEMA";
        }
        return authentication.getName();
    }
}
