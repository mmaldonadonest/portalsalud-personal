-- ============================================================================================
-- Quitar la carga vieja de las vistas - 14-oct-2026
--
-- Base: BIOMETRICO (PDBPRD) - vistas del PORTAL. No toca datos: las tablas SERV_MED_BITACORA_* se
-- quedan en la base solo como referencia.
--
-- Por que: decision del usuario (14-oct-2026): el portal ya no lee la carga vieja por nombre en
-- ningun lado. Hasta hoy, SERV_MED_V_EXAMEN (pos incapacidad), SERV_MED_V_ANTIDOPING y
-- SERV_MED_V_MATERNIDAD la leian "mientras su tabla nueva estuviera vacia". Se quita ese bloque: si
-- se vacian los lotes de prueba, esas pantallas quedan vacias en lugar de mostrar la carga vieja.
-- Mismas definiciones que excel-nss-05-vistas.sql. Idempotente (CREATE OR REPLACE).
--
-- Reversa: correr la version anterior de excel-nss-05-vistas.sql (git).
-- Correr con:  @C:\miguel\portalsalud-personal\docs\script-prod\excel-nss-15-sin-carga-vieja.sql
-- ============================================================================================
SET DEFINE OFF
SPOOL C:\miguel\sin-carga-vieja-salida.txt

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
                    WHERE h.NSS = x.NSS AND TRUNC(h.FECHA_REGISTRO) BETWEEN x.FECHA - 1 AND x.FECHA + 1);

COMMENT ON TABLE SERV_MED_V_EXAMEN IS 'Examenes: portal (SERV_MED_RESULTADO_EXAMEN_HIST) + nuevo ingreso, periodico y pos incapacidad del Excel por NSS. Sin carga vieja (14-oct-2026).';

PROMPT === SERV_MED_V_ANTIDOPING ===
CREATE OR REPLACE VIEW SERV_MED_V_ANTIDOPING AS
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
       CAST(UPPER(TRIM(p.TIPO_PRUEBA)) AS VARCHAR2(40))            TIPO_PRUEBA,
       CAST(UPPER(TRIM(p.RESULTADO)) AS VARCHAR2(40))              RESULTADO,
       CAST(UPPER(TRIM(p.SUSTANCIA)) AS VARCHAR2(40))              SUSTANCIA,
       CAST(NULL AS NUMBER)                                        ALCOHOLEMIA,
       CAST(CASE UPPER(TRIM(p.STATUS_CONCLUSION))
                 WHEN 'N/A' THEN 'N/A'
                 WHEN 'CAPA' THEN 'CAPA'
                 WHEN 'NO_INTERESADO_BAJA' THEN 'NO INTERESADO / BAJA'
                 WHEN 'NO_CONTRATADO' THEN 'NO CONTRATADO'
                 WHEN 'RECAIDA' THEN 'RECAIDA' END AS VARCHAR2(40)) CONCLUSION,
       CAST(CASE UPPER(TRIM(p.STATUS_CONCLUSION))
                 WHEN 'LABORANDO' THEN 'LABORANDO'
                 WHEN 'BAJA_NO_APLICA' THEN 'BAJA / NO CONTRATADO' END AS VARCHAR2(40)) STATUS,
       CAST(SUBSTR(p.OBSERVACIONES, 1, 1000) AS VARCHAR2(1000 CHAR)) OBSERVACIONES
  FROM SERV_MED_ANTIDOPING_RESULTADO p
  LEFT JOIN SERV_MED_V_EMPLEADO emp ON emp.NSS = p.NSS
