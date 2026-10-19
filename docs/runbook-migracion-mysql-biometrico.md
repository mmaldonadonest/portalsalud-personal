# Runbook — Migración histórica MySQL → BIOMETRICO

**Escrito el 29-sep-2026. ✅ EJECUTADO Y VERIFICADO EL 1-OCT-2026.**

| | |
|---|---|
| `SERV_MED_FS_FILE` | **22,196** filas (22,223 del origen − 27 sin `url`) |
| `SERV_MED_TAG` | **551,044** filas, diferencia **0** |
| Binarios en `/mnt/data/onedev/apps/exec/filessalud` | **22,197** archivos · 14.29 GB |
| Prueba en el portal | ✅ expediente abre y los PDF descargan |

Las siete fases cerradas en un día. Cada una tiene su resultado anotado en su sección.

**Lo que queda, ninguno bloqueante:**

1. **Borrar el zip del servidor**: `rm /mnt/data/onedev/apps/exec/filessalud/etl-prod-files.zip`
   (13 GB).
2. **La decisión 1 sigue abierta** y ahora corre el reloj: el PHP escribe ~15 archivos al día y
   el dump es del 1-oct 10:14. Cada día que pase sin definir corte o convivencia son ~15
   archivos que no están en el portal.
3. **32 nombres con encoding roto** (bloque J de la verificación). Cosmético: la búsqueda es por
   `NSS + FILE_TYPE`.
4. **Rotar la contraseña de `biometrico`**, que sigue en el historial de git.

Mueve el histórico del PHP legacy (`servicioMedico`) al esquema del portal dentro de
**BIOMETRICO**: `files` → `SERV_MED_FS_FILE` + binarios al filesystem, `tags` → `SERV_MED_TAG`.

---

## Ruta decidida

```
MySQL productivo  --dump-->  MariaDB local (staging)  --ETL desde tu PC-->  BIOMETRICO
                                                            |
                                                            +-- binarios al disco local
                                                                       |
                                                                       +-- copia al servidor
```

**Por qué no se lee el MySQL productivo en vivo.** Reducir la carga es el beneficio menor. El
grande es que **contra un origen vivo no se puede verificar**: toda la comprobación se apoya en
comparar el resultado contra el `COUNT(*)` del origen, y si el origen crece mientras cargas, ese
número deja de significar nada. Con el staging congelado, el cuadre es una prueba de verdad.

**Por qué el ETL corre en tu PC y no en el servidor.** Porque ya está probado así: la corrida de
QA del 31-ago fue exactamente este escenario —staging local, ETL en Windows, Oracle remoto— y
cargó 550,760 tags y 21,703 archivos sin un solo fallo. Y sabemos que tu PC alcanza BIOMETRICO,
aunque lo sepamos de la peor manera: el 27-sep los diez intentos fallidos que bloquearon la
cuenta llegaron hasta el servidor.

Lo único que en QA quedó a medias fue subir los binarios: los 15 GB siguen en `C:/etl-qa-files` y
la base de QA tiene 21,703 filas apuntando a archivos que no están ahí. **Por eso aquí la copia
es un paso del runbook con su verificación, no un pendiente.**

---

## Tres decisiones todavía abiertas

Llenar antes de la Fase 1.

### 1. ¿Corte o convivencia?  `___________`

Medido sobre 50 días: de 21,475 archivos el 12-ago a **22,223 el 1-oct**, o sea **~15 al día**.
El PHP sigue escribiendo. (El dato anterior —228 en 19 días, ~12 diarios— se quedó corto.)

- **Corte** — fecha fija, el PHP deja de escribir, se migra una vez, se acabó.
- **Convivencia** — funciona, porque el ETL es idempotente y se puede reejecutar sin duplicar.
  Pero hay que fijar cada cuándo corre el incremental y quién cierra la llave al final. Y
  aceptar que mientras tanto **ningún lado es la fuente de verdad**: el ETL solo trae del PHP al
  portal, nunca al revés. Si alguien captura en los dos, divergen y nadie se entera.

Manda sobre el **cuándo** sacar el dump: es una foto, y lo que se escriba después no se migra.

### 2. `portal.files.root` real del servidor  ✅ `/mnt/data/onedev/apps/exec/filessalud`

**Resuelto el 1-oct-2026.** Es la ruta que dice `application-prod.properties`; la del reporte de
QA (`/home/onedev/apps/exec/apache11_app_jdk21/filessalud`) era de otro ambiente.

