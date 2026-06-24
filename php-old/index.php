<?php
require_once('./app/routes.php');
session_start();
$uri = explode('/', $_SERVER['REQUEST_URI']);
$path = $uri[1];
$view = new routes($path);
?>