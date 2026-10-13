-- =============================================================================
-- TRASLAPE: cuantos eventos de ene-jun 2026 estan en el Excel Y en el portal.
-- SOLO LECTURA: solo SELECT. No crea, no modifica, no borra nada.
--
-- BASE: BIOMETRICO de PRODUCCION. Ahi viven las dos cosas que se comparan:
--   - Lo cargado del Excel (por nombre):  SERV_MED_BITACORA_EVENTO / _ATRIBUTO
--   - Lo capturado en el portal y el PHP: TBL_SERV_CONSULTA_MEDICA,
--     TBL_SERV_INCAPACIDAD_MEDICA, SERV_MED_ACCIDENTE, SERV_MED_RESULTADO_EXAMEN_HIST
--   (el PHP escribe por los mismos WS en las mismas tablas, asi que ya va incluido)
--
-- POR QUE ES APROXIMADO: la carga actual del Excel NO tiene NSS, solo nombre. Para
-- cruzar se toma el NSS del portal, se busca su nombre en BIO_EMPLEADO, se normaliza
-- igual que el cargador (mayusculas, sin acentos, Ñ->N, sin signos, espacios simples) y
-- se compara contra NOMBRE_NORM del Excel en los dos ordenes (apellidos-nombre y
-- nombre-apellidos). Incapacidades se cruza ademas por FOLIO, que es exacto.
-- Con los layouts nuevos (con NSS) este cruce sera exacto.
--
-- Pasame la salida de los 5 bloques.
-- =============================================================================
SET LINESIZE 220
SET PAGESIZE 500
SET FEEDBACK OFF
SET VERIFY OFF

-- Periodo a comparar: lo que cubren los Excel que ya tenemos (el ultimo archivo es del 3-jul).
-- HASTA es exclusivo.
DEFINE desde = "DATE '2026-01-01'"
DEFINE hasta = "DATE '2026-07-01'"

-- Normalizacion del nombre del biometrico, identica a Normalizador.nombrePersona del cargador.
-- Los acentos y la Ñ van con UNISTR para no depender del encoding de SQL Developer.
DEFINE norm_ini = "TRIM(REGEXP_REPLACE(REGEXP_REPLACE(TRANSLATE(UPPER("
DEFINE norm_fin = "), UNISTR('\00C1\00C9\00CD\00D3\00DA\00C0\00C8\00CC\00D2\00D9\00D1\00DC'), 'AEIOUAEIOUNU'), '[^A-Z0-9 ]', ' '), ' {2,}', ' '))"


