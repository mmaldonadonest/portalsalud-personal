-- =============================================================================
-- ¿Se pueden sacar AREA, PUESTO y AGENCIA del biometrico por NSS?  SOLO LECTURA.
-- Base: BIOMETRICO (PDBPRD o QA). Lo corre el usuario y me pasa la salida.
--
-- Lo que ya sabemos (docs/contextoWS.txt, WS Catalogo/usuario y Servcio/consulta):
--   PUESTO  -> BIO_DATOS_LABORALES_EMPLEADOS.ID_PUESTO -> BIOMETRICO_PUESTO.PUESTO_NOMBRE   (confirmado, el WS ya lo usa)
--   CUENTA  -> BIO_DATOS_LABORALES_EMPLEADOS.CUENTA_ID -> BIOMETRICO_CUENTA_SAP / BIOMETRICO_CUENTA (confirmado)
--   PREDIO  -> BIO_DATOS_LABORALES_EMPLEADOS.PREDIO   -> TAB_PREDIOS                          (confirmado)
--   AGENCIA -> el WS legacy la regresa FIJA como 'sin agencia' (contextoWS.txt:2516): nunca se leyo de una tabla.
--   AREA    -> no hay ninguna lectura en los WS. Solo aparece TBL_APPS_USUARIO.ID_AREAS (es del launcher, no del empleado).
-- =============================================================================
SET LINESIZE 200
SET PAGESIZE 200
COLUMN table_name  FORMAT A35
COLUMN column_name FORMAT A35
COLUMN data_type   FORMAT A14

-- 1) Columnas de las dos tablas del empleado: aqui se ve si hay AREA, AGENCIA, DEPARTAMENTO, EMPRESA...
SELECT table_name, column_id, column_name, data_type, data_length
  FROM user_tab_columns
 WHERE table_name IN ('BIO_EMPLEADO', 'BIO_DATOS_LABORALES_EMPLEADOS')
 ORDER BY table_name, column_id;

-- 2) Cualquier columna del esquema que suene a area / agencia / departamento / empresa
SELECT table_name, column_name, data_type
  FROM user_tab_columns
 WHERE REGEXP_LIKE(column_name, 'AREA|AGENCIA|DEPART|DEPTO|EMPRESA|RAZON|PATRON|OUTSOURC|PROVEEDOR|TURNO', 'i')
   AND table_name NOT LIKE 'SERV_MED%'
 ORDER BY table_name, column_name;

-- 3) Catalogos candidatos (tablas cuyo nombre suena a area / agencia / departamento / empresa / puesto)
SELECT t.table_name, t.num_rows
  FROM user_tables t
 WHERE REGEXP_LIKE(t.table_name, 'AREA|AGENCIA|DEPART|DEPTO|EMPRESA|PUESTO|PATRON|PROVEEDOR', 'i')
 ORDER BY t.table_name;

-- 4) Puesto: que tanto esta lleno (empleados activos con / sin puesto)
SELECT CASE WHEN c.ID_PUESTO IS NULL OR c.ID_PUESTO = 0 THEN 'SIN PUESTO'
            WHEN p.PUESTO_ID IS NULL THEN 'PUESTO NO EXISTE EN CATALOGO'
            ELSE 'CON PUESTO' END estado,
       COUNT(*) empleados
  FROM BIO_EMPLEADO d
  LEFT JOIN BIO_DATOS_LABORALES_EMPLEADOS c ON c.EMP_NSS = d.EMP_NSS
  LEFT JOIN BIOMETRICO_PUESTO p ON p.PUESTO_ID = c.ID_PUESTO
 WHERE d.EMP_STATUS = 1
 GROUP BY CASE WHEN c.ID_PUESTO IS NULL OR c.ID_PUESTO = 0 THEN 'SIN PUESTO'
               WHEN p.PUESTO_ID IS NULL THEN 'PUESTO NO EXISTE EN CATALOGO'
               ELSE 'CON PUESTO' END;

-- 5) Top 30 puestos de empleados activos (para ver si los nombres se parecen a los del Excel:
--    "AUX ALMACEN / AYUDANTE GRAL", "MONTACARGISTA", "SUPERVISOR"...)
SELECT * FROM (
  SELECT p.PUESTO_NOMBRE, COUNT(*) empleados
    FROM BIO_EMPLEADO d
    JOIN BIO_DATOS_LABORALES_EMPLEADOS c ON c.EMP_NSS = d.EMP_NSS
    JOIN BIOMETRICO_PUESTO p ON p.PUESTO_ID = c.ID_PUESTO
   WHERE d.EMP_STATUS = 1
   GROUP BY p.PUESTO_NOMBRE
   ORDER BY 2 DESC
) WHERE ROWNUM <= 30;
