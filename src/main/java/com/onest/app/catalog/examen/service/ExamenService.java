package com.onest.app.catalog.examen.service;

import com.onest.app.catalog.examen.client.ExamenClient;
import com.onest.app.catalog.examen.repository.ExamenHistoricoRepository;
import com.onest.app.catalog.pretest.repository.MedTagRepository;
import com.onest.app.catalog.examen.dto.ExamItem;
import com.onest.app.catalog.examen.dto.ExamenReporteDto;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Examen medico (U07) - Slices 1-2: heredofamiliares + APNP / Inmunizaciones / AGO.
 * Carga los datos del WS (consulta_examen, aplanados) y arma cada seccion para la vista.
 * El guardado completo se implementa en un slice posterior (evita clobber parcial).
 */
@Service
public class ExamenService {

    // type: SINO (Si/No + OBS opcional) | TEXT | DATE
    // tag: si no es null, el campo se lee/escribe en SERV_MED_TAG con ese TYPE (no en el WS) y
    // field es la columna del WS que se muestra como respaldo cuando el tag esta vacio.
    private record Campo(String field, String label, String type, boolean hasObs, String tag) {
        Campo(String field, String label, String type, boolean hasObs) {
            this(field, label, type, hasObs, null);
        }
    }

    private record Seccion(String dataKey, List<Campo> campos) {
    }

    private static Campo sino(String f, String l) {
        return new Campo(f, l, "SINO", true);
    }

    private static Campo sino0(String f, String l) {
        return new Campo(f, l, "SINO", false);
    }

    private static Campo text(String f, String l) {
        return new Campo(f, l, "TEXT", false);
    }

    private static Campo date(String f, String l) {
        return new Campo(f, l, "DATE", false);
    }

    /**
     * DOSIS (16-oct-2026): numero de dosis, botones Ninguna/1/2/3 como el FT-SO-17. COVID es
     * NUMBER y el PHP lo capturaba como numero; medido en prod: 3 dosis 1,417, 2 dosis 759,
     * 4-6 dosis 121. Como Si/No, guardar convertia esas dosis en 0.
     */
    private static Campo dosis(String f, String l) {
        return new Campo(f, l, "DOSIS", false);
    }

    /**
     * SINO_TEXTO (16-oct-2026): No / Si + detalle en UN campo de texto. Pension en el PHP era texto
     * libre; medido en prod (tag pension): ~2,470 variantes de "no" (NO, NEGADO, NINGUNO, N/A...),
     * 740 vacios y ~35 con el tramite escrito ("ST7 CERRADO 2019"). Se guarda "NO" o el detalle
     * (o "SI" sin detalle); lo ya capturado no se reescribe hasta que se toque.
     */
    private static Campo sinoTexto(String f, String l) {
        return new Campo(f, l, "SINO_TEXTO", false);
    }

    private static Campo area(String f, String l) {
        return new Campo(f, l, "AREA", false);
    }

    /**
     * OPCION (13-oct-2026): una sola opcion entre botones, como el select del PHP
     * (formsEFis/EFis.html) y los chips del examen inicial. El texto elegido se guarda en ESA
     * columna; las opciones viven en examen-seccion.html.
     */
    private static Campo opcion(String f, String l) {
        return new Campo(f, l, "OPCION", false);
    }

    /**
     * OCULTO: columnas que la tabla tiene pero el PHP nunca lleno (MESOMORFO, ENDOMORFICA,
     * INTERES...): la opcion elegida se guardaba solo en la primera columna del grupo. No se
     * muestran; viajan en un hidden con su valor para que guardar no borre lo que hubiera.
     */
    private static Campo oculto(String f, String l) {
        return new Campo(f, l, "OCULTO", false);
    }

    /**
     * Texto guardado en SERV_MED_TAG (13-oct-2026). Oidos: el PHP capturaba en los tags
     * ODCAEINPEXP... y mandaba vacias las columnas de SERV_MED_OIDOS (su JS buscaba controles
     * que no existen); el impreso lee los tags. Medido 13-oct: ~3,000 NSS con tag vs 11 filas
     * con dato en SERV_MED_OIDOS. respaldo = columna del WS que se muestra si el tag esta vacio.
     */
    private static Campo tagTexto(String tag, String l, String respaldo) {
        return new Campo(respaldo, l, "TEXT", false, tag);
    }

    /**
     * Texto que solo existe en SERV_MED_TAG, sin columna del WS (15-oct-2026, FT-SO-04 rev. 05).
     * La clave de respaldo no existe en el WS, asi que lee vacio si no hay tag.
     */
    private static Campo tagSolo(String tag, String l) {
        return new Campo(tag, l, "TEXT", false, tag);
    }

    /** Fecha que solo existe en SERV_MED_TAG (como tagSolo, pero con control de fecha). */
    private static Campo tagFecha(String tag, String l) {
        return new Campo(tag, l, "DATE", false, tag);
    }

    /** Prefijo de las claves de formulario que van a SERV_MED_TAG y no al WS. */
    private static final String PREFIJO_TAG = "TAG.";
    private static final String GRUPO_TAG_FISICO = "EXAMEN_FISICO";

    private static final Map<String, Seccion> CATALOGO = new LinkedHashMap<>();

