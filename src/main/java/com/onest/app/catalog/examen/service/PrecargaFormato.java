package com.onest.app.catalog.examen.service;

import com.onest.app.catalog.examen.dto.ExamItem;
import com.onest.app.catalog.incapacidad.dto.IncapacidadDto;
import com.onest.app.catalog.incapacidad.service.IncapacidadService;
import com.onest.app.catalog.nss.service.NssSearchService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Precarga de los bloques de los formatos EMLE con datos que el portal ya tiene (16-oct-2026):
 * <ul>
 *   <li>FT-SO-13 (tipo 4): dias, ramo, tipo y fechas de la ULTIMA incapacidad del NSS.</li>
 *   <li>FT-SO-17/18/20 (tipos 2, 6, 8): puesto actual de la ficha del empleado.</li>
 * </ul>
 * Solo llena campos vacios y solo en el tipo de examen que los pide: lo precargado viaja al
 * guardar, y fuera de su tipo crearia tags sin que el medico los haya visto.
 */
@Component
public class PrecargaFormato {

    private static final Logger log = LoggerFactory.getLogger(PrecargaFormato.class);
    private static final Set<String> TIPOS_PUESTO = Set.of("2", "6", "8");

    private final IncapacidadService incapacidades;
    private final NssSearchService nssSearch;
    private final DatosGeneralesExamenService datosGenerales;
    private final com.onest.app.catalog.examen.repository.ExamenHistoricoRepository historico;
    private final ExamenService examenService;

    public PrecargaFormato(IncapacidadService incapacidades, NssSearchService nssSearch,
                           DatosGeneralesExamenService datosGenerales,
                           com.onest.app.catalog.examen.repository.ExamenHistoricoRepository historico,
                           ExamenService examenService) {
        this.incapacidades = incapacidades;
        this.nssSearch = nssSearch;
        this.datosGenerales = datosGenerales;
        this.historico = historico;
        this.examenService = examenService;
    }

    // ============================================================================================
    // C-075 (11-nov-2026, Word SM punto 13 + notas de la reunion con SM): el EMNI «solo como complemento».
    // En Periodico (2), Cambio de puesto (3), Post incapacidad (4), Egreso (6) y Periodico mantenimiento (8),
    // lo que el examen deje VACIO se llena con el ultimo examen inicial del portal (foto del historico), solo
    // en lo que pide el formato del tipo (VisibilidadTipo) y se marca en pantalla para que el medico lo revise.
    // No se traen ficha (biometrico), laborales (propios de cada examen) ni antecedentes de la incapacidad.
    // ============================================================================================
    static final Set<String> TIPOS_EMNI = Set.of("2", "3", "4", "6", "8");
    static final Set<String> SECCIONES_EMNI = Set.of(
            "Inmunizaciones", "Gineco-obstétricos (AGO)",
            "APP: personales patológicos", "APP: oftalmológico", "APP: renal", "APP: cardiovascular",
            "APP: sistema nervioso", "APP: digestivo", "APP: músculo-esquelético", "APP: endocrino",
            "APP: toxicológico", "APP: otros",
            "Padecimiento actual", "Interrogatorio por aparatos",
            "Exploración física", "Cráneo", "Agudeza visual", "Oídos", "Cuello", "Nariz", "Tórax",
            "Columna vertebral", "Columna (movilidad)", "Abdomen", "Genitales", "Urinario", "Extremidades",
            "Piel", "Boca", "Dientes",
            "Estudios realizados", "Diagnóstico", "Plan terapéutico", "Resultado del examen");

    /** Lo que se trajo del EMNI en una seccion: items completados, campos tomados y fecha del EMNI (dd/MM/yyyy). */
    public record Complemento(List<ExamItem> items, Set<String> tomados, String fecha) {
    }

    /** Contexto del complemento para una carga (se lee una vez y se usa en todas las secciones). */
    public record ContextoEmni(String tipo, Map<String, String> foto, String fecha) {
        static final ContextoEmni VACIO = new ContextoEmni("", Map.of(), "");
    }