PROMPT
PROMPT ###########################################################################
PROMPT  1. VOLUMEN POR MES: cuantos eventos tiene cada fuente, por layout
PROMPT     Si el portal esta casi en cero en un layout, ahi no hay traslape.
PROMPT ###########################################################################
WITH exc AS (
  SELECT CASE e.FAMILIA WHEN 'ATENCION' THEN '1 ATENCIONES'
                        WHEN 'EXAMEN_INGRESO' THEN '2 EXAMEN NUEVO INGRESO'
                        WHEN 'INCAPACIDAD' THEN '5 INCAPACIDADES'
                        WHEN 'ACCIDENTE' THEN '6 ACCIDENTES' END layout,
         e.ANIO * 100 + e.MES aniomes
    FROM SERV_MED_BITACORA_EVENTO e
   WHERE e.FAMILIA IN ('ATENCION', 'EXAMEN_INGRESO', 'INCAPACIDAD', 'ACCIDENTE')
     AND e.ANIO * 100 + e.MES BETWEEN TO_NUMBER(TO_CHAR(&desde, 'YYYYMM'))
                                  AND TO_NUMBER(TO_CHAR(&hasta - 1, 'YYYYMM'))
), por AS (
  SELECT '1 ATENCIONES' layout, TO_NUMBER(TO_CHAR(c.FECHA, 'YYYYMM')) aniomes
    FROM TBL_SERV_CONSULTA_MEDICA c
   WHERE c.FECHA >= &desde AND c.FECHA < &hasta
  UNION ALL
  SELECT '2 EXAMEN NUEVO INGRESO', TO_NUMBER(TO_CHAR(h.FECHA_REGISTRO, 'YYYYMM'))
    FROM SERV_MED_RESULTADO_EXAMEN_HIST h
   WHERE h.FECHA_REGISTRO >= &desde AND h.FECHA_REGISTRO < &hasta
  UNION ALL
  SELECT '5 INCAPACIDADES', TO_NUMBER(TO_CHAR(i.f, 'YYYYMM'))
    FROM (SELECT COALESCE(
                   CASE WHEN REGEXP_LIKE(a.FECHA_INICIO, '^[0-9]{4}-[0-9]{2}-[0-9]{2}')
                          THEN TO_DATE(SUBSTR(a.FECHA_INICIO, 1, 10), 'YYYY-MM-DD')
                        WHEN REGEXP_LIKE(a.FECHA_INICIO, '^[0-9]{2}/[0-9]{2}/[0-9]{4}')
                          THEN TO_DATE(SUBSTR(a.FECHA_INICIO, 1, 10), 'DD/MM/YYYY') END,
                   TRUNC(a.FECHA_REGISTRO)) f
            FROM TBL_SERV_INCAPACIDAD_MEDICA a) i
   WHERE i.f >= &desde AND i.f < &hasta
  UNION ALL
  SELECT '6 ACCIDENTES', TO_NUMBER(TO_CHAR(NVL(x.FECHA_ACCIDENTE, x.FECHA_REGISTRO), 'YYYYMM'))
    FROM SERV_MED_ACCIDENTE x
   WHERE NVL(x.FECHA_ACCIDENTE, x.FECHA_REGISTRO) >= &desde
     AND NVL(x.FECHA_ACCIDENTE, x.FECHA_REGISTRO) < &hasta
), meses AS (
  SELECT layout, aniomes FROM exc UNION SELECT layout, aniomes FROM por
)
SELECT m.layout, m.aniomes mes,
       (SELECT COUNT(*) FROM exc WHERE exc.layout = m.layout AND exc.aniomes = m.aniomes) excel,
       (SELECT COUNT(*) FROM por WHERE por.layout = m.layout AND por.aniomes = m.aniomes) portal
  FROM meses m
 ORDER BY m.layout, m.aniomes;


PROMPT
PROMPT ###########################################################################
PROMPT  2. ATENCIONES: consultas del portal que tambien estan en el Excel
PROMPT     (misma persona por nombre + mismo dia; y con tolerancia de +-1 dia,
PROMPT      porque el portal guarda como fecha el momento de la captura)
PROMPT ###########################################################################
WITH por AS (
  SELECT ROWIDTOCHAR(c.ROWID) rid, c.NSS nss, TRUNC(c.FECHA) f
    FROM TBL_SERV_CONSULTA_MEDICA c
   WHERE c.FECHA >= &desde AND c.FECHA < &hasta
), bio AS (
  SELECT DISTINCT b.EMP_NSS nss,
         &norm_ini b.EMP_APELLIDO || ' ' || b.EMP_APELLIDO2 || ' ' || b.EMP_NOMBRE &norm_fin n1,
         &norm_ini b.EMP_NOMBRE || ' ' || b.EMP_APELLIDO || ' ' || b.EMP_APELLIDO2 &norm_fin n2
    FROM BIO_EMPLEADO b
   WHERE b.EMP_NSS IN (SELECT nss FROM por)
), exc AS (
  SELECT DISTINCT e.NOMBRE_NORM n, TRUNC(e.FECHA) f
    FROM SERV_MED_BITACORA_EVENTO e
   WHERE e.FAMILIA = 'ATENCION' AND e.FECHA >= &desde - 1 AND e.FECHA < &hasta + 1
), par AS (
  SELECT DISTINCT b.nss, x.f FROM bio b JOIN exc x ON x.n IN (b.n1, b.n2)
), por2 AS (
  SELECT p.*, CASE WHEN bn.nss IS NULL THEN 1 ELSE 0 END sin_bio
    FROM por p LEFT JOIN (SELECT DISTINCT nss FROM bio) bn ON bn.nss = p.nss
)
SELECT TO_CHAR(p.f, 'YYYY-MM') mes,
       COUNT(DISTINCT p.rid) portal,
       COUNT(DISTINCT CASE WHEN p.sin_bio = 1 THEN p.rid END) nss_sin_nombre_en_bio,
       COUNT(DISTINCT CASE WHEN m.f = p.f THEN p.rid END) en_excel_mismo_dia,
       COUNT(DISTINCT CASE WHEN m.f IS NOT NULL THEN p.rid END) en_excel_mas_menos_1_dia
  FROM por2 p
  LEFT JOIN par m ON m.nss = p.nss AND m.f BETWEEN p.f - 1 AND p.f + 1
 GROUP BY TO_CHAR(p.f, 'YYYY-MM')
 ORDER BY 1;


