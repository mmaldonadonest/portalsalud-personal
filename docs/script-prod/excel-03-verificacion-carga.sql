-- ============================================================================
-- CARGA DE EXCEL 2026 -- VERIFICACION DESPUES DE CARGAR
--
-- BASE: la del PORTAL (JDBC).
--         en QA          -> ONEWMS_QA
--         en produccion  -> BIOMETRICO
--       NO es ORDS.
--
-- SOLO LECTURA. Ni un INSERT, UPDATE, DELETE ni DDL.
--
-- Correr despues de una corrida en modo full. Contesta tres preguntas en este
-- orden, que es el orden en que importan:
--   1. Se cargo lo que debia?           -> bloques A, B y C
--   2. Los datos sirven para graficar?  -> bloques D, E y F
--   3. Si un numero no cuadra, de donde salio? -> bloques G y H
--
-- Reversa: docs/script-prod/excel-99-reversa-bitacora.sql
-- ============================================================================
SET LINESIZE 200
SET PAGESIZE 300
SET FEEDBACK OFF

PROMPT
PROMPT ##########################################################################
PROMPT #  A. Lo grueso
PROMPT ##########################################################################
SELECT USER AS usuario, SYS_CONTEXT('USERENV','DB_NAME') AS base,
       TO_CHAR(SYSTIMESTAMP,'YYYY-MM-DD HH24:MI') AS momento FROM dual;

SELECT (SELECT COUNT(*) FROM SERV_MED_BITACORA_LOTE)     AS lotes,
       (SELECT COUNT(*) FROM SERV_MED_BITACORA_EVENTO)   AS eventos,
       (SELECT COUNT(*) FROM SERV_MED_BITACORA_ATRIBUTO) AS atributos,
       (SELECT COUNT(DISTINCT PREDIO) FROM SERV_MED_BITACORA_EVENTO) AS predios,
       (SELECT COUNT(DISTINCT NOMBRE_NORM) FROM SERV_MED_BITACORA_EVENTO
         WHERE NOMBRE_NORM IS NOT NULL)                  AS personas
FROM dual;

PROMPT
PROMPT -- Por familia. Lo esperado con las 4 configuradas hoy:
PROMPT --   ATENCION ~20 mil · EXAMEN_INGRESO ~9 mil · resto menor
SELECT FAMILIA,
       COUNT(*)                        AS eventos,
       COUNT(DISTINCT PREDIO)          AS predios,
       COUNT(DISTINCT NOMBRE_NORM)     AS personas,
       TO_CHAR(MIN(FECHA),'YYYY-MM-DD') AS desde,
       TO_CHAR(MAX(FECHA),'YYYY-MM-DD') AS hasta
FROM   SERV_MED_BITACORA_EVENTO
GROUP  BY FAMILIA
ORDER  BY COUNT(*) DESC;

PROMPT
PROMPT ##########################################################################
PROMPT #  B. Por predio  (asi se veran las graficas)
PROMPT ##########################################################################
SELECT NVL(PREDIO,'(sin predio)') AS predio,
       COUNT(*)                   AS eventos,
       COUNT(DISTINCT NOMBRE_NORM) AS personas,
       ROUND(COUNT(*) / NULLIF(COUNT(DISTINCT NOMBRE_NORM),0), 2) AS por_persona
FROM   SERV_MED_BITACORA_EVENTO
GROUP  BY PREDIO
ORDER  BY COUNT(*) DESC;

PROMPT
PROMPT ##########################################################################
PROMPT #  C. EL CUADRE  --  lo mas importante de todo el script
PROMPT #
PROMPT #  Cada Excel declara su propio total en la hoja ACUMULADO. El cargador
PROMPT #  compara contra lo que leyo. Aqui se ve cuantas hojas coinciden.
PROMPT ##########################################################################
PROMPT
PROMPT -- CUATRO estados, no dos. La diferencia importa: hasta el 30-sep-2026 este bloque metia
PROMPT -- todo lo que no era "cuadra" en un solo monton de 11 hojas, y ahi habia tres cosas
PROMPT -- distintas, una sola de las cuales merece atencion.
SELECT CASE CUADRA
         WHEN 'S' THEN 'cuadra'
         WHEN 'Z' THEN 'el archivo no lleva su acumulado (hoja en ceros)'
         WHEN 'N' THEN '*** NO CUADRA -- revisar'
         ELSE 'sin comparar (no hay hoja ACUMULADO o el predio no empareja)'
       END AS resultado,
       COUNT(*) AS hojas