    static {
        // ---- Antecedentes laborales (resumen): ver docs/contextoWS.txt:4871-4889
        // (SERV_MED_ANT_LABORALES). A diferencia del resto del catalogo, este bloque del WS
        // lee/escribe las claves en MINUSCULA (SERV_ANTECEDENTESLAB.edad_inicio_laboral, no
        // .EDAD_INICIO_LABORAL) - no es un typo nuestro, es el WS real. VERIFICADO 2026-08-10
        // contra datos reales: 3208/3208 filas de SERV_MED_ANT_LABORALES tienen CANTIDAD_TRABAJOS
        // y PENSION, 3207/3208 tienen EDAD_INICIO_LABORAL - las claves en minuscula si funcionan.
        CATALOGO.put("Antecedentes laborales (resumen)", new Seccion("SERV_ANTECEDENTESLAB", List.of(
                text("edad_inicio_laboral", "Edad al iniciar a laborar"),
                text("cantidad_trabajos", "¿Cuántos trabajos ha tenido?"),
                sinoTexto("pension", "Pensión IMSS o trámite pendiente (ST2/ST3/ST4/ST7/ST9)"))));

        // ---- Antecedentes heredofamiliares (Slice 1): Si/No + observaciones ----
        CATALOGO.put("Neurología", new Seccion("NEUROLOGIA", List.of(
                sino("AVC", "AVC (accidente vascular cerebral)"),
                sino("EPILEPSIA", "Epilepsia"),
                sino("PARALISIS_FACIAL", "Parálisis facial"),
                sino("PARKINSON", "Parkinson"),
                sino("ALZHEIMER", "Alzheimer"),
                sino("MIGRAÑA", "Migraña"))));
        CATALOGO.put("Cardiopatía", new Seccion("CARDIOPATIA", List.of(
                sino("CARDIOPATIA_ISQUEMICA", "Cardiopatía isquémica"),
                sino("HIPERTENSION", "Hipertensión"),
                sino("HIPOTENSION", "Hipotensión"),
                sino("INSUFICIENCIA_CARDIACA", "Insuficiencia cardíaca"),
                sino("INSUFICIENCIA_VASCULAR_PERIFERICA", "Insuf. vascular periférica"))));
        CATALOGO.put("Neumopatía", new Seccion("NEUMOPATICA", List.of(
                sino("BRONQUITIS", "Bronquitis"),
                sino("TUBERCULOSIS", "Tuberculosis"),
                sino("ASMA", "Asma"),
                sino("INSUFICIENCIA_RESPIRATORIA", "Insuf. respiratoria"))));
        CATALOGO.put("Toxicológico", new Seccion("TOXICOLOGIOCAS", List.of(
                sino("ALCOHOLISMO", "Alcoholismo"),
                sino("TABAQUISMO", "Tabaquismo"),
                sino("DROGAS", "Drogas"))));
        CATALOGO.put("Nefropatías", new Seccion("NEFROPATIA", List.of(
                sino("IRC", "IRC (insuficiencia renal crónica)"),
                sino("LITASIS", "Litiasis"))));
        CATALOGO.put("Endocrinas", new Seccion("ENDOCRINA", List.of(
                sino("DM", "Diabetes mellitus"),
                sino("HIPERTIROIDISMO", "Hipertiroidismo"),
                sino("HIPOTIROIDISMO", "Hipotiroidismo"))));
        CATALOGO.put("Obesidad", new Seccion("OBESIDAD", List.of(
                sino("TRANSTORNO_DE_CRECIMIENTO", "Trastorno de crecimiento"))));
        CATALOGO.put("Mentales", new Seccion("MENTALES", List.of(
                sino("NEUROSIS", "Neurosis"),
                sino("PSICOSIS", "Psicosis"),
                sino("INTENTO_DE_SUICIDIO", "Intento de suicidio"),
                sino("ESQUIZOFRENIA", "Esquizofrenia"))));
        CATALOGO.put("Generales", new Seccion("GENERALES", List.of(
                sino("CANCER", "Cáncer"),
                sino("ALERGIAS", "Alergias"),
                sino("MALFORMACIONES", "Malformaciones"))));
        CATALOGO.put("Otras", new Seccion("OTRAS", List.of(
                sino("OTRAS", "Otras"))));

        // ---- Heredofamiliares (resumen por categoria): bloque separado en el WS,
        // ver docs/contextoWS.txt:1143-1160 (tabla SERV_MED_HEREDOFAMILIAR, un *_ID por categoria).
        // Significado exacto del valor numerico no confirmado (severidad/tipo vs. flag) - se captura
        // como texto libre para no asumir un rango o catalogo de valores no documentado.
        CATALOGO.put("Heredofamiliares (resumen)", new Seccion("HEREDOFAMILIARES", List.of(
                text("NEUROLOGIA_ID", "Neurología"),
                text("CARDIOPATIA_ID", "Cardiopatía"),
                text("NEUMOPATICA_ID", "Neumopatía"),
                text("TOXICOLOGICO_ID", "Toxicológico"),
                text("NEFROPATIAS_ID", "Nefropatías"),
                text("ENDOCRINAS_ID", "Endocrinas"),
                text("OBESIDAD_ID", "Obesidad"),
                text("MENTALES_ID", "Mentales"),
                text("GENERALES_ID", "Generales"),
                text("OTRAS_ID", "Otras"))));

        // ---- Antecedentes personales no patologicos (APNP): texto ----
        CATALOGO.put("APNP (no patológicos)", new Seccion("SERV_MED_ANT_PATOLOGICOS", List.of(
                text("HABITACION", "Habitación"),
                text("NO_DORMITORIOS", "No. dormitorios"),
                text("NO_HABITANTES", "No. habitantes"),
                text("PISO", "Piso"),
                text("TECHO", "Techo"),
                text("AGUA_POTABLE", "Agua potable"),
                text("LUZ", "Luz"),
                text("DRENAJE", "Drenaje"),
                text("CONVIVENCIA_CON_ANIMALES", "Convivencia con animales"),
                text("AIRE_ACONDICIONADO", "Aire acondicionado"),
                text("RELIGION", "Religión"),
                text("HIGIENE_PERSONAL", "Higiene personal"),
                text("BAÑO", "Baño"),
                text("CAMBIO_DE_ROPA", "Cambio de ropa"),
                text("ASEO_BUCAL", "Aseo bucal"),
                text("ALIMENTACION", "Alimentación"),
                text("NO_COMIDAS_AL_DIA", "No. comidas al día"),
                text("BALANCEADA", "Balanceada"),
                text("SUFICIENTE", "Suficiente"),
                text("DEPORTE", "Deporte"),
                text("FC1", "Frecuencia (deporte)"),
                text("HOBBIES_PASATIEMPOS", "Hobbies / pasatiempos"),
                text("FC2", "Frecuencia (hobbies)"))));

        // ---- Inmunizaciones: vacunas Si/No + fecha de influenza + COVID por dosis (FT-SO-17) ----
        // FECHA de la tabla es la de COVID (asi la rotulaba el PHP y casi solo hay fecha donde hay
        // dosis); la de influenza el PHP la guardaba en el tag fechaINFLUEN (saveContactosEmergencia.php).
        CATALOGO.put("Inmunizaciones", new Seccion("SERV_MED_INMUNIZACIONES", List.of(
                sino0("BLG", "BCG"),
                sino0("SABIN", "Sabin"),
                sino0("DPT", "DPT"),
                sino0("HEPATITIS_B", "Hepatitis B"),
                sino0("SR", "SR"),
                sino0("INFLUENZA", "Influenza"),
                sino0("TD", "Td"),
                tagFecha("fechaINFLUEN", "Fecha influenza"),
                dosis("COVID", "COVID-19 (dosis)"),
                date("FECHA", "Fecha COVID"))));

        // ---- Antecedentes gineco-obstetricos (AGO): texto ----
        CATALOGO.put("Gineco-obstétricos (AGO)", new Seccion("SERV_MED_ANT_GIN_OBSTETRICOS", List.of(
                text("MENARCA", "Menarca"),
                text("RITMO", "Ritmo"),
                text("IVSA", "IVSA"),
                text("FUM", "FUM"),
                // G, P, A, C: orden de los FT-SO (16-oct-2026)
                text("G", "Gesta (G)"),
                text("P", "Partos (P)"),
                text("A", "Abortos (A)"),
                text("C", "Cesáreas (C)"),
                text("MET_PLANIFICACION", "Método de planificación"),
                text("PAPANICOLAOU", "Papanicolaou"),
                text("INFECC_REP", "Infecciones (rep.)"),
                text("FUP", "FUP"))));

        // ---- Antecedentes personales patologicos (APP): Si/No (+ obs) ----
        CATALOGO.put("APP: personales patológicos", new Seccion("SERV_MED_ANT_PER_PATOLOGICOS", List.of(
                sino0("ENF_CONGENITAS", "Enf. congénitas"),
                // etiquetas como el FT-SO-04 rev. 05 (filas 66-69), con su aclaracion entre parentesis
                sino0("GENITAL", "Genital (prolapsos)"),
                sino0("ENF_PROP_INF", "Enf. propias de la infancia"),
                sino0("DERMATOPATIAS", "Dermatopatías"),
                sino0("SIST_AUDITIVO", "Sistema auditivo (sordera, perforación de tímpano)"),
                sino0("RESPIRATORIO", "Respiratorio"),
                sino0("SIS_OLFATIVO", "Sistema olfativo"),
                sino0("TRAUMATICOS", "Traumáticos"))));
        CATALOGO.put("APP: oftalmológico", new Seccion("SERV_MED_OFTALMOLOGICO", List.of(
                sino("USO_LENTES", "Uso de lentes"),
                sino("ASTIGMATISMO", "Astigmatismo"),
                sino("PRESBICIA", "Presbicia"),
                sino("ESTRABISMO", "Estrabismo"),
                sino("DALTONISMO", "Daltonismo"),
                sino("DESPENDIMIENTO_RET", "Desprendimiento de retina"),
                sino("MIOPIA", "Miopía"))));
        CATALOGO.put("APP: digestivo", new Seccion("SERV_MED_DIGESTIVO", List.of(
                sino("HERNIAS", "Hernias"),
                sino("CIRROSIS", "Cirrosis"),
                sino("COLITIS", "Colitis"),
                sino("GASTRITIS", "Gastritis y esofagitis"),
                sino("HEMORROIDES", "Hemorroides"),
                sino("CIRUGIAS", "Cirugías"))));
        CATALOGO.put("APP: renal", new Seccion("SERV_MED_RENAL", List.of(
                sino("INCONTINENSIA", "Incontinencia"),
                sino("INSUFICIENCIA", "Insuficiencia"),
                sino("LITIASIS", "Litiasis"))));
        CATALOGO.put("APP: sistema nervioso", new Seccion("SERV_MED_SIST_NERVIOSO", List.of(
                sino("ANEURISMA", "Aneurisma"),
                sino("ENF_DEG_MUS_NEU", "Enf. degenerativas musculares y neuronales"),
                sino("ACC_CER_VASCU", "Accidente cerebrovascular"),
                sino("EPILEPSIA", "Epilepsia"),
                sino("PAR_FACIAL", "Parálisis facial"))));
        CATALOGO.put("APP: músculo-esquelético", new Seccion("SERV_MED_MUS_ESQUELETICO", List.of(
                sino("ESCOLIOSIS", "Escoliosis"),
                sino("XIFOSIS", "Xifosis"),
                sino("LORDOSISI", "Lordosis"),
                sino("LUMBALGIA ", "Lumbalgia"),
                sino("BASCULAMIENTO", "Basculamiento"),
                sino("ALTERACION_DISCAL", "Alteración discal"))));
        CATALOGO.put("APP: cardiovascular", new Seccion("SERV_MED_CARDIO_NO_PATO", List.of(
                sino("ANEURISMA", "Aneurisma"),
                sino("HIPERTENSION", "Hipertensión"),
                sino("HIPOTENSION", "Hipotensión"),
                sino("INFARTOS ", "Infartos"),
                sino("INSUFIC_CAR", "Insuficiencia cardíaca"))));
        CATALOGO.put("APP: toxicológico", new Seccion("SERV_MED_TOXI_NO_PATO", List.of(
                sino("TABAQUISMO", "Tabaquismo"),
                sino("ALCOHOLISMO", "Alcoholismo"),
                sino("DROGAS", "Drogas"),
                // la pregunta del FT-SO-04: "¿Algun trastorno que repercuta en la manipulacion de cargas manuales?"
                sino("ALT_CARGA ", "Trastorno que afecte la manipulación de cargas"),
                sino("PERFORACIONES", "Perforaciones"),
                sino("TATUAJES", "Tatuajes"),
                sino("ALERGIAS", "Alergias"))));
        CATALOGO.put("APP: endocrino", new Seccion("SERV_MED_ENDOCRI_NO_PATO", List.of(
                sino("DIABETES", "Diabetes"),
                sino("HIPERTIROIDISMO", "Hipertiroidismo"),
                sino("HIPOTIROIDISMO", "Hipotiroidismo"))));
        // FT-SO-17 (periodico, 15-oct-2026): categorias del formato que no tienen tabla en el WS.
        // Texto en SERV_MED_TAG: "NEGADOS" o el antecedente. Sin columna de respaldo.
        CATALOGO.put("APP: otros", new Seccion("SERV_MED_APP_OTROS", List.of(
                tagSolo("APPHEMATOPATIAS", "Hematopatías"),
                tagSolo("APPTRANSFUSIONALES", "Transfusionales"),
                tagSolo("APPQUIRURGICOS", "Quirúrgicos"))));

        // ---- Padecimiento actual / Interrogatorio (textarea) ----
        CATALOGO.put("Padecimiento actual", new Seccion("SERV_MED_PADECI_ACTUAL", List.of(
                area("OBSERVACIONES", "Observaciones"))));
        CATALOGO.put("Interrogatorio por aparatos", new Seccion("SERV_MED_INT_APARATO_SIST", List.of(
                area("OBSERVACIONES", "Observaciones"))));

        // ---- Exploracion fisica (signos, somatotipo, actitud) ----
        CATALOGO.put("Exploración física", new Seccion("SERV_MED_EXPLORACION_FISICA", List.of(
                text("PESO", "Peso"), text("TALLA", "Talla"), text("IMC", "IMC"),
                text("FC", "FC"), text("FR", "FR"), text("TA", "TA"), text("TEMP", "Temperatura"),
                // Constitucion, estado emocional, facies y marcha: en el PHP eran UN select cada uno y
                // el texto elegido se guardaba en la primera columna del grupo (ECTOMORFICO,
                // SERENIDAD, HIPOCRITA, PARKINSON); las demas columnas nunca se llenaron (13-oct-2026)
                opcion("ECTOMORFICO", "Constitución"), oculto("MESOMORFO", "Mesomorfo"), oculto("ENDOMORFICA", "Endomórfica"),
                text("SATISFACCION", "Satisfacción"), text("COMP_TRABAJO", "Comp. trabajo"), text("COMP_ORGANIZACION", "Comp. organización"),
                text("TIEMPO", "Tiempo"), text("ESPACIO", "Espacio"), text("PERSONA", "Persona"),
                opcion("SERENIDAD", "Estado emocional"), oculto("INTERES", "Interés"), oculto("ANTAGONICO", "Antagónico"),
                oculto("ENTUSIASTA", "Entusiasta"), oculto("ENOJO", "Enojo"), opcion("HIPOCRITA", "Facies"),
                oculto("ICTERICA", "Ictérica"), oculto("ANEMICA", "Anémica"), oculto("TIROIDEA", "Tiroidea"),
                oculto("NOLMAL_FASCIES", "Facies normal"), opcion("PARKINSON", "Marcha"), oculto("HEMIPLEJICO", "Hemipléjico"),
                oculto("ATAXICA", "Atáxica"), oculto("HEMIPARESIA", "Hemiparesia"), oculto("NORMAL_MARCHA", "Marcha normal"),
                text("LENGUAJE", "Lenguaje"), text("CONVER_FLUIDA", "Conversación fluida"), text("COHERENCIA_PALABLAS", "Coherencia de palabras"))));
        CATALOGO.put("Cráneo", new Seccion("SERV_MED_CRANEO", List.of(
                text("OJOS", "Ojos"), text("PUPILAS", "Pupilas"), text("CONJUNTIVAS", "Conjuntivas"),
                text("REFLEJOS", "Reflejos"), text("FONDO_OJO", "Fondo de ojo"))));
        CATALOGO.put("Agudeza visual", new Seccion("SERV_MED_AGUDEZA_VISUAL", List.of(
                // SL/CL del FT-SO-04: OD/OIZ es la medida SIN lentes; la medida CON lentes ya la
                // capturaba el PHP en estos tags (medido 15-oct-2026: "20/10", "20/20", "SDPA", "N/A"...;
                // solo 43 de ~2,100 son 0/1). pdfGenerator.php los imprimia mal como Si/No.
                text("OD", "Ojo derecho sin lentes (SL)"), text("OIZ", "Ojo izquierdo sin lentes (SL)"),
                tagSolo("LENTEOJODERECHOEXP", "Ojo derecho con lentes (CL)"),
                tagSolo("LENTEOJOIZQUIERDOEXP", "Ojo izquierdo con lentes (CL)"),
                text("COLORES", "Colores"))));
        CATALOGO.put("Nariz", new Seccion("SERV_MED_NARIZ", List.of(
                text("CAVIDAD_NASAL", "Cavidad nasal"), text("MUCOSA", "Mucosa"),
                text("TABIQUE_NASA", "Tabique nasal"), text("OLFATO", "Olfato"))));
        CATALOGO.put("Columna vertebral", new Seccion("SERV_MED_COLUMNA_VERTEBRAL", List.of(
                text("CERVICA", "Cervical"), text("DORSAL", "Dorsal"), text("LUMBAR", "Lumbar"))));
        CATALOGO.put("Boca", new Seccion("SERV_MED_BOCA", List.of(
                text("LESIONES", "Lesiones"), text("ENCIAS", "Encías"), text("OROIFARINGE", "Orofaringe"))));
        // Los nombres de tag no corresponden a la etiqueta (OD = OIMEMTIMINPEXP): es el mapeo del
        // PHP (formsEFis/EFis.html) y del impreso (examen-documento.html), se respeta tal cual
        CATALOGO.put("Oídos", new Seccion("SERV_MED_OIDOS", List.of(
                oculto("CAE", "CAE"), oculto("MEM_TIM", "Membrana timpánica"), oculto("AGUD_AUDIT", "Agudeza auditiva"),
                oculto("OD", "Oído derecho"), oculto("OI", "Oído izquierdo"),
                tagTexto("ODCAEINPEXP", "CAE", "CAE"),
                tagTexto("ODMEMTIMINPEXP", "Membrana timpánica", "MEM_TIM"),
                tagTexto("ODAGAUDIINPEXP", "Agudeza auditiva", "AGUD_AUDIT"),
                tagTexto("OIMEMTIMINPEXP", "Oído derecho (OD)", "OD"),
                tagTexto("OIAGAUDIINPEXP", "Oído izquierdo (OI)", "OI"))));
        CATALOGO.put("Tórax", new Seccion("SERV_MED_TORAX", List.of(
                text("RUIDOS_CARDIACOS", "Ruidos cardíacos"), text("REG_PRECORDIAL", "Región precordial"),
                text("CAMPOS_PULM", "Campos pulmonares"))));
        CATALOGO.put("Columna (movilidad)", new Seccion("SERV_MED_COLUMNA_VERTEBRAL_AUX", List.of(
                text("DEFORMACIONES", "Deformaciones"), text("DOLOR", "Dolor"), text("MOVIMIENTOS", "Movimientos"),
                text("MARCHA", "Marcha"), text("LASSEGUE", "Lassegue"), text("PUNTA_TALON", "Punta-talón"))));
        CATALOGO.put("Abdomen", new Seccion("SERV_MED_ABDOMEN", List.of(
                text("FORMA", "Forma"), text("VISCEROMEGALIAS", "Visceromegalias"), text("HERNIAS", "Hernias"),
                text("DOLOR", "Dolor"), text("PERISTALSIS", "Peristalsis"))));
        CATALOGO.put("Genitales", new Seccion("SERV_MED_GENITALES", List.of(
                text("MASCULINO", "Masculino"), text("FEMENINO", "Femenino"))));
        CATALOGO.put("Urinario", new Seccion("SERV_MED_URINARIO", List.of(
                text("PUNTOS_URETRALES", "Puntos ureterales"), text("FOSAS_RENALES", "Fosas renales"))));
        CATALOGO.put("Extremidades", new Seccion("SERV_MED_EXTREMIDADES", List.of(
                text("SUPERIOR", "Superior"), text("INFERIOR", "Inferior"), text("IVP", "IVP"), text("EDEMA", "Edema"))));
        CATALOGO.put("Piel", new Seccion("SERV_MED_PIEL", List.of(
                text("LUNARES", "Lunares"), text("PIGMENTACION", "Pigmentación"), text("CICATRICES_QUIRUR", "Cicatrices quirúrgicas"))));
        CATALOGO.put("Cuello", new Seccion("SERV_MED_CUELLO", List.of(
                text("DEFORMACIONES_OBS", "Deformaciones"), text("TIROIDES_OBS", "Tiroides"), text("TRAQUEA_OBS", "Tráquea"))));
        CATALOGO.put("Dientes", new Seccion("SERV_MED_DIENTES", List.of(
                text("DIENTE_11", "11"), text("DIENTE_12", "12"), text("DIENTE_13", "13"), text("DIENTE_14", "14"),
                text("DIENTE_15", "15"), text("DIENTE_16", "16"), text("DIENTE_17", "17"), text("DIENTE_18", "18"),
                text("DIENTE_21", "21"), text("DIENTE_22", "22"), text("DIENTE_23", "23"), text("DIENTE_24", "24"),
                text("DIENTE_25", "25"), text("DIENTE_26", "26"), text("DIENTE_27", "27"), text("DIENTE_28", "28"),
                text("DIENTE_31", "31"), text("DIENTE_32", "32"), text("DIENTE_33", "33"), text("DIENTE_34", "34"),
                text("DIENTE_35", "35"), text("DIENTE_36", "36"), text("DIENTE_37", "37"), text("DIENTE_38", "38"),
                text("DIENTE_41", "41"), text("DIENTE_42", "42"), text("DIENTE_43", "43"), text("DIENTE_44", "44"),
                text("DIENTE_45", "45"), text("DIENTE_46", "46"), text("DIENTE_47", "47"), text("DIENTE_48", "48"))));

        // ---- Estudios / Diagnostico / Plan / Resultado ----
        CATALOGO.put("Estudios realizados", new Seccion("SERV_MED_ESTUDIOS_REALIZADOS", List.of(
                area("RESULTADOS", "Resultados"))));
        CATALOGO.put("Diagnóstico", new Seccion("SERV_MED_DIAGNOSTICO", List.of(
                area("OBSERVACIONES", "Diagnóstico"))));
        CATALOGO.put("Plan terapéutico", new Seccion("SERV_MED_PLAN_TERAPIA", List.of(
                area("OBSERVACIONES", "Plan terapéutico"))));
        CATALOGO.put("Resultado del examen", new Seccion("SERV_MED_RESULTADO_EXAMEN", List.of(
                new Campo("DICTAMEN", "Dictamen", "RESULTADO", false),
                area("OBSERVACIONES", "Observaciones"))));
    }

