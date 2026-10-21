-- =============================================================================
-- BASE: PORTAL (JDBC, dentro de BIOMETRICO)
-- Borra lo que crearon las pruebas del 20 y 21-oct-2026 (Tomcat local, llave de PRUEBA):
--   A) Modulo de firmas: las firmas del usuario del Dr. (68958027838), cifradas con la llave LOCAL y
--      guardadas en el disco de la computadora de desarrollo (en produccion no sirven), y sus archivos.
--   B) Autores y folios de los documentos del NSS 30048315698 impresos o creados en las pruebas.
--   C) Datos de formatos de la consulta de prueba del 21-oct (relacion 543a5826...):
--      FT-SO-29 (dos medicamentos) y FT-SO-23 (pruebas especiales).
--   D) Los 2 dictamenes FT-SO-31 de prueba del NSS 30048315698.
-- La consulta en si va aparte: ords-borrar-consulta-prueba-21oct.sql (base de ORDS).
-- NO toca la bitacora (SERV_MED_AUD_EVENT): ahi queda el rastro de las pruebas.
-- Lo corre el usuario. Revisar los SELECT antes de los DELETE.
-- =============================================================================

-- 1) Revision: cuantas filas se van a borrar
SELECT 'A SERV_MED_FIRMA' QUE, COUNT(*) FILAS FROM SERV_MED_FIRMA WHERE USUARIO = '68958027838'
UNION ALL
SELECT 'A SERV_MED_FS_FILE (firmas)', COUNT(*) FROM SERV_MED_FS_FILE WHERE FILE_TYPE = 'firma_digitalizada' AND NSS = '68958027838'
UNION ALL
SELECT 'B SERV_MED_DOC_AUTOR', COUNT(*) FROM SERV_MED_DOC_AUTOR WHERE NSS = '30048315698' AND CREATED_AT >= DATE '2026-10-20'
UNION ALL
SELECT 'B SERV_MED_FIRMA_IMPRESO', COUNT(*) FROM SERV_MED_FIRMA_IMPRESO WHERE NSS = '30048315698' AND IMPRESO_AT >= DATE '2026-10-20'
UNION ALL
SELECT 'C SERV_MED_TAG (FT-SO-29 y FT-SO-23)', COUNT(*) FROM SERV_MED_TAG
 WHERE NSS = '30048315698' AND (TYPE LIKE 'FTSO29.543a5826778f46bb8aebec3e9f378738.%' OR TYPE LIKE 'FTSO23.543a5826778f46bb8aebec3e9f378738.%')
UNION ALL
SELECT 'D SERV_MED_TAG (FT-SO-31)', COUNT(*) FROM SERV_MED_TAG
 WHERE NSS = '30048315698' AND TYPE LIKE 'FTSO31.%' AND TAG_GROUP = 'CERTIFICACION_FTSO31';

-- Detalle (para ver que todo sea de las pruebas)
SELECT ID, USUARIO, ORIGEN, ESTADO, AVISO_VERSION, CREATED_AT, CREATED_BY FROM SERV_MED_FIRMA WHERE USUARIO = '68958027838' ORDER BY ID;
SELECT REGISTRO, ELABORO_NOMBRE, ORIGEN, CREATED_AT FROM SERV_MED_DOC_AUTOR WHERE NSS = '30048315698' AND CREATED_AT >= DATE '2026-10-20';
SELECT FOLIO, CODIGO, FORMATO, REGISTRO, IMPRESO_POR, IMPRESO_AT FROM SERV_MED_FIRMA_IMPRESO
 WHERE NSS = '30048315698' AND IMPRESO_AT >= DATE '2026-10-20' ORDER BY FOLIO;

-- 2) Borrado
DELETE FROM SERV_MED_FIRMA_IMPRESO WHERE NSS = '30048315698' AND IMPRESO_AT >= DATE '2026-10-20';
DELETE FROM SERV_MED_DOC_AUTOR WHERE NSS = '30048315698' AND CREATED_AT >= DATE '2026-10-20';
DELETE FROM SERV_MED_FIRMA WHERE USUARIO = '68958027838';
DELETE FROM SERV_MED_FS_FILE WHERE FILE_TYPE = 'firma_digitalizada' AND NSS = '68958027838';
DELETE FROM SERV_MED_TAG
 WHERE NSS = '30048315698' AND (TYPE LIKE 'FTSO29.543a5826778f46bb8aebec3e9f378738.%' OR TYPE LIKE 'FTSO23.543a5826778f46bb8aebec3e9f378738.%');
DELETE FROM SERV_MED_TAG WHERE NSS = '30048315698' AND TYPE LIKE 'FTSO31.%' AND TAG_GROUP = 'CERTIFICACION_FTSO31';

-- 3) Si los conteos de los DELETE coinciden con los del paso 1:
COMMIT;