FROM   SERV_MED_BITACORA_LOTE
GROUP  BY CUADRA
ORDER  BY 2 DESC;

PROMPT
PROMPT -- 'Z' NO ES UNA FALLA. Se abrio la hoja ACUMULADO de los examenes periodicos el
PROMPT -- 30-sep-2026: existe, trae el predio en su renglon y los doce meses en CERO. El origen
PROMPT -- nunca la lleno. Ocho de las once hojas que salian como "*** NO CUADRA" eran esto, con
PROMPT -- diferencias de 183, 152 y 90 renglones que parecian perdida de datos. De esos archivos
PROMPT -- simplemente no hay contra que cuadrar; el dato que se cargo es el de la hoja de detalle.
SELECT PREDIO, FAMILIA, HOJA, FILAS_LEIDAS AS leidas, ARCHIVO
FROM   SERV_MED_BITACORA_LOTE
WHERE  CUADRA = 'Z'
ORDER  BY FILAS_LEIDAS DESC;

PROMPT
PROMPT -- Las que NO cuadran de verdad. Al 30-sep-2026 quedan tres y ninguna pasa de 2 renglones:
PROMPT -- AIFA nuevo ingreso (-2), SMO periodico (-1) y TOLUCA pos incapacidad (+1).
PROMPT
PROMPT -- La de AIFA esta averiguada: el Excel dice 1,683 y se leyeron 1,681 porque las filas 130
PROMPT -- y 792 traen '*' y 'b' en la columna del nombre. Es basura de captura que el filtro
PROMPT -- rechaza y el Excel si cuenta en su total, o sea que el cargador tiene razon y el archivo
PROMPT -- no. A esa escala es lo esperable.
PROMPT
PROMPT -- Lo que este bloque tiene que atrapar es una diferencia GRANDE, que si seria perdida de
PROMPT -- datos; el bloque G lleva de ahi al renglon del Excel.
SELECT PREDIO, FAMILIA, HOJA,
       FILAS_LEIDAS       AS leidas,
       ACUMULADO_ESPERADO AS dice_el_xls,
       FILAS_LEIDAS - ACUMULADO_ESPERADO AS diferencia,
       ARCHIVO
FROM   SERV_MED_BITACORA_LOTE
WHERE  CUADRA = 'N'
ORDER  BY ABS(FILAS_LEIDAS - ACUMULADO_ESPERADO) DESC;

PROMPT
PROMPT -- Lotes que terminaron mal o con avisos.
PROMPT
PROMPT -- DESCARTADAS son rechazos de verdad y nada mas. En el ensayo del 30-sep-2026 esta
PROMPT -- columna decia 19,718 y parecia una carga con dos tercios de basura: eran renglones
PROMPT -- buenos que el tope de 25 no alcanzo a meter. Ya no se cuentan ahi; el tope queda
PROMPT -- anotado en MENSAJES. En una corrida full el tope no existe.
SELECT ESTADO, COUNT(*) AS hojas,
       SUM(FILAS_LEIDAS) AS leidas, SUM(FILAS_INSERTADAS) AS insertadas,
       SUM(FILAS_DESCARTADAS) AS descartadas
FROM   SERV_MED_BITACORA_LOTE
GROUP  BY ESTADO ORDER BY 1;

PROMPT
PROMPT ##########################################################################
PROMPT #  D. Calidad de lo cargado
PROMPT #
PROMPT #  NSS va SIEMPRE en NULL y es correcto: los Excel no lo traen. La
PROMPT #  identidad se resuelve por NOMBRE_NORM (decision del 29-sep-2026).
PROMPT ##########################################################################
SELECT COUNT(*)                                                   AS eventos,
       COUNT(CASE WHEN FECHA IS NULL THEN 1 END)                  AS sin_fecha,
       COUNT(CASE WHEN PREDIO IS NULL THEN 1 END)                 AS sin_predio,
       COUNT(CASE WHEN NOMBRE_NORM IS NULL THEN 1 END)            AS sin_nombre,
       COUNT(CASE WHEN GENERO IS NULL THEN 1 END)                 AS sin_genero,
       COUNT(CASE WHEN RANGO_EDAD IS NULL THEN 1 END)             AS sin_rango_edad,
       COUNT(CASE WHEN NSS IS NOT NULL THEN 1 END)                AS con_nss_REVISAR
