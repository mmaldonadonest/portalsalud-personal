-- =============================================================================
-- VISTAS UNIFICADAS: lo capturado en el PORTAL + lo cargado del EXCEL por NSS, en un solo lugar
--
-- BASE: BIOMETRICO de produccion (PDBPRD). Ahi viven las tablas vivas que escriben los WS ORDS,
--       las SERV_MED_XLS_* y el biometrico. Lo corre el usuario.
--
-- PARA QUE: que graficas, modulos del expediente y un WS nuevo lean LA MISMA fuente, en vez de
--       juntar en Java lo del portal con lo historico. Las graficas las leeran detras del
--       interruptor portal.historico.fuente=VISTA (se desarrolla en local; en el servidor sigue
--       BITACORA hasta que se decida activar).
--
-- REGLAS QUE APLICAN LAS VISTAS (acordadas 10-oct-2026):
--   - "Si el evento ya esta en el portal, manda el portal": un renglon del Excel NO sale si el
--     mismo evento existe en la tabla viva. Se evalua AL LEER, asi que si alguien captura despues
--     en el portal un evento que venia en el Excel, el del Excel deja de contarse solo.
--     Incapacidad: folio (internas: NSS + inicio). Accidente: NSS + fecha. Examen: NSS + fecha
--     +-1 dia. Atencion: NSS + mismo dia. (Las mismas llaves que usa el cargador.)
--   - Del Excel solo renglones ESTADO_REGISTRO = 'VALIDO'.
--   - Del portal, nombre/genero/edad/cuenta/puesto salen del biometrico por NSS (cuenta con
--     BIOMETRICO_CUENTA, igual que los WS _cta) y el predio del mapeo SERV_MED_CUENTA_PREDIO
--     (vigente = ultima asignacion). Sin mapeo queda NULL y la pantalla dice "Sin asignar".
--   - Columna FUENTE en todas: PORTAL | EXCEL | CARGA_VIEJA, para poder separar o auditar.
--
-- QUE NO HACE: no crea tablas, no modifica datos, no toca ningun objeto existente. Solo
--       CREATE OR REPLACE VIEW de 8 vistas (5 desde la v1, 3 desde la v4); se puede volver a correr
--       las veces que sea.
--       Reversa al final (comentada): DROP VIEW de las 8, no se pierde ningun dato.
--
-- ORDEN: correr TODO el archivo de una vez (F5 en SQL Developer). Pasame la salida completa.
--
-- HISTORIAL
--   v1 11-oct-2026  primera version (corrida en produccion, 5 vistas VALID).
--   v2 11-oct-2026  para que Atenciones lea todo de la vista (fase 2):
--                   - SERV_MED_V_EMPLEADO: la cuenta "X - BIOANTERIOR" hereda el predio de "X",
--                     igual que DashboardPredioFiltro en Java.
--                   - SERV_MED_V_ATENCION: + AREA_ACCIDENTE, AREA_INVOLUCRADA, ES_ACCIDENTE
--                     (columnas al final; lo que ya lee la v1 no cambia).
--                   Volver a correr completo: CREATE OR REPLACE, no toca datos.
--   v3 11-oct-2026  SERV_MED_V_ACCIDENTE: causa RT y estatus del portal con el texto del layout
--                   (LESION_ERGONOMICA -> LESION ERGONOMICA, BAJA_INCONCLUSO -> BAJA INCONCLUSO),
--                   para que en la grafica sumen en la misma barra. Equivalencia uno a uno.
--   v4 12-oct-2026  layouts nuevos (requiere excel-nss-06 y excel-nss-08 corridos):
--                   - SERV_MED_V_EXAMEN: + pos incapacidad del layout 4 (SERV_MED_XLS_EXAMEN_POS); la
--                     de la carga vieja se lee SOLO mientras ese layout este vacio.
--                   - NUEVA SERV_MED_V_ANTIDOPING: portal + layout 7 + carga vieja (mientras el layout
--                     este vacio).
--                   - NUEVA SERV_MED_V_MATERNIDAD (historial) y SERV_MED_V_MATERNIDAD_ACTUAL (un
--                     renglon por embarazo, el mas reciente): portal + layout 8 + carga vieja.
--                   Consumibles (layout 9) no lleva vista: no hay portal con que unirlo.
-- =============================================================================
SET SERVEROUTPUT ON
SET LINESIZE 220
SET PAGESIZE 300
COLUMN table_name  FORMAT A32
COLUMN column_name FORMAT A28
COLUMN data_type   FORMAT A14
COLUMN valor       FORMAT A40

-- ----------------------------------------------------------------------
-- PRE-CHECK 1 (solo lectura): tipos de las columnas de las tablas vivas que usan las vistas.
-- ----------------------------------------------------------------------
SELECT table_name, column_name, data_type, data_length
  FROM user_tab_columns
 WHERE (table_name, column_name) IN (
       ('BIO_EMPLEADO','EMP_NSS'), ('BIO_EMPLEADO','EMP_FECHAN'), ('BIO_EMPLEADO','EMP_SEXO'),
       ('TBL_SERV_CONSULTA_MEDICA','NSS'), ('TBL_SERV_CONSULTA_MEDICA','FECHA'),
       ('TBL_SERV_CONSULTA_MEDICA','TIPO_CONSULTA'), ('TBL_SERV_CONSULTA_MEDICA','CAUSA'),
       ('TBL_SERV_CONSULTA_MEDICA','DIAGNOSTICO'), ('TBL_SERV_CONSULTA_MEDICA','TRATAMIENTO'),
       ('TBL_SERV_CONSULTA_MEDICA','AREA_DE_ACCIDENTE'), ('TBL_SERV_CONSULTA_MEDICA','AREA_ANATOMICA_INVOLUCRADA'),
       ('TBL_SERV_INCAPACIDAD_MEDICA','NSS'), ('TBL_SERV_INCAPACIDAD_MEDICA','FECHA_INICIO'),
       ('TBL_SERV_INCAPACIDAD_MEDICA','FECHA_TERMINO'), ('TBL_SERV_INCAPACIDAD_MEDICA','DIAS_AUTORIZADOS'),
       ('TBL_SERV_INCAPACIDAD_MEDICA','SALARIO_INTEGRADO'), ('TBL_SERV_INCAPACIDAD_MEDICA','COSTO'),
       ('TBL_SERV_INCAPACIDAD_MEDICA','RUBRO'), ('TBL_SERV_INCAPACIDAD_MEDICA','RAMO'),
       ('TBL_SERV_INCAPACIDAD_MEDICA','TIPO_INCAPACIDAD'), ('TBL_SERV_INCAPACIDAD_MEDICA','ESTADO_DICTAMEN'))
 ORDER BY table_name, column_name;

