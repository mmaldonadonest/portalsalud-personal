-- BASE: SSO (SSO_PRODUCTIVA, la ADB). Correr CONECTADO CON EL ESQUEMA QUE PUBLICA EL ORDS «sso»
--       (el dueño del modulo REST con login/registro_app). NO es BIOMETRICO ni el ORDS de 10.249.249.3.
-- 25-nov-2026. El portal Salud manda su login del formulario a este WS. Hoy contesta HTTP 555 (ORDS-25001).
--
-- Que cambia (la respuesta es la misma: Datos[].Mensaje = 'Login correcto' + los mismos campos):
--   1) La salida va directo a HTP (APEX_JSON sin initialize_clob_output): ya no se lee un CLOB despues de
--      APEX_JSON.free_output, que en el APEX de la ADB lo libera y rompia tambien el bloque EXCEPTION (555).
--   2) Las tablas TBL_APPS_ONEST / TBL_APPS_ROL_MENU se leen con SQL dinamico y protegido: si no existen o
--      fallan, «apps» sale vacio y el login sigue (el portal no usa «apps»).
--   3) id_app se lee como texto y se convierte (el portal manda "13").
--   4) Empleado inactivo, inexistente o duplicado = rechazo normal en JSON, no error.
--   5) Nombre hasta 400 caracteres y el NSS con bind (antes se pegaba al texto del SQL).
-- Correr como SCRIPT (F5). Primero respalda el handler actual en C:\miguel\respaldosql.

SET DEFINE OFF
SET SERVEROUTPUT ON
SET LONG 200000
SET LONGCHUNKSIZE 200000
SET PAGESIZE 0
SET LINESIZE 32767
SET TRIMSPOOL ON

PROMPT === 1) Respaldo del handler actual (POST registro_app) ===
SPOOL C:\miguel\respaldosql\sso-registro-app-ANTES-25nov.txt
SELECT m.NAME AS MODULO, m.URI_PREFIX, t.URI_TEMPLATE, h.METHOD, h.SOURCE_TYPE, h.SOURCE
  FROM USER_ORDS_HANDLERS h
  JOIN USER_ORDS_TEMPLATES t ON t.ID = h.TEMPLATE_ID
  JOIN USER_ORDS_MODULES m ON m.ID = t.MODULE_ID
 WHERE t.URI_TEMPLATE = 'registro_app' AND h.METHOD = 'POST';
SPOOL OFF
PROMPT Revisa que C:\miguel\respaldosql\sso-registro-app-ANTES-25nov.txt tenga el PL/SQL completo antes de seguir.

PROMPT === 2) ¿Existen las tablas que usa? (si faltan TBL_APPS_ONEST o TBL_APPS_ROL_MENU, el nuevo sigue funcionando) ===
SET PAGESIZE 50
SELECT OBJECT_NAME, OBJECT_TYPE FROM USER_OBJECTS
 WHERE OBJECT_NAME IN ('TBL_APPS_USUARIO', 'BIO_EMPLEADO', 'TBL_APPS_ONEST', 'TBL_APPS_ROL_MENU')
 ORDER BY OBJECT_NAME;

PROMPT === 3) Reemplazo del handler (mismo modulo, misma ruta, mismo metodo) ===
DECLARE
    v_modulo USER_ORDS_MODULES.NAME%TYPE;
