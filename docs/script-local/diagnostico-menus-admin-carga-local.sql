-- =====================================================================================
-- DIAGNOSTICO (solo lectura) - por que en LOCAL no salen los menus Administracion y Carga de datos
-- BASE: portal LOCAL (localhost:1521/PROYECTO_BASE_PDB, usuario USR_BOILERPLATE). NO es produccion.
--
-- Administracion: sale si el usuario tiene un rol cuyo CODE contiene "ADMIN" (SERV_MED_SEC_USER_ROLE).
-- Carga de datos: sale si alguno de sus roles tiene asignado el menu CARGA_EXCEL (SERV_MED_MENU_ROLE).
-- =====================================================================================

-- 1. Usuarios y sus roles (busca tu NSS o usuario)
SELECT u.ID, u.USERNAME, u.ACCOUNT_STATUS, u.ACTIVE, r.CODE rol, r.ACTIVE rol_activo
  FROM SERV_MED_SEC_USER u
  LEFT JOIN SERV_MED_SEC_USER_ROLE ur ON ur.USER_ID = u.ID
  LEFT JOIN SERV_MED_SEC_ROLE r       ON r.ID = ur.ROLE_ID
 ORDER BY u.USERNAME, r.CODE;

-- 2. Existe el menu CARGA_EXCEL en local? (se dio de alta en prod el 2-nov con
--    docs/script-prod/carga-excel-pantalla-01-menu-y-ayuda.sql)
SELECT ID, CODE, TITLE, ACTIVE FROM SERV_MED_MENU WHERE CODE = 'CARGA_EXCEL';

-- 3. A que roles esta asignado CARGA_EXCEL
SELECT r.CODE rol, m.CODE menu
  FROM SERV_MED_MENU_ROLE mr
  JOIN SERV_MED_MENU m     ON m.ID = mr.MENU_ID
  JOIN SERV_MED_SEC_ROLE r ON r.ID = mr.ROLE_ID
 WHERE m.CODE = 'CARGA_EXCEL';
