-- =============================================================================
-- Diagnostico: que menus ve un rol en "Expediente General" (submenu del NSS). SOLO LECTURA.
-- Base del portal (BIOMETRICO), NO ORDS. Cambiar el nombre del rol en el WHERE del bloque 2.
--
-- El submenu del NSS (fragments/nss-modules.html) dibuja TODOS los menus del rol excepto los
-- que tienen CODE = ANTIDOPING_SELECCION, CONSUMIBLES, CAUSAS_CONSULTA, EXAMEN_INICIAL_BANDEJA o
-- AYUDA_ADMIN. Si en el submenu aparecen Consumibles / Causas / Bandeja, su CODE en la base no
-- es exactamente ese (mayusculas, espacios) o vienen sin CODE. "Archivos" es el menu MNU_FILES.
-- =============================================================================
SET LINESIZE 200
COLUMN code  FORMAT A28
COLUMN title FORMAT A40
COLUMN rol   FORMAT A28

-- 1) Todos los menus con su CODE exacto (entre corchetes para ver espacios)
SELECT m.ID, '[' || m.CODE || ']' code, m.TITLE title, m.ORDER_NO
  FROM SERV_MED_MENU m
 ORDER BY m.ORDER_NO, m.ID;

-- 2) Menus asignados a cada rol (o a uno: descomentar el AND)
SELECT r.NAME rol, m.ID, '[' || m.CODE || ']' code, m.TITLE title
  FROM SERV_MED_MENU_ROLE mr
  JOIN SERV_MED_MENU m ON m.ID = mr.MENU_ID
  JOIN SERV_MED_SEC_ROLE r ON r.ID = mr.ROLE_ID
 -- WHERE r.NAME = 'Administrador'
 ORDER BY r.NAME, m.ORDER_NO, m.ID;
