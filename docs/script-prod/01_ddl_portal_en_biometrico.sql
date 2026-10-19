-- =====================================================================================
-- 01_ddl_portal_en_biometrico.sql
-- DDL COMPLETO del esquema del portal Java, para ejecutar en la BASE BIOMETRICO@PDBPRD.
--
-- Generado el 24-sep-2026 concatenando, EN ORDEN, los scripts de src/main/resources/db/sql.
--
-- -------------------------------------------------------------------------------------
-- NOMBRES: prefijo SERV_MED_ (cambio del 24-sep-2026)
-- -------------------------------------------------------------------------------------
-- Todas las tablas del portal pasaron de APP_* / MED_* a SERV_MED_*, quitando el APP_/MED_
-- redundante (APP_SEC_USER -> SERV_MED_SEC_USER, MED_TAG -> SERV_MED_TAG). Se renombraron
-- tambien indices, constraints, triggers, vistas, procedimientos y la funcion, porque en un
-- esquema compartido todos esos nombres viven en el MISMO namespace.
--
-- Motivo: el portal ya no tendra esquema propio, vive dentro de BIOMETRICO, donde el prefijo
-- APP_ ya esta ocupado por OTRO sistema (APP_TAREAS, APP_REPORTES, APP_CONFIG_EXCEL y 7 mas).
-- SERV_MED_ es la convencion del servicio medico en esa base.
--
-- Se respeta el limite de 30 caracteres del esquema legacy (desde 12.2 Oracle admite 128,
-- pero SERV_MED_RESULTADO_EXAMEN_HIST mide exactamente 30: se conserva la convencion).
-- El nombre mas largo que crea este script mide 30, verificado.
--
-- VERSION: BIOMETRICO@PDBPRD es Oracle 19c Enterprise Edition. Lo mas nuevo que usa este
-- script son las columnas IDENTITY (12.1) y el q-quoting de literales (10.1), asi que 19c
-- alcanza de sobra. Que uno de los scripts de origen se llame 00_init_oracle21c.sql es solo
-- el nombre del archivo, heredado del ambiente donde se escribio; no exige 21c.
--
-- QUIEN LO EJECUTA: el usuario / el DBA, en SQL Developer, conectado como BIOMETRICO.
-- El agente no ejecuta nada contra esta base (regla del 23-sep-2026).
--
-- -------------------------------------------------------------------------------------
-- ANTES DE EJECUTAR
-- -------------------------------------------------------------------------------------
-- 1. RESPALDO: NO es bloqueante. La base ya tiene respaldo automatico, no hay que hacer
--    uno a proposito. Se menciona solo para tenerlo presente: este DDL es autoconfirmado,
--    ROLLBACK no sirve, y la vuelta atras son ese respaldo o prod/02_rollback_portal_en_biometrico.sql.
-- 2. LO UNICO BLOQUEANTE: correr el bloque "PRE-CHECK" de aqui abajo. Con el prefijo nuevo hay que revisar TRES
--    cosas, no solo los nombres de tabla:
--      a) que ninguna tabla SERV_MED_* del portal exista ya,
--      b) que ningun INDICE ni CONSTRAINT choque por nombre (comparten namespace por esquema),
--      c) el inventario SERV_MED_* actual, para ver que no se confunda con el del PHP legacy.
-- 3. Privilegios: BIOMETRICO tiene CREATE TABLE y el rol RESOURCE (cubre CREATE SEQUENCE,
--    CREATE PROCEDURE, CREATE TRIGGER), asi que alcanza. Las vistas usan CREATE ANY VIEW,
--    que tambien tiene.
--
-- TODO ES IDEMPOTENTE: cada CREATE va dentro de un bloque que ignora el ORA-00955
-- ("el objeto ya existe"), asi que se puede volver a correr sin romper nada. Los CREATE
-- INDEX de los scripts de respaldo ignoran ademas ORA-01408 ("esa lista de columnas ya
-- esta indexada"), que es lo que pasa cuando 00_init ya creo el indice con otro nombre.
--
-- QUE NO INCLUYE ESTE ARCHIVO (a proposito):
--   * files-salud.sql (MED_FILE / MED_FILE_MIG_LOG): diseno anterior que el portal YA NO USA
--     (escribe en SERV_MED_FS_FILE). No hace falta crearlo y por eso conserva el nombre viejo.
--   * El seed del primer usuario ADMIN: va al final, aparte, porque hay que elegir el NSS.
--     Ver seeds/user-68958027838.sql y la nota del final de este archivo.
--
-- DESPUES DE EJECUTAR
--   * Correr el bloque "VERIFICACION" del final: deben aparecer las 20 tablas del portal.
--   * Dar de alta el primer ADMIN (seed).
--   * Arrancar el WAR con perfil prod: si falta algo, el log dira "Schema-validation: missing
--     table" con el nombre exacto.
-- =====================================================================================

-- -------------------------------------------------------------------------------------
-- PRE-CHECK  (solo lectura; revisar la salida ANTES de seguir)
-- -------------------------------------------------------------------------------------
-- (a) Tablas del portal que YA existen en este esquema (lo normal la primera vez: 0 filas)
SELECT table_name, 'ya existe' estado FROM user_tables
 WHERE table_name IN ('SERV_MED_SEC_USER','SERV_MED_SEC_ROLE','SERV_MED_SEC_USER_ROLE',
                      'SERV_MED_SEC_PERMISSION','SERV_MED_SEC_ROLE_PERMISSION','SERV_MED_MENU',
                      'SERV_MED_MENU_ROLE','SERV_MED_AUD_EVENT','SERV_MED_AUDIT_SQL_EXECUTION',
                      'SERV_MED_FS_FILE','SERV_MED_FS_FILE_VERSION','SERV_MED_FS_FILE_ACCESS_LOG',
                      'SERV_MED_FS_FILE_POLICY','SERV_MED_FS_FILE_ORPHAN','SERV_MED_IMPORT_LOTE',
                      'SERV_MED_IMPORT_FILA','SERV_MED_JOB_CATALOG','SERV_MED_NOTIF_TEMPLATE',
                      'SERV_MED_TAG','SERV_MED_TAG_MIG_LOG')
 ORDER BY table_name;

-- (b) Choque de nombres de INDICE o CONSTRAINT. Este es el riesgo nuevo del prefijo: si un
--     nombre ya existe, el guard -955 se lo traga EN SILENCIO y el objeto NO se crea.
--     Debe regresar 0 filas.
SELECT 'INDEX' tipo, index_name nombre FROM user_indexes
 WHERE index_name LIKE 'SERV\_MED\_UX\_%' ESCAPE '\'
    OR index_name LIKE 'SERV\_MED\_IX\_%' ESCAPE '\'
    OR index_name LIKE 'SERV\_MED\_IDX\_%' ESCAPE '\'
UNION ALL
SELECT 'CONSTRAINT', constraint_name FROM user_constraints
 WHERE constraint_name LIKE 'SERV\_MED\_PK\_%' ESCAPE '\'
    OR constraint_name LIKE 'SERV\_MED\_FK\_%' ESCAPE '\'
    OR constraint_name LIKE 'SERV\_MED\_CK\_%' ESCAPE '\'
UNION ALL
SELECT 'OTRO', object_name FROM user_objects
 WHERE object_name IN ('SERV_MED_FN_TAG_GROUP','SERV_MED_SP_MIGRATE_TAGS','SERV_MED_SP_LOG_SQL_EXEC',
                       'SERV_MED_VW_ACTIVE_USERS','SERV_MED_VW_FILE_LATEST','SERV_MED_V_TAG_LATEST',
                       'SERV_MED_V_TAG_BY_GROUP','SERV_MED_V_TAG_ORPHANS','SERV_MED_V_TAG_MIG_STATUS',
                       'SERV_MED_TRG_SEC_USER_BU','SERV_MED_TRG_SEC_ROLE_BU','SERV_MED_TRG_SEC_PERM_BU',
                       'SERV_MED_TRG_MENU_BU','SERV_MED_TRG_JOB_CATALOG_BU','SERV_MED_TRG_NOTIF_TPL_BU',
                       'SERV_MED_TRG_FS_FILE_BU','SERV_MED_TRG_FS_FILE_POLICY_BU')
 ORDER BY 1, 2;

-- (c) Inventario actual, para tener la linea base antes de agregar nada.
--     El 24-sep-2026 habia 64 tablas SERV_MED_* y 10 APP_*. Si los numeros cambiaron,
--     averiguar por que ANTES de seguir. De esas 64:
--        * 53 son del PHP legacy (expediente clinico: ABDOMEN, DIAGNOSTICO,
--          EXPLORACION_FISICA, CAT_INDICE_IDC10 con las 909 claves ICD, etc.)
--        * 11 son de ESTE proyecto pero del lado de ORDS, no de JDBC (Accidentes,
--          Antidoping, Causas, Restricciones, Maternidad, Predio/Cuenta). Tienen datos
--          reales y este DDL no las toca: son las unicas SERV_MED_* sin estadisticas
--          recolectadas, asi que se distinguen por NUM_ROWS / LAST_ANALYZED en blanco.
--     Despues de este DDL el esquema quedara con 84 tablas SERV_MED_*.
SELECT COUNT(*) AS serv_med_antes FROM user_tables
 WHERE table_name LIKE 'SERV\_MED\_%' ESCAPE '\';
SELECT table_name, num_rows, TO_CHAR(last_analyzed, 'DD/MM/YYYY') estadisticas_del
  FROM user_tables WHERE table_name LIKE 'SERV\_MED\_%' ESCAPE '\' ORDER BY 1;
SELECT table_name FROM user_tables WHERE table_name LIKE 'APP\_%' ESCAPE '\' ORDER BY 1;

-- (d) Privilegios del usuario con el que se va a ejecutar
SELECT privilege FROM user_sys_privs ORDER BY 1;
SELECT granted_role FROM user_role_privs ORDER BY 1;

-- =====================================================================================
-- A PARTIR DE AQUI EMPIEZA EL DDL
-- =====================================================================================



-- =====================================================================================
-- PASO 1 de 9  -  00_init_oracle21c.sql
-- Nucleo: SERV_MED_SEC_* (seguridad), SERV_MED_MENU, SERV_MED_AUD_EVENT,
--    SERV_MED_FS_FILE + satelites, SERV_MED_JOB_CATALOG, SERV_MED_NOTIF_TEMPLATE,
--    SERV_MED_AUDIT_SQL_EXECUTION, indices, triggers y vistas.
--    Es el script AUTORITATIVO de SERV_MED_FS_FILE (crea tambien sus FKs).
-- =====================================================================================
-- 00_init_oracle21c.sql
-- Bootstrap inicial Oracle 21c (idempotente) para Portal Salud
-- Ubicación: src/main/resources/db/sql
--
-- Ejecuta TODO en un solo archivo, en este orden lógico interno:
--   1) tablas
--   2) índices
--   3) constraints/FKs (con verificación en diccionario)
--   4) seeds base
--   5) vistas
--   6) procedimientos
--   7) triggers
--   8) checks post-ejecución
--
-- NOTA: Se asume ejecución con el usuario/esquema objetivo.

SET SERVEROUTPUT ON;

PROMPT ============================================================
PROMPT [1/8] CREACION DE TABLAS
PROMPT ============================================================

--------------------------------------------------------------------------------
-- SERV_MED_AUDIT_SQL_EXECUTION (soporte manifest)
--------------------------------------------------------------------------------
BEGIN
  EXECUTE IMMEDIATE q'[
    CREATE TABLE SERV_MED_AUDIT_SQL_EXECUTION (
      ID           NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
      MANIFEST_ID  VARCHAR2(150) NOT NULL,
      SCRIPT_NAME  VARCHAR2(255) NOT NULL,
      PHASE_NAME   VARCHAR2(100),
      EXECUTED_AT  TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL,
      STATUS       VARCHAR2(30) NOT NULL,
      MESSAGE      VARCHAR2(2000),
      EXECUTED_BY  VARCHAR2(100) DEFAULT USER NOT NULL
    )
  ]';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF; END;
/

--------------------------------------------------------------------------------
-- Seguridad
--------------------------------------------------------------------------------
BEGIN
  EXECUTE IMMEDIATE q'[
    CREATE TABLE SERV_MED_SEC_ROLE (
      ID           NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
      CODE         VARCHAR2(60) NOT NULL,
      NAME         VARCHAR2(120) NOT NULL,
      DESCRIPTION  VARCHAR2(500),
      ACTIVE       CHAR(1) DEFAULT 'Y' NOT NULL,
      CREATED_AT   TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL,
      CREATED_BY   VARCHAR2(100) DEFAULT USER NOT NULL,
      UPDATED_AT   TIMESTAMP(6),
      UPDATED_BY   VARCHAR2(100)
    )
  ]';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF; END;
/

BEGIN
  EXECUTE IMMEDIATE q'[
    CREATE TABLE SERV_MED_SEC_PERMISSION (
      ID           NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
      CODE         VARCHAR2(120) NOT NULL,
      NAME         VARCHAR2(180) NOT NULL,
      DESCRIPTION  VARCHAR2(500),
      MODULE       VARCHAR2(100),
      ACTIVE       CHAR(1) DEFAULT 'Y' NOT NULL,
      CREATED_AT   TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL,
      CREATED_BY   VARCHAR2(100) DEFAULT USER NOT NULL,
      UPDATED_AT   TIMESTAMP(6),
      UPDATED_BY   VARCHAR2(100)
    )
  ]';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF; END;
/

BEGIN
  EXECUTE IMMEDIATE q'[
    CREATE TABLE SERV_MED_SEC_USER (
      ID              NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
      USERNAME        VARCHAR2(120) NOT NULL,
      EMAIL           VARCHAR2(180),
      PASSWORD_HASH   VARCHAR2(500) NOT NULL,
      DISPLAY_NAME    VARCHAR2(180),
      ACCOUNT_STATUS  VARCHAR2(30) DEFAULT 'ACTIVE' NOT NULL,
      LAST_LOGIN_AT   TIMESTAMP(6),
      ACTIVE          CHAR(1) DEFAULT 'Y' NOT NULL,
      CREATED_AT      TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL,
      CREATED_BY      VARCHAR2(100) DEFAULT USER NOT NULL,
      UPDATED_AT      TIMESTAMP(6),
      UPDATED_BY      VARCHAR2(100)
    )
  ]';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF; END;
/

BEGIN
  EXECUTE IMMEDIATE q'[
    CREATE TABLE SERV_MED_SEC_USER_ROLE (
      ID          NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
      USER_ID     NUMBER NOT NULL,
      ROLE_ID     NUMBER NOT NULL,
      CREATED_AT  TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL,
      CREATED_BY  VARCHAR2(100) DEFAULT USER NOT NULL
    )
  ]';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF; END;
/

BEGIN
  EXECUTE IMMEDIATE q'[
    CREATE TABLE SERV_MED_SEC_ROLE_PERMISSION (
      ID             NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
      ROLE_ID        NUMBER NOT NULL,
      PERMISSION_ID  NUMBER NOT NULL,
      CREATED_AT     TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL,
      CREATED_BY     VARCHAR2(100) DEFAULT USER NOT NULL
    )
  ]';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF; END;
