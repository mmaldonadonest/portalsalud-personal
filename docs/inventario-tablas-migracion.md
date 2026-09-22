# Listado de tablas de la migración — Portal Salud Personal

**Fecha:** 22-sep-2026. Complementa `plan-migracion-produccion.md`, `etl-migracion-historica-especificacion.md` (ETL) y `muestra-tablas-mysql-migracion.html` (muestra del origen).

Hay **cuatro grupos distintos** y conviene no mezclarlos: sólo el grupo A mueve datos de un motor a otro; B y C son estructura que hay que crear en producción; D no se toca.

---

## A. Datos que se MIGRAN (MariaDB → Oracle) — 2 tablas

Es todo el alcance del ETL. Verificado con `SHOW TABLES FROM servicioMedico` (20-ago-2026): la base legacy sólo tiene estas dos tablas.

| # | Origen MariaDB `servicioMedico` | Filas (snapshot 13-ago) | Destino | Notas |
|---|---|---|---|---|
| 1 | `files` | 21,475 (~18 GB) | `APP_FS_FILE` (metadatos) + **filesystem** `portal.files.root` (binario) | El base64 de `url` **no** entra a Oracle; se decodifica a disco con sharding `yyyy/MM/dd/<2 hex sha256>/<uuid>.<ext>`. Excluir 27 filas con `url` vacío |
| 2 | `tags` | 550,560 (~52 MB) | `MED_TAG` | EAV puro (`type`=campo, `content`=valor), `longtext` → `CLOB`; `TAG_GROUP` lo calcula `FN_MED_TAG_GROUP` en destino |

Marca de origen: ambas cargas escriben `CREATED_BY = 'ETL_LEGACY'`, que es lo que permite distinguirlas después de lo capturado por el portal.

## B. Tablas que se CREAN en la base del portal (JDBC, esquema `APP_*`) — 22

Estructura nueva; sin datos que viajen del legacy, salvo las semillas y lo que se copie de QA (roles, menús).

