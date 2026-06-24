<?php
require_once('../../app/app.php');

error_reporting(E_ALL);
ini_set('display_errors', 1);

$model = new app;
$requestMethod = $_SERVER["REQUEST_METHOD"];
$requestMethod = strtoupper($requestMethod);
switch ($requestMethod) {
    case 'GET':
        $datas = (isset($_GET['data']) ? $_GET['data'] : null);
        $json = $model->getFileExMedPdf($datas);
        $data = json_decode($json);	
	foreach ($data->response as $filedat) {
		$decoded = base64_decode($filedat->url);
		$file = $filedat->name;
		
		file_put_contents($file, $decoded);
		//
		$info = pathinfo($file);
		if($info["extension"] == "pdf"){
                  header("Content-Type: application/pdf");		
		} else {
		  header("Content-Type: application/octet-stream");			
		
		}	
		//$file = $_GET["file"] .".pdf";
		header("Content-Disposition: inline; filename=" . urlencode($file));   
//		header("Content-Type: application/octet-stream");
//		header("Content-Type: application/download");
		header("Content-Description: File Transfer");            
		header("Content-Length: " . filesize($file));
		echo $decoded;
		/*flush(); // this doesn't really matter.
		$fp = fopen($file, "r");
		while (!feof($fp))
		{
    		    echo fread($fp, 65536);
		    flush(); // this is essential for large downloads
		} 
		fclose($fp); 		*/


        }
       
        break;

    default:
	# code...
	#
        break;
}
                      
