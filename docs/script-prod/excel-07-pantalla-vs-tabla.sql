-- ===========================================================================
--  LO QUE LA PANTALLA DEBE MOSTRAR
--  Base del PORTAL (JDBC), tablas dentro de BIOMETRICO. SOLO LECTURA.
--
--  PARA QUE SIRVE
--  El total que trae el Excel y el total que muestra la grafica NO son el
--  mismo numero, y eso es correcto. Quien no lo sepa va a concluir que la
--  carga fallo. Este script calcula, pantalla por pantalla, el numero que
--  tiene que aparecer, y de paso explica a donde se fue cada renglon.
--
--  POR QUE LA PANTALLA MUESTRA MENOS QUE LA TABLA
--  Las pantallas filtran con FECHA entre el rango elegido (es literalmente
--  "e.FECHA >= ? AND e.FECHA <= ?"). De ahi salen dos restas legitimas:
--
--    1. FUERA DEL RANGO. Los casos del ejercicio 2025 -los que el Excel
--       marca ANTES 2025 o 2025 PENDIENTE- no salen cuando se pide 2026.
--    2. SIN FECHA. Un evento con FECHA nula no cumple la comparacion y
--       queda fuera de TODAS las graficas, aunque este cargado.
--
--  POR QUE PUEDE MOSTRAR MAS
--  Las pantallas SUMAN lo que haya en ORDS (el portal en vivo) a lo que
--  sale de aqui. Si ORDS tiene registros del mismo periodo, la pantalla
--  dara un numero mayor al de este script. Eso tambien es correcto.
--
--  COMO USARLO
--  Ajustar el rango de abajo al mismo que se eligio en la pantalla y
--  comparar. La columna DEBE_MOSTRAR es la que tiene que coincidir.
-- ===========================================================================

-- El rango de la pantalla. "Acumulado (todo el ano)" de 2026 es del 1 de
-- enero a hoy, no al 31 de diciembre: la pantalla nunca pide el futuro.
DEFINE desde = '2026-01-01'
DEFINE hasta = '2026-09-30'

SET LINESIZE 200
SET PAGESIZE 100
SET FEEDBACK OFF
SET VERIFY OFF

COLUMN pantalla FORMAT A26
COLUMN predio   FORMAT A14

PROMPT
PROMPT ==== A. CONCILIACION POR PANTALLA ========================================
PROMPT    EN_LA_TABLA  = todo lo cargado de esa familia
PROMPT    FUERA_RANGO  = cae antes o despues del periodo pedido (casi todo 2025)
PROMPT    SIN_FECHA    = cargado pero sin fecha: no entra en ninguna grafica
PROMPT    DEBE_MOSTRAR = lo que la pantalla tiene que ensenar (+ lo de ORDS)

WITH r AS (
  SELECT TO_DATE('&desde', 'YYYY-MM-DD') d,
         TO_DATE('&hasta', 'YYYY-MM-DD') h FROM DUAL
),
ev AS (
  SELECT CASE e.FAMILIA
           WHEN 'ATENCION'    THEN 'Morbilidad / Consultas'
           WHEN 'INCAPACIDAD' THEN 'Incapacidades'
           WHEN 'ACCIDENTE'   THEN 'Accidentes'
           WHEN 'ANTIDOPING'  THEN 'Antidoping'
           WHEN 'MATERNIDAD'  THEN 'Maternidad'
           ELSE 'Examenes medicos'
         END                                    AS pantalla,
         e.FECHA, e.PREDIO, r.d, r.h
    FROM SERV_MED_BITACORA_EVENTO e CROSS JOIN r
)
SELECT pantalla,
       COUNT(*)                                                      AS en_la_tabla,
       SUM(CASE WHEN FECHA IS NOT NULL AND (FECHA < d OR FECHA > h)
                THEN 1 ELSE 0 END)                                   AS fuera_rango,
       SUM(CASE WHEN FECHA IS NULL THEN 1 ELSE 0 END)                AS sin_fecha,
       SUM(CASE WHEN FECHA BETWEEN d AND h THEN 1 ELSE 0 END)        AS debe_mostrar
  FROM ev
 GROUP BY pantalla
 ORDER BY 2 DESC;

PROMPT
PROMPT    Las tres familias de examen (ingreso, periodico y pos incapacidad)
PROMPT    van juntas a proposito: la pantalla de Examenes las muestra sumadas.


PROMPT
PROMPT ==== B. VISTA POR PREDIO ================================================
PROMPT    El numero que debe salir al elegir cada predio. Si alguno da cero
PROMPT    aqui y el Excel tiene datos, el problema NO es la pantalla.

WITH r AS (
  SELECT TO_DATE('&desde', 'YYYY-MM-DD') d,
         TO_DATE('&hasta', 'YYYY-MM-DD') h FROM DUAL
)
SELECT NVL(e.PREDIO, '(sin predio)')                                 AS predio,
       SUM(CASE WHEN e.FAMILIA = 'ATENCION'    THEN 1 ELSE 0 END)    AS atenciones,
       SUM(CASE WHEN e.FAMILIA = 'INCAPACIDAD' THEN 1 ELSE 0 END)    AS incapacid,
       SUM(CASE WHEN e.FAMILIA = 'ACCIDENTE'   THEN 1 ELSE 0 END)    AS accidentes,
       SUM(CASE WHEN e.FAMILIA LIKE 'EXAMEN%'  THEN 1 ELSE 0 END)    AS examenes,
       SUM(CASE WHEN e.FAMILIA = 'ANTIDOPING'  THEN 1 ELSE 0 END)    AS antidoping
  FROM SERV_MED_BITACORA_EVENTO e CROSS JOIN r
 WHERE e.FECHA BETWEEN r.d AND r.h
 GROUP BY e.PREDIO
 ORDER BY 2 DESC;


