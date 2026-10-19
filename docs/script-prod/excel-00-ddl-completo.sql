-- ============================================================================
-- CARGA DE EXCEL 2026 -- DDL COMPLETO EN UN SOLO ARCHIVO
--
-- Reune excel-01-ddl-bitacora.sql y excel-02-ddl-seguimiento.sql en el orden
-- correcto de dependencias, con un solo PRE-CHECK y una sola verificacion.
-- Pensado para aplicarlo en QA y probar la carga ahi antes de produccion.
--
-- BASE: la del PORTAL (JDBC).
--         en QA          -> ONEWMS_QA
--         en produccion  -> BIOMETRICO
--       NO es ORDS. Aqui no hay ningun WS ni tabla del legacy.
--
-- QUE HACE
--   6 CREATE TABLE
--   4 ALTER TABLE ADD sobre una de esas 6 tablas nuevas (no sobre nada ajeno)
--  15 indices explicitos  (+6 de las llaves primarias = 21 en user_indexes)
--   4 llaves f
oraneas, todas ENTRE LAS TABLAS NUEVAS
--
-- QUE NO HACE
--   Ni un solo DROP, TRUNCATE, GRANT, DELETE ni UPDATE.
--   No toca ninguna tabla que ya exista en el esquema.
--
-- IDEMPOTENTE: se puede correr dos veces. Los guards atrapan
--   -955  el objeto ya existe
--  -1408 esa columna ya esta indexada
--  -1430 esa columna ya se agrego
--
-- REVERSA: docs/script-prod/excel-99-reversa-bitacora.sql
-- PLAN:    docs/plan-carga-excel-2026.html
--
-- ESTADO
--   Produccion (BIOMETRICO@PDBPRD): excel-01 y excel-02 aplicados el 29-sep-2026.
--   QA: pendiente. Este archivo es el que va.
-- ============================================================================

SET SERVEROUTPUT ON
SET LINESIZE 200
SET PAGESIZE 200

PROMPT
PROMPT ##########################################################################
PROMPT #  PRE-CHECK  --  solo lectura. Leer la salida antes de seguir.
PROMPT ##########################################################################

DECLARE
  v_n     NUMBER;
  v_falta NUMBER := 0;
