-- =============================================================================
-- CARGA DE EXCEL POR NSS -- layout 8 MATERNIDAD: formato de SM «8 MATERNIDAD 2026» (27-nov-2026)
--
-- BASE: la del PORTAL = BIOMETRICO de produccion (tabla SERV_MED_XLS_MATERNIDAD). NO va a ORDS.
-- LO CORRE: el usuario.
--
-- Por que: el formato de SM trae columnas que la tabla no tiene:
--   MES DETECCION     -> MES_DETECCION  (ENE, FEB, MZO...; la FECHA de las graficas = dia 1 de ese mes)
--   FECHA DE INGRESO  -> FECHA_INGRESO  (ingreso a la empresa; ANTES el cargador la tomaba como fecha de corte)
--   EDAD (rango)      -> RANGO_EDAD     ("18-25", "26-35"...)
--   CUENTA            -> CUENTA_ARCHIVO (la columna CUENTA sigue siendo la del biometrico)
--   PUESTO            -> PUESTO_ARCHIVO (la columna PUESTO sigue siendo la del biometrico)
-- EDAD, CUENTA y PUESTO del archivo se guardan aparte porque SM todavia no contesta cual se usa en las
-- estadisticas (preguntas-sm-26nov-maternidad.xlsx, pregunta 2).
--
-- QUE HACE:   agrega 5 columnas vacias a SERV_MED_XLS_MATERNIDAD.
-- QUE NO HACE: no toca datos, vistas, indices ni procedimientos. Sin DROP, UPDATE ni DELETE.
-- ORDEN: correrlo ANTES de instalar el WAR que carga el formato nuevo; sin estas columnas el INSERT del
--        cargador falla (ORA-00904) y la carga de maternidad no entra.
-- Idempotente: si la columna ya existe la salta (-1430).
--
-- Correr con:  @C:\miguel\portalsalud-personal\docs\script-prod\excel-nss-20-maternidad-formato-2026.sql
-- =============================================================================
SET SERVEROUTPUT ON
SET DEFINE OFF
SPOOL C:\miguel\maternidad-formato-2026-salida.txt

PROMPT === Usuario y base (debe decir BIOMETRICO y PDBPRD) ===
SELECT USER, SYS_CONTEXT('USERENV','CON_NAME') PDB FROM DUAL;

PROMPT === Columnas nuevas en SERV_MED_XLS_MATERNIDAD ===
BEGIN EXECUTE IMMEDIATE 'ALTER TABLE SERV_MED_XLS_MATERNIDAD ADD (MES_DETECCION VARCHAR2(20))';
EXCEPTION WHEN OTHERS THEN IF SQLCODE NOT IN (-1430) THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE 'ALTER TABLE SERV_MED_XLS_MATERNIDAD ADD (FECHA_INGRESO DATE)';
EXCEPTION WHEN OTHERS THEN IF SQLCODE NOT IN (-1430) THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE 'ALTER TABLE SERV_MED_XLS_MATERNIDAD ADD (RANGO_EDAD VARCHAR2(10))';
EXCEPTION WHEN OTHERS THEN IF SQLCODE NOT IN (-1430) THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE 'ALTER TABLE SERV_MED_XLS_MATERNIDAD ADD (CUENTA_ARCHIVO VARCHAR2(160))';
EXCEPTION WHEN OTHERS THEN IF SQLCODE NOT IN (-1430) THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE 'ALTER TABLE SERV_MED_XLS_MATERNIDAD ADD (PUESTO_ARCHIVO VARCHAR2(160))';
EXCEPTION WHEN OTHERS THEN IF SQLCODE NOT IN (-1430) THEN RAISE; END IF; END;
/
COMMENT ON COLUMN SERV_MED_XLS_MATERNIDAD.MES_DETECCION IS 'MES DETECCION del Excel de SM (ENE, MZO...); FECHA = dia 1 de ese mes (27-nov-2026)';
COMMENT ON COLUMN SERV_MED_XLS_MATERNIDAD.FECHA_INGRESO IS 'FECHA DE INGRESO a la empresa, del Excel de SM (27-nov-2026)';
COMMENT ON COLUMN SERV_MED_XLS_MATERNIDAD.RANGO_EDAD IS 'EDAD (rango) del Excel de SM; EDAD es la del biometrico (27-nov-2026)';
COMMENT ON COLUMN SERV_MED_XLS_MATERNIDAD.CUENTA_ARCHIVO IS 'CUENTA del Excel de SM; CUENTA es la del biometrico (27-nov-2026)';
COMMENT ON COLUMN SERV_MED_XLS_MATERNIDAD.PUESTO_ARCHIVO IS 'PUESTO del Excel de SM; PUESTO es el del biometrico (27-nov-2026)';

PROMPT === VERIFICACION (solo lectura). Esperado: 5 filas y las 2 vistas VALID ===
SELECT column_name, data_type, data_length FROM user_tab_columns
 WHERE table_name = 'SERV_MED_XLS_MATERNIDAD'
   AND column_name IN ('MES_DETECCION', 'FECHA_INGRESO', 'RANGO_EDAD', 'CUENTA_ARCHIVO', 'PUESTO_ARCHIVO')
 ORDER BY column_name;
SELECT object_name, status FROM user_objects
 WHERE object_name IN ('SERV_MED_V_MATERNIDAD', 'SERV_MED_V_MATERNIDAD_ACTUAL') ORDER BY 1;

SPOOL OFF

-- REVERSA (solo despues de regresar al WAR anterior):
-- ALTER TABLE SERV_MED_XLS_MATERNIDAD DROP (MES_DETECCION, FECHA_INGRESO, RANGO_EDAD, CUENTA_ARCHIVO, PUESTO_ARCHIVO);
