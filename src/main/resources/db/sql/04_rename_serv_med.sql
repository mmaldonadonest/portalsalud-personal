-- =====================================================================================
-- 04_rename_serv_med.sql  -  BASE DEL PORTAL (JDBC), NO ORDS.
--
-- Renombra el esquema del portal de APP_* / MED_* a SERV_MED_* (cambio del 24-sep-2026).
-- SOLO para ambientes que YA tienen las tablas con los nombres viejos:
--      * el Oracle local de desarrollo (con los 21,448 archivos y 550,560 tags del ETL)
--      * ONEWMS_QA en 200.94.116.132
-- En BIOMETRICO@PDBPRD NO se usa: alli no existe ninguna todavia y se crean directamente
-- con prod/01_ddl_portal_en_biometrico.sql, que ya trae los nombres nuevos.
--
-- QUE HACE Y QUE NO
--   * RENAME de tablas, indices, constraints: los DATOS NO SE MUEVEN ni se copian, es un
--     cambio de nombre en el diccionario. Las FK que apuntan a una tabla renombrada siguen
--     apuntando bien (Oracle las resuelve por object_id, no por nombre).
--   * Los triggers, vistas, procedimientos y la funcion NO se renombran: se BORRAN aqui y
--     se vuelven a crear al final corriendo 00_init_oracle21c.sql y app_domain/tags-salud.sql,
--     que son idempotentes y ya traen los nombres nuevos. Si no se borraran, quedarian dos
--     triggers sobre la misma tabla (el viejo y el nuevo) disparando ambos.
--
-- IDEMPOTENTE: cada paso verifica en el diccionario antes de actuar. Volver a correrlo no
-- hace nada. Si un objeto ya tiene el nombre nuevo, lo reporta y sigue.
--
-- ORDEN DE EJECUCION
--   1. Respaldo del esquema.
--   2. Bajar el WAR (que nadie escriba mientras se renombra).
--   3. Este script.
--   4. 00_init_oracle21c.sql  y  app_domain/tags-salud.sql   (recrean vistas/procs/triggers)
--   5. Levantar el WAR: con ddl-auto=validate, si algo quedo mal no arranca y lo dice.
-- =====================================================================================

SET SERVEROUTPUT ON;

-- -------------------------------------------------------------------------------------
-- PASO 0: que hay hoy (solo lectura). Revisar la salida antes de seguir.
-- -------------------------------------------------------------------------------------
SELECT table_name, num_rows FROM user_tables
 WHERE table_name LIKE 'APP\_%' ESCAPE '\' OR table_name LIKE 'MED\_%' ESCAPE '\'
    OR table_name LIKE 'SERV\_MED\_%' ESCAPE '\'
 ORDER BY table_name;

PROMPT ============================================================
PROMPT [1/4] BORRAR triggers, vistas, procedimientos y funcion VIEJOS
PROMPT       (se recrean con nombre nuevo en el paso 4, fuera de este script)
PROMPT ============================================================

DECLARE
  PROCEDURE borrar(p_tipo VARCHAR2, p_nombre VARCHAR2) IS
    v_n NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_n FROM user_objects
     WHERE object_name = p_nombre AND object_type = p_tipo;
    IF v_n = 0 THEN
      DBMS_OUTPUT.PUT_LINE('[SKIP] no existe: ' || p_tipo || ' ' || p_nombre);
      RETURN;
    END IF;
    EXECUTE IMMEDIATE 'DROP ' || p_tipo || ' ' || p_nombre;
    DBMS_OUTPUT.PUT_LINE('[OK]   borrado: ' || p_tipo || ' ' || p_nombre);
  END;
