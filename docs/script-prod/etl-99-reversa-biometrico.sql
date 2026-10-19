-- ============================================================================
-- ETL histórico MySQL -> BIOMETRICO · REVERSA
--
-- BASE: la del PORTAL (JDBC) = BIOMETRICO. NO es ORDS.
--
-- ESTE SCRIPT BORRA. Léelo completo antes de correrlo.
--
-- Borra EXACTAMENTE lo que insertó el ETL y nada más:
--   · SERV_MED_FS_FILE  ->  BUSINESS_KEY LIKE 'legacy-%'
--     El ETL siempre escribe 'legacy-<id origen>'. El portal nunca usa
--     ese prefijo (FsFileRepository genera otras claves de negocio).
--   · SERV_MED_TAG      ->  SOURCE_ID IS NOT NULL
--     El ETL siempre llena SOURCE_ID con el id de MariaDB. El portal
--     escribe en esta misma tabla en runtime (contactos de emergencia,
--     diagnósticos secundarios, documentos de examen) pero NUNCA pone
--     SOURCE_ID. Verificado en el código el 29-sep-2026.
--
-- OJO: no usar MIGRATED_AT como criterio. Tiene DEFAULT SYSTIMESTAMP,
-- así que las filas que escribe el portal en runtime también lo traen.
-- El único discriminador confiable es SOURCE_ID.
--
-- NO borra los binarios del filesystem. Eso se hace aparte, por carpeta
-- de fecha bajo <files.root>/yyyy/MM/dd/.
-- ============================================================================
SET SERVEROUTPUT ON
SET LINESIZE 200

-- ----------------------------------------------------------------------
-- Paso 1: SIMULACRO. Déjalo en 'N' y corre el script: no borra nada,
--         solo dice cuánto borraría. Cambia a 'S' únicamente cuando el
--         conteo del simulacro sea el que esperas.
-- ----------------------------------------------------------------------
DECLARE
  v_forzar   CONSTANT VARCHAR2(1) := 'N';   -- <=== 'N' simula · 'S' BORRA

  v_files_etl     NUMBER;
  v_files_portal  NUMBER;
  v_tags_etl      NUMBER;
  v_tags_portal   NUMBER;
  -- Segundo marcador, independiente: los dos runners del ETL escriben
  -- CREATED_BY = 'ETL_LEGACY' (FileEtlRunner:33, TagEtlRunner:24).
  -- Debe coincidir con el conteo por BUSINESS_KEY / SOURCE_ID. Si no
  -- coincide, algo no es lo que creemos y NO hay que borrar.
  v_files_marca   NUMBER;
  v_tags_marca    NUMBER;
