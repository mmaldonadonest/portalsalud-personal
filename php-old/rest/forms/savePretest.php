<?php
require_once('../../app/app.php');
$view = new app();
$others = new another();
$nss = (isset($_POST['nss']) ? $_POST['nss'] : null);
$cuenta = (isset($_POST['cuenta']) ? $_POST['cuenta'] : null);
$puesto = (isset($_POST['puesto']) ? $_POST['puesto'] : null);
$agencia = (isset($_POST['agencia']) ? $_POST['agencia'] : null);
$ape = (isset($_POST['ape']) ? $_POST['ape'] : null);
$apm = (isset($_POST['apm']) ? $_POST['apm'] : null);
$nom = (isset($_POST['nom']) ? $_POST['nom'] : null);
$fechN = (isset($_POST['fechN']) ? $_POST['fechN'] : null);
$ecivil = (isset($_POST['ecivil']) ? $_POST['ecivil'] : null);
$num_imss = (isset($_POST['num_imss']) ? $_POST['num_imss'] : null);
$tel_personal = (isset($_POST['tel_personal']) ? $_POST['tel_personal'] : null);
$tel_casa = (isset($_POST['tel_casa']) ? $_POST['tel_casa'] : null);
$contacto_emergencia = (isset($_POST['contacto_emergencia']) ? $_POST['contacto_emergencia'] : null);
$LHOP = (isset($_POST['LHOP']) ? $_POST['LHOP'] : null);
$TALMED = (isset($_POST['TALMED']) ? $_POST['TALMED'] : null);
$SUAA = (isset($_POST['SUAA']) ? $_POST['SUAA'] : null);
$SAFOPH = (isset($_POST['SAFOPH']) ? $_POST['SAFOPH'] : null);
$ESEMB = (isset($_POST['ESEMB']) ? $_POST['ESEMB'] : null);
$PALENF = (isset($_POST['PALENF']) ? $_POST['PALENF'] : null);
$CONDROG = (isset($_POST['CONDROG']) ? $_POST['CONDROG'] : null);
$TOSIN = (isset($_POST['TOSIN']) ? $_POST['TOSIN'] : null);
$FIEBREIN = (isset($_POST['FIEBREIN']) ? $_POST['FIEBREIN'] : null);
$DOLOCABINP = (isset($_POST['DOLOCABINP']) ? $_POST['DOLOCABINP'] : null);
$DIFRESP = (isset($_POST['DIFRESP']) ? $_POST['DIFRESP'] : null);
$DOLARDGARG = (isset($_POST['DOLARDGARG']) ? $_POST['DOLARDGARG'] : null);
$ESCURRINAZ = (isset($_POST['ESCURRINAZ']) ? $_POST['ESCURRINAZ'] : null);
$OJOSROJOS = (isset($_POST['OJOSROJOS']) ? $_POST['OJOSROJOS'] : null);
$DOLMUSART = (isset($_POST['DOLMUSART']) ? $_POST['DOLMUSART'] : null);
$LHOPOBS = (isset($_POST['LHOPOBS']) ? $_POST['LHOPOBS'] : null);
$TALMEDOBS = (isset($_POST['TALMEDOBS']) ? $_POST['TALMEDOBS'] : null);
$SUAAOBS = (isset($_POST['SUAAOBS']) ? $_POST['SUAAOBS'] : null);
$SAFOPHOBS = (isset($_POST['SAFOPHOBS']) ? $_POST['SAFOPHOBS'] : null);
$ESEMBOBS = (isset($_POST['ESEMBOBS']) ? $_POST['ESEMBOBS'] : null);
$PALENFOBS = (isset($_POST['PALENFOBS']) ? $_POST['PALENFOBS'] : null);
$CONDROGOBS = (isset($_POST['CONDROGOBS']) ? $_POST['CONDROGOBS'] : null);
$TOSINOBS = (isset($_POST['TOSINOBS']) ? $_POST['TOSINOBS'] : null);
$FIEBREINOBS = (isset($_POST['FIEBREINOBS']) ? $_POST['FIEBREINOBS'] : null);
$DOLOCABINPOBS = (isset($_POST['DOLOCABINPOBS']) ? $_POST['DOLOCABINPOBS'] : null);
$DIFRESPOBS = (isset($_POST['DIFRESPOBS']) ? $_POST['DIFRESPOBS'] : null);
$DOLARDGARGOBS = (isset($_POST['DOLARDGARGOBS']) ? $_POST['DOLARDGARGOBS'] : null);
$ESCURRINAZOBS = (isset($_POST['ESCURRINAZOBS']) ? $_POST['ESCURRINAZOBS'] : null);
$OJOSROJOSOBS = (isset($_POST['OJOSROJOSOBS']) ? $_POST['OJOSROJOSOBS'] : null);
$DOLMUSARTOBS = (isset($_POST['DOLMUSARTOBS']) ? $_POST['DOLMUSARTOBS'] : null);
$caso_covid_quien = (isset($_POST['caso_covid_quien']) ? $_POST['caso_covid_quien'] : null);
$caso_covid_cuando = (isset($_POST['caso_covid_cuando']) ? $_POST['caso_covid_cuando'] : null);
$drawdataUrl = (isset($_POST['drawdataUrl']) ? $_POST['drawdataUrl'] : null);
$calleIn = (isset($_POST['calleIn']) ? $_POST['calleIn'] : null);
$numeroCin = (isset($_POST['numeroCin']) ? $_POST['numeroCin'] : null);
$coloniaInp = (isset($_POST['coloniaInp']) ? $_POST['coloniaInp'] : null);
$delegOmUn = (isset($_POST['delegOmUn']) ? $_POST['delegOmUn'] : null);
$comentarios_adicionales = (isset($_POST['comentarios_adicionales']) ? $_POST['comentarios_adicionales'] : null);
$contactar_a = (isset($_POST['contactar_a']) ? $_POST['contactar_a'] : null);
$contacto_emergencia1 = (isset($_POST['contacto_emergencia1']) ? $_POST['contacto_emergencia1'] : null);
$contactar_a1 = (isset($_POST['contactar_a1']) ? $_POST['contactar_a1'] : null);
$comentarios = (isset($_POST['comentarios']) ? $_POST['comentarios'] : null);



