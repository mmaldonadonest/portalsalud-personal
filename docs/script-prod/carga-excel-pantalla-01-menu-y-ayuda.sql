-- =====================================================================================
-- BASE DEL PORTAL (JDBC, SERV_MED_* dentro de BIOMETRICO), NO ORDS. La corre el usuario.
-- 2-nov-2026: Carga de Excel por pantalla (docs/plan-carga-excel-pantalla.md).
--
-- 1. Da de alta el menu CARGA_EXCEL. Despues se asigna en Administracion > Roles > Menus a
--    MEDICO_ANALISTA y a los roles de Servicio Medico. Sin el menu asignado, el rol no ve
--    «Carga de datos» en el menu lateral y recibe 403 en /carga-excel y /api/carga-excel.
-- 2. Articulo de ayuda de la pantalla (el tutorial guiado ya va en el WAR).
--
-- No crea tablas: la pantalla escribe en las SERV_MED_XLS_* que ya existen (mismas que la
-- consola) y guarda el original en SERV_MED_FS_FILE (FILE_TYPE = 'carga-excel').
-- Idempotente (MERGE por CODE / CLAVE).
-- =====================================================================================
MERGE INTO SERV_MED_MENU t USING (SELECT 'CARGA_EXCEL' code, 'Carga de Excel' title, 'ri-file-excel-2-line' icon, 18 order_no FROM dual) s
  ON (t.CODE = s.code) WHEN NOT MATCHED THEN INSERT (CODE, TITLE, ICON, ORDER_NO) VALUES (s.code, s.title, s.icon, s.order_no);

MERGE INTO SERV_MED_AYUDA_ARTICULO t
USING (SELECT 'carga-excel' clave FROM dual) s ON (t.CLAVE = s.clave)
WHEN NOT MATCHED THEN INSERT (CLAVE, MODULO, TITULO, PREGUNTA, PALABRAS_CLAVE, CUERPO, ENLACE, ORDEN, CREATED_BY, UPDATED_BY)
VALUES ('carga-excel', 'CARGA_EXCEL', 'Cargar un Excel de Servicio Médico',
 '¿Cómo cargo un Excel de atenciones, exámenes, incapacidades u otro layout?',
 'excel, carga, cargar, layout, atenciones, examen, incapacidades, accidentes, antidoping, maternidad, consumibles, errores, nss, correcciones',
 TO_CLOB('**Carga de datos → Carga de Excel** recibe los 10 layouts por NSS (Atenciones, exámenes, incapacidades, accidentes, antidoping, maternidad y consumibles). El layout se reconoce por los encabezados: el orden de las columnas no importa y las columnas de más se ignoran (la pantalla las avisa).

1. Sube el **.xlsx** y, si hace falta, el Excel de **correcciones de NSS**.
2. **Validar** revisa catálogos y NSS contra el biométrico **sin escribir nada**. Con archivos grandes tarda un par de minutos.
3. Revisa el resultado: cuántos renglones entran, el **Excel de errores** (renglón, columna y motivo) y lo que se **sustituye**: lo que llega de un predio para un mes reemplaza lo ya cargado de ese predio y mes.
4. **Cargar en la base** escribe los renglones válidos. Queda en el historial con tu usuario y una copia del archivo original.

Si el mismo archivo ya se había cargado, no se vuelve a cargar; «Cargar de nuevo» lo fuerza.'),
 'carga-excel', 82, 'SCRIPT', 'SCRIPT');

COMMIT;

-- Verificacion: 1 renglon cada una
SELECT ID, CODE, TITLE, ORDER_NO, ACTIVE FROM SERV_MED_MENU WHERE CODE = 'CARGA_EXCEL';
SELECT CLAVE, MODULO, TITULO, ENLACE FROM SERV_MED_AYUDA_ARTICULO WHERE CLAVE = 'carga-excel';
