# Especificación ETL — Migración histórica (MariaDB → Oracle)

**Estado:** decisión de negocio tomada el 20 de agosto de 2026 — la migración histórica **sí entra en el alcance**. Se ejecutará con un ETL en Oracle, a cargo de un **equipo externo**. Este documento es la especificación técnica para ese equipo: qué migrar, de dónde, a dónde, y qué ya sabemos por experiencia propia.

---

## 1. Conexiones (completar antes de ejecutar)

### Origen — MariaDB (legacy PHP)

| Parámetro | Valor |
|---|---|
| Host | `_______________________` |
| Puerto | `_______________________` (default 3306) |
| Base de datos | `servicioMedico` |
| Usuario | `_______________________` |
| Password | `_______________________` |
| Charset de conexión | **`utf8mb4` obligatorio** — ver sección 5, hallazgo de encoding |

### Destino — Oracle (portal-salud)

| Parámetro | Valor |
|---|---|
| Host | `_______________________` |
| Puerto | `_______________________` (default 1521) |
| Servicio / SID | `_______________________` |
| Schema / usuario destino | `_______________________` |
| Password | `_______________________` |
| **¿Cuál instancia?** | `_______________________` — este proyecto tiene 3 instancias Oracle distintas con distinto grado de avance (local `PROYECTO_BASE_PDB`, QA `ONEWMS_QA` en `200.94.116.132`, y la instancia real que respalda el WS ORDS en `10.249.249.3`). **Confirmar explícitamente cuál es el destino real antes de correr nada** — no son intercambiables, cada una tiene datos distintos hoy. |

### Pre-requisito: el esquema Oracle destino ya debe existir

Este ETL **no crea las tablas destino** — asume que `APP_FS_FILE` y `MED_TAG` ya existen en el schema elegido, con el DDL exacto que usa el portal Java. Confirmado que existen (vacías, listas para carga) en local y en QA (`ONEWMS_QA`) al 2026-07-30; **no confirmado** en la instancia real de `10.249.249.3`. Si el destino termina siendo esa instancia (o cualquier otra sin el esquema aplicado), correr primero, en este orden, los scripts del repositorio del portal Java:

1. `src/main/resources/db/sql/00_init_oracle21c.sql` — crea `APP_FS_FILE` (script autoritativo) + tablas satélite/FKs.
2. `src/main/resources/db/sql/app_domain/app-fs-file.sql` y `app-fs-file-reconcile.sql` — reconcilian columnas si `APP_FS_FILE` ya existe con una forma distinta (hay un antecedente real de esto: bug `ORA-00904 FILE_TYPE` por dos `CREATE TABLE` con formas distintas corriendo en el mismo destino).
3. `src/main/resources/db/sql/app_domain/tags-salud.sql` — crea `MED_TAG` + funciones/vistas auxiliares (`FN_MED_TAG_GROUP`, etc.).

Confirmar con el equipo del portal Java que estos scripts ya corrieron en el destino elegido antes de intentar cualquier `INSERT` de este ETL.

### Filesystem destino para binarios (`files` → disco, no va a Oracle)

El binario decodificado de `files` (ver sección 3) **no se guarda en Oracle** — va al filesystem del servidor donde corre el portal Java, bajo la ruta configurada en la propiedad `portal.files.root`. Hoy esa propiedad es un **valor único y fijo**, no varía por ambiente/perfil:

| Valor | Estado |
|---|---|
| `C:/portal-salud/files` | **Activo hoy** — ruta de la máquina de desarrollo local (Windows), usada en la corrida de prueba del 2026-08-13 |
| `/home/onedev/apps/exec/apache11_app_jdk21/filessalud` | Comentado/inactivo en el código — parece ser la ruta real de despliegue (Linux), **sin confirmar** |

**Confirmar explícitamente, junto con el destino Oracle de arriba, la ruta de filesystem real donde este ETL debe escribir los binarios** — no asumir ninguna de las dos rutas de la tabla sin validarlo con el equipo del portal. El proceso ETL (del lado de Oracle) necesita acceso de escritura a esa ruta/mount antes de correr.

---

## 2. Alcance de datos — lo único confirmado hoy

**Verificado por consulta directa el 20 de agosto de 2026** (`SHOW TABLES FROM servicioMedico`): la base MariaDB `servicioMedico` **solo tiene 2 tablas**. No hay ninguna otra tabla de datos históricos en este origen.

| Tabla MariaDB | Filas (snapshot 2026-08-13) | Tamaño | Destino Oracle | Estado |
|---|---|---|---|---|
| `files` | 21,475 (`COUNT(*)` real) | ~18 GB (base64) | Metadatos → `APP_FS_FILE`; binario → filesystem (`portal.files.root`, ver sección 1) | Migrado y verificado contra **Oracle LOCAL de desarrollo** (`PROYECTO_BASE_PDB`, 21,448 filas, 2026-08-13) — ver `docs/plan-etl-migracion-files.md` |
| `tags` | 550,560 (`COUNT(*)` real) | ~52 MB | `MED_TAG` (patrón EAV) | Migrado y verificado contra **Oracle LOCAL de desarrollo** (mismo alcance, 550,560 filas, 2026-08-13) — ver memoria interna `u09-etl` |

