-- ============================================================================================
-- ¿Por que las atenciones capturadas en el PORTAL no entran al ranking de Analisis > Causas? - 13-nov-2026
--
-- Base: BIOMETRICO (PDBPRD) - vista y tablas del PORTAL. NO va a ORDS.
-- SOLO LECTURA: no inserta, no borra, no cambia nada. Se puede correr cuantas veces se quiera.
--
-- Contexto: al 13-nov, SERV_MED_V_ATENCION tenia EXCEL 18,980 + PORTAL 196, y Causas 2026 decia
-- «18,980 atenciones clasificadas», exactamente lo del Excel. El ranking cuenta los renglones de la
-- vista con CAUSA no vacia dentro del periodo (VistaAtencionRepository, dimension CAUSA). Este
-- script dice, para las del portal, cuantas caen en cada anio y cuantas traen causa.
--
-- Correr con:  @C:\miguel\portalsalud-personal\docs\script-prod\diagnostico-atenciones-portal-en-causas.sql
-- ============================================================================================
SET PAGESIZE 300
SET LINESIZE 200
SET FEEDBACK ON
SPOOL C:\miguel\diagnostico-atenciones-portal-salida.txt

PROMPT === 1) Vista por fuente y anio: total, con causa y sin causa ===
SELECT FUENTE, TO_CHAR(FECHA, 'YYYY') ANIO, COUNT(*) TOTAL,
       COUNT(CASE WHEN TRIM(CAUSA) IS NOT NULL THEN 1 END) CON_CAUSA,
       COUNT(CASE WHEN TRIM(CAUSA) IS NULL THEN 1 END)     SIN_CAUSA
  FROM SERV_MED_V_ATENCION
 GROUP BY FUENTE, TO_CHAR(FECHA, 'YYYY')
 ORDER BY FUENTE, ANIO;

PROMPT === 2) Portal: que se capturo en CAUSA y en TIPO_CONSULTA (RAMO), por anio ===
SELECT TO_CHAR(FECHA, 'YYYY') ANIO, NVL(RAMO, '(vacio)') RAMO, NVL(CAUSA, '(vacio)') CAUSA, COUNT(*) N
  FROM SERV_MED_V_ATENCION
 WHERE FUENTE = 'PORTAL'
 GROUP BY TO_CHAR(FECHA, 'YYYY'), RAMO, CAUSA
 ORDER BY ANIO, N DESC;

PROMPT === 3) Portal 2026 por mes: total y con causa (para ver desde cuando se captura) ===
SELECT TO_CHAR(FECHA, 'YYYY-MM') MES, COUNT(*) TOTAL,
       COUNT(CASE WHEN TRIM(CAUSA) IS NOT NULL THEN 1 END) CON_CAUSA
  FROM SERV_MED_V_ATENCION
 WHERE FUENTE = 'PORTAL'
   AND FECHA >= DATE '2026-01-01' AND FECHA < DATE '2027-01-01'
 GROUP BY TO_CHAR(FECHA, 'YYYY-MM')
 ORDER BY MES;

PROMPT === 4) Consultas del portal que la vista no muestra como PORTAL (deberia ser 0) ===
SELECT COUNT(*) CONSULTAS_TABLA,
       (SELECT COUNT(*) FROM SERV_MED_V_ATENCION WHERE FUENTE = 'PORTAL') EN_VISTA
  FROM TBL_SERV_CONSULTA_MEDICA;

PROMPT === 5) Excel que la vista quita porque el portal tiene consulta del mismo NSS y dia ===
SELECT TO_CHAR(x.FECHA, 'YYYY') ANIO, COUNT(*) N
  FROM SERV_MED_XLS_ATENCION x
 WHERE x.ESTADO_REGISTRO = 'VALIDO'
   AND EXISTS (SELECT 1 FROM TBL_SERV_CONSULTA_MEDICA c
                WHERE c.NSS = x.NSS AND TRUNC(c.FECHA) = x.FECHA)
 GROUP BY TO_CHAR(x.FECHA, 'YYYY')
 ORDER BY ANIO;

SPOOL OFF
PROMPT Listo. Salida en C:\miguel\diagnostico-atenciones-portal-salida.txt
