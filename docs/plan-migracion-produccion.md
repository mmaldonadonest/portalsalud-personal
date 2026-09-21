# Plan de migración a producción — Portal Salud Personal

**Fecha:** 21-sep-2026. **Estado:** plan, nada ejecutado.
**Alcance:** poner el portal Java en producción conviviendo con el PHP legacy (ambos consumen la misma ORDS), con la base propia del portal, el histórico migrado y los usuarios/roles cargados.

Complementa: `paso-a-produccion-dos-bases.md` (qué script va a qué base), `etl-migracion-historica-especificacion.md` (ETL MariaDB → Oracle, equipo externo), memoria `qa-deploy-nginx-hibernate-fixes` (nginx + Tomcat).

---

## 0. Por dónde empezar (y por qué)

La propuesta de arrancar por "analizar tablas y datos del biométrico" es correcta en espíritu, pero conviene **acotarla a un diff QA → prod**, no a un análisis abierto: el portal no lee tablas del biométrico directamente, sólo consume WS de ORDS. Lo que puede fallar en producción no es "la tabla X tiene datos raros", sino **que un handler o una tabla que creamos en QA no exista en prod**. Ese inventario (Fase 1) es lo primero, y antes de él hay **una decisión que bloquea todo** (Fase 0): en qué Oracle vive el esquema `APP_*` del portal.

Orden recomendado: **0 → 1 → 2 → 3 en paralelo con 4 → 5 → 6 → 7.**

---

## Fase 0 — Decisiones e infraestructura (bloqueante)

| # | Decisión / recurso | Estado hoy | Quién |
|---|---|---|---|
| 0.1 | **Base del portal en prod** (esquema para `APP_*`, `MED_TAG`): ¿esquema nuevo en la Oracle de producción del biométrico (`10.249.249.3`) o instancia aparte? | `application-prod.properties` apunta **provisionalmente a QA** (`ONEWMS_QA`); "la Oracle de producción real todavía no existe" (spec ETL) | DBA + Miguel |
| 0.2 | Servidor de aplicación: Tomcat 11 + JDK 21 en `10.249.249.4` (donde ya está `portal.files.root`), o dónde | Existe QA en `qa-vital.onestcloud.mx` detrás de nginx | Infra |
| 0.3 | URL pública y nginx (`proxy_pass` a `/portal-salud/`, `proxy_cookie_path /portal-salud /;`, `sub_filter`, `client_max_body_size 25m`) | Config de QA documentada y probada | Infra |
| 0.4 | `portal.files.root` = `/mnt/data/onedev/apps/exec/filessalud` creado y escribible por Tomcat | Ruta confirmada; permisos por confirmar | Infra |
| 0.5 | Acceso de red: Tomcat prod → ORDS prod; ETL → MariaDB `10.249.249.4:3306` (**puerto no abierto**) | Pendiente | Infra |
| 0.6 | SSO: se queda `https://sso.onestcloud.mx/sso` (decisión 19-sep). `auth.onestcloud.mx` (Keycloak) queda fuera de este paso | Decidido | — |
| 0.7 | Ventana de salida y responsable de "go/no-go" | Por definir | Negocio |

**Entregable:** `application-prod.properties` con datasource definitivo (o variables `SPRING_DATASOURCE_*` en el Tomcat de prod) y accesos abiertos.

## Fase 1 — Inventario ORDS: QA vs producción (el "análisis de biométrico", acotado)

Objetivo: saber exactamente qué de lo que el portal usa existe ya en la ORDS de producción y qué hay que crear.

