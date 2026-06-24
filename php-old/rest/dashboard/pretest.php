<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Max-Age: 3600");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");
require_once('../../app/app.php');
require_once('../../app/templates.php');
$model = new app;
$others = new another();
$jason =  $others->showPretest($nss);
$decoded = json_decode($jason);
$requestMethod = $_SERVER["REQUEST_METHOD"];
$requestMethod = strtoupper($requestMethod);
$nss = (isset($_POST['data']) ? $_POST['data'] : null);
$cuentaPRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "cuentaPRETEST"
]);
$puestoPRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "puestoPRETEST"
]);
$agenciaPRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "agenciaPRETEST"
]);
$apePRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "apePRETEST"
]);
$apmPRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "apmPRETEST"
]);
$nomPRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "nomPRETEST"
]);
$fechNPRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "fechNPRETEST"
]);
$ecivilPRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "ecivilPRETEST"
]);
$num_imssPRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "num_imssPRETEST"
]);
$tel_personalPRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "tel_personalPRETEST"
]);
$tel_casaPRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "tel_casaPRETEST"
]);
$contacto_emergenciaPRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "contacto_emergenciaPRETEST"
]);
$LHOPPRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "LHOPPRETEST"
]);
$TALMEDPRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "TALMEDPRETEST"
]);
$SUAAPRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "SUAAPRETEST"
]);
$SAFOPHPRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "SAFOPHPRETEST"
]);
$ESEMBPRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "ESEMBPRETEST"
]);
$PALENFPRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "PALENFPRETEST"
]);
$CONDROGPRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "CONDROGPRETEST"
]);
$TOSINPRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "TOSINPRETEST"
]);
$FIEBREINPRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "FIEBREINPRETEST"
]);
$DOLOCABINPPRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "DOLOCABINPPRETEST"
]);
$DIFRESPPRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "DIFRESPPRETEST"
]);
$DOLARDGARGPRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "DOLARDGARGPRETEST"
]);
$ESCURRINAZPRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "ESCURRINAZPRETEST"
]);
$OJOSROJOSPRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "OJOSROJOSPRETEST"
]);
$DOLMUSARTPRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "DOLMUSARTPRETEST"
]);
$LHOPOBSPRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "LHOPOBSPRETEST"
]);
$TALMEDOBSPRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "TALMEDOBSPRETEST"
]);
$SUAAOBSPRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "SUAAOBSPRETEST"
]);
$SAFOPHOBSPRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "SAFOPHOBSPRETEST"
]);
$ESEMBOBSPRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "ESEMBOBSPRETEST"
]);
$PALENFOBSPRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "PALENFOBSPRETEST"
]);
$CONDROGOBSPRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "CONDROGOBSPRETEST"
]);
$TOSINOBSPRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "TOSINOBSPRETEST"
]);
$FIEBREINOBSPRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "FIEBREINOBSPRETEST"
]);
$DOLOCABINPOBSPRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "DOLOCABINPOBSPRETEST"
]);
$DIFRESPOBSPRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "DIFRESPOBSPRETEST"
]);
$DOLARDGARGOBSPRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "DOLARDGARGOBSPRETEST"
]);
$ESCURRINAZOBSPRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "ESCURRINAZOBSPRETEST"
]);
$OJOSROJOSOBSPRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "OJOSROJOSOBSPRETEST"
]);
$DOLMUSARTOBSPRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "DOLMUSARTOBSPRETEST"
]);
$caso_covid_quienPRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "caso_covid_quienPRETEST"
]);
$caso_covid_cuandoPRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "caso_covid_cuandoPRETEST"
]);
$drawdataUrlPRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "drawdataUrlPRETEST"
]);
$calleInPRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "calleInPRETEST"
]);
$numeroCinPRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "numeroCinPRETEST"
]);
$coloniaInpPRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "coloniaInpPRETEST"
]);
$delegOmUnPRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "delegOmUnPRETEST"
]);
$comentarios_adicionalesPRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "comentarios_adicionalesPRETEST"
]);
$contactar_aPRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "contactar_aPRETEST"
]);
$contacto_emergencia1PRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "contacto_emergencia1PRETEST"
]);
$contactar_a1PRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "contactar_a1PRETEST"
]);
$comentariosPRETEST = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "comentariosPRETEST"
]);





