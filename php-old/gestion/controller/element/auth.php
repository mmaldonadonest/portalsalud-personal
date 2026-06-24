<?php
session_start();
$user = (isset($_POST['user']) ? $_POST['user'] : null);
$pass = (isset($_POST['pass']) ? $_POST['pass'] : null);
$ch = curl_init();
$headers  = [
    'x-api-key: XXXXXX',
    'Content-Type: application/json; charset=utf-8'
];
$postData = [
    "id_usuario" => $user,
    "id_password" => $pass,
    "id_app" => 13

];
curl_setopt($ch, CURLOPT_URL, "http://10.249.249.3/biows/ords/security/login/registro_app");
curl_setopt($ch, CURLOPT_POST, 1);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($postData));
curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
$result     = curl_exec($ch);
$statusCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
$data = json_decode($result);
foreach ($data->Datos as $userJ) {
    $respuesta = $userJ->Mensaje;
}
if ($respuesta == "Login correcto") {
    $_SESSION['nombreG'] = $user;
}
echo $respuesta;
?>