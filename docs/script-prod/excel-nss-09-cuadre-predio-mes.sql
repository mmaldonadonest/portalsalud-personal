-- =============================================================================
-- CUADRE POR PREDIO Y MES: lo que el portal muestra (vistas SERV_MED_V_*) por layout, predio y mes,
-- para compararlo contra el resumen del servicio medico (paso 8 del plan de carga de Excel).
--
-- BASE: BIOMETRICO de produccion (PDBPRD). Lo corre el usuario.
--
-- SOLO LECTURA: puros SELECT. No crea, no modifica, no borra nada. Se puede correr las veces que sea.
--
-- QUE SALE (13 resultados):
--    0  Lotes cargados (que archivos hay en las tablas nuevas y en que estado)
--    1  Resumen por layout, anio y fuente
--    2  Atenciones          por predio y mes
--    3  Incapacidades       por predio y mes (certificados, dias, costo)
--    4  Examenes            por predio y mes (por tipo y dictamen)
--    5  Accidentes          por predio y mes (laboral/trayecto, costo)
--    6  Antidoping          por predio y mes (pruebas, alcoholemias, positivos)
--    7  Maternidad          por predio y mes (seguimientos y embarazos)
--    8  Medicamentos        por predio y mes (consumo de las 5 semanas)
--    9  Material fijo       por predio y mes (recepciones)
--   10a Conciliacion carga vs pantalla, total por layout
--   10b Conciliacion, solo los predio-mes donde no son iguales
--   11  Renglones sin predio ("Sin asignar"), por layout y fuente
--
-- COMO LEERLO
--   - Cada resultado trae un renglon por PREDIO y MES, un renglon "TOTAL" por predio y un
--     "== TODOS ==" al final: con eso se compara contra el resumen por predio del servicio medico.
--   - FUENTE: EXCEL = layouts nuevos por NSS; PORTAL = capturado en el portal; CARGA_VIEJA = la carga
--     anterior por nombre (solo se lee mientras el layout nuevo de esa familia este vacio).
--   - "Manda el portal": si un evento esta en el Excel y en el portal, cuenta UNA vez (el del
--     portal). El 10a/10b dice cuantos renglones del Excel quedaron ocultos por eso.
--   - No hay fecha de corte (decision 12-oct-2026): Excel y portal conviven y se suman.
--   - Personas = NSS distintos. La carga vieja no trae NSS: no suma personas.
--
-- PERIODO: 2025-01-01 a 2026-12-31 (alcance de la auditoria). Para otro periodo, reemplazar en
--   todo el archivo DATE '2025-01-01' y DATE '2027-01-01' (el limite superior NO se incluye).
--
-- ORDEN: correr TODO con F5 (Ejecutar script) en SQL Developer. Para comparar comodo, cada
--   resultado se puede correr solo (Ctrl+Enter) y exportar a Excel desde la cuadricula.
-- =============================================================================
-- La salida completa queda en un archivo (la ventana de salida corta los resultados largos).
-- Fuera del repositorio a proposito. Se sobreescribe en cada corrida.
SPOOL C:\miguel\cuadre-excel-nss-salida.txt
SET FEEDBACK ON
SET LINESIZE 250
SET PAGESIZE 1000
COLUMN predio  FORMAT A28
COLUMN mes     FORMAT A11
COLUMN layout  FORMAT A20
COLUMN fuente  FORMAT A12
COLUMN archivo FORMAT A70
COLUMN estado  FORMAT A12

-- ----------------------------------------------------------------------
-- 0) Lotes cargados: un renglon por archivo subido con el cargador
-- ----------------------------------------------------------------------
SELECT ID lote, LAYOUT, PREDIO, TO_CHAR(PERIODO_DESDE, 'YYYY-MM-DD') desde, TO_CHAR(PERIODO_HASTA, 'YYYY-MM-DD') hasta,
       FILAS_LEIDAS leidas, FILAS_VALIDAS validas, FILAS_DUPLICADAS duplicadas, FILAS_ERROR errores, ESTADO,
       TO_CHAR(CREATED_AT, 'YYYY-MM-DD HH24:MI') cargado, SUBSTR(ARCHIVO, 1, 70) archivo
  FROM SERV_MED_XLS_LOTE
 ORDER BY ID;

