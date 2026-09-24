-- =====================================================================================
-- ords-inventario-produccion.sql  —  ORDS / esquema legacy de PRODUCCION. SOLO LECTURA.
-- Fase 1 del plan (docs/plan-migracion-produccion.md): saber que de lo que usa el portal
-- ya existe en la base de produccion y que hay que crear.
--
-- NO MODIFICA NADA: son unicamente SELECT sobre el diccionario de datos y conteos.
-- Lo ejecuta el usuario en SQL Developer, conectado al esquema que expone ORDS.
-- Guardar la salida completa; con ella se llena la matriz "existe / falta" y se decide
-- que scripts docs/ords-*.sql hay que aplicar (Fase 3).
-- =====================================================================================

SET LINESIZE 200 PAGESIZE 500
COLUMN objeto FORMAT A42
COLUMN estado FORMAT A12
COLUMN nota   FORMAT A46

-- ---------- 0. Contexto: donde estoy parado ----------
SELECT SYS_CONTEXT('USERENV','DB_NAME')       db,
       SYS_CONTEXT('USERENV','CON_NAME')      pdb,
       SYS_CONTEXT('USERENV','CURRENT_USER')  usuario,
       SYS_CONTEXT('USERENV','SERVER_HOST')   host,
       TO_CHAR(SYSDATE,'DD/MM/YYYY HH24:MI')  momento
  FROM dual;
SELECT banner FROM v$version WHERE ROWNUM = 1;

-- ---------- 1. ¿Es la base del legacy? Tablas que el portal consume por WS ----------
-- Si estas NO existen, esta base no es la del servicio medico (o esta vacia).
SELECT t.objeto, CASE WHEN u.table_name IS NULL THEN 'FALTA' ELSE 'existe' END estado, t.nota
  FROM (
        SELECT 'BIO_EMPLEADO'                  objeto, 'Plantilla (NSS, EMP_STATUS, EMP_FECHAI)' nota FROM dual UNION ALL
        SELECT 'BIO_DATOS_LABORALES_EMPLEADOS', 'Puesto/cuenta del empleado'                     FROM dual UNION ALL
        SELECT 'BIOMETRICO_CUENTA',             'Catalogo de cuentas'                            FROM dual UNION ALL
        SELECT 'BIOMETRICO_CUENTA_SAP',         'Catalogo de cuentas SAP'                        FROM dual UNION ALL
        SELECT 'TBL_SERV_CONSULTA_MEDICA',      'Consultas medicas'                              FROM dual UNION ALL
        SELECT 'TBL_SERV_INCAPACIDAD_MEDICA',   'Incapacidades'                                  FROM dual UNION ALL
        SELECT 'SERV_MED_RESULTADO_EXAMEN',     'Dictamen del examen medico'                     FROM dual UNION ALL
        SELECT 'SERV_MED_GENERALES',            'Una de las ~41 secciones del examen'            FROM dual UNION ALL
        SELECT 'SERV_MED_FILES',                'Indice de archivos del legacy'                  FROM dual UNION ALL
        SELECT 'SER_MED_REGISTRO_MEDICO',       'Registro medico'                                FROM dual UNION ALL
        SELECT 'TBL_APPS_ONEST',                'Catalogo de apps del launcher'                  FROM dual UNION ALL
        SELECT 'TBL_APPS_ROL',                  'Roles del launcher'                             FROM dual UNION ALL
        SELECT 'TBL_APPS_ROL_MENU',             'Menus por rol/app (gate del SSO, id_app=13)'    FROM dual UNION ALL
        SELECT 'TBL_APP_ROL_USUARIO',           'Usuario-rol del launcher'                       FROM dual UNION ALL
        SELECT 'TBL_APPS_USUARIO',              'Usuarios del launcher'                          FROM dual
       ) t
  LEFT JOIN user_tables u ON u.table_name = t.objeto
 ORDER BY estado, t.objeto;

-- ---------- 2. Tablas que creo el portal Java (docs/ords-*.sql) ----------
-- Las que digan FALTA hay que crearlas con su script en la Fase 3.
SELECT t.objeto, CASE WHEN u.table_name IS NULL THEN 'FALTA' ELSE 'existe' END estado, t.nota
  FROM (
        SELECT 'SERV_MED_PREDIO'                 objeto, 'ords-predio-cuenta.sql (17 predios)'   nota FROM dual UNION ALL
        SELECT 'SERV_MED_CUENTA_PREDIO',         'ords-predio-cuenta.sql'                             FROM dual UNION ALL
        SELECT 'SERV_MED_CAT_CAUSA_CONSULTA',    'ords-causa-consulta.sql (23 causas)'                FROM dual UNION ALL
        SELECT 'SERV_MED_RESTRICCION_ASIGNADA',  'ords-restriccion.sql'                               FROM dual UNION ALL
        SELECT 'SERV_MED_ACCIDENTE',             'ords-accidentes.sql'                                FROM dual UNION ALL
        SELECT 'SERV_MED_ACCIDENTE_SEGUIMIENTO', 'ords-accidentes-seguimiento.sql'                    FROM dual UNION ALL
        SELECT 'SERV_MED_ANTIDOPING_RESULTADO',  'ords-antidoping.sql'                                FROM dual UNION ALL
        SELECT 'SERV_MED_ANTIDOPING_INVENTARIO', 'ords-antidoping.sql (consumibles)'                  FROM dual UNION ALL
        SELECT 'SERV_MED_ANTIDOPING_SELECCION',  'ords-antidoping-seleccion.sql'                      FROM dual UNION ALL
        SELECT 'SERV_MED_MATERNIDAD_SEGUIMIENTO','ords-maternidad.sql'                                FROM dual UNION ALL
        SELECT 'SERV_MED_RESULTADO_EXAMEN_HIST', 'ords-examen-historial.sql'                          FROM dual
       ) t
  LEFT JOIN user_tables u ON u.table_name = t.objeto
 ORDER BY estado, t.objeto;

