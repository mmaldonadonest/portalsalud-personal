-- ============================================================================================
-- Accidentes: DIAS en el alta del portal (13-oct-2026)
--
-- Base: BIOMETRICO (PDBPRD) - tabla del WS ORDS SERV_MED_ACCIDENTE + vista del portal.
--
-- Por que: el alta de accidente del portal no captura los dias (el layout 6 del Excel si:
-- SERV_MED_XLS_ACCIDENTE.DIAS). Sin dias no se puede cuadrar el costo (pago 60% x dias).
--
-- ORDEN (no cambiarlo):
--   1. Este script (columna + vista). El WS actual Servcio/accidente sigue funcionando igual:
--      su INSERT nombra sus columnas y DIAS queda NULL.
--   2. Publicar los clones del WS: docs/ords-accidentes-v2.sql (accidente_v2 y
--      consulta_accidente_v2). Los WS productivos NO se tocan.
--   3. Solo despues de 1 y 2, desplegar el WAR nuevo (apunta a los _v2).
--
-- Reversa: ALTER TABLE SERV_MED_ACCIDENTE DROP COLUMN DIAS; y volver a correr la version
-- anterior de excel-nss-05-vistas.sql.
--
-- Correr con:  @C:\miguel\portalsalud-personal\docs\script-prod\accidente-13-dias.sql
-- ============================================================================================
SET SERVEROUTPUT ON
SPOOL C:\miguel\accidente-dias-salida.txt

PROMPT === 1) Columna DIAS en SERV_MED_ACCIDENTE (si ya existe, no hace nada) ===
DECLARE
  n NUMBER;
BEGIN
  SELECT COUNT(*) INTO n FROM USER_TAB_COLUMNS
   WHERE TABLE_NAME = 'SERV_MED_ACCIDENTE' AND COLUMN_NAME = 'DIAS';
  IF n = 0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE SERV_MED_ACCIDENTE ADD (DIAS NUMBER(5))';
    DBMS_OUTPUT.PUT_LINE('DIAS agregada');
  ELSE
    DBMS_OUTPUT.PUT_LINE('DIAS ya existia');
  END IF;
END;
/

COMMENT ON COLUMN SERV_MED_ACCIDENTE.DIAS IS 'Dias de incapacidad del accidente (captura del portal, igual que DIAS del layout 6).';

PROMPT === 2) Vista SERV_MED_V_ACCIDENTE con DIAS (misma definicion de excel-nss-05-vistas.sql) ===
CREATE OR REPLACE VIEW SERV_MED_V_ACCIDENTE AS
SELECT CAST('PORTAL' AS VARCHAR2(12))                              FUENTE,
       TO_CHAR(a.REG_ID)                                           ID_ORIGEN,
       a.NSS                                                       NSS,
       TRUNC(NVL(a.FECHA_ACCIDENTE, a.FECHA_REGISTRO))             FECHA,
       emp.PREDIO                                                  PREDIO,
       emp.CUENTA                                                  CUENTA,
       CAST(NULL AS VARCHAR2(120))                                 AREA,
       emp.NOMBRE                                                  NOMBRE,
       emp.GENERO                                                  GENERO,
       TRUNC(MONTHS_BETWEEN(TRUNC(NVL(a.FECHA_ACCIDENTE, a.FECHA_REGISTRO)), emp.FECHA_NAC) / 12) EDAD,
       emp.PUESTO                                                  PUESTO,
       a.TIPO_RIESGO                                               TIPO_RIESGO,
       CAST(CASE UPPER(TRIM(a.CAUSA_RT))
                 WHEN 'ACCID_VIAL'        THEN 'ACCID VIAL'
                 WHEN 'AGRESION_ASALTO'   THEN 'AGRESION / ASALTO'
                 WHEN 'CAIDA'             THEN 'CAIDA'
                 WHEN 'GOLPE_DIRECTO'     THEN 'GOLPE DIRECTO CON O POR OBJETO'
                 WHEN 'HERIDA'            THEN 'HERIDA'
                 WHEN 'LESION_ERGONOMICA' THEN 'LESION ERGONOMICA'
                 WHEN 'OTRO_IMPROCEDENTE' THEN 'OTRO (IMPROCEDENTE)'
                 ELSE a.CAUSA_RT END AS VARCHAR2(60))              CAUSA_RT,
       CAST(a.DIAGNOSTICO AS VARCHAR2(1000))                       DX_IMSS,
       a.SDI                                                       SDI,
       CAST(REPLACE(a.STATUS_CALIFICACION, '_', ' ') AS VARCHAR2(30)) STATUS_CALIFICACION,
       CAST(NULL AS VARCHAR2(30))                                  STATUS,
       a.COSTO                                                     COSTO,
       CAST(a.OBSERVACIONES AS VARCHAR2(1000))                     OBSERVACIONES,
       a.DIAS                                                      DIAS
  FROM SERV_MED_ACCIDENTE a
  LEFT JOIN SERV_MED_V_EMPLEADO emp ON emp.NSS = a.NSS
UNION ALL
SELECT 'EXCEL', TO_CHAR(x.ID), x.NSS, x.FECHA, x.PREDIO, x.CUENTA, x.AREA, x.NOMBRE, x.GENERO, x.EDAD,
       x.PUESTO, x.TIPO_RIESGO, x.CAUSA_RT, x.DX_IMSS, x.SDI, x.STATUS_CALIFICACION, x.STATUS, x.COSTO,
       x.OBSERVACIONES, x.DIAS
  FROM SERV_MED_XLS_ACCIDENTE x
 WHERE x.ESTADO_REGISTRO = 'VALIDO'
   AND NOT EXISTS (SELECT 1 FROM SERV_MED_ACCIDENTE a
                    WHERE a.NSS = x.NSS AND TRUNC(NVL(a.FECHA_ACCIDENTE, a.FECHA_REGISTRO)) = x.FECHA);

COMMENT ON TABLE SERV_MED_V_ACCIDENTE IS 'Accidentes: portal (SERV_MED_ACCIDENTE) + Excel por NSS (SERV_MED_XLS_ACCIDENTE sin los que ya estan en el portal).';

PROMPT === 3) Verificacion: debe salir VALID y la columna DIAS ===
SELECT OBJECT_NAME, STATUS FROM USER_OBJECTS WHERE OBJECT_NAME = 'SERV_MED_V_ACCIDENTE';
SELECT COLUMN_NAME, DATA_TYPE FROM USER_TAB_COLUMNS
 WHERE TABLE_NAME IN ('SERV_MED_ACCIDENTE', 'SERV_MED_V_ACCIDENTE') AND COLUMN_NAME = 'DIAS';
SELECT FUENTE, COUNT(*) total, COUNT(DIAS) con_dias FROM SERV_MED_V_ACCIDENTE GROUP BY FUENTE;

SPOOL OFF
