-- =============================================================================
-- E-032 (11-nov-2026), PASO 2 de 2: CLON del WS de consulta del examen para el portal
--   PR_SERVICIO_MED_CONSULTA_V2  +  handler POST Portal_medico/consulta_examen_v2
-- =============================================================================
-- BASE    : BIOMETRICO de PRODUCCION. Correr CONECTADO COMO BIOMETRICO, con F5. Lo corre el usuario.
-- Requisito: e032-respaldo-consulta-examen.sql ya corrido (respaldo del original en C:\miguel\respaldosql\).
--
-- CAUSA (prueba E-032 del 11-nov): PR_SERVICIO_MED_CONSULTA, que arma lo que regresa Servcio/consulta_examen,
--   no escribe 3 campos que el WS de guardado SI guarda, y en Asma escribe el valor de Tuberculosis:
--     1) NEUROLOGIA.AVC_OBS                          (columna SERV_MED_NEUROLOGIA.AVC_OBS)
--     2) HEREDOFAMILIARES.NEFROPATIAS_ID             (columna SERV_MED_HEREDOFAMILIAR.NEFROPATIAS_ID)
--     3) SERV_MED_EXPLORACION_FISICA.NOLMAL_FASCIES  (columna SERV_MED_EXPLORACION_FISICA.NORMAL_FASCIES;
--        el JSON usa NOLMAL_FASCIES, igual que el WS de guardado)
--     4) NEUMOPATICA.ASMA: escribia coalesce(i.TUBERCULOSIS,0) -> coalesce(i.ASMA,0)
--   Efecto: al reabrir el examen, esos campos salian vacios (o Asma con el valor de Tuberculosis).
--
-- QUE HACE:
--   PASO 1: respalda en C:\miguel\respaldosql\ lo que ya hubiera de PR_SERVICIO_MED_CONSULTA_V2 (si existe;
--           si no, el archivo queda solo con el encabezado).
--   PASO 2: crea PR_SERVICIO_MED_CONSULTA_V2 = copia del original vivo con SOLO los 4 cambios de arriba.
--           Cada cambio debe aplicar exactamente una vez; si no, se detiene sin crear nada.
--   PASO 3: publica Portal_medico/consulta_examen_v2 (POST) = copia del handler vivo que llama al _V2.
--   PASO 4: verifica: estado del _V2, handler publicado y que el JSON del NSS de prueba ya trae los 3 campos
--           (la consulta solo LEE).
-- QUE NO HACE: no toca PR_SERVICIO_MED_CONSULTA ni Servcio/consulta_examen (los usa el PHP). No cambia datos.
--
-- Reversa: DROP PROCEDURE PR_SERVICIO_MED_CONSULTA_V2;
--          BEGIN ORDS.DELETE_TEMPLATE(p_module_name => 'Portal_medico', p_pattern => 'consulta_examen_v2'); COMMIT; END;
--   El portal, al no encontrar consulta_examen_v2 (404), vuelve solo a leer consulta_examen.
--
-- Correr con F5 (como script) o:  @C:\miguel\portalsalud-personal\docs\script-prod\e032-clon-consulta-examen-v2.sql
-- =============================================================================
SET SERVEROUTPUT ON SIZE UNLIMITED
WHENEVER SQLERROR EXIT ROLLBACK

-- ---------- PASO 1: RESPALDO de lo que ya existiera con el nombre _V2 ----------
SET PAGESIZE 0
SET LINESIZE 4000
SET LONG 1000000
SET TRIMSPOOL ON
SET FEEDBACK OFF
SET HEADING OFF
SET VERIFY OFF
SET ECHO OFF
SPOOL C:\miguel\respaldosql\PR_SERVICIO_MED_CONSULTA_V2_antes_e032_11nov2026.sql
SELECT '-- RESPALDO de PR_SERVICIO_MED_CONSULTA_V2 antes de e032-clon-consulta-examen-v2.sql, '
       || TO_CHAR(SYSDATE, 'DD-MM-YYYY HH24:MI') || ' (si no hay codigo abajo, no existia)'
  FROM DUAL;
SELECT CASE WHEN LINE = 1 THEN 'CREATE OR REPLACE ' END || RTRIM(TEXT, CHR(10) || CHR(13))
  FROM USER_SOURCE
 WHERE NAME = 'PR_SERVICIO_MED_CONSULTA_V2' AND TYPE = 'PROCEDURE'
 ORDER BY LINE;
