-- ============================================================================================
-- Rango de edad en el layout 2 (EXAMEN NUEVO INGRESO) - 14-oct-2026
--
-- Base: BIOMETRICO (PDBPRD) - tablas y vistas del PORTAL (SERV_MED_XLS_EXAMEN_ING, SERV_MED_V_EXAMEN).
--
-- Por que: los Excel del servicio medico solo marcan el RANGO de edad (18-25 ... 55+), no la edad.
-- El layout 2 cambia la columna EDAD por RANGO DE EDAD y el cargador la escribe en RANGO_EDAD.
-- La columna EDAD se queda (vacia) para no borrar nada.
-- La vista agrega RANGO_EDAD al final: Excel tal cual; donde solo hay edad, se calcula con los
-- mismos cortes; carga vieja = el rango que ya traia la bitacora.
--
-- ORDEN: correr ESTE script ANTES de cargar un layout 2 con el cargador nuevo (si no, el INSERT
-- falla porque la columna no existe). Idempotente.
--
-- Reversa: ALTER TABLE SERV_MED_XLS_EXAMEN_ING DROP COLUMN RANGO_EDAD; y la version anterior
-- de excel-nss-05-vistas.sql.
--
-- Correr con:  @C:\miguel\portalsalud-personal\docs\script-prod\excel-nss-13-rango-edad.sql
-- ============================================================================================
SET SERVEROUTPUT ON
SET DEFINE OFF
SPOOL C:\miguel\rango-edad-salida.txt

PROMPT === 1) Columna RANGO_EDAD en SERV_MED_XLS_EXAMEN_ING (si ya existe, no hace nada) ===
DECLARE
  n NUMBER;
BEGIN
  SELECT COUNT(*) INTO n FROM USER_TAB_COLUMNS
   WHERE TABLE_NAME = 'SERV_MED_XLS_EXAMEN_ING' AND COLUMN_NAME = 'RANGO_EDAD';
  IF n = 0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE SERV_MED_XLS_EXAMEN_ING ADD (RANGO_EDAD VARCHAR2(10))';
    DBMS_OUTPUT.PUT_LINE('RANGO_EDAD agregada');
  ELSE
    DBMS_OUTPUT.PUT_LINE('RANGO_EDAD ya existia');
  END IF;
END;
/

COMMENT ON COLUMN SERV_MED_XLS_EXAMEN_ING.RANGO_EDAD IS 'Rango de edad capturado en el layout 2: 18-25, 26-35, 36-45, 46-55, 55+.';

