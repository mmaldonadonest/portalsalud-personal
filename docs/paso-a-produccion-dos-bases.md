# Paso a producción — hay DOS bases de datos y cada script va a una

**Importante.** El portal Java trabaja contra **dos** Oracle distintos. Confundirlos no da error al
aplicar el script (la tabla se crea igual), pero el portal no la encuentra y falla en tiempo de
ejecución ("table or view does not exist"). Ya pasó una vez en QA con el importador Excel (11-sep-2026).

| | **Base del portal (JDBC)** | **ORDS / BIOWS (web services)** |
|---|---|---|
| Qué es | Esquema propio del portal Java. Se conecta por JDBC con `spring.datasource.url`. | Esquema del legacy PHP, expuesto como REST por ORDS. El portal lo consume por HTTP. |
| QA | `200.94.116.132:1521/orclpdb` (`application-test.properties`) | `http://10.249.249.3/biows/ords/security/Servcio/...` |
| Producción | `application-prod.properties` → `spring.datasource.url` | Misma ORDS de producción del PHP |
| Cómo se aplica | SQL Developer, conexión al esquema del portal, correr el `.sql` | SQL Developer > RESTful Services (handlers PL/SQL) o SQL sobre el esquema legacy |
| Qué vive ahí | `APP_*` (seguridad, menús, auditoría, archivos, importación) | `TBL_SERV_*`, `SERV_MED_*`, `BIO_*`, `biometrico_*`, catálogos, y los handlers `Servcio/*` |
| `ddl-auto` | `validate`: si una entidad JPA no encuentra su tabla, **el portal no arranca** | n/a |

## Scripts que van a la BASE DEL PORTAL (JDBC)

Carpeta `src/main/resources/db/sql/`:

| Script | Crea | Módulo |
|---|---|---|
| `00_init_oracle21c.sql` | `APP_SEC_*`, `APP_MENU*`, `APP_AUD_EVENT`, `APP_FS_FILE`, jobs… (autoritativo) | Base |
| `app_domain/app-fs-file.sql` (+ `app-fs-file-reconcile.sql`) | `APP_FS_FILE` si no existe / reconcilia columnas | Adjuntos PDF, importador |
| `app_domain/app-import.sql` | `APP_IMPORT_LOTE`, `APP_IMPORT_FILA` + índices | **Importar Excel** (staging temporal) |
| `app_domain/files-salud.sql`, `tags-salud.sql` | landing de la migración histórica | ETL U09 |

Además, en el **servidor** del portal: `portal.files.root` debe existir y ser escribible por Tomcat
(QA `/home/onedev/apps/exec/apache11_app_jdk21/filessalud`, prod `/mnt/data/onedev/apps/exec/filessalud`).
Ahí van los binarios (PDF y los Excel importados); la BD solo guarda metadatos. Si hay nginx delante,
`client_max_body_size` ≥ 25 MB (el importador acepta hasta 25 MB; `spring.servlet.multipart.max-file-size=25MB`).

## Scripts que van a ORDS (legacy)

Carpeta `docs/ords-*.sql`. Todos son handlers PL/SQL o tablas del esquema legacy. Los de la iniciativa
Dashboard/Análisis (sep-2026), en orden de aplicación:

1. `ords-predio-cuenta.sql` — `SERV_MED_PREDIO`, `SERV_MED_CUENTA_PREDIO`, WS `predios`, `cuenta_predio_lista`, `cuenta_predio_asignar`
2. `ords-cuenta-predio-lista-union.sql` — `cuenta_predio_lista` con UNION de `biometrico_cuenta_SAP` + `biometrico_cuenta`
3. `ords-cuenta-en-reportes.sql` — clones `consulta_incapacidades_fecha_cta`, `consulta_accidentes_fecha_cta`, `consulta_examen_fecha_cta`
4. `ords-cuenta-en-antidoping.sql` — `consulta_antidoping_fecha_cta`
5. `ords-diagnostico-en-consultas.sql` — `consulta_medica_fecha` devuelve `diagnostico` (aplicado en sitio)
6. `ords-maternidad-dashboard.sql` — WS nuevo `consulta_maternidad_fecha`
7. `ords-incapacidades-criterio-fecha-inicio.sql` — `consulta_incapacidades_fecha_cta` filtra por fecha de inicio

Los anteriores a esta iniciativa (`ords-accidentes*.sql`, `ords-antidoping*.sql`, `ords-maternidad.sql`,
`ords-restriccion.sql`, `ords-causa-consulta.sql`, `ords-examen-*.sql`, `ords-consulta-dashboard*.sql`,
`ords-usuario-email-nuevo.sql`, `ords-predio-empleado.sql`) también van a ORDS. Los `*-qa.sql` fueron
correcciones puntuales de QA.

## Regla rápida

- Empieza con `APP_` o está en `src/main/resources/db/sql/` → **base del portal**.
- Empieza con `ords-` o define un handler `Servcio/...` → **ORDS**.
- Un WS productivo existente **no se modifica**: se clona (`_cta`, etc.). Ver `docs/ords-cuenta-en-reportes.sql`.

## Orden sugerido para producción

1. Base del portal: `00_init_oracle21c.sql` (idempotente) → `app-fs-file.sql` → `app-import.sql`.
2. Crear `portal.files.root` y dar permisos a Tomcat.
3. ORDS: los 7 scripts de arriba en ese orden; verificar cada WS con el `curl` que trae cada archivo.
4. Desplegar el WAR con `spring.profiles.active=prod`; si no arranca, revisar el log por
   `Schema-validation: missing table` (= falta un script del paso 1).
5. Crear el rol `ROLE_MEDICO_ANALISTA` en `/admin/roles` y asignarlo; capturar predios en `/admin/predios`.
