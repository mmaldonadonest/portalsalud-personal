<?php
require_once('../../app/app.php');
$model = new app;
$requestMethod = $_SERVER["REQUEST_METHOD"];
$requestMethod = strtoupper($requestMethod);
$nss = (isset($_GET['nss']) ? $_GET['nss'] : null);
echo $json = $model->showIncapacidades($nss);