-- ----------------------------------------------------------------------
-- 1) Resumen por layout, anio y fuente
-- ----------------------------------------------------------------------
SELECT layout, anio, fuente, renglones, personas FROM (
  SELECT '1 ATENCIONES' layout, EXTRACT(YEAR FROM FECHA) anio, FUENTE, COUNT(*) renglones, COUNT(DISTINCT NSS) personas
    FROM SERV_MED_V_ATENCION   WHERE FECHA >= DATE '2025-01-01' AND FECHA < DATE '2027-01-01' GROUP BY EXTRACT(YEAR FROM FECHA), FUENTE
  UNION ALL
  SELECT '2-4 EXAMENES', EXTRACT(YEAR FROM FECHA), FUENTE, COUNT(*), COUNT(DISTINCT NSS)
    FROM SERV_MED_V_EXAMEN     WHERE FECHA >= DATE '2025-01-01' AND FECHA < DATE '2027-01-01' GROUP BY EXTRACT(YEAR FROM FECHA), FUENTE
  UNION ALL
  SELECT '5 INCAPACIDADES', EXTRACT(YEAR FROM FECHA), FUENTE, COUNT(*), COUNT(DISTINCT NSS)
    FROM SERV_MED_V_INCAPACIDAD WHERE FECHA >= DATE '2025-01-01' AND FECHA < DATE '2027-01-01' GROUP BY EXTRACT(YEAR FROM FECHA), FUENTE
  UNION ALL
  SELECT '6 ACCIDENTES', EXTRACT(YEAR FROM FECHA), FUENTE, COUNT(*), COUNT(DISTINCT NSS)
    FROM SERV_MED_V_ACCIDENTE  WHERE FECHA >= DATE '2025-01-01' AND FECHA < DATE '2027-01-01' GROUP BY EXTRACT(YEAR FROM FECHA), FUENTE
  UNION ALL
  SELECT '7 ANTIDOPING', EXTRACT(YEAR FROM FECHA), FUENTE, COUNT(*), COUNT(DISTINCT NSS)
    FROM SERV_MED_V_ANTIDOPING WHERE FECHA >= DATE '2025-01-01' AND FECHA < DATE '2027-01-01' GROUP BY EXTRACT(YEAR FROM FECHA), FUENTE
  UNION ALL
  SELECT '8 MATERNIDAD', EXTRACT(YEAR FROM FECHA), FUENTE, COUNT(*), COUNT(DISTINCT NSS)
    FROM SERV_MED_V_MATERNIDAD WHERE FECHA >= DATE '2025-01-01' AND FECHA < DATE '2027-01-01' GROUP BY EXTRACT(YEAR FROM FECHA), FUENTE
  UNION ALL
  SELECT '9 MEDICAMENTOS', EXTRACT(YEAR FROM FECHA), 'EXCEL', COUNT(*), NULL
    FROM SERV_MED_XLS_MEDICAMENTO WHERE ESTADO_REGISTRO = 'VALIDO' AND FECHA >= DATE '2025-01-01' AND FECHA < DATE '2027-01-01' GROUP BY EXTRACT(YEAR FROM FECHA)
  UNION ALL
  SELECT '9 MATERIAL FIJO', EXTRACT(YEAR FROM FECHA), 'EXCEL', COUNT(*), NULL
    FROM SERV_MED_XLS_MATERIAL_FIJO WHERE ESTADO_REGISTRO = 'VALIDO' AND FECHA >= DATE '2025-01-01' AND FECHA < DATE '2027-01-01' GROUP BY EXTRACT(YEAR FROM FECHA)
)
ORDER BY layout, anio, fuente;

