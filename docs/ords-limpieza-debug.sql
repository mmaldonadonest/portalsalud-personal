-- =====================================================================================
-- ords-limpieza-debug.sql  —  ORDS / esquema legacy BIOMETRICO@PDBPRD
-- Vaciado de las tablas de DEPURACION de los handlers PL/SQL. Nada de datos de negocio.
--
-- Decision del usuario (23-sep-2026): de la limpieza previa a produccion SOLO se tocan las
-- tablas de paso / debug. Los registros por NSS (consultas, incapacidades, examen, y las
-- tablas de modulos del portal) NO se borran por ahora: el usuario los depurara a mano
-- cuando lo decida. Ver docs/ords-limpieza-datos-prueba.sql, que queda EN PAUSA.
--
-- Conteos al 23-sep-2026:
--   BUG          299,683 filas   <- 'insert into bug' de los handlers, es basura de depuracion
--   PRUEBA        11,355 filas
--   ONSYS_DEBUG      290 filas
--
-- Estas tres tablas no las lee ninguna aplicacion: solo las escriben los handlers para
-- depurar. Vaciarlas ademas libera espacio en el tablespace BIOMETRICO (105 GB usados).
--
-- SIN COMMIT AUTOMATICO: revisa la verificacion y decide.
-- Recomendado: respaldo previo del esquema o al menos de estas tres tablas.
-- =====================================================================================

-- ---------- Antes: cuanto hay ----------
SELECT 'BUG' tabla, COUNT(*) filas FROM BUG
UNION ALL SELECT 'PRUEBA', COUNT(*) FROM PRUEBA
UNION ALL SELECT 'ONSYS_DEBUG', COUNT(*) FROM ONSYS_DEBUG;

-- Una mirada al contenido, por si algo no fuera basura (deberian ser trazas de los handlers)
SELECT * FROM BUG         WHERE ROWNUM <= 5;
SELECT * FROM PRUEBA      WHERE ROWNUM <= 5;
SELECT * FROM ONSYS_DEBUG WHERE ROWNUM <= 5;

-- ---------- Vaciado ----------
-- DELETE deja rastro en el UNDO y se puede revertir con ROLLBACK (por eso no se usa TRUNCATE).
-- Con 300 mil filas puede tardar; si se prefiere velocidad y NO se necesita rollback,
-- cambiar por TRUNCATE TABLE (ojo: TRUNCATE es DDL, hace COMMIT implicito y no se revierte).
DELETE FROM BUG;
DELETE FROM PRUEBA;
DELETE FROM ONSYS_DEBUG;

-- ---------- Despues: debe dar 0 en las tres ----------
SELECT 'BUG' tabla, COUNT(*) filas FROM BUG
UNION ALL SELECT 'PRUEBA', COUNT(*) FROM PRUEBA
UNION ALL SELECT 'ONSYS_DEBUG', COUNT(*) FROM ONSYS_DEBUG;

-- Si todo cuadra:
-- COMMIT;
-- Si algo no cuadra:
-- ROLLBACK;
