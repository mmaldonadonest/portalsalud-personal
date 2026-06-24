<?php
class app
{
    public $conn;
    public function __construct()
    {
        date_default_timezone_set('UTC');
        include_once('connect.php');
        $this->conn = conn();
    }
    public function showfiles($nss)
    {
        $newCon = $this->conn;
        $sql = "SELECT * FROM `files` where nss = '$nss'";
        $query = mysqli_query($newCon, $sql);
        $jsonArray = array();
        while ($arrayUsers = mysqli_fetch_array($query)) {
            $jsonArray[] = $arrayUsers;
        }
        return json_encode(array("response" => $jsonArray));
    }
    public function showfilesLb($nss)
    {
        $newCon = $this->conn;
        $sql = "SELECT * FROM `files` where nss = '$nss' AND type = 'laboratorio'";
        $query = mysqli_query($newCon, $sql);
        $jsonArray = array();
        while ($arrayUsers = mysqli_fetch_array($query)) {
            $jsonArray[] = $arrayUsers;
        }
        return json_encode(array("response" => $jsonArray));
    }
    public function showfilesExMedPdf($nss)
    {
        $newCon = $this->conn;
	$sql = "SELECT id,nss,name,date_upload, '' as 'url',type FROM `files` where nss = '$nss' AND type = 'examen_medico'";
//	echo $sql;
        $query = mysqli_query($newCon, $sql);
        $jsonArray = array();
        while ($arrayUsers = mysqli_fetch_array($query)) {
            $jsonArray[] = $arrayUsers;
        }
        return json_encode(array("response" => $jsonArray));
    }

    public function getFileExMedPdf($id)
    {
        $newCon = $this->conn;
        $sql = "SELECT * FROM `files` where id = '$id' AND type = 'examen_medico'";
        //echo $sql;
        $query = mysqli_query($newCon, $sql);
        $jsonArray = array();
        while ($arrayUsers = mysqli_fetch_array($query)) {
          $jsonArray[] = $arrayUsers;
        }
	return json_encode(array("response" => $jsonArray));

    }