BEGIN
  borrar('TRIGGER',   'TRG_APP_MENU_BU');
  borrar('TRIGGER',   'TRG_FS_FILE_BU');
  borrar('TRIGGER',   'TRG_FS_FILE_POLICY_BU');
  borrar('TRIGGER',   'TRG_JOB_CATALOG_BU');
  borrar('TRIGGER',   'TRG_NOTIF_TEMPLATE_BU');
  borrar('TRIGGER',   'TRG_SEC_PERMISSION_BU');
  borrar('TRIGGER',   'TRG_SEC_ROLE_BU');
  borrar('TRIGGER',   'TRG_SEC_USER_BU');
  borrar('VIEW',      'VW_ACTIVE_USERS');
  borrar('VIEW',      'VW_FILE_LATEST');
  borrar('VIEW',      'V_MED_TAG_BY_GROUP');
  borrar('VIEW',      'V_MED_TAG_LATEST');
  borrar('VIEW',      'V_MED_TAG_MIG_STATUS');
  borrar('VIEW',      'V_MED_TAG_ORPHANS');
  borrar('PROCEDURE', 'SP_LOG_SQL_EXECUTION');
  borrar('PROCEDURE', 'SP_MIGRATE_TAGS');
  borrar('FUNCTION',  'FN_MED_TAG_GROUP');
END;
/

PROMPT ============================================================
PROMPT [2/4] RENOMBRAR TABLAS  (los datos no se mueven)
PROMPT ============================================================

DECLARE
  PROCEDURE ren_tabla(p_viejo VARCHAR2, p_nuevo VARCHAR2) IS
    v_viejo NUMBER; v_nuevo NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_viejo FROM user_tables WHERE table_name = p_viejo;
    SELECT COUNT(*) INTO v_nuevo FROM user_tables WHERE table_name = p_nuevo;
    IF v_nuevo > 0 THEN
      DBMS_OUTPUT.PUT_LINE('[SKIP] ya tiene el nombre nuevo: ' || p_nuevo);
    ELSIF v_viejo = 0 THEN
      DBMS_OUTPUT.PUT_LINE('[SKIP] no existe en este ambiente: ' || p_viejo);
    ELSE
      EXECUTE IMMEDIATE 'ALTER TABLE ' || p_viejo || ' RENAME TO ' || p_nuevo;
      DBMS_OUTPUT.PUT_LINE('[OK]   ' || RPAD(p_viejo, 28) || ' -> ' || p_nuevo);
    END IF;
  END;
BEGIN
  ren_tabla('APP_FS_FILE_ORPHAN_TRACKING',   'SERV_MED_FS_FILE_ORPHAN');
  ren_tabla('APP_SEC_ROLE_PERMISSION',       'SERV_MED_SEC_ROLE_PERMISSION');
  ren_tabla('APP_AUDIT_SQL_EXECUTION',       'SERV_MED_AUDIT_SQL_EXECUTION');
  ren_tabla('APP_FS_FILE_ACCESS_LOG',        'SERV_MED_FS_FILE_ACCESS_LOG');
  ren_tabla('APP_FS_FILE_VERSION',           'SERV_MED_FS_FILE_VERSION');
  ren_tabla('APP_SEC_PERMISSION',            'SERV_MED_SEC_PERMISSION');
  ren_tabla('APP_FS_FILE_POLICY',            'SERV_MED_FS_FILE_POLICY');
  ren_tabla('APP_NOTIF_TEMPLATE',            'SERV_MED_NOTIF_TEMPLATE');
  ren_tabla('APP_SEC_USER_ROLE',             'SERV_MED_SEC_USER_ROLE');
  ren_tabla('APP_IMPORT_LOTE',               'SERV_MED_IMPORT_LOTE');
  ren_tabla('APP_IMPORT_FILA',               'SERV_MED_IMPORT_FILA');
  ren_tabla('APP_JOB_CATALOG',               'SERV_MED_JOB_CATALOG');
  ren_tabla('MED_TAG_MIG_LOG',               'SERV_MED_TAG_MIG_LOG');
  ren_tabla('APP_MENU_ROLE',                 'SERV_MED_MENU_ROLE');
  ren_tabla('APP_AUD_EVENT',                 'SERV_MED_AUD_EVENT');
  ren_tabla('APP_SEC_USER',                  'SERV_MED_SEC_USER');
  ren_tabla('APP_SEC_ROLE',                  'SERV_MED_SEC_ROLE');
  ren_tabla('APP_FS_FILE',                   'SERV_MED_FS_FILE');
  ren_tabla('APP_MENU',                      'SERV_MED_MENU');
  ren_tabla('MED_TAG',                       'SERV_MED_TAG');
