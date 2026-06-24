<?php
require_once('../../app/app.php');
$requestMethod = $_SERVER["REQUEST_METHOD"];
$requestMethod = strtoupper($requestMethod);
switch ($requestMethod) {
    case 'POST':
        $f1 = (isset($_POST['ini']) ? $_POST['ini'] : null);
        $f2 = (isset($_POST['fini']) ? $_POST['fini'] : null);

        if ($f1 && $f2) {
            // Crear objetos DateTime a partir de las cadenas de fecha
            $dateTime1 = new DateTime($f1);
            $fechaFormateada1 = $dateTime1->format('d/m/y');
            $dateTime2 = new DateTime($f2);
            $fechaFormateada2 = $dateTime2->format('d/m/y');

            $model = new another;

            // Imprimir el resultado de la función traerDatosGeneralIncap
            echo $model->traerDatosGeneralIncap($fechaFormateada1, $fechaFormateada2);
        } else {

        }

        break;

    default:
        # code...
        break;
}
