-- =============================================================================
-- CONSULTA MÉDICA — WS agregado para dashboard (todas las NSS en un rango)
-- =============================================================================
-- Destino : instancia Oracle que respalda el WS ORDS real (security/Servcio/*,
--           http://10.249.249.3/biows/ords/security). Tabla TBL_SERV_CONSULTA_MEDICA
--           ya existe (legacy, confirmada contra docs/contextoWS.txt:1676-1701 y
--           1844-1866, WS Servcio/conuslta_medica_usuario ya conectado en Java).
--           NO hace falta DDL nuevo, solo el handler.
-- Fecha   : 17 de agosto de 2026
-- Modelo  : replica 1:1 el patron de security/Servcio/consulta_incapacidades_fecha
--           y consulta_accidentes_fecha (docs/ords-accidentes-dashboard.sql) - mismo
--           JOIN contra bio_empleado, mismo formato de request {fecha_inicial,
--           fecha_final} en dd/mm/yy, mismo manejo de "sin datos"/"fechas incorrectas".
--
-- Payload deliberadamente LIGERO: solo campos utiles para agregados de dashboard
-- (tipo_consulta, area_accidente, area_involucrada, causa) - NO incluye campos
-- clinicos sensibles (diagnostico, tratamiento, motivo_consulta, exploracion_fisica,
-- signos vitales) porque este endpoint es para KPIs agregados de todas las NSS, no
-- para el expediente de un paciente individual (ese ya existe: Servcio/consulta y
-- Servcio/conuslta_medica_usuario, ambos por NSS).
--
-- Como aplicar (SQL Developer > conexion > RESTful Services):
--   1. Clic derecho en el modulo "Servcio" existente > New Template.
--   2. URI Template: "consulta_medica_fecha".
--   3. Dentro del template > New Handler > Method: POST, Source Type: PL/SQL.
--   4. Pegar el bloque completo de abajo como "Source".
-- =============================================================================


-- =============================================================================
-- BLOQUE 1: HANDLER ORDS — POST /Servcio/consulta_medica_fecha
-- (lista de consultas medicas de TODAS las NSS en un rango de fechas, para el
-- dashboard de Morbilidad / Resumen General)
-- =============================================================================
declare

l_response_body_clob  clob;
l_response_length     NUMBER;
l_response_buffer     VARCHAR2(32767);
l_response_chunk_size BINARY_INTEGER := 32000;
l_offset              INTEGER := 1;
j apex_json.t_values;
kjson clob;
kclob clob;
kdata varchar2(250);
KEXISTE NUMBER;
kfechaini varchar2(20);
kfechafin varchar2(20);

begin
kclob := utl_raw.cast_to_varchar2(:body);
apex_json.parse(j,kclob);

kfechaini := apex_json.get_varchar2(p_path=>'fecha_inicial',p_values=>j);
kfechafin := apex_json.get_varchar2(p_path=>'fecha_final',p_values=>j);

if kfechaini is not null and kfechafin is not null then

  select count(*) into kexiste
  from TBL_SERV_CONSULTA_MEDICA a left join bio_empleado b on a.nss=b.emp_nss
  where trunc(a.fecha) >= to_date(kfechaini,'dd/mm/yy')
    and trunc(a.fecha) <= to_date(kfechafin,'dd/mm/yy');

  if kexiste > 0 then

    APEX_JSON.initialize_clob_output;
    APEX_JSON.OPEN_OBJECT;
    APEX_JSON.open_array('Datos');

    for i in (
      select
        a.REG_ID,
        a.FECHA,
        a.NSS,
        b.emp_nombre||' '||b.emp_apellido||' '||b.emp_apellido2 nombre,
        b.emp_rfc,
        b.emp_curp,
        a.TIPO_CONSULTA,
        a.AREA_DE_ACCIDENTE,
        a.AREA_ANATOMICA_INVOLUCRADA,
        a.CAUSA
      from TBL_SERV_CONSULTA_MEDICA a left join bio_empleado b on a.nss=b.emp_nss
      where trunc(a.fecha) >= to_date(kfechaini,'dd/mm/yy')
        and trunc(a.fecha) <= to_date(kfechafin,'dd/mm/yy')
      order by a.fecha desc
    )
    loop
      APEX_JSON.open_OBJECT;
        APEX_JSON.WRITE('id_consulta',coalesce(i.reg_id,'0'));
        APEX_JSON.WRITE('fecha_consulta',coalesce(i.fecha,to_date('01/01/1900','dd/mm/yyyy')));
        APEX_JSON.WRITE('nss',coalesce(i.nss,'0'));
        APEX_JSON.WRITE('nombre',coalesce(i.nombre,'0'));
        APEX_JSON.WRITE('rfc',coalesce(i.emp_rfc,'0'));
        APEX_JSON.WRITE('curp',coalesce(i.emp_curp,'0'));
        APEX_JSON.WRITE('tipo_consulta',coalesce(i.tipo_consulta,'0'));
        APEX_JSON.WRITE('area_accidente',coalesce(i.area_de_accidente,'0'));
        APEX_JSON.WRITE('area_involucrada',coalesce(i.area_anatomica_involucrada,'0'));
        APEX_JSON.WRITE('causa',coalesce(i.causa,'0'));
      APEX_JSON.close_OBJECT;
    end loop;

    APEX_JSON.close_array;
    APEX_JSON.close_OBJECT;
    kjson := APEX_JSON.get_clob_output;
    APEX_JSON.free_output;

  else
    APEX_JSON.initialize_clob_output;
    APEX_JSON.open_OBJECT;
    APEX_JSON.open_array('Datos');
    APEX_JSON.open_OBJECT;
      APEX_JSON.write('Proceso','consulta de consultas medicas por fecha');
      APEX_JSON.write('Estado',-100);
      APEX_JSON.write('Mensaje','No se tienen datos en la consulta');
      APEX_JSON.write('Data',kdata);
    APEX_JSON.close_OBJECT;
    APEX_JSON.close_array;
    APEX_JSON.close_OBJECT;
    kjson := APEX_JSON.get_clob_output;
    APEX_JSON.free_output;
  end if;

else
  APEX_JSON.initialize_clob_output;
  APEX_JSON.open_OBJECT;
  APEX_JSON.open_array('Datos');
  APEX_JSON.open_OBJECT;
    APEX_JSON.write('Proceso','consulta de consultas medicas por fecha');
    APEX_JSON.write('Estado',-100);
    APEX_JSON.write('Mensaje','fechas incorrectas');
    APEX_JSON.write('Data',kdata);
  APEX_JSON.close_OBJECT;
  APEX_JSON.close_array;
  APEX_JSON.close_OBJECT;
  kjson := APEX_JSON.get_clob_output;
  APEX_JSON.free_output;
end if;

l_response_body_clob := kjson;
dbms_lob.open(l_response_body_clob, dbms_lob.lob_readonly);
l_response_length := dbms_lob.getlength(l_response_body_clob);

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
-- [NOTA-1] CORREGIDO tras probar en vivo 2026-08-17: aunque el INSERT de
--   Servcio/consulta manda REG_ID='' (docs/contextoWS.txt:1700-1701), la columna
--   SI trae valores numericos reales en produccion (probablemente un trigger/
--   sequence de la tabla lo sobreescribe) - id_consulta SI es util como
--   identificador, no hace falta ignorarlo.
-- [NOTA-2] CAUSA es texto clinico libre, no un catalogo cerrado (ver hallazgo del
--   17 de agosto en docs/plan-tareas-concretas.html, recuadro "Para validar con
--   Product Owner") - el dashboard debe tratarlo como texto libre agrupado tal
--   cual, sin asumir un catalogo fijo de valores.
-- [NOTA-3] Filtro por FECHA (fecha de la consulta), igual que hace el WS ya
--   conectado Servcio/conuslta_medica_usuario para el mismo campo.
-- =============================================================================