-- ----------------------------------------------------------------------
-- PRE-CHECK 2 (solo lectura): que valores marcan el dictamen en el historial de examenes del
-- portal. La vista toma como marcado cualquier valor no vacio que no sea 0/N/NO/FALSE (mismo
-- criterio que el dashboard: "cuenta filas con valor no vacio"). Esta salida lo confirma.
-- ----------------------------------------------------------------------
SELECT 'APTO' columna, APTO valor, COUNT(*) renglones FROM SERV_MED_RESULTADO_EXAMEN_HIST GROUP BY APTO
UNION ALL SELECT 'NO_APTO', NO_APTO, COUNT(*) FROM SERV_MED_RESULTADO_EXAMEN_HIST GROUP BY NO_APTO
UNION ALL SELECT 'APTO_CONDICIONADO', APTO_CONDICIONADO, COUNT(*) FROM SERV_MED_RESULTADO_EXAMEN_HIST GROUP BY APTO_CONDICIONADO
UNION ALL SELECT 'APTO_RESTRINGIDO', APTO_RESTRINGIDO, COUNT(*) FROM SERV_MED_RESULTADO_EXAMEN_HIST GROUP BY APTO_RESTRINGIDO
 ORDER BY 1, 3 DESC;

-- ======================================================================
-- 1 de 5: SERV_MED_V_EMPLEADO -- datos del empleado por NSS (para lo capturado en el portal)
-- UN renglon por NSS (si BIO_EMPLEADO repite el NSS, se prefiere el que no es de prueba, 99),
-- para que el JOIN nunca duplique eventos. Cuenta y puesto: un NSS puede tener mas de un
-- renglon laboral (multipredio); se toma uno (MAX), igual que los WS _cta.
-- ======================================================================
CREATE OR REPLACE VIEW SERV_MED_V_EMPLEADO AS
SELECT e.NSS, e.NOMBRE, e.GENERO, e.FECHA_NAC,
       d.CUENTA, pu.PUESTO_NOMBRE PUESTO, pm.PREDIO
  FROM (SELECT b.EMP_NSS NSS,
               TRIM(REGEXP_REPLACE(b.EMP_NOMBRE || ' ' || b.EMP_APELLIDO || ' ' || b.EMP_APELLIDO2, ' +', ' ')) NOMBRE,
               CASE UPPER(TRIM(TO_CHAR(b.EMP_SEXO)))
                    WHEN '1' THEN 'MASC' WHEN 'H' THEN 'MASC' WHEN 'M' THEN 'MASC' WHEN 'HOMBRE' THEN 'MASC'
                    WHEN '2' THEN 'FEM'  WHEN 'F' THEN 'FEM'  WHEN 'MUJER' THEN 'FEM' END GENERO,
               b.EMP_FECHAN FECHA_NAC,
               ROW_NUMBER() OVER (PARTITION BY b.EMP_NSS
                                  ORDER BY CASE WHEN TO_CHAR(b.EMP_STATUS) = '99' THEN 1 ELSE 0 END) rn
          FROM BIO_EMPLEADO b) e
  LEFT JOIN (SELECT dl.EMP_NSS, MAX(cta.CUENTA_NOMBRE) CUENTA, MAX(dl.ID_PUESTO) ID_PUESTO
               FROM BIO_DATOS_LABORALES_EMPLEADOS dl
               LEFT JOIN BIOMETRICO_CUENTA cta ON cta.CUENTA_ID = dl.CUENTA_ID
              GROUP BY dl.EMP_NSS) d ON d.EMP_NSS = e.NSS
  LEFT JOIN BIOMETRICO_PUESTO pu ON pu.PUESTO_ID = d.ID_PUESTO
  LEFT JOIN (SELECT UPPER(TRIM(cp.CUENTA_NOMBRE)) CUENTA_U, p.NOMBRE PREDIO,
                    ROW_NUMBER() OVER (PARTITION BY UPPER(TRIM(cp.CUENTA_NOMBRE))
                                       ORDER BY cp.FECHA_ASIGNACION DESC, cp.REG_ID DESC) rn
               FROM SERV_MED_CUENTA_PREDIO cp
               JOIN SERV_MED_PREDIO p ON p.REG_ID = cp.PREDIO_ID) pm
         -- Igual que DashboardPredioFiltro: "X - BIOANTERIOR" (cuenta del biometrico anterior)
         -- hereda el predio de "X".
         ON pm.CUENTA_U = TRIM(REGEXP_REPLACE(UPPER(d.CUENTA), '\s*-\s*BIO\s*ANTERIOR\s*$', '')) AND pm.rn = 1
 WHERE e.rn = 1;

COMMENT ON TABLE SERV_MED_V_EMPLEADO IS 'Un renglon por NSS: biometrico + mapeo cuenta->predio vigente. Apoyo de las vistas unificadas. Solo lectura.';

-- ======================================================================
-- 2 de 5: SERV_MED_V_ATENCION -- consultas del portal + atenciones del Excel
-- RAMO: del portal es TIPO_CONSULTA tal cual; del Excel, el ramo del layout.
-- Textos largos recortados a 1000 (sirve igual si la columna viva es VARCHAR2 o CLOB).
-- ======================================================================
CREATE OR REPLACE VIEW SERV_MED_V_ATENCION AS
SELECT CAST('PORTAL' AS VARCHAR2(12))                              FUENTE,
       TO_CHAR(c.REG_ID)                                           ID_ORIGEN,
       CAST(c.NSS AS VARCHAR2(13))                                 NSS,
       TRUNC(c.FECHA)                                              FECHA,
       emp.PREDIO                                                  PREDIO,
       emp.CUENTA                                                  CUENTA,
       CAST(NULL AS VARCHAR2(120))                                 AREA,
       emp.NOMBRE                                                  NOMBRE,
       emp.GENERO                                                  GENERO,
       TRUNC(MONTHS_BETWEEN(TRUNC(c.FECHA), emp.FECHA_NAC) / 12)   EDAD,
       emp.PUESTO                                                  PUESTO,
       CAST(SUBSTR(c.TIPO_CONSULTA, 1, 60) AS VARCHAR2(60 CHAR))   RAMO,
       CAST(SUBSTR(c.CAUSA, 1, 200) AS VARCHAR2(200 CHAR))         CAUSA,
       CAST(NULL AS VARCHAR2(120))                                 LESION_ME,
       CAST(SUBSTR(c.DIAGNOSTICO, 1, 1000) AS VARCHAR2(1000 CHAR)) DIAGNOSTICO,
       CAST(SUBSTR(c.TRATAMIENTO, 1, 1000) AS VARCHAR2(1000 CHAR)) TRATAMIENTO,
       -- Lo que el portal captura y el layout no: area del accidente y si la consulta fue por
       -- accidente/emergencia (mismo criterio que ConsultaReporteDto.esAccidente).
       CAST(SUBSTR(c.AREA_DE_ACCIDENTE, 1, 200) AS VARCHAR2(200 CHAR)) AREA_ACCIDENTE,
       CAST(SUBSTR(c.AREA_ANATOMICA_INVOLUCRADA, 1, 200) AS VARCHAR2(200 CHAR)) AREA_INVOLUCRADA,
       CASE WHEN LOWER(c.TIPO_CONSULTA) LIKE '%accidente%' OR LOWER(c.TIPO_CONSULTA) LIKE '%emergencia%'
            THEN 1 ELSE 0 END                                      ES_ACCIDENTE
  FROM TBL_SERV_CONSULTA_MEDICA c
  LEFT JOIN SERV_MED_V_EMPLEADO emp ON emp.NSS = c.NSS
