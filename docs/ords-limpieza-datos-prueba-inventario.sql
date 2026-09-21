-- =====================================================================================
-- ords-limpieza-datos-prueba-inventario.sql  —  ORDS / esquema legacy (NO base del portal)
-- Paso 1 de la limpieza de datos de prueba antes de producción (plan-migracion-produccion.md, Fase 1).
--
-- SOLO SELECT. No borra nada. Muestra cuántas filas y cuáles caerían con los criterios que
-- luego usa ords-limpieza-datos-prueba.sql. Revisar la salida ANTES de correr la limpieza.
--
-- Criterios (ajustar en el bloque de parámetros):
--   A) Tablas creadas por el portal Java (SERV_MED_ACCIDENTE, _ANTIDOPING_*, _MATERNIDAD_*,
--      _RESTRICCION_ASIGNADA, _RESULTADO_EXAMEN_HIST, _CUENTA_PREDIO): TODAS sus filas son del
--      portal; se listan completas para decidir si se conservan (p.ej. cuenta→predio real).
--   B) Tablas del legacy que el portal escribe (consultas, incapacidades, examen por NSS,
--      archivos): sólo filas de los NSS de prueba, o creadas desde la fecha de arranque del
--      desarrollo. Aquí conviven con datos reales del PHP: por eso NUNCA se borra por fecha
--      sola, siempre NSS de prueba + fecha, y se revisa a mano.
--   C) Tablas de depuración de los handlers (BUG, PRUEBA, ONSYS_DEBUG): basura, se pueden vaciar.
--
-- NSS de prueba usados durante el desarrollo (verificado en scripts y docs, 21-sep-2026):
--   30048315698  Miguel Maldonado  (consulta 1068 del 11-sep, incapacidades 1661-1665 folios 0001/00002,
--                                   examen con firma, Pre-Test "TEST", antidoping, accidentes, maternidad...)
--   90099119373  Mariana Gutiérrez (ficha, 1 incapacidad "sin fecha")
--   68958027838  Mauricio Cerón    (usuario de pruebas del login; revisar si tiene registros)
--   16846510093  aparece en pruebas de reportes (reg 385 con typo 2923) — es dato real, NO borrar
-- =====================================================================================

-- ---------- Parámetros ----------
-- Fecha desde la que el portal Java empezó a escribir en QA (ajústala si empezó antes)
DEFINE fecha_ini = "DATE '2026-07-01'";
-- NSS de prueba (mantener la lista entre paréntesis)
DEFINE nss_prueba = "('30048315698','90099119373','68958027838')";

SET LINESIZE 200 PAGESIZE 200
COLUMN tabla FORMAT A38
COLUMN criterio FORMAT A26

-- ---------- A) Tablas creadas por el portal: resumen ----------
SELECT 'SERV_MED_ACCIDENTE'              tabla, 'todas'   criterio, COUNT(*) filas, MIN(FECHA_REGISTRO) desde, MAX(FECHA_REGISTRO) hasta FROM SERV_MED_ACCIDENTE
UNION ALL SELECT 'SERV_MED_ACCIDENTE_SEGUIMIENTO', 'todas', COUNT(*), MIN(FECHA_SEGUIMIENTO), MAX(FECHA_SEGUIMIENTO) FROM SERV_MED_ACCIDENTE_SEGUIMIENTO
UNION ALL SELECT 'SERV_MED_ANTIDOPING_RESULTADO',  'todas', COUNT(*), MIN(FECHA_REGISTRO), MAX(FECHA_REGISTRO) FROM SERV_MED_ANTIDOPING_RESULTADO
UNION ALL SELECT 'SERV_MED_ANTIDOPING_SELECCION',  'todas', COUNT(*), MIN(FECHA_SELECCION), MAX(FECHA_SELECCION) FROM SERV_MED_ANTIDOPING_SELECCION
UNION ALL SELECT 'SERV_MED_ANTIDOPING_INVENTARIO', 'todas', COUNT(*), MIN(FECHA_REGISTRO), MAX(FECHA_REGISTRO) FROM SERV_MED_ANTIDOPING_INVENTARIO
UNION ALL SELECT 'SERV_MED_MATERNIDAD_SEGUIMIENTO','todas', COUNT(*), MIN(FECHA_REGISTRO), MAX(FECHA_REGISTRO) FROM SERV_MED_MATERNIDAD_SEGUIMIENTO
UNION ALL SELECT 'SERV_MED_RESTRICCION_ASIGNADA',  'todas', COUNT(*), NULL, NULL FROM SERV_MED_RESTRICCION_ASIGNADA
UNION ALL SELECT 'SERV_MED_RESULTADO_EXAMEN_HIST', 'todas', COUNT(*), MIN(FECHA_REGISTRO), MAX(FECHA_REGISTRO) FROM SERV_MED_RESULTADO_EXAMEN_HIST
UNION ALL SELECT 'SERV_MED_CUENTA_PREDIO',         'todas', COUNT(*), MIN(FECHA_ASIGNACION), MAX(FECHA_ASIGNACION) FROM SERV_MED_CUENTA_PREDIO;

