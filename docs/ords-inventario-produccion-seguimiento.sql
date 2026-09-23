-- =====================================================================================
-- ords-inventario-produccion-seguimiento.sql  —  BIOMETRICO@PDBPRD. SOLO LECTURA.
-- Cierra las 4 preguntas que dejó abiertas el primer inventario
-- (ver docs/fase1-inventario-resultado.md). NO MODIFICA NADA.
-- =====================================================================================

SET LINESIZE 220 PAGESIZE 500

-- =====================================================================================
-- 1. ¿Los datos de prueba del desarrollo están en ESTA base?
--    Si salen filas, se confirma que la "ORDS de QA" y esta base son la misma, y la
--    limpieza (docs/ords-limpieza-datos-prueba.sql) hay que correrla aquí.
-- =====================================================================================
DEFINE nss_prueba = "('30048315698','90099119373','68958027838')";

SELECT 'consultas'      origen, COUNT(*) filas FROM TBL_SERV_CONSULTA_MEDICA     WHERE NSS IN &nss_prueba
UNION ALL SELECT 'incapacidades',   COUNT(*) FROM TBL_SERV_INCAPACIDAD_MEDICA    WHERE NSS IN &nss_prueba
UNION ALL SELECT 'examen dictamen', COUNT(*) FROM SERV_MED_RESULTADO_EXAMEN      WHERE NSS IN &nss_prueba
UNION ALL SELECT 'examen generales',COUNT(*) FROM SERV_MED_GENERALES             WHERE NSS IN &nss_prueba
UNION ALL SELECT 'archivos',        COUNT(*) FROM SERV_MED_FILES                 WHERE NSS IN &nss_prueba;

-- Tablas que creó el portal: aquí TODO lo que haya es nuestro (nadie más escribe)
SELECT 'accidentes'   tabla, COUNT(*) filas FROM SERV_MED_ACCIDENTE
UNION ALL SELECT 'accidente_seguimiento', COUNT(*) FROM SERV_MED_ACCIDENTE_SEGUIMIENTO
UNION ALL SELECT 'antidoping_resultado',  COUNT(*) FROM SERV_MED_ANTIDOPING_RESULTADO
UNION ALL SELECT 'antidoping_seleccion',  COUNT(*) FROM SERV_MED_ANTIDOPING_SELECCION
UNION ALL SELECT 'antidoping_inventario', COUNT(*) FROM SERV_MED_ANTIDOPING_INVENTARIO
UNION ALL SELECT 'maternidad',            COUNT(*) FROM SERV_MED_MATERNIDAD_SEGUIMIENTO
UNION ALL SELECT 'restricciones',         COUNT(*) FROM SERV_MED_RESTRICCION_ASIGNADA
UNION ALL SELECT 'examen_hist',           COUNT(*) FROM SERV_MED_RESULTADO_EXAMEN_HIST
UNION ALL SELECT 'predios',               COUNT(*) FROM SERV_MED_PREDIO
UNION ALL SELECT 'cuenta_predio',         COUNT(*) FROM SERV_MED_CUENTA_PREDIO
UNION ALL SELECT 'causas_consulta',       COUNT(*) FROM SERV_MED_CAT_CAUSA_CONSULTA
UNION ALL SELECT 'bug',                   COUNT(*) FROM BUG
UNION ALL SELECT 'prueba',                COUNT(*) FROM PRUEBA
UNION ALL SELECT 'onsys_debug',           COUNT(*) FROM ONSYS_DEBUG;

-- Detalle de las consultas/incapacidades de prueba (para decidir fila por fila)
SELECT REG_ID, FECHA, NSS, TIPO_CONSULTA, SUBSTR(MOTIVO_CONSULTA,1,50) motivo
  FROM TBL_SERV_CONSULTA_MEDICA WHERE NSS IN &nss_prueba ORDER BY REG_ID;
SELECT REG_ID, FECHA_REGISTRO, NSS, FOLIO_INCAPACIDAD, RAMO, TIPO_INCAPACIDAD
  FROM TBL_SERV_INCAPACIDAD_MEDICA WHERE NSS IN &nss_prueba ORDER BY REG_ID;

-- ¿Se capturó con otros NSS durante el desarrollo? (altas desde julio-2026)
SELECT NSS, COUNT(*) consultas, MIN(FECHA) desde, MAX(FECHA) hasta
  FROM TBL_SERV_CONSULTA_MEDICA WHERE FECHA >= DATE '2026-07-01' GROUP BY NSS ORDER BY 2 DESC;