Espacio: `/mnt/data` tiene **929 GB libres** de 1 TB. Hacían falta 20. Dato de contexto: el
filesystem crece 54× más rápido que Oracle y la proyección son ~207 GB a cinco años sin purga.

Al momento de migrar la carpeta tenía **1 archivo** (68 KB): el binario de la única fila que el
portal había escrito, y que quedó huérfano al vaciar `SERV_MED_FS_FILE`. Por eso el conteo final
es **22,197** y no 22,196.

### 3. Usuario de Oracle  `___________`

El ETL solo necesita `INSERT` en dos tablas. No hay razón para que sea el dueño del esquema.

---

## Fase 0 · Preflight

- [ ] **Probar usuario y contraseña UNA VEZ en SQL Developer.** Nunca arrancando una aplicación:
      el pool abre diez conexiones y diez fallos bloquean la cuenta. Es literalmente lo que pasó
      el 27-sep.
- [ ] Correr **`docs/script-prod/etl-01-preflight-biometrico.sql`** (solo lectura, va a
      BIOMETRICO). Confirmar:
  - las dos tablas existen
  - **`SERV_MED_UX_FS_FILE_CHECKSUM` dice `NONUNIQUE`** — si dice `UNIQUE`, detenerse: los 261
    duplicados legítimos de contenido revientan con `ORA-00001`
  - `SERV_MED_FN_TAG_GROUP` está `VALID`
  - anotar los conteos de partida: son la línea base de la reversa
- [ ] Confirmar el `files.root` y el espacio en disco del servidor.
- [ ] Verificar que el JAR compila: `mvn -f etl/pom.xml package`

### ✅ Corrido el 1-oct-2026 — limpio

| Bloque | Resultado |
|---|---|
| A · sesión | `BIOMETRICO` @ `PDBPRD` / `onestdb` |
| B · tablas destino | las dos existen |
| C · **línea base de la reversa** | `SERV_MED_FS_FILE` **1 fila, del portal** · `SERV_MED_TAG` **0** |
| D · `SERV_MED_UX_FS_FILE_CHECKSUM` | **`NONUNIQUE`** — el check que podía abortar todo |
| E · permisos | dueño del objeto (ver nota abajo) |
| F · `SERV_MED_FN_TAG_GROUP` | `VALID` |
| G · espacio | **resuelto, sin bloqueo** |

**Espacio.** Las dos tablas y sus diez índices viven **todos en `BIOMETRICO`**; nada en
`BIOMETRICO_INDEX`. Ese tablespace tiene 110 GB asignados, 5.4 GB libres, **`AUTOEXTENSIBLE =
YES`** y techo de 32 TB. El ETL necesita menos de 1 GB. Los 113 MB libres de
`BIOMETRICO_INDEX` no aplican: engaña el nombre, no lo usa ninguno de estos objetos.

**Al terminar la migración, `SERV_MED_FS_FILE` debe tener 22,223 + 1 = 22,224 filas.** Esa fila
extra es del portal, su `BUSINESS_KEY` no empieza con `legacy-` y la reversa no la toca.

**Decisión 3 resuelta de hecho:** el ETL corre como `BIOMETRICO`, dueño del esquema. Funciona,
pero es la opción menos acotada — un proceso que solo necesita `INSERT` en dos tablas puede
borrar cualquiera de las 601 del esquema. Si alguna vez se crea un usuario restringido, el
momento es antes de la corrida grande.

---

## Fase 1 · Congelar el origen

**Confirmar primero el motor de las tablas:**

```sql
SELECT TABLE_NAME, ENGINE FROM information_schema.TABLES
WHERE TABLE_SCHEMA='servicioMedico' AND TABLE_NAME IN ('files','tags');
```

`--single-transaction` da una foto consistente **solo con InnoDB**. Si salen MyISAM, no sirve de
nada y haría falta `--lock-tables`, que sí bloquea escrituras en producción — en ese caso hay que
elegir ventana.

**El dump, solo las dos tablas que el ETL lee:**

```bash
mysqldump --single-transaction --quick --default-character-set=utf8mb4 \
          -h <host> -u <user> -p servicioMedico files tags \
          > salud-YYYYMMDD.sql
```

- `--quick` no es opcional: sin él, `mysqldump` intenta bufferear en memoria filas de ~539 KB de
  base64.
