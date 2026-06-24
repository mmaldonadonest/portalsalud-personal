<?php
$id_usuario = (isset($_POST['nssUserRol']) ? $_POST['nssUserRol'] : null);
$idAppUserRol = (isset($_POST['idAppUserRol']) ? $_POST['idAppUserRol'] : null);
$ch = curl_init();
$headers  = [
    'x-api-key: XXXXXX',
    'Content-Type: application/json; charset=utf-8'
];
$postData = [
    "id_usuario" => $id_usuario,
    "id_app" => $idAppUserRol
];
curl_setopt($ch, CURLOPT_URL, "http://10.249.249.3/biows/ords/security/info/consulta_app_rol_usuario");
curl_setopt($ch, CURLOPT_POST, 1);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($postData));
curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
echo $result     = curl_exec($ch);
$statusCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
$data = json_decode($result);

