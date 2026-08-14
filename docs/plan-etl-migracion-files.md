# Plan ETL — Migración de `files` (MariaDB base64) → filesystem + `APP_FS_FILE`

> **Estado:** diseño acordado, pendiente de ejecutar. Última actualización: 2026-07-30.
> **Contexto previo:** este trabajo es la fase de aplicación del caso de uso **U09** (ver memoria `u09-etl`). Se retomó después de resolver el bug `ORA-00904 FILE_TYPE` (ver `docs/inventario-webservices.md` y `app-fs-file-reconcile.sql`).

---

## 1. Objetivo

Migrar los archivos históricos del legacy PHP, hoy guardados como **base64 dentro de la columna `files.url` (LONGTEXT) en MariaDB**, al modelo de almacenamiento acordado en Java:

- **Binario → filesystem** (`portal.files.root`, sharding `yyyy/MM/dd/<2 hex del sha256>/<uuid>.<ext>`).
- **Metadatos → tabla `APP_FS_FILE`** en Oracle (NO el binario).

Se descarta pasar por `MED_FILE` (blob en Oracle): meter 13.5GB a la BD para sacarlos después es trabajo tirado. Se va **directo** MariaDB → filesystem + `APP_FS_FILE`.

---

## 2. Perfil real del origen (medido 2026-07-30)

Tabla `servicioMedico.files` en MariaDB:

| Métrica | Valor |
|---|---|
| Total de archivos | **21,089** |
| Tamaño base64 | **~18 GB** |
| Tamaño binario real estimado | **~13.5 GB** (base64 infla ~33%) |
| Archivo más grande | 2.7 MB |

**Distribución por `type`:**

| type | archivos | MB base64 |
|---|---|---|
| `examen_medico` | 16,506 | 15,796 (**87%**) |
| `laboratorio` | 2,211 | 1,251 |
| `nota_medica` | 1,624 | 1,232 |
| `nota_incapacidad` | 646 | 265 |
| ~99 valores hash MD5 (32 chars) | 1–4 c/u | adjuntos de consulta |
| `''` (vacío) | 3 | — |

**Duplicados de contenido:** 21,089 filas vs **20,828 `MD5(url)` únicos → 261 filas duplicadas (1.2%)**.
**Nombres sin extensión:** solo **3** de 21,089.

### Conclusiones del perfilado
1. **`type` es polimórfico:** o categoría funcional (`examen_medico`, etc.) o el id de `consulta_relacionada` (hash MD5). Se copia **`type → FILE_TYPE` tal cual**, sin normalizar. `APP_FS_FILE.FILE_TYPE` ya está diseñado para esto.
2. **NO existe `type='JSONCODE'`** en los datos → no hay filas "que no son binarios". Un riesgo menos.
3. **Truncado de nombres es marginal** (3 casos). El `varchar(50)` casi no dañó extensiones. Magic-bytes queda como respaldo barato, no como necesidad.
4. **Los 261 duplicados son registros de negocio distintos** (mismo PDF adjuntado a varios NSS/consultas). NO se descartan.

---

## 3. Decisión abierta (bloquea el arranque)

### Índice único de checksum → volverlo NO único
`00_init_oracle21c.sql` crea `UX_FS_FILE_CHECKSUM` **UNIQUE** sobre `APP_FS_FILE.CHECKSUM_SHA256`. Con 261 contenidos duplicados, el `INSERT` de la segunda copia choca con **`ORA-00001`**.

**Recomendación: volver el índice NO único** (queda para búsqueda/integridad, sin enforcement).
- Un único de checksum en tabla de adjuntos es incorrecto por diseño: el mismo documento se adjunta legítimamente a varios expedientes.
- Costo: 261 binarios repetidos en disco (unos MB de 13.5GB). Despreciable.
- `APP_FS_FILE` está **vacía en local y QA** → el cambio es gratis ahora.
- Alternativa descartada: dedup real (un binario, N metadatos) exige otro esquema por solo 1.2%.

**Acción pendiente:** `DROP INDEX UX_FS_FILE_CHECKSUM` + recrear como índice normal, corregir `00_init`, aplicar en local + QA (igual que el reconcile). **← decidir antes de escribir el runner.**

