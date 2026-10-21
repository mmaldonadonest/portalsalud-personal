-- =============================================================================
-- BASE: PORTAL (JDBC, dentro de BIOMETRICO), tabla SERV_MED_TAG
-- Borra los datos de prueba del 20-oct-2026 del NSS 30048315698:
--   FT-SO-29 pase de medicamento  -> FTSO29.0c0256bc93f348028323c5ac656c2171.*  (6 filas)
--   FT-SO-23 pruebas especiales   -> FTSO23.0c0256bc93f348028323c5ac656c2171.*  (5 filas)
--   FT-SO-31 dictamen certificacion -> FTSO31.*                                  (23 filas)
-- La consulta va aparte: ords-borrar-consulta-prueba-ft-so-29-23.sql (base de ORDS).
-- OJO FT-SO-31: borra el dictamen de certificacion del NSS 30048315698; es el de prueba
-- (nunca se habia expedido uno antes: el formato es nuevo del 20-oct-2026).
-- Lo corre el usuario. Revisar el SELECT antes del DELETE.
-- =============================================================================

-- 1) Deben salir 34 filas en total (6 + 5 + 23)
SELECT TAG_GROUP, COUNT(*) FILAS
  FROM SERV_MED_TAG
 WHERE NSS = '30048315698'
   AND (   (TYPE LIKE 'FTSO29.0c0256bc93f348028323c5ac656c2171.%' AND TAG_GROUP = 'PASE_MEDICAMENTO_FTSO29')
        OR (TYPE LIKE 'FTSO23.0c0256bc93f348028323c5ac656c2171.%' AND TAG_GROUP = 'PRUEBAS_ESPECIALES_FTSO23')
        OR (TYPE LIKE 'FTSO31.%' AND TAG_GROUP = 'CERTIFICACION_FTSO31'))
 GROUP BY TAG_GROUP
 ORDER BY TAG_GROUP;

-- 2) Borrado (mismas condiciones)
DELETE FROM SERV_MED_TAG
 WHERE NSS = '30048315698'
   AND (   (TYPE LIKE 'FTSO29.0c0256bc93f348028323c5ac656c2171.%' AND TAG_GROUP = 'PASE_MEDICAMENTO_FTSO29')
        OR (TYPE LIKE 'FTSO23.0c0256bc93f348028323c5ac656c2171.%' AND TAG_GROUP = 'PRUEBAS_ESPECIALES_FTSO23')
        OR (TYPE LIKE 'FTSO31.%' AND TAG_GROUP = 'CERTIFICACION_FTSO31'));

-- 3) Si dijo "34 filas eliminadas":
COMMIT;
