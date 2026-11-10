package com.onest.app.catalog.expediente.service;

import com.onest.app.catalog.expediente.web.ConsultaAltaForm;
import com.onest.app.catalog.pretest.repository.MedTagRepository;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * FT-SO-38 Nota de fisioterapia (rev. 00, 14-oct-2026; C-065). SM, 9-nov-2026: es una opcion mas de la
 * consulta medica, independiente del pase de salida y del de medicamento -> tipo de consulta
 * "fisioterapia", como "pruebas_especiales" (FT-SO-23).
 *
 * <p>Se guarda en SERV_MED_TAG (base del portal, sin SQL), un tag por campo ligado a la consulta:
 * "FTSO38.&lt;idArchivoRel&gt;.&lt;CAMPO&gt;". Un tag por campo (no un JSON) para poder sacar metricas
 * con SQL; por eso las fechas van en ISO (AAAA-MM-DD) y los numeros sin unidades.
 *
 * <p>Lo que la consulta ya captura no se pide dos veces; se copia a la nota al guardar para que el
 * impreso y las metricas no dependan del WS: motivo, signos vitales + exploracion (1a toma) y
 * tratamiento (plan o terapeutica empleada).
 */
@Service
public class FisioterapiaService {

    public static final String FORMATO = "FTSO38";
    public static final String TIPO = "fisioterapia";
    static final String GRUPO = "NOTA_FISIOTERAPIA_FTSO38";
    /** El formato trae 3 tomas: la 1a sale de la consulta; la 2a y 3a se capturan en la seccion. */
    public static final int TOMAS = 3;
    private static final ZoneId MEXICO = ZoneId.of("America/Mexico_City");

    /** Una toma de la exploracion fisica (renglon del formato + su nota). */
    public record Toma(int numero, String fecha, String peso, String talla, String imc, String fc, String fr, String ta,
                       String nota) {
        public boolean vacia() {
            return (fecha + peso + talla + imc + fc + fr + ta + nota).isBlank();
        }
    }

    private final MedTagRepository tags;

    public FisioterapiaService(MedTagRepository tags) {
        this.tags = tags;
    }

    public static boolean esFisio(String tipoConsulta) {
        return tipoConsulta != null && tipoConsulta.trim().toLowerCase(Locale.ROOT).equals(TIPO);
    }

    /** Obligatorio (antes del WS): el diagnostico fisioterapeutico. null si esta bien. */
    public static String errorObligatorios(ConsultaAltaForm form) {
        if (esFisio(form.getTipoConsulta()) && limpio(form.getFisioDxFisio()).isEmpty()) {
            return "En la nota de fisioterapia anota el diagnóstico fisioterapéutico.";
        }
        return null;
    }

    /** Guarda la nota de la consulta recien dada de alta (solo con tipo fisioterapia). */
    public void guardar(ConsultaAltaForm form) {
        String rel = limpio(form.getIdArchivoRel());
        if (!esFisio(form.getTipoConsulta()) || !rel.matches("[A-Za-z0-9]{8,64}")) {
            return;
        }
        Map<String, String> c = new LinkedHashMap<>();
        c.put("DX_MEDICO", form.getFisioDxMedico());
        c.put("EVOLUCION", form.getFisioEvolucion());
        c.put("TRAT_PREVIO", form.getFisioTratPrevio());
        String tipo = limpio(form.getFisioIncapTipo()).toUpperCase(Locale.ROOT);
        if (tipo.equals("IMSS") || tipo.equals("INTERNA")) {
            c.put("INCAP_TIPO", tipo);
            String dias = entero(form.getFisioIncapDias());
            c.put("INCAP_DIAS", dias);
            c.put("INCAP_INICIO", fechaIso(form.getFisioIncapInicio()));
            // anotacion de SM: termino = inicio + dias - 1 (05/11 con 7 dias = 11/11); se recalcula aqui
            c.put("INCAP_TERMINO", termino(form.getFisioIncapInicio(), dias));
        }
        c.put("MOTIVO", form.getMotivo());
        // 1a toma = signos vitales y exploracion de la consulta, con la fecha de hoy
        c.putAll(toma(1, LocalDate.now(MEXICO).toString(), form.getPeso(), form.getTalla(), form.getImc(), form.getFc(),
                form.getFr(), form.getTa(), form.getExploracion()));
        for (int i = 0; i < TOMAS - 1; i++) {
            c.putAll(toma(i + 2, fechaIso(en(form.getFisioTomaFecha(), i)), en(form.getFisioTomaPeso(), i),
                    en(form.getFisioTomaTalla(), i), en(form.getFisioTomaImc(), i), en(form.getFisioTomaFc(), i),
                    en(form.getFisioTomaFr(), i), en(form.getFisioTomaTa(), i), en(form.getFisioTomaNota(), i)));
        }
        c.put("DX_FISIO", form.getFisioDxFisio());
        c.put("PLAN", form.getTratamiento());

        String nss = limpio(form.getNss());
        String usuario = usuarioActual();
        c.forEach((campo, valor) -> {
            String v = limpio(valor);
            if (!v.isEmpty()) {
                tags.upsert(nss, FORMATO + "." + rel + "." + campo, v, GRUPO, usuario);
            }
        });
    }

