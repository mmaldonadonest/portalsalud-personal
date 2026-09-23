# Especificación ETL — Migración histórica (MariaDB → Oracle)

> **23-sep-2026 — el ETL ya no vive dentro del portal.** El runner (`com.onest.app.catalog.file.etl`)
> se movió tal cual a un proyecto Maven independiente en `etl/` (`portal-salud-etl`, ejecutable
> `java -jar`), junto con las clases de almacenamiento que necesita. El WAR del portal ya no lleva
> el driver de MariaDB ni el perfil `etl`. Instrucciones de construcción y corrida: `etl/README.md`.

**Estado:** decisión de negocio tomada el 20 de agosto de 2026 — la migración histórica **sí entra en el alcance**. Se ejecutará con un ETL en Oracle, a cargo de un **equipo externo**. Este documento es la especificación técnica para ese equipo: qué migrar, de dónde, a dónde, y qué ya sabemos por experiencia propia.

---

## 1. Conexiones (completar antes de ejecutar)

### Origen — MariaDB (legacy PHP)

| Parámetro | Valor |
|---|---|
| Host | `10.249.249.4` |
| Puerto | `3306` (default 3306) — **⚠️ NO abierto todavía**. Falta gestionar el acceso de red/firewall hacia este puerto desde donde corra el ETL antes de poder conectar. No arrancar la ejecución sin confirmar esto primero. |
| Base de datos | `servicioMedico` |
| Usuario | `root` |
| Password | `SysRut.000` |
| Charset de conexión | **`utf8mb4` obligatorio** — ver sección 5, hallazgo de encoding. **No confundir con el charset del servidor/BD:** `SHOW VARIABLES LIKE 'character_set_%'` en este origen muestra `character_set_database`/`character_set_server` = `latin1` — es solo el default que heredaría una tabla NUEVA sin charset propio, **no aplica a `files`/`tags`**, que ya declaran su propio `CHARSET=utf8` a nivel de tabla (ver `SHOW CREATE TABLE` abajo). **No hay que tocar el charset del servidor/BD** — solo forzar `utf8mb4` en la conexión/herramienta que hace la extracción, sin depender de qué default traiga esa herramienta en particular |

### Destino — Oracle QA (portal-salud)

| Parámetro | Valor |
|---|---|
| Host | `200.94.116.132` |
| Puerto | `1521` (default 1521) |
| Servicio / SID | `orclpdb` |
| Schema / usuario destino | `ONEWMS_QA` |
| Password | `Ol3rOn3.c4Wm5.3` |
| **¿Cuál instancia?** | **Confirmado: QA (`ONEWMS_QA` @ `200.94.116.132`)** — de las 3 instancias Oracle de este proyecto (local `PROYECTO_BASE_PDB`, QA `ONEWMS_QA`, y la instancia real que respalda el WS ORDS en `10.249.249.3`, no intercambiables entre sí), esta migración corre contra QA. |

### Pre-requisito: el esquema Oracle destino ya debe existir

Este ETL **no crea las tablas destino** — asume que `APP_FS_FILE` y `MED_TAG` ya existen en el schema elegido, con el DDL exacto que usa el portal Java. **Resuelto y re-confirmado para el destino (QA `ONEWMS_QA`):** ambas tablas existen ahí — columnas verificadas directamente con `DESCRIBE` el 2026-08-25 (ver el mapeo exacto en la sección 3) — no hace falta correr ningún script antes de cargar. Se deja la lista de scripts como referencia por si en algún momento el destino cambia a una instancia sin el esquema aplicado:

1. `src/main/resources/db/sql/00_init_oracle21c.sql` — crea `APP_FS_FILE` (script autoritativo) + tablas satélite/FKs.
2. `src/main/resources/db/sql/app_domain/app-fs-file.sql` y `app-fs-file-reconcile.sql` — reconcilian columnas si `APP_FS_FILE` ya existe con una forma distinta (hay un antecedente real de esto: bug `ORA-00904 FILE_TYPE` por dos `CREATE TABLE` con formas distintas corriendo en el mismo destino).
3. `src/main/resources/db/sql/app_domain/tags-salud.sql` — crea `MED_TAG` + funciones/vistas auxiliares (`FN_MED_TAG_GROUP`, etc.).

Confirmar con el equipo del portal Java que estos scripts ya corrieron en el destino elegido antes de intentar cualquier `INSERT` de este ETL.

### Filesystem destino para binarios (`files` → disco, no va a Oracle)

