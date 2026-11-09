-- =============================================================================
-- PUESTO CORTADO EN LA FICHA (9-nov-2026): "CAPTURISTA DE S" (reporte de SM)
-- BASE: BIOMETRICO de produccion. Lo corre el usuario. SOLO LECTURA (SELECT).
--
-- El portal muestra tal cual "nombre_puesto" del WS Catalogo/usuario, y el WS lo toma de
-- BIOMETRICO_PUESTO.PUESTO_NOMBRE (variable de 50 caracteres: el WS no lo corta). Si aqui ya sale
-- cortado, el dato viene asi del biometrico y se corrige en el catalogo de puestos, no en el portal.
--
-- Correr con:  @C:\miguel\portalsalud-personal\docs\script-prod\revisa-puestos-cortados.sql
-- =============================================================================
SET LINESIZE 200
COLUMN PUESTO_NOMBRE FORMAT A60

PROMPT === 1) Tamano de la columna del nombre del puesto ===
SELECT COLUMN_NAME, DATA_TYPE, DATA_LENGTH, CHAR_LENGTH
  FROM USER_TAB_COLUMNS
 WHERE TABLE_NAME = 'BIOMETRICO_PUESTO' AND COLUMN_NAME = 'PUESTO_NOMBRE';

PROMPT === 2) Puestos que empiezan con CAPTURISTA (como estan guardados) ===
SELECT PUESTO_ID, PUESTO_NOMBRE, LENGTH(PUESTO_NOMBRE) LARGO
  FROM BIOMETRICO_PUESTO
 WHERE UPPER(PUESTO_NOMBRE) LIKE 'CAPTURISTA%'
 ORDER BY PUESTO_NOMBRE;

PROMPT === 3) Cuantos puestos llegan justo al largo maximo (probablemente cortados) ===
SELECT LENGTH(PUESTO_NOMBRE) LARGO, COUNT(*) PUESTOS
  FROM BIOMETRICO_PUESTO
 GROUP BY LENGTH(PUESTO_NOMBRE)
 ORDER BY LARGO DESC
 FETCH FIRST 5 ROWS ONLY;
