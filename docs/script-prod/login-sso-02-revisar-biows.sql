-- BASE: la del ORDS de 10.249.249.3 (biows/ords/security), la que HOY valida el login del portal Salud en
--       produccion. NO es SSO_PRODUCTIVA ni BIOMETRICO del portal.
-- 26-nov-2026. SOLO LECTURA: no cambia nada.
-- Para que sirve: es la otra mitad de login-sso-01-revisar-sso.sql. Con las dos salidas se compara, usuario
-- por usuario, que todos los de la app 13 existan en SSO con la MISMA contrasena (se compara una huella
-- SHA-256, nunca la contrasena).
--
-- Correr como SCRIPT (F5). Pide la contrasena de 04169800663 SIN mostrarla y solo dice si coincide.
-- La salida queda en C:\miguel\login-sso-revision\02-biows.txt

SET VERIFY OFF
SET DEFINE ON
SET PAGESIZE 50000
SET LINESIZE 300
SET TRIMSPOOL ON
SET FEEDBACK ON
COLUMN HUELLA FORMAT A20

ACCEPT clave CHAR PROMPT 'Contrasena que se le dio a 04169800663: ' HIDE

SPOOL C:\miguel\login-sso-revision\02-biows.txt

PROMPT === 1) 04169800663 en TBL_APPS_USUARIO de esta base ===
SELECT EMP_NSS, ID_APP, ESTADO_ACCESO, LENGTH(PASSWORD) AS LARGO_PASSWORD,
       CASE WHEN UPPER(TRIM(PASSWORD)) = UPPER(TRIM('&clave')) THEN 'COINCIDE' ELSE 'NO COINCIDE' END AS PASSWORD_TECLEADA
  FROM TBL_APPS_USUARIO
 WHERE TRIM(TO_CHAR(EMP_NSS)) IN ('04169800663', '4169800663')
 ORDER BY ID_APP;

PROMPT === 2) 04169800663 en BIO_EMPLEADO de esta base (debe salir UN renglon con EMP_STATUS = 0) ===
SELECT EMP_NSS, EMP_STATUS, FECHA_BAJA, TIPOBAJA
  FROM BIO_EMPLEADO
 WHERE TRIM(TO_CHAR(EMP_NSS)) IN ('04169800663', '4169800663');

PROMPT === 3) Lista de la app 13 para comparar con SSO (NSS, si esta activo y huella de la contrasena) ===
SELECT TRIM(TO_CHAR(a.EMP_NSS)) AS NSS,
       (SELECT COUNT(*) FROM BIO_EMPLEADO e WHERE e.EMP_NSS = a.EMP_NSS AND e.EMP_STATUS = 0) AS ACTIVOS,
       SUBSTR(RAWTOHEX(STANDARD_HASH(UPPER(TRIM(a.PASSWORD)), 'SHA256')), 1, 16) AS HUELLA
  FROM TBL_APPS_USUARIO a
 WHERE a.ID_APP = 13
 ORDER BY 1;

SPOOL OFF
UNDEFINE clave
PROMPT Listo. Mandame C:\miguel\login-sso-revision\02-biows.txt
