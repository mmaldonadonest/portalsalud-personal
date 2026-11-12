-- =============================================================================
-- E-034 - SOLO LECTURA. Drenaje y Convivencia con animales cruzados (12-nov-2026)
--
-- BASE: BIOMETRICO de produccion (SERV_MED_ANT_PATOLOGICOS del WS + SERV_MED_TAG del portal).
-- Correr conectado como BIOMETRICO. No modifica nada.
--
-- Por que: PR_SERVICIO_MED_EXAMEN1 (PHP) y PR_SERVICIO_MED_EXAMEN1_V2 (portal, hasta el 11-nov) leen
--   cvar8 := DRENAJE  y enseguida  cvar8 := CONVIVENCIA_CON_ANIMALES  (debia ser cvar9).
-- Resultado: la columna DRENAJE guarda la respuesta de Convivencia, CONVIVENCIA_CON_ANIMALES queda
-- vacia y la respuesta real de Drenaje nunca llega a la tabla. El PHP lo compensaba guardando ambas
-- en SERV_MED_TAG (TYPE 'Drenaje' y 'CCA').
--
-- Esto mide: cuantos registros hay, si el tag CCA coincide con la columna DRENAJE (prueba del
-- cruce) y cuantos Drenaje se pueden recuperar del tag. Mandar la salida completa.
-- =============================================================================
SET SERVEROUTPUT ON SIZE UNLIMITED
SET LINESIZE 200
SET PAGESIZE 100
COLUMN QUE FORMAT A45
COLUMN VALOR FORMAT A40
COLUMN TYPE FORMAT A20

PROMPT === 1. Columnas de SERV_MED_ANT_PATOLOGICOS con fecha (para saber si se puede separar lo de antes del 11-nov) ===
SELECT COLUMN_NAME, DATA_TYPE FROM USER_TAB_COLUMNS
 WHERE TABLE_NAME = 'SERV_MED_ANT_PATOLOGICOS'
   AND (DATA_TYPE = 'DATE' OR DATA_TYPE LIKE 'TIMESTAMP%')
 ORDER BY COLUMN_ID;

PROMPT === 2. Totales en la tabla del WS ===
SELECT 'Renglones (NSS)'                              QUE, COUNT(*) N FROM SERV_MED_ANT_PATOLOGICOS
UNION ALL SELECT 'DRENAJE con valor',                     COUNT(*) FROM SERV_MED_ANT_PATOLOGICOS WHERE TRIM(DRENAJE) IS NOT NULL
UNION ALL SELECT 'CONVIVENCIA_CON_ANIMALES con valor',    COUNT(*) FROM SERV_MED_ANT_PATOLOGICOS WHERE TRIM(CONVIVENCIA_CON_ANIMALES) IS NOT NULL
UNION ALL SELECT 'Los dos con valor',                     COUNT(*) FROM SERV_MED_ANT_PATOLOGICOS WHERE TRIM(DRENAJE) IS NOT NULL AND TRIM(CONVIVENCIA_CON_ANIMALES) IS NOT NULL
UNION ALL SELECT 'DRENAJE con valor y CONVIVENCIA vacia', COUNT(*) FROM SERV_MED_ANT_PATOLOGICOS WHERE TRIM(DRENAJE) IS NOT NULL AND TRIM(CONVIVENCIA_CON_ANIMALES) IS NULL;

PROMPT === 3. Valores mas frecuentes de CONVIVENCIA_CON_ANIMALES (si casi todo es vacio, confirma el cruce) ===
SELECT * FROM (SELECT NVL(UPPER(TRIM(CONVIVENCIA_CON_ANIMALES)), '(vacio)') VALOR, COUNT(*) N
                 FROM SERV_MED_ANT_PATOLOGICOS GROUP BY UPPER(TRIM(CONVIVENCIA_CON_ANIMALES)) ORDER BY 2 DESC)
 WHERE ROWNUM <= 15;

PROMPT === 4. Tags del PHP en SERV_MED_TAG (Drenaje / CCA, cualquier mayuscula) ===
SELECT TYPE, COUNT(*) N, COUNT(DISTINCT NSS) NSS_DISTINTOS,
       TO_CHAR(MIN(CREATED_AT), 'YYYY-MM-DD') PRIMERO, TO_CHAR(MAX(CREATED_AT), 'YYYY-MM-DD') ULTIMO
  FROM SERV_MED_TAG
 WHERE UPPER(TYPE) IN ('DRENAJE', 'CCA')
 GROUP BY TYPE ORDER BY TYPE;

