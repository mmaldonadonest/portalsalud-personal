<?php
require_once('../../app/app.php');
$view = new app();
$others = new another();
$nss = (isset($_POST['nss']) ? $_POST['nss'] : null);

$checkExamPermiso = $others->selectDatsExMed($data = [
    "nss" => $nss,
    "type" => "checkExamPermiso"
]);

echo $checkExamPermiso;