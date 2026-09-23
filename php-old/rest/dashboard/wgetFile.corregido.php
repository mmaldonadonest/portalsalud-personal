<?php
/* =============================================================================
 * wgetFile.php — versión corregida (23-sep-2026)
 * Descarga de un archivo de `files` (examen medico) del portal PHP v2.
 *
 * Reemplaza a la versión original, que provocaba en algunas PCs:
 *    GET chrome-extension://mhjfbmdgcfjbbpaeojofohoefgiehjai/... net::ERR_CONNECTION_RESET
 * (mhjfbmdgcf... es el visor de PDF integrado de Chrome: recibía un flujo cuyo
 *  Content-Length no correspondía al cuerpo enviado y cortaba la conexión).
 *
 * Qué se corrigió respecto del original:
 *   1. display_errors apagado: cualquier warning de PHP se imprimía DENTRO del PDF
 *      y lo corrompía. Ahora los errores van al log del servidor.
 *   2. Content-Length = strlen($decoded) (bytes realmente enviados). Antes era
 *      filesize($file) sobre un archivo recién escrito: si la escritura fallaba por
 *      permisos, filesize() devolvía false -> cabecera vacía -> flujo cortado.
 *   3. Ya NO escribe el archivo al directorio web (era innecesario y además dejaba
 *      archivos con nombre venido de la BD dentro del docroot).
 *   4. Se limpian los buffers de salida y se desactiva la compresión antes de mandar
 *      bytes binarios (un gzip a medias también resetea la conexión).
 *   5. Content-Disposition: attachment por defecto -> descarga directa, sin pasar por
 *      el visor de PDF (que es donde fallaba). Para previsualizar en el navegador:
 *      wgetFile.php?data=123&disp=inline
 *   6. Nombre de archivo con filename + filename* (RFC 5987). El urlencode() anterior
 *      mostraba nombres con %20 y rompía los ~96 nombres con acentos mal codificados.
 *   7. `data` se valida como entero antes de llegar al SQL, y si no hay registro se
 *      responde 404 con texto claro en vez de un 200 vacío.
 *   8. Extensión resuelta por magic-bytes cuando el nombre no la trae (3 casos
 *      conocidos en la tabla `files`), igual que hace el portal Java.
 *
 * Despliegue: respaldar el wgetFile.php actual y subir éste con ese nombre.
 * ============================================================================= */

require_once('../../app/app.php');

// 1. Errores al log, NUNCA a la salida (corromperían el binario).
error_reporting(E_ALL);
ini_set('display_errors', '0');
ini_set('log_errors', '1');

// 4. Sin compresión ni buffers pendientes en una respuesta binaria.
ini_set('zlib.output_compression', 'Off');
while (ob_get_level() > 0) {
    ob_end_clean();
}

if (strtoupper($_SERVER['REQUEST_METHOD']) !== 'GET') {
    header('HTTP/1.1 405 Method Not Allowed');
    header('Allow: GET');
    header('Content-Type: text/plain; charset=UTF-8');
    echo 'Metodo no permitido.';
    exit;
}

// 7. El parametro `data` es el id de `files`: debe ser entero.
$datas = isset($_GET['data']) ? trim($_GET['data']) : '';
if ($datas === '' || !ctype_digit($datas)) {
    header('HTTP/1.1 400 Bad Request');
    header('Content-Type: text/plain; charset=UTF-8');
    echo 'Parametro data invalido.';
    exit;
}

$model = new app;
$json = $model->getFileExMedPdf($datas);
$data = json_decode($json);

if (!isset($data->response) || count($data->response) === 0) {
    header('HTTP/1.1 404 Not Found');
    header('Content-Type: text/plain; charset=UTF-8');
    echo 'No se encontro el archivo solicitado (id ' . $datas . ').';
    exit;
}

// Se envia UN archivo por peticion (id es llave primaria). Si por algun motivo
// vinieran varios, concatenarlos produciria un PDF corrupto: se toma el primero.
$filedat = $data->response[0];

$decoded = base64_decode($filedat->url, true);
if ($decoded === false || $decoded === '') {
    error_log('[wgetFile] base64 invalido o vacio para files.id=' . $datas);
    header('HTTP/1.1 500 Internal Server Error');
    header('Content-Type: text/plain; charset=UTF-8');
    echo 'El archivo esta vacio o dañado en la base de datos.';
    exit;
}

// 8. Extension: la del nombre, o deducida del contenido si no la trae.
$nombre = isset($filedat->name) ? (string) $filedat->name : ('archivo-' . $datas);
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

// 5. Descarga directa por defecto (el boton dice "Descargar archivo"); `disp=inline`
//    para previsualizar. El visor de PDF de Chrome solo entra en juego con inline.
$disposicion = (isset($_GET['disp']) && $_GET['disp'] === 'inline') ? 'inline' : 'attachment';

// 6. Nombre seguro para la cabecera: ASCII de respaldo + filename* con UTF-8.
$nombreAscii = preg_replace('/[^A-Za-z0-9._-]/', '_', $nombre);
if ($nombreAscii === '' || $nombreAscii === null) {
    $nombreAscii = 'archivo-' . $datas . ($ext !== '' ? '.' . $ext : '');
}

header('Content-Type: ' . $mime);
header('Content-Disposition: ' . $disposicion . '; filename="' . $nombreAscii . '"; '
       . "filename*=UTF-8''" . rawurlencode($nombre));
header('Content-Description: File Transfer');
header('Content-Transfer-Encoding: binary');
// 2. El tamaño real de lo que se envia.
header('Content-Length: ' . strlen($decoded));
header('Cache-Control: private, max-age=0, must-revalidate');
header('Pragma: public');
header('X-Content-Type-Options: nosniff');
header('Accept-Ranges: none');

echo $decoded;
exit;
