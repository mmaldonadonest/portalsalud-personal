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

---

## 2. Alcance de datos — lo único confirmado hoy

**Verificado por consulta directa el 20 de agosto de 2026** (`SHOW TABLES FROM servicioMedico`): la base MariaDB `servicioMedico` **solo tiene 2 tablas**. No hay ninguna otra tabla de datos históricos en este origen.

| Tabla MariaDB | Filas | Tamaño | Destino Oracle | Estado |
|---|---|---|---|---|
| `files` | 21,475 (origen real; el snapshot local usado para pruebas tiene 18,912) | ~18 GB (base64) | Metadatos → `APP_FS_FILE`; binario → filesystem (`portal.files.root`) | ✅ **YA MIGRADO Y VERIFICADO** (21,448 filas, 2026-08-13) — ver `docs/plan-etl-migracion-files.md` |
| `tags` | 551,258 (snapshot local); 550,560 migradas en la corrida real | ~52 MB | `MED_TAG` (patrón EAV) | ✅ **YA MIGRADO Y VERIFICADO** (550,560 filas, 2026-08-13) — ver memoria `u09-etl` |

**⚠️ Pregunta abierta para confirmar con el equipo externo / stakeholder que autorizó esta migración:**
¿El alcance de "migración histórica" es exactamente este (`files` + `tags`, ya migrados por este equipo en agosto) — en cuyo caso lo que falta es solo **validar/reejecutar formalmente contra el destino Oracle real** — o existe otro origen de datos (otra base, otro sistema, un dump distinto) que todavía no se ha identificado? No se debe asumir que hay más tablas de las que aquí se documentan sin confirmarlo.

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
7. **No usar un índice único de checksum** sobre el contenido de `files` — hay contenido duplicado legítimo (mismo PDF adjuntado a varios NSS/consultas), no es basura a deduplicar.

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