/

--------------------------------------------------------------------------------
-- Auditoría funcional
--------------------------------------------------------------------------------
BEGIN
  EXECUTE IMMEDIATE q'[
    CREATE TABLE SERV_MED_AUD_EVENT (
      ID           NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
      EVENT_TYPE   VARCHAR2(100) NOT NULL,
      ENTITY_NAME  VARCHAR2(120),
      ENTITY_ID    VARCHAR2(120),
      ACTION       VARCHAR2(80),
      EVENT_TS     TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL,
      USERNAME     VARCHAR2(120),
      DETAIL_JSON  CLOB,
      TRACE_ID     VARCHAR2(120),
      IP_ADDRESS   VARCHAR2(80)
    )
  ]';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF; END;
/

--------------------------------------------------------------------------------
-- Menú
--------------------------------------------------------------------------------
BEGIN
  EXECUTE IMMEDIATE q'[
    CREATE TABLE SERV_MED_MENU (
      ID                        NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
      CODE                      VARCHAR2(120) NOT NULL,
      PARENT_ID                 NUMBER,
      TITLE                     VARCHAR2(180) NOT NULL,
      PATH                      VARCHAR2(400),
      ICON                      VARCHAR2(120),
      ORDER_NO                  NUMBER DEFAULT 0 NOT NULL,
      REQUIRED_PERMISSION_CODE  VARCHAR2(120),
      ACTIVE                    CHAR(1) DEFAULT 'Y' NOT NULL,
      CREATED_AT                TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL,
      CREATED_BY                VARCHAR2(100) DEFAULT USER NOT NULL,
      UPDATED_AT                TIMESTAMP(6),
      UPDATED_BY                VARCHAR2(100)
    )
  ]';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF; END;
/

--------------------------------------------------------------------------------
-- Jobs
--------------------------------------------------------------------------------
BEGIN
  EXECUTE IMMEDIATE q'[
    CREATE TABLE SERV_MED_JOB_CATALOG (
      ID               NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
      CODE             VARCHAR2(120) NOT NULL,
      NAME             VARCHAR2(180) NOT NULL,
      CRON_EXPRESSION  VARCHAR2(120),
      ENABLED          CHAR(1) DEFAULT 'Y' NOT NULL,
      DESCRIPTION      VARCHAR2(500),
      CREATED_AT       TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL,
      CREATED_BY       VARCHAR2(100) DEFAULT USER NOT NULL,
      UPDATED_AT       TIMESTAMP(6),
      UPDATED_BY       VARCHAR2(100)
    )
  ]';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF; END;
/

--------------------------------------------------------------------------------
-- Notificaciones
--------------------------------------------------------------------------------
BEGIN
  EXECUTE IMMEDIATE q'[
    CREATE TABLE SERV_MED_NOTIF_TEMPLATE (
      ID             NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
      CODE           VARCHAR2(120) NOT NULL,
      CHANNEL        VARCHAR2(30) NOT NULL,
      SUBJECT        VARCHAR2(300),
      BODY_TEMPLATE  CLOB NOT NULL,
      ACTIVE         CHAR(1) DEFAULT 'Y' NOT NULL,
      CREATED_AT     TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL,
      CREATED_BY     VARCHAR2(100) DEFAULT USER NOT NULL,
      UPDATED_AT     TIMESTAMP(6),
      UPDATED_BY     VARCHAR2(100)
    )
  ]';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF; END;
/

--------------------------------------------------------------------------------
-- Módulo de archivos (requerido por manifest)
--------------------------------------------------------------------------------
BEGIN
  EXECUTE IMMEDIATE q'[
    CREATE TABLE SERV_MED_FS_FILE (
      ID                NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
      BUSINESS_KEY      VARCHAR2(120) NOT NULL,
      -- Identidad de negocio de los adjuntos del expediente (usadas por FsFileRepository).
      -- Deben existir aqui para que un entorno nuevo nazca completo: este bloque gana
      -- la carrera contra app_domain/app-fs-file.sql (guard -955). Ver app-fs-file-reconcile.sql.
      NSS               VARCHAR2(50),
      FILE_TYPE         VARCHAR2(120),
      ORIGINAL_NAME     VARCHAR2(400) NOT NULL,
      EXTENSION         VARCHAR2(30),
      MIME_TYPE         VARCHAR2(200),
      SIZE_BYTES        NUMBER(19) NOT NULL,
      CHECKSUM_SHA256   VARCHAR2(64) NOT NULL,
      STORAGE_PATH      VARCHAR2(1000) NOT NULL,
      STORAGE_PROVIDER  VARCHAR2(80) NOT NULL,
      STATUS            VARCHAR2(40) NOT NULL,
      CURRENT_VERSION   NUMBER DEFAULT 1 NOT NULL,
      VERSION           NUMBER DEFAULT 1,          -- el INSERT del repo setea VERSION=1
      DATE_UPLOAD       TIMESTAMP(6) DEFAULT SYSTIMESTAMP,
      CREATED_AT        TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL,
      CREATED_BY        VARCHAR2(100) DEFAULT USER NOT NULL,
      UPDATED_AT        TIMESTAMP(6),
      UPDATED_BY        VARCHAR2(100)
    )
  ]';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF; END;
/

BEGIN
  EXECUTE IMMEDIATE q'[
    CREATE TABLE SERV_MED_FS_FILE_VERSION (
      ID               NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
      FILE_ID          NUMBER NOT NULL,
      VERSION_NO       NUMBER NOT NULL,
      STORAGE_PATH     VARCHAR2(1000) NOT NULL,
      SIZE_BYTES       NUMBER(19) NOT NULL,
      CHECKSUM_SHA256  VARCHAR2(64) NOT NULL,
      CREATED_AT       TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL,
      CREATED_BY       VARCHAR2(100) DEFAULT USER NOT NULL
    )
  ]';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF; END;
/

BEGIN
  EXECUTE IMMEDIATE q'[
    CREATE TABLE SERV_MED_FS_FILE_ACCESS_LOG (
      ID           NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
      FILE_ID      NUMBER NOT NULL,
      ACCESS_TYPE  VARCHAR2(30) NOT NULL,
      ACCESSED_AT  TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL,
      ACCESSED_BY  VARCHAR2(100),
      TRACE_ID     VARCHAR2(120),
      CLIENT_IP    VARCHAR2(80)
    )
  ]';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF; END;
/

BEGIN
  EXECUTE IMMEDIATE q'[
    CREATE TABLE SERV_MED_FS_FILE_POLICY (
      ID              NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
      POLICY_CODE     VARCHAR2(120) NOT NULL,
      DESCRIPTION     VARCHAR2(500),
      MAX_SIZE_BYTES  NUMBER(19),
      ALLOWED_MIMES   VARCHAR2(2000),
      RETENTION_DAYS  NUMBER,
      ACTIVE          CHAR(1) DEFAULT 'Y' NOT NULL,
      CREATED_AT      TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL,
      CREATED_BY      VARCHAR2(100) DEFAULT USER NOT NULL,
      UPDATED_AT      TIMESTAMP(6),
      UPDATED_BY      VARCHAR2(100)
    )
  ]';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF; END;
/

BEGIN
  EXECUTE IMMEDIATE q'[
    CREATE TABLE SERV_MED_FS_FILE_ORPHAN (
      ID            NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
      FILE_ID       NUMBER,
      STORAGE_PATH  VARCHAR2(1000) NOT NULL,
      DETECTED_AT   TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL,
      RESOLVED_AT   TIMESTAMP(6),
      STATUS        VARCHAR2(30) DEFAULT 'OPEN' NOT NULL,
      NOTES         VARCHAR2(1000)
    )
  ]';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF; END;
/

PROMPT ============================================================
PROMPT [2/8] INDICES
PROMPT ============================================================

BEGIN EXECUTE IMMEDIATE 'CREATE INDEX SERV_MED_IX_AUDIT_SQL_EXEC_01 ON SERV_MED_AUDIT_SQL_EXECUTION(MANIFEST_ID, EXECUTED_AT)'; EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF; END;
/

BEGIN EXECUTE IMMEDIATE 'CREATE UNIQUE INDEX SERV_MED_UX_SEC_ROLE_CODE ON SERV_MED_SEC_ROLE(CODE)'; EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE 'CREATE UNIQUE INDEX SERV_MED_UX_SEC_PERM_CODE ON SERV_MED_SEC_PERMISSION(CODE)'; EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE 'CREATE UNIQUE INDEX SERV_MED_UX_SEC_USER_USERNAME ON SERV_MED_SEC_USER(USERNAME)'; EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE 'CREATE UNIQUE INDEX SERV_MED_UX_SEC_USER_EMAIL ON SERV_MED_SEC_USER(EMAIL)'; EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE 'CREATE UNIQUE INDEX SERV_MED_UX_SEC_USER_ROLE ON SERV_MED_SEC_USER_ROLE(USER_ID, ROLE_ID)'; EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE 'CREATE UNIQUE INDEX SERV_MED_UX_SEC_ROLE_PERM ON SERV_MED_SEC_ROLE_PERMISSION(ROLE_ID, PERMISSION_ID)'; EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF; END;
/

BEGIN EXECUTE IMMEDIATE 'CREATE UNIQUE INDEX SERV_MED_UX_MENU_CODE ON SERV_MED_MENU(CODE)'; EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE 'CREATE INDEX SERV_MED_IX_MENU_PARENT ON SERV_MED_MENU(PARENT_ID)'; EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF; END;
/

BEGIN EXECUTE IMMEDIATE 'CREATE UNIQUE INDEX SERV_MED_UX_JOB_CATALOG_CODE ON SERV_MED_JOB_CATALOG(CODE)'; EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE 'CREATE UNIQUE INDEX SERV_MED_UX_NOTIF_TPL_CH ON SERV_MED_NOTIF_TEMPLATE(CODE, CHANNEL)'; EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF; END;
/

BEGIN EXECUTE IMMEDIATE 'CREATE UNIQUE INDEX SERV_MED_UX_FS_FILE_BUSKEY ON SERV_MED_FS_FILE(BUSINESS_KEY)'; EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF; END;
/
-- NO unico: un mismo binario (checksum) puede adjuntarse legitimamente a varios NSS/consultas (ver docs/plan-etl-migracion-files.md seccion 3)
BEGIN EXECUTE IMMEDIATE 'CREATE INDEX SERV_MED_UX_FS_FILE_CHECKSUM ON SERV_MED_FS_FILE(CHECKSUM_SHA256)'; EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF; END;
/
-- Patrones de acceso de los adjuntos (FsFileRepository): por tipo y por NSS+tipo
BEGIN EXECUTE IMMEDIATE 'CREATE INDEX SERV_MED_IDX_FS_FILE_TYPE ON SERV_MED_FS_FILE(FILE_TYPE)'; EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE 'CREATE INDEX SERV_MED_IDX_FS_FILE_NSS_TYPE ON SERV_MED_FS_FILE(NSS, FILE_TYPE)'; EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE 'CREATE UNIQUE INDEX SERV_MED_UX_FS_FILE_VERSION_NO ON SERV_MED_FS_FILE_VERSION(FILE_ID, VERSION_NO)'; EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE 'CREATE INDEX SERV_MED_IX_FS_ACC_LOG ON SERV_MED_FS_FILE_ACCESS_LOG(FILE_ID, ACCESSED_AT)'; EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE 'CREATE UNIQUE INDEX SERV_MED_UX_FS_POLICY_CODE ON SERV_MED_FS_FILE_POLICY(POLICY_CODE)'; EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF; END;
/

PROMPT ============================================================
PROMPT [3/8] CONSTRAINTS Y FKs (verificadas por diccionario)
PROMPT ============================================================

DECLARE
  v_count NUMBER;
  PROCEDURE add_constraint_if_not_exists(
    p_table_name      IN VARCHAR2,
    p_constraint_name IN VARCHAR2,
    p_ddl             IN CLOB
  ) IS
  BEGIN
    SELECT COUNT(*) INTO v_count
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = UPPER(p_table_name)
       AND CONSTRAINT_NAME = UPPER(p_constraint_name);

    IF v_count = 0 THEN
      EXECUTE IMMEDIATE p_ddl;
      DBMS_OUTPUT.PUT_LINE('Constraint creada: ' || p_constraint_name);
    ELSE
      DBMS_OUTPUT.PUT_LINE('Constraint existente (omitida): ' || p_constraint_name);
    END IF;
  END;
