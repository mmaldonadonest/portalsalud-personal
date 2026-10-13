@echo off
setlocal

rem ===========================================================================
rem  VALIDAR LAYOUTS POR NSS -- SIN BASE DE DATOS
rem
rem  Revisa los Excel de una carpeta contra los 5 layouts (1 ATENCIONES,
rem  2 EXAMEN NUEVO INGRESO, 3 EXAMEN PERIODICO, 5 INCAPACIDADES, 6 ACCIDENTES).
rem  NO se conecta a ninguna base: no lleva credenciales y no escribe nada.
rem
rem  Por cada archivo con errores deja junto a el un "<archivo>.errores.csv"
rem  (renglon, columna, valor, motivo) para regresarlo al predio.
rem
rem  Lo que NO revisa (necesita la base): que el NSS exista en el biometrico.
rem  Eso lo hace correr-nss-prod.local.bat en modo nss-validar.
rem ===========================================================================

rem --- EDITAR AQUI ------------------------------------------------------------
set "ORIGEN=PON_LA_CARPETA_CON_LOS_EXCEL"
set "JAVA_BIN=C:\oracle\jdk22\jdk-22.0.2\bin\java.exe"
rem ---------------------------------------------------------------------------

if "%ORIGEN%"=="PON_LA_CARPETA_CON_LOS_EXCEL" (
  echo  [ALTO] Falta poner la carpeta con los Excel en ORIGEN.
  pause & exit /b 1
)
if not exist "%ORIGEN%" (
  echo  [ALTO] No existe la carpeta: %ORIGEN%
  pause & exit /b 1
)
if not exist "%~dp0target\portal-salud-excel-loader.jar" (
  echo  [ALTO] Falta el JAR. Construirlo:  mvn -f excel-loader\pom.xml package
  pause & exit /b 1
)

rem  Sin EXCEL_TARGET_URL el programa no puede conectarse a ninguna base
set "EXCEL_TARGET_URL="
"%JAVA_BIN%" -Dfile.encoding=UTF-8 -jar "%~dp0target\portal-salud-excel-loader.jar" ^
  --excel.modo=nss-validar --excel.origen="%ORIGEN%"

if %ERRORLEVEL%==4 (
  echo.
  echo  [!] Hay archivos o renglones que no pasan. Ver los .errores.csv en la carpeta.
) else if %ERRORLEVEL% NEQ 0 (
  echo  [!] Termino con codigo %ERRORLEVEL%. Revisar el mensaje de arriba.
) else (
  echo  [ok] Todos los renglones pasan la validacion de formato y catalogos.
)
pause
endlocal