PROMPT
PROMPT ###########################################################################
PROMPT  3. INCAPACIDADES: del portal que tambien estan en el Excel
PROMPT     por_folio  = mismo folio (exacto)
PROMPT     por_nombre = sin folio en comun, pero misma persona y mismo inicio
PROMPT ###########################################################################
WITH por AS (
  SELECT ROWIDTOCHAR(a.ROWID) rid, a.NSS nss,
         UPPER(REPLACE(a.FOLIO_INCAPACIDAD, ' ')) folio,
         COALESCE(
           CASE WHEN REGEXP_LIKE(a.FECHA_INICIO, '^[0-9]{4}-[0-9]{2}-[0-9]{2}')
                  THEN TO_DATE(SUBSTR(a.FECHA_INICIO, 1, 10), 'YYYY-MM-DD')
                WHEN REGEXP_LIKE(a.FECHA_INICIO, '^[0-9]{2}/[0-9]{2}/[0-9]{4}')
                  THEN TO_DATE(SUBSTR(a.FECHA_INICIO, 1, 10), 'DD/MM/YYYY') END,
           TRUNC(a.FECHA_REGISTRO)) f
    FROM TBL_SERV_INCAPACIDAD_MEDICA a
), por_r AS (
  SELECT * FROM por WHERE f >= &desde AND f < &hasta
), excf AS (
  SELECT DISTINCT UPPER(REPLACE(at.VALOR, ' ')) folio
    FROM SERV_MED_BITACORA_EVENTO e
    JOIN SERV_MED_BITACORA_ATRIBUTO at ON at.EVENTO_ID = e.ID
   WHERE e.FAMILIA = 'INCAPACIDAD' AND at.NOMBRE LIKE '%FOLIO%' AND at.VALOR IS NOT NULL
), bio AS (
  SELECT DISTINCT b.EMP_NSS nss,
         &norm_ini b.EMP_APELLIDO || ' ' || b.EMP_APELLIDO2 || ' ' || b.EMP_NOMBRE &norm_fin n1,
         &norm_ini b.EMP_NOMBRE || ' ' || b.EMP_APELLIDO || ' ' || b.EMP_APELLIDO2 &norm_fin n2
    FROM BIO_EMPLEADO b
   WHERE b.EMP_NSS IN (SELECT nss FROM por_r)
), exn AS (
  SELECT DISTINCT e.NOMBRE_NORM n, TRUNC(e.FECHA) f
    FROM SERV_MED_BITACORA_EVENTO e
   WHERE e.FAMILIA = 'INCAPACIDAD'
), par AS (
  SELECT DISTINCT b.nss, x.f FROM bio b JOIN exn x ON x.n IN (b.n1, b.n2)
)
SELECT TO_CHAR(p.f, 'YYYY-MM') mes,
       COUNT(DISTINCT p.rid) portal,
       COUNT(DISTINCT CASE WHEN p.folio IS NOT NULL THEN p.rid END) con_folio,
       COUNT(DISTINCT CASE WHEN ef.folio IS NOT NULL THEN p.rid END) en_excel_por_folio,
       COUNT(DISTINCT CASE WHEN ef.folio IS NULL AND m.f IS NOT NULL THEN p.rid END) en_excel_por_nombre
  FROM por_r p
  LEFT JOIN excf ef ON ef.folio = p.folio
  LEFT JOIN par m ON m.nss = p.nss AND m.f = p.f
 GROUP BY TO_CHAR(p.f, 'YYYY-MM')
 ORDER BY 1;


