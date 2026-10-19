-- ============================================================================================
-- Edad: de donde se puede sacar (SOLO LECTURA, no cambia nada) - 14-oct-2026
--
-- Base: BIOMETRICO (PDBPRD). Tablas del biometrico (BIO_EMPLEADO), del portal
-- (SERV_MED_RESULTADO_EXAMEN_HIST, SERV_MED_EXAMEN_SOLICITUD) y vista SERV_MED_V_EXAMEN.
--
-- Por que: la vista calcula la EDAD de los examenes del portal con BIO_EMPLEADO.EMP_FECHAN, y en
-- la prueba local la fecha de nacimiento llego VACIA para 4 de 4 NSS (el buscador muestra la edad
-- porque la calcula del RFC, como el PHP). Si EMP_FECHAN viene vacia casi siempre, los examenes
-- futuros quedan sin edad. Respaldos posibles: RFC (AAMMDD, fecha exacta) y NSS (digitos 5-6 =
-- anio de nacimiento, solo el anio).
--
-- Correr con:  @C:\miguel\portalsalud-personal\docs\script-prod\edad-14-revision-fuentes.sql
-- ============================================================================================
SET LINESIZE 200 PAGESIZE 100
-- El patron del RFC lleva "&": sin esto SQL Developer lo toma como variable y cancela la consulta
SET DEFINE OFF
SPOOL C:\miguel\edad-revision-salida.txt

PROMPT === 1) BIO_EMPLEADO: cuantos tienen fecha de nacimiento, RFC con fecha y NSS de 11 digitos ===
SELECT COUNT(*)                                                           total,
       COUNT(EMP_FECHAN)                                                  con_fechan,
       COUNT(CASE WHEN REGEXP_LIKE(TRIM(EMP_RFC), '^[A-Z&Ñ]{4}[0-9]{6}') THEN 1 END) con_rfc_fecha,
       COUNT(CASE WHEN REGEXP_LIKE(TRIM(EMP_NSS), '^[0-9]{11}$') THEN 1 END)        con_nss_11
  FROM BIO_EMPLEADO;

PROMPT === 2) Los 4 NSS de la prueba: fecha guardada vs RFC vs NSS ===
SELECT EMP_NSS, EMP_FECHAN, EMP_RFC, SUBSTR(EMP_NSS, 5, 2) anio_en_nss
  FROM BIO_EMPLEADO
 WHERE EMP_NSS IN ('30048315698', '02167598297', '07169966244', '17189842515');

PROMPT === 3) Cuando hay fecha y RFC: coincide el anio del RFC? y el del NSS? ===
SELECT COUNT(*) con_ambos,
       COUNT(CASE WHEN SUBSTR(TRIM(EMP_RFC), 5, 2) = TO_CHAR(EMP_FECHAN, 'YY') THEN 1 END) rfc_coincide,
       COUNT(CASE WHEN SUBSTR(EMP_NSS, 5, 2)        = TO_CHAR(EMP_FECHAN, 'YY') THEN 1 END) nss_coincide
  FROM BIO_EMPLEADO
 WHERE EMP_FECHAN IS NOT NULL
   AND REGEXP_LIKE(TRIM(EMP_RFC), '^[A-Z&Ñ]{4}[0-9]{6}')
   AND REGEXP_LIKE(TRIM(EMP_NSS), '^[0-9]{11}$');

PROMPT === 4) Examenes del portal en la vista: cuantos salen sin edad ===
SELECT FUENTE, COUNT(*) total, COUNT(EDAD) con_edad
  FROM SERV_MED_V_EXAMEN
 GROUP BY FUENTE;

PROMPT === 5) Solicitudes de OCR (candidatos): traen fecha de nacimiento? ===
SELECT COUNT(*) total, COUNT(FECHA_NACIMIENTO) con_fecha
  FROM SERV_MED_EXAMEN_SOLICITUD;

SPOOL OFF
