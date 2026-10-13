-- =============================================================================
-- Que se cargo de los Excel de morbilidad, en 5 vistas digeribles. SOLO LECTURA.
-- Base del portal (BIOMETRICO@PDBPRD o QA), NO ORDS. Correr en SQL Developer / SQL*Plus.
-- Cada bloque es independiente; se pueden correr por separado.
-- =============================================================================
SET LINESIZE 200
SET PAGESIZE 100
COLUMN familia FORMAT A22
COLUMN predio  FORMAT A16
COLUMN causa   FORMAT A40
COLUMN mes     FORMAT A8
COLUMN barra   FORMAT A40

-- ---------------------------------------------------------------------------
-- 1) TOTAL POR TIPO DE EVENTO (que hay y cuanto), con barra proporcional
-- ---------------------------------------------------------------------------
SELECT FAMILIA familia,
       COUNT(*) eventos,
       COUNT(DISTINCT PREDIO) predios,
       MIN(FECHA) desde, MAX(FECHA) hasta,
       RPAD('#', ROUND(COUNT(*) * 40 / MAX(COUNT(*)) OVER ()), '#') barra
  FROM SERV_MED_BITACORA_EVENTO
 GROUP BY FAMILIA
 ORDER BY eventos DESC;

-- ---------------------------------------------------------------------------
-- 2) MES A MES, UNA COLUMNA POR FAMILIA (la vista "tendencia" de Analisis)
-- ---------------------------------------------------------------------------
SELECT TO_CHAR(ANIO) || '-' || LPAD(MES, 2, '0') mes,
       SUM(CASE WHEN FAMILIA = 'ATENCION'         THEN 1 ELSE 0 END) atenciones,
       SUM(CASE WHEN FAMILIA = 'EXAMEN_INGRESO'   THEN 1 ELSE 0 END) ex_ingreso,
       SUM(CASE WHEN FAMILIA = 'EXAMEN_PERIODICO' THEN 1 ELSE 0 END) ex_periodico,
       SUM(CASE WHEN FAMILIA = 'EXAMEN_POS_INCAP' THEN 1 ELSE 0 END) ex_posincap,
       SUM(CASE WHEN FAMILIA = 'INCAPACIDAD'      THEN 1 ELSE 0 END) incapacidades,
       SUM(CASE WHEN FAMILIA = 'ACCIDENTE'        THEN 1 ELSE 0 END) accidentes,
       SUM(CASE WHEN FAMILIA = 'ANTIDOPING'       THEN 1 ELSE 0 END) antidoping,
       SUM(CASE WHEN FAMILIA = 'MATERNIDAD'       THEN 1 ELSE 0 END) maternidad,
       COUNT(*) total
  FROM SERV_MED_BITACORA_EVENTO
 WHERE ANIO IS NOT NULL AND MES IS NOT NULL
 GROUP BY ANIO, MES
 ORDER BY ANIO, MES;

-- ---------------------------------------------------------------------------
-- 3) POR PREDIO: atenciones, examenes, incapacidades, accidentes (una fila por predio)
-- ---------------------------------------------------------------------------
SELECT PREDIO predio,
       SUM(CASE WHEN FAMILIA = 'ATENCION'               THEN 1 ELSE 0 END) atenciones,
       SUM(CASE WHEN FAMILIA LIKE 'EXAMEN%'             THEN 1 ELSE 0 END) examenes,
       SUM(CASE WHEN FAMILIA = 'INCAPACIDAD'            THEN 1 ELSE 0 END) incapacidades,
       SUM(CASE WHEN FAMILIA = 'ACCIDENTE'              THEN 1 ELSE 0 END) accidentes,
       SUM(CASE WHEN FAMILIA = 'ANTIDOPING'             THEN 1 ELSE 0 END) antidoping,
       SUM(CASE WHEN FAMILIA = 'MATERNIDAD'             THEN 1 ELSE 0 END) maternidad,
       COUNT(*) total
  FROM SERV_MED_BITACORA_EVENTO
 GROUP BY PREDIO
 ORDER BY total DESC;

