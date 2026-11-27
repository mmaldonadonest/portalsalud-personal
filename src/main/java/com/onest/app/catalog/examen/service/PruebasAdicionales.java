package com.onest.app.catalog.examen.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pruebas adicionales del examen inicial (26-nov-2026, pedido del usuario): la banda «Prueba de Ishihara» pasa a
 * llamarse «Pruebas adicionales» y, ademas de Ishihara, trae cuatro formatos de SM (Downloads, 26-nov):
 * FT-SO-08 respirador, FT-SO-10 skin prick test, FT-SO-01 cuestionario de alergias y FT-SO-14 cadena de custodia.
 *
 * <p>Cada uno pregunta «¿Lo requiere?»; con Si se despliega su captura. Como Ishihara, TODO se guarda en un tag de
 * SERV_MED_TAG por formato (JSON: APLICA = SI/NO + una clave por campo), sin tabla ni SQL. Opcionales: no cuentan en
 * el avance. Los formatos de SM traen varias columnas de fecha (seguimiento); aqui se captura la del examen.
 *
 * <p>PRELLENADO: el boton «Prellenado» pone los valores de {@link Prueba#prellenado()}. 26-nov: SM aun no define
 * cuales son, asi que van vacios y el boton sale deshabilitado; se llenan aqui cuando lo digan.
 */
public final class PruebasAdicionales {

    /**
     * SINO (SI/NO), TEXTO, NUM, OPCION (una de opciones), CHECKS (varias, separadas por coma), PE (papula + eritema).
     * {@code ejemplo}: texto gris dentro de la casilla (placeholder) para los TEXTO; no es un valor, no se guarda.
     */
    public record Campo(String clave, String etiqueta, String tipo, List<String> opciones, String grupo, String ejemplo) {
        Campo(String clave, String etiqueta, String tipo, List<String> opciones, String grupo) {
            this(clave, etiqueta, tipo, opciones, grupo, null);
        }
    }

    public record Prueba(String codigo, String tag, String titulo, List<Campo> campos, Map<String, String> prellenado) {
        /** Grupos en el orden del formato, con sus campos. */
        public Map<String, List<Campo>> getGrupos() {
            Map<String, List<Campo>> g = new LinkedHashMap<>();
            campos.forEach(c -> g.computeIfAbsent(c.grupo(), k -> new ArrayList<>()).add(c));
            return g;
        }

        /** JSON del prellenado para el boton (vacio = boton deshabilitado). */
        public String getPrellenadoJson() {
            try {
                return prellenado.isEmpty() ? "" : JSON.writeValueAsString(prellenado);
            } catch (Exception ex) {
                return "";
            }
        }
    }

    private static final com.fasterxml.jackson.databind.ObjectMapper JSON = new com.fasterxml.jackson.databind.ObjectMapper();

    public static final String TIPO_ITEM = "PRUEBA_ADIC";

    /** Seccion (y grupo) del modulo Examen medico con las 4 pruebas, sin Ishihara (27-nov-2026, periodico). */
    public static final String SECCION = "Pruebas adicionales";

    private static Campo sino(String clave, String etiqueta, String grupo) {
        return new Campo(clave, etiqueta, "SINO", List.of(), grupo);
    }

    private static Campo texto(String clave, String etiqueta, String grupo) {
        return new Campo(clave, etiqueta, "TEXTO", List.of(), grupo);
    }

    /** Casilla para anotar una medida con el ejemplo de SM en gris («70 x'», «120/80»). */
    private static Campo medida(String clave, String etiqueta, String grupo, String ejemplo) {
        return new Campo(clave, etiqueta, "TEXTO", List.of(), grupo, ejemplo);
    }

    /** 27-nov-2026 (Word SM): numero (% BASC); la casilla solo acepta digitos y punto. */
    private static Campo numero(String clave, String etiqueta, String grupo) {
        return new Campo(clave, etiqueta, "NUM", List.of(), grupo);
    }

    /** 27-nov-2026 (Word SM): hora con formato de hora (hh:mm). */
    private static Campo hora(String clave, String etiqueta, String grupo) {
        return new Campo(clave, etiqueta, "HORA", List.of(), grupo);
    }

    /** 27-nov-2026 (Word SM): fecha de realizacion; al contestar «Si» se pone la de hoy si esta vacia. */
    private static Campo fecha(String clave, String etiqueta, String grupo) {
        return new Campo(clave, etiqueta, "FECHA", List.of(), grupo);
    }

    /**
     * 27-nov-2026 (Word SM, respuesta del usuario: si): «Realizó» = nombre del usuario con la sesion abierta, sin
     * escribirlo. Solo lectura; lo ya guardado se conserva (es quien la realizo, aunque despues la abra otro).
     */
    private static Campo realizo() {
        return new Campo("REALIZO", "Nombre", "REALIZO", List.of(), "Realizó");
    }

    private static Campo opcion(String clave, String etiqueta, String grupo, String... opciones) {
        return new Campo(clave, etiqueta, "OPCION", List.of(opciones), grupo);
    }

    private static Campo checks(String clave, String etiqueta, String grupo, String... opciones) {
        return new Campo(clave, etiqueta, "CHECKS", List.of(opciones), grupo);
    }

    private static Campo pe(String clave, String etiqueta, String grupo) {
        return new Campo(clave, etiqueta, "PE", List.of(), grupo);
    }

    /** Textos y orden tal cual los formatos de SM (rev. 14-sep-2026). */
    public static final List<Prueba> PRUEBAS = List.of(
            new Prueba("FT-SO-08", "EXM_FTSO08", "Examen médico para uso de respirador", List.of(
                    texto("HISTORIA", "Historia médica relevante", "Historia médica"),
                    sino("CF_NORMAL", "Normal", "Configuración facial"),
                    sino("CF_BARBA", "Barba excedente", "Configuración facial"),
                    sino("CF_VELLO", "Vello facial", "Configuración facial"),
                    sino("CF_OTROS", "Otros", "Configuración facial"),
                    sino("SR_ACROCIANOSIS", "Acrocianosis", "Sistema respiratorio"),
                    sino("SR_CIANOSIS", "Cianosis", "Sistema respiratorio"),
                    sino("SR_MURMULLO", "Murmullo vesicular normal", "Sistema respiratorio"),
                    sino("SR_MUSCULOS", "Uso de músculos accesorios", "Sistema respiratorio"),
                    sino("CT_NORMAL", "Normal", "Configuración torácica"),
                    sino("CT_XIFOSIS", "Xifosis", "Configuración torácica"),
                    sino("CT_ESCOLIOSIS", "Escoliosis", "Configuración torácica"),
                    sino("CT_OTROS", "Otros", "Configuración torácica"),
                    sino("CT_RX", "Rx de tórax normal", "Configuración torácica"),
                    sino("AC_RITMO", "Ritmo / frecuencia", "Auscultación cardiaca"),
                    sino("AC_MURMULLO", "Murmullo normal", "Auscultación cardiaca"),
                    // 27-nov-2026 (SM, imagen del prellenado): FC, FR y presion son medidas, no Si/No: casilla para el
                    // numero, con el ejemplo de SM en gris. No se prellenan: se miden en cada trabajador
                    medida("AC_FC", "Frecuencia cardiaca", "Auscultación cardiaca", "Ej. 70 x'"),
                    sino("AC_OTROS", "Otros", "Auscultación cardiaca"),
                    sino("NM_NORMAL", "Normal", "Neuromuscular"),
                    sino("NM_PATOLOGIA", "Patología", "Neuromuscular"),
                    sino("NM_ESPIROMETRIA", "Espirometría normal", "Neuromuscular"),
                    medida("NM_FR", "Frecuencia respiratoria", "Neuromuscular", "Ej. 20'"),
                    medida("NM_PRESION", "Presión sanguínea", "Neuromuscular", "Ej. 120/80")),
                    // 27-nov-2026: prellenado de SM («TODO NORMAL», imagen del formato FT-SO-08)
                    Map.ofEntries(
                            Map.entry("HISTORIA", "SDPA - CLINICAMENTE SANO"),
                            Map.entry("CF_NORMAL", "SI"), Map.entry("CF_BARBA", "NO"), Map.entry("CF_VELLO", "NO"),
                            Map.entry("CF_OTROS", "NO"),
                            Map.entry("SR_ACROCIANOSIS", "NO"), Map.entry("SR_CIANOSIS", "NO"), Map.entry("SR_MURMULLO", "SI"),
                            Map.entry("SR_MUSCULOS", "NO"),
                            Map.entry("CT_NORMAL", "SI"), Map.entry("CT_XIFOSIS", "NO"), Map.entry("CT_ESCOLIOSIS", "NO"),
                            Map.entry("CT_OTROS", "NO"), Map.entry("CT_RX", "SI"),
                            Map.entry("AC_RITMO", "SI"), Map.entry("AC_MURMULLO", "SI"), Map.entry("AC_OTROS", "NO"),
                            Map.entry("NM_NORMAL", "SI"), Map.entry("NM_PATOLOGIA", "NO"), Map.entry("NM_ESPIROMETRIA", "SI"))),
            new Prueba("FT-SO-10", "EXM_FTSO10", "Registro de skin prick test (SPT)", List.of(
                    texto("INFO", "Información médica relevante", "Información médica"),
                    // P = papula, E = eritema; NAT y MAN vienen N/A en el formato pero se dejan capturables
                    pe("POS", "+ (control positivo)", "Resultados (P = pápula, E = eritema)"),
                    pe("SAB", "SAB", "Resultados (P = pápula, E = eritema)"),
                    pe("LIP", "LIP", "Resultados (P = pápula, E = eritema)"),
                    pe("NAT", "NAT", "Resultados (P = pápula, E = eritema)"),
                    pe("STA", "STA", "Resultados (P = pápula, E = eritema)"),
                    pe("FNA", "FNA", "Resultados (P = pápula, E = eritema)"),
                    pe("MAN", "MAN", "Resultados (P = pápula, E = eritema)"),
                    pe("NEG", "− (control negativo)", "Resultados (P = pápula, E = eritema)"),
                    sino("MED", "Ingesta de medicamento actual", "Medicamento"),
                    texto("MED_CUAL", "¿Cuál en caso positivo?", "Medicamento"),
                    texto("INICIALES", "Iniciales de quién realiza", "Realizó")), Map.of()),
            new Prueba("FT-SO-01", "EXM_FTSO01", "Cuestionario de alergias", List.of(
                    sino("PADECE", "¿Padece alguna enfermedad alérgica como asma, rinitis, dermatitis, etc.?", "Preguntas"),
                    texto("PADECE_ESP", "Especifique", "Preguntas"),
                    sino("MEDICAMENTO", "¿Actualmente toma algún medicamento?", "Preguntas"),
                    texto("MEDICAMENTO_ESP", "Especifique", "Preguntas"),
                    sino("ALERGICO", "¿Es usted alérgico a algún medicamento, alimento, animal, planta, material u otros?", "Preguntas"),
                    texto("ALERGICO_ESP", "Especifique", "Preguntas"),
                    checks("SINTOMAS", "En caso de ser alérgico, ¿qué síntomas presenta?", "Síntomas",
                            "COMEZÓN EN OJOS", "ENROJECIMIENTO DE OJOS", "LAGRIMEO", "PRURITO NASAL", "RINORREA/RINITIS",
                            "ESTORNUDOS", "URTICARIA", "ECCEMA", "DISNEA", "BRONCOESPASMO"),
                    realizo()),
                    // 27-nov-2026 (Word SM «cambioserrores27112026»): «Todo normal» = No en las 3 preguntas
                    Map.of("PADECE", "NO", "MEDICAMENTO", "NO", "ALERGICO", "NO")),
            new Prueba("FT-SO-14", "EXM_FTSO14", "Cadena de custodia", List.of(
                    texto("FOLIO", "Folio", "Identificación"),
                    texto("ID_TIPO", "Tipo de identificación", "Identificación"),
                    texto("ID_EMITIDA", "Emitida por", "Identificación"),
                    texto("ID_NUMERO", "No. de identificación", "Identificación"),
                    opcion("RAZON", "Razón de la prueba", "Prueba", "ALEATORIA", "PERIÓDICO", "OTRO"),
                    checks("ELEMENTOS", "Elementos a detectar", "Prueba", "AMP", "COC", "OPI", "THC", "MET", "BENZO", "BARB"),
                    // 27-nov-2026 (Word SM «cambioserrores27112026»): BASC solo numero, hora con formato de hora,
                    // resultado POSITIVO o NEGATIVO + casilla opcional de la sustancia, y la fecha de realizacion
                    numero("BASC", "% BASC (porcentaje de alcohol en aire espirado)", "Prueba"),
                    sino("TRATAMIENTO", "¿Se encuentra bajo tratamiento médico?", "Tratamiento médico"),
                    texto("DX", "Diagnóstico actual", "Tratamiento médico"),
                    texto("MEDICAMENTO", "Medicamento prescrito", "Tratamiento médico"),
                    texto("RECETADO", "Recetado por", "Tratamiento médico"),
                    fecha("FECHA", "Fecha de realización", "Toma de la muestra"),
                    hora("HORA", "Hora", "Toma de la muestra"),
                    texto("LUGAR", "Lugar de la recolección", "Toma de la muestra"),
                    opcion("RESULTADO", "Resultado", "Toma de la muestra", "POSITIVO", "NEGATIVO"),
                    // casilla chica (SM: «el cuadrito más pequeño, opcional»): etiqueta corta = ancho normal
                    texto("SUSTANCIA", "Sustancia si es positivo (opcional)", "Toma de la muestra"),
                    realizo()),
                    // 27-nov-2026 (usuario): «Todo normal» = No en tratamiento medico; lo demas son datos de la persona
                    Map.of("TRATAMIENTO", "NO")));

    /** Para las plantillas: th:with="PA=${T(...PruebasAdicionales).INSTANCIA}". */
    public static final PruebasAdicionales INSTANCIA = new PruebasAdicionales();

    private PruebasAdicionales() {
    }

    public Prueba prueba(String tag) {
        return PRUEBAS.stream().filter(p -> p.tag().equals(tag)).findFirst().orElse(null);
    }

    public String v(String json, String clave) {
        return Osteomuscular.valor(json, clave);
    }

    /** "SI", "NO" o "" (sin contestar). */
    public String aplica(String json) {
        String a = Osteomuscular.valor(json, "APLICA");
        return "SI".equals(a) || "NO".equals(a) ? a : "";
    }

    /** true si lo capturado ya tiene TODOS los valores de «Todo normal» (el interruptor abre encendido). */
    public boolean esNormal(String json, Prueba p) {
        return p != null && !p.prellenado().isEmpty()
                && p.prellenado().entrySet().stream().allMatch(e -> e.getValue().equals(v(json, e.getKey())));
    }

    /** true si la opcion esta marcada en un campo CHECKS (lista separada por coma). */
    public boolean marcado(String json, String clave, String opcion) {
        for (String s : Osteomuscular.valor(json, clave).split(",")) {
            if (s.trim().equals(opcion)) {
                return true;
            }
        }
        return false;
    }
}
