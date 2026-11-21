package com.onest.app.catalog.file.service;

import com.onest.app.catalog.nss.client.NssSearchClient;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Nombre de los archivos con la nomenclatura de Servicio Medico (C-008, C-057..C-059, C-087; respuestas del
 * 17 y 18-nov-2026): PREFIJO-MES-AAAA-INICIALES, p. ej. EST-ENE-2026-GAMALA.
 * <ul>
 *   <li>MES en tres letras con el estilo de SM (P-009): ENE FEB MZO ABR MYO JUN JUL AGO SEP OCT NOV DIC.</li>
 *   <li>AAAA con 4 digitos (P-009, aclaracion del usuario 18-nov).</li>
 *   <li>Mes y año salen de la FECHA DEL DOCUMENTO (decision del usuario 18-nov), no de la de subida.</li>
 *   <li>INICIALES del trabajador: todos sus apellidos y luego todos sus nombres (P-043: GARCIA ARMAS MARCO
 *       ANTONIO LUIS ALFREDO = GAMALA; GARCIA MARCO ANTONIO = GMA). Se omiten DE, DEL, LA, LAS, LOS e Y,
 *       como en el RFC y la CURP (criterio del portal; SM no lo dijo).</li>
 * </ul>
 * Si dos archivos quedarian con el mismo nombre se agrega -2, -3... para que no se confundan.
 */
@Service
public class NomenclaturaService {

    private static final Logger log = LoggerFactory.getLogger(NomenclaturaService.class);

    public static final List<String> MESES = List.of("ENE", "FEB", "MZO", "ABR", "MYO", "JUN", "JUL", "AGO", "SEP",
            "OCT", "NOV", "DIC");
    private static final Set<String> PARTICULAS = Set.of("DE", "DEL", "LA", "LAS", "LOS", "Y");

    /**
     * Nombre que deben llevar los adjuntos de un registro que se sube DESPUES de guardarlo (la incapacidad se
     * guarda y luego el navegador sube su PDF con el mismo idArchivoRel): SERV_MED_TAG "NOMBRE_ARCHIVO.&lt;rel&gt;".
     */
    static final String TAG_NOMBRE = "NOMBRE_ARCHIVO.";
    private static final String GRUPO = "NOMENCLATURA";

    private final NssSearchClient nssSearchClient;
    private final com.onest.app.catalog.pretest.repository.MedTagRepository tags;

    public NomenclaturaService(NssSearchClient nssSearchClient,
                               com.onest.app.catalog.pretest.repository.MedTagRepository tags) {
        this.nssSearchClient = nssSearchClient;
        this.tags = tags;
    }

    /** Deja apartado el nombre (sin extension) de los adjuntos del registro {@code rel}. */
    public void apartarNombre(String nss, String rel, String base) {
        if (rel == null || !rel.trim().matches("[A-Za-z0-9]{8,64}") || base == null || base.isBlank()) {
            return;
        }
        tags.upsert(nss.trim(), TAG_NOMBRE + rel.trim(), base, GRUPO, "SISTEMA");
    }

    /** Nombre apartado para el registro {@code rel}, si lo hay. */
    public java.util.Optional<String> nombreApartado(String nss, String rel) {
        if (nss == null || rel == null || !rel.trim().matches("[A-Za-z0-9]{8,64}")) {
            return java.util.Optional.empty();
        }
        String v = tags.latestByNssAndTypePrefix(nss.trim(), TAG_NOMBRE + rel.trim()).get(TAG_NOMBRE + rel.trim());
        return v == null || v.isBlank() ? java.util.Optional.empty() : java.util.Optional.of(v.trim());
    }

    /** ENE-2026 */
    public static String mesAnio(LocalDate fecha) {
        return MESES.get(fecha.getMonthValue() - 1) + "-" + fecha.getYear();
    }

    /** Iniciales de apellidos y luego de nombres; vacio si no hay nombre. */
    public static String iniciales(String apellidoPaterno, String apellidoMaterno, String nombres) {
        StringBuilder sb = new StringBuilder();
        for (String parte : new String[]{apellidoPaterno, apellidoMaterno, nombres}) {
            for (String palabra : limpio(parte).split(" ")) {
                if (!palabra.isEmpty() && !PARTICULAS.contains(palabra)) {
                    sb.append(palabra.charAt(0));
                }
            }
        }
        return sb.toString();
    }

    /**
     * Iniciales del NSS segun el biometrico. Si no se encuentra (candidato sin registro o el WS no responde)
     * se usa el NSS: el archivo no se queda sin nombre y se sabe de quien es.
     */
    public String inicialesDe(String nss) {
        try {
            String r = nssSearchClient.findUsuario(nss, "NOMENCLATURA")
                    .map(e -> iniciales(e.apellidoPaterno(), e.apellidoMaterno(), e.nombre()))
                    .orElse("");
            if (!r.isEmpty()) {
                return r;
            }
        } catch (RuntimeException ex) {
            log.warn("[nomenclatura] sin nombre para el NSS {}: {}", nss, ex.getMessage());
        }
        return nss == null ? "" : nss.trim();
    }

    /** PREFIJO-MES-AAAA-INICIALES (sin extension). */
    public String base(String prefijo, LocalDate fecha, String nss) {
        return prefijo + "-" + mesAnio(fecha) + "-" + inicialesDe(nss);
    }

    /**
     * Nombres finales para {@code cuantos} archivos: base.ext, base-2.ext... saltando los que ya existen
     * (comparacion sin mayusculas).
     */
    public static List<String> nombres(String base, String extension, int cuantos, Collection<String> existentes) {
        return nombres(base, java.util.Collections.nCopies(cuantos, extension), existentes);
    }

    /** Uno por extension (cada archivo conserva la suya), saltando los nombres que ya existen. */
    public static List<String> nombres(String base, List<String> extensiones, Collection<String> existentes) {
        Set<String> usados = new java.util.HashSet<>();
        for (String e : existentes) {
            if (e != null) {
                usados.add(e.toUpperCase(Locale.ROOT));
            }
        }
        List<String> r = new ArrayList<>();
        int n = 1;
        for (String ext : extensiones) {
            String nombre;
            do {
                nombre = base + (n == 1 ? "" : "-" + n) + "." + ext;
                n++;
            } while (!usados.add(nombre.toUpperCase(Locale.ROOT)));
            r.add(nombre);
        }
        return r;
    }

    private static String limpio(String s) {
        if (s == null) {
            return "";
        }
        String t = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return t.toUpperCase(Locale.ROOT).replaceAll("[^A-Z ]", " ").replaceAll("\\s+", " ").trim();
    }
}
