<?php
require_once('../../app/app.php');
$view = new app();
$datas = (isset($_POST['data']) ? $_POST['data'] : null);
$view -> neurologiaDats($datas);
?>