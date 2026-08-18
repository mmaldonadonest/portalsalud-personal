-- =============================================================================
-- ANTIDOPING / ALCOHOLIMETRÍA — WS agregado para dashboard (todas las NSS en un rango)
-- =============================================================================
-- Destino : instancia Oracle que respalda el WS ORDS real (security/Servcio/*,
--           http://10.249.249.3/biows/ords/security). Tabla
--           SERV_MED_ANTIDOPING_RESULTADO ya existe (docs/ords-antidoping.sql,
--           aplicada y verificada 2026-08-14). NO hace falta DDL nuevo, solo el
--           handler.
-- Fecha   : 17 de agosto de 2026
-- Modelo  : replica 1:1 el patron de security/Servcio/consulta_incapacidades_fecha
--           - filtra por FECHA_REGISTRO (esta tabla, a diferencia de
--           SERV_MED_ACCIDENTE, no tiene una fecha de evento separada de la fecha
--           de registro), mismo JOIN contra bio_empleado, mismo formato de
--           request {fecha_inicial,fecha_final} en dd/mm/yy.
--
-- Como aplicar (SQL Developer > conexion > RESTful Services):
--   1. Clic derecho en el modulo "Servcio" existente > New Template.
--   2. URI Template: "consulta_antidoping_fecha".
--   3. Dentro del template > New Handler > Method: POST, Source Type: PL/SQL.
--   4. Pegar el bloque completo de abajo como "Source".
-- =============================================================================


-- =============================================================================
-- BLOQUE 1: HANDLER ORDS — POST /Servcio/consulta_antidoping_fecha
-- (lista de antidoping/alcoholimetria de TODAS las NSS en un rango de fechas,
-- para el dashboard)
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
  from SERV_MED_ANTIDOPING_RESULTADO a left join bio_empleado b on a.nss=b.emp_nss
  where trunc(a.fecha_registro) >= to_date(kfechaini,'dd/mm/yy')
    and trunc(a.fecha_registro) <= to_date(kfechafin,'dd/mm/yy');

  if kexiste > 0 then

    APEX_JSON.initialize_clob_output;
    APEX_JSON.OPEN_OBJECT;
    APEX_JSON.open_array('Datos');

    for i in (
      select
        a.REG_ID,
        a.FECHA_REGISTRO,
        a.NSS,
        b.emp_nombre||' '||b.emp_apellido||' '||b.emp_apellido2 nombre,
        b.emp_rfc,
        b.emp_curp,
        a.FOLIO,
        a.TIPO_PRUEBA,
        a.SUSTANCIA,
        a.RESULTADO,
        a.STATUS_CONCLUSION,
        a.NOMBRE_USUARIO
      from SERV_MED_ANTIDOPING_RESULTADO a left join bio_empleado b on a.nss=b.emp_nss
      where trunc(a.fecha_registro) >= to_date(kfechaini,'dd/mm/yy')
        and trunc(a.fecha_registro) <= to_date(kfechafin,'dd/mm/yy')
      order by a.fecha_registro desc
    )
    loop
      APEX_JSON.open_OBJECT;
        APEX_JSON.WRITE('id_registro',coalesce(i.reg_id,0));
        APEX_JSON.WRITE('fecha_registro',coalesce(i.fecha_registro,to_date('01/01/1900','dd/mm/yyyy')));
        APEX_JSON.WRITE('nss',coalesce(i.nss,'0'));
        APEX_JSON.WRITE('nombre',coalesce(i.nombre,'0'));
        APEX_JSON.WRITE('rfc',coalesce(i.emp_rfc,'0'));
        APEX_JSON.WRITE('curp',coalesce(i.emp_curp,'0'));
        APEX_JSON.WRITE('folio',coalesce(i.folio,'0'));
        APEX_JSON.WRITE('tipo_prueba',coalesce(i.tipo_prueba,'0'));
        APEX_JSON.WRITE('sustancia',coalesce(i.sustancia,'0'));
        APEX_JSON.WRITE('resultado',coalesce(i.resultado,'0'));
        APEX_JSON.WRITE('status_conclusion',coalesce(i.status_conclusion,'0'));
        APEX_JSON.WRITE('usuario',coalesce(i.nombre_usuario,'0'));
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
      APEX_JSON.write('Proceso','consulta de antidoping por fecha');
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
    APEX_JSON.write('Proceso','consulta de antidoping por fecha');
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
-- [NOTA-1] Filtro por FECHA_REGISTRO (no hay fecha de evento separada en esta
--   tabla, a diferencia de SERV_MED_ACCIDENTE que si tiene FECHA_ACCIDENTE).
-- [NOTA-2] REG_ID aqui es NUMBER identity real (a diferencia de
--   TBL_SERV_CONSULTA_MEDICA.REG_ID que es VARCHAR2 y se inserta como ''), asi
--   que se escribe como numero sin comillas.
-- [NOTA-3] No se incluye OBSERVACIONES en el payload (texto libre potencialmente
--   sensible/largo, no aporta a un agregado de dashboard) - mismo criterio de
--   payload ligero que consulta_medica_fecha.
-- =============================================================================