switch ($requestMethod) {
    case 'POST':
        $template = new Template(
            "../../view/content/pretest.html",
            $data = [
                "cuentaPRETEST" => $cuentaPRETEST,
                "puestoPRETEST" => $puestoPRETEST,
                "agenciaPRETEST" => $agenciaPRETEST,
                "apePRETEST" => $apePRETEST,
                "apmPRETEST" => $apmPRETEST,
                "nomPRETEST" => $nomPRETEST,
                "fechNPRETEST" => $fechNPRETEST,
                "ecivilPRETEST" => $ecivilPRETEST,
                "num_imssPRETEST" => $num_imssPRETEST,
                "tel_personalPRETEST" => $tel_personalPRETEST,
                "tel_casaPRETEST" => $tel_casaPRETEST,
                "contacto_emergenciaPRETEST" => $contacto_emergenciaPRETEST,
                "LHOPPRETEST" => $LHOPPRETEST,
                "TALMEDPRETEST" => $TALMEDPRETEST,
                "SUAAPRETEST" => $SUAAPRETEST,
                "SAFOPHPRETEST" => $SAFOPHPRETEST,
                "ESEMBPRETEST" => $ESEMBPRETEST,
                "PALENFPRETEST" => $PALENFPRETEST,
                "CONDROGPRETEST" => $CONDROGPRETEST,
                "TOSINPRETEST" => $TOSINPRETEST,
                "FIEBREINPRETEST" => $FIEBREINPRETEST,
                "DOLOCABINPPRETEST" => $DOLOCABINPPRETEST,
                "DIFRESPPRETEST" => $DIFRESPPRETEST,
                "DOLARDGARGPRETEST" => $DOLARDGARGPRETEST,
                "ESCURRINAZPRETEST" => $ESCURRINAZPRETEST,
                "OJOSROJOSPRETEST" => $OJOSROJOSPRETEST,
                "DOLMUSARTPRETEST" => $DOLMUSARTPRETEST,
                "LHOPOBSPRETEST" => $LHOPOBSPRETEST,
                "TALMEDOBSPRETEST" => $TALMEDOBSPRETEST,
                "SUAAOBSPRETEST" => $SUAAOBSPRETEST,
                "SAFOPHOBSPRETEST" => $SAFOPHOBSPRETEST,
                "ESEMBOBSPRETEST" => $ESEMBOBSPRETEST,
                "PALENFOBSPRETEST" => $PALENFOBSPRETEST,
                "CONDROGOBSPRETEST" => $CONDROGOBSPRETEST,
                "TOSINOBSPRETEST" => $TOSINOBSPRETEST,
                "FIEBREINOBSPRETEST" => $FIEBREINOBSPRETEST,
                "DOLOCABINPOBSPRETEST" => $DOLOCABINPOBSPRETEST,
                "DIFRESPOBSPRETEST" => $DIFRESPOBSPRETEST,
                "DOLARDGARGOBSPRETEST" => $DOLARDGARGOBSPRETEST,
                "ESCURRINAZOBSPRETEST" => $ESCURRINAZOBSPRETEST,
                "OJOSROJOSOBSPRETEST" => $OJOSROJOSOBSPRETEST,
                "DOLMUSARTOBSPRETEST" => $DOLMUSARTOBSPRETEST,
                "caso_covid_quienPRETEST" => $caso_covid_quienPRETEST,
                "caso_covid_cuandoPRETEST" => $caso_covid_cuandoPRETEST,
                "drawdataUrlPRETEST" => $drawdataUrlPRETEST,
                "calleInPRETEST" => $calleInPRETEST,
                "numeroCinPRETEST" => $numeroCinPRETEST,
                "coloniaInpPRETEST" => $coloniaInpPRETEST,
                "delegOmUnPRETEST" => $delegOmUnPRETEST,
                "comentarios_adicionalesPRETEST" => $comentarios_adicionalesPRETEST,
                "contactar_aPRETEST" => $contactar_aPRETEST,
                "contacto_emergencia1PRETEST" => $contacto_emergencia1PRETEST,
                "contactar_a1PRETEST" => $contactar_a1PRETEST,
                "comentariosPRETEST" => $comentariosPRETEST
            ]
        );
        echo $template;
        break;

    default:
        # code...
        break;
}
