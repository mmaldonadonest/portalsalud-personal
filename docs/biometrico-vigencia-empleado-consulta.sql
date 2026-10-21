-- =============================================================================
-- BASE: BIOMETRICO (la misma del portal), tabla BIO_EMPLEADO. SOLO LECTURA.
-- Para definir como sabe el portal si un firmante sigue activo (modulo de firmas, paso 8):
-- que valor de EMP_STATUS es "activo" y si hay una fecha de baja que sirva de dato extra.
-- Lo corre el usuario (20-oct-2026). No modifica nada.
-- RESULTADO 20-oct-2026: los 4 NSS de la consulta 2 son personal ACTIVO y todos tienen EMP_STATUS = 0
-- (el usuario lo confirmo: 0 = activo). EMP_FECHAN y EMP_FECHAI vienen vacias: la fecha no sirve de dato extra.
-- (90099119373 y 17189842515 tambien estan activos; la etiqueta "BAJA (sabido)" de abajo quedo equivocada.)
-- =============================================================================

-- 1) Columnas de fecha de la tabla (buscar una de baja o termino, ademas de EMP_FECHAI = ingreso)
SELECT COLUMN_NAME, DATA_TYPE
  FROM ALL_TAB_COLUMNS
 WHERE TABLE_NAME = 'BIO_EMPLEADO'
   AND (DATA_TYPE LIKE 'DATE%' OR DATA_TYPE LIKE 'TIMESTAMP%' OR COLUMN_NAME LIKE '%FEC%' OR COLUMN_NAME LIKE '%BAJA%')
 ORDER BY COLUMN_NAME;

-- 2) Activos que SABEMOS: el Dr. Ceron (68958027838) y el usuario (30048315698).
--    Dadas de baja que SABEMOS (dadas por el usuario 20-oct-2026): 90099119373 y 17189842515.
SELECT EMP_NSS,
       CASE WHEN EMP_NSS IN ('68958027838', '30048315698') THEN 'ACTIVO (sabido)' ELSE 'BAJA (sabido)' END ESPERADO,
       EMP_STATUS, EMP_FECHAI
  FROM BIO_EMPLEADO
 WHERE EMP_NSS IN ('68958027838', '30048315698', '90099119373', '17189842515')
 ORDER BY 2, EMP_NSS;

-- 3) Ingreso mas reciente por estatus: el estatus de los activos deberia tener ingresos recientes
SELECT EMP_STATUS, COUNT(*) TOTAL, MIN(EMP_FECHAI) INGRESO_MAS_VIEJO, MAX(EMP_FECHAI) INGRESO_MAS_RECIENTE
  FROM BIO_EMPLEADO
 GROUP BY EMP_STATUS
 ORDER BY EMP_STATUS;

-- 4) NSS repetidos y sus estatus (un recontratado podria tener un registro de baja y uno activo)
SELECT EMP_NSS, LISTAGG(EMP_STATUS, ',') WITHIN GROUP (ORDER BY EMP_STATUS) ESTATUS, COUNT(*) REGISTROS
  FROM BIO_EMPLEADO
 GROUP BY EMP_NSS
HAVING COUNT(*) > 1
 FETCH FIRST 20 ROWS ONLY;
