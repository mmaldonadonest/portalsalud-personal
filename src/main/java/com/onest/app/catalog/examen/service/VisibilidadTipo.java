package com.onest.app.catalog.examen.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Que ve cada tipo de examen en el modulo Examen medico, segun su formato EMLE (16-oct-2026).
 * Una sola tabla para la pantalla (examen-shell.html / examen-seccion.html, via T(...)) y el JS:
 * cada grupo, seccion o campo lleva clases "ocultar-en-N" y el acordeon la clase "tipo-N"; el CSS
 * oculta lo que no va. Nada se quita del DOM: si el medico cambia el tipo, lo capturado sigue ahi.
 *
 * Tipos (codigo del tag tipoExamenInputO, ver DatosGeneralesExamenService.TIPOS):
 * 1 Admision FT-SO-04, 2 Periodico FT-SO-17, 3 Cambio de puesto FT-SO-16, 4 Post incapacidad
 * FT-SO-13, 5 Especial (sin formato: ve todo), 6 Egreso FT-SO-18, 7 Inicial mantenimiento FT-SO-19,
 * 8 Periodico mantenimiento FT-SO-20.
 */
public final class VisibilidadTipo {

    /** Formatos "cortos" (FT-SO-17 y derivados): sin heredofamiliares, APNP ni el bloque laboral del FT-SO-04. */
    private static final Set<String> CORTOS = Set.of("2", "3", "4", "6", "8");

    // Clave = nombre de grupo, de seccion o de campo (SECCION.CAMPO); valor = tipos donde NO va.
    // FT-SO-18 no trae Plan y FT-SO-19 no trae Diagnostico: se respeta el formato (decision 16-oct).
    private static final Map<String, Set<String>> OCULTAR = Map.ofEntries(
            Map.entry("Antecedentes laborales (resumen)", CORTOS),
            // "Otros datos" del modulo: solo los piden el FT-SO-04 y el FT-SO-19
            Map.entry("Trabajos previos", CORTOS),
            Map.entry("Contactos de emergencia", CORTOS),
            // bloques propios de cada formato (5 Especial ve todo)
            Map.entry("Puesto inicial y actual", Set.of("1", "3", "4", "7")),
            Map.entry("TAG.EXM_TURNO_SALIDA", Set.of("1", "2", "3", "4", "7", "8")),
            Map.entry("TAG.EXM_ANTIGUEDAD", Set.of("1", "2", "3", "4", "7", "8")),
            Map.entry("TAG.EXM_MOTIVO_SALIDA", Set.of("1", "2", "3", "4", "7", "8")),
            Map.entry("Antecedentes laborales específicos", Set.of("1", "2", "3", "4", "6")),
            Map.entry("Cambio de puesto", Set.of("1", "2", "4", "6", "7", "8")),
            Map.entry("Antecedentes de la incapacidad", Set.of("1", "2", "3", "6", "7", "8")),
            Map.entry("Antecedentes heredofamiliares", CORTOS),
            Map.entry("APNP (no patológicos)", CORTOS),
            Map.entry("Antecedentes personales no patológicos", Set.of("4")),
            Map.entry("SERV_MED_ANT_GIN_OBSTETRICOS.FUP", Set.of("2", "3", "4", "6")),
            Map.entry("Antecedentes personales patológicos", Set.of("4")),
            Map.entry("Padecimiento actual", Set.of("4")),
            Map.entry("Interrogatorio por aparatos y sistemas", Set.of("4")),
            // FT-SO-13: de la exploracion solo pide signos vitales (seccion "Exploración física")
            Map.entry("Cráneo", Set.of("4")), Map.entry("Agudeza visual", Set.of("4")),
            Map.entry("Nariz", Set.of("4")), Map.entry("Columna vertebral", Set.of("4")),
            Map.entry("Boca", Set.of("4")), Map.entry("Oídos", Set.of("4")),
            Map.entry("Cuello", Set.of("4")), Map.entry("Tórax", Set.of("4")),
            Map.entry("Columna (movilidad)", Set.of("4")), Map.entry("Abdomen", Set.of("4")),
            Map.entry("Genitales", Set.of("4")), Map.entry("Urinario", Set.of("4")),
            Map.entry("Extremidades", Set.of("4")), Map.entry("Piel", Set.of("4")),
            Map.entry("Dientes", Set.of("4")),
            Map.entry("Estudios realizados y resultados", Set.of("4")),
            Map.entry("Diagnóstico", Set.of("7")),
            Map.entry("Plan o terapéutica empleada", Set.of("6")));

