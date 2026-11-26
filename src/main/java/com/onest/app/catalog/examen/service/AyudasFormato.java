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

    /**
     * Examen inicial (EMNI), 25-nov-2026 (Word de SM «errores11252026», puntos 13 y 16-24): «Todo normal» y el
     * Sí de cada renglon ponen ESTOS textos, que dicto SM. Solo el EMNI: el modulo Examen medico (periodico)
     * sigue con {@link #AYUDAS}. Agudeza visual sin lentes (OD, OIZ) sale de aqui: SM pidio la medida (20/20)
     * como texto libre, sin botones (punto 15).
     */
    public static final Map<String, Ayuda> AYUDAS_EMNI = construirEmni();

    /** true si algun campo de la lista tiene ayuda. */
    public static boolean alguno(List<com.onest.app.catalog.examen.dto.ExamItem> items) {
        return items != null && items.stream().anyMatch(i -> AYUDAS.containsKey(i.fieldName()));
    }

    private static Map<String, Ayuda> construirEmni() {
        Map<String, Ayuda> m = new LinkedHashMap<>(AYUDAS);
        String sdpa = "SDPA";
        String cr = "SERV_MED_CRANEO.";
        m.put(cr + "OJOS", Ayuda.texto("SIMETRICOS"));
        m.put(cr + "PUPILAS", Ayuda.texto("ISOCORICAS"));
        m.put(cr + "CONJUNTIVAS", Ayuda.texto("HIDRATADAS"));
        m.put(cr + "REFLEJOS", Ayuda.texto("PRESENTES"));
        m.put(cr + "FONDO_OJO", Ayuda.texto(sdpa));
        m.remove("SERV_MED_AGUDEZA_VISUAL.OD");
        m.remove("SERV_MED_AGUDEZA_VISUAL.OIZ");
        m.put("SERV_MED_AGUDEZA_VISUAL.COLORES", Ayuda.texto("SDPA DE ALTERACION DE COLORIMETRIA"));
        // Oidos: los tags no corresponden a la etiqueta (ver ExamenService): CAE, membrana, agudeza, OD, OI
        m.put("TAG.ODCAEINPEXP", Ayuda.texto("PRESENTE"));
        m.put("TAG.ODMEMTIMINPEXP", Ayuda.texto(sdpa));
        m.put("TAG.ODAGAUDIINPEXP", Ayuda.texto("PRESENTE"));
        m.put("TAG.OIMEMTIMINPEXP", Ayuda.texto(sdpa));
        m.put("TAG.OIAGAUDIINPEXP", Ayuda.texto(sdpa));
        m.put("SERV_MED_CUELLO.DEFORMACIONES_OBS", Ayuda.texto("AUSENTES / NO DETECTADAS"));
        m.put("SERV_MED_CUELLO.TIROIDES_OBS", Ayuda.texto(sdpa));
        m.put("SERV_MED_CUELLO.TRAQUEA_OBS", Ayuda.texto(sdpa));
        m.put("SERV_MED_NARIZ.CAVIDAD_NASAL", Ayuda.texto("PRESENTE"));
        m.put("SERV_MED_NARIZ.MUCOSA", Ayuda.texto("HIDRATADA"));
        m.put("SERV_MED_NARIZ.TABIQUE_NASA", Ayuda.texto("LINEAL"));
        m.put("SERV_MED_NARIZ.OLFATO", Ayuda.texto("PRESENTE SIN ALTERACION APARENTE"));
        m.put("SERV_MED_TORAX.RUIDOS_CARDIACOS", Ayuda.texto("RITMICOS"));
        m.put("SERV_MED_TORAX.REG_PRECORDIAL", Ayuda.texto(sdpa));
        m.put("SERV_MED_TORAX.CAMPOS_PULM", Ayuda.texto("LIMPIOS"));
        String cv = "SERV_MED_COLUMNA_VERTEBRAL_AUX.";
        m.put(cv + "DEFORMACIONES", Ayuda.texto("NEGADAS / NO DETECTADAS"));
        m.put(cv + "DOLOR", Ayuda.texto("NO REFERIDO"));
        m.put(cv + "MOVIMIENTOS", Ayuda.texto("ADECUADO"));
        m.put(cv + "MARCHA", Ayuda.texto("LINEAL"));
        m.put(cv + "LASSEGUE", Ayuda.texto("NEGATIVO"));
        m.put(cv + "PUNTA_TALON", Ayuda.texto("ADECUADO SIN ALTERACION APARENTE"));
        m.put("SERV_MED_ABDOMEN.FORMA", Ayuda.texto("GLOBOSO"));
        m.put("SERV_MED_ABDOMEN.VISCEROMEGALIAS", Ayuda.texto(sdpa));
        m.put("SERV_MED_ABDOMEN.HERNIAS", Ayuda.texto("NEGADAS / NO DETECTADAS"));
        m.put("SERV_MED_ABDOMEN.DOLOR", Ayuda.texto("NEGADAS / NO DETECTADAS"));
        m.put("SERV_MED_ABDOMEN.PERISTALSIS", Ayuda.texto(sdpa));
        m.put("SERV_MED_GENITALES.MASCULINO", Ayuda.texto("NO VALORADO"));
        m.put("SERV_MED_GENITALES.FEMENINO", Ayuda.texto("NO VALORADO"));
        m.put("SERV_MED_URINARIO.PUNTOS_URETRALES", Ayuda.texto(sdpa));
        m.put("SERV_MED_URINARIO.FOSAS_RENALES", Ayuda.texto(sdpa));
        m.put("SERV_MED_EXTREMIDADES.SUPERIOR", Ayuda.texto("INTEGROS Y SIMETRICOS"));
        m.put("SERV_MED_EXTREMIDADES.INFERIOR", Ayuda.texto("INTEGROS Y SIMETRICOS"));
        m.put("SERV_MED_EXTREMIDADES.IVP", Ayuda.texto("NO DETECTADAS / NEGADAS"));
        m.put("SERV_MED_EXTREMIDADES.EDEMA", Ayuda.texto("NO DETECTADAS / NEGADAS"));
        m.put("SERV_MED_PIEL.LUNARES", Ayuda.texto(sdpa));
        // 25-nov-2026 (decision del usuario: «respetemos la imagen» del Word de SM, punto 23): «1»
        m.put("SERV_MED_PIEL.PIGMENTACION", Ayuda.texto("1"));
        m.put("SERV_MED_PIEL.CICATRICES_QUIRUR", Ayuda.texto("AUSENTES"));
        m.put("SERV_MED_BOCA.LESIONES", Ayuda.texto("AUSENTES / NO DETECTADAS"));
        m.put("SERV_MED_BOCA.ENCIAS", Ayuda.texto(sdpa));
        m.put("SERV_MED_BOCA.OROIFARINGE", Ayuda.texto("NORMAL SDPA"));
        return Collections.unmodifiableMap(m);
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
        // C-067 (10-nov, Word SM punto 3: "quitar SIDP y colocar SDPA, es decir dejar SDPA"): lo normal es SDPA
        m.put("SERV_MED_URINARIO.FOSAS_RENALES", Ayuda.texto("SDPA"));
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