**⚠️ MariaDB de origen es una base productiva y sigue creciendo con el tiempo.** Los conteos de arriba son un snapshot puntual al 2026-08-13 (`COUNT(*)` directo, no el estimado de `information_schema.TABLES`, que en InnoDB no es confiable). **No usar estos números como cifra fija de verificación.** Antes de la carga real, el equipo ejecutor debe correr `SELECT COUNT(*) FROM files` / `SELECT COUNT(*) FROM tags` contra el origen **en el momento de la ejecución**, y usar ese número (menos los huérfanos documentados en la sección 5) como base real para el checklist de verificación de la sección 6 — no los números de esta tabla.

**⚠️ La corrida "migrado y verificado" de arriba fue contra Oracle LOCAL de desarrollo, NO contra QA ni contra la instancia real detrás del WS ORDS.** Sirvió para validar que el mapeo/proceso funciona end-to-end (conteos exactos, checksums, muestreo de archivos abiertos y validados — detalle en `u09-etl`), pero **no reemplaza la migración final**. Falta ejecutar (o re-ejecutar) formalmente contra el destino Oracle que se confirme en la sección 1, con el conteo de origen re-tomado en ese momento.

**⚠️ Pregunta abierta para confirmar con el equipo externo / stakeholder que autorizó esta migración:**
¿El alcance de "migración histórica" es exactamente este (`files` + `tags`) — en cuyo caso lo que falta es correr formalmente esta misma carga contra el destino Oracle real — o existe otro origen de datos (otra base, otro sistema, un dump distinto) que todavía no se ha identificado? No se debe asumir que hay más tablas de las que aquí se documentan sin confirmarlo.

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

### `files` → `APP_FS_FILE` + filesystem
- **NO** se guarda el binario en Oracle (se descartó explícitamente meter 13.5GB reales a la BD). El binario decodificado va al filesystem del servidor del portal, con sharding `yyyy/MM/dd/<2 hex del sha256>/<uuid>.<ext>`.
- `APP_FS_FILE` guarda solo metadatos: NSS, tipo, checksum SHA-256, ruta, fecha original (`date_upload`, **no** la fecha de la corrida del ETL).
- `type` se copia tal cual a `FILE_TYPE`, sin normalizar (es polimórfico, ver esquema arriba).

### `tags` → `MED_TAG`
- Copia directa fila por fila, `longtext` → `CLOB`. Es un patrón EAV puro (`type`=campo, `content`=valor), no requiere transformación de estructura.

---

## 4. Metodología recomendada (aprendida de la ejecución propia, 2026-08-13)

1. **Aterrizaje fiel primero, normalizar después.** No intentar limpiar/corregir datos durante la carga — copiar tal cual y documentar lo sucio por separado.
2. **Idempotencia por `BUSINESS_KEY`** (ej. `legacy-<id_origen>`) para poder reintentar sin duplicar si la corrida se interrumpe a la mitad.
3. **Charset explícito `utf8mb4`** en la conexión/export de origen — el charset de la tabla en MariaDB es `utf8` (3 bytes), no `utf8mb4`; si se exporta sin forzar `utf8mb4` en la conexión, los acentos se corrompen.
4. **Base64 limpio**, sin prefijo `data:` ni saltos de línea — decodificación directa.
5. **Commits por lote, no todo en una transacción** — el volumen de `files` es grande (~18GB), evitar un solo `INSERT` masivo.
6. **Modo de muestra antes de la corrida completa** — validar ~50-60 filas representativas (de cada `type`, incluyendo duplicados y casos límite) antes de correr el total.
7. **Verificar/alterar el índice de checksum ANTES de cargar `files`.** El DDL de origen (`00_init_oracle21c.sql`) crea `UX_FS_FILE_CHECKSUM` como **UNIQUE** sobre `APP_FS_FILE.CHECKSUM_SHA256`. Hay contenido duplicado legítimo (mismo PDF adjuntado a varios NSS/consultas — 261 casos conocidos al 2026-08-13), no es basura a deduplicar; con el índice UNIQUE, la segunda copia de cada duplicado falla con `ORA-00001`. Ya se cambió a NONUNIQUE en Oracle LOCAL de desarrollo; **sigue pendiente en QA**, y se desconoce su estado en la instancia real de `10.249.249.3`. Confirmar el estado de este índice en el destino elegido (sección 1) y alterarlo a NONUNIQUE si sigue como UNIQUE, antes de correr la carga completa.

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
