-- ============================================================================================
-- Oidos del examen medico: donde hay datos reales (SOLO LECTURA, no cambia nada)
-- 13-oct-2026
--
-- Base: BIOMETRICO (PDBPRD). Parte A = tabla del WS ORDS; parte B = tabla del portal (SERV_MED_TAG).
--
-- Por que: el PHP capturaba Oidos en tags (ODCAEINPEXP...) y mandaba vacias las columnas de
-- SERV_MED_OIDOS. El portal hoy captura en SERV_MED_OIDOS y el impreso lee los tags.
-- Antes de pasar el portal a tags hay que saber si el portal ya dejo datos en SERV_MED_OIDOS.
--
-- Correr con:  @C:\miguel\portalsalud-personal\docs\script-prod\oidos-13-revision-datos.sql
-- ============================================================================================
SET LINESIZE 200 PAGESIZE 100
SPOOL C:\miguel\oidos-revision-salida.txt

PROMPT === A) SERV_MED_OIDOS (WS ORDS): renglones con algun dato ===
SELECT COUNT(*)                                            AS total_renglones,
       COUNT(CASE WHEN TRIM(CAE)        IS NOT NULL THEN 1 END) AS con_cae,
       COUNT(CASE WHEN TRIM(MEM_TIM)    IS NOT NULL THEN 1 END) AS con_mem_tim,
       COUNT(CASE WHEN TRIM(AGUD_AUDIT) IS NOT NULL THEN 1 END) AS con_agud_audit,
       COUNT(CASE WHEN TRIM(OD)         IS NOT NULL THEN 1 END) AS con_od,
       COUNT(CASE WHEN TRIM(OI)         IS NOT NULL THEN 1 END) AS con_oi
  FROM SERV_MED_OIDOS;

PROMPT === A2) Muestra de hasta 10 renglones con dato (si los hay) ===
SELECT NSS, CAE, MEM_TIM, AGUD_AUDIT, OD, OI
  FROM SERV_MED_OIDOS
 WHERE COALESCE(TRIM(CAE), TRIM(MEM_TIM), TRIM(AGUD_AUDIT), TRIM(OD), TRIM(OI)) IS NOT NULL
 FETCH FIRST 10 ROWS ONLY;

PROMPT === B) SERV_MED_TAG (portal): NSS distintos con dato por tag de Oidos ===
SELECT TYPE, COUNT(DISTINCT NSS) AS nss_con_dato
  FROM SERV_MED_TAG
 WHERE TYPE IN ('ODCAEINPEXP','ODMEMTIMINPEXP','ODAGAUDIINPEXP','OIMEMTIMINPEXP','OIAGAUDIINPEXP',
                'ODEXCAEEXFISINP','OICAEINPEXP')
   AND TRIM(DBMS_LOB.SUBSTR(CONTENT, 200, 1)) IS NOT NULL   -- CONTENT es CLOB
 GROUP BY TYPE
 ORDER BY TYPE;

SPOOL OFF