**✅ Aplicado 2026-08-13 en Oracle local (`PROYECTO_BASE_PDB`).** Verificado: `UX_FS_FILE_CHECKSUM` ahora `NONUNIQUE`/`VALID`. `00_init_oracle21c.sql` corregido para reflejar esto en instalaciones futuras. **Pendiente: aplicar el mismo `DROP`+`CREATE` en QA** cuando se decida correr la migración ahí (la corrida completa de este ETL será contra Oracle local por ahora, según decisión del 2026-08-13).

---

## 3.1 Verificado 2026-08-12: los links no se rompen, y `nss` sí viaja

Pregunta validada antes de arrancar: ¿qué pasa con los links del portal después de migrar?

- Los links "Ver/Descargar" (`FileStoreController.java`) **no son URLs fijas** — se regeneran en cada render a partir de lo que exista en `APP_FS_FILE` en ese momento. Hoy (sin migrar) las pantallas de archivos históricos simplemente se ven vacías ("sin archivos adjuntos"), no rotas. Después de migrar, aparecen en las mismas pantallas sin distinguir viejo/nuevo.
- La pantalla U06 (Laboratorio/Histórico E.M/Nota médica/Nota incapacidad) filtra por **NSS + type**, no solo por type (`FsFileRepository.listByNssAndType`). Confirmado que `files.nss` **sí existe** como columna propia en el origen MariaDB (`varchar(50)`, ver cabecera de `files-salud.sql`) — el runner debe leerla y pasarla igual que hoy hace `FileStoreService.store(files, nss, relacion)`. No hay que resolver el NSS desde otra tabla.

---

## 4. Destino: piezas YA construidas (no reescribir)

- `FilesystemStorageProvider` (`catalog/file/storage/`): sharding, UUID, escritura atómica temp+move, sha256. **Reusar** para que los migrados queden idénticos a los nuevos.
- `FsFileRepository` (`catalog/file/repository/`): `INSERT` a `APP_FS_FILE` vía JdbcTemplate.
- `APP_FS_FILE`: tabla de metadatos, ya reconciliada en local + QA (19 columnas).
- Config: `portal.files.root=C:/portal-salud/files` (application-local.properties).

**IMPORTANTE:** el ETL debe llamar a `StorageProvider` + repo **directamente, NO a `FileStoreService`**. Ese servicio rechaza >25MB y extensiones fuera de `{pdf,png,jpg,jpeg,docx}`; con él, cualquier archivo legacy raro se cae en vez de migrarse.

---

## 5. Cambios de código pendientes en el runner

1. **Overload de fecha histórica en `FilesystemStorageProvider`.** Hoy `store()` usa `LocalDate.now()` → todos los históricos caerían en la fecha de hoy. Necesita `store(byte[] content, String ext, LocalDate fecha)` usando `files.date_upload`; el actual delega con `now()`.
2. **Detección por magic-bytes** del binario decodificado (`%PDF`, `\x89PNG`, `\xFF\xD8\xFF`, `PK\x03\x04`) para los ~3 sin extensión y truncados. Nombre solo como referencia.
3. **Idempotencia:** `BUSINESS_KEY = "legacy-<id de MariaDB>"`. Aprovecha el índice único `UX_APP_FS_FILE_BKEY` → re-ejecutar no duplica y permite reanudar si truena a media corrida.
4. **Base64 limpio:** es `base64_encode()` pelón (sin prefijo `data:`, sin saltos) → `Base64.getDecoder()` directo. Validar sha256 post-decode; mandar a cuarentena lo que no decodifique en vez de abortar el lote.
5. **Perfil `etl`** con segundo `DataSource` apuntando a MariaDB. **Decidido 2026-08-12: JDBC directo** (no DB Link) — cero dependencia de DBA/redes, controlado por completo desde código. El usuario ya tiene credenciales de acceso a MariaDB (usadas para el perfilado del 2026-07-30).
6. **Modo muestra:** aceptar una lista de ids o `LIMIT` para la prueba.

---

