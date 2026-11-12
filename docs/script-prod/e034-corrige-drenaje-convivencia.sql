-- =============================================================================
-- E-034 - CORRIGE Drenaje y Convivencia con animales cruzados (12-nov-2026)
--
-- BASE: BIOMETRICO de produccion. Modifica SERV_MED_ANT_PATOLOGICOS (tabla del examen del WS).
-- Lee SERV_MED_TAG y SERV_MED_EXAMEN_BORRADOR (portal). Correr conectado como BIOMETRICO.
--
-- Por que: PR_SERVICIO_MED_EXAMEN1 (PHP) y PR_SERVICIO_MED_EXAMEN1_V2 (portal, hasta el 11-nov) hacian
--   cvar8 := DRENAJE  y luego  cvar8 := CONVIVENCIA_CON_ANIMALES  (debia ser cvar9).
-- La columna DRENAJE guarda la respuesta de Convivencia y CONVIVENCIA_CON_ANIMALES queda vacia.
-- Comprobado con e034-revisar-drenaje-convivencia.sql: tag CCA del PHP = columna DRENAJE en 2,571 de 2,573.
--
-- Que hace, solo en renglones con DRENAJE con valor y CONVIVENCIA_CON_ANIMALES vacia:
--   1. CONVIVENCIA_CON_ANIMALES := DRENAJE  (era su respuesta real).
--   2. DRENAJE := la respuesta real de Drenaje, de donde exista, en este orden:
--        a) borrador del EMNI del portal (SERV_MED_EXAMEN_BORRADOR, guarda el formulario tal cual);
--        b) ultimo tag 'Drenaje' del PHP (SERV_MED_TAG, migrado el 1-oct);
--        c) si no hay ninguno: vacio (sin dato; la respuesta se perdio al guardar).
-- NO toca los NSS que tuvieron un guardado del portal despues de corregir el _V2 (11-nov 17:00):
--   ahi DRENAJE ya es Drenaje de verdad. Se listan para revisar a mano.
--
-- CANDADOS: simulacro por defecto ('N'); tope de renglones; todo o nada (ROLLBACK si algo falla).
-- RESPALDO: antes de tocar nada se escribe en C:\miguel\respaldosql un .sql con los UPDATE que
--   regresan cada renglon a como estaba (reversa = correr ese archivo).
--
-- Correr con:  @C:\miguel\portalsalud-personal\docs\script-prod\e034-corrige-drenaje-convivencia.sql
-- Primero con v_forzar = 'N' (abajo) y mandar la salida. Despues con 'S'.
-- =============================================================================
SET SERVEROUTPUT ON SIZE UNLIMITED
SET DEFINE OFF
SET LINESIZE 400
SET PAGESIZE 0
SET TRIMSPOOL ON
SET FEEDBACK OFF
SET HEADING OFF
-- TERMOUT OFF: los ~2,600 UPDATE del respaldo van solo al archivo, no a la pantalla (con @ si aplica)
SET TERMOUT OFF

