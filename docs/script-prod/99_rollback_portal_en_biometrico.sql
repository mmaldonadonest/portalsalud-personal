-- =====================================================================================
-- 02_rollback_portal_en_biometrico.sql  -  REVERSA de 01_ddl_portal_en_biometrico.sql
--
-- Deshace la instalacion del esquema del portal en BIOMETRICO@PDBPRD. Existe porque el DDL
-- es autoconfirmado: ROLLBACK no sirve, la unica vuelta atras es borrar lo que se creo.
--
-- -------------------------------------------------------------------------------------
-- POR QUE ESTE SCRIPT NO USA COMODINES  (leer antes de improvisar una limpieza)
-- -------------------------------------------------------------------------------------
-- Inventario real de BIOMETRICO tomado el 24-sep-2026: YA HAY 64 tablas SERV_MED_*, todas
-- con datos de produccion, y el DDL del portal agrega 20 mas. Despues de instalar, el
-- esquema tendra 84 tablas con ese prefijo. De las 64 que ya estaban:
--
--   * 53 son del PHP legacy: el expediente clinico completo (SERV_MED_ABDOMEN,
--     SERV_MED_DIAGNOSTICO, SERV_MED_EXPLORACION_FISICA, SERV_MED_RESULTADO_EXAMEN,
--     SERV_MED_CAT_INDICE_IDC10 con las 909 claves ICD, SERV_MED_DET_ANT_LABORALES con
--     37,770 filas...). Cada una ronda las 3,056-3,069 filas: un renglon por examen.
--
--   * 11 son DE ESTE PROYECTO, pero del lado de ORDS, no de JDBC: las creamos en agosto
--     para Accidentes, Antidoping, Causas de consulta, Restricciones, Maternidad y
--     Predio/Cuenta. El portal las usa por HTTP via los WS de ORDS, NO por la conexion
--     JDBC, y por eso NO estan entre las 20 de este script:
--         SERV_MED_ACCIDENTE               SERV_MED_ANTIDOPING_INVENTARIO
--         SERV_MED_ACCIDENTE_SEGUIMIENTO   SERV_MED_ANTIDOPING_RESULTADO
--         SERV_MED_CAT_CAUSA_CONSULTA      SERV_MED_ANTIDOPING_SELECCION
--         SERV_MED_CUENTA_PREDIO           SERV_MED_MATERNIDAD_SEGUIMIENTO
--         SERV_MED_PREDIO                  SERV_MED_RESTRICCION_ASIGNADA
--         SERV_MED_RESULTADO_EXAMEN_HIST
--     TIENEN DATOS REALES y esta reversa NO las toca. Son faciles de confundir con las
--     nuestras porque tambien son "del portal": la diferencia es la via de acceso.
--     (Se reconocen en el diccionario porque son las unicas SERV_MED_* sin estadisticas
--     recolectadas: NUM_ROWS y LAST_ANALYZED en blanco.)
--
--   *** UN  DROP ... WHERE table_name LIKE 'SERV_MED_%'  BORRARIA LAS 64. ***
--
-- Por eso aqui TODOS los nombres van escritos uno por uno: son exactamente los 20 que
-- crea el DDL del portal por JDBC y nada mas. Nunca sustituir esta lista por un comodin.
--
-- -------------------------------------------------------------------------------------
-- SALVAGUARDA: no borra tablas CON DATOS
-- -------------------------------------------------------------------------------------
-- Cada tabla se borra SOLO si esta vacia. Si tiene filas, se reporta y se deja intacta.
-- Asi, una corrida por accidente despues de la migracion historica no destruye los 550 mil
-- tags ni los 21 mil archivos.
--
-- Para borrar de todas formas (perdiendo esos datos), cambiar abajo:
--        v_forzar CONSTANT CHAR(1) := 'N';     -->  := 'S';
-- Hacerlo solo con una decision explicita y con respaldo verificado.
--
-- ORDEN: primero triggers/vistas/procedimientos/funcion, luego las tablas de la hoja al
-- tronco (las hijas antes que las padres), para que las FK no estorben.
-- =====================================================================================

SET SERVEROUTPUT ON;

