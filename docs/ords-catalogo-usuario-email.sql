-- =============================================================================
-- Catalogo/usuario — agrega EMAIL (emp_email) al WS de identidad del empleado
-- =============================================================================
-- Destino : misma instancia ORDS que security/Servcio/* (10.249.249.3). Extiende
--           el handler POST /Catalogo/usuario ya aplicado y EN PRODUCCION (lo usa
--           searchEmploye.php / la busqueda por NSS del portal Java, y ya se
--           reusa para CUENTA en ExpedienteService.cuentaDe()) - NO es un handler
--           nuevo, es un REEMPLAZO del Source existente.
-- Fecha   : 19 de agosto de 2026
-- Motivo  : la foto de perfil (intranet Onest) se resuelve por email - ni el
--           login (registro_app) ni este WS lo traian. Confirmado por el usuario
--           que bio_empleado SI tiene columna emp_email, accesible por NSS.
--           docs/plan-tareas-concretas.html / memoria del proyecto: "el email
--           tiene que salir del WS, no de un campo manual en APP_SEC_USER".
--
-- Como aplicar (SQL Developer > conexion > RESTful Services):
--   1. Abrir el handler POST existente "usuario" (modulo Catalogo).
--   2. REEMPLAZAR TODO el Source por el bloque de abajo (mismo handler,
--      SELECT + salida JSON extendidos con 'email' - todo lo demas identico).
-- =============================================================================


-- =============================================================================
-- BLOQUE 1: HANDLER ORDS — POST /Catalogo/usuario (REEMPLAZO)
-- =============================================================================
declare
kdata1 clob ;
l_response_body_clob clob ;
l_response_length     NUMBER;
l_response_buffer     VARCHAR2(32767);
l_response_chunk_size BINARY_INTEGER := 32000;
l_offset              INTEGER := 1;
kSQL  VARCHAR2(2000);
dcursor number;
v_NumRows integer;
kjson clob;
kcadena varchar2(200);
kestado number;
KEMP_NSS VARCHAR2(20);
KEMP_NOMBRE VARCHAR2(100);
KEMP_APELLIDO VARCHAR(100);
kEMP_APELLIDO2 VARCHAR2(100);
kEMP_RFC  VARCHAR2(20);
kexiste number;
dsql varchar2(4000);
kdata varchar2(250);
kerror varchar2(4000);
kclob clob;
kcompleto varchar2(100);
j apex_json.t_values;
kns varchar2(30);
kfechai varchar2(20);
kfechan varchar2(20);
kdireccion varchar2(2000);
ksexo varchar2(10);
kcelular varchar2(20);
ktelfijo varchar2(20);
ktelmensajes varchar2(20);
kcuenta varchar2(50);
kestadocivil varchar2(20);
kpuesto varchar2(20);
kexiste1 number;
knombre_puesto varchar2(50);
npuesto number;
kemail varchar2(150);

begin

/*{"Registro"  :
 {"Id_Ticket_id": "TK100621111432000016",
	"Id_Cuenta":"0",
	"Fecha_registro":"21/07/2021"
 }
 }*/

kclob:= utl_raw.cast_to_varchar2(:body);
apex_json.parse(j,kclob);

 kns := apex_json.get_varchar2(p_path=>'Nss',p_values=>j);
 select upper(kns) into kns from dual;

  ksql :=  'select count(*) from (
