<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");

header("Access-Control-Max-Age: 3600");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

function loadParms()
{
    session_start();
    $ch = curl_init();
    $headers  = [
        'x-api-key: XXXXXX',
        'Content-Type: application/json; charset=utf-8'
    ];
    $postData = [
        'id_usuario' =>  $_SESSION['nombre'],
        'id_app' => '13'
    ];
    curl_setopt($ch, CURLOPT_URL, "http://10.249.249.3/biows/ords/security/info/consulta_app_rol_usuario");
    /* curl_setopt($ch, CURLOPT_URL, "http://10.249.249.3/biows/ords/security/info/consulta_app_rol_usuario"); */
    curl_setopt($ch, CURLOPT_POST, 1);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($postData));
    curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
    return $result     = curl_exec($ch);
    $statusCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
}
$datsPermiso = loadParms();

$data = json_decode($datsPermiso, true);
$idRol = $data['Resultado']['Datos'][0]['id_rol'];

function loadMenus($idRol, $idApp)
{
    $ch = curl_init();
    $headers  = [
        'x-api-key: XXXXXX',
        'Content-Type: application/json; charset=utf-8'
    ];
    $postData = [
        'id_app' => $idApp,
        'id_rol' => $idRol
    ];
    curl_setopt($ch, CURLOPT_URL, "http://10.249.249.3/biows/ords/security/info/consulta_app_rol_menu");
    /* curl_setopt($ch, CURLOPT_URL, "http://10.249.249.3/biows/ords/security/info/consulta_app_rol_menu"); */
    curl_setopt($ch, CURLOPT_POST, 1);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($postData));
    curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
    return $result     = curl_exec($ch);
    $statusCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
}
echo loadMenus($idRol,13);