PROMPT === 2) Vista SERV_MED_V_EXAMEN con RANGO_EDAD (misma definicion de excel-nss-05-vistas.sql) ===
CREATE OR REPLACE VIEW SERV_MED_V_EXAMEN AS
SELECT CAST('PORTAL' AS VARCHAR2(12))                              FUENTE,
       TO_CHAR(h.REG_ID)                                           ID_ORIGEN,
       h.NSS                                                       NSS,
       TRUNC(h.FECHA_REGISTRO)                                     FECHA,
       emp.PREDIO                                                  PREDIO,
       emp.CUENTA                                                  CUENTA,
       CAST(NULL AS VARCHAR2(120))                                 AREA,
       emp.NOMBRE                                                  NOMBRE,
       emp.GENERO                                                  GENERO,
       TRUNC(MONTHS_BETWEEN(TRUNC(h.FECHA_REGISTRO), emp.FECHA_NAC) / 12) EDAD,
       emp.PUESTO                                                  PUESTO,
       CAST(NULL AS VARCHAR2(20))                                  TIPO,
       CAST(NULL AS VARCHAR2(20))                                  TIPO_EXAMEN,
       CASE WHEN TRIM(h.NO_APTO) IS NOT NULL
                 AND UPPER(TRIM(h.NO_APTO)) NOT IN ('0','N','NO','FALSE') THEN 'NO_APTO'
            WHEN TRIM(h.APTO_RESTRINGIDO) IS NOT NULL
                 AND UPPER(TRIM(h.APTO_RESTRINGIDO)) NOT IN ('0','N','NO','FALSE') THEN 'APTO_RESTRINGIDO'
            WHEN TRIM(h.APTO_CONDICIONADO) IS NOT NULL
                 AND UPPER(TRIM(h.APTO_CONDICIONADO)) NOT IN ('0','N','NO','FALSE') THEN 'APTO_CONDICIONADO'
            WHEN TRIM(h.APTO) IS NOT NULL
                 AND UPPER(TRIM(h.APTO)) NOT IN ('0','N','NO','FALSE') THEN 'APTO' END DICTAMEN,
       CAST(NULL AS VARCHAR2(1000))                                DIAGNOSTICO,
       -- RANGO_EDAD (14-oct-2026, excel-nss-13): al final para no mover las posiciones del UNION
       CAST(CASE WHEN TRUNC(MONTHS_BETWEEN(TRUNC(h.FECHA_REGISTRO), emp.FECHA_NAC) / 12) IS NULL OR TRUNC(MONTHS_BETWEEN(TRUNC(h.FECHA_REGISTRO), emp.FECHA_NAC) / 12) <= 0 THEN NULL WHEN TRUNC(MONTHS_BETWEEN(TRUNC(h.FECHA_REGISTRO), emp.FECHA_NAC) / 12) <= 25 THEN '18-25' WHEN TRUNC(MONTHS_BETWEEN(TRUNC(h.FECHA_REGISTRO), emp.FECHA_NAC) / 12) <= 35 THEN '26-35' WHEN TRUNC(MONTHS_BETWEEN(TRUNC(h.FECHA_REGISTRO), emp.FECHA_NAC) / 12) <= 45 THEN '36-45' WHEN TRUNC(MONTHS_BETWEEN(TRUNC(h.FECHA_REGISTRO), emp.FECHA_NAC) / 12) <= 55 THEN '46-55' ELSE '55+' END AS VARCHAR2(10)) RANGO_EDAD
  FROM SERV_MED_RESULTADO_EXAMEN_HIST h
  LEFT JOIN SERV_MED_V_EMPLEADO emp ON emp.NSS = h.NSS
UNION ALL
SELECT 'EXCEL', TO_CHAR(x.ID), x.NSS, x.FECHA, x.PREDIO, x.CUENTA, x.AREA,
       TRIM(REGEXP_REPLACE(x.NOMBRE || ' ' || x.APELLIDO_PATERNO || ' ' || x.APELLIDO_MATERNO, ' +', ' ')),
       x.GENERO, x.EDAD, x.PUESTO_SOLICITADO, 'NUEVO_INGRESO', x.TIPO_EXAMEN, x.DICTAMEN, x.DIAGNOSTICO,
       NVL(x.RANGO_EDAD, CASE WHEN x.EDAD IS NULL OR x.EDAD <= 0 THEN NULL WHEN x.EDAD <= 25 THEN '18-25' WHEN x.EDAD <= 35 THEN '26-35' WHEN x.EDAD <= 45 THEN '36-45' WHEN x.EDAD <= 55 THEN '46-55' ELSE '55+' END)
  FROM SERV_MED_XLS_EXAMEN_ING x
 WHERE x.ESTADO_REGISTRO = 'VALIDO'
   AND NOT EXISTS (SELECT 1 FROM SERV_MED_RESULTADO_EXAMEN_HIST h
                    WHERE h.NSS = x.NSS AND TRUNC(h.FECHA_REGISTRO) BETWEEN x.FECHA - 1 AND x.FECHA + 1)
UNION ALL
SELECT 'EXCEL', TO_CHAR(x.ID), x.NSS, x.FECHA, x.PREDIO, x.CUENTA, x.AREA, x.NOMBRE, x.GENERO, x.EDAD,
       x.PUESTO, 'PERIODICO', NULL, x.DICTAMEN, x.DIAGNOSTICO,
       CASE WHEN x.EDAD IS NULL OR x.EDAD <= 0 THEN NULL WHEN x.EDAD <= 25 THEN '18-25' WHEN x.EDAD <= 35 THEN '26-35' WHEN x.EDAD <= 45 THEN '36-45' WHEN x.EDAD <= 55 THEN '46-55' ELSE '55+' END
  FROM SERV_MED_XLS_EXAMEN_PER x
 WHERE x.ESTADO_REGISTRO = 'VALIDO'
   AND NOT EXISTS (SELECT 1 FROM SERV_MED_RESULTADO_EXAMEN_HIST h
                    WHERE h.NSS = x.NSS AND TRUNC(h.FECHA_REGISTRO) BETWEEN x.FECHA - 1 AND x.FECHA + 1)