UNION ALL
SELECT 'EXCEL', TO_CHAR(x.ID), x.NSS, x.FECHA, x.PREDIO, x.CUENTA, x.AREA, x.NOMBRE, x.GENERO, x.EDAD,
       x.PUESTO, x.RAMO, x.CAUSA, x.LESION_ME, x.DIAGNOSTICO, x.TRATAMIENTO,
       NULL, NULL, 0
  FROM SERV_MED_XLS_ATENCION x
 WHERE x.ESTADO_REGISTRO = 'VALIDO'
   AND NOT EXISTS (SELECT 1 FROM TBL_SERV_CONSULTA_MEDICA c
                    WHERE c.NSS = x.NSS AND TRUNC(c.FECHA) = x.FECHA);

COMMENT ON TABLE SERV_MED_V_ATENCION IS 'Atenciones: portal (TBL_SERV_CONSULTA_MEDICA) + Excel por NSS (SERV_MED_XLS_ATENCION sin las que ya estan en el portal).';

-- ======================================================================
-- 3 de 5: SERV_MED_V_INCAPACIDAD -- incapacidades del portal + del Excel
-- FECHA = inicio del certificado (criterio de negocio del 11-sep-2026). FECHA_INICIO es texto
-- con dos formatos: misma conversion que el WS consulta_incapacidades_fecha_cta y el cargador;
-- sin fecha legible cae a la fecha de registro (no desaparece de los reportes).
-- DIAS/SDI/COSTO: si la columna viva es texto y no es numero limpio, queda NULL (no truena).
-- RUBRO normalizado a IMSS | INTERNA; RAMO a los codigos del layout (EG, MAT, RT1, RT2).
-- ======================================================================
CREATE OR REPLACE VIEW SERV_MED_V_INCAPACIDAD AS
SELECT CAST('PORTAL' AS VARCHAR2(12))                              FUENTE,
       TO_CHAR(i.REG_ID)                                           ID_ORIGEN,
       CAST(i.NSS AS VARCHAR2(13))                                 NSS,
       COALESCE(CASE WHEN REGEXP_LIKE(i.FECHA_INICIO, '^[0-9]{4}-[0-9]{2}-[0-9]{2}')
                     THEN TO_DATE(SUBSTR(i.FECHA_INICIO, 1, 10), 'YYYY-MM-DD')
                     WHEN REGEXP_LIKE(i.FECHA_INICIO, '^[0-9]{2}/[0-9]{2}/[0-9]{4}')
                     THEN TO_DATE(SUBSTR(i.FECHA_INICIO, 1, 10), 'DD/MM/YYYY') END,
                TRUNC(i.FECHA_REGISTRO))                           FECHA,
       emp.PREDIO                                                  PREDIO,
       emp.CUENTA                                                  CUENTA,
       CAST(NULL AS VARCHAR2(120))                                 AREA,
       emp.NOMBRE                                                  NOMBRE,
       emp.GENERO                                                  GENERO,
       TRUNC(MONTHS_BETWEEN(TRUNC(i.FECHA_REGISTRO), emp.FECHA_NAC) / 12) EDAD,
       emp.PUESTO                                                  PUESTO,
       CAST(SUBSTR(UPPER(REPLACE(i.FOLIO_INCAPACIDAD, ' ')), 1, 20) AS VARCHAR2(20)) FOLIO,
       CASE WHEN UPPER(i.RUBRO) LIKE '%INTERN%' THEN 'INTERNA'
            WHEN UPPER(i.RUBRO) LIKE '%IMSS%'   THEN 'IMSS' END    RUBRO,
       CASE WHEN UPPER(TRIM(i.RAMO)) IN ('EG', 'ENFERMEDAD GENERAL', 'ENFERMEDAD_GENERAL') THEN 'EG'
            WHEN UPPER(TRIM(i.RAMO)) IN ('MAT', 'MATERNIDAD') THEN 'MAT'
            WHEN UPPER(TRIM(i.RAMO)) IN ('RT1', 'RT2') THEN UPPER(TRIM(i.RAMO))
            ELSE CAST(SUBSTR(i.RAMO, 1, 40) AS VARCHAR2(40)) END   RAMO,
       CAST(SUBSTR(i.TIPO_INCAPACIDAD, 1, 20) AS VARCHAR2(20))     TIPO,
       CASE WHEN REGEXP_LIKE(i.FECHA_TERMINO, '^[0-9]{4}-[0-9]{2}-[0-9]{2}')
            THEN TO_DATE(SUBSTR(i.FECHA_TERMINO, 1, 10), 'YYYY-MM-DD')
            WHEN REGEXP_LIKE(i.FECHA_TERMINO, '^[0-9]{2}/[0-9]{2}/[0-9]{4}')
            THEN TO_DATE(SUBSTR(i.FECHA_TERMINO, 1, 10), 'DD/MM/YYYY') END FECHA_TERMINO,
       CASE WHEN REGEXP_LIKE(TRIM(TO_CHAR(i.DIAS_AUTORIZADOS)), '^[0-9]+$')
            THEN TO_NUMBER(TRIM(TO_CHAR(i.DIAS_AUTORIZADOS))) END   DIAS,
       CASE WHEN REGEXP_LIKE(REPLACE(TRIM(TO_CHAR(i.SALARIO_INTEGRADO)), ','), '^[0-9]*\.?[0-9]+$')
            THEN TO_NUMBER(REPLACE(TRIM(TO_CHAR(i.SALARIO_INTEGRADO)), ',')) END SDI,
       CASE WHEN REGEXP_LIKE(REPLACE(TRIM(TO_CHAR(i.COSTO)), ','), '^[0-9]*\.?[0-9]+$')
            THEN TO_NUMBER(REPLACE(TRIM(TO_CHAR(i.COSTO)), ',')) END COSTO,
       CAST(SUBSTR(i.ESTADO_DICTAMEN, 1, 60) AS VARCHAR2(60))      ESTADO_DICTAMEN,
       CAST(NULL AS VARCHAR2(1000))                                DIAGNOSTICO
  FROM TBL_SERV_INCAPACIDAD_MEDICA i
  LEFT JOIN SERV_MED_V_EMPLEADO emp ON emp.NSS = i.NSS