BEGIN
  -- CHECKS
  add_constraint_if_not_exists('SERV_MED_SEC_USER','SERV_MED_CK_SEC_USER_ACTIVE',
    'ALTER TABLE SERV_MED_SEC_USER ADD CONSTRAINT SERV_MED_CK_SEC_USER_ACTIVE CHECK (ACTIVE IN (''Y'',''N''))');

  add_constraint_if_not_exists('SERV_MED_SEC_ROLE','SERV_MED_CK_SEC_ROLE_ACTIVE',
    'ALTER TABLE SERV_MED_SEC_ROLE ADD CONSTRAINT SERV_MED_CK_SEC_ROLE_ACTIVE CHECK (ACTIVE IN (''Y'',''N''))');

  add_constraint_if_not_exists('SERV_MED_SEC_PERMISSION','SERV_MED_CK_SEC_PERM_ACTIVE',
    'ALTER TABLE SERV_MED_SEC_PERMISSION ADD CONSTRAINT SERV_MED_CK_SEC_PERM_ACTIVE CHECK (ACTIVE IN (''Y'',''N''))');

  add_constraint_if_not_exists('SERV_MED_MENU','SERV_MED_CK_MENU_ACTIVE',
    'ALTER TABLE SERV_MED_MENU ADD CONSTRAINT SERV_MED_CK_MENU_ACTIVE CHECK (ACTIVE IN (''Y'',''N''))');

  add_constraint_if_not_exists('SERV_MED_JOB_CATALOG','SERV_MED_CK_JOB_CAT_ENABLED',
    'ALTER TABLE SERV_MED_JOB_CATALOG ADD CONSTRAINT SERV_MED_CK_JOB_CAT_ENABLED CHECK (ENABLED IN (''Y'',''N''))');

  add_constraint_if_not_exists('SERV_MED_NOTIF_TEMPLATE','SERV_MED_CK_NOTIF_TPL_ACTIVE',
    'ALTER TABLE SERV_MED_NOTIF_TEMPLATE ADD CONSTRAINT SERV_MED_CK_NOTIF_TPL_ACTIVE CHECK (ACTIVE IN (''Y'',''N''))');

  add_constraint_if_not_exists('SERV_MED_FS_FILE_POLICY','SERV_MED_CK_FS_POLICY_ACTIVE',
    'ALTER TABLE SERV_MED_FS_FILE_POLICY ADD CONSTRAINT SERV_MED_CK_FS_POLICY_ACTIVE CHECK (ACTIVE IN (''Y'',''N''))');

  add_constraint_if_not_exists('SERV_MED_FS_FILE','SERV_MED_CK_FS_FILE_STATUS',
    'ALTER TABLE SERV_MED_FS_FILE ADD CONSTRAINT SERV_MED_CK_FS_FILE_STATUS CHECK (STATUS IN (''ACTIVE'',''DELETED'',''ARCHIVED''))');

  -- FKs
  add_constraint_if_not_exists('SERV_MED_SEC_USER_ROLE','SERV_MED_FK_SEC_USER_ROLE_USER',
    'ALTER TABLE SERV_MED_SEC_USER_ROLE ADD CONSTRAINT SERV_MED_FK_SEC_USER_ROLE_USER FOREIGN KEY (USER_ID) REFERENCES SERV_MED_SEC_USER(ID)');

  add_constraint_if_not_exists('SERV_MED_SEC_USER_ROLE','SERV_MED_FK_SEC_USER_ROLE_ROLE',
    'ALTER TABLE SERV_MED_SEC_USER_ROLE ADD CONSTRAINT SERV_MED_FK_SEC_USER_ROLE_ROLE FOREIGN KEY (ROLE_ID) REFERENCES SERV_MED_SEC_ROLE(ID)');

  add_constraint_if_not_exists('SERV_MED_SEC_ROLE_PERMISSION','SERV_MED_FK_SEC_ROLE_PERM_ROLE',
    'ALTER TABLE SERV_MED_SEC_ROLE_PERMISSION ADD CONSTRAINT SERV_MED_FK_SEC_ROLE_PERM_ROLE FOREIGN KEY (ROLE_ID) REFERENCES SERV_MED_SEC_ROLE(ID)');

  add_constraint_if_not_exists('SERV_MED_SEC_ROLE_PERMISSION','SERV_MED_FK_SEC_ROLE_PERM_PERM',
    'ALTER TABLE SERV_MED_SEC_ROLE_PERMISSION ADD CONSTRAINT SERV_MED_FK_SEC_ROLE_PERM_PERM FOREIGN KEY (PERMISSION_ID) REFERENCES SERV_MED_SEC_PERMISSION(ID)');

  add_constraint_if_not_exists('SERV_MED_MENU','SERV_MED_FK_MENU_PARENT',
    'ALTER TABLE SERV_MED_MENU ADD CONSTRAINT SERV_MED_FK_MENU_PARENT FOREIGN KEY (PARENT_ID) REFERENCES SERV_MED_MENU(ID)');

  add_constraint_if_not_exists('SERV_MED_FS_FILE_VERSION','SERV_MED_FK_FS_VERSION_FILE',
    'ALTER TABLE SERV_MED_FS_FILE_VERSION ADD CONSTRAINT SERV_MED_FK_FS_VERSION_FILE FOREIGN KEY (FILE_ID) REFERENCES SERV_MED_FS_FILE(ID)');

  add_constraint_if_not_exists('SERV_MED_FS_FILE_ACCESS_LOG','SERV_MED_FK_FS_ACCESS_LOG_FILE',
    'ALTER TABLE SERV_MED_FS_FILE_ACCESS_LOG ADD CONSTRAINT SERV_MED_FK_FS_ACCESS_LOG_FILE FOREIGN KEY (FILE_ID) REFERENCES SERV_MED_FS_FILE(ID)');
END;
/

PROMPT ============================================================
PROMPT [4/8] SEEDS BASE
PROMPT ============================================================

-- Catálogo política default de archivos
MERGE INTO SERV_MED_FS_FILE_POLICY t
USING (SELECT 'DEFAULT_UPLOAD_POLICY' AS POLICY_CODE FROM dual) s
ON (t.POLICY_CODE = s.POLICY_CODE)
WHEN NOT MATCHED THEN
  INSERT (POLICY_CODE, DESCRIPTION, MAX_SIZE_BYTES, ALLOWED_MIMES, RETENTION_DAYS, ACTIVE, CREATED_AT, CREATED_BY)
  VALUES ('DEFAULT_UPLOAD_POLICY', 'Política por defecto para cargas', 10485760, 'application/pdf,image/png,image/jpeg', 3650, 'Y', SYSTIMESTAMP, USER);
/

-- Roles
MERGE INTO SERV_MED_SEC_ROLE t
USING (SELECT 'ROLE_ADMIN' AS CODE, 'Administrador' AS NAME FROM dual) s
ON (t.CODE = s.CODE)
WHEN NOT MATCHED THEN
  INSERT (CODE, NAME, DESCRIPTION, ACTIVE, CREATED_AT, CREATED_BY)
  VALUES (s.CODE, s.NAME, 'Rol con acceso total base', 'Y', SYSTIMESTAMP, USER);
/

-- Permisos
MERGE INTO SERV_MED_SEC_PERMISSION t
USING (
  SELECT 'MENU_VIEW' AS CODE, 'Ver menú' AS NAME, 'CORE' AS MODULE FROM dual
  UNION ALL SELECT 'USER_MANAGE', 'Gestionar usuarios', 'SECURITY' FROM dual
  UNION ALL SELECT 'ROLE_MANAGE', 'Gestionar roles', 'SECURITY' FROM dual
  UNION ALL SELECT 'FILE_UPLOAD', 'Subir archivos', 'FILES' FROM dual
  UNION ALL SELECT 'FILE_DOWNLOAD', 'Descargar archivos', 'FILES' FROM dual
) s
ON (t.CODE = s.CODE)
WHEN NOT MATCHED THEN
  INSERT (CODE, NAME, MODULE, ACTIVE, CREATED_AT, CREATED_BY)
  VALUES (s.CODE, s.NAME, s.MODULE, 'Y', SYSTIMESTAMP, USER);
/

-- Asignar permisos al admin role
MERGE INTO SERV_MED_SEC_ROLE_PERMISSION rp
USING (
  SELECT r.ID AS ROLE_ID, p.ID AS PERMISSION_ID
  FROM SERV_MED_SEC_ROLE r
  JOIN SERV_MED_SEC_PERMISSION p ON r.CODE = 'ROLE_ADMIN'
) s
ON (rp.ROLE_ID = s.ROLE_ID AND rp.PERMISSION_ID = s.PERMISSION_ID)
WHEN NOT MATCHED THEN
  INSERT (ROLE_ID, PERMISSION_ID, CREATED_AT, CREATED_BY)
  VALUES (s.ROLE_ID, s.PERMISSION_ID, SYSTIMESTAMP, USER);
/

-- Usuario admin
MERGE INTO SERV_MED_SEC_USER u
USING (
  SELECT 'admin' AS USERNAME,
         'admin@local' AS EMAIL,
         '$2a$10$7EqJtq98hPqEX7fNZaFWoOHi4pi2Vf5A9dM0f2WnQ9q7v5s8QyM7a' AS PASSWORD_HASH,
         'Administrador' AS DISPLAY_NAME
  FROM dual
) s
ON (u.USERNAME = s.USERNAME)
WHEN NOT MATCHED THEN
  INSERT (USERNAME, EMAIL, PASSWORD_HASH, DISPLAY_NAME, ACCOUNT_STATUS, ACTIVE, CREATED_AT, CREATED_BY)
  VALUES (s.USERNAME, s.EMAIL, s.PASSWORD_HASH, s.DISPLAY_NAME, 'ACTIVE', 'Y', SYSTIMESTAMP, USER);
/

MERGE INTO SERV_MED_SEC_USER_ROLE ur
USING (
  SELECT u.ID AS USER_ID, r.ID AS ROLE_ID
  FROM SERV_MED_SEC_USER u
  JOIN SERV_MED_SEC_ROLE r ON r.CODE = 'ROLE_ADMIN'
  WHERE u.USERNAME = 'admin'
) s
ON (ur.USER_ID = s.USER_ID AND ur.ROLE_ID = s.ROLE_ID)
WHEN NOT MATCHED THEN
  INSERT (USER_ID, ROLE_ID, CREATED_AT, CREATED_BY)
  VALUES (s.USER_ID, s.ROLE_ID, SYSTIMESTAMP, USER);
/

-- Menú base
MERGE INTO SERV_MED_MENU t
USING (SELECT 'MNU_ROOT' AS CODE, 'Portal Salud' AS TITLE, '/' AS PATH, 0 AS ORDER_NO FROM dual) s
ON (t.CODE = s.CODE)
WHEN NOT MATCHED THEN
  INSERT (CODE, PARENT_ID, TITLE, PATH, ICON, ORDER_NO, REQUIRED_PERMISSION_CODE, ACTIVE, CREATED_AT, CREATED_BY)
  VALUES (s.CODE, NULL, s.TITLE, s.PATH, 'home', s.ORDER_NO, 'MENU_VIEW', 'Y', SYSTIMESTAMP, USER);
/

MERGE INTO SERV_MED_MENU t
USING (
  SELECT 'MNU_SECURITY' AS CODE, 'Seguridad' AS TITLE, '/security' AS PATH, 'shield' AS ICON, 10 AS ORDER_NO, 'MENU_VIEW' AS PERM FROM dual
  UNION ALL
  SELECT 'MNU_FILES', 'Archivos', '/files', 'folder', 20, 'MENU_VIEW' FROM dual
) s
ON (t.CODE = s.CODE)
WHEN NOT MATCHED THEN
  INSERT (CODE, PARENT_ID, TITLE, PATH, ICON, ORDER_NO, REQUIRED_PERMISSION_CODE, ACTIVE, CREATED_AT, CREATED_BY)
  VALUES (
    s.CODE,
    (SELECT ID FROM SERV_MED_MENU WHERE CODE = 'MNU_ROOT'),
    s.TITLE,
    s.PATH,
    s.ICON,
    s.ORDER_NO,
    s.PERM,
    'Y',
    SYSTIMESTAMP,
    USER
  );
/

-- Jobs base
MERGE INTO SERV_MED_JOB_CATALOG t
USING (
  SELECT 'JOB_PURGE_AUDIT' AS CODE, 'Purgar auditoría antigua' AS NAME, '0 0 3 * * ?' AS CRON_EXPRESSION FROM dual
  UNION ALL
  SELECT 'JOB_FILE_ORPHAN_SCAN', 'Escaneo de huérfanos de archivos', '0 0/30 * * * ?' FROM dual
) s
ON (t.CODE = s.CODE)
WHEN NOT MATCHED THEN
  INSERT (CODE, NAME, CRON_EXPRESSION, ENABLED, DESCRIPTION, CREATED_AT, CREATED_BY)
  VALUES (s.CODE, s.NAME, s.CRON_EXPRESSION, 'Y', 'Job base inicial', SYSTIMESTAMP, USER);
/

-- Templates de notificación
MERGE INTO SERV_MED_NOTIF_TEMPLATE t
USING (
  SELECT 'WELCOME' AS CODE, 'EMAIL' AS CHANNEL, 'Bienvenido a Portal Salud' AS SUBJECT,
         'Hola ${name}, tu cuenta ha sido creada correctamente.' AS BODY_TEMPLATE FROM dual
  UNION ALL
  SELECT 'PASSWORD_RESET', 'EMAIL', 'Recuperación de contraseña',
         'Hola ${name}, usa este código para restablecer tu contraseña: ${code}' FROM dual
) s
ON (t.CODE = s.CODE AND t.CHANNEL = s.CHANNEL)
WHEN NOT MATCHED THEN
  INSERT (CODE, CHANNEL, SUBJECT, BODY_TEMPLATE, ACTIVE, CREATED_AT, CREATED_BY)
  VALUES (s.CODE, s.CHANNEL, s.SUBJECT, s.BODY_TEMPLATE, 'Y', SYSTIMESTAMP, USER);
/

PROMPT ============================================================
PROMPT [5/8] VISTAS
PROMPT ============================================================

CREATE OR REPLACE VIEW SERV_MED_VW_ACTIVE_USERS AS
SELECT u.ID,
       u.USERNAME,
       u.EMAIL,
       u.DISPLAY_NAME,
       u.LAST_LOGIN_AT
  FROM SERV_MED_SEC_USER u
 WHERE u.ACTIVE = 'Y'
   AND u.ACCOUNT_STATUS = 'ACTIVE';
/

CREATE OR REPLACE VIEW SERV_MED_VW_FILE_LATEST AS
SELECT f.ID,
       f.BUSINESS_KEY,
       f.ORIGINAL_NAME,
       f.MIME_TYPE,
       f.SIZE_BYTES,
       f.STATUS,
       f.CURRENT_VERSION,
       f.STORAGE_PATH,
       f.UPDATED_AT
  FROM SERV_MED_FS_FILE f
 WHERE f.STATUS = 'ACTIVE';
/

PROMPT ============================================================
PROMPT [6/8] PROCEDIMIENTOS
PROMPT ============================================================

CREATE OR REPLACE PROCEDURE SERV_MED_SP_LOG_SQL_EXEC(
    p_manifest_id IN VARCHAR2,
    p_script_name IN VARCHAR2,
    p_phase_name  IN VARCHAR2,
    p_status      IN VARCHAR2,
    p_message     IN VARCHAR2
) AS
BEGIN
  INSERT INTO SERV_MED_AUDIT_SQL_EXECUTION(
    MANIFEST_ID,
    SCRIPT_NAME,
    PHASE_NAME,
    STATUS,
    MESSAGE,
    EXECUTED_AT,
    EXECUTED_BY
  ) VALUES (
    p_manifest_id,
    p_script_name,
    p_phase_name,
    p_status,
    SUBSTR(p_message,1,2000),
    SYSTIMESTAMP,
    USER
  );
END;
/

PROMPT ============================================================
PROMPT [7/8] TRIGGERS
PROMPT ============================================================

CREATE OR REPLACE TRIGGER SERV_MED_TRG_SEC_USER_BU
BEFORE UPDATE ON SERV_MED_SEC_USER
FOR EACH ROW
BEGIN
  :NEW.UPDATED_AT := SYSTIMESTAMP;
  :NEW.UPDATED_BY := USER;
END;
/

CREATE OR REPLACE TRIGGER SERV_MED_TRG_SEC_ROLE_BU
BEFORE UPDATE ON SERV_MED_SEC_ROLE
FOR EACH ROW
BEGIN
  :NEW.UPDATED_AT := SYSTIMESTAMP;
  :NEW.UPDATED_BY := USER;
END;
/

CREATE OR REPLACE TRIGGER SERV_MED_TRG_SEC_PERM_BU
BEFORE UPDATE ON SERV_MED_SEC_PERMISSION
FOR EACH ROW
BEGIN
  :NEW.UPDATED_AT := SYSTIMESTAMP;
  :NEW.UPDATED_BY := USER;
END;
/

CREATE OR REPLACE TRIGGER SERV_MED_TRG_MENU_BU
BEFORE UPDATE ON SERV_MED_MENU
FOR EACH ROW
BEGIN
  :NEW.UPDATED_AT := SYSTIMESTAMP;
  :NEW.UPDATED_BY := USER;
END;
/

CREATE OR REPLACE TRIGGER SERV_MED_TRG_JOB_CATALOG_BU
BEFORE UPDATE ON SERV_MED_JOB_CATALOG
FOR EACH ROW
BEGIN
  :NEW.UPDATED_AT := SYSTIMESTAMP;
  :NEW.UPDATED_BY := USER;
END;
/

CREATE OR REPLACE TRIGGER SERV_MED_TRG_NOTIF_TPL_BU
BEFORE UPDATE ON SERV_MED_NOTIF_TEMPLATE
FOR EACH ROW
BEGIN
  :NEW.UPDATED_AT := SYSTIMESTAMP;
  :NEW.UPDATED_BY := USER;
