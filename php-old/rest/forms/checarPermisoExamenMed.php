<?php
require_once('../../app/app.php');
$view = new app();
$others = new another();
$nss = (isset($_POST['nss']) ? $_POST['nss'] : null);
$checkExamPermiso = 1;
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $checkExamPermiso,
    "name" => "checkExamPermiso",
]);