-- =============================================================================
-- E-024 (9-nov-2026): CORRECCION en PR_SERVICIO_MED_EXAMEN1_V2 (ASTIGMATISMO)
-- =============================================================================
-- BASE    : BIOMETRICO de PRODUCCION. Correr CONECTADO COMO BIOMETRICO. Lo corre el usuario.
-- Requisito: haber corrido e024-bitacora-examen-medico-v2.sql (crea el _V2 y Servcio/Medico_v2).
--
-- CAUSA (bitacora SERV_MED_LOG_PROC, ID 1, trabajador de nuevo ingreso):
--   ORA-01722 numero no valido en PR_SERVICIO_MED_EXAMEN1_V2 linea 266 = INSERT INTO SERV_MED_OFTALMOLOGICO.
--   En el VALUES, la columna ASTIGMATISMO (numerica) recibe cvar3 (el texto de PRESBICIA_OBS) en vez
--   de kvar3:   values (knns,kvar2,cvar1,cvar3,cvar2,...)   ->   values (knns,kvar2,cvar1,kvar3,cvar2,...)
--   Solo truena la PRIMERA vez que un NSS guarda Oftalmologico (INSERT) y si PRESBICIA_OBS trae texto
--   (p. ej. "NEGADOS"). El UPDATE esta bien. El mismo error esta en el original PR_SERVICIO_MED_EXAMEN1.
--
-- QUE HACE: recompila PR_SERVICIO_MED_EXAMEN1_V2 desde su codigo vivo cambiando SOLO ese cvar3 por
-- kvar3 (debe aparecer exactamente una vez; si no, se detiene sin tocar nada).
-- QUE NO HACE: no toca PR_SERVICIO_MED_EXAMEN1 (original) ni Servcio/Medico. No cambia datos.
--
-- Reversa: volver a correr el bloque 2 de e024-bitacora-examen-medico-v2.sql (vuelve a copiar el original).
--
-- Correr con:  @C:\miguel\portalsalud-personal\docs\script-prod\e024-corrige-examen1-v2-astigmatismo.sql
-- =============================================================================
SET SERVEROUTPUT ON SIZE UNLIMITED
WHENEVER SQLERROR EXIT ROLLBACK

DECLARE
  c_patron CONSTANT VARCHAR2(200) := '\(\s*knns\s*,\s*kvar2\s*,\s*cvar1\s*,\s*cvar3\s*,\s*cvar2\s*,';
  v_src    CLOB;
  v_nuevo  CLOB;
  v_estado VARCHAR2(20);
  n        NUMBER;
BEGIN
  DBMS_LOB.CREATETEMPORARY(v_src, TRUE);
  FOR l IN (SELECT TEXT FROM USER_SOURCE WHERE NAME = 'PR_SERVICIO_MED_EXAMEN1_V2' AND TYPE = 'PROCEDURE' ORDER BY LINE) LOOP
    DBMS_LOB.APPEND(v_src, l.TEXT);
  END LOOP;
  IF DBMS_LOB.GETLENGTH(v_src) = 0 THEN
    RAISE_APPLICATION_ERROR(-20030, 'No existe PR_SERVICIO_MED_EXAMEN1_V2: correr antes e024-bitacora-examen-medico-v2.sql. No se hizo nada.');
  END IF;
  n := REGEXP_COUNT(v_src, c_patron, 1, 'i');
  IF n <> 1 THEN
    RAISE_APPLICATION_ERROR(-20031, 'Se esperaba 1 VALUES con "cvar1,cvar3,cvar2" y hay ' || n || '. No se hizo nada.');
  END IF;
  v_nuevo := REGEXP_REPLACE(v_src, c_patron, '(knns,kvar2,cvar1,kvar3,cvar2,', 1, 1, 'i');
  EXECUTE IMMEDIATE 'CREATE OR REPLACE ' || v_nuevo;
  SELECT STATUS INTO v_estado FROM USER_OBJECTS WHERE OBJECT_NAME = 'PR_SERVICIO_MED_EXAMEN1_V2' AND OBJECT_TYPE = 'PROCEDURE';
  DBMS_OUTPUT.PUT_LINE('PR_SERVICIO_MED_EXAMEN1_V2 corregido (ASTIGMATISMO = kvar3): ' || v_estado);
  IF v_estado <> 'VALID' THEN
    RAISE_APPLICATION_ERROR(-20032, 'Quedo INVALIDO: revisar USER_ERRORS.');
  END IF;
END;
/

PROMPT === Verificacion: el VALUES del INSERT de oftalmologico ya dice kvar3 ===
SELECT LINE, TEXT FROM USER_SOURCE
 WHERE NAME = 'PR_SERVICIO_MED_EXAMEN1_V2' AND TYPE = 'PROCEDURE'
   AND REGEXP_LIKE(TEXT, 'knns\s*,\s*kvar2\s*,\s*cvar1', 'i');
WHENEVER SQLERROR CONTINUE