END;
/

CREATE OR REPLACE TRIGGER SERV_MED_TRG_FS_FILE_BU
BEFORE UPDATE ON SERV_MED_FS_FILE
FOR EACH ROW
BEGIN
  :NEW.UPDATED_AT := SYSTIMESTAMP;
  :NEW.UPDATED_BY := USER;
END;
/

CREATE OR REPLACE TRIGGER SERV_MED_TRG_FS_FILE_POLICY_BU
BEFORE UPDATE ON SERV_MED_FS_FILE_POLICY
FOR EACH ROW
BEGIN
  :NEW.UPDATED_AT := SYSTIMESTAMP;
  :NEW.UPDATED_BY := USER;
END;
/

PROMPT ============================================================
PROMPT [8/8] POST EXECUTION CHECKS
PROMPT ============================================================

PROMPT [CHECK 1] Required file tables
SELECT table_name
  FROM user_tables
 WHERE table_name IN ('SERV_MED_FS_FILE','SERV_MED_FS_FILE_VERSION','SERV_MED_FS_FILE_ACCESS_LOG','SERV_MED_FS_FILE_POLICY','SERV_MED_FS_FILE_ORPHAN')
 ORDER BY table_name;
/

PROMPT [CHECK 2] Admin role exists
SELECT CODE, NAME, ACTIVE FROM SERV_MED_SEC_ROLE WHERE CODE = 'ROLE_ADMIN';
/

PROMPT [CHECK 3] Base permissions count
SELECT COUNT(*) AS PERMISSIONS_COUNT FROM SERV_MED_SEC_PERMISSION;
/

PROMPT [CHECK 4] Base menu loaded
SELECT CODE, TITLE, PATH, ORDER_NO FROM SERV_MED_MENU ORDER BY ORDER_NO, CODE;
/

PROMPT [CHECK 5] Manifest log table ready
SELECT COUNT(*) AS EXEC_LOG_COUNT FROM SERV_MED_AUDIT_SQL_EXECUTION;
/

PROMPT ============================================================
PROMPT FIN OK - 00_init_oracle21c.sql
PROMPT ============================================================


-- =====================================================================================
-- PASO 2 de 9  -  01_rbac_local.sql
-- RBAC local: SERV_MED_MENU_ROLE + siembra de roles (USER, ADM, ENFERMERO, ROLE_ADMIN)
--    y de los 14 menus del expediente.
-- =====================================================================================
-- 01_rbac_local.sql
-- RBAC local para Portal Salud (roles/menus/usuarios propios) - idempotente.
-- Ubicacion: src/main/resources/db/sql
--
-- Contexto: activa el esquema SERV_MED_SEC_*/SERV_MED_MENU ya sembrado (dormant) en
-- 00_init_oracle21c.sql para resolver menus/permisos localmente en vez de
-- contra el WS ORDS compartido con el portal PHP legacy. Ver docs/plan-rbac-local.md.
--
-- Este script asume que 00_init_oracle21c.sql ya corrio (SERV_MED_SEC_ROLE, SERV_MED_SEC_USER,
-- SERV_MED_MENU, etc. ya existen). Ejecutar despues de ese, no en su lugar.

SET SERVEROUTPUT ON;

PROMPT ============================================================
PROMPT [1/4] TABLA SERV_MED_MENU_ROLE (join menu<->rol, espejo de TBL_APPS_ROL_MENU de ORDS)
PROMPT ============================================================

BEGIN
  EXECUTE IMMEDIATE q'[
    CREATE TABLE SERV_MED_MENU_ROLE (
      ID          NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
      MENU_ID     NUMBER NOT NULL,
      ROLE_ID     NUMBER NOT NULL,
      CREATED_AT  TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL,
      CREATED_BY  VARCHAR2(100) DEFAULT USER NOT NULL
    )
  ]';
  DBMS_OUTPUT.PUT_LINE('Tabla creada: SERV_MED_MENU_ROLE');
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; ELSE DBMS_OUTPUT.PUT_LINE('Tabla existente (omitida): SERV_MED_MENU_ROLE'); END IF; END;
/

BEGIN
  EXECUTE IMMEDIATE 'CREATE UNIQUE INDEX SERV_MED_UX_MENU_ROLE ON SERV_MED_MENU_ROLE(MENU_ID, ROLE_ID)';
  DBMS_OUTPUT.PUT_LINE('Indice creado: SERV_MED_UX_MENU_ROLE');
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; ELSE DBMS_OUTPUT.PUT_LINE('Indice existente (omitido): SERV_MED_UX_MENU_ROLE'); END IF; END;
/

DECLARE
  v_count NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_count FROM USER_CONSTRAINTS WHERE CONSTRAINT_NAME = 'SERV_MED_FK_MENU_ROLE_MENU';
  IF v_count = 0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE SERV_MED_MENU_ROLE ADD CONSTRAINT SERV_MED_FK_MENU_ROLE_MENU FOREIGN KEY (MENU_ID) REFERENCES SERV_MED_MENU(ID)';
    DBMS_OUTPUT.PUT_LINE('Constraint creada: SERV_MED_FK_MENU_ROLE_MENU');
  ELSE
    DBMS_OUTPUT.PUT_LINE('Constraint existente (omitida): SERV_MED_FK_MENU_ROLE_MENU');
  END IF;

  SELECT COUNT(*) INTO v_count FROM USER_CONSTRAINTS WHERE CONSTRAINT_NAME = 'SERV_MED_FK_MENU_ROLE_ROLE';
  IF v_count = 0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE SERV_MED_MENU_ROLE ADD CONSTRAINT SERV_MED_FK_MENU_ROLE_ROLE FOREIGN KEY (ROLE_ID) REFERENCES SERV_MED_SEC_ROLE(ID)';
    DBMS_OUTPUT.PUT_LINE('Constraint creada: SERV_MED_FK_MENU_ROLE_ROLE');
  ELSE
    DBMS_OUTPUT.PUT_LINE('Constraint existente (omitida): SERV_MED_FK_MENU_ROLE_ROLE');
  END IF;
END;
/

PROMPT ============================================================
PROMPT [2/4] SERV_MED_SEC_USER.PASSWORD_HASH -> NULLABLE (usuarios de solo-rol nunca autentican local)
PROMPT ============================================================

DECLARE
  v_nullable USER_TAB_COLUMNS.NULLABLE%TYPE;
BEGIN
  SELECT NULLABLE INTO v_nullable FROM USER_TAB_COLUMNS
   WHERE TABLE_NAME = 'SERV_MED_SEC_USER' AND COLUMN_NAME = 'PASSWORD_HASH';
  IF v_nullable = 'N' THEN
    EXECUTE IMMEDIATE 'ALTER TABLE SERV_MED_SEC_USER MODIFY (PASSWORD_HASH NULL)';
    DBMS_OUTPUT.PUT_LINE('Columna alterada: SERV_MED_SEC_USER.PASSWORD_HASH ahora NULL');
  ELSE
    DBMS_OUTPUT.PUT_LINE('Columna ya era NULL (omitida): SERV_MED_SEC_USER.PASSWORD_HASH');
  END IF;
END;
/

PROMPT ============================================================
PROMPT [3/4] SEED: 14 modulos del submenu (mismos nombres/iconos que fragments/nss-modules.html)
PROMPT ============================================================

MERGE INTO SERV_MED_MENU t USING (SELECT 'EXPEDIENTE_GENERAL' code, 'Expediente General' title, 'ri-booklet-line' icon, 1 order_no FROM dual) s
  ON (t.CODE = s.code) WHEN NOT MATCHED THEN INSERT (CODE, TITLE, ICON, ORDER_NO) VALUES (s.code, s.title, s.icon, s.order_no);
MERGE INTO SERV_MED_MENU t USING (SELECT 'LABORATORIO' code, 'E. Laboratorio' title, 'ri-flask-line' icon, 2 order_no FROM dual) s
  ON (t.CODE = s.code) WHEN NOT MATCHED THEN INSERT (CODE, TITLE, ICON, ORDER_NO) VALUES (s.code, s.title, s.icon, s.order_no);
MERGE INTO SERV_MED_MENU t USING (SELECT 'HISTORICO_EM' code, 'Historico E.M' title, 'ri-history-line' icon, 3 order_no FROM dual) s
  ON (t.CODE = s.code) WHEN NOT MATCHED THEN INSERT (CODE, TITLE, ICON, ORDER_NO) VALUES (s.code, s.title, s.icon, s.order_no);
MERGE INTO SERV_MED_MENU t USING (SELECT 'NOTA_MEDICA' code, 'Nota Medica' title, 'ri-file-text-line' icon, 4 order_no FROM dual) s
  ON (t.CODE = s.code) WHEN NOT MATCHED THEN INSERT (CODE, TITLE, ICON, ORDER_NO) VALUES (s.code, s.title, s.icon, s.order_no);
MERGE INTO SERV_MED_MENU t USING (SELECT 'NOTA_INCAPACIDAD' code, 'Nota Incapacidad' title, 'ri-file-paper-2-line' icon, 5 order_no FROM dual) s
  ON (t.CODE = s.code) WHEN NOT MATCHED THEN INSERT (CODE, TITLE, ICON, ORDER_NO) VALUES (s.code, s.title, s.icon, s.order_no);
MERGE INTO SERV_MED_MENU t USING (SELECT 'ARCHIVO_CONSULTAS' code, 'Archivo de Consultas' title, 'ri-book-2-line' icon, 6 order_no FROM dual) s
  ON (t.CODE = s.code) WHEN NOT MATCHED THEN INSERT (CODE, TITLE, ICON, ORDER_NO) VALUES (s.code, s.title, s.icon, s.order_no);
MERGE INTO SERV_MED_MENU t USING (SELECT 'CONSULTA_MEDICA' code, 'Consulta Medica' title, 'ri-health-book-line' icon, 7 order_no FROM dual) s
  ON (t.CODE = s.code) WHEN NOT MATCHED THEN INSERT (CODE, TITLE, ICON, ORDER_NO) VALUES (s.code, s.title, s.icon, s.order_no);
MERGE INTO SERV_MED_MENU t USING (SELECT 'INCAPACIDADES' code, 'Incapacidades' title, 'ri-first-aid-kit-line' icon, 8 order_no FROM dual) s
  ON (t.CODE = s.code) WHEN NOT MATCHED THEN INSERT (CODE, TITLE, ICON, ORDER_NO) VALUES (s.code, s.title, s.icon, s.order_no);
MERGE INTO SERV_MED_MENU t USING (SELECT 'ARCHIVO_INCAPACIDADES' code, 'Archivo Incapacidades' title, 'ri-archive-line' icon, 9 order_no FROM dual) s
  ON (t.CODE = s.code) WHEN NOT MATCHED THEN INSERT (CODE, TITLE, ICON, ORDER_NO) VALUES (s.code, s.title, s.icon, s.order_no);
MERGE INTO SERV_MED_MENU t USING (SELECT 'EXAMEN_MEDICO' code, 'Examen Medico' title, 'ri-heart-pulse-line' icon, 10 order_no FROM dual) s
  ON (t.CODE = s.code) WHEN NOT MATCHED THEN INSERT (CODE, TITLE, ICON, ORDER_NO) VALUES (s.code, s.title, s.icon, s.order_no);
MERGE INTO SERV_MED_MENU t USING (SELECT 'PRETEST' code, 'Pre-Test' title, 'ri-survey-line' icon, 11 order_no FROM dual) s
  ON (t.CODE = s.code) WHEN NOT MATCHED THEN INSERT (CODE, TITLE, ICON, ORDER_NO) VALUES (s.code, s.title, s.icon, s.order_no);
MERGE INTO SERV_MED_MENU t USING (SELECT 'ANTIDOPING' code, 'Antidoping' title, 'ri-test-tube-line' icon, 12 order_no FROM dual) s
  ON (t.CODE = s.code) WHEN NOT MATCHED THEN INSERT (CODE, TITLE, ICON, ORDER_NO) VALUES (s.code, s.title, s.icon, s.order_no);
MERGE INTO SERV_MED_MENU t USING (SELECT 'ACCIDENTES' code, 'Accidentes' title, 'ri-alarm-warning-line' icon, 13 order_no FROM dual) s
  ON (t.CODE = s.code) WHEN NOT MATCHED THEN INSERT (CODE, TITLE, ICON, ORDER_NO) VALUES (s.code, s.title, s.icon, s.order_no);
MERGE INTO SERV_MED_MENU t USING (SELECT 'MATERNIDAD' code, 'Maternidad' title, 'ri-women-line' icon, 14 order_no FROM dual) s
  ON (t.CODE = s.code) WHEN NOT MATCHED THEN INSERT (CODE, TITLE, ICON, ORDER_NO) VALUES (s.code, s.title, s.icon, s.order_no);

PROMPT ============================================================
PROMPT [4/4] SEED: 3 roles reales (USER/ADM/ENFERMERO, mismos nombres que ORDS id_app=13)
PROMPT y sus asignaciones de menu (replica lo verificado en vivo contra ORDS 2026-08-26)
PROMPT ============================================================

MERGE INTO SERV_MED_SEC_ROLE t USING (SELECT 'USER' code, 'Usuario' name FROM dual) s
  ON (t.CODE = s.code) WHEN NOT MATCHED THEN INSERT (CODE, NAME) VALUES (s.code, s.name);
MERGE INTO SERV_MED_SEC_ROLE t USING (SELECT 'ADM' code, 'Administrador clinico' name FROM dual) s
  ON (t.CODE = s.code) WHEN NOT MATCHED THEN INSERT (CODE, NAME) VALUES (s.code, s.name);
MERGE INTO SERV_MED_SEC_ROLE t USING (SELECT 'ENFERMERO' code, 'Enfermero' name FROM dual) s
  ON (t.CODE = s.code) WHEN NOT MATCHED THEN INSERT (CODE, NAME) VALUES (s.code, s.name);
COMMIT;

-- ADM: los 14 modulos (equivalente al id_rol=2 real, acceso completo verificado en vivo).
INSERT INTO SERV_MED_MENU_ROLE (MENU_ID, ROLE_ID)
SELECT m.ID, r.ID FROM SERV_MED_MENU m, SERV_MED_SEC_ROLE r
 WHERE r.CODE = 'ADM'
   AND m.CODE IN ('EXPEDIENTE_GENERAL','LABORATORIO','HISTORICO_EM','NOTA_MEDICA','NOTA_INCAPACIDAD',
                  'ARCHIVO_CONSULTAS','CONSULTA_MEDICA','INCAPACIDADES','ARCHIVO_INCAPACIDADES',
                  'EXAMEN_MEDICO','PRETEST','ANTIDOPING','ACCIDENTES','MATERNIDAD')
   AND NOT EXISTS (SELECT 1 FROM SERV_MED_MENU_ROLE x WHERE x.MENU_ID = m.ID AND x.ROLE_ID = r.ID);