-- ----------------------------------------------------------------------
-- 2) ATENCIONES por predio y mes
--    Los ramos son los del layout; el portal usa otro catalogo (P10, pendiente), por eso sus
--    atenciones no caen en esas columnas: van en "portal".
-- ----------------------------------------------------------------------
SELECT CASE WHEN GROUPING(predio_c) = 1 THEN '== TODOS ==' ELSE predio_c END predio,
       CASE WHEN GROUPING(mes_c) = 1 THEN 'TOTAL' ELSE mes_c END mes,
       COUNT(*) atenciones,
       SUM(CASE WHEN FUENTE = 'EXCEL'  THEN 1 ELSE 0 END) excel,
       SUM(CASE WHEN FUENTE = 'PORTAL' THEN 1 ELSE 0 END) portal,
       COUNT(DISTINCT NSS) personas,
       SUM(CASE WHEN RAMO = 'ENF GRAL'          THEN 1 ELSE 0 END) enf_gral,
       SUM(CASE WHEN RAMO = 'SEGUIMIENTO SALUD' THEN 1 ELSE 0 END) seguimiento,
       SUM(CASE WHEN RAMO = 'PASE SALIDA'       THEN 1 ELSE 0 END) pase_salida,
       SUM(CASE WHEN RAMO = 'RT'                THEN 1 ELSE 0 END) rt,
       SUM(CASE WHEN LESION_ME IS NOT NULL      THEN 1 ELSE 0 END) musculoesq
  FROM (SELECT v.*, NVL(v.PREDIO, 'Sin asignar') predio_c, TO_CHAR(v.FECHA, 'YYYY-MM') mes_c
          FROM SERV_MED_V_ATENCION v
         WHERE v.FECHA >= DATE '2025-01-01' AND v.FECHA < DATE '2027-01-01')
 GROUP BY GROUPING SETS ((predio_c, mes_c), (predio_c), ())
 ORDER BY GROUPING(predio_c), predio_c, GROUPING(mes_c), mes_c;

-- ----------------------------------------------------------------------
-- 3) INCAPACIDADES por predio y mes (FECHA = inicio del certificado)
--    Costo: del Excel, COSTO TOTAL capturado; del portal, el costo capturado.
-- ----------------------------------------------------------------------
SELECT CASE WHEN GROUPING(predio_c) = 1 THEN '== TODOS ==' ELSE predio_c END predio,
       CASE WHEN GROUPING(mes_c) = 1 THEN 'TOTAL' ELSE mes_c END mes,
       COUNT(*) certificados,
       SUM(CASE WHEN FUENTE = 'EXCEL'  THEN 1 ELSE 0 END) excel,
       SUM(CASE WHEN FUENTE = 'PORTAL' THEN 1 ELSE 0 END) portal,
       COUNT(DISTINCT NSS) personas,
       SUM(CASE WHEN RUBRO = 'IMSS'    THEN 1 ELSE 0 END) imss,
       SUM(CASE WHEN RUBRO = 'INTERNA' THEN 1 ELSE 0 END) internas,
       NVL(SUM(DIAS), 0) dias,
       ROUND(NVL(SUM(COSTO), 0), 2) costo
  FROM (SELECT v.*, NVL(v.PREDIO, 'Sin asignar') predio_c, TO_CHAR(v.FECHA, 'YYYY-MM') mes_c
          FROM SERV_MED_V_INCAPACIDAD v
         WHERE v.FECHA >= DATE '2025-01-01' AND v.FECHA < DATE '2027-01-01')
 GROUP BY GROUPING SETS ((predio_c, mes_c), (predio_c), ())
 ORDER BY GROUPING(predio_c), predio_c, GROUPING(mes_c), mes_c;

-- ----------------------------------------------------------------------
-- 4) EXAMENES por predio y mes (nuevo ingreso, periodico, pos incapacidad y dictamen)
--    El historial del portal no guarda el tipo: sale en "sin_tipo".
-- ----------------------------------------------------------------------
SELECT CASE WHEN GROUPING(predio_c) = 1 THEN '== TODOS ==' ELSE predio_c END predio,
       CASE WHEN GROUPING(mes_c) = 1 THEN 'TOTAL' ELSE mes_c END mes,
       COUNT(*) examenes,
       SUM(CASE WHEN FUENTE = 'EXCEL'       THEN 1 ELSE 0 END) excel,
       SUM(CASE WHEN FUENTE = 'PORTAL'      THEN 1 ELSE 0 END) portal,
       SUM(CASE WHEN FUENTE = 'CARGA_VIEJA' THEN 1 ELSE 0 END) carga_vieja,
       SUM(CASE WHEN TIPO = 'NUEVO_INGRESO'   THEN 1 ELSE 0 END) nuevo_ingreso,
       SUM(CASE WHEN TIPO = 'PERIODICO'       THEN 1 ELSE 0 END) periodico,
       SUM(CASE WHEN TIPO = 'POS_INCAPACIDAD' THEN 1 ELSE 0 END) pos_incap,
       SUM(CASE WHEN TIPO IS NULL             THEN 1 ELSE 0 END) sin_tipo,
       SUM(CASE WHEN DICTAMEN = 'APTO'              THEN 1 ELSE 0 END) apto,
       SUM(CASE WHEN DICTAMEN = 'APTO_CONDICIONADO' THEN 1 ELSE 0 END) condicionado,
       SUM(CASE WHEN DICTAMEN = 'APTO_RESTRINGIDO'  THEN 1 ELSE 0 END) restringido,
       SUM(CASE WHEN DICTAMEN = 'NO_APTO'           THEN 1 ELSE 0 END) no_apto,
       SUM(CASE WHEN DICTAMEN IS NULL               THEN 1 ELSE 0 END) sin_dictamen
  FROM (SELECT v.*, NVL(v.PREDIO, 'Sin asignar') predio_c, TO_CHAR(v.FECHA, 'YYYY-MM') mes_c
          FROM SERV_MED_V_EXAMEN v
         WHERE v.FECHA >= DATE '2025-01-01' AND v.FECHA < DATE '2027-01-01')
 GROUP BY GROUPING SETS ((predio_c, mes_c), (predio_c), ())
 ORDER BY GROUPING(predio_c), predio_c, GROUPING(mes_c), mes_c;

