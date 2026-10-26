-- =====================================================================================
-- BASE DEL PORTAL (JDBC, SERV_MED_* dentro de BIOMETRICO), NO ORDS. La corre el usuario.
-- 25-oct-2026: tutoriales guiados para todos los temas de la ayuda. Liga cada articulo a su
-- tutorial (ENLACE con ?tutorial=id): asi el articulo muestra el boton «Muestrame como».
-- Correr DESPUES de ayuda.sql y portal-ayuda-formatos-tutoriales.sql (si se re-corre ayuda.sql,
-- volver a correr este). Solo cambia ENLACE; no toca el texto de los articulos.
-- =====================================================================================
UPDATE SERV_MED_AYUDA_ARTICULO SET ENLACE = 'home?tutorial=portal-basico',                       UPDATED_AT = SYSTIMESTAMP, UPDATED_BY = 'SCRIPT' WHERE CLAVE IN ('entrar-portal', 'menu-por-rol', 'campanita', 'ayuda-como-usar');
UPDATE SERV_MED_AYUDA_ARTICULO SET ENLACE = 'home?tutorial=dashboard',                           UPDATED_AT = SYSTIMESTAMP, UPDATED_BY = 'SCRIPT' WHERE CLAVE = 'dashboard';
UPDATE SERV_MED_AYUDA_ARTICULO SET ENLACE = 'nss?tutorial=consulta',                             UPDATED_AT = SYSTIMESTAMP, UPDATED_BY = 'SCRIPT' WHERE CLAVE IN ('diagnostico-icd', 'firma-paciente');
UPDATE SERV_MED_AYUDA_ARTICULO SET ENLACE = 'nss?tutorial=consulta-historial',                   UPDATED_AT = SYSTIMESTAMP, UPDATED_BY = 'SCRIPT' WHERE CLAVE = 'consulta-historial';
UPDATE SERV_MED_AYUDA_ARTICULO SET ENLACE = 'nss?tutorial=incapacidad',                          UPDATED_AT = SYSTIMESTAMP, UPDATED_BY = 'SCRIPT' WHERE CLAVE = 'incapacidad-alta';
UPDATE SERV_MED_AYUDA_ARTICULO SET ENLACE = 'nss?tutorial=incapacidad-lista',                    UPDATED_AT = SYSTIMESTAMP, UPDATED_BY = 'SCRIPT' WHERE CLAVE = 'incapacidad-lista';
UPDATE SERV_MED_AYUDA_ARTICULO SET ENLACE = 'nss?tutorial=archivos',                             UPDATED_AT = SYSTIMESTAMP, UPDATED_BY = 'SCRIPT' WHERE CLAVE = 'archivos-pdf';
UPDATE SERV_MED_AYUDA_ARTICULO SET ENLACE = 'nss?tutorial=examen-inicial',                       UPDATED_AT = SYSTIMESTAMP, UPDATED_BY = 'SCRIPT' WHERE CLAVE IN ('examen-inicial-que-es', 'examen-inicial-captura', 'examen-inicial-finalizar');
UPDATE SERV_MED_AYUDA_ARTICULO SET ENLACE = 'examen-inicial/bandeja?tutorial=bandeja',           UPDATED_AT = SYSTIMESTAMP, UPDATED_BY = 'SCRIPT' WHERE CLAVE = 'examen-inicial-bandeja';
UPDATE SERV_MED_AYUDA_ARTICULO SET ENLACE = 'nss?tutorial=restricciones',                        UPDATED_AT = SYSTIMESTAMP, UPDATED_BY = 'SCRIPT' WHERE CLAVE = 'restricciones-medicas';
UPDATE SERV_MED_AYUDA_ARTICULO SET ENLACE = 'nss?tutorial=pretest',                              UPDATED_AT = SYSTIMESTAMP, UPDATED_BY = 'SCRIPT' WHERE CLAVE = 'pretest';
UPDATE SERV_MED_AYUDA_ARTICULO SET ENLACE = 'nss?tutorial=accidentes',                           UPDATED_AT = SYSTIMESTAMP, UPDATED_BY = 'SCRIPT' WHERE CLAVE = 'accidentes';
UPDATE SERV_MED_AYUDA_ARTICULO SET ENLACE = 'nss?tutorial=maternidad',                           UPDATED_AT = SYSTIMESTAMP, UPDATED_BY = 'SCRIPT' WHERE CLAVE = 'maternidad';
UPDATE SERV_MED_AYUDA_ARTICULO SET ENLACE = 'antidoping-seleccion?tutorial=antidoping-seleccion', UPDATED_AT = SYSTIMESTAMP, UPDATED_BY = 'SCRIPT' WHERE CLAVE = 'antidoping-seleccion';
UPDATE SERV_MED_AYUDA_ARTICULO SET ENLACE = 'consumibles?tutorial=consumibles',                  UPDATED_AT = SYSTIMESTAMP, UPDATED_BY = 'SCRIPT' WHERE CLAVE = 'consumibles';
UPDATE SERV_MED_AYUDA_ARTICULO SET ENLACE = 'causas-consulta?tutorial=causas-consulta',          UPDATED_AT = SYSTIMESTAMP, UPDATED_BY = 'SCRIPT' WHERE CLAVE = 'causas-consulta';
UPDATE SERV_MED_AYUDA_ARTICULO SET ENLACE = 'admin/roles?tutorial=admin-roles',                  UPDATED_AT = SYSTIMESTAMP, UPDATED_BY = 'SCRIPT' WHERE CLAVE = 'admin-roles-menus';
UPDATE SERV_MED_AYUDA_ARTICULO SET ENLACE = 'admin/firmas?tutorial=admin-firmas',                UPDATED_AT = SYSTIMESTAMP, UPDATED_BY = 'SCRIPT' WHERE CLAVE = 'firmas-administracion';
COMMIT;

-- Verificacion: deben salir 35 articulos con tutorial (11 que ya lo traian + 24 de este script)
SELECT CLAVE, MODULO, ENLACE FROM SERV_MED_AYUDA_ARTICULO WHERE ENLACE LIKE '%tutorial=%' ORDER BY MODULO, CLAVE;
