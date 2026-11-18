package com.onest.app.catalog.incapacidad.service;

import com.onest.app.catalog.incapacidad.repository.CadenaIncapacidadRepository;
import com.onest.app.catalog.incapacidad.repository.CadenaIncapacidadRepository.Fila;
import com.onest.app.catalog.pretest.repository.MedTagRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Cadena de una incapacidad: la inicial y sus subsecuentes (C-050, C-051, C-052, C-053, C-054; 18-nov-2026).
 *
 * <p>El WS de alta no tiene columnas para la liga ni para el diagnostico, asi que van a SERV_MED_TAG ligados al
 * idArchivoRel de cada incapacidad (mismo patron que el pase de salida FT-SO-02), sin tocar el WS:
 * {@code INCAP.<rel>.ANTERIOR} = REG_ID de la incapacidad que continua y {@code INCAP.<rel>.DIAGNOSTICO}.
 *
 * <p>Lo capturado antes no tiene liga. Para no dejarlo suelto, una subsecuente sin liga se pega a la del mismo NSS
 * cuyo termino + 1 es su inicio (asi vienen en el Excel de SM: 10/12 -> 11/12 -> 08/01) o, si no, a la que tiene
 * su misma fecha de primera incapacidad. Si no hay ninguna, queda como cadena propia.
 */
@Service
public class CadenaIncapacidadService {

    static final String PREFIJO = "INCAP.";
    static final String GRUPO = "INCAPACIDAD_CADENA";
    static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Una cadena: eslabones ordenados por inicio (la inicial primero). */
    public record Cadena(List<Fila> eslabones, String diagnostico) {

        public Fila inicial() {
            return eslabones.get(0);
        }

        public Fila ultimo() {
            return eslabones.get(eslabones.size() - 1);
        }

        /** Fecha de la primera incapacidad: el inicio de la inicial (la pone el portal, no se teclea). */
        public LocalDate primera() {
            Fila i = inicial();
            // una subsecuente que quedo sin su inicial (captura vieja) conserva la primera fecha que trae guardada
            if (i.subsecuente() && i.primera() != null) {
                return i.primera();
            }
            return i.inicio() != null ? i.inicio() : i.primera();
        }

        /** Termino de la cadena: el mayor termino de sus eslabones. */
        public LocalDate termino() {
            return eslabones.stream().map(Fila::termino).filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null);
        }

        /** C-050: acumulados = termino - primera incapacidad + 1 (el dia de inicio cuenta), como el Excel de SM. */
        public Long acumulados() {
            LocalDate p = primera();
            LocalDate t = termino();
            return p == null || t == null || t.isBefore(p) ? null : ChronoUnit.DAYS.between(p, t) + 1;
        }

        /** C-054: suma de los dias autorizados de la inicial y sus subsecuentes. */
        public int diasTotal() {
            return eslabones.stream().map(Fila::dias).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
        }

        public double costoTotal() {
            return eslabones.stream().map(Fila::costo).filter(Objects::nonNull).mapToDouble(Double::doubleValue).sum();
        }

        /** C-053: con Alta = Si en cualquier eslabon la cadena se cerro. */
        public boolean cerrada() {
            return eslabones.stream().anyMatch(Fila::conAlta);
        }

        public String folio() {
            return inicial().folio();
        }

