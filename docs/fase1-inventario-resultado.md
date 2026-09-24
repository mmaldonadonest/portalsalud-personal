# Fase 1 — Resultado del inventario de la base de ORDS (23-sep-2026)

Salida de `docs/ords-inventario-produccion.sql` ejecutada por el usuario contra la base entregada.

## Identificación de la base

| Dato | Valor |
|---|---|
| Base / PDB | `PDBPRD` |
| Esquema / usuario | `BIOMETRICO` |
| Host | `onestdb` |
| Versión | Oracle Database **19c** Enterprise Edition |
| Tablespace | `BIOMETRICO` (105 GB usados) + `BIOMETRICO_INDEX` (719 MB) |

**Es la base del servicio médico**: las 15 tablas del legacy existen y traen datos productivos
(33,907 empleados, 209 consultas, 293 incapacidades, 3,213 exámenes y 3,213 archivos).

---

## Hallazgo 1 (crítico) — Las 11 tablas del portal YA EXISTEN aquí

Las 11 tablas creadas por este proyecto (`SERV_MED_PREDIO`, `SERV_MED_CUENTA_PREDIO`,
`SERV_MED_CAT_CAUSA_CONSULTA`, `SERV_MED_RESTRICCION_ASIGNADA`, `SERV_MED_ACCIDENTE` +
`_SEGUIMIENTO`, `SERV_MED_ANTIDOPING_RESULTADO` / `_INVENTARIO` / `_SELECCION`,
`SERV_MED_MATERNIDAD_SEGUIMIENTO`, `SERV_MED_RESULTADO_EXAMEN_HIST`) **ya están creadas**.

La explicación más probable: la ORDS que usamos todo el desarrollo (`http://10.249.249.3/biows/...`,
etiquetada como "QA") **apunta a esta misma base**. Evidencia adicional: `PR_SERVICIO_MED_EXAMEN2`
tiene como último cambio el **17/08/2026**, en plena ejecución de este proyecto.

**Consecuencias:**
- **No hay Fase 3**: no hay que crear tablas ni handlers nuevos en ORDS, ya están. Queda sólo
  verificar los WS con `curl` y revisar si falta alguno de los clones (`*_cta`).
- **La limpieza de datos de prueba (tarea 1.0) es obligatoria y va aquí**, sobre producción. Los
  registros que capturamos probando (NSS `30048315698`, `90099119373`, `68958027838`) están en
  esta base, junto a los datos reales del PHP.
- Conviene asumir que durante el desarrollo se escribió en producción y revisarlo con la consulta
  de seguimiento §1.

## Hallazgo 2 (crítico) — El `id_app` del SSO: 13 vs 27

`TBL_APPS_ONEST` tiene **dos** aplicaciones relacionadas:

| id_app | Nombre | SSO |
|---|---|---|
| 13 | `SERVICIO MEDICO` | **0** (no entra por el launcher) |
| 27 | `PORTAL SALUD` | **1** (sí entra por el launcher) |

El portal Java valida el gate del SSO con `portal.biows.app-id=13` (`application.properties`).
Pero **los menús de `TBL_APPS_ROL_MENU` sólo existen para `id_app = 13`** (roles 1, 2 y 3 con 11,
14 y 9 menús); para la 27 no hay ninguno.

**Decisión pendiente:** ¿el portal Java es la app 27 (la que el launcher publica con SSO) o sigue
siendo la 13? De eso depende:
- Qué `id_app` debe llevar `portal.biows.app-id` en producción.
- Si hay que dar de alta roles y menús para la app 27 en `TBL_APPS_ROL_MENU` / `TBL_APP_ROL_USUARIO`.
- Recordar que el **menú lateral** sale del esquema local (`portal.permissions.source=LOCAL`), pero
  el **gate de entrada por SSO** sigue consultando ORDS con ese `id_app`: si el usuario no tiene rol
  ahí, no entra aunque el JWT sea válido.

## Hallazgo 3 — El esquema del portal NO debe vivir en `BIOMETRICO`

- Ya hay **10 tablas `APP_*` de otro sistema** (`APP_CONFIG_EXCEL`, `APP_TAREAS`, `APP_REPORTES`,
  `APP_DEBUG`, `APP_ESTADO`, …). Ninguna choca por nombre exacto con las nuestras
  (`APP_SEC_*`, `APP_MENU*`, `APP_AUD_EVENT`, `APP_FS_FILE`, `APP_IMPORT_*`), pero el prefijo ya
  está ocupado por terceros y el riesgo de colisión futura es real.
