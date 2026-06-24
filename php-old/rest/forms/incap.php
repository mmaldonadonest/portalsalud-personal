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
        $foilioInc = (isset($_POST['foilioInc']) ? $_POST['foilioInc'] : null);
        $ramoInp = (isset($_POST['ramoInp']) ? $_POST['ramoInp'] : null);
        $tipoIncInput = (isset($_POST['tipoIncInput']) ? $_POST['tipoIncInput'] : null);
        $fechainiInp = (isset($_POST['fechainiInp']) ? $_POST['fechainiInp'] : null);
        $fechainiInpINI = (isset($_POST['fechainiInpINI']) ? $_POST['fechainiInpINI'] : null);
        $fechaTermInp = (isset($_POST['fechaTermInp']) ? $_POST['fechaTermInp'] : null);
        $DiasAutInp = (isset($_POST['DiasAutInp']) ? $_POST['DiasAutInp'] : null);
        $SalIntegrInp = (isset($_POST['SalIntegrInp']) ? $_POST['SalIntegrInp'] : null);
        $CostoInp = (isset($_POST['CostoInp']) ? $_POST['CostoInp'] : null);
        $InpuInput = (isset($_POST['InpuInput']) ? $_POST['InpuInput'] : null);
        $EstDictInput = (isset($_POST['EstDictInput']) ? $_POST['EstDictInput'] : null);
        $GoceSueldoInp = (isset($_POST['GoceSueldoInp']) ? $_POST['GoceSueldoInp'] : null);
        $ComplSalaInp = (isset($_POST['ComplSalaInp']) ? $_POST['ComplSalaInp'] : null);
        $salarioAcomuIn = (isset($_POST['salarioAcomuIn']) ? $_POST['salarioAcomuIn'] : null);
        $st2Input = (isset($_POST['st2Input']) ? $_POST['st2Input'] : null);
        $rubroInput = (isset($_POST['rubroInput']) ? $_POST['rubroInput'] : null);
        $altaInput = (isset($_POST['altaInput']) ? $_POST['altaInput'] : null);
        $fechaAltaInp = (isset($_POST['fechaAltaInp']) ? $_POST['fechaAltaInp'] : null);
        $FIRMA = (isset($_POST['FIRMA']) ? $_POST['FIRMA'] : null);
        if (empty($salarioAcomuIn)) {
            $salarioAcomuIn = 0;
        }
        if (empty($fechainiInp)) {
            $fechainiInp = $fechainiInpINI;
        }
        echo $model->addIncapacidad($data = [
            "idNss" => $idNss,
            "idArchivoRel" => $idArchivoRel,
            "foilioInc" => $foilioInc,
            "ramoInp" => $ramoInp,
            "tipoIncInput" => $tipoIncInput,
            "fechainiInp" => $fechainiInp,
            "fechainiInpINI" => $fechainiInpINI,
            "fechaTermInp" => $fechaTermInp,
            "DiasAutInp" => $DiasAutInp,
            "SalIntegrInp" => $SalIntegrInp,
            "CostoInp" => $CostoInp,
            "InpuInput" => $InpuInput,
            "EstDictInput" => $EstDictInput,
            "GoceSueldoInp" => $GoceSueldoInp,
            "ComplSalaInp" => $ComplSalaInp,
            "salarioAcomuIn" => $salarioAcomuIn,
            "st2Input" => $st2Input,
            "rubroInput" => $rubroInput,
            "altaInput" => $altaInput,
            "fechaAltaInp" => $fechaAltaInp,
            "FIRMA" => $FIRMA,
        ]);
        break;

    default:
        # code...
        break;
}
