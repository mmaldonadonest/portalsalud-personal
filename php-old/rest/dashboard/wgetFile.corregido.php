<?php
/* =============================================================================
 * wgetFile.php — versión corregida v3 (23-sep-2026)
 * Descarga de un archivo de `files` (examen medico) del portal PHP v2.
 *
 * SINTOMAS QUE CORRIGE
 *   v1 (original): en algunas PCs, "net::ERR_CONNECTION_RESET" desde el visor de PDF
 *                  de Chrome (chrome-extension://mhjfbmdgcf...) y, despues, archivo de
 *                  0 bytes con "la conexion se interrumpio".
 *
 * CAUSA PRINCIPAL (v3): el camino original hacia CUATRO copias del archivo en memoria
 *   para un solo PDF:  mysqli_fetch_array (devuelve las columnas dos veces, numerica y
 *   asociativa) -> json_encode (escapa y copia) -> json_decode (otra copia) ->
 *   base64_decode (otra). Un PDF de 5 MB llega a ~40 MB de RAM; si memory_limit es
 *   32M/64M, PHP muere a media respuesta y el navegador guarda 0 bytes con
 *   "conexion interrumpida" (el fatal no se ve porque display_errors esta apagado).
 *   Ahora se consulta la fila directamente y se decodifica EN LA BASE con FROM_BASE64(),
 *   asi PHP recibe el binario una sola vez.
 *
 * OTROS ARREGLOS RESPECTO DEL ORIGINAL
 *   - display_errors apagado (antes los warnings se imprimian DENTRO del PDF).
 *   - Content-Length real; y si el servidor comprime y no se puede apagar, no se manda
 *     (anunciar un tamaño distinto al recibido es lo que corta la descarga).
 *   - Ya no escribe el archivo al directorio web.
 *   - Content-Disposition: attachment por defecto (no pasa por el visor de PDF);
 *     ?disp=inline para previsualizar.
 *   - filename + filename* (RFC 5987): nombres con acentos ya no salen con %20.
 *   - `data` validado como entero; 404 con texto claro si no existe.
 *   - Extension por magic-bytes cuando el nombre no la trae.
 *   - Salida en bloques de 64 KB.
 *
 * DIAGNOSTICO
 *   wgetFile.php?data=<id>&debug=1  -> JSON con lo que ve el servidor, sin mandar el
 *   binario (tamaños, primeros bytes, memoria, limites, version de MySQL/PHP).
 *
 * Despliegue: respaldar el wgetFile.php actual y subir este con ese nombre.
 * ============================================================================= */

require_once('../../app/app.php');

// --- Errores al log, NUNCA a la salida (corromperian el binario) -------------
error_reporting(E_ALL);
ini_set('display_errors', '0');
ini_set('log_errors', '1');

// --- Memoria y tiempo: un PDF grande y su decodificacion necesitan holgura ---
@ini_set('memory_limit', '512M');
@set_time_limit(300);

// --- Sin compresion ni buffers pendientes en una respuesta binaria -----------
@ini_set('zlib.output_compression', 'Off');
if (function_exists('apache_setenv')) {
    @apache_setenv('no-gzip', '1');
}
$compresion = (string) ini_get('zlib.output_compression');
$compresionActiva = ($compresion !== '' && $compresion !== '0' && strtolower($compresion) !== 'off');
while (ob_get_level() > 0) {
    ob_end_clean();
}

function salirConTexto($estado, $mensaje)
{
    header('HTTP/1.1 ' . $estado);
    header('Content-Type: text/plain; charset=UTF-8');
    echo $mensaje;
    exit;
}

if (strtoupper($_SERVER['REQUEST_METHOD']) !== 'GET') {
    header('Allow: GET');
    salirConTexto('405 Method Not Allowed', 'Metodo no permitido.');
}

// `data` es el id de `files` (llave primaria): debe ser entero.
$datas = isset($_GET['data']) ? trim($_GET['data']) : '';
if ($datas === '' || !ctype_digit($datas)) {
    salirConTexto('400 Bad Request', 'Parametro data invalido.');
}
$id = (int) $datas;
$debug = (isset($_GET['debug']) && $_GET['debug'] === '1');

$model = new app;
$conn = $model->conn;
if (!$conn) {
    error_log('[wgetFile] sin conexion a la base');
    salirConTexto('500 Internal Server Error', 'Sin conexion a la base de datos.');
}

/* Decodificacion EN LA BASE (FROM_BASE64, MySQL >= 5.6 / MariaDB >= 10.0.5):
   PHP recibe el binario ya decodificado, una sola vez, sin pasar por JSON.
   SUBSTRING_INDEX(...,'base64,',-1) quita el prefijo "data:application/pdf;base64,"
   si lo hubiera (si no esta, devuelve la cadena completa). */
$sqlBase = "SELECT name, type, date_upload, LENGTH(url) AS largo_b64, %s AS bin
              FROM `files`
             WHERE id = " . $id . " AND type = 'examen_medico'";
$expresionBin = "FROM_BASE64(REPLACE(REPLACE(SUBSTRING_INDEX(url,'base64,',-1), '\\n', ''), '\\r', ''))";

$decodificadoEn = 'mysql';
$res = @mysqli_query($conn, sprintf($sqlBase, $expresionBin));
if ($res === false) {
    // MySQL viejo sin FROM_BASE64: se trae el base64 y se decodifica en PHP.
    $decodificadoEn = 'php';
    $res = mysqli_query($conn, sprintf($sqlBase, 'url'));
    if ($res === false) {
        error_log('[wgetFile] error de consulta para id=' . $id . ': ' . mysqli_error($conn));
        salirConTexto('500 Internal Server Error', 'Error al consultar el archivo.');
    }
}
$fila = mysqli_fetch_assoc($res);
mysqli_free_result($res);

