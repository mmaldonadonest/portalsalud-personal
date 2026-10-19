-- =====================================================================================
-- pension-16-revision.sql  (16-oct-2026)
-- Base: BIOMETRICO (produccion). SOLO LECTURA: ningun INSERT/UPDATE/DELETE.
--
-- Por que: "Pension IMSS o tramite pendiente (ST2/ST3/ST4/ST7/ST9)" en el PHP era texto libre
-- (php-old/view/content/formResExam/expLab.html). Lo capturado vive en SERV_MED_TAG
-- (TYPE = 'pension'); la columna del WS casi siempre trae el stub "true" del PHP.
-- Antes de cambiarlo a botones Si/No hay que ver que se escribio de verdad.
-- CONTENT es CLOB: se compara con DBMS_LOB.SUBSTR (primeros 200 caracteres).
-- =====================================================================================

-- 1) Valores del tag (ultimo por NSS), agrupados sin importar mayusculas/espacios
SELECT UPPER(TRIM(DBMS_LOB.SUBSTR(CONTENT, 200, 1))) AS VALOR, COUNT(*) AS NSS
  FROM (SELECT NSS, CONTENT,
               ROW_NUMBER() OVER (PARTITION BY NSS ORDER BY ID DESC) RN
          FROM SERV_MED_TAG
         WHERE TYPE = 'pension')
 WHERE RN = 1
 GROUP BY UPPER(TRIM(DBMS_LOB.SUBSTR(CONTENT, 200, 1)))
 ORDER BY NSS DESC
 FETCH FIRST 60 ROWS ONLY;

-- 2) Totales: cuantos NSS tienen el tag y cuantos valores distintos hay
SELECT COUNT(*) AS NSS_CON_TAG, COUNT(DISTINCT UPPER(TRIM(DBMS_LOB.SUBSTR(CONTENT, 200, 1)))) AS VALORES_DISTINTOS
  FROM (SELECT NSS, CONTENT,
               ROW_NUMBER() OVER (PARTITION BY NSS ORDER BY ID DESC) RN
          FROM SERV_MED_TAG
         WHERE TYPE = 'pension')
 WHERE RN = 1;

-- 3) Columna del WS (SERV_MED_ANT_LABORALES.PENSION): confirmar que casi todo es el stub
SELECT NVL(UPPER(TRIM(PENSION)), '(vacio)') AS VALOR, COUNT(*) AS FILAS
  FROM SERV_MED_ANT_LABORALES
 GROUP BY NVL(UPPER(TRIM(PENSION)), '(vacio)')
 ORDER BY FILAS DESC
 FETCH FIRST 30 ROWS ONLY;