El binario decodificado de `files` (ver sección 3) **no se guarda en Oracle** — va al filesystem del servidor donde corre el portal Java, bajo la ruta configurada en la propiedad `portal.files.root`. Hoy esa propiedad es un **valor único y fijo**, no varía por ambiente/perfil:

| Valor | Estado |
|---|---|
| `C:/portal-salud/files` | Perfil `local` (`application-local.properties`) — máquina de desarrollo (Windows). **No es la ruta a usar en esta migración.** |
| `/mnt/data/onedev/apps/exec/filessalud` | **Confirmado — perfil `prod`** (`application-prod.properties`), servidor `10.249.249.4` |

**Ruta confirmada: `/mnt/data/onedev/apps/exec/filessalud` en `10.249.249.4`, usuario `onedev`.** Ya corregido en el código (2026-08-25): `portal.files.root` se quitó de `application.properties` (no tenía un default correcto, se dejó comentado por error) y ahora se define explícito por perfil — `application-local.properties` (Windows) y `application-prod.properties` (esta ruta). El equipo externo necesita permisos de escritura en esa ruta como (o equivalente a) el usuario `onedev` — confirmar si es el usuario del sistema operativo dueño del directorio, o el usuario con el que deben conectarse (SSH/SFTP) para escribir ahí.

**⚠️ Cableado PROVISIONAL para ensayar este ETL (2026-08-25), no es la configuración final:** `application-prod.properties` — que trae la ruta real de filesystem de arriba — se apuntó temporalmente al datasource de **`ONEWMS_QA`** (la misma Oracle "provisional" de la tabla de Conexiones, usada aquí solo para ensayar el ETL sin arriesgar una Oracle de producción real que todavía no existe/no está lista). Es decir: hoy, el perfil `prod` = filesystem real (`10.249.249.4`) + Oracle provisional (QA). **Antes del *go-live* real, hay que volver a cablear `spring.datasource.*` en `application-prod.properties` a la Oracle de producción definitiva** (no dejar `ONEWMS_QA` ahí de forma permanente) — el filesystem real (`10.249.249.4`) sí se queda igual.

No asumir ninguna otra ruta sin validarlo con el equipo del portal. El proceso ETL (del lado de Oracle) necesita acceso de escritura a esa ruta/mount antes de correr.

---

## 2. Alcance de datos — lo único confirmado hoy

**Verificado por consulta directa el 20 de agosto de 2026** (`SHOW TABLES FROM servicioMedico`): la base MariaDB `servicioMedico` **solo tiene 2 tablas**. No hay ninguna otra tabla de datos históricos en este origen.

| Tabla MariaDB | Filas (snapshot 2026-08-13) | Tamaño | Destino Oracle | Estado |
|---|---|---|---|---|
| `files` | 21,475 (`COUNT(*)` real) | ~18 GB (base64) | Metadatos → `APP_FS_FILE`; binario → filesystem (`portal.files.root`, ver sección 1) | Migrado y verificado contra **Oracle LOCAL de desarrollo** (`PROYECTO_BASE_PDB`, 21,448 filas, 2026-08-13) — ver `docs/plan-etl-migracion-files.md` |
| `tags` | 550,560 (`COUNT(*)` real) | ~52 MB | `MED_TAG` (patrón EAV) | Migrado y verificado contra **Oracle LOCAL de desarrollo** (mismo alcance, 550,560 filas, 2026-08-13) — ver memoria interna `u09-etl` |

**⚠️ MariaDB de origen es una base productiva y sigue creciendo con el tiempo.** Los conteos de arriba son un snapshot puntual al 2026-08-13 (`COUNT(*)` directo, no el estimado de `information_schema.TABLES`, que en InnoDB no es confiable). **No usar estos números como cifra fija de verificación.** Antes de la carga real, el equipo ejecutor debe correr `SELECT COUNT(*) FROM files` / `SELECT COUNT(*) FROM tags` contra el origen **en el momento de la ejecución**, y usar ese número (menos los huérfanos documentados en la sección 5) como base real para el checklist de verificación de la sección 6 — no los números de esta tabla.

**⚠️ La corrida "migrado y verificado" de arriba fue contra Oracle LOCAL de desarrollo, NO contra QA ni contra la instancia real detrás del WS ORDS.** Sirvió para validar que el mapeo/proceso funciona end-to-end (conteos exactos, checksums, muestreo de archivos abiertos y validados — detalle en `u09-etl`), pero **no reemplaza la migración final**. Falta ejecutar (o re-ejecutar) formalmente contra el destino Oracle que se confirme en la sección 1, con el conteo de origen re-tomado en ese momento.

