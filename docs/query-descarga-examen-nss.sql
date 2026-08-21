-- =============================================================================
-- EXAMEN MÉDICO — descarga completa de un NSS (todas las secciones)
-- =============================================================================
-- Destino : instancia Oracle que respalda el WS ORDS real (security/Servcio/*),
--           misma instancia donde viven las ~48 tablas SERV_MED_* del examen.
-- Fecha   : 20 de agosto de 2026
--
-- USO: en SQL Developer, correr como script (F5). Pedirá el bind variable NSS
-- (o reemplaza :NSS por el NSS literal entre comillas, ej. '68958027838').
-- Devuelve UNA fila por sección, con SECCION (nombre legible, mismo orden que
-- la navegación del portal), TABLA (nombre real Oracle) y DATOS (JSON con
-- todas las columnas de esa tabla para ese NSS). Copiar/exportar el grid a
-- Excel/CSV da "el examen completo" en un solo archivo.
--
-- CÓMO SE ARMÓ: cada tabla viene confirmada contra el INSERT real en
-- docs/contextoWS.txt (PR_SERVICIO_MED_EXAMEN1/2) y contra el catálogo Java
-- (ExamenService.CATALOGO) — no se adivinó ningún nombre de tabla.
--
-- OJO — supuestos a verificar antes de usar en producción:
-- 1) JSON_OBJECT(t.*) requiere Oracle 12.2+. Si la instancia es más vieja,
--    reemplazar cada bloque por "SELECT * FROM <tabla> WHERE NSS = :NSS"
--    (46 SELECT sueltos en vez de un UNION ALL) — mismo listado de tablas.
-- 2) Casi todas las tablas guardan UNA sola fila por NSS (cada examen nuevo
--    sobreescribe al anterior) — el query no distingue "el examen de cuándo".
--    Excepciones que SÍ pueden traer varias filas por NSS: SERV_MED_DET_ANT_LABORALES
--    (lista de trabajos previos) y SERV_MED_RESULTADO_EXAMEN_HIST (historial de
--    dictamen por fecha, arranca vacío desde el 17 de agosto de 2026 — sin datos
--    retroactivos, ver docs/ords-examen-historial.sql).
-- 3) SERV_MED_RESULTADO_EXAMEN incluye FIRMA_DIGITAL — si es un string largo
--    (base64), el JSON de esa sección puede pesar bastante; es normal.
-- =============================================================================

WITH examen AS (
  -- ---- Antecedentes laborales ----
  SELECT 1 AS ORDEN, 'Antecedentes laborales (resumen)' AS SECCION, 'SERV_MED_ANT_LABORALES' AS TABLA,
         JSON_OBJECT(t.*) AS DATOS FROM SERV_MED_ANT_LABORALES t WHERE t.NSS = :NSS
  UNION ALL
  SELECT 2, 'Trabajos previos (detalle)', 'SERV_MED_DET_ANT_LABORALES',
         JSON_OBJECT(t.*) FROM SERV_MED_DET_ANT_LABORALES t WHERE t.NSS = :NSS

  -- ---- Heredofamiliares (10 booleanos + resumen) ----
  UNION ALL
  SELECT 10, 'Neurología', 'SERV_MED_NEUROLOGIA', JSON_OBJECT(t.*) FROM SERV_MED_NEUROLOGIA t WHERE t.NSS = :NSS
  UNION ALL
  SELECT 11, 'Cardiopatía', 'SERV_MED_CARDIOPATIAS', JSON_OBJECT(t.*) FROM SERV_MED_CARDIOPATIAS t WHERE t.NSS = :NSS
  UNION ALL
  SELECT 12, 'Neumopatía', 'SERV_MED_NEUMOPATICA', JSON_OBJECT(t.*) FROM SERV_MED_NEUMOPATICA t WHERE t.NSS = :NSS
  UNION ALL
  SELECT 13, 'Toxicológico (heredofamiliar)', 'SERV_MED_TOXICOLOGICO', JSON_OBJECT(t.*) FROM SERV_MED_TOXICOLOGICO t WHERE t.NSS = :NSS
  UNION ALL
  SELECT 14, 'Nefropatías', 'SERV_MED_NEFROPATIA', JSON_OBJECT(t.*) FROM SERV_MED_NEFROPATIA t WHERE t.NSS = :NSS
  UNION ALL
  SELECT 15, 'Endocrinas', 'SERV_MED_ENDOCRINAS', JSON_OBJECT(t.*) FROM SERV_MED_ENDOCRINAS t WHERE t.NSS = :NSS
  UNION ALL
  SELECT 16, 'Obesidad', 'SERV_MED_OBESIDAD', JSON_OBJECT(t.*) FROM SERV_MED_OBESIDAD t WHERE t.NSS = :NSS
  UNION ALL
  SELECT 17, 'Mentales', 'SERV_MED_MENTALES', JSON_OBJECT(t.*) FROM SERV_MED_MENTALES t WHERE t.NSS = :NSS
  UNION ALL
  SELECT 18, 'Generales', 'SERV_MED_GENERALES', JSON_OBJECT(t.*) FROM SERV_MED_GENERALES t WHERE t.NSS = :NSS
  UNION ALL
  SELECT 19, 'Otras', 'SERV_MED_OTRAS', JSON_OBJECT(t.*) FROM SERV_MED_OTRAS t WHERE t.NSS = :NSS
  UNION ALL
  SELECT 20, 'Heredofamiliares (resumen por categoría)', 'SERV_MED_HEREDOFAMILIAR', JSON_OBJECT(t.*) FROM SERV_MED_HEREDOFAMILIAR t WHERE t.NSS = :NSS

  -- ---- Antecedentes personales ----
  UNION ALL
  SELECT 30, 'APNP (no patológicos)', 'SERV_MED_ANT_PATOLOGICOS', JSON_OBJECT(t.*) FROM SERV_MED_ANT_PATOLOGICOS t WHERE t.NSS = :NSS
  UNION ALL
  SELECT 31, 'Inmunizaciones', 'SERV_MED_INMUNIZACIONES', JSON_OBJECT(t.*) FROM SERV_MED_INMUNIZACIONES t WHERE t.NSS = :NSS
  UNION ALL
  SELECT 32, 'Gineco-obstétricos (AGO)', 'SERV_MED_ANT_GIN_OBSTETRICOS', JSON_OBJECT(t.*) FROM SERV_MED_ANT_GIN_OBSTETRICOS t WHERE t.NSS = :NSS
  UNION ALL
  SELECT 33, 'APP: personales patológicos', 'SERV_MED_ANT_PER_PATOLOGICOS', JSON_OBJECT(t.*) FROM SERV_MED_ANT_PER_PATOLOGICOS t WHERE t.NSS = :NSS
  UNION ALL
  SELECT 34, 'APP: oftalmológico', 'SERV_MED_OFTALMOLOGICO', JSON_OBJECT(t.*) FROM SERV_MED_OFTALMOLOGICO t WHERE t.NSS = :NSS
  UNION ALL
  SELECT 35, 'APP: digestivo', 'SERV_MED_DIGESTIVO', JSON_OBJECT(t.*) FROM SERV_MED_DIGESTIVO t WHERE t.NSS = :NSS
  UNION ALL
  SELECT 36, 'APP: renal', 'SERV_MED_RENAL', JSON_OBJECT(t.*) FROM SERV_MED_RENAL t WHERE t.NSS = :NSS
  UNION ALL
  SELECT 37, 'APP: sistema nervioso', 'SERV_MED_SIST_NERVIOSO', JSON_OBJECT(t.*) FROM SERV_MED_SIST_NERVIOSO t WHERE t.NSS = :NSS
  UNION ALL
  SELECT 38, 'APP: músculo-esquelético', 'SERV_MED_MUS_ESQUELETICO', JSON_OBJECT(t.*) FROM SERV_MED_MUS_ESQUELETICO t WHERE t.NSS = :NSS
  UNION ALL
  SELECT 39, 'APP: cardiovascular', 'SERV_MED_CARDIO_NO_PATO', JSON_OBJECT(t.*) FROM SERV_MED_CARDIO_NO_PATO t WHERE t.NSS = :NSS
  UNION ALL
  SELECT 40, 'APP: toxicológico', 'SERV_MED_TOXI_NO_PATO', JSON_OBJECT(t.*) FROM SERV_MED_TOXI_NO_PATO t WHERE t.NSS = :NSS
  UNION ALL
  SELECT 41, 'APP: endocrino', 'SERV_MED_ENDOCRI_NO_PATO', JSON_OBJECT(t.*) FROM SERV_MED_ENDOCRI_NO_PATO t WHERE t.NSS = :NSS

  -- ---- Padecimiento e interrogatorio ----
  UNION ALL
  SELECT 50, 'Padecimiento actual', 'SERV_MED_PADECI_ACTUAL', JSON_OBJECT(t.*) FROM SERV_MED_PADECI_ACTUAL t WHERE t.NSS = :NSS
  UNION ALL
  SELECT 51, 'Interrogatorio por aparatos', 'SERV_MED_INT_APARATO_SIST', JSON_OBJECT(t.*) FROM SERV_MED_INT_APARATO_SIST t WHERE t.NSS = :NSS

  -- ---- Exploración física ----
  UNION ALL
  SELECT 60, 'Exploración física', 'SERV_MED_EXPLORACION_FISICA', JSON_OBJECT(t.*) FROM SERV_MED_EXPLORACION_FISICA t WHERE t.NSS = :NSS
  UNION ALL
  SELECT 61, 'Cráneo', 'SERV_MED_CRANEO', JSON_OBJECT(t.*) FROM SERV_MED_CRANEO t WHERE t.NSS = :NSS
  UNION ALL
  SELECT 62, 'Agudeza visual', 'SERV_MED_AGUDEZA_VISUAL', JSON_OBJECT(t.*) FROM SERV_MED_AGUDEZA_VISUAL t WHERE t.NSS = :NSS
  UNION ALL
  SELECT 63, 'Nariz', 'SERV_MED_NARIZ', JSON_OBJECT(t.*) FROM SERV_MED_NARIZ t WHERE t.NSS = :NSS
  UNION ALL
  SELECT 64, 'Columna vertebral', 'SERV_MED_COLUMNA_VERTEBRAL', JSON_OBJECT(t.*) FROM SERV_MED_COLUMNA_VERTEBRAL t WHERE t.NSS = :NSS
  UNION ALL
  SELECT 65, 'Boca', 'SERV_MED_BOCA', JSON_OBJECT(t.*) FROM SERV_MED_BOCA t WHERE t.NSS = :NSS
  UNION ALL
  SELECT 66, 'Oídos', 'SERV_MED_OIDOS', JSON_OBJECT(t.*) FROM SERV_MED_OIDOS t WHERE t.NSS = :NSS
  UNION ALL
  SELECT 67, 'Tórax', 'SERV_MED_TORAX', JSON_OBJECT(t.*) FROM SERV_MED_TORAX t WHERE t.NSS = :NSS
  UNION ALL
  SELECT 68, 'Columna (movilidad)', 'SERV_MED_COLUMNA_VERTEBRAL_AUX', JSON_OBJECT(t.*) FROM SERV_MED_COLUMNA_VERTEBRAL_AUX t WHERE t.NSS = :NSS
  UNION ALL
  SELECT 69, 'Abdomen', 'SERV_MED_ABDOMEN', JSON_OBJECT(t.*) FROM SERV_MED_ABDOMEN t WHERE t.NSS = :NSS
  UNION ALL
  SELECT 70, 'Genitales', 'SERV_MED_GENITALES', JSON_OBJECT(t.*) FROM SERV_MED_GENITALES t WHERE t.NSS = :NSS
  UNION ALL
  SELECT 71, 'Urinario', 'SERV_MED_URINARIO', JSON_OBJECT(t.*) FROM SERV_MED_URINARIO t WHERE t.NSS = :NSS
  UNION ALL
  SELECT 72, 'Extremidades', 'SERV_MED_EXTREMIDADES', JSON_OBJECT(t.*) FROM SERV_MED_EXTREMIDADES t WHERE t.NSS = :NSS
  UNION ALL
  SELECT 73, 'Piel', 'SERV_MED_PIEL', JSON_OBJECT(t.*) FROM SERV_MED_PIEL t WHERE t.NSS = :NSS
  UNION ALL
  SELECT 74, 'Cuello', 'SERV_MED_CUELLO', JSON_OBJECT(t.*) FROM SERV_MED_CUELLO t WHERE t.NSS = :NSS
  UNION ALL
  SELECT 75, 'Dientes', 'SERV_MED_DIENTES', JSON_OBJECT(t.*) FROM SERV_MED_DIENTES t WHERE t.NSS = :NSS

  -- ---- Cierre ----
  UNION ALL
  SELECT 80, 'Estudios realizados', 'SERV_MED_ESTUDIOS_REALIZADOS', JSON_OBJECT(t.*) FROM SERV_MED_ESTUDIOS_REALIZADOS t WHERE t.NSS = :NSS
  UNION ALL
  SELECT 81, 'Diagnóstico', 'SERV_MED_DIAGNOSTICO', JSON_OBJECT(t.*) FROM SERV_MED_DIAGNOSTICO t WHERE t.NSS = :NSS
  UNION ALL
  SELECT 82, 'Plan terapéutico', 'SERV_MED_PLAN_TERAPIA', JSON_OBJECT(t.*) FROM SERV_MED_PLAN_TERAPIA t WHERE t.NSS = :NSS
  UNION ALL
  SELECT 83, 'Resultado del examen (dictamen)', 'SERV_MED_RESULTADO_EXAMEN', JSON_OBJECT(t.*) FROM SERV_MED_RESULTADO_EXAMEN t WHERE t.NSS = :NSS

  -- ---- Historial de dictamen (agregado 17-ago-2026, puede traer varias filas) ----
  UNION ALL
  SELECT 90, 'Historial de dictamen (por fecha)', 'SERV_MED_RESULTADO_EXAMEN_HIST', JSON_OBJECT(t.*) FROM SERV_MED_RESULTADO_EXAMEN_HIST t WHERE t.NSS = :NSS
)
SELECT ORDEN, SECCION, TABLA, DATOS
FROM examen
WHERE DATOS IS NOT NULL          -- solo secciones con datos capturados para este NSS
ORDER BY ORDEN;
