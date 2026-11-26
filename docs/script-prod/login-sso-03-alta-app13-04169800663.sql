-- BASE: SSO (SSO_PRODUCTIVA, la ADB). Conectado con el esquema que publica el ORDS «sso».
-- 26-nov-2026. 04169800663 tiene la app 27 (PORTAL SALUD del launcher) pero el login del portal (registro_app)
-- solo acepta la app 13. Se le agrega la 13 COPIANDO su renglon de la 27 (misma contrasena; no se escribe aqui).
-- Correr como SCRIPT (F5). No hace COMMIT solo.

PROMPT === 1) Antes: debe salir solo la app 27 ===
SELECT EMP_NSS, ID_APP, ESTADO_ACCESO FROM TBL_APPS_USUARIO WHERE TRIM(EMP_NSS) = '04169800663';

PROMPT === 2) Alta de la app 13 (solo si no la tiene) ===
INSERT INTO TBL_APPS_USUARIO (REG_ID, ID_APP, EMP_NSS, ESTADO_ACCESO, PASSWORD, ID_AREAS)
SELECT NULL, 13, EMP_NSS, ESTADO_ACCESO, PASSWORD, ID_AREAS
  FROM TBL_APPS_USUARIO
 WHERE TRIM(EMP_NSS) = '04169800663' AND ID_APP = 27
   AND NOT EXISTS (SELECT 1 FROM TBL_APPS_USUARIO WHERE TRIM(EMP_NSS) = '04169800663' AND ID_APP = 13);

PROMPT === 3) Despues: deben salir la 13 y la 27 ===
SELECT EMP_NSS, ID_APP, ESTADO_ACCESO, LENGTH(PASSWORD) AS LARGO_PASSWORD
  FROM TBL_APPS_USUARIO WHERE TRIM(EMP_NSS) = '04169800663' ORDER BY ID_APP;

PROMPT Si el paso 3 trae la 13 y la 27: COMMIT;   Si no: ROLLBACK;