- `--default-character-set=utf8mb4` es el que no se puede olvidar (13-ago).

**Anotar el `COUNT(*)` en ese momento** — contra esto se verifica todo al final:

```sql
SELECT 'files' t, COUNT(*) n FROM files
UNION ALL SELECT 'tags', COUNT(*) FROM tags;
```

| | Conteo | Cuándo |
|---|---:|---|
| `files` | **22,223** | 1-oct-2026, después del dump |
| `tags` | **551,044** | 1-oct-2026, después del dump |

**Son un techo, no la foto exacta.** El dump cerró a las **16:14:37** del 1-oct y el conteo se
tomó después, así que incluye lo que el PHP escribió en ese intervalo. La foto real es la que
quede en el staging al importar, y tiene que ser **igual o ligeramente menor** que estos
números — nunca mayor.

Si el staging sale mayor, el dump no es de esta base. Si sale mucho menor (cientos de filas),
el dump quedó incompleto aunque haya escrito su línea de `Dump completed`.

**Dump usado:** `C:\mysql\10012026.sql` · 19.1 GB · MariaDB 10.5.22 · `SET NAMES utf8mb4` ·
cierra con `-- Dump completed on 2026-10-01 16:14:37`.

**Crecimiento medido del origen** (para dimensionar la decisión 1, corte o convivencia):

| | 12/13-ago | 1-oct | Crecimiento |
|---|---:|---:|---|
| `files` | 21,475 | 22,223 | +748 en 50 días ≈ **15/día** |
| `tags` | 550,560 | 551,044 | +484 en 49 días ≈ **10/día** |

El PHP sigue escribiendo. Cada día que pase entre este dump y el corte definitivo son ~15
archivos que no se migran.

---

## Fase 2 · Staging local

Importar a MariaDB local. El `my.ini` de XAMPP ya quedó afinado el 13-ago
(`max_allowed_packet=1024M`, `innodb_log_file_size=512M`, `buffer_pool=2048M`,
`flush_log_at_trx_commit=0`); sin eso el import truena.

```bash
mysql -u root --default-character-set=utf8mb4 servicioMedico < salud-YYYYMMDD.sql
```

Comprobar que el `COUNT(*)` del staging es idéntico al de la Fase 1. **Si no cuadra aquí, no
seguir**: el dump vino incompleto.

### ✅ Hecho el 1-oct-2026 — cuadre exacto

| | Origen | Staging | Dif. |
|---|---:|---:|---:|
| `files` | 22,223 | **22,223** | **0** |
| `tags` | 551,044 | **551,044** | **0** |

Import de 12:00 a 12:41 (41 min), a ~490 MB/min sostenidos. La base quedó en **19.61 GB**
(`files` 19.55 + `tags` 0.06). Quedaron 48.4 GB libres en `C:`.

**Sobre la hora del dump:** el archivo cierra con `Dump completed on 2026-10-01 16:14:37`, que
es **UTC del servidor Linux** — las 10:14 hora local. El conteo del origen se tomó a las ~11:55
local, hora y media después, y aun así no hubo ni una escritura nueva. A ~15 archivos diarios
(0.6 por hora) era el resultado más probable, no una casualidad.

**Qué queda probado con esto:** que el dump trajo todo lo que el origen tenía. Es la mitad de
la cadena de verificación; la otra mitad —que Oracle reciba todo lo que trae el dump— es la
Fase 6.

El origen congelado para el resto de la migración es **22,223 / 551,044**.

Espacio: ~18 GB el dump + ~18 GB la base importada + 15 GB de binarios decodificados. Cuenta con
**~55 GB libres**.

---

## Fase 3 · Ensayo

Modo `sample`, unas decenas de filas, **contra BIOMETRICO**:

```bash
java -jar etl/target/portal-salud-etl.jar \
     --etl.files.mode=sample --etl.tags.mode=sample
```

Prueba lo único que falla de verdad —credenciales, red, permisos y el root de archivos— y se
revierte en un minuto con el script de reversa.

Las variables de entorno **las escribes tú**, no van en ningún archivo del repo:
`ETL_SOURCE_URL` (con `characterEncoding=UTF-8`), `ETL_SOURCE_USER`, `ETL_SOURCE_PASSWORD`,
`ETL_TARGET_URL`, `ETL_TARGET_USER`, `ETL_TARGET_PASSWORD`, `ETL_FILES_ROOT`.

