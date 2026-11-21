-- ============================================================================================
-- C-003: revisar los diagnosticos que SM ya capturo contra el catalogo CIE-10 nuevo (19-nov-2026)
--
-- BASE: base del portal (BIOMETRICO). SOLO LECTURA: no modifica nada.
-- Correr con:  @C:\miguel\portalsalud-personal\docs\script-prod\c003-revisar-diagnosticos-capturados.sql
-- Salida en:   C:\miguel\c003-revisar-diagnosticos-salida.txt
--
-- Contexto: el diagnostico de la consulta se guarda como TEXTO ("M54.5 - LUMBAGO NO ESPECIFICADO"), no como
-- liga al catalogo. Cambiar el catalogo no borra ni cambia lo capturado; esto revisa si esas claves siguen
-- existiendo en el catalogo nuevo (14,485) y si alguna cambio de nombre.
-- ============================================================================================
SET DEFINE OFF
SET PAGESIZE 200
SET LINESIZE 220
COLUMN CLAVE FORMAT A10
COLUMN NOMBRE_ANTES FORMAT A70
COLUMN NOMBRE_HOY FORMAT A70
COLUMN DIAGNOSTICO FORMAT A110
SPOOL C:\miguel\c003-revisar-diagnosticos-salida.txt

PROMPT === 1) Claves del catalogo ANTERIOR (909) que ya no estan en el nuevo ===
SELECT r.CLAVE_ID AS CLAVE, r.CLAVE_NOMBRE AS NOMBRE_ANTES
  FROM SERV_MED_CAT_IDC10_RESP_18NOV r
 WHERE NOT EXISTS (SELECT 1 FROM SERV_MED_CAT_INDICE_IDC10 c WHERE c.CLAVE_ID = r.CLAVE_ID)
 ORDER BY 1;

PROMPT === 2) Claves que siguen pero CAMBIARON de nombre (primeras 50) ===
SELECT * FROM (
  SELECT r.CLAVE_ID AS CLAVE, r.CLAVE_NOMBRE AS NOMBRE_ANTES, c.CLAVE_NOMBRE AS NOMBRE_HOY
    FROM SERV_MED_CAT_IDC10_RESP_18NOV r
    JOIN SERV_MED_CAT_INDICE_IDC10 c ON c.CLAVE_ID = r.CLAVE_ID
   WHERE TRIM(UPPER(r.CLAVE_NOMBRE)) <> TRIM(UPPER(c.CLAVE_NOMBRE))
   ORDER BY 1)
 WHERE ROWNUM <= 50;

PROMPT === 3) Consultas capturadas en el portal: cuantas traen clave, y si la clave esta en el catalogo nuevo ===
-- Un diagnostico puede traer varias claves ("J00.X - ... M54.5 - ..."): se revisa cada una
WITH d AS (
  SELECT ID_ORIGEN, DIAGNOSTICO FROM SERV_MED_V_ATENCION
   WHERE FUENTE = 'PORTAL' AND DIAGNOSTICO IS NOT NULL
), k AS (
  SELECT d.ID_ORIGEN,
         REGEXP_SUBSTR(d.DIAGNOSTICO, '([A-Z][0-9]{2}(\.[0-9X])?) - ', 1, l.COLUMN_VALUE, NULL, 1) AS CLAVE
    FROM d
   CROSS JOIN TABLE(CAST(MULTISET(SELECT LEVEL FROM DUAL
                                  CONNECT BY LEVEL <= REGEXP_COUNT(d.DIAGNOSTICO, '[A-Z][0-9]{2}(\.[0-9X])? - '))
                         AS SYS.ODCINUMBERLIST)) l
)
SELECT 'Consultas del portal con diagnostico' AS CONCEPTO, COUNT(*) AS CANTIDAD FROM d
UNION ALL SELECT 'Consultas SIN ninguna clave (texto libre)', COUNT(*) FROM d
           WHERE REGEXP_COUNT(d.DIAGNOSTICO, '[A-Z][0-9]{2}(\.[0-9X])? - ') = 0
UNION ALL SELECT 'Claves capturadas (todas)', COUNT(*) FROM k
UNION ALL SELECT 'Claves capturadas que SI estan en el catalogo nuevo', COUNT(*) FROM k
           WHERE EXISTS (SELECT 1 FROM SERV_MED_CAT_INDICE_IDC10 c WHERE c.CLAVE_ID = k.CLAVE)
UNION ALL SELECT 'Claves capturadas que NO estan en el catalogo nuevo', COUNT(*) FROM k
           WHERE NOT EXISTS (SELECT 1 FROM SERV_MED_CAT_INDICE_IDC10 c WHERE c.CLAVE_ID = k.CLAVE);

PROMPT === 4) Detalle: claves capturadas que NO estan en el catalogo nuevo ===
WITH d AS (
  SELECT ID_ORIGEN, DIAGNOSTICO FROM SERV_MED_V_ATENCION
   WHERE FUENTE = 'PORTAL' AND DIAGNOSTICO IS NOT NULL
), k AS (
  SELECT REGEXP_SUBSTR(d.DIAGNOSTICO, '([A-Z][0-9]{2}(\.[0-9X])?) - ', 1, l.COLUMN_VALUE, NULL, 1) AS CLAVE
    FROM d
   CROSS JOIN TABLE(CAST(MULTISET(SELECT LEVEL FROM DUAL
                                  CONNECT BY LEVEL <= REGEXP_COUNT(d.DIAGNOSTICO, '[A-Z][0-9]{2}(\.[0-9X])? - '))
                         AS SYS.ODCINUMBERLIST)) l
)
SELECT k.CLAVE, COUNT(*) AS VECES
  FROM k
 WHERE NOT EXISTS (SELECT 1 FROM SERV_MED_CAT_INDICE_IDC10 c WHERE c.CLAVE_ID = k.CLAVE)
 GROUP BY k.CLAVE ORDER BY 2 DESC;

PROMPT === 5) Muestra de consultas SIN clave (texto libre), las 20 mas recientes ===
SELECT * FROM (
  SELECT TO_CHAR(FECHA, 'DD/MM/YYYY') AS FECHA, SUBSTR(DIAGNOSTICO, 1, 110) AS DIAGNOSTICO
    FROM SERV_MED_V_ATENCION
   WHERE FUENTE = 'PORTAL' AND DIAGNOSTICO IS NOT NULL
     AND REGEXP_COUNT(DIAGNOSTICO, '[A-Z][0-9]{2}(\.[0-9X])? - ') = 0
   ORDER BY FECHA DESC)
 WHERE ROWNUM <= 20;

SPOOL OFF