FROM   SERV_MED_BITACORA_EVENTO;

PROMPT
PROMPT -- El rango de edad DEBE salir como "18-25", sin espacios: es el formato que
PROMPT -- produce DashboardConsultaService.rangoEdad(). Si aparece "18 - 25" la
PROMPT -- grafica sale con el doble de barras.
SELECT NVL(RANGO_EDAD,'(nulo)') AS rango_edad, COUNT(*) AS eventos
FROM   SERV_MED_BITACORA_EVENTO
GROUP  BY RANGO_EDAD ORDER BY 1;

PROMPT
PROMPT -- Genero: solo FEM y MASC
SELECT NVL(GENERO,'(nulo)') AS genero, COUNT(*) AS eventos
FROM   SERV_MED_BITACORA_EVENTO GROUP BY GENERO ORDER BY 2 DESC;

PROMPT
PROMPT -- ANIOS. La carga es de 2026; el anio anterior es legitimo (una incapacidad que arranca
PROMPT -- en diciembre se reporta en enero) y se ven ~45 renglones asi.
PROMPT
PROMPT -- CUALQUIER OTRO ANIO NO DEBE APARECER. El ensayo del 30-sep-2026 saco 2029, 2626, 2002
PROMPT -- y 2023: erratas de captura que el cargador guardaba tal cual. Son ~20 renglones de
PROMPT -- 12,500, pero la tendencia agrupa por ANIO/MES y el eje salia de 2002 a 2626 con un
PROMPT -- punto de un renglon en cada extremo. El cargador ya los corrige conservando dia y mes
PROMPT -- y deja el aviso; si aqui sale un anio raro, es que se cargo con la version vieja.
SELECT ANIO, COUNT(*) AS eventos, COUNT(DISTINCT FAMILIA) AS familias
FROM   SERV_MED_BITACORA_EVENTO
GROUP  BY ANIO ORDER BY ANIO;

PROMPT
PROMPT -- PREDIOS QUE DEBERIAN ESTAR FUNDIDOS. El mismo predio se escribe distinto de un archivo
PROMPT -- a otro de la misma carpeta y sin fundirlos la grafica sale con 18 barras donde hay 15,
PROMPT -- cada par con la mitad de sus atenciones.
PROMPT
PROMPT -- Devuelve el CONTEO y no los renglones: con FEEDBACK OFF, una consulta sin resultados
PROMPT -- no imprime nada, y "nada" se lee igual que "no corrio". Un cero explicito si se lee.
SELECT COUNT(*) AS a_fundir_DEBE_SER_0
FROM   SERV_MED_BITACORA_EVENTO
WHERE  PREDIO IN ('MIKELS', 'FLORANEO', 'U TEPALCAPA');

PROMPT
PROMPT ##########################################################################
PROMPT #  E. Los atributos: lo propio de cada familia
PROMPT ##########################################################################
PROMPT -- Se agrupa TAMBIEN por familia. Sin eso, la lista mezcla atributos de familias
PROMPT -- distintas y el numero no corresponde a ninguna pantalla: el portal consulta siempre
PROMPT -- con FAMILIA = 'ATENCION' (BitacoraHistoricoRepository), nunca el revuelto.
SELECT e.FAMILIA,
       a.NOMBRE AS atributo,
       COUNT(*)                          AS renglones,
       COUNT(DISTINCT a.VALOR)           AS valores_distintos,
       COUNT(a.VALOR_NUM)                AS con_numero,
       COUNT(a.VALOR_FECHA)              AS con_fecha
FROM   SERV_MED_BITACORA_ATRIBUTO a
JOIN   SERV_MED_BITACORA_EVENTO e ON e.ID = a.EVENTO_ID
GROUP  BY e.FAMILIA, a.NOMBRE
ORDER  BY e.FAMILIA, COUNT(*) DESC;

