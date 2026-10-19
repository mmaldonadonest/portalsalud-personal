-- ============================================================================
-- CARGA DE EXCEL 2026 -- DDL para los reportes de SEGUIMIENTO
-- (carnet de cronicos, CAPA/adicciones, NOM-035, seguimiento medico especial,
--  productividad preventiva)
--
-- BASE: la del PORTAL (JDBC). En produccion eso es BIOMETRICO. NO es ORDS.
-- REQUIERE: excel-01-ddl-bitacora.sql ya aplicado (estas tablas cuelgan de
--           SERV_MED_BITACORA_EVENTO).
--
-- QUE HACE:
--   1. Agrega 4 columnas a SERV_MED_BITACORA_EVENTO (tabla VACIA, ALTER
--      instantaneo y sin riesgo).
--   2. Crea 3 tablas nuevas + 6 indices.
-- QUE NO HACE: ningun DROP, TRUNCATE, GRANT ni UPDATE. No toca ninguna otra
--   tabla de BIOMETRICO.
--
-- POR QUE hacen falta objetos nuevos y no alcanza con EVENTO + ATRIBUTO:
--   a) Las 9 familias cuentan EVENTOS (una atencion, un examen). Estos
--      reportes siguen PERSONAS EN EL TIEMPO: el carnet trae el estado de
--      cada persona mes por mes (VC, VS, BAJA, ALTA, INC). Doce estados por
--      persona no son doce atributos, son una serie.
--   b) PRODUCTIVIDAD no tiene detalle por persona: solo cuenta acciones
--      (papanicolau, antigeno prostatico, VIH...) por mes y predio. No hay
--      evento al que colgarle un atributo.
--   c) Estos archivos traen diccionarios de codigos en una hoja
--      NOMENCLATURA (DM = Diabetes Mellitus, VC = Vigente Con seguimiento).
--      Sin ellos la grafica muestra siglas.
--
-- Es idempotente: guards -955 (ya existe), -1408 (columna ya indexada),
-- -1430 (columna ya agregada).
-- ============================================================================

SET SERVEROUTPUT ON
SET LINESIZE 200

-- ----------------------------------------------------------------------
-- PRE-CHECK  (solo lectura)
-- ----------------------------------------------------------------------
DECLARE
  v_n NUMBER;
