<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Max-Age: 3600");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");
require_once('../../app/app.php');
require_once('../../app/templates.php');
$model = new app;
$requestMethod = $_SERVER["REQUEST_METHOD"];
$requestMethod = strtoupper($requestMethod);
switch ($requestMethod) {
    case 'POST':
        session_start();
        $datas = (isset($_POST['data']) ? $_POST['data'] : null);
        $json =  $model->searchUser($datas);
        $data = json_decode($json);
        foreach ($data->Datos as $user) {
            $nombre = $user->nombre;
            $apellidoP = $user->apellidoPaterno;
            $apellidoM = $user->apellidoMaterno;
            $RFC = $user->rfc;
            $NSS = $user->nss;
            $sexo = $user->direccion;
            $cuenta = $user->cuenta;
            $fechaNacimiento = $user->fecha_nacimiento;
            $edoCivil = $user->estado_civil;
            $direccion = $user->sexo;
            $telefono = $user->celular;
            $turno = $user->turno;
            $rfc = $user->rfc;
            $telefonoF = $user->tel_fijo;
            $check = $user->completo;
            $puesto = $user->nombre_puesto;
            $empresa = $user->nombre_empresa;
        }
        if ($check == "Sin Registro de Usuario") {
            $model->CheckEmploye($NSS, "CANDIDATO");
        } else {
            $model->CheckEmploye($NSS, "EMPLEADO");
        }


        if ($check == "Sin Registro de Usuario") {

            $jsonP =  $model->searchProspecto($datas);
            $dataP = json_decode($jsonP);
            foreach ($dataP->Datos as $user) {
                $nombre = $user->nombre;
                $curp = $user->curp;
                $fecha_inicio_proceso = $user->fecha_inicio_proceso;
                $estado_candidato = $user->estado_candidato;
                $mensaje = $user->mensaje;
                $status = $user->status;
            }
            if ($status == 0) {
                $template = new Template("../../view/content/candidato.html", $data = [
                    "curp" => $curp,
                    "nombre" => $nombre,
                    "fechaIngreso" => $fecha_inicio_proceso,
                    "status" => $estado_candidato,


                ]);
            } else {
                $template = new Template("../../view/content/searcherror.html", $data = [
                    "key" => $check
                ]);
            }
        } else {
            // Extraer año, mes y día de nacimiento del RFC
            $anio = substr($rfc, 4, 2);
            $mes = substr($rfc, 6, 2);
            $dia = substr($rfc, 8, 2);

            // Asumiendo que todos los años están en el siglo 20, puedes agregar el prefijo "19" a los años menores de 50
            if ($anio > 50) {
                $anio = "19" . $anio;
            } else {
                $anio = "20" . $anio;
            }

            // Formar la fecha de nacimiento en formato estándar
            $fecha_nacimiento = $anio . "-" . $mes . "-" . $dia;

            // Convertir la fecha de nacimiento a un timestamp
            $fecha_timestamp = strtotime($fecha_nacimiento);

            // Calcular la edad
            $edad = date('Y') - date('Y', $fecha_timestamp);

            // Ajustar la edad si todavía no ha pasado el cumpleaños de este año
            if (date('md', $fecha_timestamp) > date('md')) {
                $edad--;
            }

            $template = new Template("../../view/content/examenMedico.html", $data = [
                "nombre" => $nombre,
                "aPaterno" => $apellidoP,
                "aMaterno" => $apellidoM,
                "cuenta" => $cuenta,
                "fechaNacimiento" => $fechaNacimiento,
                "sexo" => $sexo,
                "edoCivil" => $edoCivil,
                "direccion" => $direccion,
                "telefono" => $telefono,
                "turno" => $turno,
                "rfc" => $rfc,
                "telefonoF" => $telefonoF,
                "puesto" => $puesto,
                "empresa" => $empresa,
                "edad" => $edad

            ]);
        }


        echo $template;

        break;

    default:


        break;
}