PROMPT
PROMPT -- CAUSA: es lo que alimenta la pantalla Causas. Deben ser las ~26 del catalogo de 2026,
PROMPT -- no texto libre, y SOLO de la familia ATENCION.
PROMPT
PROMPT -- El filtro por familia no es adorno. Sin el salian 30 valores en vez de 26, y los cuatro
PROMPT -- de sobra --EG (105), RT 2 (44), RT 1 (41), INTERNA (5), MAT (1)-- venian del examen pos
PROMPT -- incapacidad, donde el bloque se llama CAUSA pero sus columnas son el RAMO de la
PROMPT -- incapacidad. Leido asi, EG parecia el segundo motivo de consulta mas frecuente del ano.
PROMPT -- Desde el 30-sep-2026 ese bloque se guarda como RAMO_INCAPACIDAD y ya no puede mezclarse.
SELECT a.VALOR AS causa, COUNT(*) AS atenciones
FROM   SERV_MED_BITACORA_ATRIBUTO a
JOIN   SERV_MED_BITACORA_EVENTO e ON e.ID = a.EVENTO_ID
WHERE  a.NOMBRE = 'CAUSA' AND e.FAMILIA = 'ATENCION'
GROUP  BY a.VALOR ORDER BY COUNT(*) DESC;

PROMPT
PROMPT -- LESION_ME: alimenta la pantalla Musculoesqueleticas. El bloque tiene 15 columnas pero
PROMPT -- NO tienen que salir 15 valores: en 2026 se usaron 10, porque de las otras cinco no
PROMPT -- hubo ni un caso. Menos de 15 es normal; MAS de 15 seria un tipo nuevo en el origen.
PROMPT
PROMPT -- Viene vacio en la mayoria de las atenciones y eso es correcto: solo se llena en riesgo
PROMPT -- de trabajo. En 2026 son 1,584 de 12,570, o sea el 13%.
SELECT a.VALOR AS lesion, COUNT(*) AS casos
FROM   SERV_MED_BITACORA_ATRIBUTO a
JOIN   SERV_MED_BITACORA_EVENTO e ON e.ID = a.EVENTO_ID
WHERE  a.NOMBRE = 'LESION_ME' AND e.FAMILIA = 'ATENCION'
GROUP  BY a.VALOR ORDER BY COUNT(*) DESC;

PROMPT
PROMPT -- DICTAMEN de los examenes. Servicio medico confirmo el 29-sep-2026:
PROMPT --   15 apto · 45 apto condicionado · 60 apto restringido (= inclusion) · 30 no apto
PROMPT --   SOLO EL 30 DESCALIFICA. El 60 es apto.
SELECT e.FAMILIA, a.VALOR AS dictamen, COUNT(*) AS examenes
FROM   SERV_MED_BITACORA_ATRIBUTO a
JOIN   SERV_MED_BITACORA_EVENTO e ON e.ID = a.EVENTO_ID
WHERE  a.NOMBRE = 'DICTAMEN'
GROUP  BY e.FAMILIA, a.VALOR ORDER BY e.FAMILIA, COUNT(*) DESC;

PROMPT
PROMPT -- RAMO_INCAPACIDAD: el bloque que antes se confundia con CAUSA. Cinco valores, del examen
PROMPT -- pos incapacidad: EG · MAT · RT 1 · RT 2 · INTERNA.
SELECT a.VALOR AS ramo, COUNT(*) AS examenes
FROM   SERV_MED_BITACORA_ATRIBUTO a
WHERE  a.NOMBRE = 'RAMO_INCAPACIDAD'
GROUP  BY a.VALOR ORDER BY COUNT(*) DESC;

PROMPT
PROMPT ##########################################################################
PROMPT #  F. Personas  --  el KPI que se resuelve por nombre
PROMPT ##########################################################################
SELECT COUNT(DISTINCT NOMBRE_NORM)                      AS personas_distintas,
       COUNT(*)                                         AS atenciones,
       ROUND(COUNT(*) / NULLIF(COUNT(DISTINCT NOMBRE_NORM),0), 2) AS por_persona