| Grupo | Tablas | Script | Datos que hay que cargar en prod |
|---|---|---|---|
| Seguridad / RBAC | `APP_SEC_USER`, `APP_SEC_ROLE`, `APP_SEC_USER_ROLE`, `APP_SEC_PERMISSION`, `APP_SEC_ROLE_PERMISSION` | `00_init_oracle21c.sql`, `01_rbac_local.sql` | Roles (`USER`, `ADM`, `ENFERMERO`, `ROLE_ADMIN`, `ROLE_MEDICO_ANALISTA`) y el primer usuario ADMIN. Sin contraseñas: el login valida contra ORDS |
| Menús | `APP_MENU`, `APP_MENU_ROLE` | `01_rbac_local.sql`, `02_fix_expediente_general_duplicado.sql`, `03_menu_examenes.sql` | 20 menús (13 del expediente + 4 archivos//otros + 3 de Exámenes) y su asignación por rol |
| Auditoría | `APP_AUD_EVENT`, `APP_AUDIT_SQL_EXECUTION` | `00_init_oracle21c.sql` | Ninguno (arranca vacía) |
| Archivos | `APP_FS_FILE` + satélites `APP_FS_FILE_VERSION`, `APP_FS_FILE_ACCESS_LOG`, `APP_FS_FILE_POLICY`, `APP_FS_FILE_ORPHAN_TRACKING` | `00_init_oracle21c.sql`, `app_domain/app-fs-file.sql` (+ `-reconcile`) | Los metadatos los pone el ETL (grupo A). **Los satélites no los usa el código**, existen por las FKs del script autoritativo |
| Importador Excel | `APP_IMPORT_LOTE`, `APP_IMPORT_FILA` | `app_domain/app-import.sql` | Ninguno (staging; los lotes de QA no se migran) |
| EAV histórico | `MED_TAG` + `MED_TAG_MIG_LOG` (bitácora de la corrida) | `app_domain/tags-salud.sql` | El ETL (grupo A). Incluye la función `FN_MED_TAG_GROUP` |
| Jobs / notificaciones | `APP_JOB_CATALOG`, `APP_NOTIF_TEMPLATE` | `00_init_oracle21c.sql` | Ninguno |
| Diseño anterior (no usado) | `MED_FILE`, `MED_FILE_MIG_LOG` | `app_domain/files-salud.sql` | **No las usa el portal**: el código escribe en `APP_FS_FILE`. Se dejan por compatibilidad del script; no cargar nada |

> Ojo con `APP_FS_FILE`: el script autoritativo crea el índice único `UX_FS_FILE_CHECKSUM`, y en `files` hay **261 duplicados legítimos de contenido** — hay que relajar el índice o manejar el `ORA-00001` antes de correr el ETL.

## C. Tablas que se CREAN en ORDS (esquema legacy) — 11

Las creó este proyecto para módulos que el PHP no tenía. Van con sus scripts `docs/ords-*.sql` (Fase 3 del plan); **nunca se modifica un WS productivo existente, se clona**.

| # | Tabla | Módulo | Script |
|---|---|---|---|
| 1 | `SERV_MED_PREDIO` | Catálogo de predios (17 filas sembradas) | `ords-predio-cuenta.sql` |
| 2 | `SERV_MED_CUENTA_PREDIO` | Asignación cuenta → predio | `ords-predio-cuenta.sql` |
| 3 | `SERV_MED_CAT_CAUSA_CONSULTA` | Causas de consulta (23 sembradas) | `ords-causa-consulta.sql` |
| 4 | `SERV_MED_RESTRICCION_ASIGNADA` | Restricciones médicas (16 códigos fijos) | `ords-restriccion.sql` |
| 5 | `SERV_MED_ACCIDENTE` | Accidentes | `ords-accidentes.sql` |
| 6 | `SERV_MED_ACCIDENTE_SEGUIMIENTO` | Seguimiento de accidentes | `ords-accidentes-seguimiento.sql` |
| 7 | `SERV_MED_ANTIDOPING_RESULTADO` | Antidoping | `ords-antidoping.sql` |
| 8 | `SERV_MED_ANTIDOPING_INVENTARIO` | Consumibles / kits | `ords-antidoping.sql` |
| 9 | `SERV_MED_ANTIDOPING_SELECCION` | Selección aleatoria | `ords-antidoping-seleccion.sql` |
| 10 | `SERV_MED_MATERNIDAD_SEGUIMIENTO` | Maternidad | `ords-maternidad.sql` |
| 11 | `SERV_MED_RESULTADO_EXAMEN_HIST` | Historial de exámenes | `ords-examen-historial.sql` |

Datos a cargar: predios y causas vienen sembrados en los scripts; **cuenta → predio lo captura negocio** (hoy 2 de 260 cuentas asignadas). Todo lo demás arranca vacío tras la limpieza de datos de prueba.

## D. Tablas que NO se migran ni se crean — se consumen tal cual

Ya viven en la Oracle del legacy y el portal las lee/escribe **por los WS de ORDS**, nunca por JDBC. No entran al ETL ni a los scripts de creación.

| Familia | Ejemplos | Qué son |
|---|---|---|
| Consultas e incapacidades | `TBL_SERV_CONSULTA_MEDICA`, `TBL_SERV_INCAPACIDAD_MEDICA` | Historial clínico productivo del PHP |
| Examen médico | ~41 tablas `SERV_MED_*` (`_GENERALES`, `_EXPLORACION_FISICA`, `_DIAGNOSTICO`, `_RESULTADO_EXAMEN`, `_HEREDOFAMILIAR`, `_ANT_LABORALES`…) | Una por sección del examen; las escribe `PR_SERVICIO_MED_EXAMEN1/2` |
| Archivos del legacy | `SERV_MED_FILES`, `SER_MED_REGISTRO_MEDICO` | Índice de archivos y registro médico del PHP |
| Empleados y cuentas | `BIO_EMPLEADO`, `BIO_DATOS_LABORALES_EMPLEADOS`, `biometrico_cuenta`, `biometrico_cuenta_SAP` | Plantilla y cuentas (vienen de RH/biométrico). Vigencia por la bandera `EMP_STATUS`, no por fecha |
| Apps y permisos del launcher | `TBL_APPS_ONEST`, `TBL_APPS_ROL`, `TBL_APPS_USUARIO`, `TBL_APP_ROL_USUARIO`, `TBL_APPS_ROL_MENU` | Gate del SSO (`id_app = 13`) |
| Catálogos | ICD/CIE (909 claves, capítulos A–B) | Catálogo del WS |
| Depuración | `BUG`, `PRUEBA`, `ONSYS_DEBUG` | Basura de los handlers: se vacían en la limpieza previa, no se migran |

---

## Resumen

| Grupo | Tablas | Mueve datos | Responsable |
|---|---|---|---|
| A — ETL legacy | **2** | Sí (21,475 archivos + 550,560 tags) | Equipo ETL externo |
| B — Base del portal | **22** | No (sólo semillas y roles/menús de QA) | DBA + Miguel |
| C — ORDS nuevas | **11** | No (semillas de catálogo; cuenta→predio lo captura negocio) | Miguel |
| D — Legacy existente | ~60 | No se toca | — |
