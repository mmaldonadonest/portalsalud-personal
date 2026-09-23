# Conexiones por ambiente — Portal Salud Personal

**Regla de oro (23-sep-2026, confirmada por el usuario):** el agente **nunca** ejecuta operaciones
contra ninguna base de datos. Prepara los `.sql` y la documentación; **el usuario los corre** en SQL
Developer / su cliente. Aplica a ORDS *y* a la base del portal, en cualquier ambiente.

**Los usuarios y contraseñas NO van en este archivo.** Este repo está publicado en GitHub
(`mmaldonadonest/portalsalud-personal` y `Onest-Development/PORTAL-SALUD`). Las credenciales se
guardan en `credenciales-acceso.local.html` (local) o en el gestor de contraseñas del equipo.

---

## Ambientes

| Ambiente | Base del portal (JDBC, `APP_*`, `MED_TAG`) | ORDS / legacy (WS `Servcio/*`) |
|---|---|---|
| Local | Oracle XE `localhost:1521/PROYECTO_BASE_PDB`, esquema `USR_BOILERPLATE` | — (consume la de QA) |
| QA | `200.94.116.132:1521/orclpdb`, esquema `ONEWMS_QA` | `http://10.249.249.3/biows/ords/security/Servcio` |
| **Producción** | **por definir — Fase 0.1 del plan** | **base entregada 23-sep-2026, datos abajo** |

## Producción — base de ORDS (entregada 23-sep-2026)

Por completar con lo que entregó infraestructura (sin contraseñas):

| Dato | Valor |
|---|---|
| Host / IP | _(pendiente)_ |
| Puerto | _(pendiente)_ |
| Servicio / SID / PDB | _(pendiente)_ |
| Esquema / usuario | _(pendiente)_ |
| URL base de ORDS | _(pendiente)_ |
| ¿Ya trae el esquema legacy cargado? (`BIO_EMPLEADO`, `TBL_SERV_*`) | _(lo responde el inventario)_ |
| ¿Alojará también el esquema `APP_*` del portal? | _(decisión 0.1)_ |

### Primer paso, sin tocar nada: inventario de sólo lectura

`docs/ords-inventario-produccion.sql` — **sólo `SELECT`** sobre el diccionario de datos y conteos.
Lo corre el usuario y pega la salida. Responde:

1. Si esa base es realmente la del servicio médico (tablas `BIO_*`, `TBL_SERV_*`, ~41 del examen).
2. Cuáles de las 11 tablas que creó el portal ya existen y cuáles faltan (Fase 3).
3. Estado de procedimientos/funciones (`PR_SERVICIO_MED_*`) y si hay objetos `INVALID`.
4. Qué valores usa `EMP_STATUS` aquí (criterio real de empleado vigente).
5. Si el gate del SSO (`TBL_APPS_ROL_MENU`, `id_app = 13`) ya tiene menús por rol.
6. Si ya hay tablas `APP_*` / `MED_TAG` en ese esquema.
7. Privilegios y espacio del usuario (para el ETL).

Con esa salida se llena la matriz "existe / falta" de la Fase 1 y se decide qué scripts
`docs/ords-*.sql` aplicar.

## Recordatorio de seguridad

`docs/etl-migracion-historica-especificacion.md` incluye usuario y contraseña de la MariaDB y de
Oracle QA **en texto plano**, y el repo ya está subido a GitHub. Conviene: quitar esos valores del
documento (dejar sólo el nombre del recurso), **rotar esas contraseñas**, y mantener el detalle en
`credenciales-acceso.local.html` fuera del control de versiones (añadirlo a `.gitignore`).
