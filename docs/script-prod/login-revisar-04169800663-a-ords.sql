-- BASE: ORDS (la del WS security/login/registro_app). Solo lectura.
-- Usuario que no puede entrar: NSS 04169800663. NO se escribe la contrasena aqui.
--
-- Lo que exige el login (docs/contextoWS.txt, registro_app, id_app = 13):
--   1) un renglon en TBL_APPS_USUARIO con EMP_NSS = el NSS tecleado, ID_APP = 13 y la contrasena
--      (sin importar mayusculas ni espacios);
--   2) y luego EXACTAMENTE UN renglon en BIO_EMPLEADO con EMP_STATUS = 0 (activo).
--      Si tiene 0 renglones en 0 (baja = 1) o mas de uno, el procedimiento truena y el login falla.

-- 1) ¿Esta dado de alta para Servicio Medico? (ojo con el cero inicial: se busca con y sin el)
SELECT EMP_NSS, ID_APP, LENGTH(EMP_NSS) largo,
       LENGTH(PASSWORD) largo_password,
       CASE WHEN PASSWORD <> TRIM(PASSWORD) THEN 'TRAE ESPACIOS' END espacios
  FROM TBL_APPS_USUARIO
 WHERE TRIM(EMP_NSS) IN ('04169800663', '4169800663');

-- 2) ¿Esta activo en el biometrico? (debe salir UN solo renglon con EMP_STATUS = 0)
SELECT EMP_NSS, EMP_NOMBRE, EMP_APELLIDO, EMP_APELLIDO2, EMP_STATUS, FECHA_BAJA, TIPOBAJA
  FROM BIO_EMPLEADO
 WHERE TRIM(TO_CHAR(EMP_NSS)) IN ('04169800663', '4169800663');

-- 3) Rol en el gateway de roles (lo usa el menu cuando los permisos vienen de ORDS)
SELECT * FROM TBL_APP_ROL_USUARIO
 WHERE TRIM(TO_CHAR(ID_USUARIO)) IN ('04169800663', '4169800663') AND ID_APP = 13;
