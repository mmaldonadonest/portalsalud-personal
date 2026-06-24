<?php
require_once('../../app/app.php');
$model = new app;
$requestMethod = $_SERVER["REQUEST_METHOD"];
$requestMethod = strtoupper($requestMethod);
$datas = (isset($_POST['data']) ? $_POST['data'] : null);
$serie = (isset($_POST['serieId']) ? $_POST['serieId'] : null);
$json = $model->showConsults($datas);
$data = json_decode($json);

foreach ($data->Datos as $filedat) {

    if ($filedat->id_consulta == $serie) {
        $fecha_consulta = $filedat->fecha_consulta;
        $tipo_consulta = $filedat->tipo_consulta;
        $area_accidente = $filedat->area_accidente;
        $area_involucrada = $filedat->area_involucrada;
        $causa = $filedat->causa;
        $peso = $filedat->peso;
        $talla = $filedat->talla;
        $imc = $filedat->imc;
        $fc = $filedat->fc;
        $fr = $filedat->fr;
        $ta = $filedat->ta;
        $temperatura = $filedat->temperatura;
        $motivo = $filedat->motivo;
        $exploracion = $filedat->exploracion;
        $diagnostico = $filedat->diagnostico;
        $tratamiento = $filedat->tratamiento;
        $consulta_relacionada = $filedat->consulta_relacionada;
        $firma_digital = $filedat->firma_digital;

?>
        <div class="row">
            <div class="col-6">
                <div class="form-group">
                    <label for="">Tipo de consulta</label>
                    <input type="text" readonly class="form-control" name="" value="<?php echo $tipo_consulta ?>" id="" aria-describedby="helpId" placeholder="">
                </div>
            </div>
            <div class="col-6">
                <div class="form-group">
                    <label for="">Area accidente</label>
                    <input type="text" readonly class="form-control" name="" value="<?php echo $area_accidente ?>" id="" aria-describedby="helpId" placeholder="">
                </div>
            </div>
            <div class="col-6">
                <div class="form-group">
                    <label for="">Area anatomica</label>
                    <input type="text" readonly class="form-control" name="" value="<?php echo $area_involucrada ?>" id="" aria-describedby="helpId" placeholder="">
                </div>
            </div>
            <div class="col-6">
                <div class="form-group">
                    <label for="">Causa</label>
                    <input type="text" readonly class="form-control" name="" value="<?php echo $causa ?>" id="" aria-describedby="helpId" placeholder="">
                </div>
            </div>
            <div class="col-6">
                <div class="form-group">
                    <label for="">Peso</label>
                    <input type="text" readonly class="form-control" name="" value="<?php echo $peso ?>" id="" aria-describedby="helpId" placeholder="">
                </div>
            </div>
            <div class="col-6">
                <div class="form-group">
                    <label for="">Talla</label>
                    <input type="text" readonly class="form-control" name="" value="<?php echo $talla ?>" id="" aria-describedby="helpId" placeholder="">
                </div>
            </div>
            <div class="col-6">
                <div class="form-group">
                    <label for="">IMC</label>
                    <input type="text" readonly class="form-control" name="" value="<?php echo $imc ?>" id="" aria-describedby="helpId" placeholder="">
                </div>
            </div>
            <div class="col-6">
                <div class="form-group">
                    <label for="">FC</label>
                    <input type="text" readonly class="form-control" name="" value="<?php echo $fc ?>" id="" aria-describedby="helpId" placeholder="">
                </div>
            </div>
            <div class="col-6">
                <div class="form-group">
                    <label for="">FR</label>
                    <input type="text" readonly class="form-control" name="" value="<?php echo $fr ?>" id="" aria-describedby="helpId" placeholder="">
                </div>
            </div>
            <div class="col-6">
                <div class="form-group">
                    <label for="">TA</label>
                    <input type="text" readonly class="form-control" name="" value="<?php echo $ta ?>" id="" aria-describedby="helpId" placeholder="">
                </div>
            </div>
            <div class="col-6">
                <div class="form-group">
                    <label for="">Temperatura</label>
                    <input type="text" readonly class="form-control" name="" value="<?php echo $temperatura ?>" id="" aria-describedby="helpId" placeholder="">
                </div>
            </div>
            <div class="col-6">
                <div class="form-group">
                    <label for="">Motivo de consulta</label>
                    <input type="text" readonly class="form-control" name="" value="<?php echo $motivo ?>" id="" aria-describedby="helpId" placeholder="">
                </div>
            </div>
            <div class="col-6">
                <div class="form-group">
                    <label for="">Exploración fisica</label>
                    <input type="text" readonly class="form-control" name="" value="<?php echo $exploracion ?>" id="" aria-describedby="helpId" placeholder="">
                </div>
            </div>
            <div class="col-6">
                <div class="form-group">
                    <label for="">Diagnostico</label>
                    <input type="text" readonly class="form-control" name="" value="<?php echo $diagnostico ?>" id="" aria-describedby="helpId" placeholder="">
                </div>
            </div>
            <div class="col-6">
                <div class="form-group">
                    <label for="">Tratamiento</label>
                    <input type="text" readonly class="form-control" name="" value="<?php echo $tratamiento ?>" id="" aria-describedby="helpId" placeholder="">
                </div>
            </div>
            <div class="col-12 mt-3">
                <center>
                    <h3>Archivos adjuntos</h3>
                </center>
                <br>

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
                <center>
                    <h4>Firma del paciente</h4>
                    <hr>
                    <img src="<?= $firma_digital ?>" class="img-fluid" alt="">
                </center>

            </div>


        </div>
<?php
        break;
    }
}
?>