Con los modos en `none` (el default) el proceso arranca, no hace nada y termina. No hay forma de
dispararlo sin querer.

### ✅ Corrido el 1-oct-2026 — sin un solo fallo

| | ETL reportó | Oracle tiene |
|---|---:|---:|
| `SERV_MED_FS_FILE` | migrado **49**, sin_url 4, **cuarentena 0** | **49** |
| `SERV_MED_TAG` | migrado **46**, sin_type 0 | **46** |

49 PDFs, 32.41 MB, en carpetas `2023/` y `2024/`: las fechas históricas del origen se
respetaron, no se usó la de hoy.

**`cuarentena=0` es el dato que más vale.** Cuenta los base64 que no decodifican. Cero significa
que el encoding sobrevivió intacto MySQL → dump → MariaDB → Oracle, que era el riesgo heredado
de agosto.

**El pool se llamó `etl-destino`**, no `HikariPool-2`: prueba de que el
`@ConfigurationProperties(prefix="spring.datasource.hikari")` del bean tomó efecto y el pool
está acotado a un intento de login.

**Estas 49 + 46 filas NO se revierten.** Son el primer tramo de la migración; la corrida `full`
las salta por `BUSINESS_KEY` / `SOURCE_ID`. Las metas siguen siendo 22,196 y 551,044.

**Dos datos para planear la Fase 4:**

- El `sampleIds()` de archivos tardó **4 min 40 s** por una sola consulta,
  `GROUP BY MD5(url)`, que obliga a leer los 19.5 GB de la tabla a ~66 MB/s. **`runFull()` no la
  ejecuta** —recorre por rangos de id— así que la corrida completa arranca de inmediato.
- 49 archivos en 31 s, incluido el viaje a Oracle. Extrapolado a 22,196: **~4 horas**.

### Defecto cosmético corregido en el .bat

`echo %ETL_SOURCE_URL%` imprimía la URL partida y cmd intentaba ejecutar el resto
(`'characterEncoding' is not recognized...`). El `&` de la URL se re-interpreta como separador
de comandos al expandir la variable en un `echo`. **El valor llega intacto al proceso java**;
lo único roto era mostrarlo. Se imprime recortado a propósito.

---

## Fase 4 · La corrida

**Tags primero.** Son ~15 minutos y validan la tubería completa antes de comprometer horas:

```bash
java -jar etl/target/portal-salud-etl.jar --etl.tags.mode=full
```

**Después los archivos.** Horas, 15 GB. Los PDF se decodifican en memoria:

```bash
java -Xmx2g -jar etl/target/portal-salud-etl.jar --etl.files.mode=full
```

Es idempotente: si se corta, se vuelve a lanzar y retoma. `files` se salta lo ya cargado por
`BUSINESS_KEY = legacy-<id>`, `tags` por `SOURCE_ID`.

---

## Fase 5 · Copiar los binarios

**El paso que se quedó sin hacer en QA.** Conserva la estructura `yyyy/MM/dd/<hash>/<uuid>.pdf`,
que es como el portal localiza cada archivo:

```bash
rsync -av --progress <ETL_FILES_ROOT local>/ usuario@servidor:<files.root>/
```

### Correrlo DURANTE la Fase 4, no después

El runbook original daba por hecho que nadie usaba el portal todavía, así que la ventana entre
"ya están las filas en Oracle" y "ya están los archivos en el servidor" era gratis. **Desde el
30-sep el portal está en producción**, y esa ventana es gente abriendo expedientes que dan 404.

No hace falta esperar: **`rsync` es incremental y el ETL escribe rutas que nunca cambian** —
nombre UUID aleatorio, carpeta por la fecha *histórica* del archivo (`2023/…`, `2024/…`), nunca
sobrescribe. Así que se lanza el mismo comando dos o tres veces mientras la Fase 4 corre, y una
pasada final al terminar. Cada pasada solo sube lo nuevo. La ventana baja de horas a minutos.

### Por qué "archivos en disco == filas" puede fallar sin que nada esté mal

Dentro de cada fila el orden es `store()` y luego `insert()` (ver `FileEtlRunner`), que es el
orden correcto: si algo se corta, quedan **binarios huérfanos en disco**, nunca filas sin
archivo. Pero `store()` genera un **UUID nuevo cada vez** y no es idempotente en la ruta: la
idempotencia la da el `existsByBusinessKey` del runner, que corta antes de llegar ahí.