-- Detalle A (para decidir fila por fila; las de NSS de prueba son las candidatas seguras)
SELECT 'ACCIDENTE' t, REG_ID, NSS, FECHA_REGISTRO, ID_USUARIO FROM SERV_MED_ACCIDENTE ORDER BY REG_ID;
SELECT 'ANTIDOPING_RESULTADO' t, REG_ID, NSS, FECHA_REGISTRO, ID_USUARIO FROM SERV_MED_ANTIDOPING_RESULTADO ORDER BY REG_ID;
SELECT 'ANTIDOPING_SELECCION' t, REG_ID, NSS, FECHA_SELECCION, ID_USUARIO FROM SERV_MED_ANTIDOPING_SELECCION ORDER BY REG_ID;
SELECT 'ANTIDOPING_INVENTARIO' t, REG_ID, FECHA_REGISTRO FROM SERV_MED_ANTIDOPING_INVENTARIO ORDER BY REG_ID;
SELECT 'MATERNIDAD' t, REG_ID, NSS, FECHA_REGISTRO, ID_USUARIO FROM SERV_MED_MATERNIDAD_SEGUIMIENTO ORDER BY REG_ID;
SELECT 'RESTRICCION' t, REG_ID, NSS FROM SERV_MED_RESTRICCION_ASIGNADA ORDER BY REG_ID;
SELECT 'EXAMEN_HIST' t, REG_ID, NSS, FECHA_REGISTRO FROM SERV_MED_RESULTADO_EXAMEN_HIST ORDER BY REG_ID;
SELECT 'CUENTA_PREDIO' t, REG_ID, CUENTA_NOMBRE, PREDIO_ID, FECHA_ASIGNACION, ID_USUARIO FROM SERV_MED_CUENTA_PREDIO ORDER BY REG_ID;

-- ---------- B) Tablas del legacy escritas por el portal: NSS de prueba ----------
SELECT 'TBL_SERV_CONSULTA_MEDICA' tabla, 'nss prueba' criterio, COUNT(*) filas, MIN(FECHA) desde, MAX(FECHA) hasta
  FROM TBL_SERV_CONSULTA_MEDICA WHERE NSS IN &nss_prueba
UNION ALL SELECT 'TBL_SERV_CONSULTA_MEDICA', 'desde fecha_ini (todos)', COUNT(*), MIN(FECHA), MAX(FECHA)
  FROM TBL_SERV_CONSULTA_MEDICA WHERE FECHA >= &fecha_ini
UNION ALL SELECT 'TBL_SERV_INCAPACIDAD_MEDICA', 'nss prueba', COUNT(*), MIN(FECHA_REGISTRO), MAX(FECHA_REGISTRO)
  FROM TBL_SERV_INCAPACIDAD_MEDICA WHERE NSS IN &nss_prueba
UNION ALL SELECT 'TBL_SERV_INCAPACIDAD_MEDICA', 'desde fecha_ini (todos)', COUNT(*), MIN(FECHA_REGISTRO), MAX(FECHA_REGISTRO)
  FROM TBL_SERV_INCAPACIDAD_MEDICA WHERE FECHA_REGISTRO >= &fecha_ini
UNION ALL SELECT 'SERV_MED_FILES', 'nss prueba', COUNT(*), MIN(DATE_UPLOAD), MAX(DATE_UPLOAD)
  FROM SERV_MED_FILES WHERE NSS IN &nss_prueba
