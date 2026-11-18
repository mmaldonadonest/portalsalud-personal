-- ============================================================================================
-- P-035: ¿donde esta el SDI (salario diario integrado) en el biometrico? - 18-nov-2026
--
-- Base: BIOMETRICO (PDBPRD), conectado como BIOMETRICO. Solo diccionario de datos y conteos.
-- NO va a ORDS ni al portal.
-- SOLO LECTURA: no inserta, no borra, no cambia nada. Se puede correr cuantas veces se quiera.
--
-- Para que sirve: hoy el SDI se teclea a mano en Incapacidades («Salario diario») y en Accidentes
-- («SDI»). Para calcular el costo (C-043, C-047) hay que tomarlo del biometrico, y SM pidio el SDI
-- vigente a la fecha de inicio de la incapacidad y a la fecha del accidente (P-036). Este script
-- solo UBICA columnas candidatas y si tienen historia (fechas de vigencia); no muestra salarios
-- de nadie: solo cuantos registros hay con dato y el rango de valores, para saber si es diario,
-- mensual o basura.
--
-- Correr con:  @C:\miguel\portalsalud-personal\docs\script-prod\diagnostico-sdi-biometrico.sql
-- Salida en:   C:\miguel\diagnostico-sdi-salida.txt  (mandarla tal cual)
-- ============================================================================================
SET PAGESIZE 200
SET LINESIZE 220
SET SERVEROUTPUT ON SIZE UNLIMITED
COLUMN OWNER FORMAT A20
COLUMN TABLE_NAME FORMAT A40
COLUMN COLUMN_NAME FORMAT A35
COLUMN DATA_TYPE FORMAT A15
SPOOL C:\miguel\diagnostico-sdi-salida.txt

PROMPT === 1) Columnas candidatas a SDI o salario en todo lo que BIOMETRICO puede ver ===
SELECT c.OWNER, c.TABLE_NAME, c.COLUMN_NAME, c.DATA_TYPE,
       CASE WHEN o.OBJECT_TYPE = 'VIEW' THEN 'VISTA' ELSE 'TABLA' END TIPO
  FROM ALL_TAB_COLUMNS c
  JOIN ALL_OBJECTS o ON o.OWNER = c.OWNER AND o.OBJECT_NAME = c.TABLE_NAME AND o.OBJECT_TYPE IN ('TABLE', 'VIEW')
 WHERE c.OWNER NOT IN ('SYS', 'SYSTEM', 'XDB', 'MDSYS', 'CTXSYS', 'ORDS_METADATA', 'APEX_PUBLIC_USER', 'WMSYS', 'OLAPSYS')
   AND c.OWNER NOT LIKE 'APEX%'
   AND (   REGEXP_LIKE(c.COLUMN_NAME, '(^|_)SDI(_|$)')
        OR c.COLUMN_NAME LIKE '%SALARIO%'
        OR c.COLUMN_NAME LIKE '%SUELDO%'
        OR c.COLUMN_NAME LIKE '%INTEGRADO%'
        OR c.COLUMN_NAME LIKE '%SBC%')
 ORDER BY c.OWNER, c.TABLE_NAME, c.COLUMN_ID;

PROMPT === 2) Tablas de datos laborales del empleado: todas sus columnas (por si el SDI tiene otro nombre) ===
SELECT c.TABLE_NAME, c.COLUMN_ID, c.COLUMN_NAME, c.DATA_TYPE
  FROM USER_TAB_COLUMNS c
 WHERE c.TABLE_NAME IN ('BIO_DATOS_LABORALES_EMPLEADOS', 'BIOMETRICO_EMPLEADO', 'BIOMETRICO_EMPLEADOS')
 ORDER BY c.TABLE_NAME, c.COLUMN_ID;

