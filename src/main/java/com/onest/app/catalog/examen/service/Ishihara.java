package com.onest.app.catalog.examen.service;

import java.util.List;

/**
 * Prueba de Ishihara (vision de colores), C-069 (10-nov-2026, Word de SM punto 5; formato «prueba ishihara.xlsx»).
 * Solo en el examen inicial (decision del usuario 10-nov), hoja de Exploracion junto a Agudeza visual. Opcional:
 * no cuenta en el avance ni impide finalizar.
 *
 * <p>El formato de SM trae 4 versiones con las mismas laminas en distinto orden; el medico marca cual aplico
 * (decision del usuario 10-nov) y el impreso sale con esa hoja. Imagenes: static/img/ishihara/v{version}-{1..9}.jpg,
 * recortadas del Excel de SM en el orden en que aparecen.
 *
 * <p>Se guarda TODO en un tag de SERV_MED_TAG, {@code EXM_ISHIHARA}, como JSON: VERSION (1-4), R1..R9 (lo que el
 * trabajador vio en cada lamina, en el orden de la version), RESULTADO (NORMAL o MONOCROMATICO) y TIPO
 * (DEUTERANOPIA, PROTANOPIA, TRITANOPIA o ACROMATIA). No hay tabla: el nombre de la seccion es solo de referencia.
 */
public final class Ishihara {

    public static final String SECCION = "Prueba de Ishihara";
    public static final String TAG = "EXM_ISHIHARA";
    public static final int VERSIONES = 4;
    public static final int LAMINAS = 9;

    /** Tipos de monocromatico del formato, en su orden (clave, texto). */
    public static final List<Osteomuscular.Eje> TIPOS = List.of(
            new Osteomuscular.Eje("DEUTERANOPIA", "Deuteranopia"), new Osteomuscular.Eje("PROTANOPIA", "Protanopia"),
            new Osteomuscular.Eje("TRITANOPIA", "Tritanopia"), new Osteomuscular.Eje("ACROMATIA", "Acromatia"));

    /** Para las plantillas: th:with="I=${T(...Ishihara).INSTANCIA}". */
    public static final Ishihara INSTANCIA = new Ishihara();

    private Ishihara() {
    }

    public List<Osteomuscular.Eje> getTipos() {
        return TIPOS;
    }

    public List<Integer> getVersiones() {
        return java.util.stream.IntStream.rangeClosed(1, VERSIONES).boxed().toList();
    }

    public List<Integer> getLaminas() {
        return java.util.stream.IntStream.rangeClosed(1, LAMINAS).boxed().toList();
    }

    /** Valor de una clave del JSON ("" si no hay); mismo lector tolerante que el osteomuscular. */
    public String v(String json, String clave) {
        return Osteomuscular.valor(json, clave);
    }

    /** Version a mostrar: la guardada o la 1. */
    public int version(String json) {
        try {
            int n = Integer.parseInt(Osteomuscular.valor(json, "VERSION"));
            return n >= 1 && n <= VERSIONES ? n : 1;
        } catch (NumberFormatException ex) {
            return 1;
        }
    }

    /** true si se capturo algo (el impreso solo agrega la hoja en ese caso). */
    public boolean tieneDatos(String json) {
        return Osteomuscular.leer(json).entrySet().stream()
                .anyMatch(e -> !"VERSION".equals(e.getKey()) && e.getValue() != null && !e.getValue().isBlank());
    }
}
