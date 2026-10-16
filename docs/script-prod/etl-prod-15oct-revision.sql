-- ============================================================================
-- Base de PRODUCCION (BIOMETRICO @ PDBPRD). SOLO LECTURA.
-- Antes de migrar a produccion lo nuevo desde el 1-oct-2026 (staging local 15-oct:
-- files 365 con ids legacy 23310..23687, tags 551,048 con ids 252..1,349,022).
--
-- Correr en SQL Developer con F5 (Ejecutar script) o en SQL*Plus. Deja la salida en
--   C:\miguel\portalsalud-personal\docs\script-prod\etl-prod-15oct-revision.txt
-- (Si SQL Developer no permite esa ruta, cambiar la linea SPOOL por otra carpeta.)
-- ============================================================================
SET ECHO OFF
SET FEEDBACK ON
SET HEADING ON
SET LINESIZE 200
SET PAGESIZE 500
SET TRIMSPOOL ON
COLUMN k FORMAT A45

SPOOL C:\miguel\portalsalud-personal\docs\script-prod\etl-prod-15oct-revision.txt

PROMPT === 1) Resumen tags y archivos ===
SELECT 'TAG total' k, COUNT(*) n FROM SERV_MED_TAG
UNION ALL SELECT 'TAG con SOURCE_ID', COUNT(SOURCE_ID) FROM SERV_MED_TAG
UNION ALL SELECT 'TAG max SOURCE_ID', MAX(SOURCE_ID) FROM SERV_MED_TAG
UNION ALL SELECT 'TAG SOURCE_ID > 1,346,670', COUNT(*) FROM SERV_MED_TAG WHERE SOURCE_ID > 1346670
UNION ALL SELECT 'TAG SOURCE_ID > 1,347,384', COUNT(*) FROM SERV_MED_TAG WHERE SOURCE_ID > 1347384
UNION ALL SELECT 'TAG sin SOURCE_ID (capturados en el portal)', COUNT(*) FROM SERV_MED_TAG WHERE SOURCE_ID IS NULL
UNION ALL SELECT 'FILE total', COUNT(*) FROM SERV_MED_FS_FILE
UNION ALL SELECT 'FILE legacy', COUNT(*) FROM SERV_MED_FS_FILE WHERE BUSINESS_KEY LIKE 'legacy-%'
UNION ALL SELECT 'FILE max id legacy', MAX(TO_NUMBER(SUBSTR(BUSINESS_KEY, 8))) FROM SERV_MED_FS_FILE WHERE BUSINESS_KEY LIKE 'legacy-%'
UNION ALL SELECT 'FILE ids 22,759-23,309 (antes del 1-oct)', COUNT(*) FROM SERV_MED_FS_FILE
          WHERE BUSINESS_KEY LIKE 'legacy-%' AND TO_NUMBER(SUBSTR(BUSINESS_KEY, 8)) BETWEEN 22759 AND 23309
UNION ALL SELECT 'FILE ids >= 23,310 (los 365 nuevos)', COUNT(*) FROM SERV_MED_FS_FILE
          WHERE BUSINESS_KEY LIKE 'legacy-%' AND TO_NUMBER(SUBSTR(BUSINESS_KEY, 8)) >= 23310
UNION ALL SELECT 'FILE capturados en el portal (no legacy)', COUNT(*) FROM SERV_MED_FS_FILE
          WHERE BUSINESS_KEY IS NULL OR BUSINESS_KEY NOT LIKE 'legacy-%';

PROMPT === 2) Archivos legacy por dia de carga, ultimos 20 dias del legacy ===
SELECT TRUNC(DATE_UPLOAD) dia, COUNT(*) archivos,
       MIN(TO_NUMBER(SUBSTR(BUSINESS_KEY, 8))) id_min, MAX(TO_NUMBER(SUBSTR(BUSINESS_KEY, 8))) id_max
  FROM SERV_MED_FS_FILE
 WHERE BUSINESS_KEY LIKE 'legacy-%' AND DATE_UPLOAD >= DATE '2026-09-20'
 GROUP BY TRUNC(DATE_UPLOAD)
 ORDER BY 1;

PROMPT === 3) Tags por SOURCE_ID en bloques de 1,000 desde 1,340,000 ===
SELECT TRUNC(SOURCE_ID / 1000) * 1000 bloque, COUNT(*) tags, MIN(MIGRATED_AT) migrado_desde, MAX(MIGRATED_AT) migrado_hasta
  FROM SERV_MED_TAG
 WHERE SOURCE_ID >= 1340000
 GROUP BY TRUNC(SOURCE_ID / 1000) * 1000
 ORDER BY 1;

PROMPT === 4) Cuando se migro (por dia y usuario) ===
SELECT TRUNC(MIGRATED_AT) dia, CREATED_BY, COUNT(*) tags
  FROM SERV_MED_TAG
 WHERE MIGRATED_AT IS NOT NULL
 GROUP BY TRUNC(MIGRATED_AT), CREATED_BY
 ORDER BY 1;

SELECT TRUNC(CREATED_AT) dia, CREATED_BY, COUNT(*) archivos
  FROM SERV_MED_FS_FILE
 WHERE CREATED_BY = 'ETL_LEGACY'
 GROUP BY TRUNC(CREATED_AT), CREATED_BY
 ORDER BY 1;

SPOOL OFF
PROMPT Listo: salida en docs\script-prod\etl-prod-15oct-revision.txt