BEGIN
  DBMS_OUTPUT.PUT_LINE('=== PRE-CHECK ===');
  DBMS_OUTPUT.PUT_LINE('Usuario: ' || USER ||
                       '  Base: ' || SYS_CONTEXT('USERENV','DB_NAME') ||
                       '  PDB: '  || SYS_CONTEXT('USERENV','CON_NAME'));

  SELECT COUNT(*) INTO v_n FROM user_tables
   WHERE table_name = 'SERV_MED_BITACORA_EVENTO';
  IF v_n = 0 THEN
    DBMS_OUTPUT.PUT_LINE('ERROR: falta SERV_MED_BITACORA_EVENTO.');
    DBMS_OUTPUT.PUT_LINE('Correr primero excel-01-ddl-bitacora.sql. NO SEGUIR.');
    RETURN;
  END IF;

  -- El ALTER es seguro solo si la tabla esta vacia
  EXECUTE IMMEDIATE 'SELECT COUNT(*) FROM SERV_MED_BITACORA_EVENTO' INTO v_n;
  IF v_n = 0 THEN
    DBMS_OUTPUT.PUT_LINE('SERV_MED_BITACORA_EVENTO esta VACIA: el ALTER es instantaneo.');
  ELSE
    DBMS_OUTPUT.PUT_LINE('AVISO: SERV_MED_BITACORA_EVENTO ya tiene ' || v_n || ' filas.');
    DBMS_OUTPUT.PUT_LINE('Agregar columnas nullable sigue siendo seguro (no reescribe filas),');
    DBMS_OUTPUT.PUT_LINE('pero quedaran en NULL para lo ya cargado.');
  END IF;

  SELECT COUNT(*) INTO v_n FROM user_objects
   WHERE object_name IN ('SERV_MED_BITACORA_ESTADO_MES',
                         'SERV_MED_BITACORA_METRICA',
                         'SERV_MED_BITACORA_NOMENCL');
  DBMS_OUTPUT.PUT_LINE('De los 3 objetos nuevos ya existen: ' || v_n || ' (esperado 0).');

  SELECT COUNT(*) INTO v_n FROM (
    SELECT 'SERV_MED_BITACORA_ESTADO_MES' n FROM dual UNION ALL
    SELECT 'SERV_MED_BITACORA_METRICA'      FROM dual UNION ALL
    SELECT 'SERV_MED_BITACORA_NOMENCL'      FROM dual UNION ALL
    SELECT 'SERV_MED_PK_BITACORA_EST_MES'   FROM dual UNION ALL
    SELECT 'SERV_MED_PK_BITACORA_METRICA'   FROM dual UNION ALL
    SELECT 'SERV_MED_PK_BITACORA_NOMENCL'   FROM dual UNION ALL
    SELECT 'SERV_MED_FK_EST_MES_EVENTO'     FROM dual UNION ALL
    SELECT 'SERV_MED_UX_EST_MES'            FROM dual UNION ALL
    SELECT 'SERV_MED_IX_EST_MES_EST'        FROM dual UNION ALL
    SELECT 'SERV_MED_UX_METRICA'            FROM dual UNION ALL
    SELECT 'SERV_MED_IX_METRICA_CONCEPTO'   FROM dual UNION ALL
    SELECT 'SERV_MED_UX_NOMENCL'            FROM dual
  ) WHERE LENGTH(n) > 30;
  IF v_n > 0 THEN
    DBMS_OUTPUT.PUT_LINE('ERROR: ' || v_n || ' nombres pasan de 30 caracteres.');
  ELSE
    DBMS_OUTPUT.PUT_LINE('Todos los nombres caben en 30 caracteres: OK.');
  END IF;
  DBMS_OUTPUT.PUT_LINE('=== FIN PRE-CHECK ===');
END;
/


-- ======================================================================
-- PARTE 1: 4 columnas mas en SERV_MED_BITACORA_EVENTO
--
-- No son parches: son dimensiones que varios de estos reportes traen y
-- que las graficas usan para filtrar. Por eso van como columnas y no
-- como atributos EAV (un atributo obligaria a un JOIN en cada consulta).
--
--   EDAD               las 9 familias solo dan el RANGO ("18 - 25"); estos
--                      reportes dan la edad exacta. Se conservan las dos:
--                      cuando hay EDAD se deriva el rango, no al reves.
--   FECHA_NACIMIENTO   origen real de la edad, permite recalcularla.
--   FECHA_INGRESO      la traen seguimiento especial y CAPA; con ella sale
--                      la antiguedad sin confiar en la formula del Excel.
--   ESTATUS            VIGENTE / BAJA / LABORANDO / ALTA. Aparece en los
--                      cuatro reportes de seguimiento y es el filtro
--                      principal ("cuantos estan en control HOY").
--
-- OJO con la antiguedad: NO se copia del Excel. Varios archivos la traen
-- como formula y en las filas vacias arroja 126 anios (fecha nula = 1900).
-- Se calcula en la consulta desde FECHA_INGRESO.
-- ======================================================================
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
  'Edad exacta. Solo la traen los reportes de seguimiento; las 9 familias dan solo RANGO_EDAD. NUNCA copiar la formula del Excel: en filas vacias da 126.';
COMMENT ON COLUMN SERV_MED_BITACORA_EVENTO.ESTATUS IS
  'VIGENTE / BAJA / LABORANDO / ALTA. Filtro principal de los tableros de seguimiento.';

BEGIN EXECUTE IMMEDIATE
  'CREATE INDEX SERV_MED_IX_BITACORA_ESTATUS ON SERV_MED_BITACORA_EVENTO (ESTATUS)';
EXCEPTION WHEN OTHERS THEN IF SQLCODE NOT IN (-955,-1408) THEN RAISE; END IF; END;
/


