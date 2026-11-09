-- =====================================================================================
-- BASE DEL PORTAL (JDBC, SERV_MED_* dentro de BIOMETRICO), NO ORDS. La corre el usuario.
-- 3-nov-2026: regla nueva de la Carga de Excel. Un renglon cuyo NSS NO existe en el biometrico
-- (ej. 1 ATENCIONES NOV renglon 13305) ENTRA y CUENTA en los totales, marcado EN_BIOMETRICO = 'N'
-- (sin nombre, genero, edad, cuenta ni puesto). Los NSS de relleno (00000000001...) y los
-- arrastrados en Excel siguen sin entrar.
--
-- CORRER ANTES DE DESPLEGAR EL WAR: el WAR nuevo inserta EN_BIOMETRICO; sin la columna, «Cargar»
-- falla completo (ORA-00904). Validar no se afecta.
--
-- Valores: 'S' = NSS encontrado en el biometrico | 'N' = no encontrado, entro marcado |
--          NULL = sin persona (NSS 00000000000) o cargado antes de esta regla (todos validados).
-- Aplica a los 7 layouts con empleado (EXAMEN NUEVO INGRESO y consumibles no se cruzan con el
-- biometrico). Las vistas SERV_MED_V_* no cambian: siguen leyendo ESTADO_REGISTRO = 'VALIDO'.
-- Idempotente: si la columna ya existe se salta.
-- =====================================================================================
SET SERVEROUTPUT ON

DECLARE
  TYPE t_lista IS TABLE OF VARCHAR2(30);
  v_tablas t_lista := t_lista('SERV_MED_XLS_ATENCION', 'SERV_MED_XLS_EXAMEN_PER', 'SERV_MED_XLS_EXAMEN_POS',
                              'SERV_MED_XLS_INCAPACIDAD', 'SERV_MED_XLS_ACCIDENTE', 'SERV_MED_XLS_ANTIDOPING',
                              'SERV_MED_XLS_MATERNIDAD');
  v_n NUMBER;
BEGIN
  FOR i IN 1 .. v_tablas.COUNT LOOP
    SELECT COUNT(*) INTO v_n FROM user_tab_columns
     WHERE table_name = v_tablas(i) AND column_name = 'EN_BIOMETRICO';
    IF v_n = 0 THEN
      EXECUTE IMMEDIATE 'ALTER TABLE ' || v_tablas(i) || ' ADD (EN_BIOMETRICO VARCHAR2(1) '
                     || 'CONSTRAINT ' || SUBSTR('SERV_MED_CK_' || REPLACE(v_tablas(i), 'SERV_MED_XLS_') || '_BIO', 1, 30)
                     || ' CHECK (EN_BIOMETRICO IN (''S'', ''N'')))';
      EXECUTE IMMEDIATE 'COMMENT ON COLUMN ' || v_tablas(i) || '.EN_BIOMETRICO IS '
                     || '''S = NSS en el biometrico; N = no existe, entro marcado (regla 3-nov-2026); NULL = sin persona o anterior a la regla''';
      DBMS_OUTPUT.PUT_LINE('  ' || RPAD(v_tablas(i), 28) || 'columna agregada');
    ELSE
      DBMS_OUTPUT.PUT_LINE('  ' || RPAD(v_tablas(i), 28) || 'ya la tenia');
    END IF;
  END LOOP;
END;
/

-- Comprobacion (solo lectura): deben salir 7 renglones
SELECT table_name, column_name, data_type, data_length, nullable
  FROM user_tab_columns
 WHERE table_name LIKE 'SERV_MED_XLS%' AND column_name = 'EN_BIOMETRICO'
 ORDER BY table_name;

-- Para despues de cargar (solo lectura): cuantos entraron marcados, por lote
-- SELECT LOTE_ID, PREDIO, TRUNC(FECHA, 'MM') MES, COUNT(*) N
--   FROM SERV_MED_XLS_ATENCION WHERE EN_BIOMETRICO = 'N'
--  GROUP BY LOTE_ID, PREDIO, TRUNC(FECHA, 'MM') ORDER BY 1, 2, 3;
