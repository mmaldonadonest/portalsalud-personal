-- =============================================================================
-- C-003 - RESPALDO del catalogo de diagnosticos ACTUAL (18-nov-2026), antes de cargar el de SM
--
-- BASE: la del PORTAL = BIOMETRICO de produccion. Correr conectado como BIOMETRICO. LO CORRE: el usuario.
-- Tabla: SERV_MED_CAT_INDICE_IDC10 (la que lee el WS Servcio/indice = buscador ICD del portal).
--
-- QUE HACE (no modifica la tabla original):
--   1. Copia completa, con TODAS sus columnas, en la tabla nueva SERV_MED_CAT_IDC10_RESP_18NOV
--      (si ya existe, no la pisa: avisa y sigue).
--   2. Archivo en disco C:\miguel\respaldosql\c003_cat_indice_idc10_antes_18nov2026.sql con los
--      INSERT de CLAVE_ID y CLAVE_NOMBRE (las dos columnas que usa el WS).
--   3. Cuenta renglones de la original y de la copia: deben ser iguales.
--
-- REVERSA (solo si la carga nueva saliera mal), con la copia completa:
--   DELETE FROM SERV_MED_CAT_INDICE_IDC10;
--   INSERT INTO SERV_MED_CAT_INDICE_IDC10 SELECT * FROM SERV_MED_CAT_IDC10_RESP_18NOV;
--   COMMIT;
-- Quitar la copia cuando ya no haga falta:  DROP TABLE SERV_MED_CAT_IDC10_RESP_18NOV;
--
-- Correr con:  @C:\miguel\portalsalud-personal\docs\script-prod\c003-respaldo-catalogo-diagnosticos.sql
-- =============================================================================
SET SERVEROUTPUT ON
SET DEFINE OFF

-- 1. copia completa dentro de la base
BEGIN
    EXECUTE IMMEDIATE 'CREATE TABLE SERV_MED_CAT_IDC10_RESP_18NOV AS SELECT * FROM SERV_MED_CAT_INDICE_IDC10';
    DBMS_OUTPUT.PUT_LINE('Copia creada: SERV_MED_CAT_IDC10_RESP_18NOV');
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -955 THEN
            DBMS_OUTPUT.PUT_LINE('AVISO: SERV_MED_CAT_IDC10_RESP_18NOV ya existia; no se toco.');
        ELSE
            RAISE;
        END IF;
END;
/

-- 2. respaldo en disco (INSERT de las dos columnas del WS)
SET LINESIZE 600
SET PAGESIZE 0
SET TRIMSPOOL ON
SET FEEDBACK OFF
SET HEADING OFF
SET TERMOUT OFF
SPOOL C:\miguel\respaldosql\c003_cat_indice_idc10_antes_18nov2026.sql
SELECT '-- RESPALDO C-003: SERV_MED_CAT_INDICE_IDC10 antes de cargar el catalogo de SM' FROM DUAL;
SELECT '-- BASE: BIOMETRICO de produccion. Generado ' || TO_CHAR(SYSDATE, 'DD-MM-YYYY HH24:MI') FROM DUAL;
SELECT 'SET DEFINE OFF' FROM DUAL;
SELECT 'INSERT INTO SERV_MED_CAT_INDICE_IDC10 (CLAVE_ID, CLAVE_NOMBRE) VALUES ('
       || NVL2(c.CLAVE_ID, '''' || REPLACE(c.CLAVE_ID, '''', '''''') || '''', 'NULL') || ', '
       || NVL2(c.CLAVE_NOMBRE, '''' || REPLACE(c.CLAVE_NOMBRE, '''', '''''') || '''', 'NULL') || ');'
  FROM SERV_MED_CAT_INDICE_IDC10 c
 ORDER BY c.CLAVE_ID;
SELECT 'COMMIT;' FROM DUAL;
SPOOL OFF
SET TERMOUT ON
PROMPT Respaldo escrito en C:\miguel\respaldosql\c003_cat_indice_idc10_antes_18nov2026.sql

-- 3. verificacion: las dos cifras deben ser iguales
SET PAGESIZE 100
SET HEADING ON
SET FEEDBACK ON
SELECT (SELECT COUNT(*) FROM SERV_MED_CAT_INDICE_IDC10)      AS ORIGINAL,
       (SELECT COUNT(*) FROM SERV_MED_CAT_IDC10_RESP_18NOV) AS COPIA
  FROM DUAL;
-- columnas de la tabla, para saber que trae ademas de CLAVE_ID y CLAVE_NOMBRE
SELECT COLUMN_NAME, DATA_TYPE, DATA_LENGTH
  FROM USER_TAB_COLUMNS
 WHERE TABLE_NAME = 'SERV_MED_CAT_INDICE_IDC10'
 ORDER BY COLUMN_ID;
