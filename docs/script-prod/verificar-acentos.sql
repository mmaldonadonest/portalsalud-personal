-- =====================================================================================
-- verificar-acentos.sql   -   SOLO LECTURA. No modifica nada.
--
-- Comprueba que los 13 textos con acentos y ñ que sembró el DDL entraron correctos.
-- Es la única cosa del paso 1 que no se pudo probar en QA: allí esas filas ya existían
-- y los MERGE reportaron "0 rows merged", así que nunca llegaron a escribirse.
--
-- POR QUE NO BASTA MIRARLOS EN PANTALLA
-- -------------------------------------
-- Leer "Ver menú" en la salida puede engañar en las dos direcciones: el dato puede estar
-- bien y verse mal (culpa del encoding del cliente), o estar mal y verse bien. Por eso el
-- BLOQUE 2 compara contra UNISTR('\00FA'), que produce el carácter U+00FA correcto sin
-- importar cómo se codifique el texto de esta consulta. Ese es el veredicto real.
-- =====================================================================================

-- OJO: BODY_TEMPLATE es un CLOB y SUBSTR sobre CLOB devuelve CLOB, que un UNION ALL no
-- admite junto a VARCHAR2 -> ORA-01790. Va envuelto en TO_CHAR (el texto mide ~70
-- caracteres, muy lejos del limite de 4000 bytes). Falló asi en la primera corrida.
SET LINESIZE 200
SET PAGESIZE 60

-- -------------------------------------------------------------------------------------
-- BLOQUE 0: juego de caracteres de la base (contexto)
--   Lo normal y deseable es AL32UTF8. Si fuera WE8ISO8859P1 los acentos caben igual,
--   pero un carácter ocupa 1 byte y las comparaciones del bloque 2 hay que leerlas
--   distinto (LONGITUD_BYTES sería igual a LONGITUD_CARS).
-- -------------------------------------------------------------------------------------
SELECT parameter, value
  FROM nls_database_parameters
 WHERE parameter IN ('NLS_CHARACTERSET', 'NLS_NCHAR_CHARACTERSET');

-- -------------------------------------------------------------------------------------
-- BLOQUE 1: los 13 textos, para leerlos a ojo
--   Deben verse: menú · Política · auditoría · huérfanos · Recuperación · contraseña
-- -------------------------------------------------------------------------------------
PROMPT
PROMPT === Los textos tal como quedaron guardados ===
SELECT 'PERMISSION.NAME'      AS origen, CODE AS clave, NAME AS texto
  FROM SERV_MED_SEC_PERMISSION WHERE CODE = 'MENU_VIEW'
UNION ALL
SELECT 'FS_FILE_POLICY.DESC', POLICY_CODE, DESCRIPTION
  FROM SERV_MED_FS_FILE_POLICY
UNION ALL
SELECT 'JOB_CATALOG.NAME', CODE, NAME
  FROM SERV_MED_JOB_CATALOG
UNION ALL
SELECT 'NOTIF_TEMPLATE.SUBJECT', CODE, SUBJECT
  FROM SERV_MED_NOTIF_TEMPLATE
UNION ALL
SELECT 'NOTIF_TEMPLATE.BODY', CODE, TO_CHAR(SUBSTR(BODY_TEMPLATE, 1, 90))
  FROM SERV_MED_NOTIF_TEMPLATE
ORDER BY 1, 2;

-- -------------------------------------------------------------------------------------
-- BLOQUE 2: EL VEREDICTO. Independiente del encoding del cliente.
--   UNISTR('\00FA') es siempre 'ú', UNISTR('\00F1') siempre 'ñ', etc., sin importar
--   cómo viaje el texto de esta consulta. Si una fila dice MAL, ese texto se guardó
--   corrupto. Todas deben decir OK.
-- -------------------------------------------------------------------------------------
PROMPT
PROMPT === VEREDICTO (todas deben decir OK) ===
SELECT 'menu con u acentuada' AS esperado,
       CASE WHEN NAME = 'Ver men' || UNISTR('\00FA') THEN 'OK' ELSE 'MAL' END AS resultado,
       NAME AS valor_actual
  FROM SERV_MED_SEC_PERMISSION WHERE CODE = 'MENU_VIEW'
UNION ALL
SELECT 'Politica con i acentuada',
       CASE WHEN DESCRIPTION = 'Pol' || UNISTR('\00ED') || 'tica por defecto para cargas'
            THEN 'OK' ELSE 'MAL' END, DESCRIPTION
  FROM SERV_MED_FS_FILE_POLICY WHERE POLICY_CODE = 'DEFAULT_UPLOAD_POLICY'
UNION ALL
SELECT 'auditoria con i acentuada',
       CASE WHEN NAME = 'Purgar auditor' || UNISTR('\00ED') || 'a antigua'
            THEN 'OK' ELSE 'MAL' END, NAME
  FROM SERV_MED_JOB_CATALOG WHERE CODE = 'JOB_PURGE_AUDIT'
UNION ALL
SELECT 'huerfanos con e acentuada',
       CASE WHEN NAME = 'Escaneo de hu' || UNISTR('\00E9') || 'rfanos de archivos'
            THEN 'OK' ELSE 'MAL' END, NAME
  FROM SERV_MED_JOB_CATALOG WHERE CODE = 'JOB_FILE_ORPHAN_SCAN'
