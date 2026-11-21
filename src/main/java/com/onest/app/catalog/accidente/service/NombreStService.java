package com.onest.app.catalog.accidente.service;

import com.onest.app.catalog.file.service.NomenclaturaService;
import com.onest.app.catalog.nss.client.NssSearchClient;
import com.onest.app.catalog.predio.service.PredioService;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * C-087 (P-032, hoja amarilla «CODIGO RIESGO» de SM, respuestas del 18-nov-2026): nombre con que se guardan
 * los ST de un accidente.
 * <ul>
 *   <li>ST7 / ST2: INICIALES-MESAAAA-RIESGO-ESTATUS-PREDIO, p. ej. GAMA-SEP2025-RT1-C-AIF. La ST2 va con la
 *       ST7 (mismo codigo). Si la ST2 se da de alta en otro año se agrega ese año: GAMA-SEP2025-2026-RT1-C-AIF
 *       (P-045).</li>
 *   <li>ST 9 / 8 / 4 / 3: INICIALES-MESAAAA-ST9, sin riesgo, estatus ni predio (P-046).</li>
 * </ul>
 * MES y AÑO salen de la fecha de inicio de labores del caso (P-044, C-092); el mes en tres letras y el año con
 * 4 digitos como todos los nombres (P-009: el ejemplo de SM trae SEP25, pero la regla de SM es para todos).
 * RIESGO: RT1 trabajo, RT2 trayecto, EP enfermedad profesional (P-047). ESTATUS: C concluido, J juridico
 * (abandona el proceso y es baja).
 */
@Service
public class NombreStService {

    private static final Logger log = LoggerFactory.getLogger(NombreStService.class);

    /** Documento que se sube: el ST7/ST2 lleva el codigo de riesgo completo; los demas solo su numero. */
    public static final String ST7_ST2 = "ST7_ST2";
    public static final java.util.List<String> ST_NUMERO = java.util.List.of("ST9", "ST8", "ST4", "ST3");

