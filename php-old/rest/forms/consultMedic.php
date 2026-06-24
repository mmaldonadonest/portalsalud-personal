<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Max-Age: 3600");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");
require_once('../../app/app.php');
$model = new app;
$requestMethod = $_SERVER["REQUEST_METHOD"];
$requestMethod = strtoupper($requestMethod);
switch ($requestMethod) {
    case 'POST':
        $idNss = (isset($_POST['idNss']) ? $_POST['idNss'] : null);
        $idArchivoRel = (isset($_POST['idArchivoRel']) ? $_POST['idArchivoRel'] : null);
        $TipoConsultaInput = (isset($_POST['TipoConsultaInput']) ? $_POST['TipoConsultaInput'] : null);
        $AreaAccidenteInput = (isset($_POST['AreaAccidenteInput']) ? $_POST['AreaAccidenteInput'] : null);
        $AreaAnatomicaInput = (isset($_POST['AreaAnatomicaInput']) ? $_POST['AreaAnatomicaInput'] : null);
        $CausaInput = (isset($_POST['CausaInput']) ? $_POST['CausaInput'] : null);
        $PesoInput = (isset($_POST['PesoInput']) ? $_POST['PesoInput'] : null);
        $TallaInput = (isset($_POST['TallaInput']) ? $_POST['TallaInput'] : null);
        $IMCINPUT = (isset($_POST['IMCINPUT']) ? $_POST['IMCINPUT'] : null);
        $FCINPUT = (isset($_POST['FCINPUT']) ? $_POST['FCINPUT'] : null);
        $FRINPUT = (isset($_POST['FRINPUT']) ? $_POST['FRINPUT'] : null);
        $TAINPUT = (isset($_POST['TAINPUT']) ? $_POST['TAINPUT'] : null);
        $TemperaturaInput = (isset($_POST['TemperaturaInput']) ? $_POST['TemperaturaInput'] : null);
        $MOTIVOCONSULTAINP = (isset($_POST['MOTIVOCONSULTAINP']) ? $_POST['MOTIVOCONSULTAINP'] : null);
        $EXPLOFISIINP = (isset($_POST['EXPLOFISIINP']) ? $_POST['EXPLOFISIINP'] : null);
        $DIAGNOSTICOINPUT = (isset($_POST['DIAGNOSTICOINPUT']) ? $_POST['DIAGNOSTICOINPUT'] : null);
        $TRATAMIENTOINPUT = (isset($_POST['TRATAMIENTOINPUT']) ? $_POST['TRATAMIENTOINPUT'] : null);
        $FIRMA = (isset($_POST['FIRMA']) ? $_POST['FIRMA'] : null);
        echo $model->addConsultM($data = [
            "idNss" => $idNss,
            "idArchivoRel" => $idArchivoRel,
            "TipoConsultaInput" => $TipoConsultaInput,
            "AreaAccidenteInput" => $AreaAccidenteInput,
            "AreaAnatomicaInput" => $AreaAnatomicaInput,
            "CausaInput" => $CausaInput,
            "PesoInput" => $PesoInput,
            "TallaInput" => $TallaInput,
            "IMCINPUT" => $IMCINPUT,
            "FCINPUT" => $FCINPUT,
            "FRINPUT" => $FRINPUT,
            "TAINPUT" => $TAINPUT,
            "TemperaturaInput" => $TemperaturaInput,
            "MOTIVOCONSULTAINP" => $MOTIVOCONSULTAINP,
            "EXPLOFISIINP" => $EXPLOFISIINP,
            "DIAGNOSTICOINPUT" => $DIAGNOSTICOINPUT,
            "TRATAMIENTOINPUT" => $TRATAMIENTOINPUT,
            "FIRMA" => $FIRMA,
        ]);
        break;

    default:
        # code...
        break;
}
