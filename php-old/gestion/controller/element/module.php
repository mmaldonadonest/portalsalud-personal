<?php
$app = (isset($_POST['data']) ? $_POST['data'] : null);
$nss = (isset($_POST['nss']) ? $_POST['nss'] : null);
$ch = curl_init();
$headers  = [
    'x-api-key: XXXXXX',
    'Content-Type: application/json; charset=utf-8'
];
$postData = [
    "id_usuario" => $nss,
    "id_password" => "123",
    "id_app" => $app

];
curl_setopt($ch, CURLOPT_URL, "http://10.249.249.3/biows/ords/security/login/menus");
curl_setopt($ch, CURLOPT_POST, 1);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($postData));
curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
$result     = curl_exec($ch);
$statusCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
$data = json_decode($result);

?>
<div class="row">
    <div class="col-12 mb-3">
        <div class="mb-3">
            <label for="" class="form-label">Rol</label>
            <select
                class="form-select form-select-lg"
                name="SelectRol"
                id="SelectRol"
            >
                <option selected>Selecciona</option>
            </select>
        </div>
        
    </div>
    <div class="col-12 mb-3">
        <hr>
    </div>
    <input type="hidden" id="nssUserRol" value="<?=$nss?>">
    <input type="hidden" id="idAppUserRol" value="<?=$app?>">
    <div class="col-6">
        <div class="mb-3">
            <label for="" class="form-label">Contraseña</label>
            <input type="password" class="form-control" name="passInpRols1" id="passInpRols1" aria-describedby="helpId" placeholder="" />
        </div>
    </div>
    <div class="col-6">
        <div class="mb-3">
            <label for="" class="form-label">Repetir contraseña</label>
            <input type="password" class="form-control" name="passInpRols2" id="passInpRols2" aria-describedby="helpId" placeholder="" />
        </div>
    </div>

</div>
<center>
    <button type="button" class="btn btn-success" onclick="saverolUser()">Guardar</button>
</center>