1. **Handlers**: listar los `Servcio/*` (e `info/*`, `security/*`) que consume el portal (17 WS legacy + los nuevos) y verificarlos con `curl` contra ORDS prod. Los nuevos/clonados de este proyecto son los de `docs/ords-*.sql` (27 archivos; el orden de los 7 de Dashboard está en `paso-a-produccion-dos-bases.md`).
2. **Tablas nuevas del esquema legacy** creadas por esos scripts: `SERV_MED_PREDIO`, `SERV_MED_CUENTA_PREDIO`, accidentes-seguimiento, antidoping-selección/consumibles, causas de consulta, restricciones, maternidad, `usuario_email`. Verificar con `SELECT … FROM user_tables` en prod.
3. **Catálogos con datos que sí importan al portal** (aquí sí toca ver datos): `biometrico_cuenta` / `biometrico_cuenta_SAP` (cuentas históricas con sufijo, `sin-historicas.sql`), `TBL_APPS_ROL_MENU` (menús por rol/app para `id_app=13`, hoy sólo dados de alta para `id_rol=2` en QA — ver `nss-modules.html`), catálogo ICD (909 claves A–B; hallazgo conocido), `SERV_MED_ANT_LABORALES` (stub del PHP). Confirmar si prod tiene los mismos huecos.
4. **Datos productivos ≠ QA**: los WS de prod devuelven datos reales; correr el set de Playwright de lectura (`pw/smoke.js`, dashboards) contra un ambiente apuntado a ORDS prod para ver que los parsers aguantan (fechas `d/M/yy` vs ISO, "sin datos", tipos mezclados ya cubiertos).

**Entregable:** matriz `WS/tabla → existe en prod (sí/no) → script que lo crea → verificado`. Est. **1 jornada** (anclado: el inventario de 17 WS de `contextoWS.txt` tomó ~medio día).

## Fase 2 — Base del portal (JDBC) en producción

En el esquema decidido en 0.1, en este orden (todos idempotentes):

1. `00_init_oracle21c.sql` (autoritativo: `APP_SEC_*`, `APP_MENU`, `APP_AUD_EVENT`, `APP_FS_FILE`, jobs).
2. `01_rbac_local.sql` (roles/menús locales, `APP_MENU_ROLE`) → `02_fix_expediente_general_duplicado.sql` → `03_menu_examenes.sql` → `03_null_password_legacy_php.sql`.
3. `app_domain/app-fs-file.sql` + `app-fs-file-reconcile.sql`, `app-import.sql`, `tags-salud.sql`, `files-salud.sql`.
4. Semilla del **primer `ADMIN`** (`seeds/user-68958027838.sql` o el NSS que se decida): sin él nadie entra a `/admin`.
5. Verificación: `SELECT table_name FROM user_tables WHERE table_name LIKE 'APP_%' OR table_name = 'MED_TAG'` y arranque del WAR con `ddl-auto=validate` (si falta algo, el log dice `Schema-validation: missing table`).

Est. **½ jornada**.

## Fase 3 — ORDS de producción (lo que falte según Fase 1)

- Aplicar `docs/ords-*.sql` que falten, respetando la regla: **un WS productivo existente nunca se modifica, se clona** (`_cta`, etc.). Los `*-qa.sql` no van a prod (fueron parches de QA).
- Alta de menús de la app 13 en `TBL_APPS_ROL_MENU` para los roles de prod (`crud_alta_app_menu`), o confirmar que con `portal.permissions.source=LOCAL` ya no se usa para el menú (sí se sigue usando para el **gate del SSO**: `consulta_app_rol_usuario` con `id_app=13`).
- Verificar cada WS con el `curl` que trae cada script.

Est. **1 jornada** (el usuario publica handlers en minutos; el tiempo es verificación).

## Fase 4 — Datos: histórico y catálogos

| Dato | Fuente | Cómo | Estado |
|---|---|---|---|
| Histórico `files` (21,448) y `tags` (550,560) | MariaDB `servicioMedico` | ETL equipo externo → `APP_FS_FILE` + filesystem + `MED_TAG` (spec en `etl-migracion-historica-especificacion.md`). Requiere puerto 3306 y el esquema de la Fase 2 | Ya ejecutado en QA/local; **pendiente en prod** |
| Predios (17) y cuenta→predio | `SERV_MED_PREDIO` / `SERV_MED_CUENTA_PREDIO` (ORDS) | Pantalla `/admin/predios` o SQL. **Hoy sólo 2 de 260 cuentas tienen predio**; sin esto la Vista por Predio y filtros quedan en "Sin asignar". Propuesta: CSV con ~30 deducibles del nombre + el resto lo llena negocio | Pendiente de negocio |
| Roles, menús por rol, usuarios | `APP_SEC_ROLE`, `APP_MENU_ROLE`, `APP_SEC_USER_ROLE` (portal) | Exportar de QA (roles USER/ADM/ENFERMERO/ROLE_ADMIN/ROLE_MEDICO_ANALISTA + sus menús) como `INSERT`s; usuarios se van dando de alta por NSS en `/admin/usuarios`. Limpiar el rol duplicado `MEDICO_ANALISTA` (id 42) | Pendiente |
| Causas de consulta (23), restricciones (16 fijas) | ORDS | Vienen en sus `ords-*.sql` (seed incluido) | Con Fase 3 |
| Importador Excel | `APP_IMPORT_*` | Sólo estructura; los lotes de QA no se migran | — |

