-- ============================================================================
-- ETL histórico MySQL -> BIOMETRICO · PASO 1: PREFLIGHT
--
-- BASE: la del PORTAL (JDBC) = BIOMETRICO. NO es ORDS.
-- Conectarse como el usuario con el que va a correr el ETL.
--
-- SOLO LECTURA. No inserta, no borra, no altera nada.
-- Correr ANTES de cualquier corrida del ETL, incluso la de muestra.
-- ============================================================================
SET LINESIZE 200
SET PAGESIZE 200
SET FEEDBACK OFF

PROMPT
PROMPT ===========================================================
PROMPT  A. Identidad de la sesion  (confirmar base y usuario)
PROMPT ===========================================================
SELECT USER                                          AS usuario_conectado,
       SYS_CONTEXT('USERENV','DB_NAME')              AS base,
       SYS_CONTEXT('USERENV','CON_NAME')             AS pdb,
       SYS_CONTEXT('USERENV','SERVER_HOST')          AS host,
       TO_CHAR(SYSTIMESTAMP,'YYYY-MM-DD HH24:MI:SS') AS momento
FROM dual;

PROMPT
PROMPT ===========================================================
PROMPT  B. Las dos tablas destino existen?   (esperado: 2 filas)
PROMPT ===========================================================
SELECT table_name, num_rows AS num_rows_estadistica_puede_estar_vieja
FROM   user_tables
WHERE  table_name IN ('SERV_MED_FS_FILE','SERV_MED_TAG')
ORDER  BY table_name;

PROMPT
PROMPT ===========================================================
PROMPT  C. Conteo REAL de partida  (COUNT(*), no estadisticas)
PROMPT     Anotar estos numeros: son la linea base de la reversa.
PROMPT ===========================================================
SELECT 'SERV_MED_FS_FILE' AS tabla,
       COUNT(*)                                                  AS filas_totales,
       COUNT(CASE WHEN BUSINESS_KEY LIKE 'legacy-%' THEN 1 END)  AS ya_migradas_etl,
       COUNT(CASE WHEN BUSINESS_KEY NOT LIKE 'legacy-%' THEN 1 END) AS del_portal_NO_TOCAR
FROM   SERV_MED_FS_FILE
UNION ALL
SELECT 'SERV_MED_TAG',
       COUNT(*),
       COUNT(CASE WHEN SOURCE_ID IS NOT NULL THEN 1 END),
       COUNT(CASE WHEN SOURCE_ID IS NULL THEN 1 END)
FROM   SERV_MED_TAG;

PROMPT
PROMPT ===========================================================
PROMPT  D. Indice de checksum: DEBE decir NONUNIQUE
PROMPT     Si dice UNIQUE, la carga truena con ORA-00001 por los
PROMPT     261 duplicados legitimos de contenido. NO CONTINUAR.
PROMPT ===========================================================
SELECT index_name, uniqueness, status
FROM   user_indexes
WHERE  table_name = 'SERV_MED_FS_FILE'
ORDER  BY index_name;

PROMPT
PROMPT ===========================================================
PROMPT  E. El usuario puede escribir en las dos tablas?
PROMPT     Si sale vacio y NO eres el dueno del esquema, falta GRANT.
PROMPT ===========================================================
SELECT table_name, privilege
FROM   user_tab_privs_recd
WHERE  table_name IN ('SERV_MED_FS_FILE','SERV_MED_TAG')
UNION ALL
SELECT table_name, 'DUENO DEL OBJETO'
FROM   user_tables
WHERE  table_name IN ('SERV_MED_FS_FILE','SERV_MED_TAG');

PROMPT
PROMPT ===========================================================
PROMPT  F. La funcion de clasificacion de tags existe y compila?
PROMPT     Esperado: VALID. Si esta INVALID, TAG_GROUP sale nulo.
PROMPT ===========================================================
SELECT object_name, object_type, status
FROM   user_objects
WHERE  object_name = 'SERV_MED_FN_TAG_GROUP';

