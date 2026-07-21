package com.onest.app.catalog.examen.service;

import com.onest.app.catalog.examen.client.ExamenClient;
import com.onest.app.catalog.examen.dto.ExamItem;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Examen medico (U07) - Slices 1-2: heredofamiliares + APNP / Inmunizaciones / AGO.
 * Carga los datos del WS (consulta_examen, aplanados) y arma cada seccion para la vista.
 * El guardado completo se implementa en un slice posterior (evita clobber parcial).
 */
@Service
public class ExamenService {

    // type: SINO (Si/No + OBS opcional) | TEXT | DATE
    private record Campo(String field, String label, String type, boolean hasObs) {
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

    private static Campo area(String f, String l) {
        return new Campo(f, l, "AREA", false);
    }

    private static final Map<String, Seccion> CATALOGO = new LinkedHashMap<>();

    static {
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

        // ---- Inmunizaciones: vacunas Si/No + fecha ----
        CATALOGO.put("Inmunizaciones", new Seccion("SERV_MED_INMUNIZACIONES", List.of(
                sino0("BLG", "BCG"),
                sino0("SABIN ", "Sabin"),
                sino0("DPT", "DPT"),
                sino0("HEPATITIS_B", "Hepatitis B"),
                sino0("SR", "SR"),
                sino0("INFLUENZA", "Influenza"),
                sino0("TD", "Td"),
                sino0("COVID", "COVID"),
                date("FECHA", "Fecha"))));

        // ---- Antecedentes gineco-obstetricos (AGO): texto ----
        CATALOGO.put("Gineco-obstétricos (AGO)", new Seccion("SERV_MED_ANT_GIN_OBSTETRICOS", List.of(
                text("MENARCA", "Menarca"),
                text("RITMO", "Ritmo"),
                text("IVSA", "IVSA"),
                text("FUM", "FUM"),
                text("G", "Gesta (G)"),
                text("A", "Abortos (A)"),
                text("P", "Partos (P)"),
                text("C", "Cesáreas (C)"),
                text("MET_PLANIFICACION", "Método de planificación"),
                text("PAPANICOLAOU", "Papanicolaou"),
                text("INFECC_REP", "Infecciones (rep.)"),
                text("FUP", "FUP"))));

        // ---- Antecedentes personales patologicos (APP): Si/No (+ obs) ----
        CATALOGO.put("APP: personales patológicos", new Seccion("SERV_MED_ANT_PER_PATOLOGICOS", List.of(
                sino0("ENF_CONGENITAS", "Enf. congénitas"),
                sino0("GENITAL", "Genital"),
                sino0("ENF_PROP_INF", "Enf. propias de la infancia"),
                sino0("DERMATOPATIAS", "Dermatopatías"),
                sino0("SIST_AUDITIVO", "Sistema auditivo"),
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
                sino("GASTRITIS", "Gastritis"),
                sino("HEMORROIDES", "Hemorroides"),
                sino("CIRUGIAS", "Cirugías"))));
        CATALOGO.put("APP: renal", new Seccion("SERV_MED_RENAL", List.of(
                sino("INCONTINENSIA", "Incontinencia"),
                sino("INSUFICIENCIA", "Insuficiencia"),
                sino("LITIASIS", "Litiasis"))));
        CATALOGO.put("APP: sistema nervioso", new Seccion("SERV_MED_SIST_NERVIOSO", List.of(
                sino("ANEURISMA", "Aneurisma"),
                sino("ENF_DEG_MUS_NEU", "Enf. degenerativa musc./neuro."),
                sino("ACC_CER_VASCU", "Acc. cerebrovascular"),
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
                sino("ALT_CARGA ", "Alteración de carga"),
                sino("PERFORACIONES", "Perforaciones"),
                sino("TATUAJES", "Tatuajes"),
                sino("ALERGIAS", "Alergias"))));
        CATALOGO.put("APP: endocrino", new Seccion("SERV_MED_ENDOCRI_NO_PATO", List.of(
                sino("DIABETES", "Diabetes"),
                sino("HIPERTIROIDISMO", "Hipertiroidismo"),
                sino("HIPOTIROIDISMO", "Hipotiroidismo"))));

        // ---- Padecimiento actual / Interrogatorio (textarea) ----
        CATALOGO.put("Padecimiento actual", new Seccion("SERV_MED_PADECI_ACTUAL", List.of(
                area("OBSERVACIONES", "Observaciones"))));
        CATALOGO.put("Interrogatorio por aparatos", new Seccion("SERV_MED_INT_APARATO_SIST", List.of(
                area("OBSERVACIONES", "Observaciones"))));

        // ---- Exploracion fisica (signos, somatotipo, actitud) ----
        CATALOGO.put("Exploración física", new Seccion("SERV_MED_EXPLORACION_FISICA", List.of(
                text("PESO", "Peso"), text("TALLA", "Talla"), text("IMC", "IMC"),
                text("FC", "FC"), text("FR", "FR"), text("TA", "TA"), text("TEMP", "Temperatura"),
                text("ECTOMORFICO", "Ectomórfico"), text("MESOMORFO", "Mesomorfo"), text("ENDOMORFICA", "Endomórfica"),
                text("SATISFACCION", "Satisfacción"), text("COMP_TRABAJO", "Comp. trabajo"), text("COMP_ORGANIZACION", "Comp. organización"),
                text("TIEMPO", "Tiempo"), text("ESPACIO", "Espacio"), text("PERSONA", "Persona"),
                text("SERENIDAD", "Serenidad"), text("INTERES", "Interés"), text("ANTAGONICO", "Antagónico"),
                text("ENTUSIASTA", "Entusiasta"), text("ENOJO", "Enojo"), text("HIPOCRITA", "Hipócrita"),
                text("ICTERICA", "Ictérica"), text("ANEMICA", "Anémica"), text("TIROIDEA", "Tiroidea"),
                text("NOLMAL_FASCIES", "Facies normal"), text("PARKINSON", "Parkinson"), text("HEMIPLEJICO", "Hemipléjico"),
                text("ATAXICA", "Atáxica"), text("HEMIPARESIA", "Hemiparesia"), text("NORMAL_MARCHA", "Marcha normal"),
                text("LENGUAJE", "Lenguaje"), text("CONVER_FLUIDA", "Conversación fluida"), text("COHERENCIA_PALABLAS", "Coherencia de palabras"))));
        CATALOGO.put("Cráneo", new Seccion("SERV_MED_CRANEO", List.of(
                text("OJOS", "Ojos"), text("PUPILAS", "Pupilas"), text("CONJUNTIVAS", "Conjuntivas"),
                text("REFLEJOS", "Reflejos"), text("FONDO_OJO", "Fondo de ojo"))));
        CATALOGO.put("Agudeza visual", new Seccion("SERV_MED_AGUDEZA_VISUAL", List.of(
                text("OD", "Ojo derecho"), text("OIZ", "Ojo izquierdo"), text("COLORES", "Colores"))));
        CATALOGO.put("Nariz", new Seccion("SERV_MED_NARIZ", List.of(
                text("CAVIDAD_NASAL", "Cavidad nasal"), text("MUCOSA", "Mucosa"),
                text("TABIQUE_NASA", "Tabique nasal"), text("OLFATO", "Olfato"))));
        CATALOGO.put("Columna vertebral", new Seccion("SERV_MED_COLUMNA_VERTEBRAL", List.of(
                text("CERVICA", "Cervical"), text("DORSAL", "Dorsal"), text("LUMBAR", "Lumbar"))));
        CATALOGO.put("Boca", new Seccion("SERV_MED_BOCA", List.of(
                text("LESIONES", "Lesiones"), text("ENCIAS", "Encías"), text("OROIFARINGE", "Orofaringe"))));
        CATALOGO.put("Oídos", new Seccion("SERV_MED_OIDOS", List.of(
                text("CAE", "CAE"), text("MEM_TIM", "Membrana timpánica"), text("AGUD_AUDIT", "Agudeza auditiva"),
                text("OD", "Oído derecho"), text("OI", "Oído izquierdo"))));
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
                text("DIENTE_2", "21"), text("DIENTE_22", "22"), text("DIENTE_23", "23"), text("DIENTE_24", "24"),
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

    private final ExamenClient client;

    public ExamenService(ExamenClient client) {
        this.client = client;
    }

    /** Nombres de las secciones (orden de navegacion). */
    public List<String> secciones() {
        return new ArrayList<>(CATALOGO.keySet());
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
        Map<String, String> data = client.getExamenData(normalizeNss(nss));
        List<ExamItem> items = new ArrayList<>();
        for (Campo campo : sec.campos()) {
            String base = sec.dataKey() + "." + campo.field();
            String obsName = campo.hasObs() ? base + "_OBS" : null;
            String obsValue = campo.hasObs() ? data.getOrDefault(base + "_OBS", "") : null;
            items.add(new ExamItem(
                    campo.type(),
                    campo.label(),
                    base,
                    data.getOrDefault(base, ""),
                    obsName,
                    obsValue));
        }
        return items;
    }

    /** Guarda el examen (CAMBIO). campos = name->value de las secciones cargadas. */
    public String guardar(String nss, Map<String, String> campos, String firma) {
        return client.guardar(normalizeNss(nss), campos == null ? Map.of() : campos, firma);
    }

    private String normalizeNss(String nss) {
        if (nss == null || nss.isBlank()) {
            throw new IllegalArgumentException("El NSS es obligatorio");
        }
        return nss.trim();
    }
}
