-- ============================================================================================
-- Incapacidades INTERNAS sin area ni ramo - 14-oct-2026
--
-- Base: BIOMETRICO (PDBPRD) - tabla y vista del PORTAL (SERV_MED_XLS_INCAPACIDAD, SERV_MED_V_INCAPACIDAD).
--
-- Por que: en los Excel de servicio medico las incapacidades internas van en hoja aparte ("<PREDIO> INT")
-- sin RAMO (el ramo EG/MAT/RT es del IMSS) y con DEPARTAMENTO en lugar de AREA, a veces vacio. El
-- cargador ya no las rechaza: si el RUBRO es INTERNA, AREA y RAMO son opcionales y se anota un AVISO en
-- el .errores.csv (para medirlo). Para eso AREA tiene que aceptar vacio en esta tabla.
-- La vista muestra el ramo de una interna sin ramo como 'INTERNA' (no "Sin dato" en las graficas).
--
-- ORDEN: correr ESTE script ANTES de cargar un layout 5 con internas sin area (si no, el INSERT falla
-- por AREA NOT NULL y el archivo completo no entra). Idempotente.
-- Reversa: ALTER TABLE SERV_MED_XLS_INCAPACIDAD MODIFY (AREA NOT NULL) -- solo si no hay renglones sin
-- area -- y la version anterior de excel-nss-05-vistas.sql.
--
-- Correr con:  @C:\miguel\portalsalud-personal\docs\script-prod\excel-nss-14-incap-interna.sql
-- ============================================================================================
SET SERVEROUTPUT ON
SET DEFINE OFF
SPOOL C:\miguel\incap-interna-salida.txt

PROMPT === 1) AREA opcional en SERV_MED_XLS_INCAPACIDAD (si ya acepta vacio, no hace nada) ===
DECLARE
  v_nullable VARCHAR2(1);
BEGIN
  SELECT NULLABLE INTO v_nullable FROM USER_TAB_COLUMNS
   WHERE TABLE_NAME = 'SERV_MED_XLS_INCAPACIDAD' AND COLUMN_NAME = 'AREA';
  IF v_nullable = 'N' THEN
    EXECUTE IMMEDIATE 'ALTER TABLE SERV_MED_XLS_INCAPACIDAD MODIFY (AREA NULL)';
    DBMS_OUTPUT.PUT_LINE('AREA ahora acepta vacio');
  ELSE
    DBMS_OUTPUT.PUT_LINE('AREA ya aceptaba vacio');
  END IF;
END;
/

PROMPT === 2) Vista SERV_MED_V_INCAPACIDAD (misma definicion de excel-nss-05-vistas.sql) ===
CREATE OR REPLACE VIEW SERV_MED_V_INCAPACIDAD AS
SELECT CAST('PORTAL' AS VARCHAR2(12))                              FUENTE,
       TO_CHAR(i.REG_ID)                                           ID_ORIGEN,
       CAST(i.NSS AS VARCHAR2(13))                                 NSS,
       COALESCE(CASE WHEN REGEXP_LIKE(i.FECHA_INICIO, '^[0-9]{4}-[0-9]{2}-[0-9]{2}')
                     THEN TO_DATE(SUBSTR(i.FECHA_INICIO, 1, 10), 'YYYY-MM-DD')
                     WHEN REGEXP_LIKE(i.FECHA_INICIO, '^[0-9]{2}/[0-9]{2}/[0-9]{4}')
                     THEN TO_DATE(SUBSTR(i.FECHA_INICIO, 1, 10), 'DD/MM/YYYY') END,
                TRUNC(i.FECHA_REGISTRO))                           FECHA,
       emp.PREDIO                                                  PREDIO,
       emp.CUENTA                                                  CUENTA,
       CAST(NULL AS VARCHAR2(120))                                 AREA,
       emp.NOMBRE                                                  NOMBRE,
       emp.GENERO                                                  GENERO,
       TRUNC(MONTHS_BETWEEN(TRUNC(i.FECHA_REGISTRO), emp.FECHA_NAC) / 12) EDAD,
       emp.PUESTO                                                  PUESTO,
       CAST(SUBSTR(UPPER(REPLACE(i.FOLIO_INCAPACIDAD, ' ')), 1, 20) AS VARCHAR2(20)) FOLIO,
       CASE WHEN UPPER(i.RUBRO) LIKE '%INTERN%' THEN 'INTERNA'
            WHEN UPPER(i.RUBRO) LIKE '%IMSS%'   THEN 'IMSS' END    RUBRO,
       CASE WHEN UPPER(TRIM(i.RAMO)) IN ('EG', 'ENFERMEDAD GENERAL', 'ENFERMEDAD_GENERAL') THEN 'EG'
            WHEN UPPER(TRIM(i.RAMO)) IN ('MAT', 'MATERNIDAD') THEN 'MAT'
            WHEN UPPER(TRIM(i.RAMO)) IN ('RT1', 'RT2') THEN UPPER(TRIM(i.RAMO))
            ELSE CAST(SUBSTR(i.RAMO, 1, 40) AS VARCHAR2(40)) END   RAMO,
       CAST(SUBSTR(i.TIPO_INCAPACIDAD, 1, 20) AS VARCHAR2(20))     TIPO,
       CASE WHEN REGEXP_LIKE(i.FECHA_TERMINO, '^[0-9]{4}-[0-9]{2}-[0-9]{2}')
            THEN TO_DATE(SUBSTR(i.FECHA_TERMINO, 1, 10), 'YYYY-MM-DD')
            WHEN REGEXP_LIKE(i.FECHA_TERMINO, '^[0-9]{2}/[0-9]{2}/[0-9]{4}')
            THEN TO_DATE(SUBSTR(i.FECHA_TERMINO, 1, 10), 'DD/MM/YYYY') END FECHA_TERMINO,
       CASE WHEN REGEXP_LIKE(TRIM(TO_CHAR(i.DIAS_AUTORIZADOS)), '^[0-9]+$')
            THEN TO_NUMBER(TRIM(TO_CHAR(i.DIAS_AUTORIZADOS))) END   DIAS,
       CASE WHEN REGEXP_LIKE(REPLACE(TRIM(TO_CHAR(i.SALARIO_INTEGRADO)), ','), '^[0-9]*\.?[0-9]+$')
            THEN TO_NUMBER(REPLACE(TRIM(TO_CHAR(i.SALARIO_INTEGRADO)), ',')) END SDI,
       CASE WHEN REGEXP_LIKE(REPLACE(TRIM(TO_CHAR(i.COSTO)), ','), '^[0-9]*\.?[0-9]+$')
            THEN TO_NUMBER(REPLACE(TRIM(TO_CHAR(i.COSTO)), ',')) END COSTO,
       CAST(SUBSTR(i.ESTADO_DICTAMEN, 1, 60) AS VARCHAR2(60))      ESTADO_DICTAMEN,
       CAST(NULL AS VARCHAR2(1000))                                DIAGNOSTICO
  FROM TBL_SERV_INCAPACIDAD_MEDICA i
  LEFT JOIN SERV_MED_V_EMPLEADO emp ON emp.NSS = i.NSS
