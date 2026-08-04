-- =============================================================================
-- APP_FS_FILE — RECONCILIACIÓN de esquema (parche QA)
-- =============================================================================
-- Proyecto : Portal Salud — portalsalud-personal
-- Fecha    : 2026-07-21
-- Motivo   : ORA-00904 "FILE_TYPE": invalid identifier al listar adjuntos
--            (menús Nota Médica, Histórico E.M., E. Laboratorio).
--
-- CAUSA RAÍZ
-- ──────────
-- Existían DOS definiciones de APP_FS_FILE, ambas con guard idempotente
-- (SKIP si SQLCODE=-955):
--   · 00_init_oracle21c.sql        -> forma SIN NSS / FILE_TYPE / DATE_UPLOAD, usa CURRENT_VERSION
--   · app_domain/app-fs-file.sql   -> forma que espera el código Java (FsFileRepository)
-- En QA corrió primero 00_init, creó la tabla con la forma vieja, y el CREATE de
-- app-fs-file.sql se saltó por -955. Resultado: faltan columnas que el código usa.
--
-- Este script AGREGA solo las columnas faltantes (add-if-not-exists). Es seguro
-- re-ejecutarlo: si la columna ya existe, se salta (ORA-01430).
--
-- Columnas que el código Java necesita: NSS, FILE_TYPE, VERSION, DATE_UPLOAD.
-- =============================================================================

SET SERVEROUTPUT ON;

DECLARE
  PROCEDURE add_col_if_absent(p_col VARCHAR2, p_ddl VARCHAR2) IS
  BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE APP_FS_FILE ADD (' || p_ddl || ')';
    DBMS_OUTPUT.PUT_LINE('[OK]   columna agregada: ' || p_col);
  EXCEPTION WHEN OTHERS THEN
    IF SQLCODE = -1430 THEN                       -- column already exists
      DBMS_OUTPUT.PUT_LINE('[SKIP] ya existe: ' || p_col);
    ELSE RAISE; END IF;
  END;
BEGIN
  add_col_if_absent('NSS',         'NSS VARCHAR2(50)');
  add_col_if_absent('FILE_TYPE',   'FILE_TYPE VARCHAR2(120)');
  add_col_if_absent('DATE_UPLOAD', 'DATE_UPLOAD TIMESTAMP(6) DEFAULT SYSTIMESTAMP');
  -- El INSERT del repositorio setea VERSION=1. La tabla vieja usa CURRENT_VERSION;
  -- agregamos VERSION (no se renombra para no romper otros usos).
  add_col_if_absent('VERSION',     'VERSION NUMBER DEFAULT 1');

  -- CURRENT_VERSION es NOT NULL y el INSERT del repo NO la setea. Garantizamos su
  -- DEFAULT para que el INSERT no falle con ORA-01400 (cannot insert NULL).
  BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE APP_FS_FILE MODIFY (CURRENT_VERSION DEFAULT 1)';
    DBMS_OUTPUT.PUT_LINE('[OK]   CURRENT_VERSION DEFAULT 1 garantizado');
  EXCEPTION WHEN OTHERS THEN
    DBMS_OUTPUT.PUT_LINE('[WARN] no se pudo fijar DEFAULT en CURRENT_VERSION: ' || SQLERRM);
  END;
END;
/

-- Backfill mínimo para filas ya migradas/insertadas antes del parche
UPDATE APP_FS_FILE SET VERSION     = 1           WHERE VERSION     IS NULL;
UPDATE APP_FS_FILE SET DATE_UPLOAD = CREATED_AT  WHERE DATE_UPLOAD IS NULL;
COMMIT;

-- Índices que acompañan los patrones de acceso (add-if-not-exists via -955)
BEGIN EXECUTE IMMEDIATE 'CREATE INDEX IDX_APP_FS_FILE_TYPE     ON APP_FS_FILE (FILE_TYPE)';
  DBMS_OUTPUT.PUT_LINE('[OK]   IDX_APP_FS_FILE_TYPE');
EXCEPTION WHEN OTHERS THEN IF SQLCODE=-955 THEN DBMS_OUTPUT.PUT_LINE('[SKIP] IDX_APP_FS_FILE_TYPE'); ELSE RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE 'CREATE INDEX IDX_APP_FS_FILE_NSS_TYPE ON APP_FS_FILE (NSS, FILE_TYPE)';
  DBMS_OUTPUT.PUT_LINE('[OK]   IDX_APP_FS_FILE_NSS_TYPE');
EXCEPTION WHEN OTHERS THEN IF SQLCODE=-955 THEN DBMS_OUTPUT.PUT_LINE('[SKIP] IDX_APP_FS_FILE_NSS_TYPE'); ELSE RAISE; END IF; END;
/

-- Verificación final
PROMPT === Columnas de APP_FS_FILE tras el parche ===
SELECT column_name, data_type, data_length, nullable
FROM   user_tab_columns
WHERE  table_name = 'APP_FS_FILE'
ORDER  BY column_id;
