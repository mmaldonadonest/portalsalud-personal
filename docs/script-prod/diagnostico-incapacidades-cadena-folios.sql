-- ============================================================================================
-- Diagnostico de Incapacidades antes del bloque C-045..C-056 (cadena inicial -> subsecuentes,
-- folio IMSS con mascara, alta que cierra, acumulados) - 17-nov-2026
--
-- Base: BIOMETRICO (PDBPRD), conectado como BIOMETRICO. Lee la tabla que llena el WS de ORDS
-- (TBL_SERV_INCAPACIDAD_MEDICA), la tabla del Excel por NSS (SERV_MED_XLS_INCAPACIDAD) y el
-- catalogo de ORDS (USER_ORDS_*). NO va al portal ni llama a ORDS.
-- SOLO LECTURA: no inserta, no borra, no cambia nada. Se puede correr cuantas veces se quiera.
--
-- Para que sirve:
--   1-2) Columnas reales de la tabla del WS (para disenar las columnas nuevas sin adivinar).
--   3)   Fuente del WS de alta publicada HOY: en docs/contextoWS.txt el INSERT pone ALTA en la
--        columna ST2 y ST2 en ALTA (orden de VALUES cruzado). Se confirma aqui.
--   4)   Valores reales de ST2 y ALTA (si ALTA trae "No_aplica", estan cruzadas).
--   5-7) Folios: cuantos cumplen 2 letras + 6 numeros (C-046), cuales no y cuales se repiten,
--        en el portal, en el Excel y entre los dos.
--   8-9) Fechas y dias: formatos guardados y si DIAS = termino - inicio + 1 (regla de C-045).
--   10)  Excel: como viene la cadena (TIPO I/S, FECHA_PRIMERA) para C-050/C-051/C-054.
--
-- Correr con:  @C:\miguel\portalsalud-personal\docs\script-prod\diagnostico-incapacidades-cadena-folios.sql
-- Salida en:   C:\miguel\diagnostico-incapacidades-salida.txt
-- ============================================================================================
SET PAGESIZE 300
SET LINESIZE 250
SET LONG 200000
SET LONGCHUNKSIZE 200000
SET FEEDBACK ON
SPOOL C:\miguel\diagnostico-incapacidades-salida.txt

PROMPT === 1) Columnas de TBL_SERV_INCAPACIDAD_MEDICA ===
COLUMN COLUMN_NAME FORMAT A30
COLUMN DATA_TYPE FORMAT A15
SELECT COLUMN_ID, COLUMN_NAME, DATA_TYPE, DATA_LENGTH, NULLABLE
  FROM USER_TAB_COLUMNS
 WHERE TABLE_NAME = 'TBL_SERV_INCAPACIDAD_MEDICA'
 ORDER BY COLUMN_ID;

PROMPT === 2) Indices y llave de la tabla (para saber si REG_ID es unico) ===
SELECT i.INDEX_NAME, i.UNIQUENESS, c.COLUMN_NAME
  FROM USER_INDEXES i JOIN USER_IND_COLUMNS c ON c.INDEX_NAME = i.INDEX_NAME
 WHERE i.TABLE_NAME = 'TBL_SERV_INCAPACIDAD_MEDICA'
 ORDER BY i.INDEX_NAME, c.COLUMN_POSITION;
SELECT TRIGGER_NAME, TRIGGERING_EVENT, STATUS
  FROM USER_TRIGGERS WHERE TABLE_NAME = 'TBL_SERV_INCAPACIDAD_MEDICA';

PROMPT === 3) WS de incapacidades publicados en ORDS (patron y fuente) ===
COLUMN PATTERN FORMAT A50
COLUMN METHOD FORMAT A6
SELECT t.URI_TEMPLATE PATTERN, h.METHOD, h.SOURCE_TYPE
  FROM USER_ORDS_TEMPLATES t JOIN USER_ORDS_HANDLERS h ON h.TEMPLATE_ID = t.ID
 WHERE LOWER(t.URI_TEMPLATE) LIKE '%incapacid%'
 ORDER BY t.URI_TEMPLATE, h.METHOD;