Est. ETL: del equipo externo (en local tomó ~1 jornada de ejecución + verificación). Catálogos: **½ jornada** + lo que tarde negocio en llenar predios.

## Fase 5 — Despliegue y smoke test

1. WAR con `spring.profiles.active=prod` (`target/portal-salud-prod-<fecha>-<commit>.war`; el actual es `20260919-6a4ec4d`). Nombre versionado siempre.
2. Tomcat: `CATALINA_OPTS` con perfil y, si aplica, `SPRING_DATASOURCE_*`. Arranque ~40 s (metadata `individually` ya configurado).
3. nginx según la config de QA (tres bugs ya documentados: cookie path, `contextPath` JS, arranque colgado).
4. **Smoke test** (scripts Playwright del scratchpad, adaptando `BASE`): login password y SSO, Búsqueda NSS + ficha, los 15 módulos de Análisis (200 sin errores JS), 4 PDFs, Importar Excel (archivo sintético), Auditoría muestra los eventos anteriores, página de error 404/403.
5. Asignar en `/admin/roles/<id>/menus` los 3 menús de Exámenes y el rol `ROLE_MEDICO_ANALISTA` a los analistas.

Est. **1 jornada** incluyendo correcciones menores.

## Fase 6 — Convivencia con el PHP y corte

- **No hay corte de datos**: PHP y Java escriben en la misma ORDS; el portal Java sólo agrega tablas propias (`APP_*`, `MED_TAG`) y WS clonados. Pueden convivir el tiempo que se quiera.
- Lo único que se duplica es el **histórico de archivos/tags** (MariaDB vs Oracle): definir la fecha de corte del ETL y, a partir de ella, que los adjuntos nuevos se suban sólo por el portal Java (o repetir el ETL incremental por `SOURCE_ID`, que es idempotente).
- Usuarios: el login por contraseña sigue validando contra ORDS (mismo password que el PHP), así que no hay migración de credenciales.
- Retiro del PHP: decisión de negocio, no técnica; se puede apagar por módulo.

## Fase 7 — Rollback y operación

- Rollback = apuntar nginx de vuelta al PHP; las tablas `APP_*` y los WS clonados no afectan al PHP, no hay que deshacer nada.
- Respaldo del esquema del portal (`expdp` del esquema) antes del ETL y antes del go-live.
- Operación: log de Tomcat con `traceId`, `/actuator/health`, pantalla `/analisis/auditoria`; retención del `portal.files.root` en el respaldo del servidor.

---

## Resumen de esfuerzo (nuestro lado, anclado a precedentes del proyecto)

| Fase | Est. |
|---|---|
| 0 Decisiones/infra | depende de DBA/Infra (nuestro lado: ½ jornada de acompañamiento) |
| 1 Inventario ORDS QA vs prod | 1 jornada |
| 2 Base del portal | ½ jornada |
| 3 ORDS prod | 1 jornada |
| 4 Catálogos/roles (sin ETL) | ½ jornada + negocio |
| 5 Despliegue + smoke | 1 jornada |
| **Total nuestro** | **~4.5 jornadas** + ETL externo + tiempos de negocio (predios) |

## Checklist de salida (go/no-go)

- [ ] 0.1 base del portal definida y `application-prod.properties` cableado (sin `ONEWMS_QA`)
- [ ] Puerto 3306 abierto para el ETL; `files.root` escribible
- [ ] Matriz Fase 1 sin "no existe" pendientes
- [ ] Scripts Fase 2 aplicados; WAR arranca con `validate`
- [ ] ETL ejecutado y verificado (conteos de `etl-migracion-historica-especificacion.md` §6)
- [ ] Predios/cuentas cargados (al menos las cuentas activas)
- [ ] Roles y menús cargados; primer ADMIN puede entrar
- [ ] Smoke test verde (login, NSS, Análisis, PDFs, Auditoría)
- [ ] nginx en prod con la config de QA
- [ ] Respaldo del esquema tomado
