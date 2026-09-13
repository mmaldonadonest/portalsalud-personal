package com.onest.app.catalog.examen.service;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Reconcilia los nombres de campo entre el WS de LECTURA (consulta_examen) y el de ESCRITURA
 * (Medico / PR_SERVICIO_MED_EXAMEN*): no coinciden. Verificado 13-sep-2026 contra el JSON
 * real: la lectura devuelve claves con espacios ('SABIN ', 'INFARTOS ', 'LUMBALGIA _OBS'),
 * typos distintos a los de escritura (edad_inicio_laborar / edad_inicio_laboral,
 * LORDOSIS_OBS / LORDOSISI_OBS, DIENTE_2 / DIENTE_21) y algunos campos que ni siquiera
 * devuelve (AVC_OBS, NEFROPATIAS_ID, NOLMAL_FASCIES).
 *
 * <p>Los nombres del formulario son los de ESCRITURA (los que espera el proc). Para leer se
 * busca la clave de lectura equivalente: exacta, o por alias, o por forma normalizada
 * (sin espacios, sin acentos/Ñ, mayusculas) dentro de la misma seccion. Para fusionar el
 * estado vigente antes de guardar se hace el camino inverso.
 */
public final class ExamenClaves {

    /** escritura -> lectura, para los casos que la normalizacion no resuelve. */
    private static final Map<String, String> ALIAS_LECTURA = Map.of(
            "SERV_ANTECEDENTESLAB.edad_inicio_laboral", "SERV_ANTECEDENTESLAB.edad_inicio_laborar",
            "SERV_MED_MUS_ESQUELETICO.LORDOSISI_OBS", "SERV_MED_MUS_ESQUELETICO.LORDOSIS_OBS",
            "SERV_MED_DIENTES.DIENTE_21", "SERV_MED_DIENTES.DIENTE_2");

    private ExamenClaves() {
    }

    /** Valor de lectura para una clave de escritura ("SECCION.CAMPO"); null si el WS no la trae. */
    public static String leer(Map<String, String> data, String claveEscritura) {
        String directo = data.get(claveEscritura);
        if (directo != null) {
            return directo;
        }
        String alias = ALIAS_LECTURA.get(claveEscritura);
        if (alias != null && data.containsKey(alias)) {
            return data.get(alias);
        }
        String buscada = normaliza(claveEscritura);
        for (Map.Entry<String, String> e : data.entrySet()) {
            if (normaliza(e.getKey()).equals(buscada)) {
                return e.getValue();
            }
        }
        return null;
    }

    /**
     * Clave de escritura equivalente a una clave de lectura, si existe entre las conocidas;
     * si no, la misma clave (el proc la ignorara si no la conoce, sin romper nada).
     */
    public static String aEscritura(String claveLectura, Set<String> clavesEscritura) {
        if (clavesEscritura.contains(claveLectura)) {
            return claveLectura;
        }
        for (Map.Entry<String, String> a : ALIAS_LECTURA.entrySet()) {
            if (a.getValue().equals(claveLectura)) {
                return a.getKey();
            }
        }
        String buscada = normaliza(claveLectura);
        Map<String, String> indice = new HashMap<>();
        for (String k : clavesEscritura) {
            indice.putIfAbsent(normaliza(k), k);
        }
        String hit = indice.get(buscada);
        return hit != null ? hit : claveLectura.trim();
    }

    static String normaliza(String k) {
        return k.replace(" ", "").replace("Ñ", "N").replace("ñ", "n").toUpperCase(Locale.ROOT);
    }
}
