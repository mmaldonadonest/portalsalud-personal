package com.onest.app.catalog.dashboard.service;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Clasifica una consulta como lesion musculoesqueletica a partir de la clave CIE-10 que
 * encabeza el diagnostico ("M54.5 - Lumbago") y, cuando la clave no dice la region
 * (capitulo M), del area involucrada capturada en la consulta.
 *
 * <p>Reproduce la agrupacion clinica del prototipo (cambios/Dashboard Salud, pantalla
 * Musculoesqueleticas): tipo (Algia/contusion MT, Cervicalgia/dorsalgia/lumbalgia,
 * Esguince MP, Fractura...) + region (Miembro toracico, Miembro pelvico, Columna,
 * Cabeza/tronco, Multiple) + grupo para los 4 KPIs (algias, columna, traumatismos,
 * fracturas/amputaciones). Sin clave CIE-10 la consulta NO se clasifica: no se adivina
 * a partir de la causa en texto libre.
 *
 * <p>Alcance CIE-10: capitulo M (enfermedades del sistema osteomuscular), capitulo S
 * (traumatismos por region: S0x cabeza ... S9x tobillo/pie, segundo digito = tipo de
 * lesion) y T00-T14 (traumatismos de regiones multiples / no especificadas).
 */
final class LesionMusculoesqueletica {

    static final String MT = "Miembro torácico";
    static final String MP = "Miembro pélvico";
    static final String COLUMNA = "Columna";
    static final String CABEZA_TRONCO = "Cabeza/tronco";
    static final String MULTIPLE = "Múltiple";

    enum Grupo { ALGIA, COLUMNA, TRAUMATISMO, FRACTURA }

    record Clasificacion(String tipo, String region, Grupo grupo) {
    }

    private static final Pattern CLAVE = Pattern.compile("^\\s*([A-Za-z])(\\d)(\\d)");

    private LesionMusculoesqueletica() {
    }

    static Optional<Clasificacion> clasificar(String diagnostico, String areaInvolucrada) {
        if (diagnostico == null) {
            return Optional.empty();
        }
        Matcher m = CLAVE.matcher(diagnostico);
        if (!m.find()) {
            return Optional.empty();
        }
        char capitulo = Character.toUpperCase(m.group(1).charAt(0));
        int d1 = m.group(2).charAt(0) - '0';
        int d2 = m.group(3).charAt(0) - '0';
        return switch (capitulo) {
            case 'M' -> Optional.of(capituloM(d1 * 10 + d2, areaInvolucrada));
            case 'S' -> Optional.of(capituloS(d1, d2));
            case 'T' -> d1 == 0 || d1 == 1 ? Optional.of(capituloT(d1 * 10 + d2)) : Optional.empty();
            default -> Optional.empty();
        };
    }

    /** M40-M54 dorsopatias -> columna; el resto (artropatias, tejidos blandos) toma la region del area involucrada. */
    private static Clasificacion capituloM(int codigo, String areaInvolucrada) {
        if (codigo >= 40 && codigo <= 54) {
            return new Clasificacion("Cervicalgia/dorsalgia/lumbalgia", COLUMNA, Grupo.COLUMNA);
        }
        String region = regionDeArea(areaInvolucrada);
        return switch (region) {
            case MT -> new Clasificacion("Algia/contusión MT", MT, Grupo.ALGIA);
            case MP -> new Clasificacion("Algia/contusión MP", MP, Grupo.ALGIA);
            case COLUMNA -> new Clasificacion("Cervicalgia/dorsalgia/lumbalgia", COLUMNA, Grupo.COLUMNA);
            default -> new Clasificacion("Algia/trastorno " + region.toLowerCase(), region, Grupo.ALGIA);
        };
    }

    /**
     * Sxy: x = region (0 cabeza, 1 cuello, 2 torax, 3 abdomen/lumbar/pelvis, 4-6 miembro
     * toracico, 7-9 miembro pelvico); y = tipo (0 contusion, 1 herida, 2 fractura, 3
     * luxacion/esguince, 4 nervio, 5 vaso, 6 musculo/tendon, 7 aplastamiento, 8 amputacion,
     * 9 otro). Los esguinces/fracturas de cuello, torax y lumbar (S13/S23/S33, S12/S22/S32)
     * se cuentan como columna.
     */
    private static Clasificacion capituloS(int region, int tipo) {
        boolean tronco = region <= 3;
        boolean columna = region >= 1 && region <= 3 && (tipo == 2 || tipo == 3);
        String reg = columna ? COLUMNA : tronco ? CABEZA_TRONCO : region <= 6 ? MT : MP;
        String sufijo = reg.equals(MT) ? " MT" : reg.equals(MP) ? " MP" : "";
        String zona = region == 0 ? "cabeza" : "tronco";
        return switch (tipo) {
            case 0 -> tronco
                    ? new Clasificacion("Contusión/trauma " + zona, CABEZA_TRONCO, Grupo.TRAUMATISMO)
                    : new Clasificacion("Algia/contusión" + sufijo, reg, Grupo.ALGIA);
            case 2 -> columna
                    ? new Clasificacion("Fractura vertebral", COLUMNA, Grupo.FRACTURA)
                    : new Clasificacion("Fractura" + (tronco ? " " + zona : sufijo), reg, Grupo.FRACTURA);
            case 3 -> columna
                    ? new Clasificacion("Esguince cervical/dorsal/lumbar", COLUMNA, Grupo.COLUMNA)
                    : new Clasificacion("Esguince/luxación" + sufijo, reg, Grupo.TRAUMATISMO);
            case 6 -> new Clasificacion("Lesión muscular/tendón" + sufijo, reg, Grupo.TRAUMATISMO);
            case 8 -> new Clasificacion("Amputación traumática" + sufijo, reg, Grupo.FRACTURA);
            default -> new Clasificacion((tronco ? "Traumatismo " + zona : "Traumatismo" + sufijo), reg, Grupo.TRAUMATISMO);
        };
    }

    /** T00-T07 regiones multiples, T08-T14 region no especificada. */
    private static Clasificacion capituloT(int codigo) {
        return switch (codigo) {
            case 2, 12 -> new Clasificacion("Fracturas múltiples", MULTIPLE, Grupo.FRACTURA);
            case 5 -> new Clasificacion("Amputaciones múltiples", MULTIPLE, Grupo.FRACTURA);
            case 3, 13 -> new Clasificacion("Esguinces/luxaciones múltiples", MULTIPLE, Grupo.TRAUMATISMO);
            default -> new Clasificacion("Policontundido", MULTIPLE, Grupo.TRAUMATISMO);
        };
    }

    /** Catalogo de area involucrada del formulario de consulta (fragments/consulta-form.html). */
    static String regionDeArea(String area) {
        if (area == null) {
            return MULTIPLE;
        }
        String a = area.trim().toLowerCase();
        if (a.contains("superior")) {
            return MT;
        }
        if (a.contains("inferior")) {
            return MP;
        }
        if (a.contains("cuello")) {
            return COLUMNA;
        }
        if (a.contains("cabeza") || a.contains("torax") || a.contains("tórax") || a.contains("abdomen")) {
            return CABEZA_TRONCO;
        }
        return MULTIPLE;
    }
}
