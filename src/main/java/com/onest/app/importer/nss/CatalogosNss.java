package com.onest.app.importer.nss;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Catalogos cerrados de los layouts, copiados de la pestana CATALOGOS de docs/09102026/ (y de
 * docs/09102026/corregidos/ para pos incapacidad y antidoping).
 *
 * <p>Se comparan NORMALIZADOS (mayusculas, sin acentos, espacios simples, sin espacios junto a
 * "/"), asi que "Enf. gral" no pasa pero "enf gral" o "ENF  GRAL" si, y se guarda la forma oficial.
 * Un valor fuera del catalogo es un error del renglon: la grafica filtra por igualdad exacta y un
 * valor raro se volveria una barra suelta.
 */
public final class CatalogosNss {

    private static final Map<String, List<String>> CATALOGOS = new LinkedHashMap<>();

    static {
        CATALOGOS.put("PREDIO", List.of("AIFA", "ATIZAPAN", "CHARCON", "MACRO I", "MACRO II", "MERCURIO",
                "MIKELS", "U TEPALCAPA", "FLORA", "SIGLO XXI", "TOLUCA", "TULTIPARK", "WORLD PARK",
                "Z VALLEJO", "SMO", "FORANEO", "KILOTON", "MALTA"));
        // CLIENTE / EXTERNO: 1,227 renglones de 1 ATENCIONES. Su pestana CATALOGOS decia EXTERNO; el
        // servicio medico eligio CLIENTE / EXTERNO (P1 de preguntas-catalogos-atenciones-sm-v6, 2-nov-2026)
        CATALOGOS.put("AREA", List.of("STAFF", "CALIDAD", "CAPACITACION", "CLIENTE / EXTERNO", "COMEDOR", "DESPACHO", "EMBARQUES",
                "INVENTARIOS", "LOCKERS", "MTTO", "OPERACION", "PATIO", "PREVENCION / SEG TRANSPORTE",
                "REC HUMANOS / RECLUTAMIENTO", "RECUPERADO", "SALUD OCUP", "SEG E HIG",
                "SERV GRAL / LIMPIEZA", "SISTEMAS", "TRAFICO"));
        // SEG SALUD y no SEGUIMIENTO SALUD (P2, 2-nov-2026): asi lo dice su catalogo, aunque en los
        // renglones lo capturen completo (EQUIVALENCIAS)
        // PASE MEDICO (12-nov-2026, decision del usuario): reemplaza a PASE SALIDA, como dice la pestana
        // CATALOGOS del formato 2026 de SM. PASE SALIDA se rechaza diciendo que se use PASE MEDICO.
        CATALOGOS.put("RAMO_ATENCIONES", List.of("ENF GRAL", "SEG SALUD", "PASE MEDICO", "RT"));
        CATALOGOS.put("CAUSA_CONSULTA", List.of("ACIDO PEPTICA", "ALERGIA / INTOXICACION", "BUCODENTAL",
                "CIRCULATORIO", "CURACION", "DERMATOLOGICO", "DIGESTIVO", "EMBARAZO (DETECCION/CONTROL)",
                "ENDOCRINO", "GENITOURINARIO", "GINECOLOGICO", "INYECCION", "NEUROLOGICO", "OFTALMICO",
                "OTICO", "PSICOSOMATICO", "RESPIRATORIO", "TENSION ARTERIAL (DETEC/CONTROL)",
                "TOMA DE GLUCOSA", "VACUNA / METODO PF / SEG SALUD", "FISIOTERAPIA", "NOM 035",
                "MT - ALGIA / MIALGIA /CONTUSION", "MP - ALGIA / MIALGIA /CONTUSION",
                "GRAL - ALGIA / MIALGIA /CONTUSION", "CERVICAL / DORSAL / LUMBAR",
                // 14-oct-2026: decision del usuario, catalogo oficial = el del Excel + CEFALEA (los
                // medicos la capturan aparte de NEUROLOGICO en el Excel y en el portal)
                "CEFALEA"));
        CATALOGOS.put("LESION_ME", List.of("AMPUTACION MT", "AMPUTACION MP", "ALGIA / MIALGIA / CONTUSION MT",
                "ALGIA / MIALGIA / CONTUSION MP", "CERVICALGIA / DORSALGIA / LUMBALGIA",
                "CONTUSION/TRAUMA CABEZA", "CONTUSION/TRAUMA TRONCO", "ESGUINCE MT", "ESGUINCE MP",
                "FX MT", "FX MP", "FX OTRO", "LUXACION MT", "LUXACION MP", "POLICONTUNDIDO"));
        CATALOGOS.put("TIPO_EXAMEN_INGRESO", List.of("EMNI", "PRETEST"));
        CATALOGOS.put("DICTAMEN", List.of("APTO", "APTO_CONDICIONADO", "APTO_RESTRINGIDO", "NO_APTO"));
        CATALOGOS.put("GENERO", List.of("FEM", "MASC"));
        // Rango de edad de nuevo ingreso (14-oct-2026): los mismos cortes que marcan los Excel
        // originales y que usa la grafica de edades del portal (Normalizador.rangoEdad)
        CATALOGOS.put("RANGO_EDAD", List.of("18-25", "26-35", "36-45", "46-55", "55+"));
        CATALOGOS.put("RUBRO_INCAP", List.of("IMSS", "INTERNA"));
        CATALOGOS.put("RAMO_INCAPACIDADES", List.of("EG", "MAT", "RT1", "RT2"));
        CATALOGOS.put("TIPO_INCAP", List.of("I", "S", "MAT"));
        CATALOGOS.put("SI_NO", List.of("SI", "NO"));
        CATALOGOS.put("TIPO_RIESGO", List.of("LABORAL", "TRAYECTO"));
        CATALOGOS.put("CAUSA_RT", List.of("ACCID VIAL", "AGRESION / ASALTO", "CAIDA",
                "GOLPE DIRECTO CON O POR OBJETO", "HERIDA", "LESION ERGONOMICA", "OTRO (IMPROCEDENTE)"));
        CATALOGOS.put("ESTATUS_ACCIDENTE", List.of("PENDIENTE", "CALIFICADO", "IMPROCEDENTE", "BAJA INCONCLUSO"));
        CATALOGOS.put("STATUS_CASO", List.of("PENDIENTE", "CONCLUIDO", "BAJA"));

        // 12-oct-2026, de docs/09102026/corregidos/ (pos incapacidad y antidoping)
        // "RT 1"/"RT 2" con espacio, como la plantilla; en incapacidades es "RT1"/"RT2" y la vista
        // los iguala al leer.
        CATALOGOS.put("CAUSA_EXAMEN_POS_INCAP", List.of("EG", "MAT", "RT 1", "RT 2", "INTERNA"));
        CATALOGOS.put("TIPO_PRUEBA", List.of("ANTIDOPING", "ALCOHOLEMIA"));
        CATALOGOS.put("RESULTADO_PRUEBA", List.of("NEGATIVO", "POSITIVO"));
        CATALOGOS.put("SUSTANCIA", List.of("AMF", "BARB", "BENZO", "COC", "LSD", "MET", "OPI", "PVC", "THC",
                "SOLVENTE", "OTRO"));
        CATALOGOS.put("CONCLUSION_ANTIDOPING", List.of("N/A", "CAPA", "NO INTERESADO / BAJA", "NO CONTRATADO",
                "RECAIDA"));
        CATALOGOS.put("STATUS_ANTIDOPING", List.of("LABORANDO", "BAJA / NO CONTRATADO"));

        // 12-oct-2026: maternidad
        CATALOGOS.put("ESTATUS_LABORAL", List.of("LABORANDO EMBARAZADA", "INCAPACIDAD", "LACTANCIA", "LABORANDO",
                "TERMINO", "BAJA", "ABORTO"));

        // 12-oct-2026: consumibles (medicamentos y material fijo). DESCRIPCION del medicamento queda
        // como texto: su lista depende de la categoria y cambia con lo que se compra.
        CATALOGOS.put("MES", ValidadorNss.MESES);
        CATALOGOS.put("CATEGORIA_MEDICAMENTO", List.of("VIA ORAL", "INYECTABLE", "TOPICO", "OFTALMICO", "OTICO",
                "ANTIBIOTICO INYECTABLE", "ANTIBIOTICO ORAL", "OTROS", "ESPECIFICO", "MATERIAL", "URGENCIA"));
        CATALOGOS.put("EQUIPO", List.of("ALCOHOLIMETRO", "BAUMANOMETRO", "COLLARIN RIGIDO", "COLLARIN SUAVE", "DEA",
                "ESTETOSCOPIO", "ESTUCHE DE DIAGNOSTICO (OTOSCOPIO)", "ESTUCHE DE DISECCION", "GLUCOMETRO",
                "INMOVILIZADOR PARA DEDO", "LAMPARA MANUAL", "MALETA P/ BOTIQUIN", "OTOSCOPIO", "OXIMETRO",
                "SILLA DE RUEDAS", "TANQUE DE OXIGENO", "TERMOMETRO DIGITAL", "RIÑON ACERO INOXIDABLE"));
        CATALOGOS.put("MOTIVO_RECEPCION", List.of("INICIAL", "CAMBIO", "DAÑO", "AUMENTO"));
    }