BEGIN
  DBMS_OUTPUT.PUT_LINE('Usuario: ' || USER ||
                       '  ·  Base: ' || SYS_CONTEXT('USERENV','DB_NAME') ||
                       '  ·  PDB: ' || SYS_CONTEXT('USERENV','CON_NAME'));
  DBMS_OUTPUT.PUT_LINE('Momento: ' || TO_CHAR(SYSTIMESTAMP,'YYYY-MM-DD HH24:MI:SS'));

  SELECT COUNT(*) INTO v_n FROM user_tables;
  DBMS_OUTPUT.PUT_LINE('Tablas que ya tiene este esquema: ' || v_n);

  -- 1. Colision de nombres
  SELECT COUNT(*) INTO v_n FROM user_objects
   WHERE object_name IN ('SERV_MED_BITACORA_LOTE','SERV_MED_BITACORA_EVENTO',
                         'SERV_MED_BITACORA_ATRIBUTO','SERV_MED_BITACORA_ESTADO_MES',
                         'SERV_MED_BITACORA_METRICA','SERV_MED_BITACORA_NOMENCL');
  IF v_n = 0 THEN
    DBMS_OUTPUT.PUT_LINE('OK  · Ninguna de las 6 tablas existe: se van a crear todas.');
  ELSE
    DBMS_OUTPUT.PUT_LINE('AVISO · Ya existen ' || v_n || ' de las 6 tablas.');
    DBMS_OUTPUT.PUT_LINE('        El script las salta, no las modifica.');
  END IF;

  -- 2. Limite de 30 caracteres del esquema legacy
  SELECT COUNT(*) INTO v_n FROM (
    SELECT 'SERV_MED_BITACORA_LOTE' n FROM dual UNION ALL
    SELECT 'SERV_MED_BITACORA_EVENTO'        FROM dual UNION ALL
    SELECT 'SERV_MED_BITACORA_ATRIBUTO'      FROM dual UNION ALL
    SELECT 'SERV_MED_BITACORA_ESTADO_MES'    FROM dual UNION ALL
    SELECT 'SERV_MED_BITACORA_METRICA'       FROM dual UNION ALL
    SELECT 'SERV_MED_BITACORA_NOMENCL'       FROM dual UNION ALL
    SELECT 'SERV_MED_PK_BITACORA_LOTE'       FROM dual UNION ALL
    SELECT 'SERV_MED_PK_BITACORA_EVE'        FROM dual UNION ALL
    SELECT 'SERV_MED_PK_BITACORA_ATR'        FROM dual UNION ALL
    SELECT 'SERV_MED_PK_BITACORA_EST_MES'    FROM dual UNION ALL
    SELECT 'SERV_MED_PK_BITACORA_METRICA'    FROM dual UNION ALL
    SELECT 'SERV_MED_PK_BITACORA_NOMENCL'    FROM dual UNION ALL
    SELECT 'SERV_MED_FK_BITACORA_LOTE'       FROM dual UNION ALL
    SELECT 'SERV_MED_FK_BITACORA_EVENTO'     FROM dual UNION ALL
    SELECT 'SERV_MED_FK_EST_MES_EVENTO'      FROM dual UNION ALL
    SELECT 'SERV_MED_FK_METRICA_LOTE'        FROM dual UNION ALL
    SELECT 'SERV_MED_UX_BITACORA_LOTE_AR'    FROM dual UNION ALL
    SELECT 'SERV_MED_UX_BITACORA_ORIGEN'     FROM dual UNION ALL
    SELECT 'SERV_MED_IX_BITACORA_FAM_MES'    FROM dual UNION ALL
    SELECT 'SERV_MED_IX_BITACORA_PREDIO'     FROM dual UNION ALL
    SELECT 'SERV_MED_IX_BITACORA_CUENTA'     FROM dual UNION ALL
    SELECT 'SERV_MED_IX_BITACORA_LOTE'       FROM dual UNION ALL
    SELECT 'SERV_MED_IX_BITACORA_ESTATUS'    FROM dual UNION ALL
    SELECT 'SERV_MED_IX_ATRIB_EVENTO'        FROM dual UNION ALL
    SELECT 'SERV_MED_IX_ATRIB_NOM_VAL'       FROM dual UNION ALL
    SELECT 'SERV_MED_UX_EST_MES'             FROM dual UNION ALL
    SELECT 'SERV_MED_IX_EST_MES_EST'         FROM dual UNION ALL
    SELECT 'SERV_MED_UX_METRICA'             FROM dual UNION ALL
    SELECT 'SERV_MED_IX_METRICA_CONCEPTO'    FROM dual UNION ALL
    SELECT 'SERV_MED_UX_NOMENCL'             FROM dual
  ) WHERE LENGTH(n) > 30;
  IF v_n > 0 THEN
    DBMS_OUTPUT.PUT_LINE('ERROR · ' || v_n || ' nombres pasan de 30 caracteres. NO SEGUIR.');
    v_falta := 1;
  ELSE
    DBMS_OUTPUT.PUT_LINE('OK  · Los 30 nombres caben en 30 caracteres.');
  END IF;

  -- 3. Version de Oracle: las columnas IDENTITY existen desde 12.1
  SELECT TO_NUMBER(REGEXP_SUBSTR(version, '^\d+')) INTO v_n
    FROM product_component_version
   WHERE product LIKE 'Oracle%' AND ROWNUM = 1;
  IF v_n < 12 THEN
    DBMS_OUTPUT.PUT_LINE('ERROR · Oracle ' || v_n || ': las columnas IDENTITY piden 12.1+. NO SEGUIR.');
    v_falta := 1;
  ELSE
    DBMS_OUTPUT.PUT_LINE('OK  · Oracle ' || v_n || ': soporta columnas IDENTITY.');
  END IF;

  -- 4. Espacio. Estas tablas son chicas: ~20 mil eventos y ~80 mil atributos.
  --    El grueso del origen son los 132 archivos de Excel, que NO entran a la base.
  BEGIN
    SELECT ROUND(SUM(bytes)/1024/1024) INTO v_n FROM user_free_space;
    DBMS_OUTPUT.PUT_LINE('OK  · Espacio libre visible: ' || NVL(v_n,0) || ' MB (se necesitan <50).');
  EXCEPTION WHEN OTHERS THEN
    DBMS_OUTPUT.PUT_LINE('--  · No se pudo medir el espacio (falta permiso). No es bloqueante.');
  END;

  IF v_falta = 1 THEN
    DBMS_OUTPUT.PUT_LINE('*** PRE-CHECK CON ERRORES: no continuar. ***');
  ELSE
    DBMS_OUTPUT.PUT_LINE('--> PRE-CHECK LIMPIO. Se puede aplicar el resto.');
  END IF;