-- USER: todo excepto Antidoping/Accidentes/Maternidad (equivalente al id_rol=1 real).
INSERT INTO SERV_MED_MENU_ROLE (MENU_ID, ROLE_ID)
SELECT m.ID, r.ID FROM SERV_MED_MENU m, SERV_MED_SEC_ROLE r
 WHERE r.CODE = 'USER'
   AND m.CODE IN ('EXPEDIENTE_GENERAL','LABORATORIO','HISTORICO_EM','NOTA_MEDICA','NOTA_INCAPACIDAD',
                  'ARCHIVO_CONSULTAS','CONSULTA_MEDICA','INCAPACIDADES','ARCHIVO_INCAPACIDADES',
                  'EXAMEN_MEDICO','PRETEST')
   AND NOT EXISTS (SELECT 1 FROM SERV_MED_MENU_ROLE x WHERE x.MENU_ID = m.ID AND x.ROLE_ID = r.ID);

-- ENFERMERO: sin Expediente General/Laboratorio ni Antidoping/Accidentes/Maternidad
-- (equivalente al id_rol=3 real).
INSERT INTO SERV_MED_MENU_ROLE (MENU_ID, ROLE_ID)
SELECT m.ID, r.ID FROM SERV_MED_MENU m, SERV_MED_SEC_ROLE r
 WHERE r.CODE = 'ENFERMERO'
   AND m.CODE IN ('HISTORICO_EM','NOTA_MEDICA','NOTA_INCAPACIDAD','ARCHIVO_CONSULTAS',
                  'CONSULTA_MEDICA','INCAPACIDADES','ARCHIVO_INCAPACIDADES','EXAMEN_MEDICO','PRETEST')
   AND NOT EXISTS (SELECT 1 FROM SERV_MED_MENU_ROLE x WHERE x.MENU_ID = m.ID AND x.ROLE_ID = r.ID);

COMMIT;

PROMPT ============================================================
PROMPT [CHECK] Verificacion post-ejecucion
PROMPT ============================================================
SELECT r.CODE role_code, COUNT(*) menus_asignados
  FROM SERV_MED_MENU_ROLE mr JOIN SERV_MED_SEC_ROLE r ON r.ID = mr.ROLE_ID
 GROUP BY r.CODE ORDER BY r.CODE;


-- =====================================================================================
-- PASO 3 de 9  -  02_fix_expediente_general_duplicado.sql
-- Correccion: quita el menu "Expediente General" duplicado.
-- =====================================================================================
-- Expediente General ya se muestra como encabezado estatico del grupo
-- (fragments/menu.html:17, #nssModulesGroup). No tiene 'action' en
-- fragments/nss-modules.html (es un contenedor, no un modulo real), asi que
-- asignarlo tambien como item de submenu solo produce un duplicado visual
-- ("Expediente General" aparece dos veces en el sidebar).
--
-- Bajo ORDS esto nunca pasaba porque id_menu=1 (Expediente General) nunca
-- estaba en TBL_APPS_ROL_MENU para ningun rol real. Quitamos la asignacion
-- equivalente en el esquema local para que ambas fuentes se comporten igual.
DELETE FROM SERV_MED_MENU_ROLE
WHERE MENU_ID = (SELECT ID FROM SERV_MED_MENU WHERE CODE = 'EXPEDIENTE_GENERAL');
COMMIT;

-- Verificacion: debe regresar 0 filas.
SELECT COUNT(*) AS asignaciones_restantes
FROM SERV_MED_MENU_ROLE mr
JOIN SERV_MED_MENU m ON m.ID = mr.MENU_ID
WHERE m.CODE = 'EXPEDIENTE_GENERAL';


-- =====================================================================================
-- PASO 4 de 9  -  03_menu_examenes.sql
-- Menus del grupo Examenes: ANTIDOPING_SELECCION, CONSUMIBLES, CAUSAS_CONSULTA.
-- =====================================================================================
-- =====================================================================================
-- 03_menu_examenes.sql  -  BASE DEL PORTAL (JDBC: APP_*), NO ORDS.
-- Da de alta en el catalogo de menus (SERV_MED_MENU) las 3 pantallas del grupo "Examenes" del
-- sidebar (Antidoping - Seleccion, Consumibles, Causas de consulta) para que se asignen
-- por rol en Administracion > Roles > Menus (SERV_MED_MENU_ROLE) igual que los modulos del
-- Expediente. Hasta ahora eran paginas sin restriccion de rol (17-sep-2026).
-- Idempotente (MERGE por CODE). Aplicar en cada ambiente (QA ONEWMS_QA, prod) con el
-- usuario del esquema del portal.
-- Despues de aplicarlo: asignar los 3 menus a los roles que deban verlos; los usuarios
-- sin el menu dejan de ver la opcion y reciben 403 en la ruta.
-- =====================================================================================
MERGE INTO SERV_MED_MENU t USING (SELECT 'ANTIDOPING_SELECCION' code, 'Antidoping - Seleccion' title, 'ri-shuffle-line' icon, 15 order_no FROM dual) s
  ON (t.CODE = s.code) WHEN NOT MATCHED THEN INSERT (CODE, TITLE, ICON, ORDER_NO) VALUES (s.code, s.title, s.icon, s.order_no);
MERGE INTO SERV_MED_MENU t USING (SELECT 'CONSUMIBLES' code, 'Consumibles' title, 'ri-file-list-3-line' icon, 16 order_no FROM dual) s
  ON (t.CODE = s.code) WHEN NOT MATCHED THEN INSERT (CODE, TITLE, ICON, ORDER_NO) VALUES (s.code, s.title, s.icon, s.order_no);
MERGE INTO SERV_MED_MENU t USING (SELECT 'CAUSAS_CONSULTA' code, 'Causas de consulta' title, 'ri-list-settings-line' icon, 17 order_no FROM dual) s
  ON (t.CODE = s.code) WHEN NOT MATCHED THEN INSERT (CODE, TITLE, ICON, ORDER_NO) VALUES (s.code, s.title, s.icon, s.order_no);
COMMIT;

-- Verificacion
SELECT ID, CODE, TITLE, ORDER_NO, ACTIVE FROM SERV_MED_MENU WHERE CODE IN ('ANTIDOPING_SELECCION','CONSUMIBLES','CAUSAS_CONSULTA') ORDER BY ORDER_NO;


-- =====================================================================================
-- PASO 5 de 9  -  03_null_password_legacy_php.sql
-- Permite PASSWORD_HASH nulo: con portal.auth.strategy=LEGACY_PHP la contrasena la valida
--    ORDS, el portal no la guarda.
-- =====================================================================================
-- portal.auth.strategy paso a LEGACY_PHP: el password SIEMPRE se valida contra
-- ORDS ahora (ver LegacyPhpAuthenticationProvider), incluido el admin del panel
-- /admin. El hash local de 68958027838 ya no se usa para autenticar - se deja
-- en NULL (columna ya nullable) para que quede claro que no es fuente de verdad
-- y no pueda divergir en silencio del password real de ORDS.
UPDATE SERV_MED_SEC_USER
SET PASSWORD_HASH = NULL
WHERE USERNAME = '68958027838';
COMMIT;

-- Verificacion: debe salir NULL.
SELECT USERNAME, PASSWORD_HASH FROM SERV_MED_SEC_USER WHERE USERNAME = '68958027838';


-- =====================================================================================
-- PASO 6 de 9  -  app_domain/app-fs-file.sql
-- SERV_MED_FS_FILE si no existiera (el creador autoritativo es 00_init; este es el respaldo).
-- =====================================================================================
-- =============================================================================
-- SERV_MED_FS_FILE — Metadatos de archivos (binario en filesystem, NO en BD)
-- =============================================================================
-- Proyecto   : Portal Salud — portalsalud-personal
-- Contrato   : docs/architecture/architecture-contract.json > fileManagementContract
--              storageModel = "metadata-in-db-and-binary-in-filesystem"
-- Autor      : Agente IA (Claude) + revisión Tech Lead
-- Fecha      : 2026-07-13
-- Versión    : V1
--
-- [!] CREADOR AUTORITATIVO = 00_init_oracle21c.sql
--     SERV_MED_FS_FILE (y sus tablas satelite, FKs, trigger, vista) se crean en
--     00_init, que corre primero y GANA el guard -955. El CREATE de este script
--     se salta si la tabla ya existe. Ambas definiciones ya convergen en las
--     columnas que usa FsFileRepository (NSS, FILE_TYPE, DATE_UPLOAD, VERSION).
--     Para parchear una BD existente con la forma vieja: app-fs-file-reconcile.sql.
--
-- PROPÓSITO
-- ─────────
-- Tabla de metadatos del modelo de almacenamiento acordado: el BINARIO vive en
-- el filesystem (portal.files.root, sharding yyyy/MM/dd/<2 del sha256>, nombre
-- UUID) y AQUÍ solo se guardan sus metadatos + la ruta (STORAGE_PATH relativa).
--
-- Reemplaza, para los archivos NUEVOS, el uso de MED_FILE.CONTENT_BLOB (que
-- guardaba el binario en la BD). MED_FILE queda SOLO como capa de aterrizaje de
-- la migración histórica del legacy (files.url base64). Ver files-salud.sql NOTA-5.
--
-- PATRONES DE ACCESO (igual que el legacy files):
--   · Adjuntos de una consulta:  WHERE FILE_TYPE = <consulta_relacionada>
--   · Por NSS + tipo funcional:  WHERE NSS = ? AND FILE_TYPE = 'examen_medico'|...
-- =============================================================================

SET SERVEROUTPUT ON;

-- =============================================================================
-- BLOQUE 1: TABLA SERV_MED_FS_FILE
-- =============================================================================
BEGIN
  EXECUTE IMMEDIATE q'[
    CREATE TABLE SERV_MED_FS_FILE (
      -- Clave primaria
      ID              NUMBER
                      GENERATED BY DEFAULT AS IDENTITY
                      CONSTRAINT SERV_MED_PK_FS_FILE PRIMARY KEY,

      -- Identidad de negocio
      BUSINESS_KEY    VARCHAR2(64)  NOT NULL,     -- UUID estable por archivo
      NSS             VARCHAR2(50),               -- dueño (empleado/candidato)
      FILE_TYPE       VARCHAR2(120),              -- relación consulta o tipo funcional

      -- Metadatos del archivo
      ORIGINAL_NAME   VARCHAR2(400) NOT NULL,     -- nombre original (solo referencia)
      EXTENSION       VARCHAR2(20),
      MIME_TYPE       VARCHAR2(200) DEFAULT 'application/pdf',
      SIZE_BYTES      NUMBER        NOT NULL,
      CHECKSUM_SHA256 VARCHAR2(64)  NOT NULL,     -- integridad del binario

      -- Ubicación física (el binario NO está en la BD)
      STORAGE_PATH    VARCHAR2(400) NOT NULL,     -- ruta RELATIVA a portal.files.root
      STORAGE_PROVIDER VARCHAR2(40) DEFAULT 'FILESYSTEM' NOT NULL,

      -- Estado / versión
      STATUS          VARCHAR2(20)  DEFAULT 'ACTIVE' NOT NULL,  -- ACTIVE|DELETED
      VERSION         NUMBER        DEFAULT 1 NOT NULL,
      DATE_UPLOAD     TIMESTAMP(6)  DEFAULT SYSTIMESTAMP NOT NULL,

      -- Auditoría estándar
      CREATED_AT      TIMESTAMP(6)  DEFAULT SYSTIMESTAMP NOT NULL,
      CREATED_BY      VARCHAR2(100) DEFAULT 'SISTEMA' NOT NULL,
      UPDATED_AT      TIMESTAMP(6),
      UPDATED_BY      VARCHAR2(100)
    )
  ]';
  DBMS_OUTPUT.PUT_LINE('[OK] Tabla SERV_MED_FS_FILE creada.');
EXCEPTION WHEN OTHERS THEN
  IF SQLCODE = -955 THEN DBMS_OUTPUT.PUT_LINE('[SKIP] SERV_MED_FS_FILE ya existe.');
  ELSE RAISE; END IF;
END;
/

COMMENT ON TABLE  SERV_MED_FS_FILE                 IS 'Metadatos de archivos; binario en filesystem (portal.files.root). Contrato metadata-in-db-and-binary-in-filesystem.';
COMMENT ON COLUMN SERV_MED_FS_FILE.STORAGE_PATH    IS 'Ruta RELATIVA a la raiz por entorno: yyyy/MM/dd/<2 del sha256>/<uuid>.<ext>';
COMMENT ON COLUMN SERV_MED_FS_FILE.CHECKSUM_SHA256 IS 'SHA-256 del contenido; integridad y futura deduplicacion.';
COMMENT ON COLUMN SERV_MED_FS_FILE.FILE_TYPE       IS 'consulta_relacionada | examen_medico | nota_medica | laboratorio | nota_incapacidad';
COMMENT ON COLUMN SERV_MED_FS_FILE.STATUS          IS 'ACTIVE|DELETED (hoy el borrado es fisico; columna reservada para soft-delete).';
/

-- =============================================================================
-- BLOQUE 2: ÍNDICES (según patrones de acceso reales)
-- =============================================================================
BEGIN EXECUTE IMMEDIATE 'CREATE INDEX SERV_MED_IDX_FS_FILE_TYPE     ON SERV_MED_FS_FILE (FILE_TYPE)';
  DBMS_OUTPUT.PUT_LINE('[OK] SERV_MED_IDX_FS_FILE_TYPE');
EXCEPTION WHEN OTHERS THEN IF SQLCODE IN (-955, -1408) THEN NULL; ELSE RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE 'CREATE INDEX SERV_MED_IDX_FS_FILE_NSS_TYPE ON SERV_MED_FS_FILE (NSS, FILE_TYPE)';
  DBMS_OUTPUT.PUT_LINE('[OK] SERV_MED_IDX_FS_FILE_NSS_TYPE');
EXCEPTION WHEN OTHERS THEN IF SQLCODE IN (-955, -1408) THEN NULL; ELSE RAISE; END IF; END;
/
-- OJO: 00_init_oracle21c.sql ya crea un UNIQUE sobre esta misma columna, pero con OTRO
-- nombre (SERV_MED_UX_FS_FILE_BUSKEY). Al correr los scripts en orden, Oracle responde
-- ORA-01408 "such column list already indexed", NO ORA-00955, asi que hay que atrapar
-- tambien -1408 o el script aborta en un esquema nuevo. Mismo motivo en los dos de arriba.
BEGIN EXECUTE IMMEDIATE 'CREATE UNIQUE INDEX SERV_MED_UX_FS_FILE_BKEY ON SERV_MED_FS_FILE (BUSINESS_KEY)';
  DBMS_OUTPUT.PUT_LINE('[OK] SERV_MED_UX_FS_FILE_BKEY');
EXCEPTION WHEN OTHERS THEN IF SQLCODE IN (-955, -1408) THEN NULL; ELSE RAISE; END IF; END;
/

-- =============================================================================
-- FIN DEL SCRIPT
-- =============================================================================


