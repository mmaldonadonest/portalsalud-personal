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