## 6. Estrategia de muestra (NO cortar por tamaño)

Cortar 300MB por `id` trae puros archivos parecidos y se pierde los casos límite. Mejor **~60 archivos que ejerciten todos los caminos**:

```sql
(SELECT id FROM files WHERE type='examen_medico'    ORDER BY id LIMIT 15)
UNION (SELECT id FROM files WHERE type='laboratorio'      ORDER BY id LIMIT 10)
UNION (SELECT id FROM files WHERE type='nota_medica'      ORDER BY id LIMIT 10)
UNION (SELECT id FROM files WHERE type='nota_incapacidad' ORDER BY id LIMIT 10)
UNION (SELECT id FROM files WHERE LENGTH(type)=32         ORDER BY id LIMIT 5)   -- adjuntos de consulta (hash)
UNION (SELECT id FROM files WHERE name NOT LIKE '%.%')                            -- los 3 sin extensión
UNION (SELECT id FROM files GROUP BY MD5(url) HAVING COUNT(*)>1 ORDER BY id LIMIT 4); -- duplicados reales
```

- **Target aislado:** XE local + carpeta temporal (`portal.files.root=C:/portal-salud/files-sample`), NO producción ni QA. Si sale mal: borrar carpeta + `TRUNCATE APP_FS_FILE` y reintentar.
- **Validación manual (el punto de la muestra):**
  - Un par de PDF que abran en visor; una imagen que se vea.
  - `sha256` de `APP_FS_FILE` coincide al releer el archivo del disco.
  - Los duplicados generaron 2 filas (con el índice ya no único).
  - Los 3 sin extensión se resolvieron por magic-bytes.
- **Cronometrar** bytes/s (decode+sha256+escritura) y extrapolar a 13.5GB. Estimación gruesa: **15–40 min** la corrida completa single-thread.

---

## 7. Plan de ejecución (orden)

1. [ ] **Decidir índice checksum** → no único. Preparar `ALTER` (DROP + recreate) + fix `00_init`, aplicar en local + QA.
2. [ ] **Construir runner ETL** con modo muestra (reusa `FilesystemStorageProvider` + overload de fecha + magic-bytes + `BUSINESS_KEY=legacy-<id>`).
3. [ ] **Correr muestra** (~60 ids) en XE local + carpeta temporal. Validar a mano.
4. [ ] **Extrapolar** tiempo y espacio con los tiempos de la muestra.
5. [ ] **Corrida completa** por rangos de `id`, sin traer `url` en el SELECT de planeación, commits por lote, reanudable.
6. [ ] **Verificar totales:** `COUNT(*)` en `APP_FS_FILE` vs `files`; sumar `SIZE_BYTES` vs espacio en disco; muestreo aleatorio de apertura de archivos.

---

## 8. Landmines y cómo se manejan (resumen)

| Riesgo | Severidad real | Manejo |
|---|---|---|
| `LocalDate.now()` mete todo en fecha de hoy | Alta si no se corrige | Overload con `date_upload` |
| Contenido duplicado → `ORA-00001` | 261 filas (1.2%) | Índice checksum **no único** |
| Nombre truncado / sin extensión | 3 filas (0.01%) | Magic-bytes + nombre como referencia |
| base64 que no decodifica | desconocido, esperado bajo | Cuarentena por fila, no abortar lote |
| Re-ejecución duplica | — | `BUSINESS_KEY=legacy-<id>` + único |
| `JSONCODE` no-binario | **no existe en los datos** | N/A |
| Usar `FileStoreService` (rechaza >25MB / ext.) | — | Llamar `StorageProvider` + repo directo |
| 18GB en memoria | — | Fila por fila (máx 2.7MB c/u), paginar por id |

---

## 9. Tabla `tags` (borradores del examen) — trivial, aparte

Sin binarios. `content` es texto (JSON/HTML). Copia directa de filas a `MED_TAG`, `longtext → CLOB`. Único cuidado: **encoding** — si la BD legacy es `latin1` y no `utf8mb4`, los acentos de los campos médicos se corrompen. Verificar con `SHOW CREATE TABLE tags` antes.
