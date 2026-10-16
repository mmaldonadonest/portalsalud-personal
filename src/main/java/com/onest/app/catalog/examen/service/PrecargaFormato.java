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

    public PrecargaFormato(IncapacidadService incapacidades, NssSearchService nssSearch,
                           DatosGeneralesExamenService datosGenerales) {
        this.incapacidades = incapacidades;
        this.nssSearch = nssSearch;
        this.datosGenerales = datosGenerales;
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
