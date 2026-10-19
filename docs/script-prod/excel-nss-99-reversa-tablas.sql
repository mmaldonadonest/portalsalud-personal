-- =============================================================================
-- CARGA DE EXCEL POR NSS -- REVERSA de las tablas nuevas (excel-nss-01-ddl-tablas.sql)
--
-- BASE: BIOMETRICO de produccion. Lo corre el usuario.
-- PARA QUE: recrear las tablas con columnas ajustadas mientras se prueban los layouts
--           (hasta la carga de prueba del paso 6).
--
-- SEGURO: solo borra las tablas SERV_MED_XLS_* (6 del 10-oct + 5 del 12-oct) y SE NIEGA si alguna
--         tiene datos. Las vistas SERV_MED_V_* quedan INVALID hasta recrear las tablas.
--         No toca la carga vieja (SERV_MED_BITACORA_*) ni ninguna otra tabla.
-- =============================================================================
SET SERVEROUTPUT ON

DECLARE
  TYPE t_lista IS TABLE OF VARCHAR2(30);
  -- Orden: primero las de evento (tienen FK al lote), al final el lote. EXAMEN_POS, ANTIDOPING,
  -- MEDICAMENTO y MATERIAL_FIJO desde el 12-oct-2026 (excel-nss-06 y excel-nss-07); si no existen se
  -- reportan y se saltan.
  v_tablas t_lista := t_lista('SERV_MED_XLS_ATENCION','SERV_MED_XLS_EXAMEN_ING','SERV_MED_XLS_EXAMEN_PER',
                              'SERV_MED_XLS_INCAPACIDAD','SERV_MED_XLS_ACCIDENTE',
                              'SERV_MED_XLS_EXAMEN_POS','SERV_MED_XLS_ANTIDOPING',
                              'SERV_MED_XLS_MEDICAMENTO','SERV_MED_XLS_MATERIAL_FIJO',
                              'SERV_MED_XLS_MATERNIDAD','SERV_MED_XLS_LOTE');
  v_n     NUMBER;
  v_total NUMBER := 0;
BEGIN
  -- 1) Freno: si hay un solo renglon cargado, no se borra nada
  FOR i IN 1 .. v_tablas.COUNT LOOP
    BEGIN
      EXECUTE IMMEDIATE 'SELECT COUNT(*) FROM ' || v_tablas(i) INTO v_n;
      DBMS_OUTPUT.PUT_LINE(RPAD(v_tablas(i), 30) || v_n || ' renglones');
      v_total := v_total + v_n;
    EXCEPTION WHEN OTHERS THEN
      IF SQLCODE = -942 THEN DBMS_OUTPUT.PUT_LINE(RPAD(v_tablas(i), 30) || 'no existe');
      ELSE RAISE; END IF;
    END;
  END LOOP;

  IF v_total > 0 THEN
    DBMS_OUTPUT.PUT_LINE('*** ALTO: hay ' || v_total || ' renglones cargados. No se borra nada.');
    DBMS_OUTPUT.PUT_LINE('    Para recrear con datos hay que planearlo aparte (ALTER, no DROP).');
    RETURN;
  END IF;

  -- 2) Tablas vacias: se borran
  FOR i IN 1 .. v_tablas.COUNT LOOP
    BEGIN
      EXECUTE IMMEDIATE 'DROP TABLE ' || v_tablas(i) || ' PURGE';
      DBMS_OUTPUT.PUT_LINE('Borrada ' || v_tablas(i));
    EXCEPTION WHEN OTHERS THEN
      IF SQLCODE <> -942 THEN RAISE; END IF;
    END;
  END LOOP;
END;
/

-- Verificacion: debe salir "no rows selected"
SELECT table_name FROM user_tables WHERE table_name LIKE 'SERV_MED_XLS%';