SELECT NSS, COUNT(*) incapacidades, MIN(FECHA_REGISTRO) desde, MAX(FECHA_REGISTRO) hasta
  FROM TBL_SERV_INCAPACIDAD_MEDICA WHERE FECHA_REGISTRO >= DATE '2026-07-01' GROUP BY NSS ORDER BY 2 DESC;

-- =====================================================================================
-- 2. EMP_STATUS: ¿cuál es el valor de un empleado VIGENTE?
--    Se contrasta con NSS conocidos (el del usuario del portal entra por login, que exige 0).
-- =====================================================================================
SELECT EMP_NSS, EMP_STATUS, EMP_FECHAI, EMP_NOMBRE, EMP_APELLIDO
  FROM BIO_EMPLEADO WHERE EMP_NSS IN &nss_prueba ORDER BY EMP_NSS;

-- Altas de los últimos 90 días: casi todas deberían ser de empleados vigentes
SELECT EMP_STATUS, COUNT(*) altas_recientes
  FROM BIO_EMPLEADO WHERE EMP_FECHAI >= SYSDATE - 90 GROUP BY EMP_STATUS ORDER BY 2 DESC;

-- Los valores 99 y 100: ver ejemplos para entender qué significan
SELECT EMP_STATUS, EMP_NSS, EMP_FECHAI, EMP_NOMBRE FROM BIO_EMPLEADO
 WHERE EMP_STATUS IN (99,100) AND ROWNUM <= 20 ORDER BY EMP_STATUS;
SELECT EMP_NSS, EMP_STATUS, EMP_FECHAI FROM BIO_EMPLEADO WHERE EMP_STATUS IS NULL;

-- =====================================================================================
-- 3. Catálogo ICD/CIE-10: la tabla se llama SERV_MED_CAT_INDICE_IDC10 (con "IDC")
--    En lo que veíamos sólo había 909 claves (capítulos A y B). ¿Aquí también?
-- =====================================================================================
SELECT COUNT(*) claves FROM SERV_MED_CAT_INDICE_IDC10;
SELECT SUBSTR(CLAVE, 1, 1) capitulo, COUNT(*) claves
  FROM SERV_MED_CAT_INDICE_IDC10 GROUP BY SUBSTR(CLAVE, 1, 1) ORDER BY 1;
SELECT * FROM SERV_MED_CAT_INDICE_IDC10 WHERE ROWNUM <= 10;
-- (si la columna no se llama CLAVE, ver primero la estructura)
SELECT column_name, data_type, data_length FROM user_tab_columns
 WHERE table_name = 'SERV_MED_CAT_INDICE_IDC10' ORDER BY column_id;

-- =====================================================================================
-- 4. Gate del SSO: la app 13 (SERVICIO MEDICO, sso=0) vs la 27 (PORTAL SALUD, sso=1)
--    El portal Java valida hoy con id_app=13. Hay que decidir cuál corresponde.
-- =====================================================================================
SELECT * FROM TBL_APPS_ONEST WHERE ID_APP IN (13, 27);

-- ¿Cuántos usuarios tienen rol en cada una?
SELECT ID_APP, ID_ROL, COUNT(*) usuarios FROM TBL_APP_ROL_USUARIO
 WHERE ID_APP IN (13, 27) GROUP BY ID_APP, ID_ROL ORDER BY ID_APP, ID_ROL;

-- Ejemplos, incluidos los NSS de prueba (¿con qué rol entran hoy?)
SELECT * FROM TBL_APP_ROL_USUARIO WHERE ID_APP IN (13, 27) AND ROWNUM <= 30;
SELECT * FROM TBL_APP_ROL_USUARIO WHERE ID_USUARIO IN &nss_prueba ORDER BY ID_APP, ID_ROL;

-- Roles definidos para cada app y menús de la 27 (hoy esperamos 0 filas para la 27)
SELECT * FROM TBL_APPS_ROL WHERE ROWNUM <= 30;
SELECT * FROM TBL_APPS_ROL_MENU WHERE ID_APP = 27;

-- Estructura de estas tablas, por si hay que dar de alta la app 27
SELECT table_name, column_name, data_type, nullable FROM user_tab_columns
 WHERE table_name IN ('TBL_APPS_ONEST','TBL_APPS_ROL','TBL_APPS_ROL_MENU','TBL_APP_ROL_USUARIO')
 ORDER BY table_name, column_id;
