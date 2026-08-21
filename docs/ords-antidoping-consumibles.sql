-- =============================================================================
-- CONSUMIBLES DE ANTIDOPING (inventario) — handlers ORDS
-- =============================================================================
-- Destino : misma instancia ORDS que security/Servcio/* (10.249.249.3) — NO la
--           BD local del proyecto Java ni ONEWMS_QA. Sin DDL: la tabla
--           SERV_MED_ANTIDOPING_INVENTARIO ya existe (docs/ords-antidoping.sql
--           BLOQUE 2, aplicada 14 de agosto de 2026) - sigue vacia, sin handler.
-- Fecha   : 18 de agosto de 2026
-- Alcance : NO esta asociado a NSS/empleado - es inventario de kits de prueba
--           por PREDIO y mes, separado del resultado individual de cada
--           antidoping (SERV_MED_ANTIDOPING_RESULTADO). Identificado sin
--           bloqueador de negocio (docs/plan-tareas-concretas.html, 18 de
--           agosto) - revision de salud-ocupacional-v2
--           (AntidopingConsumibles.jsx).
--
-- Como aplicar (SQL Developer > conexion > RESTful Services):
--   1. Dentro del modulo "Servcio" existente > New Template.
--   2. URI Template: "consumibles" (alta) / "consulta_consumibles" (lista).
--   3. New Handler > Method: POST, Source Type: PL/SQL. Pegar BLOQUE 1 / BLOQUE 2.
-- =============================================================================


-- =============================================================================
-- BLOQUE 1: HANDLER ORDS — POST /Servcio/consumibles (alta)
-- Mismo patron que security/Servcio/antidoping (docs/ords-antidoping.sql BLOQUE 3).
-- Pegar completo como "Source" del handler POST.
-- =============================================================================
declare

l_response_body_clob  clob ;
l_response_length     NUMBER;
l_response_buffer     VARCHAR2(32767);
l_response_chunk_size BINARY_INTEGER := 32000;
l_offset              INTEGER := 1;
j apex_json.t_values;
kjson clob;
kclob clob;
kpredio varchar2(50);
kanio number;
kmes number;
kcantidadinicial number;
kentregamensual number;
kconsumomensual number;
kobservaciones varchar2(500);

begin
kclob:= utl_raw.cast_to_varchar2(:body);
apex_json.parse(j,kclob);

kpredio          := apex_json.get_varchar2(p_path=>'PREDIO',p_values=>j);
kanio            := apex_json.get_number(p_path=>'ANIO',p_values=>j);
kmes             := apex_json.get_number(p_path=>'MES',p_values=>j);
kcantidadinicial := apex_json.get_number(p_path=>'CANTIDAD_INICIAL',p_values=>j);
kentregamensual  := apex_json.get_number(p_path=>'ENTREGA_MENSUAL',p_values=>j);
kconsumomensual  := apex_json.get_number(p_path=>'CONSUMO_MENSUAL',p_values=>j);
kobservaciones   := apex_json.get_varchar2(p_path=>'OBSERVACIONES',p_values=>j);

insert into SERV_MED_ANTIDOPING_INVENTARIO(
  PREDIO, ANIO, MES, CANTIDAD_INICIAL, ENTREGA_MENSUAL, CONSUMO_MENSUAL, OBSERVACIONES)
values (kpredio, kanio, kmes, kcantidadinicial, kentregamensual, kconsumomensual, kobservaciones);

commit;

APEX_JSON.initialize_clob_output;
   APEX_JSON.open_OBJECT;
    APEX_JSON.open_array('Datos');
   APEX_JSON.open_OBJECT;
      APEX_JSON.write('Proceso','Registro de consumibles procesado correctamente');
      APEX_JSON.write('Estado',100);
      APEX_JSON.write('Mensaje','Insercion correcta');
      APEX_JSON.write('Data','Proceso correcto');
   APEX_JSON.close_OBJECT;
  APEX_JSON.close_array;
 APEX_JSON.close_OBJECT;
kjson := APEX_JSON.get_clob_output;
APEX_JSON.free_output;

l_response_body_clob := kjson;
dbms_lob.open(l_response_body_clob, dbms_lob.lob_readonly);
l_response_length := dbms_lob.getlength (l_response_body_clob);

WHILE (l_response_length > 0)
 LOOP
    dbms_lob.read(l_response_body_clob, l_response_chunk_size, l_offset, l_response_buffer);
     htp.prn(l_response_buffer);
     l_offset := l_offset + l_response_chunk_size;
     l_response_length := l_response_length - l_response_chunk_size;
 END LOOP;

  dbms_lob.close(l_response_body_clob);

end;


-- =============================================================================
-- BLOQUE 2: HANDLER ORDS — POST /Servcio/consulta_consumibles (lista)
-- PREDIO es OPCIONAL: si viene vacio/null, regresa TODOS los registros
-- (son pocos predios, distinto del patron NSS-obligatorio de otros consulta_*).
-- Pegar completo como "Source" del handler POST.
-- =============================================================================
declare

l_response_body_clob  clob ;
l_response_length     NUMBER;
l_response_buffer     VARCHAR2(32767);
l_response_chunk_size BINARY_INTEGER := 32000;
l_offset              INTEGER := 1;
j apex_json.t_values;
kjson clob;
kclob clob;
kdata varchar2(250);
KEXISTE NUMBER;
kpredio varchar2(50);

begin
kclob:= utl_raw.cast_to_varchar2(:body);
apex_json.parse(j,kclob);

kpredio  := apex_json.get_varchar2(p_path=>'PREDIO',p_values=>j);

select COUNT(*) into KEXISTE from SERV_MED_ANTIDOPING_INVENTARIO
  where (kpredio is null or PREDIO = kpredio);

APEX_JSON.initialize_clob_output;
APEX_JSON.OPEN_OBJECT;

IF KEXISTE > 0 THEN
APEX_JSON.open_array('Datos');
for i in (select
REG_ID, PREDIO, ANIO, MES, CANTIDAD_INICIAL, ENTREGA_MENSUAL, CONSUMO_MENSUAL,
OBSERVACIONES, FECHA_REGISTRO
from SERV_MED_ANTIDOPING_INVENTARIO
where (kpredio is null or PREDIO = kpredio)
order by ANIO desc, MES desc, PREDIO)
loop
  APEX_JSON.open_OBJECT;
    APEX_JSON.WRITE('id_registro',coalesce(i.reg_id,0));
    APEX_JSON.WRITE('predio',coalesce(i.predio,'0'));
    APEX_JSON.WRITE('anio',coalesce(i.anio,0));
    APEX_JSON.WRITE('mes',coalesce(i.mes,0));
    APEX_JSON.WRITE('cantidad_inicial',coalesce(i.cantidad_inicial,0));
    APEX_JSON.WRITE('entrega_mensual',coalesce(i.entrega_mensual,0));
    APEX_JSON.WRITE('consumo_mensual',coalesce(i.consumo_mensual,0));
    APEX_JSON.WRITE('observaciones',coalesce(i.observaciones,'0'));
    APEX_JSON.WRITE('fecha_registro',coalesce(to_char(i.fecha_registro,'DD/MM/YYYY'),'0'));
  APEX_JSON.close_OBJECT;
end loop;
   APEX_JSON.CLOSE_array;
  APEX_JSON.close_OBJECT;
       kjson := APEX_JSON.get_clob_output;
    APEX_JSON.free_output;

ELSE
   APEX_JSON.initialize_clob_output;
            APEX_JSON.open_OBJECT;
             APEX_JSON.open_array('Datos');
            APEX_JSON.open_OBJECT;
               APEX_JSON.write('Proceso','consulta de consumibles');
               APEX_JSON.write('Estado',-100);
               APEX_JSON.write('Mensaje','No se tienen datos en la consulta');
               APEX_JSON.write('Data',kdata);
            APEX_JSON.close_OBJECT;
           APEX_JSON.close_array;
          APEX_JSON.close_OBJECT;
              kjson := APEX_JSON.get_clob_output;
    APEX_JSON.free_output;
END IF;

l_response_body_clob := kjson;
dbms_lob.open(l_response_body_clob, dbms_lob.lob_readonly);
l_response_length := dbms_lob.getlength (l_response_body_clob);

WHILE (l_response_length > 0)
 LOOP
    dbms_lob.read(l_response_body_clob, l_response_chunk_size, l_offset, l_response_buffer);
     htp.prn(l_response_buffer);
     l_offset := l_offset + l_response_chunk_size;
     l_response_length := l_response_length - l_response_chunk_size;
 END LOOP;

  dbms_lob.close(l_response_body_clob);

end;


-- =============================================================================
-- NOTAS
-- =============================================================================
-- [NOTA-1] "Estado":100 replica el mismo patron que docs/ords-antidoping.sql
--   (alta de resultado) - mismo autor/tabla hermana, sin evidencia para
--   desviarse. Si al probar en vivo el WS real usa otra convencion, ajustar.
-- [NOTA-2] PREDIO es texto libre, sin catalogo cerrado - no existe un catalogo
--   de predios confirmado en ningun WS (ver hallazgo "no existe campo de
--   sitio/predio" en bio_empleado, docs/plan-tareas-concretas.html 18-ago).
--   Se captura tal cual lo escriba el usuario, igual que el resto del
--   esquema SERV_MED_* (sin CHECK constraint).
-- [NOTA-3] No hay UNIQUE(PREDIO,ANIO,MES) a proposito - si alguien registra
--   dos veces el mismo mes/predio, ambas filas quedan (mismo criterio de
--   "aterrizaje fiel, sin deduplicar" ya usado en U09). El listado ordena
--   por ANIO/MES/PREDIO, no colapsa duplicados.
-- =============================================================================