UNION ALL
-- v4: pos incapacidad del layout nuevo por NSS (SERV_MED_XLS_EXAMEN_POS, excel-nss-06)
SELECT 'EXCEL', TO_CHAR(x.ID), x.NSS, x.FECHA, x.PREDIO, x.CUENTA, x.AREA, x.NOMBRE, x.GENERO, x.EDAD,
       x.PUESTO, 'POS_INCAPACIDAD', NULL, x.DICTAMEN, x.DIAGNOSTICO,
       CASE WHEN x.EDAD IS NULL OR x.EDAD <= 0 THEN NULL WHEN x.EDAD <= 25 THEN '18-25' WHEN x.EDAD <= 35 THEN '26-35' WHEN x.EDAD <= 45 THEN '36-45' WHEN x.EDAD <= 55 THEN '46-55' ELSE '55+' END
  FROM SERV_MED_XLS_EXAMEN_POS x
 WHERE x.ESTADO_REGISTRO = 'VALIDO'
   AND NOT EXISTS (SELECT 1 FROM SERV_MED_RESULTADO_EXAMEN_HIST h
                    WHERE h.NSS = x.NSS AND TRUNC(h.FECHA_REGISTRO) BETWEEN x.FECHA - 1 AND x.FECHA + 1)
UNION ALL
-- Carga vieja: el dictamen viene como texto del Excel ("APTO (15)", "NO APTO", ...). El orden de
-- los WHEN importa: "NO APTO" y "APTO CONDICIONADO" tambien contienen "APTO".
-- v4: solo MIENTRAS el layout nuevo de pos incapacidad este vacio. En cuanto se carga el primer
-- renglon valido, la carga vieja deja de leerse sola (si no, se contaria dos veces).
SELECT 'CARGA_VIEJA', TO_CHAR(ev.ID), NULL, ev.FECHA, ev.PREDIO, ev.CUENTA, ev.AREA,
       ev.NOMBRE, ev.GENERO, NULL, ev.PUESTO, 'POS_INCAPACIDAD', NULL,
       CASE WHEN UPPER(at.VALOR) LIKE '%NO APTO%'   OR at.VALOR LIKE '%(30)%' THEN 'NO_APTO'
            WHEN UPPER(at.VALOR) LIKE '%RESTRING%'  OR at.VALOR LIKE '%(60)%' THEN 'APTO_RESTRINGIDO'
            WHEN UPPER(at.VALOR) LIKE '%CONDICION%' OR at.VALOR LIKE '%(45)%' THEN 'APTO_CONDICIONADO'
            WHEN UPPER(at.VALOR) LIKE '%APTO%'      OR at.VALOR LIKE '%(15)%' THEN 'APTO' END,
       NULL,
       ev.RANGO_EDAD
  FROM SERV_MED_BITACORA_EVENTO ev
  LEFT JOIN SERV_MED_BITACORA_ATRIBUTO at ON at.EVENTO_ID = ev.ID AND at.NOMBRE = 'DICTAMEN'
 WHERE ev.FAMILIA = 'EXAMEN_POS_INCAP'
   AND NOT EXISTS (SELECT 1 FROM SERV_MED_XLS_EXAMEN_POS n WHERE n.ESTADO_REGISTRO = 'VALIDO');

COMMENT ON TABLE SERV_MED_V_EXAMEN IS 'Examenes: portal (SERV_MED_RESULTADO_EXAMEN_HIST) + nuevo ingreso y periodico del Excel por NSS + pos incapacidad de la carga vieja.';

PROMPT === 3) Verificacion: VALID y rangos por fuente ===
SELECT OBJECT_NAME, STATUS FROM USER_OBJECTS WHERE OBJECT_NAME = 'SERV_MED_V_EXAMEN';
SELECT FUENTE, TIPO, COUNT(*) total, COUNT(RANGO_EDAD) con_rango
  FROM SERV_MED_V_EXAMEN GROUP BY FUENTE, TIPO ORDER BY 1, 2;

SPOOL OFF