UNION ALL
SELECT 'EXCEL', TO_CHAR(x.ID), x.NSS, x.FECHA, x.PREDIO, x.CUENTA, x.AREA, x.NOMBRE, x.GENERO, x.EDAD,
       x.PUESTO, x.FOLIO, x.RUBRO, x.RAMO, x.TIPO, x.FECHA_TERMINO, x.DIAS, x.SDI, x.COSTO_TOTAL,
       NULL, x.DIAGNOSTICO
  FROM SERV_MED_XLS_INCAPACIDAD x
 WHERE x.ESTADO_REGISTRO = 'VALIDO'
   AND NOT EXISTS (SELECT 1 FROM TBL_SERV_INCAPACIDAD_MEDICA p
                    WHERE (x.FOLIO IS NOT NULL
                           AND UPPER(REPLACE(p.FOLIO_INCAPACIDAD, ' ')) = UPPER(REPLACE(x.FOLIO, ' ')))
                       OR (x.FOLIO IS NULL AND p.NSS = x.NSS AND
                           CASE WHEN REGEXP_LIKE(p.FECHA_INICIO, '^[0-9]{4}-[0-9]{2}-[0-9]{2}')
                                THEN TO_DATE(SUBSTR(p.FECHA_INICIO, 1, 10), 'YYYY-MM-DD')
                                WHEN REGEXP_LIKE(p.FECHA_INICIO, '^[0-9]{2}/[0-9]{2}/[0-9]{4}')
                                THEN TO_DATE(SUBSTR(p.FECHA_INICIO, 1, 10), 'DD/MM/YYYY') END = x.FECHA));

COMMENT ON TABLE SERV_MED_V_INCAPACIDAD IS 'Incapacidades: portal (TBL_SERV_INCAPACIDAD_MEDICA) + Excel por NSS (SERV_MED_XLS_INCAPACIDAD sin las que ya estan en el portal). FECHA = inicio.';

-- ======================================================================
-- 4 de 5: SERV_MED_V_EXAMEN -- examenes del portal + nuevo ingreso y periodico del Excel
--   + pos incapacidad de la carga VIEJA (por nombre, sin NSS) mientras su layout sigue en revision.
-- DICTAMEN normalizado: APTO | APTO_CONDICIONADO | APTO_RESTRINGIDO | NO_APTO.
--   Portal: la columna marcada (ver PRE-CHECK 2); si hubiera mas de una, gana la mas severa.
-- TIPO: NUEVO_INGRESO | PERIODICO | POS_INCAPACIDAD | NULL (el historial del portal no lo guarda).
-- ======================================================================
CREATE OR REPLACE VIEW SERV_MED_V_EXAMEN AS
SELECT CAST('PORTAL' AS VARCHAR2(12))                              FUENTE,
       TO_CHAR(h.REG_ID)                                           ID_ORIGEN,
       h.NSS                                                       NSS,
       TRUNC(h.FECHA_REGISTRO)                                     FECHA,
       emp.PREDIO                                                  PREDIO,
       emp.CUENTA                                                  CUENTA,
       CAST(NULL AS VARCHAR2(120))                                 AREA,
       emp.NOMBRE                                                  NOMBRE,
       emp.GENERO                                                  GENERO,
       TRUNC(MONTHS_BETWEEN(TRUNC(h.FECHA_REGISTRO), emp.FECHA_NAC) / 12) EDAD,
       emp.PUESTO                                                  PUESTO,
       CAST(NULL AS VARCHAR2(20))                                  TIPO,
       CAST(NULL AS VARCHAR2(20))                                  TIPO_EXAMEN,
       CASE WHEN TRIM(h.NO_APTO) IS NOT NULL
                 AND UPPER(TRIM(h.NO_APTO)) NOT IN ('0','N','NO','FALSE') THEN 'NO_APTO'
            WHEN TRIM(h.APTO_RESTRINGIDO) IS NOT NULL
                 AND UPPER(TRIM(h.APTO_RESTRINGIDO)) NOT IN ('0','N','NO','FALSE') THEN 'APTO_RESTRINGIDO'
            WHEN TRIM(h.APTO_CONDICIONADO) IS NOT NULL
                 AND UPPER(TRIM(h.APTO_CONDICIONADO)) NOT IN ('0','N','NO','FALSE') THEN 'APTO_CONDICIONADO'
            WHEN TRIM(h.APTO) IS NOT NULL
                 AND UPPER(TRIM(h.APTO)) NOT IN ('0','N','NO','FALSE') THEN 'APTO' END DICTAMEN,
       CAST(NULL AS VARCHAR2(1000))                                DIAGNOSTICO
  FROM SERV_MED_RESULTADO_EXAMEN_HIST h
  LEFT JOIN SERV_MED_V_EMPLEADO emp ON emp.NSS = h.NSS
UNION ALL
SELECT 'EXCEL', TO_CHAR(x.ID), x.NSS, x.FECHA, x.PREDIO, x.CUENTA, x.AREA,
       TRIM(REGEXP_REPLACE(x.NOMBRE || ' ' || x.APELLIDO_PATERNO || ' ' || x.APELLIDO_MATERNO, ' +', ' ')),
       x.GENERO, x.EDAD, x.PUESTO_SOLICITADO, 'NUEVO_INGRESO', x.TIPO_EXAMEN, x.DICTAMEN, x.DIAGNOSTICO
  FROM SERV_MED_XLS_EXAMEN_ING x
 WHERE x.ESTADO_REGISTRO = 'VALIDO'
   AND NOT EXISTS (SELECT 1 FROM SERV_MED_RESULTADO_EXAMEN_HIST h
                    WHERE h.NSS = x.NSS AND TRUNC(h.FECHA_REGISTRO) BETWEEN x.FECHA - 1 AND x.FECHA + 1)
UNION ALL
SELECT 'EXCEL', TO_CHAR(x.ID), x.NSS, x.FECHA, x.PREDIO, x.CUENTA, x.AREA, x.NOMBRE, x.GENERO, x.EDAD,
       x.PUESTO, 'PERIODICO', NULL, x.DICTAMEN, x.DIAGNOSTICO
  FROM SERV_MED_XLS_EXAMEN_PER x
 WHERE x.ESTADO_REGISTRO = 'VALIDO'
   AND NOT EXISTS (SELECT 1 FROM SERV_MED_RESULTADO_EXAMEN_HIST h
                    WHERE h.NSS = x.NSS AND TRUNC(h.FECHA_REGISTRO) BETWEEN x.FECHA - 1 AND x.FECHA + 1)