Consecuencia: si la Fase 4 se interrumpe y se relanza, por cada fila cortada entre `store()` e
`insert()` queda un binario de más. **La comprobación de la Fase 6 daría archivos > filas sin
que haya ningún error real.** Son bytes desperdiciados, no corrupción.

Si pasa: contar la diferencia. Unos pocos archivos de más tras un corte es lo esperado. Cientos
no, y ahí sí hay que investigar.

Y contar en el servidor:

```bash
find <files.root> -type f | wc -l
du -sh <files.root>
```

### ✅ Hecho el 1-oct-2026 — 22,197 archivos en el servidor

Se subió como un `.zip` de 13 GB y se extrajo en destino. **Resultado: 22,197** = los 22,196
migrados más el huérfano que ya estaba. Cuadra al archivo.

```bash
cd /mnt/data/onedev/apps/exec/filessalud
nohup unzip -n etl-prod-files.zip > /tmp/unzip-etl.log 2>&1 &
find /mnt/data/onedev/apps/exec/filessalud -type f ! -name "*.zip" | wc -l
```

Tres detalles que importaron:

- **`unzip -n` nunca sobrescribe**, así que no podía pisar lo que ya estaba. Y es reanudable:
  si se corta, se relanza y salta lo ya extraído.
- **El `! -name "*.zip"` del conteo** no es cosmético: el zip quedó dentro de `filessalud` y sin
  excluirlo el total daba uno de más.
- **`unzip -l` reporta 41,792 "files"**, pero ahí van incluidas las ~19,596 entradas de
  directorio del árbol `año/mes/día/hash`. Los PDF son 22,196. El tamaño descomprimido,
  15,349,060,737 bytes, sí coincide exacto con los 14.29 GB medidos en origen.

**Borrar el zip después de verificar** para recuperar los 13 GB.

---

## Fase 6 · Verificación

Correr **`docs/script-prod/etl-02-verificacion-biometrico.sql`** (solo lectura, va a BIOMETRICO),
llenando arriba los dos conteos congelados de la Fase 1.

Qué tiene que salir:

### Los números exactos de esta corrida (medidos el 1-oct-2026)

**Línea base antes de cargar: las dos tablas en CERO.** El 1-oct se vació también la única fila
que tenía `SERV_MED_FS_FILE` (una del portal). Eso simplifica el cuadre: cualquier fila que
aparezca ahí es del ETL, sin discriminar por `BUSINESS_KEY` ni `SOURCE_ID`.

| | Origen congelado | Se omiten | **Deben quedar en Oracle** |
|---|---:|---:|---:|
| `files` | 22,223 | **27** sin `url` | **22,196** |
| `tags` | 551,044 | **0** | **551,044** |

Los 27 huérfanos son **los mismos que en agosto**: de los 748 archivos que entraron desde
entonces, ninguno vino sin contenido. No hay un defecto activo en el PHP generando filas vacías.
En `tags` no hay ni uno sin `type`, así que se migran todos.

Las dos cifras están medidas sobre **este** dump, no heredadas del de agosto. Con la línea base
en cero, el cuadre final es una resta directa: si `SERV_MED_FS_FILE` no da 22,196 o
`SERV_MED_TAG` no da 551,044, falta algo y hay que buscarlo.

| Comprobación | Esperado |
|---|---|
| Cuadre `tags` | **551,044** filas, diferencia **0** |
| Cuadre `files` | **22,196** filas, diferencia **27** contra el origen |
| `SOURCE_ID` / `BUSINESS_KEY` repetidos | **0 y 0** |
| Estado de los archivos | todo `ACTIVE`, 0 sin extensión, 0 de tamaño cero |
| `TAG_GROUP = OTRO` | **0** — si aparece, el origen trae tipos que la función no conoce |
| `DATE_UPLOAD` | rango histórico real (2023→), **no** todo con la fecha de hoy |
| **Archivos en disco vs. filas** | **iguales** ← el que faltó en QA |
| Muestra de 10 | existen, pesan igual, el sha256 coincide, abren como PDF |

### ✅ Corrido el 1-oct-2026 — todo cuadra

