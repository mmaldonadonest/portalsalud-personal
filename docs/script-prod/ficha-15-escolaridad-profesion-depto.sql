-- ============================================================================
-- Base de PRODUCCION (BIOMETRICO). SOLO LECTURA.
-- Ficha de identificacion del FT-SO-17 (15-oct-2026): el portal no muestra ESCOLARIDAD,
-- PROFESION U OFICIO ni DEPARTAMENTO. Esta consulta busca de donde sacarlos por empleado.
-- ============================================================================
SET LINESIZE 200
SET PAGESIZE 200

-- 1) Columnas del biometrico que suenan a escolaridad, profesion u oficio, departamento
SELECT table_name, column_name, data_type, data_length
  FROM user_tab_columns
 WHERE REGEXP_LIKE(column_name, 'ESTUD|ESCOLAR|PROFES|OFICIO|OCUPA|DEPART|DEPTO|DIVISION|AREA', 'i')
   AND table_name LIKE 'BIO%'
 ORDER BY table_name, column_name;

-- 2) Valores de NIV_ESTUIDIOS (nivel de estudios) en empleados activos
SELECT NVL(TRIM(NIV_ESTUIDIOS), '(vacio)') nivel_estudios, COUNT(*) empleados
  FROM BIO_EMPLEADO
 WHERE EMP_STATUS = 1
 GROUP BY NVL(TRIM(NIV_ESTUIDIOS), '(vacio)')
 ORDER BY 2 DESC;

-- 3) DIVISION de los datos laborales (posible "departamento"), empleados activos, top 30
SELECT * FROM (
  SELECT NVL(TRIM(c.DIVISION), '(vacio)') division, COUNT(*) empleados
    FROM BIO_DATOS_LABORALES_EMPLEADOS c
    JOIN BIO_EMPLEADO d ON d.EMP_NSS = c.EMP_NSS AND d.EMP_STATUS = 1
   GROUP BY NVL(TRIM(c.DIVISION), '(vacio)')
   ORDER BY 2 DESC
) WHERE ROWNUM <= 30;

-- 4) Ejemplo con el NSS de prueba: lo que hay hoy para escolaridad y division
SELECT d.EMP_NSS, d.NIV_ESTUIDIOS, c.DIVISION
  FROM BIO_EMPLEADO d
  LEFT JOIN BIO_DATOS_LABORALES_EMPLEADOS c ON c.EMP_NSS = d.EMP_NSS
 WHERE d.EMP_NSS = '30048315698';