select emp_nombre||'||''''||' '||''''||'||emp_apellido||'||''''||' '||''''||'||emp_apellido2 as empleado from BIO_EMPLEADO where
    emp_nss = '||''''||kns||''''||')';

  execute immediate ksql into kexiste;

 APEX_JSON.initialize_clob_output;
  APEX_JSON.open_object;
   APEX_JSON.open_array('Datos');

  if kexiste > 0 then

     dsql:='select * from (SELECT
                            d.EMP_NSS,
                            d.emp_nombre ,
                            d.emp_apellido ,
                            d.emp_apellido2 ,
                            d.emp_RFC,
                            d.emp_nombre||'||''''||' '||''''||'||d.emp_apellido||'||''''||' '||''''||'||d.emp_apellido2 completo,
                            d.emp_sexo,
                            d.emp_calle||'||''''||' '||''''||'||d.emp_colonia||'||''''||' '||''''||'||d.EMP_CP||'||''''||' '||''''||'||d.emp_delegacion direccion,
                            d.emp_celular,
                            d.emp_telefono_fijo,
                            d.EMP_ESTADO_CIVIL,
                            d.EMP_TEL_MENSAJES,
                            coalesce(bc.cuenta_nombre,'||''''||'Sin cuenta_asignada en sap'||''''||'),
                            d.EMP_FECHAN,
                            d.EMP_FECHAI,
                            coalesce(c.id_puesto,0),
                            d.emp_email
                     FROM BIO_EMPLEADO d
                     left JOIN BIO_DATOS_LABORALES_EMPLEADOS c
                     ON d.EMP_NSS=c.EMP_NSS
                     left join biometrico_cuenta_SAP bc
                     on c.cuenta_id=bc.cuenta_id
                     where d.emp_nss='||''''||kns||''''||')';

	    dcursor := DBMS_SQL.OPEN_CURSOR;

         dbms_sql.parse(dCURSOR,DSQL,dbms_sql.native);

         dbms_sql.define_column(dcursor,1,KEMP_NSS,50);
         dbms_sql.define_column(dcursor,2,Kemp_nombre,50);
         dbms_sql.define_column(dcursor,3,kemp_apellido,100);
         dbms_sql.define_column(dcursor,4,kemp_apellido2,100);
         dbms_sql.define_column(dcursor,5,kemp_RFC,20);
         dbms_sql.define_column(dcursor,6,kcompleto,100);
         dbms_sql.define_column(dcursor,7,ksexo,20);
         dbms_sql.define_column(dcursor,8,kdireccion,2000);
         dbms_sql.define_column(dcursor,9,kcelular,20);
         dbms_sql.define_column(dcursor,10,ktelfijo,20);
         dbms_sql.define_column(dcursor,11,kestadocivil,20);
         dbms_sql.define_column(dcursor,12,ktelmensajes,20);
         dbms_sql.define_column(dcursor,13,kcuenta,50);
         dbms_sql.define_column(dcursor,14,kfechan,20);
         dbms_sql.define_column(dcursor,15,kfechai,20);
         dbms_sql.define_column(dcursor,16,kpuesto,20);
         dbms_sql.define_column(dcursor,17,kemail,150);

          v_NumRows := DBMS_SQL.EXECUTE(dcursor);

        LOOP
           IF DBMS_SQL.FETCH_ROWS(dcursor)=0 then
        EXIT;
           ELSE

            DBMS_SQL.COLUMN_VALUE(dcursor,1,KEMP_NSS);
            DBMS_SQL.COLUMN_VALUE(dcursor,2,KEMP_NOMBRE);
            DBMS_SQL.COLUMN_VALUE(dcursor,3,KEMP_APELLIDO);
            DBMS_SQL.COLUMN_VALUE(dcursor,4,kEMP_APELLIDO2);
            DBMS_SQL.COLUMN_VALUE(dcursor,5,kEMP_RFC);
            DBMS_SQL.COLUMN_VALUE(dcursor,6,kcompleto);
            DBMS_SQL.COLUMN_VALUE(dcursor,7,ksexo);
            DBMS_SQL.COLUMN_VALUE(dcursor,8,kdireccion);
            DBMS_SQL.COLUMN_VALUE(dcursor,9,kcelular);
            DBMS_SQL.COLUMN_VALUE(dcursor,10,ktelfijo);
            DBMS_SQL.COLUMN_VALUE(dcursor,11,kestadocivil);
            DBMS_SQL.COLUMN_VALUE(dcursor,12,ktelmensajes);
            DBMS_SQL.COLUMN_VALUE(dcursor,13,kcuenta);
            DBMS_SQL.COLUMN_VALUE(dcursor,14,kfechan);
            DBMS_SQL.COLUMN_VALUE(dcursor,15,kfechai);
            DBMS_SQL.COLUMN_VALUE(dcursor,16,kpuesto);
            DBMS_SQL.COLUMN_VALUE(dcursor,17,kemail);

         if kpuesto>0 or kpuesto is not null  then
          select count(*) into kexiste1 from biometrico_puesto
          where puesto_id=kpuesto;

           if kexiste1 >0 then
             select puesto_nombre into knombre_puesto from biometrico_puesto where puesto_id=kpuesto;
           else
             knombre_puesto := 'el no. de puesto no existe en base de datos';
         end if;
         else
           knombre_puesto := 'sin puesto_asignado';
         end if;

         APEX_JSON.open_object;
           APEX_JSON.write('nss',KEMP_NSS);
           APEX_JSON.write('nombre',KEMP_NOMBRE);
           APEX_JSON.write('apellidoPaterno',KEMP_APELLIDO);
           APEX_JSON.write('apellidoMaterno',kEMP_APELLIDO2);
           APEX_JSON.write('rfc',KEMP_RFC);
           APEX_JSON.write('completo',Kcompleto);
           APEX_JSON.write('direccion',Kdireccion);
           APEX_JSON.write('sexo',Ksexo);
           APEX_JSON.write('celular',Kcelular);
           APEX_JSON.write('tel_fijo',Ktelfijo);
           APEX_JSON.write('estado_civil',Kestadocivil);
           APEX_JSON.write('tel_mensajes',Ktelmensajes);
           APEX_JSON.write('cuenta',Kcuenta);
           APEX_JSON.write('fecha_nacimiento',Kfechan);
           APEX_JSON.write('fecha_ingreso',Kfechai);
           APEX_JSON.write('id_puesto',Kpuesto);
           APEX_JSON.write('nombre_puesto',Knombre_puesto);
           APEX_JSON.write('nombre_empresa','Onest México s.a de c.v');
           APEX_JSON.write('turno','Matutino');
           APEX_JSON.write('giro_industrial','Lógistica');
           APEX_JSON.write('antiguedad','10.3');
           APEX_JSON.write('email',coalesce(Kemail,'0'));
         APEX_JSON.CLOSE_object;

    END IF;
   END LOOP;
  DBMS_SQL.CLOSE_CURSOR(dcursor);

 APEX_JSON.CLOSE_array;
 APEX_JSON.close_object;
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

