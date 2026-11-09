-- =============================================================================
-- Carga de Excel por pantalla: ¿donde registra el biometrico las bajas?
-- BASE: base del portal (esquema BIOMETRICO). SOLO LECTURA, no modifica nada.
--
-- Hoy la carga solo rechaza el NSS que no existe en BIO_EMPLEADO o que tiene
-- EMP_STATUS 99 (prueba). Para avisar "empleado dado de baja" falta saber que
-- dato lo dice: un valor de EMP_STATUS, una fecha de baja o una tabla aparte.
-- =============================================================================

-- 1. Valores de EMP_STATUS y cuantos empleados tiene cada uno
SELECT EMP_STATUS, COUNT(*) AS EMPLEADOS
  FROM BIO_EMPLEADO
 GROUP BY EMP_STATUS
 ORDER BY EMP_STATUS;

-- 2. Columnas de BIO_EMPLEADO y de los datos laborales que hablen de baja, estatus o vigencia
SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE
  FROM USER_TAB_COLUMNS
 WHERE TABLE_NAME IN ('BIO_EMPLEADO', 'BIO_DATOS_LABORALES_EMPLEADOS')
   AND (COLUMN_NAME LIKE '%BAJA%' OR COLUMN_NAME LIKE '%STAT%' OR COLUMN_NAME LIKE '%ESTATUS%'
        OR COLUMN_NAME LIKE '%ACTIV%' OR COLUMN_NAME LIKE '%FECHA%' OR COLUMN_NAME LIKE '%VIGEN%')
 ORDER BY TABLE_NAME, COLUMN_ID;

-- 3. Tablas o vistas del esquema cuyo nombre hable de bajas
SELECT OBJECT_NAME, OBJECT_TYPE
  FROM USER_OBJECTS
 WHERE OBJECT_TYPE IN ('TABLE', 'VIEW')
   AND (OBJECT_NAME LIKE '%BAJA%' OR OBJECT_NAME LIKE '%BAJAS%' OR OBJECT_NAME LIKE '%MOVIMIENTO%'
        OR OBJECT_NAME LIKE '%ROTACION%')
 ORDER BY OBJECT_NAME;

-- 4. Columnas en cualquier tabla del esquema con "BAJA" en el nombre
SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE
  FROM USER_TAB_COLUMNS
 WHERE COLUMN_NAME LIKE '%BAJA%'
   AND TABLE_NAME NOT LIKE 'SERV_MED%'
 ORDER BY TABLE_NAME, COLUMN_NAME;