-- ----------------------------------------------------------------------
-- 5) ACCIDENTES por predio y mes
--    El COSTO puede ser negativo (decision 12-oct, bloque IMPROCEDENTE): se suma tal cual y se
--    cuenta aparte para que se vea.
-- ----------------------------------------------------------------------
SELECT CASE WHEN GROUPING(predio_c) = 1 THEN '== TODOS ==' ELSE predio_c END predio,
       CASE WHEN GROUPING(mes_c) = 1 THEN 'TOTAL' ELSE mes_c END mes,
       COUNT(*) accidentes,
       SUM(CASE WHEN FUENTE = 'EXCEL'  THEN 1 ELSE 0 END) excel,
       SUM(CASE WHEN FUENTE = 'PORTAL' THEN 1 ELSE 0 END) portal,
       COUNT(DISTINCT NSS) personas,
       SUM(CASE WHEN UPPER(TIPO_RIESGO) LIKE 'LABORAL%'  THEN 1 ELSE 0 END) laborales,
       SUM(CASE WHEN UPPER(TIPO_RIESGO) LIKE 'TRAYECTO%' THEN 1 ELSE 0 END) trayecto,
       ROUND(NVL(SUM(COSTO), 0), 2) costo,
       SUM(CASE WHEN COSTO < 0 THEN 1 ELSE 0 END) costos_negativos
  FROM (SELECT v.*, NVL(v.PREDIO, 'Sin asignar') predio_c, TO_CHAR(v.FECHA, 'YYYY-MM') mes_c
          FROM SERV_MED_V_ACCIDENTE v
         WHERE v.FECHA >= DATE '2025-01-01' AND v.FECHA < DATE '2027-01-01')
 GROUP BY GROUPING SETS ((predio_c, mes_c), (predio_c), ())
 ORDER BY GROUPING(predio_c), predio_c, GROUPING(mes_c), mes_c;

-- ----------------------------------------------------------------------
-- 6) ANTIDOPING por predio y mes
-- ----------------------------------------------------------------------
SELECT CASE WHEN GROUPING(predio_c) = 1 THEN '== TODOS ==' ELSE predio_c END predio,
       CASE WHEN GROUPING(mes_c) = 1 THEN 'TOTAL' ELSE mes_c END mes,
       COUNT(*) pruebas,
       SUM(CASE WHEN FUENTE = 'EXCEL'       THEN 1 ELSE 0 END) excel,
       SUM(CASE WHEN FUENTE = 'PORTAL'      THEN 1 ELSE 0 END) portal,
       SUM(CASE WHEN FUENTE = 'CARGA_VIEJA' THEN 1 ELSE 0 END) carga_vieja,
       SUM(CASE WHEN UPPER(TIPO_PRUEBA) = 'ANTIDOPING'  THEN 1 ELSE 0 END) antidoping,
       SUM(CASE WHEN UPPER(TIPO_PRUEBA) = 'ALCOHOLEMIA' THEN 1 ELSE 0 END) alcoholemia,
       SUM(CASE WHEN UPPER(RESULTADO) = 'POSITIVO'      THEN 1 ELSE 0 END) positivos
  FROM (SELECT v.*, NVL(v.PREDIO, 'Sin asignar') predio_c, TO_CHAR(v.FECHA, 'YYYY-MM') mes_c
          FROM SERV_MED_V_ANTIDOPING v
         WHERE v.FECHA >= DATE '2025-01-01' AND v.FECHA < DATE '2027-01-01')
 GROUP BY GROUPING SETS ((predio_c, mes_c), (predio_c), ())
 ORDER BY GROUPING(predio_c), predio_c, GROUPING(mes_c), mes_c;

