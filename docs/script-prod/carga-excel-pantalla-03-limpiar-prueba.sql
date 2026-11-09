-- =====================================================================================
-- BASE DEL PORTAL (JDBC, SERV_MED_* dentro de BIOMETRICO), NO ORDS. La corre el usuario.
-- 3-nov-2026: limpiar la carga de PRUEBA hecha desde la pantalla «Carga de Excel»
-- (archivo prueba-2-renglones-atenciones.xlsx: 2 atenciones de AIFA, julio 2026).
--
-- Uso:
--   1. Correr el BLOQUE 1 (solo lectura) y tomar el ID del lote de la prueba.
--   2. Poner ese ID en v_lote y correr el BLOQUE 2 con v_forzar = 'N' (simulacro: solo cuenta).
--   3. Si los conteos cuadran (2 renglones), correr otra vez con v_forzar = 'S'.
--
-- Que borra: los renglones del lote en SERV_MED_XLS_ATENCION y el lote en SERV_MED_XLS_LOTE.
-- El original en SERV_MED_FS_FILE queda STATUS='DELETED' (igual que el portal: deja de verse y
-- descargarse; el binario se queda en disco).
-- Candado: solo borra un lote de ATENCIONES cargado desde la pantalla con 10 renglones o menos,
-- para que un ID equivocado no se lleve una carga real.
-- =====================================================================================
SET SERVEROUTPUT ON

-- BLOQUE 1 (solo lectura): los ultimos lotes; el de la prueba es ARCHIVO = prueba-2-renglones-atenciones.xlsx
SELECT ID, LAYOUT, ARCHIVO, PREDIO, PERIODO_DESDE, PERIODO_HASTA, FILAS_LEIDAS, FILAS_VALIDAS,
       FILAS_DUPLICADAS, FILAS_ERROR, ESTADO, CREATED_BY, CREATED_AT
  FROM SERV_MED_XLS_LOTE
 ORDER BY ID DESC
 FETCH FIRST 10 ROWS ONLY;

-- BLOQUE 2: borrar
DECLARE
  -- 3-nov-2026 ya se uso para los lotes 101 y 121 (2 renglones + 1 original cada uno, borrados). Se
  -- deja en blanco y en simulacro para que volver a correrlo no borre nada por accidente.
  v_lote    CONSTANT NUMBER      := NULL;   -- <== ID del lote de la prueba (ya usado: 101 y 121)
  v_forzar  CONSTANT VARCHAR2(1) := 'N';    -- <== 'N' simula | 'S' borra

  v_layout  SERV_MED_XLS_LOTE.LAYOUT%TYPE;
  v_archivo SERV_MED_XLS_LOTE.ARCHIVO%TYPE;
  v_por     SERV_MED_XLS_LOTE.CREATED_BY%TYPE;
  v_leidas  SERV_MED_XLS_LOTE.FILAS_LEIDAS%TYPE;
  v_filas   NUMBER;
  v_orig    NUMBER;
BEGIN
  IF v_lote IS NULL THEN
    DBMS_OUTPUT.PUT_LINE('*** Falta poner v_lote. No se hizo nada.');
    RETURN;
  END IF;

  BEGIN
    SELECT LAYOUT, ARCHIVO, CREATED_BY, FILAS_LEIDAS INTO v_layout, v_archivo, v_por, v_leidas
      FROM SERV_MED_XLS_LOTE WHERE ID = v_lote;
  EXCEPTION WHEN NO_DATA_FOUND THEN
    DBMS_OUTPUT.PUT_LINE('*** No existe el lote ' || v_lote || '. No se hizo nada.');
    RETURN;
  END;
  DBMS_OUTPUT.PUT_LINE('Lote ' || v_lote || ' | ' || v_layout || ' | ' || v_archivo || ' | ' || v_por
                       || ' | ' || v_leidas || ' leidos');

  -- Candado: la prueba es de ATENCIONES, desde la pantalla (CREATED_BY = usuario, no la consola) y chica
  IF v_layout <> 'ATENCIONES' OR v_por = 'CARGA_EXCEL_NSS' OR v_leidas > 10 THEN
    DBMS_OUTPUT.PUT_LINE('*** Este lote no parece la prueba (layout, origen o tamano). No se hizo nada.');
    RETURN;
  END IF;

  SELECT COUNT(*) INTO v_filas FROM SERV_MED_XLS_ATENCION WHERE LOTE_ID = v_lote;
  SELECT COUNT(*) INTO v_orig FROM SERV_MED_FS_FILE
   WHERE FILE_TYPE = 'carga-excel' AND STATUS = 'ACTIVE'
     AND BUSINESS_KEY IN ('XLS_LOTE:' || v_lote, 'XLS_LOTE:' || v_lote || ':CORRECCIONES');
  DBMS_OUTPUT.PUT_LINE('  SERV_MED_XLS_ATENCION       ' || v_filas || ' renglones (esperado: 2)');
  DBMS_OUTPUT.PUT_LINE('  SERV_MED_FS_FILE (original) ' || v_orig || ' archivo(s) (esperado: 1)');

  IF v_forzar <> 'S' THEN
    DBMS_OUTPUT.PUT_LINE('SIMULACRO: no se borro nada. Si cuadra, poner v_forzar = ''S''.');
    RETURN;
  END IF;

  DELETE FROM SERV_MED_XLS_ATENCION WHERE LOTE_ID = v_lote;
  DELETE FROM SERV_MED_XLS_LOTE WHERE ID = v_lote;
  UPDATE SERV_MED_FS_FILE SET STATUS = 'DELETED', UPDATED_AT = SYSTIMESTAMP, UPDATED_BY = 'LIMPIEZA_PRUEBA'
   WHERE FILE_TYPE = 'carga-excel' AND STATUS = 'ACTIVE'
     AND BUSINESS_KEY IN ('XLS_LOTE:' || v_lote, 'XLS_LOTE:' || v_lote || ':CORRECCIONES');
  COMMIT;
  DBMS_OUTPUT.PUT_LINE('*** Lote ' || v_lote || ' borrado.');
END;
/

-- BLOQUE 3 (solo lectura): renglones de Excel en julio 2026 por predio; tras borrar la prueba no
-- debe quedar ni AIFA (prueba-2-renglones-atenciones) ni MACRO II (prueba-2-renglones-sin-biometrico)
SELECT PREDIO, COUNT(*) AS RENGLONES_JUL_2026
  FROM SERV_MED_XLS_ATENCION
 WHERE FECHA >= DATE '2026-07-01' AND FECHA < DATE '2026-08-01'
 GROUP BY PREDIO ORDER BY PREDIO;
