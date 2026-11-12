-- =============================================================================
-- SOLO LECTURA. ¿Que se ha guardado realmente en los campos que el EMNI captura con botones Si/No?
--   (12-nov-2026, antes de pasar Examen medico a los mismos botones que el EMNI)
--
-- BASE: BIOMETRICO de produccion (tablas del examen del WS: SERV_MED_ANT_PATOLOGICOS y
-- SERV_MED_EXPLORACION_FISICA). Correr conectado como BIOMETRICO. No modifica nada.
--
-- Por cada columna lista los 15 valores mas frecuentes (normalizados a mayusculas y sin espacios a los lados)
-- y cuantos renglones tienen un valor que NO es Si/No/vacio. Los nombres de columna se toman de
-- USER_TAB_COLUMNS porque en Exploracion traen acentos y typos (Satisfacción, Comprimiso_org).
-- =============================================================================
SET SERVEROUTPUT ON SIZE UNLIMITED

DECLARE
  TYPE t_cur IS REF CURSOR;
  c        t_cur;
  v_valor  VARCHAR2(4000);
  v_n      NUMBER;
  v_otros  NUMBER;
  v_total  NUMBER;
  v_norm   CONSTANT VARCHAR2(200) := q'[UPPER(TRIM(TRANSLATE(TO_CHAR(%C), 'ÍíÁáÉéÓóÚú', 'IiAaEeOoUu')))]';
  v_expr   VARCHAR2(400);
BEGIN
  FOR col IN (
      SELECT TABLE_NAME, COLUMN_NAME FROM USER_TAB_COLUMNS
       WHERE (TABLE_NAME = 'SERV_MED_ANT_PATOLOGICOS'
              AND COLUMN_NAME IN ('AGUA_POTABLE', 'LUZ', 'DRENAJE', 'AIRE_ACONDICIONADO', 'BALANCEADA', 'SUFICIENTE'))
          OR (TABLE_NAME = 'SERV_MED_EXPLORACION_FISICA'
              AND (COLUMN_NAME LIKE 'SATISFACC%' OR COLUMN_NAME LIKE 'COMPROMISO%' OR COLUMN_NAME LIKE 'COMPRIMISO%'
                   OR COLUMN_NAME = 'LENGUAJE' OR COLUMN_NAME LIKE 'CONVERSACION%' OR COLUMN_NAME LIKE 'COHERENCIA%'))
       ORDER BY TABLE_NAME, COLUMN_ID) LOOP
    v_expr := REPLACE(v_norm, '%C', '"' || col.COLUMN_NAME || '"');
    EXECUTE IMMEDIATE 'SELECT COUNT(*) FROM ' || col.TABLE_NAME INTO v_total;
    EXECUTE IMMEDIATE 'SELECT COUNT(*) FROM ' || col.TABLE_NAME || ' WHERE ' || v_expr
                      || ' IS NOT NULL AND ' || v_expr || q'[ NOT IN ('SI', 'NO', '1', '0', 'S', 'N')]' INTO v_otros;
    DBMS_OUTPUT.PUT_LINE('');
    DBMS_OUTPUT.PUT_LINE(col.TABLE_NAME || '.' || col.COLUMN_NAME || '  (renglones: ' || v_total
                         || ', con algo distinto de Si/No/1/0: ' || v_otros || ')');
    OPEN c FOR 'SELECT * FROM (SELECT ' || v_expr || ' V, COUNT(*) N FROM ' || col.TABLE_NAME
               || ' GROUP BY ' || v_expr || ' ORDER BY 2 DESC) WHERE ROWNUM <= 15';
    LOOP
      FETCH c INTO v_valor, v_n;
      EXIT WHEN c%NOTFOUND;
      DBMS_OUTPUT.PUT_LINE('   ' || RPAD(NVL(SUBSTR(v_valor, 1, 60), '(vacio)'), 62) || LPAD(v_n, 8));
    END LOOP;
    CLOSE c;
  END LOOP;
END;
/
