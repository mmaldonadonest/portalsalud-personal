-- =============================================================================
-- FIX: cuenta_predio_lista debe cubrir TODAS las cuentas que aparecen en los datos
-- =============================================================================
-- Destino : misma instancia ORDS que security/Servcio/* (10.249.249.3).
-- Fecha   : 10 de septiembre de 2026
--
-- Problema detectado al verificar los WS _cta (docs/ords-cuenta-en-reportes.sql):
--   de las 20 cuentas distintas que devuelve consulta_incapacidades_fecha_cta en
--   2024, dos NO existen en cuenta_predio_lista — DIRECCION DE SISTEMAS (20
--   registros, la 3a mas frecuente) y PREMENA. Al no estar en la lista, la
--   pantalla /admin/predios no las muestra y NUNCA se les puede asignar predio:
--   quedarian en "Sin asignar" de forma permanente.
--
-- Causa: el BLOQUE 4 de docs/ords-predio-cuenta.sql lee de biometrico_cuenta_SAP,
--   pero los reportes (consulta_medica_fecha y los tres _cta) resuelven la cuenta
--   contra biometrico_cuenta:
--       join biometrico_cuenta cta on dl.cuenta_id = cta.cuenta_id
--   Son dos catalogos distintos. Cualquier cuenta que este en biometrico_cuenta y
--   no en la SAP es invisible para la pantalla de asignacion.
--
-- Lo que NO se debe hacer: insertar esas cuentas a mano en biometrico_cuenta_SAP.
--   Es un catalogo productivo de otro sistema; las filas manuales lo contaminan y
--   se pierden en la siguiente carga desde SAP.
--
-- Solucion: que el handler liste la UNION de ambos catalogos, deduplicada por
--   nombre. El mapeo SERV_MED_CUENTA_PREDIO se llavea por CUENTA_NOMBRE (no por
--   id), asi que da igual de que catalogo venga el id — con que el NOMBRE aparezca
--   en la lista, la asignacion funciona.
--
-- Como aplicar: reemplazar SOLO el "for x in (...)" del handler POST
--   /Servcio/cuenta_predio_lista. El resto del bloque (APEX_JSON, chunking del
--   CLOB) se queda igual. Es un WS nuestro, creado en ago/sep-2026, y su unico
--   consumidor es PredioService — no hay legacy que se rompa.
-- =============================================================================

-- Datos confirmados 10-sep-2026 en SQL Developer:
--   biometrico_cuenta_SAP = 190 filas, cuenta_id VARCHAR2
--   biometrico_cuenta     = 291 filas, cuenta_id NUMBER
--   DIRECCION DE SISTEMAS (2000450) y PREMENA (2001080) existen SOLO en BIO.
--
-- Por eso el to_char() en las dos ramas: sin el, el UNION ALL falla con
-- "ORA-01790: expression must have same datatype as corresponding expression".
-- Normalizar a texto no cambia el contrato - hoy el JSON ya devuelve
-- "cuentaId":"2000885" entrecomillado y CuentaPredioDto.cuentaId es String.
--
-- min() sobre nombre/id solo elige una grafia y un id cuando la misma cuenta
-- existe en los dos catalogos; el mapeo se llavea por UPPER(cuenta_nombre).

  for x in (
    select c.cuenta_id, c.cuenta_nombre, v.predio_id, v.predio_nombre
      from (
        select min(bc.cuenta_id) cuenta_id, min(bc.cuenta_nombre) cuenta_nombre
          from ( select to_char(cuenta_id) cuenta_id, cuenta_nombre from biometrico_cuenta_SAP
                 union all
                 select to_char(cuenta_id) cuenta_id, cuenta_nombre from biometrico_cuenta ) bc
         group by upper(bc.cuenta_nombre)
      ) c
      left join (
        select m.cuenta_nombre, m.predio_id, p.nombre predio_nombre,
               row_number() over (partition by upper(m.cuenta_nombre) order by m.fecha_asignacion desc) rn
          from SERV_MED_CUENTA_PREDIO m
          join SERV_MED_PREDIO p on p.reg_id = m.predio_id
      ) v on upper(v.cuenta_nombre) = upper(c.cuenta_nombre) and v.rn = 1
     order by c.cuenta_nombre
  ) loop


-- =============================================================================
-- VERIFICACION
-- =============================================================================
--   POST http://10.249.249.3/biows/ords/security/Servcio/cuenta_predio_lista
--   Body: {}
--
--   1. Que aparezcan DIRECCION DE SISTEMAS y PREMENA.
--   2. Que el total suba respecto a las 187 de hoy (cuanto, depende de cuantas
--      cuentas tenga biometrico_cuenta que no esten en la SAP — puede ser bastante
--      mas; si la lista se vuelve inmanejable para la pantalla admin, el siguiente
--      paso seria filtrar por cuentas con actividad clinica real, no ampliar mas).
--   3. Que "47 BRAND" siga saliendo con predioId=1 (AIFA), la unica asignacion
--      que existe hoy — confirma que el left join no se rompio.
--
-- Nota aparte, independiente de este fix: al 10-sep-2026 solo 1 de 187 cuentas
-- tiene predio asignado. Los WS ya entregan la cuenta en los 4 reportes, pero el
-- filtro por predio va a mandar casi todo a "Sin asignar" hasta que alguien
-- capture las asignaciones en /admin/predios.
-- =============================================================================


-- =============================================================================
-- APLICADO Y VERIFICADO — 10-sep-2026
-- =============================================================================
-- HTTP 200. Total de cuentas 187 -> 292. DIRECCION DE SISTEMAS y PREMENA ya
-- aparecen. Control OK: {"cuentaId":"2000885","cuentaNombre":"47 BRAND",
-- "predioId":1,"predioNombre":"AIFA"} - el left join del mapeo sigue resolviendo.
-- Cruce final contra consulta_incapacidades_fecha_cta (2024, 221 filas): las 20
-- cuentas distintas que aparecen en los datos existen en el catalogo. Ya no hay
-- ninguna cuenta imposible de mapear.
--
-- PENDIENTE (decidido dejarlo para despues, 10-sep-2026): la union arrastro 35
-- cuentas historicas de biometrico_cuenta que la tabla SAP no tenia - casi todas
-- con sufijo "- BIOANTERIOR" (47 BRAND - BIOANTERIOR, ALBATROS - BIOANTERIOR,
-- CARTERS - BIOANTERIOR...) mas una llamada literalmente "BAJA". Nadie les va a
-- asignar predio, solo ensucian /admin/predios (~292 filas para capturar a mano).
-- Al revisarlo: NO asumir que el criterio es el sufijo - biometrico_cuenta tiene
-- una columna CUENTA_ACTIVO (NUMBER(1)) que probablemente sea el filtro correcto
-- y mas limpio. Verificar con datos antes de elegir.
-- =============================================================================
