-- =============================================================================
-- Por que tarda >20 s el guardado del examen (POST Servcio/Medico). SOLO LECTURA.
-- Base: BIOMETRICO de PRODUCCION (las tablas del examen viven ahi). Lo corre el usuario.
--
-- El guardado ejecuta, por cada una de estas 49 tablas, un COUNT(*) WHERE NSS = ... y luego
-- un UPDATE o INSERT. Si una tabla no tiene indice por NSS, cada sentencia la recorre completa.
-- Este script mide: filas, si hay indice que empiece por NSS, y triggers.
-- Pasame la salida de los 3 bloques.
-- =============================================================================
SET LINESIZE 200
SET PAGESIZE 200
COLUMN tabla        FORMAT A32
COLUMN indice_nss   FORMAT A30
COLUMN trigger_name FORMAT A30
COLUMN evento       FORMAT A30

-- 1) Por tabla: filas (estadisticas), tamano y si tiene indice cuya PRIMERA columna es NSS.
--    Las que digan SIN_INDICE y tengan muchas filas son las sospechosas.
WITH t AS (
  SELECT column_value tabla FROM TABLE(sys.odcivarchar2list(
    'SERV_MED_ABDOMEN','SERV_MED_AGUDEZA_VISUAL','SERV_MED_ANT_GIN_OBSTETRICOS','SERV_MED_ANT_LABORALES',
    'SERV_MED_ANT_PATOLOGICOS','SERV_MED_ANT_PER_PATOLOGICOS','SERV_MED_BOCA','SERV_MED_CARDIOPATIAS',
    'SERV_MED_CARDIO_NO_PATO','SERV_MED_COLUMNA_VERTEBRAL','SERV_MED_COLUMNA_VERTEBRAL_AUX','SERV_MED_CRANEO',
    'SERV_MED_CUELLO','SERV_MED_DET_ANT_LABORALES','SERV_MED_DIAGNOSTICO','SERV_MED_DIENTES',
    'SERV_MED_DIGESTIVO','SERV_MED_ENDOCRINAS','SERV_MED_ENDOCRI_NO_PATO','SERV_MED_ESTUDIOS_REALIZADOS',
    'SERV_MED_EXPLORACION_FISICA','SERV_MED_EXTREMIDADES','SERV_MED_FILES','SERV_MED_GENERALES',
    'SERV_MED_GENITALES','SERV_MED_HEREDOFAMILIAR','SERV_MED_INMUNIZACIONES','SERV_MED_INT_APARATO_SIST',
    'SERV_MED_MENTALES','SERV_MED_MUS_ESQUELETICO','SERV_MED_NARIZ','SERV_MED_NEFROPATIA',
    'SERV_MED_NEUMOPATICA','SERV_MED_NEUROLOGIA','SERV_MED_OBESIDAD','SERV_MED_OFTALMOLOGICO',
    'SERV_MED_OIDOS','SERV_MED_OTRAS','SERV_MED_PADECI_ACTUAL','SERV_MED_PIEL','SERV_MED_PLAN_TERAPIA',
    'SERV_MED_RENAL','SERV_MED_RESULTADO_EXAMEN','SERV_MED_SIST_NERVIOSO','SERV_MED_TORAX',
    'SERV_MED_TOXICOLOGICO','SERV_MED_TOXI_NO_PATO','SERV_MED_URINARIO','SER_MED_REGISTRO_MEDICO'))
)
SELECT t.tabla,
       ut.num_rows                                            filas_estadistica,
       TO_CHAR(ut.last_analyzed, 'DD/MM/YYYY')                stats_del,
       ROUND(NVL(s.bytes, 0) / 1024 / 1024, 1)                mb,
       NVL((SELECT MIN(ic.index_name) FROM user_ind_columns ic
             WHERE ic.table_name = t.tabla AND ic.column_name = 'NSS' AND ic.column_position = 1),
           'SIN_INDICE')                                      indice_nss
  FROM t
  LEFT JOIN user_tables   ut ON ut.table_name = t.tabla
  LEFT JOIN user_segments s  ON s.segment_name = t.tabla AND s.segment_type = 'TABLE'
 ORDER BY CASE WHEN NVL((SELECT MIN(ic.index_name) FROM user_ind_columns ic
                          WHERE ic.table_name = t.tabla AND ic.column_name = 'NSS'
                            AND ic.column_position = 1), 'X') = 'X' THEN 0 ELSE 1 END,
          ut.num_rows DESC NULLS LAST;

-- 2) Triggers sobre esas tablas (un trigger lento se ejecuta en cada UPDATE/INSERT)
SELECT table_name tabla, trigger_name, triggering_event evento, status
  FROM user_triggers
 WHERE table_name LIKE 'SERV_MED_%' OR table_name = 'SER_MED_REGISTRO_MEDICO'
 ORDER BY table_name;

-- 3) Cuantas filas tiene de verdad el NSS de prueba en las tablas grandes (deberia ser 1 por tabla;
--    si hay cientos, el UPDATE WHERE NSS toca cientos de filas en cada guardado)
SELECT 'DET_ANT_LABORALES' tabla, COUNT(*) filas FROM SERV_MED_DET_ANT_LABORALES WHERE NSS = '30048315698'
UNION ALL SELECT 'ANT_LABORALES',     COUNT(*) FROM SERV_MED_ANT_LABORALES     WHERE NSS = '30048315698'
UNION ALL SELECT 'RESULTADO_EXAMEN',  COUNT(*) FROM SERV_MED_RESULTADO_EXAMEN  WHERE NSS = '30048315698'
UNION ALL SELECT 'DIAGNOSTICO',       COUNT(*) FROM SERV_MED_DIAGNOSTICO       WHERE NSS = '30048315698'
UNION ALL SELECT 'DIENTES',           COUNT(*) FROM SERV_MED_DIENTES           WHERE NSS = '30048315698'
UNION ALL SELECT 'FILES',             COUNT(*) FROM SERV_MED_FILES             WHERE NSS = '30048315698';
