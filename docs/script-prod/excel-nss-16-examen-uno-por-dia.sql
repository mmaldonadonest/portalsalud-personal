-- ============================================================================================
-- Examenes del portal: UNO por persona por dia en SERV_MED_V_EXAMEN - 30-oct-2026
--
-- Base: BIOMETRICO (PDBPRD) - vista del PORTAL. No toca datos. Lo corre el usuario.
--
-- Por que: el Home contaba 38 examenes del portal de solo 14 personas (Norma Angelica Rosales 8 el
-- 27-oct, Maribel Hernandez 7 el 14-sep, Luis Hernandez 6 el 11-sep). PR_SERVICIO_MED_EXAMEN2 (ORDS)
-- inserta un renglon en SERV_MED_RESULTADO_EXAMEN_HIST CADA VEZ que se guarda el examen, tambien al
-- guardar una sola seccion (docs/ords-examen-historial.sql, bloque 2). Ocho guardados del mismo
-- examen eran ocho examenes en las graficas.
--
-- Cambio: la rama PORTAL toma solo el ULTIMO guardado de cada NSS en cada dia (su dictamen es el
-- final). Las ramas del Excel quedan identicas a excel-nss-15-sin-carga-vieja.sql. La tabla
-- SERV_MED_RESULTADO_EXAMEN_HIST no se toca: sigue teniendo cada guardado.
-- Afecta a todo lo que lee la vista: Home, Analisis de examenes y el conteo "Portal N".
--
-- Reversa: correr excel-nss-15-sin-carga-vieja.sql (solo su bloque de SERV_MED_V_EXAMEN).
-- Correr con:  @C:\miguel\portalsalud-personal\docs\script-prod\excel-nss-16-examen-uno-por-dia.sql
-- ============================================================================================
SET DEFINE OFF
SPOOL C:\miguel\examen-uno-por-dia-salida.txt

PROMPT === ANTES: guardados por persona y dia (solo los que tienen mas de uno) ===
SELECT h.NSS, TRUNC(h.FECHA_REGISTRO) DIA, COUNT(*) GUARDADOS,
       TO_CHAR(MIN(h.FECHA_REGISTRO), 'HH24:MI') PRIMERO, TO_CHAR(MAX(h.FECHA_REGISTRO), 'HH24:MI') ULTIMO
  FROM SERV_MED_RESULTADO_EXAMEN_HIST h
 GROUP BY h.NSS, TRUNC(h.FECHA_REGISTRO)
HAVING COUNT(*) > 1
 ORDER BY 3 DESC, 1;

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
SELECT 'EXCEL', TO_CHAR(x.ID), x.NSS, x.FECHA, x.PREDIO, x.CUENTA, x.AREA, x.NOMBRE, x.GENERO, x.EDAD,
       x.PUESTO, 'POS_INCAPACIDAD', NULL, x.DICTAMEN, x.DIAGNOSTICO,
       CASE WHEN x.EDAD IS NULL OR x.EDAD <= 0 THEN NULL WHEN x.EDAD <= 25 THEN '18-25' WHEN x.EDAD <= 35 THEN '26-35' WHEN x.EDAD <= 45 THEN '36-45' WHEN x.EDAD <= 55 THEN '46-55' ELSE '55+' END
  FROM SERV_MED_XLS_EXAMEN_POS x
 WHERE x.ESTADO_REGISTRO = 'VALIDO'
   AND NOT EXISTS (SELECT 1 FROM SERV_MED_RESULTADO_EXAMEN_HIST h
                    WHERE h.NSS = x.NSS AND TRUNC(h.FECHA_REGISTRO) BETWEEN x.FECHA - 1 AND x.FECHA + 1);

COMMENT ON TABLE SERV_MED_V_EXAMEN IS 'Examenes: portal (ultimo guardado por NSS y dia de SERV_MED_RESULTADO_EXAMEN_HIST, 30-oct-2026) + nuevo ingreso, periodico y pos incapacidad del Excel por NSS. Sin carga vieja.';

PROMPT === Verificacion: VALID ===
SELECT OBJECT_NAME, STATUS FROM USER_OBJECTS WHERE OBJECT_NAME = 'SERV_MED_V_EXAMEN';

PROMPT === DESPUES: examenes del portal en la vista (debe ser personas x dia) ===
SELECT COUNT(*) EXAMENES_PORTAL_DESPUES FROM SERV_MED_V_EXAMEN WHERE FUENTE = 'PORTAL';
SELECT COUNT(*) PERSONAS_POR_DIA
  FROM (SELECT DISTINCT NSS, TRUNC(FECHA_REGISTRO) FROM SERV_MED_RESULTADO_EXAMEN_HIST);

SPOOL OFF
