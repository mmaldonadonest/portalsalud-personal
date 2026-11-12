-- =============================================================================
-- E-033 (12-nov-2026): CORRECCION en el clon del portal PR_SERVICIO_MED_EXAMEN1_V2 (Partos / Abortos)
-- =============================================================================
-- BASE    : BIOMETRICO de PRODUCCION. Correr CONECTADO COMO BIOMETRICO. Lo corre el usuario.
--
-- CAUSA: en Gineco-obstetricos el JSON de Abortos (.A) se lee en cvar4 y el de Partos (.P) en cvar5, pero el
--   INSERT y el UPDATE guardan P=cvar4 y A=cvar5. Lo capturado en Partos termina en la columna A y viceversa.
--   El WS de consulta regresa P y A sin voltear, asi que cada "abrir y guardar sin tocar" vuelve a voltear
--   el dato (ping-pong). El PHP tenia el mismo error y tampoco lo compensaba.
-- ARREGLO: se intercambian las dos lecturas:
--     cvar4 := ...'SERV_MED_ANT_GIN_OBSTETRICOS.A'  ->  cvar5 := ...
--     cvar5 := ...'SERV_MED_ANT_GIN_OBSTETRICOS.P'  ->  cvar4 := ...
--   Asi P=cvar4 recibe Partos y A=cvar5 recibe Abortos, en el INSERT y en el UPDATE.
--
-- PASO 1, RESPALDO (antes de cambiar nada): el codigo vivo del clon se guarda en
--     C:\miguel\respaldosql\PR_SERVICIO_MED_EXAMEN1_V2_antes_e033_12nov2026.sql
--   como CREATE OR REPLACE ejecutable. REVISAR que exista y termine en «/».
--   Si SQL Developer dice que no pudo crear el archivo, DETENER y no seguir.
-- PASO 2, CORRECCION: recompila el clon desde su codigo vivo cambiando SOLO esas dos variables. Si alguna no
--   aparece exactamente una vez, se detiene sin tocar nada.
-- QUE NO HACE: no toca el original PR_SERVICIO_MED_EXAMEN1 (PHP, ya desactivado) ni Servcio/Medico. NO cambia
--   datos ya guardados: por el ping-pong no se puede saber cuales renglones estan volteados (ver E-033).
--
-- Reversa: correr el respaldo (F5):
--   @C:\miguel\respaldosql\PR_SERVICIO_MED_EXAMEN1_V2_antes_e033_12nov2026.sql
--
-- Correr con F5 (como script) o:  @C:\miguel\portalsalud-personal\docs\script-prod\e033-corrige-examen1-v2-partos-abortos.sql
-- =============================================================================
SET SERVEROUTPUT ON SIZE UNLIMITED
WHENEVER SQLERROR EXIT ROLLBACK

-- ---------- PASO 1: RESPALDO del clon tal como esta hoy ----------
SET PAGESIZE 0
SET LINESIZE 4000
SET LONG 100000
SET TRIMSPOOL ON
SET FEEDBACK OFF
SET HEADING OFF
SET VERIFY OFF
SET ECHO OFF
SPOOL C:\miguel\respaldosql\PR_SERVICIO_MED_EXAMEN1_V2_antes_e033_12nov2026.sql
SELECT '-- RESPALDO de PR_SERVICIO_MED_EXAMEN1_V2 (base BIOMETRICO de produccion), '
       || TO_CHAR(SYSDATE, 'DD-MM-YYYY HH24:MI') || ', antes de e033-corrige-examen1-v2-partos-abortos.sql'
  FROM DUAL;
SELECT '-- Restaurar: conectado como BIOMETRICO, correr este archivo completo (F5).' FROM DUAL;
SELECT CASE WHEN LINE = 1 THEN 'CREATE OR REPLACE ' END || RTRIM(TEXT, CHR(10) || CHR(13))
  FROM USER_SOURCE
 WHERE NAME = 'PR_SERVICIO_MED_EXAMEN1_V2' AND TYPE = 'PROCEDURE'
 ORDER BY LINE;
SELECT '/' FROM DUAL;
SPOOL OFF
SET PAGESIZE 50
SET HEADING ON
SET FEEDBACK ON

