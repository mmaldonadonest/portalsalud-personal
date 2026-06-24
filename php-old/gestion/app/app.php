<?php
class appGestion
{
    public function contarApps()
    {
        $ch = curl_init();
        $headers  = [
            'x-api-key: XXXXXX',
            'Content-Type: application/json; charset=utf-8'
        ];
        curl_setopt($ch, CURLOPT_URL, "http://10.249.249.3/biows/ords/security/info/apps_onest");
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
        $result     = curl_exec($ch);
        $statusCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        $data = json_decode($result);
        $maxIdApp = null;
        foreach ($data->Datos as $user) {
            if ($maxIdApp === null || $user->id_app > $maxIdApp) {
                $maxIdApp = $user->id_app;
            }
        }
        $maxIdApp = $maxIdApp + 1;
        return $maxIdApp;
    }
    public function altaApp($idApp, $nombreApp)
    {
        $ch = curl_init();
        $headers  = [
            'x-api-key: XXXXXX',
            'Content-Type: application/json; charset=utf-8'
        ];
        $postData = [
            "id_app" => $idApp,
            "id_menu" => 1,
            "nombre_menu" => "$nombreApp",
            "id_rol" => 1,
            "tipo_mov" => "ALTA",
            "servicio" => "APP"
        ];

        curl_setopt($ch, CURLOPT_URL, "http://10.249.249.3/biows/ords/security/info/crud_alta_app_menu");
        curl_setopt($ch, CURLOPT_POST, 1);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($postData));
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
        $result     = curl_exec($ch);
        $statusCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        return $result;
    }
    public function buscarIdAp($targetIdApp)
    {
        $ch = curl_init();
        $headers  = [
            'x-api-key: XXXXXX',
            'Content-Type: application/json; charset=utf-8'
        ];
        curl_setopt($ch, CURLOPT_URL, "http://10.249.249.3/biows/ords/security/info/apps_onest");
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
        $result     = curl_exec($ch);
        $statusCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);

        // Decodificar el JSON a un array asociativo
        $data = json_decode($result, true);

        // ID de la aplicación que deseas seleccionar

        // Buscar el ID de la aplicación en el array de datos
        foreach ($data['Datos'] as $app) {
            if ($app['id_app'] == $targetIdApp) {
                // Encontraste la aplicación deseada
                $selectedApp = $app;
                break;
            }
        }

        // Verificar si se encontró la aplicación
        if (isset($selectedApp)) {
            // Crear un nuevo array con la aplicación seleccionada
            $resultArray = array('Resultado' => array(
                'Proceso' => 'Selección de ID_APP',
                'Estado' => 1000,
                'Mensaje' => 'Aplicación encontrada y seleccionada',
                'Datos' => array($selectedApp)
            ));

            // Convertir el array a JSON
            echo  $resultJson = json_encode($resultArray, JSON_PRETTY_PRINT);
        } else {
            // La aplicación no fue encontrada
            $resultArray = array('Resultado' => array(
                'Proceso' => 'Selección de ID_APP',
                'Estado' => 2000,
                'Mensaje' => 'Aplicación no encontrada'
            ));

            // Convertir el array a JSON
            echo $resultJson = json_encode($resultArray, JSON_PRETTY_PRINT);
        }
    }
    public function altaMenu($idApp, $idMenu, $nombreMenu, $idRol)
    {
        $ch = curl_init();
        $headers  = [
            'x-api-key: XXXXXX',
            'Content-Type: application/json; charset=utf-8'
        ];
        $postData = [
            "id_app" => $idApp,
            "id_menu" => $idMenu,
            "nombre_menu" => "$nombreMenu",
            "id_rol" => $idRol,
            "tipo_mov" => "ALTA",
            "servicio" => "MENU"
        ];

        curl_setopt($ch, CURLOPT_URL, "http://10.249.249.3/biows/ords/security/info/crud_alta_app_menu");
        curl_setopt($ch, CURLOPT_POST, 1);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($postData));
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
        $result     = curl_exec($ch);
        $statusCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        return $result;
    }
    public function altaRol($idApp, $idROL, $nombreROL)
    {
        $ch = curl_init();
        $headers  = [
            'x-api-key: XXXXXX',
            'Content-Type: application/json; charset=utf-8'
        ];
        $postData = [
            "id_app" => $idApp,
            "id_menu" => "1",
            "nombre_menu" => "$nombreROL",
            "id_rol" => $idROL,
            "tipo_mov" => "ALTA",
            "servicio" => "ROL"
        ];

        curl_setopt($ch, CURLOPT_URL, "http://10.249.249.3/biows/ords/security/info/crud_alta_app_menu");
        curl_setopt($ch, CURLOPT_POST, 1);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($postData));
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
        $result     = curl_exec($ch);
        $statusCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        return $result;
    }
    public function bajaApp($idApp)
    {
        $ch = curl_init();
        $headers  = [
            'x-api-key: XXXXXX',
            'Content-Type: application/json; charset=utf-8'
        ];
        $postData = [
            "id_app" => $idApp,
            "id_menu" => "1",
            "nombre_menu" => "1",
            "id_rol" => "1",
            "tipo_mov" => "BAJA",
            "servicio" => "APP"
        ];

        curl_setopt($ch, CURLOPT_URL, "http://10.249.249.3/biows/ords/security/info/crud_alta_app_menu");
        curl_setopt($ch, CURLOPT_POST, 1);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($postData));
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
        $result     = curl_exec($ch);
        $statusCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        return $result;
    }
    public function bajaRol($idApp, $idRol)
    {
        $ch = curl_init();
        $headers  = [
            'x-api-key: XXXXXX',
            'Content-Type: application/json; charset=utf-8'
        ];
        $postData = [
            "id_app" => $idApp,
            "id_menu" => "1",
            "nombre_menu" => "ADMIN",
            "id_rol" => $idRol,
            "tipo_mov" => "BAJA",
            "servicio" => "ROL"
        ];

        curl_setopt($ch, CURLOPT_URL, "http://10.249.249.3/biows/ords/security/info/crud_alta_app_menu");
        curl_setopt($ch, CURLOPT_POST, 1);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($postData));
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
        $result     = curl_exec($ch);
        $statusCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        return $result;
    }
    public function cambioApp($idApp,$nombreApp){
        $ch = curl_init();
        $headers  = [
            'x-api-key: XXXXXX',
            'Content-Type: application/json; charset=utf-8'
        ];
        $postData = [
            "id_app" => $idApp,
            "id_menu" => "1",
            "nombre_menu" => "$nombreApp",
            "id_rol" => "1",
            "tipo_mov" => "CAMBIO",
            "servicio" => "APP"
        ];

        curl_setopt($ch, CURLOPT_URL, "http://10.249.249.3/biows/ords/security/info/crud_alta_app_menu");
        curl_setopt($ch, CURLOPT_POST, 1);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($postData));
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
        $result     = curl_exec($ch);
        $statusCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        return $result;
    }
    public function traerMenus($idApp, $idRol){
        $ch = curl_init();
        $headers  = [
            'x-api-key: XXXXXX',
            'Content-Type: application/json; charset=utf-8'
        ];
        $postData = [
            "id_app" => $idApp,
            "id_rol" => $idRol,
           
        ];

        curl_setopt($ch, CURLOPT_URL, "http://10.249.249.3/biows/ords/security/info/app_menu");
        curl_setopt($ch, CURLOPT_POST, 1);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($postData));
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
        $result     = curl_exec($ch);
        $statusCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        return $result;
    }
    public function editRol($idApp, $idRol, $name){
        $ch = curl_init();
        $headers  = [
            'x-api-key: XXXXXX',
            'Content-Type: application/json; charset=utf-8'
        ];
        $postData = [
            "id_app" => $idApp,
            "id_menu" => "1",
            "nombre_menu" => "$name",
            "id_rol" => $idRol,
            "tipo_mov" => "CAMBIO",
            "servicio" => "ROL"
        ];

        curl_setopt($ch, CURLOPT_URL, "http://10.249.249.3/biows/ords/security/info/crud_alta_app_menu");
        curl_setopt($ch, CURLOPT_POST, 1);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($postData));
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
        $result     = curl_exec($ch);
        $statusCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        return $result;
    }
    public function editMenu($idApp, $idMenu, $nombreMenu, $idRol)
    {
        $ch = curl_init();
        $headers  = [
            'x-api-key: XXXXXX',
            'Content-Type: application/json; charset=utf-8'
        ];
        $postData = [
            "id_app" => $idApp,
            "id_menu" => $idMenu,
            "nombre_menu" => "$nombreMenu",
            "id_rol" => $idRol,
            "tipo_mov" => "CAMBIO",
            "servicio" => "MENU"
        ];

        curl_setopt($ch, CURLOPT_URL, "http://10.249.249.3/biows/ords/security/info/crud_alta_app_menu");
        curl_setopt($ch, CURLOPT_POST, 1);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($postData));
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
        $result     = curl_exec($ch);
        $statusCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        return $result;
    }
    public function bajaMenu($idApp, $idMenu, $idRol){
        $ch = curl_init();
        $headers  = [
            'x-api-key: XXXXXX',
            'Content-Type: application/json; charset=utf-8'
        ];
        $postData = [
            "id_app" => $idApp,
            "id_menu" => $idMenu,
            "nombre_menu" => "menu",
            "id_rol" => $idRol,
            "tipo_mov" => "BAJA",
            "servicio" => "MENU"
        ];

        curl_setopt($ch, CURLOPT_URL, "http://10.249.249.3/biows/ords/security/info/crud_alta_app_menu");
        curl_setopt($ch, CURLOPT_POST, 1);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($postData));
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
        $result     = curl_exec($ch);
        $statusCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        return $result;
    }

}
