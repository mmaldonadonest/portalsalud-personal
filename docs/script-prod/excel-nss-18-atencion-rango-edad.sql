-- ============================================================================================
-- Rango de edad en el layout 1 (ATENCIONES) - 12-nov-2026
--
-- Base: BIOMETRICO (PDBPRD) - tabla y vista del PORTAL (SERV_MED_XLS_ATENCION, SERV_MED_V_ATENCION).
--       NO va a ORDS.
--
-- Por que: el formato 2026 de Servicio Medico (ACUMULADO MODIFICAR 2026) trae la columna EDAD como
-- RANGO (18 - 25, 26 - 35, 36 - 45, 46 - 55, 55 +), no como numero. Mismo cambio que se hizo en
-- nuevo ingreso el 14-oct (excel-nss-13-rango-edad.sql): el cargador lo escribe en RANGO_EDAD
-- ("18-25" ... "55+") y la columna EDAD se queda para la edad del biometrico (casi siempre vacia).
--
-- Que hace:
--   0. Respaldo de la vista actual en C:\miguel\respaldosql (antes de cambiar nada).
--   1. Agrega RANGO_EDAD VARCHAR2(10) a SERV_MED_XLS_ATENCION (si ya existe, no hace nada).
--   2. Vuelve a crear SERV_MED_V_ATENCION igual que excel-nss-05-vistas.sql + RANGO_EDAD al final
--      (PORTAL = NULL: ahi el portal calcula el rango con la edad; EXCEL = lo que trajo el archivo).
-- No toca datos. Idempotente.
--
-- ORDEN: correr ESTE script ANTES de instalar el WAR que lee RANGO_EDAD. Con el WAR nuevo y sin
-- este script, la grafica de edades de atenciones falla (columna inexistente) y la carga tambien.
--
-- Reversa: correr el respaldo del paso 0 (recrea la vista anterior) y despues
--   ALTER TABLE SERV_MED_XLS_ATENCION DROP COLUMN RANGO_EDAD;
-- (solo despues de regresar al WAR anterior).
--
-- Correr con:  @C:\miguel\portalsalud-personal\docs\script-prod\excel-nss-18-atencion-rango-edad.sql
-- ============================================================================================
SET SERVEROUTPUT ON
SET DEFINE OFF

PROMPT === 0) Respaldo de la vista actual ===
SET LONG 200000
SET LONGCHUNKSIZE 200000
SET PAGESIZE 0
SET LINESIZE 32767
SET TRIMSPOOL ON
SET FEEDBACK OFF
SET HEADING OFF
EXEC DBMS_METADATA.SET_TRANSFORM_PARAM(DBMS_METADATA.SESSION_TRANSFORM, 'SQLTERMINATOR', TRUE);
SPOOL C:\miguel\respaldosql\SERV_MED_V_ATENCION_antes_rango_edad_12nov2026.sql
SELECT DBMS_METADATA.GET_DDL('VIEW', 'SERV_MED_V_ATENCION') FROM DUAL;
SPOOL OFF
SET PAGESIZE 300
SET LINESIZE 220
SET FEEDBACK ON
SET HEADING ON

SPOOL C:\miguel\atencion-rango-edad-salida.txt

PROMPT === 1) Columna RANGO_EDAD en SERV_MED_XLS_ATENCION (si ya existe, no hace nada) ===
DECLARE
  n NUMBER;
BEGIN
  SELECT COUNT(*) INTO n FROM USER_TAB_COLUMNS
   WHERE TABLE_NAME = 'SERV_MED_XLS_ATENCION' AND COLUMN_NAME = 'RANGO_EDAD';
  IF n = 0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE SERV_MED_XLS_ATENCION ADD (RANGO_EDAD VARCHAR2(10))';
    DBMS_OUTPUT.PUT_LINE('RANGO_EDAD agregada');
  ELSE
    DBMS_OUTPUT.PUT_LINE('RANGO_EDAD ya existia');
  END IF;
END;
/

COMMENT ON COLUMN SERV_MED_XLS_ATENCION.RANGO_EDAD IS 'Rango de edad capturado en el layout 1 (formato 2026): 18-25, 26-35, 36-45, 46-55, 55+.';