    /** Grupo de secciones para agrupar los botones de navegacion en la UI. */
    public record Grupo(String nombre, List<String> secciones) {
    }

    // Agrupacion puramente de UI (no afecta guardado/lectura) - un nombre de seccion que no
    // aparezca aqui simplemente no se agrupa (no rompe el catalogo si cambia).
    // Un grupo por seccion del FT-SO-04 / FT-SO-17, con su mismo titulo y orden (16-oct-2026);
    // los dos coinciden si la exploracion se lee por columnas, que es como estan maquetados.
    // Heredofamiliares y APNP solo los pide el FT-SO-04: examen-shell.html los oculta en el
    // periodico y ahi agrega «"Inmunizaciones"» al titulo de no patologicos, como el FT-SO-17.
    // Los nombres de grupo los compara examen-shell.html: si cambian, cambiarlos tambien alla.
    private static final List<Map.Entry<String, List<String>>> GRUPOS = List.of(
            Map.entry("Antecedentes laborales", List.of(
                    "Antecedentes laborales (resumen)")),
            Map.entry("Antecedentes heredofamiliares", List.of(
                    "Neurología", "Cardiopatía", "Neumopatía", "Toxicológico", "Nefropatías",
                    "Endocrinas", "Obesidad", "Mentales", "Generales", "Otras",
                    "Heredofamiliares (resumen)")),
            Map.entry("Antecedentes personales no patológicos", List.of(
                    "APNP (no patológicos)", "Inmunizaciones")),
            Map.entry("Antecedentes gineco-obstétricos", List.of(
                    "Gineco-obstétricos (AGO)")),
            // La pestana "Patologicos (resumen)" la inserta examen-shell.html antes de las APP
            Map.entry("Antecedentes personales patológicos", List.of(
                    "APP: personales patológicos", "APP: oftalmológico", "APP: renal",
                    "APP: cardiovascular", "APP: sistema nervioso", "APP: digestivo",
                    "APP: músculo-esquelético", "APP: endocrino", "APP: toxicológico", "APP: otros")),
            Map.entry("Padecimiento actual", List.of(
                    "Padecimiento actual")),
            Map.entry("Interrogatorio por aparatos y sistemas", List.of(
                    "Interrogatorio por aparatos")),
            Map.entry("Exploración física", List.of(
                    "Exploración física", "Cráneo", "Agudeza visual", "Nariz", "Columna vertebral",
                    "Boca", "Oídos", "Cuello", "Tórax", "Columna (movilidad)", "Abdomen", "Genitales",
                    "Urinario", "Extremidades", "Piel", "Dientes")),
            Map.entry("Estudios realizados y resultados", List.of(
                    "Estudios realizados")),
            Map.entry("Diagnóstico", List.of(
                    "Diagnóstico")),
            Map.entry("Plan o terapéutica empleada", List.of(
                    "Plan terapéutico")),
            Map.entry("Resultado del examen", List.of(
                    "Resultado del examen")));

