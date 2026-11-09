-- ============================================================================================
-- Examenes del portal: solo cuentan los que tienen DICTAMEN - 30-oct-2026
--
-- Base: BIOMETRICO (PDBPRD) - vista del PORTAL. No toca datos. Lo corre el usuario.
--
-- Por que: respuesta de Servicio Medico (registros-sospechosos-prueba-v2.xlsx):
--   P10 "¿Un examen debe contar en las graficas solo si tiene dictamen?" -> ASI ES.
--   P11 "Un examen sin dictamen es..." -> un examen no terminado (pendiente).
-- De 9 examenes capturados en el portal ago-oct 2026, 8 no tenian dictamen y contaban en el Home.
--
-- Cambio sobre excel-nss-16 (uno por persona por dia, ultimo guardado):
--   1. Rama PORTAL: el ultimo guardado del dia cuenta SOLO si trae dictamen (APTO, APTO
--      CONDICIONADO, APTO RESTRINGIDO o NO APTO). Sin dictamen = pendiente, no sale en la vista.
--   2. Ramas del Excel: "manda el portal" solo frente a un examen del portal CON dictamen. Antes,
--      un borrador del portal sin dictamen escondia el examen terminado del Excel de esa fecha.
-- La tabla SERV_MED_RESULTADO_EXAMEN_HIST no se toca: los pendientes siguen ahi y en el expediente.
-- Los examenes del Excel sin STATUS no cambian (la pregunta fue sobre el portal).
-- Afecta a todo lo que lee la vista: Home, Analisis de examenes y el conteo "Portal N".
--
-- Reversa: correr excel-nss-16-examen-uno-por-dia.sql.
-- Correr con:  @C:\miguel\portalsalud-personal\docs\script-prod\excel-nss-17-examen-solo-con-dictamen.sql
-- ============================================================================================
SET DEFINE OFF
SPOOL C:\miguel\examen-solo-con-dictamen-salida.txt

PROMPT === ANTES: examenes del portal por dictamen (los SIN DICTAMEN dejan de contar) ===
SELECT NVL(DICTAMEN, 'SIN DICTAMEN (pendiente)') DICTAMEN, COUNT(*) EXAMENES
  FROM SERV_MED_V_EXAMEN WHERE FUENTE = 'PORTAL'
 GROUP BY DICTAMEN ORDER BY 2 DESC;

PROMPT === ANTES: examenes del portal en la vista ===
SELECT COUNT(*) EXAMENES_PORTAL_ANTES FROM SERV_MED_V_EXAMEN WHERE FUENTE = 'PORTAL';

PROMPT === SERV_MED_V_EXAMEN ===
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
       CAST(CASE WHEN TRUNC(MONTHS_BETWEEN(TRUNC(h.FECHA_REGISTRO), emp.FECHA_NAC) / 12) IS NULL OR TRUNC(MONTHS_BETWEEN(TRUNC(h.FECHA_REGISTRO), emp.FECHA_NAC) / 12) <= 0 THEN NULL WHEN TRUNC(MONTHS_BETWEEN(TRUNC(h.FECHA_REGISTRO), emp.FECHA_NAC) / 12) <= 25 THEN '18-25' WHEN TRUNC(MONTHS_BETWEEN(TRUNC(h.FECHA_REGISTRO), emp.FECHA_NAC) / 12) <= 35 THEN '26-35' WHEN TRUNC(MONTHS_BETWEEN(TRUNC(h.FECHA_REGISTRO), emp.FECHA_NAC) / 12) <= 45 THEN '36-45' WHEN TRUNC(MONTHS_BETWEEN(TRUNC(h.FECHA_REGISTRO), emp.FECHA_NAC) / 12) <= 55 THEN '46-55' ELSE '55+' END AS VARCHAR2(10)) RANGO_EDAD
  -- 30-oct-2026: solo el ultimo guardado de cada NSS por dia (el WS deja un renglon por guardado)
  FROM (SELECT r.*,
               ROW_NUMBER() OVER (PARTITION BY r.NSS, TRUNC(r.FECHA_REGISTRO)
                                  ORDER BY r.FECHA_REGISTRO DESC, r.REG_ID DESC) RN
          FROM SERV_MED_RESULTADO_EXAMEN_HIST r) h
  LEFT JOIN SERV_MED_V_EMPLEADO emp ON emp.NSS = h.NSS
 WHERE h.RN = 1
   -- 30-oct-2026 (P10 de Servicio Medico): sin dictamen = examen no terminado, no cuenta
   AND (   (TRIM(h.NO_APTO) IS NOT NULL AND UPPER(TRIM(h.NO_APTO)) NOT IN ('0','N','NO','FALSE'))
        OR (TRIM(h.APTO_RESTRINGIDO) IS NOT NULL AND UPPER(TRIM(h.APTO_RESTRINGIDO)) NOT IN ('0','N','NO','FALSE'))
        OR (TRIM(h.APTO_CONDICIONADO) IS NOT NULL AND UPPER(TRIM(h.APTO_CONDICIONADO)) NOT IN ('0','N','NO','FALSE'))
        OR (TRIM(h.APTO) IS NOT NULL AND UPPER(TRIM(h.APTO)) NOT IN ('0','N','NO','FALSE')))
