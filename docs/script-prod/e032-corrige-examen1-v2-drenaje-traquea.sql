-- =============================================================================
-- E-032 (11-nov-2026): CORRECCION en los clones del portal
--   PR_SERVICIO_MED_EXAMEN1_V2 (DRENAJE / CONVIVENCIA)  y  PR_SERVICIO_MED_EXAMEN2_V2 (TRAQUEA_OBS)
-- =============================================================================
-- BASE    : BIOMETRICO de PRODUCCION. Correr CONECTADO COMO BIOMETRICO. Lo corre el usuario.
-- Requisito: existen los dos clones (los creo e024-bitacora-examen-medico-v2.sql).
--
-- CAUSA (prueba E-032 del 11-nov con el NSS de prueba: capturar, finalizar y volver a abrir):
--   1) EXAMEN1_V2, Vivienda: el JSON de CONVIVENCIA_CON_ANIMALES se lee en cvar8, la misma variable que
--      DRENAJE, y la columna CONVIVENCIA_CON_ANIMALES recibe cvar9, que nunca se asigna.
--      Efecto: DRENAJE guarda lo de Convivencia y Convivencia queda vacia.
--      Arreglo:  cvar8 := ...'SERV_MED_ANT_PATOLOGICOS.CONVIVENCIA_CON_ANIMALES'  ->  cvar9 := ...
--   2) EXAMEN2_V2, Cuello: el JSON de TRAQUEA_OBS se lee en cvar4, pero el INSERT y el UPDATE guardan CVAR3,
--      que trae lo que haya quedado de una seccion anterior.
--      Efecto: la observacion de traquea se pierde y en su lugar queda un texto de otra seccion.
--      Arreglo:  cvar4 := ...'SERV_MED_CUELLO.TRAQUEA_OBS'  ->  cvar3 := ...
--   Los mismos errores estan en los originales PR_SERVICIO_MED_EXAMEN1/2 (los usa el PHP); este script NO
--   los toca.
--
-- PASO 1, RESPALDO (antes de cambiar nada): el codigo vivo de cada clon se guarda en C:\miguel\respaldosql\
--     PR_SERVICIO_MED_EXAMEN1_V2_antes_e032_11nov2026.sql
--     PR_SERVICIO_MED_EXAMEN2_V2_antes_e032_11nov2026.sql
--   como CREATE OR REPLACE ejecutable. REVISAR que los dos archivos existan y terminen en «/».
--   Si SQL Developer dice que no pudo crear un archivo (la carpeta no existe), DETENER y no seguir.
-- PASO 2, CORRECCION: recompila cada clon desde su codigo vivo cambiando SOLO esa variable. Primero revisa
--   los dos: si alguno no aparece exactamente una vez, se detiene sin tocar NINGUNO.
-- QUE NO HACE: no toca PR_SERVICIO_MED_EXAMEN1/2 (originales) ni Servcio/Medico. No cambia datos ya
--   guardados: los examenes de antes siguen con Drenaje/Convivencia y Traquea como quedaron.
--
-- Reversa: correr los dos respaldos (F5):
--   @C:\miguel\respaldosql\PR_SERVICIO_MED_EXAMEN1_V2_antes_e032_11nov2026.sql
--   @C:\miguel\respaldosql\PR_SERVICIO_MED_EXAMEN2_V2_antes_e032_11nov2026.sql
--
-- Correr con F5 (como script) o:  @C:\miguel\portalsalud-personal\docs\script-prod\e032-corrige-examen1-v2-drenaje-traquea.sql
-- =============================================================================
SET SERVEROUTPUT ON SIZE UNLIMITED
WHENEVER SQLERROR EXIT ROLLBACK

-- ---------- PASO 1: RESPALDO de los dos clones tal como estan hoy ----------
SET PAGESIZE 0
SET LINESIZE 4000
SET LONG 100000
SET TRIMSPOOL ON
SET FEEDBACK OFF
SET HEADING OFF
SET VERIFY OFF
SET ECHO OFF
SPOOL C:\miguel\respaldosql\PR_SERVICIO_MED_EXAMEN1_V2_antes_e032_11nov2026.sql
SELECT '-- RESPALDO de PR_SERVICIO_MED_EXAMEN1_V2 (base BIOMETRICO de produccion), '
       || TO_CHAR(SYSDATE, 'DD-MM-YYYY HH24:MI') || ', antes de e032-corrige-examen1-v2-drenaje-traquea.sql'
  FROM DUAL;
SELECT '-- Restaurar: conectado como BIOMETRICO, correr este archivo completo (F5).' FROM DUAL;
SELECT CASE WHEN LINE = 1 THEN 'CREATE OR REPLACE ' END || RTRIM(TEXT, CHR(10) || CHR(13))
  FROM USER_SOURCE
 WHERE NAME = 'PR_SERVICIO_MED_EXAMEN1_V2' AND TYPE = 'PROCEDURE'
 ORDER BY LINE;
SELECT '/' FROM DUAL;
SPOOL OFF
SPOOL C:\miguel\respaldosql\PR_SERVICIO_MED_EXAMEN2_V2_antes_e032_11nov2026.sql
SELECT '-- RESPALDO de PR_SERVICIO_MED_EXAMEN2_V2 (base BIOMETRICO de produccion), '
       || TO_CHAR(SYSDATE, 'DD-MM-YYYY HH24:MI') || ', antes de e032-corrige-examen1-v2-drenaje-traquea.sql'
  FROM DUAL;