UNION ALL SELECT 'SER_MED_REGISTRO_MEDICO', 'nss prueba', COUNT(*), NULL, NULL
  FROM SER_MED_REGISTRO_MEDICO WHERE NSS IN &nss_prueba
UNION ALL SELECT 'SERV_MED_RESULTADO_EXAMEN', 'nss prueba', COUNT(*), NULL, NULL
  FROM SERV_MED_RESULTADO_EXAMEN WHERE NSS IN &nss_prueba;

-- Detalle B: consultas e incapacidades de los NSS de prueba (revisar fila por fila)
SELECT REG_ID, FECHA, NSS, TIPO_CONSULTA, SUBSTR(MOTIVO_CONSULTA,1,60) motivo FROM TBL_SERV_CONSULTA_MEDICA WHERE NSS IN &nss_prueba ORDER BY REG_ID;
SELECT REG_ID, FECHA_REGISTRO, NSS, FOLIO_INCAPACIDAD, RAMO, TIPO_INCAPACIDAD FROM TBL_SERV_INCAPACIDAD_MEDICA WHERE NSS IN &nss_prueba ORDER BY REG_ID;
-- "Desde fecha_ini" de TODOS los NSS: para detectar altas hechas con otros NSS durante las pruebas.
-- Si aquí aparecen NSS que no son de prueba, son datos reales del PHP: NO tocarlos.
SELECT NSS, COUNT(*) consultas, MIN(FECHA) desde, MAX(FECHA) hasta FROM TBL_SERV_CONSULTA_MEDICA WHERE FECHA >= &fecha_ini GROUP BY NSS ORDER BY 2 DESC;
SELECT NSS, COUNT(*) incapacidades, MIN(FECHA_REGISTRO) desde, MAX(FECHA_REGISTRO) hasta FROM TBL_SERV_INCAPACIDAD_MEDICA WHERE FECHA_REGISTRO >= &fecha_ini GROUP BY NSS ORDER BY 2 DESC;

-- Examen médico: una fila por NSS en cada sección (PR_SERVICIO_MED_EXAMEN1/2 hace insert/update por NSS).
-- Cuenta en cuántas secciones existe cada NSS de prueba.
SELECT nss, SUM(n) secciones_con_fila FROM (
  SELECT NSS nss, COUNT(*) n FROM SERV_MED_GENERALES WHERE NSS IN &nss_prueba GROUP BY NSS
  UNION ALL SELECT NSS, COUNT(*) FROM SERV_MED_EXPLORACION_FISICA WHERE NSS IN &nss_prueba GROUP BY NSS
  UNION ALL SELECT NSS, COUNT(*) FROM SERV_MED_DIAGNOSTICO WHERE NSS IN &nss_prueba GROUP BY NSS
  UNION ALL SELECT NSS, COUNT(*) FROM SERV_MED_RESULTADO_EXAMEN WHERE NSS IN &nss_prueba GROUP BY NSS
  UNION ALL SELECT NSS, COUNT(*) FROM SERV_MED_HEREDOFAMILIAR WHERE NSS IN &nss_prueba GROUP BY NSS
  UNION ALL SELECT NSS, COUNT(*) FROM SERV_MED_ANT_LABORALES WHERE NSS IN &nss_prueba GROUP BY NSS
) GROUP BY nss;

-- ---------- C) Tablas de depuración de los handlers ----------
SELECT 'BUG' tabla, COUNT(*) filas FROM BUG
UNION ALL SELECT 'PRUEBA', COUNT(*) FROM PRUEBA
UNION ALL SELECT 'ONSYS_DEBUG', COUNT(*) FROM ONSYS_DEBUG;

-- ---------- D) Catálogos sembrados por el portal (NO son basura, se listan para confirmar) ----------
SELECT 'SERV_MED_PREDIO' tabla, COUNT(*) filas FROM SERV_MED_PREDIO
UNION ALL SELECT 'SERV_MED_CAT_CAUSA_CONSULTA', COUNT(*) FROM SERV_MED_CAT_CAUSA_CONSULTA;
SELECT REG_ID, NOMBRE FROM SERV_MED_PREDIO ORDER BY REG_ID;
