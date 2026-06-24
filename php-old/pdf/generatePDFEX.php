<?php

require_once('../app/app.php');
$model = new app;
$others = new another();
$idArch = (isset($_GET['id']) ? $_GET['id'] : null);
$base64F =  $model->extractFileJson($idArch);
$jason = base64_decode($base64F);
$decoded = json_decode($jason);
foreach ($decoded as $key) {
    $nss = $key->{'nss'};
    $CARDIOPATIA_ISQUEMICA = $key->{'CARDIOPATIA_ISQUEMICA'};
    $HIPERTENSION = $key->{'HIPERTENSION'};
    $HIPOTENSION = $key->{'HIPOTENSION'};
    $INSUFICIENCIA_CARDIACA = $key->{'INSUFICIENCIA_CARDIACA'};
    $INSUFICIENCIA_VASCULAR_PERIFERICA = $key->{'INSUFICIENCIA_VASCULAR_PERIFERICA'};
    $CARDIOPATIA_ISQUEMICA_OBS = $key->{'CARDIOPATIA_ISQUEMICA_OBS'};
    $HIPERTENSION_OBS = $key->{'HIPERTENSION_OBS'};
    $HIPOTENSION_OBS = $key->{'HIPOTENSION_OBS'};
    $INSUFICIENCIA_CARDIACA_OBS = $key->{'INSUFICIENCIA_CARDIACA_OBS'};
    $INSUFICIENCIA_VASCULAR_PERIFERICA_OBS = $key->{'INSUFICIENCIA_VASCULAR_PERIFERICA_OBS'};
    $ID = $key->{'ID'};
    $DM = $key->{'DM'};
    $HIPERTIROIDISMO = $key->{'HIPERTIROIDISMO'};
    $HIPOTIROIDISMO = $key->{'HIPOTIROIDISMO'};
    $DM_OBS = $key->{'DM_OBS'};
    $HIPERTIROIDISMO_OBS = $key->{'HIPERTIROIDISMO_OBS'};
    $HIPOTIROIDISMO_OBS = $key->{'HIPOTIROIDISMO_OBS'};
    $CANCER = $key->{'CANCER'};
    $ALERGIAS = $key->{'ALERGIAS'};
    $MALFORMACIONES = $key->{'MALFORMACIONES'};
    $CANCER_OBS = $key->{'CANCER_OBS'};
    $ALERGIAS_OBS = $key->{'ALERGIAS_OBS'};
    $MALFORMACIONES_OBS = $key->{'MALFORMACIONES_OBS'};
    $NEUROSIS = $key->{'NEUROSIS'};
    $PSICOSIS = $key->{'PSICOSIS'};
    $INTENTO_DE_SUICIDIO = $key->{'INTENTO_DE_SUICIDIO'};
    $ESQUIZOFRENIA = $key->{'ESQUIZOFRENIA'};
    $NEUROSIS_OBS = $key->{'NEUROSIS_OBS'};
    $PSICOSIS_OBS = $key->{'PSICOSIS_OBS'};
    $INTENTO_DE_SUICIDIO_OBS = $key->{'INTENTO_DE_SUICIDIO_OBS'};
    $ESQUIZOFRENIA_OBS = $key->{'ESQUIZOFRENIA_OBS'};
    $IRC = $key->{'IRC'};
    $LITASIS = $key->{'LITASIS'};
    $IRC_OBS = $key->{'IRC_OBS'};
    $LITASIS_OBS = $key->{'LITASIS_OBS'};
    $BRONQUITIS = $key->{'BRONQUITIS'};
    $TUBERCULOSIS = $key->{'TUBERCULOSIS'};
    $ASMA = $key->{'ASMA'};
    $INSUFICIENCIA_RESPIRATORIA = $key->{'INSUFICIENCIA_RESPIRATORIA'};
    $BRONQUITIS_OBS = $key->{'BRONQUITIS_OBS'};
    $TUBERCULOSIS_OBS = $key->{'TUBERCULOSIS_OBS'};
    $ASMA_OBS = $key->{'ASMA_OBS'};
    $INSUFICIENCIA_RESPIRATORIA_OBS = $key->{'INSUFICIENCIA_RESPIRATORIA_OBS'};
    $AVC = $key->{'AVC'};
    $EPILEPSIA = $key->{'EPILEPSIA'};
    $PARALISIS_FACIAL = $key->{'PARALISIS_FACIAL'};
    $PARKINSON = $key->{'PARKINSON'};
    $ALZHEIMER = $key->{'ALZHEIMER'};
    $MIGRANA = $key->{'MIGRANA'};
    $EPILEPSIA_OBS = $key->{'EPILEPSIA_OBS'};
    $PARALISIS_FACIAL_OBS = $key->{'PARALISIS_FACIAL_OBS'};
    $PARKINSON_OBS = $key->{'PARKINSON_OBS'};
    $ALZHEIMER_OBS = $key->{'ALZHEIMER_OBS'};
    $MIGRANA_OBS = $key->{'MIGRANA_OBS'};
    $TRANSTORNO_DE_CRECIMIENTO = $key->{'TRANSTORNO_DE_CRECIMIENTO'};
    $TRANSTORNO_DE_CRECIMIENTO_OBS = $key->{'TRANSTORNO_DE_CRECIMIENTO_OBS'};
    $OTRAS = $key->{'OTRAS'};
    $OTRAS_OBS = $key->{'OTRAS_OBS'};
    $ALCOHOLISMO = $key->{'ALCOHOLISMO'};
    $TABAQUISMO = $key->{'TABAQUISMO'};
    $DROGAS = $key->{'DROGAS'};
    $ALCOHOLISMO_OBS = $key->{'ALCOHOLISMO_OBS'};
    $TABAQUISMO_OBS = $key->{'TABAQUISMO_OBS'};
    $DROGAS_OBS = $key->{'DROGAS_OBS'};
    $HABITACION = $key->{'HABITACION'};
    $NO_DORMITORIOS = $key->{'NO_DORMITORIOS'};
    $NO_HABITANTES = $key->{'NO_HABITANTES'};
    $PISO = $key->{'PISO'};
    $TECHO = $key->{'TECHO'};
    $AGUA_POTABLE = $key->{'AGUA_POTABLE'};
    $LUZ = $key->{'LUZ'};
    $DRENAJE = $key->{'DRENAJE'};
    $CONVIVENCIA_CON_ANIMALES = $key->{'CONVIVENCIA_CON_ANIMALES'};
    $AIRE_ACONDICIONADO = $key->{'AIRE_ACONDICIONADO'};
    $RELIGION = $key->{'RELIGION'};
    $HIGIENE_PERSONAL = $key->{'HIGIENE_PERSONAL'};
    $BAÑO = $key->{'BANO'};
    $CAMBIO_DE_ROPA = $key->{'CAMBIO_DE_ROPA'};
    $ASEO_BUCAL = $key->{'ASEO_BUCAL'};
    $ALIMENTACION = $key->{'ALIMENTACION'};
    $NO_COMIDAS_AL_DIA = $key->{'NO_COMIDAS_AL_DIA'};
    $BALANCEADA = $key->{'BALANCEADA'};
    $SUFICIENTE = $key->{'SUFICIENTE'};
    $DEPORTE = $key->{'DEPORTE'};
    $FC1 = $key->{'FC1'};
    $HOBBIES_PASATIEMPOS = $key->{'HOBBIES_PASATIEMPOS'};
    $FC2 = $key->{'FC2'};
    $BLG = $key->{'BLG'};
    $SABIN = $key->{'SABIN'};
    $DPT = $key->{'DPT'};
    $HEPATITIS_B = $key->{'HEPATITIS_B'};
    $SR = $key->{'SR'};
    $INFLUENZA = $key->{'INFLUENZA'};
    $TD = $key->{'TD'};
    $view = $key->{'view'};
    $COVID = $key->{'COVID'};
    $FECHA = $key->{'FECHA'};
    $MENARCA = $key->{'MENARCA'};
    $RITMO = $key->{'RITMO'};
    $IVSA = $key->{'IVSA'};
    $FUM = $key->{'FUM'};
    $G = $key->{'G'};
    $A = $key->{'A'};
    $P = $key->{'P'};
    $C = $key->{'C'};
    $MET_PLANIFICACION = $key->{'MET_PLANIFICACION'};
    $PAPANICOLAOU = $key->{'PAPANICOLAOU'};
    $INFECC_REP = $key->{'INFECC_REP'};
    $FUP = $key->{'FUP'};
    $ENF_CONGENITAS = $key->{'ENF_CONGENITAS'};
    $GENITAL = $key->{'GENITAL'};
    $ENF_PROP_INF = $key->{'ENF_PROP_INF'};
    $DERMATOPATIAS = $key->{'DERMATOPATIAS'};
    $SIST_AUDITIVO = $key->{'SIST_AUDITIVO'};
    $RESPIRATORIO = $key->{'RESPIRATORIO'};
    $SIS_OLFATIVO = $key->{'SIS_OLFATIVO'};
    $TRAUMATICOS = $key->{'TRAUMATICOS'};
    $USO_LENTES = $key->{'USO_LENTES'};
    $USO_LENTES_OBS = $key->{'USO_LENTES_OBS'};
    $ASTIGMATISMO = $key->{'ASTIGMATISMO'};
    $ASTIGMATISMO_OBS = $key->{'ASTIGMATISMO_OBS'};
    $PRESBICIA = $key->{'PRESBICIA'};
    $PRESBICIA_OBS = $key->{'PRESBICIA_OBS'};
    $ESTRABISMO = $key->{'ESTRABISMO'};
    $ESTRABISMO_OBS = $key->{'ESTRABISMO_OBS'};
    $DALTONISMO = $key->{'DALTONISMO'};
    $DALTONISMO_OBS = $key->{'DALTONISMO_OBS'};
    $DESPENDIMIENTO_RET = $key->{'DESPENDIMIENTO_RET'};
    $DESPENDIMIENTO_RET_OBS = $key->{'DESPENDIMIENTO_RET_OBS'};
    $MIOPIA = $key->{'MIOPIA'};
    $MIOPIA_OBS = $key->{'MIOPIA_OBS'};
    $HERNIAS = $key->{'HERNIAS'};
    $HERNIAS_OBS = $key->{'HERNIAS_OBS'};
    $CIRROSIS = $key->{'CIRROSIS'};
    $CIRROSIS_OBS = $key->{'CIRROSIS_OBS'};
    $COLITIS = $key->{'COLITIS'};
    $COLITIS_OBS = $key->{'COLITIS_OBS'};
    $GASTRITIS = $key->{'GASTRITIS'};
    $GASTRITIS_OBS = $key->{'GASTRITIS_OBS'};
    $HEMORROIDES = $key->{'HEMORROIDES'};
    $HEMORROIDES_OBS = $key->{'HEMORROIDES_OBS'};
    $CIRUGIAS = $key->{'CIRUGIAS'};
    $CIRUGIAS_OBS = $key->{'CIRUGIAS_OBS'};
    $INCONTINENSIA = $key->{'INCONTINENSIA'};
    $INCONTINENSIA_OBS = $key->{'INCONTINENSIA_OBS'};
    $INSUFICIENCIA = $key->{'INSUFICIENCIA'};
    $INSUFICIENCIA_OBS = $key->{'INSUFICIENCIA_OBS'};
    $LITIASIS = $key->{'LITIASIS'};
    $LITIASIS_OBS = $key->{'LITIASIS_OBS'};
    $ANEURISMA = $key->{'ANEURISMA'};
    $ANEURISMA_OBS = $key->{'ANEURISMA_OBS'};
    $ENF_DEG_MUS_NEU = $key->{'ENF_DEG_MUS_NEU'};
    $ENF_DEG_MUS_NEU_OBS = $key->{'ENF_DEG_MUS_NEU_OBS'};
    $ACC_CER_VASCU = $key->{'ACC_CER_VASCU'};
    $ACC_CER_VASCU_OBS = $key->{'ACC_CER_VASCU_OBS'};
    $EPILEPSIA = $key->{'EPILEPSIA'};
    $EPILEPSIA_OBS = $key->{'EPILEPSIA_OBS'};
    $PAR_FACIAL = $key->{'PAR_FACIAL'};
    $PAR_FACIAL_OBS = $key->{'PAR_FACIAL_OBS'};
    $ESCOLIOSIS = $key->{'ESCOLIOSIS'};
    $ESCOLIOSIS_OBS = $key->{'ESCOLIOSIS_OBS'};
    $XIFOSIS = $key->{'XIFOSIS'};
    $XIFOSIS_OBS = $key->{'XIFOSIS_OBS'};
    $LORDOSISI = $key->{'LORDOSISI'};
    $LORDOSISI_OBS = $key->{'LORDOSISI_OBS'};
    $LUMBALGIA = $key->{'LUMBALGIA'};
    $LUMBALGIA_OBS = $key->{'LUMBALGIA_OBS'};
    $BASCULAMIENTO = $key->{'BASCULAMIENTO'};
    $BASCULAMIENTO_OBS = $key->{'BASCULAMIENTO_OBS'};
    $ALTERACION_DISCAL = $key->{'ALTERACION_DISCAL'};
    $ALTERACION_DISCAL_OBS = $key->{'ALTERACION_DISCAL_OBS'};
    $ANEURISMA = $key->{'ANEURISMA'};
    $ANEURISMA_OBS = $key->{'ANEURISMA_OBS'};
    $HIPERTENSION = $key->{'HIPERTENSION'};
    $HIPERTENSION_OBS = $key->{'HIPERTENSION_OBS'};
    $HIPOTENSION = $key->{'HIPOTENSION'};
    $HIPOTENSION_OBS = $key->{'HIPOTENSION_OBS'};
    $INFARTOS = $key->{'INFARTOS'};
    $INFARTOS_OBS = $key->{'INFARTOS_OBS'};
    $INSUFIC_CAR = $key->{'INSUFIC_CAR'};
    $INSUFIC_CAR_OBS = $key->{'INSUFIC_CAR_OBS'};
    $TABAQUISMO = $key->{'TABAQUISMO'};
    $TABAQUISMO_OBS = $key->{'TABAQUISMO_OBS'};
    $ALCOHOLISMO = $key->{'ALCOHOLISMO'};
    $ALCOHOLISMO_OBS = $key->{'ALCOHOLISMO_OBS'};
    $DROGAS = $key->{'DROGAS'};
    $DROGAS_OBS = $key->{'DROGAS_OBS'};
    $ALT_CARGA = $key->{'ALT_CARGA'};
    $ALT_CARGA_OBS = $key->{'ALT_CARGA_OBS'};
    $PERFORACIONES = $key->{'PERFORACIONES'};
    $PERFORACIONES_OBS = $key->{'PERFORACIONES_OBS'};
    $TATUAJES = $key->{'TATUAJES'};
    $TATUAJES_OBS = $key->{'TATUAJES_OBS'};
    $ALERGIAS = $key->{'ALERGIAS'};
    $ALERGIAS_OBS = $key->{'ALERGIAS_OBS'};
    $DIABETES = $key->{'DIABETES'};
    $DIABETES_OBS = $key->{'DIABETES_OBS'};
    $HIPERTIROIDISMO = $key->{'HIPERTIROIDISMO'};
    $HIPERTIROIDISMO_OBS = $key->{'HIPERTIROIDISMO_OBS'};
    $HIPOTIROIDISMO = $key->{'HIPOTIROIDISMO'};
    $HIPOTIROIDISMO_OBS = $key->{'HIPOTIROIDISMO_OBS'};
    $OBSERVACIONES = $key->{'OBSERVACIONES'};
    $OBSERVACIONES = $key->{'OBSERVACIONES'};
    $PESO = $key->{'PESO'};
    $TALLA = $key->{'TALLA'};
    $IMC = $key->{'IMC'};
    $FC = $key->{'FC'};
    $FR = $key->{'FR'};
    $TA = $key->{'TA'};
    $TEMP = $key->{'TEMP'};
    $ECTOMORFICO = $key->{'ECTOMORFICO'};
    $MESOMORFO = $key->{'MESOMORFO'};
    $ENDOMORFICA = $key->{'ENDOMORFICA'};
    $SATISFACCION = $key->{'SATISFACCION'};
    $COMP_TRABAJO = $key->{'COMP_TRABAJO'};
    $COMP_ORGANIZACION = $key->{'COMP_ORGANIZACION'};
    $TIEMPO = $key->{'TIEMPO'};
    $ESPACIO = $key->{'ESPACIO'};
    $PERSONA = $key->{'PERSONA'};
    $SERENIDAD = $key->{'SERENIDAD'};
    $INTERES = $key->{'INTERES'};
    $ANTAGONICO = $key->{'ANTAGONICO'};
    $ENTUSIASTA = $key->{'ENTUSIASTA'};
    $ENOJO = $key->{'ENOJO'};
    $HIPOCRITA = $key->{'HIPOCRITA'};
    $ICTERICA = $key->{'ICTERICA'};
    $ANEMICA = $key->{'ANEMICA'};
    $TIROIDEA = $key->{'TIROIDEA'};
    $NOLMAL_FASCIES = $key->{'NOLMAL_FASCIES'};
    $PARKINSON = $key->{'PARKINSON'};
    $HEMIPLEJICO = $key->{'HEMIPLEJICO'};
    $ATAXICA = $key->{'ATAXICA'};
    $HEMIPARESIA = $key->{'HEMIPARESIA'};
    $NORMAL_MARCHA = $key->{'NORMAL_MARCHA'};
    $LENGUAJE = $key->{'LENGUAJE'};
    $CONVER_FLUIDA = $key->{'CONVER_FLUIDA'};
    $COHERENCIA_PALABLAS = $key->{'COHERENCIA_PALABLAS'};
    $OJOS = $key->{'OJOS'};
    $PUPILAS = $key->{'PUPILAS'};
    $CONJUNTIVAS = $key->{'CONJUNTIVAS'};
    $REFLEJOS = $key->{'REFLEJOS'};
    $FONDO_OJO = $key->{'FONDO_OJO'};
    $OD = $key->{'OD'};
    $OIZ = $key->{'OIZ'};
    $COLORES = $key->{'COLORES'};
    $CAVIDAD_NASAL = $key->{'CAVIDAD_NASAL'};
    $MUCOSA = $key->{'MUCOSA'};
    $TABIQUE_NASAL = $key->{'TABIQUE_NASAL'};
    $OLFATO = $key->{'OLFATO'};
    $CERVICAL = $key->{'CERVICAL'};
    $DORSAL = $key->{'DORSAL'};
    $LUMBAR = $key->{'LUMBAR'};
    $LESIONES = $key->{'LESIONES'};
    $ENCIAS = $key->{'ENCIAS'};
    $OROIFARINGE = $key->{'OROIFARINGE'};
    $CAE = $key->{'CAE'};
    $MEM_TIM = $key->{'MEM_TIM'};
    $AGUD_AUDIT = $key->{'AGUD_AUDIT'};
    $OD = $key->{'OD'};
    $OI = $key->{'OI'};
    $RUIDOS_CARDIACOS = $key->{'RUIDOS_CARDIACOS'};
    $REG_PRECORDIAL = $key->{'REG_PRECORDIAL'};
    $CAMPOS_PULM = $key->{'CAMPOS_PULM'};
    $DEFORMACIONES = $key->{'DEFORMACIONES'};
    $DOLOR = $key->{'DOLOR'};
    $MOVIMIENTOS = $key->{'MOVIMIENTOS'};
    $MARCHA = $key->{'MARCHA'};
    $LASSEGUE = $key->{'LASSEGUE'};
    $PUNTA_TALON = $key->{'PUNTA_TALON'};
    $DEFORMACIONES_OBS = $key->{'DEFORMACIONES_OBS'};
    $TIROIDES_OBS = $key->{'TIROIDES_OBS'};
    $TRAQUEA_OBS = $key->{'TRAQUEA_OBS'};
    $FORMA = $key->{'FORMA'};
    $VISCEROMEGALIAS = $key->{'VISCEROMEGALIAS'};
    $HERNIAS = $key->{'HERNIAS'};
    $DOLOR = $key->{'DOLOR'};
    $PERISTALSIS = $key->{'PERISTALSIS'};
    $MASCULINO = $key->{'MASCULINO'};
    $FEMENINO = $key->{'FEMENINO'};
    $PUNTOS_URETRALES = $key->{'PUNTOS_URETRALES'};
    $FOSAS_RENALES = $key->{'FOSAS_RENALES'};
    $SUPERIOR = $key->{'SUPERIOR'};
    $INFERIOR = $key->{'INFERIOR'};
    $IVP = $key->{'IVP'};
    $EDEMA = $key->{'EDEMA'};
    $LUNARES = $key->{'LUNARES'};
    $PIGMENTACION = $key->{'PIGMENTACION'};
    $CICATRICES_QUIRUR = $key->{'CICATRICES_QUIRUR'};
    $DIENTE_11 = $key->{'DIENTE_11'};
    $DIENTE_12 = $key->{'DIENTE_12'};
    $DIENTE_13 = $key->{'DIENTE_13'};
    $DIENTE_14 = $key->{'DIENTE_14'};
    $DIENTE_15 = $key->{'DIENTE_15'};
    $DIENTE_16 = $key->{'DIENTE_16'};
    $DIENTE_17 = $key->{'DIENTE_17'};
    $DIENTE_18 = $key->{'DIENTE_18'};
    $DIENTE_41 = $key->{'DIENTE_41'};
    $DIENTE_42 = $key->{'DIENTE_42'};
    $DIENTE_43 = $key->{'DIENTE_43'};
    $DIENTE_44 = $key->{'DIENTE_44'};
    $DIENTE_45 = $key->{'DIENTE_45'};
    $DIENTE_46 = $key->{'DIENTE_46'};
    $DIENTE_47 = $key->{'DIENTE_47'};
    $DIENTE_48 = $key->{'DIENTE_48'};
    $DIENTE_2 = $key->{'DIENTE_2'};
    $DIENTE_22 = $key->{'DIENTE_22'};
    $DIENTE_23 = $key->{'DIENTE_23'};
    $DIENTE_24 = $key->{'DIENTE_24'};
    $DIENTE_25 = $key->{'DIENTE_25'};
    $DIENTE_26 = $key->{'DIENTE_26'};
    $DIENTE_27 = $key->{'DIENTE_27'};
    $DIENTE_28 = $key->{'DIENTE_28'};
    $DIENTE_31 = $key->{'DIENTE_31'};
    $DIENTE_32 = $key->{'DIENTE_32'};
    $DIENTE_33 = $key->{'DIENTE_33'};
    $DIENTE_34 = $key->{'DIENTE_34'};
    $DIENTE_35 = $key->{'DIENTE_35'};
    $DIENTE_36 = $key->{'DIENTE_36'};
    $DIENTE_37 = $key->{'DIENTE_37'};
    $DIENTE_38 = $key->{'DIENTE_38'};
    $RESULTADOS = $key->{'RESULTADOS'};
    $OBSERVACIONES = $key->{'OBSERVACIONES'};
    $OBSERVACIONES = $key->{'OBSERVACIONES'};
    $OBSERVACIONES = $key->{'OBSERVACIONES'};
    $FIRMA_DIGITAL = $key->{'FIRMA_DIGITAL'};
    $DRENAJE = $key->{'Drenajein'};
    $CONVIVENCIA_CON_ANIMALES = $key->{'CCAIn'};
    $VOZCLARAYFUERTE = $key->{'VOZCLARAYFUERTEINP'};
    $STPO2 = $key->{'STPO2INP'};
    $edad_inicio_laborar = $key->{'edad_inicio_laborar'};
    $cantidad_trabajos = $key->{'cantidad_trabajos'};
    $pension = $key->{'pension'};
    $nombre1 = $key->{'nombre1'};
    $giro1 = $key->{'giro1'};
    $puesto1 = $key->{'puesto1'};
    $turno1 = $key->{'turno1'};
    $antiguedad1 = $key->{'antiguedad1'};
    $salida1 = $key->{'salida1'};
    $descripcion1 = $key->{'descripcion1'};
    $riesgos1 = $key->{'riesgos1'};
    $epp1 = $key->{'epp1'};
    $observaciones1 = $key->{'observaciones1'};
    $nombre2 = $key->{'nombre2'};
    $giro2 = $key->{'giro2'};
    $puesto2 = $key->{'puesto2'};
    $turno2 = $key->{'turno2'};
    $antiguedad2 = $key->{'antiguedad2'};
    $salida2 = $key->{'salida2'};
    $descripcion2 = $key->{'descripcion2'};
    $riesgos2 = $key->{'riesgos2'};
    $epp2 = $key->{'epp2'};
    $observaciones2 = $key->{'observaciones2'};
    $nombre3 = $key->{'nombre3'};
    $giro3 = $key->{'giro3'};
    $puesto3 = $key->{'puesto3'};
    $turno3 = $key->{'turno3'};
    $antiguedad3 = $key->{'antiguedad3'};
    $salida3 = $key->{'salida3'};
    $descripcion3 = $key->{'descripcion3'};
    $riesgos3 = $key->{'riesgos3'};
    $epp3 = $key->{'epp3'};
    $observaciones3 = $key->{'observaciones3'};
    $nombre4 = $key->{'nombre4'};
    $giro4 = $key->{'giro4'};
    $puesto4 = $key->{'puesto4'};
    $turno4 = $key->{'turno4'};
    $antiguedad4 = $key->{'antiguedad4'};
    $salida4 = $key->{'salida4'};
    $descripcion4 = $key->{'descripcion4'};
    $riesgos4 = $key->{'riesgos4'};
    $epp4 = $key->{'epp4'};
    $observaciones4 = $key->{'observaciones4'};
    $OBSGENERALINPEX = $key->{'OBSGENERALINPEX'};
    $ODEXCAEEXFISINP = $key->{'ODEXCAEEXFISINP'};
    $ODCAEINPEXP = $key->{'ODCAEINPEXP'};
    $ODMEMTIMINPEXP = $key->{'ODMEMTIMINPEXP'};
    $ODAGAUDIINPEXP = $key->{'ODAGAUDIINPEXP'};
    $OICAEINPEXP = $key->{'OICAEINPEXP'};
    $OIMEMTIMINPEXP = $key->{'OIMEMTIMINPEXP'};
    $OIAGAUDIINPEXP = $key->{'OIAGAUDIINPEXP'};
    $TRAQUEA_OBS = $key->{'TRAQUEA_OBSINP'};
    $DIENTE_2 = $key->{'DIENTE_2'};
    $CIRUGIAS_OBS = $key->{'ciruOBS'};
}
$EPILEPSIA = ($EPILEPSIA == 0) ? "No" : "Sí";
$PARALISIS_FACIAL = ($PARALISIS_FACIAL == 0) ? "No" : "Sí";
$PARKINSON = ($PARKINSON == 0) ? "No" : "Sí";
$ALZHEIMER = ($ALZHEIMER == 0) ? "No" : "Sí";
$MIGRANA = ($MIGRANA == 0) ? "No" : "Sí";


