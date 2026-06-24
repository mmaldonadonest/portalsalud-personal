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
        $name = (isset($_POST['name']) ? $_POST['name'] : null);
        $idApp = (isset($_POST['idApp']) ? $_POST['idApp'] : null);
        $cambio = $model->cambioApp($idApp, $name);
        echo $cambio;
        break;

    default:
        # code...
        break;
}
