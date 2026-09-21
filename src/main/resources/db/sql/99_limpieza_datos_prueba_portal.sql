-- =====================================================================================
-- 99_limpieza_datos_prueba_portal.sql  —  BASE DEL PORTAL (JDBC: APP_*, MED_TAG), NO ORDS.
-- Limpieza de datos de prueba capturados durante el desarrollo (21-sep-2026).
--
-- SOLO aplica si producción va a REUTILIZAR el esquema de QA (ONEWMS_QA). Si prod nace con
-- esquema nuevo (Fase 2 del plan), este script no hace falta: ahí sólo entra el ETL.
--
-- Distingue lo real de lo de prueba por CREATED_BY:
--   * 'ETL_LEGACY'  -> histórico migrado del PHP (21,448 archivos / 550,560 tags): SE CONSERVA.
--   * cualquier otro (NSS del usuario del portal, 'SISTEMA') -> capturado desde el portal en
--     pruebas: se borra para los NSS de prueba.
-- Primero el inventario (SELECT), luego los DELETE; COMMIT comentado al final.
-- =====================================================================================

DEFINE nss_prueba = "('30048315698','90099119373','68958027838')";

-- ---------- Inventario ----------
SELECT 'APP_FS_FILE prueba (no ETL)' q, COUNT(*) n FROM APP_FS_FILE WHERE CREATED_BY <> 'ETL_LEGACY' AND NSS IN &nss_prueba
UNION ALL SELECT 'APP_FS_FILE no ETL (todos los NSS)', COUNT(*) FROM APP_FS_FILE WHERE CREATED_BY <> 'ETL_LEGACY'
UNION ALL SELECT 'APP_FS_FILE ETL (se conserva)', COUNT(*) FROM APP_FS_FILE WHERE CREATED_BY = 'ETL_LEGACY'
UNION ALL SELECT 'MED_TAG prueba (no ETL)', COUNT(*) FROM MED_TAG WHERE CREATED_BY <> 'ETL_LEGACY' AND NSS IN &nss_prueba
UNION ALL SELECT 'MED_TAG ETL (se conserva)', COUNT(*) FROM MED_TAG WHERE CREATED_BY = 'ETL_LEGACY'
UNION ALL SELECT 'APP_IMPORT_LOTE', COUNT(*) FROM APP_IMPORT_LOTE
UNION ALL SELECT 'APP_IMPORT_FILA', COUNT(*) FROM APP_IMPORT_FILA
UNION ALL SELECT 'APP_AUD_EVENT', COUNT(*) FROM APP_AUD_EVENT;

-- Archivos de prueba: nombre y ruta física (el binario en portal.files.root hay que borrarlo aparte)
SELECT ID, NSS, FILE_TYPE, ORIGINAL_NAME, CREATED_AT, CREATED_BY FROM APP_FS_FILE
 WHERE CREATED_BY <> 'ETL_LEGACY' ORDER BY CREATED_AT;

-- ---------- Limpieza ----------
-- 1) Adjuntos capturados en pruebas (metadatos; el archivo físico se borra del filesystem con la lista de arriba)
DELETE FROM APP_FS_FILE WHERE CREATED_BY <> 'ETL_LEGACY' AND NSS IN &nss_prueba;
-- Lotes del importador Excel de pruebas (los binarios también viven en files.root)
DELETE FROM APP_FS_FILE WHERE CREATED_BY <> 'ETL_LEGACY' AND FILE_TYPE = 'importacion';
DELETE FROM APP_IMPORT_FILA;
DELETE FROM APP_IMPORT_LOTE;

-- 2) Pre-Test y tags capturados en pruebas para los NSS de prueba (el histórico ETL no se toca)
DELETE FROM MED_TAG WHERE CREATED_BY <> 'ETL_LEGACY' AND NSS IN &nss_prueba;

-- 3) Bitácora de auditoría: es de desarrollo, se vacía para arrancar limpia en prod
DELETE FROM APP_AUD_EVENT;

-- 4) Rol duplicado MEDICO_ANALISTA (id 42, sin ROLE_): Security usa ROLE_MEDICO_ANALISTA (id 43)
DELETE FROM APP_SEC_USER_ROLE WHERE ROLE_ID IN (SELECT ID FROM APP_SEC_ROLE WHERE CODE = 'MEDICO_ANALISTA');
DELETE FROM APP_MENU_ROLE     WHERE ROLE_ID IN (SELECT ID FROM APP_SEC_ROLE WHERE CODE = 'MEDICO_ANALISTA');
DELETE FROM APP_SEC_ROLE      WHERE CODE = 'MEDICO_ANALISTA';

-- NO se tocan: APP_SEC_USER / APP_SEC_ROLE (resto) / APP_MENU / APP_MENU_ROLE (roles y menús
-- que sí van a prod), ni nada con CREATED_BY = 'ETL_LEGACY'.

-- ---------- Verificación ----------
SELECT 'fs_file prueba' q, COUNT(*) n FROM APP_FS_FILE WHERE CREATED_BY <> 'ETL_LEGACY' AND NSS IN &nss_prueba
UNION ALL SELECT 'med_tag prueba', COUNT(*) FROM MED_TAG WHERE CREATED_BY <> 'ETL_LEGACY' AND NSS IN &nss_prueba
UNION ALL SELECT 'import lotes', COUNT(*) FROM APP_IMPORT_LOTE
UNION ALL SELECT 'auditoria', COUNT(*) FROM APP_AUD_EVENT
UNION ALL SELECT 'fs_file ETL (debe seguir igual)', COUNT(*) FROM APP_FS_FILE WHERE CREATED_BY = 'ETL_LEGACY'
UNION ALL SELECT 'med_tag ETL (debe seguir igual)', COUNT(*) FROM MED_TAG WHERE CREATED_BY = 'ETL_LEGACY';

-- COMMIT;
-- ROLLBACK;