$CARDIOPATIA_ISQUEMICA = ($CARDIOPATIA_ISQUEMICA == 0) ? "No" : "Sí";
$HIPERTENSION = ($HIPERTENSION == 0) ? "No" : "Sí";
$HIPOTENSION = ($HIPOTENSION == 0) ? "No" : "Sí";
$INSUFICIENCIA_CARDIACA = ($INSUFICIENCIA_CARDIACA == 0) ? "No" : "Sí";
$INSUFICIENCIA_VASCULAR_PERIFERICA = ($INSUFICIENCIA_VASCULAR_PERIFERICA == 0) ? "No" : "Sí";

$BRONQUITIS = ($BRONQUITIS == 0) ? "No" : "Sí";
$TUBERCULOSIS = ($TUBERCULOSIS == 0) ? "No" : "Sí";
$ASMA = ($ASMA == 0) ? "No" : "Sí";
$INSUFICIENCIA_RESPIRATORIA = ($INSUFICIENCIA_RESPIRATORIA == 0) ? "No" : "Sí";

$ALCOHOLISMO = ($ALCOHOLISMO == 0) ? "No" : "Sí";
$TABAQUISMO = ($TABAQUISMO == 0) ? "No" : "Sí";
$DROGAS = ($DROGAS == 0) ? "No" : "Sí";

