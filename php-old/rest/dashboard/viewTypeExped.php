<?php
require_once('../../app/vistas.php');
$view = new vistas();
$type = (isset($_POST['data']) ? $_POST['data'] : null);
$nss = (isset($_POST['nss']) ? $_POST['nss'] : null);
$view->viewExpHeredoFam($type, $nss);
