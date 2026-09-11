-- =============================================================================
-- WS NUEVO Y AISLADO: POST /Servcio/predio_empleado
-- =============================================================================
-- Motivo: el modulo de dashboards analiticos (rol MEDICO_ANALISTA, ver
--         cambios/Dashboard Salud/docs) necesita agrupar incapacidades/examenes/
--         antidoping/accidentes/maternidad POR PREDIO. BIO_DATOS_LABORALES_EMPLEADOS
--         ya tiene la columna PREDIO (NUMBER, FK logica a TAB_PREDIOS.PREDIO_ID) -
--         MISMA tabla que Catalogo/usuario ya usa para resolver cuenta
--         (join BIO_EMPLEADO -> BIO_DATOS_LABORALES_EMPLEADOS -> biometrico_cuenta_SAP).
--         No se modifica Catalogo/usuario (WS productivo, compartido con el portal
--         PHP viejo) - este es un handler nuevo, propio, sin consumidores existentes.
-- Modulo: "Servcio" (mismo criterio que usuario_email - dominio de Servicio Medico).
-- Alcance: minimo a proposito - recibe Nss, regresa predio_id + predio_desc
--          (join a TAB_PREDIOS para el nombre legible, ej. "AIFA", "MACRO I").
--          Nada de cuenta/puesto/etc., eso ya lo da Catalogo/usuario.
-- Nota SERV_MED_ANTIDOPING_INVENTARIO: esa tabla (corrida anterior de este mismo
--          proyecto) trae su propia columna PREDIO aislada (VARCHAR2, sin FK a
--          TAB_PREDIOS) - separado, pendiente de reconciliar para que tambien
--          referencie TAB_PREDIOS.PREDIO_ID en vez de texto libre. No es parte de
--          este WS, se menciona solo para no perder el pendiente.
-- Fecha: 08 de septiembre de 2026
--
-- Como crearlo (SQL Developer > conexion > RESTful Services):
--   1. Ubicar el modulo "Servcio" (mismo que ya contiene /Medico, /consulta,
--      /usuario_email, etc.).
--   2. Click derecho sobre el modulo > New Resource Template.
--      URI Template: predio_empleado
--   3. Sobre ese Resource Template > New Handler.
--      Method: POST | Source Type: PL/SQL | Mime Types Consumed: application/json
--   4. Pegar el bloque de abajo como Source > Apply Changes.
--   5. Probar con curl/Postman: POST .../security/Servcio/predio_empleado
--      body: {"Nss":"68958027838"}
-- =============================================================================

declare
  l_response_body_clob  clob;
  l_response_length     number;
  l_response_buffer     varchar2(32767);
  l_response_chunk_size binary_integer := 32000;
  l_offset              integer := 1;
  kjson                 clob;
  j                     apex_json.t_values;
  kns                   varchar2(30);
  kpredio_id            number;
  kpredio_desc          varchar2(150);
begin
  apex_json.parse(j, utl_raw.cast_to_varchar2(:body));
  kns := upper(apex_json.get_varchar2(p_path => 'Nss', p_values => j));

  APEX_JSON.initialize_clob_output;
  APEX_JSON.open_object;
  APEX_JSON.open_array('Datos');

  begin
    select c.predio, p.predio_desc
      into kpredio_id, kpredio_desc
      from BIO_DATOS_LABORALES_EMPLEADOS c
      left join TAB_PREDIOS p
        on p.predio_id = c.predio
     where c.emp_nss = kns
       and rownum = 1;

    APEX_JSON.open_object;
      APEX_JSON.write('nss', kns);
      APEX_JSON.write('predioId', kpredio_id);
      -- convenio "0"=sin dato, mismo que ya usan Examen/Antidoping/Consumibles
      APEX_JSON.write('predioDesc', coalesce(kpredio_desc, '0'));
    APEX_JSON.close_object;
  exception
    when no_data_found then
      null; -- NSS sin fila en BIO_DATOS_LABORALES_EMPLEADOS: Datos queda vacio
  end;

  APEX_JSON.close_array;
  APEX_JSON.close_object;
  kjson := APEX_JSON.get_clob_output;
  APEX_JSON.free_output;

  l_response_body_clob := kjson;
  dbms_lob.open(l_response_body_clob, dbms_lob.lob_readonly);
  l_response_length := dbms_lob.getlength(l_response_body_clob);

  while (l_response_length > 0) loop
    dbms_lob.read(l_response_body_clob, l_response_chunk_size, l_offset, l_response_buffer);
    htp.prn(l_response_buffer);
    l_offset := l_offset + l_response_chunk_size;
    l_response_length := l_response_length - l_response_chunk_size;
  end loop;
  dbms_lob.close(l_response_body_clob);
end;

-- =============================================================================
-- NOTAS
-- =============================================================================
-- [NOTA-1] rownum=1 por si un NSS tuviera mas de una fila en
--   BIO_DATOS_LABORALES_EMPLEADOS (ej. multipredio, ver [[nss-multipredio-use-case]]
--   en la memoria del proyecto) - se toma la primera, mismo criterio conservador que
--   Catalogo/usuario (que tambien trunca a la primera fila salvo el flujo especial
--   de findAsociaciones). Si en la practica un NSS multipredio necesita las varias
--   filas para el dashboard, hay que revisar esto - no se probo ese caso.
-- [NOTA-2] Respuesta: {"Datos":[{"nss":"...","predioId":N,"predioDesc":"..."}]} si
--   existe fila, {"Datos":[]} si el NSS no tiene fila en BIO_DATOS_LABORALES_EMPLEADOS.
--   Mismo contrato (array "Datos") que el resto de la API.
-- [NOTA-3] Un solo NSS por llamada, igual que TODOS los WS existentes de este
--   sistema (usuario_email, Catalogo/usuario, etc.) - para el dashboard analitico
--   que agrega "por predio" sobre potencialmente cientos/miles de casos, el lado
--   Java debe cachear por NSS (varios casos comparten NSS) para no repetir llamadas,
--   no se construyo un endpoint batch aqui para mantener el mismo patron minimo que
--   ya usa el resto del sistema. Si el volumen real hace esto lento, revisar batch.
-- [NOTA-4] No se corrio contra el diccionario de datos real en esta sesion - si el
--   nombre de columna PREDIO en BIO_DATOS_LABORALES_EMPLEADOS o PREDIO_ID/PREDIO_DESC
--   en TAB_PREDIOS difiere de lo aqui asumido, ajustar antes de aplicar.
-- =============================================================================
