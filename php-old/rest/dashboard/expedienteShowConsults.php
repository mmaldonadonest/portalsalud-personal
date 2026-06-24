<?php
require_once('../../app/app.php');
$model = new app;
$requestMethod = $_SERVER["REQUEST_METHOD"];
$requestMethod = strtoupper($requestMethod);
$type = (isset($_GET['type']) ? $_GET['type'] : null);
switch ($requestMethod) {
    case 'GET':
        switch ($type) {
            case 'expedienteConsulta':
                $datas = (isset($_GET['data']) ? $_GET['data'] : null);
                $json = $model->showConsults($datas);
                $data = json_decode($json);
?>
                <div class="table-responsive">
                    <table id="filetable" class="table animate__animated animate__fadeIn">
                        <thead class="thead-dark">
                            <tr>
                                <th>Fecha de consulta</th>
                                <th>Tipo de consulta</th>
                                <th>Area de accidente</th>
                                <th>Ver registro completo</th>

                            </tr>
                        </thead>
                        <tbody>
                            <?php
                            foreach ($data->Datos as $filedat) {
                            ?>
                                <tr>
                                    <td scope="row"><?= date("d-m-Y", strtotime($filedat->fecha_consulta)) ?></td>
                                    <td><?= $filedat->tipo_consulta ?></td>
                                    <td><?= $filedat->area_accidente ?></td>
                                    <td><button name="" id="" onclick="showDetailsCons(<?= $filedat->id_consulta ?>)" data-toggle="modal" data-target="#exampleModal" class="btn btn-info btn-block"><i class="fas fa-folder-open"></i></button></td>

                                </tr>
                            <?php
                            }
                            ?>
                        </tbody>
                    </table>
                </div>

            <?php

                break;
            case 'expedienteIncapacidad':
                $datas = (isset($_GET['data']) ? $_GET['data'] : null);
                $json = $model->showIncapacidades($datas);
                $data = json_decode($json);
            ?>
                <div class="table-responsive">
                    <table id="filetable" class="table animate__animated animate__fadeIn">
                        <thead class="thead-dark">
                            <tr>
                                <th>Fecha de consulta</th>
                                <th>Rubro</th>
                                <th>Folio</th>
                                <th>Tipo de incapacidad</th>
                                <th>Ramo</th>
                                <th>Imputable</th>
                                <th>Estado dictamen</th>
                                <th>Goce sueldo</th>
                                <th>Complemento salarial</th>
                                <th>ST2</th>
                                <th>Alta</th>
                                <th>Fecha alta</th>
                                <th>Dias autorizados</th>
                                <th>Fecha primera incapacidad</th>
                                <th>Fecha inicio</th>
                                <th>Fecha termino</th>
                                <th>Salario integrado</th>
                                <th>Salario acomulado</th>
                                <th>Costo</th>
                                <th></th>

                            </tr>
                        </thead>
                        <tbody>
                            <?php
                            foreach ($data->Datos as $filedat) {
                            ?>
                                <tr>
                                    <td scope="row"><?= date("d-m-Y", strtotime($filedat->fecha_consulta)) ?></td>
                                    <td><?= $filedat->rubro ?></td>
                                    <td><?= $filedat->folio_incapacidad ?></td>
                                    <td><?= $filedat->tipo_incapacidad ?></td>
                                    <td><?= $filedat->ramo ?></td>
                                    <td><?= $filedat->imputable ?></td>
                                    <td><?= $filedat->estado_dictamen ?></td>
                                    <td><?= $filedat->goce_sueldo ?></td>
                                    <td><?= $filedat->complemento_salarial ?></td>
                                    <td><?= $filedat->st2 ?></td>
                                    <td><?= $filedat->alta ?></td>
                                    <td><?= $filedat->fecha_alta ?></td>
                                    <td><?= $filedat->dias_autorizados ?></td>
                                    <td><?= $filedat->fecha_primera_incapacidad ?></td>
                                    <td><?= $filedat->fecha_inicio ?></td>
                                    <td><?= $filedat->fecha_termino ?></td>
                                    <td><?= $filedat->salario_integrado ?></td>
                                    <td><?= $filedat->salario_acumulado ?></td>
                                    <td><?= $filedat->costo ?></td>
                                    <td><button name="" id="" onclick="showDetailsIncap(<?= $filedat->id_consulta ?>)" data-toggle="modal" data-target="#exampleModal" class="btn btn-info btn-block"><i class="fas fa-folder-open"></i></button></td>

                                </tr>
                            <?php
                            }
                            ?>
                        </tbody>
                    </table>
                </div>

<?php
                break;
        }

        break;

    default:
        # code...
        break;
}
