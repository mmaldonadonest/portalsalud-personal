package com.onest.app.catalog.nss.dto;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;

/**
 * Etiqueta del estado civil (4-nov-2026). BIO_EMPLEADO.EMP_ESTADO_CIVIL no tiene catalogo: los activos traen
 * el codigo de SAP y las bajas viejas texto libre (soltero, CASADO, union_libre, S, C, U...). La ficha lo
 * pintaba crudo ("Estado civil 0") y el impreso borraba el 0, que en realidad es Soltero.
 *
 * <p>0 y 1 los confirman las vistas del biometrico (VW_MAESTRO_EMPLEADOS / VW_IA_EMPLEADOS: 1 = CASADO).
 * 2, 3, 5 y 6 son el estandar de SAP que eligio el usuario; falta que RH los confirme (Pendientes del
 * Control de cambios). 4, 7, 8, X, "sin definir" y vacio no tienen traduccion: null (la pantalla pone "—").
 * Cualquier otro texto se muestra tal cual, con mayuscula inicial.
 */
public final class EstadoCivil {

    private static final Map<String, String> ETIQUETAS = Map.ofEntries(
            Map.entry("0", "Soltero"), Map.entry("1", "Casado"), Map.entry("2", "Viudo"),
            Map.entry("3", "Divorciado"), Map.entry("5", "Separado"), Map.entry("6", "Unión libre"),
            // texto libre de las bajas viejas y de la solicitud de OCR (ya sin acentos y en minusculas)
            Map.entry("s", "Soltero"), Map.entry("soltero", "Soltero"), Map.entry("soltera", "Soltero"),
            Map.entry("c", "Casado"), Map.entry("casado", "Casado"), Map.entry("casada", "Casado"),
            Map.entry("u", "Unión libre"), Map.entry("union libre", "Unión libre"), Map.entry("union_libre", "Unión libre"),
            Map.entry("d", "Divorciado"), Map.entry("divorciado", "Divorciado"), Map.entry("divorciada", "Divorciado"),
            Map.entry("v", "Viudo"), Map.entry("viudo", "Viudo"), Map.entry("viuda", "Viudo"),
            Map.entry("separado", "Separado"), Map.entry("separada", "Separado"));

    private static final java.util.Set<String> SIN_DATO = java.util.Set.of("", "x", "sin definir", "4", "7", "8");

    private EstadoCivil() {
    }

    /** Etiqueta para mostrar, o null si no hay dato (la pantalla decide si pone "—" o vacio). */
    public static String etiqueta(String valor) {
        if (valor == null) {
            return null;
        }
        String clave = Normalizer.normalize(valor.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        if (SIN_DATO.contains(clave)) {
            return null;
        }
        String e = ETIQUETAS.get(clave);
        if (e != null) {
            return e;
        }
        String t = valor.trim();
        return t.substring(0, 1).toUpperCase(Locale.ROOT) + t.substring(1).toLowerCase(Locale.ROOT);
    }
}
