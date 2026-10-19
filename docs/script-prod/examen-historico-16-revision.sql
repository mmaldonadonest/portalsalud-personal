-- =====================================================================================
-- examen-historico-16-revision.sql  (16-oct-2026)
-- Base: BIOMETRICO (produccion; tabla del portal SERV_MED_EXAMEN_HISTORICO). SOLO LECTURA.
--
-- Por que: el WS guarda UN examen vigente por NSS (cada examen nuevo pisa al anterior, igual
-- que el PHP). El historial de los examenes vive en SERV_MED_EXAMEN_HISTORICO: una fila por
-- examen CERRADO (guardado con dictamen), desde el modulo Examen medico y el inicial.
-- Desde el 16-oct-2026 la foto (DATOS) tambien incluye los tags (bloques de los formatos EMLE).
-- =====================================================================================

-- 1) Desde cuando hay historico y cuantos examenes, por modulo
SELECT ORIGEN,
       COUNT(*)                          AS EXAMENES,
       COUNT(DISTINCT NSS)               AS NSS,
       MIN(FECHA_EXAMEN)                 AS PRIMERO,
       MAX(FECHA_EXAMEN)                 AS ULTIMO
  FROM SERV_MED_EXAMEN_HISTORICO
 GROUP BY ORIGEN
 ORDER BY ORIGEN;

-- 2) Por tipo de examen (codigo: 1 Admision, 2 Periodico, 3 Cambio de puesto, 4 Post incapacidad,
--    5 Especial, 6 Egreso, 7 Inicial mantenimiento, 8 Periodico mantenimiento; vacio = sin tipo)
SELECT NVL(TIPO_EXAMEN, '(sin tipo)') AS TIPO_EXAMEN, COUNT(*) AS EXAMENES,
       MIN(FECHA_EXAMEN) AS PRIMERO, MAX(FECHA_EXAMEN) AS ULTIMO
  FROM SERV_MED_EXAMEN_HISTORICO
 GROUP BY NVL(TIPO_EXAMEN, '(sin tipo)')
 ORDER BY 1;

-- 3) NSS con mas de un examen cerrado (los que si tienen "movimientos" en el historico)
SELECT NSS, COUNT(*) AS EXAMENES, MIN(FECHA_EXAMEN) AS PRIMERO, MAX(FECHA_EXAMEN) AS ULTIMO
  FROM SERV_MED_EXAMEN_HISTORICO
 GROUP BY NSS
HAVING COUNT(*) > 1
 ORDER BY EXAMENES DESC
 FETCH FIRST 20 ROWS ONLY;