PROMPT
PROMPT ###########################################################################
PROMPT  4. ACCIDENTES y EXAMENES: del portal que tambien estan en el Excel
PROMPT     Accidentes: misma persona y MISMO MES (el Excel de accidentes no trae
PROMPT       fecha del evento, solo el mes reportado).
PROMPT     Examenes: misma persona y +-1 dia. OJO: el historial del portal guarda
PROMPT       todos los tipos de examen, y en el Excel de nuevo ingreso casi todos
PROMPT       son candidatos que aun no tienen NSS en el biometrico.
PROMPT ###########################################################################
WITH por AS (
  SELECT '6 ACCIDENTES' layout, ROWIDTOCHAR(x.ROWID) rid, x.NSS nss,
         TRUNC(NVL(x.FECHA_ACCIDENTE, x.FECHA_REGISTRO)) f
    FROM SERV_MED_ACCIDENTE x
   WHERE NVL(x.FECHA_ACCIDENTE, x.FECHA_REGISTRO) >= &desde
     AND NVL(x.FECHA_ACCIDENTE, x.FECHA_REGISTRO) < &hasta
  UNION ALL
  SELECT '2 EXAMEN NUEVO INGRESO', ROWIDTOCHAR(h.ROWID), h.NSS, TRUNC(h.FECHA_REGISTRO)
    FROM SERV_MED_RESULTADO_EXAMEN_HIST h
   WHERE h.FECHA_REGISTRO >= &desde AND h.FECHA_REGISTRO < &hasta
), bio AS (
  SELECT DISTINCT b.EMP_NSS nss,
         &norm_ini b.EMP_APELLIDO || ' ' || b.EMP_APELLIDO2 || ' ' || b.EMP_NOMBRE &norm_fin n1,
         &norm_ini b.EMP_NOMBRE || ' ' || b.EMP_APELLIDO || ' ' || b.EMP_APELLIDO2 &norm_fin n2
    FROM BIO_EMPLEADO b
   WHERE b.EMP_NSS IN (SELECT nss FROM por)
), exc AS (
  SELECT DISTINCT DECODE(e.FAMILIA, 'ACCIDENTE', '6 ACCIDENTES', '2 EXAMEN NUEVO INGRESO') layout,
         e.NOMBRE_NORM n, TRUNC(e.FECHA) f, e.ANIO * 100 + e.MES aniomes
    FROM SERV_MED_BITACORA_EVENTO e
   WHERE e.FAMILIA IN ('ACCIDENTE', 'EXAMEN_INGRESO')
), par AS (
  SELECT DISTINCT x.layout, b.nss, x.f, x.aniomes
    FROM bio b JOIN exc x ON x.n IN (b.n1, b.n2)
)
SELECT p.layout, TO_CHAR(p.f, 'YYYY-MM') mes,
       COUNT(DISTINCT p.rid) portal,
       COUNT(DISTINCT CASE WHEN m.nss IS NOT NULL THEN p.rid END) en_excel
  FROM por p
  LEFT JOIN par m
         ON m.layout = p.layout AND m.nss = p.nss
        AND ( (p.layout = '6 ACCIDENTES' AND m.aniomes = TO_NUMBER(TO_CHAR(p.f, 'YYYYMM')))
           OR (p.layout <> '6 ACCIDENTES' AND m.f BETWEEN p.f - 1 AND p.f + 1) )
 GROUP BY p.layout, TO_CHAR(p.f, 'YYYY-MM')
 ORDER BY 1, 2;


PROMPT
PROMPT ###########################################################################
PROMPT  5. ATENCIONES POR PREDIO (lado Excel): donde se concentra el volumen
PROMPT     El portal no guarda predio; este bloque solo dice cuanto pesa cada
PROMPT     predio del Excel, para decidir el corte por predio si hiciera falta.
PROMPT ###########################################################################
SELECT e.PREDIO, COUNT(*) atenciones_excel
  FROM SERV_MED_BITACORA_EVENTO e
 WHERE e.FAMILIA = 'ATENCION' AND e.FECHA >= &desde AND e.FECHA < &hasta
 GROUP BY e.PREDIO
 ORDER BY 2 DESC;

PROMPT
PROMPT ===========================================================================
PROMPT  COMO SE LEE
PROMPT   - Bloque 1: portal ~0 en un layout -> ahi manda el Excel, no hay traslape.
PROMPT   - Bloques 2-4: en_excel alto respecto a portal -> se captura doble; la carga
PROMPT     nueva tiene que aplicar "manda el portal" tambien a la foto inicial.
PROMPT   - nss_sin_nombre_en_bio alto -> el cruce por nombre no alcanza a ver esos casos.
PROMPT ===========================================================================
SET FEEDBACK ON
UNDEFINE desde hasta norm_ini norm_fin