    public function showfilesNotaMedPdf($nss)
    {
        $newCon = $this->conn;
        $sql = "SELECT * FROM `files` where nss = '$nss' AND type = 'nota_medica'";
        $query = mysqli_query($newCon, $sql);
        $jsonArray = array();
        while ($arrayUsers = mysqli_fetch_array($query)) {
            $jsonArray[] = $arrayUsers;
        }
        return json_encode(array("response" => $jsonArray));
    }
    public function showfilesNotaIncPdf($nss)
    {
        $newCon = $this->conn;
        $sql = "SELECT * FROM `files` where nss = '$nss' AND type = 'nota_incapacidad'";
        $query = mysqli_query($newCon, $sql);
        $jsonArray = array();
        while ($arrayUsers = mysqli_fetch_array($query)) {
            $jsonArray[] = $arrayUsers;
        }
        return json_encode(array("response" => $jsonArray));
    }
    public function showfilesExMedJSON($nss)
    {
        $newCon = $this->conn;
        $sql = "SELECT * FROM `files` where nss = '$nss' AND type = 'JSONCODE'";
        $query = mysqli_query($newCon, $sql);
        $jsonArray = array();
        while ($arrayUsers = mysqli_fetch_array($query)) {
            $jsonArray[] = $arrayUsers;
        }
        return json_encode(array("response" => $jsonArray));
    }
    public function extractFileJson($id)
    {
        $newCon = $this->conn;
        $sql = "SELECT url FROM `files` where id = '$id'";
        $query = mysqli_query($newCon, $sql);

        while ($arrayUsers = mysqli_fetch_array($query)) {
            return $arrayUsers[0];
        }
    }
    public function loginApiRest($userV, $pass)
    {
        $ch = curl_init();
        $headers  = [
            'x-api-key: XXXXXX',
            'Content-Type: application/json; charset=utf-8'
        ];
        $postData = [
            'id_usuario' => $userV,
            'id_password' => $pass,
            'id_app' => '13'
	];
//	echo var_dump(json_encode($postData));
        /* curl_setopt($ch, CURLOPT_URL, "http://10.249.249.3/biows/ords/security/login/registro_app"); */
        curl_setopt($ch, CURLOPT_URL, "http://10.249.249.3/biows/ords/security/login/registro_app");
        curl_setopt($ch, CURLOPT_POST, 1);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($postData));
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
        $result     = curl_exec($ch);
        $statusCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        return $result;
    }
    public function searchUser($datas)
    {
        $ch = curl_init();
        $headers  = [
            'x-api-key: XXXXXX',
            'Content-Type: application/json; charset=utf-8'
        ];
        $postData = [
            'Nss' => $datas,
            'Fecha_consulta' => date("d/m/y H:i:s"),
            'Usuario_consulta' => $_SESSION['nombre'],
            'Aplicacion_id' => 'Biometrico',
            'Id_aplicacion' => '4'

        ];
        /*  curl_setopt($ch, CURLOPT_URL, "http://10.249.249.3/biows/ords/security/Catalogo/usuario"); */
        curl_setopt($ch, CURLOPT_URL, "http://10.249.249.3/biows/ords/security/Catalogo/usuario");
        curl_setopt($ch, CURLOPT_POST, 1);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($postData));
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
        $result     = curl_exec($ch);
        $statusCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        return $result;
    }
    public function searchProspecto($datas)
    {
        $ch = curl_init();
        $headers  = [
            'x-api-key: XXXXXX',
            'Content-Type: application/json; charset=utf-8'
        ];
        $postData = [
            'curp' => $datas

        ];
        curl_setopt($ch, CURLOPT_URL, "http://10.249.249.3/biows/ords/security/Catalogo/Candidatos");
        curl_setopt($ch, CURLOPT_POST, 1);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($postData));
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
        $result     = curl_exec($ch);
        $statusCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        return $result;
    }
    public function addFileBD($nss, $namefile, $url, $type)
    {
        $newCon = $this->conn;
        $sql = "INSERT INTO `files` (`id`, `nss`, `name`, `date_upload`, `url`, `type`) VALUES (NULL, '$nss', '$namefile', NOW(), '$url', '$type');";
        $query = mysqli_query($newCon, $sql);
        return 1;
    }
    public function checkExp($nss)
    {
        $newCon = $this->conn;
        $sql = "SELECT * FROM `antecedentes` WHERE NSS = '$nss'";
        $query = mysqli_query($newCon, $sql);
        if ($f = mysqli_fetch_assoc($query)) {
            return 1;
        } else {
            $sql = "INSERT INTO `antecedentes` (`ID`, `NSS`, `HEREDOFAM_ID`, `ANTPERSNOPAT_ID`, `GINECO_OBS_ID`) VALUES (NULL, '$nss', NULL, NULL, NULL);SELECT LAST_INSERT_ID();";
            $query = mysqli_query($newCon, $sql);
            while ($array = mysqli_fetch_array($query)) {
                $idAntecedente = $array[0];
            }
        }
    }
    public function neurologiaDats($nss)
    {
        $newCon = $this->conn;
        $sql = "SELECT * FROM `antecedentes` WHERE NSS = '$nss'";
        $query = mysqli_query($newCon, $sql);
        if ($f = mysqli_fetch_assoc($query)) {
            echo $x = "1";
        } else {
            $sqlAnt = "INSERT INTO `antecedentes` (`ID`, `NSS`, `HEREDOFAM_ID`, `ANTPERSNOPAT_ID`, `GINECO_OBS_ID`) VALUES (NULL, '$nss', NULL, NULL, NULL)";
            $queryAnt = mysqli_query($newCon, $sqlAnt);
            $ant_id = mysqli_insert_id($newCon);
            $sqlHeredoFam = "INSERT INTO `heredofamiliar` (`ID`, `ANTID`, `NEUROLOGIA_ID`, `CARDIOPATIA_ID`, `NEUMOPATICA_ID`, `TOXICOLOGICO_ID`, `NEFROPATIAS_ID`, `ENDOCRINAS_ID`, `OBESIDAD_ID`, `MENTALES_ID`, `GENERALES_ID`, `OTRAS_ID`) VALUES (NULL, '$ant_id', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL)";
            $queryHeredo = mysqli_query($newCon, $sqlHeredoFam);
            $ant_id = mysqli_insert_id($newCon);
            $sqlNeurologia = "INSERT INTO `neurologia` (`ID`, `IDHEREDOFAM`, `AVC`, `EPILEPSIA`, `PARALISIS_FACIAL`, `PARKINSON`, `ALZHEIMER`, `MIGRAÑA`, `AVC_OBS`, `EPILEPSIA_OBS`, `PARALISIS_FACIAL_OBS`, `PARKINSON_OBS`, `ALZHEIMER_OBS`, `MIGRAÑA_OBS`) VALUES (NULL, '$ant_id', '1', '1', '1', '1', '1', '1', 'hghgh', 'jkjj', 'jjjk', 'ggh', 'hhj', 'hhj');";
            $queryNeurologia = mysqli_query($newCon, $sqlNeurologia);
            echo "0";
        }
    }
    public function sendHeredoFamDats($nss, $type, $data = [])
    {
        extract($data);
        $ch = curl_init();
        $headers  = [
            'x-api-key: XXXXXX',
            'Content-Type: application/json; charset=utf-8'
        ];
        switch ($type) {
            case 'CAMBIO':
                $postData = [
                    'NNS' => $nss,
                    'FECHA' => date("d/m/y H:i:s"),
                    'TIPO_REGISTROS' => 'CAMBIO',
                    'USUARIO_CAMBIO' => $_SESSION['nombre'],
                    'CARDIOPATIA' => [
                        'CARDIOPATIA_ISQUEMICA' => $cardIsq,
                        'HIPERTENSION' => $hiper,
                        'HIPOTENSION' => $hipotension,
                        'INSUFICIENCIA_CARDIACA' => $insuCard,
                        'INSUFICIENCIA_VASCULAR_PERIFERICA' => $insuVasPer,
                        'CARDIOPATIA_ISQUEMICA_OBS' => $cardIsqOBS,
                        'HIPERTENSION_OBS' => $hiperOBS,
                        'HIPOTENSION_OBS' => $hipotensionObs,
                        'INSUFICIENCIA_CARDIACA_OBS' => $insuCardObs,
                        'INSUFICIENCIA_VASCULAR_PERIFERICA_OBS' => $insuVasPerObs
                    ],
                    'ENDOCRINA' => [
                        'ID' => $nss,
                        'DM' => $dm,
                        'HIPERTIROIDISMO' => $hipertirod,
                        'HIPOTIROIDISMO' => $hipotiro,
                        'DM_OBS' => $dmOBS,
                        'HIPERTIROIDISMO_OBS' => $hipertirodOBS,
                        'HIPOTIROIDISMO_OBS' => $hipotiroObs
                    ],
                    'GENERALES' => [
                        'CANCER' => $cancer,
                        'ALERGIAS' => $alergias,
                        'MALFORMACIONES' => $malforma,
                        'CANCER_OBS' => $cancerOBS,
                        'ALERGIAS_OBS' => $alergiasOBS,
                        'MALFORMACIONES_OBS' => $malformaObs
                    ],
                    'MENTALES' => [
                        'NEUROSIS' => $neurosis,
                        'PSICOSIS' => $psicosis,
                        'INTENTO_DE_SUICIDIO' => $intentoSuicidio,
                        'ESQUIZOFRENIA' => $esquizo,
                        'NEUROSIS_OBS' => $neurosisOBS,
                        'PSICOSIS_OBS' => $psicosisOBS,
                        'INTENTO_DE_SUICIDIO_OBS' => $intentoSuicidioObs,
                        'ESQUIZOFRENIA_OBS' => $esquizoObs
                    ],
                    'NEFROPATIA' => [
                        'IRC' => $irc,
                        'LITASIS' => $litiasis,
                        'IRC_OBS' => $ircOBS,
                        'LITASIS_OBS' => $litiasisOBS
                    ],
                    'NEUMOPATICA' => [
                        'BRONQUITIS' => $bronqui,
                        'TUBERCULOSIS' => $tuber,
                        'ASMA' => $asma,
                        'INSUFICIENCIA_RESPIRATORIA' => $insuResp,
                        'BRONQUITIS_OBS' => $bronquiOBS,
                        'TUBERCULOSIS_OBS' => $tuberOBS,
                        'ASMA_OBS' => $asmaObs,
                        'INSUFICIENCIA_RESPIRATORIA_OBS' => $insuRespObs
                    ],
                    'NEUROLOGIA' => [
                        'AVC' => $avc,
                        'EPILEPSIA' => $emilepcheck,
                        'PARALISIS_FACIAL' => $paralisisFac,
                        'PARKINSON' => $parkinson,
                        'ALZHEIMER' => $alzheimer,
                        'MIGRANA' => $migraña,
                        'EPILEPSIA_OBS' => $epilepCom,
                        'PARALISIS_FACIAL_OBS' => $paralisisFacObs,
                        'PARKINSON_OBS' => $parkinsonObs,
                        'ALZHEIMER_OBS' => $alzheimerObs,
                        'MIGRANA_OBS' => $migrañaObs
                    ],
                    'OBESIDAD' => [
                        'TRANSTORNO_DE_CRECIMIENTO' => $transtornoCreci,
                        'TRANSTORNO_DE_CRECIMIENTO_OBS' => $transtornoCreciOBS
                    ],
                    'OTRAS' => [
                        'OTRAS' => $otras,
                        'OTRAS_OBS' => $otrasOBS
                    ],
                    'TOXICOLOGIOCAS' => [
                        'ALCOHOLISMO' => $alcoholismo,
                        'TABAQUISMO' => $tabaquismo,
                        'DROGAS' => $drogas,
                        'ALCOHOLISMO_OBS' => $alcoholismoOBS,
                        'TABAQUISMO_OBS' => $tabaquismoOBS,
                        'DROGAS_OBS' => $drogasObs
                    ],
                    'SERV_MED_ANT_PATOLOGICOS' => [
                        'HABITACION' => $habitaInp,
                        'NO_DORMITORIOS' => $NoDorm,
                        'NO_HABITANTES' => $NoHabita,
                        'PISO' => $pisoInp,
                        'TECHO' => $techoInp,
                        'AGUA_POTABLE' => $AguaPotable,
                        'LUZ' => $LuzInput,
                        'DRENAJE' => $Drenajein,
                        'CONVIVENCIA_CON_ANIMALES' => $CCAIn,
                        'AIRE_ACONDICIONADO' => $AAconIn,
                        'RELIGION' => $ReligionIn,
                        'HIGIENE_PERSONAL' => $HPIn,
                        'BANO' => $BañoIn,
                        'CAMBIO_DE_ROPA' => $CrInp,
                        'ASEO_BUCAL' => $ABInp,
                        'ALIMENTACION' => $AlimInp,
                        'NO_COMIDAS_AL_DIA' => $NCDInp,
                        'BALANCEADA' => $BalIn,
                        'SUFICIENTE' => $SufIn,
                        'DEPORTE' => $DepIn,
                        'FC1' => $FCInp1,
                        'HOBBIES_PASATIEMPOS' => $HOPInp,
                        'FC2' => $FCInp2
                    ],
                    'SERV_MED_INMUNIZACIONES' => [
                        'BLG' => $bcgs,
                        'SABIN' => $sabin,
                        'DPT' => $dptin,
                        'HEPATITIS_B' => $hepatitis,
                        'SR' => $srinp,
                        'INFLUENZA' => $influenza,
                        'TD' => $tdinp,
                        'view' => $view,
                        "COVID" => $covidInp,
                        "FECHA" => $fechaInm
                    ],
                    'SERV_MED_ANT_GIN_OBSTETRICOS' => [
                        'MENARCA' => $MenarcaInp,
                        'RITMO' => $RitmoInp,
                        'IVSA' => $IvsaInp,
                        'FUM' => $FumInp,
                        'G' => $GInp,
                        'A' => $AInp,
                        'P' => $PInp,
                        'C' => $CInp,
                        'MET_PLANIFICACION' => $MdPFInp,
                        'PAPANICOLAOU' => $PapanInp,
                        'INFECC_REP' => $InReInp,
                        'FUP' => $FupInp,
                    ],
                    "SERV_MED_ANT_PER_PATOLOGICOS" => [
                        'ENF_CONGENITAS' => $ENFcONGiNP,
                        "GENITAL" => $genitals,
                        "ENF_PROP_INF" => $enfPropInf,
                        "DERMATOPATIAS" => $dermopatics,
                        "SIST_AUDITIVO" => $sisTAudt,
                        "RESPIRATORIO" => $respirInp,
                        "SIS_OLFATIVO" => $SisOlfas,
                        "TRAUMATICOS" => $TraumasInp
                    ],

                    "SERV_MED_OFTALMOLOGICO" => [
                        "USO_LENTES" => $ofta,
                        "USO_LENTES_OBS" => $oftaOBS,
                        "ASTIGMATISMO" => $astigma,
                        "ASTIGMATISMO_OBS" => $astigmaOBS,
                        "PRESBICIA" => $prebisia,
                        "PRESBICIA_OBS" => $prebisiaOBS,
                        "ESTRABISMO" => $estra,
                        "ESTRABISMO_OBS" => $estraOBS,
                        "DALTONISMO" => $dalto,
                        "DALTONISMO_OBS" => $daltoOBS,
                        "DESPENDIMIENTO_RET" => $desp,
                        "DESPENDIMIENTO_RET_OBS" => $despOBS,
                        "MIOPIA" => $miope,
                        "MIOPIA_OBS" => $miopeOBS
                    ],

                    "SERV_MED_DIGESTIVO" => [
                        "HERNIAS" => $herns,
                        "HERNIAS_OBS" => $hernsOBS,
                        "CIRROSIS" => $cirrosiss,
                        "CIRROSIS_OBS" => $cirrosissOBS,
                        "COLITIS" => $colis,
                        "COLITIS_OBS" => $colisOBS,
                        "GASTRITIS" => $gast,
                        "GASTRITIS_OBS" => $gastOBS,
                        "HEMORROIDES" => $hemos,
                        "HEMORROIDES_OBS" => $hemosOBS,
                        "CIRUGIAS" => $ciru,
                        "CIRUGIAS _OBS" => $ciruOBS
                    ],

                    "SERV_MED_RENAL" => [
                        "INCONTINENSIA" => $inconti,
                        "INCONTINENSIA_OBS" => $incontiOBS,
                        "INSUFICIENCIA" => $insufi,
                        "INSUFICIENCIA_OBS" => $insufiOBS,
                        "LITIASIS" => $litia,
                        "LITIASIS_OBS" => $litiaOBS
                    ],

                    "SERV_MED_SIST_NERVIOSO" => [
                        "ANEURISMA" => $aneuri,
                        "ANEURISMA_OBS" => $aneuriOBS,
                        "ENF_DEG_MUS_NEU" => $enfdegneu,
                        "ENF_DEG_MUS_NEU_OBS" => $enfdegneuOBS,
                        "ACC_CER_VASCU" => $acccervascu,
                        "ACC_CER_VASCU_OBS" => $acccervascuOBS,
                        "EPILEPSIA" => $epilep,
                        "EPILEPSIA_OBS" => $epilepOBS,
                        "PAR_FACIAL" => $parfacil,
                        "PAR_FACIAL_OBS" => $parfacilOBS
                    ],

                    "SERV_MED_MUS_ESQUELETICO" => [
                        "ESCOLIOSIS" => $escoliosi,
                        "ESCOLIOSIS_OBS" => $escoliosiOBS,
                        "XIFOSIS" => $xifosis,
                        "XIFOSIS_OBS" => $xifosisOBS,
                        "LORDOSISI" => $lordosis,
                        "LORDOSISI_OBS" => $lordosisOBS,
                        "LUMBALGIA" => $lumbagia,
                        "LUMBALGIA_OBS" => $lumbagiaOBS,
                        "BASCULAMIENTO" => $basculamiento,
                        "BASCULAMIENTO_OBS" => $basculamientoOBS,
                        "ALTERACION_DISCAL" => $altdiscal,
                        "ALTERACION_DISCAL_OBS" => $altdiscalOBS
                    ],

                    "SERV_MED_CARDIO_NO_PATO" => [
                        "ANEURISMA" => $aneurism2,
                        "ANEURISMA_OBS" => $aneurism2OBS,
                        "HIPERTENSION" => $himertensss,
                        "HIPERTENSION_OBS" => $himertensssOBS,
                        "HIPOTENSION" => $hipotensi,
                        "HIPOTENSION_OBS" => $hipotensiOBS,
                        "INFARTOS " => $infs,
                        "INFARTOS_OBS" => $infsOBS,
                        "INSUFIC_CAR" => $insfcards,
                        "INSUFIC_CAR_OBS" => $insfcardsOBS
                    ],

                    "SERV_MED_TOXI_NO_PATO" => [
                        "TABAQUISMO" => $tabaqqq,
                        "TABAQUISMO_OBS" => $tabaqqqOBS,
                        "ALCOHOLISMO" => $alcohool,
                        "ALCOHOLISMO_OBS" => $alcohoolOBS,
                        "DROGAS" => $grugss,
                        "DROGAS_OBS" => $grugssOBS,
                        "ALT_CARGA" => $altcargs,
                        "ALT_CARGA_OBS" => $altcargsOBS,
                        "PERFORACIONES" => $perfoss,
                        "PERFORACIONES_OBS" => $perfossOBS,
                        "TATUAJES" => $tattoo,
                        "TATUAJES_OBS" => $tattooOBS,
                        "ALERGIAS" => $alergiasInp,
                        "ALERGIAS_OBS" => $alergiasInOBS,

                    ],

                    "SERV_MED_ENDOCRI_NO_PATO" => [
                        "DIABETES" => $diabolica,
                        "DIABETES_OBS" => $diabolicaOBS,
                        "HIPERTIROIDISMO" => $hipertirones,
                        "HIPERTIROIDISMO_OBS" => $hipertironesOBS,
                        "HIPOTIROIDISMO" => $superhipo,
                        "HIPOTIROIDISMO_OBS" => $superhipoOBS
                    ],
                    "SERV_MED_PADECI_ACTUAL" => [
                        "OBSERVACIONES" => $padecimientoActual
                    ],
                    "SERV_MED_INT_APARATO_SIST" => [
                        "OBSERVACIONES" => $interAS
                    ],
                    "SERV_MED_EXPLORACION_FISICA" => [
                        "PESO" => $PESOEXFISINP,
                        "TALLA" => $TALLAEXFISINP,
                        "IMC" => $IMCEXFISINP,
                        "FC" => $FCEXFISINP,
                        "FR" => $FREXFISINP,
                        "TA" => $TAEXFISINP,
                        "TEMP" => $TEMPEXFISINP,
                        "ECTOMORFICO" => $ECTOMORFICOEXFISINP,
                        "MESOMORFO" => $MESOMORFOEXFISINP,
                        "ENDOMORFICA" => $ENDOMORFICAEXFISINP,
                        "SATISFACCION" => $SATISFACCIONEXFISINP,
                        "COMP_TRABAJO" => $COMP_TRABAJOEXFISINP,
                        "COMP_ORGANIZACION" => $COMP_ORGANIZACIONEXFISINP,
                        "TIEMPO" => $TIEMPOEXFISINP,
                        "ESPACIO" => $ESPACIOEXFISINP,
                        "PERSONA" => $PERSONAEXFISINP,
                        "SERENIDAD" => $SERENIDADEXFISINP,
                        "INTERES" => $INTERESEXFISINP,
                        "ANTAGONICO" => $ANTAGONICOEXFISINP,
                        "ENTUSIASTA" => $ENTUSIASTAEXFISINP,
                        "ENOJO" => $ENOJOEXFISINP,
                        "HIPOCRITA" => $HIPOCRITAEXFISINP,
                        "ICTERICA" => $ICTERICAEXFISINP,
                        "ANEMICA" => $ANEMICAEXFISINP,
                        "TIROIDEA" => $TIROIDEAEXFISINP,
                        "NOLMAL_FASCIES" => $NOLMAL_FASCIESEXFISINP,
                        "PARKINSON" => $PARKINSONEXFISINP,
                        "HEMIPLEJICO" => $HEMIPLEJICOEXFISINP,
                        "ATAXICA" => $ATAXICAEXFISINP,
                        "HEMIPARESIA" => $HEMIPARESIAEXFISINP,
                        "NORMAL_MARCHA" => $NORMAL_MARCHAEXFISINP,
                        "LENGUAJE" => $LENGUAJEEXFISINP,
                        "CONVER_FLUIDA" => $CONVER_FLUIDAEXFISINP,
                        "COHERENCIA_PALABLAS" => $COHERENCIA_PALABLASEXFISINP
                    ],


                    "SERV_MED_CRANEO" => [
                        "OJOS" => $OJOSEXFISINP,
                        "PUPILAS" => $PUPILASEXFISINP,
                        "CONJUNTIVAS" => $CONJUNTIVASEXFISINP,
                        "REFLEJOS" => $REFLEJOSEXFISINP,
                        "FONDO_OJO" => $FONDO_OJOEXFISINP
                    ],

                    "SERV_MED_AGUDEZA_VISUAL" => [
                        "OD" => $ODEXFISINP,
                        "OIZ" => $OIZEXFISINP,
                        "COLORES" => $COLORESEXFISINP
                    ],

                    "SERV_MED_NARIZ" => [
                        "CAVIDAD_NASAL" => $CAVIDAD_NASALEXFISINP,
                        "MUCOSA" => $MUCOSAEXFISINP,
                        "TABIQUE_NASAL" => $TABIQUE_NASALEXFISINP,
                        "OLFATO" => $OLFATOEXFISINP
                    ],


                    "SERV_MED_COLUMNA_VERTEBRAL" => [
                        "CERVICAL" => $CERVICALEXFISINP,
                        "DORSAL" => $DORSALEXFISINP,
                        "LUMBAR" => $LUMBAREXFISINP
                    ],

                    "SERV_MED_BOCA" => [
                        "LESIONES" => $LESIONESEXFISINP,
                        "ENCIAS" => $ENCIASEXFISINP,
                        "OROIFARINGE" => $OROIFARINGEEXFISINP
                    ],

                    "SERV_MED_OIDOS" => [
                        "CAE" => $CAEEXFISINP,
                        "MEM_TIM" => $MEM_TIMEXCAEEXFISINP,
                        "AGUD_AUDIT" => $AGUD_AUDITEXCAEEXFISINP,
                        "OD" => $ODEXCAEEXFISINP,
                        "OI" => $OIEXCAEEXFISINP
                    ],

                    "SERV_MED_TORAX" => [
                        "RUIDOS_CARDIACOS" => $RUIDOS_CARDIACOSEXFISINP,
                        "REG_PRECORDIAL" => $REG_PRECORDIALEXFISINP,
                        "CAMPOS_PULM" => $CAMPOS_PULMEXFISINP
                    ],

                    "SERV_MED_COLUMNA_VERTEBRAL_AUX" => [
                        "DEFORMACIONES" => $DEFORMACIONESEXFISINP,
                        "DOLOR" => $DOLOREXFISINP,
                        "MOVIMIENTOS" => $MOVIMIENTOSEXFISINP,
                        "MARCHA" => $MARCHAEXFISINP,
                        "LASSEGUE" => $LASSEGUEEXFISINP,
                        "PUNTA_TALON" => $PUNTA_TALONEXFISINP
                    ],
                    "SERV_MED_CUELLO" => [
                        "DEFORMACIONES" => 1,
                        "DEFORMACIONES_OBS" => $DEFORMACIONES_OBSINP,
                        "TIROIDES" => 1,
                        "TIROIDES_OBS" => $TIROIDES_OBSINP,
                        "TRAQUEA" => 1,
                        "TRAQUEA_OBS" => $TRAQUEA_OBSINP,

                    ],

                    "SERV_MED_ABDOMEN" => [
                        "FORMA" => $FORMAEXFISINP,
                        "VISCEROMEGALIAS" => $VISCEROMEGALIASEXFISINP,
                        "HERNIAS" => $HERNIASEXFISINP,
                        "DOLOR" => $DOLOREXFISINP,
                        "PERISTALSIS" => $PERISTALSISEXFISINP
                    ],

                    "SERV_MED_GENITALES" => [
                        "MASCULINO" => $MASCULINOEXFISINP,
                        "FEMENINO" => $FEMENINOEXFISINP
                    ],

                    "SERV_MED_URINARIO" => [
                        "PUNTOS_URETRALES" => $PUNTOS_URETRALESEXFISINP,
                        "FOSAS_RENALES" => $FOSAS_RENALESEXFISINP
                    ],

                    "SERV_MED_EXTREMIDADES" => [
                        "SUPERIOR" => $SUPERIOREXFISINP,
                        "INFERIOR" => $INFERIOREXFISINP,
                        "IVP" => $IVPEXFISINP,
                        "EDEMA" => $EDEMAEXFISINP
                    ],

                    "SERV_MED_PIEL" => [
                        "LUNARES" => $LUNARESEXFISINP,
                        "PIGMENTACION" => $PIGMENTACIONEXFISINP,
                        "CICATRICES_QUIRUR" => $CICATRICES_QUIRUREXFISINP
                    ],
                    "SERV_MED_DIENTES" => [
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
                        "DIENTE_38" => $DIENTE_38
                    ],
                    "SERV_MED_ESTUDIOS_REALIZADOS" => [
                        "RESULTADOS" => $RESULTADOSESYRES
                    ],

                    "SERV_MED_DIAGNOSTICO" => [
                        "OBSERVACIONES" => $OBSERVACIONESDIAGINP
                    ],

                    "SERV_MED_PLAN_TERAPIA" => [
                        "OBSERVACIONES" => $OBSERVACIONESPLATERINP
                    ],
                    "SERV_MED_RESULTADO_EXAMEN" => [
                        "OBSERVACIONES" => $resExamIn,
                        "APTO" => "",
                        "NO_APTO" => "",
                        "APTO_CONDICIONADO" => "",
                        "APTO_RESTRINGIDO" => "",
                        "FIRMA_DIGITAL" => $FIRMA,

                    ],
                    "SERV_ANTECEDENTESLAB" => [
                        "edad_inicio_laboral" => 20,
                        "cantidad_trabajos" => 3,
                        "pension" => "true",
                        "trabajos" => array(
                            [
                                "nombre" => "Trabajo 1",
                                "giro" => "Giro 1",
                                "puesto" => "Puesto 1",
                                "turno" => "Turno 1",
                                "antiguedad" => "5 años",
                                "salida" => "2022",
                                "descripcion" => "Descripción del trabajo 1",
                                "riesgos" => "Alto",
                                "epp" => "Sí",
                                "observaciones" => "Observaciones del trabajo 1000"
                            ],
                            [
                                "nombre" => "Trabajo 2",
                                "giro" => "Giro 2",
                                "puesto" => "Puesto 2",
                                "turno" => "Turno 2",
                                "antiguedad" => "3 años",
                                "salida" => "2020",
                                "descripcion" => "Descripción del trabajo 2",
                                "riesgos" => "Medio",
                                "epp" => "Sí",
                                "observaciones" => "Observaciones del trabajo 2"
                            ],
                            [
                                "nombre" => "Trabajo 3",
                                "giro" => "Giro 3",
                                "puesto" => "Puesto 3",
                                "turno" => "Turno 3",
                                "antiguedad" => "8 años",
                                "salida" => "2019",
                                "descripcion" => "Descripción del trabajo 3",
                                "riesgos" => "Bajo",
                                "epp" => "No",
                                "observaciones" => "Observaciones del trabajo 3"
                            ],
                            [
                                "nombre" => "Trabajo 4",
                                "giro" => "Giro 4",
                                "puesto" => "Puesto 4",
                                "turno" => "Turno 4",
                                "antiguedad" => "2 años",
                                "salida" => "2023",
                                "descripcion" => "Descripción del trabajo 4",
                                "riesgos" => "Alto",
                                "epp" => "Sí",
                                "observaciones" => "Observaciones del trabajo 4"
                            ]
                        )


                    ]


                ];
                break;
            case 'ALTA':
                $postData = [
                    'NNS' => $nss,
                    'FECHA' => date("d/m/y H:i:s"),
                    'TIPO_REGISTROS' => 'ALTA',
                    'TIPO_EMPLEADO' => $typeuser,
                    'USUARIO_CAMBIO' => $_SESSION['nombre'],
                    'OTRAS' => [
                        'OTRAS' => '',
                        'OTRAS_OBS' => ''
                    ]


                ];
                break;
            default:
                # code...
                break;
        }


        curl_setopt($ch, CURLOPT_URL, "http://10.249.249.3/biows/ords/security/Servcio/Medico");
        curl_setopt($ch, CURLOPT_POST, 1);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($postData));
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
        $result     = curl_exec($ch);
        $statusCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        $data = json_decode($result);
        foreach ($data->Datos as $key) {
            echo  $check = $key->Proceso;
        }
    }
    public function CheckEmploye($nss, $user)
    {
        $objet = new app;
        $ch = curl_init();
        $headers  = [
            'x-api-key: XXXXXX',
            'Content-Type: application/json; charset=utf-8'
        ];
        $postData = [
            'NNS' => $nss,
            'FECHA' => date("d/m/y H:i:s"),
            'USUARIO' => $_SESSION['nombre']
        ];
        curl_setopt($ch, CURLOPT_URL, "http://10.249.249.3/biows/ords/security/Servcio/consulta_examen");
        curl_setopt($ch, CURLOPT_POST, 1);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($postData));
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
        $result     = curl_exec($ch);
        $statusCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        $data = json_decode($result);
        foreach ($data->Datos as $key) {
            $check = $key->Estado;
        }
        if ($check == 0) {
        } else {
            $objet->sendHeredoFamDats($nss, 'ALTA', $data = ["typeuser" => $user]);
        }
    }
    public function getDatsNss($nss)
    {
        $ch = curl_init();
        $headers  = [
            'x-api-key: XXXXXX',
	    'Content-Type: application/json; charset=utf-8',
	    'Connection: Keep-Alive',
	    'Accept-Encoding: gzip,deflate' 
        ];
        $postData = [
            'NNS' => $nss,
            'FECHA' => date("d/m/y H:i:s"),
            'USUARIO' => $_SESSION['nombre']
	];
	//echo json_encode($postData) ;
        curl_setopt($ch, CURLOPT_URL, "http://10.249.249.3/biows/ords/security/Servcio/consulta_examen");
        curl_setopt($ch, CURLOPT_POST, 1);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($postData));
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
        return $result     = curl_exec($ch);
        $statusCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    }
    public function addConsultM($data = [])
    {
        extract($data);
        $ch = curl_init();
        $headers  = [
            'x-api-key: XXXXXX',
            'Content-Type: application/json; charset=utf-8'
        ];
        $postData = [
            "FECHA" => date("d/m/y H:i:s"),
            "NSS" => $idNss,
            "TIPO_CONSULTA" => $TipoConsultaInput,
            "AREA_ACCIDENTE" => $AreaAccidenteInput,
            "AREA_ANATOMICA" => $AreaAnatomicaInput,
            "CAUSA" => $CausaInput,
            "PESO" => $PesoInput,
            "TALLA" => $TallaInput,
            "IMC" => $IMCINPUT,
            "FC" => $FCINPUT,
            "FR" => $FRINPUT,
            "TA" => $TAINPUT,
            "TEMPERATURA" => $TemperaturaInput,
            "MOTIVO_CONSULTA" => $MOTIVOCONSULTAINP,
            "EXPLORACION_FISICA" => $EXPLOFISIINP,
            "DIAGNOSTICO" => $DIAGNOSTICOINPUT,
            "TRATAMIENTO" => $TRATAMIENTOINPUT,
            "CONSULTA_RELACIONADA" => $idArchivoRel,
            "ID_ARCHIVOS" => $idArchivoRel,
            "USUARIO" => "747849849",
            "NOMBRE_USUARIO" => $_SESSION['nombre'],
            "FIRMA_DIGITAL" => $FIRMA,

        ];
        curl_setopt($ch, CURLOPT_URL, "http://10.249.249.3/biows/ords/security/Servcio/consulta");
        curl_setopt($ch, CURLOPT_POST, 1);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($postData));
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
        $result     = curl_exec($ch);
        $statusCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        $data = json_decode($result);
        foreach ($data->Datos as $key) {
            echo  $check = $key->Proceso;
        }
    }
    public function addIncapacidad($data = [])
    {
        extract($data);
        $ch = curl_init();
        $headers  = [
            'x-api-key: XXXXXX',
            'Content-Type: application/json; charset=utf-8'
        ];
        $postData = [
            "FECHA" => date("d/m/y H:i:s"),
            "NSS" => $idNss,
            "FOLIO_INCAPACIDAD" => $foilioInc,
            "RAMO" => $ramoInp,
            "TIPO_INCAPACIDAD" => $tipoIncInput,
            "FECHA_INICIO" => $fechainiInp,
            "FECHA_TERMINO" => $fechaTermInp,
            "DIAS_AUTORIZADOS" => $DiasAutInp,
            "SALARIO_INTEGRADO" => $SalIntegrInp,
            "COSTO" => $CostoInp,
            "IMPUTABLE" => $InpuInput,
            "ESTADO_DICTAMEN" => $EstDictInput,
            "GOCE_SUEDO" => $GoceSueldoInp,
            "COMPLEMENTO_SALARIAL" => $ComplSalaInp,
            "URL_ARCHIVOS" => $idArchivoRel,
            "USUARIO" => "747849849",
            "NOMBRE_USUARIO" => $_SESSION['nombre'],
            "FIRMA_DIGITAL" => $FIRMA,
            "RUBRO" => "$rubroInput",
            "FECHA_ALTA" => "$fechaAltaInp",
            "FECHA_PRIMERA_INCAPACIDAD" => "$fechainiInpINI",
            "ST2" => "$st2Input",
            "SALARIO_ACUMULADO" => "$salarioAcomuIn",
            "ALTA" => "$altaInput"

        ];
        curl_setopt($ch, CURLOPT_URL, "http://10.249.249.3/biows/ords/security/Servcio/incapacidades");
        curl_setopt($ch, CURLOPT_POST, 1);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($postData));
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
        $result     = curl_exec($ch);
        $statusCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        $data = json_decode($result);
        foreach ($data->Datos as $key) {
            echo  $check = $key->Proceso;
        }
    }
    public function showConsults($nss)
    {


        $ch = curl_init();
        $headers  = [
            'x-api-key: XXXXXX',
            'Content-Type: application/json; charset=utf-8'
        ];
        $postData = [
            "NSS" => $nss,


        ];
        curl_setopt($ch, CURLOPT_URL, "http://10.249.249.3/biows/ords/security/Servcio/conuslta_medica_usuario");
        curl_setopt($ch, CURLOPT_POST, 1);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($postData));
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
        $result     = curl_exec($ch);
        $statusCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        return $result;
    }
    public function showIncapacidades($nss)
    {


        $ch = curl_init();
        $headers  = [
            'x-api-key: XXXXXX',
            'Content-Type: application/json; charset=utf-8'
        ];
        $postData = [
            "NSS" => $nss,


        ];
        curl_setopt($ch, CURLOPT_URL, "http://10.249.249.3/biows/ords/security/Servcio/consulta_incapacidad");
        curl_setopt($ch, CURLOPT_POST, 1);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($postData));
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
        $result     = curl_exec($ch);
        $statusCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        return $result;
    }
    public function showFilesConsult($id)
    {
        $newCon = $this->conn;
        $sql = "SELECT * FROM `files` where type = '$id'";
        return $query = mysqli_query($newCon, $sql);
    }
    public function showICD($data)
    {


        $ch = curl_init();
        $headers  = [
            'x-api-key: XXXXXX',
            'Content-Type: application/json; charset=utf-8'
        ];
        $postData = [
            "variable" => $data,


        ];
        curl_setopt($ch, CURLOPT_URL, "http://10.249.249.3/biows/ords/security/Servcio/indice");
        curl_setopt($ch, CURLOPT_POST, 1);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($postData));
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
        $result     = curl_exec($ch);
        $statusCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        return $result;
    }
}
class another extends app
{
    public function insertDatsExpFis($data = [])
    {
        extract($data);
        $newCon = $this->conn;
        $sqlD = "DELETE FROM `tags` WHERE nss = '$nss' AND type = '$name' ";
        $queryD = mysqli_query($newCon, $sqlD);
        $sql = "INSERT INTO `tags` (`id`, `nss`, `type`, `content`) VALUES (NULL, '$nss', '$name', '$content');";
        $query = mysqli_query($newCon, $sql);
        if (!$query) {
            echo "Error en la consulta: " . mysqli_error($newCon);
        }
    }
    public function selectDatsExMed($data = [])
    {
        extract($data);
        $newCon = $this->conn;
        $sql = "SELECT * FROM `tags` WHERE nss = '$nss' AND type = '$type' ORDER BY id DESC LIMIT 1";
        $query = mysqli_query($newCon, $sql);
        while ($array = mysqli_fetch_array($query)) {
            return $array[3];
        }
        if (!$query) {
            echo "Error en la consulta: " . mysqli_error($newCon);
        }
    }
    public function eraseDock($id)
    {
        $newCon = $this->conn;
        $sql = "DELETE FROM `files` WHERE `files`.`id` = '$id'";
        $query = mysqli_query($newCon, $sql);
        if (!$query) {
            echo "Error en la consulta: " . mysqli_error($newCon);
        }
    }
    public function showPretest($nss)
    {

        $ch = curl_init();
        $headers  = [
            'x-api-key: XXXXXX',
            'Content-Type: application/json; charset=utf-8'
        ];
        $postData = [
            "NSS" => $nss,


        ];
        curl_setopt($ch, CURLOPT_URL, "http://10.249.249.3/biows/ords/security/Servcio/consulta_cuestionario");
        curl_setopt($ch, CURLOPT_POST, 1);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($postData));
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
        $result     = curl_exec($ch);
        $statusCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        return $result;
    }
    public function insertDatsPretest($data = [])
    {

        extract($data);
        $newCon = $this->conn;
        $sqlD = "DELETE FROM `tags` WHERE nss = '$nss' AND type = '$name' ";
        $queryD = mysqli_query($newCon, $sqlD);
        $sql = "INSERT INTO `tags` (`id`, `nss`, `type`, `content`) VALUES (NULL, '$nss', '$name', '$content');";
        $query = mysqli_query($newCon, $sql);
        if (!$query) {
            echo "Error en la consulta: " . mysqli_error($newCon);
        }
    }
    public function traerDatosGeneralIncap($fecha_inicial,$fecha_final)
    {
        $ch = curl_init();
        $headers  = [
            'x-api-key: XXXXXX',
            'Content-Type: application/json; charset=utf-8'
        ];
        $postData = [
            "fecha_inicial" => $fecha_inicial,
            "fecha_final" => $fecha_final,


        ];
        curl_setopt($ch, CURLOPT_URL, "http://10.249.249.3/biows/ords/security/Servcio/consulta_incapacidades_fecha");
        curl_setopt($ch, CURLOPT_POST, 1);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($postData));
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
        $result     = curl_exec($ch);
        $statusCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        return $result;
    }
}