PROMPT --- 3b) Fuente del WS de alta (incapacidades, POST): revisar el orden de ST2 / ALTA en el VALUES
SELECT h.SOURCE
  FROM USER_ORDS_TEMPLATES t JOIN USER_ORDS_HANDLERS h ON h.TEMPLATE_ID = t.ID
 WHERE LOWER(t.URI_TEMPLATE) = 'incapacidades' AND h.METHOD = 'POST';

PROMPT === 4) Valores guardados en ST2 y en ALTA (si ALTA trae No_aplica, estan cruzadas) ===
SELECT NVL(TO_CHAR(SUBSTR(ST2, 1, 30)), '(nulo)')  ST2,
       NVL(TO_CHAR(SUBSTR(ALTA, 1, 30)), '(nulo)') ALTA,
       COUNT(*) N
  FROM TBL_SERV_INCAPACIDAD_MEDICA
 GROUP BY TO_CHAR(SUBSTR(ST2, 1, 30)), TO_CHAR(SUBSTR(ALTA, 1, 30))
 ORDER BY N DESC;

PROMPT === 5) Folios del PORTAL por rubro: con mascara AA999999, otro formato o vacios ===
SELECT NVL(TO_CHAR(SUBSTR(RUBRO, 1, 20)), '(nulo)') RUBRO,
       COUNT(*) TOTAL,
       COUNT(CASE WHEN REGEXP_LIKE(UPPER(REPLACE(FOLIO_INCAPACIDAD, ' ')), '^[A-Z]{2}[0-9]{6}$') THEN 1 END) MASCARA_OK,
       COUNT(CASE WHEN TRIM(FOLIO_INCAPACIDAD) IS NOT NULL
                   AND NOT REGEXP_LIKE(UPPER(REPLACE(FOLIO_INCAPACIDAD, ' ')), '^[A-Z]{2}[0-9]{6}$') THEN 1 END) OTRO_FORMATO,
       COUNT(CASE WHEN TRIM(FOLIO_INCAPACIDAD) IS NULL THEN 1 END) SIN_FOLIO
  FROM TBL_SERV_INCAPACIDAD_MEDICA
 GROUP BY TO_CHAR(SUBSTR(RUBRO, 1, 20))
 ORDER BY TOTAL DESC;
PROMPT --- 5b) Portal: folios con otro formato (muestra de 50)
SELECT REG_ID, TO_CHAR(SUBSTR(RUBRO, 1, 20)) RUBRO, FOLIO_INCAPACIDAD, TRUNC(FECHA_REGISTRO) REGISTRO
  FROM TBL_SERV_INCAPACIDAD_MEDICA
 WHERE TRIM(FOLIO_INCAPACIDAD) IS NOT NULL
   AND NOT REGEXP_LIKE(UPPER(REPLACE(FOLIO_INCAPACIDAD, ' ')), '^[A-Z]{2}[0-9]{6}$')
   AND ROWNUM <= 50;

PROMPT === 6) Folios repetidos DENTRO del portal ===
SELECT UPPER(REPLACE(FOLIO_INCAPACIDAD, ' ')) FOLIO, COUNT(*) VECES, COUNT(DISTINCT NSS) NSS_DISTINTOS,
       MIN(TRUNC(FECHA_REGISTRO)) PRIMERO, MAX(TRUNC(FECHA_REGISTRO)) ULTIMO
  FROM TBL_SERV_INCAPACIDAD_MEDICA
 WHERE TRIM(FOLIO_INCAPACIDAD) IS NOT NULL
 GROUP BY UPPER(REPLACE(FOLIO_INCAPACIDAD, ' '))
HAVING COUNT(*) > 1
 ORDER BY VECES DESC;

