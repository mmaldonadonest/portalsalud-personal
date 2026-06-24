<?php
$ch = curl_init();
$headers  = [
    'x-api-key: XXXXXX',
    'Content-Type: application/json; charset=utf-8'
];
$postData = [];
curl_setopt($ch, CURLOPT_URL, "http://10.249.249.3/biows/ords/security/info/apps_onest");
curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
$result     = curl_exec($ch);
$statusCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
$data = json_decode($result);



?>
<div class="row">
    <div class="col-12 d-flex align-items-center justify-content-center mb-3">
        <h4>Consulta y alta de aplicaciones</h4>
        <hr class="hr">
    </div>
    <div class="col-12 col-md-12 mb-3">
        <h5 class="mb-2">Alta de app</h5>
        <div class="form-group">
            <label for="">Nombre de la app</label>
            <input type="text" class="form-control" name="appInput" id="appInput" aria-describedby="helpId" placeholder="">
            <small id="helpId" class="form-text text-muted">Ingrese un nombre valido para dar de alta la app</small>
        </div>
        <center>
            <button type="button" onclick="saveDats()" class="btn btn-primary mb-3">Guardar</button>
            <br>
            <div class="progress" id="progre1">
                <div class="progress-bar progress-bar-striped progress-bar-animated" role="progressbar" aria-valuenow="75" aria-valuemin="0" aria-valuemax="100" style="width: 100%"></div>
            </div>
        </center>
    </div>
    <div class="col-12 col-md-12 mb-3">
        <h5 class="mb-2">Consulta de app</h5>
        <label for="" class="form-label">App</label>
        <select onchange="searchIdApp()" class="form-select form-select-lg" name="appId" id="appId">
            <option selected>Seleccionar app</option>
            <?php
            foreach ($data->Datos as $user) {
            ?>
                <option value="<?= $user->id_app ?>"><?= $user->nombre_app ?></option>
            <?php
            }
            ?>
        </select>
        <br>
        <div class="progress" id="progre2">
            <div class="progress-bar progress-bar-striped progress-bar-animated" role="progressbar" aria-valuenow="75" aria-valuemin="0" aria-valuemax="100" style="width: 100%"></div>
        </div>
    </div>
    <br>
    <div class="col-12 row">
        <div class="col-12 col-md-4">
            <div class="form-group">
                <label for="">ID_APP</label>
                <input type="number" readonly class="form-control" name="idAppInput" id="idAppInput" aria-describedby="helpId" placeholder="">
            </div>
        </div>
        <div class="col-12 col-md-8">
            <div class="form-group">
                <label for="">Nombre</label>
                <input type="text" class="form-control" onchange="cambioApp()" name="nameAppInput" id="nameAppInput" aria-describedby="helpId" placeholder="">
            </div>
        </div>
        <div class="col-12 mt-3 mb-5">

            <div class="row">
                <div class="col-6">

                    <h3 class="mb-4 mt-3">Lista de roles</h3>

                </div>
                <div class="col-6">
                    <center>
                        <div id="btnModalIn"></div>
                    </center>


                </div>
            </div>
            <center>
                <div id="btnModalRol" class="d-grid gap-2"></div>


            </center>

            <table id="rolesTable" class="display table">
                <thead>
                    <tr>
                        <th>ID de rol</th>
                        <th>Nombre de rol</th>
                        <th>Acciones</th>
                    </tr>
                </thead>
                <tbody>
                    <!-- Las filas de la tabla se llenarán dinámicamente con JavaScript -->
                </tbody>
            </table>
        </div>
        <div class="col-12 mt-5">
            <div id="btnBorrar"></div>

        </div>
    </div>
</div>