PROMPT === Respaldo: renglones guardados del clon (debe ser mayor que 0) ===
SELECT NAME, COUNT(*) RENGLONES_RESPALDADOS FROM USER_SOURCE
 WHERE NAME = 'PR_SERVICIO_MED_EXAMEN1_V2' AND TYPE = 'PROCEDURE'
 GROUP BY NAME;
PROMPT Archivo: C:\miguel\respaldosql\PR_SERVICIO_MED_EXAMEN1_V2_antes_e033_12nov2026.sql

-- ---------- PASO 2: correccion ----------
DECLARE
  c_a CONSTANT VARCHAR2(300) :=
    'cvar4(\s*:=\s*apex_json\.get_varchar2\s*\(\s*p_path\s*=>\s*''SERV_MED_ANT_GIN_OBSTETRICOS\.A'')';
  c_p CONSTANT VARCHAR2(300) :=
    'cvar5(\s*:=\s*apex_json\.get_varchar2\s*\(\s*p_path\s*=>\s*''SERV_MED_ANT_GIN_OBSTETRICOS\.P'')';
  v_src    CLOB;
  v_nuevo  CLOB;
  n_a      NUMBER;
  n_p      NUMBER;
  v_estado VARCHAR2(20);
BEGIN
  DBMS_LOB.CREATETEMPORARY(v_src, TRUE);
  FOR l IN (SELECT TEXT FROM USER_SOURCE WHERE NAME = 'PR_SERVICIO_MED_EXAMEN1_V2' AND TYPE = 'PROCEDURE' ORDER BY LINE) LOOP
    DBMS_LOB.APPEND(v_src, l.TEXT);
  END LOOP;
  IF DBMS_LOB.GETLENGTH(v_src) = 0 THEN
    RAISE_APPLICATION_ERROR(-20040, 'No existe PR_SERVICIO_MED_EXAMEN1_V2. No se hizo nada.');
  END IF;

  n_a := REGEXP_COUNT(v_src, c_a, 1, 'i');
  n_p := REGEXP_COUNT(v_src, c_p, 1, 'i');
  DBMS_OUTPUT.PUT_LINE('Encontrados: Abortos (.A) en cvar4 = ' || n_a || ', Partos (.P) en cvar5 = ' || n_p
                       || ' (se espera 1 y 1)');
  IF n_a <> 1 OR n_p <> 1 THEN
    RAISE_APPLICATION_ERROR(-20041, 'No coincide con lo esperado (ya corregido o el codigo cambio). No se hizo nada.');
  END IF;

  -- primero .A -> cvar5; luego .P -> cvar4 (el segundo patron solo casa con la ruta .P, no hay choque)
  v_nuevo := REGEXP_REPLACE(v_src, c_a, 'cvar5\1', 1, 1, 'i');
  v_nuevo := REGEXP_REPLACE(v_nuevo, c_p, 'cvar4\1', 1, 1, 'i');

  EXECUTE IMMEDIATE 'CREATE OR REPLACE ' || v_nuevo;
  SELECT STATUS INTO v_estado FROM USER_OBJECTS WHERE OBJECT_NAME = 'PR_SERVICIO_MED_EXAMEN1_V2' AND OBJECT_TYPE = 'PROCEDURE';
  DBMS_OUTPUT.PUT_LINE('PR_SERVICIO_MED_EXAMEN1_V2 corregido: ' || v_estado);
  IF v_estado <> 'VALID' THEN
    RAISE_APPLICATION_ERROR(-20042, 'PR_SERVICIO_MED_EXAMEN1_V2 quedo INVALIDO: revisar USER_ERRORS y restaurar su respaldo.');
  END IF;
END;
/

PROMPT === Verificacion: lecturas de G/A/P/C (debe decir cvar4 = .P y cvar5 = .A) y el UPDATE P=cvar4, A=cvar5 ===
SELECT LINE, TRIM(TEXT) TEXTO FROM USER_SOURCE
 WHERE NAME = 'PR_SERVICIO_MED_EXAMEN1_V2' AND TYPE = 'PROCEDURE'
   AND (   REGEXP_LIKE(TEXT, 'SERV_MED_ANT_GIN_OBSTETRICOS\.(G|A|P|C)''', 'i')
        OR REGEXP_LIKE(TEXT, '^\s*(P|A)\s*=\s*cvar', 'i'))
 ORDER BY LINE;
WHENEVER SQLERROR CONTINUE
