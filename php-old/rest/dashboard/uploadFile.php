<?php
require_once('../../app/app.php');
$model = new app;
$requestMethod = $_SERVER["REQUEST_METHOD"];
$requestMethod = strtoupper($requestMethod);
switch ($requestMethod) {
    case 'POST':
        $id = (isset($_POST['id']) ? $_POST['id'] : null);
        $type = (isset($_POST['type']) ? $_POST['type'] : null);
        $model = new app;
        $fechaActual = date('d-m-Y');
        $rand = rand(1, 99);
        $filename = $rand . $id . $fechaActual . $_FILES['file']['name'];
        /* move_uploaded_file($_FILES["file"]["tmp_name"], "../../files/" . $filename); */
        $base64 = base64_encode(file_get_contents($_FILES['file']['tmp_name']));
        $model -> addFileBD($id,$filename,$base64,$type);
        break;

    default:
        # code...
        break;
}
