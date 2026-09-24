-- =====================================================================================
-- 03_menu_examenes.sql  -  BASE DEL PORTAL (JDBC: APP_*), NO ORDS.
-- Da de alta en el catalogo de menus (SERV_MED_MENU) las 3 pantallas del grupo "Examenes" del
-- sidebar (Antidoping - Seleccion, Consumibles, Causas de consulta) para que se asignen
-- por rol en Administracion > Roles > Menus (SERV_MED_MENU_ROLE) igual que los modulos del
-- Expediente. Hasta ahora eran paginas sin restriccion de rol (17-sep-2026).
-- Idempotente (MERGE por CODE). Aplicar en cada ambiente (QA ONEWMS_QA, prod) con el
-- usuario del esquema del portal.
-- Despues de aplicarlo: asignar los 3 menus a los roles que deban verlos; los usuarios
-- sin el menu dejan de ver la opcion y reciben 403 en la ruta.
-- =====================================================================================
MERGE INTO SERV_MED_MENU t USING (SELECT 'ANTIDOPING_SELECCION' code, 'Antidoping - Seleccion' title, 'ri-shuffle-line' icon, 15 order_no FROM dual) s
  ON (t.CODE = s.code) WHEN NOT MATCHED THEN INSERT (CODE, TITLE, ICON, ORDER_NO) VALUES (s.code, s.title, s.icon, s.order_no);
MERGE INTO SERV_MED_MENU t USING (SELECT 'CONSUMIBLES' code, 'Consumibles' title, 'ri-file-list-3-line' icon, 16 order_no FROM dual) s
  ON (t.CODE = s.code) WHEN NOT MATCHED THEN INSERT (CODE, TITLE, ICON, ORDER_NO) VALUES (s.code, s.title, s.icon, s.order_no);
MERGE INTO SERV_MED_MENU t USING (SELECT 'CAUSAS_CONSULTA' code, 'Causas de consulta' title, 'ri-list-settings-line' icon, 17 order_no FROM dual) s
  ON (t.CODE = s.code) WHEN NOT MATCHED THEN INSERT (CODE, TITLE, ICON, ORDER_NO) VALUES (s.code, s.title, s.icon, s.order_no);
COMMIT;

-- Verificacion
SELECT ID, CODE, TITLE, ORDER_NO, ACTIVE FROM SERV_MED_MENU WHERE CODE IN ('ANTIDOPING_SELECCION','CONSUMIBLES','CAUSAS_CONSULTA') ORDER BY ORDER_NO;
