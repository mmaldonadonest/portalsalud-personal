-- ===========================================================================
--  CUADRE: LO QUE DICE EL EXCEL CONTRA LO QUE QUEDO CARGADO
--  Base del PORTAL (JDBC), tablas dentro de BIOMETRICO. SOLO LECTURA.
--
--  Cada Excel trae una hoja ACUMULADO con los totales que servicio medico
--  declara. El cargador conto los renglones y comparo; el veredicto quedo en
--  SERV_MED_BITACORA_LOTE.CUADRA, hoja por hoja:
--
--     S   cuadra exacto
--     N   NO cuadra
--     Z   el Excel no lleva total (su ACUMULADO esta en cero)
--    nulo no hay hoja ACUMULADO, o su predio no empareja con el de la hoja
--
--  LO QUE ESTE CUADRE SI DETECTA: que falten o sobren renglones.
--  LO QUE NO DETECTA: que un renglon este bien contado pero mal clasificado.
--  Un predio equivocado, una fecha de otro anio o una causa leida de la
--  columna de al lado cuadran perfecto en el total y estan mal. Eso se busca
--  con el bloque E, comparando por dimension y no por total.
-- ===========================================================================

SET LINESIZE 200
SET PAGESIZE 200
SET FEEDBACK OFF

COLUMN situacion FORMAT A48
COLUMN predio    FORMAT A14
COLUMN familia   FORMAT A17
COLUMN hoja      FORMAT A22
COLUMN archivo   FORMAT A46

PROMPT
PROMPT ==== A. EL TABLERO: como quedo cada hoja =================================

SELECT CASE NVL(CUADRA, '-')
         WHEN 'S' THEN 'Cuadra exacto con el ACUMULADO'
         WHEN 'N' THEN 'NO CUADRA  <<< revisar'
         WHEN 'Z' THEN 'El Excel no lleva total (ACUMULADO en cero)'
         ELSE          'Sin ACUMULADO: no hay contra que comparar'
       END                 AS situacion,
       COUNT(*)            AS hojas,
       SUM(FILAS_LEIDAS)   AS renglones
  FROM SERV_MED_BITACORA_LOTE
 GROUP BY CASE NVL(CUADRA, '-')
         WHEN 'S' THEN 'Cuadra exacto con el ACUMULADO'
         WHEN 'N' THEN 'NO CUADRA  <<< revisar'
         WHEN 'Z' THEN 'El Excel no lleva total (ACUMULADO en cero)'
         ELSE          'Sin ACUMULADO: no hay contra que comparar'
       END
 ORDER BY 2 DESC;


PROMPT
PROMPT ==== B. LAS QUE NO CUADRAN  ==============================================
PROMPT    Son pocas y la diferencia es de uno o dos renglones. Hay que abrir el
PROMPT    archivo y ver cual de los dos numeros tiene razon: pasa que el
PROMPT    ACUMULADO del Excel esta desactualizado, no que falte el dato.

SELECT PREDIO                               AS predio,
       FAMILIA                              AS familia,
       HOJA                                 AS hoja,
       FILAS_LEIDAS                         AS se_leyeron,
       ACUMULADO_ESPERADO                   AS dice_el_excel,
       FILAS_LEIDAS - ACUMULADO_ESPERADO    AS diferencia,
       ARCHIVO                              AS archivo
  FROM SERV_MED_BITACORA_LOTE
 WHERE CUADRA = 'N'
 ORDER BY ABS(FILAS_LEIDAS - ACUMULADO_ESPERADO) DESC, PREDIO;


PROMPT
PROMPT ==== C. DONDE ESTA EL HUECO: hojas sin con que comparar ==================
PROMPT    Estas familias no traen hoja ACUMULADO. No es que esten mal: es que
PROMPT    nadie las esta verificando. Es aqui donde conviene invertir.

SELECT FAMILIA               AS familia,
       COUNT(*)              AS hojas,
       SUM(FILAS_LEIDAS)     AS renglones
  FROM SERV_MED_BITACORA_LOTE
 WHERE CUADRA IS NULL
 GROUP BY FAMILIA
 ORDER BY 3 DESC;