BEGIN
  SELECT COUNT(CASE WHEN BUSINESS_KEY LIKE 'legacy-%' THEN 1 END),
         COUNT(CASE WHEN BUSINESS_KEY NOT LIKE 'legacy-%' THEN 1 END),
         COUNT(CASE WHEN CREATED_BY = 'ETL_LEGACY' THEN 1 END)
    INTO v_files_etl, v_files_portal, v_files_marca
    FROM SERV_MED_FS_FILE;

  SELECT COUNT(CASE WHEN SOURCE_ID IS NOT NULL THEN 1 END),
         COUNT(CASE WHEN SOURCE_ID IS NULL THEN 1 END),
         COUNT(CASE WHEN CREATED_BY = 'ETL_LEGACY' THEN 1 END)
    INTO v_tags_etl, v_tags_portal, v_tags_marca
    FROM SERV_MED_TAG;

  DBMS_OUTPUT.PUT_LINE('===========================================================');
  DBMS_OUTPUT.PUT_LINE(' REVERSA DEL ETL - ' ||
                       CASE v_forzar WHEN 'S' THEN '*** BORRANDO DE VERDAD ***'
                                               ELSE 'SIMULACRO (no borra)' END);
  DBMS_OUTPUT.PUT_LINE('===========================================================');
  DBMS_OUTPUT.PUT_LINE('SERV_MED_FS_FILE');
  DBMS_OUTPUT.PUT_LINE('   se borrarian (ETL) : ' || v_files_etl);
  DBMS_OUTPUT.PUT_LINE('   se conservan       : ' || v_files_portal || '  <- capturadas en el portal');
  DBMS_OUTPUT.PUT_LINE('SERV_MED_TAG');
  DBMS_OUTPUT.PUT_LINE('   se borrarian (ETL) : ' || v_tags_etl);
  DBMS_OUTPUT.PUT_LINE('   se conservan       : ' || v_tags_portal || '  <- capturadas en el portal');
  DBMS_OUTPUT.PUT_LINE('-----------------------------------------------------------');
  DBMS_OUTPUT.PUT_LINE('Verificacion cruzada por CREATED_BY = ''ETL_LEGACY'':');
  DBMS_OUTPUT.PUT_LINE('   SERV_MED_FS_FILE : ' || v_files_marca ||
                       CASE WHEN v_files_marca = v_files_etl THEN '  (coincide)'
                            ELSE '  *** NO COINCIDE con ' || v_files_etl || ' ***' END);
  DBMS_OUTPUT.PUT_LINE('   SERV_MED_TAG     : ' || v_tags_marca ||
                       CASE WHEN v_tags_marca = v_tags_etl THEN '  (coincide)'
                            ELSE '  *** NO COINCIDE con ' || v_tags_etl || ' ***' END);
  DBMS_OUTPUT.PUT_LINE('-----------------------------------------------------------');

  -- Freno duro: si los dos marcadores independientes no dan el mismo numero,
  -- el criterio de borrado no es de fiar. Mejor abortar que borrar de mas.
  IF v_forzar = 'S'
     AND (v_files_marca <> v_files_etl OR v_tags_marca <> v_tags_etl) THEN
    DBMS_OUTPUT.PUT_LINE('ABORTADO: los dos marcadores no coinciden.');
    DBMS_OUTPUT.PUT_LINE('Revisar a mano antes de borrar nada. No se borro nada.');
    RETURN;
  END IF;

  IF v_forzar <> 'S' THEN
    DBMS_OUTPUT.PUT_LINE('Simulacro: no se borro nada.');
    DBMS_OUTPUT.PUT_LINE('Si los numeros son los correctos, pon v_forzar := ''S''.');
    RETURN;
  END IF;

  DELETE FROM SERV_MED_FS_FILE WHERE BUSINESS_KEY LIKE 'legacy-%';
  DBMS_OUTPUT.PUT_LINE('SERV_MED_FS_FILE borradas : ' || SQL%ROWCOUNT);

  DELETE FROM SERV_MED_TAG WHERE SOURCE_ID IS NOT NULL;
  DBMS_OUTPUT.PUT_LINE('SERV_MED_TAG     borradas : ' || SQL%ROWCOUNT);

  COMMIT;
  DBMS_OUTPUT.PUT_LINE('-----------------------------------------------------------');
  DBMS_OUTPUT.PUT_LINE('COMMIT hecho.');
  DBMS_OUTPUT.PUT_LINE('Falta borrar los binarios del filesystem, este script NO los toca.');
END;
/

-- ----------------------------------------------------------------------
-- Paso 2: comprobación (solo lectura). Después de un borrado real,
--         las dos columnas "quedan_del_etl" deben dar 0.
-- ----------------------------------------------------------------------
SELECT 'SERV_MED_FS_FILE' AS tabla,
       COUNT(CASE WHEN BUSINESS_KEY LIKE 'legacy-%' THEN 1 END)     AS quedan_del_etl,
       COUNT(CASE WHEN BUSINESS_KEY NOT LIKE 'legacy-%' THEN 1 END) AS del_portal
FROM   SERV_MED_FS_FILE
UNION ALL
SELECT 'SERV_MED_TAG',
       COUNT(CASE WHEN SOURCE_ID IS NOT NULL THEN 1 END),
       COUNT(CASE WHEN SOURCE_ID IS NULL THEN 1 END)
FROM   SERV_MED_TAG;
