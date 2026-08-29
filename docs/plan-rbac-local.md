# Plan: RBAC local para Portal Salud (roles/menús/usuarios propios, login sin cambios)

## Contexto

Portal Salud hoy resuelve "qué módulos clínicos puede ver un usuario" contra el esquema legacy de ORDS (`tbl_apps_rol`, `TBL_APPS_ROL_MENU`, `TBL_APP_ROL_USUARIO`, `tbl_apps_usuario`), compartido con el portal PHP que **sigue productivo hoy**. Se investigó a fondo la idea de construir un admin de roles sobre ese mismo esquema compartido, y se descartó: cualquier UI de administración ahí abre riesgo de blast radius sobre datos que otra app en producción también escribe (ver hallazgos de la sesión: `crud_alta_app_menu` no valida huérfanos al borrar un rol, `registra_rol_usuario` actualiza password como side-effect del cambio de rol, y el propio PHP legacy puede modificar esas tablas en cualquier momento).

**Decisión del usuario:** el login/autenticación sigue exactamente igual (WS ORDS como única fuente de identidad — `registro_app`, sin tocar). Pero **roles, menús y la asignación de rol a usuario pasan a administrarse localmente**, en el propio esquema Oracle de Portal Salud, vía una pantalla de admin nueva. El usuario acepta explícitamente el riesgo de desincronización con el PHP legacy como costo de la migración (dos sistemas de roles en paralelo mientras el legacy siga vivo).

**Hallazgo clave que reduce el alcance real:** el esquema RBAC local **ya existe** — `APP_SEC_ROLE`, `APP_SEC_USER`, `APP_SEC_USER_ROLE`, `APP_MENU` (más `APP_SEC_PERMISSION`/`APP_SEC_ROLE_PERMISSION`, que quedan fuera de este alcance) están creados y sembrados en `src/main/resources/db/sql/00_init_oracle21c.sql`, pero **dormant** — hoy solo habilitan el login local (estrategia `JAVA`), nunca se usan para resolver menús/permisos. No hay que diseñar tablas desde cero: hay que activarlas, agregar una tabla de unión que falta, y construir el admin encima.

**Nota técnica importante:** este proyecto **no usa Flyway** pese a un comentario viejo en `application.properties` que lo sugiere (confirmado: sin dependencia, sin bean de migración). El mecanismo real es SQL idempotente a mano en `db/sql/`, ejecutado manualmente contra Oracle antes de levantar la app (`ddl-auto=validate` — Hibernate solo valida, nunca crea).

## Punto de enganche (ya confirmado en código, cero riesgo de romper algo existente)

`PermissionService` (el filtro 403 real) y `ModulePermissionService` (el que arma el submenú lateral dinámico) **ambos dependen únicamente de la interfaz `ModulePermissionClient`** (`findRoleId`, `findMenusByRole`), hoy satisfecha por el único `@Component BiowsModulePermissionClient` (habla con ORDS). Ninguna otra clase depende de ella. Esto significa: **agregar una segunda implementación y elegir cuál se activa por configuración es el único punto de integración necesario** — cero cambios en `PermissionService`/`ModulePermissionService`.

## Enfoque

### 1. Base de datos (SQL manual idempotente, mismo estilo que `00_init_oracle21c.sql`)