PROMPT
PROMPT ==== D. RENGLONES QUE SE LEYERON Y NO SE GUARDARON =======================
PROMPT    DESCARTADAS tiene que ser 0 en todo. Un descarte es un renglon que el
PROMPT    cargador vio y decidio no guardar; si aparece alguno hay que saber por
PROMPT    que antes de dar el dato por bueno.
PROMPT
PROMPT    CONSUMIBLE va aparte a proposito: ahi INSERTADAS cuenta metricas y no
PROMPT    eventos, asi que el numero es legitimamente distinto de LEIDAS.

SELECT FAMILIA                   AS familia,
       SUM(FILAS_LEIDAS)         AS leidas,
       SUM(FILAS_INSERTADAS)     AS insertadas,
       SUM(FILAS_DESCARTADAS)    AS descartadas
  FROM SERV_MED_BITACORA_LOTE
 GROUP BY FAMILIA
 ORDER BY 2 DESC;


PROMPT
PROMPT ==== E. LA TABLA PARA PEGAR CONTRA EL EXCEL ==============================
PROMPT    Predio x familia, que es como servicio medico lleva sus numeros. Esta
PROMPT    es la que se manda a revisar: no pregunta "cuadra el total" sino
PROMPT    "cuadra cada casilla", que es donde se ve un predio mal asignado.
PROMPT
PROMPT    Se cuenta sobre EVENTO y no sobre LOTE a proposito: asi se mide lo que
PROMPT    las pantallas realmente muestran, no lo que la bitacora dice que hizo.

SELECT NVL(PREDIO, '(sin predio)') AS predio,
       FAMILIA                     AS familia,
       COUNT(*)                    AS eventos,
       COUNT(DISTINCT NOMBRE_NORM) AS personas,
       MIN(ANIO)                   AS anio_min,
       MAX(ANIO)                   AS anio_max
  FROM SERV_MED_BITACORA_EVENTO
 GROUP BY PREDIO, FAMILIA
 ORDER BY predio, familia;


PROMPT
PROMPT ==== F. POR MES: para cotejar contra el reporte mensual ==================
PROMPT    Si servicio medico reporta por mes, esta es la que se compara. Un mes
PROMPT    con cero donde deberia haber actividad vale mas que cualquier total.

SELECT FAMILIA                                            AS familia,
       ANIO                                               AS anio,
       SUM(CASE WHEN MES = 1  THEN 1 ELSE 0 END)          AS ene,
       SUM(CASE WHEN MES = 2  THEN 1 ELSE 0 END)          AS feb,
       SUM(CASE WHEN MES = 3  THEN 1 ELSE 0 END)          AS mar,
       SUM(CASE WHEN MES = 4  THEN 1 ELSE 0 END)          AS abr,
       SUM(CASE WHEN MES = 5  THEN 1 ELSE 0 END)          AS may,
       SUM(CASE WHEN MES = 6  THEN 1 ELSE 0 END)          AS jun,
       SUM(CASE WHEN MES = 7  THEN 1 ELSE 0 END)          AS jul,
       SUM(CASE WHEN MES = 8  THEN 1 ELSE 0 END)          AS ago,
       SUM(CASE WHEN MES = 9  THEN 1 ELSE 0 END)          AS sep,
       SUM(CASE WHEN MES = 10 THEN 1 ELSE 0 END)          AS oct,
       SUM(CASE WHEN MES = 11 THEN 1 ELSE 0 END)          AS nov,
       SUM(CASE WHEN MES = 12 THEN 1 ELSE 0 END)          AS dic,
       SUM(CASE WHEN MES IS NULL THEN 1 ELSE 0 END)       AS sin_mes,
       COUNT(*)                                           AS total
  FROM SERV_MED_BITACORA_EVENTO
 GROUP BY FAMILIA, ANIO
 ORDER BY FAMILIA, ANIO;


PROMPT
PROMPT ===========================================================================
PROMPT  COMO LEERLO
PROMPT    A  el tablero. Lo que importa es cuantas hojas quedaron sin verificar.
PROMPT    B  las que no cuadran. Son pocas: se abren una por una.
PROMPT    C  donde falta verificacion. Aqui es donde conviene trabajar.
PROMPT    D  descartadas tiene que ser cero.
PROMPT    E  y F  son las que se le pasan a servicio medico para cotejar.
PROMPT ===========================================================================
PROMPT

SET FEEDBACK ON