    private CatalogosNss() {
    }

    /**
     * Variantes que se capturan con otro nombre y equivalen a un valor oficial (14-oct-2026). Se
     * guardan como el oficial y el validador deja un AVISO, para que se vea que se tradujo.
     * Clave: catalogo -> variante (como la escriben) -> valor oficial.
     *
     * <p>Las P1-P7 de SM ya NO se traducen: van en NO_ELEGIDOS (3-nov-2026).
     */
    private static final Map<String, Map<String, String>> EQUIVALENCIAS = Map.of(
            "CAUSA_CONSULTA", Map.of(
                    "OFTALMOLOGICA", "OFTALMICO",
                    "OFTALMOLOGICO", "OFTALMICO"),
            // 12-nov-2026: genero de atenciones; SM no ha dicho como lo captura. Las formas vistas en el
            // biometrico y en los Excel (BiometricoNss.genero): 1/2, H/F, HOMBRE/MUJER, MASCULINO/FEMENINO.
            // "M" NO: es ambigua (masculino o mujer) y se rechaza con su propio mensaje (ambiguo()).
            "GENERO", Map.of(
                    "F", "FEM", "MUJER", "FEM", "FEMENINO", "FEM", "2", "FEM",
                    "H", "MASC", "HOMBRE", "MASC", "MASCULINO", "MASC", "1", "MASC"),
            // 12-nov-2026: el formato 2026 de SM escribe el rango con espacios ("18 - 25", "55 +")
            "RANGO_EDAD", Map.of(
                    "18 - 25", "18-25", "26 - 35", "26-35", "36 - 45", "36-45", "46 - 55", "46-55", "55 +", "55+"));

