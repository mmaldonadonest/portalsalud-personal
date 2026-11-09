-- =============================================================================
-- Estado civil: deducir que significa cada codigo numerico (0,1,2,3,5,6,X) del biometrico.
-- BASE: la del portal (BIOMETRICO). NO ORDS. SOLO LECTURA.
--
-- Los activos traen codigos (vienen de SAP); el texto (soltero, casado...) es de bajas viejas.
-- No hay catalogo en el esquema. Se cruza el codigo de BIO_EMPLEADO contra el TEXTO que la
-- misma persona trae en otras fuentes: la solicitud de examen (OCR) y el Excel de nuevo ingreso.
-- Si cada codigo cae casi siempre en un mismo texto, ese es su significado.
-- Pasame el resultado de las 2 consultas.
-- =============================================================================

-- 1) Codigo del biometrico vs texto de la solicitud de examen (OCR)
SELECT TRIM(b.EMP_ESTADO_CIVIL)      AS codigo,
       UPPER(TRIM(s.ESTADO_CIVIL))   AS texto_solicitud,
       COUNT(DISTINCT b.EMP_NSS)     AS personas
  FROM BIO_EMPLEADO b
  JOIN SERV_MED_EXAMEN_SOLICITUD s ON s.NSS = b.EMP_NSS
 WHERE TRIM(b.EMP_ESTADO_CIVIL) IN ('0','1','2','3','4','5','6','X')
   AND TRIM(s.ESTADO_CIVIL) IS NOT NULL
 GROUP BY TRIM(b.EMP_ESTADO_CIVIL), UPPER(TRIM(s.ESTADO_CIVIL))
 ORDER BY 1, 3 DESC;

-- 2) Codigo del biometrico vs texto del Excel de nuevo ingreso
SELECT TRIM(b.EMP_ESTADO_CIVIL)      AS codigo,
       UPPER(TRIM(x.ESTADO_CIVIL))   AS texto_excel,
       COUNT(DISTINCT b.EMP_NSS)     AS personas
  FROM BIO_EMPLEADO b
  JOIN SERV_MED_XLS_EXAMEN_ING x ON x.NSS = b.EMP_NSS
 WHERE TRIM(b.EMP_ESTADO_CIVIL) IN ('0','1','2','3','4','5','6','X')
   AND TRIM(x.ESTADO_CIVIL) IS NOT NULL
 GROUP BY TRIM(b.EMP_ESTADO_CIVIL), UPPER(TRIM(x.ESTADO_CIVIL))
 ORDER BY 1, 3 DESC;
