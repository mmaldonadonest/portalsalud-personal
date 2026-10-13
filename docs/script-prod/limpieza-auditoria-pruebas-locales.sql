-- =============================================================================
-- Limpieza de AUDITORIA que dejaron las pruebas locales contra produccion (10/11-oct-2026)
--
-- BASE: BIOMETRICO de produccion (tabla del portal SERV_MED_AUD_EVENT). Lo corre el usuario.
-- Las pruebas de PDF del examen (Chromium 00:16 y Edge 00:20 del 11-oct) abrieron el documento
-- "Expediente impreso" de los NSS 30048315698 y 17189842515: 4 renglones (2 por navegador).
--
-- Uso: correr primero tal cual (SOLO MUESTRA). Si los renglones son los esperados, poner
-- v_forzar := 'S' en el bloque 3 y correrlo.
-- =============================================================================
SET SERVEROUTPUT ON
SET LINESIZE 220
COLUMN entity_name FORMAT A22
COLUMN action      FORMAT A10
COLUMN username    FORMAT A14
COLUMN ip_address  FORMAT A18

-- 1) Los renglones de las pruebas de PDF (lo que se va a borrar)
SELECT ID, TO_CHAR(EVENT_TS, 'DD/MM/YYYY HH24:MI:SS') fecha, USERNAME, ENTITY_NAME, ENTITY_ID, ACTION, IP_ADDRESS
  FROM SERV_MED_AUD_EVENT
 WHERE ENTITY_NAME = 'Expediente impreso'
   AND USERNAME = '68958027838'
   AND ENTITY_ID IN ('30048315698', '17189842515')
   AND EVENT_TS >= TIMESTAMP '2026-10-11 00:00:00'
   AND EVENT_TS <  TIMESTAMP '2026-10-11 01:00:00'
 ORDER BY EVENT_TS;

-- 2) SOLO PARA REVISAR (no se borra): todo lo que registraron las corridas locales del 10/11-oct
--    desde este equipo (IP local). Si se quiere limpiar tambien, se decide aparte.
SELECT IP_ADDRESS, ENTITY_NAME, ACTION, COUNT(*) renglones,
       TO_CHAR(MIN(EVENT_TS), 'DD/MM HH24:MI') desde, TO_CHAR(MAX(EVENT_TS), 'DD/MM HH24:MI') hasta
  FROM SERV_MED_AUD_EVENT
 WHERE EVENT_TS >= TIMESTAMP '2026-10-10 16:00:00'
   AND EVENT_TS <  TIMESTAMP '2026-10-11 02:00:00'
   AND (IP_ADDRESS IN ('127.0.0.1', '0:0:0:0:0:0:0:1', '::1') OR IP_ADDRESS LIKE '192.168.%')
 GROUP BY IP_ADDRESS, ENTITY_NAME, ACTION
 ORDER BY 1, 2;

-- 3) Borrado de los renglones del bloque 1
DECLARE
  v_forzar CONSTANT VARCHAR2(1) := 'N';   -- <== 'N' solo cuenta | 'S' borra
  v_n      NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_n
    FROM SERV_MED_AUD_EVENT
   WHERE ENTITY_NAME = 'Expediente impreso'
     AND USERNAME = '68958027838'
     AND ENTITY_ID IN ('30048315698', '17189842515')
     AND EVENT_TS >= TIMESTAMP '2026-10-11 00:00:00'
     AND EVENT_TS <  TIMESTAMP '2026-10-11 01:00:00';
  DBMS_OUTPUT.PUT_LINE('Renglones de las pruebas de PDF: ' || v_n || ' (se esperan 4)');
  IF v_forzar <> 'S' THEN
    DBMS_OUTPUT.PUT_LINE('Simulacro: no se borro nada. Poner v_forzar := ''S'' para borrar.');
    RETURN;
  END IF;
  DELETE FROM SERV_MED_AUD_EVENT
   WHERE ENTITY_NAME = 'Expediente impreso'
     AND USERNAME = '68958027838'
     AND ENTITY_ID IN ('30048315698', '17189842515')
     AND EVENT_TS >= TIMESTAMP '2026-10-11 00:00:00'
     AND EVENT_TS <  TIMESTAMP '2026-10-11 01:00:00';
  DBMS_OUTPUT.PUT_LINE('Borrados: ' || SQL%ROWCOUNT);
  COMMIT;
END;
/
