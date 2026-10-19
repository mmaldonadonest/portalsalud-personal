-- =====================================================================================
-- archivos-16-recuperar-borrado.sql  (16-oct-2026)
-- Base: BIOMETRICO (produccion). SOLO SE USA SI HAY QUE RECUPERAR UN ARCHIVO BORRADO.
--
-- Desde el 16-oct-2026 el borrado de archivos del expediente es LOGICO: la fila de
-- SERV_MED_FS_FILE queda con STATUS='DELETED' (UPDATED_AT / UPDATED_BY = cuando y quien) y el
-- binario se queda en disco. Este script lista lo borrado y regresa uno a 'ACTIVE'.
-- =====================================================================================

-- 1) Archivos borrados (mas recientes primero). Revisar NSS, tipo y nombre antes de recuperar.
SELECT ID, NSS, FILE_TYPE, ORIGINAL_NAME, DATE_UPLOAD, UPDATED_AT AS BORRADO_EL, UPDATED_BY AS BORRADO_POR
  FROM SERV_MED_FS_FILE
 WHERE STATUS = 'DELETED'
 ORDER BY UPDATED_AT DESC NULLS LAST
 FETCH FIRST 50 ROWS ONLY;

-- 2) Recuperar UNO: cambiar 0 por el ID del paso 1 y ejecutar; luego COMMIT.
UPDATE SERV_MED_FS_FILE
   SET STATUS = 'ACTIVE', UPDATED_AT = SYSTIMESTAMP, UPDATED_BY = 'RECUPERADO_MANUAL'
 WHERE ID = 0
   AND STATUS = 'DELETED';
-- Debe decir "1 fila actualizada". Si dice 0, el ID no existe o no estaba borrado.
-- COMMIT;
