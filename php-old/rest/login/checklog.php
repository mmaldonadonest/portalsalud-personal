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
        $userV = (isset($_POST['user']) ? $_POST['user'] : null);
        $pass = (isset($_POST['pass']) ? $_POST['pass'] : null);
        $json =  $model->showusers($userV);
        $data = json_decode($json);
        foreach ($data->response as $user) {
            $correo = $user->correo;
            $contrasena =  $user->pass;
        }
        if ($userV == $correo) {
            if ($pass == $contrasena) {
                echo "Usuario valido";
            } else {
                echo "Contraseña invalida";
            }
        } else {
            echo "Usuario no existe";
        }
        break;

    default:


        break;
}
