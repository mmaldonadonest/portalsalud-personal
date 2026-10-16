-- ============================================================================================
-- Edad 14b: por que 0 de 81 examenes del portal tienen edad (SOLO LECTURA) - 14-oct-2026
--
-- Base: BIOMETRICO (PDBPRD). Tablas del portal (SERV_MED_RESULTADO_EXAMEN_HIST) y biometrico
-- (BIO_EMPLEADO via SERV_MED_V_EMPLEADO).
--
-- Por que: el 89% de BIO_EMPLEADO SI tiene EMP_FECHAN (30,492 de 34,121), pero la vista da 0 de
-- 81 examenes del portal con edad. Sospecha: el cruce por NSS falla (NSS de 10 digitos sin el
-- cero inicial, espacios) o los 81 son de pocos NSS de prueba.
--
-- Correr con:  @C:\miguel\portalsalud-personal\docs\script-prod\edad-14b-cruce-examen-empleado.sql
-- ============================================================================================
SET LINESIZE 200 PAGESIZE 100
SET DEFINE OFF
SPOOL C:\miguel\edad-cruce-salida.txt

PROMPT === 1) Examenes del portal: NSS distintos, largo del NSS y si cruzan con el empleado ===
SELECT LENGTH(TRIM(h.NSS))                                       largo_nss,
       COUNT(*)                                                  examenes,
       COUNT(DISTINCT h.NSS)                                     nss_distintos,
       COUNT(DISTINCT CASE WHEN emp.NSS IS NOT NULL THEN h.NSS END)       cruzan_tal_cual,
       COUNT(DISTINCT CASE WHEN emp.FECHA_NAC IS NOT NULL THEN h.NSS END) cruzan_con_fecha,
       COUNT(DISTINCT CASE WHEN emp2.NSS IS NOT NULL THEN h.NSS END)      cruzan_con_cero
  FROM SERV_MED_RESULTADO_EXAMEN_HIST h
  LEFT JOIN SERV_MED_V_EMPLEADO emp  ON emp.NSS  = h.NSS
  LEFT JOIN SERV_MED_V_EMPLEADO emp2 ON emp2.NSS = LPAD(TRIM(h.NSS), 11, '0')
 GROUP BY LENGTH(TRIM(h.NSS))
 ORDER BY 1;

PROMPT === 2) Los NSS con mas examenes (para ver si son de prueba) ===
SELECT h.NSS, COUNT(*) examenes, MAX(emp.NOMBRE) nombre, MAX(emp.FECHA_NAC) fecha_nac
  FROM SERV_MED_RESULTADO_EXAMEN_HIST h
  LEFT JOIN SERV_MED_V_EMPLEADO emp ON emp.NSS = h.NSS
 GROUP BY h.NSS
 ORDER BY 2 DESC
 FETCH FIRST 15 ROWS ONLY;

PROMPT === 3) Tipo de dato de NSS en las dos tablas (un NUMBER pierde el cero inicial) ===
SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, DATA_LENGTH
  FROM ALL_TAB_COLUMNS
 WHERE (TABLE_NAME, COLUMN_NAME) IN (('SERV_MED_RESULTADO_EXAMEN_HIST', 'NSS'), ('BIO_EMPLEADO', 'EMP_NSS'));

SPOOL OFF
