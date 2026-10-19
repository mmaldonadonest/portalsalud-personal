-- =============================================================================
-- EXAMEN NUEVO INGRESO: + RESULTADOS PENDIENTES y OBSERVACIONES (13-oct-2026)
--
-- BASE: BIOMETRICO de produccion (PDBPRD). Lo corre el usuario.
--
-- POR QUE: el layout 2 EXAMEN NUEVO INGRESO se diseno sin esas dos columnas y el Excel original del
--   servicio medico si las trae (columnas DIAGNOSTICO | RESULTADOS PENDIENTES | OBSERVACIONES).
--
-- QUE HACE: agrega dos columnas (vacias, aceptan nulo) a SERV_MED_XLS_EXAMEN_ING, y (v2) OBSERVACIONES a
--   SERV_MED_XLS_EXAMEN_PER y SERV_MED_XLS_EXAMEN_POS. No borra ni modifica
--   datos. Se puede correr mas de una vez (si ya existen, lo dice y sigue).
--
-- ORDEN: correr ANTES de cargar el layout 2 con el cargador nuevo (si no, la carga falla con
--   ORA-00904 "identificador no valido").
--
-- REVERSA: ALTER TABLE SERV_MED_XLS_EXAMEN_ING DROP (RESULTADOS_PENDIENTES, OBSERVACIONES);
-- =============================================================================
SET SERVEROUTPUT ON

BEGIN
  EXECUTE IMMEDIATE 'ALTER TABLE SERV_MED_XLS_EXAMEN_ING ADD (RESULTADOS_PENDIENTES VARCHAR2(500))';
  DBMS_OUTPUT.PUT_LINE('RESULTADOS_PENDIENTES agregada');
EXCEPTION WHEN OTHERS THEN
  IF SQLCODE = -1430 THEN DBMS_OUTPUT.PUT_LINE('RESULTADOS_PENDIENTES ya existia'); ELSE RAISE; END IF;
END;
/

BEGIN
  EXECUTE IMMEDIATE 'ALTER TABLE SERV_MED_XLS_EXAMEN_ING ADD (OBSERVACIONES VARCHAR2(1000))';
  DBMS_OUTPUT.PUT_LINE('OBSERVACIONES agregada');
EXCEPTION WHEN OTHERS THEN
  IF SQLCODE = -1430 THEN DBMS_OUTPUT.PUT_LINE('OBSERVACIONES ya existia'); ELSE RAISE; END IF;
END;
/

-- v2 (13-oct-2026): OBSERVACIONES tambien en periodico y pos incapacidad (sus Excel originales la traen)
BEGIN
  EXECUTE IMMEDIATE 'ALTER TABLE SERV_MED_XLS_EXAMEN_PER ADD (OBSERVACIONES VARCHAR2(1000))';
  DBMS_OUTPUT.PUT_LINE('EXAMEN_PER.OBSERVACIONES agregada');
EXCEPTION WHEN OTHERS THEN
  IF SQLCODE = -1430 THEN DBMS_OUTPUT.PUT_LINE('EXAMEN_PER.OBSERVACIONES ya existia'); ELSE RAISE; END IF;
END;
/

BEGIN
  EXECUTE IMMEDIATE 'ALTER TABLE SERV_MED_XLS_EXAMEN_POS ADD (OBSERVACIONES VARCHAR2(1000))';
  DBMS_OUTPUT.PUT_LINE('EXAMEN_POS.OBSERVACIONES agregada');
EXCEPTION WHEN OTHERS THEN
  IF SQLCODE = -1430 THEN DBMS_OUTPUT.PUT_LINE('EXAMEN_POS.OBSERVACIONES ya existia'); ELSE RAISE; END IF;
END;
/

-- La vista de examenes no usa estas columnas, pero se recompila por si quedo invalida
ALTER VIEW SERV_MED_V_EXAMEN COMPILE;

-- VERIFICACION (solo lectura): deben salir 4 renglones (ING x2, PER y POS) y la vista VALID
SELECT table_name, column_name, data_type, data_length FROM user_tab_columns
 WHERE table_name IN ('SERV_MED_XLS_EXAMEN_ING', 'SERV_MED_XLS_EXAMEN_PER', 'SERV_MED_XLS_EXAMEN_POS')
   AND column_name IN ('RESULTADOS_PENDIENTES', 'OBSERVACIONES')
   AND NOT (table_name <> 'SERV_MED_XLS_EXAMEN_ING' AND column_name = 'RESULTADOS_PENDIENTES')
 ORDER BY 1, 2;

SELECT object_name, status FROM user_objects WHERE object_name = 'SERV_MED_V_EXAMEN';