END;
/


PROMPT
PROMPT ##########################################################################
PROMPT #  1 de 6  ·  SERV_MED_BITACORA_LOTE
PROMPT #  Un renglon por ARCHIVO+HOJA procesado. Es la bitacora de la carga y
PROMPT #  el CUADRE: cada Excel trae su hoja ACUMULADO con los totales, asi que
PROMPT #  el cargador compara lo que conto contra lo que el archivo dice de si
PROMPT #  mismo. Verificado: en ENE 2017 el detalle da 103 y el ACUMULADO 103.
PROMPT ##########################################################################
BEGIN
  EXECUTE IMMEDIATE q'[
    CREATE TABLE SERV_MED_BITACORA_LOTE (
      ID                  NUMBER GENERATED BY DEFAULT AS IDENTITY
                          CONSTRAINT SERV_MED_PK_BITACORA_LOTE PRIMARY KEY,
      ARCHIVO             VARCHAR2(400)  NOT NULL,
      RUTA                VARCHAR2(1000),
      CHECKSUM_SHA256     VARCHAR2(64),
      HOJA                VARCHAR2(120),
      FAMILIA             VARCHAR2(40)   NOT NULL,
      PREDIO              VARCHAR2(120),
      ANIO                NUMBER(4),
      FILAS_LEIDAS        NUMBER DEFAULT 0,
      FILAS_INSERTADAS    NUMBER DEFAULT 0,
      FILAS_DESCARTADAS   NUMBER DEFAULT 0,
      ACUMULADO_ESPERADO  NUMBER,
      CUADRA              VARCHAR2(1),
      ESTADO              VARCHAR2(30) DEFAULT 'PENDIENTE' NOT NULL,
      MENSAJES            CLOB,
      STARTED_AT          TIMESTAMP(6),
      FINISHED_AT         TIMESTAMP(6),
      CREATED_AT          TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL,
      CREATED_BY          VARCHAR2(100) DEFAULT 'CARGA_EXCEL' NOT NULL
    )
  ]';
EXCEPTION WHEN OTHERS THEN IF SQLCODE NOT IN (-955) THEN RAISE; END IF; END;
/

COMMENT ON TABLE SERV_MED_BITACORA_LOTE IS
  'Carga de Excel: un renglon por archivo+hoja. CUADRA compara lo contado contra la hoja ACUMULADO del propio Excel.';
COMMENT ON COLUMN SERV_MED_BITACORA_LOTE.CHECKSUM_SHA256 IS
  'Detecta copias con nombre distinto: TOLUCA.xlsm y TOLUCA1.xlsm pueden ser el mismo archivo.';
COMMENT ON COLUMN SERV_MED_BITACORA_LOTE.ANIO IS
  'Del CONTENIDO del archivo, nunca de la carpeta: en MORBILIDAD 2026 hay nueve reportes de 2025.';

BEGIN EXECUTE IMMEDIATE
  'CREATE UNIQUE INDEX SERV_MED_UX_BITACORA_LOTE_AR ON SERV_MED_BITACORA_LOTE (ARCHIVO, HOJA)';
EXCEPTION WHEN OTHERS THEN IF SQLCODE NOT IN (-955,-1408) THEN RAISE; END IF; END;
/


