-- =============================================================================
-- INDICE DE NOMBRES DEL BIOMETRICO -> archivo CSV local, para poner el NSS por nombre a la prueba
-- de TOLUCA enero 2025 (layouts generados desde los Excel originales de MORBILIDAD 2025).
--
-- BASE: BIOMETRICO de produccion (PDBPRD). Lo corre el usuario.
--
-- SOLO LECTURA: un SELECT. No crea, no modifica, no borra nada.
--
-- POR QUE TODO EL INDICE Y NO SOLO LOS NOMBRES DE TOLUCA: el cruce por nombre necesita la misma
--   normalizacion que el cargador (sin acentos, ambos ordenes "apellidos nombre" y "nombre
--   apellidos", espacios). Hacerlo en SQL seria otra regla distinta y fallaria en silencio. Es
--   exactamente lo que leia el generador de los lotes de prueba 2 a 6 (PrellenadoNss).
--   Sin los EMP_STATUS 99: son registros viejos de prueba.
--
-- SALIDA: C:\miguel\biometrico-nombres.csv (FUERA del repositorio: trae NSS y nombres de personas
--   reales; no compartir ni subir). Se sobreescribe en cada corrida.
--
-- COMO CORRERLO (SQL Developer), cualquiera de las dos:
--   a) F5 (Ejecutar script) sobre este archivo. Escribe el CSV con SPOOL; puede tardar porque
--      tambien pinta los renglones en la ventana de salida.
--   b) Mas rapido: seleccionar solo el SELECT, Ctrl+Enter, clic derecho en la cuadricula >
--      Exportar > formato csv, archivo C:\miguel\biometrico-nombres.csv, con encabezados.
-- =============================================================================
SET SQLFORMAT csv
SET FEEDBACK OFF
SPOOL C:\miguel\biometrico-nombres.csv

SELECT EMP_NSS, EMP_NOMBRE, EMP_APELLIDO, EMP_APELLIDO2, EMP_STATUS
  FROM BIO_EMPLEADO
 WHERE EMP_STATUS IS NULL OR TRIM(TO_CHAR(EMP_STATUS)) <> '99';

SPOOL OFF
SET FEEDBACK ON
SET SQLFORMAT DEFAULT