UNION ALL
SELECT 'EXCEL', TO_CHAR(x.ID), x.NSS, x.FECHA, x.PREDIO, x.CUENTA, x.AREA, x.NOMBRE, x.GENERO, x.EDAD,
       x.PUESTO, x.FOLIO, x.RUBRO,
       -- Interna sin ramo (14-oct-2026, excel-nss-14): el ramo es del IMSS y una interna no lo tiene;
       -- se muestra como INTERNA para que en las graficas por ramo no caiga en "Sin dato"
       NVL(x.RAMO, CASE WHEN x.RUBRO = 'INTERNA' THEN 'INTERNA' END),
       x.TIPO, x.FECHA_TERMINO, x.DIAS, x.SDI, x.COSTO_TOTAL,
       NULL, x.DIAGNOSTICO
  FROM SERV_MED_XLS_INCAPACIDAD x
 WHERE x.ESTADO_REGISTRO = 'VALIDO'
   AND NOT EXISTS (SELECT 1 FROM TBL_SERV_INCAPACIDAD_MEDICA p
                    WHERE (x.FOLIO IS NOT NULL
                           AND UPPER(REPLACE(p.FOLIO_INCAPACIDAD, ' ')) = UPPER(REPLACE(x.FOLIO, ' ')))
                       OR (x.FOLIO IS NULL AND p.NSS = x.NSS AND
                           CASE WHEN REGEXP_LIKE(p.FECHA_INICIO, '^[0-9]{4}-[0-9]{2}-[0-9]{2}')
                                THEN TO_DATE(SUBSTR(p.FECHA_INICIO, 1, 10), 'YYYY-MM-DD')
                                WHEN REGEXP_LIKE(p.FECHA_INICIO, '^[0-9]{2}/[0-9]{2}/[0-9]{4}')
                                THEN TO_DATE(SUBSTR(p.FECHA_INICIO, 1, 10), 'DD/MM/YYYY') END = x.FECHA));

COMMENT ON TABLE SERV_MED_V_INCAPACIDAD IS 'Incapacidades: portal (TBL_SERV_INCAPACIDAD_MEDICA) + Excel por NSS (SERV_MED_XLS_INCAPACIDAD sin las que ya estan en el portal). FECHA = inicio.';

PROMPT === 3) Verificacion ===
SELECT OBJECT_NAME, STATUS FROM USER_OBJECTS WHERE OBJECT_NAME = 'SERV_MED_V_INCAPACIDAD';
SELECT COLUMN_NAME, NULLABLE FROM USER_TAB_COLUMNS
 WHERE TABLE_NAME = 'SERV_MED_XLS_INCAPACIDAD' AND COLUMN_NAME IN ('AREA', 'RAMO');
SELECT FUENTE, RUBRO, RAMO, COUNT(*) total FROM SERV_MED_V_INCAPACIDAD
 WHERE FUENTE = 'EXCEL' GROUP BY FUENTE, RUBRO, RAMO ORDER BY 1, 2, 3;

SPOOL OFF
