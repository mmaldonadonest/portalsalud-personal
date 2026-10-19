package com.onest.app.catalog.examen.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ayudas de captura de la exploracion fisica de los formatos EMLE (15-oct-2026). Las usan el
 * examen inicial (FT-SO-04 rev. 05, filas 129-153) y el modulo Examen medico (periodico FT-SO-17
 * rev. 06, filas 49-74): los dos formatos traen los mismos textos predeterminados.
 *
 * <p>Lo que se guarda es TEXTO en la misma columna de siempre: el OK/NO no tiene columna propia,
 * se deduce del texto al volver a abrir. Sin cambios de WS ni de base.
 */
public final class AyudasFormato {

    private AyudasFormato() {
    }

    /**
     * <ul>
     *   <li>{@code TEXTO}: OK guarda {@code normal} ("SIN AGREGADOS", "DIFERIDO"); NO deja escribir el hallazgo.</li>
     *   <li>{@code DOS}: dos opciones; OK = {@code normal} ("AUSENTES", "NEGATIVO"), NO = {@code alterno} y se puede detallar.</li>
     *   <li>{@code OPCIONES}: una de {@code opciones} (curvatura: normal / aumentada / disminuida).</li>
     * </ul>
     */
    public record Ayuda(String modo, String normal, String alterno, List<String> opciones) {
        static Ayuda texto(String normal) {
            return new Ayuda("TEXTO", normal, "", List.of());
        }

        static Ayuda dos(String normal, String alterno) {
            return new Ayuda("DOS", normal, alterno, List.of());
        }

        static Ayuda opciones(String normal, String... resto) {
            List<String> todas = new ArrayList<>();
            todas.add(normal);
            todas.addAll(List.of(resto));
            return new Ayuda("OPCIONES", normal, "", List.copyOf(todas));
        }
    }

    private static final String SA = "SIN AGREGADOS";

    /** fieldName -> ayuda, copiado celda por celda de los Excel EMLE. */
    public static final Map<String, Ayuda> AYUDAS = construir();

    /** true si algun campo de la lista tiene ayuda. */
    public static boolean alguno(List<com.onest.app.catalog.examen.dto.ExamItem> items) {
        return items != null && items.stream().anyMatch(i -> AYUDAS.containsKey(i.fieldName()));
    }

    private static Map<String, Ayuda> construir() {
        Map<String, Ayuda> m = new LinkedHashMap<>();
        Ayuda sa = Ayuda.texto(SA);
        Ayuda ausentes = Ayuda.dos("AUSENTES", "PRESENTES");
        Ayuda ausente = Ayuda.dos("AUSENTE", "PRESENTE");
        Ayuda negativo = Ayuda.dos("NEGATIVO", "POSITIVO");
        for (String c : List.of("OJOS", "PUPILAS", "CONJUNTIVAS", "FONDO_OJO")) {
            m.put("SERV_MED_CRANEO." + c, sa);
        }
        // reflejos: lo normal es que esten presentes
        m.put("SERV_MED_CRANEO.REFLEJOS", Ayuda.dos("PRESENTES", "AUSENTES"));
        // Agudeza visual (FT-SO-04 filas 135-137): OK / NO sin texto predeterminado; OK escribe "OK" y
        // con NO se anota la medida o el hallazgo (20/20 ya capturado cuenta como OK)
        for (String c : List.of("OD", "OIZ", "COLORES")) {
            m.put("SERV_MED_AGUDEZA_VISUAL." + c, Ayuda.texto("OK"));
        }
        for (String t : List.of("ODCAEINPEXP", "ODMEMTIMINPEXP", "ODAGAUDIINPEXP", "OIMEMTIMINPEXP", "OIAGAUDIINPEXP")) {
            m.put("TAG." + t, sa);
        }
        m.put("SERV_MED_CUELLO.DEFORMACIONES_OBS", ausentes);
        m.put("SERV_MED_CUELLO.TIROIDES_OBS", sa);
        m.put("SERV_MED_CUELLO.TRAQUEA_OBS", sa);
        for (String c : List.of("CAVIDAD_NASAL", "MUCOSA", "TABIQUE_NASA", "OLFATO")) {
            m.put("SERV_MED_NARIZ." + c, sa);
        }
        for (String c : List.of("RUIDOS_CARDIACOS", "REG_PRECORDIAL", "CAMPOS_PULM")) {
            m.put("SERV_MED_TORAX." + c, sa);
        }
        // "CURVATURA FISIOLOGICA ANT-POST: NORMAL / AUMENTADA / DISMINUIDA" para cada segmento
        Ayuda curvatura = Ayuda.opciones("NORMAL", "AUMENTADA", "DISMINUIDA");
        for (String c : List.of("CERVICA", "DORSAL", "LUMBAR")) {
            m.put("SERV_MED_COLUMNA_VERTEBRAL." + c, curvatura);
        }
        String cv = "SERV_MED_COLUMNA_VERTEBRAL_AUX.";
        m.put(cv + "DEFORMACIONES", ausentes);
        m.put(cv + "DOLOR", ausentes);
        m.put(cv + "MOVIMIENTOS", sa);
        m.put(cv + "MARCHA", sa);
        m.put(cv + "LASSEGUE", negativo);
        m.put(cv + "PUNTA_TALON", sa);
        m.put("SERV_MED_ABDOMEN.FORMA", sa);
        m.put("SERV_MED_ABDOMEN.VISCEROMEGALIAS", ausentes);
        m.put("SERV_MED_ABDOMEN.HERNIAS", ausentes);
        m.put("SERV_MED_ABDOMEN.DOLOR", ausente);
        m.put("SERV_MED_ABDOMEN.PERISTALSIS", sa);
        m.put("SERV_MED_GENITALES.MASCULINO", Ayuda.texto("DIFERIDO"));
        m.put("SERV_MED_GENITALES.FEMENINO", Ayuda.texto("DIFERIDO"));
        m.put("SERV_MED_URINARIO.PUNTOS_URETRALES", negativo);
        m.put("SERV_MED_URINARIO.FOSAS_RENALES", sa);
        m.put("SERV_MED_EXTREMIDADES.SUPERIOR", sa);
        m.put("SERV_MED_EXTREMIDADES.INFERIOR", sa);
        m.put("SERV_MED_EXTREMIDADES.IVP", ausente);
        m.put("SERV_MED_EXTREMIDADES.EDEMA", negativo);
        m.put("SERV_MED_PIEL.LUNARES", sa);
        m.put("SERV_MED_PIEL.PIGMENTACION", sa);
        m.put("SERV_MED_PIEL.CICATRICES_QUIRUR", ausentes);
        m.put("SERV_MED_BOCA.LESIONES", ausentes);
        m.put("SERV_MED_BOCA.ENCIAS", sa);
        m.put("SERV_MED_BOCA.OROIFARINGE", sa);
        return Collections.unmodifiableMap(m);
    }
}
