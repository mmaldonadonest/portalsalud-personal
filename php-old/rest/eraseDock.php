<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Max-Age: 3600");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");
require_once('../app/app.php');
$model = new another;
$iddock = (isset($_GET['iddock']) ? $_GET['iddock'] : null);
$model -> eraseDock($iddock);


