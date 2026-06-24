<?php
require_once('../../app/app.php');
$model = new app;
$requestMethod = $_SERVER["REQUEST_METHOD"];
$requestMethod = strtoupper($requestMethod);
switch ($requestMethod) {
    case 'GET':
        $datas = (isset($_GET['data']) ? $_GET['data'] : null);
        $json = $model->showfilesNotaMedPdf($datas);
        $data = json_decode($json);
?>
        <div class="table-responsive mb-3">
            <table id="filetable" class="table animate__animated animate__fadeIn">
                <thead class="thead-dark">
                    <tr>
                        <th>Nombre del archivo</th>
                        <th>Fecha de subida</th>

                        <th>Descarga</th>
                        <!-- <th>Borrar</th> -->
                    </tr>
                </thead>
                <tbody>
                    <?php
                    foreach ($data->response as $filedat) {
                    ?>
                        <tr>
                            <td scope="row"><?= $filedat->name ?></td>
                            <td><?= $filedat->date_upload ?></td>

                            <td class="row"><textarea name="" style="display: none;" id="text<?= $filedat->id ?>" cols="30" rows="10"><?= $filedat->url ?></textarea>
                                <div class="col-12 col-md-12 col-lg-6"><button type="button" name="" onclick="downloadPDFCons('<?= $filedat->id ?>')" id="" class="btn btn-info btn-block mb-3">Descargar archivo <i class="fas fa-file-pdf"></i></button></div>
                                <div class="col-12 col-md-12 col-lg-6"><button type="button" name="" onclick="erasedocksNotaMed('<?= $datas ?>','<?= $filedat->id ?>')" id="" class="btn btn-danger btn-block">Borrar archivo <i class="fas fa-backspace"></i></button></div>
                            </td>
                            <!-- <td><button onclick="eraseDat()" name="" id="" class="btn btn-danger btn-block"><i class="fas fa-eraser"></i></button></td>
                         -->
                        </tr>
                    <?php
                    }
                    ?>
                </tbody>
            </table>
        </div>
        

<?php

        break;

    default:
        # code...
        break;
}
