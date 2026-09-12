-- =============================================================================
-- MATERNIDAD — WS agregado para el dashboard (todos los casos en un rango de fechas)
-- =============================================================================
-- Destino : misma instancia ORDS que security/Servcio/* (10.249.249.3). La tabla
--           SERV_MED_MATERNIDAD_SEGUIMIENTO ya existe (docs/ords-maternidad.sql).
-- Fecha   : 11 de septiembre de 2026
--
-- Motivo: el modulo Maternidad del prototipo se alimenta de incapacidades con ramo
--   "maternidad". En nuestros datos ese ramo NO existe (0 de 291 incapacidades en todo el
--   historico) - la pantalla saldria vacia. En cambio el portal SI tiene un seguimiento de
--   maternidad propio (semanas de gestacion, fecha probable de parto, restricciones,
--   proxima revision, estatus, reincorporacion) que es mas rico que el del mock. Lo unico
--   que le falta es un WS "todos los casos en un rango": consulta_maternidad es por NSS.
--
-- Modelo: mismo patron que consulta_accidentes_fecha_cta (docs/ords-accidentes-dashboard.sql
--   + docs/ords-cuenta-en-reportes.sql): request {fecha_inicial, fecha_final} en dd/mm/yy,
--   join a bio_empleado para nombre, CUENTA por subconsulta escalar con cast (el patron
--   final, ya probado 4 veces), manejo de "sin datos" y "fechas incorrectas".
--
-- Como aplicar (SQL Developer > conexion > RESTful Services):
--   1. Modulo "Servcio" > New Template > URI Template: consulta_maternidad_fecha
--   2. New Handler > Method: POST, Source Type: PL/SQL.
--   3. Pegar el bloque completo de abajo como Source.
-- =============================================================================


-- =============================================================================
-- HANDLER ORDS — POST /Servcio/consulta_maternidad_fecha
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
  from SERV_MED_MATERNIDAD_SEGUIMIENTO a
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
        coalesce(cast((select max(cta.CUENTA_NOMBRE)
                         from bio_datos_laborales_empleados dl
                         join biometrico_cuenta cta on dl.cuenta_id = cta.cuenta_id
                        where dl.emp_nss = a.nss) as varchar2(100)),
                 'sin cuenta asignada') cuenta,
        a.SEMANAS_GESTACION,
        a.FECHA_PROBABLE_PARTO,
        a.RESTRICCIONES_LABORALES,
        a.PROXIMA_REVISION,
        a.ESTATUS,
        a.INCAPACIDAD,
        a.REINCORPORACION,
        a.NOMBRE_USUARIO
      from SERV_MED_MATERNIDAD_SEGUIMIENTO a left join bio_empleado b on a.nss=b.emp_nss
      where trunc(a.fecha_registro) >= to_date(kfechaini,'dd/mm/yy')
        and trunc(a.fecha_registro) <= to_date(kfechafin,'dd/mm/yy')
      order by a.fecha_registro desc
    )
    loop
      APEX_JSON.open_OBJECT;
        APEX_JSON.WRITE('id_registro',coalesce(i.reg_id,0));
        APEX_JSON.WRITE('fecha_registro',coalesce(to_char(i.fecha_registro,'YYYY-MM-DD'),'0'));
        APEX_JSON.WRITE('nss',coalesce(i.nss,'0'));
        APEX_JSON.WRITE('nombre',coalesce(i.nombre,'0'));
        APEX_JSON.WRITE('cuenta',coalesce(i.cuenta,'0'));
        APEX_JSON.WRITE('semanas_gestacion',coalesce(i.semanas_gestacion,0));
        APEX_JSON.WRITE('fecha_probable_parto',coalesce(to_char(i.fecha_probable_parto,'YYYY-MM-DD'),'0'));
        APEX_JSON.WRITE('restricciones_laborales',coalesce(i.restricciones_laborales,'0'));
        APEX_JSON.WRITE('proxima_revision',coalesce(to_char(i.proxima_revision,'YYYY-MM-DD'),'0'));
        APEX_JSON.WRITE('estatus',coalesce(i.estatus,'0'));
        APEX_JSON.WRITE('incapacidad',coalesce(i.incapacidad,'0'));
        APEX_JSON.WRITE('reincorporacion',coalesce(to_char(i.reincorporacion,'YYYY-MM-DD'),'0'));
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
      APEX_JSON.write('Proceso','consulta de maternidad por fecha');
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
    APEX_JSON.write('Proceso','consulta de maternidad por fecha');
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
-- VERIFICACION
-- =============================================================================
--   POST http://10.249.249.3/biows/ords/security/Servcio/consulta_maternidad_fecha
--   Body: {"fecha_inicial":"01/01/20","fecha_final":"31/12/26"}
--
--   Comparar el numero de filas contra:
--     select count(*) from SERV_MED_MATERNIDAD_SEGUIMIENTO;
--   y que cada fila traiga "cuenta". Las fechas van en YYYY-MM-DD (no DD/MM/YYYY como en
--   consulta_maternidad por NSS): el dashboard agrupa por mes y ese formato ordena solo.
-- =============================================================================
