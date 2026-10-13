-- =============================================================================
-- Donde se va el tiempo del guardado del examen. SOLO LECTURA.
-- Base: BIOMETRICO de PRODUCCION. Lo corre el usuario (o el DBA si da ORA-00942: falta
-- permiso para ver V$SQL / V$SESSION).
--
-- COMO USARLO:
--   Bloque 1: correrlo MIENTRAS el portal esta guardando (en los segundos de espera).
--             Dice si la sesion de ORDS esta esperando un bloqueo y quien la bloquea.
--   Bloque 2: correrlo JUSTO DESPUES de un guardado. Dice que sentencia del guardado
--             tardo mas (tiempo acumulado por sentencia).
-- =============================================================================
SET LINESIZE 250
SET PAGESIZE 200
COLUMN evento     FORMAT A40
COLUMN sql_texto  FORMAT A90
COLUMN programa   FORMAT A30

-- 1) Sesiones esperando AHORA (en particular, bloqueos de fila "enq: TX - row lock contention")
SELECT s.sid, s.serial#, s.username, s.program programa, s.status,
       s.event evento, s.seconds_in_wait seg_esperando,
       s.blocking_session bloqueada_por, s.sql_id
  FROM v$session s
 WHERE s.username = 'BIOMETRICO'
   AND s.status = 'ACTIVE'
   AND s.wait_class <> 'Idle'
 ORDER BY s.seconds_in_wait DESC;

-- 1b) Quien tiene el bloqueo (si el bloque 1 mostro BLOQUEADA_POR): que hace esa sesion
SELECT s.sid, s.serial#, s.username, s.program programa, s.machine, s.status,
       s.logon_time, s.last_call_et seg_desde_ultima_llamada
  FROM v$session s
 WHERE s.sid IN (SELECT blocking_session FROM v$session WHERE blocking_session IS NOT NULL);

-- 2) Las sentencias del guardado, por tiempo total (ultimas 2 horas)
SELECT sql_id,
       executions                                         ejecuciones,
       ROUND(elapsed_time / 1e6, 2)                       seg_total,
       ROUND(elapsed_time / NULLIF(executions, 0) / 1e3, 1) ms_por_ejecucion,
       SUBSTR(REPLACE(sql_text, CHR(10), ' '), 1, 90)     sql_texto
  FROM v$sql
 WHERE parsing_schema_name = 'BIOMETRICO'
   AND last_active_time > SYSDATE - 2/24
   AND (UPPER(sql_text) LIKE '%SERV_MED_%' OR UPPER(sql_text) LIKE '%PR_SERVICIO_MED_EXAMEN%')
 ORDER BY elapsed_time DESC
 FETCH FIRST 25 ROWS ONLY;
