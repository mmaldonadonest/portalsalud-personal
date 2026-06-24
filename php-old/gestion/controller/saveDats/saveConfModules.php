<?php
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
curl_setopt($ch, CURLOPT_URL, "http://10.249.249.3/biows/ords/security/login/login_asigna_modulos");
curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
$result     = curl_exec($ch);
$statusCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
$data = json_decode($result);


