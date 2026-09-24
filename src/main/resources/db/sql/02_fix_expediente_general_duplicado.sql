-- Expediente General ya se muestra como encabezado estatico del grupo
-- (fragments/menu.html:17, #nssModulesGroup). No tiene 'action' en
-- fragments/nss-modules.html (es un contenedor, no un modulo real), asi que
-- asignarlo tambien como item de submenu solo produce un duplicado visual
-- ("Expediente General" aparece dos veces en el sidebar).
--
-- Bajo ORDS esto nunca pasaba porque id_menu=1 (Expediente General) nunca
-- estaba en TBL_APPS_ROL_MENU para ningun rol real. Quitamos la asignacion
-- equivalente en el esquema local para que ambas fuentes se comporten igual.
DELETE FROM SERV_MED_MENU_ROLE
WHERE MENU_ID = (SELECT ID FROM SERV_MED_MENU WHERE CODE = 'EXPEDIENTE_GENERAL');
COMMIT;

-- Verificacion: debe regresar 0 filas.
SELECT COUNT(*) AS asignaciones_restantes
FROM SERV_MED_MENU_ROLE mr
JOIN SERV_MED_MENU m ON m.ID = mr.MENU_ID
WHERE m.CODE = 'EXPEDIENTE_GENERAL';