PROMPT
PROMPT ##########################################################################
PROMPT #  2 de 6  ·  SERV_MED_BITACORA_EVENTO
PROMPT #  Un renglon por atencion / examen / incapacidad / accidente, YA
PROMPT #  TRANSPUESTO: el "marca con 1" del Excel se resuelve aqui a un valor.
PROMPT #  Las columnas fijas son el esqueleto que comparten las 8 familias de
PROMPT #  eventos; lo propio de cada una va en _ATRIBUTO, y por eso no hace
PROMPT #  falta una tabla por familia.
PROMPT ##########################################################################
BEGIN
  EXECUTE IMMEDIATE q'[
    CREATE TABLE SERV_MED_BITACORA_EVENTO (
      ID              NUMBER GENERATED BY DEFAULT AS IDENTITY
                      CONSTRAINT SERV_MED_PK_BITACORA_EVE PRIMARY KEY,
      LOTE_ID         NUMBER        NOT NULL,
      FAMILIA         VARCHAR2(40)  NOT NULL,

      FECHA           DATE,
      ANIO            NUMBER(4),
      MES             NUMBER(2),

      PREDIO          VARCHAR2(120),
      CUENTA          VARCHAR2(160),
      AREA            VARCHAR2(120),
      PUESTO          VARCHAR2(120),
      AGENCIA         VARCHAR2(120),

      NOMBRE          VARCHAR2(300),
      NOMBRE_NORM     VARCHAR2(300),
      NSS             VARCHAR2(50),
      RANGO_EDAD      VARCHAR2(20),
      GENERO          VARCHAR2(10),

      ORIGEN_ARCHIVO  VARCHAR2(400) NOT NULL,
      ORIGEN_HOJA     VARCHAR2(120) NOT NULL,
      ORIGEN_FILA     NUMBER        NOT NULL,

      CREATED_AT      TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL,
      CREATED_BY      VARCHAR2(100) DEFAULT 'CARGA_EXCEL' NOT NULL,

      CONSTRAINT SERV_MED_FK_BITACORA_LOTE
        FOREIGN KEY (LOTE_ID) REFERENCES SERV_MED_BITACORA_LOTE (ID)
    )
  ]';
EXCEPTION WHEN OTHERS THEN IF SQLCODE NOT IN (-955) THEN RAISE; END IF; END;
/

COMMENT ON TABLE SERV_MED_BITACORA_EVENTO IS
  'Carga de Excel: un evento por renglon, ya transpuesto. Lo propio de cada familia vive en SERV_MED_BITACORA_ATRIBUTO.';
COMMENT ON COLUMN SERV_MED_BITACORA_EVENTO.NSS IS
  'Siempre NULL: los Excel no traen NSS, solo nombre. Reservada por si se resuelve la identidad despues.';
COMMENT ON COLUMN SERV_MED_BITACORA_EVENTO.PREDIO IS
  'Sale de la HOJA del archivo, no del mapeo cuenta->predio del portal (regla confirmada por servicio medico el 29-sep-2026).';
COMMENT ON COLUMN SERV_MED_BITACORA_EVENTO.PUESTO IS
  'Aqui se distingue al cliente: cuando dice "cliente", CUENTA indica de que cuenta es. Es el criterio que dio servicio medico.';
COMMENT ON COLUMN SERV_MED_BITACORA_EVENTO.RANGO_EDAD IS
  'Normalizado a "18-25". El Excel escribe "18 - 25" con espacios y el dashboard produce "18-25": sin normalizar la grafica sale duplicada.';
COMMENT ON COLUMN SERV_MED_BITACORA_EVENTO.ORIGEN_FILA IS
  'Con ORIGEN_ARCHIVO y ORIGEN_HOJA permite rastrear cualquier cifra de cualquier grafica hasta la celda que la origino.';

BEGIN EXECUTE IMMEDIATE
  'CREATE UNIQUE INDEX SERV_MED_UX_BITACORA_ORIGEN ON SERV_MED_BITACORA_EVENTO (ORIGEN_ARCHIVO, ORIGEN_HOJA, ORIGEN_FILA)';
EXCEPTION WHEN OTHERS THEN IF SQLCODE NOT IN (-955,-1408) THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE
  'CREATE INDEX SERV_MED_IX_BITACORA_FAM_MES ON SERV_MED_BITACORA_EVENTO (FAMILIA, ANIO, MES)';
EXCEPTION WHEN OTHERS THEN IF SQLCODE NOT IN (-955,-1408) THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE
  'CREATE INDEX SERV_MED_IX_BITACORA_PREDIO ON SERV_MED_BITACORA_EVENTO (PREDIO)';
EXCEPTION WHEN OTHERS THEN IF SQLCODE NOT IN (-955,-1408) THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE
  'CREATE INDEX SERV_MED_IX_BITACORA_CUENTA ON SERV_MED_BITACORA_EVENTO (CUENTA)';
EXCEPTION WHEN OTHERS THEN IF SQLCODE NOT IN (-955,-1408) THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE
  'CREATE INDEX SERV_MED_IX_BITACORA_LOTE ON SERV_MED_BITACORA_EVENTO (LOTE_ID)';
EXCEPTION WHEN OTHERS THEN IF SQLCODE NOT IN (-955,-1408) THEN RAISE; END IF; END;
/