UNION ALL
SELECT 'EXCEL', TO_CHAR(x.ID), x.NSS, x.FECHA, x.PREDIO, x.CUENTA, x.AREA,
       TRIM(REGEXP_REPLACE(x.NOMBRE || ' ' || x.APELLIDO_PATERNO || ' ' || x.APELLIDO_MATERNO, ' +', ' ')),
       x.GENERO, x.EDAD, x.PUESTO_SOLICITADO, 'NUEVO_INGRESO', x.TIPO_EXAMEN, x.DICTAMEN, x.DIAGNOSTICO,
       NVL(x.RANGO_EDAD, CASE WHEN x.EDAD IS NULL OR x.EDAD <= 0 THEN NULL WHEN x.EDAD <= 25 THEN '18-25' WHEN x.EDAD <= 35 THEN '26-35' WHEN x.EDAD <= 45 THEN '36-45' WHEN x.EDAD <= 55 THEN '46-55' ELSE '55+' END)
  FROM SERV_MED_XLS_EXAMEN_ING x
 WHERE x.ESTADO_REGISTRO = 'VALIDO'
   AND NOT EXISTS (SELECT 1 FROM SERV_MED_RESULTADO_EXAMEN_HIST h
                    WHERE h.NSS = x.NSS AND TRUNC(h.FECHA_REGISTRO) BETWEEN x.FECHA - 1 AND x.FECHA + 1
                      -- solo un examen del portal CON dictamen le gana al del Excel (30-oct-2026)
                      AND (   (TRIM(h.NO_APTO) IS NOT NULL AND UPPER(TRIM(h.NO_APTO)) NOT IN ('0','N','NO','FALSE'))
                           OR (TRIM(h.APTO_RESTRINGIDO) IS NOT NULL AND UPPER(TRIM(h.APTO_RESTRINGIDO)) NOT IN ('0','N','NO','FALSE'))
                           OR (TRIM(h.APTO_CONDICIONADO) IS NOT NULL AND UPPER(TRIM(h.APTO_CONDICIONADO)) NOT IN ('0','N','NO','FALSE'))
                           OR (TRIM(h.APTO) IS NOT NULL AND UPPER(TRIM(h.APTO)) NOT IN ('0','N','NO','FALSE'))))
