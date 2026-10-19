-- ============================================================================
-- CARGA DE EXCEL 2026 -- REVERSA
--
-- BASE: la del PORTAL (JDBC). En produccion eso es BIOMETRICO. NO es ORDS.
--
-- ESTE SCRIPT BORRA. Leelo completo antes de correrlo.
--
-- Dos niveles:
--   A) Vaciar los datos pero conservar las tablas (lo normal: recargar).
--   B) Eliminar las 3 tablas por completo (deshacer el DDL).
--
-- Es seguro por construccion: las 3 tablas son EXCLUSIVAS de esta carga.
-- Ningun otro modulo del portal las lee ni las escribe, asi que no hay
-- forma de borrar de mas. Se borran en orden por las llaves foraneas.
-- ============================================================================
SET SERVEROUTPUT ON
SET LINESIZE 200

-- ----------------------------------------------------------------------
-- A) VACIAR  --  'N' simula y solo cuenta, 'S' borra de verdad.
--    Opcional: limitar a un archivo o a una familia.
-- ----------------------------------------------------------------------
DECLARE
  v_forzar   CONSTANT VARCHAR2(1)   := 'N';     -- <== 'N' simula · 'S' BORRA
                                                --     Se deja SIEMPRE en 'N'. Se pone en 'S'
                                                --     para una corrida y se regresa.
                                                --     Corridas con 'S' en QA:
                                                --       30-sep: 2486/1003/48  (1er ensayo)
                                                --       30-sep: 3656/1255/66  (2o ensayo, antes
                                                --               del full; quedo en 0/0/0)
  v_familia  CONSTANT VARCHAR2(40)  := NULL;    -- NULL = todas
  v_archivo  CONSTANT VARCHAR2(400) := NULL;    -- NULL = todos

  v_eventos NUMBER; v_atribs NUMBER; v_lotes NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_eventos FROM SERV_MED_BITACORA_EVENTO
   WHERE (v_familia IS NULL OR FAMILIA        = v_familia)
     AND (v_archivo IS NULL OR ORIGEN_ARCHIVO = v_archivo);

  SELECT COUNT(*) INTO v_atribs FROM SERV_MED_BITACORA_ATRIBUTO a
   WHERE EXISTS (SELECT 1 FROM SERV_MED_BITACORA_EVENTO e
                  WHERE e.ID = a.EVENTO_ID
                    AND (v_familia IS NULL OR e.FAMILIA        = v_familia)
                    AND (v_archivo IS NULL OR e.ORIGEN_ARCHIVO = v_archivo));

  SELECT COUNT(*) INTO v_lotes FROM SERV_MED_BITACORA_LOTE
   WHERE (v_familia IS NULL OR FAMILIA = v_familia)
     AND (v_archivo IS NULL OR ARCHIVO = v_archivo);

  DBMS_OUTPUT.PUT_LINE('===========================================================');
  DBMS_OUTPUT.PUT_LINE(' REVERSA CARGA EXCEL - ' ||
       CASE v_forzar WHEN 'S' THEN '*** BORRANDO ***' ELSE 'SIMULACRO' END);
  DBMS_OUTPUT.PUT_LINE(' Filtro familia: ' || NVL(v_familia,'(todas)') ||
                       '   archivo: ' || NVL(v_archivo,'(todos)'));
  DBMS_OUTPUT.PUT_LINE('-----------------------------------------------------------');
  DBMS_OUTPUT.PUT_LINE(' atributos : ' || v_atribs);
  DBMS_OUTPUT.PUT_LINE(' eventos   : ' || v_eventos);
  DBMS_OUTPUT.PUT_LINE(' lotes     : ' || v_lotes);
  DBMS_OUTPUT.PUT_LINE('-----------------------------------------------------------');

  IF v_forzar <> 'S' THEN
    DBMS_OUTPUT.PUT_LINE('Simulacro: no se borro nada. Poner v_forzar := ''S'' para borrar.');
    RETURN;
  END IF;

  -- METRICA cuelga del LOTE, no del EVENTO: las familias sin persona -consumibles- no producen
  -- eventos. Se agrego el 30-sep-2026; antes esta reversa dejaba las metricas huerfanas.
  DELETE FROM SERV_MED_BITACORA_METRICA
   WHERE (v_familia IS NULL OR FAMILIA = v_familia)
     AND LOTE_ID IN (SELECT ID FROM SERV_MED_BITACORA_LOTE
                      WHERE (v_archivo IS NULL OR ARCHIVO = v_archivo));
  DBMS_OUTPUT.PUT_LINE('metricas borradas  : ' || SQL%ROWCOUNT);

  -- Orden obligado por las llaves foraneas: atributo -> evento -> lote
  DELETE FROM SERV_MED_BITACORA_ATRIBUTO a
   WHERE EXISTS (SELECT 1 FROM SERV_MED_BITACORA_EVENTO e
                  WHERE e.ID = a.EVENTO_ID
                    AND (v_familia IS NULL OR e.FAMILIA        = v_familia)
                    AND (v_archivo IS NULL OR e.ORIGEN_ARCHIVO = v_archivo));
  DBMS_OUTPUT.PUT_LINE('atributos borrados : ' || SQL%ROWCOUNT);

  DELETE FROM SERV_MED_BITACORA_EVENTO
   WHERE (v_familia IS NULL OR FAMILIA        = v_familia)
     AND (v_archivo IS NULL OR ORIGEN_ARCHIVO = v_archivo);
  DBMS_OUTPUT.PUT_LINE('eventos borrados   : ' || SQL%ROWCOUNT);

  DELETE FROM SERV_MED_BITACORA_LOTE
   WHERE (v_familia IS NULL OR FAMILIA = v_familia)
     AND (v_archivo IS NULL OR ARCHIVO = v_archivo);
  DBMS_OUTPUT.PUT_LINE('lotes borrados     : ' || SQL%ROWCOUNT);

  COMMIT;
  DBMS_OUTPUT.PUT_LINE('COMMIT hecho.');
END;
/

-- Comprobacion
SELECT 'ATRIBUTO' t, COUNT(*) filas FROM SERV_MED_BITACORA_ATRIBUTO
UNION ALL SELECT 'EVENTO',  COUNT(*) FROM SERV_MED_BITACORA_EVENTO
UNION ALL SELECT 'METRICA', COUNT(*) FROM SERV_MED_BITACORA_METRICA
UNION ALL SELECT 'LOTE',    COUNT(*) FROM SERV_MED_BITACORA_LOTE;


-- ----------------------------------------------------------------------
-- B) ELIMINAR LAS TABLAS  --  deshace el DDL por completo.
--    Descomentar solo si se decide que esta carga no va.
--    El orden importa por las llaves foraneas.
-- ----------------------------------------------------------------------
-- DROP TABLE SERV_MED_BITACORA_ATRIBUTO PURGE;
-- DROP TABLE SERV_MED_BITACORA_EVENTO   PURGE;
-- DROP TABLE SERV_MED_BITACORA_LOTE     PURGE;
