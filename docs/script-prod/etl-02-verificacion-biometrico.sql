-- ============================================================================
-- ETL histórico MySQL -> BIOMETRICO · PASO 2: VERIFICACIÓN POST-CARGA
--
-- BASE: la del PORTAL (JDBC) = BIOMETRICO. NO es ORDS.
--
-- SOLO LECTURA.
-- Correr DESPUÉS de la corrida completa y DESPUÉS de copiar los binarios.
--
-- Antes de correrlo, llenar estos dos números con el COUNT(*) que se tomó
-- del ORIGEN CONGELADO (el staging local), no del MySQL productivo.
-- ============================================================================
-- Medidos el 1-oct-2026 sobre el staging local (dump C:\mysql\10012026.sql),
-- que cuadro exacto contra el MySQL productivo. Si se vuelve a sacar un dump,
-- hay que cambiarlos: con ceros el bloque A no dice nada util.
DEFINE origen_files = 22223
DEFINE origen_tags  = 551044

SET LINESIZE 200
SET PAGESIZE 200
SET FEEDBACK OFF

PROMPT
PROMPT ===========================================================
PROMPT  A. EL CUADRE  (la prueba de que la migracion salio completa)
PROMPT
PROMPT     Los 27 huerfanos conocidos son filas de `files` con `url`
PROMPT     vacia: no tienen binario que migrar. Es dato legitimo.
PROMPT ===========================================================
SELECT 'files' AS origen,
       &origen_files                                             AS en_origen,
       (SELECT COUNT(*) FROM SERV_MED_FS_FILE
         WHERE BUSINESS_KEY LIKE 'legacy-%')                     AS migradas,
       &origen_files - (SELECT COUNT(*) FROM SERV_MED_FS_FILE
         WHERE BUSINESS_KEY LIKE 'legacy-%')                     AS diferencia_esperado_27
FROM dual
UNION ALL
SELECT 'tags',
       &origen_tags,
       (SELECT COUNT(*) FROM SERV_MED_TAG WHERE SOURCE_ID IS NOT NULL),
       &origen_tags - (SELECT COUNT(*) FROM SERV_MED_TAG WHERE SOURCE_ID IS NOT NULL)
FROM dual;

PROMPT
PROMPT ===========================================================
PROMPT  B. Nada duplicado  (SOURCE_ID y BUSINESS_KEY son la
PROMPT     idempotencia del ETL: si hay repetidos, corrio mal)
PROMPT     Esperado: 0 y 0.
PROMPT ===========================================================
SELECT (SELECT COUNT(*) FROM (SELECT SOURCE_ID FROM SERV_MED_TAG
          WHERE SOURCE_ID IS NOT NULL
          GROUP BY SOURCE_ID HAVING COUNT(*) > 1))           AS tags_source_id_repetido,
       (SELECT COUNT(*) FROM (SELECT BUSINESS_KEY FROM SERV_MED_FS_FILE
          WHERE BUSINESS_KEY LIKE 'legacy-%'
          GROUP BY BUSINESS_KEY HAVING COUNT(*) > 1))        AS files_business_key_repetido
FROM dual;

PROMPT
PROMPT ===========================================================
PROMPT  C. Estado de los archivos   (esperado: todo ACTIVE, nada
PROMPT     en cuarentena, 0 sin extension, 100% pdf)
PROMPT ===========================================================
SELECT STATUS,
       COUNT(*)                                            AS filas,
       COUNT(CASE WHEN EXTENSION IS NULL THEN 1 END)       AS sin_extension,
       COUNT(CASE WHEN SIZE_BYTES = 0 THEN 1 END)          AS tamano_cero,
       COUNT(CASE WHEN STORAGE_PATH IS NULL THEN 1 END)    AS sin_ruta
FROM   SERV_MED_FS_FILE
WHERE  BUSINESS_KEY LIKE 'legacy-%'
GROUP  BY STATUS;

PROMPT
PROMPT ===========================================================
PROMPT  D. Distribucion por FILE_TYPE   (debe parecerse al perfil:
PROMPT     examen_medico ~16.8k · laboratorio ~2.2k ·
PROMPT     nota_medica ~1.6k · nota_incapacidad ~648 + hashes)
PROMPT ===========================================================
SELECT FILE_TYPE, COUNT(*) AS filas
FROM   SERV_MED_FS_FILE
WHERE  BUSINESS_KEY LIKE 'legacy-%'
GROUP  BY FILE_TYPE
HAVING COUNT(*) >= 100
ORDER  BY COUNT(*) DESC;