**✅ Alcance confirmado (2026-08-25): es exactamente `files` + `tags`, no hay otro origen de datos.** Lo que falta es correr formalmente esta misma carga (ya validada end-to-end en LOCAL) contra el destino Oracle real (QA `ONEWMS_QA`, sección 1).

### Esquema de origen (referencia exacta, `SHOW CREATE TABLE`)

```sql
CREATE TABLE `files` (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `nss` varchar(50) NOT NULL,
  `name` varchar(50) NOT NULL,
  `date_upload` datetime NOT NULL,
  `url` longtext NOT NULL,      -- PDF/imagen en base64, SIN prefijo data:, sin saltos de línea
  `type` varchar(50) NOT NULL,  -- polimórfico: categoría funcional O hash MD5 de consulta relacionada
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `tags` (
  `id` int(255) NOT NULL AUTO_INCREMENT,
  `nss` varchar(255) DEFAULT NULL,
  `type` varchar(50) DEFAULT NULL,     -- nombre de campo EAV (ej. "STPO2", "TOSINOBSPRETEST")
  `content` longtext,                  -- valor del campo, 27% legítimamente vacío
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
```

---

## 3. Mapeo de destino en Oracle

Columnas reales verificadas en QA (`DESCRIBE APP_FS_FILE` / `DESCRIBE MED_TAG`, 2026-08-25) y valores exactos confirmados contra el runner Java propio ya ejecutado y probado (`com.onest.app.catalog.file.etl`, ver sección 7) — no son una propuesta, es lo que YA corrió con éxito contra Oracle LOCAL.

### `files` → `APP_FS_FILE` + filesystem
**NO** se guarda el binario en Oracle (se descartó explícitamente meter 13.5GB reales a la BD) — el binario decodificado va al filesystem (sección 1), con sharding `yyyy/MM/dd/<2 hex del sha256>/<uuid>.<ext>`. `APP_FS_FILE` solo guarda metadatos:

| Columna `APP_FS_FILE` | Origen / valor |
|---|---|
| `NSS` | `files.nss` tal cual |
| `BUSINESS_KEY` | `'legacy-' \|\| files.id` — idempotencia (permite reintentar sin duplicar) |
| `FILE_TYPE` | `files.type` tal cual, **sin normalizar** (es polimórfico: categoría funcional o hash MD5 de consulta) |
| `ORIGINAL_NAME` | `files.name` (base, sin extensión) |
| `EXTENSION` | de `files.name` si la trae; si no, resuelta por magic-bytes (`%PDF`, `\x89PNG`, `\xFF\xD8\xFF`) — 3 casos conocidos sin extensión |
| `MIME_TYPE` | derivado de la extensión resuelta |
| `SIZE_BYTES` | tamaño del binario ya decodificado (no el tamaño del base64) |
| `CHECKSUM_SHA256` | SHA-256 del binario decodificado |
| `STORAGE_PATH` | ruta relativa a `portal.files.root` generada por el sharding de arriba |
| `STORAGE_PROVIDER` | literal `'FILESYSTEM'` |
| `STATUS` | literal `'ACTIVE'` |
| `CURRENT_VERSION` / `VERSION` | literal `1` |
| `DATE_UPLOAD` | `files.date_upload` (fecha **original** del legacy — NO la fecha de la corrida del ETL) |
| `CREATED_BY` | literal `'ETL_LEGACY'` (mismo valor usado en la corrida propia — permite distinguir estas filas de las creadas por el portal en uso normal) |
| `CREATED_AT` | se deja al `DEFAULT SYSTIMESTAMP` de la columna — no hace falta setearlo |

Excluir las 27 filas con `url` vacío (huérfanas, ver sección 5).

### `tags` → `MED_TAG`
Patrón EAV puro (`type`=campo, `content`=valor), `longtext` → `CLOB`, sin transformación de estructura:

| Columna `MED_TAG` | Origen / valor |
|---|---|
| `NSS` | `tags.nss` tal cual (117 filas con `nss` NULL son legítimas, no excluir) |
| `TYPE` | `tags.type` tal cual |
| `CONTENT` | `tags.content` tal cual (27% vacío es normal, EAV disperso) |
| `TAG_GROUP` | `FN_MED_TAG_GROUP(tags.type)` — función PL/SQL ya existente en el destino (`tags-salud.sql`), NO calcularlo del lado del ETL |
| `SOURCE_ID` | `tags.id` — idempotencia (`SELECT COUNT(*) FROM MED_TAG WHERE SOURCE_ID = ?` antes de insertar) |
| `CREATED_BY` | mismo criterio que `files`, usar un literal identificable (ej. `'ETL_LEGACY'`) |
| `MIGRATED_AT` / `CREATED_AT` | `SYSTIMESTAMP` (fecha de la corrida del ETL — a diferencia de `files`, aquí no hay una fecha original que preservar) |