SPOOL OFF
SET PAGESIZE 50
SET HEADING ON
SET FEEDBACK ON
PROMPT === Respaldo: renglones de un _V2 previo (0 = no existia) ===
SELECT COUNT(*) RENGLONES_V2_PREVIO FROM USER_SOURCE WHERE NAME = 'PR_SERVICIO_MED_CONSULTA_V2' AND TYPE = 'PROCEDURE';

-- ---------- PASO 2: PR_SERVICIO_MED_CONSULTA_V2 ----------
PROMPT === 2) PR_SERVICIO_MED_CONSULTA_V2 (copia del vivo + 4 correcciones) ===
DECLARE
  v_src    CLOB;
  v_nuevo  CLOB;
  v_estado VARCHAR2(20);
  nl       CONSTANT VARCHAR2(10) := CHR(10) || '    ';

  -- aplica un cambio que debe aparecer exactamente una vez
  PROCEDURE cambiar(p_texto IN OUT NOCOPY CLOB, p_patron VARCHAR2, p_por VARCHAR2, p_que VARCHAR2) IS
    n NUMBER := REGEXP_COUNT(p_texto, p_patron, 1, 'i');
  BEGIN
    DBMS_OUTPUT.PUT_LINE('  ' || RPAD(p_que, 46) || ' encontrado ' || n || ' vez(es) (se espera 1)');
    IF n <> 1 THEN
      RAISE_APPLICATION_ERROR(-20051, p_que || ': se esperaba 1 y hay ' || n || '. No se creo nada.');
    END IF;
    p_texto := REGEXP_REPLACE(p_texto, p_patron, p_por, 1, 1, 'i');
  END;
BEGIN
  DBMS_LOB.CREATETEMPORARY(v_src, TRUE);
  FOR l IN (SELECT TEXT FROM USER_SOURCE WHERE NAME = 'PR_SERVICIO_MED_CONSULTA' AND TYPE = 'PROCEDURE' ORDER BY LINE) LOOP
    DBMS_LOB.APPEND(v_src, l.TEXT);
  END LOOP;
  IF DBMS_LOB.GETLENGTH(v_src) = 0 THEN
    RAISE_APPLICATION_ERROR(-20050, 'No existe PR_SERVICIO_MED_CONSULTA. No se hizo nada.');
  END IF;
  v_nuevo := v_src;
  cambiar(v_nuevo, '(PROCEDURE\s+)PR_SERVICIO_MED_CONSULTA(\s*\()', '\1PR_SERVICIO_MED_CONSULTA_V2\2', 'nombre del procedimiento');
  cambiar(v_nuevo, 'END\s+PR_SERVICIO_MED_CONSULTA\s*;', 'END PR_SERVICIO_MED_CONSULTA_V2;', 'END del procedimiento');
  cambiar(v_nuevo, '(APEX_JSON\.WRITE\(''PARALISIS_FACIAL_OBS'')',
          'APEX_JSON.WRITE(''AVC_OBS'',coalesce(i.AVC_OBS,''sin observaciones''));' || nl || '\1', '1) NEUROLOGIA.AVC_OBS');
  cambiar(v_nuevo, '(APEX_JSON\.WRITE\(''OBESIDAD_ID'')',
          'APEX_JSON.WRITE(''NEFROPATIAS_ID'',coalesce(i.NEFROPATIAS_ID,0));' || nl || '\1', '2) HEREDOFAMILIARES.NEFROPATIAS_ID');
  cambiar(v_nuevo, '(APEX_JSON\.WRITE\(''NORMAL_MARCHA'')',
          'APEX_JSON.WRITE(''NOLMAL_FASCIES'',coalesce(i.NORMAL_FASCIES,''sin observaciones''));' || nl || '\1',
          '3) EXPLORACION_FISICA.NOLMAL_FASCIES');
  cambiar(v_nuevo, '(''ASMA''\s*,\s*coalesce\(\s*i\.)TUBERCULOSIS', '\1ASMA', '4) NEUMOPATICA.ASMA');
  EXECUTE IMMEDIATE 'CREATE OR REPLACE ' || v_nuevo;
  SELECT STATUS INTO v_estado FROM USER_OBJECTS WHERE OBJECT_NAME = 'PR_SERVICIO_MED_CONSULTA_V2' AND OBJECT_TYPE = 'PROCEDURE';
  DBMS_OUTPUT.PUT_LINE('PR_SERVICIO_MED_CONSULTA_V2 creado: ' || v_estado);
  IF v_estado <> 'VALID' THEN
    RAISE_APPLICATION_ERROR(-20052, 'PR_SERVICIO_MED_CONSULTA_V2 quedo INVALIDO: revisar USER_ERRORS. El handler no se publico.');
  END IF;
