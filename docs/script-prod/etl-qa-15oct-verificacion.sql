-- ============================================================================
-- Base de QA (ONEWMS_QA @ 10.249.249.3:1523/orclpdb). SOLO LECTURA.
-- Verificacion de la carga del 15-oct-2026 ~15:31 (ETL local MariaDB -> QA):
--   SERV_MED_FS_FILE : 365 archivos nuevos, BUSINESS_KEY legacy-23310 .. legacy-23687
--   SERV_MED_TAG     : 556 tags nuevos (SOURCE_ID que QA no tenia)
-- Esperado despues de la carga: FS_FILE 22,042 · TAG 551,486.
-- ============================================================================
SET LINESIZE 200
SET PAGESIZE 200

-- 1) Totales (antes: 21,677 / 550,930)
SELECT 'FS_FILE' tabla, COUNT(*) total FROM SERV_MED_FS_FILE
UNION ALL
SELECT 'TAG', COUNT(*) FROM SERV_MED_TAG;

-- 2) Lo que entro hoy por el ETL (esperado: 365 archivos y 556 tags)
SELECT 'FS_FILE' tabla, COUNT(*) filas, MIN(CREATED_AT) primero, MAX(CREATED_AT) ultimo
  FROM SERV_MED_FS_FILE
 WHERE CREATED_BY = 'ETL_LEGACY' AND CREATED_AT >= TIMESTAMP '2026-10-15 15:00:00'
UNION ALL
SELECT 'TAG', COUNT(*), MIN(MIGRATED_AT), MAX(MIGRATED_AT)
  FROM SERV_MED_TAG
 WHERE CREATED_BY = 'ETL_LEGACY' AND MIGRATED_AT >= TIMESTAMP '2026-10-15 15:00:00';

-- 3) Archivos nuevos: rango de ids del legacy y fechas reales de carga (esperado 23310..23687,
--    del 1-oct-2026 15:53 al 15-oct-2026 19:07)
SELECT COUNT(*) archivos,
       MIN(TO_NUMBER(SUBSTR(BUSINESS_KEY, 8))) id_min, MAX(TO_NUMBER(SUBSTR(BUSINESS_KEY, 8))) id_max,
       MIN(DATE_UPLOAD) desde, MAX(DATE_UPLOAD) hasta,
       ROUND(SUM(SIZE_BYTES) / 1024 / 1024, 1) mb
  FROM SERV_MED_FS_FILE
 WHERE BUSINESS_KEY LIKE 'legacy-%' AND TO_NUMBER(SUBSTR(BUSINESS_KEY, 8)) >= 23310;

-- 4) Archivos nuevos por tipo (examen_medico, laboratorio, nota_medica, nota_incapacidad, hash de consulta)
SELECT CASE WHEN LENGTH(FILE_TYPE) = 32 THEN '(adjunto de consulta)' ELSE FILE_TYPE END tipo,
       COUNT(*) archivos, COUNT(DISTINCT NSS) nss
  FROM SERV_MED_FS_FILE
 WHERE BUSINESS_KEY LIKE 'legacy-%' AND TO_NUMBER(SUBSTR(BUSINESS_KEY, 8)) >= 23310
 GROUP BY CASE WHEN LENGTH(FILE_TYPE) = 32 THEN '(adjunto de consulta)' ELSE FILE_TYPE END
 ORDER BY 2 DESC;

-- 5) Calidad de los archivos nuevos: sin NSS, sin checksum, sin ruta, tamano 0, no PDF
SELECT SUM(CASE WHEN NSS IS NULL OR TRIM(NSS) IS NULL THEN 1 ELSE 0 END) sin_nss,
       SUM(CASE WHEN CHECKSUM_SHA256 IS NULL THEN 1 ELSE 0 END)          sin_checksum,
       SUM(CASE WHEN STORAGE_PATH IS NULL THEN 1 ELSE 0 END)             sin_ruta,
       SUM(CASE WHEN NVL(SIZE_BYTES, 0) = 0 THEN 1 ELSE 0 END)           tamano_cero,
       SUM(CASE WHEN EXTENSION <> 'pdf' THEN 1 ELSE 0 END)               no_pdf,
       SUM(CASE WHEN STATUS <> 'ACTIVE' THEN 1 ELSE 0 END)               no_activos
  FROM SERV_MED_FS_FILE
 WHERE BUSINESS_KEY LIKE 'legacy-%' AND TO_NUMBER(SUBSTR(BUSINESS_KEY, 8)) >= 23310;

-- 6) Ningun BUSINESS_KEY duplicado (esperado: sin filas)
SELECT BUSINESS_KEY, COUNT(*) FROM SERV_MED_FS_FILE
 WHERE BUSINESS_KEY LIKE 'legacy-%' GROUP BY BUSINESS_KEY HAVING COUNT(*) > 1;

-- 7) Muestra: 10 archivos nuevos (para abrirlos en el portal de QA cuando se suban los binarios)
SELECT * FROM (
  SELECT ID, NSS, BUSINESS_KEY, FILE_TYPE, ORIGINAL_NAME, DATE_UPLOAD, SIZE_BYTES, STORAGE_PATH
    FROM SERV_MED_FS_FILE
   WHERE BUSINESS_KEY LIKE 'legacy-%' AND TO_NUMBER(SUBSTR(BUSINESS_KEY, 8)) >= 23310
   ORDER BY DATE_UPLOAD DESC
) WHERE ROWNUM <= 10;

-- 8) Tags nuevos: rango de SOURCE_ID y reparto por grupo (ninguno debe caer en 'OTRO')
SELECT TAG_GROUP, COUNT(*) tags, COUNT(DISTINCT NSS) nss, MIN(SOURCE_ID) src_min, MAX(SOURCE_ID) src_max
  FROM SERV_MED_TAG
 WHERE CREATED_BY = 'ETL_LEGACY' AND MIGRATED_AT >= TIMESTAMP '2026-10-15 15:00:00'
 GROUP BY TAG_GROUP
 ORDER BY 2 DESC;

-- 9) Tags nuevos por tipo (los 20 mas frecuentes)
SELECT * FROM (
  SELECT TYPE, COUNT(*) tags
    FROM SERV_MED_TAG
   WHERE CREATED_BY = 'ETL_LEGACY' AND MIGRATED_AT >= TIMESTAMP '2026-10-15 15:00:00'
   GROUP BY TYPE ORDER BY 2 DESC
) WHERE ROWNUM <= 20;

-- 10) Ningun SOURCE_ID duplicado (esperado: sin filas)
SELECT SOURCE_ID, COUNT(*) FROM SERV_MED_TAG
 WHERE SOURCE_ID IS NOT NULL GROUP BY SOURCE_ID HAVING COUNT(*) > 1;

-- 11) Hueco conocido: archivos del legacy con id entre 22,759 y 23,309 (subidos antes del 1-oct,
--     despues de la carga previa a QA). Esperado: 0 en QA, porque el respaldo de hoy no los traia.
SELECT COUNT(*) en_qa_del_hueco FROM SERV_MED_FS_FILE
 WHERE BUSINESS_KEY LIKE 'legacy-%' AND TO_NUMBER(SUBSTR(BUSINESS_KEY, 8)) BETWEEN 22759 AND 23309;
