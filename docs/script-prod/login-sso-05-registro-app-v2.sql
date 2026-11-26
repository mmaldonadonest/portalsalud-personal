-- BASE: SSO (SSO_PRODUCTIVA, la ADB). Conectado con el esquema que publica el ORDS «sso».
-- 26-nov-2026. CLON de login/registro_app -> login/registro_app_v2. El original NO se toca.
-- Unico cambio contra el original: el Portal Salud existe como app 13 (la de siempre) y como app 27 (PORTAL SALUD
-- del launcher). El v2 deja entrar a quien tenga cualquiera de las dos. Lo demas es identico (misma respuesta).
-- Correr como SCRIPT (F5).

SET DEFINE OFF
SET SERVEROUTPUT ON

PROMPT === 1) Alta del clon registro_app_v2 en el mismo modulo «login» ===
DECLARE
    v_modulo USER_ORDS_MODULES.NAME%TYPE;
BEGIN
    SELECT m.NAME INTO v_modulo
      FROM USER_ORDS_MODULES m JOIN USER_ORDS_TEMPLATES t ON t.MODULE_ID = m.ID
     WHERE t.URI_TEMPLATE = 'registro_app' AND TRIM(BOTH '/' FROM m.URI_PREFIX) = 'login';

    ORDS.DEFINE_TEMPLATE(p_module_name => v_modulo, p_pattern => 'registro_app_v2');
    ORDS.DEFINE_HANDLER(
        p_module_name    => v_modulo,
        p_pattern        => 'registro_app_v2',
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
  kapp      := to_number(trim(apex_json.get_varchar2(p_path => 'id_app', p_values => j)) default null on conversion error);

  -- Portal Salud: 13 (la de siempre) o 27 (PORTAL SALUD del launcher)
  if kapp is null or kapp not in (13, 27) then
    rechazo(100, 'El password o el usuario no es valido para acceso');
    return;
  end if;

  select count(*) into kexiste
    from tbl_apps_usuario
   where emp_nss = kusuario
     and upper(trim(password)) = upper(trim(kpassword))
     and id_app in (13, 27);
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
    DBMS_OUTPUT.PUT_LINE('Clon registro_app_v2 creado en el modulo ' || v_modulo || '; registro_app sin cambios');
END;
/

PROMPT === 2) Verificacion: deben salir registro_app (original) y registro_app_v2 ===
SELECT m.NAME AS MODULO, t.URI_TEMPLATE, h.METHOD
  FROM USER_ORDS_HANDLERS h
  JOIN USER_ORDS_TEMPLATES t ON t.ID = h.TEMPLATE_ID
  JOIN USER_ORDS_MODULES m ON m.ID = t.MODULE_ID
 WHERE t.URI_TEMPLATE IN ('registro_app', 'registro_app_v2')
 ORDER BY t.URI_TEMPLATE;

-- PARA QUITARLO: ORDS.DELETE_TEMPLATE(p_module_name => '<modulo>', p_pattern => 'registro_app_v2'); COMMIT;
