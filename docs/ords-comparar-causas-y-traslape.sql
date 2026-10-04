-- ============================================================================
-- Comparar el catálogo de causas del portal contra el de los Excel históricos,
-- y medir el traslape entre las dos fuentes de las gráficas.
--
-- BASE: la de ORDS (donde viven TBL_SERV_CONSULTA_MEDICA y el catálogo).
--       NO es la base del portal (SERV_MED_FS_FILE / SERV_MED_TAG).
--
-- SOLO LECTURA. Ningún INSERT, UPDATE, DELETE ni DDL.
--
-- Para qué: la carga histórica de Excel alimentará las gráficas hasta una fecha
-- de corte, y de ahí en adelante seguirá la captura del portal. Para que la serie
-- de diez años sea continua y no un escalón, los dos lados tienen que hablar el
-- mismo idioma. Estas cinco consultas dicen si lo hablan.
-- ============================================================================
SET LINESIZE 200
SET PAGESIZE 500
SET FEEDBACK OFF

PROMPT
PROMPT ###########################################################
PROMPT  1. EL CATALOGO DEL PORTAL  (esperado: 23 causas + Otro)
PROMPT     Esto es lo que el usuario puede elegir hoy en Consulta.
PROMPT ###########################################################
SELECT * FROM SERV_MED_CAT_CAUSA_CONSULTA ORDER BY NOMBRE;

PROMPT
PROMPT ###########################################################
PROMPT  2. LO QUE DE VERDAD HAY CAPTURADO en TBL_SERV_CONSULTA_MEDICA
PROMPT
PROMPT     OJO: no hay llave foranea entre CAUSA y el catalogo (lo
PROMPT     dice ords-causa-consulta.sql). Antes del 24-ago-2026 ese
PROMPT     campo era texto libre. Esta lista muestra el desorden real
PROMPT     con el que hoy se dibuja la grafica de causas.
PROMPT ###########################################################
SELECT NVL(TRIM(CAUSA),'(VACIO)') AS causa_capturada,
       COUNT(*)                                        AS veces,
       TO_CHAR(MIN(FECHA),'YYYY-MM-DD')                AS primera_vez,
       TO_CHAR(MAX(FECHA),'YYYY-MM-DD')                AS ultima_vez
FROM   TBL_SERV_CONSULTA_MEDICA
GROUP  BY NVL(TRIM(CAUSA),'(VACIO)')
ORDER  BY COUNT(*) DESC;

PROMPT
PROMPT ###########################################################
PROMPT  3. CUANTO DE LO CAPTURADO EMPATA CON EL CATALOGO
PROMPT     Si "fuera_del_catalogo" es alto, el lado vivo tambien
PROMPT     necesita mapeo, no solo los Excel.
PROMPT ###########################################################
SELECT COUNT(*)                                                       AS consultas_totales,
       COUNT(CASE WHEN c.NOMBRE IS NOT NULL THEN 1 END)               AS empatan_con_catalogo,
       COUNT(CASE WHEN c.NOMBRE IS NULL AND TRIM(s.CAUSA) IS NOT NULL
                  THEN 1 END)                                         AS fuera_del_catalogo,
       COUNT(CASE WHEN TRIM(s.CAUSA) IS NULL THEN 1 END)              AS sin_causa
FROM   TBL_SERV_CONSULTA_MEDICA s
LEFT   JOIN SERV_MED_CAT_CAUSA_CONSULTA c
       ON UPPER(TRIM(s.CAUSA)) = UPPER(TRIM(c.NOMBRE));

PROMPT
PROMPT ###########################################################
PROMPT  4. EL TRASLAPE  (esta es la consulta que decide la fecha de corte)
PROMPT
PROMPT     Los Excel cubren 2017 a 2026. Aqui se ve cuanto tiene ORDS
PROMPT     en esos mismos anios. Donde ORDS tenga poco y el Excel
PROMPT     mucho, conviene que manden los Excel; el corte deberia ir
PROMPT     donde ORDS ya sea la fuente completa.
PROMPT
PROMPT     Referencia del Excel: ~2,000 atenciones por predio-anio.
PROMPT ###########################################################
SELECT EXTRACT(YEAR FROM FECHA)                     AS anio,
       COUNT(*)                                     AS consultas,
       COUNT(DISTINCT NSS)                          AS personas_distintas,
       COUNT(DISTINCT TRIM(CAUSA))                  AS causas_distintas
FROM   TBL_SERV_CONSULTA_MEDICA
WHERE  FECHA IS NOT NULL
GROUP  BY EXTRACT(YEAR FROM FECHA)
ORDER  BY 1;

PROMPT
PROMPT  ... y el detalle mensual de los dos ultimos anios, para ver
PROMPT      desde que mes ORDS ya es confiable:
SELECT TO_CHAR(FECHA,'YYYY-MM') AS mes, COUNT(*) AS consultas
FROM   TBL_SERV_CONSULTA_MEDICA
WHERE  FECHA >= ADD_MONTHS(TRUNC(SYSDATE,'YYYY'), -12)
GROUP  BY TO_CHAR(FECHA,'YYYY-MM')
ORDER  BY 1;

PROMPT
PROMPT ###########################################################
PROMPT  5. LA OTRA DIMENSION QUE TIENE QUE EMPATAR: TIPO_CONSULTA
PROMPT
PROMPT     El codigo marca una consulta como accidente buscando las
PROMPT     palabras "accidente" u "emergencia" en este campo
PROMPT     (ConsultaReporteDto.esAccidente). El Excel usa RAMO con
PROMPT     cuatro valores: ENF GRAL, SEGUIMIENTO, PASE SALIDA y RT.
PROMPT     Hay que ver como se corresponden.
PROMPT ###########################################################
SELECT NVL(TRIM(TIPO_CONSULTA),'(VACIO)') AS tipo_consulta,
       COUNT(*)                           AS veces
FROM   TBL_SERV_CONSULTA_MEDICA
GROUP  BY NVL(TRIM(TIPO_CONSULTA),'(VACIO)')
ORDER  BY COUNT(*) DESC;

PROMPT
PROMPT ===========================================================
PROMPT  FIN. Pasarme las cinco salidas.
PROMPT  Con la 1 y la 2 armo el mapa de equivalencias contra las 26
PROMPT  causas del Excel; con la 4, la fecha de corte.
PROMPT ===========================================================
SET FEEDBACK ON