-- ======================================================================
-- PARTE 2 -- TABLA 1 de 3:  SERV_MED_BITACORA_ESTADO_MES
--
-- La serie mensual de cada persona. Es lo que el carnet de cronicos pone
-- como doce columnas (ENE..DIC) con el estado de ese mes, y lo que CAPA
-- lleva como citas programadas contra asistencia y faltas.
--
-- Un renglon por persona y mes. Asi la grafica puede contestar "cuantos
-- diabeticos estuvieron en control en marzo" sin pivotear nada.
-- ======================================================================
BEGIN
  EXECUTE IMMEDIATE q'[
    CREATE TABLE SERV_MED_BITACORA_ESTADO_MES (
      ID            NUMBER GENERATED BY DEFAULT AS IDENTITY
                    CONSTRAINT SERV_MED_PK_BITACORA_EST_MES PRIMARY KEY,
      EVENTO_ID     NUMBER       NOT NULL,   -- la persona, en EVENTO
      ANIO          NUMBER(4)    NOT NULL,
      MES           NUMBER(2)    NOT NULL,
      ESTADO        VARCHAR2(40),            -- VC, VS, BAJA, ALTA, INC, CAM, ING
      -- Solo para CAPA: control de asistencia a citas
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
  'Seguimiento longitudinal: estado de cada persona mes por mes (carnet de cronicos, CAPA). Un renglon por persona y mes.';

BEGIN EXECUTE IMMEDIATE
  'CREATE UNIQUE INDEX SERV_MED_UX_EST_MES ON SERV_MED_BITACORA_ESTADO_MES (EVENTO_ID, ANIO, MES)';
EXCEPTION WHEN OTHERS THEN IF SQLCODE NOT IN (-955,-1408) THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE
  'CREATE INDEX SERV_MED_IX_EST_MES_EST ON SERV_MED_BITACORA_ESTADO_MES (ESTADO, ANIO, MES)';
EXCEPTION WHEN OTHERS THEN IF SQLCODE NOT IN (-955,-1408) THEN RAISE; END IF; END;
/


-- ======================================================================
-- PARTE 2 -- TABLA 2 de 3:  SERV_MED_BITACORA_METRICA
--
-- Conteos que NO cuelgan de una persona. Dos usos:
--
--   1. PRODUCTIVIDAD: el archivo solo trae "accion x mes" (papanicolau 81,
--      antigeno prostatico 40, VIH 5, uroanalisis 109...). No hay detalle
--      nominal al que colgar un atributo.
--   2. Las hojas CONCENTRADO / GENERAL de los otros reportes, guardadas
--      tal cual. Sirven de contraste: si el conteo del detalle cargado no
--      coincide con lo que el propio Excel resume, se ve aqui.
--
-- Deliberadamente generica: CONCEPTO es texto libre, no un catalogo. Asi
-- un reporte nuevo no necesita DDL.
-- ======================================================================
BEGIN
  EXECUTE IMMEDIATE q'[
    CREATE TABLE SERV_MED_BITACORA_METRICA (
      ID            NUMBER GENERATED BY DEFAULT AS IDENTITY
                    CONSTRAINT SERV_MED_PK_BITACORA_METRICA PRIMARY KEY,
      LOTE_ID       NUMBER       NOT NULL,
      FAMILIA       VARCHAR2(40) NOT NULL,   -- PRODUCTIVIDAD, CARNET, CAPA, ...
      ORIGEN        VARCHAR2(20) NOT NULL,   -- DETALLE o CONCENTRADO
      PREDIO        VARCHAR2(120),
      ANIO          NUMBER(4),
      MES           NUMBER(2),               -- NULL si el dato es anual
      CONCEPTO      VARCHAR2(200) NOT NULL,  -- PAPANICOLAU, PBA VIH, TOTAL DETECTADOS...
      SUBCONCEPTO   VARCHAR2(200),           -- FEM/MASC, LABORANDO/BAJA, ...
      VALOR         NUMBER,
      CREATED_AT    TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL,
      CONSTRAINT SERV_MED_FK_METRICA_LOTE
        FOREIGN KEY (LOTE_ID) REFERENCES SERV_MED_BITACORA_LOTE (ID)
    )
  ]';
