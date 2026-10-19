-- =============================================================================
-- CARGA DE EXCEL POR NSS -- layout 8 MATERNIDAD: SEMANA y MES DE GESTACION (15-oct-2026)
--
-- BASE: la del PORTAL = BIOMETRICO de produccion. LO CORRE: el usuario.
-- La plantilla docs/paquete-servicio-medico/plantillas/8 MATERNIDAD.xlsx agrego dos columnas
-- (G = SEMANA GESTACION, H = MES GESTACION). Se capturan, no se calculan de la FUM.
--
-- QUE HACE:   agrega 2 columnas NUMBER(2) vacias a SERV_MED_XLS_MATERNIDAD.
-- QUE NO HACE: no toca los datos ya cargados (quedan con las columnas vacias), ni vistas, ni indices.
-- ORDEN: correrlo ANTES de cargar un archivo de maternidad con el cargador nuevo; sin estas
--        columnas el INSERT del cargador falla (ORA-00904).
-- Idempotente: si la columna ya existe la salta (-1430).
-- =============================================================================
SET SERVEROUTPUT ON

BEGIN EXECUTE IMMEDIATE 'ALTER TABLE SERV_MED_XLS_MATERNIDAD ADD (SEMANA_GESTACION NUMBER(2))';
EXCEPTION WHEN OTHERS THEN IF SQLCODE NOT IN (-1430) THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE 'ALTER TABLE SERV_MED_XLS_MATERNIDAD ADD (MES_GESTACION NUMBER(2))';
EXCEPTION WHEN OTHERS THEN IF SQLCODE NOT IN (-1430) THEN RAISE; END IF; END;
/
COMMENT ON COLUMN SERV_MED_XLS_MATERNIDAD.SEMANA_GESTACION IS 'SEMANA GESTACION capturada en el Excel (15-oct-2026)';
COMMENT ON COLUMN SERV_MED_XLS_MATERNIDAD.MES_GESTACION IS 'MES GESTACION capturado en el Excel (15-oct-2026)';

-- VERIFICACION (solo lectura). Esperado: 2 filas NUMBER(2), y la vista sigue VALID.
SELECT column_name, data_type, data_precision FROM user_tab_columns
 WHERE table_name = 'SERV_MED_XLS_MATERNIDAD' AND column_name IN ('SEMANA_GESTACION', 'MES_GESTACION');
SELECT object_name, status FROM user_objects WHERE object_name = 'SERV_MED_V_MATERNIDAD';

-- REVERSA (solo si hiciera falta):
-- ALTER TABLE SERV_MED_XLS_MATERNIDAD DROP (SEMANA_GESTACION, MES_GESTACION);
