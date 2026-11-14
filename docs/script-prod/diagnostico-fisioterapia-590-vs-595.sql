-- ============================================================================================
-- Diagnostico FISIOTERAPIA 2026: el portal cuenta 590, las laminas de SM 595 - 13-nov-2026
--
-- Base: BIOMETRICO (PDBPRD) - tablas y vista del PORTAL. NO va a ORDS.
-- SOLO LECTURA: no inserta, no borra, no cambia nada.
--
-- Hipotesis a revisar (de la mas a la menos probable):
--   1. La vista quita el renglon del Excel si ese NSS tiene consulta en el portal el MISMO dia
--      (NOT EXISTS por NSS + fecha). Si esa consulta del portal trae otra causa, la FISIOTERAPIA
--      del Excel se pierde.
--   2. Renglones marcados DUPLICADO al cargar (no entran a la vista).
--   3. Causa escrita distinto (FISIOTERPIA, FISIOTERAPIA RT, etc.): el detalle compara exacto.
--   4. Renglones que no entraron (error de NSS, predio o fecha): estan en el REPORTE del lote.
--
-- Correr con:  @C:\miguel\portalsalud-personal\docs\script-prod\diagnostico-fisioterapia-590-vs-595.sql
-- ============================================================================================
SET PAGESIZE 300
SET LINESIZE 250
SET FEEDBACK ON
SPOOL C:\miguel\diagnostico-fisioterapia-salida.txt

PROMPT === 1) Lotes de ATENCIONES cargados (el ultimo debe ser el de Angie) ===
SELECT ID, ARCHIVO, ESTADO, FILAS_LEIDAS, FILAS_VALIDAS, FILAS_DUPLICADAS, FILAS_ERROR,
       TO_CHAR(CREATED_AT, 'DD/MM/YYYY HH24:MI') CARGADO, CREATED_BY
  FROM SERV_MED_XLS_LOTE
 WHERE LAYOUT = 'ATENCIONES'
 ORDER BY ID;

PROMPT === 2) Como viene escrita la causa (todo lo que se parezca a FISIO), 2026, en el Excel ===
SELECT '[' || CAUSA || ']' CAUSA_TAL_CUAL, ESTADO_REGISTRO, COUNT(*) N
  FROM SERV_MED_XLS_ATENCION
 WHERE UPPER(CAUSA) LIKE '%FISIO%'
   AND FECHA >= DATE '2026-01-01' AND FECHA < DATE '2027-01-01'
 GROUP BY CAUSA, ESTADO_REGISTRO
 ORDER BY CAUSA, ESTADO_REGISTRO;

PROMPT === 3) Excel FISIOTERAPIA 2026 VALIDO que la vista quita por tener consulta del portal el mismo dia ===
SELECT x.ID, x.LOTE_ID, x.ORIGEN_FILA, x.NSS, TO_CHAR(x.FECHA, 'DD/MM/YYYY') FECHA, x.PREDIO,
       c.REG_ID REG_PORTAL, c.CAUSA CAUSA_EN_PORTAL
  FROM SERV_MED_XLS_ATENCION x
  JOIN TBL_SERV_CONSULTA_MEDICA c ON c.NSS = x.NSS AND TRUNC(c.FECHA) = x.FECHA
 WHERE UPPER(TRIM(x.CAUSA)) = 'FISIOTERAPIA'
   AND x.ESTADO_REGISTRO = 'VALIDO'
   AND x.FECHA >= DATE '2026-01-01' AND x.FECHA < DATE '2027-01-01'
 ORDER BY x.FECHA;

PROMPT === 4) FISIOTERAPIA 2026 por mes: Excel completo vs lo que cuenta la vista ===
SELECT TO_CHAR(m.MES, 'YYYY-MM') MES,
       (SELECT COUNT(*) FROM SERV_MED_XLS_ATENCION x
         WHERE UPPER(TRIM(x.CAUSA)) = 'FISIOTERAPIA' AND TRUNC(x.FECHA, 'MM') = m.MES) EXCEL_TODO,
       (SELECT COUNT(*) FROM SERV_MED_XLS_ATENCION x
         WHERE UPPER(TRIM(x.CAUSA)) = 'FISIOTERAPIA' AND TRUNC(x.FECHA, 'MM') = m.MES
           AND x.ESTADO_REGISTRO = 'VALIDO') EXCEL_VALIDO,
       (SELECT COUNT(*) FROM SERV_MED_V_ATENCION v
         WHERE UPPER(TRIM(v.CAUSA)) = 'FISIOTERAPIA' AND TRUNC(v.FECHA, 'MM') = m.MES) VISTA
  FROM (SELECT ADD_MONTHS(DATE '2026-01-01', LEVEL - 1) MES FROM DUAL CONNECT BY LEVEL <= 12) m
 ORDER BY m.MES;

PROMPT === 5) FISIOTERAPIA 2026 por predio: Excel completo vs vista (SM: Z VALLEJO 29) ===
SELECT NVL(e.PREDIO, v.PREDIO) PREDIO, NVL(e.N, 0) EXCEL_TODO, NVL(v.N, 0) VISTA
  FROM (SELECT UPPER(TRIM(PREDIO)) PREDIO, COUNT(*) N FROM SERV_MED_XLS_ATENCION
         WHERE UPPER(TRIM(CAUSA)) = 'FISIOTERAPIA'
           AND FECHA >= DATE '2026-01-01' AND FECHA < DATE '2027-01-01'
         GROUP BY UPPER(TRIM(PREDIO))) e
  FULL JOIN
       (SELECT UPPER(TRIM(PREDIO)) PREDIO, COUNT(*) N FROM SERV_MED_V_ATENCION
         WHERE UPPER(TRIM(CAUSA)) = 'FISIOTERAPIA'
           AND FECHA >= DATE '2026-01-01' AND FECHA < DATE '2027-01-01'
         GROUP BY UPPER(TRIM(PREDIO))) v
    ON v.PREDIO = e.PREDIO
 ORDER BY 1;

PROMPT === 6) Errores del lote mas reciente que mencionen FISIO (renglones que no entraron) ===
SELECT ID, DBMS_LOB.SUBSTR(REPORTE, 4000, 1) REPORTE_INICIO
  FROM SERV_MED_XLS_LOTE
 WHERE LAYOUT = 'ATENCIONES'
   AND ID = (SELECT MAX(ID) FROM SERV_MED_XLS_LOTE WHERE LAYOUT = 'ATENCIONES')
   AND DBMS_LOB.INSTR(UPPER(REPORTE), 'FISIO') > 0;

SPOOL OFF
PROMPT Listo. Salida en C:\miguel\diagnostico-fisioterapia-salida.txt