    private final ExamenClient client;
    private final MedTagRepository tags;
    private final ExamenHistoricoRepository historico;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ExamenService.class);

    /**
     * Cache corta de la lectura del WS por NSS. El acordeon carga cada seccion con una peticion
     * y cada una leia el examen COMPLETO del WS otra vez ("Abrir todo" = 46 llamadas a ORDS
     * para el mismo dato). Con 20 s basta para que una carga de pantalla sea una sola llamada;
     * guardar invalida la entrada del NSS.
     */
    private static final long CACHE_MS = 20_000;
    private final java.util.concurrent.ConcurrentHashMap<String, Object[]> cacheLectura = new java.util.concurrent.ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    private Map<String, String> datosExamen(String nss) {
        long ahora = System.currentTimeMillis();
        Object[] hit = cacheLectura.get(nss);
        if (hit != null && ahora - (long) hit[0] < CACHE_MS) {
            return (Map<String, String>) hit[1];
        }
        Map<String, String> data = client.getExamenData(nss);
        cacheLectura.put(nss, new Object[]{ahora, data});
        return data;
    }

    private void invalidar(String nss) {
        cacheLectura.remove(nss);
    }

    public ExamenService(ExamenClient client, MedTagRepository tags, ExamenHistoricoRepository historico) {
        this.client = client;
        this.tags = tags;
        this.historico = historico;
    }

    /** Nombres de las secciones (orden de navegacion). */
    public List<String> secciones() {
        return new ArrayList<>(CATALOGO.keySet());
    }

    /** Secciones agrupadas por categoria, para no mostrar ~46 botones en una sola fila. */
    public List<Grupo> grupos() {
        List<Grupo> out = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : GRUPOS) {
            List<String> presentes = entry.getValue().stream().filter(CATALOGO::containsKey).toList();
            if (!presentes.isEmpty()) {
                out.add(new Grupo(entry.getKey(), presentes));
            }
        }
        return out;
    }

    /** Todas las claves de escritura "SECCION.CAMPO" (y sus _OBS) del catalogo, para reconciliar al guardar. */
    public static Set<String> clavesEscritura() {
        Set<String> out = new java.util.HashSet<>();
        for (Seccion sec : CATALOGO.values()) {
            for (Campo c : sec.campos()) {
                if (c.tag() != null) {
                    continue;   // va a SERV_MED_TAG; su columna de respaldo ya esta como OCULTO
                }
                out.add(sec.dataKey() + "." + c.field());
                if (c.hasObs()) {
                    out.add(sec.dataKey() + "." + c.field() + "_OBS");
                }
            }
        }
        return out;
    }

    public boolean existeSeccion(String seccion) {
        return CATALOGO.containsKey(seccion);
    }

    /** Items de una seccion, pre-cargados con los datos del examen del NSS. */
    public List<ExamItem> itemsDeSeccion(String seccion, String nss) {
        Seccion sec = CATALOGO.get(seccion);
        if (sec == null) {
            throw new IllegalArgumentException("Seccion desconocida: " + seccion);
        }
        Map<String, String> data = new java.util.LinkedHashMap<>(datosExamen(normalizeNss(nss)));
        if ("SERV_ANTECEDENTESLAB".equals(sec.dataKey())) {
            completarDesdeTags(data, normalizeNss(nss));
        }
        if (sec.campos().stream().anyMatch(c -> c.tag() != null)) {
            cargarCamposTag(data, normalizeNss(nss));
        }
        return itemsDesde(sec, data);
    }

    /** TYPEs de SERV_MED_TAG que el catalogo permite leer/escribir (lista cerrada). */
    private static Set<String> tiposTag() {
        Set<String> out = new java.util.LinkedHashSet<>();
        for (Seccion sec : CATALOGO.values()) {
            for (Campo c : sec.campos()) {
                if (c.tag() != null) {
                    out.add(c.tag());
                }
            }
        }
        return out;
    }

    /** Ultimo valor de cada tag del catalogo, o mapa vacio si no se pudo leer. */
    private Map<String, String> leerTags(String nss) {
        Map<String, String> out = new LinkedHashMap<>();
        Set<String> tipos = tiposTag();
        try {
            // una lectura por prefijo de 2 letras (OD/OI de Oidos, LE de lentes) y se filtra a la lista
            Set<String> prefijos = new java.util.TreeSet<>();
            tipos.forEach(t -> prefijos.add(t.substring(0, Math.min(2, t.length()))));
            for (String prefijo : prefijos) {
                tags.latestByNssAndTypePrefix(nss, prefijo).forEach((k, v) -> {
                    if (tipos.contains(k)) {
                        out.put(k, v);
                    }
                });
            }
        } catch (RuntimeException ex) {
            log.warn("[examen] no se pudieron leer los tags de {}: {}", nss, ex.getMessage());
        }
        return out;
    }

    /** Pone en data las claves "TAG.<TYPE>" con el ultimo valor de SERV_MED_TAG. */
    private void cargarCamposTag(Map<String, String> data, String nss) {
        leerTags(nss).forEach((k, v) -> data.put(PREFIJO_TAG + k, v));
    }

    /**
     * Antecedentes laborales (resumen): la tabla del WS solo trae el stub que el PHP mandaba
     * hardcodeado (pension="true", edad 20, 3 trabajos; confirmado 12-sep-2026: 3,209 de
     * 3,212 filas) o nada. Lo capturado de verdad vive en SERV_MED_TAG (el PHP lo guardaba en
     * "tags"). Si el WS viene vacio o con el stub, se precarga desde SERV_MED_TAG; lo que el
     * usuario guarde desde aqui va al WS como siempre.
     */
    private void completarDesdeTags(Map<String, String> data, String nss) {
        Map<String, String> t;
        try {
            t = tags.latestByNssAndTypeSuffix(nss, "");
        } catch (RuntimeException ex) {
            return;
        }
        completar(data, "SERV_ANTECEDENTESLAB.edad_inicio_laborar", t.get("edad_inicio_laborar"), "20");
        completar(data, "SERV_ANTECEDENTESLAB.cantidad_trabajos", t.get("cantidad_trabajos"), "3");
        completar(data, "SERV_ANTECEDENTESLAB.pension", t.get("pension"), "true");
    }

    private static void completar(Map<String, String> data, String clave, String deTags, String valorStub) {
        String ws = data.get(clave);
        boolean vacioOStub = mostrable(ws).isBlank() || valorStub.equalsIgnoreCase(mostrable(ws).trim());
        if (vacioOStub && deTags != null && !deTags.isBlank()) {
            data.put(clave, deTags.trim());
        }
    }

    /** Una seccion ya resuelta, para la vista de impresion. */
    public record SeccionImpresa(String nombre, List<ExamItem> items) {
    }

    public record GrupoImpreso(String nombre, List<SeccionImpresa> secciones) {
    }

    /** {@code fecha}: FECHA del examen tal como la devuelve el WS (puede venir vacia). */
    public record ExpedienteImpreso(String fecha, List<GrupoImpreso> grupos) {
    }

    /**
     * Examen completo (todos los grupos y secciones) con UNA sola llamada al WS - lo que el
     * PHP hacia en pdf/pdfGenerator.php ("Imprimir expediente"). itemsDeSeccion() llama al WS
     * por seccion porque el acordeon carga una a la vez; aqui serian 22 llamadas por nada.
     */
    public ExpedienteImpreso expedienteCompleto(String nss) {
        Map<String, String> data = new LinkedHashMap<>(client.getExamenData(normalizeNss(nss)));
        cargarCamposTag(data, normalizeNss(nss));
        List<GrupoImpreso> grupos = new ArrayList<>();
        for (Grupo g : grupos()) {
            List<SeccionImpresa> secciones = new ArrayList<>();
            for (String nombre : g.secciones()) {
                List<ExamItem> items = new ArrayList<>();
                for (ExamItem it : itemsDesde(CATALOGO.get(nombre), data)) {
                    if ("OCULTO".equals(it.type())) {
                        continue;   // columnas que el PHP nunca lleno: en papel solo serian renglones vacios
                    }
                    items.add(new ExamItem(it.type(), it.label(), it.fieldName(), legible(it.value()),
                            it.obsName(), legible(it.obsValue())));
                }
                secciones.add(new SeccionImpresa(nombre, items));
            }
            grupos.add(new GrupoImpreso(g.nombre(), secciones));
        }
        String fecha = data.getOrDefault("FECHA", "");
        return new ExpedienteImpreso("0".equals(fecha) ? "" : fecha, grupos);
    }

    /**
     * Para impresion: el proc del WS usa COALESCE con textos de relleno ("sin observación",
     * "sin datos", "0") y booleanos como "true"/"false"; en papel eso se lee como dato.
     */
    private static String legible(String v) {
        if (v == null) {
            return null;
        }
        String t = v.trim();
        String u = t.toLowerCase();
        if (u.isEmpty() || u.startsWith("sin obs") || u.equals("sin datos") || u.equals("null")
                || u.contains("no existe en base de datos")) {
            return "";
        }
        if (u.equals("true")) {
            return "Sí";
        }
        if (u.equals("false")) {
            return "No";
        }
        return t;
    }

    private static List<ExamItem> itemsDesde(Seccion sec, Map<String, String> data) {
        List<ExamItem> items = new ArrayList<>();
        for (Campo campo : sec.campos()) {
            if (campo.tag() != null) {
                // tag primero; si esta vacio, lo que hubiera en la columna del WS (asi no se pierde
                // y al guardar pasa al tag)
                String deTag = mostrable(data.get(PREFIJO_TAG + campo.tag()));
                String valor = deTag.isBlank()
                        ? mostrable(ExamenClaves.leer(data, sec.dataKey() + "." + campo.field())) : deTag;
                items.add(new ExamItem(campo.type(), campo.label(), PREFIJO_TAG + campo.tag(), valor, null, null));
                continue;
            }
            String base = sec.dataKey() + "." + campo.field();
            String obsName = campo.hasObs() ? base + "_OBS" : null;
            // ExamenClaves.leer(): el WS de lectura devuelve varias claves con nombre distinto
            // al de escritura (espacios, typos) - ver esa clase.
            String obsValue = campo.hasObs() ? mostrable(ExamenClaves.leer(data, base + "_OBS")) : null;
            String value = "RESULTADO".equals(campo.type())
                    ? dictamenDesdeFlags(data, sec.dataKey())
                    : mostrable(ExamenClaves.leer(data, base));
            items.add(new ExamItem(campo.type(), campo.label(), base, value, obsName, obsValue));
        }
        return items;
    }

    /**
     * El WS no devuelve DICTAMEN: devuelve 4 banderas (APTO, NO_APTO, APTO_CONDICIONADO,
     * APTO_RESTRINGIDO) con "1" en la elegida - las mismas que BiowsExamenClient.guardar()
     * escribe a partir del select. Sin esto el select volvia a "Seleccionar" tras guardar.
     */
    private static String dictamenDesdeFlags(Map<String, String> data, String seccion) {
        if ("1".equals(data.get(seccion + ".APTO"))) {
            return "apto";
        }
        if ("1".equals(data.get(seccion + ".NO_APTO"))) {
            return "no_apto";
        }
        if ("1".equals(data.get(seccion + ".APTO_CONDICIONADO"))) {
            return "apto_condicionado";
        }
        if ("1".equals(data.get(seccion + ".APTO_RESTRINGIDO"))) {
            return "apto_restringido";
        }
        return "";
    }

    /**
     * El PL/SQL devuelve textos de relleno en lugar de null ("sin observaciones", "sin obs",
     * "sin datos", "sin firma"); en el formulario se muestran vacios para que el usuario no
     * los guarde como si fueran captura. "0" se conserva (en Si/No significa No).
     */
    private static String mostrable(String v) {
        if (v == null) {
            return "";
        }
        String t = v.trim().toLowerCase(Locale.ROOT);
        if (t.equals("sin datos") || t.equals("sin firma") || t.startsWith("sin obs")) {
            return "";
        }
        return v;
    }

    /**
     * Firma del paciente ya guardada (SERV_MED_RESULTADO_EXAMEN.FIRMA_DIGITAL) como data URL
     * PNG, o null si no hay o el WS trae su relleno ("sin firma"). Para mostrarla en el shell.
     */
    public String firmaGuardada(String nss) {
        try {
            String f = datosExamen(normalizeNss(nss)).get("SERV_MED_RESULTADO_EXAMEN.FIRMA_DIGITAL");
            return f != null && f.startsWith("data:image") ? f : null;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    /** Guarda el examen (CAMBIO). campos = name->value de las secciones cargadas. */
    public String guardar(String nss, Map<String, String> campos, String firma) {
        return guardar(nss, campos, firma, ExamenHistoricoRepository.ORIGEN_GENERAL);
    }

    /**
     * Guarda y, si el guardado trae dictamen (el examen "cierra"), deja la foto completa en
     * SERV_MED_EXAMEN_HISTORICO con el origen indicado. Los guardados parciales por seccion
     * del modulo general no generan fila: solo los que incluyen Resultado del examen.
     */
    public String guardar(String nss, Map<String, String> campos, String firma, String origen) {
        String limpio = normalizeNss(nss);
        invalidar(limpio);
        // Las claves "TAG.<TYPE>" van a SERV_MED_TAG, no al WS: se separan antes de mandarlo
        Map<String, String> c = new LinkedHashMap<>();
        Map<String, String> nuevosTags = new LinkedHashMap<>();
        Set<String> tipos = tiposTag();
        if (campos != null) {
            campos.forEach((k, v) -> {
                if (k.startsWith(PREFIJO_TAG)) {
                    String tipo = k.substring(PREFIJO_TAG.length());
                    if (tipos.contains(tipo)) {
                        nuevosTags.put(tipo, v);
                    }
                } else {
                    c.put(k, v);
                }
            });
        }
        String r = client.guardar(limpio, c, firma);
        guardarTags(limpio, nuevosTags);
        invalidar(limpio);
        String dictamen = c.getOrDefault("SERV_MED_RESULTADO_EXAMEN.DICTAMEN", "");
        if (dictamen != null && !dictamen.isBlank()) {
            registrarHistorico(limpio, origen, dictamen.trim());
        }
        return r;
    }

    /**
     * Escribe solo los tags cuyo valor cambio respecto al vigente: abrir y guardar sin tocar
     * Oidos no reescribe ~3,000 historicos ni crea filas vacias. Va despues del WS: si el WS
     * falla, no queda medio examen en tags.
     */
    private void guardarTags(String nss, Map<String, String> nuevos) {
        if (nuevos.isEmpty()) {
            return;
        }
        Map<String, String> vigentes = leerTags(nss);
        String usuario = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() == null
                ? "SISTEMA" : org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        nuevos.forEach((tipo, valor) -> {
            String nuevo = valor == null ? "" : valor.trim();
            String actual = mostrable(vigentes.get(tipo)).trim();
            if (!nuevo.equals(actual)) {
                tags.upsert(nss, tipo, nuevo, GRUPO_TAG_FISICO, usuario);
            }
        });
    }

    /**
     * Foto del examen tal como quedo en el WS (lectura fresca, aplanada) + tipo de examen del
     * tag del legacy. Nunca lanza: el guardado ya ocurrio y un fallo aqui no debe reportarse
     * como error del examen.
     */
    private void registrarHistorico(String nss, String origen, String dictamen) {
        try {
            Map<String, String> data = client.getExamenData(nss);
            String tipo = null;
            try {
                tipo = tags.latestByNssAndTypePrefix(nss, "tipoExamenInputO").get("tipoExamenInputO");
            } catch (RuntimeException ex) {
                // sin tag: queda NULL
            }
            if (tipo != null && (tipo.isBlank() || "0".equals(tipo.trim()))) {
                tipo = null;
            }
            String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(data);
            String usuario = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() == null
                    ? "SISTEMA" : org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
            historico.registrar(nss, origen, tipo, dictamen, usuario, json);
        } catch (Exception ex) {
            log.warn("[examen] no se pudo registrar el historico de {} ({}): {}", nss, origen, ex.getMessage());
        }
    }

    /** Dictamen vigente en el WS (apto/no_apto/...), o vacio si no hay. Para el candado del examen inicial. */
    public String dictamenVigente(String nss) {
        try {
            return dictamenDesdeFlags(datosExamen(normalizeNss(nss)), "SERV_MED_RESULTADO_EXAMEN");
        } catch (RuntimeException ex) {
            return "";
        }
    }

    /** Todas las secciones de una vez (para "Abrir todo": 1 peticion HTTP y 1 lectura del WS). */
    public Map<String, List<ExamItem>> itemsDeTodas(String nss) {
        Map<String, String> data = new java.util.LinkedHashMap<>(datosExamen(normalizeNss(nss)));
        completarDesdeTags(data, normalizeNss(nss));
        cargarCamposTag(data, normalizeNss(nss));
        Map<String, List<ExamItem>> out = new LinkedHashMap<>();
        for (Map.Entry<String, Seccion> e : CATALOGO.entrySet()) {
            out.put(e.getKey(), itemsDesde(e.getValue(), data));
        }
        return out;
    }

    private static final DateTimeFormatter FECHA_REPORTE = DateTimeFormatter.ofPattern("dd/MM/yy");

    /**
     * Reporte administrativo de dictamenes por rango de fechas (todas las NSS), para
     * el dashboard. fechaInicial/fechaFinal llegan en formato ISO (yyyy-MM-dd) y se
     * reformatean a "dd/MM/yy". SIN datos retroactivos (ver ExamenClient.reportePorFecha).
     */
    public List<ExamenReporteDto> reportePorFecha(String fechaInicial, String fechaFinal) {
        LocalDate desde = parseFechaIso(fechaInicial, "inicial");
        LocalDate hasta = parseFechaIso(fechaFinal, "final");
        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la fecha final");
        }
        return client.reportePorFecha(desde.format(FECHA_REPORTE), hasta.format(FECHA_REPORTE));
    }

    private static LocalDate parseFechaIso(String valor, String etiqueta) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("La fecha " + etiqueta + " es obligatoria");
        }
        try {
            return LocalDate.parse(valor.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("La fecha " + etiqueta + " no es valida", ex);
        }
    }

    private String normalizeNss(String nss) {
        if (nss == null || nss.isBlank()) {
            throw new IllegalArgumentException("El NSS es obligatorio");
        }
        return nss.trim();
    }
}