PROMPT === 7) Folios del EXCEL: mascara y repetidos (solo VALIDO) ===
SELECT NVL(RUBRO, '(nulo)') RUBRO, COUNT(*) TOTAL,
       COUNT(CASE WHEN REGEXP_LIKE(UPPER(REPLACE(FOLIO, ' ')), '^[A-Z]{2}[0-9]{6}$') THEN 1 END) MASCARA_OK,
       COUNT(CASE WHEN FOLIO IS NOT NULL
                   AND NOT REGEXP_LIKE(UPPER(REPLACE(FOLIO, ' ')), '^[A-Z]{2}[0-9]{6}$') THEN 1 END) OTRO_FORMATO,
       COUNT(CASE WHEN FOLIO IS NULL THEN 1 END) SIN_FOLIO
  FROM SERV_MED_XLS_INCAPACIDAD
 WHERE ESTADO_REGISTRO = 'VALIDO'
 GROUP BY RUBRO;
PROMPT --- 7b) Excel: folios repetidos
SELECT UPPER(REPLACE(FOLIO, ' ')) FOLIO, COUNT(*) VECES, COUNT(DISTINCT NSS) NSS_DISTINTOS,
       MIN(FECHA) PRIMERA_FECHA, MAX(FECHA) ULTIMA_FECHA
  FROM SERV_MED_XLS_INCAPACIDAD
 WHERE ESTADO_REGISTRO = 'VALIDO' AND FOLIO IS NOT NULL
 GROUP BY UPPER(REPLACE(FOLIO, ' '))
HAVING COUNT(*) > 1
 ORDER BY VECES DESC;
PROMPT --- 7c) Mismo folio en portal y Excel con NSS DISTINTO (la vista los empata por folio solamente)
SELECT UPPER(REPLACE(p.FOLIO_INCAPACIDAD, ' ')) FOLIO, p.NSS NSS_PORTAL, x.NSS NSS_EXCEL
  FROM TBL_SERV_INCAPACIDAD_MEDICA p
  JOIN SERV_MED_XLS_INCAPACIDAD x
    ON UPPER(REPLACE(x.FOLIO, ' ')) = UPPER(REPLACE(p.FOLIO_INCAPACIDAD, ' '))
 WHERE x.ESTADO_REGISTRO = 'VALIDO' AND p.NSS <> x.NSS;

PROMPT === 8) Portal: formato de las fechas guardadas (texto) ===
SELECT CASE WHEN FECHA_INICIO IS NULL THEN '(nulo)'
            WHEN REGEXP_LIKE(FECHA_INICIO, '^[0-9]{4}-[0-9]{2}-[0-9]{2}') THEN 'AAAA-MM-DD'
            WHEN REGEXP_LIKE(FECHA_INICIO, '^[0-9]{2}/[0-9]{2}/[0-9]{4}') THEN 'DD/MM/AAAA'
            ELSE 'OTRO' END INICIO,
       CASE WHEN FECHA_TERMINO IS NULL THEN '(nulo)'
            WHEN REGEXP_LIKE(FECHA_TERMINO, '^[0-9]{4}-[0-9]{2}-[0-9]{2}') THEN 'AAAA-MM-DD'
            WHEN REGEXP_LIKE(FECHA_TERMINO, '^[0-9]{2}/[0-9]{2}/[0-9]{4}') THEN 'DD/MM/AAAA'
            ELSE 'OTRO' END TERMINO,
       CASE WHEN TRIM(TO_CHAR(SUBSTR(FECHA_PRIMERA_INCAPACIDAD, 1, 30))) IS NULL THEN '(nulo)'
            WHEN REGEXP_LIKE(TO_CHAR(SUBSTR(FECHA_PRIMERA_INCAPACIDAD, 1, 30)), '^[0-9]{4}-[0-9]{2}-[0-9]{2}') THEN 'AAAA-MM-DD'
            ELSE 'OTRO' END PRIMERA,
       NVL(TO_CHAR(SUBSTR(TIPO_INCAPACIDAD, 1, 20)), '(nulo)') TIPO,
       COUNT(*) N
  FROM TBL_SERV_INCAPACIDAD_MEDICA
 GROUP BY CASE WHEN FECHA_INICIO IS NULL THEN '(nulo)'
            WHEN REGEXP_LIKE(FECHA_INICIO, '^[0-9]{4}-[0-9]{2}-[0-9]{2}') THEN 'AAAA-MM-DD'
            WHEN REGEXP_LIKE(FECHA_INICIO, '^[0-9]{2}/[0-9]{2}/[0-9]{4}') THEN 'DD/MM/AAAA'
            ELSE 'OTRO' END,
          CASE WHEN FECHA_TERMINO IS NULL THEN '(nulo)'
            WHEN REGEXP_LIKE(FECHA_TERMINO, '^[0-9]{4}-[0-9]{2}-[0-9]{2}') THEN 'AAAA-MM-DD'
            WHEN REGEXP_LIKE(FECHA_TERMINO, '^[0-9]{2}/[0-9]{2}/[0-9]{4}') THEN 'DD/MM/AAAA'
            ELSE 'OTRO' END,
          CASE WHEN TRIM(TO_CHAR(SUBSTR(FECHA_PRIMERA_INCAPACIDAD, 1, 30))) IS NULL THEN '(nulo)'
            WHEN REGEXP_LIKE(TO_CHAR(SUBSTR(FECHA_PRIMERA_INCAPACIDAD, 1, 30)), '^[0-9]{4}-[0-9]{2}-[0-9]{2}') THEN 'AAAA-MM-DD'
            ELSE 'OTRO' END,
          TO_CHAR(SUBSTR(TIPO_INCAPACIDAD, 1, 20))
 ORDER BY N DESC;

