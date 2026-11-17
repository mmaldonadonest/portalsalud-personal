-- =============================================================================
-- E-043 (17-nov-2026), PASO 2 de 2: VOLTEAR ST2 y ALTA en las incapacidades ya guardadas
-- =============================================================================
-- BASE    : BIOMETRICO de PRODUCCION, tabla del WS de ORDS (TBL_SERV_INCAPACIDAD_MEDICA). Correr CONECTADO COMO
--           BIOMETRICO, con F5. Lo corre el usuario. SI CAMBIA DATOS (con respaldo antes y confirmacion).
--
-- Por que se puede voltear todo: el WS Servcio/incapacidades las cruza desde su origen (tambien con el PHP), asi
--   que TODO lo guardado con el esta cruzado igual. Se voltea solo lo registrado ANTES del corte = la hora en que
--   se instalo el WAR que ya usa incapacidades_v2 (lo que entre despues ya llega bien y no se toca).
--
-- ORDEN: 1) e043-01-clon-incapacidades-v2.sql  2) instalar el WAR  3) este script, con la hora de instalacion.
--   OJO: si el PHP sigue capturando incapacidades, lo que capture DESPUES del corte vuelve a quedar cruzado.
--
-- QUE HACE:
--   PASO 1: pide el corte (DD/MM/YYYY HH24:MI) y muestra cuantas filas se voltearian y como quedarian.
--   PASO 2: RESPALDO en C:\miguel\respaldosql\e043_st2_alta_antes_17nov2026.sql = un UPDATE por fila que regresa
--           ST2 y ALTA a como estaban (sirve de reversa: correrlo y COMMIT).
--   PASO 3: pide confirmar con SI; voltea (ST2 <-> ALTA) y verifica. Sin SI no cambia nada.
--
-- Correr con F5 (como script) o:  @C:\miguel\portalsalud-personal\docs\script-prod\e043-02-voltear-st2-alta.sql
-- =============================================================================
SET SERVEROUTPUT ON SIZE UNLIMITED
SET VERIFY OFF
WHENEVER SQLERROR EXIT ROLLBACK

ACCEPT corte CHAR PROMPT 'Corte = fecha y hora en que se instalo el WAR (DD/MM/YYYY HH24:MI): '

PROMPT === 1) Antes: como estan hoy (ST2 / ALTA) en lo que se va a voltear ===
SELECT NVL(ST2, '(nulo)') ST2_HOY, NVL(ALTA, '(nulo)') ALTA_HOY, COUNT(*) N
  FROM TBL_SERV_INCAPACIDAD_MEDICA
 WHERE FECHA_REGISTRO < TO_DATE('&corte', 'DD/MM/YYYY HH24:MI')
 GROUP BY ST2, ALTA
 ORDER BY N DESC;
SELECT COUNT(*) FILAS_A_VOLTEAR,
       COUNT(CASE WHEN ST2 IS NOT NULL OR ALTA IS NOT NULL THEN 1 END) CON_ALGUN_DATO
  FROM TBL_SERV_INCAPACIDAD_MEDICA
 WHERE FECHA_REGISTRO < TO_DATE('&corte', 'DD/MM/YYYY HH24:MI');

-- ---------- PASO 2: RESPALDO (reversa fila por fila) ----------
SET PAGESIZE 0
SET LINESIZE 400
SET TRIMSPOOL ON
SET FEEDBACK OFF
SET HEADING OFF
SPOOL C:\miguel\respaldosql\e043_st2_alta_antes_17nov2026.sql
SELECT '-- RESPALDO E-043: ST2 y ALTA antes de voltear, ' || TO_CHAR(SYSDATE, 'DD-MM-YYYY HH24:MI')
       || '. Corte: &corte. Para regresar: correr este archivo y COMMIT.' FROM DUAL;
SELECT 'UPDATE TBL_SERV_INCAPACIDAD_MEDICA SET ST2 = '
       || CASE WHEN ST2 IS NULL THEN 'NULL' ELSE '''' || REPLACE(ST2, '''', '''''') || '''' END
       || ', ALTA = '
       || CASE WHEN ALTA IS NULL THEN 'NULL' ELSE '''' || REPLACE(ALTA, '''', '''''') || '''' END
       || ' WHERE REG_ID = ' || REG_ID
       || ' AND NSS = ''' || NSS || ''' AND FECHA_REGISTRO = TO_DATE(''' || TO_CHAR(FECHA_REGISTRO, 'DD/MM/YYYY HH24:MI:SS')
       || ''', ''DD/MM/YYYY HH24:MI:SS'');'
  FROM TBL_SERV_INCAPACIDAD_MEDICA
 WHERE FECHA_REGISTRO < TO_DATE('&corte', 'DD/MM/YYYY HH24:MI')
 ORDER BY REG_ID;
SPOOL OFF
SET PAGESIZE 50
SET HEADING ON
SET FEEDBACK ON
PROMPT Respaldo escrito en C:\miguel\respaldosql\e043_st2_alta_antes_17nov2026.sql (revisar que traiga los UPDATE)

-- ---------- PASO 3: voltear con confirmacion ----------
ACCEPT confirmar CHAR PROMPT 'Escribe SI para voltear ST2 y ALTA (cualquier otra cosa = no cambia nada): '
DECLARE
  n NUMBER;
BEGIN
  IF UPPER(TRIM('&confirmar')) <> 'SI' THEN
    DBMS_OUTPUT.PUT_LINE('No se confirmo: no se cambio nada.');
    RETURN;
  END IF;
  -- Oracle evalua el lado derecho con los valores de antes: ST2 recibe el ALTA viejo y ALTA el ST2 viejo
  UPDATE TBL_SERV_INCAPACIDAD_MEDICA
     SET ST2 = ALTA, ALTA = ST2
   WHERE FECHA_REGISTRO < TO_DATE('&corte', 'DD/MM/YYYY HH24:MI');
  n := SQL%ROWCOUNT;
  COMMIT;
  DBMS_OUTPUT.PUT_LINE('Volteadas ' || n || ' filas (ST2 <-> ALTA). COMMIT hecho.');
END;
/

PROMPT === 4) Despues: ALTA ya no debe traer No_aplica (y ST2 si puede) ===
SELECT NVL(ST2, '(nulo)') ST2, NVL(ALTA, '(nulo)') ALTA, COUNT(*) N
  FROM TBL_SERV_INCAPACIDAD_MEDICA
 GROUP BY ST2, ALTA
 ORDER BY N DESC;
