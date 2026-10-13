package com.onest.excel.nss;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.onest.excel.normalizacion.Normalizador;

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
        CATALOGOS.put("AREA", List.of("STAFF", "CALIDAD", "CAPACITACION", "COMEDOR", "DESPACHO", "EMBARQUES",
                "INVENTARIOS", "LOCKERS", "MTTO", "OPERACION", "PATIO", "PREVENCION / SEG TRANSPORTE",
                "REC HUMANOS / RECLUTAMIENTO", "RECUPERADO", "SALUD OCUP", "SEG E HIG",
                "SERV GRAL / LIMPIEZA", "SISTEMAS", "TRAFICO"));
        CATALOGOS.put("RAMO_ATENCIONES", List.of("ENF GRAL", "SEGUIMIENTO SALUD", "PASE SALIDA", "RT"));
        CATALOGOS.put("CAUSA_CONSULTA", List.of("ACIDO PEPTICA", "ALERGIA / INTOXICACION", "BUCODENTAL",
                "CIRCULATORIO", "CURACION", "DERMATOLOGICO", "DIGESTIVO", "EMBARAZO (DETECCION/CONTROL)",
                "ENDOCRINO", "GENITOURINARIO", "GINECOLOGICO", "INYECCION", "NEUROLOGICO", "OFTALMICO",
                "OTICO", "PSICOSOMATICO", "RESPIRATORIO", "TENSION ARTERIAL (DETEC/CONTROL)",
                "TOMA DE GLUCOSA", "VACUNA / METODO PF / SEG SALUD", "FISIOTERAPIA", "NOM 035",
                "MT - ALGIA / MIALGIA /CONTUSION", "MP - ALGIA / MIALGIA /CONTUSION",
                "GRAL - ALGIA / MIALGIA /CONTUSION", "CERVICAL / DORSAL / LUMBAR"));
        CATALOGOS.put("LESION_ME", List.of("AMPUTACION MT", "AMPUTACION MP", "ALGIA / MIALGIA / CONTUSION MT",
                "ALGIA / MIALGIA / CONTUSION MP", "CERVICALGIA / DORSALGIA / LUMBALGIA",
                "CONTUSION/TRAUMA CABEZA", "CONTUSION/TRAUMA TRONCO", "ESGUINCE MT", "ESGUINCE MP",
                "FX MT", "FX MP", "FX OTRO", "LUXACION MT", "LUXACION MP", "POLICONTUNDIDO"));
        CATALOGOS.put("TIPO_EXAMEN_INGRESO", List.of("EMNI", "PRETEST"));
        CATALOGOS.put("DICTAMEN", List.of("APTO", "APTO_CONDICIONADO", "APTO_RESTRINGIDO", "NO_APTO"));
        CATALOGOS.put("GENERO", List.of("FEM", "MASC"));
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

    /** Forma oficial del valor si esta en el catalogo; vacio si no. */
    public static Optional<String> buscar(String catalogo, String valor) {
        List<String> lista = CATALOGOS.get(catalogo);
        if (lista == null || valor == null) {
            return Optional.empty();
        }
        String buscado = clave(valor);
        return lista.stream().filter(v -> clave(v).equals(buscado)).findFirst();
    }

    /** Para el mensaje de error: los valores validos, abreviados si son muchos. */
    public static String validos(String catalogo) {
        List<String> lista = CATALOGOS.getOrDefault(catalogo, List.of());
        return lista.size() <= 8 ? String.join(", ", lista)
                : String.join(", ", lista.subList(0, 8)) + "... (ver pestana CATALOGOS)";
    }

    /** Normaliza para comparar: sin acentos, mayusculas, sin espacios alrededor de "/" ni dobles. */
    static String clave(String s) {
        String t = Normalizador.texto(s);
        return t == null ? "" : t.replaceAll("\\s*/\\s*", "/").replaceAll("\\s+", " ").trim();
    }
}