- **`MED_TAG` no existe** aquí (confirma que el histórico aún no se migró a esta base).
- El usuario `BIOMETRICO` tiene `CREATE TABLE`, `CREATE ANY VIEW`, `DROP ANY VIEW`, `CREATE ROLE`,
  `CREATE JOB`, `EXP/IMP_FULL_DATABASE` y `UNLIMITED TABLESPACE`: demasiados privilegios para que
  una aplicación web se conecte con él.

**Recomendación para la decisión 0.1:** esquema propio (por ejemplo `PORTAL_SALUD`) en esta misma
instancia `PDBPRD`, con un usuario que sólo tenga `CREATE SESSION` + DML sobre sus tablas, y un
segundo usuario de sólo lectura para pruebas.

## Hallazgo 4 — `EMP_STATUS`: no es booleano

| EMP_STATUS | Empleados | Ingreso mín. | Ingreso máx. |
|---|---|---|---|
| 0 | 3,584 | 23-APR-71 | **23-SEP-26 (hoy)** |
| 1 | 28,155 | 07-JUL-14 | 15-DEC-99 |
| 99 | 2,154 | 05-MAY-05 | 09-JUN-99 |
| 100 | 12 | 10-FEB-23 | 17-OCT-23 |
| (nulo) | 2 | 27-JUN-23 | 10-OCT-23 |

Indicios de que **`0` = vigente**: las altas más recientes (hasta hoy) tienen status 0, y el WS de
login del legacy exige `emp_status = 0` — como los logins funcionan, ese es el valor de un empleado
activo. Los 28,155 en status 1 serían el histórico de bajas. **Falta confirmarlo con RH/biométrico**
(consulta de seguimiento §2), y aclarar qué significan 99 y 100.

Recordatorio del hallazgo previo: el WS de examen (`contextoWS.txt` ~línea 2497) valida
`EMP_status = 1`. Si 0 es el vigente, ese handler está comprobando lo contrario.

## Hallazgo 5 — El catálogo ICD sí existe, con otro nombre

Mi consulta buscó `%ICD%` / `%CIE%` y no encontró nada. La tabla real es
**`SERV_MED_CAT_INDICE_IDC10`** (con "IDC", no "ICD"), que es la que consulta el WS
`Servcio/indice`. Falta contar sus filas para saber si aquí también están sólo los capítulos A y B
(909 claves en lo que veíamos) — consulta de seguimiento §3.

## Hallazgo 6 — 91 objetos INVALID, ninguno del portal

Los inválidos son de otros módulos del biométrico (vistas de asistencia, nómina, capacitación,
procedimientos `PRO_*`, `BIO_*`, clases Java `BASE64`/`Authenticate`). **Los del servicio médico
están VALID**: `PR_SERVICIO_MED_CONSULTA`, `PR_SERVICIO_MED_EXAMEN1`, `PR_SERVICIO_MED_EXAMEN2`,
`PR_BIO_CONS_SERVICIO_MEDICO`, `PR_BIO_GUARDA_SERVICIO_MEDICO1`. No bloquea al portal, pero vale la
pena reportarlo al DBA: un esquema con 91 objetos inválidos es señal de recompilaciones pendientes.

## Hallazgo 7 — Las tablas de depuración existen

`BUG`, `PRUEBA` y `ONSYS_DEBUG` están creadas: aplica lo previsto en el runbook de limpieza.

---

## Cómo queda la matriz de la Fase 1

| Grupo | Resultado |
|---|---|
| Tablas del legacy que el portal consume (15) | **Todas existen**, con datos productivos |
| Tablas creadas por el portal (11) | **Todas existen** → la Fase 3 se reduce a verificar WS |
| Procedimientos del servicio médico | **VALID** |
| Catálogo ICD | Existe como `SERV_MED_CAT_INDICE_IDC10` (falta conteo) |
| Esquema del portal (`APP_SEC_*`, `MED_TAG`) | **No existe** → Fase 2, en esquema propio |
| Tablas de depuración | Existen → limpieza |

## Siguientes pasos

1. Correr `docs/ords-inventario-produccion-seguimiento.sql` (sólo `SELECT`) y revisar §1 (datos de
   prueba en producción), §2 (`EMP_STATUS`), §3 (ICD) y §4 (apps 13 vs 27).
2. **Decidir el `id_app`** (13 o 27) con quien administra el launcher.
3. **Decidir el esquema del portal** (0.1) — recomendación: esquema y usuario propios en `PDBPRD`.
4. Ejecutar la limpieza de datos de prueba aquí, con respaldo previo.
5. Verificar los WS con `curl` (lo que queda de la Fase 3).

