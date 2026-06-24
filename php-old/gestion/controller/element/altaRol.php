<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Max-Age: 3600");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");
require_once('../../app/app.php');
$model = new appGestion;
$requestMethod = $_SERVER["REQUEST_METHOD"];
$requestMethod = strtoupper($requestMethod);
switch ($requestMethod) {
    case 'POST':
        $idAppModalRol = (isset($_POST['idAppModalRol']) ? $_POST['idAppModalRol'] : null);
        $IdROLM = (isset($_POST['IdROLM']) ? $_POST['IdROLM'] : null);
        $idNombreMROL = (isset($_POST['idNombreMROL']) ? $_POST['idNombreMROL'] : null);
        
        $altaApp = $model->altaRol($idAppModalRol, $IdROLM, $idNombreMROL);
        echo $altaApp;
        break;

    default:
        # code...
        break;
}