FROM   SERV_MED_BITACORA_EVENTO
WHERE  FAMILIA = 'ATENCION';

PROMPT
PROMPT -- Nombres en MAS DE UN predio: o la misma persona que roto, o homonimos.
PROMPT -- Medido en 2026: 48 casos de 4,613 personas (1%). Se cuentan una vez.
SELECT COUNT(*) AS nombres_en_varios_predios FROM (
  SELECT NOMBRE_NORM FROM SERV_MED_BITACORA_EVENTO
   WHERE FAMILIA = 'ATENCION' AND NOMBRE_NORM IS NOT NULL
   GROUP BY NOMBRE_NORM HAVING COUNT(DISTINCT PREDIO) > 1);

PROMPT
PROMPT ##########################################################################
PROMPT #  G. Rastreo: de una cifra de la grafica a la celda del Excel
PROMPT ##########################################################################
PROMPT -- Diez renglones al azar con su origen exacto
SELECT * FROM (
  SELECT e.FAMILIA, e.PREDIO, TO_CHAR(e.FECHA,'YYYY-MM-DD') AS fecha,
         SUBSTR(e.NOMBRE,1,28) AS nombre, e.GENERO, e.RANGO_EDAD,
         e.ORIGEN_HOJA, e.ORIGEN_FILA, SUBSTR(e.ORIGEN_ARCHIVO,1,38) AS archivo
  FROM   SERV_MED_BITACORA_EVENTO e
  ORDER  BY DBMS_RANDOM.VALUE)
WHERE ROWNUM <= 10;

PROMPT
PROMPT ##########################################################################
PROMPT #  H. Los avisos de la corrida
PROMPT ##########################################################################
SELECT PREDIO, FAMILIA, HOJA,
       LENGTH(MENSAJES) AS largo_mensajes,
       SUBSTR(MENSAJES, 1, 150) AS primeros_avisos
FROM   SERV_MED_BITACORA_LOTE
WHERE  MENSAJES IS NOT NULL
ORDER  BY LENGTH(MENSAJES) DESC
FETCH FIRST 10 ROWS ONLY;

PROMPT
PROMPT ==========================================================================
PROMPT  LO QUE TIENE QUE SALIR
PROMPT
PROMPT  Lo de la derecha es lo que salio en la carga real del 30-sep-2026 en QA. Sirve de
PROMPT  referencia: una desviacion grande contra esas cifras es senal de algo, no la cifra sola.
PROMPT
PROMPT   A  21,186 eventos · 68,798 atributos · 66 lotes · 15 predios
PROMPT      ATENCION 12,570 · INGRESO 7,069 · PERIODICO 1,270 · POS_INCAP 277
PROMPT   B  15 predios, de AIFA (5,108) a SMO (28)
PROMPT   C  54 cuadra · 8 acumulado en cero · 3 no cuadran · 1 sin comparar
PROMPT      DESCARTADAS = 0 en todo. Si sale distinto de cero, hay rechazos que investigar.
PROMPT   D  sin_predio 0 · con_nss 0 · sin_fecha 46 (0.2%) · rango de edad SIN espacios
PROMPT      ANIO: SOLO 2026 (21,135) y 2025 (51). Cualquier otro = cargado con el jar viejo.
PROMPT      a_fundir_DEBE_SER_0 = 0
PROMPT   E  CAUSA 26 valores, sin EG ni RT ni INTERNA · LESION_ME 10 de 15 posibles
PROMPT      RAMO_INCAPACIDAD aparte, 5 valores, solo en POS_INCAP
PROMPT   F  4,679 personas en 12,570 atenciones = 2.69 por persona
PROMPT      49 nombres en mas de un predio (1%), se cuentan una vez
PROMPT   G  cada renglon rastreable hasta archivo, hoja y fila
PROMPT   H  los avisos de "anio imposible" deben estar AHI, con el valor original:
PROMPT      "fila 801 · anio imposible 2002, se tomo 2026-05-23"
PROMPT
PROMPT  Si algo no cuadra, el bloque G lleva de la cifra a la celda que la origino.
PROMPT ==========================================================================
SET FEEDBACK ON
