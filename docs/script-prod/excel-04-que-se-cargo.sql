-- ===========================================================================
--  QUE SE CARGO  ·  base del PORTAL (JDBC), tablas dentro de BIOMETRICO
--
--  SOLO LECTURA. Cinco consultas para ensenar que la carga quedo. No cambia
--  nada, no hace commit, se puede correr cuantas veces se quiera.
--
--  El cuadre fino (anios fuera de rango, predios a fundir) esta en el otro:
--    docs\script-prod\excel-03-verificacion-carga.sql
-- ===========================================================================

SET LINESIZE 200
SET PAGESIZE 100
SET FEEDBACK OFF

PROMPT
PROMPT ==== 1. Las seis tablas y cuanto tiene cada una ============================

SELECT 'SERV_MED_BITACORA_LOTE'       AS tabla, COUNT(*) AS renglones FROM SERV_MED_BITACORA_LOTE
UNION ALL
SELECT 'SERV_MED_BITACORA_EVENTO',    COUNT(*) FROM SERV_MED_BITACORA_EVENTO
UNION ALL
SELECT 'SERV_MED_BITACORA_ATRIBUTO',  COUNT(*) FROM SERV_MED_BITACORA_ATRIBUTO
UNION ALL
SELECT 'SERV_MED_BITACORA_METRICA',   COUNT(*) FROM SERV_MED_BITACORA_METRICA
UNION ALL
SELECT 'SERV_MED_BITACORA_ESTADO_MES',COUNT(*) FROM SERV_MED_BITACORA_ESTADO_MES
UNION ALL
SELECT 'SERV_MED_BITACORA_NOMENCL',   COUNT(*) FROM SERV_MED_BITACORA_NOMENCL
ORDER BY 2 DESC;

PROMPT
PROMPT    Esperado: LOTE 166 · EVENTO 24,236 · ATRIBUTO 118,515 · METRICA 4,742
PROMPT    ESTADO_MES y NOMENCL en cero: son de fase 3, todavia no se llenan
PROMPT

PROMPT
PROMPT ==== 2. Por familia: de donde sale cada pantalla ===========================

COLUMN familia   FORMAT A18
COLUMN primera   FORMAT A12
COLUMN ultima    FORMAT A12

SELECT e.FAMILIA                        AS familia,
       COUNT(DISTINCT e.ID)             AS eventos,
       COUNT(a.ID)                      AS atributos,
       COUNT(DISTINCT e.PREDIO)         AS predios,
       TO_CHAR(MIN(e.FECHA), 'DD/MM/YYYY') AS primera,
       TO_CHAR(MAX(e.FECHA), 'DD/MM/YYYY') AS ultima
  FROM SERV_MED_BITACORA_EVENTO e
  LEFT JOIN SERV_MED_BITACORA_ATRIBUTO a ON a.EVENTO_ID = e.ID
 GROUP BY e.FAMILIA
 ORDER BY eventos DESC;

PROMPT
PROMPT ==== 3. Por predio: que tanto aporta cada uno ==============================

COLUMN predio FORMAT A20

SELECT NVL(PREDIO, '(sin predio)') AS predio,
       COUNT(*)                    AS eventos,
       COUNT(DISTINCT FAMILIA)     AS familias
  FROM SERV_MED_BITACORA_EVENTO
 GROUP BY PREDIO
 ORDER BY eventos DESC;

PROMPT
PROMPT ==== 4. Consumo de medicamentos (no cuelga de una persona) =================
PROMPT    ORIGEN: MATRIZ = las 12 columnas de meses · EQUIPO = hoja MATERIAL FIJO

COLUMN origen FORMAT A10

SELECT ORIGEN                  AS origen,
       COUNT(*)                AS metricas,
       COUNT(DISTINCT CONCEPTO) AS conceptos,
       COUNT(DISTINCT PREDIO)  AS predios,
       -- formato ancho a proposito: si el numero no cabe, Oracle imprime ###
       TO_CHAR(SUM(VALOR), '9,999,999,999') AS piezas
  FROM SERV_MED_BITACORA_METRICA
 GROUP BY ORIGEN
 ORDER BY 1;

PROMPT
PROMPT ==== 5. Un renglon de verdad, con su rastro al Excel =======================
PROMPT    Una atencion al azar y los atributos que se le leyeron. ORIGEN_ARCHIVO
PROMPT    + ORIGEN_HOJA + ORIGEN_FILA es la celda exacta de donde salio.

COLUMN que      FORMAT A22
COLUMN dato     FORMAT A60

SELECT 'archivo'        AS que, e.ORIGEN_ARCHIVO AS dato FROM SERV_MED_BITACORA_EVENTO e WHERE e.ID = (SELECT MIN(ID) FROM SERV_MED_BITACORA_EVENTO WHERE FAMILIA = 'ATENCION')
UNION ALL
SELECT 'hoja / fila',   e.ORIGEN_HOJA || ' · fila ' || e.ORIGEN_FILA FROM SERV_MED_BITACORA_EVENTO e WHERE e.ID = (SELECT MIN(ID) FROM SERV_MED_BITACORA_EVENTO WHERE FAMILIA = 'ATENCION')
UNION ALL
SELECT 'fecha',         TO_CHAR(e.FECHA, 'DD/MM/YYYY') FROM SERV_MED_BITACORA_EVENTO e WHERE e.ID = (SELECT MIN(ID) FROM SERV_MED_BITACORA_EVENTO WHERE FAMILIA = 'ATENCION')
UNION ALL
SELECT 'predio',        e.PREDIO FROM SERV_MED_BITACORA_EVENTO e WHERE e.ID = (SELECT MIN(ID) FROM SERV_MED_BITACORA_EVENTO WHERE FAMILIA = 'ATENCION')
UNION ALL
SELECT 'atrib: ' || a.NOMBRE, a.VALOR
  FROM SERV_MED_BITACORA_ATRIBUTO a
 WHERE a.EVENTO_ID = (SELECT MIN(ID) FROM SERV_MED_BITACORA_EVENTO WHERE FAMILIA = 'ATENCION');

PROMPT
PROMPT ===========================================================================
PROMPT  Si el bloque 1 trae los cuatro conteos y el 5 ensena un renglon con sus
PROMPT  atributos, la carga esta. No se escribio nada: no hace falta commit.
PROMPT ===========================================================================
PROMPT

SET FEEDBACK ON
