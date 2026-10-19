-- =============================================================================
-- Paso 7 (foto inicial): como guardo la carga VIEJA cada dato, para prellenar los layouts.
-- SOLO LECTURA. Base: BIOMETRICO de produccion. Lo corre el usuario y me pasa la salida.
-- =============================================================================
SET LINESIZE 250
SET PAGESIZE 1000
COLUMN familia  FORMAT A18
COLUMN nombre   FORMAT A60
COLUMN ej_min   FORMAT A35
COLUMN ej_max   FORMAT A35

-- 1) Atributos por familia: nombre exacto, cuantos, cuantos valores distintos y dos ejemplos
SELECT e.FAMILIA familia, a.NOMBRE nombre, COUNT(*) renglones,
       COUNT(DISTINCT a.VALOR) distintos,
       COUNT(a.VALOR_NUM) con_numero, COUNT(a.VALOR_FECHA) con_fecha,
       SUBSTR(MIN(a.VALOR), 1, 35) ej_min, SUBSTR(MAX(a.VALOR), 1, 35) ej_max
  FROM SERV_MED_BITACORA_EVENTO e
  JOIN SERV_MED_BITACORA_ATRIBUTO a ON a.EVENTO_ID = e.ID
 WHERE e.FAMILIA IN ('ATENCION', 'EXAMEN_INGRESO', 'EXAMEN_PERIODICO', 'INCAPACIDAD', 'ACCIDENTE')
 GROUP BY e.FAMILIA, a.NOMBRE
 ORDER BY e.FAMILIA, COUNT(*) DESC;

-- 2) Columnas fijas del evento: que tan llenas estan, por familia
SELECT FAMILIA familia, COUNT(*) eventos, COUNT(FECHA) con_fecha, COUNT(PREDIO) con_predio,
       COUNT(AREA) con_area, COUNT(CUENTA) con_cuenta, COUNT(PUESTO) con_puesto,
       COUNT(NOMBRE) con_nombre, COUNT(GENERO) con_genero, COUNT(RANGO_EDAD) con_edad,
       MIN(FECHA) fecha_min, MAX(FECHA) fecha_max
  FROM SERV_MED_BITACORA_EVENTO
 WHERE FAMILIA IN ('ATENCION', 'EXAMEN_INGRESO', 'EXAMEN_PERIODICO', 'INCAPACIDAD', 'ACCIDENTE')
 GROUP BY FAMILIA
 ORDER BY FAMILIA;

-- 3) Cuantos NOMBRES distintos hay que resolver a NSS, por familia (lo que tendra que confirmar
--    el servicio medico)
SELECT FAMILIA familia, COUNT(DISTINCT NOMBRE_NORM) nombres_distintos
  FROM SERV_MED_BITACORA_EVENTO
 WHERE FAMILIA IN ('ATENCION', 'EXAMEN_INGRESO', 'EXAMEN_PERIODICO', 'INCAPACIDAD', 'ACCIDENTE')
 GROUP BY FAMILIA
 ORDER BY FAMILIA;
