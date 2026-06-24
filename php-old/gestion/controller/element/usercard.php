<?php
$nss = (isset($_POST['data']) ? $_POST['data'] : null);
$ch = curl_init();
$headers  = [
    'x-api-key: XXXXXX',
    'Content-Type: application/json; charset=utf-8'
];
$postData = [
    "id_usuario" => $nss,
    "id_password" => "123",
    "id_app" => 13

];
curl_setopt($ch, CURLOPT_URL, "http://10.249.249.3/biows/ords/security/login/menus");
curl_setopt($ch, CURLOPT_POST, 1);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($postData));
curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
$result     = curl_exec($ch);
$statusCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
$data = json_decode($result);
foreach ($data->Resultado as $datos) {
    echo $datos;
}

?>
<div class="card border-primary mb-3 col-12">
    <?php
    foreach ($data->Datos as $user) {
    ?>
        <div class="card-header"><?= $user->nombre_usuario; ?></div>
        <div class="card-body">
            <h4 class="card-title"><?= $user->id_usuario; ?></h4>
            <br>
            <div class="mb-3">
                <label for="" class="form-label">Aplicación</label>
                <select class="form-select form-select-lg" onchange="showModules()" name="" id="appSelect">
                <option selected>Selecciona aplicación</option>
                    <?php
                    foreach ($user->apps as $value) {
                        
                        ?>
                        <option value="<?=$value->id_app;?>"><?=$value->nombre_app;?></option>
                        <?php
                    }
                    ?>
                </select>
            </div>
            <br>
            <div id="resModulo"></div>
        </div>
    <?php


    }
    ?>



</div>
<?php
