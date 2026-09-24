-- =====================================================================================
-- 00_crear_esquema_portal.sql  —  BASE DEL PORTAL (JDBC), instancia PDBPRD.
-- Crea el ESQUEMA PROPIO del portal Java y sus usuarios. Lo ejecuta el DBA, no el agente.
--
-- Por que un esquema propio y no BIOMETRICO (verificado el 23-sep-2026, ver
-- docs/fase1-inventario-resultado.md):
--   * En BIOMETRICO ya hay 10 tablas APP_* de otro sistema (APP_TAREAS, APP_REPORTES,
--     APP_CONFIG_EXCEL...): el prefijo esta ocupado y el riesgo de colision es real.
--   * El usuario BIOMETRICO tiene CREATE TABLE, DROP ANY VIEW, CREATE ROLE,
--     EXP/IMP_FULL_DATABASE y UNLIMITED TABLESPACE: demasiado poder para una aplicacion web.
--   * El portal NO necesita leer ninguna tabla del legacy por JDBC. Verificado en el codigo:
--     las unicas tablas que consulta son SERV_MED_FS_FILE, SERV_MED_IMPORT_LOTE, SERV_MED_IMPORT_FILA y
--     SERV_MED_TAG, todas suyas. Lo demas (consultas, incapacidades, examen, empleados) lo obtiene
--     por los WS de ORDS via HTTP. Es decir: el esquema del portal es INDEPENDIENTE y no
--     requiere ningun GRANT cruzado contra BIOMETRICO.
--
-- Decisiones que debe tomar el DBA antes de correrlo (ver la seccion de parametros):
--   1. Nombre del esquema / usuario propietario.
--   2. Tablespace y cuota.
--   3. Si se acepta el esquema de 3 usuarios (propietario, aplicacion, solo lectura).
--
-- Despues de este script va el orden de la Fase 2:
--   00_init_oracle21c.sql -> 01_rbac_local.sql -> 02_fix_expediente_general_duplicado.sql
--   -> 03_menu_examenes.sql -> 03_null_password_legacy_php.sql
--   -> app_domain/app-fs-file.sql (+ -reconcile) -> app-import.sql -> tags-salud.sql
--   -> seeds/<primer ADMIN>.sql
-- =====================================================================================

-- ---------- Parametros (ajustar y sustituir en el script) ----------
--   ESQUEMA      PORTAL_SALUD          propietario de las tablas
--   APP_USER     PORTAL_SALUD_APP      con el que se conecta el WAR (solo DML)
--   RO_USER      PORTAL_SALUD_RO       solo lectura, para pruebas desde equipos de desarrollo
--   TABLESPACE   PORTAL_SALUD_DATA     o el que indique el DBA
--   CUOTA        2G                    SERV_MED_TAG son ~550 mil filas (~52 MB) + SERV_MED_FS_FILE solo
--                                      metadatos; los binarios van al filesystem, no a Oracle

-- ---------- 1. Tablespace (omitir si se reutiliza uno existente) ----------
-- CREATE TABLESPACE PORTAL_SALUD_DATA
--   DATAFILE '+DATA' SIZE 500M AUTOEXTEND ON NEXT 100M MAXSIZE 5G;

-- ---------- 2. Propietario del esquema ----------
-- Tiene DDL SOLO para la instalacion; al terminar la Fase 2 se le revoca (paso 5).
CREATE USER PORTAL_SALUD IDENTIFIED BY "<contrasena-fuerte>"
  DEFAULT TABLESPACE PORTAL_SALUD_DATA
  TEMPORARY TABLESPACE TEMP
  QUOTA 2G ON PORTAL_SALUD_DATA;