if (empty($nss)) {
    $nss = $num_imss;
}elseif($nss == "null"){
    $nss = $num_imss;
}

$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $cuenta,
    "name" => "cuentaPRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $puesto,
    "name" => "puestoPRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $agencia,
    "name" => "agenciaPRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $ape,
    "name" => "apePRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $apm,
    "name" => "apmPRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $nom,
    "name" => "nomPRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $fechN,
    "name" => "fechNPRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $ecivil,
    "name" => "ecivilPRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $num_imss,
    "name" => "num_imssPRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $tel_personal,
    "name" => "tel_personalPRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $tel_casa,
    "name" => "tel_casaPRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $contacto_emergencia,
    "name" => "contacto_emergenciaPRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $LHOP,
    "name" => "LHOPPRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $TALMED,
    "name" => "TALMEDPRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $SUAA,
    "name" => "SUAAPRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $SAFOPH,
    "name" => "SAFOPHPRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $ESEMB,
    "name" => "ESEMBPRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $PALENF,
    "name" => "PALENFPRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $CONDROG,
    "name" => "CONDROGPRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $TOSIN,
    "name" => "TOSINPRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $FIEBREIN,
    "name" => "FIEBREINPRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $DOLOCABINP,
    "name" => "DOLOCABINPPRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $DIFRESP,
    "name" => "DIFRESPPRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $DOLARDGARG,
    "name" => "DOLARDGARGPRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $ESCURRINAZ,
    "name" => "ESCURRINAZPRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $OJOSROJOS,
    "name" => "OJOSROJOSPRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $DOLMUSART,
    "name" => "DOLMUSARTPRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $LHOPOBS,
    "name" => "LHOPOBSPRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $TALMEDOBS,
    "name" => "TALMEDOBSPRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $SUAAOBS,
    "name" => "SUAAOBSPRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $SAFOPHOBS,
    "name" => "SAFOPHOBSPRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $ESEMBOBS,
    "name" => "ESEMBOBSPRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $PALENFOBS,
    "name" => "PALENFOBSPRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $CONDROGOBS,
    "name" => "CONDROGOBSPRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $TOSINOBS,
    "name" => "TOSINOBSPRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $FIEBREINOBS,
    "name" => "FIEBREINOBSPRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $DOLOCABINPOBS,
    "name" => "DOLOCABINPOBSPRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $DIFRESPOBS,
    "name" => "DIFRESPOBSPRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $DOLARDGARGOBS,
    "name" => "DOLARDGARGOBSPRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $ESCURRINAZOBS,
    "name" => "ESCURRINAZOBSPRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $OJOSROJOSOBS,
    "name" => "OJOSROJOSOBSPRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $DOLMUSARTOBS,
    "name" => "DOLMUSARTOBSPRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $caso_covid_quien,
    "name" => "caso_covid_quienPRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $caso_covid_cuando,
    "name" => "caso_covid_cuandoPRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $drawdataUrl,
    "name" => "drawdataUrlPRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $calleIn,
    "name" => "calleInPRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $numeroCin,
    "name" => "numeroCinPRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $coloniaInp,
    "name" => "coloniaInpPRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $delegOmUn,
    "name" => "delegOmUnPRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $comentarios_adicionales,
    "name" => "comentarios_adicionalesPRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $contactar_a,
    "name" => "contactar_aPRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $contacto_emergencia1,
    "name" => "contacto_emergencia1PRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $contactar_a1,
    "name" => "contactar_a1PRETEST",
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $comentarios,
    "name" => "comentariosPRETEST",
]);


