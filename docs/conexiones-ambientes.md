# Conexiones por ambiente — Portal Salud Personal

**Regla de oro (23-sep-2026, confirmada por el usuario):** el agente **nunca** ejecuta operaciones
contra ninguna base de datos. Prepara los `.sql` y la documentación; **el usuario los corre** en SQL
Developer / su cliente. Aplica a ORDS *y* a la base del portal, en cualquier ambiente.

**Los usuarios y contraseñas NO van en este archivo.** Este repo está publicado en GitHub
(`mmaldonadonest/portalsalud-personal` y `Onest-Development/PORTAL-SALUD`). Las credenciales se
guardan en `credenciales-acceso.local.html` (local) o en el gestor de contraseñas del equipo.

---

## Ambientes

| Ambiente | Base del portal (JDBC, `APP_*`, `SERV_MED_TAG`) | ORDS / legacy (WS `Servcio/*`) |
|---|---|---|
| Local | Oracle XE `localhost:1521/PROYECTO_BASE_PDB`, esquema `USR_BOILERPLATE` | — (consume la de QA) |
| QA | `200.94.116.132:1521/orclpdb`, esquema `ONEWMS_QA` | `http://10.249.249.3/biows/ords/security/Servcio` |
| **Producción** | **`PDBPRD`, tablas `SERV_MED_*` dentro de `BIOMETRICO`** (alias TNS `PDBPRD`) | `http://10.249.249.3/biows/ords/security/Servcio` |

## Producción — base de ORDS (entregada e inventariada 23-sep-2026)

| Dato | Valor |
|---|---|
| Base / PDB | `PDBPRD` |
| Host | `onestdb` |
| Esquema / usuario | `BIOMETRICO` |
| Versión | Oracle 19c Enterprise Edition |
| Tablespace | `BIOMETRICO` (105 GB usados) |
| URL base de ORDS | _(pendiente — confirmar si es la misma `10.249.249.3/biows/ords/security`)_ |
| ¿Trae el esquema legacy? | **Sí**, completo y con datos productivos (33,907 empleados) |
| ¿Trae las 11 tablas creadas por el portal? | **Sí, ya existen** → ver `fase1-inventario-resultado.md` |
| ¿Alojará el esquema `APP_*` del portal? | **No se recomienda**: ya hay 10 tablas `APP_*` de otro sistema y el usuario tiene privilegios de DDL. Propuesta: esquema propio en la misma `PDBPRD` |

**Importante (hallazgo del inventario):** todo indica que la ORDS que usamos como "QA"
(`10.249.249.3`) apunta a **esta misma base**, es decir que el desarrollo se hizo contra
producción. Confirmarlo con `docs/ords-inventario-produccion-seguimiento.sql` §1.

### Primer paso, sin tocar nada: inventario de sólo lectura

`docs/ords-inventario-produccion.sql` — **sólo `SELECT`** sobre el diccionario de datos y conteos.
Lo corre el usuario y pega la salida. Responde:

1. Si esa base es realmente la del servicio médico (tablas `BIO_*`, `TBL_SERV_*`, ~41 del examen).
2. Cuáles de las 11 tablas que creó el portal ya existen y cuáles faltan (Fase 3).
3. Estado de procedimientos/funciones (`PR_SERVICIO_MED_*`) y si hay objetos `INVALID`.
4. Qué valores usa `EMP_STATUS` aquí (criterio real de empleado vigente).
5. Si el gate del SSO (`TBL_APPS_ROL_MENU`, `id_app = 13`) ya tiene menús por rol.
6. Si ya hay tablas `APP_*` / `SERV_MED_TAG` en ese esquema.
7. Privilegios y espacio del usuario (para el ETL).

Con esa salida se llena la matriz "existe / falta" de la Fase 1 y se decide qué scripts
`docs/ords-*.sql` aplicar.

## Configuración del perfil `prod`: dentro del WAR

**Ya no se usan variables de entorno ni `setenv`** (decisión 14-oct-2026, confirmada 19-oct-2026).
Las variables y el `setenv` que describía antes esta sección quedaron fuera: un placeholder en
`setenv.bat` bloqueó la cuenta `BIOMETRICO` el 27-sep-2026.

Toda la configuración de producción va dentro del WAR. Maven la mete al compilar con `-Pprod`,
desde `config/produccion/portal-salud.local.properties`. La base es `BIOMETRICO` en `PDBPRD`, por
el alias TNS `PDBPRD`. El servidor solo pone el `tnsnames.ora` y la carpeta de archivos.

El stack, los requisitos del servidor, el build y el despliegue están en **`README.md`**.

Verificación al arrancar: el bloque `BASE DE DATOS ACTIVA` del log imprime la URL JDBC y el
`DB_NAME` que realmente tomó.

## Recordatorio de seguridad

`docs/etl-migracion-historica-especificacion.md` incluye usuario y contraseña de la MariaDB y de
Oracle QA **en texto plano**, y el repo ya está subido a GitHub. Conviene: quitar esos valores del
documento (dejar sólo el nombre del recurso), **rotar esas contraseñas**, y mantener el detalle en
`credenciales-acceso.local.html` fuera del control de versiones (añadirlo a `.gitignore`).