$IRC = ($IRC == 0) ? "No" : "Sí";
$LITASIS = ($LITASIS == 0) ? "No" : "Sí";
$DM = ($DM == 0) ? "No" : "Sí";
$HIPERTIROIDISMO = ($HIPERTIROIDISMO == 0) ? "No" : "Sí";
$HIPOTIROIDISMO = ($HIPOTIROIDISMO == 0) ? "No" : "Sí";
$TRANSTORNO_DE_CRECIMIENTO = ($TRANSTORNO_DE_CRECIMIENTO == 0) ? "No" : "Sí";
$NEUROSIS = ($NEUROSIS == 0) ? "No" : "Sí";
$PSICOSIS = ($PSICOSIS == 0) ? "No" : "Sí";
$INTENTO_DE_SUICIDIO = ($INTENTO_DE_SUICIDIO == 0) ? "No" : "Sí";
$ESQUIZOFRENIA = ($ESQUIZOFRENIA == 0) ? "No" : "Sí";
$CANCER = ($CANCER == 0) ? "No" : "Sí";
$ALERGIAS = ($ALERGIAS == 0) ? "No" : "Sí";
$MALFORMACIONES = ($MALFORMACIONES == 0) ? "No" : "Sí";
$OTRAS = ($OTRAS == 0) ? "No" : "Sí";
$AGUA_POTABLE = ($AGUA_POTABLE == 0) ? "No" : "Sí";
$LUZ = ($LUZ == 0) ? "No" : "Sí";
$AIRE_ACONDICIONADO = ($AIRE_ACONDICIONADO == 0) ? "No" : "Sí";
$ALIMENTACION = ($ALIMENTACION == 0) ? "No" : "Sí";
$SUFICIENTE = ($SUFICIENTE == 0) ? "No" : "Sí";
$BALANCEADA = ($BALANCEADA == 0) ? "No" : "Sí";
$BLG = ($BLG == 0) ? "No" : "Sí";
$SABIN = ($SABIN == 0) ? "No" : "Sí"; // Ten en cuenta el espacio en 'SABIN '
$DPT = ($DPT == 0) ? "No" : "Sí";
$HEPATITIS_B = ($HEPATITIS_B == 0) ? "No" : "Sí";
$SR = ($SR == 0) ? "No" : "Sí";
$INFLUENZA = ($INFLUENZA == 0) ? "No" : "Sí";
$TD = ($TD == 0) ? "No" : "Sí";
$USO_LENTES = ($USO_LENTES == 0) ? "No" : "Sí";
$ASTIGMATISMO = ($ASTIGMATISMO == 0) ? "No" : "Sí";
$PRESBICIA = ($PRESBICIA == 0) ? "No" : "Sí";
$ESTRABISMO = ($ESTRABISMO == 0) ? "No" : "Sí";
$DALTONISMO = ($DALTONISMO == 0) ? "No" : "Sí";
$DESPENDIMIENTO_RET = ($DESPENDIMIENTO_RET == 0) ? "No" : "Sí";
$MIOPIA = ($MIOPIA == 0) ? "No" : "Sí";

$HERNIAS = ($HERNIAS == 0) ? "No" : "Sí";
$CIRROSIS = ($CIRROSIS == 0) ? "No" : "Sí";
$COLITIS = ($COLITIS == 0) ? "No" : "Sí";
$GASTRITIS = ($GASTRITIS == 0) ? "No" : "Sí";
$HEMORROIDES = ($HEMORROIDES == 0) ? "No" : "Sí";
$CIRUGIAS = ($CIRUGIAS == 0) ? "No" : "Sí";

$INCONTINENSIA = ($INCONTINENSIA == 0) ? "No" : "Sí";
$INSUFICIENCIA = ($INSUFICIENCIA == 0) ? "No" : "Sí";
$LITIASIS = ($LITIASIS == 0) ? "No" : "Sí";
$ANEURISMA = ($ANEURISMA == 0) ? "No" : "Sí";
$ENF_DEG_MUS_NEU = ($ENF_DEG_MUS_NEU == 0) ? "No" : "Sí";
$ACC_CER_VASCU = ($ACC_CER_VASCU == 0) ? "No" : "Sí";
$EPILEPSIA2 = ($EPILEPSIA == 0) ? "No" : "Sí";
$PAR_FACIAL = ($PAR_FACIAL == 0) ? "No" : "Sí";
$ESCOLIOSIS = ($ESCOLIOSIS == 0) ? "No" : "Sí";
$XIFOSIS = ($XIFOSIS == 0) ? "No" : "Sí";
$LORDOSISI = ($LORDOSISI == 0) ? "No" : "Sí";
$LUMBALGIA = ($LUMBALGIA == 0) ? "No" : "Sí"; // Fijate que hay un espacio en 'LUMBALGIA '
$BASCULAMIENTO = ($BASCULAMIENTO == 0) ? "No" : "Sí";
$ALTERACION_DISCAL = ($ALTERACION_DISCAL == 0) ? "No" : "Sí";
$ANEURISMA2 = ($ANEURISMA == 0) ? "No" : "Sí";
$HIPERTENSION2 = ($HIPERTENSION == 0) ? "No" : "Sí";
$HIPOTENSION2 = ($HIPOTENSION == 0) ? "No" : "Sí";
$INFARTOS = ($INFARTOS == 0) ? "No" : "Sí";
$INSUFIC_CAR = ($INSUFIC_CAR == 0) ? "No" : "Sí";

$TABAQUISMO2 = ($TABAQUISMO == 0) ? "No" : "Sí";
$ALCOHOLISMO2 = ($ALCOHOLISMO == 0) ? "No" : "Sí";
$DROGAS2 = ($DROGAS == 0) ? "No" : "Sí";
$ALT_CARGA = ($ALT_CARGA == 0) ? "No" : "Sí";
$PERFORACIONES = ($PERFORACIONES == 0) ? "No" : "Sí";
$TATUAJES = ($TATUAJES == 0) ? "No" : "Sí";

$DIABETES = ($DIABETES == 0) ? "No" : "Sí";
$HIPERTIROIDISMO2 = ($HIPERTIROIDISMO == 0) ? "No" : "Sí";
$HIPOTIROIDISMO2 = ($HIPOTIROIDISMO == 0) ? "No" : "Sí";
$SATISFACCION = ($SATISFACCION == 0) ? "No" : "Sí";
$COMP_TRABAJO = ($COMP_TRABAJO == 0) ? "No" : "Sí";
$COMP_ORGANIZACION = ($COMP_ORGANIZACION == 0) ? "No" : "Sí";

if ($OBSERVACIONESRESEXAM == 4) {
    $OBSERVACIONESRESEXAM1 = "No apto";
} elseif ($OBSERVACIONESRESEXAM == 1) {
    $OBSERVACIONESRESEXAM1 = "Apto";
} elseif ($OBSERVACIONESRESEXAM == 2) {
    $OBSERVACIONESRESEXAM1 = "Apto condicionado";
} elseif ($OBSERVACIONESRESEXAM == 3) {
    $OBSERVACIONESRESEXAM1 = "Apto restringido";
}

$DRENAJE = ($DRENAJE == 0) ? "No" : "Sí";
$VOZCLARAYFUERTE = ($VOZCLARAYFUERTE == 0) ? "No" : "Sí";

$jsonUser =  $model->searchUser($nss);
$dataUser = json_decode($jsonUser);

foreach ($dataUser->Datos as $user) {
    $nombre = $user->nombre;
    $apellidoP = $user->apellidoPaterno;
    $apellidoM = $user->apellidoMaterno;
    $RFC = $user->rfc;
    $NSS = $user->nss;
    $sexo = $user->direccion;
    $cuenta = $user->cuenta;
    $fechaNacimiento = $user->fecha_nacimiento;
    $edoCivil = $user->estado_civil;
    $direccion = $user->sexo;
    $telefono = $user->celular;
    $turno = $user->turno;
    $rfc = $user->rfc;
    $telefonoF = $user->tel_fijo;
    $check = $user->completo;
}

?>
<!DOCTYPE html>
<html>

<head>
    <title>SISTEMA MEDICO ONEST</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css" integrity="sha384-rbsA2VBKQhggwzxH7pPCaAqO46MgnOM80zW1RWuH61DGLwZJEdK2Kadq2F9CUG65" crossorigin="anonymous">
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.min.js" integrity="sha384-cuYeSxntonz0PPNlHhBs68uyIAVpIIOZZ5JqeqvYYIcEL727kskC66kF92t6Xl2V" crossorigin="anonymous"></script>
    <style>
        td,
        th,
        p {
            font-size: 9px;
        }

        .footerTxt {
            font-size: 7px;
        }
    </style>
</head>