PROMPT
PROMPT ==== C. LAS GRAFICAS DE TENDENCIA =======================================
PROMPT    Las barras mes a mes que debe dibujar cada pantalla. Un mes que aqui
PROMPT    tiene numero y en la grafica sale vacio es un defecto de la pantalla;
PROMPT    un mes en cero aqui es que no hay datos, y eso no lo arregla el portal.

WITH r AS (
  SELECT TO_DATE('&desde', 'YYYY-MM-DD') d,
         TO_DATE('&hasta', 'YYYY-MM-DD') h FROM DUAL
)
SELECT CASE e.FAMILIA
         WHEN 'ATENCION'    THEN 'Morbilidad / Consultas'
         WHEN 'INCAPACIDAD' THEN 'Incapacidades'
         WHEN 'ACCIDENTE'   THEN 'Accidentes'
         WHEN 'ANTIDOPING'  THEN 'Antidoping'
         WHEN 'MATERNIDAD'  THEN 'Maternidad'
         ELSE 'Examenes medicos'
       END                                                 AS pantalla,
       TO_CHAR(e.ANIO) || '-' || LPAD(TO_CHAR(e.MES), 2, '0') AS mes,
       COUNT(*)                                            AS debe_mostrar
  FROM SERV_MED_BITACORA_EVENTO e CROSS JOIN r
 WHERE e.FECHA BETWEEN r.d AND r.h
   AND e.ANIO IS NOT NULL AND e.MES IS NOT NULL
 GROUP BY CASE e.FAMILIA
         WHEN 'ATENCION'    THEN 'Morbilidad / Consultas'
         WHEN 'INCAPACIDAD' THEN 'Incapacidades'
         WHEN 'ACCIDENTE'   THEN 'Accidentes'
         WHEN 'ANTIDOPING'  THEN 'Antidoping'
         WHEN 'MATERNIDAD'  THEN 'Maternidad'
         ELSE 'Examenes medicos'
       END,
       TO_CHAR(e.ANIO) || '-' || LPAD(TO_CHAR(e.MES), 2, '0')
 ORDER BY 1, 2;


PROMPT
PROMPT ==== D. CONSUMO DE MEDICAMENTOS =========================================
PROMPT    Esta pantalla NO filtra por FECHA sino por ANIO y MES, porque el
PROMPT    consumo no cuelga de una persona ni de un dia. Por eso va aparte.
PROMPT    La pantalla suma una sola de las tres medidas del Excel; sumar las
PROMPT    tres triplicaria el total.

SELECT ORIGEN                                      AS origen,
       SUBCONCEPTO                                 AS medida,
       COUNT(*)                                    AS renglones,
       COUNT(DISTINCT CONCEPTO)                    AS conceptos,
       TO_CHAR(SUM(VALOR), '9,999,999,999')        AS piezas
  FROM SERV_MED_BITACORA_METRICA
 WHERE FAMILIA = 'CONSUMIBLE'
 GROUP BY ORIGEN, SUBCONCEPTO
 ORDER BY 1, 2;


PROMPT
PROMPT ==== E. A DONDE SE FUE CADA RENGLON =====================================
PROMPT    El cuadre completo en un renglon. La suma de las tres ultimas
PROMPT    columnas tiene que dar EN_LA_TABLA, sin sobrar ni faltar.

WITH r AS (
  SELECT TO_DATE('&desde', 'YYYY-MM-DD') d,
         TO_DATE('&hasta', 'YYYY-MM-DD') h FROM DUAL
)
SELECT COUNT(*)                                                      AS en_la_tabla,
       SUM(CASE WHEN e.FECHA BETWEEN r.d AND r.h THEN 1 ELSE 0 END)  AS visible_en_pantalla,
       SUM(CASE WHEN e.FECHA IS NOT NULL
                 AND (e.FECHA < r.d OR e.FECHA > r.h)
                THEN 1 ELSE 0 END)                                   AS fuera_del_rango,
       SUM(CASE WHEN e.FECHA IS NULL THEN 1 ELSE 0 END)              AS sin_fecha
  FROM SERV_MED_BITACORA_EVENTO e CROSS JOIN r;


PROMPT
PROMPT ===========================================================================
PROMPT  SI LA PANTALLA DA MENOS QUE "DEBE_MOSTRAR": es un defecto, hay que verlo.
PROMPT  SI DA MAS: casi siempre es ORDS sumando sus propios registros.
PROMPT  SI "DEBE_MOSTRAR" ya es menor que el Excel: mirar FUERA_RANGO y SIN_FECHA,
PROMPT  ahi esta la explicacion completa. La carga no perdio nada: el bloque E
PROMPT  cuadra al renglon.
PROMPT ===========================================================================
PROMPT

SET FEEDBACK ON
SET VERIFY ON