-- =====================================================================================
-- PASO 7 de 9  -  app_domain/app-fs-file-reconcile.sql
-- Reconcilia columnas de SERV_MED_FS_FILE si la tabla ya existia con otra forma
--    (antecedente real: ORA-00904 FILE_TYPE por dos CREATE TABLE distintos).
-- =====================================================================================
-- =============================================================================
-- SERV_MED_FS_FILE — RECONCILIACIÓN de esquema (parche QA)
-- =============================================================================
-- Proyecto : Portal Salud — portalsalud-personal
-- Fecha    : 2026-07-21
-- Motivo   : ORA-00904 "FILE_TYPE": invalid identifier al listar adjuntos
--            (menús Nota Médica, Histórico E.M., E. Laboratorio).
--
-- CAUSA RAÍZ
-- ──────────
-- Existían DOS definiciones de SERV_MED_FS_FILE, ambas con guard idempotente
-- (SKIP si SQLCODE=-955):
--   · 00_init_oracle21c.sql        -> forma SIN NSS / FILE_TYPE / DATE_UPLOAD, usa CURRENT_VERSION
--   · app_domain/app-fs-file.sql   -> forma que espera el código Java (FsFileRepository)
-- En QA corrió primero 00_init, creó la tabla con la forma vieja, y el CREATE de
-- app-fs-file.sql se saltó por -955. Resultado: faltan columnas que el código usa.
--
-- Este script AGREGA solo las columnas faltantes (add-if-not-exists). Es seguro
-- re-ejecutarlo: si la columna ya existe, se salta (ORA-01430).
--
-- Columnas que el código Java necesita: NSS, FILE_TYPE, VERSION, DATE_UPLOAD.
-- =============================================================================

SET SERVEROUTPUT ON;

DECLARE
  PROCEDURE add_col_if_absent(p_col VARCHAR2, p_ddl VARCHAR2) IS
  BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE SERV_MED_FS_FILE ADD (' || p_ddl || ')';
    DBMS_OUTPUT.PUT_LINE('[OK]   columna agregada: ' || p_col);
  EXCEPTION WHEN OTHERS THEN
    IF SQLCODE = -1430 THEN                       -- column already exists
      DBMS_OUTPUT.PUT_LINE('[SKIP] ya existe: ' || p_col);
    ELSE RAISE; END IF;
  END;
BEGIN
  add_col_if_absent('NSS',         'NSS VARCHAR2(50)');
  add_col_if_absent('FILE_TYPE',   'FILE_TYPE VARCHAR2(120)');
  add_col_if_absent('DATE_UPLOAD', 'DATE_UPLOAD TIMESTAMP(6) DEFAULT SYSTIMESTAMP');
  -- El INSERT del repositorio setea VERSION=1. La tabla vieja usa CURRENT_VERSION;
  -- agregamos VERSION (no se renombra para no romper otros usos).
  add_col_if_absent('VERSION',     'VERSION NUMBER DEFAULT 1');

  -- CURRENT_VERSION es NOT NULL y el INSERT del repo NO la setea. Garantizamos su
  -- DEFAULT para que el INSERT no falle con ORA-01400 (cannot insert NULL).
  BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE SERV_MED_FS_FILE MODIFY (CURRENT_VERSION DEFAULT 1)';
    DBMS_OUTPUT.PUT_LINE('[OK]   CURRENT_VERSION DEFAULT 1 garantizado');
  EXCEPTION WHEN OTHERS THEN
    DBMS_OUTPUT.PUT_LINE('[WARN] no se pudo fijar DEFAULT en CURRENT_VERSION: ' || SQLERRM);
  END;
END;
/

-- Backfill mínimo para filas ya migradas/insertadas antes del parche
UPDATE SERV_MED_FS_FILE SET VERSION     = 1           WHERE VERSION     IS NULL;
UPDATE SERV_MED_FS_FILE SET DATE_UPLOAD = CREATED_AT  WHERE DATE_UPLOAD IS NULL;
COMMIT;

-- Índices que acompañan los patrones de acceso (add-if-not-exists via -955)
BEGIN EXECUTE IMMEDIATE 'CREATE INDEX SERV_MED_IDX_FS_FILE_TYPE     ON SERV_MED_FS_FILE (FILE_TYPE)';
  DBMS_OUTPUT.PUT_LINE('[OK]   SERV_MED_IDX_FS_FILE_TYPE');
EXCEPTION WHEN OTHERS THEN IF SQLCODE IN (-955, -1408) THEN DBMS_OUTPUT.PUT_LINE('[SKIP] SERV_MED_IDX_FS_FILE_TYPE'); ELSE RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE 'CREATE INDEX SERV_MED_IDX_FS_FILE_NSS_TYPE ON SERV_MED_FS_FILE (NSS, FILE_TYPE)';
  DBMS_OUTPUT.PUT_LINE('[OK]   SERV_MED_IDX_FS_FILE_NSS_TYPE');
EXCEPTION WHEN OTHERS THEN IF SQLCODE IN (-955, -1408) THEN DBMS_OUTPUT.PUT_LINE('[SKIP] SERV_MED_IDX_FS_FILE_NSS_TYPE'); ELSE RAISE; END IF; END;
/

-- Verificación final
PROMPT === Columnas de SERV_MED_FS_FILE tras el parche ===
SELECT column_name, data_type, data_length, nullable
FROM   user_tab_columns
WHERE  table_name = 'SERV_MED_FS_FILE'
ORDER  BY column_id;


-- =====================================================================================
-- PASO 8 de 9  -  app_domain/app-import.sql
-- Importador de Excel: SERV_MED_IMPORT_LOTE y SERV_MED_IMPORT_FILA (staging) + indices.
-- =====================================================================================
-- =============================================================================
-- Importador Excel (Analisis > Importar Excel) - tablas TEMPORALES de staging.
-- Aplicar en la BD del portal (la misma de SERV_MED_FS_FILE / SERV_MED_AUD_EVENT), NO en ORDS.
--
-- Por que "temporal": hoy no existen los layouts ni las tablas destino del reporte
-- gerencial. El importador lee el archivo, lo valida y deja TODO aqui (una fila por
-- renglon de cada hoja, valores en JSON) mas el archivo original en SERV_MED_FS_FILE.
-- Cuando lleguen los layouts, el mapeo hoja -> tabla final se hace desde estas
-- tablas sin volver a pedir el archivo.
--
-- Versionado: si se vuelve a cargar un archivo con el MISMO nombre, el lote
-- anterior no se borra: pasa a estado SUSTITUIDO y el nuevo lleva VERSION + 1.
-- El binario anterior tambien se conserva (SERV_MED_FS_FILE.VERSION).
-- =============================================================================
SET SERVEROUTPUT ON;

BEGIN
  EXECUTE IMMEDIATE q'[
    CREATE TABLE SERV_MED_IMPORT_LOTE (
      ID               NUMBER GENERATED BY DEFAULT AS IDENTITY
                       CONSTRAINT SERV_MED_PK_IMPORT_LOTE PRIMARY KEY,
      -- archivo
      FS_FILE_ID       NUMBER,                        -- SERV_MED_FS_FILE.ID (binario en filesystem)
      NOMBRE_ARCHIVO   VARCHAR2(400) NOT NULL,
      EXTENSION        VARCHAR2(10),
      SIZE_BYTES       NUMBER,
      CHECKSUM_SHA256  VARCHAR2(64),
      VERSION          NUMBER DEFAULT 1 NOT NULL,     -- por NOMBRE_ARCHIVO
      -- resultado de la validacion
      ESTADO           VARCHAR2(20) DEFAULT 'VALIDADO' NOT NULL, -- VALIDADO|CONFIRMADO|DESCARTADO|SUSTITUIDO
      HOJAS            NUMBER DEFAULT 0 NOT NULL,
      PROCESADOS       NUMBER DEFAULT 0 NOT NULL,
      CORRECTOS        NUMBER DEFAULT 0 NOT NULL,
      ADVERTENCIAS     NUMBER DEFAULT 0 NOT NULL,
      RECHAZADOS       NUMBER DEFAULT 0 NOT NULL,
      RESUMEN_JSON     CLOB,                          -- hojas + incidencias, tal como se pintan
      -- auditoria
      CREATED_AT       TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL,
      CREATED_BY       VARCHAR2(120),
      CONFIRMED_AT     TIMESTAMP(6),
      CONFIRMED_BY     VARCHAR2(120)
    )
  ]';
  DBMS_OUTPUT.PUT_LINE('[OK] SERV_MED_IMPORT_LOTE creada.');
EXCEPTION WHEN OTHERS THEN
  IF SQLCODE = -955 THEN DBMS_OUTPUT.PUT_LINE('[SKIP] SERV_MED_IMPORT_LOTE ya existe.');
  ELSE RAISE; END IF;
END;
/

BEGIN
  EXECUTE IMMEDIATE q'[
    CREATE TABLE SERV_MED_IMPORT_FILA (
      ID               NUMBER GENERATED BY DEFAULT AS IDENTITY
                       CONSTRAINT SERV_MED_PK_IMPORT_FILA PRIMARY KEY,
      LOTE_ID          NUMBER NOT NULL
                       CONSTRAINT SERV_MED_FK_IMPORT_FILA_LOTE REFERENCES SERV_MED_IMPORT_LOTE (ID),
      HOJA             VARCHAR2(120) NOT NULL,
      FILA_NUM         NUMBER NOT NULL,               -- numero de renglon en la hoja (1-based, como Excel)
      ESTADO           VARCHAR2(20) NOT NULL,         -- OK|ADVERTENCIA|RECHAZADO
      MENSAJES         VARCHAR2(2000),                -- " · " separado
      VALORES_JSON     CLOB NOT NULL                  -- {"ENCABEZADO": valor, ...} en el orden de la hoja
    )
  ]';
  DBMS_OUTPUT.PUT_LINE('[OK] SERV_MED_IMPORT_FILA creada.');
EXCEPTION WHEN OTHERS THEN
  IF SQLCODE = -955 THEN DBMS_OUTPUT.PUT_LINE('[SKIP] SERV_MED_IMPORT_FILA ya existe.');
  ELSE RAISE; END IF;
END;
/

BEGIN
  EXECUTE IMMEDIATE 'CREATE INDEX SERV_MED_IX_IMPORT_FILA_LOTE ON SERV_MED_IMPORT_FILA (LOTE_ID, HOJA, FILA_NUM)';
  DBMS_OUTPUT.PUT_LINE('[OK] SERV_MED_IX_IMPORT_FILA_LOTE creado.');
EXCEPTION WHEN OTHERS THEN
  IF SQLCODE IN (-955, -1408) THEN DBMS_OUTPUT.PUT_LINE('[SKIP] SERV_MED_IX_IMPORT_FILA_LOTE ya existe.');
  ELSE RAISE; END IF;
END;
/

BEGIN
  EXECUTE IMMEDIATE 'CREATE INDEX SERV_MED_IX_IMPORT_LOTE_NOMBRE ON SERV_MED_IMPORT_LOTE (NOMBRE_ARCHIVO, VERSION)';
  DBMS_OUTPUT.PUT_LINE('[OK] SERV_MED_IX_IMPORT_LOTE_NOMBRE creado.');
EXCEPTION WHEN OTHERS THEN
  IF SQLCODE IN (-955, -1408) THEN DBMS_OUTPUT.PUT_LINE('[SKIP] SERV_MED_IX_IMPORT_LOTE_NOMBRE ya existe.');
  ELSE RAISE; END IF;
END;
/

COMMENT ON TABLE SERV_MED_IMPORT_LOTE IS 'Importador Excel: un lote por archivo cargado. Staging temporal hasta tener layouts destino.';
COMMENT ON TABLE SERV_MED_IMPORT_FILA IS 'Importador Excel: un renglon por fila de cada hoja del lote, valores en JSON.';

-- Verificacion rapida:
--   select * from SERV_MED_IMPORT_LOTE order by id desc;
--   select hoja, estado, count(*) from SERV_MED_IMPORT_FILA where lote_id = :id group by hoja, estado;


-- =====================================================================================
-- PASO 9 de 9  -  app_domain/tags-salud.sql
-- SERV_MED_TAG (EAV del historico), SERV_MED_TAG_MIG_LOG y la funcion SERV_MED_FN_TAG_GROUP
--    que clasifica los 137 tipos. Destino del ETL de tags.
-- =====================================================================================
-- =============================================================================
-- MIGRACIÓN: servicioMedico.tags (MariaDB) → SERV_MED_TAG (Oracle 21c)
-- =============================================================================
-- Proyecto   : Portal Salud — portalsalud-personal
-- Origen     : MariaDB · servicioMedico.tags
-- Destino    : Oracle 21c · SERV_MED_TAG
-- Autor      : Agente IA (Claude) + revisión Tech Lead
-- Fecha      : 2026-06-29
-- Versión    : V2  (Flyway-compatible)
--
-- ALCANCE DE ESTE SCRIPT
-- ──────────────────────
-- Solo cubre la tabla `tags`. La tabla `files` NO se migra aquí porque
-- el módulo SERV_MED_FS_FILE del proyecto Java ya gestiona almacenamiento de
-- archivos (metadatos en Oracle + archivo físico en filesystem).
-- El ETL de files → SERV_MED_FS_FILE es un proceso separado.
--
-- CONTEXTO DE LA TABLA ORIGEN
-- ────────────────────────────
-- Schema real confirmado en MariaDB:
--
--   tags
--   ├── id       int(255)      PK AUTO_INCREMENT  NOT NULL
--   ├── nss      varchar(255)  NULL
--   ├── type     varchar(50)   NULL
--   └── content  longtext      NULL
--
-- Patrón de uso: EAV (Entity-Attribute-Value)
--   · nss     → NSS del empleado/paciente
--   · type    → nombre del campo del formulario médico (137 valores distintos)
--   · content → valor del campo (texto libre, JSON o base64 para firma digital)
--
-- Comportamiento del PHP (crítico para migración):
--   · Cada guardado hace DELETE + INSERT (no UPDATE)
--   · Pueden existir múltiples filas con mismo nss+type (versiones)
--   · El PHP siempre lee: ORDER BY id DESC LIMIT 1  (el más reciente)
--   · Algunos content son base64 de firma digital → candidatos a SERV_MED_FS_FILE
--     pero eso se decide en una fase posterior, no aquí
--
-- DECISIÓN DE DISEÑO
-- ───────────────────
-- Se migra como EAV fiel (copia exacta de la estructura) sin normalizar.
-- La normalización (MED_PRETEST, MED_EXAMEN_FISICO, etc.) ocurrirá en una
-- fase posterior, leyendo desde SERV_MED_TAG ya en Oracle.
-- Esto garantiza cero pérdida de datos: MariaDB.tags → SERV_MED_TAG es una copia
-- completa antes de cualquier transformación.
--
-- GRUPOS FUNCIONALES identificados en código PHP (137 tipos):
--   PRETEST          → campos del pre-test de ingreso (*PRETEST)
--   EXAMEN_FISICO    → audiometría, agudeza visual, SpO2, Romberg, etc.
--   CONTACTO_EMER    → hasta 3 contactos de emergencia
--   HISTORIA_LABORAL → hasta 4 empleos anteriores
--   VACUNAS          → fechaINFLUEN y similares
--   FIRMA_DIGITAL    → drawdataUrlPRETEST (base64 canvas)
--   PERMISO_EXAMEN   → checkExamPermiso, tipoExamenInputO
--   CLINICO_MISC     → CCA, DIENTE_2, Drenaje, ciruOBS, pension, etc.
-- =============================================================================


