-- =============================================================================
-- CARGA DE EXCEL POR NSS -- VACIAR TODOS LOS LOTES DE PRUEBA (para recargar desde cero)
--
-- BASE: BIOMETRICO de produccion. Lo corre el usuario.
--
-- QUE BORRA: todos los renglones de las 10 tablas SERV_MED_XLS_* y todos los lotes de
--   SERV_MED_XLS_LOTE. Hoy solo hay lotes de prueba (TOLUCA 2025 y el material fijo de CHARCON).
--   No toca la carga vieja (SERV_MED_BITACORA_*) ni las tablas vivas del portal.
--
-- CANDADO: solo borra si TODOS los lotes son de prueba (su ARCHIVO dice "PRUEBA"). Si aparece uno
--   solo que no lo diga -por ejemplo, la captura oficial del servicio medico- no borra NADA y lo
--   lista. Asi este script nunca se lleva datos reales.
--
-- USO: correr con v_forzar = 'N' (simulacro: solo cuenta). Si los conteos cuadran, v_forzar = 'S'.
--   Todo o nada: un solo COMMIT al final.
-- =============================================================================
SET SERVEROUTPUT ON

DECLARE
  v_forzar  CONSTANT VARCHAR2(1) := 'N';    -- <== 'N' simula | 'S' borra

  TYPE t_lista IS TABLE OF VARCHAR2(30);
  v_tablas t_lista := t_lista('SERV_MED_XLS_ATENCION','SERV_MED_XLS_EXAMEN_ING','SERV_MED_XLS_EXAMEN_PER',
                              'SERV_MED_XLS_EXAMEN_POS','SERV_MED_XLS_INCAPACIDAD','SERV_MED_XLS_ACCIDENTE',
                              'SERV_MED_XLS_ANTIDOPING','SERV_MED_XLS_MATERNIDAD',
                              'SERV_MED_XLS_MEDICAMENTO','SERV_MED_XLS_MATERIAL_FIJO');
  v_n      NUMBER;
  v_total  NUMBER := 0;
  v_reales NUMBER := 0;
  v_lotes  NUMBER;
BEGIN
  -- 1) Candado: ningun lote que no sea de prueba
  FOR l IN (SELECT ID, LAYOUT, ARCHIVO FROM SERV_MED_XLS_LOTE
             WHERE INSTR(UPPER(ARCHIVO), 'PRUEBA') = 0 ORDER BY ID) LOOP
    v_reales := v_reales + 1;
    DBMS_OUTPUT.PUT_LINE('*** Lote ' || l.ID || ' (' || l.LAYOUT || ') NO es de prueba: ' || l.ARCHIVO);
  END LOOP;
  IF v_reales > 0 THEN
    DBMS_OUTPUT.PUT_LINE('*** Hay ' || v_reales || ' lote(s) que no son de prueba. No se borro NADA.');
    RETURN;
  END IF;

  SELECT COUNT(*) INTO v_lotes FROM SERV_MED_XLS_LOTE;
  DBMS_OUTPUT.PUT_LINE(CASE v_forzar WHEN 'S' THEN '*** BORRANDO ***' ELSE 'SIMULACRO (no se borra nada)' END);
  DBMS_OUTPUT.PUT_LINE('Lotes: ' || v_lotes || ' (todos de prueba)');

  -- 2) Renglones por tabla
  FOR i IN 1 .. v_tablas.COUNT LOOP
    EXECUTE IMMEDIATE 'SELECT COUNT(*) FROM ' || v_tablas(i) INTO v_n;
    DBMS_OUTPUT.PUT_LINE('  ' || RPAD(v_tablas(i), 28) || v_n || ' renglones');
    v_total := v_total + v_n;
    IF v_forzar = 'S' THEN
      EXECUTE IMMEDIATE 'DELETE FROM ' || v_tablas(i);
    END IF;
  END LOOP;

  DBMS_OUTPUT.PUT_LINE('Total: ' || v_total || ' renglones');
  IF v_forzar = 'S' THEN
    DELETE FROM SERV_MED_XLS_LOTE;
    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Tablas SERV_MED_XLS_* y lotes vaciados.');
  END IF;
EXCEPTION WHEN OTHERS THEN
  ROLLBACK;
  DBMS_OUTPUT.PUT_LINE('*** Error, no se borro nada: ' || SQLERRM);
  RAISE;
END;
/

-- Que quedo (solo lectura). Despues de borrar con 'S' debe salir sin renglones.
SELECT ID, LAYOUT, ESTADO, FILAS_LEIDAS, FILAS_VALIDAS, CREATED_AT, SUBSTR(ARCHIVO, 1, 70) ARCHIVO
  FROM SERV_MED_XLS_LOTE ORDER BY ID;