- **Tabla nueva `APP_MENU_ROLE`** (join directo menú↔rol, espejo de `TBL_APPS_ROL_MENU` de ORDS): `ID` identity PK, `MENU_ID` FK→`APP_MENU`, `ROLE_ID` FK→`APP_SEC_ROLE`, `CREATED_AT`, `CREATED_BY`. Índice único `(MENU_ID, ROLE_ID)`.
- **Seed de los 14 módulos del submenú** (no solo 9) — corrección importante: `nss-modules.html` pinta 14 entradas (ids 1-14 de ORDS: Expediente General, E. Laboratorio, Histórico E.M, Nota Médica, Nota Incapacidad, Archivo de Consultas, Consulta Médica, Incapacidades, Archivo Incapacidades, Examen Médico, Pre-Test, Antidoping, Accidentes, Maternidad), pero `ClinicalAccessFilter` solo **exige** 9 de esas 14 (las otras 5 — Expediente General como contenedor, Laboratorio, Histórico E.M, Nota Médica, Nota Incapacidad — comparten la ruta genérica `/api/nss/archivos`, ya documentada como hueco sin protección, fuera de alcance de este plan). Hay que sembrar las **14** en `APP_MENU` para que el sidebar no pierda entradas, aunque el filtro solo aplique 403 sobre 9.
- `CODE` explícito y legible por fila (`EXPEDIENTE_GENERAL`, `LABORATORIO`, `HISTORICO_EM`, `NOTA_MEDICA`, `NOTA_INCAPACIDAD`, `ARCHIVO_CONSULTAS`, `CONSULTA_MEDICA`, `INCAPACIDADES`, `ARCHIVO_INCAPACIDADES`, `EXAMEN_MEDICO`, `PRETEST`, `ANTIDOPING`, `ACCIDENTES`, `MATERNIDAD`) — **no** se reutiliza la numeración 1-14 de ORDS a propósito: es un espacio de IDs independiente, coincidir con ORDS por accidente sería frágil.
- **`APP_SEC_USER.PASSWORD_HASH` pasa a nullable.** Hoy es `NOT NULL` porque la tabla solo modelaba cuentas de login local. Un usuario que recibe un rol pero sigue autenticando por LEGACY/HYBRID nunca tiene un hash real — forzar un placeholder sería confuso (parecería una cuenta de login válida). Nullable + un chequeo en el `DaoAuthenticationProvider` existente de que estos usuarios no puedan autenticar localmente sin hash.
- **Extender las entidades JPA existentes** (`AppSecUser`, `AppSecRole`, hoy con mapeo parcial) para incluir `ACTIVE`/audit/`DESCRIPTION`, que ya existen en la tabla pero no están mapeados.
- **Migración inicial (dato, no código):** ya tenemos el query de lectura contra ORDS (`SELECT id_usuario, id_rol FROM TBL_APP_ROL_USUARIO WHERE id_app=13`) para poblar `APP_SEC_USER`/`APP_SEC_USER_ROLE` la primera vez. El usuario corre esto manualmente antes de sembrar — no se ejecuta como parte de este plan.

### 2. Backend (Java)