-- =============================================================================
-- BLOQUE 1: TABLA DESTINO SERV_MED_TAG
-- =============================================================================
BEGIN
  EXECUTE IMMEDIATE q'[
    CREATE TABLE SERV_MED_TAG (

      -- Clave primaria
      ID            NUMBER
                    GENERATED BY DEFAULT AS IDENTITY
                    START WITH 1 INCREMENT BY 1
                    CONSTRAINT SERV_MED_PK_TAG PRIMARY KEY,

      -- Datos de negocio (mapeo directo de tags legacy)
      NSS           VARCHAR2(255),           -- tags.nss  (nullable conservado del origen)
      TYPE          VARCHAR2(120) NOT NULL,  -- tags.type (ampliado de 50 → 120 por seguridad)
      CONTENT       CLOB,                    -- tags.content (longtext → CLOB, hasta 4 GB)

      -- Columna derivada: grupo funcional para queries y futura normalización
      -- Se puebla automáticamente vía SERV_MED_FN_TAG_GROUP durante la migración
      TAG_GROUP     VARCHAR2(60),

      -- Trazabilidad de origen (no tocar después de migración)
      SOURCE_ID     NUMBER,                  -- tags.id original en MariaDB
      MIGRATED_AT   TIMESTAMP(6) DEFAULT SYSTIMESTAMP,

      -- Auditoría estándar
      CREATED_AT    TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL,
      CREATED_BY    VARCHAR2(100) DEFAULT 'MIGRATION'  NOT NULL,
      UPDATED_AT    TIMESTAMP(6),
      UPDATED_BY    VARCHAR2(100)
    )
  ]';
  DBMS_OUTPUT.PUT_LINE('[OK] Tabla SERV_MED_TAG creada.');
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE = -955 THEN
      DBMS_OUTPUT.PUT_LINE('[SKIP] Tabla SERV_MED_TAG ya existe — sin cambios.');
    ELSE
      RAISE;
    END IF;
END;
/

-- Comentarios de columna
COMMENT ON TABLE  SERV_MED_TAG           IS 'EAV de formularios médicos — migrado desde MariaDB.servicioMedico.tags';
COMMENT ON COLUMN SERV_MED_TAG.NSS       IS 'NSS del empleado/paciente. NULL permitido por compatibilidad con origen.';
COMMENT ON COLUMN SERV_MED_TAG.TYPE      IS 'Nombre del atributo del formulario (137 valores distintos en legacy)';
COMMENT ON COLUMN SERV_MED_TAG.CONTENT   IS 'Valor del atributo. Puede ser texto, JSON o base64 (firma digital).';
COMMENT ON COLUMN SERV_MED_TAG.TAG_GROUP IS 'Grupo funcional derivado de TYPE. Facilita futura normalización.';
COMMENT ON COLUMN SERV_MED_TAG.SOURCE_ID IS 'ID original en MariaDB.tags. Permite reconciliación y rollback.';
/


-- =============================================================================
-- BLOQUE 2: ÍNDICES
-- Basados en los patrones de acceso reales del PHP:
--   SELECT * FROM tags WHERE nss=? AND type=? ORDER BY id DESC LIMIT 1
-- =============================================================================

-- Acceso principal: por NSS + TYPE (el más usado)
BEGIN
  EXECUTE IMMEDIATE
    'CREATE INDEX SERV_MED_IDX_TAG_NSS_TYPE
     ON SERV_MED_TAG (NSS, TYPE)';
  DBMS_OUTPUT.PUT_LINE('[OK] Índice SERV_MED_IDX_TAG_NSS_TYPE creado.');
EXCEPTION WHEN OTHERS THEN
  IF SQLCODE = -955 THEN
    DBMS_OUTPUT.PUT_LINE('[SKIP] SERV_MED_IDX_TAG_NSS_TYPE ya existe.');
  ELSE RAISE; END IF;
END;
/

-- Acceso por grupo funcional (para futura normalización por fases)
BEGIN
  EXECUTE IMMEDIATE
    'CREATE INDEX SERV_MED_IDX_TAG_NSS_GROUP
     ON SERV_MED_TAG (NSS, TAG_GROUP)';
  DBMS_OUTPUT.PUT_LINE('[OK] Índice SERV_MED_IDX_TAG_NSS_GROUP creado.');
EXCEPTION WHEN OTHERS THEN
  IF SQLCODE = -955 THEN
    DBMS_OUTPUT.PUT_LINE('[SKIP] SERV_MED_IDX_TAG_NSS_GROUP ya existe.');
  ELSE RAISE; END IF;
END;
/

-- Trazabilidad: buscar por ID de origen para reconciliación
BEGIN
  EXECUTE IMMEDIATE
    'CREATE INDEX SERV_MED_IDX_TAG_SOURCE_ID
     ON SERV_MED_TAG (SOURCE_ID)';
  DBMS_OUTPUT.PUT_LINE('[OK] Índice SERV_MED_IDX_TAG_SOURCE_ID creado.');
EXCEPTION WHEN OTHERS THEN
  IF SQLCODE = -955 THEN
    DBMS_OUTPUT.PUT_LINE('[SKIP] SERV_MED_IDX_TAG_SOURCE_ID ya existe.');
  ELSE RAISE; END IF;
END;
/


-- =============================================================================
-- BLOQUE 3: FUNCIÓN DE CLASIFICACIÓN SERV_MED_FN_TAG_GROUP
-- Clasifica el valor de TYPE en un grupo funcional.
-- DETERMINISTIC permite que Oracle la use en índices funcionales si se necesita.
-- Basada en los 137 tipos reales encontrados en el código PHP.
-- =============================================================================
CREATE OR REPLACE FUNCTION SERV_MED_FN_TAG_GROUP(p_type IN VARCHAR2)
RETURN VARCHAR2
DETERMINISTIC
IS
BEGIN
  -- Pre-Test de ingreso (sufijo *PRETEST en todos sus campos)
  IF p_type LIKE '%PRETEST%' THEN
    RETURN 'PRETEST';
  END IF;

  -- Firma digital (base64 de canvas HTML — candidato a SERV_MED_FS_FILE en fase futura)
  IF p_type = 'drawdataUrlPRETEST' THEN
    RETURN 'FIRMA_DIGITAL';
  END IF;

  -- Examen físico: audiometría OD/OI, agudeza visual, SpO2, Romberg, voz, traquea
  IF p_type IN (
    'ODAGAUDIINPEXP', 'ODCAEINPEXP', 'ODEXCAEEXFISINP',
    'ODEXFISINPCERC', 'ODEXFISINPCERCIZQ', 'ODMEMTIMINPEXP',
    'OIAGAUDIINPEXP', 'OICAEINPEXP',  'OIMEMTIMINPEXP',
    'LENTEOJODERECHOEXP', 'LENTEOJOIZQUIERDOEXP',
    'ROMBERGEXFIS', 'STPO2', 'VOZCLARAYFUERTE',
    'TRAQUEA_OBSINP', 'OBSGENERALINPEX'
  ) THEN
    RETURN 'EXAMEN_FISICO';
  END IF;

  -- Contactos de emergencia (prefijo contactoEmer*)
  IF p_type LIKE 'contactoEmer%' THEN
    RETURN 'CONTACTO_EMERGENCIA';
  END IF;

  -- Historia laboral: hasta 4 empleos anteriores (sufijo 1..4)
  IF p_type IN (
    'nombre1',       'nombre2',       'nombre3',       'nombre4',
    'puesto1',       'puesto2',       'puesto3',       'puesto4',
    'giro1',         'giro2',         'giro3',         'giro4',
    'antiguedad1',   'antiguedad2',   'antiguedad3',   'antiguedad4',
    'salida1',       'salida2',       'salida3',       'salida4',
    'turno1',        'turno2',        'turno3',        'turno4',
    'riesgos1',      'riesgos2',      'riesgos3',      'riesgos4',
    'epp1',          'epp2',          'epp3',          'epp4',
    'observaciones1','observaciones2','observaciones3','observaciones4',
    'descripcion1',  'descripcion2',  'descripcion3',  'descripcion4',
    'cantidad_trabajos', 'edad_inicio_laborar'
  ) THEN
    RETURN 'HISTORIA_LABORAL';
  END IF;

  -- Vacunas / inmunizaciones
  IF p_type = 'fechaINFLUEN' THEN
    RETURN 'VACUNAS';
  END IF;

  -- Permisos de examen médico
  IF p_type IN ('checkExamPermiso', 'tipoExamenInputO') THEN
    RETURN 'PERMISO_EXAMEN';
  END IF;

  -- Incapacidades
  IF p_type LIKE '%INCAP%' OR p_type LIKE '%incap%' THEN
    RETURN 'INCAPACIDAD';
  END IF;

  -- Campos clínicos misceláneos
  IF p_type IN (
    'CCA', 'DIENTE_2', 'Drenaje', 'ciruOBS', 'pension',
    'cadaCuandoDep', 'cadaCuandoPasa'
  ) THEN
    RETURN 'CLINICO_MISC';
  END IF;

  -- Cualquier tipo no clasificado queda en OTRO para revisión posterior
  RETURN 'OTRO';

END SERV_MED_FN_TAG_GROUP;
/


-- =============================================================================
-- BLOQUE 4: TABLA DE CONTROL DE MIGRACIÓN POR LOTES
-- Registra el progreso de cada lote para poder reanudar si falla a medias.
-- =============================================================================
BEGIN
  EXECUTE IMMEDIATE q'[
    CREATE TABLE SERV_MED_TAG_MIG_LOG (
      ID             NUMBER  GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
      BATCH_NO       NUMBER  NOT NULL,
      SOURCE_ID_FROM NUMBER  NOT NULL,
      SOURCE_ID_TO   NUMBER  NOT NULL,
      ROWS_EXPECTED  NUMBER,
      ROWS_INSERTED  NUMBER,
      STATUS         VARCHAR2(20) DEFAULT 'PENDING' NOT NULL,
                     -- PENDING | RUNNING | DONE | ERROR | DRY-RUN
      STARTED_AT     TIMESTAMP(6),
      FINISHED_AT    TIMESTAMP(6),
      ERROR_MSG      VARCHAR2(4000)
    )
  ]';
  DBMS_OUTPUT.PUT_LINE('[OK] Tabla SERV_MED_TAG_MIG_LOG creada.');
EXCEPTION WHEN OTHERS THEN
  IF SQLCODE = -955 THEN
    DBMS_OUTPUT.PUT_LINE('[SKIP] SERV_MED_TAG_MIG_LOG ya existe.');
  ELSE RAISE; END IF;
END;
/