-- ----------------------------------------------------------------------
-- 7) MATERNIDAD por predio y mes (FECHA = fecha de corte / mes reportado)
--    "seguimientos" = renglones (una embarazada por mes); "embarazos" = CLAVE_EMBARAZO distintas.
--    En el TOTAL, embarazos NO es la suma de los meses: la misma embarazada se repite cada mes.
-- ----------------------------------------------------------------------
SELECT CASE WHEN GROUPING(predio_c) = 1 THEN '== TODOS ==' ELSE predio_c END predio,
       CASE WHEN GROUPING(mes_c) = 1 THEN 'TOTAL' ELSE mes_c END mes,
       COUNT(*) seguimientos,
       COUNT(DISTINCT CLAVE_EMBARAZO) embarazos,
       SUM(CASE WHEN FUENTE = 'EXCEL'       THEN 1 ELSE 0 END) excel,
       SUM(CASE WHEN FUENTE = 'PORTAL'      THEN 1 ELSE 0 END) portal,
       SUM(CASE WHEN FUENTE = 'CARGA_VIEJA' THEN 1 ELSE 0 END) carga_vieja
  FROM (SELECT v.*, NVL(v.PREDIO, 'Sin asignar') predio_c, TO_CHAR(v.FECHA, 'YYYY-MM') mes_c
          FROM SERV_MED_V_MATERNIDAD v
         WHERE v.FECHA >= DATE '2025-01-01' AND v.FECHA < DATE '2027-01-01')
 GROUP BY GROUPING SETS ((predio_c, mes_c), (predio_c), ())
 ORDER BY GROUPING(predio_c), predio_c, GROUPING(mes_c), mes_c;

-- ----------------------------------------------------------------------
-- 8) MEDICAMENTOS por predio y mes (layout 9, sin vista: no hay portal con que unirlo)
--    consumo = semanas 1 a 5. Mientras la tabla este vacia, las graficas siguen con la carga vieja
--    y este resultado sale sin renglones.
-- ----------------------------------------------------------------------
SELECT CASE WHEN GROUPING(predio_c) = 1 THEN '== TODOS ==' ELSE predio_c END predio,
       CASE WHEN GROUPING(mes_c) = 1 THEN 'TOTAL' ELSE mes_c END mes,
       COUNT(*) renglones_lote,
       COUNT(DISTINCT DESCRIPCION) medicamentos,
       NVL(SUM(RECIBIDO_MES), 0) recibido,
       NVL(SUM(NVL(CONSUMO_S1, 0) + NVL(CONSUMO_S2, 0) + NVL(CONSUMO_S3, 0) + NVL(CONSUMO_S4, 0) + NVL(CONSUMO_S5, 0)), 0) consumo,
       NVL(SUM(EXISTENCIA_PROX_MES), 0) existencia_prox_mes
  FROM (SELECT m.*, m.PREDIO predio_c, TO_CHAR(m.FECHA, 'YYYY-MM') mes_c
          FROM SERV_MED_XLS_MEDICAMENTO m
         WHERE m.ESTADO_REGISTRO = 'VALIDO' AND m.FECHA >= DATE '2025-01-01' AND m.FECHA < DATE '2027-01-01')
 GROUP BY GROUPING SETS ((predio_c, mes_c), (predio_c), ())
 ORDER BY GROUPING(predio_c), predio_c, GROUPING(mes_c), mes_c;

