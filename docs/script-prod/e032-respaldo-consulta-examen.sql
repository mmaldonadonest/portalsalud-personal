-- =============================================================================
-- E-032 (11-nov-2026), PASO 1 de 2: RESPALDO del WS de consulta del examen. SOLO LECTURA: no cambia nada.
-- =============================================================================
-- BASE    : BIOMETRICO de PRODUCCION. Correr CONECTADO COMO BIOMETRICO, con F5. Lo corre el usuario.
--
-- POR QUE: el WS de consulta (Servcio/consulta_examen -> PR_SERVICIO_MED_CONSULTA) no regresa 3 campos que
--   SI se guardan: NEUROLOGIA.AVC_OBS, HEREDOFAMILIARES.NEFROPATIAS_ID y SERV_MED_EXPLORACION_FISICA.NOLMAL_FASCIES.
--   Al reabrir el examen salen vacios. Se va a clonar para el portal (PR_SERVICIO_MED_CONSULTA_V2 y
--   Servcio/consulta_examen_v2) sin tocar el original, que usa el PHP. Para armar el clon hace falta su codigo.
--
-- QUE HACE: guarda en C:\miguel\respaldosql\ (en la maquina donde corre SQL Developer):
--     PR_SERVICIO_MED_CONSULTA_antes_e032_11nov2026.sql    (CREATE OR REPLACE ejecutable, termina en «/»)
--     ORDS_consulta_examen_antes_e032_11nov2026.sql         (codigo del handler POST, solo para consulta)
--   y muestra cuantos renglones tiene cada uno.
-- QUE NO HACE: no crea, no cambia ni borra nada.
-- Si SQL Developer dice que no pudo crear un archivo (la carpeta no existe), crear C:\miguel\respaldosql y repetir.
-- =============================================================================
SET SERVEROUTPUT ON SIZE UNLIMITED
SET PAGESIZE 0
SET LINESIZE 4000
SET LONG 1000000
SET LONGCHUNKSIZE 32000
SET TRIMSPOOL ON
SET FEEDBACK OFF
SET HEADING OFF
SET VERIFY OFF
SET ECHO OFF

SPOOL C:\miguel\respaldosql\PR_SERVICIO_MED_CONSULTA_antes_e032_11nov2026.sql
SELECT '-- RESPALDO de PR_SERVICIO_MED_CONSULTA (base BIOMETRICO de produccion), '
       || TO_CHAR(SYSDATE, 'DD-MM-YYYY HH24:MI') || ', antes del clon E-032'
  FROM DUAL;
SELECT '-- Restaurar: conectado como BIOMETRICO, correr este archivo completo (F5).' FROM DUAL;
SELECT CASE WHEN LINE = 1 THEN 'CREATE OR REPLACE ' END || RTRIM(TEXT, CHR(10) || CHR(13))
  FROM USER_SOURCE
 WHERE NAME = 'PR_SERVICIO_MED_CONSULTA' AND TYPE = 'PROCEDURE'
 ORDER BY LINE;
SELECT '/' FROM DUAL;
SPOOL OFF

SPOOL C:\miguel\respaldosql\ORDS_consulta_examen_antes_e032_11nov2026.sql
SELECT '-- RESPALDO del handler ORDS ' || m.NAME || '/' || t.URI_TEMPLATE || ' ' || h.METHOD
       || ' (base BIOMETRICO de produccion), ' || TO_CHAR(SYSDATE, 'DD-MM-YYYY HH24:MI')
  FROM USER_ORDS_HANDLERS h
  JOIN USER_ORDS_TEMPLATES t ON t.ID = h.TEMPLATE_ID
  JOIN USER_ORDS_MODULES m ON m.ID = t.MODULE_ID
 WHERE t.URI_TEMPLATE = 'consulta_examen';
SELECT h.SOURCE
  FROM USER_ORDS_HANDLERS h
  JOIN USER_ORDS_TEMPLATES t ON t.ID = h.TEMPLATE_ID
 WHERE t.URI_TEMPLATE = 'consulta_examen';
SPOOL OFF

SET PAGESIZE 50
SET HEADING ON
SET FEEDBACK ON
PROMPT === Respaldo: renglones de PR_SERVICIO_MED_CONSULTA (debe ser mayor que 0) ===
SELECT COUNT(*) RENGLONES FROM USER_SOURCE WHERE NAME = 'PR_SERVICIO_MED_CONSULTA' AND TYPE = 'PROCEDURE';
PROMPT === Handler consulta_examen (modulo, metodo, tamano del codigo) ===
SELECT m.NAME MODULO, t.URI_TEMPLATE, h.METHOD, DBMS_LOB.GETLENGTH(h.SOURCE) LARGO
  FROM USER_ORDS_HANDLERS h
  JOIN USER_ORDS_TEMPLATES t ON t.ID = h.TEMPLATE_ID
  JOIN USER_ORDS_MODULES m ON m.ID = t.MODULE_ID
 WHERE t.URI_TEMPLATE IN ('consulta_examen', 'consulta_examen_v2');
PROMPT Archivos en C:\miguel\respaldosql\