END;
/

PROMPT ============================================================
PROMPT [3/4] RENOMBRAR INDICES Y CONSTRAINTS
PROMPT ============================================================

DECLARE
  PROCEDURE ren_indice(p_viejo VARCHAR2, p_nuevo VARCHAR2) IS
    v_viejo NUMBER; v_nuevo NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_viejo FROM user_indexes WHERE index_name = p_viejo;
    SELECT COUNT(*) INTO v_nuevo FROM user_indexes WHERE index_name = p_nuevo;
    IF v_nuevo > 0 THEN
      DBMS_OUTPUT.PUT_LINE('[SKIP] indice ya renombrado: ' || p_nuevo);
    ELSIF v_viejo = 0 THEN
      DBMS_OUTPUT.PUT_LINE('[SKIP] indice inexistente: ' || p_viejo);
    ELSE
      EXECUTE IMMEDIATE 'ALTER INDEX ' || p_viejo || ' RENAME TO ' || p_nuevo;
      DBMS_OUTPUT.PUT_LINE('[OK]   indice ' || RPAD(p_viejo, 30) || ' -> ' || p_nuevo);
    END IF;
  END;

  -- La tabla dueña se busca en el diccionario: asi no hay que mantenerla a mano.
  PROCEDURE ren_constraint(p_viejo VARCHAR2, p_nuevo VARCHAR2) IS
    v_tabla USER_CONSTRAINTS.TABLE_NAME%TYPE;
    v_nuevo NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_nuevo FROM user_constraints WHERE constraint_name = p_nuevo;
    IF v_nuevo > 0 THEN
      DBMS_OUTPUT.PUT_LINE('[SKIP] constraint ya renombrada: ' || p_nuevo);
      RETURN;
    END IF;
    BEGIN
      SELECT table_name INTO v_tabla FROM user_constraints WHERE constraint_name = p_viejo;
    EXCEPTION WHEN NO_DATA_FOUND THEN
      DBMS_OUTPUT.PUT_LINE('[SKIP] constraint inexistente: ' || p_viejo);
      RETURN;
    END;
    EXECUTE IMMEDIATE 'ALTER TABLE ' || v_tabla || ' RENAME CONSTRAINT ' || p_viejo ||
                      ' TO ' || p_nuevo;
    DBMS_OUTPUT.PUT_LINE('[OK]   constraint ' || RPAD(p_viejo, 28) || ' -> ' || p_nuevo);
  END;