-- ----------------------------------------------------------------------
-- 9) MATERIAL FIJO por predio y mes de recepcion (layout 9)
-- ----------------------------------------------------------------------
SELECT CASE WHEN GROUPING(predio_c) = 1 THEN '== TODOS ==' ELSE predio_c END predio,
       CASE WHEN GROUPING(mes_c) = 1 THEN 'TOTAL' ELSE mes_c END mes,
       COUNT(*) recepciones,
       COUNT(DISTINCT EQUIPO) equipos,
       NVL(SUM(CANTIDAD_SURTIDA), 0) cantidad_surtida,
       SUM(CASE WHEN FECHA_BAJA IS NOT NULL THEN 1 ELSE 0 END) con_baja
  FROM (SELECT f.*, f.PREDIO predio_c, TO_CHAR(f.FECHA, 'YYYY-MM') mes_c
          FROM SERV_MED_XLS_MATERIAL_FIJO f
         WHERE f.ESTADO_REGISTRO = 'VALIDO' AND f.FECHA >= DATE '2025-01-01' AND f.FECHA < DATE '2027-01-01')
 GROUP BY GROUPING SETS ((predio_c, mes_c), (predio_c), ())
 ORDER BY GROUPING(predio_c), predio_c, GROUPING(mes_c), mes_c;

-- ----------------------------------------------------------------------
-- 10) CONCILIACION: lo cargado del Excel contra lo que muestra la pantalla
--     validos     = renglones del Excel que entraron (ESTADO_REGISTRO = VALIDO)
--     duplicados  = marcados al cargar porque ya estaban en el portal (no se muestran)
--     en_pantalla = renglones del Excel que salen en la vista
--     ocultos     = validos - en_pantalla: alguien los capturo en el portal DESPUES de la carga,
--                   y "manda el portal" los quito al leer. Si sale negativo, algo esta mal: avisar.
--     Consumibles no lleva columna: no se compara contra el portal.
-- ----------------------------------------------------------------------
-- 10a) Total por layout
WITH cargado AS (
  SELECT 'ATENCIONES' layout, PREDIO, TO_CHAR(FECHA, 'YYYY-MM') mes, ESTADO_REGISTRO est FROM SERV_MED_XLS_ATENCION
  UNION ALL SELECT 'EXAMEN_INGRESO',   PREDIO, TO_CHAR(FECHA, 'YYYY-MM'), ESTADO_REGISTRO FROM SERV_MED_XLS_EXAMEN_ING
  UNION ALL SELECT 'EXAMEN_PERIODICO', PREDIO, TO_CHAR(FECHA, 'YYYY-MM'), ESTADO_REGISTRO FROM SERV_MED_XLS_EXAMEN_PER
  UNION ALL SELECT 'EXAMEN_POS_INCAP', PREDIO, TO_CHAR(FECHA, 'YYYY-MM'), ESTADO_REGISTRO FROM SERV_MED_XLS_EXAMEN_POS
  UNION ALL SELECT 'INCAPACIDADES',    PREDIO, TO_CHAR(FECHA, 'YYYY-MM'), ESTADO_REGISTRO FROM SERV_MED_XLS_INCAPACIDAD
  UNION ALL SELECT 'ACCIDENTES',       PREDIO, TO_CHAR(FECHA, 'YYYY-MM'), ESTADO_REGISTRO FROM SERV_MED_XLS_ACCIDENTE
  UNION ALL SELECT 'ANTIDOPING',       PREDIO, TO_CHAR(FECHA, 'YYYY-MM'), ESTADO_REGISTRO FROM SERV_MED_XLS_ANTIDOPING
  UNION ALL SELECT 'MATERNIDAD',       PREDIO, TO_CHAR(FECHA, 'YYYY-MM'), ESTADO_REGISTRO FROM SERV_MED_XLS_MATERNIDAD
), pantalla AS (
  SELECT 'ATENCIONES' layout, PREDIO, TO_CHAR(FECHA, 'YYYY-MM') mes FROM SERV_MED_V_ATENCION WHERE FUENTE = 'EXCEL'
  UNION ALL SELECT CASE TIPO WHEN 'NUEVO_INGRESO' THEN 'EXAMEN_INGRESO' WHEN 'PERIODICO' THEN 'EXAMEN_PERIODICO'
                             ELSE 'EXAMEN_POS_INCAP' END, PREDIO, TO_CHAR(FECHA, 'YYYY-MM')
              FROM SERV_MED_V_EXAMEN WHERE FUENTE = 'EXCEL'
  UNION ALL SELECT 'INCAPACIDADES', PREDIO, TO_CHAR(FECHA, 'YYYY-MM') FROM SERV_MED_V_INCAPACIDAD WHERE FUENTE = 'EXCEL'
  UNION ALL SELECT 'ACCIDENTES',    PREDIO, TO_CHAR(FECHA, 'YYYY-MM') FROM SERV_MED_V_ACCIDENTE   WHERE FUENTE = 'EXCEL'
  UNION ALL SELECT 'ANTIDOPING',    PREDIO, TO_CHAR(FECHA, 'YYYY-MM') FROM SERV_MED_V_ANTIDOPING  WHERE FUENTE = 'EXCEL'
  UNION ALL SELECT 'MATERNIDAD',    PREDIO, TO_CHAR(FECHA, 'YYYY-MM') FROM SERV_MED_V_MATERNIDAD  WHERE FUENTE = 'EXCEL'
), c AS (
  SELECT layout, SUM(CASE WHEN est = 'VALIDO' THEN 1 ELSE 0 END) validos,
         SUM(CASE WHEN est = 'DUPLICADO' THEN 1 ELSE 0 END) duplicados
    FROM cargado WHERE mes >= '2025-01' AND mes < '2027-01' GROUP BY layout
), p AS (
  SELECT layout, COUNT(*) en_pantalla FROM pantalla WHERE mes >= '2025-01' AND mes < '2027-01' GROUP BY layout
)
SELECT c.layout, c.validos, c.duplicados, NVL(p.en_pantalla, 0) en_pantalla,
       c.validos - NVL(p.en_pantalla, 0) ocultos
  FROM c LEFT JOIN p ON p.layout = c.layout
 ORDER BY c.layout;