UNION ALL
-- v4: pos incapacidad del layout nuevo por NSS (SERV_MED_XLS_EXAMEN_POS, excel-nss-06)
SELECT 'EXCEL', TO_CHAR(x.ID), x.NSS, x.FECHA, x.PREDIO, x.CUENTA, x.AREA, x.NOMBRE, x.GENERO, x.EDAD,
       x.PUESTO, 'POS_INCAPACIDAD', NULL, x.DICTAMEN, x.DIAGNOSTICO
  FROM SERV_MED_XLS_EXAMEN_POS x
 WHERE x.ESTADO_REGISTRO = 'VALIDO'
   AND NOT EXISTS (SELECT 1 FROM SERV_MED_RESULTADO_EXAMEN_HIST h
                    WHERE h.NSS = x.NSS AND TRUNC(h.FECHA_REGISTRO) BETWEEN x.FECHA - 1 AND x.FECHA + 1)
UNION ALL
-- Carga vieja: el dictamen viene como texto del Excel ("APTO (15)", "NO APTO", ...). El orden de
-- los WHEN importa: "NO APTO" y "APTO CONDICIONADO" tambien contienen "APTO".
-- v4: solo MIENTRAS el layout nuevo de pos incapacidad este vacio. En cuanto se carga el primer
-- renglon valido, la carga vieja deja de leerse sola (si no, se contaria dos veces).
SELECT 'CARGA_VIEJA', TO_CHAR(ev.ID), NULL, ev.FECHA, ev.PREDIO, ev.CUENTA, ev.AREA,
       ev.NOMBRE, ev.GENERO, NULL, ev.PUESTO, 'POS_INCAPACIDAD', NULL,
       CASE WHEN UPPER(at.VALOR) LIKE '%NO APTO%'   OR at.VALOR LIKE '%(30)%' THEN 'NO_APTO'
            WHEN UPPER(at.VALOR) LIKE '%RESTRING%'  OR at.VALOR LIKE '%(60)%' THEN 'APTO_RESTRINGIDO'
            WHEN UPPER(at.VALOR) LIKE '%CONDICION%' OR at.VALOR LIKE '%(45)%' THEN 'APTO_CONDICIONADO'
            WHEN UPPER(at.VALOR) LIKE '%APTO%'      OR at.VALOR LIKE '%(15)%' THEN 'APTO' END,
       NULL
  FROM SERV_MED_BITACORA_EVENTO ev
  LEFT JOIN SERV_MED_BITACORA_ATRIBUTO at ON at.EVENTO_ID = ev.ID AND at.NOMBRE = 'DICTAMEN'
 WHERE ev.FAMILIA = 'EXAMEN_POS_INCAP'
   AND NOT EXISTS (SELECT 1 FROM SERV_MED_XLS_EXAMEN_POS n WHERE n.ESTADO_REGISTRO = 'VALIDO');

COMMENT ON TABLE SERV_MED_V_EXAMEN IS 'Examenes: portal (SERV_MED_RESULTADO_EXAMEN_HIST) + nuevo ingreso y periodico del Excel por NSS + pos incapacidad de la carga vieja.';

-- ======================================================================
-- 5 de 5: SERV_MED_V_ACCIDENTE -- accidentes del portal + del Excel
-- FECHA del portal = fecha del accidente (si no se capturo, la de registro).
-- ======================================================================
CREATE OR REPLACE VIEW SERV_MED_V_ACCIDENTE AS
SELECT CAST('PORTAL' AS VARCHAR2(12))                              FUENTE,
       TO_CHAR(a.REG_ID)                                           ID_ORIGEN,
       a.NSS                                                       NSS,
       TRUNC(NVL(a.FECHA_ACCIDENTE, a.FECHA_REGISTRO))             FECHA,
       emp.PREDIO                                                  PREDIO,
       emp.CUENTA                                                  CUENTA,
       CAST(NULL AS VARCHAR2(120))                                 AREA,
       emp.NOMBRE                                                  NOMBRE,
       emp.GENERO                                                  GENERO,
       TRUNC(MONTHS_BETWEEN(TRUNC(NVL(a.FECHA_ACCIDENTE, a.FECHA_REGISTRO)), emp.FECHA_NAC) / 12) EDAD,
       emp.PUESTO                                                  PUESTO,
       a.TIPO_RIESGO                                               TIPO_RIESGO,
       -- El portal guarda el codigo (LESION_ERGONOMICA) y el layout el texto (LESION ERGONOMICA):
       -- son las mismas 7 causas, uno a uno. Se deja el texto del layout para que sumen juntas.
       CAST(CASE UPPER(TRIM(a.CAUSA_RT))
                 WHEN 'ACCID_VIAL'        THEN 'ACCID VIAL'
                 WHEN 'AGRESION_ASALTO'   THEN 'AGRESION / ASALTO'
                 WHEN 'CAIDA'             THEN 'CAIDA'
                 WHEN 'GOLPE_DIRECTO'     THEN 'GOLPE DIRECTO CON O POR OBJETO'
                 WHEN 'HERIDA'            THEN 'HERIDA'
                 WHEN 'LESION_ERGONOMICA' THEN 'LESION ERGONOMICA'
                 WHEN 'OTRO_IMPROCEDENTE' THEN 'OTRO (IMPROCEDENTE)'
                 ELSE a.CAUSA_RT END AS VARCHAR2(60))              CAUSA_RT,
       CAST(a.DIAGNOSTICO AS VARCHAR2(1000))                       DX_IMSS,
       a.SDI                                                       SDI,
       CAST(REPLACE(a.STATUS_CALIFICACION, '_', ' ') AS VARCHAR2(30)) STATUS_CALIFICACION,
       CAST(NULL AS VARCHAR2(30))                                  STATUS,
       a.COSTO                                                     COSTO,
       CAST(a.OBSERVACIONES AS VARCHAR2(1000))                     OBSERVACIONES,
       -- DIAS: columna agregada 13-oct-2026 (accidente-13-dias.sql); va al final para no mover
       -- las posiciones del UNION
       a.DIAS                                                      DIAS
  FROM SERV_MED_ACCIDENTE a
  LEFT JOIN SERV_MED_V_EMPLEADO emp ON emp.NSS = a.NSS
UNION ALL
SELECT 'EXCEL', TO_CHAR(x.ID), x.NSS, x.FECHA, x.PREDIO, x.CUENTA, x.AREA, x.NOMBRE, x.GENERO, x.EDAD,
       x.PUESTO, x.TIPO_RIESGO, x.CAUSA_RT, x.DX_IMSS, x.SDI, x.STATUS_CALIFICACION, x.STATUS, x.COSTO,
       x.OBSERVACIONES, x.DIAS
  FROM SERV_MED_XLS_ACCIDENTE x
 WHERE x.ESTADO_REGISTRO = 'VALIDO'
   AND NOT EXISTS (SELECT 1 FROM SERV_MED_ACCIDENTE a
                    WHERE a.NSS = x.NSS AND TRUNC(NVL(a.FECHA_ACCIDENTE, a.FECHA_REGISTRO)) = x.FECHA);

COMMENT ON TABLE SERV_MED_V_ACCIDENTE IS 'Accidentes: portal (SERV_MED_ACCIDENTE) + Excel por NSS (SERV_MED_XLS_ACCIDENTE sin los que ya estan en el portal).';

