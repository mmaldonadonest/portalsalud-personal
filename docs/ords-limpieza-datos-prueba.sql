-- =====================================================================================
-- ords-limpieza-datos-prueba.sql  —  ORDS / esquema legacy (NO base del portal)
-- Paso 2 de la limpieza de datos de prueba antes de producción. Correr DESPUÉS de revisar la
-- salida de ords-limpieza-datos-prueba-inventario.sql, con los MISMOS parámetros.
--
-- Reglas:
--   * No hay COMMIT automático: al final hay un bloque de verificación y el COMMIT está comentado.
--     Revisa los conteos, y si todo cuadra, COMMIT; si no, ROLLBACK.
--   * Sección A (tablas creadas por el portal): borra TODO salvo lo que se marque como real
--     (por default se conserva SERV_MED_CUENTA_PREDIO: es catálogo, aunque hoy sólo tenga 2
--     asignaciones de prueba — bórralas a mano si son las de 47 BRAND/AVANTE).
--   * Sección B (tablas del legacy con datos reales del PHP): SOLO por NSS de prueba. Nunca
--     por fecha sola. Si en el inventario apareció otro NSS de prueba, agrégalo a la lista.
--   * Sección C (BUG/PRUEBA/ONSYS_DEBUG): se vacían; son tablas de depuración de los handlers.
--   * NO se tocan: SERV_MED_PREDIO (17 predios), SERV_MED_CAT_CAUSA_CONSULTA (23 causas),
--     TBL_APPS_* (roles/menús/usuarios de las apps), biometrico_*, catálogos ICD.
--   * Antes de correr: respaldo del esquema (expdp) o al menos de estas tablas.
-- =====================================================================================

DEFINE nss_prueba = "('30048315698','90099119373','68958027838')";

SET SERVEROUTPUT ON

-- ---------- A) Tablas creadas por el portal Java ----------
DELETE FROM SERV_MED_ACCIDENTE_SEGUIMIENTO;     -- hijas primero (FK a SERV_MED_ACCIDENTE)
DELETE FROM SERV_MED_ACCIDENTE;
DELETE FROM SERV_MED_ANTIDOPING_RESULTADO;
DELETE FROM SERV_MED_ANTIDOPING_SELECCION;
DELETE FROM SERV_MED_ANTIDOPING_INVENTARIO;
DELETE FROM SERV_MED_MATERNIDAD_SEGUIMIENTO;
DELETE FROM SERV_MED_RESTRICCION_ASIGNADA;
DELETE FROM SERV_MED_RESULTADO_EXAMEN_HIST;
-- Cuenta→predio: sólo las asignaciones de prueba (ajusta o comenta si ya hay reales)
DELETE FROM SERV_MED_CUENTA_PREDIO WHERE UPPER(CUENTA_NOMBRE) IN ('47 BRAND','AVANTE');

-- ---------- B) Legacy: SOLO NSS de prueba ----------
-- Consultas e incapacidades capturadas desde el portal para los NSS de prueba
DELETE FROM TBL_SERV_CONSULTA_MEDICA     WHERE NSS IN &nss_prueba;
DELETE FROM TBL_SERV_INCAPACIDAD_MEDICA  WHERE NSS IN &nss_prueba;
DELETE FROM SER_MED_REGISTRO_MEDICO      WHERE NSS IN &nss_prueba;
DELETE FROM SERV_MED_FILES               WHERE NSS IN &nss_prueba;

