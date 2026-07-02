-- =====================================================================
-- Seed: usuario 68958027838 con rol admin (tablas de seguridad del portal Java)
-- Ubicacion: src/main/resources/db/sql/seeds
--
-- Login resultante: usuario = 68958027838 | password = admin
--   (el PASSWORD_HASH es bcrypt de 'admin'; para 'Onest.2026' ver nota al final)
--
-- Idempotente (MERGE): se puede ejecutar varias veces sin duplicar.
-- Requiere que exista el esquema de 00_init_oracle21c.sql.
-- Nota: los permisos de MODULOS (validateModules) los resuelve el ORDS externo
--       (10.249.249.3); este seed solo habilita el LOGIN en el portal Java.
-- =====================================================================

-- (defensivo) asegurar rol admin; normalmente ya existe por 00_init_oracle21c.sql
MERGE INTO APP_SEC_ROLE t
USING (SELECT 'ROLE_ADMIN' AS CODE, 'Administrador' AS NAME FROM dual) s
ON (t.CODE = s.CODE)
WHEN NOT MATCHED THEN
  INSERT (CODE, NAME, DESCRIPTION, ACTIVE, CREATED_AT, CREATED_BY)
  VALUES (s.CODE, s.NAME, 'Rol con acceso total base', 'Y', SYSTIMESTAMP, USER);
/

-- Usuario
MERGE INTO APP_SEC_USER u
USING (
  SELECT '68958027838'                                                  AS USERNAME,
         '68958027838@onest.local'                                      AS EMAIL,
         '$2a$10$J6f.TpIHaI91bUGhf4uFfOIXbNE.a7qdkPW3yYwQVS6mi10rgVIQW' AS PASSWORD_HASH, -- password: admin
         'Administrador 68958027838'                                    AS DISPLAY_NAME
  FROM dual
) s
ON (u.USERNAME = s.USERNAME)
WHEN NOT MATCHED THEN
  INSERT (USERNAME, EMAIL, PASSWORD_HASH, DISPLAY_NAME, ACCOUNT_STATUS, ACTIVE, CREATED_AT, CREATED_BY)
  VALUES (s.USERNAME, s.EMAIL, s.PASSWORD_HASH, s.DISPLAY_NAME, 'ACTIVE', 'Y', SYSTIMESTAMP, USER);
/

-- Asignacion usuario -> rol admin
MERGE INTO APP_SEC_USER_ROLE ur
USING (
  SELECT u.ID AS USER_ID, r.ID AS ROLE_ID
  FROM APP_SEC_USER u
  JOIN APP_SEC_ROLE r ON r.CODE = 'ROLE_ADMIN'
  WHERE u.USERNAME = '68958027838'
) s
ON (ur.USER_ID = s.USER_ID AND ur.ROLE_ID = s.ROLE_ID)
WHEN NOT MATCHED THEN
  INSERT (USER_ID, ROLE_ID, CREATED_AT, CREATED_BY)
  VALUES (s.USER_ID, s.ROLE_ID, SYSTIMESTAMP, USER);
/

COMMIT;
/

-- Verificacion rapida
SELECT u.USERNAME, u.ACCOUNT_STATUS, u.ACTIVE, r.CODE AS ROLE_CODE
  FROM APP_SEC_USER u
  JOIN APP_SEC_USER_ROLE ur ON ur.USER_ID = u.ID
  JOIN APP_SEC_ROLE r       ON r.ID = ur.ROLE_ID
 WHERE u.USERNAME = '68958027838';
/

-- Alternativa de contrasena 'Onest.2026' (reemplazar el PASSWORD_HASH de arriba):
--   '$2a$10$j7tQL1w17wNMI8JE0KK78.sMKBNQDPHSbcZizi7QRCTrXuS4s/rRW'
