-- ============================================================================================
-- ¿Por que el FT-SO-05 (cuestionario osteomuscular) del EMNI sale vacio en el impreso? - 17-nov-2026
--
-- Base: BIOMETRICO (PDBPRD), conectado como BIOMETRICO. Tablas del PORTAL. NO va a ORDS.
-- SOLO LECTURA: no inserta, no borra, no cambia nada.
--
-- El cuestionario se guarda como JSON en el campo TAG.EXM_OSTEO del borrador (SERV_MED_EXAMEN_BORRADOR.DATOS)
-- y al finalizar pasa a SERV_MED_TAG (TYPE = 'EXM_OSTEO'), de donde lo lee el impreso.
--   - Si 1) no trae EXM_OSTEO: nunca se capturo en el borrador (el «Todo normal» que se marco fue otro).
--   - Si 1) lo trae pero 2) no: se perdio al finalizar (bug del portal).
--
-- Correr con:  @C:\miguel\portalsalud-personal\docs\script-prod\diagnostico-osteomuscular-emni.sql
-- ============================================================================================
SET PAGESIZE 100
SET LINESIZE 220
SET LONG 400
-- El NSS se pide al correr (no se versionan NSS reales)
ACCEPT NSS CHAR PROMPT 'NSS a revisar: '

PROMPT === 1) Ultimos borradores del EMNI: ¿traen TAG.EXM_OSTEO? (largo y primeros 120 caracteres) ===
COLUMN OSTEO FORMAT A120
SELECT ID, ESTATUS, TO_CHAR(UPDATED_AT, 'DD/MM HH24:MI:SS') ACTUALIZADO, TO_CHAR(FINALIZADO_AT, 'DD/MM HH24:MI:SS') FINALIZADO,
       DBMS_LOB.INSTR(DATOS, '"TAG.EXM_OSTEO"') POSICION,
       LENGTH(JSON_VALUE(DATOS, '$."TAG.EXM_OSTEO"' RETURNING VARCHAR2(4000) NULL ON ERROR)) LARGO,
       SUBSTR(JSON_VALUE(DATOS, '$."TAG.EXM_OSTEO"' RETURNING VARCHAR2(4000) NULL ON ERROR), 1, 120) OSTEO
  FROM SERV_MED_EXAMEN_BORRADOR
 WHERE NSS = '&NSS'
 ORDER BY ID DESC
 FETCH FIRST 5 ROWS ONLY;

PROMPT === 2) Tag EXM_OSTEO en SERV_MED_TAG (lo que lee el impreso) ===
SELECT ID, TO_CHAR(CREATED_AT, 'DD/MM HH24:MI:SS') CREADO, CREATED_BY, DBMS_LOB.GETLENGTH(CONTENT) LARGO,
       DBMS_LOB.SUBSTR(CONTENT, 120, 1) OSTEO
  FROM SERV_MED_TAG
 WHERE NSS = '&NSS' AND TYPE = 'EXM_OSTEO'
 ORDER BY ID DESC;
