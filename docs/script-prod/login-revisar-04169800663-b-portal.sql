-- BASE: portal (JDBC), esquema BIOMETRICO. Solo lectura.
-- Usuario que no puede entrar: NSS 04169800663.
-- Si el login de ORDS pasa, el portal le arma el menu con sus roles locales (portal.permissions.source=LOCAL).
-- Sin rol local entra pero no ve modulos.

-- 1) ¿Tiene usuario local y esta activo?
SELECT ID, USERNAME, EMAIL, DISPLAY_NAME, ACCOUNT_STATUS, ACTIVE, LAST_LOGIN_AT
  FROM SERV_MED_SEC_USER
 WHERE TRIM(USERNAME) IN ('04169800663', '4169800663');

-- 2) ¿Que roles tiene?
SELECT u.USERNAME, r.CODE, r.NAME, r.ACTIVE rol_activo
  FROM SERV_MED_SEC_USER u
  JOIN SERV_MED_SEC_USER_ROLE ur ON ur.USER_ID = u.ID
  JOIN SERV_MED_SEC_ROLE r ON r.ID = ur.ROLE_ID
 WHERE TRIM(u.USERNAME) IN ('04169800663', '4169800663');
