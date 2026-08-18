-- =============================================================================
-- EXAMEN MÉDICO — WS agregado para dashboard (todas las NSS en un rango)
-- =============================================================================
-- Destino : instancia Oracle que respalda el WS ORDS real (security/Servcio/*,
--           http://10.249.249.3/biows/ords/security). Requiere que
--           docs/ords-examen-historial.sql ya este aplicado (tabla
--           SERV_MED_RESULTADO_EXAMEN_HIST + el INSERT aditivo en
--           PR_SERVICIO_MED_EXAMEN2) - sin eso, esta tabla esta vacia.
-- Fecha   : 17 de agosto de 2026
-- Modelo  : replica 1:1 el patron de los 4 WS agregados anteriores
--           (consulta_incapacidades_fecha / consulta_accidentes_fecha /
--           consulta_medica_fecha / consulta_antidoping_fecha).
--
-- IMPORTANTE - SIN DATOS RETROACTIVOS: el historial arranco vacio el 17 de
-- agosto de 2026. Un rango de fechas anterior a esa fecha siempre dara "sin
-- datos", aunque el empleado ya se haya examinado antes.
--
-- Como aplicar (SQL Developer > conexion > RESTful Services):
--   1. Clic derecho en el modulo "Servcio" existente > New Template.
--   2. URI Template: "consulta_examen_fecha".
--   3. Dentro del template > New Handler > Method: POST, Source Type: PL/SQL.
--   4. Pegar el bloque completo de abajo como "Source".
-- =============================================================================


-- =============================================================================
-- BLOQUE 1: HANDLER ORDS — POST /Servcio/consulta_examen_fecha
-- (lista de dictamenes de examen de TODAS las NSS en un rango de fechas,
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
  from SERV_MED_RESULTADO_EXAMEN_HIST a left join bio_empleado b on a.nss=b.emp_nss
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
        a.APTO,
        a.NO_APTO,
        a.APTO_CONDICIONADO,
        a.APTO_RESTRINGIDO
      from SERV_MED_RESULTADO_EXAMEN_HIST a left join bio_empleado b on a.nss=b.emp_nss
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
        APEX_JSON.WRITE('apto',coalesce(i.apto,'0'));
        APEX_JSON.WRITE('no_apto',coalesce(i.no_apto,'0'));
        APEX_JSON.WRITE('apto_condicionado',coalesce(i.apto_condicionado,'0'));
        APEX_JSON.WRITE('apto_restringido',coalesce(i.apto_restringido,'0'));
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
      APEX_JSON.write('Proceso','consulta de examenes por fecha');
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
    APEX_JSON.write('Proceso','consulta de examenes por fecha');
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
-- [NOTA-1] Filtro por FECHA_REGISTRO de SERV_MED_RESULTADO_EXAMEN_HIST (fecha
--   en que se guardo el examen, unica fecha disponible en el historial).
-- [NOTA-2] APTO/NO_APTO/APTO_CONDICIONADO/APTO_RESTRINGIDO son 4 columnas
--   independientes (no un solo campo "dictamen") - el valor exacto que marca
--   "seleccionado" (ej. "SI", "1", el texto del dictamen) no esta confirmado;
--   el dashboard cuenta filas con valor no vacio por columna, sin asumir un
--   valor especifico.
-- =============================================================================
