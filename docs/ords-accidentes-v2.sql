-- =============================================================================
-- ACCIDENTES v2 — clones de los WS con DIAS (13-oct-2026)
-- =============================================================================
-- Destino : ORDS http://10.249.249.3/biows/ords/security, modulo "Servcio"
--           (base BIOMETRICO, donde vive SERV_MED_ACCIDENTE).
-- Requisito: haber corrido antes docs/script-prod/accidente-13-dias.sql (columna DIAS).
--
-- Por que clones y no modificar: regla del proyecto, los WS productivos no se tocan.
-- Servcio/accidente y Servcio/consulta_accidente quedan IGUAL; el portal pasa a usar:
--   Servcio/accidente_v2           POST  alta (igual al original + DIAS)
--   Servcio/consulta_accidente_v2  POST  lista por NSS (igual al original + dias)
-- Diferencias contra docs/ords-accidentes.sql marcadas con  -- [v2]
--
-- Como publicar: SQL Developer > conexion > RESTful Services > modulo "Servcio" >
-- New Template "accidente_v2" > New Handler POST, Source Type PL/SQL, pegar BLOQUE 1.
-- Igual para "consulta_accidente_v2" con el BLOQUE 2.
-- =============================================================================


-- =============================================================================
-- BLOQUE 1: POST /Servcio/accidente_v2 (alta)
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
KNNS VARCHAR2(13);
kfechaaccidente varchar2(20);
ktiporiesgo varchar2(20);
kcausart varchar2(30);
kdiagnostico varchar2(500);
ksdi number;
kstatuscalificacion varchar2(30);
kcosto number;
kobservaciones varchar2(500);
kidusuario varchar2(50);
knombreusuario varchar2(100);
kdias number;                                                                  -- [v2]

begin
kclob:= utl_raw.cast_to_varchar2(:body);
apex_json.parse(j,kclob);

knns                := apex_json.get_varchar2(p_path=>'NSS',p_values=>j);
kfechaaccidente     := apex_json.get_varchar2(p_path=>'FECHA_ACCIDENTE',p_values=>j);
ktiporiesgo         := apex_json.get_varchar2(p_path=>'TIPO_RIESGO',p_values=>j);
kcausart            := apex_json.get_varchar2(p_path=>'CAUSA_RT',p_values=>j);
kdiagnostico        := apex_json.get_varchar2(p_path=>'DIAGNOSTICO',p_values=>j);
ksdi                := apex_json.get_number(p_path=>'SDI',p_values=>j);
kstatuscalificacion := apex_json.get_varchar2(p_path=>'STATUS_CALIFICACION',p_values=>j);
kcosto              := apex_json.get_number(p_path=>'COSTO',p_values=>j);
kobservaciones      := apex_json.get_varchar2(p_path=>'OBSERVACIONES',p_values=>j);
kidusuario          := apex_json.get_varchar2(p_path=>'USUARIO_ID',p_values=>j);
knombreusuario      := apex_json.get_varchar2(p_path=>'NOMBRE_USUARIO',p_values=>j);
kdias               := apex_json.get_number(p_path=>'DIAS',p_values=>j);       -- [v2]

insert into SERV_MED_ACCIDENTE(
  NSS, FECHA_ACCIDENTE, TIPO_RIESGO, CAUSA_RT, DIAGNOSTICO, SDI,
  STATUS_CALIFICACION, COSTO, OBSERVACIONES, ID_USUARIO, NOMBRE_USUARIO, DIAS)  -- [v2]
values (knns, to_date(kfechaaccidente,'DD/MM/YYYY'), ktiporiesgo, kcausart, kdiagnostico, ksdi,
  kstatuscalificacion, kcosto, kobservaciones, kidusuario, knombreusuario, kdias); -- [v2]

commit;

APEX_JSON.initialize_clob_output;
   APEX_JSON.open_OBJECT;
    APEX_JSON.open_array('Datos');
   APEX_JSON.open_OBJECT;
      APEX_JSON.write('Proceso','Registro de accidente procesado correctamente');
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
-- BLOQUE 2: POST /Servcio/consulta_accidente_v2 (lista por NSS)
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
KNNS VARCHAR2(13);

begin
kclob:= utl_raw.cast_to_varchar2(:body);
apex_json.parse(j,kclob);

knns  := apex_json.get_varchar2(p_path=>'NSS',p_values=>j);

select COUNT(*) into KEXISTE from SERV_MED_ACCIDENTE where NSS=knns;

APEX_JSON.initialize_clob_output;
APEX_JSON.OPEN_OBJECT;

IF KEXISTE > 0 THEN
APEX_JSON.open_array('Datos');
for i in (select
REG_ID, FECHA_REGISTRO, FECHA_ACCIDENTE, TIPO_RIESGO, CAUSA_RT, DIAGNOSTICO,
SDI, STATUS_CALIFICACION, COSTO, OBSERVACIONES, NOMBRE_USUARIO, DIAS            -- [v2]
from SERV_MED_ACCIDENTE where NSS=knns order by FECHA_ACCIDENTE desc)
loop
  APEX_JSON.open_OBJECT;
    APEX_JSON.WRITE('id_registro',coalesce(i.reg_id,0));
    APEX_JSON.WRITE('fecha_registro',coalesce(to_char(i.fecha_registro,'DD/MM/YYYY'),'0'));
    APEX_JSON.WRITE('fecha_accidente',coalesce(to_char(i.fecha_accidente,'DD/MM/YYYY'),'0'));
    APEX_JSON.WRITE('tipo_riesgo',coalesce(i.tipo_riesgo,'0'));
    APEX_JSON.WRITE('causa_rt',coalesce(i.causa_rt,'0'));
    APEX_JSON.WRITE('diagnostico',coalesce(i.diagnostico,'0'));
    APEX_JSON.WRITE('sdi',coalesce(i.sdi,0));
    APEX_JSON.WRITE('status_calificacion',coalesce(i.status_calificacion,'0'));
    APEX_JSON.WRITE('costo',coalesce(i.costo,0));
    APEX_JSON.WRITE('observaciones',coalesce(i.observaciones,'0'));
    APEX_JSON.WRITE('usuario',coalesce(i.nombre_usuario,'0'));
    -- [v2] sin coalesce: null = no se capturo (los registros anteriores a v2), distinto de 0 dias
    APEX_JSON.WRITE('dias',i.dias, p_write_null => true);
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
               APEX_JSON.write('Proceso','consulta de accidentes por NSS');
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
