-- =====================================================================================
-- 05_exportar_rbac_desde_qa.sql  -  BASE DEL PORTAL (JDBC), NO ORDS.
--
-- SOLO LECTURA. No inserta, no borra, no modifica nada: cada consulta GENERA TEXTO con
-- las sentencias que hay que correr en el ambiente destino. Se copia la salida, se revisa
-- y se pega en el destino. Nada se aplica automaticamente.
--
-- PARA QUE SIRVE
-- --------------
-- El DDL de produccion (prod/01_ddl_portal_en_biometrico.sql) siembra la LINEA BASE del
-- RBAC: 4 roles (ROLE_ADMIN, USER, ADM, ENFERMERO), 20 menus y las asignaciones de
-- USER/ADM/ENFERMERO. Pero desde el 28-ago el RBAC se administra por pantalla
-- (/admin/roles, /admin/roles/menus, /admin/usuarios), asi que el estado real de QA ya
-- NO es la linea base. Lo que falta, concretamente:
--
--   * ROLE_MEDICO_ANALISTA: no lo siembra ningun script (es a proposito, ver
--     PortalViewController.java:149 "se crea desde /admin/roles"). Sin ese rol,
--     /analisis/** da 403 a TODOS: SecurityConfiguration lo exige con
--     hasRole("MEDICO_ANALISTA") y la autoridad es el CODE tal cual, asi que el CODE
--     en SERV_MED_SEC_ROLE tiene que ser exactamente ROLE_MEDICO_ANALISTA.
--   * Los 3 menus del grupo Examenes (ANTIDOPING_SELECCION, CONSUMIBLES,
--     CAUSAS_CONSULTA): 03_menu_examenes.sql los CREA pero no los ASIGNA a ningun rol.
--   * Los usuarios reales y sus roles.
--   * Cualquier asignacion menu-rol movida a mano por pantalla.
--
-- DONDE Y CUANDO CORRERLO
-- -----------------------
--   * En QA (ONEWMS_QA), DESPUES del renombrado (04_rename_serv_med.sql), porque usa los
--     nombres SERV_MED_*. QA es la referencia: es lo que negocio ha estado usando.
--   * Conviene correrlo TAMBIEN en el Oracle local y comparar: si las salidas difieren,
--     hay divergencia entre ambientes y vale la pena saberlo antes de tocar produccion.
--
-- CLAVE DE DISENO: nada se exporta por ID. Los IDENTITY de SERV_MED_MENU y
-- SERV_MED_SEC_ROLE son autogenerados y NO coinciden entre ambientes. Todo se resuelve
-- por CODE (roles y menus) y por USERNAME (usuarios), que si son estables.
--
-- EN SQL DEVELOPER: usar "Run Script" (F5), no "Run Statement" (Ctrl+Enter), y poner la
-- salida en modo texto para poder copiarla completa.
-- =====================================================================================

SET PAGESIZE 0
SET LINESIZE 32767
SET TRIMSPOOL ON
SET FEEDBACK OFF
SET LONG 100000

PROMPT
PROMPT ==========================================================================
PROMPT  BLOQUE 0 - RESUMEN (para saber que esperar de los bloques siguientes)
PROMPT ==========================================================================

SELECT 'roles'                  objeto, COUNT(*) cuantos FROM SERV_MED_SEC_ROLE
UNION ALL SELECT 'menus',                COUNT(*) FROM SERV_MED_MENU
UNION ALL SELECT 'asignaciones menu-rol',COUNT(*) FROM SERV_MED_MENU_ROLE
UNION ALL SELECT 'usuarios',             COUNT(*) FROM SERV_MED_SEC_USER
UNION ALL SELECT 'asignaciones user-rol',COUNT(*) FROM SERV_MED_SEC_USER_ROLE;

PROMPT
PROMPT -- Menus por rol (asi se ve hoy el sidebar de cada rol):
SELECT r.CODE AS rol, COUNT(*) AS menus
  FROM SERV_MED_MENU_ROLE mr
  JOIN SERV_MED_SEC_ROLE r ON r.ID = mr.ROLE_ID
 GROUP BY r.CODE ORDER BY r.CODE;

PROMPT
PROMPT -- Menus que existen pero NO estan asignados a ningun rol (nadie los ve):
SELECT m.CODE
  FROM SERV_MED_MENU m
 WHERE NOT EXISTS (SELECT 1 FROM SERV_MED_MENU_ROLE mr WHERE mr.MENU_ID = m.ID)
 ORDER BY m.ORDER_NO, m.CODE;

PROMPT
PROMPT ==========================================================================
PROMPT  BLOQUE 1 - ROLES que NO estan en la linea base del DDL
PROMPT  (aqui debe salir ROLE_MEDICO_ANALISTA; si no sale, el rol no existe en
PROMPT   este ambiente y el Dashboard Ejecutivo esta dando 403)
PROMPT ==========================================================================

SELECT 'MERGE INTO SERV_MED_SEC_ROLE t USING (SELECT '''
       || REPLACE(CODE, '''', '''''') || ''' code, '''
       || REPLACE(NAME, '''', '''''') || ''' name FROM dual) s'
       || ' ON (t.CODE = s.code) WHEN NOT MATCHED THEN INSERT (CODE, NAME, DESCRIPTION, ACTIVE)'
       || ' VALUES (s.code, s.name, '
       || CASE WHEN DESCRIPTION IS NULL THEN 'NULL'
               ELSE '''' || REPLACE(DESCRIPTION, '''', '''''') || '''' END
       || ', ''' || ACTIVE || ''');' AS sentencia
  FROM SERV_MED_SEC_ROLE
 WHERE CODE NOT IN ('ROLE_ADMIN', 'USER', 'ADM', 'ENFERMERO')
 ORDER BY CODE;

PROMPT
PROMPT ==========================================================================
PROMPT  BLOQUE 2 - MENUS que NO estan en la linea base del DDL
PROMPT ==========================================================================

SELECT 'MERGE INTO SERV_MED_MENU t USING (SELECT '''
       || REPLACE(CODE, '''', '''''') || ''' code FROM dual) s ON (t.CODE = s.code)'
       || ' WHEN NOT MATCHED THEN INSERT (CODE, TITLE, PATH, ICON, ORDER_NO, ACTIVE) VALUES ('''
       || REPLACE(CODE, '''', '''''') || ''', '''
       || REPLACE(TITLE, '''', '''''') || ''', '
       || CASE WHEN PATH IS NULL THEN 'NULL'
               ELSE '''' || REPLACE(PATH, '''', '''''') || '''' END || ', '
       || CASE WHEN ICON IS NULL THEN 'NULL'
               ELSE '''' || REPLACE(ICON, '''', '''''') || '''' END || ', '
       || ORDER_NO || ', ''' || ACTIVE || ''');' AS sentencia
  FROM SERV_MED_MENU
 WHERE CODE NOT IN ('MNU_ROOT','MNU_SECURITY','MNU_FILES',
                    'EXPEDIENTE_GENERAL','LABORATORIO','HISTORICO_EM','NOTA_MEDICA',
                    'NOTA_INCAPACIDAD','ARCHIVO_CONSULTAS','CONSULTA_MEDICA','INCAPACIDADES',
                    'ARCHIVO_INCAPACIDADES','EXAMEN_MEDICO','PRETEST','ANTIDOPING',
                    'ACCIDENTES','MATERNIDAD',
                    'ANTIDOPING_SELECCION','CONSUMIBLES','CAUSAS_CONSULTA')
 ORDER BY ORDER_NO, CODE;

PROMPT
PROMPT ==========================================================================
PROMPT  BLOQUE 3 - TODAS las asignaciones MENU-ROL
PROMPT  Se exportan completas (no solo las nuevas) porque en QA pudieron QUITARSE
PROMPT  asignaciones de la linea base desde /admin/roles/menus. Son idempotentes:
PROMPT  el NOT EXISTS evita duplicar lo que el DDL ya puso.
PROMPT  OJO: esto AGREGA lo que falte, no QUITA lo que el DDL puso de mas. Si en QA
PROMPT  algun rol tiene MENOS menus que la linea base, comparar con el Bloque 0 y
PROMPT  quitar a mano esa asignacion en produccion.
PROMPT ==========================================================================

SELECT 'INSERT INTO SERV_MED_MENU_ROLE (MENU_ID, ROLE_ID) SELECT m.ID, r.ID'
       || ' FROM SERV_MED_MENU m, SERV_MED_SEC_ROLE r WHERE m.CODE = '''
       || REPLACE(m.CODE, '''', '''''') || ''' AND r.CODE = '''
       || REPLACE(r.CODE, '''', '''''') || ''''
       || ' AND NOT EXISTS (SELECT 1 FROM SERV_MED_MENU_ROLE x WHERE x.MENU_ID = m.ID AND x.ROLE_ID = r.ID);' AS sentencia
  FROM SERV_MED_MENU_ROLE mr
  JOIN SERV_MED_MENU m     ON m.ID = mr.MENU_ID
  JOIN SERV_MED_SEC_ROLE r ON r.ID = mr.ROLE_ID
 ORDER BY r.CODE, m.ORDER_NO, m.CODE;

PROMPT
PROMPT ==========================================================================
PROMPT  BLOQUE 4 - USUARIOS
PROMPT  PASSWORD_HASH se exporta como NULL A PROPOSITO: con
PROMPT  portal.auth.strategy=LEGACY_PHP la contrasena la valida ORDS y el hash local
PROMPT  no es fuente de verdad (ver 03_null_password_legacy_php.sql). Ademas asi no
PROMPT  viajan hashes en un texto que se copia y pega.
PROMPT ==========================================================================

SELECT 'MERGE INTO SERV_MED_SEC_USER t USING (SELECT '''
       || REPLACE(USERNAME, '''', '''''') || ''' username FROM dual) s'
       || ' ON (t.USERNAME = s.username) WHEN NOT MATCHED THEN'
       || ' INSERT (USERNAME, EMAIL, PASSWORD_HASH, DISPLAY_NAME, ACCOUNT_STATUS, ACTIVE) VALUES ('''
       || REPLACE(USERNAME, '''', '''''') || ''', '
       || CASE WHEN EMAIL IS NULL THEN 'NULL'
               ELSE '''' || REPLACE(EMAIL, '''', '''''') || '''' END
       || ', NULL, '
       || CASE WHEN DISPLAY_NAME IS NULL THEN 'NULL'
               ELSE '''' || REPLACE(DISPLAY_NAME, '''', '''''') || '''' END
       || ', ''' || ACCOUNT_STATUS || ''', ''' || ACTIVE || ''');' AS sentencia
  FROM SERV_MED_SEC_USER
 ORDER BY USERNAME;

PROMPT
PROMPT ==========================================================================
PROMPT  BLOQUE 5 - ASIGNACIONES USUARIO-ROL
PROMPT ==========================================================================

SELECT 'INSERT INTO SERV_MED_SEC_USER_ROLE (USER_ID, ROLE_ID) SELECT u.ID, r.ID'
       || ' FROM SERV_MED_SEC_USER u, SERV_MED_SEC_ROLE r WHERE u.USERNAME = '''
       || REPLACE(u.USERNAME, '''', '''''') || ''' AND r.CODE = '''
       || REPLACE(r.CODE, '''', '''''') || ''''
       || ' AND NOT EXISTS (SELECT 1 FROM SERV_MED_SEC_USER_ROLE x WHERE x.USER_ID = u.ID AND x.ROLE_ID = r.ID);' AS sentencia
  FROM SERV_MED_SEC_USER_ROLE ur
  JOIN SERV_MED_SEC_USER u     ON u.ID = ur.USER_ID
  JOIN SERV_MED_SEC_ROLE r     ON r.ID = ur.ROLE_ID
 ORDER BY u.USERNAME, r.CODE;

PROMPT
PROMPT ==========================================================================
PROMPT  FIN. Al pegar la salida en produccion, cerrar con COMMIT; (son INSERT/MERGE,
PROMPT  no DDL autoconfirmado). Verificar despues con el Bloque 0 de este mismo
PROMPT  script corrido en produccion: los conteos deben coincidir con QA.
PROMPT ==========================================================================

SET FEEDBACK ON
SET PAGESIZE 14