PROMPT
PROMPT ===========================================================
PROMPT  E. Distribucion por TAG_GROUP
PROMPT     CLAVE: "OTRO" debe dar 0. Si aparece, el origen trae
PROMPT     tipos que SERV_MED_FN_TAG_GROUP no conoce (los 137
PROMPT     documentados ya no alcanzan) y hay que revisarlos.
PROMPT ===========================================================
SELECT NVL(TAG_GROUP,'(NULO)') AS grupo, COUNT(*) AS filas
FROM   SERV_MED_TAG
WHERE  SOURCE_ID IS NOT NULL
GROUP  BY TAG_GROUP
ORDER  BY COUNT(*) DESC;

PROMPT
PROMPT ===========================================================
PROMPT  F. Fechas historicas preservadas
PROMPT     DATE_UPLOAD debe traer el rango real del legacy
PROMPT     (2023 en adelante), NO todo con la fecha de hoy.
PROMPT ===========================================================
SELECT TO_CHAR(MIN(DATE_UPLOAD),'YYYY-MM-DD') AS mas_antigua,
       TO_CHAR(MAX(DATE_UPLOAD),'YYYY-MM-DD') AS mas_reciente,
       COUNT(CASE WHEN TRUNC(DATE_UPLOAD) = TRUNC(SYSDATE) THEN 1 END) AS con_fecha_de_hoy
FROM   SERV_MED_FS_FILE
WHERE  BUSINESS_KEY LIKE 'legacy-%';

PROMPT
PROMPT ===========================================================
PROMPT  G. Total de archivos en disco que DEBE haber
PROMPT     Comparar contra el conteo en el servidor:
PROMPT       find <files.root> -type f | wc -l
PROMPT     Si el numero de disco es menor, faltan binarios por
PROMPT     copiar: hay filas apuntando a archivos inexistentes.
PROMPT ===========================================================
SELECT COUNT(*)                        AS archivos_que_deben_existir,
       ROUND(SUM(SIZE_BYTES)/1024/1024/1024, 2) AS gb_que_deben_pesar
FROM   SERV_MED_FS_FILE
WHERE  BUSINESS_KEY LIKE 'legacy-%';

PROMPT
PROMPT ===========================================================
PROMPT  H. Muestra para revision fisica
PROMPT     Tomar estas 10 rutas en el servidor y comprobar que el
PROMPT     archivo existe, que pesa lo mismo y que su sha256 coincide.
PROMPT ===========================================================
SELECT ID, SIZE_BYTES, CHECKSUM_SHA256, STORAGE_PATH
FROM   (SELECT ID, SIZE_BYTES, CHECKSUM_SHA256, STORAGE_PATH
        FROM   SERV_MED_FS_FILE
        WHERE  BUSINESS_KEY LIKE 'legacy-%'
        ORDER  BY DBMS_RANDOM.VALUE)
WHERE  ROWNUM <= 10;

PROMPT
PROMPT ===========================================================
PROMPT  I. Duplicados de contenido  (NO es error: un mismo PDF
PROMPT     puede adjuntarse a varios NSS. En QA fueron ~228.)
PROMPT ===========================================================
SELECT COUNT(*) AS checksums_repetidos
FROM   (SELECT CHECKSUM_SHA256
        FROM   SERV_MED_FS_FILE
        WHERE  BUSINESS_KEY LIKE 'legacy-%'
        GROUP  BY CHECKSUM_SHA256
        HAVING COUNT(*) > 1);

PROMPT
PROMPT ===========================================================
PROMPT  J. Nombres con encoding roto  (~96 esperados, cosmetico)
PROMPT     Solo afecta el nombre que se muestra al descargar. La
PROMPT     busqueda es por NSS+FILE_TYPE, nunca por ORIGINAL_NAME.
PROMPT     Esta lista es el insumo para corregirlos a mano despues.
PROMPT ===========================================================
SELECT ID, NSS, ORIGINAL_NAME
FROM   SERV_MED_FS_FILE
WHERE  BUSINESS_KEY LIKE 'legacy-%'
  AND  ORIGINAL_NAME IS NOT NULL
  AND  REGEXP_LIKE(ORIGINAL_NAME, UNISTR('[\00C2\00C3\00C4\00C5]'))
ORDER  BY ID;

SET FEEDBACK ON