GRANT CREATE SESSION      TO PORTAL_SALUD;
GRANT CREATE TABLE        TO PORTAL_SALUD;
GRANT CREATE SEQUENCE     TO PORTAL_SALUD;
GRANT CREATE VIEW         TO PORTAL_SALUD;
GRANT CREATE PROCEDURE    TO PORTAL_SALUD;   -- tags-salud.sql crea SERV_MED_FN_TAG_GROUP
GRANT CREATE TRIGGER      TO PORTAL_SALUD;   -- 00_init crea un trigger de SERV_MED_FS_FILE

-- ---------- 3. Usuario de la APLICACION (el que va en SPRING_DATASOURCE_USERNAME) ----------
-- Sin ningun privilegio de DDL: aunque una propiedad mal puesta intentara alterar el esquema,
-- la base lo rechaza. Es la garantia dura de "el arranque no modifica la BD".
CREATE USER PORTAL_SALUD_APP IDENTIFIED BY "<contrasena-fuerte>"
  DEFAULT TABLESPACE PORTAL_SALUD_DATA
  TEMPORARY TABLESPACE TEMP
  QUOTA 0 ON PORTAL_SALUD_DATA;
GRANT CREATE SESSION TO PORTAL_SALUD_APP;

-- ---------- 4. Usuario de SOLO LECTURA (pruebas desde equipos de desarrollo) ----------
CREATE USER PORTAL_SALUD_RO IDENTIFIED BY "<contrasena-fuerte>"
  DEFAULT TABLESPACE PORTAL_SALUD_DATA TEMPORARY TABLESPACE TEMP QUOTA 0 ON PORTAL_SALUD_DATA;
GRANT CREATE SESSION TO PORTAL_SALUD_RO;

-- ---------- 5. DESPUES de correr los scripts de la Fase 2 ----------
-- Los GRANT por tabla y los sinonimos se generan cuando las tablas ya existen. Conectado como
-- PORTAL_SALUD, estas dos consultas GENERAN el SQL a ejecutar (no lo ejecutan):
--
--   -- DML para la aplicacion:
--   SELECT 'GRANT SELECT, INSERT, UPDATE, DELETE ON ' || table_name || ' TO PORTAL_SALUD_APP;'
--     FROM user_tables ORDER BY table_name;
--   -- Solo lectura:
--   SELECT 'GRANT SELECT ON ' || table_name || ' TO PORTAL_SALUD_RO;'
--     FROM user_tables ORDER BY table_name;
--   -- Sinonimos publicos para que ambos usuarios vean las tablas sin prefijo de esquema:
--   SELECT 'CREATE OR REPLACE PUBLIC SYNONYM ' || table_name || ' FOR PORTAL_SALUD.' || table_name || ';'
--     FROM user_tables ORDER BY table_name;
--   -- Secuencias (si las hubiera):
--   SELECT 'GRANT SELECT ON ' || sequence_name || ' TO PORTAL_SALUD_APP;' FROM user_sequences;
--
-- Y una vez instalado todo, quitarle el DDL al propietario:
--   REVOKE CREATE TABLE, CREATE SEQUENCE, CREATE VIEW, CREATE PROCEDURE, CREATE TRIGGER
--     FROM PORTAL_SALUD;
-- (se le vuelven a otorgar puntualmente cuando haya que aplicar un cambio de esquema)

-- ---------- 6. Verificacion ----------
-- SELECT username, account_status, default_tablespace FROM dba_users
--  WHERE username LIKE 'PORTAL_SALUD%';
-- SELECT * FROM dba_sys_privs WHERE grantee LIKE 'PORTAL_SALUD%' ORDER BY grantee, privilege;

-- ---------- 7. Lo que queda del lado del portal ----------
-- En el Tomcat de produccion:
--   SPRING_DATASOURCE_URL=jdbc:oracle:thin:@PDBPRD      (alias de tnsnames.ora)
--   ORACLE_TNS_ADMIN=<carpeta con tnsnames.ora en el servidor>
--   SPRING_DATASOURCE_USERNAME=PORTAL_SALUD_APP
--   SPRING_DATASOURCE_PASSWORD=<la contrasena>
-- Ver docs/conexiones-ambientes.md.
