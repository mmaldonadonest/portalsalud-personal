-- =============================================================================
-- E-043 (17-nov-2026), PASO 1 de 2: CLON del WS de alta de incapacidades con ST2 y ALTA en su lugar
--   handler POST Servcio/incapacidades_v2
-- =============================================================================
-- BASE    : ORDS de PRODUCCION (modulo del WS; vive en BIOMETRICO). Correr CONECTADO COMO BIOMETRICO, con F5.
--           Lo corre el usuario. NO cambia datos: solo publica un WS nuevo.
--
-- CAUSA (diagnostico-incapacidades-cadena-folios.sql, 17-nov): el WS vivo Servcio/incapacidades inserta
--   ... FECHA_PRIMERA_INCAPACIDAD, ST2,    SALARIO_ACUMULADO, ALTA)
--   values (... CVAR18,               CVAR19, CVAR21,            CVAR20)
--   y CVAR19 es lo que llega como ALTA y CVAR20 lo que llega como ST2: van cruzadas. En produccion la columna
--   ALTA tiene 57 «No_aplica», opcion que solo existe en ST2.
--
-- QUE HACE:
--   PASO 1: localiza el handler vivo (POST incapacidades que inserta en TBL_SERV_INCAPACIDAD_MEDICA). Debe
--           ser exactamente uno; si no, se detiene sin publicar nada.
--   PASO 2: publica incapacidades_v2 (POST) en el MISMO modulo = copia del vivo con SOLO el orden de esos
--           dos valores corregido (CVAR18, CVAR20, CVAR21, CVAR19). El cambio debe aplicar una sola vez.
--   PASO 3: verifica (solo lectura).
-- QUE NO HACE: no toca Servcio/incapacidades (lo usa el PHP). No cambia datos (eso es el paso 2 de 2).
--
-- ORDEN: 1) este script  2) instalar el WAR que usa incapacidades_v2  3) e043-02-voltear-st2-alta.sql
--
-- Reversa: BEGIN ORDS.DELETE_TEMPLATE(p_module_name => '<modulo que imprime el paso 1>',
--                                     p_pattern => 'incapacidades_v2'); COMMIT; END;
--
-- Correr con F5 (como script) o:  @C:\miguel\portalsalud-personal\docs\script-prod\e043-01-clon-incapacidades-v2.sql
-- =============================================================================
SET SERVEROUTPUT ON SIZE UNLIMITED
WHENEVER SQLERROR EXIT ROLLBACK

PROMPT === 1 y 2) Handler POST incapacidades_v2 (copia del vivo, con ST2 y ALTA en su lugar) ===
DECLARE
  v_modulo VARCHAR2(255);
  v_base   VARCHAR2(255);
  v_src    CLOB;
  v_nuevo  CLOB;
  n        NUMBER;
  ya       NUMBER;
BEGIN
  SELECT COUNT(*) INTO n
    FROM USER_ORDS_HANDLERS h
    JOIN USER_ORDS_TEMPLATES t ON t.ID = h.TEMPLATE_ID
   WHERE t.URI_TEMPLATE = 'incapacidades' AND h.METHOD = 'POST'
     AND DBMS_LOB.INSTR(UPPER(h.SOURCE), 'INSERT INTO TBL_SERV_INCAPACIDAD_MEDICA') > 0;
  IF n <> 1 THEN
    RAISE_APPLICATION_ERROR(-20431, 'Se esperaba 1 handler POST incapacidades que inserte en TBL_SERV_INCAPACIDAD_MEDICA y hay ' || n || '. No se publico nada.');
  END IF;
  SELECT m.NAME, m.URI_PREFIX, h.SOURCE INTO v_modulo, v_base, v_src
    FROM USER_ORDS_HANDLERS h
    JOIN USER_ORDS_TEMPLATES t ON t.ID = h.TEMPLATE_ID
    JOIN USER_ORDS_MODULES m ON m.ID = t.MODULE_ID
   WHERE t.URI_TEMPLATE = 'incapacidades' AND h.METHOD = 'POST'
     AND DBMS_LOB.INSTR(UPPER(h.SOURCE), 'INSERT INTO TBL_SERV_INCAPACIDAD_MEDICA') > 0;
  DBMS_OUTPUT.PUT_LINE('Modulo del WS vivo: ' || v_modulo || '  (ruta ' || v_base || ')');

  SELECT COUNT(*) INTO ya
    FROM USER_ORDS_TEMPLATES t JOIN USER_ORDS_MODULES m ON m.ID = t.MODULE_ID
   WHERE m.NAME = v_modulo AND t.URI_TEMPLATE = 'incapacidades_v2';
  IF ya > 0 THEN
    RAISE_APPLICATION_ERROR(-20432, 'Ya existe incapacidades_v2 en ' || v_modulo || '. No se toco; revisar antes de volver a correr.');
  END IF;

  -- el unico cambio: el orden de ST2 (CVAR20) y ALTA (CVAR19) en el VALUES
  n := REGEXP_COUNT(v_src, 'CVAR18\s*,\s*CVAR19\s*,\s*CVAR21\s*,\s*CVAR20', 1, 'i');
  IF n <> 1 THEN
    RAISE_APPLICATION_ERROR(-20433, 'El orden CVAR18,CVAR19,CVAR21,CVAR20 aparece ' || n || ' veces (se espera 1). No se publico.');
  END IF;
  v_nuevo := REGEXP_REPLACE(v_src, 'CVAR18\s*,\s*CVAR19\s*,\s*CVAR21\s*,\s*CVAR20', 'CVAR18,CVAR20,CVAR21,CVAR19', 1, 1, 'i');

  ORDS.DEFINE_TEMPLATE(p_module_name => v_modulo, p_pattern => 'incapacidades_v2',
                       p_comments => 'E-043: igual a incapacidades, con ST2 y ALTA en su columna (el original las cruza)');
  ORDS.DEFINE_HANDLER(p_module_name => v_modulo, p_pattern => 'incapacidades_v2', p_method => 'POST',
                      p_source_type => ORDS.source_type_plsql, p_source => v_nuevo);
  COMMIT;
  DBMS_OUTPUT.PUT_LINE(v_base || 'incapacidades_v2 publicado (POST).');
END;
/

PROMPT === 3) Verificacion: los dos handlers y el orden de sus valores ===
SELECT m.NAME MODULO, t.URI_TEMPLATE, h.METHOD, DBMS_LOB.GETLENGTH(h.SOURCE) LARGO,
       CASE WHEN REGEXP_COUNT(h.SOURCE, 'CVAR18\s*,\s*CVAR20\s*,\s*CVAR21\s*,\s*CVAR19', 1, 'i') = 1 THEN 'CORRECTO (ST2, ALTA)'
            WHEN REGEXP_COUNT(h.SOURCE, 'CVAR18\s*,\s*CVAR19\s*,\s*CVAR21\s*,\s*CVAR20', 1, 'i') = 1 THEN 'CRUZADO (el original)'
            ELSE '?' END ORDEN
  FROM USER_ORDS_HANDLERS h
  JOIN USER_ORDS_TEMPLATES t ON t.ID = h.TEMPLATE_ID
  JOIN USER_ORDS_MODULES m ON m.ID = t.MODULE_ID
 WHERE t.URI_TEMPLATE IN ('incapacidades', 'incapacidades_v2') AND h.METHOD = 'POST'
   AND DBMS_LOB.INSTR(UPPER(h.SOURCE), 'INSERT INTO TBL_SERV_INCAPACIDAD_MEDICA') > 0;