-- ======================================================================
-- v4: SERV_MED_V_ANTIDOPING -- pruebas del portal + Excel por NSS (solo empleados) + carga vieja
-- El portal junta conclusion y estatus en STATUS_CONCLUSION (codigos con guion bajo); aqui se
-- separan con el texto del layout para que las tres fuentes sumen en las mismas barras.
-- La carga vieja (por nombre, sin NSS) se lee SOLO mientras el layout nuevo este vacio.
-- ======================================================================
CREATE OR REPLACE VIEW SERV_MED_V_ANTIDOPING AS
SELECT CAST('PORTAL' AS VARCHAR2(12))                              FUENTE,
       TO_CHAR(p.REG_ID)                                           ID_ORIGEN,
       CAST(p.NSS AS VARCHAR2(13))                                 NSS,
       TRUNC(p.FECHA_REGISTRO)                                     FECHA,
       emp.PREDIO                                                  PREDIO,
       emp.CUENTA                                                  CUENTA,
       CAST(NULL AS VARCHAR2(120))                                 AREA,
       emp.NOMBRE                                                  NOMBRE,
       emp.GENERO                                                  GENERO,
       TRUNC(MONTHS_BETWEEN(TRUNC(p.FECHA_REGISTRO), emp.FECHA_NAC) / 12) EDAD,
       emp.PUESTO                                                  PUESTO,
       CAST(UPPER(TRIM(p.TIPO_PRUEBA)) AS VARCHAR2(40))            TIPO_PRUEBA,
       CAST(UPPER(TRIM(p.RESULTADO)) AS VARCHAR2(40))              RESULTADO,
       CAST(UPPER(TRIM(p.SUSTANCIA)) AS VARCHAR2(40))              SUSTANCIA,
       CAST(NULL AS NUMBER)                                        ALCOHOLEMIA,
       CAST(CASE UPPER(TRIM(p.STATUS_CONCLUSION))
                 WHEN 'N/A' THEN 'N/A'
                 WHEN 'CAPA' THEN 'CAPA'
                 WHEN 'NO_INTERESADO_BAJA' THEN 'NO INTERESADO / BAJA'
                 WHEN 'NO_CONTRATADO' THEN 'NO CONTRATADO'
                 WHEN 'RECAIDA' THEN 'RECAIDA' END AS VARCHAR2(40)) CONCLUSION,
       CAST(CASE UPPER(TRIM(p.STATUS_CONCLUSION))
                 WHEN 'LABORANDO' THEN 'LABORANDO'
                 WHEN 'BAJA_NO_APLICA' THEN 'BAJA / NO CONTRATADO' END AS VARCHAR2(40)) STATUS,
       CAST(SUBSTR(p.OBSERVACIONES, 1, 1000) AS VARCHAR2(1000 CHAR)) OBSERVACIONES
  FROM SERV_MED_ANTIDOPING_RESULTADO p
  LEFT JOIN SERV_MED_V_EMPLEADO emp ON emp.NSS = p.NSS
UNION ALL
SELECT 'EXCEL', TO_CHAR(x.ID), x.NSS, x.FECHA, x.PREDIO, x.CUENTA, x.AREA, x.NOMBRE, x.GENERO, x.EDAD, x.PUESTO,
       x.TIPO_PRUEBA, x.RESULTADO, x.SUSTANCIA, x.ALCOHOLEMIA, x.CONCLUSION, x.STATUS, x.OBSERVACIONES
  FROM SERV_MED_XLS_ANTIDOPING x
 WHERE x.ESTADO_REGISTRO = 'VALIDO'
   AND NOT EXISTS (SELECT 1 FROM SERV_MED_ANTIDOPING_RESULTADO p
                    WHERE p.NSS = x.NSS AND TRUNC(p.FECHA_REGISTRO) = x.FECHA
                      AND UPPER(TRIM(p.TIPO_PRUEBA)) = x.TIPO_PRUEBA)
UNION ALL
-- Carga vieja: atributos EAV. Un positivo podia traer mas de una sustancia: aqui sale una (la
-- primera); la pantalla vieja contaba hallazgos, esta cuenta pruebas.
SELECT 'CARGA_VIEJA', TO_CHAR(ev.ID), NULL, ev.FECHA, ev.PREDIO, ev.CUENTA, ev.AREA, ev.NOMBRE, ev.GENERO, NULL,
       ev.PUESTO,
       (SELECT MAX(a.VALOR) FROM SERV_MED_BITACORA_ATRIBUTO a WHERE a.EVENTO_ID = ev.ID AND a.NOMBRE = 'TIPO_PRUEBA'),
       (SELECT MAX(a.VALOR) FROM SERV_MED_BITACORA_ATRIBUTO a WHERE a.EVENTO_ID = ev.ID AND a.NOMBRE = 'RESULTADO'),
       (SELECT MIN(a.VALOR) FROM SERV_MED_BITACORA_ATRIBUTO a WHERE a.EVENTO_ID = ev.ID AND a.NOMBRE = 'SUSTANCIA'),
       (SELECT MAX(a.VALOR_NUM) FROM SERV_MED_BITACORA_ATRIBUTO a WHERE a.EVENTO_ID = ev.ID AND a.NOMBRE = 'ALCOHOLEMIA'),
       (SELECT MAX(a.VALOR) FROM SERV_MED_BITACORA_ATRIBUTO a WHERE a.EVENTO_ID = ev.ID AND a.NOMBRE = 'CONCLUSION'),
       ev.ESTATUS,
       NULL
  FROM SERV_MED_BITACORA_EVENTO ev
 WHERE ev.FAMILIA = 'ANTIDOPING'
   AND NOT EXISTS (SELECT 1 FROM SERV_MED_XLS_ANTIDOPING n WHERE n.ESTADO_REGISTRO = 'VALIDO');

COMMENT ON TABLE SERV_MED_V_ANTIDOPING IS 'Antidoping: portal (SERV_MED_ANTIDOPING_RESULTADO) + Excel por NSS (SERV_MED_XLS_ANTIDOPING) + carga vieja mientras el Excel nuevo este vacio.';

