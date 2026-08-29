-- =============================================================================
-- WS NUEVO Y AISLADO: POST /Servcio/usuario_email
-- =============================================================================
-- Motivo: la foto de perfil (intranet Onest) se resuelve por email; Catalogo/usuario
--         no lo trae y NO se quiere modificar ese handler porque lo consume tambien
--         el portal PHP productivo (php-old/app/app.php:143, mismo host 10.249.249.3).
--         Este handler es nuevo, propio, sin consumidores existentes - si algo sale
--         mal aqui, solo se pierde el avatar, nunca login/busqueda NSS/nada del legacy.
-- Modulo: "Servcio" (no "Catalogo") - es el modulo del dominio de Servicio Medico
--         (Medico, consulta, incapacidades, indice, etc., ver docs/contextoWS.txt),
--         mismo criterio de organizacion que el resto de estos WS. El aislamiento
--         de riesgo no depende del modulo (cada Resource Template/Handler de ORDS
--         es independiente) - es solo agrupacion logica.
-- Alcance: minimo a proposito - recibe Nss, regresa email. Nada mas (ver discusion:
--          todo lo demas que se necesita ya lo da Catalogo/usuario sin tocarlo).
-- Fecha: 28 de agosto de 2026
--
-- Como crearlo (SQL Developer > conexion > RESTful Services):
--   1. Ubicar el modulo "Servcio" (mismo que ya contiene /Medico, /consulta, etc.).
--   2. Click derecho sobre el modulo > New Resource Template.
--      URI Template: usuario_email
--   3. Sobre ese Resource Template > New Handler.
--      Method: POST | Source Type: PL/SQL | Mime Types Consumed: application/json
--   4. Pegar el bloque de abajo como Source > Apply Changes.
--   5. Probar con curl/Postman: POST .../security/Servcio/usuario_email
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
  kemail                varchar2(150);
begin
  apex_json.parse(j, utl_raw.cast_to_varchar2(:body));
  kns := upper(apex_json.get_varchar2(p_path => 'Nss', p_values => j));

  APEX_JSON.initialize_clob_output;
  APEX_JSON.open_object;
  APEX_JSON.open_array('Datos');

  begin
    select emp_email into kemail
      from BIO_EMPLEADO
     where emp_nss = kns;

    APEX_JSON.open_object;
      APEX_JSON.write('nss', kns);
      -- convenio "0"=sin dato, mismo que ya usan Examen/Antidoping/Consumibles
      APEX_JSON.write('email', coalesce(kemail, '0'));
    APEX_JSON.close_object;
  exception
    when no_data_found then
      null; -- NSS no encontrado: Datos queda vacio (mismo criterio que Catalogo/usuario)
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
-- [NOTA-1] Cursor implicito con bind variable (kns) en vez de DBMS_SQL dinamico -
--   mas simple y seguro que el patron de concatenacion de string usado en los
--   handlers viejos (Catalogo/usuario, etc.) - aqui no hace falta esa complejidad
--   porque la consulta es fija (2 columnas, sin construir SQL variable).
-- [NOTA-2] Respuesta: {"Datos":[{"nss":"...","email":"..."}]} si existe, {"Datos":[]}
--   si el NSS no existe en BIO_EMPLEADO. Mismo contrato (array "Datos") que el resto
--   de la API para que el cliente Java pueda reusar el mismo patron de parseo.
-- [NOTA-3] Si BIO_EMPLEADO.EMP_EMAIL no es el nombre real de la columna, ajustar
--   antes de aplicar - no se corrio contra el diccionario de datos real en esta sesion.
-- =============================================================================
