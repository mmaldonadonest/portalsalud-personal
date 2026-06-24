<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Max-Age: 3600");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");
require_once('../../app/app.php');
require_once('../../app/templates.php');
$model = new app;
$requestMethod = $_SERVER["REQUEST_METHOD"];
$requestMethod = strtoupper($requestMethod);
switch ($requestMethod) {
    case 'POST':
        $rand = rand(10000, 99999);
        $hoy = date("d/m/y H:i:s");
        $number = md5($rand.$hoy);
        $template = new Template("../../view/content/incapacidadForm.html", $data = [
            'number' => $number
        ]);
        echo $template;
        break;

    default:
        # code...
        break;
}
