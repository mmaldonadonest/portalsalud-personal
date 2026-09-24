-- =============================================================================
-- FIX: SERV_MED_UX_FS_FILE_CHECKSUM de UNIQUE a NONUNIQUE (QA ONEWMS_QA)
-- =============================================================================
-- Destino : QA (ONEWMS_QA @ 200.94.116.132:1521/orclpdb) - NO tocar en local
--           (ahi ya esta NONUNIQUE) ni en ninguna otra instancia.
-- Fecha   : 25 de agosto de 2026
-- Motivo  : docs/etl-migracion-historica-especificacion.md seccion 4 punto 7.
--           SERV_MED_FS_FILE.CHECKSUM_SHA256 tiene 261 duplicados de contenido
--           legitimos (mismo PDF adjuntado a varios NSS/consultas) - con el
--           indice como UNIQUE, la migracion historica fallaria con ORA-00001
--           en el primer duplicado. Confirmado que es un indice suelto, SIN
--           constraint que lo respalde (verificado contra ALL_CONSTRAINTS,
--           2026-08-25) - el DROP/CREATE directo es seguro.
-- =============================================================================

DROP INDEX SERV_MED_UX_FS_FILE_CHECKSUM;

CREATE INDEX SERV_MED_UX_FS_FILE_CHECKSUM ON SERV_MED_FS_FILE (CHECKSUM_SHA256);

-- Verificacion (debe mostrar NONUNIQUE):
-- SELECT INDEX_NAME, UNIQUENESS FROM ALL_INDEXES
--  WHERE TABLE_NAME = 'SERV_MED_FS_FILE' AND INDEX_NAME = 'SERV_MED_UX_FS_FILE_CHECKSUM';
