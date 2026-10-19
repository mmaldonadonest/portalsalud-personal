-- =============================================================================
-- Bandeja de examenes iniciales + asignacion (quien lo esta atendiendo).
-- Aplicar en la BASE DEL PORTAL (tablas SERV_MED_* dentro de BIOMETRICO), NO en ORDS.
-- Requiere integracion-ocr.sql aplicado antes (SERV_MED_EXAMEN_SOLICITUD).
--
-- 1. ATENDIDO_POR / ATENDIDO_AT: el primer medico que abre el examen inicial de una solicitud
--    queda asignado; otro usuario lo ve y puede "Tomarlo" explicitamente; finalizar solo lo
--    puede hacer quien lo tiene asignado (evita que dos personas capturen el mismo examen).
-- 2. Menu EXAMEN_INICIAL_BANDEJA en el grupo "Examenes" del sidebar: se asigna por rol en
--    Administracion > Menus por rol, como ANTIDOPING_SELECCION / CONSUMIBLES.
-- =============================================================================
SET SERVEROUTPUT ON;
SET DEFINE OFF;

BEGIN
  EXECUTE IMMEDIATE 'ALTER TABLE SERV_MED_EXAMEN_SOLICITUD ADD (ATENDIDO_POR VARCHAR2(100), ATENDIDO_AT TIMESTAMP(6))';
  DBMS_OUTPUT.PUT_LINE('ATENDIDO_POR / ATENDIDO_AT agregadas');
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -1430 THEN RAISE; ELSE DBMS_OUTPUT.PUT_LINE('ATENDIDO_POR / ATENDIDO_AT ya existian'); END IF; END;
/

MERGE INTO SERV_MED_MENU t USING (SELECT 'EXAMEN_INICIAL_BANDEJA' code, 'Exámenes iniciales (bandeja)' title, 'ri-inbox-line' icon, 18 order_no FROM dual) s
  ON (t.CODE = s.code) WHEN NOT MATCHED THEN INSERT (CODE, TITLE, ICON, ORDER_NO) VALUES (s.code, s.title, s.icon, s.order_no);
COMMIT;

-- Asignar de inicio a los mismos roles que ven EXAMEN_INICIAL (despues se ajusta en Administracion > Menus por rol)
INSERT INTO SERV_MED_MENU_ROLE (MENU_ID, ROLE_ID)
SELECT n.ID, mr.ROLE_ID
  FROM SERV_MED_MENU n, SERV_MED_MENU m
  JOIN SERV_MED_MENU_ROLE mr ON mr.MENU_ID = m.ID
 WHERE n.CODE = 'EXAMEN_INICIAL_BANDEJA' AND m.CODE = 'EXAMEN_INICIAL'
   AND NOT EXISTS (SELECT 1 FROM SERV_MED_MENU_ROLE x WHERE x.MENU_ID = n.ID AND x.ROLE_ID = mr.ROLE_ID);
COMMIT;

SELECT column_name, data_type FROM user_tab_columns WHERE table_name = 'SERV_MED_EXAMEN_SOLICITUD' AND column_name LIKE 'ATENDIDO%';
SELECT m.CODE, m.TITLE, COUNT(mr.ROLE_ID) roles FROM SERV_MED_MENU m LEFT JOIN SERV_MED_MENU_ROLE mr ON mr.MENU_ID = m.ID
 WHERE m.CODE = 'EXAMEN_INICIAL_BANDEJA' GROUP BY m.CODE, m.TITLE;