-- -------------------------------------------------------------------------------------
-- PASO 0: que se va a borrar y cuantas filas tiene cada tabla (SOLO LECTURA).
--         Revisar esta salida ANTES de dejar correr el resto.
-- -------------------------------------------------------------------------------------
DECLARE
  PROCEDURE contar(p_tabla VARCHAR2) IS
    v_filas NUMBER; v_existe NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_existe FROM user_tables WHERE table_name = p_tabla;
    IF v_existe = 0 THEN
      DBMS_OUTPUT.PUT_LINE(RPAD(p_tabla, 32) || '  (no existe)');
      RETURN;
    END IF;
    EXECUTE IMMEDIATE 'SELECT COUNT(*) FROM ' || p_tabla INTO v_filas;
    DBMS_OUTPUT.PUT_LINE(RPAD(p_tabla, 32) || TO_CHAR(v_filas, '9,999,999,999') ||
                         CASE WHEN v_filas > 0 THEN '   <-- CON DATOS, no se borrara' END);
  END;
BEGIN
  DBMS_OUTPUT.PUT_LINE('--- tablas del portal y sus filas ---');
  contar('SERV_MED_AUDIT_SQL_EXECUTION');
  contar('SERV_MED_AUD_EVENT');
  contar('SERV_MED_FS_FILE');
  contar('SERV_MED_FS_FILE_ACCESS_LOG');
  contar('SERV_MED_FS_FILE_ORPHAN');
  contar('SERV_MED_FS_FILE_POLICY');
  contar('SERV_MED_FS_FILE_VERSION');
  contar('SERV_MED_IMPORT_FILA');
  contar('SERV_MED_IMPORT_LOTE');
  contar('SERV_MED_JOB_CATALOG');
  contar('SERV_MED_MENU');
  contar('SERV_MED_MENU_ROLE');
  contar('SERV_MED_NOTIF_TEMPLATE');
  contar('SERV_MED_SEC_PERMISSION');
  contar('SERV_MED_SEC_ROLE');
  contar('SERV_MED_SEC_ROLE_PERMISSION');
  contar('SERV_MED_SEC_USER');
  contar('SERV_MED_SEC_USER_ROLE');
  contar('SERV_MED_TAG');
  contar('SERV_MED_TAG_MIG_LOG');
END;
/

PROMPT ============================================================
PROMPT [1/3] TRIGGERS, VISTAS, PROCEDIMIENTO Y FUNCION
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
  borrar('TRIGGER',   'SERV_MED_TRG_FS_FILE_BU');
  borrar('TRIGGER',   'SERV_MED_TRG_FS_FILE_POLICY_BU');
  borrar('TRIGGER',   'SERV_MED_TRG_JOB_CATALOG_BU');
  borrar('TRIGGER',   'SERV_MED_TRG_MENU_BU');
  borrar('TRIGGER',   'SERV_MED_TRG_NOTIF_TPL_BU');
  borrar('TRIGGER',   'SERV_MED_TRG_SEC_PERM_BU');
  borrar('TRIGGER',   'SERV_MED_TRG_SEC_ROLE_BU');
  borrar('TRIGGER',   'SERV_MED_TRG_SEC_USER_BU');
  borrar('VIEW',      'SERV_MED_VW_ACTIVE_USERS');
  borrar('VIEW',      'SERV_MED_VW_FILE_LATEST');
  borrar('VIEW',      'SERV_MED_V_TAG_BY_GROUP');
  borrar('VIEW',      'SERV_MED_V_TAG_LATEST');
  borrar('VIEW',      'SERV_MED_V_TAG_MIG_STATUS');
  borrar('VIEW',      'SERV_MED_V_TAG_ORPHANS');
  borrar('PROCEDURE', 'SERV_MED_SP_LOG_SQL_EXEC');
  borrar('PROCEDURE', 'SERV_MED_SP_MIGRATE_TAGS');
  borrar('FUNCTION',  'SERV_MED_FN_TAG_GROUP');
END;
/

PROMPT ============================================================
PROMPT [2/3] TABLAS  (solo las vacias, salvo que v_forzar = 'S')
PROMPT ============================================================