PROMPT
PROMPT ##########################################################################
PROMPT #  3 de 6  ·  SERV_MED_BITACORA_ATRIBUTO
PROMPT #  Lo que cambia de familia a familia, sin una tabla por familia.
PROMPT #  Mismo patron EAV que ya usa SERV_MED_TAG (550 mil renglones, probado).
PROMPT #  Agregar una familia nueva NO requiere DDL: solo renglones aqui.
PROMPT ##########################################################################
BEGIN
  EXECUTE IMMEDIATE q'[
    CREATE TABLE SERV_MED_BITACORA_ATRIBUTO (
      ID            NUMBER GENERATED BY DEFAULT AS IDENTITY
                    CONSTRAINT SERV_MED_PK_BITACORA_ATR PRIMARY KEY,
      EVENTO_ID     NUMBER        NOT NULL,
      NOMBRE        VARCHAR2(60)  NOT NULL,
      VALOR         VARCHAR2(300),
      VALOR_NUM     NUMBER,
      VALOR_FECHA   DATE,
      CREATED_AT    TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL,
      CONSTRAINT SERV_MED_FK_BITACORA_EVENTO
        FOREIGN KEY (EVENTO_ID) REFERENCES SERV_MED_BITACORA_EVENTO (ID)
    )
  ]';
EXCEPTION WHEN OTHERS THEN IF SQLCODE NOT IN (-955) THEN RAISE; END IF; END;
/

COMMENT ON TABLE SERV_MED_BITACORA_ATRIBUTO IS
  'Carga de Excel: atributos propios de cada familia (EAV). ATENCION -> CAUSA/RAMO/DIAGNOSTICO/LESION_ME · EXAMEN_* -> DICTAMEN · INCAPACIDAD -> FOLIO/DIAS/COSTO/FECHA_ALTA · ACCIDENTE -> TIPO · ANTIDOPING -> RESULTADO/DESENLACE · MATERNIDAD -> ESTADO/DIAS.';
COMMENT ON COLUMN SERV_MED_BITACORA_ATRIBUTO.VALOR IS
  'Valor CRUDO del Excel. Si una equivalencia de negocio cambia, se recalcula sin releer los 132 archivos.';
COMMENT ON COLUMN SERV_MED_BITACORA_ATRIBUTO.VALOR_NUM IS
  'Dias y costos. En incapacidades se guardan DOS: DIAS_ACUMULADOS como lo dice el Excel y DIAS_ACUMULADOS_CALC calculado por el cargador. Nunca se hereda un numero de una formula sin verificarlo.';

BEGIN EXECUTE IMMEDIATE
  'CREATE INDEX SERV_MED_IX_ATRIB_EVENTO ON SERV_MED_BITACORA_ATRIBUTO (EVENTO_ID)';
EXCEPTION WHEN OTHERS THEN IF SQLCODE NOT IN (-955,-1408) THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE
  'CREATE INDEX SERV_MED_IX_ATRIB_NOM_VAL ON SERV_MED_BITACORA_ATRIBUTO (NOMBRE, VALOR)';
EXCEPTION WHEN OTHERS THEN IF SQLCODE NOT IN (-955,-1408) THEN RAISE; END IF; END;
/


PROMPT
PROMPT ##########################################################################
PROMPT #  4 de 6  ·  4 columnas mas en SERV_MED_BITACORA_EVENTO
PROMPT #  Dimensiones que traen los reportes de seguimiento y que las graficas
PROMPT #  usan para filtrar. Van como columnas y no como atributos EAV porque
PROMPT #  un atributo obligaria a un JOIN en cada consulta.
PROMPT ##########################################################################
BEGIN EXECUTE IMMEDIATE
  'ALTER TABLE SERV_MED_BITACORA_EVENTO ADD (EDAD NUMBER(3))';
EXCEPTION WHEN OTHERS THEN IF SQLCODE NOT IN (-1430) THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE
  'ALTER TABLE SERV_MED_BITACORA_EVENTO ADD (FECHA_NACIMIENTO DATE)';
EXCEPTION WHEN OTHERS THEN IF SQLCODE NOT IN (-1430) THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE
  'ALTER TABLE SERV_MED_BITACORA_EVENTO ADD (FECHA_INGRESO DATE)';
EXCEPTION WHEN OTHERS THEN IF SQLCODE NOT IN (-1430) THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE
  'ALTER TABLE SERV_MED_BITACORA_EVENTO ADD (ESTATUS VARCHAR2(40))';
EXCEPTION WHEN OTHERS THEN IF SQLCODE NOT IN (-1430) THEN RAISE; END IF; END;
/

