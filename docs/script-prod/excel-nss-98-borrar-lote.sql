-- =============================================================================
-- CARGA DE EXCEL POR NSS -- BORRAR UN LOTE (lo que entro con un archivo)
--
-- BASE: BIOMETRICO de produccion. Lo corre el usuario.
-- Uso: poner el numero de lote (sale en el reporte del cargador: "lote N") y correr con
--      v_forzar = 'N' (simulacro: solo cuenta). Si los conteos cuadran, v_forzar = 'S'.
-- Solo toca SERV_MED_XLS_*. No toca la carga vieja ni las tablas vivas del portal.
-- =============================================================================
SET SERVEROUTPUT ON

DECLARE
  v_lote    CONSTANT NUMBER      := NULL;   -- <== numero de lote
  v_forzar  CONSTANT VARCHAR2(1) := 'N';    -- <== 'N' simula | 'S' borra

  TYPE t_lista IS TABLE OF VARCHAR2(30);
  -- Las 5 ultimas desde el 12-oct-2026 (excel-nss-06, 07 y 08); si aun no existen se saltan
  v_tablas t_lista := t_lista('SERV_MED_XLS_ATENCION','SERV_MED_XLS_EXAMEN_ING','SERV_MED_XLS_EXAMEN_PER',
                              'SERV_MED_XLS_INCAPACIDAD','SERV_MED_XLS_ACCIDENTE',
                              'SERV_MED_XLS_EXAMEN_POS','SERV_MED_XLS_ANTIDOPING',
                              'SERV_MED_XLS_MEDICAMENTO','SERV_MED_XLS_MATERIAL_FIJO',
                              'SERV_MED_XLS_MATERNIDAD');
  v_n      NUMBER;
  v_layout VARCHAR2(30);
  v_archivo VARCHAR2(400);
BEGIN
  IF v_lote IS NULL THEN
    DBMS_OUTPUT.PUT_LINE('*** Falta poner v_lote. No se hizo nada.');
    RETURN;
  END IF;

  BEGIN
    SELECT LAYOUT, ARCHIVO INTO v_layout, v_archivo FROM SERV_MED_XLS_LOTE WHERE ID = v_lote;
  EXCEPTION WHEN NO_DATA_FOUND THEN
    DBMS_OUTPUT.PUT_LINE('*** No existe el lote ' || v_lote || '. No se hizo nada.');
    RETURN;
  END;
  DBMS_OUTPUT.PUT_LINE('Lote ' || v_lote || ' | ' || v_layout || ' | ' || v_archivo);
  DBMS_OUTPUT.PUT_LINE(CASE v_forzar WHEN 'S' THEN '*** BORRANDO ***' ELSE 'SIMULACRO (no se borra nada)' END);

  FOR i IN 1 .. v_tablas.COUNT LOOP
    BEGIN
      EXECUTE IMMEDIATE 'SELECT COUNT(*) FROM ' || v_tablas(i) || ' WHERE LOTE_ID = :1' INTO v_n USING v_lote;
    EXCEPTION WHEN OTHERS THEN
      IF SQLCODE = -942 THEN v_n := 0; ELSE RAISE; END IF;   -- tabla aun no creada
    END;
    IF v_n > 0 THEN
      DBMS_OUTPUT.PUT_LINE('  ' || RPAD(v_tablas(i), 28) || v_n || ' renglones');
      IF v_forzar = 'S' THEN
        EXECUTE IMMEDIATE 'DELETE FROM ' || v_tablas(i) || ' WHERE LOTE_ID = :1' USING v_lote;
      END IF;
    END IF;
  END LOOP;

  IF v_forzar = 'S' THEN
    DELETE FROM SERV_MED_XLS_LOTE WHERE ID = v_lote;
    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Lote ' || v_lote || ' borrado.');
  END IF;
END;
/

-- Que quedo (solo lectura)
SELECT ID, LAYOUT, ESTADO, FILAS_LEIDAS, FILAS_VALIDAS, FILAS_DUPLICADAS, FILAS_ERROR, CREATED_AT
  FROM SERV_MED_XLS_LOTE ORDER BY ID DESC;