    /** Abreviaturas de la hoja de SM, en el orden en que se muestran (predio del portal -> abreviatura). */
    public static final Map<String, String> ABREVIATURAS;

    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("AIFA", "AIF");
        m.put("ATIZAPAN", "ATZ");
        m.put("CHARCON", "CHA");
        m.put("FLORA", "FLR");
        m.put("MACRO I", "M1");
        m.put("MACRO II", "M2");
        m.put("MERCURIO", "MRCR");
        m.put("MIKELS", "MKLS");
        m.put("SIGLO XXI", "SGL");
        m.put("SMO", "SMO");
        m.put("TOLUCA", "TOL");
        m.put("TULTIPARK", "TP3");
        m.put("U TEPALCAPA", "UT");
        m.put("WORLD PARK", "WP");
        m.put("Z VALLEJO", "ZV");
        m.put("FORANEO", "FOR");
        ABREVIATURAS = java.util.Collections.unmodifiableMap(m);
    }

    // Nombres con que el predio puede venir del mapeo cuenta->predio o de la hoja de SM
    private static final Map<String, String> SINONIMOS = Map.of(
            "MACRO 1", "MACRO I",
            "MACRO 2", "MACRO II",
            "UNILEVER TEPALCAPA", "U TEPALCAPA",
            "ZARA VALLEJO", "Z VALLEJO",
            "SIGLO", "SIGLO XXI",
            "TULTITLAN", "TULTIPARK");

    private final NomenclaturaService nomenclatura;
    private final NssSearchClient nssSearchClient;
    private final PredioService predioService;

    public NombreStService(NomenclaturaService nomenclatura, NssSearchClient nssSearchClient,
                           PredioService predioService) {
        this.nomenclatura = nomenclatura;
        this.nssSearchClient = nssSearchClient;
        this.predioService = predioService;
    }

    /** RT1 / RT2 / EP a partir del tipo de riesgo del caso; vacio si no se reconoce. */
    public static String riesgo(String tipoRiesgo) {
        String t = limpio(tipoRiesgo);
        if (t.contains("TRAYECTO") || t.equals("RT2")) {
            return "RT2";
        }
        if (t.contains("LABORAL") || t.contains("TRABAJO") || t.equals("RT1")) {
            return "RT1";
        }
        if (t.equals("EP") || t.contains("ENFERMEDAD")) {
            return "EP";
        }
        return "";
    }

    /** Estatus sugerido: J si el caso es baja / inconcluso (lo lleva juridico), C en otro caso. */
    public static String estatusSugerido(String statusCalificacion) {
        return InicioLaboresService.BAJA.equals(statusCalificacion) ? "J" : "C";
    }

    /** Abreviatura de un predio (nombre del portal o de la hoja de SM); vacio si no esta en la hoja. */
    public static String abreviatura(String predio) {
        String p = limpio(predio);
        p = SINONIMOS.getOrDefault(p, p);
        return ABREVIATURAS.getOrDefault(p, "");
    }

    /** Abreviatura del predio del trabajador segun su cuenta; vacio si la cuenta no tiene predio asignado. */
    public String predioSugerido(String nss) {
        try {
            return nssSearchClient.findUsuario(nss, "NOMENCLATURA")
                    .flatMap(e -> predioService.predioFinoPorCuenta(e.cuenta()))
                    .map(cp -> abreviatura(cp.predioNombre()))
                    .orElse("");
        } catch (RuntimeException ex) {
            log.warn("[st] sin predio sugerido para el NSS: {}", ex.getMessage());
            return "";
        }
    }

    /**
     * Nombre sin extension.
     *
     * @param documento  {@link #ST7_ST2} o uno de {@link #ST_NUMERO}
     * @param fechaAltaSt2 solo ST7/ST2: si la ST2 se dio de alta en otro año que el inicio de labores
     */
    public static String codigo(String iniciales, LocalDate inicioLabores, String documento, String riesgo,
                                String estatus, String predioAbrev, LocalDate fechaAltaSt2) {
        if (inicioLabores == null) {
            throw new IllegalArgumentException("Captura primero la fecha de inicio de labores del caso: de ella sale el mes y año del nombre.");
        }
        String mes = NomenclaturaService.MESES.get(inicioLabores.getMonthValue() - 1) + inicioLabores.getYear();
        String doc = documento == null ? "" : documento.trim().toUpperCase(Locale.ROOT);
        if (ST_NUMERO.contains(doc)) {
            return iniciales + "-" + mes + "-" + doc;
        }
        if (!ST7_ST2.equals(doc)) {
            throw new IllegalArgumentException("Tipo de documento no válido.");
        }
        if (riesgo == null || !java.util.Set.of("RT1", "RT2", "EP").contains(riesgo)) {
            throw new IllegalArgumentException("El caso no tiene tipo de riesgo (Laboral o Trayecto).");
        }
        String e = estatus == null ? "" : estatus.trim().toUpperCase(Locale.ROOT);
        if (!e.equals("C") && !e.equals("J")) {
            throw new IllegalArgumentException("Indica el estatus: C concluido o J jurídico.");
        }
        String p = predioAbrev == null ? "" : predioAbrev.trim().toUpperCase(Locale.ROOT);
        if (!ABREVIATURAS.containsValue(p)) {
            throw new IllegalArgumentException("Selecciona el predio.");
        }
        String anioAlta = "";
        if (fechaAltaSt2 != null) {
            if (fechaAltaSt2.isBefore(inicioLabores.withDayOfYear(1))) {
                throw new IllegalArgumentException("La alta de la ST2 no puede ser de un año anterior al inicio de labores.");
            }
            if (fechaAltaSt2.getYear() != inicioLabores.getYear()) {
                anioAlta = "-" + fechaAltaSt2.getYear();
            }
        }
        return iniciales + "-" + mes + anioAlta + "-" + riesgo + "-" + e + "-" + p;
    }

    /** Iniciales del trabajador (las mismas de toda la nomenclatura). */
    public String iniciales(String nss) {
        return nomenclatura.inicialesDe(nss);
    }

    static Optional<LocalDate> fechaOpcional(String iso) {
        if (iso == null || iso.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(iso.trim()));
        } catch (java.time.format.DateTimeParseException ex) {
            throw new IllegalArgumentException("La fecha de alta de la ST2 no es válida.");
        }
    }

    public static LocalDate fechaAlta(String iso) {
        return fechaOpcional(iso).orElse(null);
    }

    private static String limpio(String s) {
        if (s == null) {
            return "";
        }
        String t = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return t.toUpperCase(Locale.ROOT).replace('_', ' ').replaceAll("\\s+", " ").trim();
    }
}
