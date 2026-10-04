package com.onest.excel.inventario;

import java.util.List;

/**
 * Los tipos de reporte que hay en la carpeta MORBILIDAD.
 *
 * <p><b>La clasificacion va por el TEXTO del nombre, nunca por el prefijo numerico.</b>
 * El numero no es confiable: en ZARA 2026 el archivo {@code 4} es un pos-incapacidad y el
 * {@code 5} un reporte de incapacidades, al revés que en los demas predios; en ZARA 2022 el
 * {@code 7} es un pos-incapacidad y el {@code 8} consumibles. Medido el 29-sep-2026.
 *
 * <p>El orden de {@link #PATRONES} importa: gana el primero que coincida, y por eso
 * "POS INCAPACIDAD" tiene que evaluarse antes que "INCAPACIDAD" — si no, todos los
 * pos-incapacidad caerian en INCAPACIDAD.
 *
 * <p>Las nueve primeras son las que servicio medico aprobo en el cuestionario del 29-sep-2026.
 * Las siguientes salieron al recorrer los 132 archivos de 2026: 24 no pertenecian a ninguna de
 * las nueve y nadie las habia presentado. Ver docs/plan-carga-excel-2026.html fase 3.
 */
public enum Familia {

    // --- Las 9 aprobadas en el cuestionario ---
    ATENCION("Atenciones diarias"),
    EXAMEN_INGRESO("Examen de nuevo ingreso"),
    EXAMEN_PERIODICO("Examen periodico"),
    EXAMEN_POS_INCAP("Examen pos-incapacidad"),
    INCAPACIDAD("Incapacidades"),
    ACCIDENTE("Accidentabilidad"),
    ANTIDOPING("Antidoping"),
    MATERNIDAD("Maternidad"),
    CONSUMIBLE("Consumibles"),

    // --- Reportes de seguimiento (fase 3) ---
    CARNET("Carnet de cronicos"),
    CAPA("CAPA / adicciones"),
    NOM035("NOM-035 riesgo psicosocial"),
    SEGUIMIENTO_ESP("Seguimiento medico especial"),
    PRODUCTIVIDAD("Productividad preventiva"),
    MANIFIESTOS("Manifiestos de residuos"),

    // --- Resumen, no detalle ---
    RESUMEN_MORBILIDAD("Reporte general de morbilidad"),

    DESCONOCIDA("Sin clasificar");

    private final String descripcion;

    Familia(String descripcion) {
        this.descripcion = descripcion;
    }

    public String descripcion() {
        return descripcion;
    }

    /** Si entra al alcance acordado: las 9 del cuestionario mas los 4 de seguimiento que se aprobaron. */
    public boolean enAlcance() {
        return switch (this) {
            case ATENCION, EXAMEN_INGRESO, EXAMEN_PERIODICO, EXAMEN_POS_INCAP,
                 INCAPACIDAD, ACCIDENTE, ANTIDOPING, MATERNIDAD, CONSUMIBLE,
                 CARNET, CAPA, NOM035, SEGUIMIENTO_ESP -> true;
            // PRODUCTIVIDAD existe en un solo predio; MANIFIESTOS es control de residuos,
            // no salud de personas; RESUMEN_MORBILIDAD es agregado de los demas.
            case PRODUCTIVIDAD, MANIFIESTOS, RESUMEN_MORBILIDAD, DESCONOCIDA -> false;
        };
    }

    /** Si el reporte sigue PERSONAS en el tiempo en vez de contar EVENTOS (define el layout). */
    public boolean esSeguimiento() {
        return switch (this) {
            case CARNET, CAPA, NOM035, SEGUIMIENTO_ESP -> true;
            default -> false;
        };
    }

    private record Patron(String texto, Familia familia) {}

    /** Orden significativo: gana la primera coincidencia. Lo mas especifico primero. */
    private static final List<Patron> PATRONES = List.of(
            // Especificos antes que generales
            new Patron("POS INCAPACIDAD", EXAMEN_POS_INCAP),
            new Patron("POST INCAPACIDAD", EXAMEN_POS_INCAP),
            new Patron("NUEVO INGRESO", EXAMEN_INGRESO),
            new Patron("PERIODICO", EXAMEN_PERIODICO),
            // "REGISTRO DIARIO" es la firma real de atenciones. NO usar "CONTROL":
            // CONTROL CAPA y CONTROL MANIFIESTOS no son atenciones (error cometido el 29-sep).
            new Patron("REGISTRO DIARIO", ATENCION),
            new Patron("CONTROL CAPA", CAPA),
            new Patron("CONTROL MANIFIESTOS", MANIFIESTOS),
            new Patron("MANIFIESTO", MANIFIESTOS),
            new Patron("NOM035", NOM035),
            new Patron("NOM-035", NOM035),
            new Patron("SEGUIMIENTO MEDICO ESPECIAL", SEGUIMIENTO_ESP),
            new Patron("CARNET", CARNET),
            new Patron("PRODUCTIVIDAD", PRODUCTIVIDAD),
            new Patron("ACCIDENTABILIDAD", ACCIDENTE),
            new Patron("ANTIDOPING", ANTIDOPING),
            new Patron("MATERNIDAD", MATERNIDAD),
            new Patron("CONSUMIBLE", CONSUMIBLE),
            // Generales al final
            new Patron("INCAPACIDAD", INCAPACIDAD),
            new Patron("MORBILIDAD", RESUMEN_MORBILIDAD)
    );

    /** Clasifica por el nombre del archivo. Devuelve {@link #DESCONOCIDA} si nada coincide. */
    public static Familia deNombreArchivo(String nombre) {
        if (nombre == null) {
            return DESCONOCIDA;
        }
        String u = normalizar(nombre);
        for (Patron p : PATRONES) {
            if (u.contains(p.texto())) {
                return p.familia();
            }
        }
        return DESCONOCIDA;
    }

    /** Mayusculas, sin acentos y con espacios simples: el origen escribe el mismo nombre de varias formas. */
    static String normalizar(String s) {
        String u = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toUpperCase();
        return u.replaceAll("\\s+", " ").trim();
    }
}