SELECT '-- Restaurar: conectado como BIOMETRICO, correr este archivo completo (F5).' FROM DUAL;
SELECT CASE WHEN LINE = 1 THEN 'CREATE OR REPLACE ' END || RTRIM(TEXT, CHR(10) || CHR(13))
  FROM USER_SOURCE
 WHERE NAME = 'PR_SERVICIO_MED_EXAMEN2_V2' AND TYPE = 'PROCEDURE'
 ORDER BY LINE;
SELECT '/' FROM DUAL;
SPOOL OFF
SET PAGESIZE 50
SET HEADING ON
SET FEEDBACK ON

PROMPT === Respaldo: renglones guardados de cada clon (los dos deben ser mayores que 0) ===
SELECT NAME, COUNT(*) RENGLONES_RESPALDADOS FROM USER_SOURCE
 WHERE NAME IN ('PR_SERVICIO_MED_EXAMEN1_V2', 'PR_SERVICIO_MED_EXAMEN2_V2') AND TYPE = 'PROCEDURE'
 GROUP BY NAME ORDER BY NAME;
PROMPT Archivos en C:\miguel\respaldosql\

-- ---------- PASO 2: correccion ----------
DECLARE
  c_conv CONSTANT VARCHAR2(300) :=
    'cvar8(\s*:=\s*apex_json\.get_varchar2\s*\(\s*p_path\s*=>\s*''SERV_MED_ANT_PATOLOGICOS\.CONVIVENCIA_CON_ANIMALES'')';
  c_traq CONSTANT VARCHAR2(300) :=
    'cvar4(\s*:=\s*apex_json\.get_varchar2\s*\(\s*p_path\s*=>\s*''SERV_MED_CUELLO\.TRAQUEA_OBS'')';
  v_src1 CLOB;
  v_src2 CLOB;
  n1     NUMBER;
  n2     NUMBER;

  FUNCTION fuente(p_nombre VARCHAR2) RETURN CLOB IS
    v CLOB;
  BEGIN
    DBMS_LOB.CREATETEMPORARY(v, TRUE);
    FOR l IN (SELECT TEXT FROM USER_SOURCE WHERE NAME = p_nombre AND TYPE = 'PROCEDURE' ORDER BY LINE) LOOP
      DBMS_LOB.APPEND(v, l.TEXT);
    END LOOP;
    IF DBMS_LOB.GETLENGTH(v) = 0 THEN
      RAISE_APPLICATION_ERROR(-20040, 'No existe ' || p_nombre || '. No se hizo nada.');
    END IF;
    RETURN v;
  END;

  PROCEDURE recompilar(p_nombre VARCHAR2, p_nuevo CLOB) IS
    v_estado VARCHAR2(20);
  BEGIN
    EXECUTE IMMEDIATE 'CREATE OR REPLACE ' || p_nuevo;
    SELECT STATUS INTO v_estado FROM USER_OBJECTS WHERE OBJECT_NAME = p_nombre AND OBJECT_TYPE = 'PROCEDURE';
    DBMS_OUTPUT.PUT_LINE(p_nombre || ' corregido: ' || v_estado);
    IF v_estado <> 'VALID' THEN
      RAISE_APPLICATION_ERROR(-20042, p_nombre || ' quedo INVALIDO: revisar USER_ERRORS y restaurar su respaldo.');
    END IF;
  END;
BEGIN
  v_src1 := fuente('PR_SERVICIO_MED_EXAMEN1_V2');
  v_src2 := fuente('PR_SERVICIO_MED_EXAMEN2_V2');
  n1 := REGEXP_COUNT(v_src1, c_conv, 1, 'i');
  n2 := REGEXP_COUNT(v_src2, c_traq, 1, 'i');
  DBMS_OUTPUT.PUT_LINE('Encontrados: EXAMEN1_V2 CONVIVENCIA en cvar8 = ' || n1
                       || ', EXAMEN2_V2 TRAQUEA_OBS en cvar4 = ' || n2 || ' (se espera 1 y 1)');
  IF n1 <> 1 OR n2 <> 1 THEN
    RAISE_APPLICATION_ERROR(-20041, 'No coincide con lo esperado (ya corregido o el codigo cambio). No se hizo nada.');
  END IF;
  recompilar('PR_SERVICIO_MED_EXAMEN1_V2', REGEXP_REPLACE(v_src1, c_conv, 'cvar9\1', 1, 1, 'i'));
  recompilar('PR_SERVICIO_MED_EXAMEN2_V2', REGEXP_REPLACE(v_src2, c_traq, 'cvar3\1', 1, 1, 'i'));
END;
/

PROMPT === Verificacion: las dos lecturas ya usan cvar9 y cvar3 ===
SELECT NAME, LINE, TRIM(TEXT) TEXTO FROM USER_SOURCE
 WHERE NAME IN ('PR_SERVICIO_MED_EXAMEN1_V2', 'PR_SERVICIO_MED_EXAMEN2_V2') AND TYPE = 'PROCEDURE'
   AND (INSTR(TEXT, 'CONVIVENCIA_CON_ANIMALES''') > 0 OR INSTR(TEXT, 'TRAQUEA_OBS''') > 0)
 ORDER BY NAME, LINE;
WHENEVER SQLERROR CONTINUE