COMMENT ON COLUMN SERV_MED_BITACORA_EVENTO.EDAD IS
  'Edad exacta. Solo la traen los reportes de seguimiento; las 9 familias dan solo RANGO_EDAD. NUNCA copiar la formula del Excel: en filas vacias arroja 126 (fecha nula = 1900).';
COMMENT ON COLUMN SERV_MED_BITACORA_EVENTO.FECHA_INGRESO IS
  'La antiguedad se calcula desde aqui, NO se copia del Excel: viene como formula y en filas vacias da 126 anios.';
COMMENT ON COLUMN SERV_MED_BITACORA_EVENTO.ESTATUS IS
  'VIGENTE / BAJA / LABORANDO / ALTA. Filtro principal de los tableros de seguimiento ("cuantos estan en control HOY").';

BEGIN EXECUTE IMMEDIATE
  'CREATE INDEX SERV_MED_IX_BITACORA_ESTATUS ON SERV_MED_BITACORA_EVENTO (ESTATUS)';
EXCEPTION WHEN OTHERS THEN IF SQLCODE NOT IN (-955,-1408) THEN RAISE; END IF; END;
/


PROMPT
PROMPT ##########################################################################
PROMPT #  5 de 6  ·  SERV_MED_BITACORA_ESTADO_MES
PROMPT #  La serie mensual de cada persona: lo que el carnet de cronicos pone
PROMPT #  como doce columnas ENE..DIC con el estado de ese mes, y lo que CAPA
PROMPT #  lleva como citas programadas contra asistencia y faltas.
PROMPT #  Doce estados por persona no son doce atributos, son una serie.
PROMPT ##########################################################################
BEGIN
  EXECUTE IMMEDIATE q'[
    CREATE TABLE SERV_MED_BITACORA_ESTADO_MES (
      ID            NUMBER GENERATED BY DEFAULT AS IDENTITY
                    CONSTRAINT SERV_MED_PK_BITACORA_EST_MES PRIMARY KEY,
      EVENTO_ID     NUMBER       NOT NULL,
      ANIO          NUMBER(4)    NOT NULL,
      MES           NUMBER(2)    NOT NULL,
      ESTADO        VARCHAR2(40),
      CITAS_PROG    NUMBER,
      CITAS_ASIST   NUMBER,
      CITAS_FALTA   NUMBER,
      CREATED_AT    TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL,
      CONSTRAINT SERV_MED_FK_EST_MES_EVENTO
        FOREIGN KEY (EVENTO_ID) REFERENCES SERV_MED_BITACORA_EVENTO (ID)
    )
  ]';
EXCEPTION WHEN OTHERS THEN IF SQLCODE NOT IN (-955) THEN RAISE; END IF; END;
/

COMMENT ON TABLE SERV_MED_BITACORA_ESTADO_MES IS
  'Seguimiento longitudinal: estado de cada persona mes por mes (carnet de cronicos, CAPA). Permite contestar "cuantos diabeticos estuvieron en control en marzo" sin pivotear.';
COMMENT ON COLUMN SERV_MED_BITACORA_ESTADO_MES.ESTADO IS
  'Codigos del carnet: ING inicia · VC vigente con seguimiento · VS vigente sin seguimiento · BAJA · ALTA · CAM cambio de predio · INC incapacidad. Se traducen con SERV_MED_BITACORA_NOMENCL.';

BEGIN EXECUTE IMMEDIATE
  'CREATE UNIQUE INDEX SERV_MED_UX_EST_MES ON SERV_MED_BITACORA_ESTADO_MES (EVENTO_ID, ANIO, MES)';
EXCEPTION WHEN OTHERS THEN IF SQLCODE NOT IN (-955,-1408) THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE
  'CREATE INDEX SERV_MED_IX_EST_MES_EST ON SERV_MED_BITACORA_ESTADO_MES (ESTADO, ANIO, MES)';
EXCEPTION WHEN OTHERS THEN IF SQLCODE NOT IN (-955,-1408) THEN RAISE; END IF; END;
/


