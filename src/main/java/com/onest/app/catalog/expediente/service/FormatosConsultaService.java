package com.onest.app.catalog.expediente.service;

import com.onest.app.catalog.expediente.web.ConsultaAltaForm;
import com.onest.app.catalog.pretest.repository.MedTagRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Formatos opcionales que salen de una consulta medica (20-oct-2026), con lo que el WS de consulta no
 * guarda en SERV_MED_TAG (base del portal), ligado al idArchivoRel de la consulta como el pase de salida:
 * <ul>
 *   <li>FT-SO-29 Pase de medicamento (casilla en cualquier consulta): "FTSO29.&lt;rel&gt;.&lt;CAMPO&gt;".
 *       Varios medicamentos por pase (respuesta de SM 20-oct): MED_&lt;i&gt;_MEDICAMENTO, _GRAMAJE,
 *       _DURACION, _FRECUENCIA; VIGENCIA = fecha autorizada para ingresar el medicamento o "PERMANENTE".</li>
 *   <li>FT-SO-23 Pruebas especiales (tipo de consulta "pruebas_especiales", respuesta de SM 20-oct):
 *       "FTSO23.&lt;rel&gt;.&lt;CAMPO&gt;". El resultado se guarda y se imprime: SM confirmo que
 *       "sospechoso" no es "positivo" y no altera la confidencialidad.</li>
 * </ul>
 */
@Service
public class FormatosConsultaService {

    public static final String MEDICAMENTO = "FTSO29";
    public static final String PRUEBAS = "FTSO23";
    public static final String TIPO_PRUEBAS = "pruebas_especiales";
    public static final String PERMANENTE = "PERMANENTE";
    static final String GRUPO_MEDICAMENTO = "PASE_MEDICAMENTO_FTSO29";
    static final String GRUPO_PRUEBAS = "PRUEBAS_ESPECIALES_FTSO23";

    /** Un renglon del pase de medicamento. */
    public record Medicamento(String medicamento, String gramaje, String duracion, String frecuencia) {
    }

    private final MedTagRepository tags;

    public FormatosConsultaService(MedTagRepository tags) {
        this.tags = tags;
    }

    /** true si la consulta es de pruebas especiales (el valor del select o variantes en mayusculas). */
    public static boolean esPruebas(String tipoConsulta) {
        return tipoConsulta != null && tipoConsulta.trim().toLowerCase().replace(' ', '_').equals(TIPO_PRUEBAS);
    }

    /** Obligatorios de cada formato (antes del WS). null si esta bien; si no, el mensaje. */
    public static String errorObligatorios(ConsultaAltaForm form) {
        if ("SI".equals(form.getMedGenerar()) && medicamentos(form).isEmpty()) {
            return "En el pase de medicamento anota al menos un medicamento.";
        }
        if (esPruebas(form.getTipoConsulta()) && limpio(form.getPruebaAnticuerpos()).isEmpty()) {
            return "En pruebas especiales indica la prueba (anticuerpos contra...).";
        }
        return null;
    }

    /** Guarda los formatos de la consulta recien dada de alta. */
    public void guardar(ConsultaAltaForm form) {
        String rel = limpio(form.getIdArchivoRel());
        if (!rel.matches("[A-Za-z0-9]{8,64}")) {
            return;
        }
        String nss = limpio(form.getNss());
        if ("SI".equals(form.getMedGenerar())) {
            Map<String, String> c = new LinkedHashMap<>();
            List<Medicamento> meds = medicamentos(form);
            for (int i = 0; i < meds.size(); i++) {
                Medicamento m = meds.get(i);
                c.put("MED_" + i + "_MEDICAMENTO", m.medicamento());
                c.put("MED_" + i + "_GRAMAJE", m.gramaje());
                c.put("MED_" + i + "_DURACION", m.duracion());
                c.put("MED_" + i + "_FRECUENCIA", m.frecuencia());
            }
            c.put("MEDICAMENTOS", String.valueOf(meds.size()));
            c.put("VIGENCIA", "SI".equals(form.getMedPermanente()) ? PERMANENTE : form.getMedVigencia());
            c.put("OBSERVACIONES", form.getMedObservaciones());
            escribir(nss, MEDICAMENTO, rel, c, GRUPO_MEDICAMENTO);
        }
        if (esPruebas(form.getTipoConsulta())) {
            Map<String, String> c = new LinkedHashMap<>();
            c.put("ANTICUERPOS", form.getPruebaAnticuerpos());
            c.put("AUTORIZA", form.getPruebaAutoriza());
            c.put("FECHA", form.getPruebaFecha());
            c.put("LUGAR", form.getPruebaLugar());
            c.put("RESULTADO", form.getPruebaResultado());
            escribir(nss, PRUEBAS, rel, c, GRUPO_PRUEBAS);
        }
    }

    /** Campos de un formato de la consulta (sin el prefijo); vacio si no se genero. */
    public Map<String, String> leer(String formato, String nss, String rel) {
        Map<String, String> r = new LinkedHashMap<>();
        if (rel == null || rel.isBlank()) {
            return r;
        }
        String prefijo = formato + "." + rel.trim() + ".";
        tags.latestByNssAndTypePrefix(nss, prefijo).forEach((k, v) -> r.put(k.substring(prefijo.length()), v == null ? "" : v));
        return r;
    }

    /** Renglones de medicamentos guardados de un pase, en orden. */
    public static List<Medicamento> medicamentosGuardados(Map<String, String> campos) {
        List<Medicamento> r = new ArrayList<>();
        int n;
        try {
            n = Integer.parseInt(campos.getOrDefault("MEDICAMENTOS", "0").trim());
        } catch (NumberFormatException ex) {
            n = 0;
        }
        for (int i = 0; i < n; i++) {
            r.add(new Medicamento(campos.getOrDefault("MED_" + i + "_MEDICAMENTO", ""), campos.getOrDefault("MED_" + i + "_GRAMAJE", ""),
                    campos.getOrDefault("MED_" + i + "_DURACION", ""), campos.getOrDefault("MED_" + i + "_FRECUENCIA", "")));
        }
        return r;
    }

    /** Renglones capturados con medicamento (los vacios se descartan). */
    private static List<Medicamento> medicamentos(ConsultaAltaForm form) {
        List<Medicamento> r = new ArrayList<>();
        List<String> meds = form.getMedMedicamento() == null ? List.of() : form.getMedMedicamento();
        for (int i = 0; i < meds.size(); i++) {
            String med = limpio(meds.get(i));
            if (!med.isEmpty()) {
                r.add(new Medicamento(med, en(form.getMedGramaje(), i), en(form.getMedDuracion(), i), en(form.getMedFrecuencia(), i)));
            }
        }
        return r;
    }

    private static String en(List<String> lista, int i) {
        return lista != null && i < lista.size() ? limpio(lista.get(i)) : "";
    }

    private void escribir(String nss, String formato, String rel, Map<String, String> campos, String grupo) {
        String usuario = usuarioActual();
        campos.forEach((campo, valor) -> {
            String v = limpio(valor);
            if (!v.isEmpty()) {
                tags.upsert(nss, formato + "." + rel + "." + campo, v, grupo, usuario);
            }
        });
    }

    private static String limpio(String s) {
        return s == null ? "" : s.trim();
    }

    private static String usuarioActual() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a == null || a.getName() == null ? "SISTEMA" : a.getName();
    }
}