EXCEPTION WHEN OTHERS THEN IF SQLCODE NOT IN (-955) THEN RAISE; END IF; END;
/

COMMENT ON TABLE SERV_MED_BITACORA_METRICA IS
  'Conteos sin persona: productividad preventiva y las hojas CONCENTRADO/GENERAL de los reportes de seguimiento.';
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


-- ======================================================================
-- PARTE 2 -- TABLA 3 de 3:  SERV_MED_BITACORA_NOMENCL
--
-- Los diccionarios de codigos que estos archivos traen en su hoja
-- NOMENCLATURA. Sin ellos la grafica muestra siglas:
--
--   Diagnostico:  DM = Diabetes Mellitus · HAS = Hipertension Arterial
--                 OBE 2 / OBE 3 / OBE MOR = Obesidad · DISLI = Dislipidemia
--                 el prefijo PB significa "probable" (PB DM, PB HAS)
--   Carnet:       ING = Inicia carnet · VC = Vigente Con seguimiento
--                 VS = Vigente Sin seguimiento · BAJA = Baja de Onest
--                 ALTA = Alta de carnet · CAM = Cambio de predio
--                 INC = Incapacidad
--   NOM-035:      ATSL = Acontecimiento traumatico · AS = Apoyo social
--
-- Se carga de la propia hoja del Excel, no se siembra a mano: si el
-- servicio medico agrega un codigo, entra con la siguiente corrida.
-- ======================================================================
BEGIN
  EXECUTE IMMEDIATE q'[
    CREATE TABLE SERV_MED_BITACORA_NOMENCL (
      ID            NUMBER GENERATED BY DEFAULT AS IDENTITY
                    CONSTRAINT SERV_MED_PK_BITACORA_NOMENCL PRIMARY KEY,
      TIPO          VARCHAR2(40)  NOT NULL,  -- DIAGNOSTICO, ESTADO_CARNET, NOM035, ...
      CODIGO        VARCHAR2(40)  NOT NULL,  -- DM, VC, ATSL
      SIGNIFICADO   VARCHAR2(400) NOT NULL,  -- Diabetes Mellitus
      ORIGEN_ARCHIVO VARCHAR2(400),
      CREATED_AT    TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL
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


-- ----------------------------------------------------------------------
-- VERIFICACION  (solo lectura)
-- ----------------------------------------------------------------------
PROMPT
PROMPT -- Las 4 columnas nuevas en EVENTO (esperado: 4 filas)
SELECT column_name, data_type, data_length, nullable
FROM   user_tab_columns
WHERE  table_name = 'SERV_MED_BITACORA_EVENTO'
  AND  column_name IN ('EDAD','FECHA_NACIMIENTO','FECHA_INGRESO','ESTATUS')
ORDER  BY column_name;

PROMPT
PROMPT -- Las 3 tablas nuevas (esperado: 3 VALID)
SELECT table_name, status FROM user_tables
WHERE  table_name IN ('SERV_MED_BITACORA_ESTADO_MES',
                      'SERV_MED_BITACORA_METRICA',
                      'SERV_MED_BITACORA_NOMENCL')
ORDER  BY table_name;

PROMPT
PROMPT -- Indices de las 3 nuevas + el de ESTATUS
SELECT index_name, table_name, uniqueness FROM user_indexes
WHERE  table_name IN ('SERV_MED_BITACORA_ESTADO_MES',
                      'SERV_MED_BITACORA_METRICA',
                      'SERV_MED_BITACORA_NOMENCL')
   OR  index_name = 'SERV_MED_IX_BITACORA_ESTATUS'
ORDER  BY table_name, index_name;

-- Esperado:
--   4 columnas nuevas en SERV_MED_BITACORA_EVENTO, todas nullable (Y)
--   3 tablas VALID
--  10 indices: ESTADO_MES 3 (PK + UX + IX) · METRICA 3 (PK + UX + IX)
--              NOMENCL 2 (PK + UX) · EVENTO 1 (IX de ESTATUS)
--              + 1 del LOB si alguna trae CLOB (ninguna lo trae aqui)
--              -> en la practica salen 9 en las 3 tablas nuevas + 1 en EVENTO