-- ---------------------------------------------------------------------------
-- 4) TOP 15 CAUSAS DE CONSULTA (atenciones), con porcentaje y barra
-- ---------------------------------------------------------------------------
SELECT * FROM (
  SELECT a.VALOR causa,
         COUNT(*) atenciones,
         ROUND(COUNT(*) * 100 / SUM(COUNT(*)) OVER (), 1) pct,
         RPAD('#', ROUND(COUNT(*) * 40 / MAX(COUNT(*)) OVER ()), '#') barra
    FROM SERV_MED_BITACORA_EVENTO e
    JOIN SERV_MED_BITACORA_ATRIBUTO a ON a.EVENTO_ID = e.ID AND a.NOMBRE = 'CAUSA'
   WHERE e.FAMILIA = 'ATENCION'
   GROUP BY a.VALOR
   ORDER BY atenciones DESC
) WHERE ROWNUM <= 15;

-- ---------------------------------------------------------------------------
-- 5) EXAMENES POR DICTAMEN  e  INCAPACIDADES POR RAMO (dos resumenes chicos)
-- ---------------------------------------------------------------------------
SELECT 'EXAMEN' bloque, a.VALOR categoria, COUNT(*) n
  FROM SERV_MED_BITACORA_EVENTO e
  JOIN SERV_MED_BITACORA_ATRIBUTO a ON a.EVENTO_ID = e.ID AND a.NOMBRE = 'DICTAMEN'
 WHERE e.FAMILIA LIKE 'EXAMEN%'
 GROUP BY a.VALOR
UNION ALL
SELECT 'INCAPACIDAD', a.VALOR, COUNT(*)
  FROM SERV_MED_BITACORA_EVENTO e
  JOIN SERV_MED_BITACORA_ATRIBUTO a ON a.EVENTO_ID = e.ID AND a.NOMBRE = 'RAMO'
 WHERE e.FAMILIA = 'INCAPACIDAD'
 GROUP BY a.VALOR
 ORDER BY 1, 3 DESC;

-- ---------------------------------------------------------------------------
-- 6) QUE ARCHIVOS ENTRARON Y SI CUADRARON (resumen de la bitacora de carga)
-- ---------------------------------------------------------------------------
SELECT FAMILIA familia,
       COUNT(*) hojas,
       SUM(FILAS_INSERTADAS) insertadas,
       SUM(CASE WHEN CUADRA = 'S' THEN 1 ELSE 0 END) cuadran,
       SUM(CASE WHEN CUADRA = 'N' THEN 1 ELSE 0 END) no_cuadran,
       SUM(CASE WHEN CUADRA IS NULL OR CUADRA = 'Z' THEN 1 ELSE 0 END) sin_acumulado
  FROM SERV_MED_BITACORA_LOTE
 GROUP BY FAMILIA
 ORDER BY insertadas DESC;

-- =============================================================================
-- DETALLE POR PERSONA. En los Excel no viene NSS: la persona es el NOMBRE (columna
-- NOMBRE_NORM = sin acentos, mayusculas, espacios simples). Si un nombre se escribio de dos
-- formas en distintos archivos, sale como dos personas: comparar a ojo antes de concluir.
-- =============================================================================
COLUMN nombre      FORMAT A40
COLUMN detalle     FORMAT A70
COLUMN diagnostico FORMAT A40

-- ---------------------------------------------------------------------------
-- 7) RESUMEN POR PERSONA: cuantos eventos de cada tipo tiene cada quien (top 50 por volumen).
--    Para buscar a alguien: quitar el ROWNUM y poner  AND e.NOMBRE_NORM LIKE '%PEREZ%LOPEZ%'
-- ---------------------------------------------------------------------------
SELECT * FROM (
  SELECT e.NOMBRE_NORM nombre,
         MAX(e.PREDIO) predio,
         SUM(CASE WHEN e.FAMILIA = 'ATENCION'    THEN 1 ELSE 0 END) atenciones,
         SUM(CASE WHEN e.FAMILIA LIKE 'EXAMEN%'  THEN 1 ELSE 0 END) examenes,
         SUM(CASE WHEN e.FAMILIA = 'INCAPACIDAD' THEN 1 ELSE 0 END) incapacidades,
         SUM(CASE WHEN e.FAMILIA = 'ACCIDENTE'   THEN 1 ELSE 0 END) accidentes,
         SUM(CASE WHEN e.FAMILIA = 'ANTIDOPING'  THEN 1 ELSE 0 END) antidoping,
         COUNT(*) total,
         MIN(e.FECHA) primer_evento, MAX(e.FECHA) ultimo_evento
    FROM SERV_MED_BITACORA_EVENTO e
   WHERE e.NOMBRE_NORM IS NOT NULL
   GROUP BY e.NOMBRE_NORM
   ORDER BY total DESC
) WHERE ROWNUM <= 50;