- **`ModuleDto` gana un campo `code` (String).** Adición no rompe la implementación ORDS existente (queda null ahí). Necesario para que `nss-modules.html` (hoy indexado por el `id_menu` numérico de ORDS) pueda re-indexarse por `code` en vez de un número que ya no tendrá relación con nada.
- **Nueva `LocalModulePermissionClient implements ModulePermissionClient`** (`@Component`), respaldada por JPA: `findRoleId` busca `AppSecUser` por `username` = NSS autenticado (reusando `AppSecUserRepository.findActiveByIdentifier`, ya existe) y regresa el id de su único rol; `findMenusByRole` consulta `APP_MENU_ROLE` join `APP_MENU` filtrando activos.
- **Selector de fuente vía propiedad** `portal.permissions.source=ORDS|LOCAL` (mismo patrón ya usado por `portal.auth.strategy`), controla cuál `@Component` queda activo (`@ConditionalOnProperty`). Arranca en `ORDS` (sin cambio de comportamiento) hasta que el modo sombra confirme paridad.
- **Modo sombra** (decidido con el usuario): mientras `portal.permissions.source=ORDS` siga activo para las DECISIONES reales, `ClinicalAccessFilter` recibe también una referencia (opcional, inyectada solo si `portal.permissions.shadow=true`) a `LocalModulePermissionClient`, y en cada request calcula EN PARALELO qué hubiera decidido el esquema local, sin afectar la respuesta. Log de una sola línea por request: `[permisos-sombra] usuario=X ruta=Y ords={permitido|403} local={permitido|403} coincide={true|false}`. Al final del periodo de prueba, un `grep coincide=false` sobre los logs da la lista exacta de discrepancias a resolver (usuarios sin migrar, roles mal mapeados, etc.) antes de voltear `portal.permissions.source=LOCAL` de verdad.
- **Kill switch:** dado que el selector es una sola propiedad (`portal.permissions.source`), volver a `ORDS` ante cualquier problema post-corte es un cambio de config + redeploy, no un rollback de datos — las tablas ORDS nunca se tocan en ningún punto de este plan, así que revertir es seguro en cualquier momento.
- **`ClinicalAccessFilter.RUTAS_CLINICAS`** pasa de `Map<String, Set<Integer>>` (ids ORDS) a `Map<String, Set<String>>` (los `CODE` locales) — mismas 9 rutas, mismo mecanismo de bloqueo (403 real), solo cambia la clave. Las 4 rutas clínicas restantes (de las 13 totales) y los adjuntos genéricos siguen exactamente igual de desprotegidos que hoy — no es parte de este plan cerrarlo.
- **Repos JPA nuevos:** `AppMenuRepository`, `AppMenuRoleRepository`. `APP_SEC_USER_ROLE` ya se cubre con la relación `@ManyToMany` existente en `AppSecUser`.
- **`fragments/nss-modules.html`** — los mapas `icons`/`labels`/`actions`/`tipos` están indexados hoy por el `id_menu` numérico de ORDS (`{1:'ri-booklet-line', 2:'ri-flask-line', ...}`); pasan a indexarse por `${m.code}` (el nuevo campo de `ModuleDto`) en vez de `${m.idMenu}` — cambio mecánico, mismas 14 entradas, misma lógica de iconos/acciones, solo cambia la llave de los mapas Thymeleaf.
- **`SecurityConfiguration`** — agregar `.requestMatchers("/admin/**").hasRole("ADMIN")` en `authorizeHttpRequests`, antes de `.anyRequest().authenticated()`. Confirmado: con `portal.auth.strategy=HYBRID` (el valor real activo hoy en `application.properties`, no el default `JAVA` del código), el usuario semilla `68958027838` autentica por `daoAuthenticationProvider` (BD local) y ya trae `ROLE_ADMIN` — sirve como admin inicial sin bootstrap adicional. Sería el primer uso de `.hasRole()` en el proyecto (hoy todo el control de acceso pasa por `ClinicalAccessFilter`, no por reglas declarativas de Spring Security) — precedente menor, vale la pena anotarlo aunque no cambie el enfoque.

**Fuera de alcance de este plan, a propósito:**
- `APP_SEC_PERMISSION`/`APP_SEC_ROLE_PERMISSION` (existen, quedan sin usar — no se introduce una capa de permisos granulares separada de "menú", se sigue el mismo modelo plano rol→menú que ya usa ORDS).
- Cerrar el hueco de las 4 rutas clínicas + adjuntos genéricos sin protección (ya documentado, independiente de este cambio).
- Migrar el PHP legacy a este mismo esquema — sigue en ORDS indefinidamente, es la fuente aceptada de desincronización.

### 3. Admin — 3 pantallas separadas, mismo patrón que `causas-consulta.html`

Cada una: Controller `@RestController` bajo `/api/admin/...` + Service JPA + página Thymeleaf con fetch/JSON inline (sin el helper `guardarRegistro()`, que es propio de `nss-search.html` y no aplica a un CRUD JSON simple), registrada como un `@GetMapping` más en `PortalViewController` (mismo patrón que todas las páginas simples del proyecto) y un link nuevo en `fragments/menu.html`.

