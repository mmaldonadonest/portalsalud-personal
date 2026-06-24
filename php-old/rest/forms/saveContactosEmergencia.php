<?php
require_once('../../app/app.php');
$view = new app();
$others = new another();
$nss = (isset($_POST['nss']) ? $_POST['nss'] : null);
$nombre1 = (isset($_POST['nombre1']) ? $_POST['nombre1'] : null);
$apellido_paterno1 = (isset($_POST['apellido_paterno1']) ? $_POST['apellido_paterno1'] : null);
$apellido_materno1 = (isset($_POST['apellido_materno1']) ? $_POST['apellido_materno1'] : null);
$parentesco1 = (isset($_POST['parentesco1']) ? $_POST['parentesco1'] : null);
$telefono1 = (isset($_POST['telefono1']) ? $_POST['telefono1'] : null);
$nombre2 = (isset($_POST['nombre2']) ? $_POST['nombre2'] : null);
$apellido_paterno2 = (isset($_POST['apellido_paterno2']) ? $_POST['apellido_paterno2'] : null);
$apellido_materno2 = (isset($_POST['apellido_materno2']) ? $_POST['apellido_materno2'] : null);
$parentesco2 = (isset($_POST['parentesco2']) ? $_POST['parentesco2'] : null);
$telefono2 = (isset($_POST['telefono2']) ? $_POST['telefono2'] : null);
$nombre3 = (isset($_POST['nombre3']) ? $_POST['nombre3'] : null);
$apellido_paterno3 = (isset($_POST['apellido_paterno3']) ? $_POST['apellido_paterno3'] : null);
$apellido_materno3 = (isset($_POST['apellido_materno3']) ? $_POST['apellido_materno3'] : null);
$parentesco3 = (isset($_POST['parentesco3']) ? $_POST['parentesco3'] : null);
$telefono3 = (isset($_POST['telefono3']) ? $_POST['telefono3'] : null);
$cadaCuandoDep = (isset($_POST['cadaCuandoDep']) ? $_POST['cadaCuandoDep'] : null);
$cadaCuandoPasa = (isset($_POST['cadaCuandoPasa']) ? $_POST['cadaCuandoPasa'] : null);
$fechaINFLUEN = (isset($_POST['fechaINFLUEN']) ? $_POST['fechaINFLUEN'] : null);

$ODEXFISINPCERC = (isset($_POST['ODEXFISINPCERC']) ? $_POST['ODEXFISINPCERC'] : null);
$ODEXFISINPCERCIZQ = (isset($_POST['ODEXFISINPCERCIZQ']) ? $_POST['ODEXFISINPCERCIZQ'] : null);




$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $nombre1,
    "name" => "contactoEmernombre1"
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $apellido_paterno1,
    "name" => "contactoEmerapellido_paterno1"
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $apellido_materno1,
    "name" => "contactoEmerapellido_materno1"
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $parentesco1,
    "name" => "contactoEmerparentesco1"
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $telefono1,
    "name" => "contactoEmertelefono1"
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $nombre2,
    "name" => "contactoEmernombre2"
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $apellido_paterno2,
    "name" => "contactoEmerapellido_paterno2"
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $apellido_materno2,
    "name" => "contactoEmerapellido_materno2"
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $parentesco2,
    "name" => "contactoEmerparentesco2"
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $telefono2,
    "name" => "contactoEmertelefono2"
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $nombre3,
    "name" => "contactoEmernombre3"
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $apellido_paterno3,
    "name" => "contactoEmerapellido_paterno3"
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $apellido_materno3,
    "name" => "contactoEmerapellido_materno3"
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $parentesco3,
    "name" => "contactoEmerparentesco3"
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $telefono3,
    "name" => "contactoEmertelefono3"
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $cadaCuandoDep,
    "name" => "cadaCuandoDep"
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $cadaCuandoPasa,
    "name" => "cadaCuandoPasa"
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $fechaINFLUEN,
    "name" => "fechaINFLUEN"
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $ODEXFISINPCERC,
    "name" => "ODEXFISINPCERC"
]);
$others->insertDatsExpFis($data = [
    "nss" => $nss,
    "content" => $ODEXFISINPCERCIZQ,
    "name" => "ODEXFISINPCERCIZQ"
]);



echo "Contactos de emergencia guardados";
