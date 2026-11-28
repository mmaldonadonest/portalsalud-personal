-- ============================================================================================
-- Maternidad: edad y cuenta DEL ARCHIVO en las estadisticas - 27-nov-2026
--
-- Base: BIOMETRICO (PDBPRD) - vistas del PORTAL SERV_MED_V_MATERNIDAD y SERV_MED_V_MATERNIDAD_ACTUAL.
--       NO va a ORDS.
--
-- Por que: respuesta de Servicio Medico (preguntas-sm-27nov-maternidad, pregunta 2 = a): para las estadisticas de
-- maternidad se usan la EDAD y la CUENTA del archivo «8 MATERNIDAD 2026», no las del biometrico. El cargador ya las
-- guarda aparte (excel-nss-20: RANGO_EDAD y CUENTA_ARCHIVO); la vista seguia dando las del biometrico.
--
-- Que hace:
--   0. Respaldo de las 2 vistas actuales en C:\miguel\respaldosql (antes de cambiar nada).
--   1. Vuelve a crear SERV_MED_V_MATERNIDAD igual que excel-nss-05-vistas.sql (v4) con 2 cambios, solo en los
--      renglones del Excel:
--        - CUENTA = la del archivo (CUENTA_ARCHIVO); si el renglon no la trae (cargas anteriores), la del biometrico.
--        - columna nueva RANGO_EDAD al final = el rango del archivo ("26-35"...). En los del portal va vacia: ahi el
--          portal calcula el rango con la edad del biometrico, como hoy.
--   2. Vuelve a crear SERV_MED_V_MATERNIDAD_ACTUAL (misma definicion; se recrea para que tome la columna nueva).
-- No toca datos ni tablas. Idempotente.
--
-- ORDEN: correr ESTE script ANTES de instalar el WAR que lee RANGO_EDAD. Con el WAR nuevo y sin este script, la
-- grafica de edades de maternidad falla (columna inexistente).
--
-- Reversa: correr los 2 respaldos del paso 0 (recrean las vistas como estaban), solo despues de regresar al WAR
-- anterior.
--
-- Correr con:  @C:\miguel\portalsalud-personal\docs\script-prod\excel-nss-21-maternidad-edad-cuenta-archivo.sql
-- ============================================================================================
SET SERVEROUTPUT ON
SET DEFINE OFF

PROMPT === 0) Respaldo de las vistas actuales ===
SET LONG 200000
SET LONGCHUNKSIZE 200000
SET PAGESIZE 0
SET LINESIZE 32767
SET TRIMSPOOL ON
SET FEEDBACK OFF
SET HEADING OFF
EXEC DBMS_METADATA.SET_TRANSFORM_PARAM(DBMS_METADATA.SESSION_TRANSFORM, 'SQLTERMINATOR', TRUE);
SPOOL C:\miguel\respaldosql\SERV_MED_V_MATERNIDAD_antes_edad_cuenta_27nov2026.sql
SELECT DBMS_METADATA.GET_DDL('VIEW', 'SERV_MED_V_MATERNIDAD') FROM DUAL;
SPOOL OFF
SPOOL C:\miguel\respaldosql\SERV_MED_V_MATERNIDAD_ACTUAL_antes_edad_cuenta_27nov2026.sql
SELECT DBMS_METADATA.GET_DDL('VIEW', 'SERV_MED_V_MATERNIDAD_ACTUAL') FROM DUAL;
SPOOL OFF
SET PAGESIZE 300
SET LINESIZE 220
SET FEEDBACK ON
SET HEADING ON

SPOOL C:\miguel\maternidad-edad-cuenta-salida.txt

PROMPT === Usuario y base (debe decir BIOMETRICO y PDBPRD) ===
SELECT USER, SYS_CONTEXT('USERENV','CON_NAME') PDB FROM DUAL;