1. **`/admin/roles`** — listar/crear/renombrar/activar-desactivar rol. Preferir `ACTIVE=N` sobre `DELETE` (la columna ya existe) — mejora deliberada sobre ORDS, que borra en duro sin validar huérfanos.
2. **`/admin/roles/{id}/menus`** — checklist de los 14 módulos del catálogo fijo sembrado (**no** "crear menú libre" en este alcance — mismo criterio que Restricciones médicas: catálogo fijo, no inventado desde la UI) para marcar/desmarcar por rol. De esos 14, solo 9 tienen 403 real de por medio (ver nota en `ClinicalAccessFilter` arriba); los otros 5 solo afectan si el ítem aparece en el sidebar.
3. **`/admin/usuarios`** — buscar NSS (reusar `NssSearchClient.findUsuario(nss, usuarioConsulta)` directo — **no** `NssSearchService.findByNss()`, que tiene el side-effect de dar de alta en `Servcio/Medico`) y asignar/cambiar su único rol.

**Gate de acceso al admin:** reusar el `ROLE_ADMIN` ya sembrado en el proveedor JAVA local (`68958027838`, `db/sql/seeds/user-68958027838.sql`) como el admin inicial — ya existe, ya tiene esa autoridad, no hay que inventar un mecanismo de bootstrap nuevo. Las 3 rutas `/admin/**` se protegen con `.hasRole("ADMIN")` en `SecurityConfiguration`.

## SQL concreto (mismo estilo guardado que `00_init_oracle21c.sql`)

Confirmado el patrón exacto contra `APP_MENU` (ya existe, líneas 152-168 del script): cada `CREATE`/`ALTER` va envuelto en `BEGIN ... EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF; END; /` (ignora "ya existe", -955), así que estos scripts se pueden re-correr sin romper nada si ya se aplicaron antes.

```sql
-- Tabla nueva: join directo menu<->rol (espejo de TBL_APPS_ROL_MENU)
BEGIN
  EXECUTE IMMEDIATE q'[
    CREATE TABLE APP_MENU_ROLE (
      ID          NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
      MENU_ID     NUMBER NOT NULL,
      ROLE_ID     NUMBER NOT NULL,
      CREATED_AT  TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL,
      CREATED_BY  VARCHAR2(100) DEFAULT USER NOT NULL
    )
  ]';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF; END;
/

BEGIN EXECUTE IMMEDIATE 'CREATE UNIQUE INDEX UX_APP_MENU_ROLE ON APP_MENU_ROLE(MENU_ID, ROLE_ID)';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF; END;
/

-- FKs (mismo patron add_constraint_if_not_exists ya usado en el script para las demas tablas)
ALTER TABLE APP_MENU_ROLE ADD CONSTRAINT FK_MENU_ROLE_MENU FOREIGN KEY (MENU_ID) REFERENCES APP_MENU(ID);
ALTER TABLE APP_MENU_ROLE ADD CONSTRAINT FK_MENU_ROLE_ROLE FOREIGN KEY (ROLE_ID) REFERENCES APP_SEC_ROLE(ID);

-- PASSWORD_HASH nullable (usuarios de solo-rol nunca autentican local)
ALTER TABLE APP_SEC_USER MODIFY (PASSWORD_HASH NULL);

-- Seed: los 14 modulos del submenu (mismos nombres que fragments/nss-modules.html)
INSERT INTO APP_MENU (CODE, TITLE, ICON, ORDER_NO) VALUES ('EXPEDIENTE_GENERAL', 'Expediente General', 'ri-booklet-line', 1);
INSERT INTO APP_MENU (CODE, TITLE, ICON, ORDER_NO) VALUES ('LABORATORIO', 'E. Laboratorio', 'ri-flask-line', 2);
INSERT INTO APP_MENU (CODE, TITLE, ICON, ORDER_NO) VALUES ('HISTORICO_EM', 'Historico E.M', 'ri-history-line', 3);
INSERT INTO APP_MENU (CODE, TITLE, ICON, ORDER_NO) VALUES ('NOTA_MEDICA', 'Nota Medica', 'ri-file-text-line', 4);
INSERT INTO APP_MENU (CODE, TITLE, ICON, ORDER_NO) VALUES ('NOTA_INCAPACIDAD', 'Nota Incapacidad', 'ri-file-paper-2-line', 5);
INSERT INTO APP_MENU (CODE, TITLE, ICON, ORDER_NO) VALUES ('ARCHIVO_CONSULTAS', 'Archivo de Consultas', 'ri-book-2-line', 6);
INSERT INTO APP_MENU (CODE, TITLE, ICON, ORDER_NO) VALUES ('CONSULTA_MEDICA', 'Consulta Medica', 'ri-health-book-line', 7);
INSERT INTO APP_MENU (CODE, TITLE, ICON, ORDER_NO) VALUES ('INCAPACIDADES', 'Incapacidades', 'ri-first-aid-kit-line', 8);
INSERT INTO APP_MENU (CODE, TITLE, ICON, ORDER_NO) VALUES ('ARCHIVO_INCAPACIDADES', 'Archivo Incapacidades', 'ri-archive-line', 9);
INSERT INTO APP_MENU (CODE, TITLE, ICON, ORDER_NO) VALUES ('EXAMEN_MEDICO', 'Examen Medico', 'ri-heart-pulse-line', 10);
INSERT INTO APP_MENU (CODE, TITLE, ICON, ORDER_NO) VALUES ('PRETEST', 'Pre-Test', 'ri-survey-line', 11);
INSERT INTO APP_MENU (CODE, TITLE, ICON, ORDER_NO) VALUES ('ANTIDOPING', 'Antidoping', 'ri-test-tube-line', 12);
INSERT INTO APP_MENU (CODE, TITLE, ICON, ORDER_NO) VALUES ('ACCIDENTES', 'Accidentes', 'ri-alarm-warning-line', 13);
INSERT INTO APP_MENU (CODE, TITLE, ICON, ORDER_NO) VALUES ('MATERNIDAD', 'Maternidad', 'ri-women-line', 14);
COMMIT;
```

