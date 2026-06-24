<?php
class vistas
{
    public function __construct()
    {
        require_once('templates.php');
    }
    public function examenMedico()
    {
        $template = new Template("../../view/content/examenMedico.html");
        echo $template;
    }
    public function expHeroFam()
    {
        $template = new Template("../../view/content/menuHeredoFam.html");
        echo $template;
    }
    public function viewExpHeredoFam($type, $nss)
    {
        require_once('app.php');
        $json = new app;
        $others = new another();
        $jason =  $json->getDatsNss($nss);
        $decoded = json_decode($jason);
        foreach ($decoded->Datos as $key) {
            $FECHAExam = $key->Data->FECHA;
            $avc = $key->Data->NEUROLOGIA->AVC;
            $EPILEPSIA = $key->Data->NEUROLOGIA->EPILEPSIA;
            $PARALISIS_FACIAL = $key->Data->NEUROLOGIA->PARALISIS_FACIAL;
            $PARKINSON = $key->Data->NEUROLOGIA->PARKINSON;
            $ALZHEIMER = $key->Data->NEUROLOGIA->ALZHEIMER;
            $MIGRAÑA = $key->Data->NEUROLOGIA->MIGRAÑA;
            $AVC_OBS = $key->Data->NEUROLOGIA->AVC_OBS;
            $EPILEPSIA_OBS = $key->Data->NEUROLOGIA->EPILEPSIA_OBS;
            $PARALISIS_FACIAL_OBS = $key->Data->NEUROLOGIA->PARALISIS_FACIAL_OBS;
            $PARKINSON_OBS = $key->Data->NEUROLOGIA->PARKINSON_OBS;
            $ALZHEIMER_OBS = $key->Data->NEUROLOGIA->ALZHEIMER_OBS;
            $MIGRAÑA_OBS = $key->Data->NEUROLOGIA->MIGRAÑA_OBS;
            /* cardiopatia */
            $CARDIOPATIA_ISQUEMICA = $key->Data->CARDIOPATIA->CARDIOPATIA_ISQUEMICA;
            $HIPERTENSION = $key->Data->CARDIOPATIA->HIPERTENSION;
            $HIPOTENSION = $key->Data->CARDIOPATIA->HIPOTENSION;
            $INSUFICIENCIA_CARDIACA = $key->Data->CARDIOPATIA->INSUFICIENCIA_CARDIACA;
            $INSUFICIENCIA_VASCULAR_PERIFERICA = $key->Data->CARDIOPATIA->INSUFICIENCIA_VASCULAR_PERIFERICA;
            $CARDIOPATIA_ISQUEMICA_OBS = $key->Data->CARDIOPATIA->CARDIOPATIA_ISQUEMICA_OBS;
            $HIPERTENSION_OBS = $key->Data->CARDIOPATIA->HIPERTENSION_OBS;
            $HIPOTENSION_OBS = $key->Data->CARDIOPATIA->HIPOTENSION_OBS;
            $INSUFICIENCIA_CARDIACA_OBS = $key->Data->CARDIOPATIA->INSUFICIENCIA_CARDIACA_OBS;
            $INSUFICIENCIA_VASCULAR_PERIFERICA_OBS = $key->Data->CARDIOPATIA->INSUFICIENCIA_VASCULAR_PERIFERICA_OBS;
            /* Neumopatica */
            $BRONQUITIS = $key->Data->NEUMOPATICA->BRONQUITIS;
            $TUBERCULOSIS = $key->Data->NEUMOPATICA->TUBERCULOSIS;
            $ASMA = $key->Data->NEUMOPATICA->ASMA;
            $INSUFICIENCIA_RESPIRATORIA = $key->Data->NEUMOPATICA->INSUFICIENCIA_RESPIRATORIA;
            $BRONQUITIS_OBS = $key->Data->NEUMOPATICA->BRONQUITIS_OBS;
            $TUBERCULOSIS_OBS = $key->Data->NEUMOPATICA->TUBERCULOSIS_OBS;
            $ASMA_OBS = $key->Data->NEUMOPATICA->ASMA_OBS;
            $INSUFICIENCIA_RESPIRATORIA_OBS = $key->Data->NEUMOPATICA->INSUFICIENCIA_RESPIRATORIA_OBS;
            /* Toxicologico */
            $ALCOHOLISMO = $key->Data->TOXICOLOGIOCAS->ALCOHOLISMO;
            $TABAQUISMO = $key->Data->TOXICOLOGIOCAS->TABAQUISMO;
            $DROGAS = $key->Data->TOXICOLOGIOCAS->DROGAS;
            $ALCOHOLISMO_OBS = $key->Data->TOXICOLOGIOCAS->ALCOHOLISMO_OBS;
            $TABAQUISMO_OBS = $key->Data->TOXICOLOGIOCAS->TABAQUISMO_OBS;
            $DROGAS_OBS = $key->Data->TOXICOLOGIOCAS->DROGAS_OBS;
            /* Nefropatias */
            $IRC = $key->Data->NEFROPATIA->IRC;
            $LITASIS = $key->Data->NEFROPATIA->LITASIS;
            $IRC_OBS = $key->Data->NEFROPATIA->IRC_OBS;
            $LITASIS_OBS = $key->Data->NEFROPATIA->LITASIS_OBS;
            /* Endocrinas */
            $DM = $key->Data->ENDOCRINA->DM;
            $HIPERTIROIDISMO = $key->Data->ENDOCRINA->HIPERTIROIDISMO;
            $HIPOTIROIDISMO = $key->Data->ENDOCRINA->HIPOTIROIDISMO;
            $DM_OBS = $key->Data->ENDOCRINA->DM_OBS;
            $HIPERTIROIDISMO_OBS = $key->Data->ENDOCRINA->HIPERTIROIDISMO_OBS;
            $HIPOTIROIDISMO_OBS = $key->Data->ENDOCRINA->HIPOTIROIDISMO_OBS;
            /* Obesidad */
            $TRANSTORNO_DE_CRECIMIENTO = $key->Data->OBESIDAD->TRANSTORNO_DE_CRECIMIENTO;
            $TRANSTORNO_DE_CRECIMIENTO_OBS = $key->Data->OBESIDAD->TRANSTORNO_DE_CRECIMIENTO_OBS;
            /* Mentales */
            $NEUROSIS = $key->Data->MENTALES->NEUROSIS;
            $PSICOSIS = $key->Data->MENTALES->PSICOSIS;
            $INTENTO_DE_SUICIDIO = $key->Data->MENTALES->INTENTO_DE_SUICIDIO;
            $ESQUIZOFRENIA = $key->Data->MENTALES->ESQUIZOFRENIA;
            $NEUROSIS_OBS = $key->Data->MENTALES->NEUROSIS_OBS;
            $PSICOSIS_OBS = $key->Data->MENTALES->PSICOSIS_OBS;
            $INTENTO_DE_SUICIDIO_OBS = $key->Data->MENTALES->INTENTO_DE_SUICIDIO_OBS;
            $ESQUIZOFRENIA_OBS = $key->Data->MENTALES->ESQUIZOFRENIA_OBS;
            /* Generales */
            $CANCER = $key->Data->GENERALES->CANCER;
            $ALERGIAS = $key->Data->GENERALES->ALERGIAS;
            $MALFORMACIONES = $key->Data->GENERALES->MALFORMACIONES;
            $CANCER_OBS = $key->Data->GENERALES->CANCER_OBS;
            $ALERGIAS_OBS = $key->Data->GENERALES->ALERGIAS_OBS;
            $MALFORMACIONES_OBS = $key->Data->GENERALES->MALFORMACIONES_OBS;
            /* Otras */
            $OTRAS = $key->Data->OTRAS->OTRAS;
            $OTRAS_OBS = $key->Data->OTRAS->OTRAS_OBS;
            /* APNP */
            $HABITACION = $key->Data->SERV_MED_ANT_PATOLOGICOS->HABITACION;
            $NO_DORMITORIOS = $key->Data->SERV_MED_ANT_PATOLOGICOS->NO_DORMITORIOS;
            $NO_HABITANTES = $key->Data->SERV_MED_ANT_PATOLOGICOS->NO_HABITANTES;
            $PISO = $key->Data->SERV_MED_ANT_PATOLOGICOS->PISO;
            $TECHO = $key->Data->SERV_MED_ANT_PATOLOGICOS->TECHO;
            $AGUA_POTABLE = $key->Data->SERV_MED_ANT_PATOLOGICOS->AGUA_POTABLE;
            $LUZ = $key->Data->SERV_MED_ANT_PATOLOGICOS->LUZ;
            $DRENAJE = $key->Data->SERV_MED_ANT_PATOLOGICOS->DRENAJE;
            $CONVIVENCIA_CON_ANIMALES = $key->Data->SERV_MED_ANT_PATOLOGICOS->CONVIVENCIA_CON_ANIMALES;
            $AIRE_ACONDICIONADO = $key->Data->SERV_MED_ANT_PATOLOGICOS->{'AIRE_ACONDICIONADO'};
            $RELIGION = $key->Data->SERV_MED_ANT_PATOLOGICOS->RELIGION;
            $HIGIENE_PERSONAL = $key->Data->SERV_MED_ANT_PATOLOGICOS->HIGIENE_PERSONAL;
            $BAÑO = $key->Data->SERV_MED_ANT_PATOLOGICOS->BAÑO;
            $CAMBIO_DE_ROPA = $key->Data->SERV_MED_ANT_PATOLOGICOS->CAMBIO_DE_ROPA;
            $ASEO_BUCAL = $key->Data->SERV_MED_ANT_PATOLOGICOS->ASEO_BUCAL;
            $ALIMENTACION = $key->Data->SERV_MED_ANT_PATOLOGICOS->ALIMENTACION;
            $NO_COMIDAS_AL_DIA = $key->Data->SERV_MED_ANT_PATOLOGICOS->NO_COMIDAS_AL_DIA;
            $BALANCEADA = $key->Data->SERV_MED_ANT_PATOLOGICOS->BALANCEADA;
            $SUFICIENTE = $key->Data->SERV_MED_ANT_PATOLOGICOS->SUFICIENTE;
            $DEPORTE = $key->Data->SERV_MED_ANT_PATOLOGICOS->DEPORTE;
            $FC1 = $key->Data->SERV_MED_ANT_PATOLOGICOS->FC1;
            $HOBBIES_PASATIEMPOS = $key->Data->SERV_MED_ANT_PATOLOGICOS->HOBBIES_PASATIEMPOS;
            $FC2 = $key->Data->SERV_MED_ANT_PATOLOGICOS->FC2;
            /* Inmunizaciones */
            $BLG = $key->Data->SERV_MED_INMUNIZACIONES->BLG;
            $SABIN = $key->Data->SERV_MED_INMUNIZACIONES->{'SABIN '};
            $DPT = $key->Data->SERV_MED_INMUNIZACIONES->DPT;
            $HEPATITIS_B = $key->Data->SERV_MED_INMUNIZACIONES->HEPATITIS_B;
            $SR = $key->Data->SERV_MED_INMUNIZACIONES->SR;
            $INFLUENZA = $key->Data->SERV_MED_INMUNIZACIONES->INFLUENZA;
            $TD = $key->Data->SERV_MED_INMUNIZACIONES->TD;
            $COVID = $key->Data->SERV_MED_INMUNIZACIONES->COVID;
            $FECHA = $key->Data->SERV_MED_INMUNIZACIONES->FECHA;
            /* AGO */
            $MENARCA = $key->Data->SERV_MED_ANT_GIN_OBSTETRICOS->{'MENARCA'};
            $RITMO = $key->Data->SERV_MED_ANT_GIN_OBSTETRICOS->{'RITMO'};
            $IVSA = $key->Data->SERV_MED_ANT_GIN_OBSTETRICOS->{'IVSA'};
            $FUM = $key->Data->SERV_MED_ANT_GIN_OBSTETRICOS->{'FUM'};
            $G = $key->Data->SERV_MED_ANT_GIN_OBSTETRICOS->{'G'};
            $A = $key->Data->SERV_MED_ANT_GIN_OBSTETRICOS->{'A'};
            $P = $key->Data->SERV_MED_ANT_GIN_OBSTETRICOS->{'P'};
            $C = $key->Data->SERV_MED_ANT_GIN_OBSTETRICOS->{'C'};
            $MET_PLANIFICACION = $key->Data->SERV_MED_ANT_GIN_OBSTETRICOS->{'MET_PLANIFICACION'};
            $PAPANICOLAOU = $key->Data->SERV_MED_ANT_GIN_OBSTETRICOS->PAPANICOLAOU;
            $INFECC_REP = $key->Data->SERV_MED_ANT_GIN_OBSTETRICOS->INFECC_REP;
            $FUP = $key->Data->SERV_MED_ANT_GIN_OBSTETRICOS->{'FUP'};
            /* ANTECEDENTES PERSONALES  PATOLÓGICOS */
            $ENF_CONGENITAS = $key->Data->SERV_MED_ANT_PER_PATOLOGICOS->ENF_CONGENITAS;
            $GENITAL = $key->Data->SERV_MED_ANT_PER_PATOLOGICOS->{'GENITAL'};
            $ENF_PROP_INF = $key->Data->SERV_MED_ANT_PER_PATOLOGICOS->{'ENF_PROP_INF'};
            $DERMATOPATIAS = $key->Data->SERV_MED_ANT_PER_PATOLOGICOS->{'DERMATOPATIAS'};
            $SIST_AUDITIVO = $key->Data->SERV_MED_ANT_PER_PATOLOGICOS->{'SIST_AUDITIVO'};
            $RESPIRATORIO = $key->Data->SERV_MED_ANT_PER_PATOLOGICOS->{'RESPIRATORIO'};
            $SIS_OLFATIVO = $key->Data->SERV_MED_ANT_PER_PATOLOGICOS->{'SIS_OLFATIVO'};
            $TRAUMATICOS = $key->Data->SERV_MED_ANT_PER_PATOLOGICOS->{'TRAUMATICOS'};
            /* pt1 */
            $USO_LENTES = $key->Data->SERV_MED_OFTALMOLOGICO->{'USO_LENTES'};
            $USO_LENTES_OBS = $key->Data->SERV_MED_OFTALMOLOGICO->{'USO_LENTES_OBS'};
            $ASTIGMATISMO = $key->Data->SERV_MED_OFTALMOLOGICO->{'ASTIGMATISMO'};
            $ASTIGMATISMO_OBS = $key->Data->SERV_MED_OFTALMOLOGICO->{'ASTIGMATISMO_OBS'};
            $PRESBICIA = $key->Data->SERV_MED_OFTALMOLOGICO->{'PRESBICIA'};
            $PRESBICIA_OBS = $key->Data->SERV_MED_OFTALMOLOGICO->{'PRESBICIA_OBS'};
            $ESTRABISMO = $key->Data->SERV_MED_OFTALMOLOGICO->{'ESTRABISMO'};
            $ESTRABISMO_OBS = $key->Data->SERV_MED_OFTALMOLOGICO->{'ESTRABISMO_OBS'};
            $DALTONISMO = $key->Data->SERV_MED_OFTALMOLOGICO->{'DALTONISMO'};
            $DALTONISMO_OBS = $key->Data->SERV_MED_OFTALMOLOGICO->{'DALTONISMO_OBS'};
            $DESPENDIMIENTO_RET = $key->Data->SERV_MED_OFTALMOLOGICO->{'DESPENDIMIENTO_RET'};
            $DESPENDIMIENTO_RET_OBS = $key->Data->SERV_MED_OFTALMOLOGICO->{'DESPENDIMIENTO_RET_OBS'};
            $MIOPIA = $key->Data->SERV_MED_OFTALMOLOGICO->{'MIOPIA'};
            $MIOPE_OBS = $key->Data->SERV_MED_OFTALMOLOGICO->{'MIOPIA_OBS'};
            /* pt2 */
            $HERNIAS = $key->Data->SERV_MED_DIGESTIVO->{'HERNIAS'};
            $HERNIAS_OBS = $key->Data->SERV_MED_DIGESTIVO->{'HERNIAS_OBS'};
            $CIRROSIS = $key->Data->SERV_MED_DIGESTIVO->{'CIRROSIS'};
            $CIRROSIS_OBS = $key->Data->SERV_MED_DIGESTIVO->{'CIRROSIS_OBS'};
            $COLITIS = $key->Data->SERV_MED_DIGESTIVO->{'COLITIS'};
            $COLITIS_OBS = $key->Data->SERV_MED_DIGESTIVO->{'COLITIS_OBS'};
            $GASTRITIS = $key->Data->SERV_MED_DIGESTIVO->{'GASTRITIS'};
            $GASTRITIS_OBS = $key->Data->SERV_MED_DIGESTIVO->{'GASTRITIS_OBS'};
            $HEMORROIDES = $key->Data->SERV_MED_DIGESTIVO->{'HEMORROIDES'};
            $HEMORROIDES_OBS = $key->Data->SERV_MED_DIGESTIVO->{'HEMORROIDES_OBS'};
            $CIRUGIAS = $key->Data->SERV_MED_DIGESTIVO->{'CIRUGIAS'};
            $CIRUGIAS_OBS = $key->Data->SERV_MED_DIGESTIVO->{'CIRUGIAS_OBS'};
            /* pt3 */
            $INCONTINENSIA = $key->Data->SERV_MED_RENAL->{'INCONTINENSIA'};
            $INCONTINENSIA_OBS = $key->Data->SERV_MED_RENAL->{'INCONTINENSIA_OBS'};
            $INSUFICIENCIA = $key->Data->SERV_MED_RENAL->{'INSUFICIENCIA'};
            $INSUFICIENCIA_OBS = $key->Data->SERV_MED_RENAL->{'INSUFICIENCIA_OBS'};
            $LITIASIS = $key->Data->SERV_MED_RENAL->{'LITIASIS'};
            $LITIASIS_OBS = $key->Data->SERV_MED_RENAL->{'LITIASIS_OBS'};
            /* pt4 */
            $ANEURISMA = $key->Data->SERV_MED_SIST_NERVIOSO->{'ANEURISMA'};
            $ANEURISMA_OBS = $key->Data->SERV_MED_SIST_NERVIOSO->{'ANEURISMA_OBS'};
            $ENF_DEG_MUS_NEU = $key->Data->SERV_MED_SIST_NERVIOSO->{'ENF_DEG_MUS_NEU'};
            $ENF_DEG_MUS_NEU_OBS = $key->Data->SERV_MED_SIST_NERVIOSO->{'ENF_DEG_MUS_NEU_OBS'};
            $ACC_CER_VASCU = $key->Data->SERV_MED_SIST_NERVIOSO->{'ACC_CER_VASCU'};
            $ACC_CER_VASCU_OBS = $key->Data->SERV_MED_SIST_NERVIOSO->{'ACC_CER_VASCU_OBS'};
            $EPILEPSIA2 = $key->Data->SERV_MED_SIST_NERVIOSO->EPILEPSIA;
            $EPILEPSIA_OBS2 = $key->Data->SERV_MED_SIST_NERVIOSO->EPILEPSIA_OBS;
            $PAR_FACIAL = $key->Data->SERV_MED_SIST_NERVIOSO->{'PAR_FACIAL'};
            $PAR_FACIAL_OBS = $key->Data->SERV_MED_SIST_NERVIOSO->{'PAR_FACIAL_OBS'};
            /* pt5 */
            $ESCOLIOSIS = $key->Data->SERV_MED_MUS_ESQUELETICO->{'ESCOLIOSIS'};
            $ESCOLIOSIS_OBS = $key->Data->SERV_MED_MUS_ESQUELETICO->{'ESCOLIOSIS_OBS'};
            $XIFOSIS = $key->Data->SERV_MED_MUS_ESQUELETICO->{'XIFOSIS'};
            $XIFOSIS_OBS = $key->Data->SERV_MED_MUS_ESQUELETICO->{'XIFOSIS_OBS'};
            $LORDOSISI = $key->Data->SERV_MED_MUS_ESQUELETICO->LORDOSISI;
            $LORDOSISI_OBS = $key->Data->SERV_MED_MUS_ESQUELETICO->LORDOSIS_OBS;
            $LUMBALGIA = $key->Data->SERV_MED_MUS_ESQUELETICO->{'LUMBALGIA '};
            $LUMBALGIA_OBS = $key->Data->SERV_MED_MUS_ESQUELETICO->{'LUMBALGIA _OBS'};
            $BASCULAMIENTO = $key->Data->SERV_MED_MUS_ESQUELETICO->{'BASCULAMIENTO'};
            $BASCULAMIENTO_OBS = $key->Data->SERV_MED_MUS_ESQUELETICO->{'BASCULAMIENTO_OBS'};
            $ALTERACION_DISCAL = $key->Data->SERV_MED_MUS_ESQUELETICO->{'ALTERACION_DISCAL'};
            $ALTERACION_DISCAL_OBS = $key->Data->SERV_MED_MUS_ESQUELETICO->{'ALTERACION_DISCAL_OBS'};
            /* pt6 */
            $ANEURISMA2 = $key->Data->SERV_MED_CARDIO_NO_PATO->{'ANEURISMA'};
            $ANEURISMA_OBS2 = $key->Data->SERV_MED_CARDIO_NO_PATO->{'ANEURISMA_OBS'};
            $HIPERTENSION2 = $key->Data->SERV_MED_CARDIO_NO_PATO->{'HIPERTENSION'};
            $HIPERTENSION_OBS2 = $key->Data->SERV_MED_CARDIO_NO_PATO->{'HIPERTENSION_OBS'};
            $HIPOTENSION2 = $key->Data->SERV_MED_CARDIO_NO_PATO->{'HIPOTENSION'};
            $HIPOTENSION_OBS2 = $key->Data->SERV_MED_CARDIO_NO_PATO->{'HIPOTENSION_OBS'};
            $INFARTOS = $key->Data->SERV_MED_CARDIO_NO_PATO->{'INFARTOS '};
            $INFARTOS_OBS = $key->Data->SERV_MED_CARDIO_NO_PATO->{'INFARTOS_OBS'};
            $INSUFIC_CAR = $key->Data->SERV_MED_CARDIO_NO_PATO->{'INSUFIC_CAR'};
            $INSUFIC_CAR_OBS = $key->Data->SERV_MED_CARDIO_NO_PATO->{'INSUFIC_CAR_OBS'};
            /* pt7 */
            $TABAQUISMO2 = $key->Data->SERV_MED_TOXI_NO_PATO->{'TABAQUISMO'};
            $TABAQUISMO_OBS2 = $key->Data->SERV_MED_TOXI_NO_PATO->{'TABAQUISMO_OBS'};
            $ALCOHOLISMO2 = $key->Data->SERV_MED_TOXI_NO_PATO->{'ALCOHOLISMO'};
            $ALCOHOLISMO_OBS2 = $key->Data->SERV_MED_TOXI_NO_PATO->{'ALCOHOLISMO_OBS'};
            $DROGAS2 = $key->Data->SERV_MED_TOXI_NO_PATO->{'DROGAS'};
            $DROGAS_OBS2 = $key->Data->SERV_MED_TOXI_NO_PATO->{'DROGAS_OBS'};
            $ALT_CARGA = $key->Data->SERV_MED_TOXI_NO_PATO->{'ALT_CARGA '};
            $ALT_CARGA_OBS = $key->Data->SERV_MED_TOXI_NO_PATO->{'ALT_CARGA_OBS'};
            $PERFORACIONES = $key->Data->SERV_MED_TOXI_NO_PATO->{'PERFORACIONES'};
            $PERFORACIONES_OBS = $key->Data->SERV_MED_TOXI_NO_PATO->{'PERFORACIONES_OBS'};
            $TATUAJES = $key->Data->SERV_MED_TOXI_NO_PATO->{'TATUAJES'};
            $TATUAJES_OBS = $key->Data->SERV_MED_TOXI_NO_PATO->{'TATUAJES_OBS'};
            $ALERGIAS = $key->Data->SERV_MED_TOXI_NO_PATO->{'ALERGIAS'};
            $ALERGIAS_OBS = $key->Data->SERV_MED_TOXI_NO_PATO->{'ALERGIAS_OBS'};
            /* pt8 */
            $DIABETES = $key->Data->SERV_MED_ENDOCRI_NO_PATO->{'DIABETES'};
            $DIABETES_OBS = $key->Data->SERV_MED_ENDOCRI_NO_PATO->{'DIABETES_OBS'};
            $HIPERTIROIDISMO2 = $key->Data->SERV_MED_ENDOCRI_NO_PATO->{'HIPERTIROIDISMO'};
            $HIPERTIROIDISMO_OBS2 = $key->Data->SERV_MED_ENDOCRI_NO_PATO->{'HIPERTIROIDISMO_OBS'};
            $HIPOTIROIDISMO2 = $key->Data->SERV_MED_ENDOCRI_NO_PATO->{'HIPOTIROIDISMO'};
            $HIPOTIROIDISMO_OBS2 = $key->Data->SERV_MED_ENDOCRI_NO_PATO->{'HIPOTIROIDISMO_OBS'};
            /* PA */
            $OBSERVACIONES = $key->Data->SERV_MED_PADECI_ACTUAL->{'OBSERVACIONES'};
            /* IPAYS */
            $OBSERVACIONES2 = $key->Data->SERV_MED_INT_APARATO_SIST->{'OBSERVACIONES'};
            /* EXFIS */
            $PESO = $key->Data->SERV_MED_EXPLORACION_FISICA->{"PESO"};
            $TALLA = $key->Data->SERV_MED_EXPLORACION_FISICA->{"TALLA"};
            $IMC = $key->Data->SERV_MED_EXPLORACION_FISICA->{"IMC"};
            $FC = $key->Data->SERV_MED_EXPLORACION_FISICA->{"FC"};
            $FR = $key->Data->SERV_MED_EXPLORACION_FISICA->{"FR"};
            $TA = $key->Data->SERV_MED_EXPLORACION_FISICA->{"TA"};
            $TEMP = $key->Data->SERV_MED_EXPLORACION_FISICA->{"TEMP"};
            $ECTOMORFICO = $key->Data->SERV_MED_EXPLORACION_FISICA->{"ECTOMORFICO"};
            $MESOMORFO = $key->Data->SERV_MED_EXPLORACION_FISICA->{"MESOMORFO"};
            $ENDOMORFICA = $key->Data->SERV_MED_EXPLORACION_FISICA->{"ENDOMORFICA"};
            $SATISFACCION = $key->Data->SERV_MED_EXPLORACION_FISICA->{"SATISFACCION"};
            $COMP_TRABAJO = $key->Data->SERV_MED_EXPLORACION_FISICA->{"COMP_TRABAJO"};
            $COMP_ORGANIZACION = $key->Data->SERV_MED_EXPLORACION_FISICA->{"COMP_ORGANIZACION"};
            $TIEMPO = $key->Data->SERV_MED_EXPLORACION_FISICA->{"TIEMPO"};
            $ESPACIO = $key->Data->SERV_MED_EXPLORACION_FISICA->{"ESPACIO"};
            $PERSONA = $key->Data->SERV_MED_EXPLORACION_FISICA->{"PERSONA"};
            $SERENIDAD = $key->Data->SERV_MED_EXPLORACION_FISICA->{"SERENIDAD"};
            $INTERES = $key->Data->SERV_MED_EXPLORACION_FISICA->{"INTERES"};
            $ANTAGONICO = $key->Data->SERV_MED_EXPLORACION_FISICA->{"ANTAGONICO"};
            $ENTUSIASTA = $key->Data->SERV_MED_EXPLORACION_FISICA->{"ENTUSIASTA"};
            $ENOJO = $key->Data->SERV_MED_EXPLORACION_FISICA->{"ENOJO"};
            $HIPOCRITA = $key->Data->SERV_MED_EXPLORACION_FISICA->{"HIPOCRITA"};
            $ICTERICA = $key->Data->SERV_MED_EXPLORACION_FISICA->{"ICTERICA"};
            $ANEMICA = $key->Data->SERV_MED_EXPLORACION_FISICA->{"ANEMICA"};
            $TIROIDEA = $key->Data->SERV_MED_EXPLORACION_FISICA->{"TIROIDEA"};
            $NOLMAL_FASCIES = $key->Data->SERV_MED_EXPLORACION_FISICA->{"NOLMAL_FASCIES"};
            $PARKINSON2 = $key->Data->SERV_MED_EXPLORACION_FISICA->{"PARKINSON"};
            $HEMIPLEJICO = $key->Data->SERV_MED_EXPLORACION_FISICA->{"HEMIPLEJICO"};
            $ATAXICA = $key->Data->SERV_MED_EXPLORACION_FISICA->{"ATAXICA"};
            $HEMIPARESIA = $key->Data->SERV_MED_EXPLORACION_FISICA->{"HEMIPARESIA"};
            $NORMAL_MARCHA = $key->Data->SERV_MED_EXPLORACION_FISICA->{"NORMAL_MARCHA"};
            $LENGUAJE = $key->Data->SERV_MED_EXPLORACION_FISICA->{"LENGUAJE"};
            $CONVER_FLUIDA = $key->Data->SERV_MED_EXPLORACION_FISICA->{"CONVER_FLUIDA"};
            $COHERENCIA_PALABLAS = $key->Data->SERV_MED_EXPLORACION_FISICA->{"COHERENCIA_PALABLAS"};


            $OJOS = $key->Data->SERV_MED_CRANEO->{"OJOS"};
            $PUPILAS = $key->Data->SERV_MED_CRANEO->{"PUPILAS"};
            $CONJUNTIVAS = $key->Data->SERV_MED_CRANEO->{"CONJUNTIVAS"};
            $REFLEJOS = $key->Data->SERV_MED_CRANEO->{"REFLEJOS"};
            $FONDO_OJO = $key->Data->SERV_MED_CRANEO->{"FONDO_OJO"};


            $OD = $key->Data->SERV_MED_AGUDEZA_VISUAL->{"OD"};
            $OIZ = $key->Data->SERV_MED_AGUDEZA_VISUAL->{"OIZ"};
            $COLORES = $key->Data->SERV_MED_AGUDEZA_VISUAL->{"COLORES"};


            $CAVIDAD_NASAL = $key->Data->SERV_MED_NARIZ->{"CAVIDAD_NASAL"};
            $MUCOSA = $key->Data->SERV_MED_NARIZ->{"MUCOSA"};
            $TABIQUE_NASAL = $key->Data->SERV_MED_NARIZ->{"TABIQUE_NASA"};
            $OLFATO = $key->Data->SERV_MED_NARIZ->{"OLFATO"};


            $CERVICAL = $key->Data->SERV_MED_COLUMNA_VERTEBRAL->{"CERVICA"};
            $DORSAL = $key->Data->SERV_MED_COLUMNA_VERTEBRAL->{"DORSAL"};
            $LUMBAR = $key->Data->SERV_MED_COLUMNA_VERTEBRAL->{"LUMBAR"};


            $LESIONES = $key->Data->SERV_MED_BOCA->{"LESIONES"};
            $ENCIAS = $key->Data->SERV_MED_BOCA->{"ENCIAS"};
            $OROIFARINGE = $key->Data->SERV_MED_BOCA->{"OROIFARINGE"};


            $CAE = $key->Data->SERV_MED_OIDOS->{"CAE"};
            $MEM_TIM = $key->Data->SERV_MED_OIDOS->{"MEM_TIM"};
            $AGUD_AUDIT = $key->Data->SERV_MED_OIDOS->{"AGUD_AUDIT"};
            $OID = $key->Data->SERV_MED_OIDOS->{"OD"};
            $OI = $key->Data->SERV_MED_OIDOS->{"OI"};


            $RUIDOS_CARDIACOS = $key->Data->SERV_MED_TORAX->{"RUIDOS_CARDIACOS"};
            $REG_PRECORDIAL = $key->Data->SERV_MED_TORAX->{"REG_PRECORDIAL"};
            $CAMPOS_PULM = $key->Data->SERV_MED_TORAX->{"CAMPOS_PULM"};


            $DEFORMACIONES = $key->Data->SERV_MED_COLUMNA_VERTEBRAL_AUX->{"DEFORMACIONES"};
            $DOLOR = $key->Data->SERV_MED_COLUMNA_VERTEBRAL_AUX->{"DOLOR"};
            $MOVIMIENTOS = $key->Data->SERV_MED_COLUMNA_VERTEBRAL_AUX->{"MOVIMIENTOS"};
            $MARCHA = $key->Data->SERV_MED_COLUMNA_VERTEBRAL_AUX->{"MARCHA"};
            $LASSEGUE = $key->Data->SERV_MED_COLUMNA_VERTEBRAL_AUX->LASSEGUE;
            $PUNTA_TALON = $key->Data->SERV_MED_COLUMNA_VERTEBRAL_AUX->PUNTA_TALON;


            $FORMA = $key->Data->SERV_MED_ABDOMEN->{"FORMA"};
            $VISCEROMEGALIAS = $key->Data->SERV_MED_ABDOMEN->{"VISCEROMEGALIAS"};
            $HERNIAS2 = $key->Data->SERV_MED_ABDOMEN->{"HERNIAS"};
            $DOLOR = $key->Data->SERV_MED_ABDOMEN->{"DOLOR"};
            $PERISTALSIS = $key->Data->SERV_MED_ABDOMEN->{"PERISTALSIS"};


            $MASCULINO = $key->Data->SERV_MED_GENITALES->{"MASCULINO"};
            $FEMENINO = $key->Data->SERV_MED_GENITALES->{"FEMENINO"};


            $PUNTOS_URETRALES = $key->Data->SERV_MED_URINARIO->{"PUNTOS_URETRALES"};
            $FOSAS_RENALES = $key->Data->SERV_MED_URINARIO->{"FOSAS_RENALES"};


            $SUPERIOR = $key->Data->SERV_MED_EXTREMIDADES->{"SUPERIOR"};
            $INFERIOR = $key->Data->SERV_MED_EXTREMIDADES->{"INFERIOR"};
            $IVP = $key->Data->SERV_MED_EXTREMIDADES->{"IVP"};
            $EDEMA = $key->Data->SERV_MED_EXTREMIDADES->{"EDEMA"};


            $LUNARES = $key->Data->SERV_MED_PIEL->{"LUNARES"};
            $PIGMENTACION = $key->Data->SERV_MED_PIEL->{"PIGMENTACION"};
            $CICATRICES_QUIRUR = $key->Data->SERV_MED_PIEL->{"CICATRICES_QUIRUR"};

            $DIENTE_11 = $key->Data->SERV_MED_DIENTES->{"DIENTE_11"};
            $DIENTE_12 = $key->Data->SERV_MED_DIENTES->{"DIENTE_12"};
            $DIENTE_13 = $key->Data->SERV_MED_DIENTES->{"DIENTE_13"};
            $DIENTE_14 = $key->Data->SERV_MED_DIENTES->{"DIENTE_14"};
            $DIENTE_15 = $key->Data->SERV_MED_DIENTES->{"DIENTE_15"};
            $DIENTE_16 = $key->Data->SERV_MED_DIENTES->{"DIENTE_16"};
            $DIENTE_17 = $key->Data->SERV_MED_DIENTES->{"DIENTE_17"};
            $DIENTE_18 = $key->Data->SERV_MED_DIENTES->{"DIENTE_18"};
            $DIENTE_41 = $key->Data->SERV_MED_DIENTES->{"DIENTE_41"};
            $DIENTE_42 = $key->Data->SERV_MED_DIENTES->{"DIENTE_42"};
            $DIENTE_43 = $key->Data->SERV_MED_DIENTES->{"DIENTE_43"};
            $DIENTE_44 = $key->Data->SERV_MED_DIENTES->{"DIENTE_44"};
            $DIENTE_45 = $key->Data->SERV_MED_DIENTES->{"DIENTE_45"};
            $DIENTE_46 = $key->Data->SERV_MED_DIENTES->{"DIENTE_46"};
            $DIENTE_47 = $key->Data->SERV_MED_DIENTES->{"DIENTE_47"};
            $DIENTE_48 = $key->Data->SERV_MED_DIENTES->{"DIENTE_48"};
            $DIENTE_2 = $key->Data->SERV_MED_DIENTES->{"DIENTE_2"};
            $DIENTE_22 = $key->Data->SERV_MED_DIENTES->{"DIENTE_22"};
            $DIENTE_23 = $key->Data->SERV_MED_DIENTES->{"DIENTE_23"};
            $DIENTE_24 = $key->Data->SERV_MED_DIENTES->{"DIENTE_24"};
            $DIENTE_25 = $key->Data->SERV_MED_DIENTES->{"DIENTE_25"};
            $DIENTE_26 = $key->Data->SERV_MED_DIENTES->{"DIENTE_26"};
            $DIENTE_27 = $key->Data->SERV_MED_DIENTES->{"DIENTE_27"};
            $DIENTE_28 = $key->Data->SERV_MED_DIENTES->{"DIENTE_28"};
            $DIENTE_31 = $key->Data->SERV_MED_DIENTES->{"DIENTE_31"};
            $DIENTE_32 = $key->Data->SERV_MED_DIENTES->{"DIENTE_32"};
            $DIENTE_33 = $key->Data->SERV_MED_DIENTES->{"DIENTE_33"};
            $DIENTE_34 = $key->Data->SERV_MED_DIENTES->{"DIENTE_34"};
            $DIENTE_35 = $key->Data->SERV_MED_DIENTES->{"DIENTE_35"};
            $DIENTE_36 = $key->Data->SERV_MED_DIENTES->{"DIENTE_36"};
            $DIENTE_37 = $key->Data->SERV_MED_DIENTES->{"DIENTE_37"};
            $DIENTE_38 = $key->Data->SERV_MED_DIENTES->{"DIENTE_38"};

            $RESULTADOS3 = $key->Data->SERV_MED_ESTUDIOS_REALIZADOS->{"RESULTADOS"};
            $OBSERVACIONES3 = $key->Data->SERV_MED_DIAGNOSTICO->{"OBSERVACIONES"};
            $OBSERVACIONES32 = $key->Data->SERV_MED_PLAN_TERAPIA->{"OBSERVACIONES"};

            $DEFORMACIONES_OBS = $key->Data->SERV_MED_CUELLO->{"DEFORMACIONES_OBS"};
            $TIROIDES_OBS = $key->Data->SERV_MED_CUELLO->{"TIROIDES_OBS"};
            $TRAQUEA_OBS = $key->Data->SERV_MED_CUELLO->{"TRAQUEA_OBS"};

            $OBSERVACIONESRESEXAM = $key->Data->SERV_MED_RESULTADO_EXAMEN->{"OBSERVACIONES"};
        }
        $DRENAJE = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "Drenaje"
        ]);
        $CONVIVENCIA_CON_ANIMALES = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "CCA"
        ]);
        $VOZCLARAYFUERTE = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "VOZCLARAYFUERTE"
        ]);
        $STPO2 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "STPO2"
        ]);

        $edad_inicio_laborar = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "edad_inicio_laborar"
        ]);
        $cantidad_trabajos = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "cantidad_trabajos"
        ]);
        $pension = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "pension"
        ]);

        $nombre1 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "nombre1"
        ]);
        $giro1 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "giro1"
        ]);
        $puesto1 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "puesto1"
        ]);
        $turno1 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "turno1"
        ]);
        $antiguedad1 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "antiguedad1"
        ]);
        $salida1 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "salida1"
        ]);
        $descripcion1 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "descripcion1"
        ]);
        $riesgos1 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "riesgos1"
        ]);
        $epp1 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "epp1"
        ]);
        $observaciones1 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "observaciones1"
        ]);
        $nombre2 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "nombre2"
        ]);
        $giro2 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "giro2"
        ]);
        $puesto2 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "puesto2"
        ]);
        $turno2 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "turno2"
        ]);
        $antiguedad2 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "antiguedad2"
        ]);
        $salida2 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "salida2"
        ]);
        $descripcion2 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "descripcion2"
        ]);
        $riesgos2 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "riesgos2"
        ]);
        $epp2 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "epp2"
        ]);
        $observaciones2 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "observaciones2"
        ]);
        $nombre3 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "nombre3"
        ]);
        $giro3 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "giro3"
        ]);
        $puesto3 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "puesto3"
        ]);
        $turno3 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "turno3"
        ]);
        $antiguedad3 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "antiguedad3"
        ]);
        $salida3 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "salida3"
        ]);
        $descripcion3 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "descripcion3"
        ]);
        $riesgos3 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "riesgos3"
        ]);
        $epp3 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "epp3"
        ]);
        $observaciones3 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "observaciones3"
        ]);
        $nombre4 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "nombre4"
        ]);
        $giro4 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "giro4"
        ]);
        $puesto4 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "puesto4"
        ]);
        $turno4 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "turno4"
        ]);
        $antiguedad4 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "antiguedad4"
        ]);
        $salida4 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "salida4"
        ]);
        $descripcion4 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "descripcion4"
        ]);
        $riesgos4 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "riesgos4"
        ]);
        $epp4 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "epp4"
        ]);
        $observaciones4 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "observaciones4"
        ]);
        $OBSGENERALINPEX = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "OBSGENERALINPEX"
        ]);
        $ODEXCAEEXFISINP = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "ODEXCAEEXFISINP"
        ]);
        $ODCAEINPEXP = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "ODCAEINPEXP"
        ]);
        $ODMEMTIMINPEXP = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "ODMEMTIMINPEXP"
        ]);
        $ODAGAUDIINPEXP = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "ODAGAUDIINPEXP"
        ]);
        $OICAEINPEXP = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "OICAEINPEXP"
        ]);
        $OIMEMTIMINPEXP = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "OIMEMTIMINPEXP"
        ]);
        $OIAGAUDIINPEXP = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "OIAGAUDIINPEXP"
        ]);
        $TRAQUEA_OBS = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "TRAQUEA_OBSINP"
        ]);
        $DIENTE_2 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "DIENTE_2"
        ]);
        $CIRUGIAS_OBS = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "ciruOBS"
        ]);
        $LENTEOJODERECHOEXP = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "LENTEOJODERECHOEXP"
        ]);
        $LENTEOJOIZQUIERDOEXP = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "LENTEOJOIZQUIERDOEXP"
        ]);
        $ROMBERGEXFIS = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "ROMBERGEXFIS"
        ]);
        $contactoEmernombre1 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "contactoEmernombre1"
        ]);
        $contactoEmerapellido_paterno1 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "contactoEmerapellido_paterno1"
        ]);
        $contactoEmerapellido_materno1 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "contactoEmerapellido_materno1"
        ]);
        $contactoEmerparentesco1 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "contactoEmerparentesco1"
        ]);
        $contactoEmertelefono1 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "contactoEmertelefono1"
        ]);
        $contactoEmernombre2 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "contactoEmernombre2"
        ]);
        $contactoEmerapellido_paterno2 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "contactoEmerapellido_paterno2"
        ]);
        $contactoEmerapellido_materno2 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "contactoEmerapellido_materno2"
        ]);
        $contactoEmerparentesco2 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "contactoEmerparentesco2"
        ]);
        $contactoEmertelefono2 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "contactoEmertelefono2"
        ]);
        $contactoEmernombre3 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "contactoEmernombre3"
        ]);
        $contactoEmerapellido_paterno3 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "contactoEmerapellido_paterno3"
        ]);
        $contactoEmerapellido_materno3 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "contactoEmerapellido_materno3"
        ]);
        $contactoEmerparentesco3 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "contactoEmerparentesco3"
        ]);
        $contactoEmertelefono3 = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "contactoEmertelefono3"
        ]);
        $cadaCuandoDep = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "cadaCuandoDep"
        ]);
        $cadaCuandoPasa = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "cadaCuandoPasa"
        ]);
        $fechaINFLUEN = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "fechaINFLUEN"
        ]);
        $ODEXFISINPCERC = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "ODEXFISINPCERC"
        ]);
        $ODEXFISINPCERCIZQ = $others->selectDatsExMed($data = [
            "nss" => $nss,
            "type" => "ODEXFISINPCERCIZQ"
        ]);




        // Función para generar las opciones del select
        function generarOpciones($seleccionado)
        {
            $opciones = array('dia', 'semana', 'mes');

            // Valor seleccionado por defecto
            $seleccionado = isset($seleccionado) ? $seleccionado : '';
            $html = '';
            foreach ($opciones as $opcion) {
                // Si la opción es igual al valor seleccionado, añadir el atributo "selected"
                $selected = ($opcion == $seleccionado) ? 'selected' : '';
                $html .= '<option value="' . $opcion . '" ' . $selected . '>' . ucfirst($opcion) . '</option>';
            }
            return $html;
        }
        switch ($type) {
            case 'Neurología':
                $form = new Template("../../view/content/formsHeredoFam/neurologia.html", $data = [
                    "avc" => $avc,
                    "EPILEPSIA" => $EPILEPSIA,
                    "PARALISIS_FACIAL" => $PARALISIS_FACIAL,
                    "PARKINSON" => $PARKINSON,
                    "ALZHEIMER" => $ALZHEIMER,
                    "MIGRAÑA" => $MIGRAÑA,
                    "AVC_OBS" => $AVC_OBS,
                    "EPILEPSIA_OBS" => $EPILEPSIA_OBS,
                    "PARALISIS_FACIAL_OBS" => $PARALISIS_FACIAL_OBS,
                    "PARKINSON_OBS" => $PARKINSON_OBS,
                    "ALZHEIMER_OBS" => $ALZHEIMER_OBS,
                    "MIGRAÑA_OBS" => $MIGRAÑA_OBS,

                ]);
                break;
            case 'Cardiopatía':
                $form = new Template("../../view/content/formsHeredoFam/cardiopatia.html", $data = [
                    "CARDIOPATIA_ISQUEMICA" => $CARDIOPATIA_ISQUEMICA,
                    "HIPERTENSION" => $HIPERTENSION,
                    "HIPOTENSION" => $HIPOTENSION,
                    "INSUFICIENCIA_CARDIACA" => $INSUFICIENCIA_CARDIACA,
                    "INSUFICIENCIA_VASCULAR_PERIFERICA" => $INSUFICIENCIA_VASCULAR_PERIFERICA,
                    "CARDIOPATIA_ISQUEMICA_OBS" => $CARDIOPATIA_ISQUEMICA_OBS,
                    "HIPERTENSION_OBS" => $HIPERTENSION_OBS,
                    "HIPOTENSION_OBS" => $HIPOTENSION_OBS,
                    "INSUFICIENCIA_CARDIACA_OBS" => $INSUFICIENCIA_CARDIACA_OBS,
                    "INSUFICIENCIA_VASCULAR_PERIFERICA_OBS" => $INSUFICIENCIA_VASCULAR_PERIFERICA_OBS

                ]);
                break;
            case 'Neumopatía':
                $form = new Template("../../view/content/formsHeredoFam/neumopatia.html", $data = [
                    "BRONQUITIS" => $BRONQUITIS,
                    "TUBERCULOSIS" => $TUBERCULOSIS,
                    "ASMA" => $ASMA,
                    "INSUFICIENCIA_RESPIRATORIA" => $INSUFICIENCIA_RESPIRATORIA,
                    "BRONQUITIS_OBS" => $BRONQUITIS_OBS,
                    "TUBERCULOSIS_OBS" => $TUBERCULOSIS_OBS,
                    "ASMA_OBS" => $ASMA_OBS,
                    "INSUFICIENCIA_RESPIRATORIA_OBS" => $INSUFICIENCIA_RESPIRATORIA_OBS

                ]);
                break;
            case 'Toxicológico':
                $form = new Template("../../view/content/formsHeredoFam/toxicologico.html", $data = [
                    "ALCOHOLISMO" => $ALCOHOLISMO,
                    "TABAQUISMO" => $TABAQUISMO,
                    "DROGAS" => $DROGAS,
                    "ALCOHOLISMO_OBS" => $ALCOHOLISMO_OBS,
                    "TABAQUISMO_OBS" => $TABAQUISMO_OBS,
                    "DROGAS_OBS" => $DROGAS_OBS,
                ]);
                break;
            case 'Nefropatías':
                $form = new Template("../../view/content/formsHeredoFam/nefropatias.html", $data = [
                    "IRC" => $IRC,
                    "LITASIS" => $LITASIS,
                    "IRC_OBS" => $IRC_OBS,
                    "LITASIS_OBS" => $LITASIS_OBS
                ]);
                break;
            case 'Endocrinas':
                $form = new Template("../../view/content/formsHeredoFam/endocrinas.html", $data = [
                    "DM" => $DM,
                    "HIPERTIROIDISMO" => $HIPERTIROIDISMO,
                    "HIPOTIROIDISMO" => $HIPOTIROIDISMO,
                    "DM_OBS" => $DM_OBS,
                    "HIPERTIROIDISMO_OBS" => $HIPERTIROIDISMO_OBS,
                    "HIPOTIROIDISMO_OBS" => $HIPOTIROIDISMO_OBS
                ]);
                break;
            case 'Obesidad':
                $form = new Template("../../view/content/formsHeredoFam/obesidad.html", $data = [
                    "TRANSTORNO_DE_CRECIMIENTO" => $TRANSTORNO_DE_CRECIMIENTO,
                    "TRANSTORNO_DE_CRECIMIENTO_OBS" => $TRANSTORNO_DE_CRECIMIENTO_OBS
                ]);
                break;
            case 'Mentales':
                $form = new Template("../../view/content/formsHeredoFam/mentales.html", $data = [
                    "NEUROSIS" => $NEUROSIS,
                    "PSICOSIS" => $PSICOSIS,
                    "INTENTO_DE_SUICIDIO" => $INTENTO_DE_SUICIDIO,
                    "ESQUIZOFRENIA" => $ESQUIZOFRENIA,
                    "NEUROSIS_OBS" => $NEUROSIS_OBS,
                    "PSICOSIS_OBS" => $PSICOSIS_OBS,
                    "INTENTO_DE_SUICIDIO_OBS" => $INTENTO_DE_SUICIDIO_OBS,
                    "ESQUIZOFRENIA_OBS" => $ESQUIZOFRENIA_OBS
                ]);
                break;
            case 'Generales':
                $form = new Template("../../view/content/formsHeredoFam/generales.html", $data = [
                    "CANCER" => $CANCER,
                    "ALERGIAS" => $ALERGIAS,
                    "MALFORMACIONES" => $MALFORMACIONES,
                    "CANCER_OBS" => $CANCER_OBS,
                    "ALERGIAS_OBS" => $ALERGIAS_OBS,
                    "MALFORMACIONES_OBS" => $MALFORMACIONES_OBS

                ]);
                break;
            case 'Otras':
                $form = new Template("../../view/content/formsHeredoFam/otras.html", $data = [
                    "OTRAS" => $OTRAS,
                    "OTRAS_OBS" => $OTRAS_OBS

                ]);
                break;
            case 'APNP':
                $form = new Template("../../view/content/formsAntPP/antPNP.html", $data = [
                    "HABITACION" => $HABITACION,
                    "NO_DORMITORIOS" => $NO_DORMITORIOS,
                    "NO_HABITANTES" => $NO_HABITANTES,
                    "PISO" => $PISO,
                    "TECHO" => $TECHO,
                    "AGUA_POTABLE" => $AGUA_POTABLE,
                    "LUZ" => $LUZ,
                    "DRENAJE" => $DRENAJE,
                    "CONVIVENCIA_CON_ANIMALES" => $CONVIVENCIA_CON_ANIMALES,
                    "AIRE_ACONDICIONADO" => $AIRE_ACONDICIONADO,
                    "RELIGION" => $RELIGION,
                    "HIGIENE_PERSONAL" => $HIGIENE_PERSONAL,
                    "BAÑO" => $BAÑO,
                    "CAMBIO_DE_ROPA" => $CAMBIO_DE_ROPA,
                    "ASEO_BUCAL" => $ASEO_BUCAL,
                    "ALIMENTACION" => $ALIMENTACION,
                    "NO_COMIDAS_AL_DIA" => $NO_COMIDAS_AL_DIA,
                    "BALANCEADA" => $BALANCEADA,
                    "SUFICIENTE" => $SUFICIENTE,
                    "DEPORTE" => $DEPORTE,
                    "FC1" => $FC1,
                    "HOBBIES_PASATIEMPOS" => $HOBBIES_PASATIEMPOS,
                    "FC2" => $FC2,
                    "cadaCuandoDep" => $cadaCuandoDep,
                    "cadaCuandoPasa" => $cadaCuandoPasa,

                ]);
                break;
            case 'Inmunizaciones':
                $form = new Template("../../view/content/formsAntPP/inmunizaciones.html", $data = [
                    "BLG" => $BLG,
                    "SABIN" => $SABIN,
                    "DPT" => $DPT,
                    "HEPATITIS_B" => $HEPATITIS_B,
                    "SR" => $SR,
                    "INFLUENZA" => $INFLUENZA,
                    "TD" => $TD,
                    "COVID" => $COVID,
                    "FECHA" => $FECHA,
                    "FECHAINFLUEN" => $fechaINFLUEN,

                ]);
                break;

            case 'AGO':
                $form = new Template("../../view/content/formsGinecoObs/AGO.html", $data = [
                    "MENARCA" => $MENARCA,
                    "RITMO" => $RITMO,
                    "IVSA" => $IVSA,
                    "FUM" => $FUM,
                    "G" => $G,
                    "A" => $A,
                    "P" => $P,
                    "C" => $C,
                    "MET_PLANIFICACION" => $MET_PLANIFICACION,
                    "PAPANICOLAOU" => $PAPANICOLAOU,
                    "INFECC_REP" => $INFECC_REP,
                    "FUP" => $FUP,
                ]);
                break;
            case 'APP':
                $form = new Template("../../view/content/formsAPP/APP.html", $data = [
                    "ENF_CONGENITAS" => $ENF_CONGENITAS,
                    "GENITAL" => $GENITAL,
                    "ENF_PROP_INF" => $ENF_PROP_INF,
                    "DERMATOPATIAS" => $DERMATOPATIAS,
                    "SIST_AUDITIVO" => $SIST_AUDITIVO,
                    "RESPIRATORIO" => $RESPIRATORIO,
                    "SIS_OLFATIVO" => $SIS_OLFATIVO,
                    "TRAUMATICOS" => $TRAUMATICOS,
                    "USO_LENTES" => $USO_LENTES,
                    "USO_LENTES_OBS" => $USO_LENTES_OBS,
                    "ASTIGMATISMO" => $ASTIGMATISMO,
                    "ASTIGMATISMO_OBS" => $ASTIGMATISMO_OBS,
                    "PRESBICIA" => $PRESBICIA,
                    "PRESBICIA_OBS" => $PRESBICIA_OBS,
                    "ESTRABISMO" => $ESTRABISMO,
                    "ESTRABISMO_OBS" => $ESTRABISMO_OBS,
                    "DALTONISMO" => $DALTONISMO,
                    "DALTONISMO_OBS" => $DALTONISMO_OBS,
                    "DESPENDIMIENTO_RET" => $DESPENDIMIENTO_RET,
                    "DESPENDIMIENTO_RET_OBS" => $DESPENDIMIENTO_RET_OBS,
                    "MIOPIA" => $MIOPIA,
                    "MIOPE_OBS" => $MIOPE_OBS,
                    "HERNIAS" => $HERNIAS,
                    "HERNIAS_OBS" => $HERNIAS_OBS,
                    "CIRROSIS" => $CIRROSIS,
                    "CIRROSIS_OBS" => $CIRROSIS_OBS,
                    "COLITIS" => $COLITIS,
                    "COLITIS_OBS" => $COLITIS_OBS,
                    "GASTRITIS" => $GASTRITIS,
                    "GASTRITIS_OBS" => $GASTRITIS_OBS,
                    "HEMORROIDES" => $HEMORROIDES,
                    "HEMORROIDES_OBS" => $HEMORROIDES_OBS,
                    "CIRUGIAS" => $CIRUGIAS,
                    "CIRUGIAS_OBS" => $CIRUGIAS_OBS,
                    "INCONTINENSIA" => $INCONTINENSIA,
                    "INCONTINENSIA_OBS" => $INCONTINENSIA_OBS,
                    "INSUFICIENCIA" => $INSUFICIENCIA,
                    "INSUFICIENCIA_OBS" => $INSUFICIENCIA_OBS,
                    "LITIASIS" => $LITIASIS,
                    "LITIASIS_OBS" => $LITIASIS_OBS,
                    "ANEURISMA" => $ANEURISMA,
                    "ANEURISMA_OBS" => $ANEURISMA_OBS,
                    "ENF_DEG_MUS_NEU" => $ENF_DEG_MUS_NEU,
                    "ENF_DEG_MUS_NEU_OBS" => $ENF_DEG_MUS_NEU_OBS,
                    "ACC_CER_VASCU" => $ACC_CER_VASCU,
                    "ACC_CER_VASCU_OBS" => $ACC_CER_VASCU_OBS,
                    "EPILEPSIA2" => $EPILEPSIA2,
                    "EPILEPSIA_OBS2" => $EPILEPSIA_OBS2,
                    "PAR_FACIAL" => $PAR_FACIAL,
                    "PAR_FACIAL_OBS" => $PAR_FACIAL_OBS,
                    "ESCOLIOSIS" => $ESCOLIOSIS,
                    "ESCOLIOSIS_OBS" => $ESCOLIOSIS_OBS,
                    "XIFOSIS" => $XIFOSIS,
                    "XIFOSIS_OBS" => $XIFOSIS_OBS,
                    "LORDOSISI" => $LORDOSISI,
                    "LORDOSISI_OBS" => $LORDOSISI_OBS,
                    "LUMBALGIA" => $LUMBALGIA,
                    "LUMBALGIA_OBS" => $LUMBALGIA_OBS,
                    "BASCULAMIENTO" => $BASCULAMIENTO,
                    "BASCULAMIENTO_OBS" => $BASCULAMIENTO_OBS,
                    "ALTERACION_DISCAL" => $ALTERACION_DISCAL,
                    "ALTERACION_DISCAL_OBS" => $ALTERACION_DISCAL_OBS,
                    "ANEURISMA2" => $ANEURISMA2,
                    "ANEURISMA_OBS2" => $ANEURISMA_OBS2,
                    "HIPERTENSION2" => $HIPERTENSION2,
                    "HIPERTENSION_OBS2" => $HIPERTENSION_OBS2,
                    "HIPOTENSION2" => $HIPOTENSION2,
                    "HIPOTENSION_OBS2" => $HIPOTENSION_OBS2,
                    "INFARTOS" => $INFARTOS,
                    "INFARTOS_OBS" => $INFARTOS_OBS,
                    "INSUFIC_CAR" => $INSUFIC_CAR,
                    "INSUFIC_CAR_OBS" => $INSUFIC_CAR_OBS,
                    "TABAQUISMO2" => $TABAQUISMO2,
                    "TABAQUISMO_OBS2" => $TABAQUISMO_OBS2,
                    "ALCOHOLISMO2" => $ALCOHOLISMO2,
                    "ALCOHOLISMO_OBS2" => $ALCOHOLISMO_OBS2,
                    "DROGAS2" => $DROGAS2,
                    "DROGAS_OBS2" => $DROGAS_OBS2,
                    "ALT_CARGA" => $ALT_CARGA,
                    "ALT_CARGA_OBS" => $ALT_CARGA_OBS,
                    "PERFORACIONES" => $PERFORACIONES,
                    "PERFORACIONES_OBS" => $PERFORACIONES_OBS,
                    "TATUAJES" => $TATUAJES,
                    "TATUAJES_OBS" => $TATUAJES_OBS,
                    "ALERGIAS" => $ALERGIAS,
                    "ALERGIAS_OBS" => $ALERGIAS_OBS,
                    "DIABETES" => $DIABETES,
                    "DIABETES_OBS" => $DIABETES_OBS,
                    "HIPERTIROIDISMO2" => $HIPERTIROIDISMO2,
                    "HIPERTIROIDISMO_OBS2" => $HIPERTIROIDISMO_OBS2,
                    "HIPOTIROIDISMO2" => $HIPOTIROIDISMO2,
                    "HIPOTIROIDISMO_OBS2" => $HIPOTIROIDISMO_OBS2,

                ]);
                break;
            case 'PA':
                $form = new Template("../../view/content/formsPaAc/PA.html", $data = [
                    "OBSERVACIONES" => $OBSERVACIONES
                ]);
                break;
            case 'IPAYS':
                $form = new Template("../../view/content/formsPaAc/InterrogatorioPs.html", $data = [
                    "OBSERVACIONES2" => $OBSERVACIONES2
                ]);
                break;
            case 'EF':
                $form = new Template("../../view/content/formsEFis/EFis.html", $data = [
                    "PESO" => $PESO,
                    "TALLA" => $TALLA,
                    "IMC" => $IMC,
                    "FC" => $FC,
                    "FR" => $FR,
                    "TA" => $TA,
                    "TEMP" => $TEMP,
                    "STPO2" => $STPO2,
                    "ECTOMORFICO" => $ECTOMORFICO,
                    "MESOMORFO" => $MESOMORFO,
                    "ENDOMORFICA" => $ENDOMORFICA,
                    "SATISFACCION" => $SATISFACCION,
                    "COMP_TRABAJO" => $COMP_TRABAJO,
                    "COMP_ORGANIZACION" => $COMP_ORGANIZACION,
                    "TIEMPO" => $TIEMPO,
                    "ESPACIO" => $ESPACIO,
                    "PERSONA" => $PERSONA,
                    "SERENIDAD" => $SERENIDAD,
                    "INTERES" => $INTERES,
                    "ANTAGONICO" => $ANTAGONICO,
                    "ENTUSIASTA" => $ENTUSIASTA,
                    "ENOJO" => $ENOJO,
                    "HIPOCRITA" => $HIPOCRITA,
                    "ICTERICA" => $ICTERICA,
                    "ANEMICA" => $ANEMICA,
                    "TIROIDEA" => $TIROIDEA,
                    "NOLMAL_FASCIES" => $NOLMAL_FASCIES,
                    "PARKINSON2" => $PARKINSON2,
                    "HEMIPLEJICO" => $HEMIPLEJICO,
                    "ATAXICA" => $ATAXICA,
                    "HEMIPARESIA" => $HEMIPARESIA,
                    "NORMAL_MARCHA" => $NORMAL_MARCHA,
                    "LENGUAJE" => $LENGUAJE,
                    "CONVER_FLUIDA" => $CONVER_FLUIDA,
                    "COHERENCIA_PALABLAS" => $COHERENCIA_PALABLAS,
                    "VOZCLARAYFUERTE" => $VOZCLARAYFUERTE,
                    "OJOS" => $OJOS,
                    "PUPILAS" => $PUPILAS,
                    "CONJUNTIVAS" => $CONJUNTIVAS,
                    "REFLEJOS" => $REFLEJOS,
                    "FONDO_OJO" => $FONDO_OJO,
                    "OD" => $OD,
                    "OIZ" => $OIZ,
                    "COLORES" => $COLORES,
                    "CAVIDAD_NASAL" => $CAVIDAD_NASAL,
                    "MUCOSA" => $MUCOSA,
                    "TABIQUE_NASAL" => $TABIQUE_NASAL,
                    "OLFATO" => $OLFATO,
                    "CERVICAL" => $CERVICAL,
                    "DORSAL" => $DORSAL,
                    "LUMBAR" => $LUMBAR,
                    "LESIONES" => $LESIONES,
                    "ENCIAS" => $ENCIAS,
                    "OROIFARINGE" => $OROIFARINGE,
                    "ODEXCAEEXFISINP" => $ODEXCAEEXFISINP,
                    "ODCAEINPEXP" => $ODCAEINPEXP,
                    "ODMEMTIMINPEXP" => $ODMEMTIMINPEXP,
                    "ODAGAUDIINPEXP" => $ODAGAUDIINPEXP,
                    "OICAEINPEXP" => $OICAEINPEXP,
                    "OIMEMTIMINPEXP" => $OIMEMTIMINPEXP,
                    "OIAGAUDIINPEXP" => $OIAGAUDIINPEXP,
                    "CAE" => $CAE,
                    "MEM_TIM" => $MEM_TIM,
                    "AGUD_AUDIT" => $AGUD_AUDIT,
                    "OID" => $OID,
                    "OI" => $OI,
                    "RUIDOS_CARDIACOS" => $RUIDOS_CARDIACOS,
                    "REG_PRECORDIAL" => $REG_PRECORDIAL,
                    "CAMPOS_PULM" => $CAMPOS_PULM,
                    "DEFORMACIONES" => $DEFORMACIONES,
                    "DOLOR" => $DOLOR,
                    "MOVIMIENTOS" => $MOVIMIENTOS,
                    "MARCHA" => $MARCHA,
                    "LASSEGUE" => $LASSEGUE,
                    "PUNTA_TALON" => $PUNTA_TALON,
                    "FORMA" => $FORMA,
                    "VISCEROMEGALIAS" => $VISCEROMEGALIAS,
                    "HERNIAS2" => $HERNIAS2,
                    "DOLOR" => $DOLOR,
                    "PERISTALSIS" => $PERISTALSIS,
                    "MASCULINO" => $MASCULINO,
                    "FEMENINO" => $FEMENINO,
                    "PUNTOS_URETRALES" => $PUNTOS_URETRALES,
                    "FOSAS_RENALES" => $FOSAS_RENALES,
                    "SUPERIOR" => $SUPERIOR,
                    "INFERIOR" => $INFERIOR,
                    "IVP" => $IVP,
                    "EDEMA" => $EDEMA,
                    "LUNARES" => $LUNARES,
                    "PIGMENTACION" => $PIGMENTACION,
                    "CICATRICES_QUIRUR" => $CICATRICES_QUIRUR,
                    "DEFORMACIONES_OBS" => $DEFORMACIONES_OBS,
                    "TIROIDES_OBS" => $TIROIDES_OBS,
                    "TRAQUEA_OBS" => $TRAQUEA_OBS,
                    "OBSGENERALINPEX" => $OBSGENERALINPEX,
                    "DIENTE_11" => $DIENTE_11,
                    "DIENTE_12" => $DIENTE_12,
                    "DIENTE_13" => $DIENTE_13,
                    "DIENTE_14" => $DIENTE_14,
                    "DIENTE_15" => $DIENTE_15,
                    "DIENTE_16" => $DIENTE_16,
                    "DIENTE_17" => $DIENTE_17,
                    "DIENTE_18" => $DIENTE_18,
                    "DIENTE_41" => $DIENTE_41,
                    "DIENTE_42" => $DIENTE_42,
                    "DIENTE_43" => $DIENTE_43,
                    "DIENTE_44" => $DIENTE_44,
                    "DIENTE_45" => $DIENTE_45,
                    "DIENTE_46" => $DIENTE_46,
                    "DIENTE_47" => $DIENTE_47,
                    "DIENTE_48" => $DIENTE_48,
                    "DIENTE_2" => $DIENTE_2,
                    "DIENTE_22" => $DIENTE_22,
                    "DIENTE_23" => $DIENTE_23,
                    "DIENTE_24" => $DIENTE_24,
                    "DIENTE_25" => $DIENTE_25,
                    "DIENTE_26" => $DIENTE_26,
                    "DIENTE_27" => $DIENTE_27,
                    "DIENTE_28" => $DIENTE_28,
                    "DIENTE_31" => $DIENTE_31,
                    "DIENTE_32" => $DIENTE_32,
                    "DIENTE_33" => $DIENTE_33,
                    "DIENTE_34" => $DIENTE_34,
                    "DIENTE_35" => $DIENTE_35,
                    "DIENTE_36" => $DIENTE_36,
                    "DIENTE_37" => $DIENTE_37,
                    "DIENTE_38" => $DIENTE_38,
                    "LENTEOJODERECHOEXP" => $LENTEOJODERECHOEXP,
                    "LENTEOJOIZQUIERDOEXP" => $LENTEOJOIZQUIERDOEXP,
                    "ROMBERGEXFIS" => $ROMBERGEXFIS,
                    "ODEXFISINPCERC" => $ODEXFISINPCERC,
                    "ODEXFISINPCERCIZQ" => $ODEXFISINPCERCIZQ

                ]);
                break;
            case 'ERR':
                $form = new Template("../../view/content/formsPaAc/ERR.html", $data = [
                    "OBSERVACIONES" => $RESULTADOS3
                ]);
                break;
            case 'Diagnóstico':
                $form = new Template("../../view/content/formsPaAc/DIAG.html", $data = [
                    "OBSERVACIONES" => $OBSERVACIONES3
                ]);
                break;
            case 'POTE':
                $form = new Template("../../view/content/formsPaAc/POTE.html", $data = [
                    "OBSERVACIONES" => $OBSERVACIONES32
                ]);
                break;
            case 'RE':
                $form = new Template("../../view/content/formResExam/resEx.html", $data = [
                    "OBSERVACIONES" => $OBSERVACIONESRESEXAM
                ]);
                break;
            case 'EXPLAB':
                $form = new Template("../../view/content/formResExam/expLab.html", $data = [
                    "fechaExp" => $FECHAExam,
                    "edad_inicio_laborar" => $edad_inicio_laborar,
                    "cantidad_trabajos" => $cantidad_trabajos,
                    "pension" => $pension,
                    "nombre1" => $nombre1,
                    "giro1" => $giro1,
                    "puesto1" => $puesto1,
                    "turno1" => $turno1,
                    "antiguedad1" => $antiguedad1,
                    "salida1" => $salida1,
                    "descripcion1" => $descripcion1,
                    "riesgos1" => $riesgos1,
                    "epp1" => $epp1,
                    "observaciones1" => $observaciones1,
                    "nombre2" => $nombre2,
                    "giro2" => $giro2,
                    "puesto2" => $puesto2,
                    "turno2" => $turno2,
                    "antiguedad2" => $antiguedad2,
                    "salida2" => $salida2,
                    "descripcion2" => $descripcion2,
                    "riesgos2" => $riesgos2,
                    "epp2" => $epp2,
                    "observaciones2" => $observaciones2,
                    "nombre3" => $nombre3,
                    "giro3" => $giro3,
                    "puesto3" => $puesto3,
                    "turno3" => $turno3,
                    "antiguedad3" => $antiguedad3,
                    "salida3" => $salida3,
                    "descripcion3" => $descripcion3,
                    "riesgos3" => $riesgos3,
                    "epp3" => $epp3,
                    "observaciones3" => $observaciones3,
                    "nombre4" => $nombre4,
                    "giro4" => $giro4,
                    "puesto4" => $puesto4,
                    "turno4" => $turno4,
                    "antiguedad4" => $antiguedad4,
                    "salida4" => $salida4,
                    "descripcion4" => $descripcion4,
                    "riesgos4" => $riesgos4,
                    "epp4" => $epp4,
                    "observaciones4" => $observaciones4,
                    "contactoEmernombre1" => $contactoEmernombre1,
                    "contactoEmerapellido_paterno1" => $contactoEmerapellido_paterno1,
                    "contactoEmerapellido_materno1" => $contactoEmerapellido_materno1,
                    "contactoEmerparentesco1" => $contactoEmerparentesco1,
                    "contactoEmertelefono1" => $contactoEmertelefono1,
                    "contactoEmernombre2" => $contactoEmernombre2,
                    "contactoEmerapellido_paterno2" => $contactoEmerapellido_paterno2,
                    "contactoEmerapellido_materno2" => $contactoEmerapellido_materno2,
                    "contactoEmerparentesco2" => $contactoEmerparentesco2,
                    "contactoEmertelefono2" => $contactoEmertelefono2,
                    "contactoEmernombre3" => $contactoEmernombre3,
                    "contactoEmerapellido_paterno3" => $contactoEmerapellido_paterno3,
                    "contactoEmerapellido_materno3" => $contactoEmerapellido_materno3,
                    "contactoEmerparentesco3" => $contactoEmerparentesco3,
                    "contactoEmertelefono3" => $contactoEmertelefono3,
                ]);
                break;
            default:
                # code...
                break;
        }
        $template = new Template("../../view/content/heredoFamViewCard.html", $data = [
            "tittle" => $type,
            "form" => $form,
        ]);
        echo $template;
    }
}
