-- =============================================================================
-- Estado civil (3er intento): el cruce por NSS contra solicitud y Excel no dio filas.
-- Ahora: las vistas del biometrico que exponen el estado civil (alguna puede traducir el
-- codigo con un DECODE/CASE) y lo que traen las tablas de paso de SAP.
-- BASE: la del portal (BIOMETRICO). NO ORDS. SOLO LECTURA.
-- Pasame el resultado de las 3 consultas (la 1 trae el texto de las vistas: copialo completo).
-- =============================================================================

SET LONG 200000
SET LINESIZE 32767
SET PAGESIZE 0

-- 1) Definicion de las 3 vistas que tienen columna de estado civil
SELECT VIEW_NAME, TEXT
  FROM ALL_VIEWS
 WHERE OWNER = 'BIOMETRICO'
   AND VIEW_NAME IN ('VW_BIO_CONSULTA_EMPLEADOS', 'VW_IA_EMPLEADOS', 'VW_MAESTRO_EMPLEADOS');

SET PAGESIZE 100

-- 2) Valores que traen las tablas de paso de SAP
SELECT 'BIO_TEMP_EMPLEADO_SAP' AS tabla, NVL(TRIM(CIVIL), '(vacio)') AS valor, COUNT(*) AS filas
  FROM BIO_TEMP_EMPLEADO_SAP GROUP BY NVL(TRIM(CIVIL), '(vacio)')
UNION ALL
SELECT 'BIO_TEMP_EMPLEADO', NVL(TRIM(CIVIL), '(vacio)'), COUNT(*)
  FROM BIO_TEMP_EMPLEADO GROUP BY NVL(TRIM(CIVIL), '(vacio)')
UNION ALL
SELECT 'TMP_SAP_PASO', NVL(TRIM(CIVIL), '(vacio)'), COUNT(*)
  FROM TMP_SAP_PASO GROUP BY NVL(TRIM(CIVIL), '(vacio)')
 ORDER BY 1, 3 DESC;

-- 3) Codigo del biometrico vs lo que dicen las vistas para las mismas personas
SELECT TRIM(b.EMP_ESTADO_CIVIL)         AS codigo,
       NVL(TRIM(m.ESTADO_CIVIL), '(vacio)') AS vw_maestro,
       NVL(TRIM(i.ESTADO_CIVIL), '(vacio)') AS vw_ia,
       COUNT(*)                         AS personas
  FROM BIO_EMPLEADO b
  LEFT JOIN VW_MAESTRO_EMPLEADOS m ON TO_CHAR(m.NSS) = TO_CHAR(b.EMP_NSS)
  LEFT JOIN VW_IA_EMPLEADOS      i ON TO_CHAR(i.NSS) = TO_CHAR(b.EMP_NSS)
 WHERE b.EMP_STATUS = 0
 GROUP BY TRIM(b.EMP_ESTADO_CIVIL), NVL(TRIM(m.ESTADO_CIVIL), '(vacio)'), NVL(TRIM(i.ESTADO_CIVIL), '(vacio)')
 ORDER BY 1, 4 DESC;