-- ---------------------------------------------------------------- respaldo (reversa) -------------
SPOOL C:\miguel\respaldosql\e034_ant_patologicos_drenaje_convivencia_antes_12nov2026.sql
SELECT '-- REVERSA E-034: regresa DRENAJE y CONVIVENCIA_CON_ANIMALES a como estaban antes de la correccion' FROM DUAL;
SELECT '-- BASE: BIOMETRICO de produccion. Generado ' || TO_CHAR(SYSDATE, 'DD-MM-YYYY HH24:MI') FROM DUAL;
SELECT 'SET DEFINE OFF' FROM DUAL;
SELECT 'UPDATE SERV_MED_ANT_PATOLOGICOS SET DRENAJE = '
       || NVL2(p.DRENAJE, '''' || REPLACE(p.DRENAJE, '''', '''''') || '''', 'NULL')
       || ', CONVIVENCIA_CON_ANIMALES = '
       || NVL2(p.CONVIVENCIA_CON_ANIMALES, '''' || REPLACE(p.CONVIVENCIA_CON_ANIMALES, '''', '''''') || '''', 'NULL')
       || ' WHERE NSS = ''' || REPLACE(p.NSS, '''', '''''') || ''';'
  FROM SERV_MED_ANT_PATOLOGICOS p
 WHERE TRIM(p.DRENAJE) IS NOT NULL
   AND TRIM(p.CONVIVENCIA_CON_ANIMALES) IS NULL
 ORDER BY p.NSS;
SELECT 'COMMIT;' FROM DUAL;
SPOOL OFF
SET TERMOUT ON
PROMPT Respaldo escrito en C:\miguel\respaldosql\e034_ant_patologicos_drenaje_convivencia_antes_12nov2026.sql

SET PAGESIZE 100
SET HEADING ON
SET FEEDBACK ON

-- ---------------------------------------------------------------- correccion ---------------------
DECLARE
  v_forzar   CONSTANT VARCHAR2(1) := 'N';              -- 'S' para aplicar
  v_maximo   CONSTANT PLS_INTEGER := 2700;             -- el revisar dio 2,577 candidatos
  v_corte    CONSTANT TIMESTAMP   := TIMESTAMP '2026-11-11 17:00:00';   -- se corrigio el _V2
  v_len_conv NUMBER;
  v_len_max  NUMBER;
  n_total    PLS_INTEGER := 0;
  n_borr     PLS_INTEGER := 0;
  n_tag      PLS_INTEGER := 0;
  n_sin      PLS_INTEGER := 0;
  n_excl     PLS_INTEGER := 0;
  n_upd      PLS_INTEGER := 0;
BEGIN
  DBMS_OUTPUT.PUT_LINE(CASE v_forzar WHEN 'S' THEN '*** CORRIGIENDO ***' ELSE 'SIMULACRO (no se modifica nada)' END);

  -- que quepa: CONVIVENCIA_CON_ANIMALES debe aguantar lo mas largo de DRENAJE
  SELECT DATA_LENGTH INTO v_len_conv FROM USER_TAB_COLUMNS
   WHERE TABLE_NAME = 'SERV_MED_ANT_PATOLOGICOS' AND COLUMN_NAME = 'CONVIVENCIA_CON_ANIMALES';
  SELECT NVL(MAX(LENGTHB(DRENAJE)), 0) INTO v_len_max FROM SERV_MED_ANT_PATOLOGICOS;
  DBMS_OUTPUT.PUT_LINE('Largo CONVIVENCIA_CON_ANIMALES: ' || v_len_conv || ' bytes; DRENAJE mas largo: ' || v_len_max);
  IF v_len_max > v_len_conv THEN
    RAISE_APPLICATION_ERROR(-20001, 'DRENAJE no cabe en CONVIVENCIA_CON_ANIMALES. No se hace nada.');
  END IF;

  DBMS_OUTPUT.PUT_LINE('');
  DBMS_OUTPUT.PUT_LINE('Excluidos (guardados del portal despues del ' || TO_CHAR(v_corte, 'DD-MM-YYYY HH24:MI')
                       || ': DRENAJE ya es correcto, revisar a mano):');
  FOR x IN (SELECT p.NSS, p.DRENAJE FROM SERV_MED_ANT_PATOLOGICOS p
             WHERE TRIM(p.DRENAJE) IS NOT NULL AND TRIM(p.CONVIVENCIA_CON_ANIMALES) IS NULL
               AND EXISTS (SELECT 1 FROM SERV_MED_RESULTADO_EXAMEN_HIST h
                            WHERE h.NSS = p.NSS AND h.FECHA_REGISTRO >= v_corte)) LOOP
    n_excl := n_excl + 1;
    DBMS_OUTPUT.PUT_LINE('  ' || x.NSS || '  DRENAJE=' || x.DRENAJE);
  END LOOP;
  IF n_excl = 0 THEN DBMS_OUTPUT.PUT_LINE('  (ninguno)'); END IF;

  FOR r IN (
      WITH tagd AS (
        SELECT NSS, TRIM(DBMS_LOB.SUBSTR(CONTENT, 200, 1)) VALOR,
               ROW_NUMBER() OVER (PARTITION BY NSS ORDER BY CREATED_AT DESC) RN
          FROM SERV_MED_TAG WHERE UPPER(TYPE) = 'DRENAJE'),
      borr AS (
        SELECT NSS, TRIM(JSON_VALUE(DATOS, '$."SERV_MED_ANT_PATOLOGICOS.DRENAJE"' RETURNING VARCHAR2(200))) VALOR,
               ROW_NUMBER() OVER (PARTITION BY NSS ORDER BY UPDATED_AT DESC) RN
          FROM SERV_MED_EXAMEN_BORRADOR)
      SELECT p.NSS, p.DRENAJE,
             b.VALOR BORRADOR, t.VALOR TAG
        FROM SERV_MED_ANT_PATOLOGICOS p
        LEFT JOIN borr b ON b.NSS = p.NSS AND b.RN = 1
        LEFT JOIN tagd t ON t.NSS = p.NSS AND t.RN = 1
       WHERE TRIM(p.DRENAJE) IS NOT NULL
         AND TRIM(p.CONVIVENCIA_CON_ANIMALES) IS NULL
         AND NOT EXISTS (SELECT 1 FROM SERV_MED_RESULTADO_EXAMEN_HIST h
                          WHERE h.NSS = p.NSS AND h.FECHA_REGISTRO >= v_corte)) LOOP
    n_total := n_total + 1;
    IF r.BORRADOR IS NOT NULL THEN n_borr := n_borr + 1;
    ELSIF r.TAG IS NOT NULL THEN n_tag := n_tag + 1;
    ELSE n_sin := n_sin + 1;
    END IF;
    IF v_forzar = 'S' THEN
      UPDATE SERV_MED_ANT_PATOLOGICOS
         SET CONVIVENCIA_CON_ANIMALES = DRENAJE,
             DRENAJE = NVL(r.BORRADOR, r.TAG)
       WHERE NSS = r.NSS
         AND TRIM(CONVIVENCIA_CON_ANIMALES) IS NULL;
      n_upd := n_upd + SQL%ROWCOUNT;
    END IF;
  END LOOP;

  DBMS_OUTPUT.PUT_LINE('');
  DBMS_OUTPUT.PUT_LINE('A corregir: ' || n_total || ' renglones');
  DBMS_OUTPUT.PUT_LINE('  Drenaje desde el borrador del EMNI:  ' || n_borr);
  DBMS_OUTPUT.PUT_LINE('  Drenaje desde el tag del PHP:        ' || n_tag);
  DBMS_OUTPUT.PUT_LINE('  Drenaje queda vacio (sin dato):      ' || n_sin);
  DBMS_OUTPUT.PUT_LINE('  Excluidos para revisar a mano:       ' || n_excl);

  IF n_total > v_maximo THEN
    ROLLBACK;
    RAISE_APPLICATION_ERROR(-20002, 'Son ' || n_total || ' renglones, mas que el tope ' || v_maximo || '. No se hace nada.');
  END IF;

  IF v_forzar = 'S' THEN
    IF n_upd <> n_total THEN
      ROLLBACK;
      RAISE_APPLICATION_ERROR(-20003, 'Se contaron ' || n_total || ' y el UPDATE toco ' || n_upd || '. ROLLBACK.');
    END IF;
    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Corregidos: ' || n_upd || '. COMMIT hecho.');
  ELSE
    DBMS_OUTPUT.PUT_LINE('Simulacro terminado. Si cuadra, correr con v_forzar = ''S''.');
  END IF;
EXCEPTION
  WHEN OTHERS THEN
    ROLLBACK;
    RAISE;
END;
/

-- ---------------------------------------------------------------- comprobacion -------------------
PROMPT === Comprobacion: DRENAJE con valor y CONVIVENCIA vacia (con 'S' debe quedar solo lo excluido) ===
SELECT COUNT(*) PENDIENTES FROM SERV_MED_ANT_PATOLOGICOS
 WHERE TRIM(DRENAJE) IS NOT NULL AND TRIM(CONVIVENCIA_CON_ANIMALES) IS NULL;

PROMPT === Valores de DRENAJE (con 'S' deben ser 1/0/SI/NO o vacio) ===
SELECT * FROM (SELECT NVL(UPPER(TRIM(DRENAJE)), '(vacio)') VALOR, COUNT(*) N FROM SERV_MED_ANT_PATOLOGICOS
                GROUP BY UPPER(TRIM(DRENAJE)) ORDER BY 2 DESC) WHERE ROWNUM <= 10;

PROMPT === Valores de CONVIVENCIA_CON_ANIMALES (con 'S' aqui quedan los animales) ===
SELECT * FROM (SELECT NVL(UPPER(TRIM(CONVIVENCIA_CON_ANIMALES)), '(vacio)') VALOR, COUNT(*) N FROM SERV_MED_ANT_PATOLOGICOS
                GROUP BY UPPER(TRIM(CONVIVENCIA_CON_ANIMALES)) ORDER BY 2 DESC) WHERE ROWNUM <= 10;
