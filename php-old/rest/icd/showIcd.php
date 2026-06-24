<?php
require_once('../../app/app.php');
$model = new app;
$requestMethod = $_SERVER["REQUEST_METHOD"];
$requestMethod = strtoupper($requestMethod);
$icd = (isset($_GET['icd']) ? $_GET['icd'] : null);
$json = $model->showICD($icd);
$data = json_decode($json);
?>
<div class="table-responsive">
    <table id="filetableICD" class="table animate__animated animate__fadeIn">
        <thead class="thead-dark">
            <tr>
                <th>Clave</th>


            </tr>
        </thead>
        <tbody>
            <?php
            foreach ($data as $filedat) {
            ?>
                <tr>
                    <td><a class="btn btn-light" onclick="addIcdConsMed('<?= $filedat->clave_id ?> - <?= $filedat->nombre_clave ?> ')"><?= $filedat->clave_id ?> - <?= $filedat->nombre_clave ?></a></td>
                </tr>
            <?php
            }
            ?>
        </tbody>
    </table>
</div>

<?php