UNION ALL
SELECT 'Recuperacion + contrasena (o acentuada y enie)',
       CASE WHEN SUBJECT = 'Recuperaci' || UNISTR('\00F3') || 'n de contrase'
                           || UNISTR('\00F1') || 'a'
            THEN 'OK' ELSE 'MAL' END, SUBJECT
  FROM SERV_MED_NOTIF_TEMPLATE WHERE CODE = 'PASSWORD_RESET'
UNION ALL
SELECT 'codigo + contrasena en el cuerpo de la plantilla',
       CASE WHEN BODY_TEMPLATE LIKE '%c' || UNISTR('\00F3') || 'digo%'
                 AND BODY_TEMPLATE LIKE '%contrase' || UNISTR('\00F1') || 'a%'
            THEN 'OK' ELSE 'MAL' END, TO_CHAR(SUBSTR(BODY_TEMPLATE, 1, 60))
  FROM SERV_MED_NOTIF_TEMPLATE WHERE CODE = 'PASSWORD_RESET';

-- -------------------------------------------------------------------------------------
-- BLOQUE 3: bytes contra caracteres
--   En AL32UTF8 un acento ocupa 2 bytes, así que LONGITUD_BYTES debe ser MAYOR que
--   LONGITUD_CARS. Si son iguales, el acento se perdió (se guardó un '?' o similar).
--   Si la diferencia es el doble de la esperada, hubo doble codificación (Ã³ en vez de ó).
-- -------------------------------------------------------------------------------------
PROMPT
PROMPT === Bytes vs caracteres (en AL32UTF8, bytes DEBE ser mayor) ===
SELECT CODE AS clave, SUBJECT AS texto,
       LENGTH(SUBJECT)  AS longitud_cars,
       LENGTHB(SUBJECT) AS longitud_bytes,
       LENGTHB(SUBJECT) - LENGTH(SUBJECT) AS acentos_detectados
  FROM SERV_MED_NOTIF_TEMPLATE
 ORDER BY CODE;
-- 'Recuperación de contraseña' tiene 2 caracteres acentuados (ó y ñ) -> diferencia = 2
-- 'Bienvenido a Portal Salud' no tiene ninguno                      -> diferencia = 0

-- -------------------------------------------------------------------------------------
-- BLOQUE 4: los comentarios del diccionario
--   COMMENT ON TABLE / COLUMN también llevan acentos y se guardan en el diccionario,
--   no en las tablas. Deben leerse "médicos", "normalización", "reconciliación".
-- -------------------------------------------------------------------------------------
PROMPT
PROMPT === Comentarios de tabla y columna ===
SELECT table_name, comments
  FROM user_tab_comments
 WHERE table_name IN ('SERV_MED_TAG', 'SERV_MED_FS_FILE',
                      'SERV_MED_IMPORT_LOTE', 'SERV_MED_IMPORT_FILA')
   AND comments IS NOT NULL
 ORDER BY table_name;

SELECT table_name, column_name, comments
  FROM user_col_comments
 WHERE table_name IN ('SERV_MED_TAG', 'SERV_MED_FS_FILE')
   AND comments IS NOT NULL
 ORDER BY table_name, column_name;

-- =====================================================================================
-- SI ALGO SALIO MAL
-- =====================================================================================
-- Son etiquetas visibles en la aplicación, no claves ni identificadores: se corrigen con
-- estos UPDATE y no hace falta volver a correr el DDL. UNISTR no depende del encoding del
-- cliente, así que estos UPDATE escriben el carácter correcto aunque SQL Developer esté
-- mal configurado. Están comentados: descomentar sólo lo que el BLOQUE 2 marcó MAL.
--
-- UPDATE SERV_MED_SEC_PERMISSION
--    SET NAME = 'Ver men' || UNISTR('\00FA')
--  WHERE CODE = 'MENU_VIEW';
--
-- UPDATE SERV_MED_FS_FILE_POLICY
--    SET DESCRIPTION = 'Pol' || UNISTR('\00ED') || 'tica por defecto para cargas'
--  WHERE POLICY_CODE = 'DEFAULT_UPLOAD_POLICY';
--
-- UPDATE SERV_MED_JOB_CATALOG
--    SET NAME = 'Purgar auditor' || UNISTR('\00ED') || 'a antigua'
--  WHERE CODE = 'JOB_PURGE_AUDIT';
--
-- UPDATE SERV_MED_JOB_CATALOG
--    SET NAME = 'Escaneo de hu' || UNISTR('\00E9') || 'rfanos de archivos'
--  WHERE CODE = 'JOB_FILE_ORPHAN_SCAN';
--
-- UPDATE SERV_MED_NOTIF_TEMPLATE
--    SET SUBJECT = 'Recuperaci' || UNISTR('\00F3') || 'n de contrase' || UNISTR('\00F1') || 'a',
--        BODY_TEMPLATE = 'Hola ${name}, usa este c' || UNISTR('\00F3')
--                        || 'digo para restablecer tu contrase' || UNISTR('\00F1')
--                        || 'a: ${code}'
--  WHERE CODE = 'PASSWORD_RESET';
--
-- COMMIT;
--
-- Los comentarios del diccionario (BLOQUE 4) se rehacen con COMMENT ON TABLE/COLUMN, y
-- son puramente documentales: si quedaron mal, puede dejarse para después.
-- =====================================================================================
