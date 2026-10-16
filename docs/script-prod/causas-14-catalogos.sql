-- ============================================================================================
-- Causas de consulta: catalogo del portal y textos realmente usados (SOLO LECTURA) - 14-oct-2026
--
-- Base: BIOMETRICO (PDBPRD). Tablas del portal: SERV_MED_CAT_CAUSA_CONSULTA (catalogo administrable),
-- TBL_SERV_CONSULTA_MEDICA (consultas capturadas en el portal/PHP) y SERV_MED_XLS_ATENCION (Excel).
--
-- Por que: el cargador de Excel valida contra su propia lista de 26 causas (en codigo) y el portal
-- contra esta tabla. Antes de decidir cual es el catalogo oficial hay que ver que tiene la tabla y
-- que textos se usan de verdad en cada fuente.
--
-- Correr con:  @C:\miguel\portalsalud-personal\docs\script-prod\causas-14-catalogos.sql
-- ============================================================================================
SET LINESIZE 200 PAGESIZE 200
SET DEFINE OFF
COLUMN NOMBRE FORMAT A45
COLUMN CAUSA FORMAT A60
COLUMN NOMBRE_USUARIO FORMAT A25
SPOOL C:\miguel\causas-catalogos-salida.txt

PROMPT === 1) Catalogo del portal (SERV_MED_CAT_CAUSA_CONSULTA), activas e inactivas ===
SELECT REG_ID, NOMBRE, ACTIVO, TO_CHAR(FECHA_ALTA, 'YYYY-MM-DD') ALTA,
       TO_CHAR(FECHA_BAJA, 'YYYY-MM-DD') BAJA, NOMBRE_USUARIO
  FROM SERV_MED_CAT_CAUSA_CONSULTA
 ORDER BY ACTIVO DESC, NOMBRE;

PROMPT === 2) Textos de CAUSA usados en consultas del portal (TBL_SERV_CONSULTA_MEDICA) ===
PROMPT     EN_CATALOGO = S si el texto coincide con una causa del catalogo (sin importar mayusculas)
SELECT TRIM(c.CAUSA) CAUSA, COUNT(*) consultas,
       CASE WHEN EXISTS (SELECT 1 FROM SERV_MED_CAT_CAUSA_CONSULTA k
                          WHERE UPPER(TRIM(k.NOMBRE)) = UPPER(TRIM(c.CAUSA))) THEN 'S' ELSE 'N' END EN_CATALOGO
  FROM TBL_SERV_CONSULTA_MEDICA c
 GROUP BY TRIM(c.CAUSA)
 ORDER BY 2 DESC;

PROMPT === 3) Textos de CAUSA cargados del Excel por NSS (SERV_MED_XLS_ATENCION) ===
SELECT CAUSA, COUNT(*) atenciones
  FROM SERV_MED_XLS_ATENCION
 WHERE ESTADO_REGISTRO = 'VALIDO'
 GROUP BY CAUSA
 ORDER BY 2 DESC;

SPOOL OFF
