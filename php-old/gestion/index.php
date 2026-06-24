<?php
require_once('./app/templates.php');
session_start();

if (empty($_SESSION['nombreG'])) {
    echo new Template("login.html");
} else {
    echo new Template("landing.html");
}