Íconos copiados 1:1 del mapa `icons` que ya vive en `fragments/nss-modules.html` — cero adivinanza.

**Migración inicial de asignaciones (dato real, el usuario decide cuándo correrla, no forma parte de la automatización):** por cada fila que regrese `SELECT id_usuario, id_rol FROM TBL_APP_ROL_USUARIO WHERE id_app=13` (ya lo corrimos antes), insertar/asegurar un `APP_SEC_USER` (username=id_usuario, active='Y', account_status='ACTIVE', password_hash=NULL) y su `APP_SEC_USER_ROLE` correspondiente al rol ya creado localmente con el mismo nombre (USER/ADM/ENFERMERO, mapeados 1:1 a los 3 roles reales que encontramos).

## Orden de implementación sugerido

1. DDL (`APP_MENU_ROLE`, `PASSWORD_HASH` nullable) + seed de los 14 menús + los 3 roles reales (USER/ADM/ENFERMERO) + sus asignaciones de menú (replicando lo que ya vimos en vivo: USER y ENFERMERO sin Antidoping/Accidentes/Maternidad).
2. Entidades/repos JPA (`AppMenu`, `AppMenuRole` o mapeo directo, extender `AppSecUser`/`AppSecRole`).
3. `LocalModulePermissionClient` + propiedad `portal.permissions.source` (arranca en `ORDS`, sin efecto visible todavía) + `ModuleDto.code`.
4. Modo sombra (`portal.permissions.shadow=true`) — correr en paralelo, sin afectar respuestas, revisar logs.
5. Rework de `fragments/nss-modules.html` (indexar por `code`) — se puede hacer en paralelo al paso 4, no depende de qué fuente esté activa.
6. Las 3 pantallas admin (`/admin/roles`, `/admin/roles/{id}/menus`, `/admin/usuarios`) + gate `.hasRole("ADMIN")`.
7. Migración de datos reales (usuario ejecuta) + validar modo sombra sin discrepancias.
8. Cutover: `portal.permissions.source=LOCAL`.

