-- =====================================================================================
-- inmunizaciones-16-covid-revision.sql  (16-oct-2026)
-- Base: BIOMETRICO (produccion). SOLO LECTURA: ningun INSERT/UPDATE/DELETE.
--
-- Por que: en el PHP (php-old/view/content/formsAntPP/inmunizaciones.html) COVID era un
-- campo NUMERICO (numero de dosis) y FECHA se rotulaba "Fecha COVID". El portal lo captura
-- como Si/No (0/1). El FT-SO-17 pide COVID-19 por DOSIS (1, 2, 3). Antes de cambiar la
-- captura hay que ver que valores hay guardados, para no pisar dosis 2 o 3 con un Si/No.
-- =====================================================================================

-- 1) Valores de COVID y cuantas filas tiene cada uno
SELECT NVL(TRIM(TO_CHAR(COVID)), '(vacio)') AS COVID, COUNT(*) AS FILAS
  FROM SERV_MED_INMUNIZACIONES
 GROUP BY NVL(TRIM(TO_CHAR(COVID)), '(vacio)')
 ORDER BY FILAS DESC;

-- 2) Valores de las demas vacunas (para confirmar que si son 0/1)
SELECT 'BLG' VACUNA, NVL(TRIM(TO_CHAR(BLG)), '(vacio)') VALOR, COUNT(*) FILAS FROM SERV_MED_INMUNIZACIONES GROUP BY NVL(TRIM(TO_CHAR(BLG)), '(vacio)')
UNION ALL SELECT 'INFLUENZA', NVL(TRIM(TO_CHAR(INFLUENZA)), '(vacio)'), COUNT(*) FROM SERV_MED_INMUNIZACIONES GROUP BY NVL(TRIM(TO_CHAR(INFLUENZA)), '(vacio)')
UNION ALL SELECT 'TD', NVL(TRIM(TO_CHAR(TD)), '(vacio)'), COUNT(*) FROM SERV_MED_INMUNIZACIONES GROUP BY NVL(TRIM(TO_CHAR(TD)), '(vacio)')
ORDER BY 1, 3 DESC;

-- 3) FECHA: cuantas filas la tienen y con que COVID aparece (si la fecha va con COVID > 0
--    confirma que es "Fecha COVID" como en el PHP y no la fecha de influenza del formato)
SELECT NVL(TRIM(TO_CHAR(COVID)), '(vacio)') AS COVID,
       COUNT(*) AS FILAS,
       COUNT(FECHA) AS CON_FECHA
  FROM SERV_MED_INMUNIZACIONES
 GROUP BY NVL(TRIM(TO_CHAR(COVID)), '(vacio)')
 ORDER BY FILAS DESC;

-- 4) Tipo de dato real de las columnas
SELECT COLUMN_NAME, DATA_TYPE, DATA_LENGTH
  FROM USER_TAB_COLUMNS
 WHERE TABLE_NAME = 'SERV_MED_INMUNIZACIONES'
 ORDER BY COLUMN_ID;