PROMPT
PROMPT ##########################################################################
PROMPT #  6 de 6a ·  SERV_MED_BITACORA_METRICA
PROMPT #  Conteos que NO cuelgan de una persona:
PROMPT #    1. PRODUCTIVIDAD, que solo trae "accion x mes" (papanicolau 81,
PROMPT #       antigeno prostatico 40, VIH 5...) sin detalle nominal.
PROMPT #    2. Las hojas CONCENTRADO / GENERAL de los otros reportes, tal cual,
PROMPT #       para contrastar contra lo que el cargador conto.
PROMPT ##########################################################################
BEGIN
  EXECUTE IMMEDIATE q'[
    CREATE TABLE SERV_MED_BITACORA_METRICA (
      ID            NUMBER GENERATED BY DEFAULT AS IDENTITY
                    CONSTRAINT SERV_MED_PK_BITACORA_METRICA PRIMARY KEY,
      LOTE_ID       NUMBER       NOT NULL,
      FAMILIA       VARCHAR2(40) NOT NULL,
      ORIGEN        VARCHAR2(20) NOT NULL,
      PREDIO        VARCHAR2(120),
      ANIO          NUMBER(4),
      MES           NUMBER(2),
      CONCEPTO      VARCHAR2(200) NOT NULL,
      SUBCONCEPTO   VARCHAR2(200),
      VALOR         NUMBER,
      CREATED_AT    TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL,
      CONSTRAINT SERV_MED_FK_METRICA_LOTE
        FOREIGN KEY (LOTE_ID) REFERENCES SERV_MED_BITACORA_LOTE (ID)
    )
  ]';
EXCEPTION WHEN OTHERS THEN IF SQLCODE NOT IN (-955) THEN RAISE; END IF; END;
/

COMMENT ON TABLE SERV_MED_BITACORA_METRICA IS
  'Conteos sin persona: productividad preventiva y las hojas CONCENTRADO/GENERAL de los reportes de seguimiento. CONCEPTO es texto libre para que un reporte nuevo no pida DDL.';
COMMENT ON COLUMN SERV_MED_BITACORA_METRICA.ORIGEN IS
  'DETALLE = contado por el cargador. CONCENTRADO = copiado del resumen del Excel. Comparar los dos es el cuadre.';

BEGIN EXECUTE IMMEDIATE
  'CREATE UNIQUE INDEX SERV_MED_UX_METRICA ON SERV_MED_BITACORA_METRICA (LOTE_ID, ORIGEN, PREDIO, ANIO, MES, CONCEPTO, SUBCONCEPTO)';
EXCEPTION WHEN OTHERS THEN IF SQLCODE NOT IN (-955,-1408) THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE
  'CREATE INDEX SERV_MED_IX_METRICA_CONCEPTO ON SERV_MED_BITACORA_METRICA (FAMILIA, CONCEPTO, ANIO, MES)';
EXCEPTION WHEN OTHERS THEN IF SQLCODE NOT IN (-955,-1408) THEN RAISE; END IF; END;
/


PROMPT
PROMPT ##########################################################################
PROMPT #  6 de 6b ·  SERV_MED_BITACORA_NOMENCL
PROMPT #  Los diccionarios que estos archivos traen en su hoja NOMENCLATURA.
PROMPT #  Sin ellos la grafica muestra siglas:
PROMPT #    DM = Diabetes Mellitus · HAS = Hipertension · OBE 2/3/MOR = Obesidad
PROMPT #    DISLI = Dislipidemia · el prefijo PB significa "probable"
PROMPT #    VC = Vigente Con seguimiento · ATSL = acontecimiento traumatico
PROMPT #  Se leen del propio Excel: si agregan un codigo, entra en la siguiente
PROMPT #  corrida sin tocar nada.
PROMPT ##########################################################################
BEGIN
  EXECUTE IMMEDIATE q'[
    CREATE TABLE SERV_MED_BITACORA_NOMENCL (
      ID             NUMBER GENERATED BY DEFAULT AS IDENTITY
                     CONSTRAINT SERV_MED_PK_BITACORA_NOMENCL PRIMARY KEY,
      TIPO           VARCHAR2(40)  NOT NULL,
      CODIGO         VARCHAR2(40)  NOT NULL,
      SIGNIFICADO    VARCHAR2(400) NOT NULL,
      ORIGEN_ARCHIVO VARCHAR2(400),
      CREATED_AT     TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL
    )
  ]';
EXCEPTION WHEN OTHERS THEN IF SQLCODE NOT IN (-955) THEN RAISE; END IF; END;
/

COMMENT ON TABLE SERV_MED_BITACORA_NOMENCL IS
  'Diccionario de codigos leido de la hoja NOMENCLATURA de cada Excel. Traduce siglas (DM, VC, ATSL) para las graficas.';

