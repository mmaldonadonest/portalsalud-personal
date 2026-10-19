-- ============================================================================
-- Base de PRODUCCION (BIOMETRICO @ PDBPRD). SOLO LECTURA.
-- Lista SOLO los ids que ya estan migrados (sin contenido ni datos de pacientes), para
-- cruzarlos contra el staging local y saber de antemano que entraria en la corrida:
--   etl-prod-15oct-tags-ids.txt  -> SOURCE_ID de SERV_MED_TAG   (~551 mil numeros, ~4 MB)
--   etl-prod-15oct-files-ids.txt -> id legacy de SERV_MED_FS_FILE (~22 mil numeros)
--
-- Correr con F5 (Ejecutar script) en SQL Developer, o en SQL*Plus. Si la ruta del SPOOL
-- no se puede escribir, cambiarla por otra carpeta.
-- ============================================================================
SET ECHO OFF
SET FEEDBACK OFF
SET HEADING OFF
SET PAGESIZE 0
SET LINESIZE 20
SET TRIMSPOOL ON
SET TERMOUT OFF
SET ARRAYSIZE 5000

SPOOL C:\miguel\portalsalud-personal\docs\script-prod\etl-prod-15oct-tags-ids.txt
SELECT SOURCE_ID FROM SERV_MED_TAG WHERE SOURCE_ID IS NOT NULL ORDER BY SOURCE_ID;
SPOOL OFF

SPOOL C:\miguel\portalsalud-personal\docs\script-prod\etl-prod-15oct-files-ids.txt
SELECT TO_NUMBER(SUBSTR(BUSINESS_KEY, 8)) FROM SERV_MED_FS_FILE
 WHERE BUSINESS_KEY LIKE 'legacy-%' ORDER BY 1;
SPOOL OFF

SET TERMOUT ON
PROMPT Listo: etl-prod-15oct-tags-ids.txt y etl-prod-15oct-files-ids.txt en docs\script-prod
