-- =====================================================================================
-- BASE DEL PORTAL (JDBC, SERV_MED_* dentro de BIOMETRICO), NO ORDS. La corre el usuario.
-- 25-oct-2026: la Ayuda pasa al header fijo y se asigna POR ROL, igual que el grupo Examenes.
-- Da de alta el menu AYUDA en el catalogo; despues se asigna a los roles que deban verla en
-- Administracion > Roles > Menus. Sin el menu asignado, el rol no ve el boton «Ayuda» y
-- recibe 403 en /ayuda y /api/ayuda. Idempotente (MERGE por CODE).
-- =====================================================================================
MERGE INTO SERV_MED_MENU t USING (SELECT 'AYUDA' code, 'Ayuda' title, 'ri-question-line' icon, 94 order_no FROM dual) s
  ON (t.CODE = s.code) WHEN NOT MATCHED THEN INSERT (CODE, TITLE, ICON, ORDER_NO) VALUES (s.code, s.title, s.icon, s.order_no);
COMMIT;

-- Verificacion: 1 renglon, ACTIVE = 1
SELECT ID, CODE, TITLE, ORDER_NO, ACTIVE FROM SERV_MED_MENU WHERE CODE = 'AYUDA';