    /** Lenguaje (voz clara, conversacion fluida, coherencia): solo lo piden FT-SO-04 y FT-SO-19 (check 28-oct-2026). */
    private static final Set<String> SIN_LENGUAJE = Set.of("2", "3", "4", "6", "8");
    /** FT-SO-13: de la exploracion solo signos vitales; constitucion, actitud, orientacion, etc. no van. */
    private static final Set<String> SOLO_SIGNOS = Set.of("4");

    // Campos sueltos de la seccion "Exploracion fisica" (tabla SERV_MED_EXPLORACION_FISICA), 28-oct-2026
    private static final Map<String, Set<String>> OCULTAR_CAMPOS = Map.ofEntries(
            Map.entry("SERV_MED_EXPLORACION_FISICA.LENGUAJE", SIN_LENGUAJE),
            Map.entry("SERV_MED_EXPLORACION_FISICA.CONVER_FLUIDA", SIN_LENGUAJE),
            Map.entry("SERV_MED_EXPLORACION_FISICA.COHERENCIA_PALABLAS", SIN_LENGUAJE),
            Map.entry("SERV_MED_EXPLORACION_FISICA.ECTOMORFICO", SOLO_SIGNOS),
            Map.entry("SERV_MED_EXPLORACION_FISICA.SATISFACCION", SOLO_SIGNOS),
            Map.entry("SERV_MED_EXPLORACION_FISICA.COMP_TRABAJO", SOLO_SIGNOS),
            Map.entry("SERV_MED_EXPLORACION_FISICA.COMP_ORGANIZACION", SOLO_SIGNOS),
            Map.entry("SERV_MED_EXPLORACION_FISICA.TIEMPO", SOLO_SIGNOS),
            Map.entry("SERV_MED_EXPLORACION_FISICA.ESPACIO", SOLO_SIGNOS),
            Map.entry("SERV_MED_EXPLORACION_FISICA.PERSONA", SOLO_SIGNOS),
            Map.entry("SERV_MED_EXPLORACION_FISICA.SERENIDAD", SOLO_SIGNOS),
            Map.entry("SERV_MED_EXPLORACION_FISICA.HIPOCRITA", SOLO_SIGNOS),
            Map.entry("SERV_MED_EXPLORACION_FISICA.PARKINSON", SOLO_SIGNOS));

    /** El titulo de no patologicos lleva «"Inmunizaciones"» donde el formato solo pide inmunizaciones. */
    public static final String TITULO_INMUNIZACIONES = "ocultar-en-1 ocultar-en-4 ocultar-en-5 ocultar-en-7 ocultar-en-";

    private VisibilidadTipo() {
    }

    private static Set<String> ocultoEn(String clave) {
        if (clave == null) {
            return Set.of();
        }
        Set<String> s = OCULTAR.get(clave);
        return s != null ? s : OCULTAR_CAMPOS.getOrDefault(clave, Set.of());
    }

    /**
     * Para el impreso (examen-documento.html): true si TODAS las claves (separadas por "|") se
     * ocultan en el tipo. Con una sola clave es "esta seccion no va"; con varias, "esta hoja queda vacia".
     */
    public static boolean oculto(String claves, String tipo) {
        String t = tipo == null ? "" : tipo.trim();
        if (t.isEmpty() || claves == null) {
            return false;
        }
        for (String c : claves.split("\\|")) {
            if (!ocultoEn(c.trim()).contains(t)) {
                return false;
            }
        }
        return true;
    }

    /** Clases "ocultar-en-N" de un grupo, seccion o campo ("" si va en todos los tipos). */
    public static String clases(String clave) {
        return a(ocultoEn(clave));
    }

    /** Un grupo se oculta donde se oculta el mismo o todas sus secciones. */
    public static String clasesGrupo(String grupo, List<String> secciones) {
        Set<String> tipos = new TreeSet<>(ocultoEn(grupo));
        if (secciones != null && !secciones.isEmpty()) {
            Set<String> todas = new TreeSet<>(ocultoEn(secciones.get(0)));
            secciones.forEach(s -> todas.retainAll(ocultoEn(s)));
            tipos.addAll(todas);
        }
        return a(tipos);
    }

    /** Indice de la primera seccion visible para el tipo (la pestana que abre activa). */
    public static int primeraVisible(List<String> secciones, String tipo) {
        for (int i = 0; secciones != null && i < secciones.size(); i++) {
            if (!ocultoEn(secciones.get(i)).contains(tipo == null ? "" : tipo.trim())) {
                return i;
            }
        }
        return 0;
    }

    private static String a(Set<String> tipos) {
        StringBuilder sb = new StringBuilder();
        new TreeSet<>(tipos).forEach(t -> sb.append(sb.length() == 0 ? "" : " ").append("ocultar-en-").append(t));
        return sb.toString();
    }
}