UNION ALL
SELECT 'EXCEL', TO_CHAR(x.ID), x.NSS, x.FECHA, x.PREDIO, x.CUENTA, x.AREA, x.NOMBRE, x.GENERO, x.EDAD, x.PUESTO,
       x.TIPO_PRUEBA, x.RESULTADO, x.SUSTANCIA, x.ALCOHOLEMIA, x.CONCLUSION, x.STATUS, x.OBSERVACIONES
  FROM SERV_MED_XLS_ANTIDOPING x
 WHERE x.ESTADO_REGISTRO = 'VALIDO'
   AND NOT EXISTS (SELECT 1 FROM SERV_MED_ANTIDOPING_RESULTADO p
                    WHERE p.NSS = x.NSS AND TRUNC(p.FECHA_REGISTRO) = x.FECHA
                      AND UPPER(TRIM(p.TIPO_PRUEBA)) = x.TIPO_PRUEBA);

COMMENT ON TABLE SERV_MED_V_ANTIDOPING IS 'Antidoping: portal (SERV_MED_ANTIDOPING_RESULTADO) + Excel por NSS (SERV_MED_XLS_ANTIDOPING). Sin carga vieja (14-oct-2026).';

PROMPT === SERV_MED_V_MATERNIDAD ===
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
       p.PROXIMA_REVISION                                          PROXIMA_REVISION
  FROM SERV_MED_MATERNIDAD_SEGUIMIENTO p
  LEFT JOIN SERV_MED_V_EMPLEADO emp ON emp.NSS = p.NSS
UNION ALL
SELECT 'EXCEL', TO_CHAR(x.ID), x.NSS, x.FECHA, x.PREDIO, x.CUENTA, x.AREA, x.NOMBRE, x.GENERO, x.EDAD, x.PUESTO,
       x.CLAVE_EMBARAZO, x.STATUS_LABORAL, x.FUM,
       CASE WHEN x.FUM IS NOT NULL AND x.FECHA >= x.FUM THEN TRUNC((x.FECHA - x.FUM) / 7) END,
       x.FECHA_PROB_PARTO, x.FECHA_REAL_REINICIO, x.FECHA_INICIO_INCAP, x.FECHA_TERMINO_INCAP,
       x.FOLIO_INCAPACIDAD, x.FECHA_INICIO_LACTANCIA, x.FECHA_TERMINO_LACTANCIA, NULL
  FROM SERV_MED_XLS_MATERNIDAD x
 WHERE x.ESTADO_REGISTRO = 'VALIDO'
   AND NOT EXISTS (SELECT 1 FROM SERV_MED_MATERNIDAD_SEGUIMIENTO p
                    WHERE p.NSS = x.NSS AND TRUNC(p.FECHA_REGISTRO, 'MM') = TRUNC(x.FECHA, 'MM'));

COMMENT ON TABLE SERV_MED_V_MATERNIDAD IS 'Maternidad con historial: chequeos del portal + reportes mensuales del Excel (SERV_MED_XLS_MATERNIDAD). Sin carga vieja (14-oct-2026). Embarazo = CLAVE_EMBARAZO.';

PROMPT === Recompilar la vista que depende de SERV_MED_V_ANTIDOPING (Oracle la marca INVALID al recrearla) ===
ALTER VIEW SERV_MED_V_ANTIDOPING_SUSTANCIA COMPILE;

PROMPT === Verificacion: VALID y ninguna fila CARGA_VIEJA ===
SELECT OBJECT_NAME, STATUS FROM USER_OBJECTS
 WHERE OBJECT_NAME IN ('SERV_MED_V_EXAMEN', 'SERV_MED_V_ANTIDOPING', 'SERV_MED_V_MATERNIDAD',
                       'SERV_MED_V_MATERNIDAD_ACTUAL', 'SERV_MED_V_ANTIDOPING_SUSTANCIA')
 ORDER BY 1;
SELECT 'EXAMEN' vista, FUENTE, COUNT(*) FROM SERV_MED_V_EXAMEN GROUP BY FUENTE
UNION ALL SELECT 'ANTIDOPING', FUENTE, COUNT(*) FROM SERV_MED_V_ANTIDOPING GROUP BY FUENTE
UNION ALL SELECT 'MATERNIDAD', FUENTE, COUNT(*) FROM SERV_MED_V_MATERNIDAD GROUP BY FUENTE
ORDER BY 1, 2;

SPOOL OFF