-- ---------- 3. Procedimientos y funciones que usan los WS ----------
SELECT o.object_name, o.object_type, o.status, TO_CHAR(o.last_ddl_time,'DD/MM/YYYY') ultimo_cambio
  FROM user_objects o
 WHERE o.object_type IN ('PROCEDURE','FUNCTION','PACKAGE')
   AND (o.object_name LIKE 'PR_SERVICIO%' OR o.object_name LIKE '%MED%' OR o.object_name LIKE 'FN_%')
 ORDER BY o.object_type, o.object_name;

-- Cualquier objeto INVALID en el esquema (un proc invalido hace fallar el WS que lo llama)
SELECT object_name, object_type, status FROM user_objects WHERE status <> 'VALID' ORDER BY object_type, object_name;

-- ---------- 4. Cuantos datos hay (para saber si es la base productiva) ----------
SELECT 'BIO_EMPLEADO' tabla, COUNT(*) filas FROM BIO_EMPLEADO
UNION ALL SELECT 'TBL_SERV_CONSULTA_MEDICA', COUNT(*) FROM TBL_SERV_CONSULTA_MEDICA
UNION ALL SELECT 'TBL_SERV_INCAPACIDAD_MEDICA', COUNT(*) FROM TBL_SERV_INCAPACIDAD_MEDICA
UNION ALL SELECT 'SERV_MED_RESULTADO_EXAMEN', COUNT(*) FROM SERV_MED_RESULTADO_EXAMEN
UNION ALL SELECT 'SERV_MED_FILES', COUNT(*) FROM SERV_MED_FILES;

-- Vigencia del empleado: que valores usa EMP_STATUS en esta base (el legacy es inconsistente,
-- el login trata 0 como vigente y el WS de examen trata 1). Aqui se decide el criterio real.
SELECT EMP_STATUS, COUNT(*) empleados, MIN(EMP_FECHAI) ingreso_min, MAX(EMP_FECHAI) ingreso_max
  FROM BIO_EMPLEADO GROUP BY EMP_STATUS ORDER BY 1;

-- ---------- 5. Gate del SSO: la app de Salud (id_app = 13) ----------
SELECT * FROM TBL_APPS_ONEST ORDER BY 1;
SELECT ID_APP, ID_ROL, COUNT(*) menus FROM TBL_APPS_ROL_MENU GROUP BY ID_APP, ID_ROL ORDER BY ID_APP, ID_ROL;
-- Menus dados de alta para la app de Salud (sin esto, el usuario entra por SSO y no ve modulos)
SELECT * FROM TBL_APPS_ROL_MENU WHERE ID_APP = 13 ORDER BY ID_ROL;

-- ---------- 6. Catalogos que el portal espera ----------
-- ICD/CIE: en QA solo hay 909 claves (capitulos A y B). Confirmar si aqui pasa lo mismo.
SELECT COUNT(*) claves_icd FROM user_tables WHERE table_name LIKE '%ICD%' OR table_name LIKE '%CIE%';
SELECT table_name FROM user_tables WHERE table_name LIKE '%ICD%' OR table_name LIKE '%CIE%' ORDER BY 1;

-- ---------- 7. ¿Esta base albergara tambien el esquema del portal (APP_*)? ----------
-- Si ya hay tablas APP_*, alguien las creo antes; si no, es la Fase 2 del plan.
SELECT table_name FROM user_tables WHERE table_name LIKE 'APP\_%' ESCAPE '\' OR table_name = 'SERV_MED_TAG' ORDER BY 1;

-- Espacio y privilegios del usuario (para dimensionar la carga del ETL: ~18 GB de archivos
-- van al filesystem, no a Oracle, pero SERV_MED_TAG son ~550 mil filas).
SELECT tablespace_name, ROUND(SUM(bytes)/1024/1024) mb_usados FROM user_segments GROUP BY tablespace_name ORDER BY 2 DESC;
SELECT * FROM user_sys_privs ORDER BY privilege;
SELECT granted_role FROM user_role_privs ORDER BY 1;

-- ---------- 8. Tablas de depuracion que hay que vaciar antes de salir a produccion ----------
SELECT t.objeto, CASE WHEN u.table_name IS NULL THEN 'no existe' ELSE 'existe' END estado, ' ' nota
  FROM (SELECT 'BUG' objeto FROM dual UNION ALL SELECT 'PRUEBA' FROM dual UNION ALL SELECT 'ONSYS_DEBUG' FROM dual) t
  LEFT JOIN user_tables u ON u.table_name = t.objeto;
