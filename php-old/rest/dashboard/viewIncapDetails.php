<?php
require_once('../../app/app.php');
$model = new app;
$requestMethod = $_SERVER["REQUEST_METHOD"];
$requestMethod = strtoupper($requestMethod);
$datas = (isset($_POST['data']) ? $_POST['data'] : null);
$serie = (isset($_POST['serieId']) ? $_POST['serieId'] : null);
$json = $model->showIncapacidades($datas);
$data = json_decode($json);

foreach ($data->Datos as $filedat) {

    if ($filedat->id_consulta == $serie) {
        $fecha_consulta = $filedat->fecha_consulta;
        $folio_incapacidad = $filedat->folio_incapacidad;
        $ramo = $filedat->ramo;
        $tipo_incapacidad = $filedat->tipo_incapacidad;
        $fecha_inicio = $filedat->fecha_inicio;
        $fecha_termino = $filedat->fecha_termino;
        $dias_autorizados = $filedat->dias_autorizados;
        $salario_integrado = $filedat->salario_integrado;
        $costo = $filedat->costo;
        $imputable = $filedat->imputable;
        $estado_dictamen = $filedat->estado_dictamen;
        $goce_sueldo = $filedat->goce_sueldo;
        $complemento_salarial = $filedat->complemento_salarial;
        $consulta_relacionada = $filedat->url_archivos;
        $RUBRO = $filedat->rubro;
        $FECHA_ALTA = $filedat->fecha_alta;
        $ST2 = $filedat->st2;
        $ALTA = $filedat->alta;
        $salario_acomulado = $filedat->salario_acumulado;
        $firma_digital = $filedat->firma_digital;

?>
        <div class="row">
            <div class="col-6">
                <div class="form-group">
                    <label for="">Fecha de incapacidad</label>
                    <input type="text" readonly class="form-control" name="" value="<?php echo date("d-m-Y", strtotime($fecha_consulta)) ?>" id="" aria-describedby="helpId" placeholder="">
                </div>
            </div>
            <div class="col-6">
                <div class="form-group">
                    <label for="">Folio incapacidad</label>
                    <input type="text" readonly class="form-control" name="" value="<?php echo $folio_incapacidad ?>" id="" aria-describedby="helpId" placeholder="">
                </div>
            </div>
            <div class="col-6">
                <div class="form-group">
                    <label for="">Ramo</label>
                    <input type="text" readonly class="form-control" name="" value="<?php echo $ramo ?>" id="" aria-describedby="helpId" placeholder="">
                </div>
            </div>
            <div class="col-6">
                <div class="form-group">
                    <label for="">Tipo de incapacidad</label>
                    <input type="text" readonly class="form-control" name="" value="<?php echo $tipo_incapacidad ?>" id="" aria-describedby="helpId" placeholder="">
                </div>
            </div>
            <div class="col-6">
                <div class="form-group">
                    <label for="">Fecha de inicio</label>
                    <input type="text" readonly class="form-control" name="" value="<?php echo $fecha_inicio ?>" id="" aria-describedby="helpId" placeholder="">
                </div>
            </div>
            <div class="col-6">
                <div class="form-group">
                    <label for="">Fecha de termino</label>
                    <input type="text" readonly class="form-control" name="" value="<?php echo $fecha_termino ?>" id="" aria-describedby="helpId" placeholder="">
                </div>
            </div>
            <div class="col-6">
                <div class="form-group">
                    <label for="">Dias autorizados</label>
                    <input type="text" readonly class="form-control" name="" value="<?php echo $dias_autorizados ?>" id="" aria-describedby="helpId" placeholder="">
                </div>
            </div>
            <div class="col-6">
                <div class="form-group">
                    <label for="">Salario integrado</label>
                    <input type="text" readonly class="form-control" name="" value="<?php echo $salario_integrado ?>" id="" aria-describedby="helpId" placeholder="">
                </div>
            </div>
            <div class="col-6">
                <div class="form-group">
                    <label for="">Costo</label>
                    <input type="text" readonly class="form-control" name="" value="<?php echo $costo ?>" id="" aria-describedby="helpId" placeholder="">
                </div>
            </div>
            <div class="col-6">
                <div class="form-group">
                    <label for="">Imputable</label>
                    <input type="text" readonly class="form-control" name="" value="<?php echo $imputable ?>" id="" aria-describedby="helpId" placeholder="">
                </div>
            </div>
            <div class="col-6">
                <div class="form-group">
                    <label for="">Estado del dictamen</label>
                    <input type="text" readonly class="form-control" name="" value="<?php echo $estado_dictamen ?>" id="" aria-describedby="helpId" placeholder="">
                </div>
            </div>
            <div class="col-6">
                <div class="form-group">
                    <label for="">Goce de sueldo</label>
                    <input type="text" readonly class="form-control" name="" value="<?php if ($goce_sueldo == 1) {
                                                                                        echo "Si";
                                                                                    } else {
                                                                                        echo "No";
                                                                                    }
                                                                                    ?>" id="" aria-describedby="helpId" placeholder="">
                </div>
            </div>
            <div class="col-6">
                <div class="form-group">
                    <label for="">Complemento salarial</label>
                    <input type="text" readonly class="form-control" name="" value="<?php echo $complemento_salarial ?>" id="" aria-describedby="helpId" placeholder="">
                </div>
            </div>
            <div class="col-6">
                <div class="form-group">
                    <label for="">Rubro</label>
                    <input type="text" readonly class="form-control" name="" value="<?php echo $RUBRO ?>" id="" aria-describedby="helpId" placeholder="">
                </div>
            </div>
            <div class="col-6">
                <div class="form-group">
                    <label for="">Fecha de alta</label>
                    <input type="text" readonly class="form-control" name="" value="<?php echo $FECHA_ALTA ?>" id="" aria-describedby="helpId" placeholder="">
                </div>
            </div>
            <div class="col-6">
                <div class="form-group">
                    <label for="">ST2</label>
                    <input type="text" readonly class="form-control" name="" value="<?php echo $ST2 ?>" id="" aria-describedby="helpId" placeholder="">
                </div>
            </div>
            <div class="col-6">
                <div class="form-group">
                    <label for="">Alta</label>
                    <input type="text" readonly class="form-control" name="" value="<?php echo $ALTA ?>" id="" aria-describedby="helpId" placeholder="">
                </div>
            </div>
            <div class="col-6">
                <div class="form-group">
                    <label for="">Salario acomulado</label>
                    <input type="text" readonly class="form-control" name="" value="<?php echo $salario_acomulado ?>" id="" aria-describedby="helpId" placeholder="">
                </div>
            </div>
            <div class="col-12 mt-3">
                <center>
                    <h3>Archivos adjuntos</h3>
                </center>
                <br>
                <div class="row">

                </div>
                <?php
                $query = $model->showFilesConsult($consulta_relacionada);
                while ($row = mysqli_fetch_array($query)) {


                ?>
                    <div class="col-6 mb-3">
                        <textarea name="" style="display: none;" id="text<?php echo $row[0]; ?>" cols="30" rows="10"><?php echo $row[4]; ?></textarea>
                        <button type="button" name="" onclick="downloadPDFCons('<?php echo $row[0]; ?>')" id="" class="btn btn-info">Descargar archivo <i class="fas fa-file-pdf"></i></button>
                    </div>

                <?php
                }

                ?>
                <br>
                <hr>
                <!-- <center>
                    <h4>Firma del paciente</h4>
                    <hr>
                    <img src="" class="img-fluid" alt="">
                </center> -->
            </div>


        </div>
<?php
        break;
    }
}
?>