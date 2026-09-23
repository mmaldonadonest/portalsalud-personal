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

## Variables de entorno del perfil `prod`

`application-prod.properties` ya no trae valores fijos: todo se resuelve como
`${VARIABLE:valor_por_defecto}`. Si la variable existe en el entorno del Tomcat, gana.

| Variable | Propiedad | Default en el WAR |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `spring.datasource.url` | QA (`200.94.116.132/orclpdb`) — **provisional** |
| `SPRING_DATASOURCE_USERNAME` | `spring.datasource.username` | `ONEWMS_QA` |
| `SPRING_DATASOURCE_PASSWORD` | `spring.datasource.password` | **vacío — obligatorio definirla** |
| `PORTAL_FILES_ROOT` | `portal.files.root` | `/mnt/data/onedev/apps/exec/filessalud` |
| `PORTAL_BIOWS_BASEURL` | `portal.biows.base-url` | `http://10.249.249.3/biows/ords/security` |
| `PORTAL_SSO_JWKS_URI` | `portal.sso.jwks-uri` | `https://sso.onestcloud.mx/sso/.well-known/jwks.json` |
| `PORTAL_SSO_ISSUER_URI` | `portal.sso.issuer-uri` | `https://sso.onestcloud.mx/sso` |
| `PORTAL_AUTH_STRATEGY` | `portal.auth.strategy` | `LEGACY_PHP` |
| `PORTAL_PERMISSIONS_SOURCE` | `portal.permissions.source` | `LOCAL` |

**Linux (servidor):** `$CATALINA_HOME/bin/setenv.sh` o el unit de systemd:
```sh
export SPRING_DATASOURCE_URL="jdbc:oracle:thin:@HOST:1521/SERVICIO"
export SPRING_DATASOURCE_USERNAME="USUARIO"
export SPRING_DATASOURCE_PASSWORD="********"
export CATALINA_OPTS="$CATALINA_OPTS -Dspring.profiles.active=prod"
```

**Windows (pruebas locales):** `C:	omcatpache-tomcat-11.0.21in\setenv.bat`:
```bat
set "SPRING_DATASOURCE_URL=jdbc:oracle:thin:@HOST:1521/SERVICIO"
set "SPRING_DATASOURCE_USERNAME=USUARIO"
set "SPRING_DATASOURCE_PASSWORD=********"
set "CATALINA_OPTS=%CATALINA_OPTS% -Dspring.profiles.active=prod"
```

`setenv.sh` / `setenv.bat` viven en el Tomcat, **fuera del repo**: ahí sí puede ir la contraseña.
Verificación al arrancar: el banner `BASE DE DATOS ACTIVA` del log imprime el `JDBC URL` y el
`DB_NAME` que realmente tomó.

## Recordatorio de seguridad

`docs/etl-migracion-historica-especificacion.md` incluye usuario y contraseña de la MariaDB y de
Oracle QA **en texto plano**, y el repo ya está subido a GitHub. Conviene: quitar esos valores del
documento (dejar sólo el nombre del recurso), **rotar esas contraseñas**, y mantener el detalle en
`credenciales-acceso.local.html` fuera del control de versiones (añadirlo a `.gitignore`).