PROMPT
PROMPT ===========================================================
PROMPT  G. Espacio: SOLO donde viven estas dos tablas
PROMPT     El grueso son 15 GB de binarios al FILESYSTEM, no aqui.
PROMPT     En Oracle solo entran ~550 mil tags y ~22 mil metadatos.
PROMPT
PROMPT     Listar los 46 tablespaces del esquema no dice nada: lo
PROMPT     que importa es en cual caen ESTAS tablas y sus indices.
PROMPT ===========================================================
PROMPT
PROMPT -- G1. En que tablespace cae cada objeto
SELECT 'TABLA'  AS que, table_name AS objeto, tablespace_name
FROM   user_tables
WHERE  table_name IN ('SERV_MED_FS_FILE','SERV_MED_TAG')
UNION ALL
SELECT 'INDICE', index_name, tablespace_name
FROM   user_indexes
WHERE  table_name IN ('SERV_MED_FS_FILE','SERV_MED_TAG')
ORDER  BY 1 DESC, 3, 2;

PROMPT
PROMPT -- G2. Libre AHORA en esos tablespaces, y hasta donde pueden crecer
PROMPT     AUTOEXTENSIBLE = YES vuelve irrelevante el "libre ahora":
PROMPT     el datafile crece solo hasta MAX_GB. Si dice NO, el libre
PROMPT     de hoy es el techo y hay que pedir espacio al DBA.
SELECT df.tablespace_name,
       ROUND(SUM(df.bytes)/1024/1024)                      AS mb_asignados,
       ROUND(NVL(MAX(fs.mb_libres), 0))                    AS mb_libres,
       MAX(df.autoextensible)                              AS autoextensible,
       ROUND(SUM(GREATEST(df.maxbytes, df.bytes))/1024/1024/1024) AS max_gb
FROM   user_tablespaces ts
JOIN   dba_data_files df ON df.tablespace_name = ts.tablespace_name
LEFT   JOIN (SELECT tablespace_name, SUM(bytes)/1024/1024 AS mb_libres
               FROM user_free_space GROUP BY tablespace_name) fs
         ON fs.tablespace_name = df.tablespace_name
WHERE  ts.tablespace_name IN (
         SELECT tablespace_name FROM user_tables
          WHERE table_name IN ('SERV_MED_FS_FILE','SERV_MED_TAG')
         UNION
         SELECT tablespace_name FROM user_indexes
          WHERE table_name IN ('SERV_MED_FS_FILE','SERV_MED_TAG'))
GROUP  BY df.tablespace_name
ORDER  BY df.tablespace_name;

PROMPT
PROMPT     Si G2 falla por permisos sobre DBA_DATA_FILES, pedirsela al
PROMPT     DBA o correr solo esto, que no necesita privilegios:
PROMPT       SELECT tablespace_name, ROUND(SUM(bytes)/1024/1024) mb_libres
PROMPT         FROM user_free_space
PROMPT        WHERE tablespace_name IN ('BIOMETRICO','BIOMETRICO_INDEX')
PROMPT        GROUP BY tablespace_name;
PROMPT
PROMPT     Cuanto hace falta: los 551 mil tags son ~52 MB de datos en
PROMPT     el origen; con overhead de CLOB e indices, contar con unos
PROMPT     cientos de MB. Los 22 mil metadatos, decenas de MB. Menos
PROMPT     de 1 GB en total, pero 113 MB de indices NO alcanzan.

PROMPT
PROMPT ===========================================================
PROMPT  FIN DEL PREFLIGHT
PROMPT
PROMPT  Antes de arrancar el ETL, faltan DOS cosas que NO se
PROMPT  comprueban aqui:
PROMPT   1) Probar usuario/contrasena UNA VEZ en SQL Developer.
PROMPT      NUNCA arrancando la aplicacion: el pool abre 10
PROMPT      conexiones y 10 fallos bloquean la cuenta (27-sep-2026).
PROMPT   2) Confirmar el portal.files.root real del servidor y
PROMPT      que tenga >= 20 GB libres.
PROMPT ===========================================================
SET FEEDBACK ON
