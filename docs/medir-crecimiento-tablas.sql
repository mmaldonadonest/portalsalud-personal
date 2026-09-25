-- =====================================================================================
-- medir-crecimiento-tablas.sql   -   SOLO LECTURA. No inserta, no borra, no modifica.
--
-- Mide el crecimiento REAL de las tablas del servicio medico para poder proyectar
-- capacidad a 1, 3 y 5 anos. Pensado para BIOMETRICO@PDBPRD, pero corre igual en QA.
--
-- QUE HACE
--   Por cada tabla SERV_MED_*: filas exactas, MB que ocupa, y -si tiene alguna columna
--   de fecha- cuantas filas entraron en los ultimos 30 / 90 / 365 dias, mas la fecha del
--   registro mas viejo y del mas nuevo. Con eso sale la tasa por mes sin adivinarla.
--
--   La columna de fecha NO va escrita a mano: el script la descubre del diccionario,
--   prefiriendo en este orden FECHA* > CREATED_AT > DATE_* > cualquier DATE/TIMESTAMP.
--   Asi funciona igual en las tablas del legacy (cuyos nombres de columna no conocemos)
--   que en las del portal.
--
-- COSTO
--   Hace un COUNT(*) por tabla: ~84 tablas, casi todas de 3,000 filas; la mas grande es
--   SERV_MED_TAG. En QA tardo poco. Aun asi, correrlo fuera de horario de captura.
--
-- COMO LEER LA SALIDA
--   * FILAS_365 / 12  = filas por mes de ese anio. Es la tasa mas confiable.
--   * FILAS_30 vs FILAS_90/3  dice si el ritmo se acelero o fue un mes atipico.
--   * Si ANTIGUEDAD_MESES es chica y las filas son muchas, la tabla es nueva y esta
--     llenandose rapido: ojo al proyectar linealmente.
--   * Si no hay columna de fecha (las tablas EAV del legacy no la tienen), la tasa se
--     deriva del driver: ver la seccion 3 de abajo.
--
-- EN SQL DEVELOPER: Run Script (F5) y panel DBMS Output abierto.
-- =====================================================================================

SET SERVEROUTPUT ON SIZE UNLIMITED
SET LINESIZE 200

-- -------------------------------------------------------------------------------------
-- 1. TAMANO EN DISCO  (instantaneo: sale del diccionario, no cuenta filas)
-- -------------------------------------------------------------------------------------
PROMPT
PROMPT === MB por tabla (las 15 mas grandes) ===
SELECT segment_name AS tabla,
       ROUND(SUM(bytes)/1024/1024, 1) AS mb
  FROM user_segments
 WHERE segment_type = 'TABLE'
   AND segment_name LIKE 'SERV\_MED\_%' ESCAPE '\'
 GROUP BY segment_name
 ORDER BY SUM(bytes) DESC
 FETCH FIRST 15 ROWS ONLY;