PROMPT === 3) Por cada columna NUMERICA del punto 1 en BIOMETRICO: cuantos con dato y rango ===
PROMPT     (minimo, mediana y maximo, sin NSS ni nombres: un SDI diario anda en cientos o pocos miles;
PROMPT      si la mediana anda en decenas de miles es mensual)
DECLARE
  v_total NUMBER; v_con NUMBER; v_min NUMBER; v_med NUMBER; v_max NUMBER;
BEGIN
  FOR c IN (SELECT TABLE_NAME, COLUMN_NAME
              FROM USER_TAB_COLUMNS
             WHERE DATA_TYPE IN ('NUMBER', 'FLOAT', 'BINARY_DOUBLE')
               AND (   REGEXP_LIKE(COLUMN_NAME, '(^|_)SDI(_|$)') OR COLUMN_NAME LIKE '%SALARIO%'
                    OR COLUMN_NAME LIKE '%SUELDO%' OR COLUMN_NAME LIKE '%INTEGRADO%' OR COLUMN_NAME LIKE '%SBC%')
             ORDER BY TABLE_NAME, COLUMN_NAME) LOOP
    BEGIN
      EXECUTE IMMEDIATE 'SELECT COUNT(*), COUNT(CASE WHEN "' || c.COLUMN_NAME || '" > 0 THEN 1 END),'
                     || ' MIN(CASE WHEN "' || c.COLUMN_NAME || '" > 0 THEN "' || c.COLUMN_NAME || '" END),'
                     || ' MEDIAN(CASE WHEN "' || c.COLUMN_NAME || '" > 0 THEN "' || c.COLUMN_NAME || '" END),'
                     || ' MAX("' || c.COLUMN_NAME || '") FROM "' || c.TABLE_NAME || '"'
        INTO v_total, v_con, v_min, v_med, v_max;
      DBMS_OUTPUT.PUT_LINE(RPAD(c.TABLE_NAME || '.' || c.COLUMN_NAME, 60) || ' filas=' || v_total
                           || '  con_dato=' || v_con || '  min=' || ROUND(v_min, 2)
                           || '  mediana=' || ROUND(v_med, 2) || '  max=' || ROUND(v_max, 2));
    EXCEPTION WHEN OTHERS THEN
      DBMS_OUTPUT.PUT_LINE(RPAD(c.TABLE_NAME || '.' || c.COLUMN_NAME, 60) || ' (no se pudo leer: ' || SQLERRM || ')');
    END;
  END LOOP;
END;
/

PROMPT === 4) Columnas de fecha en esas mismas tablas (¿hay historia del salario o solo el vigente?) ===
SELECT c.TABLE_NAME, c.COLUMN_NAME, c.DATA_TYPE
  FROM USER_TAB_COLUMNS c
 WHERE c.TABLE_NAME IN (SELECT TABLE_NAME FROM USER_TAB_COLUMNS
                         WHERE REGEXP_LIKE(COLUMN_NAME, '(^|_)SDI(_|$)') OR COLUMN_NAME LIKE '%SALARIO%'
                            OR COLUMN_NAME LIKE '%SUELDO%' OR COLUMN_NAME LIKE '%INTEGRADO%' OR COLUMN_NAME LIKE '%SBC%')
   AND (c.DATA_TYPE = 'DATE' OR c.DATA_TYPE LIKE 'TIMESTAMP%' OR c.COLUMN_NAME LIKE '%FECHA%' OR c.COLUMN_NAME LIKE '%VIGEN%')
 ORDER BY c.TABLE_NAME, c.COLUMN_NAME;

PROMPT === 5) Procedimientos o vistas que mencionan SDI (pista de donde lo cargan) ===
SELECT TYPE, NAME, LINE, SUBSTR(TRIM(TEXT), 1, 150) TEXTO
  FROM USER_SOURCE
 WHERE REGEXP_LIKE(UPPER(TEXT), '(^|[^A-Z])SDI([^A-Z]|$)')
   AND ROWNUM <= 60
 ORDER BY NAME, LINE;

SPOOL OFF
PROMPT Listo. Mandar C:\miguel\diagnostico-sdi-salida.txt
