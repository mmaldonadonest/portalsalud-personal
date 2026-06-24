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
        session_start();
        $userV = (isset($_POST['user']) ? $_POST['user'] : null);
        $pass = (isset($_POST['pass']) ? $_POST['pass'] : null);
        $json =  $model->loginApiRest($userV, $pass);
	$data = json_decode($json);
//	echo var_dump($data);
        foreach ($data->Datos as $userJ) {
            $respuestaNew = $userJ->Mensaje;
        }
        if ($respuestaNew == "Login correcto") {
            $_SESSION['nombre'] = $userV;
            $_SESSION['idfecha'] = $pass;
            echo "1";
        } else {
            echo "100";
        }


        break;

    default:
        break;
}