-- =============================================================================
-- BLOQUE 5: PROCEDIMIENTO DE MIGRACIÓN SERV_MED_SP_MIGRATE_TAGS
--
-- MODO DE USO:
--   -- Primero correr en dry-run para validar conteos sin insertar:
--   EXEC SERV_MED_SP_MIGRATE_TAGS(p_dry_run => 'S');
--
--   -- Luego correr la migración real en lotes de 500 (ajustar según volumen):
--   EXEC SERV_MED_SP_MIGRATE_TAGS(p_batch_size => 500, p_dry_run => 'N');
--
-- PREREQUISITO:
--   Requiere Database Link desde Oracle hacia MariaDB llamado MARIADB_LINK.
--   Si no existe DB Link ver BLOQUE 6 (alternativa ETL externo).
-- =============================================================================
-- ---------------------------------------------------------------------------
-- *** DESACTIVADO 24-sep-2026 - NO SE CREA EN NINGUN AMBIENTE ***
--
-- Este procedimiento NO COMPILA y no puede compilar. Dos motivos independientes:
--
--   1. El cursor lee  tags@MARIADB_LINK  y ese database link NO EXISTE en ningun
--      ambiente (local, QA ni produccion) ni existio nunca -> ORA-00942.
--   2. Usa SQLERRM dentro de un UPDATE (ERROR_MSG = SUBSTR(SQLERRM,1,4000)).
--      Oracle no permite llamar SQLERRM desde una sentencia SQL -> ORA-00904
--      "SQLERRM": invalid identifier. Ese bug siempre estuvo aqui; nunca se habia
--      visto porque el procedimiento jamas llego a compilarse.
--
-- Es CODIGO MUERTO: la carga historica de tags (550,560 filas) se hizo con el
-- proyecto ETL en Java (carpeta etl/), no con este camino de DB Link. Se deja
-- comentado como referencia del diseno original, no se ejecuta.
--
-- Se detecto al correr el DDL consolidado en el Oracle local el 24-sep: dejaba el
-- objeto en estado INVALID, que en produccion habria sido ruido para el DBA y
-- ademas dispara la alerta del bloque de VERIFICACION ("ningun objeto SERV_MED_*
-- debe quedar INVALID").
--
-- SERV_MED_TAG_MIG_LOG y SERV_MED_V_TAG_MIG_STATUS (bloques 4 y 7d) SI se crean:
-- son objetos validos y quedan vacios. Son vestigios de este mismo camino.
-- ---------------------------------------------------------------------------
-- CREATE OR REPLACE PROCEDURE SERV_MED_SP_MIGRATE_TAGS (
--   p_batch_size  IN NUMBER DEFAULT 500,
--   p_dry_run     IN CHAR   DEFAULT 'N'   -- 'S' = solo cuenta, no inserta
-- )
-- IS
--   -- Cursor sobre DB Link a MariaDB
--   -- Solo trae registros que aún no existen en Oracle (reanudable)
--   CURSOR c_tags IS
--     SELECT id, nss, type, content
--     FROM   tags@MARIADB_LINK
--     WHERE  id NOT IN (
--              SELECT SOURCE_ID
--              FROM   SERV_MED_TAG
--              WHERE  SOURCE_ID IS NOT NULL
--            )
--     ORDER BY id ASC;
--
--   TYPE t_ids      IS TABLE OF NUMBER;
--   TYPE t_nss      IS TABLE OF VARCHAR2(255);
--   TYPE t_types    IS TABLE OF VARCHAR2(120);
--   TYPE t_contents IS TABLE OF CLOB;
--
--   v_ids      t_ids;
--   v_nss      t_nss;
--   v_types    t_types;
--   v_contents t_contents;
--
--   v_batch_no   NUMBER := 1;
--   v_total_ins  NUMBER := 0;
--   v_batch_ins  NUMBER := 0;
--   v_id_from    NUMBER;
--   v_id_to      NUMBER;
--
-- BEGIN
--   DBMS_OUTPUT.PUT_LINE('════════════════════════════════════════════');
--   DBMS_OUTPUT.PUT_LINE('SERV_MED_SP_MIGRATE_TAGS — inicio: ' ||
--                        TO_CHAR(SYSTIMESTAMP,'YYYY-MM-DD HH24:MI:SS'));
--   DBMS_OUTPUT.PUT_LINE('Modo    : ' ||
--                        CASE p_dry_run WHEN 'S' THEN 'DRY-RUN (solo conteo)'
--                                       ELSE 'REAL (insertando en Oracle)' END);
--   DBMS_OUTPUT.PUT_LINE('Lote    : ' || p_batch_size || ' filas');
--   DBMS_OUTPUT.PUT_LINE('════════════════════════════════════════════');
--
--   OPEN c_tags;
--   LOOP
--     FETCH c_tags
--     BULK COLLECT INTO v_ids, v_nss, v_types, v_contents
--     LIMIT p_batch_size;
--
--     EXIT WHEN v_ids.COUNT = 0;
--
--     v_id_from := v_ids(1);
--     v_id_to   := v_ids(v_ids.LAST);
--
--     -- Registrar inicio de lote
--     INSERT INTO SERV_MED_TAG_MIG_LOG
--       (BATCH_NO, SOURCE_ID_FROM, SOURCE_ID_TO,
--        ROWS_EXPECTED, STATUS, STARTED_AT)
--     VALUES
--       (v_batch_no, v_id_from, v_id_to,
--        v_ids.COUNT, 'RUNNING', SYSTIMESTAMP);
--     COMMIT;
--
--     IF p_dry_run = 'N' THEN
--       BEGIN
--         -- Inserción masiva con clasificación automática de TAG_GROUP
--         FORALL i IN 1..v_ids.COUNT
--           INSERT INTO SERV_MED_TAG (
--             NSS, TYPE, CONTENT, TAG_GROUP,
--             SOURCE_ID,   MIGRATED_AT,
--             CREATED_AT,  CREATED_BY
--           ) VALUES (
--             v_nss(i),
--             v_types(i),
--             v_contents(i),
--             SERV_MED_FN_TAG_GROUP(v_types(i)),  -- clasificación automática
--             v_ids(i),
--             SYSTIMESTAMP,
--             SYSTIMESTAMP,
--             'SP_MIGRATE_TAGS_V2'
--           );
--
--         v_batch_ins := SQL%ROWCOUNT;
--         v_total_ins := v_total_ins + v_batch_ins;
--
--         UPDATE SERV_MED_TAG_MIG_LOG
--         SET    STATUS       = 'DONE',
--                ROWS_INSERTED = v_batch_ins,
--                FINISHED_AT  = SYSTIMESTAMP
--         WHERE  BATCH_NO = v_batch_no AND STATUS = 'RUNNING';
--         COMMIT;
--
--         DBMS_OUTPUT.PUT_LINE('[LOTE ' || LPAD(v_batch_no,3) || ']' ||
--                              ' IDs '   || v_id_from || ' → ' || v_id_to ||
--                              ' | ins: ' || v_batch_ins);
--
--       EXCEPTION WHEN OTHERS THEN
--         ROLLBACK;
--         UPDATE SERV_MED_TAG_MIG_LOG
--         SET    STATUS      = 'ERROR',
--                ERROR_MSG   = SUBSTR(SQLERRM, 1, 4000),
--                FINISHED_AT = SYSTIMESTAMP
--         WHERE  BATCH_NO = v_batch_no AND STATUS = 'RUNNING';
--         COMMIT;
--         DBMS_OUTPUT.PUT_LINE('[ERROR lote ' || v_batch_no || '] ' || SQLERRM);
--         EXIT;  -- detener en primer error; relanzar desde el lote fallido
--       END;
--
--     ELSE
--       -- Dry-run: solo reporta, no inserta
--       UPDATE SERV_MED_TAG_MIG_LOG
--       SET    STATUS       = 'DRY-RUN',
--              ROWS_INSERTED = 0,
--              FINISHED_AT  = SYSTIMESTAMP
--       WHERE  BATCH_NO = v_batch_no AND STATUS = 'RUNNING';
--       COMMIT;
--       DBMS_OUTPUT.PUT_LINE('[DRY-RUN lote ' || LPAD(v_batch_no,3) || ']' ||
--                            ' IDs ' || v_id_from || ' → ' || v_id_to ||
--                            ' | filas: ' || v_ids.COUNT);
--     END IF;
--
--     v_batch_no := v_batch_no + 1;
--   END LOOP;
--
--   CLOSE c_tags;
--
--   DBMS_OUTPUT.PUT_LINE('════════════════════════════════════════════');
--   DBMS_OUTPUT.PUT_LINE('Total insertados : ' || v_total_ins);
--   DBMS_OUTPUT.PUT_LINE('Fin              : ' ||
--                        TO_CHAR(SYSTIMESTAMP,'YYYY-MM-DD HH24:MI:SS'));
--   DBMS_OUTPUT.PUT_LINE('════════════════════════════════════════════');
--
-- END SERV_MED_SP_MIGRATE_TAGS;
-- /


-- =============================================================================
-- BLOQUE 6: ALTERNATIVA SIN DB LINK (ETL externo)
-- Si no puedes crear un Database Link Oracle → MariaDB:
--
-- PASO A — Exportar desde MariaDB:
--   mysqldump -u root -p --no-create-info --complete-insert \
--     --where="1=1" servicioMedico tags > tags_data.sql
--
--   O en TSV para SQL*Loader:
--   mysql -u root -p servicioMedico \
--     -e "SELECT id, nss, type, content FROM tags ORDER BY id" \
--     --batch --silent > tags_export.tsv
--
-- PASO B — Cada fila debe insertarse en Oracle así:
--   INSERT INTO SERV_MED_TAG
--     (NSS, TYPE, CONTENT, TAG_GROUP, SOURCE_ID, MIGRATED_AT, CREATED_AT, CREATED_BY)
--   VALUES
--     (:nss, :type, :content,
--      SERV_MED_FN_TAG_GROUP(:type),
--      :source_id, SYSTIMESTAMP, SYSTIMESTAMP, 'ETL_EXTERNAL');
--
-- Herramientas compatibles: SQL*Loader, DBeaver ETL, Apache Hop, Kettle/PDI.
-- =============================================================================


-- =============================================================================
-- BLOQUE 7: VISTAS DE RECONCILIACIÓN
-- Ejecutar DESPUÉS de SERV_MED_SP_MIGRATE_TAGS para validar integridad.
-- =============================================================================

-- 7a. Resumen por grupo funcional
CREATE OR REPLACE VIEW SERV_MED_V_TAG_BY_GROUP AS
SELECT
  TAG_GROUP,
  COUNT(*)             AS TOTAL_FILAS,
  COUNT(DISTINCT NSS)  AS TOTAL_NSS,
  COUNT(DISTINCT TYPE) AS TOTAL_TIPOS
FROM  SERV_MED_TAG
GROUP BY TAG_GROUP
ORDER BY TOTAL_FILAS DESC;
/

-- 7b. Último valor por NSS+TYPE
--     Replica el comportamiento PHP: ORDER BY id DESC LIMIT 1
--     Úsala desde Java en lugar de consultar directamente SERV_MED_TAG
--     NOTA (2026-08-13): la version original usaba SELECT DISTINCT sobre una
--     columna CLOB (CONTENT), lo cual Oracle rechaza con ORA-00932
--     ("inconsistent datatypes: expected - got CLOB"). ROW_NUMBER()+filtro
--     evita el DISTINCT sobre CLOB sin cambiar el resultado.
CREATE OR REPLACE VIEW SERV_MED_V_TAG_LATEST AS
SELECT NSS, TYPE, TAG_GROUP, CONTENT, ID AS LAST_ID
FROM (
  SELECT NSS, TYPE, TAG_GROUP, CONTENT, ID,
         ROW_NUMBER() OVER (PARTITION BY NSS, TYPE ORDER BY ID DESC) AS RN
  FROM SERV_MED_TAG
)
WHERE RN = 1;
/

-- 7c. Registros sin NSS (huérfanos del legacy)
CREATE OR REPLACE VIEW SERV_MED_V_TAG_ORPHANS AS
SELECT ID, TYPE, TAG_GROUP, SOURCE_ID, MIGRATED_AT
FROM   SERV_MED_TAG
WHERE  NSS IS NULL OR TRIM(NSS) IS NULL;
/

-- 7d. Estado de lotes de migración
CREATE OR REPLACE VIEW SERV_MED_V_TAG_MIG_STATUS AS
SELECT
  STATUS,
  COUNT(*)          AS LOTES,
  SUM(ROWS_EXPECTED) AS FILAS_ESPERADAS,
  SUM(ROWS_INSERTED) AS FILAS_INSERTADAS
FROM  SERV_MED_TAG_MIG_LOG
GROUP BY STATUS;
/


-- =============================================================================
-- BLOQUE 8: QUERIES DE VALIDACIÓN POST-MIGRACIÓN
-- Ejecutar manualmente y comparar resultados con MariaDB.
-- =============================================================================

-- ① Total migrado vs origen
--    Oracle : SELECT COUNT(*) FROM SERV_MED_TAG;
--    MariaDB: SELECT COUNT(*) FROM tags;
--    → Deben ser iguales

-- ② Estado de lotes
--    SELECT * FROM SERV_MED_V_TAG_MIG_STATUS;
--    → STATUS debe ser solo 'DONE' o 'DRY-RUN', ningún 'ERROR' o 'RUNNING'

-- ③ Distribución por grupo funcional
--    SELECT * FROM SERV_MED_V_TAG_BY_GROUP;

-- ④ Huérfanos (nss nulo en origen)
--    SELECT COUNT(*) FROM SERV_MED_V_TAG_ORPHANS;

-- ⑤ Tipos no clasificados (caen en OTRO — revisar si hay tipos nuevos en prod)
--    SELECT DISTINCT TYPE FROM SERV_MED_TAG WHERE TAG_GROUP = 'OTRO' ORDER BY TYPE;

-- ⑥ Verificar que ningún SOURCE_ID del origen falte en Oracle
--    (requiere DB Link activo)
--    SELECT COUNT(*) AS FALTANTES
--    FROM  (SELECT id FROM tags@MARIADB_LINK
--           MINUS
--           SELECT SOURCE_ID FROM SERV_MED_TAG WHERE SOURCE_ID IS NOT NULL);
--    → Debe ser 0

-- ⑦ Muestra de datos por grupo para revisión manual
--    SELECT NSS, TYPE, SUBSTR(CONTENT,1,80) AS PREVIEW
--    FROM   SERV_MED_V_TAG_LATEST
--    WHERE  TAG_GROUP = 'PRETEST'
--    AND    ROWNUM   <= 10;


-- =============================================================================
-- BLOQUE 9: NOTAS PARA EL EQUIPO
-- =============================================================================
--
-- [NOTA-1] SCOPE
--   Este script solo crea el esquema y migra datos de `tags`.
--   La tabla `files` queda fuera porque SERV_MED_FS_FILE en Java ya maneja
--   almacenamiento de archivos. El ETL de files es un proceso separado.
--
-- [NOTA-2] DEUDA TÉCNICA DOCUMENTADA
--   NSS y TYPE admiten NULL en Oracle por compatibilidad con el origen.
--   Evaluar NOT NULL en sprint de hardening después de validar datos.
--
-- [NOTA-3] NORMALIZACIÓN FUTURA
--   SERV_MED_TAG es la capa de aterrizaje segura. En una fase posterior se
--   crearán MED_PRETEST, MED_EXAMEN_FISICO, etc. leyendo desde SERV_MED_TAG.
--   No hacer esa normalización hasta que SERV_MED_TAG esté validado en producción.
--
-- [NOTA-4] FIRMA DIGITAL
--   Los registros con TAG_GROUP = 'FIRMA_DIGITAL' contienen base64 de
--   la firma del paciente. En fase futura migrar a SERV_MED_FS_FILE.
--   Por ahora se quedan en SERV_MED_TAG.CONTENT (CLOB soporta el tamaño).
--
-- [NOTA-5] PATRÓN DELETE+INSERT del PHP
--   El PHP nunca hace UPDATE — siempre DELETE+INSERT por campo.
--   SERV_MED_TAG conserva todo el historial porque SOURCE_ID evita duplicados
--   al relanzar SERV_MED_SP_MIGRATE_TAGS. La vista SERV_MED_V_TAG_LATEST replica el
--   comportamiento legacy para el código Java que consuma estos datos.
--
-- =============================================================================
-- FIN DEL SCRIPT
-- =============================================================================


-- =====================================================================================
-- VERIFICACION (solo lectura)
-- =====================================================================================
-- Deben aparecer las 20 tablas del portal:
SELECT table_name FROM user_tables
 WHERE table_name IN ('SERV_MED_SEC_USER','SERV_MED_SEC_ROLE','SERV_MED_SEC_USER_ROLE',
                      'SERV_MED_SEC_PERMISSION','SERV_MED_SEC_ROLE_PERMISSION','SERV_MED_MENU',
                      'SERV_MED_MENU_ROLE','SERV_MED_AUD_EVENT','SERV_MED_AUDIT_SQL_EXECUTION',
                      'SERV_MED_FS_FILE','SERV_MED_FS_FILE_VERSION','SERV_MED_FS_FILE_ACCESS_LOG',
                      'SERV_MED_FS_FILE_POLICY','SERV_MED_FS_FILE_ORPHAN','SERV_MED_IMPORT_LOTE',
                      'SERV_MED_IMPORT_FILA','SERV_MED_JOB_CATALOG','SERV_MED_NOTIF_TEMPLATE',
                      'SERV_MED_TAG','SERV_MED_TAG_MIG_LOG')
 ORDER BY table_name;

-- Ningun objeto del portal debe quedar INVALID (el esquema ya arrastra 91 invalidos de otros
-- modulos; aqui solo importan los SERV_MED_* que acabamos de crear):
SELECT object_name, object_type, status FROM user_objects
 WHERE status <> 'VALID' AND object_name LIKE 'SERV\_MED\_%' ESCAPE '\'
 ORDER BY object_type, object_name;

-- Semillas: roles y menus que dejo 01_rbac_local.sql / 03_menu_examenes.sql
SELECT CODE, NAME, ACTIVE FROM SERV_MED_SEC_ROLE ORDER BY ID;
SELECT ID, CODE, TITLE, ORDER_NO FROM SERV_MED_MENU ORDER BY ORDER_NO;

-- La funcion de clasificacion del ETL de tags debe estar VALID:
SELECT object_name, status FROM user_objects WHERE object_name = 'SERV_MED_FN_TAG_GROUP';

-- Conteo rapido: 20 tablas + sus indices/constraints/triggers/vistas
SELECT object_type, COUNT(*) FROM user_objects
 WHERE object_name LIKE 'SERV\_MED\_%' ESCAPE '\' GROUP BY object_type ORDER BY 1;

-- COMMIT;   <- las semillas (INSERT/MERGE) necesitan COMMIT; los CREATE ya son DDL autoconfirmado.

-- =====================================================================================
-- PASO SIGUIENTE, APARTE: el primer usuario ADMIN
-- =====================================================================================
-- Sin el, nadie puede entrar a /admin del portal. El script de ejemplo esta en
-- src/main/resources/db/sql/seeds/user-68958027838.sql: da de alta al NSS 68958027838
-- (Mauricio Ceron) con rol ROLE_ADMIN. Ajustar el NSS/nombre al usuario que decidan y
-- correrlo aparte. Con portal.auth.strategy=LEGACY_PHP la contrasena la valida ORDS, asi
-- que el PASSWORD_HASH puede quedar nulo (ver 03_null_password_legacy_php.sql).
