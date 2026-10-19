-- =============================================================================
-- ANTIDOPING: VARIAS SUSTANCIAS POR PRUEBA (13-oct-2026)
--
-- BASE: BIOMETRICO de produccion (PDBPRD). Lo corre el usuario.
--
-- POR QUE: el layout 7 cambio la columna DOPING por 11 columnas (AMF, BARB, BENZO, COC, LSD, MET,
--   OPI, PVC, THC, SOLVENTE, OTRO) con 1 en las que aplican: en los Excel de 2025 y 2026 un tercio
--   de los positivos trae 2 o mas sustancias (hasta 6). El cargador las guarda juntas en SUSTANCIA
--   ("AMF, MET, THC"), y la columna de 20 caracteres no alcanza (las 11 juntas son 63).
--
-- QUE HACE: amplia SERV_MED_XLS_ANTIDOPING.SUSTANCIA de VARCHAR2(20) a VARCHAR2(100), recompila la
--   vista SERV_MED_V_ANTIDOPING que la lee y crea SERV_MED_V_ANTIDOPING_SUSTANCIA (un renglon por
--   prueba y sustancia, para estadistica por sustancia). NO borra ni modifica datos: ampliar una columna VARCHAR2
--   es inmediato y conserva lo que ya hay. Se puede correr mas de una vez.
--
-- ORDEN: correr ANTES de volver a cargar el layout 7 con el cargador nuevo (si no, una prueba con
--   varias sustancias falla con ORA-12899 "value too large").
--
-- REVERSA (solo si no hay valores de mas de 20 caracteres):
--   ALTER TABLE SERV_MED_XLS_ANTIDOPING MODIFY (SUSTANCIA VARCHAR2(20));
-- =============================================================================
SET SERVEROUTPUT ON

ALTER TABLE SERV_MED_XLS_ANTIDOPING MODIFY (SUSTANCIA VARCHAR2(100));

COMMENT ON COLUMN SERV_MED_XLS_ANTIDOPING.SUSTANCIA IS
  'Sustancias a las que salio positivo, separadas por coma (AMF, MET, THC). Del layout 7: una columna por sustancia con 1.';

-- La vista se invalida al cambiar la columna; se recompila para dejarla VALID de una vez
ALTER VIEW SERV_MED_V_ANTIDOPING COMPILE;

-- Vista para estadistica por sustancia: UN renglon por prueba y sustancia ("AMF, COC, MET" = 3
-- renglones). Para contar PRUEBAS o POSITIVOS se usa SERV_MED_V_ANTIDOPING (un renglon por prueba).
CREATE OR REPLACE VIEW SERV_MED_V_ANTIDOPING_SUSTANCIA AS
SELECT e.FUENTE, e.ID_ORIGEN, e.NSS, e.FECHA, e.PREDIO, e.CUENTA, e.AREA, e.TIPO_PRUEBA, e.RESULTADO,
       s.SUSTANCIA
  FROM SERV_MED_V_ANTIDOPING e
  JOIN (SELECT 'AMF' SUSTANCIA FROM dual UNION ALL SELECT 'BARB' FROM dual UNION ALL
        SELECT 'BENZO' FROM dual UNION ALL SELECT 'COC' FROM dual UNION ALL SELECT 'LSD' FROM dual UNION ALL
        SELECT 'MET' FROM dual UNION ALL SELECT 'OPI' FROM dual UNION ALL SELECT 'PVC' FROM dual UNION ALL
        SELECT 'THC' FROM dual UNION ALL SELECT 'SOLVENTE' FROM dual UNION ALL SELECT 'OTRO' FROM dual) s
    ON ', ' || e.SUSTANCIA || ',' LIKE '%, ' || s.SUSTANCIA || ',%';

COMMENT ON TABLE SERV_MED_V_ANTIDOPING_SUSTANCIA IS 'Antidoping: un renglon por prueba y sustancia, para estadistica por sustancia. Las pruebas se cuentan en SERV_MED_V_ANTIDOPING.';

-- VERIFICACION (solo lectura): debe salir DATA_LENGTH = 100 y las dos vistas VALID
SELECT column_name, data_type, data_length
  FROM user_tab_columns
 WHERE table_name = 'SERV_MED_XLS_ANTIDOPING' AND column_name = 'SUSTANCIA';

SELECT object_name, status FROM user_objects
 WHERE object_name IN ('SERV_MED_V_ANTIDOPING', 'SERV_MED_V_ANTIDOPING_SUSTANCIA');
