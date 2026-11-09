-- =============================================================================
-- Estado civil en la ficha de Busqueda NSS: que valores trae el biometrico.
-- BASE: la del portal (BIOMETRICO, donde viven BIO_EMPLEADO y las SERV_MED_*). NO ORDS.
-- SOLO LECTURA: no modifica nada.
--
-- La ficha muestra "Estado civil 0" porque el WS de busqueda regresa EMP_ESTADO_CIVIL
-- tal cual. Para poner etiquetas (Soltero, Casado...) hace falta saber que codigos hay
-- y si existe un catalogo en el biometrico.
-- Pasame el resultado de las 3 consultas.
-- =============================================================================

-- 1) Valores distintos y cuantos empleados tienen cada uno
SELECT NVL(TO_CHAR(EMP_ESTADO_CIVIL), '(vacio)') AS valor,
       COUNT(*)                                   AS empleados
  FROM BIO_EMPLEADO
 GROUP BY EMP_ESTADO_CIVIL
 ORDER BY COUNT(*) DESC;

-- 2) Valores separados por empleados activos (EMP_STATUS 0 = activo)
SELECT NVL(TO_CHAR(EMP_ESTADO_CIVIL), '(vacio)') AS valor,
       SUM(CASE WHEN EMP_STATUS = 0 THEN 1 ELSE 0 END) AS activos,
       SUM(CASE WHEN EMP_STATUS = 0 THEN 0 ELSE 1 END) AS otros
  FROM BIO_EMPLEADO
 GROUP BY EMP_ESTADO_CIVIL
 ORDER BY 2 DESC;

-- 3) Tablas o columnas que parezcan catalogo de estado civil en el esquema
SELECT OWNER, TABLE_NAME, COLUMN_NAME, DATA_TYPE
  FROM ALL_TAB_COLUMNS
 WHERE COLUMN_NAME LIKE '%CIVIL%'
    OR TABLE_NAME  LIKE '%CIVIL%'
 ORDER BY OWNER, TABLE_NAME, COLUMN_NAME;
