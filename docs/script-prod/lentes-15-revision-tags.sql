-- ============================================================================
-- Base del portal (BIOMETRICO, tabla SERV_MED_TAG). SOLO LECTURA.
-- 15-oct-2026: el examen inicial ahora captura el SL/CL del FT-SO-04 rev. 05 en los
-- tags que ya usaba el PHP (LENTEOJODERECHOEXP / LENTEOJOIZQUIERDOEXP) como 1/0
-- (1 = medido con lentes). Esta consulta confirma que el PHP guardo 0/1 y no otra cosa;
-- si aparecen otros valores, el impreso los mostraria como "CL (con lentes)".
-- CONTENT es CLOB: se agrupa por sus primeros 50 caracteres.
-- ============================================================================

-- 1) Valores distintos y cuantos NSS tienen cada uno
SELECT TYPE, DBMS_LOB.SUBSTR(CONTENT, 50, 1) AS VALOR, COUNT(*) AS FILAS, COUNT(DISTINCT NSS) AS NSS
  FROM SERV_MED_TAG
 WHERE TYPE IN ('LENTEOJODERECHOEXP', 'LENTEOJOIZQUIERDOEXP')
 GROUP BY TYPE, DBMS_LOB.SUBSTR(CONTENT, 50, 1)
 ORDER BY TYPE, FILAS DESC;

-- 2) Grupo en que se guardaron (el portal escribe en EXAMEN_FISICO)
SELECT TYPE, TAG_GROUP, COUNT(*) AS FILAS
  FROM SERV_MED_TAG
 WHERE TYPE IN ('LENTEOJODERECHOEXP', 'LENTEOJOIZQUIERDOEXP')
 GROUP BY TYPE, TAG_GROUP;