        public String periodo() {
            return fmt(primera()) + " a " + fmt(termino());
        }
    }

    private final CadenaIncapacidadRepository repo;
    private final MedTagRepository tags;

    public CadenaIncapacidadService(CadenaIncapacidadRepository repo, MedTagRepository tags) {
        this.repo = repo;
        this.tags = tags;
    }

    /** Todas las cadenas del NSS, la mas reciente primero. */
    public List<Cadena> cadenas(String nss) {
        String n = nss.trim();
        // sin ninguna fecha no hay padecimiento que resumir ni que continuar (renglones viejos del PHP con fechas
        // vacias, diagnostico 17-nov seccion 8); siguen visibles en la lista de incapacidades
        return armar(repo.porNss(n), tags.latestByNssAndTypePrefix(n, PREFIJO)).stream()
                .filter(c -> c.primera() != null || c.termino() != null)
                .toList();
    }

    /** Cadenas que aun admiten una subsecuente (sin Alta = Si). Es lo que ofrece el formulario. */
    public List<Cadena> abiertas(String nss) {
        return cadenas(nss).stream().filter(c -> !c.cerrada()).toList();
    }

    /**
     * La cadena a la que pertenece la incapacidad {@code regIdAnterior}, validando que sea de este NSS y que siga
     * abierta. IllegalArgumentException con el mensaje para el usuario si no.
     */
    public Cadena paraSubsecuente(String nss, String regIdAnterior) {
        if (regIdAnterior == null || !regIdAnterior.trim().matches("\\d{1,18}")) {
            throw new IllegalArgumentException("En una subsecuente hay que elegir la incapacidad que continúa.");
        }
        long id = Long.parseLong(regIdAnterior.trim());
        Cadena c = cadenas(nss).stream()
                .filter(x -> x.eslabones().stream().anyMatch(f -> f.regId() == id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("La incapacidad que continúa no existe para este NSS. Recarga la pantalla."));
        if (c.cerrada()) {
            throw new IllegalArgumentException("La incapacidad " + etiqueta(c) + " ya tiene Alta: no se le pueden agregar subsecuentes.");
        }
        return c;
    }

    /** Guarda la liga y el diagnostico de la incapacidad recien dada de alta. */
    public void guardarExtras(String nss, String rel, Long anterior, String diagnostico) {
        if (rel == null || !rel.trim().matches("[A-Za-z0-9]{8,64}")) {
            return;
        }
        String base = PREFIJO + rel.trim() + ".";
        String usuario = usuarioActual();
        if (anterior != null) {
            tags.upsert(nss.trim(), base + "ANTERIOR", String.valueOf(anterior), GRUPO, usuario);
        }
        String d = diagnostico == null ? "" : diagnostico.trim().replaceAll("\\s+", " ");
        if (!d.isEmpty()) {
            tags.upsert(nss.trim(), base + "DIAGNOSTICO", d.length() > 300 ? d.substring(0, 300) : d, GRUPO, usuario);
        }
    }

    public static String etiqueta(Cadena c) {
        String folio = c.folio() == null || c.folio().isBlank() ? "sin folio" : "folio " + c.folio().trim();
        return folio + " (" + c.periodo() + ")";
    }

    static String fmt(LocalDate d) {
        return d == null ? "?" : d.format(DMY);
    }

    // ----------------------------------------------------------------------------------------------------------

    static List<Cadena> armar(List<Fila> filas, Map<String, String> tagsNss) {
        Map<Long, Fila> porId = new LinkedHashMap<>();
        filas.forEach(f -> porId.put(f.regId(), f));
        Map<Long, Long> padre = new LinkedHashMap<>();
        for (Fila f : filas) {
            Long p = anteriorDeclarado(f, tagsNss, porId);
            if (p == null && f.subsecuente()) {
                p = anteriorInferido(f, filas);
            }
            if (p != null) {
                padre.put(f.regId(), p);
            }
        }
        // raiz de cada fila: se sube por los padres (siempre a un REG_ID menor, asi que no hay ciclos)
        Map<Long, List<Fila>> grupos = new LinkedHashMap<>();
        for (Fila f : filas) {
            long r = f.regId();
            while (padre.containsKey(r)) {
                r = padre.get(r);
            }
            grupos.computeIfAbsent(r, k -> new ArrayList<>()).add(f);
        }
        List<Cadena> out = new ArrayList<>();
        for (Map.Entry<Long, List<Fila>> e : grupos.entrySet()) {
            List<Fila> es = new ArrayList<>(e.getValue());
            long raiz = e.getKey();
            // la raiz va primero; el resto por inicio y luego por orden de captura
            es.sort(Comparator.<Fila, Boolean>comparing(f -> f.regId() != raiz)
                    .thenComparing(Fila::inicio, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparingLong(Fila::regId));
            out.add(new Cadena(List.copyOf(es), diagnostico(es, tagsNss)));
        }
        out.sort(Comparator.comparing((Cadena c) -> c.ultimo().regId()).reversed());
        return out;
    }

    private static Long anteriorDeclarado(Fila f, Map<String, String> tagsNss, Map<Long, Fila> porId) {
        String v = f.rel() == null ? null : tagsNss.get(PREFIJO + f.rel().trim() + ".ANTERIOR");
        if (v == null || !v.trim().matches("\\d{1,18}")) {
            return null;
        }
        long id = Long.parseLong(v.trim());
        return id < f.regId() && porId.containsKey(id) ? id : null;
    }

    private static Long anteriorInferido(Fila f, List<Fila> filas) {
        if (f.inicio() != null) {
            Optional<Fila> pegada = filas.stream()
                    .filter(g -> g.regId() < f.regId() && g.termino() != null && g.termino().plusDays(1).equals(f.inicio()))
                    .max(Comparator.comparingLong(Fila::regId));
            if (pegada.isPresent()) {
                return pegada.get().regId();
            }
        }
        if (f.primera() != null) {
            return filas.stream()
                    .filter(g -> g.regId() < f.regId()
                            && (f.primera().equals(g.inicio()) || f.primera().equals(g.primera())))
                    .max(Comparator.comparingLong(Fila::regId))
                    .map(Fila::regId).orElse(null);
        }
        return null;
    }

    /** El de la subsecuente mas reciente que lo traiga (se puede precisar al continuar); si no, el de la inicial. */
    private static String diagnostico(List<Fila> es, Map<String, String> tagsNss) {
        for (int i = es.size() - 1; i >= 0; i--) {
            Fila f = es.get(i);
            String d = f.rel() == null ? null : tagsNss.get(PREFIJO + f.rel().trim() + ".DIAGNOSTICO");
            if (d != null && !d.isBlank()) {
                return d.trim();
            }
        }
        return null;
    }

    private static String usuarioActual() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a == null || a.getName() == null ? "SISTEMA" : a.getName();
    }
}
