-- BASE: SSO (SSO_PRODUCTIVA, la ADB). Conectado con el esquema que publica el ORDS «sso» (dueño de
--       login/registro_app). NO es BIOMETRICO del portal ni el ORDS de 10.249.249.3.
-- 26-nov-2026. SOLO LECTURA: no cambia nada.
-- Para que sirve: antes de pasar el login del portal Salud al WS de SSO, saber si TODOS los que hoy
-- entran (app 13) van a poder entrar, y por que se rechaza a 04169800663.
--
-- Lo que exige el WS registro_app de SSO para dejar entrar:
--   1) TBL_APPS_USUARIO: EMP_NSS = usuario, ID_APP = 13, PASSWORD igual (sin importar mayusculas ni espacios);
--   2) BIO_EMPLEADO de ESTA base: exactamente UN renglon con EMP_STATUS = 0 (activo).
--
-- Correr como SCRIPT (F5). Pide la contrasena de 04169800663 SIN mostrarla y solo dice si coincide.
-- La salida queda en C:\miguel\login-sso-revision\01-sso.txt (sin contrasenas: solo una huella SHA-256).

SET VERIFY OFF
SET DEFINE ON
SET PAGESIZE 50000
SET LINESIZE 300
SET TRIMSPOOL ON
SET FEEDBACK ON
COLUMN HUELLA FORMAT A20

ACCEPT clave CHAR PROMPT 'Contrasena que se le dio a 04169800663: ' HIDE

SPOOL C:\miguel\login-sso-revision\01-sso.txt

PROMPT === 1) ¿El WS registro_app de SSO es la version corregida del 25-nov? (debe decir NUEVO) ===
SELECT m.NAME AS MODULO, t.URI_TEMPLATE, h.METHOD,
       CASE WHEN INSTR(h.SOURCE, 'get_clob_output') > 0 THEN 'VIEJO (da HTTP 555)' ELSE 'NUEVO' END AS VERSION
  FROM USER_ORDS_HANDLERS h
  JOIN USER_ORDS_TEMPLATES t ON t.ID = h.TEMPLATE_ID
  JOIN USER_ORDS_MODULES m ON m.ID = t.MODULE_ID
 WHERE t.URI_TEMPLATE = 'registro_app' AND h.METHOD = 'POST';

PROMPT === 2) 04169800663 en TBL_APPS_USUARIO (todas sus apps; la que importa es la 13) ===
SELECT EMP_NSS, ID_APP, ESTADO_ACCESO, LENGTH(EMP_NSS) AS LARGO_NSS, LENGTH(PASSWORD) AS LARGO_PASSWORD,
       CASE WHEN UPPER(TRIM(PASSWORD)) = UPPER(TRIM('&clave')) THEN 'COINCIDE' ELSE 'NO COINCIDE' END AS PASSWORD_TECLEADA
  FROM TBL_APPS_USUARIO
 WHERE TRIM(EMP_NSS) IN ('04169800663', '4169800663')
 ORDER BY ID_APP;

PROMPT === 3) 04169800663 en BIO_EMPLEADO de esta base (debe salir UN renglon con EMP_STATUS = 0) ===
SELECT EMP_NSS, EMP_STATUS, FECHA_BAJA, TIPOBAJA
  FROM BIO_EMPLEADO
 WHERE TRIM(TO_CHAR(EMP_NSS)) IN ('04169800663', '4169800663');

PROMPT === 4) Resumen de la app 13: cuantos entrarian y cuantos se rechazarian (y por que) ===
WITH u AS (
    SELECT TRIM(a.EMP_NSS) AS NSS,
           (SELECT COUNT(*) FROM BIO_EMPLEADO e WHERE e.EMP_NSS = a.EMP_NSS AND e.EMP_STATUS = 0) AS ACTIVOS,
           a.PASSWORD
      FROM TBL_APPS_USUARIO a
     WHERE a.ID_APP = 13)
SELECT CASE WHEN PASSWORD IS NULL OR TRIM(PASSWORD) IS NULL THEN 'RECHAZO: sin contrasena'
            WHEN ACTIVOS = 0 THEN 'RECHAZO: no esta activo en BIO_EMPLEADO'
            WHEN ACTIVOS > 1 THEN 'RECHAZO: activo mas de una vez'
            ELSE 'ENTRA' END AS RESULTADO,
       COUNT(*) AS USUARIOS
  FROM u
 GROUP BY CASE WHEN PASSWORD IS NULL OR TRIM(PASSWORD) IS NULL THEN 'RECHAZO: sin contrasena'
               WHEN ACTIVOS = 0 THEN 'RECHAZO: no esta activo en BIO_EMPLEADO'
               WHEN ACTIVOS > 1 THEN 'RECHAZO: activo mas de una vez'
               ELSE 'ENTRA' END
 ORDER BY 1;

PROMPT === 5) Lista para comparar con la otra base (NSS, si entraria y huella de la contrasena; NO la contrasena) ===
SELECT TRIM(a.EMP_NSS) AS NSS,
       (SELECT COUNT(*) FROM BIO_EMPLEADO e WHERE e.EMP_NSS = a.EMP_NSS AND e.EMP_STATUS = 0) AS ACTIVOS,
       SUBSTR(RAWTOHEX(STANDARD_HASH(UPPER(TRIM(a.PASSWORD)), 'SHA256')), 1, 16) AS HUELLA
  FROM TBL_APPS_USUARIO a
 WHERE a.ID_APP = 13
 ORDER BY 1;

SPOOL OFF
UNDEFINE clave
PROMPT Listo. Mandame C:\miguel\login-sso-revision\01-sso.txt
