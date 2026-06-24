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
        $targetIdApp = (isset($_POST['data']) ? $_POST['data'] : null);
        $buscarApp = $model->buscarIdAp($targetIdApp);
        echo $buscarApp;
        break;

    default:
        # code...
        break;
}
