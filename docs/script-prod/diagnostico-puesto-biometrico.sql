-- ============================================================================================
-- ¿Por que el impreso del examen sale con «Puesto» vacio? - 17-nov-2026
--
-- Base: BIOMETRICO (PDBPRD), conectado como BIOMETRICO. Tablas del biometrico. NO va a ORDS.
-- SOLO LECTURA: no inserta, no borra, no cambia nada.
--
-- El impreso toma el puesto del WS; si no viene, de la solicitud de OCR; y desde el 17-nov, del biometrico
-- con esta misma union (FichaBiometricoRepository.puesto). Si la consulta 2 sale vacia, el NSS no tiene
-- puesto capturado en el biometrico y el impreso tiene que quedar en blanco (no se inventa).
--
-- Correr con:  @C:\miguel\portalsalud-personal\docs\script-prod\diagnostico-puesto-biometrico.sql
-- ============================================================================================
SET PAGESIZE 100
SET LINESIZE 200
-- El NSS se pide al correr (no se versionan NSS reales)
ACCEPT NSS CHAR PROMPT 'NSS a revisar: '

PROMPT === 1) Datos laborales del NSS (ID_PUESTO) ===
SELECT c.EMP_NSS, c.ID_PUESTO
  FROM BIO_DATOS_LABORALES_EMPLEADOS c
 WHERE c.EMP_NSS = '&NSS';

PROMPT === 2) Lo que lee el portal (misma consulta que FichaBiometricoRepository.puesto) ===
SELECT MAX(TRIM(pu.PUESTO_NOMBRE)) PUESTO
  FROM BIO_DATOS_LABORALES_EMPLEADOS c
  JOIN BIOMETRICO_PUESTO pu ON pu.PUESTO_ID = c.ID_PUESTO
 WHERE c.EMP_NSS = '&NSS';

PROMPT === 3) Lo que dice la vista del portal ===
SELECT NSS, PUESTO, CUENTA, PREDIO FROM SERV_MED_V_EMPLEADO WHERE NSS = '&NSS';

PROMPT === 4) Cuantos empleados tienen puesto en el biometrico (para saber que tanto ayuda) ===
SELECT COUNT(*) EMPLEADOS, COUNT(PUESTO) CON_PUESTO FROM SERV_MED_V_EMPLEADO;