DECLARE
  -- Cambiar a 'S' SOLO con decision explicita: borra tablas AUNQUE TENGAN DATOS.
  v_forzar CONSTANT CHAR(1) := 'N';

  PROCEDURE borrar_tabla(p_tabla VARCHAR2) IS
    v_existe NUMBER;
    v_filas  NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_existe FROM user_tables WHERE table_name = p_tabla;
    IF v_existe = 0 THEN
      DBMS_OUTPUT.PUT_LINE('[SKIP] no existe: ' || p_tabla);
      RETURN;
    END IF;

    EXECUTE IMMEDIATE 'SELECT COUNT(*) FROM ' || p_tabla INTO v_filas;
    IF v_filas > 0 AND v_forzar <> 'S' THEN
      DBMS_OUTPUT.PUT_LINE('[ALTO] ' || RPAD(p_tabla, 32) || TO_CHAR(v_filas, '9,999,999,999')
                           || ' filas: NO se borra (v_forzar = N)');
      RETURN;
    END IF;

    EXECUTE IMMEDIATE 'DROP TABLE ' || p_tabla || ' CASCADE CONSTRAINTS PURGE';
    DBMS_OUTPUT.PUT_LINE('[OK]   borrada: ' || RPAD(p_tabla, 32) ||
                         CASE WHEN v_filas > 0 THEN '(tenia ' || v_filas || ' filas)' END);
  END;
BEGIN
  borrar_tabla('SERV_MED_MENU_ROLE');
  borrar_tabla('SERV_MED_SEC_USER_ROLE');
  borrar_tabla('SERV_MED_SEC_ROLE_PERMISSION');
  borrar_tabla('SERV_MED_IMPORT_FILA');
  borrar_tabla('SERV_MED_IMPORT_LOTE');
  borrar_tabla('SERV_MED_FS_FILE_ACCESS_LOG');
  borrar_tabla('SERV_MED_FS_FILE_VERSION');
  borrar_tabla('SERV_MED_FS_FILE_ORPHAN');
  borrar_tabla('SERV_MED_FS_FILE_POLICY');
  borrar_tabla('SERV_MED_FS_FILE');
  borrar_tabla('SERV_MED_MENU');
  borrar_tabla('SERV_MED_SEC_USER');
  borrar_tabla('SERV_MED_SEC_ROLE');
  borrar_tabla('SERV_MED_SEC_PERMISSION');
  borrar_tabla('SERV_MED_AUD_EVENT');
  borrar_tabla('SERV_MED_AUDIT_SQL_EXECUTION');
  borrar_tabla('SERV_MED_JOB_CATALOG');
  borrar_tabla('SERV_MED_NOTIF_TEMPLATE');
  borrar_tabla('SERV_MED_TAG_MIG_LOG');
  borrar_tabla('SERV_MED_TAG');
END;
/

PROMPT ============================================================
PROMPT [3/3] VERIFICACION
PROMPT ============================================================

-- (a) Ninguna de las 20 del portal debe seguir aqui. Si queda alguna, es porque tenia
--     datos y la salvaguarda la protegio: revisar la salida del PASO 0.
SELECT table_name FROM user_tables
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
                      'SERV_MED_TAG_MIG_LOG')
 ORDER BY table_name;

-- (b) Las 64 que ya estaban deben seguir INTACTAS: 53 del PHP legacy + 11 del portal via
--     ORDS. Este conteo tiene que dar 64 (o el numero que arroje el PRE-CHECK bloque (c)
--     el dia que se instale, si entre tanto alguien agrego otra).
SELECT COUNT(*) AS serv_med_preexistentes FROM user_tables
 WHERE table_name LIKE 'SERV\_MED\_%' ESCAPE '\'
   AND table_name NOT IN ('SERV_MED_AUDIT_SQL_EXECUTION',
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

-- (b2) Nominal, las 11 del portal del lado de ORDS: deben aparecer LAS ONCE. Si falta
--      alguna, esta reversa borro algo que no debia y hay que restaurar del respaldo.
SELECT table_name FROM user_tables
 WHERE table_name IN ('SERV_MED_ACCIDENTE','SERV_MED_ACCIDENTE_SEGUIMIENTO',
                      'SERV_MED_ANTIDOPING_INVENTARIO','SERV_MED_ANTIDOPING_RESULTADO',
                      'SERV_MED_ANTIDOPING_SELECCION','SERV_MED_CAT_CAUSA_CONSULTA',
                      'SERV_MED_CUENTA_PREDIO','SERV_MED_MATERNIDAD_SEGUIMIENTO',
                      'SERV_MED_PREDIO','SERV_MED_RESTRICCION_ASIGNADA',
                      'SERV_MED_RESULTADO_EXAMEN_HIST')
 ORDER BY table_name;

-- (c) Y las APP_* del otro sistema, tambien intactas: deben seguir siendo 10.
SELECT COUNT(*) AS app_otro_sistema FROM user_tables
 WHERE table_name LIKE 'APP\_%' ESCAPE '\';

