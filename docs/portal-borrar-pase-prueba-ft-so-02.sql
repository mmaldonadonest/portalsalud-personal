-- =============================================================================
-- BASE: PORTAL (JDBC, dentro de BIOMETRICO), tabla SERV_MED_TAG
-- Borra los datos del pase de salida de la consulta de prueba del 20-oct-2026 (NSS 30048315698,
-- relacion 67e5ba9c3eff4a3aac78ed0184cb808a) usada para revisar el FT-SO-02. La consulta va
-- aparte: ords-borrar-consulta-prueba-ft-so-02.sql (base de ORDS).
-- Lo corre el usuario. Revisar el SELECT antes del DELETE.
-- =============================================================================

-- 1) Deben salir 9 filas: ENTRADA, FECHA_CITA, INCAP_DIAS, INCAP_INICIA, INCAP_TERMINA, MOTIVO,
--    OBSERVACIONES, REGRESA, SALIDA (todas con prefijo FTSO02.67e5ba9c3eff4a3aac78ed0184cb808a.)
SELECT ID, NSS, TYPE, CONTENT, TAG_GROUP, CREATED_BY, CREATED_AT
  FROM SERV_MED_TAG
 WHERE NSS = '30048315698'
   AND TYPE LIKE 'FTSO02.67e5ba9c3eff4a3aac78ed0184cb808a.%'
   AND TAG_GROUP = 'PASE_SALIDA_FTSO02'
 ORDER BY TYPE;

-- 2) Borrado (mismas condiciones)
DELETE FROM SERV_MED_TAG
 WHERE NSS = '30048315698'
   AND TYPE LIKE 'FTSO02.67e5ba9c3eff4a3aac78ed0184cb808a.%'
   AND TAG_GROUP = 'PASE_SALIDA_FTSO02';

-- 3) Si el numero de filas eliminadas es el mismo que el del SELECT:
COMMIT;
