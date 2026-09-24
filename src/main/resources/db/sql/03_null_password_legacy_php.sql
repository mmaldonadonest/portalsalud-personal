-- portal.auth.strategy paso a LEGACY_PHP: el password SIEMPRE se valida contra
-- ORDS ahora (ver LegacyPhpAuthenticationProvider), incluido el admin del panel
-- /admin. El hash local de 68958027838 ya no se usa para autenticar - se deja
-- en NULL (columna ya nullable) para que quede claro que no es fuente de verdad
-- y no pueda divergir en silencio del password real de ORDS.
UPDATE SERV_MED_SEC_USER
SET PASSWORD_HASH = NULL
WHERE USERNAME = '68958027838';
COMMIT;

-- Verificacion: debe salir NULL.
SELECT USERNAME, PASSWORD_HASH FROM SERV_MED_SEC_USER WHERE USERNAME = '68958027838';