| Bloque | Resultado |
|---|---|
| A · cuadre | **22,196** archivos · **551,044** tags — las dos metas exactas |
| B · duplicados | **0** `SOURCE_ID` y **0** `BUSINESS_KEY` repetidos |
| C · estado | 22,196 `ACTIVE` · 0 sin extensión · 0 de tamaño cero · 0 sin ruta |
| D · por tipo | `examen_medico` 17,387 · `laboratorio` 2,294 · `nota_medica` 1,680 · `nota_incapacidad` 741 |
| E · `TAG_GROUP` | **ningún `OTRO`**, y los siete grupos suman 551,044 exacto |
| F · fechas | 2023-08-20 → 2026-10-01 (ver nota) |
| G · deben existir | 22,196 archivos, 14.29 GB |
| I · checksums repetidos | 238 |
| J · nombres con encoding roto | 32 |

**En disco local quedaron 22,196 binarios, 14.29 GB — idéntico al conteo de Oracle.** Cero
huérfanos, o sea la corrida no tuvo un solo corte entre `store()` e `insert()`.

Distribución por año de los binarios: 2023 → 990 · 2024 → 7,158 · 2025 → 8,824 · 2026 → 5,224.

**Los 238 checksums repetidos justifican el preflight.** Con el índice de checksum en `UNIQUE`
la carga habría reventado con `ORA-00001` a media corrida. Por eso el bloque D del preflight es
bloqueante y no informativo.

**Un archivo quedó fechado hoy** (`con_fecha_de_hoy = 1`). No es defecto del ETL: es una fila
del legacy sin `date_upload`, y el runner cae a `LocalDateTime.now()` cuando falta. Su binario
fue a parar a `2026/10/01/`. Uno de 22,196.

**Los nombres con encoding roto fueron 32, no ~96.** El estimado venía de agosto. Sigue siendo
cosmético: la búsqueda es por `NSS + FILE_TYPE`, nunca por `ORIGINAL_NAME`.

Lo que **no** es error: ~228 checksums repetidos (un mismo PDF adjunto a varios NSS) y ~96
nombres con encoding roto. Eso último es cosmético — la búsqueda es por `NSS + FILE_TYPE`, nunca
por `ORIGINAL_NAME`. La consulta J del script saca la lista para corregirlos a mano después.

---

## Si hay que revertir

**`docs/script-prod/etl-99-reversa-biometrico.sql`** (va a BIOMETRICO). Trae un simulacro:
con `v_forzar := 'N'` dice cuánto borraría sin borrar nada.

Borra exactamente lo que insertó el ETL:

- `SERV_MED_FS_FILE` → `BUSINESS_KEY LIKE 'legacy-%'`. El portal nunca usa ese prefijo.
- `SERV_MED_TAG` → `SOURCE_ID IS NOT NULL`. El portal escribe en esa misma tabla en runtime
  (contactos de emergencia, diagnósticos secundarios, documentos de examen) pero **nunca** pone
  `SOURCE_ID`. Verificado en el código.

**No usar `MIGRATED_AT` como criterio**: tiene `DEFAULT SYSTIMESTAMP`, así que las filas del
portal también lo traen. El único discriminador confiable es `SOURCE_ID`.

Los binarios se borran aparte, por carpeta de fecha.

---

## Nota de secuencia

El portal todavía no arranca contra BIOMETRICO en producción y la app 27 del launcher tiene 0
roles, así que nadie entra aún. **Eso juega a favor de hacerlo ahora**: la ventana entre "ya
están las filas" y "ya están los archivos" hoy es inofensiva. Con usuarios dentro, esa misma
ventana es gente abriendo archivos que dan 404.

Sigue pendiente, aparte de esto: **rotar la contraseña de `biometrico`**, que quedó en el
historial de git.

---

## Archivos de este paquete

| Archivo | Qué es | Base |
|---|---|---|
| `script-prod/etl-01-preflight-biometrico.sql` | Antes de empezar. Solo lectura | Portal / BIOMETRICO |
| `script-prod/etl-02-verificacion-biometrico.sql` | Después de cargar y copiar. Solo lectura | Portal / BIOMETRICO |
| `script-prod/etl-99-reversa-biometrico.sql` | Reversa, con simulacro. **Borra** | Portal / BIOMETRICO |
| `etl/README.md` | Variables y modos del ETL | — |
| `etl-migracion-historica-especificacion.md` | Mapeo columna por columna | — |
| `etl-migracion-qa-reporte-final.md` | Cómo salió en QA | — |

Ninguno toca ORDS.