    /**
     * Catalogos cuya equivalencia entra sin aviso por renglon: el genero y el rango de edad vienen en TODOS los renglones y un
     * aviso por cada uno taparia los avisos que si importan en «Entran con ajuste».
     */
    public static boolean avisaEquivalencia(String catalogo) {
        return !"GENERO".equals(catalogo) && !"RANGO_EDAD".equals(catalogo);
    }

    /** Valor que no se puede traducir sin adivinar: el motivo del rechazo (vacio si no es el caso). */
    public static Optional<String> ambiguo(String catalogo, String valor) {
        if ("GENERO".equals(catalogo) && "M".equals(clave(valor))) {
            return Optional.of("«M» es ambiguo (masculino o mujer): usar FEM o MASC, F o H, MUJER u HOMBRE");
        }
        return Optional.empty();
    }

    /**
     * Respuestas del servicio medico a preguntas-catalogos-atenciones-sm-v6 ("cual valor se toma",
     * columna amarilla, 2-nov-2026). Regla del 3-nov-2026: se EXIGE el valor elegido; el otro nombre
     * no se traduce ni se cuenta, se rechaza diciendo cual usar. Clave: catalogo -> descartado -> elegido.
     */
    private static final Map<String, Map<String, String>> NO_ELEGIDOS = Map.of(
            "AREA", Map.of(
                    "EXTERNO", "CLIENTE / EXTERNO"),                          // P1
            "RAMO_ATENCIONES", Map.of(
                    "SEGUIMIENTO SALUD", "SEG SALUD",                         // P2
                    "PASE SALIDA", "PASE MEDICO",                             // 12-nov-2026
                    "PASE DE SALIDA", "PASE MEDICO"),
            "LESION_ME", Map.of(
                    "ALGIA / CONTUSION MT", "ALGIA / MIALGIA / CONTUSION MT", // P3
                    "ALGIA / CONTUSION MP", "ALGIA / MIALGIA / CONTUSION MP"),// P4
            "CAUSA_CONSULTA", Map.of(
                    "INYECCIONES", "INYECCION",                               // P5
                    "EMBARAZO", "EMBARAZO (DETECCION/CONTROL)"),              // P6
            "CAUSA_RT", Map.of(
                    "IMPROCEDENTE", "OTRO (IMPROCEDENTE)"));                  // P7

    /** Si el valor es el nombre que SM descarto: el que eligio (para decirlo en el rechazo). */
    public static Optional<String> elegidoEnLugarDe(String catalogo, String valor) {
        String buscado = clave(valor);
        return NO_ELEGIDOS.getOrDefault(catalogo, Map.of()).entrySet().stream()
                .filter(e -> clave(e.getKey()).equals(buscado))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    /** Forma oficial del valor si esta en el catalogo (o es una variante conocida); vacio si no. */
    public static Optional<String> buscar(String catalogo, String valor) {
        List<String> lista = CATALOGOS.get(catalogo);
        if (lista == null || valor == null) {
            return Optional.empty();
        }
        String buscado = clave(valor);
        Optional<String> oficial = lista.stream().filter(v -> clave(v).equals(buscado)).findFirst();
        if (oficial.isPresent()) {
            return oficial;
        }
        return EQUIVALENCIAS.getOrDefault(catalogo, Map.of()).entrySet().stream()
                .filter(e -> clave(e.getKey()).equals(buscado))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    /** Para el mensaje de error: los valores validos, abreviados si son muchos. */
    public static String validos(String catalogo) {
        List<String> lista = CATALOGOS.getOrDefault(catalogo, List.of());
        return lista.size() <= 8 ? String.join(", ", lista)
                : String.join(", ", lista.subList(0, 8)) + "...";
    }

    /** Normaliza para comparar: sin acentos, mayusculas, sin espacios alrededor de "/" ni dobles. */
    static String clave(String s) {
        if (s == null) {
            return "";
        }
        // Mismo criterio que Normalizador.texto del cargador de consola (no se trajo esa clase al portal)
        // Espacio duro (U+00A0, sale al copiar de web o Word): \s no lo reconoce y "ENF GRAL" no empataria
        String t = java.text.Normalizer.normalize(s.replace(' ', ' '), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "").toUpperCase(java.util.Locale.ROOT);
        return t.replaceAll("\\s*/\\s*", "/").replaceAll("\\s+", " ").trim();
    }
}