-- Examen médico: una fila por NSS en cada sección (las 41 tablas que escriben
-- PR_SERVICIO_MED_EXAMEN1/2). Se borra el examen completo de los NSS de prueba.
DELETE FROM SERV_MED_ABDOMEN                WHERE NSS IN &nss_prueba;
DELETE FROM SERV_MED_AGUDEZA_VISUAL         WHERE NSS IN &nss_prueba;
DELETE FROM SERV_MED_ANT_LABORALES          WHERE NSS IN &nss_prueba;
DELETE FROM SERV_MED_DET_ANT_LABORALES      WHERE NSS IN &nss_prueba;
DELETE FROM SERV_MED_ANT_PATOLOGICOS        WHERE NSS IN &nss_prueba;
DELETE FROM SERV_MED_BOCA                   WHERE NSS IN &nss_prueba;
DELETE FROM SERV_MED_CARDIOPATIAS           WHERE NSS IN &nss_prueba;
DELETE FROM SERV_MED_COLUMNA_VERTEBRAL      WHERE NSS IN &nss_prueba;
DELETE FROM SERV_MED_COLUMNA_VERTEBRAL_AUX  WHERE NSS IN &nss_prueba;
DELETE FROM SERV_MED_CRANEO                 WHERE NSS IN &nss_prueba;
DELETE FROM SERV_MED_CUELLO                 WHERE NSS IN &nss_prueba;
DELETE FROM SERV_MED_DIAGNOSTICO            WHERE NSS IN &nss_prueba;
DELETE FROM SERV_MED_DIENTES                WHERE NSS IN &nss_prueba;
DELETE FROM SERV_MED_ENDOCRINAS             WHERE NSS IN &nss_prueba;
DELETE FROM SERV_MED_ENDOCRI_NO_PATO        WHERE NSS IN &nss_prueba;
DELETE FROM SERV_MED_ESTUDIOS_REALIZADOS    WHERE NSS IN &nss_prueba;
DELETE FROM SERV_MED_EXPLORACION_FISICA     WHERE NSS IN &nss_prueba;
DELETE FROM SERV_MED_EXTREMIDADES           WHERE NSS IN &nss_prueba;
DELETE FROM SERV_MED_GENERALES              WHERE NSS IN &nss_prueba;
DELETE FROM SERV_MED_GENITALES              WHERE NSS IN &nss_prueba;
DELETE FROM SERV_MED_HEREDOFAMILIAR         WHERE NSS IN &nss_prueba;
DELETE FROM SERV_MED_INMUNIZACIONES         WHERE NSS IN &nss_prueba;
DELETE FROM SERV_MED_INT_APARATO_SIST       WHERE NSS IN &nss_prueba;
DELETE FROM SERV_MED_MENTALES               WHERE NSS IN &nss_prueba;
DELETE FROM SERV_MED_MUS_ESQUELETICO        WHERE NSS IN &nss_prueba;
DELETE FROM SERV_MED_NARIZ                  WHERE NSS IN &nss_prueba;
DELETE FROM SERV_MED_NEFROPATIA             WHERE NSS IN &nss_prueba;
DELETE FROM SERV_MED_NEUMOPATICA            WHERE NSS IN &nss_prueba;
DELETE FROM SERV_MED_NEUROLOGIA             WHERE NSS IN &nss_prueba;
DELETE FROM SERV_MED_OBESIDAD               WHERE NSS IN &nss_prueba;
DELETE FROM SERV_MED_OIDOS                  WHERE NSS IN &nss_prueba;
DELETE FROM SERV_MED_OTRAS                  WHERE NSS IN &nss_prueba;
DELETE FROM SERV_MED_PADECI_ACTUAL          WHERE NSS IN &nss_prueba;
DELETE FROM SERV_MED_PIEL                   WHERE NSS IN &nss_prueba;
DELETE FROM SERV_MED_PLAN_TERAPIA           WHERE NSS IN &nss_prueba;
DELETE FROM SERV_MED_RENAL                  WHERE NSS IN &nss_prueba;
DELETE FROM SERV_MED_RESULTADO_EXAMEN       WHERE NSS IN &nss_prueba;
DELETE FROM SERV_MED_SIST_NERVIOSO          WHERE NSS IN &nss_prueba;
DELETE FROM SERV_MED_TORAX                  WHERE NSS IN &nss_prueba;
DELETE FROM SERV_MED_TOXICOLOGICO           WHERE NSS IN &nss_prueba;
DELETE FROM SERV_MED_TOXI_NO_PATO           WHERE NSS IN &nss_prueba;
DELETE FROM SERV_MED_URINARIO               WHERE NSS IN &nss_prueba;
-- Estas secciones no llevan NSS en su INSERT del handler (se ligan por otra clave); si el
-- inventario muestra filas de prueba, borrarlas a mano: SERV_MED_ANT_GIN_OBSTETRICOS,
-- SERV_MED_ANT_PER_PATOLOGICOS, SERV_MED_CARDIO_NO_PATO, SERV_MED_DIGESTIVO, SERV_MED_OFTALMOLOGICO.

-- ---------- C) Tablas de depuración de los handlers ----------
DELETE FROM BUG;
DELETE FROM PRUEBA;
DELETE FROM ONSYS_DEBUG;

-- ---------- Verificación (debe dar 0 en todo lo de prueba) ----------
SELECT 'consultas nss prueba' q, COUNT(*) n FROM TBL_SERV_CONSULTA_MEDICA WHERE NSS IN &nss_prueba
UNION ALL SELECT 'incapacidades nss prueba', COUNT(*) FROM TBL_SERV_INCAPACIDAD_MEDICA WHERE NSS IN &nss_prueba
UNION ALL SELECT 'examen generales nss prueba', COUNT(*) FROM SERV_MED_GENERALES WHERE NSS IN &nss_prueba
UNION ALL SELECT 'accidentes', COUNT(*) FROM SERV_MED_ACCIDENTE
UNION ALL SELECT 'antidoping resultado', COUNT(*) FROM SERV_MED_ANTIDOPING_RESULTADO
UNION ALL SELECT 'maternidad', COUNT(*) FROM SERV_MED_MATERNIDAD_SEGUIMIENTO
UNION ALL SELECT 'restricciones', COUNT(*) FROM SERV_MED_RESTRICCION_ASIGNADA
UNION ALL SELECT 'cuenta_predio', COUNT(*) FROM SERV_MED_CUENTA_PREDIO
UNION ALL SELECT 'bug', COUNT(*) FROM BUG
UNION ALL SELECT 'predios (debe seguir 17)', COUNT(*) FROM SERV_MED_PREDIO
UNION ALL SELECT 'causas (debe seguir 23)', COUNT(*) FROM SERV_MED_CAT_CAUSA_CONSULTA;

-- Si todo cuadra:
-- COMMIT;
-- Si algo no cuadra:
-- ROLLBACK;