END;
/

-- ---------- PASO 3: handler POST Portal_medico/consulta_examen_v2 ----------
PROMPT === 3) Handler POST Servcio/consulta_examen_v2 (copia del vivo, llamando al _V2) ===
DECLARE
  v_src   CLOB;
  v_nuevo CLOB;
  n       NUMBER;
BEGIN
  SELECT h.SOURCE INTO v_src
    FROM USER_ORDS_HANDLERS h
    JOIN USER_ORDS_TEMPLATES t ON t.ID = h.TEMPLATE_ID
    JOIN USER_ORDS_MODULES m ON m.ID = t.MODULE_ID
   WHERE m.NAME = 'Portal_medico' AND t.URI_TEMPLATE = 'consulta_examen' AND h.METHOD = 'POST';
  n := REGEXP_COUNT(v_src, 'PR_SERVICIO_MED_CONSULTA\s*\(', 1, 'i');
  IF n <> 1 THEN
    RAISE_APPLICATION_ERROR(-20053, 'El handler vivo llama ' || n || ' veces a PR_SERVICIO_MED_CONSULTA (se espera 1). No se publico.');
  END IF;
  v_nuevo := REGEXP_REPLACE(v_src, 'PR_SERVICIO_MED_CONSULTA\s*\(', 'PR_SERVICIO_MED_CONSULTA_V2(', 1, 1, 'i');
  ORDS.DEFINE_TEMPLATE(p_module_name => 'Portal_medico', p_pattern => 'consulta_examen_v2',
                       p_comments => 'E-032: igual a consulta_examen, llama a PR_SERVICIO_MED_CONSULTA_V2 (AVC_OBS, NEFROPATIAS_ID, NOLMAL_FASCIES, ASMA)');
  ORDS.DEFINE_HANDLER(p_module_name => 'Portal_medico', p_pattern => 'consulta_examen_v2', p_method => 'POST',
                      p_source_type => ORDS.source_type_plsql, p_source => v_nuevo);
  COMMIT;
  DBMS_OUTPUT.PUT_LINE('Servcio/consulta_examen_v2 publicado (POST).');
END;
/

-- ---------- PASO 4: verificacion (solo lectura) ----------
PROMPT === 4) Verificacion ===
SELECT OBJECT_NAME, STATUS, TO_CHAR(LAST_DDL_TIME, 'DD-MM-YYYY HH24:MI') CAMBIO FROM USER_OBJECTS
 WHERE OBJECT_NAME IN ('PR_SERVICIO_MED_CONSULTA', 'PR_SERVICIO_MED_CONSULTA_V2') AND OBJECT_TYPE = 'PROCEDURE'
 ORDER BY OBJECT_NAME;
SELECT t.URI_TEMPLATE, h.METHOD, DBMS_LOB.GETLENGTH(h.SOURCE) LARGO
  FROM USER_ORDS_HANDLERS h JOIN USER_ORDS_TEMPLATES t ON t.ID = h.TEMPLATE_ID JOIN USER_ORDS_MODULES m ON m.ID = t.MODULE_ID
 WHERE m.NAME = 'Portal_medico' AND t.URI_TEMPLATE IN ('consulta_examen', 'consulta_examen_v2');
-- El NSS se pide al correr (no se versionan scripts con NSS); usar uno de prueba con examen capturado.
ACCEPT nss_prueba CHAR PROMPT 'NSS de prueba con examen: '
PROMPT === JSON del NSS de prueba: los 3 campos deben decir SI ===
DECLARE
  c CLOB;
BEGIN
  PR_SERVICIO_MED_CONSULTA_V2('&nss_prueba', c);
  DBMS_OUTPUT.PUT_LINE('AVC_OBS         ' || CASE WHEN INSTR(c, '"AVC_OBS"') > 0 THEN 'SI' ELSE 'NO' END);
  DBMS_OUTPUT.PUT_LINE('NEFROPATIAS_ID  ' || CASE WHEN INSTR(c, '"NEFROPATIAS_ID"') > 0 THEN 'SI' ELSE 'NO' END);
  DBMS_OUTPUT.PUT_LINE('NOLMAL_FASCIES  ' || CASE WHEN INSTR(c, '"NOLMAL_FASCIES"') > 0 THEN 'SI' ELSE 'NO' END);
  DBMS_OUTPUT.PUT_LINE('Largo del JSON: ' || DBMS_LOB.GETLENGTH(c));
END;
/
WHENEVER SQLERROR CONTINUE
