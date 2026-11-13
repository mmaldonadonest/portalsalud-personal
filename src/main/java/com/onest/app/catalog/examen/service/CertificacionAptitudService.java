package com.onest.app.catalog.examen.service;

import com.onest.app.catalog.pretest.repository.MedTagRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * FT-SO-31 Dictamen de certificacion de aptitud medica laboral (rev. 03, 20-oct-2026, formato B
 * "opcional": solo si el cliente lo pide). Enfermeria marca la aptitud de cada tipo de trabajo; la
 * vigencia es FIJA de 1 ano desde la expedicion (decision de SM).
 *
 * <p>Historial (respuesta de SM 20-oct-2026: "que se vayan archivando las aptitudes al menos por 5
 * anos"): cada expedicion se guarda aparte en SERV_MED_TAG como "FTSO31.&lt;ID&gt;.&lt;CAMPO&gt;", con
 * ID = fecha y hora de la expedicion (yyyyMMddHHmmss). Nada se sobrescribe ni se borra; el portal no
 * purga SERV_MED_TAG, asi que se conservan mas de 5 anos.
 */
@Service
public class CertificacionAptitudService {

    static final String GRUPO = "CERTIFICACION_FTSO31";
    static final String PREFIJO = "FTSO31.";
    public static final Set<String> APTITUDES = Set.of("APTO", "NO_APTO", "CONDICIONADO", "NO_APLICA");
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter ID = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** Un dictamen expedido (para la lista de anteriores). */
    public record Expedicion(String id, String expedicion, String vigencia, String centro) {
    }

    /** Los 10 tipos de trabajo del formato, en su orden. */
    public static final List<String> TRABAJOS = List.of(
            "TRABAJOS GENERALES O ADMINISTRATIVOS",
            "TRABAJOS DE MANUFACTURA O MAQUILA",
            "MANEJO DE VEHICULOS A MOTOR",
            "ESPACIOS CONFINADOS DE CORTE, SOLDADURA Y ELECTRICOS",
            "TRABAJOS EN ALTURAS",
            "MANEJO DE ENZIMAS",
            "VIGILANCIA INTRAMUROS O PUESTOS CLAVE (CONFIANZA)",
            "BRIGADISTAS",
            "MANEJO MANUAL DE CARGAS",
            "USO DE RESPIRADOR");

    /**
     * EMNI (13-nov-2026): id del dictamen expedido al finalizar el ultimo examen inicial con «¿Desea emitir
     * dictamen?» = Sí; vacio si se contesto N/A. Con el, el impreso del examen inicial agrega el FT-SO-31.
     * Sin punto despues de FTSO31 para que todas() no lo confunda con una expedicion.
     */
    public static final String TAG_EMNI = "FTSO31_EMNI";

    private final MedTagRepository tags;
    private final com.onest.app.catalog.predio.service.PredioService predios;

    public CertificacionAptitudService(MedTagRepository tags, com.onest.app.catalog.predio.service.PredioService predios) {
        this.tags = tags;
        this.predios = predios;
    }

    /**
     * Centro de trabajo predeterminado = "CEDIS / cuenta" (respuesta de SM 20-oct-2026, ej. "Charcon / Diageo"):
     * el predio asignado a la cuenta en /admin/predios + la cuenta; solo la cuenta si no tiene predio.
     */
    public String centroDeTrabajo(String cuenta) {
        String cta = cuenta == null ? "" : cuenta.trim();
        try {
            return predios.predioFinoPorCuenta(cta).map(p -> p.predioNombre() + " / " + cta).orElse(cta);
        } catch (RuntimeException ex) {
            return cta;
        }
    }

    /** Id del dictamen ligado al ultimo EMNI, o vacio. */
    public String idDelEmni(String nss) {
        try {
            String id = tags.latestByNssAndTypePrefix(nss, TAG_EMNI).get(TAG_EMNI);
            return id == null ? "" : id.trim();
        } catch (RuntimeException ex) {
            return "";
        }
    }

    /** Liga (id) o desliga ("") el dictamen del ultimo EMNI. */
    public void ligarEmni(String nss, String id) {
        tags.upsert(nss, TAG_EMNI, id == null ? "" : id, GRUPO, usuarioActual());
    }

    /** Todas las expediciones del NSS: id -> campos, de la mas reciente a la mas vieja. */
    private TreeMap<String, Map<String, String>> todas(String nss) {
        TreeMap<String, Map<String, String>> r = new TreeMap<>(Comparator.reverseOrder());
        tags.latestByNssAndTypePrefix(nss, PREFIJO).forEach((k, v) -> {
            String resto = k.substring(PREFIJO.length());
            int punto = resto.indexOf('.');
            if (punto > 0 && resto.substring(0, punto).matches("\\d{14}")) {
                r.computeIfAbsent(resto.substring(0, punto), x -> new LinkedHashMap<>()).put(resto.substring(punto + 1), v == null ? "" : v);
            }
        });
        return r;
    }

    /** Dictamenes expedidos del NSS, del mas reciente al mas viejo. */
    public List<Expedicion> expediciones(String nss) {
        List<Expedicion> r = new ArrayList<>();
        todas(nss).forEach((id, c) -> r.add(new Expedicion(id, c.getOrDefault("EXPEDICION", ""), c.getOrDefault("VIGENCIA", ""),
                c.getOrDefault("CENTRO", ""))));
        return r;
    }

    /** Un dictamen por id, o el ultimo si id es null; vacio si no hay. La clave "ID" lleva su id. */
    public Map<String, String> leer(String nss, String id) {
        TreeMap<String, Map<String, String>> t = todas(nss);
        Optional<Map.Entry<String, Map<String, String>>> e = id == null || id.isBlank()
                ? Optional.ofNullable(t.firstEntry())
                : Optional.ofNullable(t.get(id.trim())).map(m -> Map.entry(id.trim(), m));
        if (e.isEmpty()) {
            return new LinkedHashMap<>();
        }
        Map<String, String> r = new LinkedHashMap<>(e.get().getValue());
        r.put("ID", e.get().getKey());
        return r;
    }

    /**
     * Expide un dictamen nuevo hoy (no toca los anteriores). Todos los trabajos deben tener aptitud y el
     * condicionado lleva que especificar. Devuelve el id; IllegalArgumentException con el motivo si falta algo.
     */
    public String expedir(String nss, Map<String, String> aptitud, Map<String, String> especificar, String centro) {
        validar(aptitud, especificar);
        String usuario = usuarioActual();
        LocalDateTime ahora = LocalDateTime.now();
        LocalDate hoy = ahora.toLocalDate();
        String id = ahora.format(ID);
        Map<String, String> campos = new LinkedHashMap<>();
        for (int i = 0; i < TRABAJOS.size(); i++) {
            String a = limpio(aptitud.get(String.valueOf(i)));
            campos.put("APTITUD_" + i, a);
            String esp = "CONDICIONADO".equals(a) ? limpio(especificar.get(String.valueOf(i))) : "";
            if (!esp.isEmpty()) {
                campos.put("ESPECIFICAR_" + i, esp);
            }
        }
        campos.put("CENTRO", limpio(centro));
        campos.put("EXPEDICION", hoy.format(DIA));
        campos.put("VIGENCIA", hoy.plusYears(1).format(DIA));
        campos.forEach((campo, valor) -> tags.upsert(nss, PREFIJO + id + "." + campo, valor, GRUPO, usuario));
        return id;
    }

    /**
     * Todos los trabajos con aptitud y el condicionado con su especificacion; IllegalArgumentException con el
     * motivo si falta algo. Publico para el EMNI (13-nov-2026): valida ANTES de mandar el examen al WS, asi
     * un dictamen incompleto no deja el examen guardado y el dictamen sin expedir.
     */
    public void validar(Map<String, String> aptitud, Map<String, String> especificar) {
        for (int i = 0; i < TRABAJOS.size(); i++) {
            String a = limpio(aptitud.get(String.valueOf(i)));
            if (!APTITUDES.contains(a)) {
                throw new IllegalArgumentException("Falta marcar la aptitud de «" + TRABAJOS.get(i) + "».");
            }
            if ("CONDICIONADO".equals(a) && limpio(especificar.get(String.valueOf(i))).isEmpty()) {
                throw new IllegalArgumentException("Especifica la condición de «" + TRABAJOS.get(i) + "».");
            }
        }
    }

    private static String limpio(String s) {
        return s == null ? "" : s.trim();
    }

    private static String usuarioActual() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a == null || a.getName() == null ? "SISTEMA" : a.getName();
    }
}