-- ---------------------------------------------------------------------------
-- 8) EXPEDIENTE DE UNA PERSONA: todos sus eventos en orden, con el detalle en una columna.
--    Cambiar el nombre del WHERE (parte del nombre basta; sin acentos, en mayusculas).
-- ---------------------------------------------------------------------------
SELECT e.FECHA fecha,
       e.FAMILIA familia,
       e.PREDIO predio,
       e.PUESTO puesto,
       (SELECT LISTAGG(a.NOMBRE || '=' || a.VALOR, ' | ') WITHIN GROUP (ORDER BY a.NOMBRE)
          FROM SERV_MED_BITACORA_ATRIBUTO a
         WHERE a.EVENTO_ID = e.ID
           AND a.NOMBRE IN ('CAUSA','DIAGNOSTICO','TRATAMIENTO','RAMO','TIPO','DIAS','COSTO','DICTAMEN','RESULTADO','DESENLACE','ESTADO')) detalle,
       l.ARCHIVO || ' / ' || l.HOJA origen
  FROM SERV_MED_BITACORA_EVENTO e
  JOIN SERV_MED_BITACORA_LOTE l ON l.ID = e.LOTE_ID
 WHERE e.NOMBRE_NORM LIKE '%PEREZ%LOPEZ%'
 ORDER BY e.FECHA NULLS LAST, e.FAMILIA;

-- ---------------------------------------------------------------------------
-- 9) PERSONAS CON MAS INCAPACIDAD (dias acumulados) - util para seguimiento
-- ---------------------------------------------------------------------------
SELECT * FROM (
  SELECT e.NOMBRE_NORM nombre, MAX(e.PREDIO) predio,
         COUNT(*) incapacidades,
         SUM(a.VALOR_NUM) dias_acumulados
    FROM SERV_MED_BITACORA_EVENTO e
    JOIN SERV_MED_BITACORA_ATRIBUTO a ON a.EVENTO_ID = e.ID AND a.NOMBRE = 'DIAS'
   WHERE e.FAMILIA = 'INCAPACIDAD'
   GROUP BY e.NOMBRE_NORM
   ORDER BY dias_acumulados DESC NULLS LAST
) WHERE ROWNUM <= 30;

-- ---------------------------------------------------------------------------
-- 10) DETALLE DE TODOS: un renglon por evento de cada persona, agrupado por nombre.
--     Son ~24 mil renglones: en SQL Developer conviene exportar a Excel (clic derecho en la
--     rejilla -> Exportar) o filtrar por predio / familia / fecha con los AND comentados.
-- ---------------------------------------------------------------------------
SELECT e.NOMBRE_NORM nombre,
       e.FECHA fecha,
       e.FAMILIA familia,
       e.PREDIO predio,
       e.CUENTA cuenta,
       e.PUESTO puesto,
       e.GENERO genero,
       e.RANGO_EDAD rango_edad,
       (SELECT LISTAGG(a.NOMBRE || '=' || a.VALOR, ' | ') WITHIN GROUP (ORDER BY a.NOMBRE)
          FROM SERV_MED_BITACORA_ATRIBUTO a
         WHERE a.EVENTO_ID = e.ID
           AND a.NOMBRE IN ('CAUSA','DIAGNOSTICO','TRATAMIENTO','RAMO','TIPO','DIAS','COSTO','DICTAMEN','RESULTADO','DESENLACE','ESTADO')) detalle,
       l.ARCHIVO || ' / ' || l.HOJA origen
  FROM SERV_MED_BITACORA_EVENTO e
  JOIN SERV_MED_BITACORA_LOTE l ON l.ID = e.LOTE_ID
 WHERE e.NOMBRE_NORM IS NOT NULL
   -- AND e.PREDIO = 'MACRO II'
   -- AND e.FAMILIA = 'ATENCION'
   -- AND e.FECHA BETWEEN DATE '2026-01-01' AND DATE '2026-03-31'
 ORDER BY e.NOMBRE_NORM, e.FECHA NULLS LAST, e.FAMILIA;