-- ======================================================================
-- v4: SERV_MED_V_MATERNIDAD -- seguimiento CON HISTORIAL: chequeos del portal + reportes mensuales
-- del Excel (un renglon por caso por mes, FECHA = corte) + carga vieja mientras el Excel este vacio.
-- CLAVE_EMBARAZO agrupa el historial: Excel NSS|FUM (o NSS|FPP); el portal no captura FUM, asi que
-- su clave es NSS|FPP (o NSS|PORTAL si tampoco tiene fecha probable de parto); la carga vieja no
-- tiene NSS y usa el nombre.
-- SEMANAS: el portal las captura; del Excel se calculan de FUM a la fecha de corte.
-- ======================================================================
CREATE OR REPLACE VIEW SERV_MED_V_MATERNIDAD AS
SELECT CAST('PORTAL' AS VARCHAR2(12))                              FUENTE,
       TO_CHAR(p.REG_ID)                                           ID_ORIGEN,
       CAST(p.NSS AS VARCHAR2(13))                                 NSS,
       TRUNC(p.FECHA_REGISTRO)                                     FECHA,
       emp.PREDIO                                                  PREDIO,
       emp.CUENTA                                                  CUENTA,
       CAST(NULL AS VARCHAR2(120))                                 AREA,
       emp.NOMBRE                                                  NOMBRE,
       emp.GENERO                                                  GENERO,
       TRUNC(MONTHS_BETWEEN(TRUNC(p.FECHA_REGISTRO), emp.FECHA_NAC) / 12) EDAD,
       emp.PUESTO                                                  PUESTO,
       CAST(p.NSS || CASE WHEN p.FECHA_PROBABLE_PARTO IS NOT NULL
                          THEN '|FPP:' || TO_CHAR(p.FECHA_PROBABLE_PARTO, 'YYYY-MM-DD')
                          ELSE '|PORTAL' END AS VARCHAR2(320))     CLAVE_EMBARAZO,
       CAST(SUBSTR(p.ESTATUS, 1, 40) AS VARCHAR2(40))              STATUS_LABORAL,
       CAST(NULL AS DATE)                                          FUM,
       p.SEMANAS_GESTACION                                         SEMANAS,
       p.FECHA_PROBABLE_PARTO                                      FECHA_PROB_PARTO,
       p.REINCORPORACION                                           FECHA_REAL_REINICIO,
       CAST(NULL AS DATE)                                          FECHA_INICIO_INCAP,
       CAST(NULL AS DATE)                                          FECHA_TERMINO_INCAP,
       CAST(SUBSTR(p.INCAPACIDAD, 1, 100) AS VARCHAR2(100))        FOLIO_INCAPACIDAD,
       CAST(NULL AS DATE)                                          FECHA_INICIO_LACTANCIA,
       CAST(NULL AS DATE)                                          FECHA_TERMINO_LACTANCIA,
       p.PROXIMA_REVISION                                          PROXIMA_REVISION
  FROM SERV_MED_MATERNIDAD_SEGUIMIENTO p
  LEFT JOIN SERV_MED_V_EMPLEADO emp ON emp.NSS = p.NSS
UNION ALL
SELECT 'EXCEL', TO_CHAR(x.ID), x.NSS, x.FECHA, x.PREDIO, x.CUENTA, x.AREA, x.NOMBRE, x.GENERO, x.EDAD, x.PUESTO,
       x.CLAVE_EMBARAZO, x.STATUS_LABORAL, x.FUM,
       CASE WHEN x.FUM IS NOT NULL AND x.FECHA >= x.FUM THEN TRUNC((x.FECHA - x.FUM) / 7) END,
       x.FECHA_PROB_PARTO, x.FECHA_REAL_REINICIO, x.FECHA_INICIO_INCAP, x.FECHA_TERMINO_INCAP,
       x.FOLIO_INCAPACIDAD, x.FECHA_INICIO_LACTANCIA, x.FECHA_TERMINO_LACTANCIA, NULL
  FROM SERV_MED_XLS_MATERNIDAD x
 WHERE x.ESTADO_REGISTRO = 'VALIDO'
   AND NOT EXISTS (SELECT 1 FROM SERV_MED_MATERNIDAD_SEGUIMIENTO p
                    WHERE p.NSS = x.NSS AND TRUNC(p.FECHA_REGISTRO, 'MM') = TRUNC(x.FECHA, 'MM'))
UNION ALL
-- Carga vieja: atributos EAV, sin NSS. La fecha probable de parto es texto ISO (era formula).
SELECT 'CARGA_VIEJA', TO_CHAR(ev.ID), NULL, ev.FECHA, ev.PREDIO, ev.CUENTA, ev.AREA, ev.NOMBRE, ev.GENERO, NULL,
       ev.PUESTO,
       'NOMBRE|' || ev.NOMBRE_NORM,
       ev.ESTATUS,
       NULL,
       (SELECT MAX(a.VALOR_NUM) FROM SERV_MED_BITACORA_ATRIBUTO a
         WHERE a.EVENTO_ID = ev.ID AND a.NOMBRE = 'EDAD_GESTACIONAL.SEMANA' AND a.VALOR_NUM BETWEEN 1 AND 45),
       (SELECT MAX(CASE WHEN REGEXP_LIKE(a.VALOR, '^[0-9]{4}-[0-9]{2}-[0-9]{2}')
                        THEN TO_DATE(SUBSTR(a.VALOR, 1, 10), 'YYYY-MM-DD') END)
          FROM SERV_MED_BITACORA_ATRIBUTO a WHERE a.EVENTO_ID = ev.ID AND a.NOMBRE = 'FECHA_PROBABLE_PARTO'),
       (SELECT MAX(COALESCE(a.VALOR_FECHA, CASE WHEN REGEXP_LIKE(a.VALOR, '^[0-9]{4}-[0-9]{2}-[0-9]{2}')
                                                THEN TO_DATE(SUBSTR(a.VALOR, 1, 10), 'YYYY-MM-DD') END))
          FROM SERV_MED_BITACORA_ATRIBUTO a WHERE a.EVENTO_ID = ev.ID AND a.NOMBRE = 'REINICIO_REAL'),
       NULL, NULL, NULL, NULL, NULL, NULL
  FROM SERV_MED_BITACORA_EVENTO ev
 WHERE ev.FAMILIA = 'MATERNIDAD'
   AND NOT EXISTS (SELECT 1 FROM SERV_MED_XLS_MATERNIDAD n WHERE n.ESTADO_REGISTRO = 'VALIDO');

COMMENT ON TABLE SERV_MED_V_MATERNIDAD IS 'Maternidad con historial: chequeos del portal + reportes mensuales del Excel (SERV_MED_XLS_MATERNIDAD) + carga vieja mientras el Excel nuevo este vacio. Embarazo = CLAVE_EMBARAZO.';

-- ======================================================================
-- v4: SERV_MED_V_MATERNIDAD_ACTUAL -- estado actual de cada embarazo = su renglon mas reciente
-- ("fecha de ultima actualizacion" = FECHA de ese renglon). Para la pantalla y el expediente.
-- ======================================================================
CREATE OR REPLACE VIEW SERV_MED_V_MATERNIDAD_ACTUAL AS
SELECT m.*
  FROM (SELECT v.*, ROW_NUMBER() OVER (PARTITION BY v.CLAVE_EMBARAZO
                                        ORDER BY v.FECHA DESC, v.FUENTE, v.ID_ORIGEN DESC) rn
          FROM SERV_MED_V_MATERNIDAD v) m
 WHERE m.rn = 1;