BEGIN
    SELECT m.NAME INTO v_modulo
      FROM USER_ORDS_MODULES m JOIN USER_ORDS_TEMPLATES t ON t.MODULE_ID = m.ID
     WHERE t.URI_TEMPLATE = 'registro_app' AND TRIM(BOTH '/' FROM m.URI_PREFIX) = 'login';

    ORDS.DEFINE_HANDLER(
        p_module_name    => v_modulo,
        p_pattern        => 'registro_app',
        p_method         => 'POST',
        p_source_type    => ORDS.source_type_plsql,
        p_items_per_page => 0,
        p_source         => q'~
declare
  j           apex_json.t_values;
  kusuario    varchar2(50);
  kpassword   varchar2(100);
  kapp        number;
  kexiste     number;
  kid_usuario varchar2(30);
  knombre     varchar2(400);
  type t_txt is table of varchar2(400);
  app_id   t_txt := t_txt();
  app_nom  t_txt := t_txt();
  mod_app  t_txt := t_txt();
  mod_id   t_txt := t_txt();
  mod_nom  t_txt := t_txt();

  -- Rechazo con la misma forma que el handler original ({"Resultado":{Proceso, Estado, Mensaje}})
  procedure rechazo(p_estado number, p_mensaje varchar2) is
  begin
    apex_json.open_object;
    apex_json.open_object('Resultado');
    apex_json.write('Proceso', 'Login');
    apex_json.write('Estado', p_estado);
    apex_json.write('Mensaje', p_mensaje);
    apex_json.close_object;
    apex_json.close_object;
  end;
begin
  apex_json.parse(j, utl_raw.cast_to_varchar2(:body));
  kusuario  := trim(apex_json.get_varchar2(p_path => 'id_usuario', p_values => j));
  kpassword := apex_json.get_varchar2(p_path => 'id_password', p_values => j);
  -- el portal lo manda como texto ("13"); get_varchar2 acepta numero o texto
  kapp      := to_number(trim(apex_json.get_varchar2(p_path => 'id_app', p_values => j)) default null on conversion error);

  if kapp is null or kapp <> 13 then
    rechazo(100, 'El password o el usuario no es valido para acceso');
    return;
  end if;

  select count(*) into kexiste
    from tbl_apps_usuario
   where emp_nss = kusuario
     and upper(trim(password)) = upper(trim(kpassword))
     and id_app = 13;
  if kexiste = 0 then
    rechazo(-100, 'El usuario no se encuentra registrado en base de datos');
    return;
  end if;

  begin
    select e.emp_nss, trim(e.emp_nombre || ' ' || e.emp_apellido || ' ' || e.emp_apellido2)
      into kid_usuario, knombre
      from bio_empleado e
     where e.emp_status = 0
       and e.emp_nss = kusuario;
  exception
    when no_data_found then
      rechazo(-101, 'El usuario no esta activo');
      return;
    when too_many_rows then
      rechazo(-102, 'El usuario aparece activo mas de una vez');
      return;
  end;

  -- «apps» y «modulos»: informativos (el portal no los usa). Si las tablas no existen o fallan, van vacios.
  begin
    execute immediate 'select distinct to_char(a.id_app), a.nombre_aplicacion'
                   || '  from tbl_apps_onest a join tbl_apps_rol_menu r on a.id_app = r.id_app'
      bulk collect into app_id, app_nom;
    execute immediate 'select to_char(a.id_app), to_char(r.id_menu), r.nombre_menu'
                   || '  from tbl_apps_usuario a join tbl_apps_rol_menu r on a.id_app = r.id_app'
                   || ' where a.emp_nss = :1 and r.id_rol = 1'
      bulk collect into mod_app, mod_id, mod_nom using kid_usuario;
  exception
    when others then
      app_id := t_txt(); app_nom := t_txt(); mod_app := t_txt(); mod_id := t_txt(); mod_nom := t_txt();
  end;

  -- Todo lo que podia fallar ya paso: se escribe la respuesta directo a HTP (sin CLOB)
  apex_json.open_object;
  apex_json.open_array('Datos');
  apex_json.open_object;
  apex_json.write('Proceso', 'Login');
  apex_json.write('Estado', 0);
  apex_json.write('Mensaje', 'Login correcto');
  apex_json.write('id_rol', 'usuario');
  apex_json.write('id_usuario', kid_usuario);
  apex_json.write('nombre_usuario', knombre);
  apex_json.open_array('apps');
  for x in 1 .. app_id.count loop
    apex_json.open_object;
    apex_json.write('id_app', app_id(x));
    apex_json.write('nombre_app', app_nom(x));
    apex_json.open_array('modulos');
    for i in 1 .. mod_app.count loop
      if mod_app(i) = app_id(x) then
        apex_json.open_object;
        apex_json.write('id_menu', mod_id(i));
        apex_json.write('modulo', mod_nom(i));
        apex_json.write('acess', true);
        apex_json.close_object;
      end if;
    end loop;
    apex_json.close_array;
    apex_json.close_object;
  end loop;
  apex_json.close_array;
  apex_json.close_object;
  apex_json.close_array;
  apex_json.close_object;
exception
  when others then
    rechazo(1000, 'Error en la consulta del Servicio');
end;
~');
    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Handler registro_app reemplazado en el modulo ' || v_modulo);
END;
/

PROMPT === 4) Verificacion: el handler nuevo ya no usa get_clob_output ===
SET PAGESIZE 50
SELECT m.NAME AS MODULO, t.URI_TEMPLATE, h.METHOD,
       CASE WHEN INSTR(h.SOURCE, 'get_clob_output') > 0 THEN 'VIEJO' ELSE 'NUEVO' END AS VERSION
  FROM USER_ORDS_HANDLERS h
  JOIN USER_ORDS_TEMPLATES t ON t.ID = h.TEMPLATE_ID
  JOIN USER_ORDS_MODULES m ON m.ID = t.MODULE_ID
 WHERE t.URI_TEMPLATE = 'registro_app' AND h.METHOD = 'POST';

-- PARA REGRESAR AL ANTERIOR: copiar el SOURCE de C:\miguel\respaldosql\sso-registro-app-ANTES-25nov.txt en el
-- editor del handler (SQL Developer > REST Data Services > modulo > registro_app > POST) y guardar.