PROMPT === 9) Dias vs fechas (regla C-045: termino = inicio + dias - 1), portal y Excel ===
SELECT FUENTE,
       COUNT(*) CON_LAS_TRES,
       COUNT(CASE WHEN FECHA_TERMINO - FECHA + 1 = DIAS THEN 1 END) CUADRA,
       COUNT(CASE WHEN FECHA_TERMINO - FECHA = DIAS THEN 1 END)     TERMINO_MAS_UNO,
       COUNT(CASE WHEN FECHA_TERMINO - FECHA + 1 NOT IN (DIAS, DIAS + 1) THEN 1 END) OTRO
  FROM SERV_MED_V_INCAPACIDAD
 WHERE FECHA IS NOT NULL AND FECHA_TERMINO IS NOT NULL AND DIAS IS NOT NULL
 GROUP BY FUENTE;

PROMPT === 10) Excel: como viene la cadena (tipo, primera incapacidad, alta) ===
SELECT NVL(TIPO, '(nulo)') TIPO, NVL(RUBRO, '(nulo)') RUBRO,
       COUNT(*) N,
       COUNT(FECHA_PRIMERA) CON_PRIMERA,
       COUNT(CASE WHEN FECHA_PRIMERA = FECHA THEN 1 END) PRIMERA_IGUAL_INICIO,
       COUNT(CASE WHEN ALTA_OPERACION = 'SI' THEN 1 END) CON_ALTA
  FROM SERV_MED_XLS_INCAPACIDAD
 WHERE ESTADO_REGISTRO = 'VALIDO'
 GROUP BY TIPO, RUBRO
 ORDER BY N DESC;
PROMPT --- 10b) Excel: subsecuentes cuya fecha = termino de otra del mismo NSS + 1 (se pueden encadenar solas)
SELECT COUNT(*) SUBSECUENTES,
       COUNT(CASE WHEN EXISTS (SELECT 1 FROM SERV_MED_XLS_INCAPACIDAD a
                                WHERE a.NSS = s.NSS AND a.ESTADO_REGISTRO = 'VALIDO'
                                  AND a.FECHA_TERMINO + 1 = s.FECHA) THEN 1 END) ENCADENA_POR_FECHA
  FROM SERV_MED_XLS_INCAPACIDAD s
 WHERE s.ESTADO_REGISTRO = 'VALIDO' AND s.TIPO = 'S';

SPOOL OFF
PROMPT Listo. Mandar C:\miguel\diagnostico-incapacidades-salida.txt