else
APEX_JSON.initialize_clob_output;
   APEX_JSON.open_object;
   APEX_JSON.open_array('Datos');

       APEX_JSON.open_object;
           APEX_JSON.write('nss','      ');
           APEX_JSON.write('nombre','------------');
           APEX_JSON.write('apellidoPaterno','--------------');
           APEX_JSON.write('apellidoMaterno','--------------');
           APEX_JSON.write('rfc','Sin RFC');
           APEX_JSON.write('completo','Sin Registro de Usuario');
         APEX_JSON.CLOSE_object;
  APEX_JSON.CLOSE_array;
 APEX_JSON.close_object;
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

end if;

end;


-- =============================================================================
-- NOTAS
-- =============================================================================
-- [NOTA-1] Unico cambio real vs. el Source original: se agrego d.emp_email al
--   SELECT (columna 17), su define_column/column_value, y APEX_JSON.write('email', ...)
--   en la salida. Todo lo demas (rama "sin registro", manejo de errores, resto de
--   campos) queda IDENTICO al handler que ya esta en produccion - no se toco nada mas.
-- [NOTA-2] coalesce(Kemail,'0') sigue el mismo convenio "0"=sin dato ya usado en
--   Examen/Antidoping/Consumibles - el cliente Java debe tratar "0" como ausente,
--   no como email literal.
-- [NOTA-3] Este WS es POST /Catalogo/usuario, YA CONECTADO en produccion
--   (BiowsNssSearchClient.findUsuario, usado por la busqueda por NSS y por
--   ExpedienteService.cuentaDe()) - agregar email no rompe nada existente,
--   los consumidores actuales simplemente ignoran el campo nuevo si no lo leen.
-- =============================================================================
