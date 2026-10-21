-- =============================================================================
-- BASE: PORTAL (JDBC, dentro de BIOMETRICO), tabla SERV_MED_TAG
-- Borra los datos de cadena de custodia de la prueba 62 del NSS 30048315698 (antidoping de
-- prueba del 20-oct-2026 para revisar el FT-SO-14). El registro de antidoping va aparte:
-- ords-borrar-antidoping-prueba-ft-so-14.sql (base de ORDS).
-- Lo corre el usuario. Revisar el SELECT antes del DELETE.
-- =============================================================================

-- 1) Deben salir 12 filas: DIAGNOSTICO, ELEMENTOS, HORA, IDENT_EMITIDA, IDENT_NUMERO, IDENT_TIPO,
--    LUGAR, MEDICAMENTO, RAZON, RECETADO_POR, RESPONSABLE, TRATAMIENTO (todas con prefijo FTSO14.62.)
SELECT ID, NSS, TYPE, CONTENT, TAG_GROUP, CREATED_BY, CREATED_AT
  FROM SERV_MED_TAG
 WHERE NSS = '30048315698'
   AND TYPE LIKE 'FTSO14.62.%'
   AND TAG_GROUP = 'CUSTODIA_FTSO14'
 ORDER BY TYPE;

-- 2) Borrado (mismas condiciones)
DELETE FROM SERV_MED_TAG
 WHERE NSS = '30048315698'
   AND TYPE LIKE 'FTSO14.62.%'
   AND TAG_GROUP = 'CUSTODIA_FTSO14';

-- 3) Si el numero de filas eliminadas es el mismo que el del SELECT:
COMMIT;