-- 10b) Detalle: solo los predio-mes donde lo cargado y lo que se ve no son iguales
WITH cargado AS (
  SELECT 'ATENCIONES' layout, PREDIO, TO_CHAR(FECHA, 'YYYY-MM') mes FROM SERV_MED_XLS_ATENCION WHERE ESTADO_REGISTRO = 'VALIDO'
  UNION ALL SELECT 'EXAMEN_INGRESO',   PREDIO, TO_CHAR(FECHA, 'YYYY-MM') FROM SERV_MED_XLS_EXAMEN_ING  WHERE ESTADO_REGISTRO = 'VALIDO'
  UNION ALL SELECT 'EXAMEN_PERIODICO', PREDIO, TO_CHAR(FECHA, 'YYYY-MM') FROM SERV_MED_XLS_EXAMEN_PER  WHERE ESTADO_REGISTRO = 'VALIDO'
  UNION ALL SELECT 'EXAMEN_POS_INCAP', PREDIO, TO_CHAR(FECHA, 'YYYY-MM') FROM SERV_MED_XLS_EXAMEN_POS  WHERE ESTADO_REGISTRO = 'VALIDO'
  UNION ALL SELECT 'INCAPACIDADES',    PREDIO, TO_CHAR(FECHA, 'YYYY-MM') FROM SERV_MED_XLS_INCAPACIDAD WHERE ESTADO_REGISTRO = 'VALIDO'
  UNION ALL SELECT 'ACCIDENTES',       PREDIO, TO_CHAR(FECHA, 'YYYY-MM') FROM SERV_MED_XLS_ACCIDENTE   WHERE ESTADO_REGISTRO = 'VALIDO'
  UNION ALL SELECT 'ANTIDOPING',       PREDIO, TO_CHAR(FECHA, 'YYYY-MM') FROM SERV_MED_XLS_ANTIDOPING  WHERE ESTADO_REGISTRO = 'VALIDO'
  UNION ALL SELECT 'MATERNIDAD',       PREDIO, TO_CHAR(FECHA, 'YYYY-MM') FROM SERV_MED_XLS_MATERNIDAD  WHERE ESTADO_REGISTRO = 'VALIDO'
), pantalla AS (
  SELECT 'ATENCIONES' layout, PREDIO, TO_CHAR(FECHA, 'YYYY-MM') mes FROM SERV_MED_V_ATENCION WHERE FUENTE = 'EXCEL'
  UNION ALL SELECT CASE TIPO WHEN 'NUEVO_INGRESO' THEN 'EXAMEN_INGRESO' WHEN 'PERIODICO' THEN 'EXAMEN_PERIODICO'
                             ELSE 'EXAMEN_POS_INCAP' END, PREDIO, TO_CHAR(FECHA, 'YYYY-MM')
              FROM SERV_MED_V_EXAMEN WHERE FUENTE = 'EXCEL'
  UNION ALL SELECT 'INCAPACIDADES', PREDIO, TO_CHAR(FECHA, 'YYYY-MM') FROM SERV_MED_V_INCAPACIDAD WHERE FUENTE = 'EXCEL'
  UNION ALL SELECT 'ACCIDENTES',    PREDIO, TO_CHAR(FECHA, 'YYYY-MM') FROM SERV_MED_V_ACCIDENTE   WHERE FUENTE = 'EXCEL'
  UNION ALL SELECT 'ANTIDOPING',    PREDIO, TO_CHAR(FECHA, 'YYYY-MM') FROM SERV_MED_V_ANTIDOPING  WHERE FUENTE = 'EXCEL'
  UNION ALL SELECT 'MATERNIDAD',    PREDIO, TO_CHAR(FECHA, 'YYYY-MM') FROM SERV_MED_V_MATERNIDAD  WHERE FUENTE = 'EXCEL'
), c AS (SELECT layout, PREDIO, mes, COUNT(*) validos FROM cargado GROUP BY layout, PREDIO, mes),
   p AS (SELECT layout, PREDIO, mes, COUNT(*) en_pantalla FROM pantalla GROUP BY layout, PREDIO, mes)