PROMPT
PROMPT === MB totales del servicio medico (tablas + indices + LOBs) ===
SELECT ROUND(SUM(bytes)/1024/1024, 1) AS mb_total
  FROM user_segments
 WHERE segment_name LIKE 'SERV\_MED\_%' ESCAPE '\'
    OR segment_name IN (SELECT index_name FROM user_indexes
                         WHERE table_name LIKE 'SERV\_MED\_%' ESCAPE '\')
    OR segment_name IN (SELECT segment_name FROM user_lobs
                         WHERE table_name LIKE 'SERV\_MED\_%' ESCAPE '\');

-- -------------------------------------------------------------------------------------
-- 2. FILAS Y RITMO DE ENTRADA, tabla por tabla
-- -------------------------------------------------------------------------------------
PROMPT
PROMPT === filas exactas y filas nuevas por ventana ===
DECLARE
  v_col      VARCHAR2(128);
  v_filas    NUMBER;
  v_30       NUMBER;
  v_90       NUMBER;
  v_365      NUMBER;
  v_min      DATE;
  v_max      DATE;
  v_meses    NUMBER;
  v_sql      VARCHAR2(4000);

  -- Elige la columna de fecha mas representativa de la tabla.
  FUNCTION col_fecha(p_tabla VARCHAR2) RETURN VARCHAR2 IS
    v VARCHAR2(128);
  BEGIN
    SELECT column_name INTO v FROM (
      SELECT column_name,
             CASE WHEN column_name LIKE 'FECHA%'      THEN 1
                  WHEN column_name = 'CREATED_AT'     THEN 2
                  WHEN column_name LIKE 'DATE\_%' ESCAPE '\' THEN 3
                  WHEN column_name LIKE '%FECHA%'     THEN 4
                  ELSE 5 END AS prioridad
        FROM user_tab_columns
       WHERE table_name = p_tabla
         AND data_type IN ('DATE', 'TIMESTAMP(6)', 'TIMESTAMP(3)', 'TIMESTAMP(0)')
       ORDER BY prioridad, column_id
    ) WHERE ROWNUM = 1;
    RETURN v;
  EXCEPTION WHEN NO_DATA_FOUND THEN RETURN NULL;
  END;
BEGIN
  DBMS_OUTPUT.PUT_LINE(
    RPAD('TABLA', 34) || LPAD('FILAS', 12) || LPAD('30d', 9) || LPAD('90d', 9) ||
    LPAD('365d', 10) || '  ' || RPAD('COL_FECHA', 18) || 'DESDE      HASTA');
  DBMS_OUTPUT.PUT_LINE(RPAD('-', 130, '-'));

  FOR t IN (SELECT table_name FROM user_tables
             WHERE table_name LIKE 'SERV\_MED\_%' ESCAPE '\'
             ORDER BY table_name) LOOP

    EXECUTE IMMEDIATE 'SELECT COUNT(*) FROM ' || t.table_name INTO v_filas;
    v_col := col_fecha(t.table_name);

    IF v_col IS NULL OR v_filas = 0 THEN
      DBMS_OUTPUT.PUT_LINE(RPAD(t.table_name, 34) || LPAD(TO_CHAR(v_filas, '999,999,999'), 12)
        || LPAD('-', 9) || LPAD('-', 9) || LPAD('-', 10) || '  '
        || RPAD(NVL(v_col, '(sin fecha)'), 18));
    ELSE
      v_sql := 'SELECT COUNT(CASE WHEN ' || v_col || ' >= SYSDATE-30  THEN 1 END),'
            || '       COUNT(CASE WHEN ' || v_col || ' >= SYSDATE-90  THEN 1 END),'
            || '       COUNT(CASE WHEN ' || v_col || ' >= SYSDATE-365 THEN 1 END),'
            || '       MIN(' || v_col || '), MAX(' || v_col || ')'
            || '  FROM ' || t.table_name;
      EXECUTE IMMEDIATE v_sql INTO v_30, v_90, v_365, v_min, v_max;

      DBMS_OUTPUT.PUT_LINE(RPAD(t.table_name, 34)
        || LPAD(TO_CHAR(v_filas, '999,999,999'), 12)
        || LPAD(TO_CHAR(v_30,  '999,999'), 9)
        || LPAD(TO_CHAR(v_90,  '999,999'), 9)
        || LPAD(TO_CHAR(v_365, '9,999,999'), 10) || '  '
        || RPAD(v_col, 18)
        || TO_CHAR(v_min, 'MM/YYYY') || '    ' || TO_CHAR(v_max, 'MM/YYYY'));
    END IF;
  END LOOP;
END;
/

-- -------------------------------------------------------------------------------------
-- 3. LOS DRIVERS DE NEGOCIO
--    Todo lo demas es un multiplicador de estos. Aqui se miden directo.
-- -------------------------------------------------------------------------------------
-- PLANTILLA, no se ejecuta: el nombre de la columna de fecha sale del bloque 2 de arriba.
-- Descomentar sustituyendo <COL> por lo que reporte la columna COL_FECHA de esa tabla.
--
--   SELECT TO_CHAR(<COL>,'YYYY-MM') AS mes, COUNT(*) AS examenes
--     FROM SERV_MED_RESULTADO_EXAMEN
--    WHERE <COL> >= ADD_MONTHS(SYSDATE,-24)
--    GROUP BY TO_CHAR(<COL>,'YYYY-MM') ORDER BY 1;

PROMPT
PROMPT === altas de empleados por mes (driver del pre-test) ===
-- Empleados vigentes (EMP_STATUS = 0). Cambiar el nombre de tabla/columna si difiere.
-- SELECT TO_CHAR(EMP_FECHA_INGRESO,'YYYY-MM') mes, COUNT(*) altas
--   FROM <tabla de empleados>
--  WHERE EMP_FECHA_INGRESO >= ADD_MONTHS(SYSDATE,-24)
--  GROUP BY TO_CHAR(EMP_FECHA_INGRESO,'YYYY-MM') ORDER BY 1;

PROMPT
PROMPT === multiplicadores ya conocidos (24-sep-2026, medidos en el ETL) ===
PROMPT   tags por examen      = 550,560 / 3,213  =~ 171
PROMPT   archivos por examen  =  21,448 / 3,213  =~ 6.7
PROMPT   filas clinicas/examen = 1 en cada una de ~53 tablas
PROMPT   altas de empleados    = 652 en 90 dias  =~ 217 / mes

-- -------------------------------------------------------------------------------------
-- 4. LO QUE NO ESTA EN ORACLE: los binarios
-- -------------------------------------------------------------------------------------
-- SERV_MED_FS_FILE guarda METADATOS; el archivo vive en el filesystem
-- (portal.files.root). Ese es el consumo que crece mas rapido y NO lo ve ninguna
-- consulta de aqui. Para medirlo, en el servidor:
--
--     Linux:    du -sh /mnt/data/onedev/apps/exec/filessalud
--               find  /mnt/data/onedev/apps/exec/filessalud -type f | wc -l
--     Windows:  powershell "(Get-ChildItem -Recurse C:\portal-salud\files |
--                            Measure-Object -Sum Length).Sum / 1GB"
--
-- Y el peso real de los binarios sale de la propia tabla, sin tocar el disco.
-- Va dentro de un bloque con guarda porque en BIOMETRICO la tabla NO EXISTE hasta que se
-- instale el portal: sin la guarda, el script cerraria con un ORA-00942 confuso.
DECLARE
  v_existe NUMBER;
  v_n NUMBER; v_gb NUMBER; v_prom NUMBER; v_max NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_existe FROM user_tables WHERE table_name = 'SERV_MED_FS_FILE';
  IF v_existe = 0 THEN
    DBMS_OUTPUT.PUT_LINE('[SKIP] SERV_MED_FS_FILE aun no existe en este esquema: '
                         || 'el portal no esta instalado. Medir el directorio con du/PowerShell.');
    RETURN;
  END IF;
  SELECT COUNT(*), ROUND(SUM(SIZE_BYTES)/1024/1024/1024, 2),
         ROUND(AVG(SIZE_BYTES)/1024/1024, 2), ROUND(MAX(SIZE_BYTES)/1024/1024, 2)
    INTO v_n, v_gb, v_prom, v_max
    FROM SERV_MED_FS_FILE;
  DBMS_OUTPUT.PUT_LINE('archivos: ' || v_n || ' | GB totales: ' || v_gb ||
                       ' | MB promedio: ' || v_prom || ' | MB del mayor: ' || v_max);
END;
/

-- Medido en QA el 24-sep-2026, como referencia:
--   21,677 archivos · 13.98 GB · 0.66 MB promedio · 2 MB el mayor
--   contra 224.8 MB de TODO el esquema Oracle.
--   El filesystem pesa ~62 veces mas que la base: ahi esta el problema de capacidad,
--   no en el tablespace.