PROMPT === 1) SERV_MED_V_MATERNIDAD con CUENTA y RANGO_EDAD del archivo ===
CREATE OR REPLACE VIEW SERV_MED_V_MATERNIDAD AS
SELECT CAST('PORTAL' AS VARCHAR2(12))                              FUENTE,
       TO_CHAR(p.REG_ID)                                           ID_ORIGEN,
       CAST(p.NSS AS VARCHAR2(13))                                 NSS,
       TRUNC(p.FECHA_REGISTRO)                                     FECHA,
       emp.PREDIO                                                  PREDIO,
       emp.CUENTA                                                  CUENTA,
       CAST(NULL AS VARCHAR2(120))                                 AREA,
       emp.NOMBRE                                                  NOMBRE,
       emp.GENERO                                                  GENERO,
       TRUNC(MONTHS_BETWEEN(TRUNC(p.FECHA_REGISTRO), emp.FECHA_NAC) / 12) EDAD,
       emp.PUESTO                                                  PUESTO,
       CAST(p.NSS || CASE WHEN p.FECHA_PROBABLE_PARTO IS NOT NULL
                          THEN '|FPP:' || TO_CHAR(p.FECHA_PROBABLE_PARTO, 'YYYY-MM-DD')
                          ELSE '|PORTAL' END AS VARCHAR2(320))     CLAVE_EMBARAZO,
       CAST(SUBSTR(p.ESTATUS, 1, 40) AS VARCHAR2(40))              STATUS_LABORAL,
       CAST(NULL AS DATE)                                          FUM,
       p.SEMANAS_GESTACION                                         SEMANAS,
       p.FECHA_PROBABLE_PARTO                                      FECHA_PROB_PARTO,
       p.REINCORPORACION                                           FECHA_REAL_REINICIO,
       CAST(NULL AS DATE)                                          FECHA_INICIO_INCAP,
       CAST(NULL AS DATE)                                          FECHA_TERMINO_INCAP,
       CAST(SUBSTR(p.INCAPACIDAD, 1, 100) AS VARCHAR2(100))        FOLIO_INCAPACIDAD,
       CAST(NULL AS DATE)                                          FECHA_INICIO_LACTANCIA,
       CAST(NULL AS DATE)                                          FECHA_TERMINO_LACTANCIA,
       p.PROXIMA_REVISION                                          PROXIMA_REVISION,
       CAST(NULL AS VARCHAR2(10))                                  RANGO_EDAD
  FROM SERV_MED_MATERNIDAD_SEGUIMIENTO p
  LEFT JOIN SERV_MED_V_EMPLEADO emp ON emp.NSS = p.NSS
UNION ALL
SELECT 'EXCEL', TO_CHAR(x.ID), x.NSS, x.FECHA, x.PREDIO,
       NVL(TRIM(x.CUENTA_ARCHIVO), x.CUENTA),
       x.AREA, x.NOMBRE, x.GENERO, x.EDAD, x.PUESTO,
       x.CLAVE_EMBARAZO, x.STATUS_LABORAL, x.FUM,
       CASE WHEN x.FUM IS NOT NULL AND x.FECHA >= x.FUM THEN TRUNC((x.FECHA - x.FUM) / 7) END,
       x.FECHA_PROB_PARTO, x.FECHA_REAL_REINICIO, x.FECHA_INICIO_INCAP, x.FECHA_TERMINO_INCAP,
       x.FOLIO_INCAPACIDAD, x.FECHA_INICIO_LACTANCIA, x.FECHA_TERMINO_LACTANCIA, NULL,
       x.RANGO_EDAD
  FROM SERV_MED_XLS_MATERNIDAD x
 WHERE x.ESTADO_REGISTRO = 'VALIDO'
   AND NOT EXISTS (SELECT 1 FROM SERV_MED_MATERNIDAD_SEGUIMIENTO p
                    WHERE p.NSS = x.NSS AND TRUNC(p.FECHA_REGISTRO, 'MM') = TRUNC(x.FECHA, 'MM'));

COMMENT ON TABLE SERV_MED_V_MATERNIDAD IS 'Maternidad con historial: chequeos del portal + Excel (SERV_MED_XLS_MATERNIDAD). Del Excel, CUENTA y RANGO_EDAD del archivo (SM 27-nov-2026). Embarazo = CLAVE_EMBARAZO.';

PROMPT === 2) SERV_MED_V_MATERNIDAD_ACTUAL (misma definicion, toma la columna nueva) ===
CREATE OR REPLACE VIEW SERV_MED_V_MATERNIDAD_ACTUAL AS
SELECT m.*
  FROM (SELECT v.*, ROW_NUMBER() OVER (PARTITION BY v.CLAVE_EMBARAZO
                                        ORDER BY v.FECHA DESC, v.FUENTE, v.ID_ORIGEN DESC) rn
          FROM SERV_MED_V_MATERNIDAD v) m
 WHERE m.rn = 1;

COMMENT ON TABLE SERV_MED_V_MATERNIDAD_ACTUAL IS 'Maternidad: un renglon por embarazo con su estado mas reciente (ultima actualizacion).';

PROMPT === VERIFICACION (solo lectura). Esperado: 2 vistas VALID y la columna RANGO_EDAD en las dos ===
SELECT object_name, status FROM user_objects
 WHERE object_name IN ('SERV_MED_V_MATERNIDAD', 'SERV_MED_V_MATERNIDAD_ACTUAL') ORDER BY 1;
SELECT table_name, column_name FROM user_tab_columns
 WHERE table_name IN ('SERV_MED_V_MATERNIDAD', 'SERV_MED_V_MATERNIDAD_ACTUAL') AND column_name = 'RANGO_EDAD' ORDER BY 1;
PROMPT === Casos del Excel por cuenta y rango de edad (lo que van a ver las graficas) ===
SELECT CUENTA, RANGO_EDAD, COUNT(*) renglones FROM SERV_MED_V_MATERNIDAD
 WHERE FUENTE = 'EXCEL' GROUP BY CUENTA, RANGO_EDAD ORDER BY 1, 2;

SPOOL OFF