if ($fila === null || $fila === false) {
    salirConTexto('404 Not Found', 'No se encontro el archivo solicitado (id ' . $id . ').');
}

$decoded = isset($fila['bin']) ? $fila['bin'] : null;
if ($decodificadoEn === 'php' && $decoded !== null) {
    $crudo = (string) $decoded;
    $pos = strpos($crudo, 'base64,');
    if ($pos !== false) {
        $crudo = substr($crudo, $pos + 7);
    }
    $crudo = preg_replace('/\s+/', '', $crudo);
    $decoded = base64_decode($crudo, true);
    if ($decoded === false) {
        $decoded = base64_decode($crudo);   // tolerante: ignora caracteres invalidos
    }
    unset($crudo);
}
// FROM_BASE64 devuelve NULL si el contenido no es base64 valido: se reintenta en PHP.
if ($decoded === null && $decodificadoEn === 'mysql') {
    $res2 = mysqli_query($conn, sprintf($sqlBase, 'url'));
    if ($res2 !== false) {
        $fila2 = mysqli_fetch_assoc($res2);
        mysqli_free_result($res2);
        if ($fila2 !== null && $fila2 !== false) {
            $crudo = (string) $fila2['bin'];
            $pos = strpos($crudo, 'base64,');
            if ($pos !== false) {
                $crudo = substr($crudo, $pos + 7);
            }
            $decoded = base64_decode(preg_replace('/\s+/', '', $crudo));
            $decodificadoEn = 'php (respaldo)';
            unset($crudo, $fila2);
        }
    }
}

$nombre = isset($fila['name']) ? (string) $fila['name'] : ('archivo-' . $id);
$largoBin = ($decoded === false || $decoded === null) ? 0 : strlen($decoded);

// --- Modo diagnostico: no manda el binario ----------------------------------
if ($debug) {
    header('Content-Type: application/json; charset=UTF-8');
    echo json_encode(array(
        'id'                 => $id,
        'name'               => $nombre,
        'type'               => isset($fila['type']) ? $fila['type'] : null,
        'date_upload'        => isset($fila['date_upload']) ? $fila['date_upload'] : null,
        'largo_base64'       => isset($fila['largo_b64']) ? (int) $fila['largo_b64'] : null,
        'largo_decodificado' => $largoBin,
        'decodificado_en'    => $decodificadoEn,
        'primeros_bytes_hex' => $largoBin > 0 ? strtoupper(bin2hex(substr($decoded, 0, 8))) : null,
        'es_pdf'             => ($largoBin > 3 && substr($decoded, 0, 4) === '%PDF'),
        'memory_limit'       => ini_get('memory_limit'),
        'memoria_pico_mb'    => round(memory_get_peak_usage(true) / 1048576, 1),
        'compresion_activa'  => $compresionActiva,
        'php'                => PHP_VERSION,
        'mysql'              => mysqli_get_server_info($conn),
    ));
    exit;
}

if ($largoBin === 0) {
    error_log('[wgetFile] contenido vacio o base64 invalido en files.id=' . $id);
    salirConTexto('500 Internal Server Error', 'El archivo esta vacio o dañado en la base de datos.');
}

// --- Extension: la del nombre, o deducida del contenido ----------------------
$info = pathinfo($nombre);
$ext = isset($info['extension']) ? strtolower($info['extension']) : '';
if ($ext === '') {
    if (substr($decoded, 0, 4) === '%PDF') {
        $ext = 'pdf';
    } elseif (substr($decoded, 0, 4) === "\x89PNG") {
        $ext = 'png';
    } elseif (substr($decoded, 0, 3) === "\xFF\xD8\xFF") {
        $ext = 'jpg';
    }
    if ($ext !== '') {
        $nombre .= '.' . $ext;
    }
}

$tipos = array(
    'pdf'  => 'application/pdf',
    'png'  => 'image/png',
    'jpg'  => 'image/jpeg',
    'jpeg' => 'image/jpeg',
    'gif'  => 'image/gif',
    'tif'  => 'image/tiff',
    'tiff' => 'image/tiff',
);
$mime = isset($tipos[$ext]) ? $tipos[$ext] : 'application/octet-stream';

// Descarga directa por defecto (el boton dice "Descargar archivo"); ?disp=inline previsualiza.
$disposicion = (isset($_GET['disp']) && $_GET['disp'] === 'inline') ? 'inline' : 'attachment';

$nombreAscii = preg_replace('/[^A-Za-z0-9._-]/', '_', $nombre);
if ($nombreAscii === '' || $nombreAscii === null) {
    $nombreAscii = 'archivo-' . $id . ($ext !== '' ? '.' . $ext : '');
}

header('Content-Type: ' . $mime);
header('Content-Disposition: ' . $disposicion . '; filename="' . $nombreAscii . '"; '
       . "filename*=UTF-8''" . rawurlencode($nombre));
header('Content-Description: File Transfer');
header('Content-Transfer-Encoding: binary');
if (!$compresionActiva) {
    header('Content-Length: ' . $largoBin);
}
header('Cache-Control: private, max-age=0, must-revalidate');
header('Pragma: public');
header('X-Content-Type-Options: nosniff');
header('Accept-Ranges: none');

// Salida en bloques de 64 KB.
for ($i = 0; $i < $largoBin; $i += 65536) {
    echo substr($decoded, $i, 65536);
    flush();
}
exit;