## Estimado (anclado a precedentes reales del proyecto, no adivinado)

Comparables ya construidos y medidos en esta misma sesión de trabajo: Causa de consulta (catálogo + `UPDATE` + 1 pantalla admin) = 9h reales. Restricciones médicas (catálogo fijo + tabla de asignación + tarjeta embebida) = 11h reales. Matriz de permisos original (filtro 403 + wiring, sin admin UI) = 18h estimadas.

Este plan es más grande que cualquiera de esos tres por separado (2 tablas nuevas + 3 pantallas admin completas + una segunda implementación de `ModulePermissionClient` + modo sombra + rework de `nss-modules.html`) pero reusa mucho de lo ya construido (esquema ya sembrado, patrón de `causas-consulta.html` para las pantallas, `ClinicalAccessFilter` sin tocar su lógica). Estimado: **~26-30h** (DDL+entidades ~5h, `LocalModulePermissionClient`+modo sombra ~5h, 3 pantallas admin ~4h c/u = 12h, rework `nss-modules.html`+`SecurityConfiguration` ~2h, migración+pruebas de humo ~3h).

## Verificación

1. `mvn -o compile` tras cada fase.
2. Con `portal.permissions.source=ORDS` (default, sin cambio): confirmar cero regresión — mismo comportamiento de hoy para el usuario de pruebas y para cualquier NSS real.
3. Correr el modo sombra un tiempo razonable, revisar el log comparativo por discrepancias (ej. usuarios sin fila migrada localmente).
4. Voltear a `portal.permissions.source=LOCAL` en un ambiente de prueba, repetir la prueba de humo con rol completo (ADM) y rol reducido (mismo criterio que ya identificamos: alguien sin Antidoping/Accidentes/Maternidad) para confirmar 403 real.
5. Confirmar que `nss-modules.html` sigue pintando íconos/labels correctos ahora indexado por `code` en vez del id numérico viejo.

## Estado y pendientes (26-28 de agosto)

Implementado y funcionando en local (`source=LOCAL`, `strategy=LEGACY_PHP`): las 3 pantallas admin, `LocalModulePermissionClient`, 403 real por código, avatar/nombre reales vía ORDS. Pendiente para continuar:

1. **Aplicar `docs/ords-catalogo-usuario-email.sql` en ORDS** (SQL Developer → RESTful Services → handler `Catalogo/usuario`, reemplazar Source). Sin esto `email` siempre llega `null` y el avatar de perfil nunca se resuelve bajo `LEGACY_PHP` — confirmado en vivo con NSS 68958027838.
2. **Probar en navegador las 3 pantallas admin** (`/admin/roles`, `/admin/roles/{id}/menus`, `/admin/usuarios`) — construidas y compilan, nunca abiertas.
3. **Migrar usuarios reales** — hoy solo `68958027838` tiene rol local; con `source=LOCAL` cualquier otro NSS ve el submenú vacío hasta que se le asigne rol vía `/admin/usuarios`.
4. **Llevar a QA**: 5 scripts SQL en orden (`00_init_oracle21c.sql` si falta, `seeds/user-68958027838.sql` opcional, `01_rbac_local.sql`, `02_fix_expediente_general_duplicado.sql`, `03_null_password_legacy_php.sql`) + decidir si QA arranca en `ORDS`+`shadow=true` antes de voltear a `LOCAL` (el modo sombra nunca se ejerció en local, se saltó directo a `LOCAL`).
5. Rebuild + redeploy pendiente para probar en navegador todo lo de esta sesión (login `LEGACY_PHP`, avatar/nombre real, `currentPath` activo, 3 pantallas admin, fix de `EXPEDIENTE_GENERAL` duplicado).