---

# Resultado del seguimiento (misma sesión, 23-sep-2026)

Salida de `docs/ords-inventario-produccion-seguimiento.sql`.

## §1 — CONFIRMADO: el desarrollo se hizo contra producción

Los datos de prueba están en esta base. La ORDS que llamábamos "QA" es esta misma.

| Dónde | Filas de prueba | Detalle |
|---|---|---|
| `TBL_SERV_CONSULTA_MEDICA` | **3** | REG_ID 1068 (11-sep), 1088 (14-sep), 1108 (19-sep) — NSS `30048315698` |
| `TBL_SERV_INCAPACIDAD_MEDICA` | **8** | REG_ID 1661–1665 (27-ago), 1681, 1682 (2-sep), 1701 (14-sep) |
| `SERV_MED_RESULTADO_EXAMEN` / `_GENERALES` / `SERV_MED_FILES` | 2 cada una | |
| Tablas creadas por el portal | accidentes 4 (+4 seguimiento), antidoping 7 / 7 / 2, maternidad 2, restricciones 3, examen_hist 50, cuenta_predio 4 | todo es nuestro: el PHP no tiene esos módulos |
| Catálogos | predios **17**, causas **24** | se conservan |
| Depuración | **BUG 299,683** · PRUEBA 11,355 · ONSYS_DEBUG 290 | basura de los `insert into bug` de los handlers |

**Dos NSS que NO son de prueba y no hay que tocar:**
- `07180371705` — incapacidad del **23-SEP-26** (de hoy): captura real del PHP.
- `92088502122` — 1 consulta + 1 incapacidad del 25-ago: **confirmar con negocio**. Aparece como
  ejemplo en una pantalla del portal, pero pudo ser captura real.

El script `ords-limpieza-datos-prueba.sql` ya quedó actualizado con estos números y avisos.

## §2 — CONFIRMADO: `EMP_STATUS = 0` es el empleado vigente

Los tres NSS conocidos (los tres activos) tienen **status 0**: Miguel `30048315698` (alta 22-nov-10),
Mauricio `68958027838` (30-ene-17), Mariana `90099119373` (29-jun-26).

Altas de los últimos 90 días: **652 con status 0**, 500 con 1, 9 con 99 — es decir, `1` no es sólo
histórico viejo: hay bajas recientes. Interpretación de trabajo: **0 = vigente, 1 = baja**; `99`
(2,154 registros, altas de 2002-2016) y `100` (12) son otros estados **por confirmar con RH**.
Hay 2 registros con status nulo, uno de ellos el NSS dummy `00000000099`.

Sigue en pie el hallazgo previo: el WS de examen valida `EMP_status = 1`, o sea lo contrario.

## §3 — CONFIRMADO: el catálogo ICD tiene el mismo hueco

`SERV_MED_CAT_INDICE_IDC10` — columnas `CLAVE_ID` (no `CLAVE`, por eso mi consulta falló con
`ORA-00904`, sin consecuencia) y `CLAVE_NOMBRE`. **909 claves**, las mismas de siempre: sólo
capítulos A y B (`A96.X`, `A98.0`, …). Si la nota médica debe llevar diagnóstico codificado
completo, hay que cargar el CIE-10 íntegro — es tema de negocio, no del portal.

## §4 — CORRECCIÓN IMPORTANTE sobre el `id_app`

Con la salida en mano, el asunto es distinto a lo que escribí arriba:

- **El gate del SSO del portal Java NO usa ORDS en producción.** Con `portal.permissions.source=LOCAL`
  (lo que trae el perfil `prod`), `BiowsModulePermissionClient` ni siquiera se instancia: quien
  responde `findRoleId` es `LocalModulePermissionClient`, contra `APP_SEC_USER` de la base del
  portal. Por lo tanto **`portal.biows.app-id=13` es irrelevante mientras la fuente sea LOCAL**.
- **Lo que sí depende de ORDS es el launcher:** muestra el mosaico de una app cuando el usuario
  tiene rol en ella (`TBL_APP_ROL_USUARIO`). Hoy:
  - app **13 SERVICIO MEDICO** (`sso=0`): 39 usuarios — 2 con rol 1 (`USER`), 23 con rol 2 (`ADM`),
    14 con rol 3 (`ENFERMERO`). Son los del **PHP v2**.
  - app **27 PORTAL SALUD** (`sso=1`): **0 usuarios, 0 roles, 0 menús**.
