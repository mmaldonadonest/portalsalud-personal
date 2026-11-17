-- ============================================================================================
-- Osteomuscular del EMNI, parte 2 - 17-nov-2026
--
-- Base: BIOMETRICO (PDBPRD), conectado como BIOMETRICO. Tabla del PORTAL. NO va a ORDS.
-- SOLO LECTURA.
--
-- La parte 1 mostro que la clave TAG.EXM_OSTEO existe en los borradores pero JSON_VALUE la devolvio vacia
-- (o vacia de verdad, o mas de 4000 caracteres escapados). Aqui se lee el texto crudo que sigue a la clave.
--   "TAG.EXM_OSTEO":""            -> el cuestionario nunca se capturo en ese borrador
--   "TAG.EXM_OSTEO":"{\"CURV...   -> si se capturo y se perdio al finalizar (bug)
--
-- Correr con:  @C:\miguel\portalsalud-personal\docs\script-prod\diagnostico-osteomuscular-emni-2.sql
-- ============================================================================================
SET PAGESIZE 100
SET LINESIZE 220
COLUMN TEXTO FORMAT A150
-- El NSS se pide al correr (no se versionan NSS reales)
ACCEPT NSS CHAR PROMPT 'NSS a revisar: '

SELECT ID, ESTATUS,
       DBMS_LOB.SUBSTR(DATOS, 150, DBMS_LOB.INSTR(DATOS, '"TAG.EXM_OSTEO"')) TEXTO
  FROM SERV_MED_EXAMEN_BORRADOR
 WHERE NSS = '&NSS'
 ORDER BY ID DESC
 FETCH FIRST 4 ROWS ONLY;
