-- ============================================================================
-- Base de PRODUCCION (BIOMETRICO). REVERSA de la carga ETL del 15-oct-2026 (SOLO si sale mal).
-- Quita lo que inserto esa corrida: 364 archivos (BUSINESS_KEY legacy-23311..legacy-23687) y 4
-- tags (SOURCE_ID 1,349,019..1,349,022), todo con CREATED_BY = 'ETL_LEGACY' y fecha 15-oct.
-- No toca lo migrado el 1-oct ni lo capturado en el portal.
-- Correr primero el bloque 1 (cuenta) y comparar con lo esperado ANTES del bloque 2.
-- Los binarios en disco (C:\etl-prod-files-15oct o el servidor) se borran aparte.
-- ============================================================================

-- 1) Que se borraria (esperado: 364 y 4)
SELECT 'FS_FILE' tabla, COUNT(*) n FROM SERV_MED_FS_FILE
 WHERE CREATED_BY = 'ETL_LEGACY' AND CREATED_AT >= DATE '2026-10-15'
   AND BUSINESS_KEY LIKE 'legacy-%' AND TO_NUMBER(SUBSTR(BUSINESS_KEY, 8)) BETWEEN 23311 AND 23687
UNION ALL
SELECT 'TAG', COUNT(*) FROM SERV_MED_TAG
 WHERE CREATED_BY = 'ETL_LEGACY' AND MIGRATED_AT >= DATE '2026-10-15'
   AND SOURCE_ID BETWEEN 1349019 AND 1349022;

-- 2) Borrado (descomentar solo si el bloque 1 dio 364 y 4)
-- DELETE FROM SERV_MED_FS_FILE
--  WHERE CREATED_BY = 'ETL_LEGACY' AND CREATED_AT >= DATE '2026-10-15'
--    AND BUSINESS_KEY LIKE 'legacy-%' AND TO_NUMBER(SUBSTR(BUSINESS_KEY, 8)) BETWEEN 23311 AND 23687;
-- DELETE FROM SERV_MED_TAG
--  WHERE CREATED_BY = 'ETL_LEGACY' AND MIGRATED_AT >= DATE '2026-10-15'
--    AND SOURCE_ID BETWEEN 1349019 AND 1349022;
-- COMMIT;