COMMENT ON TABLE SERV_MED_V_MATERNIDAD_ACTUAL IS 'Maternidad: un renglon por embarazo con su estado mas reciente (ultima actualizacion).';

-- ----------------------------------------------------------------------
-- VERIFICACION (solo lectura)
-- ----------------------------------------------------------------------
-- a) Las 8 vistas compiladas: deben salir 8 renglones con STATUS = VALID
SELECT object_name, status FROM user_objects
 WHERE object_type = 'VIEW' AND object_name IN
       ('SERV_MED_V_EMPLEADO','SERV_MED_V_ATENCION','SERV_MED_V_INCAPACIDAD','SERV_MED_V_EXAMEN','SERV_MED_V_ACCIDENTE',
        'SERV_MED_V_ANTIDOPING','SERV_MED_V_MATERNIDAD','SERV_MED_V_MATERNIDAD_ACTUAL')
 ORDER BY 1;

-- b) Un renglon por NSS en la vista de empleado: debe salir 0
SELECT COUNT(*) nss_repetidos FROM (SELECT NSS FROM SERV_MED_V_EMPLEADO GROUP BY NSS HAVING COUNT(*) > 1);

-- c) Renglones por vista, anio y fuente. Las vistas NO filtran fechas; este corte es solo para
--    revisar el alcance de la auditoria (2025 y 2026). Referencia de la prueba con XLS (solo Excel,
--    2026): atenciones 8,618 / examenes 5,128 / personas con incapacidad 474.
--    Hoy el Excel cargado (lotes de prueba) solo trae 2026: un 2025 en cero del lado EXCEL es esperado.
SELECT 'ATENCION' vista, EXTRACT(YEAR FROM FECHA) anio, FUENTE, COUNT(*) renglones, COUNT(DISTINCT NSS) personas
  FROM SERV_MED_V_ATENCION
 WHERE FECHA >= DATE '2025-01-01' AND FECHA < DATE '2027-01-01' GROUP BY EXTRACT(YEAR FROM FECHA), FUENTE
UNION ALL SELECT 'INCAPACIDAD', EXTRACT(YEAR FROM FECHA), FUENTE, COUNT(*), COUNT(DISTINCT NSS)
  FROM SERV_MED_V_INCAPACIDAD
 WHERE FECHA >= DATE '2025-01-01' AND FECHA < DATE '2027-01-01' GROUP BY EXTRACT(YEAR FROM FECHA), FUENTE
UNION ALL SELECT 'EXAMEN', EXTRACT(YEAR FROM FECHA), FUENTE, COUNT(*), COUNT(DISTINCT NSS)
  FROM SERV_MED_V_EXAMEN
 WHERE FECHA >= DATE '2025-01-01' AND FECHA < DATE '2027-01-01' GROUP BY EXTRACT(YEAR FROM FECHA), FUENTE
UNION ALL SELECT 'ACCIDENTE', EXTRACT(YEAR FROM FECHA), FUENTE, COUNT(*), COUNT(DISTINCT NSS)
  FROM SERV_MED_V_ACCIDENTE
 WHERE FECHA >= DATE '2025-01-01' AND FECHA < DATE '2027-01-01' GROUP BY EXTRACT(YEAR FROM FECHA), FUENTE
UNION ALL SELECT 'ANTIDOPING', EXTRACT(YEAR FROM FECHA), FUENTE, COUNT(*), COUNT(DISTINCT NSS)
  FROM SERV_MED_V_ANTIDOPING
 WHERE FECHA >= DATE '2025-01-01' AND FECHA < DATE '2027-01-01' GROUP BY EXTRACT(YEAR FROM FECHA), FUENTE
UNION ALL SELECT 'MATERNIDAD', EXTRACT(YEAR FROM FECHA), FUENTE, COUNT(*), COUNT(DISTINCT CLAVE_EMBARAZO)
  FROM SERV_MED_V_MATERNIDAD
 WHERE FECHA >= DATE '2025-01-01' AND FECHA < DATE '2027-01-01' GROUP BY EXTRACT(YEAR FROM FECHA), FUENTE
 ORDER BY 1, 2, 3;

-- c2) v4: examenes por TIPO y fuente (la carga vieja de pos incapacidad debe seguir saliendo
--     mientras SERV_MED_XLS_EXAMEN_POS este vacia) y embarazos en el estado actual
SELECT 'EXAMEN' vista, NVL(TIPO, '(portal)') tipo, FUENTE, COUNT(*) renglones FROM SERV_MED_V_EXAMEN
 GROUP BY NVL(TIPO, '(portal)'), FUENTE
UNION ALL SELECT 'MATERNIDAD_ACTUAL', NVL(STATUS_LABORAL, '(sin status)'), FUENTE, COUNT(*)
  FROM SERV_MED_V_MATERNIDAD_ACTUAL GROUP BY NVL(STATUS_LABORAL, '(sin status)'), FUENTE
 ORDER BY 1, 3, 2;

-- d) Lo del portal: cuantos renglones quedan sin predio ("Sin asignar") y examenes sin dictamen legible
SELECT 'ATENCION' vista, COUNT(*) portal, SUM(CASE WHEN PREDIO IS NULL THEN 1 ELSE 0 END) sin_predio,
       0 sin_dictamen FROM SERV_MED_V_ATENCION WHERE FUENTE = 'PORTAL'
UNION ALL SELECT 'INCAPACIDAD', COUNT(*), SUM(CASE WHEN PREDIO IS NULL THEN 1 ELSE 0 END), 0
  FROM SERV_MED_V_INCAPACIDAD WHERE FUENTE = 'PORTAL'
UNION ALL SELECT 'EXAMEN', COUNT(*), SUM(CASE WHEN PREDIO IS NULL THEN 1 ELSE 0 END),
       SUM(CASE WHEN DICTAMEN IS NULL THEN 1 ELSE 0 END)
  FROM SERV_MED_V_EXAMEN WHERE FUENTE = 'PORTAL'
UNION ALL SELECT 'ACCIDENTE', COUNT(*), SUM(CASE WHEN PREDIO IS NULL THEN 1 ELSE 0 END), 0
  FROM SERV_MED_V_ACCIDENTE WHERE FUENTE = 'PORTAL';

-- ----------------------------------------------------------------------
-- REVERSA (solo si hiciera falta): las vistas no guardan datos, borrarlas no pierde nada.
-- ----------------------------------------------------------------------
-- DROP VIEW SERV_MED_V_MATERNIDAD_ACTUAL;
-- DROP VIEW SERV_MED_V_MATERNIDAD;
-- DROP VIEW SERV_MED_V_ANTIDOPING;
-- DROP VIEW SERV_MED_V_ACCIDENTE;
-- DROP VIEW SERV_MED_V_EXAMEN;
-- DROP VIEW SERV_MED_V_INCAPACIDAD;
-- DROP VIEW SERV_MED_V_ATENCION;
-- DROP VIEW SERV_MED_V_EMPLEADO;