BEGIN
  ren_indice('UX_NOTIF_TEMPLATE_CODE_CHANNEL',   'SERV_MED_UX_NOTIF_TPL_CH');
  ren_indice('IX_FS_FILE_ACCESS_LOG_FILE',       'SERV_MED_IX_FS_ACC_LOG');
  ren_indice('IX_APP_IMPORT_LOTE_NOMBRE',        'SERV_MED_IX_IMPORT_LOTE_NOMBRE');
  ren_indice('IDX_APP_FS_FILE_NSS_TYPE',         'SERV_MED_IDX_FS_FILE_NSS_TYPE');
  ren_indice('UX_FS_FILE_BUSINESS_KEY',          'SERV_MED_UX_FS_FILE_BUSKEY');
  ren_indice('IX_APP_IMPORT_FILA_LOTE',          'SERV_MED_IX_IMPORT_FILA_LOTE');
  ren_indice('UX_SEC_PERMISSION_CODE',           'SERV_MED_UX_SEC_PERM_CODE');
  ren_indice('UX_SEC_ROLE_PERMISSION',           'SERV_MED_UX_SEC_ROLE_PERM');
  ren_indice('UX_FS_FILE_POLICY_CODE',           'SERV_MED_UX_FS_POLICY_CODE');
  ren_indice('UX_FS_FILE_VERSION_NO',            'SERV_MED_UX_FS_FILE_VERSION_NO');
  ren_indice('IDX_MED_TAG_NSS_GROUP',            'SERV_MED_IDX_TAG_NSS_GROUP');
  ren_indice('IDX_MED_TAG_SOURCE_ID',            'SERV_MED_IDX_TAG_SOURCE_ID');
  ren_indice('IX_AUDIT_SQL_EXEC_01',             'SERV_MED_IX_AUDIT_SQL_EXEC_01');
  ren_indice('UX_SEC_USER_USERNAME',             'SERV_MED_UX_SEC_USER_USERNAME');
  ren_indice('IDX_APP_FS_FILE_TYPE',             'SERV_MED_IDX_FS_FILE_TYPE');
  ren_indice('IDX_MED_TAG_NSS_TYPE',             'SERV_MED_IDX_TAG_NSS_TYPE');
  ren_indice('UX_JOB_CATALOG_CODE',              'SERV_MED_UX_JOB_CATALOG_CODE');
  ren_indice('UX_FS_FILE_CHECKSUM',              'SERV_MED_UX_FS_FILE_CHECKSUM');
  ren_indice('UX_APP_FS_FILE_BKEY',              'SERV_MED_UX_FS_FILE_BKEY');
  ren_indice('IX_APP_MENU_PARENT',               'SERV_MED_IX_MENU_PARENT');
  ren_indice('UX_SEC_USER_EMAIL',                'SERV_MED_UX_SEC_USER_EMAIL');
  ren_indice('UX_SEC_ROLE_CODE',                 'SERV_MED_UX_SEC_ROLE_CODE');
  ren_indice('UX_SEC_USER_ROLE',                 'SERV_MED_UX_SEC_USER_ROLE');
  ren_indice('UX_APP_MENU_CODE',                 'SERV_MED_UX_MENU_CODE');
  ren_indice('UX_APP_MENU_ROLE',                 'SERV_MED_UX_MENU_ROLE');

  ren_constraint('FK_SEC_ROLE_PERMISSION_ROLE',   'SERV_MED_FK_SEC_ROLE_PERM_ROLE');
  ren_constraint('FK_SEC_ROLE_PERMISSION_PERM',   'SERV_MED_FK_SEC_ROLE_PERM_PERM');
  ren_constraint('CK_SEC_PERMISSION_ACTIVE',      'SERV_MED_CK_SEC_PERM_ACTIVE');
  ren_constraint('CK_NOTIF_TEMPLATE_ACTIVE',      'SERV_MED_CK_NOTIF_TPL_ACTIVE');
  ren_constraint('CK_FS_FILE_POLICY_ACTIVE',      'SERV_MED_CK_FS_POLICY_ACTIVE');
  ren_constraint('FK_FS_FILE_VERSION_FILE',       'SERV_MED_FK_FS_VERSION_FILE');
  ren_constraint('FK_APP_IMPORT_FILA_LOTE',       'SERV_MED_FK_IMPORT_FILA_LOTE');
  ren_constraint('CK_JOB_CATALOG_ENABLED',        'SERV_MED_CK_JOB_CAT_ENABLED');
  ren_constraint('FK_SEC_USER_ROLE_USER',         'SERV_MED_FK_SEC_USER_ROLE_USER');
  ren_constraint('FK_SEC_USER_ROLE_ROLE',         'SERV_MED_FK_SEC_USER_ROLE_ROLE');
  ren_constraint('FK_FS_ACCESS_LOG_FILE',         'SERV_MED_FK_FS_ACCESS_LOG_FILE');
  ren_constraint('CK_SEC_USER_ACTIVE',            'SERV_MED_CK_SEC_USER_ACTIVE');
  ren_constraint('CK_SEC_ROLE_ACTIVE',            'SERV_MED_CK_SEC_ROLE_ACTIVE');
  ren_constraint('CK_APP_MENU_ACTIVE',            'SERV_MED_CK_MENU_ACTIVE');
  ren_constraint('FK_APP_MENU_PARENT',            'SERV_MED_FK_MENU_PARENT');
  ren_constraint('PK_APP_IMPORT_LOTE',            'SERV_MED_PK_IMPORT_LOTE');
  ren_constraint('PK_APP_IMPORT_FILA',            'SERV_MED_PK_IMPORT_FILA');
  ren_constraint('CK_FS_FILE_STATUS',             'SERV_MED_CK_FS_FILE_STATUS');
  ren_constraint('FK_MENU_ROLE_MENU',             'SERV_MED_FK_MENU_ROLE_MENU');
  ren_constraint('FK_MENU_ROLE_ROLE',             'SERV_MED_FK_MENU_ROLE_ROLE');
  ren_constraint('PK_APP_FS_FILE',                'SERV_MED_PK_FS_FILE');
  ren_constraint('PK_MED_TAG',                    'SERV_MED_PK_TAG');
