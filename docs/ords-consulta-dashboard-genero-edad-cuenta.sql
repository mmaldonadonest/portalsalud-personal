-- =============================================================================
-- CONSULTA MÉDICA — agrega Género/Edad/Cuenta al WS agregado del dashboard
-- =============================================================================
-- Destino : misma instancia ORDS que security/Servcio/* (10.249.249.3). Extiende
--           el handler consulta_medica_fecha ya aplicado (docs/ords-consulta-dashboard.sql)
--           - NO es un handler nuevo, es un REEMPLAZO del Source existente.
-- Fecha   : 19 de agosto de 2026
-- Motivo  : identificado 18 de agosto revisando salud-ocupacional-v2
--           (dashboard/Morbilidad.jsx) - ese dashboard de referencia muestra
--           "Por cuenta" y "Por edad y género" solo para Morbilidad/Consulta,
--           no para los otros 4 dominios. Se replica el mismo alcance aqui
--           (ver docs/plan-tareas-concretas.html, filas "Dashboard - desglose
--           por Genero/Edad/Cuenta").
-- Datos   : bio_empleado.emp_sexo y bio_empleado.emp_fechan YA EXISTEN
--           (confirmado docs/contextoWS.txt:671 y :2500). CUENTA via el mismo
--           JOIN ya usado en ExpedienteService.cuentaDe() (bio_datos_laborales_empleados
--           + biometrico_cuenta, ver docs/contextoWS.txt:2503-2506).
--
-- Como aplicar (SQL Developer > conexion > RESTful Services):
--   1. Abrir el handler POST existente "consulta_medica_fecha" (modulo Servcio).
--   2. REEMPLAZAR TODO el Source por el bloque de abajo (no es un nuevo template,
--      es el mismo handler con el SELECT extendido).
-- =============================================================================


-- =============================================================================
-- BLOQUE 1: HANDLER ORDS — POST /Servcio/consulta_medica_fecha (REEMPLAZO)
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
        b.emp_sexo,
        TRUNC(MONTHS_BETWEEN(SYSDATE, b.emp_fechan)/12) edad,
        coalesce(cta.CUENTA_NOMBRE,'sin cuenta asignada') cuenta,
        a.TIPO_CONSULTA,
        a.AREA_DE_ACCIDENTE,
        a.AREA_ANATOMICA_INVOLUCRADA,
        a.CAUSA
      from TBL_SERV_CONSULTA_MEDICA a
        left join bio_empleado b on a.nss=b.emp_nss
        left join bio_datos_laborales_empleados dl on a.nss=dl.emp_nss
        left join biometrico_cuenta cta on dl.cuenta_id=cta.cuenta_id
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
        APEX_JSON.WRITE('genero',coalesce(i.emp_sexo,'0'));
        APEX_JSON.WRITE('edad',coalesce(i.edad,0));
        APEX_JSON.WRITE('cuenta',coalesce(i.cuenta,'0'));
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
-- [NOTA-1] EDAD se calcula contra SYSDATE (edad actual del empleado), no edad al
--   momento de la consulta - simplifica el calculo y evita que una misma persona
--   "cambie de bucket" segun cuando fue cada consulta dentro del rango. Es un
--   agregado de KPI, no un dato clinico preciso.
-- [NOTA-2] GENERO se pasa tal cual viene de emp_sexo (coalesce a '0' si null,
--   mismo convenio "0"=sin dato ya usado en Examen/Antidoping) - NO se asume un
--   catalogo M/F, se agrupa por el valor real que traiga la columna (verificar
--   con datos reales una vez aplicado, no inventar valores).
-- [NOTA-3] CUENTA reusa el mismo JOIN que ExpedienteService.cuentaDe() (WS
--   Servcio/Medico via docs/contextoWS.txt:2503-2506) - resuelto en la BD, no
--   requiere una llamada N+1 por NSS desde Java.
-- =============================================================================