PROMPT === 2) Vista SERV_MED_V_ATENCION con RANGO_EDAD al final ===
CREATE OR REPLACE VIEW SERV_MED_V_ATENCION AS
SELECT CAST('PORTAL' AS VARCHAR2(12))                              FUENTE,
       TO_CHAR(c.REG_ID)                                           ID_ORIGEN,
       CAST(c.NSS AS VARCHAR2(13))                                 NSS,
       TRUNC(c.FECHA)                                              FECHA,
       emp.PREDIO                                                  PREDIO,
       emp.CUENTA                                                  CUENTA,
       CAST(NULL AS VARCHAR2(120))                                 AREA,
       emp.NOMBRE                                                  NOMBRE,
       emp.GENERO                                                  GENERO,
       TRUNC(MONTHS_BETWEEN(TRUNC(c.FECHA), emp.FECHA_NAC) / 12)   EDAD,
       emp.PUESTO                                                  PUESTO,
       CAST(SUBSTR(c.TIPO_CONSULTA, 1, 60) AS VARCHAR2(60 CHAR))   RAMO,
       CAST(SUBSTR(c.CAUSA, 1, 200) AS VARCHAR2(200 CHAR))         CAUSA,
       CAST(NULL AS VARCHAR2(120))                                 LESION_ME,
       CAST(SUBSTR(c.DIAGNOSTICO, 1, 1000) AS VARCHAR2(1000 CHAR)) DIAGNOSTICO,
       CAST(SUBSTR(c.TRATAMIENTO, 1, 1000) AS VARCHAR2(1000 CHAR)) TRATAMIENTO,
       CAST(SUBSTR(c.AREA_DE_ACCIDENTE, 1, 200) AS VARCHAR2(200 CHAR)) AREA_ACCIDENTE,
       CAST(SUBSTR(c.AREA_ANATOMICA_INVOLUCRADA, 1, 200) AS VARCHAR2(200 CHAR)) AREA_INVOLUCRADA,
       CASE WHEN LOWER(c.TIPO_CONSULTA) LIKE '%accidente%' OR LOWER(c.TIPO_CONSULTA) LIKE '%emergencia%'
            THEN 1 ELSE 0 END                                      ES_ACCIDENTE,
       -- RANGO_EDAD (12-nov-2026, excel-nss-18): al final para no mover las posiciones del UNION.
       -- Del portal va vacio: la grafica calcula el rango con EDAD (mismos cortes).
       CAST(NULL AS VARCHAR2(10))                                  RANGO_EDAD
  FROM TBL_SERV_CONSULTA_MEDICA c
  LEFT JOIN SERV_MED_V_EMPLEADO emp ON emp.NSS = c.NSS
UNION ALL
SELECT 'EXCEL', TO_CHAR(x.ID), x.NSS, x.FECHA, x.PREDIO, x.CUENTA, x.AREA, x.NOMBRE, x.GENERO, x.EDAD,
       x.PUESTO, x.RAMO, x.CAUSA, x.LESION_ME, x.DIAGNOSTICO, x.TRATAMIENTO,
       NULL, NULL, 0, x.RANGO_EDAD
  FROM SERV_MED_XLS_ATENCION x
 WHERE x.ESTADO_REGISTRO = 'VALIDO'
   AND NOT EXISTS (SELECT 1 FROM TBL_SERV_CONSULTA_MEDICA c
                    WHERE c.NSS = x.NSS AND TRUNC(c.FECHA) = x.FECHA);

COMMENT ON TABLE SERV_MED_V_ATENCION IS 'Atenciones: portal (TBL_SERV_CONSULTA_MEDICA) + Excel por NSS (SERV_MED_XLS_ATENCION sin las que ya estan en el portal).';

PROMPT === 3) Comprobacion: debe salir VALID y la columna RANGO_EDAD en tabla y vista ===
SELECT object_name, status FROM USER_OBJECTS WHERE object_name = 'SERV_MED_V_ATENCION';
SELECT table_name, column_name, data_type, data_length FROM USER_TAB_COLUMNS
 WHERE table_name IN ('SERV_MED_XLS_ATENCION', 'SERV_MED_V_ATENCION') AND column_name = 'RANGO_EDAD';
SELECT FUENTE, COUNT(*) N FROM SERV_MED_V_ATENCION GROUP BY FUENTE;

SPOOL OFF
PROMPT Listo. Salida en C:\miguel\atencion-rango-edad-salida.txt
