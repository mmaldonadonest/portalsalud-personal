-- BASE: la del ORDS 10.249.249.3 (biows/ords/security). NO es SSO_PRODUCTIVA ni BIOMETRICO del portal.
-- Alta de 04169800663 en Servicio Medico (app 13) para que entre por el formulario de health.onestcloud.mx.
-- Hace lo mismo que el WS security/login/registra_rol_usuario (pantalla de gestion del PHP viejo).
-- Correr como SCRIPT (F5 en SQL Developer): pide la contrasena y no la guarda en el archivo.

SET VERIFY OFF
ACCEPT clave CHAR PROMPT 'Contrasena para 04169800663 (app 13): ' HIDE

PROMPT === 1) Antes: no debe existir para la app 13 ===
SELECT EMP_NSS, ID_APP FROM TBL_APPS_USUARIO WHERE TRIM(TO_CHAR(EMP_NSS)) IN ('04169800663', '4169800663');

PROMPT === 2) Debe estar activo: UN renglon con EMP_STATUS = 0 (si no, el login falla aunque tenga contrasena) ===
SELECT EMP_NSS, EMP_STATUS, FECHA_BAJA, TIPOBAJA FROM BIO_EMPLEADO WHERE TRIM(TO_CHAR(EMP_NSS)) IN ('04169800663', '4169800663');

PROMPT === 3) Alta (solo si no existe; mismos valores que pone el WS) ===
INSERT INTO TBL_APPS_USUARIO (REG_ID, ID_APP, EMP_NSS, ESTADO_ACCESO, PASSWORD, ID_AREAS)
SELECT NULL, 13, '04169800663', 0, '&clave', 1 FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM TBL_APPS_USUARIO WHERE TRIM(TO_CHAR(EMP_NSS)) = '04169800663' AND ID_APP = 13);

PROMPT === 4) Despues: debe salir 1 renglon de la app 13 (sin mostrar la contrasena) ===
SELECT EMP_NSS, ID_APP, ESTADO_ACCESO, LENGTH(PASSWORD) AS LARGO
  FROM TBL_APPS_USUARIO WHERE TRIM(TO_CHAR(EMP_NSS)) = '04169800663' AND ID_APP = 13;

PROMPT Si el paso 4 sale bien: COMMIT;   Si no: ROLLBACK;
UNDEFINE clave