- De los NSS conocidos, sólo Mauricio `68958027838` tiene rol (app 13, rol 2 = `ADM`). Miguel y
  Mariana no tienen rol en ninguna app: por eso a Mariana no le aparecía el portal en el launcher.

**Qué hay que hacer para que la gente entre por el launcher:**
1. Dar de alta **roles** para la app 27 en `TBL_APPS_ROL` (los mismos tres: `USER`=1, `ADM`=2,
   `ENFERMERO`=3) y **usuarios** en `TBL_APP_ROL_USUARIO` con `id_app=27`. Se pueden copiar los 39
   de la app 13. **Sin mover los de la 13**, que los sigue usando el PHP v2.
2. Cargar esos mismos usuarios en la base del portal (`APP_SEC_USER` + rol), que es lo que de
   verdad valida el portal Java. Es parte de la Fase 4.
3. `portal.biows.app-id`: dejarlo en **27** por coherencia (y porque aplicaría si alguna vez se
   cambia a `source=ORDS`). Conviene exponerlo como variable de entorno.

No hace falta cargar menús en `TBL_APPS_ROL_MENU` para la 27: el menú lateral del portal Java sale
del esquema local (`APP_MENU` / `APP_MENU_ROLE`).

## Qué cambia en el plan

| Antes | Ahora |
|---|---|
| Fase 3 = aplicar 27 scripts `ords-*.sql` | **Ya están aplicados.** Queda sólo verificar los WS con `curl` |
| Limpieza de datos de prueba = "en QA" | **Va aquí, en producción**, con respaldo previo |
| Decisión 0.1 pendiente sin datos | Con datos: **esquema propio** (10 tablas `APP_*` ajenas, usuario con DDL) |
| `EMP_STATUS` incógnita | **0 = vigente**; falta confirmar 99 y 100 con RH |
| ICD "quizá esté completo en prod" | **909 claves, mismo hueco** |
| "El portal valida el SSO con id_app=13" | **Con `source=LOCAL` valida local**; el `id_app` importa para el launcher, y ahí la app es la **27**, hoy sin usuarios |

---

## Decisión sobre la limpieza (23-sep-2026) — CANCELADA la limpieza por NSS

**El borrado de datos de prueba por NSS queda descartado definitivamente.** No se ejecutará desde
script, ni ahora ni más adelante. Si en algún momento hay que depurar registros concretos, lo hará
el usuario **a mano, fila por fila**. `docs/ords-limpieza-datos-prueba.sql` quedó con **todo su
contenido comentado**, sólo como referencia histórica de qué se habría borrado y con qué criterio.

**No se borra ningún dato por NSS.** El usuario depurará manualmente los registros de prueba
cuando lo decida. De la limpieza previa a producción **sólo se harán las tablas de paso / debug**,
y también de forma manual: `docs/ords-limpieza-debug.sql` (BUG 299,683 · PRUEBA 11,355 ·
ONSYS_DEBUG 290). `docs/ords-limpieza-datos-prueba.sql` queda **EN PAUSA**, como referencia.

### Por qué fue acertado revisar antes: `SERV_MED_RESULTADO_EXAMEN_HIST`

Las 50 filas **no son todas nuestras**. Reparto real:

| NSS | Filas | |
|---|---|---|
| `30048315698` | 26 | NSS de prueba |
| `68917304278` | 7 | |
| `02229815655` | 6 | |
| `07160015108` | 3 | |
| `45927014667` | 2 | |
| `62150082097` | 2 | |
| `26179715326` | 1 | |
| `30190093523` | 1 | **usuario real de la app 13 del PHP** (aparece en `TBL_APP_ROL_USUARIO`) |
| `68958027838` | 1 | Mauricio (usuario real, además de probador) |
| `92088502122` | 1 | el NSS en duda |

Es decir, **24 de 50 filas son historial ajeno**. El script traía `DELETE FROM
SERV_MED_RESULTADO_EXAMEN_HIST;` sin condición, que las habría borrado: ya quedó corregido a un
`DELETE ... WHERE NSS IN (...)` comentado. Lo mismo aplica a `SERV_MED_CUENTA_PREDIO`, que pasó de
2 filas (14-sep) a 4 (23-sep): conviene ver el detalle antes de tocarla.

Conclusión operativa: las tablas que creó el portal **no son "sólo nuestras"** — el PHP v2 también
escribe en algunas a través de los procedimientos compartidos. Cualquier borrado futuro debe ir
acotado por NSS y revisado fila por fila.