END;
/

PROMPT ============================================================
PROMPT [4/4] VERIFICACION
PROMPT ============================================================

-- (a) Ya no debe quedar NINGUNA tabla del portal con el nombre viejo. 0 filas.
SELECT table_name FROM user_tables
 WHERE table_name IN ('APP_AUDIT_SQL_EXECUTION',
                      'APP_AUD_EVENT',
                      'APP_FS_FILE',
                      'APP_FS_FILE_ACCESS_LOG',
                      'APP_FS_FILE_ORPHAN_TRACKING',
                      'APP_FS_FILE_POLICY',
                      'APP_FS_FILE_VERSION',
                      'APP_IMPORT_FILA',
                      'APP_IMPORT_LOTE',
                      'APP_JOB_CATALOG',
                      'APP_MENU',
                      'APP_MENU_ROLE',
                      'APP_NOTIF_TEMPLATE',
                      'APP_SEC_PERMISSION',
                      'APP_SEC_ROLE',
                      'APP_SEC_ROLE_PERMISSION',
                      'APP_SEC_USER',
                      'APP_SEC_USER_ROLE',
                      'MED_TAG',
                      'MED_TAG_MIG_LOG')
 ORDER BY table_name;

-- (b) Deben estar las 20 con el nombre nuevo.
SELECT COUNT(*) AS tablas_serv_med FROM user_tables
 WHERE table_name IN ('SERV_MED_AUDIT_SQL_EXECUTION',
                      'SERV_MED_AUD_EVENT',
                      'SERV_MED_FS_FILE',
                      'SERV_MED_FS_FILE_ACCESS_LOG',
                      'SERV_MED_FS_FILE_ORPHAN',
                      'SERV_MED_FS_FILE_POLICY',
                      'SERV_MED_FS_FILE_VERSION',
                      'SERV_MED_IMPORT_FILA',
                      'SERV_MED_IMPORT_LOTE',
                      'SERV_MED_JOB_CATALOG',
                      'SERV_MED_MENU',
                      'SERV_MED_MENU_ROLE',
                      'SERV_MED_NOTIF_TEMPLATE',
                      'SERV_MED_SEC_PERMISSION',
                      'SERV_MED_SEC_ROLE',
                      'SERV_MED_SEC_ROLE_PERMISSION',
                      'SERV_MED_SEC_USER',
                      'SERV_MED_SEC_USER_ROLE',
                      'SERV_MED_TAG',
                      'SERV_MED_TAG_MIG_LOG');

-- (c) Nada del portal debe quedar INVALID. Si hay vistas/procs invalidos aqui, es normal
--     ANTES del paso 4 (recrear con 00_init y tags-salud); despues debe salir 0 filas.
SELECT object_name, object_type, status FROM user_objects
 WHERE status <> 'VALID' AND object_name LIKE 'SERV\_MED\_%' ESCAPE '\'
 ORDER BY object_type, object_name;

-- (d) Conteo de filas: debe coincidir con lo que habia ANTES del renombrado.
SELECT 'SERV_MED_TAG'     t, COUNT(*) filas FROM SERV_MED_TAG
UNION ALL SELECT 'SERV_MED_FS_FILE',  COUNT(*) FROM SERV_MED_FS_FILE
UNION ALL SELECT 'SERV_MED_SEC_USER', COUNT(*) FROM SERV_MED_SEC_USER
UNION ALL SELECT 'SERV_MED_MENU',     COUNT(*) FROM SERV_MED_MENU
UNION ALL SELECT 'SERV_MED_MENU_ROLE',COUNT(*) FROM SERV_MED_MENU_ROLE;

-- Recordatorio: falta el PASO 4 del encabezado -> correr 00_init_oracle21c.sql y
-- app_domain/tags-salud.sql para recrear vistas, procedimientos, funcion y triggers.