BEGIN EXECUTE IMMEDIATE
  'CREATE UNIQUE INDEX SERV_MED_UX_NOMENCL ON SERV_MED_BITACORA_NOMENCL (TIPO, CODIGO)';
EXCEPTION WHEN OTHERS THEN IF SQLCODE NOT IN (-955,-1408) THEN RAISE; END IF; END;
/


PROMPT
PROMPT ##########################################################################
PROMPT #  VERIFICACION  --  solo lectura
PROMPT ##########################################################################

PROMPT
PROMPT -- Las 6 tablas (esperado: 6 en VALID)
SELECT table_name, status FROM user_tables
 WHERE table_name LIKE 'SERV_MED_BITACORA%' ORDER BY table_name;

PROMPT
PROMPT -- Las 4 columnas que entraron por ALTER (esperado: 4, todas nullable = Y)
SELECT column_name, data_type, data_length, nullable
  FROM user_tab_columns
 WHERE table_name = 'SERV_MED_BITACORA_EVENTO'
   AND column_name IN ('EDAD','FECHA_NACIMIENTO','FECHA_INGRESO','ESTATUS')
 ORDER BY column_name;

PROMPT
PROMPT -- Indices por tabla (esperado: 21 en total)
SELECT table_name, COUNT(*) AS indices,
       SUM(CASE WHEN uniqueness = 'UNIQUE' THEN 1 ELSE 0 END) AS unicos
  FROM user_indexes
 WHERE table_name LIKE 'SERV_MED_BITACORA%'
 GROUP BY table_name ORDER BY table_name;

PROMPT
PROMPT -- Los 3 indices UNIQUE explicitos que sostienen la idempotencia
PROMPT -- (si falta alguno, la carga puede duplicar sin avisar)
SELECT index_name, table_name, uniqueness FROM user_indexes
 WHERE index_name IN ('SERV_MED_UX_BITACORA_LOTE_AR',
                      'SERV_MED_UX_BITACORA_ORIGEN',
                      'SERV_MED_UX_EST_MES',
                      'SERV_MED_UX_METRICA',
                      'SERV_MED_UX_NOMENCL')
 ORDER BY index_name;

PROMPT
PROMPT -- Llaves primarias y foraneas (esperado: 6 P y 4 R)
SELECT constraint_type, COUNT(*) AS cuantas FROM user_constraints
 WHERE table_name LIKE 'SERV_MED_BITACORA%'
 GROUP BY constraint_type ORDER BY constraint_type;

PROMPT
PROMPT -- Que las 6 esten vacias antes de la primera carga
SELECT 'LOTE' t, COUNT(*) filas FROM SERV_MED_BITACORA_LOTE
UNION ALL SELECT 'EVENTO',     COUNT(*) FROM SERV_MED_BITACORA_EVENTO
UNION ALL SELECT 'ATRIBUTO',   COUNT(*) FROM SERV_MED_BITACORA_ATRIBUTO
UNION ALL SELECT 'ESTADO_MES', COUNT(*) FROM SERV_MED_BITACORA_ESTADO_MES
UNION ALL SELECT 'METRICA',    COUNT(*) FROM SERV_MED_BITACORA_METRICA
UNION ALL SELECT 'NOMENCL',    COUNT(*) FROM SERV_MED_BITACORA_NOMENCL;

-- ============================================================================
-- LO QUE TIENE QUE SALIR
--
--   6 tablas en VALID
--   4 columnas nuevas en SERV_MED_BITACORA_EVENTO, nullable = Y
--  21 indices en total:
--       ATRIBUTO    3   (PK + 2 IX)
--       ESTADO_MES  3   (PK + UX + IX)
--       EVENTO      7   (PK + UX + 5 IX)
--       LOTE        3   (PK + UX + 1 que Oracle crea para el CLOB MENSAJES)
--       METRICA     3   (PK + UX + IX)
--       NOMENCL     2   (PK + UX)
--   6 constraints tipo P y 4 tipo R  (ademas saldran varios tipo C: los NOT NULL)
--   Las 6 tablas en 0 filas
--
-- LO MAS IMPORTANTE DE REVISAR son los UNIQUE explicitos. Como en Oracle los
-- nombres de indice son unicos en TODO el esquema, si ya existiera uno con el
-- mismo nombre colgado de otra tabla el guard -955 lo saltaria en silencio y
-- la tabla se quedaria SIN su indice unico: creerias tener idempotencia sin
-- tenerla. Por eso la consulta los lista uno por uno.
-- ============================================================================