PROMPT === 5. Valores mas frecuentes del tag Drenaje (lo que se recuperaria) ===
SELECT * FROM (SELECT NVL(UPPER(TRIM(DBMS_LOB.SUBSTR(CONTENT, 200, 1))), '(vacio)') VALOR, COUNT(*) N
                 FROM SERV_MED_TAG WHERE UPPER(TYPE) = 'DRENAJE'
                GROUP BY UPPER(TRIM(DBMS_LOB.SUBSTR(CONTENT, 200, 1))) ORDER BY 2 DESC)
 WHERE ROWNUM <= 15;

PROMPT === 6. Cruce por NSS: ultimo tag de cada NSS contra la columna DRENAJE ===
WITH t AS (
  SELECT NSS, UPPER(TYPE) TIPO, UPPER(TRIM(DBMS_LOB.SUBSTR(CONTENT, 200, 1))) VALOR,
         ROW_NUMBER() OVER (PARTITION BY NSS, UPPER(TYPE) ORDER BY CREATED_AT DESC) RN
    FROM SERV_MED_TAG WHERE UPPER(TYPE) IN ('DRENAJE', 'CCA')),
d AS (SELECT NSS, VALOR FROM t WHERE TIPO = 'DRENAJE' AND RN = 1),
c AS (SELECT NSS, VALOR FROM t WHERE TIPO = 'CCA'     AND RN = 1),
a AS (SELECT p.NSS, UPPER(TRIM(p.DRENAJE)) DRENAJE, d.VALOR TAG_DRENAJE, c.VALOR TAG_CCA
        FROM SERV_MED_ANT_PATOLOGICOS p
        LEFT JOIN d ON d.NSS = p.NSS
        LEFT JOIN c ON c.NSS = p.NSS
       WHERE TRIM(p.DRENAJE) IS NOT NULL)
SELECT 'NSS con DRENAJE en la tabla'                            QUE, COUNT(*) N FROM a
UNION ALL SELECT 'con tag Drenaje (recuperable)',                 COUNT(*) FROM a WHERE TAG_DRENAJE IS NOT NULL
UNION ALL SELECT 'sin tag Drenaje (se queda sin dato)',           COUNT(*) FROM a WHERE TAG_DRENAJE IS NULL
UNION ALL SELECT 'con tag CCA',                                   COUNT(*) FROM a WHERE TAG_CCA IS NOT NULL
UNION ALL SELECT '  tag CCA = columna DRENAJE (prueba del cruce)', COUNT(*) FROM a WHERE TAG_CCA = DRENAJE
UNION ALL SELECT '  tag CCA distinto de la columna DRENAJE',      COUNT(*) FROM a WHERE TAG_CCA IS NOT NULL AND TAG_CCA <> DRENAJE
UNION ALL SELECT 'tag Drenaje = columna DRENAJE (no cruzado?)',   COUNT(*) FROM a WHERE TAG_DRENAJE = DRENAJE;

PROMPT === 7. Muestra de 20 diferencias tag CCA vs columna DRENAJE (sin NSS) ===
WITH t AS (
  SELECT NSS, UPPER(TRIM(DBMS_LOB.SUBSTR(CONTENT, 200, 1))) VALOR,
         ROW_NUMBER() OVER (PARTITION BY NSS ORDER BY CREATED_AT DESC) RN
    FROM SERV_MED_TAG WHERE UPPER(TYPE) = 'CCA')
SELECT * FROM (SELECT SUBSTR(UPPER(TRIM(p.DRENAJE)), 1, 40) COLUMNA_DRENAJE, SUBSTR(t.VALOR, 1, 40) TAG_CCA
                 FROM SERV_MED_ANT_PATOLOGICOS p JOIN t ON t.NSS = p.NSS AND t.RN = 1
                WHERE TRIM(p.DRENAJE) IS NOT NULL AND t.VALOR <> UPPER(TRIM(p.DRENAJE)))
 WHERE ROWNUM <= 20;