    /** Tipo de examen elegido y foto del ultimo EMNI; VACIO si el tipo no lleva complemento o no hay EMNI. Nunca lanza. */
    public ContextoEmni contextoEmni(String nss) {
        try {
            String t = tipo(nss);
            if (!TIPOS_EMNI.contains(t)) {
                return ContextoEmni.VACIO;
            }
            var foto = historico.ultimoInicial(nss.trim()).orElse(null);
            if (foto == null || foto.datosJson() == null || foto.datosJson().isBlank()) {
                return ContextoEmni.VACIO;
            }
            Map<String, String> datos = new com.fasterxml.jackson.databind.ObjectMapper().readValue(foto.datosJson(),
                    new com.fasterxml.jackson.core.type.TypeReference<java.util.LinkedHashMap<String, String>>() { });
            return new ContextoEmni(t, datos,
                    foto.fecha().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        } catch (Exception ex) {
            log.warn("[examen] complemento del EMNI no disponible para {}: {}", nss, ex.getMessage());
            return ContextoEmni.VACIO;
        }
    }

    /** Llena con el EMNI los campos VACIOS de la seccion que el formato del tipo pide. Nunca lanza. */
    public Complemento complementoEmni(String seccion, List<ExamItem> items, ContextoEmni ctx) {
        if (ctx == null || ctx.foto().isEmpty() || !SECCIONES_EMNI.contains(seccion) || ocultaEnTipo(seccion, ctx.tipo())) {
            return new Complemento(items, Set.of(), "");
        }
        try {
            Map<String, ExamItem> delEmni = new java.util.HashMap<>();
            ExamenService.itemsDesdeDatos(seccion, ctx.foto()).forEach(i -> delEmni.put(i.fieldName(), i));
            List<ExamItem> out = new ArrayList<>(items.size());
            Set<String> tomados = new java.util.LinkedHashSet<>();
            for (ExamItem it : items) {
                ExamItem e = delEmni.get(it.fieldName());
                boolean oculto = VisibilidadTipo.oculto(it.fieldName(), ctx.tipo()) || "OCULTO".equals(it.type());
                String valor = it.value();
                String obs = it.obsValue();
                if (e != null && !oculto) {
                    if (vacio(valor) && !vacio(e.value())) {
                        valor = e.value();
                        tomados.add(it.fieldName());
                    }
                    if (it.obsName() != null && vacio(obs) && !vacio(e.obsValue())) {
                        obs = e.obsValue();
                        tomados.add(it.obsName());
                    }
                }
                out.add(valor == it.value() && obs == it.obsValue() ? it
                        : new ExamItem(it.type(), it.label(), it.fieldName(), valor, it.obsName(), obs));
            }
            return new Complemento(out, tomados, tomados.isEmpty() ? "" : ctx.fecha());
        } catch (RuntimeException ex) {
            log.warn("[examen] complemento del EMNI de {} no disponible: {}", seccion, ex.getMessage());
            return new Complemento(items, Set.of(), "");
        }
    }

    /** La seccion (o su grupo) no va en el formato del tipo. */
    private boolean ocultaEnTipo(String seccion, String tipo) {
        if (VisibilidadTipo.oculto(seccion, tipo)) {
            return true;
        }
        return examenService.grupos().stream()
                .filter(g -> g.secciones().contains(seccion))
                .anyMatch(g -> VisibilidadTipo.oculto(g.nombre(), tipo));
    }

    private static boolean vacio(String v) {
        return v == null || v.isBlank();
    }

    /** Items de la seccion con lo precargado (o los mismos si no aplica). Nunca lanza. */
    public List<ExamItem> aplicar(String seccion, List<ExamItem> items, String nss) {
        try {
            if ("Antecedentes de la incapacidad".equals(seccion) && "4".equals(tipo(nss))) {
                return conValores(items, desdeIncapacidad(nss));
            }
            if ("Puesto inicial y actual".equals(seccion) && TIPOS_PUESTO.contains(tipo(nss))) {
                return conValores(items, Map.of("TAG.EXM_PUESTO_ACTUAL", puestoActual(nss)));
            }
        } catch (RuntimeException ex) {
            log.warn("[examen] precarga de {} no disponible: {}", seccion, ex.getMessage());
        }
        return items;
    }

    private String tipo(String nss) {
        String t = datosGenerales.cargar(nss).tipoExamen();
        return t == null ? "" : t.trim();
    }

    private Map<String, String> desdeIncapacidad(String nss) {
        List<IncapacidadDto> lista = incapacidades.byNss(nss);
        IncapacidadDto u = lista.stream()
                .max(Comparator.comparing((IncapacidadDto i) -> fechaIso(i.fechaInicio())))
                .orElse(null);
        if (u == null) {
            return Map.of();
        }
        return Map.of(
                "TAG.EXM_INC_DIAS", nz(u.diasAutorizados()),
                "TAG.EXM_INC_RAMO", ramo(u.ramo()),
                "TAG.EXM_INC_TIPO", (nz(u.rubro()) + " " + nz(u.tipoIncapacidad())).toUpperCase(Locale.ROOT).contains("INTERN") ? "INTERNA" : "IMSS",
                "TAG.EXM_INC_INICIO", fechaIso(u.fechaInicio()),
                "TAG.EXM_INC_TERMINO", fechaIso(u.fechaTermino()));
    }

    private String puestoActual(String nss) {
        String p = nssSearch.findByNss(nss)
                .map(r -> r.empleado() == null ? "" : nz(r.empleado().nombrePuesto()))
                .orElse("");
        // el WS manda mensajes en lugar del puesto ("el no. de puesto no existe en base de datos")
        return p.toLowerCase(Locale.ROOT).matches(".*(no existe|no disponible|sin puesto).*") ? "" : p;
    }

    /** Ramo del WS (texto libre en el legacy) a las opciones del formato: EG, MAT, RT, ENLACE. */
    static String ramo(String v) {
        String r = nz(v).toUpperCase(Locale.ROOT);
        if (r.contains("ENLACE")) {
            return "ENLACE";
        }
        if (r.startsWith("MAT")) {
            return "MAT";
        }
        if (r.equals("RT") || r.contains("RIESGO") || r.contains("TRABAJO") || r.contains("TRAYECTO")) {
            return "RT";
        }
        if (r.equals("EG") || r.contains("ENFERMEDAD")) {
            return "EG";
        }
        return r;
    }

    /** yyyy-MM-dd (lo que pide el control de fecha) o "" si la fecha no viene en ese formato. */
    static String fechaIso(String v) {
        String f = nz(v);
        return f.matches("^\\d{4}-\\d{2}-\\d{2}.*") ? f.substring(0, 10) : "";
    }

    private static String nz(String v) {
        return v == null ? "" : v.trim();
    }

    private static List<ExamItem> conValores(List<ExamItem> items, Map<String, String> valores) {
        List<ExamItem> out = new ArrayList<>(items.size());
        for (ExamItem it : items) {
            String nuevo = valores.get(it.fieldName());
            boolean vacio = it.value() == null || it.value().isBlank();
            out.add(vacio && nuevo != null && !nuevo.isBlank()
                    ? new ExamItem(it.type(), it.label(), it.fieldName(), nuevo, it.obsName(), it.obsValue())
                    : it);
        }
        return out;
    }
}