SELECT COALESCE(c.layout, p.layout) layout, COALESCE(c.PREDIO, p.PREDIO) predio, COALESCE(c.mes, p.mes) mes,
       NVL(c.validos, 0) validos, NVL(p.en_pantalla, 0) en_pantalla, NVL(c.validos, 0) - NVL(p.en_pantalla, 0) ocultos
  FROM c FULL OUTER JOIN p ON p.layout = c.layout AND p.PREDIO = c.PREDIO AND p.mes = c.mes
 WHERE NVL(c.validos, 0) <> NVL(p.en_pantalla, 0)
   AND COALESCE(c.mes, p.mes) >= '2025-01' AND COALESCE(c.mes, p.mes) < '2027-01'
 ORDER BY 1, 2, 3;

-- ----------------------------------------------------------------------
-- 11) Renglones sin predio ("Sin asignar" en las graficas), por layout y fuente.
--     Del portal: cuentas sin mapeo en SERV_MED_CUENTA_PREDIO o NSS de prueba. Del Excel no deberia
--     haber (PREDIO es obligatorio en el layout).
-- ----------------------------------------------------------------------
SELECT layout, fuente, COUNT(*) sin_predio FROM (
  SELECT '1 ATENCIONES' layout, FUENTE fuente FROM SERV_MED_V_ATENCION    WHERE PREDIO IS NULL AND FECHA >= DATE '2025-01-01' AND FECHA < DATE '2027-01-01'
  UNION ALL SELECT '2-4 EXAMENES',    FUENTE FROM SERV_MED_V_EXAMEN      WHERE PREDIO IS NULL AND FECHA >= DATE '2025-01-01' AND FECHA < DATE '2027-01-01'
  UNION ALL SELECT '5 INCAPACIDADES', FUENTE FROM SERV_MED_V_INCAPACIDAD WHERE PREDIO IS NULL AND FECHA >= DATE '2025-01-01' AND FECHA < DATE '2027-01-01'
  UNION ALL SELECT '6 ACCIDENTES',    FUENTE FROM SERV_MED_V_ACCIDENTE   WHERE PREDIO IS NULL AND FECHA >= DATE '2025-01-01' AND FECHA < DATE '2027-01-01'
  UNION ALL SELECT '7 ANTIDOPING',    FUENTE FROM SERV_MED_V_ANTIDOPING  WHERE PREDIO IS NULL AND FECHA >= DATE '2025-01-01' AND FECHA < DATE '2027-01-01'
  UNION ALL SELECT '8 MATERNIDAD',    FUENTE FROM SERV_MED_V_MATERNIDAD  WHERE PREDIO IS NULL AND FECHA >= DATE '2025-01-01' AND FECHA < DATE '2027-01-01'
)
GROUP BY layout, fuente
ORDER BY layout, fuente;

SPOOL OFF
