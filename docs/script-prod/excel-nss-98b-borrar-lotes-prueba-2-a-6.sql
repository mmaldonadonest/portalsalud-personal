-- =============================================================================
-- CARGA DE EXCEL POR NSS -- BORRAR LOS LOTES DE PRUEBA 2 A 6 DE UNA VEZ (pendiente P1)
--
-- BASE: BIOMETRICO de produccion. Lo corre el usuario.
--
-- QUE BORRA: los 5 lotes de la prueba interna del 10-oct-2026 (layouts generados desde la carga
--   vieja, NSS deducido por nombre): 2 ATENCIONES, 3 EXAMEN_INGRESO, 4 EXAMEN_PERIODICO,
--   5 INCAPACIDADES, 6 ACCIDENTES. Misma logica que excel-nss-98-borrar-lote.sql, en un solo paso.
--   Solo toca SERV_MED_XLS_*. No toca la carga vieja ni las tablas vivas del portal.
--
-- CANDADO: solo borra un lote si su ARCHIVO dice "PRUEBA desde carga vieja". Si alguno de los 5 no
--   lo dice (o no existe), no se borra NADA y se avisa: asi es imposible llevarse por error un lote
--   que no sea de esa prueba.
--
-- USO: correr con v_forzar = 'N' (simulacro: solo cuenta). Si los conteos cuadran, v_forzar = 'S'.
--   Todo o nada: un solo COMMIT al final.
--
-- CONTEOS ESPERADOS (de la salida del cuadre del 12-oct): ATENCION 8,614 | EXAMEN_ING 3,671 |
--   EXAMEN_PER 1,103 | INCAPACIDAD 1,319 | ACCIDENTE 180 = 14,887 renglones.
-- =============================================================================
SET SERVEROUTPUT ON

DECLARE
  v_forzar  CONSTANT VARCHAR2(1) := 'N';    -- <== 'N' simula | 'S' borra

  TYPE t_lista IS TABLE OF VARCHAR2(30);
  TYPE t_ids   IS TABLE OF NUMBER;
  v_lotes  t_ids   := t_ids(2, 3, 4, 5, 6);
  v_tablas t_lista := t_lista('SERV_MED_XLS_ATENCION','SERV_MED_XLS_EXAMEN_ING','SERV_MED_XLS_EXAMEN_PER',
                              'SERV_MED_XLS_INCAPACIDAD','SERV_MED_XLS_ACCIDENTE',
                              'SERV_MED_XLS_EXAMEN_POS','SERV_MED_XLS_ANTIDOPING',
                              'SERV_MED_XLS_MEDICAMENTO','SERV_MED_XLS_MATERIAL_FIJO',
                              'SERV_MED_XLS_MATERNIDAD');
  v_n       NUMBER;
  v_total   NUMBER := 0;
  v_layout  VARCHAR2(30);
  v_archivo VARCHAR2(400);
BEGIN
  -- 1) Candado: los 5 tienen que existir y ser de la prueba de la carga vieja
  FOR k IN 1 .. v_lotes.COUNT LOOP
    BEGIN
      SELECT LAYOUT, ARCHIVO INTO v_layout, v_archivo FROM SERV_MED_XLS_LOTE WHERE ID = v_lotes(k);
    EXCEPTION WHEN NO_DATA_FOUND THEN
      DBMS_OUTPUT.PUT_LINE('*** No existe el lote ' || v_lotes(k) || '. No se borro NADA.');
      RETURN;
    END;
    IF INSTR(UPPER(v_archivo), 'PRUEBA DESDE CARGA VIEJA') = 0 THEN
      DBMS_OUTPUT.PUT_LINE('*** El lote ' || v_lotes(k) || ' no es de la prueba de la carga vieja (' || v_archivo
                           || '). No se borro NADA.');
      RETURN;
    END IF;
  END LOOP;

  DBMS_OUTPUT.PUT_LINE(CASE v_forzar WHEN 'S' THEN '*** BORRANDO ***' ELSE 'SIMULACRO (no se borra nada)' END);

  -- 2) Por lote: contar (y borrar si v_forzar = 'S')
  FOR k IN 1 .. v_lotes.COUNT LOOP
    SELECT LAYOUT, ARCHIVO INTO v_layout, v_archivo FROM SERV_MED_XLS_LOTE WHERE ID = v_lotes(k);
    DBMS_OUTPUT.PUT_LINE('Lote ' || v_lotes(k) || ' | ' || v_layout);
    FOR i IN 1 .. v_tablas.COUNT LOOP
      BEGIN
        EXECUTE IMMEDIATE 'SELECT COUNT(*) FROM ' || v_tablas(i) || ' WHERE LOTE_ID = :1' INTO v_n USING v_lotes(k);
      EXCEPTION WHEN OTHERS THEN
        IF SQLCODE = -942 THEN v_n := 0; ELSE RAISE; END IF;   -- tabla aun no creada
      END;
      IF v_n > 0 THEN
        DBMS_OUTPUT.PUT_LINE('  ' || RPAD(v_tablas(i), 28) || v_n || ' renglones');
        v_total := v_total + v_n;
        IF v_forzar = 'S' THEN
          EXECUTE IMMEDIATE 'DELETE FROM ' || v_tablas(i) || ' WHERE LOTE_ID = :1' USING v_lotes(k);
        END IF;
      END IF;
    END LOOP;
    IF v_forzar = 'S' THEN
      DELETE FROM SERV_MED_XLS_LOTE WHERE ID = v_lotes(k);
    END IF;
  END LOOP;

  DBMS_OUTPUT.PUT_LINE('Total: ' || v_total || ' renglones (esperado 14,887)');
  IF v_forzar = 'S' THEN
    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Lotes 2 a 6 borrados.');
  END IF;
EXCEPTION WHEN OTHERS THEN
  ROLLBACK;
  DBMS_OUTPUT.PUT_LINE('*** Error, no se borro nada: ' || SQLERRM);
  RAISE;
END;
/

-- Que quedo (solo lectura). Despues de borrar con 'S' debe salir sin renglones.
SELECT ID, LAYOUT, ESTADO, FILAS_LEIDAS, FILAS_VALIDAS, FILAS_DUPLICADAS, FILAS_ERROR, CREATED_AT
  FROM SERV_MED_XLS_LOTE ORDER BY ID DESC;