<body class="container">


    <div class="row">
        <div class="navbar-brand col-3 " href="#">
            <center><img src="../files/Imagen1.png" alt="" width="100" class="mt-2"></center>
        </div>

        <div class="col-9">
            <table class="table table-bordered table-sm border-top border-dark">
                <thead>
                </thead>
                <tbody>
                    <tr>
                        <td colspan="2">
                            <center>EXAMEN MEDICO LABORAL</center>
                        </td>
                    </tr>
                    <tr class="">
                        <td>F. Implementación: <?= $FECHAExam; ?></td>
                        <td>Revisión: 03</td>


                    </tr>
                    <td>Clave de control: FT-SO-04</td>
                    <td>Reemplaza a: 02</td>
                    <tr>

                    </tr>
                    <tr class="">

                        <td>Motivo del cambio: Actualización por vigencia.</td>
                        <td>Hoja 1 de 5</td>

                    </tr>
                </tbody>
            </table>
        </div>




    </div>


    <div class="col-12">
        <h6>Información Personal</h6>
        <div class="row">
            <div class="col-6">
                <table class="table table-bordered table-sm">
                    <tr>
                        <th></th>
                        <th></th>
                    </tr>
                    <tr>
                        <td>Nombre</td>
                        <td><?php echo $nombre; ?></td>
                    </tr>
                    <tr>
                        <td>Apellido Paterno</td>
                        <td><?php echo $apellidoP; ?></td>
                    </tr>
                    <tr>
                        <td>Apellido Materno</td>
                        <td><?php echo $apellidoM; ?></td>
                    </tr>
                    <tr>
                        <td>RFC</td>
                        <td><?php echo $RFC; ?></td>
                    </tr>
                    <tr>
                        <td>NSS</td>
                        <td><?php echo $NSS; ?></td>
                    </tr>
                    <tr>
                        <td>Sexo</td>
                        <td><?php echo $sexo; ?></td>
                    </tr>
                    <tr>
                        <td>Cuenta</td>
                        <td><?php echo $cuenta; ?></td>
                    </tr>

                </table>
            </div>
            <div class="col-6">
                <table class="table table-bordered table-sm">
                    <tr>
                        <th></th>
                        <th></th>
                    <tr>
                        <td>Fecha de Nacimiento</td>
                        <td><?php echo $fechaNacimiento; ?></td>
                    </tr>
                    <tr>
                        <td>Estado Civil</td>
                        <td><?php echo $edoCivil; ?></td>
                    </tr>
                    <tr>
                        <td>Dirección</td>
                        <td><?php echo $direccion; ?></td>
                    </tr>
                    <tr>
                        <td>Teléfono Celular</td>
                        <td><?php echo $telefono; ?></td>
                    </tr>
                    <tr>
                        <td>Turno</td>
                        <td><?php echo $turno; ?></td>
                    </tr>
                    <tr>
                        <td>Teléfono Fijo</td>
                        <td><?php echo $telefonoF; ?></td>
                    </tr>
                    <tr>
                        <td>Completo</td>
                        <td><?php echo $check; ?></td>
                    </tr>
                </table>
            </div>
        </div>

    </div>
    <hr>
    <h6>Antecedentes Laborales</h6>
    <hr>
    <table class="table table-bordered table-sm">
        <tr>
            <th>Edad de inicio al trabajar</th>
            <td><?= $edad_inicio_laborar ?></td>
        </tr>
        <tr>
            <th>Cantidad de trabajos</th>
            <td><?= $cantidad_trabajos ?></td>
        </tr>
        <tr>
            <th>Pensión</th>
            <td><?= $pension ?></td>
        </tr>
    </table>

    <h6>Detalles de Trabajos Anteriores</h6>
    <table class="table table-bordered table-sm">
        <tr>
            <th>Nombre</th>
            <th>Giro</th>
            <th>Puesto</th>
            <th>Turno</th>
            <th>Antigüedad</th>
            <th>Salida</th>
            <th>Descripción</th>
            <th>Riesgos</th>
            <th>EPP (Equipo de Protección Personal)</th>
            <th>Observaciones</th>
        </tr>
        <tr>
            <td><?= $nombre1 ?></td>
            <td><?= $giro1 ?></td>
            <td><?= $puesto1 ?></td>
            <td><?= $turno1 ?></td>
            <td><?= $antiguedad1 ?></td>
            <td><?= $salida1 ?></td>
            <td><?= $descripcion1 ?></td>
            <td><?= $riesgos1 ?></td>
            <td><?= $epp1 ?></td>
            <td><?= $observaciones1 ?></td>
        </tr>
        <tr>
            <td><?= $nombre2 ?></td>
            <td><?= $giro2 ?></td>
            <td><?= $puesto2 ?></td>
            <td><?= $turno2 ?></td>
            <td><?= $antiguedad2 ?></td>
            <td><?= $salida2 ?></td>
            <td><?= $descripcion2 ?></td>
            <td><?= $riesgos2 ?></td>
            <td><?= $epp2 ?></td>
            <td><?= $observaciones2 ?></td>
        </tr>
        <tr>
            <td><?= $nombre3 ?></td>
            <td><?= $giro3 ?></td>
            <td><?= $puesto3 ?></td>
            <td><?= $turno3 ?></td>
            <td><?= $antiguedad3 ?></td>
            <td><?= $salida3 ?></td>
            <td><?= $descripcion3 ?></td>
            <td><?= $riesgos3 ?></td>
            <td><?= $epp3 ?></td>
            <td><?= $observaciones3 ?></td>
        </tr>
        <tr>
            <td><?= $nombre4 ?></td>
            <td><?= $giro4 ?></td>
            <td><?= $puesto4 ?></td>
            <td><?= $turno4 ?></td>
            <td><?= $antiguedad4 ?></td>
            <td><?= $salida4 ?></td>
            <td><?= $descripcion4 ?></td>
            <td><?= $riesgos4 ?></td>
            <td><?= $epp4 ?></td>
            <td><?= $observaciones4 ?></td>
        </tr>
    </table>
    <div class="col-12" style="margin-top: 510px;">
        <div class="row">
            <div class="navbar-brand col-3 " href="#">
                <center><img src="../files/Imagen1.png" alt="" width="100" class="mt-2"></center>
            </div>

            <div class="col-9">
                <table class="table table-bordered table-sm border-top border-dark">
                    <thead>
                    </thead>
                    <tbody>
                        <tr>
                            <td colspan="2">
                                <center>EXAMEN MEDICO LABORAL</center>
                            </td>
                        </tr>
                        <tr class="">
                            <td>F. Implementación: <?= $FECHAExam; ?></td>
                            <td>Revisión: 03</td>


                        </tr>
                        <td>Clave de control: FT-SO-04</td>
                        <td>Reemplaza a: 02</td>
                        <tr>

                        </tr>
                        <tr class="">

                            <td>Motivo del cambio: Actualización por vigencia.</td>
                            <td>Hoja 1 de 5</td>

                        </tr>
                    </tbody>
                </table>
            </div>




        </div>
    </div>
    <hr>
    <h6>Antecedentes Heredofamiliares</h6>
    <hr>


    <div class="row">
        <div class="col-6">
            <h6>Neurología</h6>
            <table class="table table-bordered table-sm">
                <tr>
                    <th class="table-primary">Condición</th>
                    <th class="table-primary">Valor</th>
                    <th class="table-primary">Observaciones</th>
                </tr>
                <tr>
                    <td>AVC (Accidente Cerebrovascular)</td>
                    <td><?php echo $avc; ?></td>
                    <td><?php echo $AVC_OBS; ?></td>
                </tr>
                <tr>
                    <td>Epilepsia</td>
                    <td><?php echo ($EPILEPSIA == 0) ? "No" : "Sí"; ?></td>
                    <td><?php echo $EPILEPSIA_OBS; ?></td>
                </tr>
                <tr>
                    <td>Parálisis Facial</td>
                    <td><?php echo $PARALISIS_FACIAL; ?></td>
                    <td><?php echo $PARALISIS_FACIAL_OBS; ?></td>
                </tr>
                <tr>
                    <td>Parkinson</td>
                    <td><?php echo $PARKINSON; ?></td>
                    <td><?php echo $PARKINSON_OBS; ?></td>
                </tr>
                <tr>
                    <td>Alzheimer</td>
                    <td><?php echo $ALZHEIMER; ?></td>
                    <td><?php echo $ALZHEIMER_OBS; ?></td>
                </tr>
                <tr>
                    <td>Migraña</td>
                    <td><?php echo $MIGRANA; ?></td>
                    <td><?php echo $MIGRANA_OBS; ?></td>
                </tr>
            </table>
        </div>
        <div class="col-6">
            <h6>Cardiopatía</h6>
            <table class="table table-bordered table-sm">
                <tr>
                    <th class="table-primary">Condición</th>
                    <th class="table-primary">Valor</th>
                    <th class="table-primary">Observaciones</th>
                </tr>
                <tr>
                    <td>Cardiopatía Isquémica</td>
                    <td><?php echo $CARDIOPATIA_ISQUEMICA; ?></td>
                    <td><?php echo $CARDIOPATIA_ISQUEMICA_OBS; ?></td>
                </tr>
                <tr>
                    <td>Hipertensión</td>
                    <td><?php echo $HIPERTENSION; ?></td>
                    <td><?php echo $HIPERTENSION_OBS; ?></td>
                </tr>
                <tr>
                    <td>Hipotensión</td>
                    <td><?php echo $HIPOTENSION; ?></td>
                    <td><?php echo $HIPOTENSION_OBS; ?></td>
                </tr>
                <tr>
                    <td>Insuficiencia Cardíaca</td>
                    <td><?php echo $INSUFICIENCIA_CARDIACA; ?></td>
                    <td><?php echo $INSUFICIENCIA_CARDIACA_OBS; ?></td>
                </tr>
                <tr>
                    <td>Insuficiencia Vascular Periférica</td>
                    <td><?php echo $INSUFICIENCIA_VASCULAR_PERIFERICA; ?></td>
                    <td><?php echo $INSUFICIENCIA_VASCULAR_PERIFERICA_OBS; ?></td>
                </tr>
            </table>
        </div>
        <div class="col-6">
            <h6>Neumopatía</h6>
            <table class="table table-bordered table-sm">
                <tr>
                    <th class="table-primary">Condición</th>
                    <th class="table-primary">Valor</th>
                    <th class="table-primary">Observaciones</th>
                </tr>
                <tr>
                    <td>Bronquitis</td>
                    <td><?php echo $BRONQUITIS; ?></td>
                    <td><?php echo $BRONQUITIS_OBS; ?></td>
                </tr>
                <tr>
                    <td>Tuberculosis</td>
                    <td><?php echo $TUBERCULOSIS; ?></td>
                    <td><?php echo $TUBERCULOSIS_OBS; ?></td>
                </tr>
                <tr>
                    <td>Asma</td>
                    <td><?php echo $ASMA; ?></td>
                    <td><?php echo $ASMA_OBS; ?></td>
                </tr>
                <tr>
                    <td>Insuficiencia Respiratoria</td>
                    <td><?php echo $INSUFICIENCIA_RESPIRATORIA; ?></td>
                    <td><?php echo $INSUFICIENCIA_RESPIRATORIA_OBS; ?></td>
                </tr>
            </table>
        </div>
        <div class="col-6">
            <h6>Toxicología</h6>
            <table class="table table-bordered table-sm">
                <tr>
                    <th class="table-primary">Condición</th>
                    <th class="table-primary">Valor</th>
                    <th class="table-primary">Observaciones</th>
                </tr>
                <tr>
                    <td>Alcoholismo</td>
                    <td><?php echo $ALCOHOLISMO; ?></td>
                    <td><?php echo $ALCOHOLISMO_OBS; ?></td>
                </tr>
                <tr>
                    <td>Tabaquismo</td>
                    <td><?php echo $TABAQUISMO; ?></td>
                    <td><?php echo $TABAQUISMO_OBS; ?></td>
                </tr>
                <tr>
                    <td>Drogas</td>
                    <td><?php echo $DROGAS; ?></td>
                    <td><?php echo $DROGAS_OBS; ?></td>
                </tr>
            </table>
        </div>
        <div class="col-6">
            <h6>Nefropatía</h6>
            <table class="table table-bordered table-sm">
                <tr>
                    <th class="table-primary">Condición</th>
                    <th class="table-primary">Valor</th>
                    <th class="table-primary">Observaciones</th>
                </tr>
                <tr>
                    <td>IRC (Insuficiencia Renal Crónica)</td>
                    <td><?php echo $IRC; ?></td>
                    <td><?php echo $IRC_OBS; ?></td>
                </tr>
                <tr>
                    <td>Litiasis Renal</td>
                    <td><?php echo $LITASIS; ?></td>
                    <td><?php echo $LITASIS_OBS; ?></td>
                </tr>
            </table>
        </div>
        <div class="col-6">
            <h6>Endocrina</h6>
            <table class="table table-bordered table-sm">
                <tr>
                    <th class="table-primary">Condición</th>
                    <th class="table-primary">Valor</th>
                    <th class="table-primary">Observaciones</th>
                </tr>
                <tr>
                    <td>Diabetes Mellitus (DM)</td>
                    <td><?php echo $DM; ?></td>
                    <td><?php echo $DM_OBS; ?></td>
                </tr>
                <tr>
                    <td>Hipertiroidismo</td>
                    <td><?php echo $HIPERTIROIDISMO; ?></td>
                    <td><?php echo $HIPERTIROIDISMO_OBS; ?></td>
                </tr>
                <tr>
                    <td>Hipotiroidismo</td>
                    <td><?php echo $HIPOTIROIDISMO; ?></td>
                    <td><?php echo $HIPOTIROIDISMO_OBS; ?></td>
                </tr>
            </table>
        </div>

        <div class="col-6">
            <h6>Obesidad</h6>
            <table class="table table-bordered table-sm">
                <tr>
                    <th class="table-primary">Trastorno de Crecimiento</th>
                    <th class="table-primary">Valor</th>
                    <th class="table-primary">Observaciones</th>
                </tr>
                <tr>
                    <td>Trastorno de Crecimiento</td>
                    <td><?php echo $TRANSTORNO_DE_CRECIMIENTO; ?></td>
                    <td><?php echo $TRANSTORNO_DE_CRECIMIENTO_OBS; ?></td>
                </tr>
            </table>
        </div>
        <div class="col-6">
            <h6>Salud Mental</h6>
            <table class="table table-bordered table-sm">
                <tr>
                    <th class="table-primary">Condición</th>
                    <th class="table-primary">Valor</th>
                    <th class="table-primary">Observaciones</th>
                </tr>
                <tr>
                    <td>Neurosis</td>
                    <td><?php echo $NEUROSIS; ?></td>
                    <td><?php echo $NEUROSIS_OBS; ?></td>
                </tr>
                <tr>
                    <td>Psicosis</td>
                    <td><?php echo $PSICOSIS; ?></td>
                    <td><?php echo $PSICOSIS_OBS; ?></td>
                </tr>
                <tr>
                    <td>Intento de Suicidio</td>
                    <td><?php echo $INTENTO_DE_SUICIDIO; ?></td>
                    <td><?php echo $INTENTO_DE_SUICIDIO_OBS; ?></td>
                </tr>
                <tr>
                    <td>Esquizofrenia</td>
                    <td><?php echo $ESQUIZOFRENIA; ?></td>
                    <td><?php echo $ESQUIZOFRENIA_OBS; ?></td>
                </tr>
            </table>
        </div>
        <div class="col-12" style="margin-top: 100px;">
            <div class="row">
                <div class="navbar-brand col-3 " href="#">
                    <center><img src="../files/Imagen1.png" alt="" width="100" class="mt-2"></center>
                </div>

                <div class="col-9">
                    <table class="table table-bordered table-sm border-top border-dark">
                        <thead>
                        </thead>
                        <tbody>
                            <tr>
                                <td colspan="2">
                                    <center>EXAMEN MEDICO LABORAL</center>
                                </td>
                            </tr>
                            <tr class="">
                                <td>F. Implementación: <?= $FECHAExam; ?></td>
                                <td>Revisión: 03</td>


                            </tr>
                            <td>Clave de control: FT-SO-04</td>
                            <td>Reemplaza a: 02</td>
                            <tr>

                            </tr>
                            <tr class="">

                                <td>Motivo del cambio: Actualización por vigencia.</td>
                                <td>Hoja 2 de 5</td>

                            </tr>
                        </tbody>
                    </table>
                </div>




            </div>
        </div>
        <div class="col-6">
            <h6>Generales</h6>
            <table class="table table-bordered table-sm">
                <tr>
                    <th class="table-primary">Condición</th>
                    <th class="table-primary">Valor</th>
                    <th class="table-primary">Observaciones</th>
                </tr>
                <tr>
                    <td>Cáncer</td>
                    <td><?php echo $CANCER; ?></td>
                    <td><?php echo $CANCER_OBS; ?></td>
                </tr>
                <tr>
                    <td>Alergias</td>
                    <td><?php echo $ALERGIAS; ?></td>
                    <td><?php echo $ALERGIAS_OBS; ?></td>
                </tr>
                <tr>
                    <td>Malformaciones</td>
                    <td><?php echo $MALFORMACIONES; ?></td>
                    <td><?php echo $MALFORMACIONES_OBS; ?></td>
                </tr>
            </table>
        </div>
        <div class="col-6">
            <h6>Otras Condiciones</h6>
            <table class="table table-bordered table-sm">
                <tr>
                    <th class="table-primary">Condición</th>
                    <th class="table-primary">Valor</th>
                    <th class="table-primary">Observaciones</th>
                </tr>
                <tr>
                    <td>Otras</td>
                    <td><?php echo $OTRAS; ?></td>
                    <td><?php echo $OTRAS_OBS; ?></td>
                </tr>
            </table>
        </div>
        <div class="col-12">
            <hr>
            <h6>Antecedentes personales no Patológicos</h6>
            <div class="row">
                <div class="col-6">
                    <table class="table table table-bordered table-sm">
                        <tr>
                            <th class="table-primary">Aspecto</th>
                            <th class="table-primary">Valor</th>
                        </tr>
                        <tr>
                            <td>Habitación</td>
                            <td><?php echo $HABITACION; ?></td>
                        </tr>
                        <tr>
                            <td>Número de Dormitorios</td>
                            <td><?php echo $NO_DORMITORIOS; ?></td>
                        </tr>
                        <tr>
                            <td>Número de Habitantes</td>
                            <td><?php echo $NO_HABITANTES; ?></td>
                        </tr>
                        <tr>
                            <td>Piso</td>
                            <td><?php echo $PISO; ?></td>
                        </tr>
                        <tr>
                            <td>Techo</td>
                            <td><?php echo $TECHO; ?></td>
                        </tr>
                        <tr>
                            <td>Agua Potable</td>
                            <td><?php echo $AGUA_POTABLE; ?></td>
                        </tr>
                        <tr>
                            <td>Luz Eléctrica</td>
                            <td><?php echo $LUZ; ?></td>
                        </tr>
                        <tr>
                            <td>Drenaje</td>
                            <td><?php echo $DRENAJE; ?></td>
                        </tr>
                        <tr>
                            <td>Convivencia con Animales</td>
                            <td><?php echo $CONVIVENCIA_CON_ANIMALES; ?></td>
                        </tr>
                        <tr>
                            <td>Aire Acondicionado</td>
                            <td><?php echo $AIRE_ACONDICIONADO; ?></td>
                        </tr>
                        <tr>
                            <td>Religión</td>
                            <td><?php echo $RELIGION; ?></td>
                        </tr>
                        <tr>
                            <td>Higiene Personal</td>
                            <td><?php echo $HIGIENE_PERSONAL; ?></td>
                        </tr>

                    </table>
                </div>
                <div class="col-6">
                    <table class="table table table-bordered table-sm">
                        <tr>
                            <th class="table-primary">Aspecto</th>
                            <th class="table-primary">Valor</th>
                        </tr>

                        <tr>
                            <td>Baño</td>
                            <td><?php echo $BAÑO; ?></td>
                        </tr>
                        <tr>
                            <td>Cambio de Ropa</td>
                            <td><?php echo $CAMBIO_DE_ROPA; ?></td>
                        </tr>
                        <tr>
                            <td>Aseo Bucal</td>
                            <td><?php echo $ASEO_BUCAL; ?></td>
                        </tr>
                        <tr>
                            <td>Alimentación</td>
                            <td><?php echo $ALIMENTACION; ?></td>
                        </tr>
                        <tr>
                            <td>Número de Comidas al Día</td>
                            <td><?php echo $NO_COMIDAS_AL_DIA; ?></td>
                        </tr>
                        <tr>
                            <td>Alimentación Balanceada</td>
                            <td><?php echo $BALANCEADA; ?></td>
                        </tr>
                        <tr>
                            <td>Alimentación Suficiente</td>
                            <td><?php echo $SUFICIENTE; ?></td>
                        </tr>
                        <tr>
                            <td>Deporte</td>
                            <td><?php echo $DEPORTE; ?></td>
                        </tr>
                        <tr>
                            <td>Frecuencia 1</td>
                            <td><?php echo $FC1; ?></td>
                        </tr>
                        <tr>
                            <td>Hobbies y Pasatiempos</td>
                            <td><?php echo $HOBBIES_PASATIEMPOS; ?></td>
                        </tr>
                        <tr>
                            <td>Frecuencia 2</td>
                            <td><?php echo $FC2; ?></td>
                        </tr>
                    </table>
                </div>
            </div>

        </div>

        <div class="col-12">
            <h6>Inmunizaciones</h6>
            <table class="table table-bordered table-sm">
                <tr>
                    <th class="table-primary">Vacuna</th>
                    <th class="table-primary">Valor</th>
                </tr>
                <tr>
                    <td>BCG</td>
                    <td><?php echo $BLG; ?></td>
                </tr>
                <tr>
                    <td>Sabin</td>
                    <td><?php echo $SABIN; ?></td>
                </tr>
                <tr>
                    <td>DPT</td>
                    <td><?php echo $DPT; ?></td>
                </tr>
                <tr>
                    <td>Hepatitis B</td>
                    <td><?php echo $HEPATITIS_B; ?></td>
                </tr>
                <tr>
                    <td>SR</td>
                    <td><?php echo $SR; ?></td>
                </tr>
                <tr>
                    <td>Influenza</td>
                    <td><?php echo $INFLUENZA; ?></td>
                </tr>
                <tr>
                    <td>TD</td>
                    <td><?php echo $TD; ?></td>
                </tr>
                <tr>
                    <td>COVID</td>
                    <td><?php echo $COVID; ?></td>
                </tr>
                <tr>
                    <td>Fecha</td>
                    <td><?php echo $FECHA; ?></td>
                </tr>
            </table>
        </div>
        <div class="col-12" style="margin-top: 100px;">
            <div class="row">
                <div class="navbar-brand col-3 " href="#">
                    <center><img src="../files/Imagen1.png" alt="" width="100" class="mt-2"></center>
                </div>

                <div class="col-9">
                    <table class="table table-bordered table-sm border-top border-dark">
                        <thead>
                        </thead>
                        <tbody>
                            <tr>
                                <td colspan="2">
                                    <center>EXAMEN MEDICO LABORAL</center>
                                </td>
                            </tr>
                            <tr class="">
                                <td>F. Implementación: <?= $FECHAExam; ?></td>
                                <td>Revisión: 03</td>


                            </tr>
                            <td>Clave de control: FT-SO-04</td>
                            <td>Reemplaza a: 02</td>
                            <tr>

                            </tr>
                            <tr class="">

                                <td>Motivo del cambio: Actualización por vigencia.</td>
                                <td>Hoja 2 de 5</td>

                            </tr>
                        </tbody>
                    </table>
                </div>




            </div>
        </div>
        <div class="col-12">
            <h6>Antecedentes Ginecológicos y Obstétricos</h6>
            <table class="table table-bordered table-sm">
                <tr>
                    <th class="table-primary">Variable</th>
                    <th class="table-primary">Valor</th>
                </tr>
                <tr>
                    <td>Menarca</td>
                    <td><?php echo $MENARCA; ?></td>
                </tr>
                <tr>
                    <td>Ritmo Menstrual</td>
                    <td><?php echo $RITMO; ?></td>
                </tr>
                <tr>
                    <td>IVSA (Inicio de Vida Sexual Activa)</td>
                    <td><?php echo $IVSA; ?></td>
                </tr>
                <tr>
                    <td>FUM (Fecha de Última Menstruación)</td>
                    <td><?php echo $FUM; ?></td>
                </tr>
                <tr>
                    <td>G (Gestas)</td>
                    <td><?php echo $G; ?></td>
                </tr>
                <tr>
                    <td>A (Abortos)</td>
                    <td><?php echo $A; ?></td>
                </tr>
                <tr>
                    <td>P (Partos)</td>
                    <td><?php echo $P; ?></td>
                </tr>
                <tr>
                    <td>C (Cesáreas)</td>
                    <td><?php echo $C; ?></td>
                </tr>
                <tr>
                    <td>Método de Planificación</td>
                    <td><?php echo $MET_PLANIFICACION; ?></td>
                </tr>
                <tr>
                    <td>Papanicolaou</td>
                    <td><?php echo $PAPANICOLAOU; ?></td>
                </tr>
                <tr>
                    <td>Infecciones Reproductivas</td>
                    <td><?php echo $INFECC_REP; ?></td>
                </tr>
                <tr>
                    <td>Fecha de Última Consulta Ginecológica</td>
                    <td><?php echo $FUP; ?></td>
                </tr>
            </table>
        </div>
        <div class="col-12">
            <h6>Antecedentes Patológicos Personales</h6>
            <table class="table table-bordered table-sm">
                <tr>
                    <th class="table-primary">Condición</th>
                    <th class="table-primary">Valor</th>
                </tr>
                <tr>
                    <td>Enfermedades Congénitas</td>
                    <td><?php echo $ENF_CONGENITAS; ?></td>
                </tr>
                <tr>
                    <td>Genital</td>
                    <td><?php echo $GENITAL; ?></td>
                </tr>
                <tr>
                    <td>Enfermedades Propias de la Infancia</td>
                    <td><?php echo $ENF_PROP_INF; ?></td>
                </tr>
                <tr>
                    <td>Dermatopatías</td>
                    <td><?php echo $DERMATOPATIAS; ?></td>
                </tr>
                <tr>
                    <td>Sistema Auditivo</td>
                    <td><?php echo $SIST_AUDITIVO; ?></td>
                </tr>
                <tr>
                    <td>Sistema Respiratorio</td>
                    <td><?php echo $RESPIRATORIO; ?></td>
                </tr>
                <tr>
                    <td>Sistema Olfativo</td>
                    <td><?php echo $SIS_OLFATIVO; ?></td>
                </tr>
                <tr>
                    <td>Traumatismos</td>
                    <td><?php echo $TRAUMATICOS; ?></td>
                </tr>
            </table>
        </div>

        <div class="col-6">
            <h6>Oftalmológico</h6>
            <table class="table table-bordered table-sm">
                <tr>
                    <th class="table-primary">Condición</th>
                    <th class="table-primary">Valor</th>
                    <th class="table-primary">Observaciones</th>
                </tr>
                <tr>
                    <td>Uso de Lentes</td>
                    <td><?php echo $USO_LENTES; ?></td>
                    <td><?php echo $USO_LENTES_OBS; ?></td>
                </tr>
                <tr>
                    <td>Astigmatismo</td>
                    <td><?php echo $ASTIGMATISMO; ?></td>
                    <td><?php echo $ASTIGMATISMO_OBS; ?></td>
                </tr>
                <tr>
                    <td>Presbicia</td>
                    <td><?php echo $PRESBICIA; ?></td>
                    <td><?php echo $PRESBICIA_OBS; ?></td>
                </tr>
                <tr>
                    <td>Estrabismo</td>
                    <td><?php echo $ESTRABISMO; ?></td>
                    <td><?php echo $ESTRABISMO_OBS; ?></td>
                </tr>
                <tr>
                    <td>Daltonismo</td>
                    <td><?php echo $DALTONISMO; ?></td>
                    <td><?php echo $DALTONISMO_OBS; ?></td>
                </tr>
                <tr>
                    <td>Desprendimiento de Retina</td>
                    <td><?php echo $DESPENDIMIENTO_RET; ?></td>
                    <td><?php echo $DESPENDIMIENTO_RET_OBS; ?></td>
                </tr>
                <tr>
                    <td>Miopia</td>
                    <td><?php echo $MIOPIA; ?></td>
                    <td><?php echo $MIOPIA_OBS; ?></td>
                </tr>
            </table>
        </div>
        <div class="col-6">
            <h6>Trastornos Digestivos</h6>
            <table class="table table-bordered table-sm">
                <tr>
                    <th class="table-primary">Condición</th>
                    <th class="table-primary">Valor</th>
                    <th class="table-primary">Observaciones</th>
                </tr>
                <tr>
                    <td>Hernias</td>
                    <td><?php echo $HERNIAS; ?></td>
                    <td><?php echo $HERNIAS_OBS; ?></td>
                </tr>
                <tr>
                    <td>Cirrosis</td>
                    <td><?php echo $CIRROSIS; ?></td>
                    <td><?php echo $CIRROSIS_OBS; ?></td>
                </tr>
                <tr>
                    <td>Colitis</td>
                    <td><?php echo $COLITIS; ?></td>
                    <td><?php echo $COLITIS_OBS; ?></td>
                </tr>
                <tr>
                    <td>Gastritis</td>
                    <td><?php echo $GASTRITIS; ?></td>
                    <td><?php echo $GASTRITIS_OBS; ?></td>
                </tr>
                <tr>
                    <td>Hemorroides</td>
                    <td><?php echo $HEMORROIDES; ?></td>
                    <td><?php echo $HEMORROIDES_OBS; ?></td>
                </tr>
                <tr>
                    <td>Cirugías</td>
                    <td><?php echo $CIRUGIAS; ?></td>
                    <td><?php echo $CIRUGIAS_OBS; ?></td>
                </tr>
            </table>

        </div>
        <div class="col-12" style="margin-top: 60px;">
            <div class="row">
                <div class="navbar-brand col-3 " href="#">
                    <center><img src="../files/Imagen1.png" alt="" width="100" class="mt-2"></center>
                </div>

                <div class="col-9">
                    <table class="table table-bordered table-sm border-top border-dark">
                        <thead>
                        </thead>
                        <tbody>
                            <tr>
                                <td colspan="2">
                                    <center>EXAMEN MEDICO LABORAL</center>
                                </td>
                            </tr>
                            <tr class="">
                                <td>F. Implementación: <?= $FECHAExam; ?></td>
                                <td>Revisión: 03</td>


                            </tr>
                            <td>Clave de control: FT-SO-04</td>
                            <td>Reemplaza a: 02</td>
                            <tr>

                            </tr>
                            <tr class="">

                                <td>Motivo del cambio: Actualización por vigencia.</td>
                                <td>Hoja 3 de 5</td>

                            </tr>
                        </tbody>
                    </table>
                </div>




            </div>
        </div>
        <div class="col-6">
            <h6>Renal</h6>
            <table class="table table-bordered table-sm">
                <tr>
                    <th class="table-primary">Condición</th>
                    <th class="table-primary">Valor</th>
                    <th class="table-primary">Observaciones</th>
                </tr>
                <tr>
                    <td>Incontinencia</td>
                    <td><?php echo $INCONTINENSIA; ?></td>
                    <td><?php echo $INCONTINENSIA_OBS; ?></td>
                </tr>
                <tr>
                    <td>Insuficiencia Renal</td>
                    <td><?php echo $INSUFICIENCIA; ?></td>
                    <td><?php echo $INSUFICIENCIA_OBS; ?></td>
                </tr>
                <tr>
                    <td>Litiasis Renal</td>
                    <td><?php echo $LITIASIS; ?></td>
                    <td><?php echo $LITIASIS_OBS; ?></td>
                </tr>
            </table>
        </div>
        <div class="col-6">
            <h6>Sistema Nervioso</h6>
            <table class="table table-bordered table-sm">
                <tr>
                    <th class="table-primary">Condición</th>
                    <th class="table-primary">Valor</th>
                    <th class="table-primary">Observaciones</th>
                </tr>
                <tr>
                    <td>Aneurisma</td>
                    <td><?php echo $ANEURISMA; ?></td>
                    <td><?php echo $ANEURISMA_OBS; ?></td>
                </tr>
                <tr>
                    <td>Enfermedad Degenerativa Muscular/Nerviosa</td>
                    <td><?php echo $ENF_DEG_MUS_NEU; ?></td>
                    <td><?php echo $ENF_DEG_MUS_NEU_OBS; ?></td>
                </tr>
                <tr>
                    <td>Accidente Cerebrovascular</td>
                    <td><?php echo $ACC_CER_VASCU; ?></td>
                    <td><?php echo $ACC_CER_VASCU_OBS; ?></td>
                </tr>
                <tr>
                    <td>Epilepsia</td>
                    <td><?php echo $EPILEPSIA2; ?></td>
                    <td><?php echo $EPILEPSIA_OBS2; ?></td>
                </tr>
                <tr>
                    <td>Parálisis Facial</td>
                    <td><?php echo $PAR_FACIAL; ?></td>
                    <td><?php echo $PAR_FACIAL_OBS; ?></td>
                </tr>
            </table>
        </div>
        <div class="col-6">
            <h6>Musculoesquelético</h6>
            <table class="table table-bordered table-sm">
                <tr>
                    <th class="table-primary">Condición</th>
                    <th class="table-primary">Valor</th>
                    <th class="table-primary">Observaciones</th>
                </tr>
                <tr>
                    <td>Escoliosis</td>
                    <td><?php echo $ESCOLIOSIS; ?></td>
                    <td><?php echo $ESCOLIOSIS_OBS; ?></td>
                </tr>
                <tr>
                    <td>Xifosis</td>
                    <td><?php echo $XIFOSIS; ?></td>
                    <td><?php echo $XIFOSIS_OBS; ?></td>
                </tr>
                <tr>
                    <td>Lordosis</td>
                    <td><?php echo $LORDOSISI; ?></td>
                    <td><?php echo $LORDOSISI_OBS; ?></td>
                </tr>
                <tr>
                    <td>Lumbalgia</td>
                    <td><?php echo $LUMBALGIA; ?></td>
                    <td><?php echo $LUMBALGIA_OBS; ?></td>
                </tr>
                <tr>
                    <td>Basculamiento</td>
                    <td><?php echo $BASCULAMIENTO; ?></td>
                    <td><?php echo $BASCULAMIENTO_OBS; ?></td>
                </tr>
                <tr>
                    <td>Alteración Discal</td>
                    <td><?php echo $ALTERACION_DISCAL; ?></td>
                    <td><?php echo $ALTERACION_DISCAL_OBS; ?></td>
                </tr>
            </table>
        </div>
        <div class="col-6">
            <h6>Cardiopatía</h6>
            <table class="table table-bordered table-sm">
                <tr>
                    <th class="table-primary">Condición</th>
                    <th class="table-primary">Valor</th>
                    <th class="table-primary">Observaciones</th>
                </tr>
                <tr>
                    <td>Aneurisma</td>
                    <td><?php echo $ANEURISMA2; ?></td>
                    <td><?php echo $ANEURISMA_OBS2; ?></td>
                </tr>
                <tr>
                    <td>Hipertensión</td>
                    <td><?php echo $HIPERTENSION2; ?></td>
                    <td><?php echo $HIPERTENSION_OBS2; ?></td>
                </tr>
                <tr>
                    <td>Hipotensión</td>
                    <td><?php echo $HIPOTENSION2; ?></td>
                    <td><?php echo $HIPOTENSION_OBS2; ?></td>
                </tr>
                <tr>
                    <td>Infartos</td>
                    <td><?php echo $INFARTOS; ?></td>
                    <td><?php echo $INFARTOS_OBS; ?></td>
                </tr>
                <tr>
                    <td>Insuficiencia Cardíaca</td>
                    <td><?php echo $INSUFIC_CAR; ?></td>
                    <td><?php echo $INSUFIC_CAR_OBS; ?></td>
                </tr>
            </table>
        </div>
        <div class="col-6">
            <h6>Toxicológicos</h6>
            <table class="table table-bordered table-sm">
                <tr>
                    <th class="table-primary">Factor</th>
                    <th class="table-primary">Valor</th>
                    <th class="table-primary">Observaciones</th>
                </tr>
                <tr>
                    <td>Tabaquismo</td>
                    <td><?php echo $TABAQUISMO2; ?></td>
                    <td><?php echo $TABAQUISMO_OBS2; ?></td>
                </tr>
                <tr>
                    <td>Alcoholismo</td>
                    <td><?php echo $ALCOHOLISMO2; ?></td>
                    <td><?php echo $ALCOHOLISMO_OBS2; ?></td>
                </tr>
                <tr>
                    <td>Drogas</td>
                    <td><?php echo $DROGAS2; ?></td>
                    <td><?php echo $DROGAS_OBS2; ?></td>
                </tr>
                <tr>
                    <td>Alteración de Carga</td>
                    <td><?php echo $ALT_CARGA; ?></td>
                    <td><?php echo $ALT_CARGA_OBS; ?></td>
                </tr>
                <tr>
                    <td>Perforaciones</td>
                    <td><?php echo $PERFORACIONES; ?></td>
                    <td><?php echo $PERFORACIONES_OBS; ?></td>
                </tr>
                <tr>
                    <td>Tatuajes</td>
                    <td><?php echo $TATUAJES; ?></td>
                    <td><?php echo $TATUAJES_OBS; ?></td>
                </tr>
            </table>
        </div>
        <div class="col-6">
            <h6>Endocrinología</h6>
            <table class="table table-bordered table-sm">
                <tr>
                    <th class="table-primary">Condición</th>
                    <th class="table-primary">Valor</th>
                    <th class="table-primary">Observaciones</th>
                </tr>
                <tr>
                    <td>Diabetes</td>
                    <td><?php echo $DIABETES; ?></td>
                    <td><?php echo $DIABETES_OBS; ?></td>
                </tr>
                <tr>
                    <td>Hipertiroidismo</td>
                    <td><?php echo $HIPERTIROIDISMO2; ?></td>
                    <td><?php echo $HIPERTIROIDISMO_OBS2; ?></td>
                </tr>
                <tr>
                    <td>Hipotiroidismo</td>
                    <td><?php echo $HIPOTIROIDISMO2; ?></td>
                    <td><?php echo $HIPOTIROIDISMO_OBS2; ?></td>
                </tr>
            </table>
        </div>
        <div class="col-12">
            <hr>
        </div>

        <div class="col-12">
            <h6>Padecimiento Actual</h6>
            <table class="table table-bordered table-sm">
                <tr>
                    <th class="table-primary">Observaciones</th>
                </tr>
                <tr>
                    <td><?php echo $OBSERVACIONES; ?></td>
                </tr>
            </table>

            <h6>Interrogatorio por Aparatos y Sistemas</h6>
            <table class="table table-bordered table-sm">
                <tr>
                    <th class="table-primary">Observaciones</th>
                </tr>
                <tr>
                    <td><?php echo $OBSERVACIONES2; ?></td>
                </tr>
            </table>
        </div>
        <div class="col-12" style="margin-top: 50px;">
            <div class="row">
                <div class="navbar-brand col-3 " href="#">
                    <center><img src="../files/Imagen1.png" alt="" width="100" class="mt-2"></center>
                </div>

                <div class="col-9">
                    <table class="table table-bordered table-sm border-top border-dark">
                        <thead>
                        </thead>
                        <tbody>
                            <tr>
                                <td colspan="2">
                                    <center>EXAMEN MEDICO LABORAL</center>
                                </td>
                            </tr>
                            <tr class="">
                                <td>F. Implementación: <?= $FECHAExam; ?></td>
                                <td>Revisión: 03</td>


                            </tr>
                            <td>Clave de control: FT-SO-04</td>
                            <td>Reemplaza a: 02</td>
                            <tr>

                            </tr>
                            <tr class="">

                                <td>Motivo del cambio: Actualización por vigencia.</td>
                                <td>Hoja 3 de 5</td>

                            </tr>
                        </tbody>
                    </table>
                </div>




            </div>
        </div>
        <div class="col-12">

            <h6>Exploración Física</h6>
        </div>
        <div class="col-6">

            <table class="table table-bordered table-sm">
                <tr>
                    <th class="table-primary">Medición</th>
                    <th class="table-primary">Valor</th>
                </tr>
                <tr>
                    <td>Peso</td>
                    <td><?php echo $PESO; ?></td>
                </tr>
                <tr>
                    <td>Talla</td>
                    <td><?php echo $TALLA; ?></td>
                </tr>
                <tr>
                    <td>IMC (Índice de Masa Corporal)</td>
                    <td><?php echo $IMC; ?></td>
                </tr>
                <tr>
                    <td>Frecuencia Cardíaca (FC)</td>
                    <td><?php echo $FC; ?></td>
                </tr>
                <tr>
                    <td>Frecuencia Respiratoria (FR)</td>
                    <td><?php echo $FR; ?></td>
                </tr>
                <tr>
                    <td>Tensión Arterial (TA)</td>
                    <td><?php echo $TA; ?></td>
                </tr>
                <tr>
                    <td>Temperatura</td>
                    <td><?php echo $TEMP; ?></td>
                </tr>
                <tr>
                    <td>STPO2</td>
                    <td><?php echo $STPO2; ?></td>
                </tr>
                <tr>
                    <td>Constitución</td>
                    <td><?php echo $ECTOMORFICO; ?></td>
                </tr>
                <tr>
                    <td>Satisfacción</td>
                    <td><?php echo $SATISFACCION; ?></td>
                </tr>
                <tr>
                    <td>Compromiso con el Trabajo</td>
                    <td><?php echo $COMP_TRABAJO; ?></td>
                </tr>
                <tr>
                    <td>Compromiso con la Organización</td>
                    <td><?php echo $COMP_ORGANIZACION; ?></td>
                </tr>
                <tr>
                    <td>Tiempo</td>
                    <td><?php echo $TIEMPO; ?></td>
                </tr>
                <tr>
                    <td>Espacio Personal</td>
                    <td><?php echo $ESPACIO; ?></td>
                </tr>
                <tr>
                    <td>Persona</td>
                    <td><?php echo $PERSONA; ?></td>
                </tr>
                <tr>
                    <td>Estado emocional</td>
                    <td><?php echo $SERENIDAD; ?></td>
                </tr>
            </table>
        </div>
        <div class="col-6">
            <table class="table table-bordered table-sm">
                <tr>
                    <th class="table-primary">Aspecto</th>
                    <th class="table-primary">Valor</th>
                </tr>
                <tr>
                    <td>Fascies</td>
                    <td><?php echo $HIPOCRITA; ?></td>
                </tr>
                <tr>
                    <td>Marcha</td>
                    <td><?php echo $PARKINSON2; ?></td>
                </tr>
                <tr>
                    <td>Voz clara y fuerte</td>
                    <td><?php echo $VOZCLARAYFUERTE; ?></td>
                </tr>
                <tr>
                    <td>Lenguaje</td>
                    <td><?php echo $LENGUAJE; ?></td>
                </tr>
                <tr>
                    <td>Conversación Fluida</td>
                    <td><?php echo $CONVER_FLUIDA; ?></td>
                </tr>
                <tr>
                    <td>Coherencia Palabras</td>
                    <td><?php echo $COHERENCIA_PALABLAS; ?></td>
                </tr>
            </table>
        </div>
        <hr>
        <div class="col-12">
            <h6>Cráneo</h6>
            <table class="table table-bordered table-sm">
                <tr>
                    <th class="table-primary">Ítem</th>
                    <th class="table-primary">Valor</th>
                </tr>
                <tr>
                    <td>Ojos</td>
                    <td><?php echo $OJOS; ?></td>
                </tr>
                <tr>
                    <td>Pupilas</td>
                    <td><?php echo $PUPILAS; ?></td>
                </tr>
                <tr>
                    <td>Conjuntivas</td>
                    <td><?php echo $CONJUNTIVAS; ?></td>
                </tr>
                <tr>
                    <td>Reflejos</td>
                    <td><?php echo $REFLEJOS; ?></td>
                </tr>
                <tr>
                    <td>Fondo de Ojo</td>
                    <td><?php echo $FONDO_OJO; ?></td>
                </tr>
            </table>

            <h6>Agudeza Visual</h6>
            <table class="table table-bordered table-sm">
                <tr>
                    <th class="table-primary">Ítem</th>
                    <th class="table-primary">Valor</th>
                </tr>
                <tr>
                    <td>OD (Ojo Derecho)</td>
                    <td><?php echo $OD; ?></td>
                </tr>
                <tr>
                    <td>OIZ (Ojo Izquierdo)</td>
                    <td><?php echo $OIZ; ?></td>
                </tr>
                <tr>
                    <td>Colores</td>
                    <td><?php echo $COLORES; ?></td>
                </tr>
            </table>
            <div class="col-12" style="margin-top: 100px;">
                <div class="row">
                    <div class="navbar-brand col-3 " href="#">
                        <center><img src="../files/Imagen1.png" alt="" width="100" class="mt-2"></center>
                    </div>

                    <div class="col-9">
                        <table class="table table-bordered table-sm border-top border-dark">
                            <thead>
                            </thead>
                            <tbody>
                                <tr>
                                    <td colspan="2">
                                        <center>EXAMEN MEDICO LABORAL</center>
                                    </td>
                                </tr>
                                <tr class="">
                                    <td>F. Implementación: <?= $FECHAExam; ?></td>
                                    <td>Revisión: 03</td>


                                </tr>
                                <td>Clave de control: FT-SO-04</td>
                                <td>Reemplaza a: 02</td>
                                <tr>

                                </tr>
                                <tr class="">

                                    <td>Motivo del cambio: Actualización por vigencia.</td>
                                    <td>Hoja 4 de 5</td>

                                </tr>
                            </tbody>
                        </table>
                    </div>




                </div>
            </div>
            <h6>Nariz</h6>
            <table class="table table-bordered table-sm">
                <tr>
                    <th class="table-primary">Ítem</th>
                    <th class="table-primary">Valor</th>
                </tr>
                <tr>
                    <td>Cavidad Nasal</td>
                    <td><?php echo $CAVIDAD_NASAL; ?></td>
                </tr>
                <tr>
                    <td>Mucosa</td>
                    <td><?php echo $MUCOSA; ?></td>
                </tr>
                <tr>
                    <td>Tabique Nasal</td>
                    <td><?php echo $TABIQUE_NASAL; ?></td>
                </tr>
                <tr>
                    <td>Olfato</td>
                    <td><?php echo $OLFATO; ?></td>
                </tr>
            </table>

            <h6>Columna Vertebral</h6>
            <table class="table table-bordered table-sm">
                <tr>
                    <th class="table-primary">Ítem</th>
                    <th class="table-primary">Valor</th>
                </tr>
                <tr>
                    <td>Cervical</td>
                    <td><?php echo $CERVICAL; ?></td>
                </tr>
                <tr>
                    <td>Dorsal</td>
                    <td><?php echo $DORSAL; ?></td>
                </tr>
                <tr>
                    <td>Lumbar</td>
                    <td><?php echo $LUMBAR; ?></td>
                </tr>
            </table>
        </div>
        <div class="col-12">
            <!-- Lesiones en la Boca -->
            <h6>Lesiones en la Boca</h6>
            <table class="table table-bordered table-sm">
                <tr>
                    <th class="table-primary">Lesiones</th>
                    <th class="table-primary">Encías</th>
                    <th class="table-primary">Orofaringe</th>
                </tr>
                <tr>
                    <td><?php echo $LESIONES; ?></td>
                    <td><?php echo $ENCIAS; ?></td>
                    <td><?php echo $OROIFARINGE; ?></td>
                </tr>
            </table>

            <!-- Oídos -->
            <h6>Oídos</h6>
            <table class="table table-bordered table-sm">
                <tr>
                    <th class="table-primary">CAE</th>
                    <th class="table-primary">Membrana Timpanica</th>
                    <th class="table-primary">Agudeza Auditiva</th>
                    <th class="table-primary">Oído Derecho (OD)</th>
                    <th class="table-primary">Oído Izquierdo (OI)</th>
                </tr>
                <tr>
                    <td><?= $ODCAEINPEXP ?></td>
                    <td><?= $ODMEMTIMINPEXP ?></td>
                    <td><?= $ODAGAUDIINPEXP ?></td>
                    <td><?= $OIMEMTIMINPEXP ?></td>
                    <td><?= $OIAGAUDIINPEXP ?></td>
                </tr>
            </table>

            <!-- Tórax -->
            <h6>Tórax</h6>
            <table class="table table-bordered table-sm">
                <tr>
                    <th class="table-primary">Ruidos Cardiacos</th>
                    <th class="table-primary">Región Precordial</th>
                    <th class="table-primary">Campos Pulmonares</th>
                </tr>
                <tr>
                    <td><?php echo $RUIDOS_CARDIACOS; ?></td>
                    <td><?php echo $REG_PRECORDIAL; ?></td>
                    <td><?php echo $CAMPOS_PULM; ?></td>
                </tr>
            </table>

            <!-- Columna Vertebral -->
            <h6>Columna Vertebral</h6>
            <table class="table table-bordered table-sm">
                <tr>
                    <th class="table-primary">Deformaciones</th>
                    <th class="table-primary">Dolor</th>
                    <th class="table-primary">Movimientos</th>
                    <th class="table-primary">Marcha</th>
                    <th class="table-primary">Lasègue</th>
                    <th class="table-primary">Punta del Talón</th>
                </tr>
                <tr>
                    <td><?php echo $DEFORMACIONES; ?></td>
                    <td><?php echo $DOLOR; ?></td>
                    <td><?php echo $MOVIMIENTOS; ?></td>
                    <td><?php echo $MARCHA; ?></td>
                    <td><?php echo $LASSEGUE; ?></td>
                    <td><?php echo $PUNTA_TALON; ?></td>
                </tr>
            </table>
        </div>

        <div class="col-12">
            <h6>Abdomen</h6>
            <table class="table table-bordered table-sm">
                <tr>
                    <th class="table-primary">Condición</th>
                    <th class="table-primary">Valor</th>
                </tr>
                <tr>
                    <td>Forma</td>
                    <td><?php echo $FORMA; ?></td>
                </tr>
                <tr>
                    <td>Visceromegalias</td>
                    <td><?php echo $VISCEROMEGALIAS; ?></td>
                </tr>
                <tr>
                    <td>Hernias</td>
                    <td><?php echo $HERNIAS2; ?></td>
                </tr>
                <tr>
                    <td>Dolor</td>
                    <td><?php echo $DOLOR; ?></td>
                </tr>
                <tr>
                    <td>Peristalsis</td>
                    <td><?php echo $PERISTALSIS; ?></td>
                </tr>
            </table>
            <div class="col-12" style="margin-top: 50px;">
                <div class="row">
                    <div class="navbar-brand col-3 " href="#">
                        <center><img src="../files/Imagen1.png" alt="" width="100" class="mt-2"></center>
                    </div>

                    <div class="col-9">
                        <table class="table table-bordered table-sm border-top border-dark">
                            <thead>
                            </thead>
                            <tbody>
                                <tr>
                                    <td colspan="2">
                                        <center>EXAMEN MEDICO LABORAL</center>
                                    </td>
                                </tr>
                                <tr class="">
                                    <td>F. Implementación: <?= $FECHAExam; ?></td>
                                    <td>Revisión: 03</td>


                                </tr>
                                <td>Clave de control: FT-SO-04</td>
                                <td>Reemplaza a: 02</td>
                                <tr>

                                </tr>
                                <tr class="">

                                    <td>Motivo del cambio: Actualización por vigencia.</td>
                                    <td>Hoja 4 de 5</td>

                                </tr>
                            </tbody>
                        </table>
                    </div>




                </div>
            </div>
            <h6>Genitales</h6>
            <table class="table table-bordered table-sm">
                <tr>
                    <th class="table-primary">Género</th>
                    <th class="table-primary">Valor</th>
                </tr>
                <tr>
                    <td>Masculino</td>
                    <td><?php echo $MASCULINO; ?></td>
                </tr>
                <tr>
                    <td>Femenino</td>
                    <td><?php echo $FEMENINO; ?></td>
                </tr>
            </table>

            <h6>Urinario</h6>
            <table class="table table-bordered table-sm">
                <tr>
                    <th class="table-primary">Condición</th>
                    <th class="table-primary">Valor</th>
                </tr>
                <tr>
                    <td>Puntos Ureterales</td>
                    <td><?php echo $PUNTOS_URETRALES; ?></td>
                </tr>
                <tr>
                    <td>Fosas Renales</td>
                    <td><?php echo $FOSAS_RENALES; ?></td>
                </tr>
            </table>

            <h6>Extremidades</h6>
            <table class="table table-bordered table-sm">
                <tr>
                    <th class="table-primary">Extremidad</th>
                    <th class="table-primary">Valor</th>
                </tr>
                <tr>
                    <td>Superior</td>
                    <td><?php echo $SUPERIOR; ?></td>
                </tr>
                <tr>
                    <td>Inferior</td>
                    <td><?php echo $INFERIOR; ?></td>
                </tr>
                <tr>
                    <td>IVP</td>
                    <td><?php echo $IVP; ?></td>
                </tr>
                <tr>
                    <td>Edema</td>
                    <td><?php echo $EDEMA; ?></td>
                </tr>
            </table>

            <h6>Piel</h6>
            <table class="table table-bordered table-sm">
                <tr>
                    <th class="table-primary">Condición</th>
                    <th class="table-primary">Valor</th>
                </tr>
                <tr>
                    <td>Lunares</td>
                    <td><?php echo $LUNARES; ?></td>
                </tr>
                <tr>
                    <td>Pigmentación</td>
                    <td><?php echo $PIGMENTACION; ?></td>
                </tr>
                <tr>
                    <td>Cicatrices Quirúrgicas</td>
                    <td><?php echo $CICATRICES_QUIRUR; ?></td>
                </tr>
            </table>
        </div>
        <div class="col-12">
            <h6>Cuello</h6>
            <table class="table table-bordered table-sm">
                <tr>
                    <th class="table-primary">Deformaciones</th>
                    <th class="table-primary">Observaciones Tiroides</th>
                    <th class="table-primary">Observaciones Tráquea</th>
                </tr>
                <tr>
                    <td><?php echo $DEFORMACIONES_OBS; ?></td>
                    <td><?php echo $TIROIDES_OBS; ?></td>
                    <td><?php echo $TRAQUEA_OBS; ?></td>
                </tr>
            </table>
        </div>
        <div class="col-12">
            <h6>Observaciones generales</h6>
            <table class="table table-bordered table-sm">
                <tr>
                    <th class="table-primary">Observaciones</th>
                </tr>
                <tr>
                    <td><?php echo $OBSGENERALINPEX; ?></td>
                </tr>
            </table>
        </div>

        <div class="col-12" style="margin-top: 150px;">
            <div class="row">
                <div class="navbar-brand col-3 " href="#">
                    <center><img src="../files/Imagen1.png" alt="" width="100" class="mt-2"></center>
                </div>

                <div class="col-9">
                    <table class="table table-bordered table-sm border-top border-dark">
                        <thead>
                        </thead>
                        <tbody>
                            <tr>
                                <td colspan="2">
                                    <center>EXAMEN MEDICO LABORAL</center>
                                </td>
                            </tr>
                            <tr class="">
                                <td>F. Implementación: <?= $FECHAExam; ?></td>
                                <td>Revisión: 03</td>


                            </tr>
                            <td>Clave de control: FT-SO-04</td>
                            <td>Reemplaza a: 02</td>
                            <tr>

                            </tr>
                            <tr class="">

                                <td>Motivo del cambio: Actualización por vigencia.</td>
                                <td>Hoja 5 de 5</td>

                            </tr>
                        </tbody>
                    </table>
                </div>




            </div>
        </div>
        <div class="col-6">
            <h6>Dientes</h6>
            <table class="table table-bordered table-sm">
                <tr>
                    <th class="table-primary">Diente</th>
                    <th class="table-primary">Valor</th>
                </tr>
                <tr>
                    <td>Diente 11</td>
                    <td><?php echo $DIENTE_11; ?></td>
                </tr>
                <tr>
                    <td>Diente 12</td>
                    <td><?php echo $DIENTE_12; ?></td>
                </tr>
                <tr>
                    <td>Diente 13</td>
                    <td><?php echo $DIENTE_13; ?></td>
                </tr>
                <tr>
                    <td>Diente 14</td>
                    <td><?php echo $DIENTE_14; ?></td>
                </tr>
                <tr>
                    <td>Diente 15</td>
                    <td><?php echo $DIENTE_15; ?></td>
                </tr>
                <tr>
                    <td>Diente 16</td>
                    <td><?php echo $DIENTE_16; ?></td>
                </tr>
                <tr>
                    <td>Diente 17</td>
                    <td><?php echo $DIENTE_17; ?></td>
                </tr>
                <tr>
                    <td>Diente 18</td>
                    <td><?php echo $DIENTE_18; ?></td>
                </tr>
                <tr>
                    <td>Diente 41</td>
                    <td><?php echo $DIENTE_41; ?></td>
                </tr>
                <tr>
                    <td>Diente 42</td>
                    <td><?php echo $DIENTE_42; ?></td>
                </tr>
                <tr>
                    <td>Diente 43</td>
                    <td><?php echo $DIENTE_43; ?></td>
                </tr>
                <tr>
                    <td>Diente 44</td>
                    <td><?php echo $DIENTE_44; ?></td>
                </tr>
                <tr>
                    <td>Diente 45</td>
                    <td><?php echo $DIENTE_45; ?></td>
                </tr>
                <tr>
                    <td>Diente 46</td>
                    <td><?php echo $DIENTE_46; ?></td>
                </tr>
                <tr>
                    <td>Diente 47</td>
                    <td><?php echo $DIENTE_47; ?></td>
                </tr>
                <tr>
                    <td>Diente 48</td>
                    <td><?php echo $DIENTE_48; ?></td>
                </tr>
            </table>
        </div>
        <div class="col-6">
            <h6>Dientes</h6>
            <table class="table table-bordered table-sm">
                <tr>
                    <th class="table-primary">Diente</th>
                    <th class="table-primary">Valor</th>
                </tr>
                <tr>
                    <td>Diente 21</td>
                    <td><?php echo $DIENTE_2; ?></td>
                </tr>
                <tr>
                    <td>Diente 22</td>
                    <td><?php echo $DIENTE_22; ?></td>
                </tr>
                <tr>
                    <td>Diente 23</td>
                    <td><?php echo $DIENTE_23; ?></td>
                </tr>
                <tr>
                    <td>Diente 24</td>
                    <td><?php echo $DIENTE_24; ?></td>
                </tr>
                <tr>
                    <td>Diente 25</td>
                    <td><?php echo $DIENTE_25; ?></td>
                </tr>
                <tr>
                    <td>Diente 26</td>
                    <td><?php echo $DIENTE_26; ?></td>
                </tr>
                <tr>
                    <td>Diente 27</td>
                    <td><?php echo $DIENTE_27; ?></td>
                </tr>
                <tr>
                    <td>Diente 28</td>
                    <td><?php echo $DIENTE_28; ?></td>
                </tr>
                <tr>
                    <td>Diente 31</td>
                    <td><?php echo $DIENTE_31; ?></td>
                </tr>
                <tr>
                    <td>Diente 32</td>
                    <td><?php echo $DIENTE_32; ?></td>
                </tr>
                <tr>
                    <td>Diente 33</td>
                    <td><?php echo $DIENTE_33; ?></td>
                </tr>
                <tr>
                    <td>Diente 34</td>
                    <td><?php echo $DIENTE_34; ?></td>
                </tr>
                <tr>
                    <td>Diente 35</td>
                    <td><?php echo $DIENTE_35; ?></td>
                </tr>
                <tr>
                    <td>Diente 36</td>
                    <td><?php echo $DIENTE_36; ?></td>
                </tr>
                <tr>
                    <td>Diente 37</td>
                    <td><?php echo $DIENTE_37; ?></td>
                </tr>
                <tr>
                    <td>Diente 38</td>
                    <td><?php echo $DIENTE_38; ?></td>
                </tr>
            </table>
        </div>
        <div class="col-12">
            <hr>
        </div>
        <div class="col-6">
            <h6>Resultados de Estudios Realizados</h6>
            <table class="table table-bordered table-sm">
                <tr>
                    <th class="table-primary">Resultados</th>
                </tr>
                <tr>
                    <td><?php echo $RESULTADOS3; ?></td>
                </tr>
            </table>

            <h6>Diagnóstico</h6>
            <table class="table table-bordered table-sm">
                <tr>
                    <th class="table-primary">Observaciones</th>
                </tr>
                <tr>
                    <td><?php echo $OBSERVACIONES3; ?></td>
                </tr>
            </table>


        </div>
        <div class="col-6">
            <h6>Plan de Terapia</h6>
            <table class="table table-bordered table-sm">
                <tr>
                    <th class="table-primary">Observaciones</th>
                </tr>
                <tr>
                    <td><?php echo $OBSERVACIONES32; ?></td>
                </tr>
            </table>
            <h6>Resultado de Examen</h6>
            <table class="table table-bordered table-sm">
                <tr>
                    <th class="table-primary">Observaciones</th>
                </tr>
                <tr>
                    <td><?php echo $OBSERVACIONESRESEXAM1; ?></td>
                </tr>
            </table>
        </div>


        <div class="col-6">
            <hr>
            <center>
                <p>Dr. Mauricio Cerón Solana CP 5154089 / 7237048
                <p>
                    <hr>
                    <img src="../files/firmaDoc.png" class="img-fluid" width="70px" alt="">
                    <br>
                    <hr>
                <p class="footerTxt">Realizó</p>
            </center>
        </div>
        <div class="col-6">
            <hr>
            <center>
                <p>Firma del Trabajador
                <p>
                    <hr>
                    <img src="<?= $FIRMA  ?>" class="img-fluid" width="50px" alt="">
                    <br>
                    <hr>
                <p class="footerTxt">
                    Bajo protesta de decir verdad, certifico que los datos anteriormente proporcionados son verdaderos y estoy consciente que cualquier omisión o falsificación pueden ser motivo de despido, y autorizo al servicio médico de la compañía para que realice exámenes necesarios con motivo de mi trabajo para protección para detección oportuna y estudio de enfermedades o accidentes.
                </p>
            </center>
        </div>
    </div>


</body>
<script src="https://code.jquery.com/jquery-3.7.0.min.js" integrity="sha256-2Pmvv0kuTBOenSvLm6bvfBSSHrUJ+3A7x6P5Ebd07/g=" crossorigin="anonymous"></script>
<script>
    $(document).ready(function() {
        window.print();
    });
</script>

</html>