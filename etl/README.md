# portal-salud-etl — migración histórica MariaDB → Oracle

Proceso de consola (Spring Boot, sin web) que migra el histórico del portal PHP legacy:

| Origen (MariaDB `servicioMedico`) | Destino |
|---|---|
| `files` (≈21,475 filas, ~18 GB de base64) | metadatos → `APP_FS_FILE` · binario decodificado → filesystem (`portal.files.root`) |
| `tags` (≈550,560 filas) | `MED_TAG` (EAV, `longtext` → `CLOB`) |

Especificación completa (mapeo columna por columna, hallazgos de calidad, verificación):
`../docs/etl-migracion-historica-especificacion.md`. Muestra del origen:
`../docs/muestra-tablas-mysql-migracion.html`.

## Por qué está separado del portal

Hasta el 23-sep-2026 este código vivía **dentro del WAR** del portal, bajo el perfil `etl`. Eso
significaba llevar el driver de MariaDB a producción y que una carga masiva pudiera dispararse por
un perfil mal puesto. Se movió **tal cual** (mismos paquetes, misma lógica): la corrida del
2026-08-13 ya estaba verificada —21,448 archivos y 550,560 tags— y cambiar el código habría
arriesgado las rutas de *sharding* y los checksums con los que el portal localiza los binarios.

Las clases de almacenamiento (`FilesystemStorageProvider`, `FsFileRepository`, …) están
**duplicadas** aquí a propósito: el ETL debe escribir exactamente igual que el portal. Si alguna
vez se cambia el sharding en el portal, hay que replicarlo aquí.

## Construir

```bash
mvn -f etl/pom.xml package        # genera etl/target/portal-salud-etl.jar
```

## Configurar (todo por variable de entorno, sin contraseñas en el repo)

| Variable | Qué es |
|---|---|
| `ETL_SOURCE_URL` | MariaDB origen. **Siempre con `characterEncoding=UTF-8`** |
| `ETL_SOURCE_USER` / `ETL_SOURCE_PASSWORD` | credenciales del origen |
| `ETL_TARGET_URL` | Oracle destino (el esquema del portal: `APP_FS_FILE`, `MED_TAG`) |
| `ETL_TARGET_USER` / `ETL_TARGET_PASSWORD` | credenciales del destino |
| `ETL_FILES_ROOT` | raíz de archivos; **debe ser la misma `portal.files.root` del portal** |
| `ETL_FILES_MODE` · `ETL_TAGS_MODE` | `none` (default) · `sample` · `full` |
| `ETL_FILES_BATCH` · `ETL_TAGS_BATCH` | tamaño de lote (200 / 500 por default) |

## Correr

Nada se ejecuta solo: con los modos en `none` el proceso arranca, no hace nada y termina.

```bash
# 1) Ensayo dirigido (unas decenas de archivos y tags): SIEMPRE primero
java -jar etl/target/portal-salud-etl.jar --etl.files.mode=sample --etl.tags.mode=sample

# 2) Carga completa
java -jar etl/target/portal-salud-etl.jar --etl.files.mode=full --etl.tags.mode=full

# Memoria: los PDF grandes se decodifican en RAM
java -Xmx2g -jar etl/target/portal-salud-etl.jar --etl.files.mode=full
```

Es **idempotente**: `files` se salta lo ya cargado por `BUSINESS_KEY` (`legacy-<id>`) y `tags` por
`SOURCE_ID`. Se puede reintentar sin duplicar.

## Antes de la corrida real

1. El esquema destino debe existir (`00_init_oracle21c.sql`, `app-fs-file.sql`, `tags-salud.sql`).
2. `ETL_FILES_ROOT` creado y escribible por el usuario que corre el ETL.
3. Puerto 3306 del origen accesible desde donde corra el ETL.
4. **Ojo con `UX_FS_FILE_CHECKSUM`**: hay 261 duplicados legítimos de contenido en `files`; con ese
   índice único la carga truena con `ORA-00001`. Relajarlo o manejar el choque.
5. `COUNT(*)` del origen tomado **en ese momento** (la base productiva sigue creciendo) para la
   verificación final de la spec §6.