Verificación esperada tras la carga: 137 valores distintos de `TYPE`, cero filas con `TAG_GROUP` = `'OTRO'` (indicaría un `type` nuevo no cubierto por `FN_MED_TAG_GROUP`).

---

## 4. Metodología recomendada (aprendida de la ejecución propia, 2026-08-13)

1. **Aterrizaje fiel primero, normalizar después.** No intentar limpiar/corregir datos durante la carga — copiar tal cual y documentar lo sucio por separado.
2. **Idempotencia por `BUSINESS_KEY`** (ej. `legacy-<id_origen>`) para poder reintentar sin duplicar si la corrida se interrumpe a la mitad.
3. **Charset explícito `utf8mb4`** en la conexión/export de origen — el charset de la tabla en MariaDB es `utf8` (3 bytes), no `utf8mb4`; si se exporta sin forzar `utf8mb4` en la conexión, los acentos se corrompen.
4. **Base64 limpio**, sin prefijo `data:` ni saltos de línea — decodificación directa.
5. **Commits por lote, no todo en una transacción** — el volumen de `files` es grande (~18GB), evitar un solo `INSERT` masivo.
6. **Modo de muestra antes de la corrida completa** — validar ~50-60 filas representativas (de cada `type`, incluyendo duplicados y casos límite) antes de correr el total.
7. **Índice de checksum — RESUELTO en QA (2026-08-25).** El DDL de origen (`00_init_oracle21c.sql`) crea `UX_FS_FILE_CHECKSUM` como UNIQUE sobre `APP_FS_FILE.CHECKSUM_SHA256`; hay contenido duplicado legítimo (mismo PDF adjuntado a varios NSS/consultas — 261 casos conocidos), y con el índice UNIQUE la segunda copia de cada duplicado fallaría con `ORA-00001`. **Ya aplicado en ambos destinos:** Oracle LOCAL de desarrollo y ahora también QA `ONEWMS_QA` quedaron en **NONUNIQUE** (confirmado por consulta directa tras aplicar `docs/ords-fix-checksum-index-qa.sql`). No queda ninguna acción pendiente en este punto.

---

## 5. Hallazgos de calidad de datos ya conocidos (para no redescubrir)

- **`files`**: 27 filas huérfanas con `url` vacío (se excluyen). 3 nombres sin extensión (resueltos por magic-bytes: `%PDF`, `\x89PNG`, `\xFF\xD8\xFF`). 261 duplicados de contenido por `MD5(url)` — son registros de negocio legítimos, no basura. ~96 nombres de archivo (0.45%) con **encoding roto preexistente** en el origen (doble-encoding, no causado por ningún export/import ya hecho) — cosmético, no afecta la localización ni el contenido del archivo, solo el nombre mostrado al descargar.
- **`tags`**: 27% de `content` vacío es **normal** (EAV disperso, no dato corrupto). 117 filas huérfanas con `nss` null — dato legítimo. 137 valores distintos de `type` confirmados, cero sin clasificar.
- **Ambas tablas**: `id` de MariaDB **no** es secuencial/denso (el patrón del PHP legacy es DELETE+INSERT, nunca UPDATE) — no asumir que el rango de ids es continuo al paginar.

---

## 6. Verificación esperada al terminar

- `COUNT(*)` de origen vs. destino coincide (descontando huérfanos ya documentados).
- Suma de tamaños de archivo en disco vs. `SIZE_BYTES` en `APP_FS_FILE`.
- Muestreo aleatorio: abrir 5-10 archivos migrados y confirmar que son PDFs/imágenes válidos.
- `MED_TAG`: conteo por `TAG_GROUP` sin filas en categoría "OTRO" (indicaría un `type` nuevo no clasificado).

---

## 7. Contactos / referencias internas

- Memoria técnica completa de la ejecución propia (2026-08-13): archivo de memoria `u09-etl` del equipo de desarrollo del portal Java.
- Plan detallado usado para la corrida propia: `docs/plan-etl-migracion-files.md` (mismo repositorio).
- Runner Java propio (referencia de implementación, no es lo que usará el equipo externo): `com.onest.app.catalog.file.etl` en el código fuente del portal.