UNION ALL
SELECT 'EXCEL', TO_CHAR(x.ID), x.NSS, x.FECHA, x.PREDIO, x.CUENTA, x.AREA, x.NOMBRE, x.GENERO, x.EDAD,
       x.PUESTO, 'PERIODICO', NULL, x.DICTAMEN, x.DIAGNOSTICO,
       CASE WHEN x.EDAD IS NULL OR x.EDAD <= 0 THEN NULL WHEN x.EDAD <= 25 THEN '18-25' WHEN x.EDAD <= 35 THEN '26-35' WHEN x.EDAD <= 45 THEN '36-45' WHEN x.EDAD <= 55 THEN '46-55' ELSE '55+' END
  FROM SERV_MED_XLS_EXAMEN_PER x
 WHERE x.ESTADO_REGISTRO = 'VALIDO'
   AND NOT EXISTS (SELECT 1 FROM SERV_MED_RESULTADO_EXAMEN_HIST h
                    WHERE h.NSS = x.NSS AND TRUNC(h.FECHA_REGISTRO) BETWEEN x.FECHA - 1 AND x.FECHA + 1
                      -- solo un examen del portal CON dictamen le gana al del Excel (30-oct-2026)
                      AND (   (TRIM(h.NO_APTO) IS NOT NULL AND UPPER(TRIM(h.NO_APTO)) NOT IN ('0','N','NO','FALSE'))
                           OR (TRIM(h.APTO_RESTRINGIDO) IS NOT NULL AND UPPER(TRIM(h.APTO_RESTRINGIDO)) NOT IN ('0','N','NO','FALSE'))
                           OR (TRIM(h.APTO_CONDICIONADO) IS NOT NULL AND UPPER(TRIM(h.APTO_CONDICIONADO)) NOT IN ('0','N','NO','FALSE'))
                           OR (TRIM(h.APTO) IS NOT NULL AND UPPER(TRIM(h.APTO)) NOT IN ('0','N','NO','FALSE'))))
UNION ALL
SELECT 'EXCEL', TO_CHAR(x.ID), x.NSS, x.FECHA, x.PREDIO, x.CUENTA, x.AREA, x.NOMBRE, x.GENERO, x.EDAD,
       x.PUESTO, 'POS_INCAPACIDAD', NULL, x.DICTAMEN, x.DIAGNOSTICO,
       CASE WHEN x.EDAD IS NULL OR x.EDAD <= 0 THEN NULL WHEN x.EDAD <= 25 THEN '18-25' WHEN x.EDAD <= 35 THEN '26-35' WHEN x.EDAD <= 45 THEN '36-45' WHEN x.EDAD <= 55 THEN '46-55' ELSE '55+' END
  FROM SERV_MED_XLS_EXAMEN_POS x
 WHERE x.ESTADO_REGISTRO = 'VALIDO'
   AND NOT EXISTS (SELECT 1 FROM SERV_MED_RESULTADO_EXAMEN_HIST h
                    WHERE h.NSS = x.NSS AND TRUNC(h.FECHA_REGISTRO) BETWEEN x.FECHA - 1 AND x.FECHA + 1
                      -- solo un examen del portal CON dictamen le gana al del Excel (30-oct-2026)
                      AND (   (TRIM(h.NO_APTO) IS NOT NULL AND UPPER(TRIM(h.NO_APTO)) NOT IN ('0','N','NO','FALSE'))
                           OR (TRIM(h.APTO_RESTRINGIDO) IS NOT NULL AND UPPER(TRIM(h.APTO_RESTRINGIDO)) NOT IN ('0','N','NO','FALSE'))
                           OR (TRIM(h.APTO_CONDICIONADO) IS NOT NULL AND UPPER(TRIM(h.APTO_CONDICIONADO)) NOT IN ('0','N','NO','FALSE'))
                           OR (TRIM(h.APTO) IS NOT NULL AND UPPER(TRIM(h.APTO)) NOT IN ('0','N','NO','FALSE'))));

COMMENT ON TABLE SERV_MED_V_EXAMEN IS 'Examenes: portal (ultimo guardado por NSS y dia CON dictamen, SERV_MED_RESULTADO_EXAMEN_HIST, 30-oct-2026) + nuevo ingreso, periodico y pos incapacidad del Excel por NSS. Sin carga vieja.';

PROMPT === Verificacion: VALID ===
SELECT OBJECT_NAME, STATUS FROM USER_OBJECTS WHERE OBJECT_NAME = 'SERV_MED_V_EXAMEN';

PROMPT === DESPUES: examenes del portal en la vista (ninguno SIN DICTAMEN) ===
SELECT COUNT(*) EXAMENES_PORTAL_DESPUES FROM SERV_MED_V_EXAMEN WHERE FUENTE = 'PORTAL';
SELECT NVL(DICTAMEN, 'SIN DICTAMEN') DICTAMEN, COUNT(*) EXAMENES
  FROM SERV_MED_V_EXAMEN WHERE FUENTE = 'PORTAL' GROUP BY DICTAMEN ORDER BY 2 DESC;

SPOOL OFF