    /** Campos de la nota (sin el prefijo); vacio si la consulta no tiene nota de fisioterapia. */
    public Map<String, String> leer(String nss, String rel) {
        Map<String, String> r = new LinkedHashMap<>();
        if (rel == null || rel.isBlank()) {
            return r;
        }
        String prefijo = FORMATO + "." + rel.trim() + ".";
        tags.latestByNssAndTypePrefix(nss, prefijo).forEach((k, v) -> r.put(k.substring(prefijo.length()), v == null ? "" : v));
        return r;
    }

    /** Las 3 tomas en orden (vacias incluidas: el impreso deja su renglon para llenarse a mano). */
    public static List<Toma> tomas(Map<String, String> campos) {
        List<Toma> r = new ArrayList<>();
        for (int n = 1; n <= TOMAS; n++) {
            String p = "T" + n + "_";
            r.add(new Toma(n, campos.getOrDefault(p + "FECHA", ""), campos.getOrDefault(p + "PESO", ""),
                    campos.getOrDefault(p + "TALLA", ""), campos.getOrDefault(p + "IMC", ""), campos.getOrDefault(p + "FC", ""),
                    campos.getOrDefault(p + "FR", ""), campos.getOrDefault(p + "TA", ""), campos.getOrDefault(p + "NOTA", "")));
        }
        return r;
    }

    private static Map<String, String> toma(int n, String fecha, String peso, String talla, String imc, String fc, String fr,
                                            String ta, String nota) {
        String p = "T" + n + "_";
        Map<String, String> m = new LinkedHashMap<>();
        String pe = numero(peso);
        String tl = numero(talla);
        String im = numero(imc);
        // IMC como en el examen: se calcula si hay peso y talla (anotacion de SM)
        if (!pe.isEmpty() && !tl.isEmpty()) {
            im = imc(pe, tl);
        }
        boolean hayDatos = !(pe + tl + im + numero(fc) + numero(fr) + limpio(ta) + limpio(nota)).isEmpty();
        // una toma sin ningun dato no deja fecha suelta (la 1a siempre lleva la de hoy si hay signos)
        m.put(p + "FECHA", hayDatos ? fecha : "");
        m.put(p + "PESO", pe);
        m.put(p + "TALLA", tl);
        m.put(p + "IMC", im);
        m.put(p + "FC", numero(fc));
        m.put(p + "FR", numero(fr));
        m.put(p + "TA", limpio(ta));
        m.put(p + "NOTA", limpio(nota));
        return m;
    }

    static String imc(String peso, String talla) {
        try {
            double kg = Double.parseDouble(peso);
            double m = Double.parseDouble(talla);
            if (kg <= 0 || m <= 0) {
                return "";
            }
            return String.format(Locale.ROOT, "%.2f", kg / (m * m));
        } catch (NumberFormatException ex) {
            return "";
        }
    }

    /** inicio + dias - 1, en ISO; vacio si falta alguno o no es valido. */
    static String termino(String inicio, String dias) {
        String ini = fechaIso(inicio);
        if (ini.isEmpty() || dias.isEmpty()) {
            return "";
        }
        int d = Integer.parseInt(dias);
        return d < 1 ? "" : LocalDate.parse(ini).plusDays(d - 1L).toString();
    }

    /** Solo fechas AAAA-MM-DD validas (input date); lo demas se descarta. */
    static String fechaIso(String v) {
        String t = limpio(v);
        try {
            return t.isEmpty() ? "" : LocalDate.parse(t).toString();
        } catch (DateTimeException ex) {
            return "";
        }
    }

    /** Numero sin unidades ("72", "1.70"); coma decimal -> punto; lo que no es numero se descarta. */
    static String numero(String v) {
        String t = limpio(v).replace(',', '.');
        return t.matches("\\d{1,4}(\\.\\d{1,3})?") ? t : "";
    }

    static String entero(String v) {
        String t = limpio(v);
        return t.matches("\\d{1,4}") ? String.valueOf(Integer.parseInt(t)) : "";
    }

    private static String en(List<String> lista, int i) {
        return lista != null && i < lista.size() ? limpio(lista.get(i)) : "";
    }

    private static String limpio(String s) {
        return s == null ? "" : s.trim();
    }

    private static String usuarioActual() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a == null || a.getName() == null ? "SISTEMA" : a.getName();
    }
}
