<?php
// Cadena de caracteres que deseas comprimir
$cadena = "Esta es una cadena que quiero comprimir. Esta cadena es bastante larga y repetitiva para demostrar la compresión.";

// Comprimir la cadena utilizando gzcompress
$cadena_comprimida = gzcompress($cadena);

// Imprimir la longitud de la cadena original y la comprimida
echo "Longitud de la cadena original: " . strlen($cadena) . " bytes<br>";
echo "Longitud de la cadena comprimida: " . strlen($cadena_comprimida) . " bytes<br>";

// Para descomprimir la cadena, puedes utilizar gzuncompress
$cadena_descomprimida = gzuncompress($cadena_comprimida);

// Imprimir la cadena descomprimida
echo "Cadena descomprimida:<br>$cadena_descomprimida";
?>