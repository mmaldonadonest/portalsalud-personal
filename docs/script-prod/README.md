# Scripts para producción — Portal Salud en BIOMETRICO

Todo lo que hay que ejecutar en `BIOMETRICO@PDBPRD` para que el portal funcione, en orden.
Preparado el **24-sep-2026**.

**Ejecuta:** el DBA / el área de base de datos, en SQL Developer, conectado como
`BIOMETRICO`, con **Run Script (F5)** (hay bloques PL/SQL delimitados por `/`) y el panel de
**DBMS Output** abierto.

---

## Los archivos

| Orden | Archivo | Qué hace |
|:--:|---|---|
| **1** | `01_ddl_portal_en_biometrico.sql` | Crea las 20 tablas del portal y todo lo que las acompaña. Incluye al inicio el bloque **PRE-CHECK**, que es de solo lectura. |
| **2** | `02_rbac_produccion.local.sql` | Da de alta el rol `ROLE_MEDICO_ANALISTA`, los 7 usuarios y sus permisos. Trae el `COMMIT` al final. |
| — | `99_rollback_portal_en_biometrico.sql` | **Sólo si hay que deshacer.** No forma parte de la instalación. |

Y dos archivos de apoyo, que no se ejecutan como parte de la instalación:

| Archivo | Para qué |
|---|---|
| `proyeccion-crecimiento.md` | Cuánto va a crecer esto. **Lo que hay que provisionar es disco de archivos, no tablespace** — ver el resumen de abajo. |
| `medir-crecimiento-tablas.sql` | Solo lectura. Produjo los números de la proyección y sirve para volver a medir en 3 y 6 meses. |

> **`02_rbac_produccion.local.sql` no está en GitHub, a propósito.** Contiene NSS y nombres
> de empleados reales, así que el `.gitignore` excluye `*.local.sql`. Se entrega por canal
> interno. Si no lo tienes, pídelo — sin él nadie puede entrar a `/admin` ni al Dashboard
> Ejecutivo.

---

## Paso 1 — el DDL

### Antes

**El respaldo no es bloqueante:** la base ya tiene respaldo automático. Se menciona sólo
para tenerlo presente, porque el DDL es autoconfirmado y `ROLLBACK` no sirve.

**Lo único que sí hay que hacer antes es correr el bloque PRE-CHECK** (está al inicio del
archivo, es de solo lectura) y revisar la salida:

| Bloque | Qué debe dar |
|---|---|
| (a) tablas del portal que ya existen | **vacío** |
| (b) choque de nombres de índice o constraint | **vacío** |
| (c) inventario | **64** tablas `SERV_MED_*` y **10** `APP_*` |
| (d) privilegios | `CREATE TABLE` + rol `RESOURCE` + `CREATE ANY VIEW` |

El **(b)** es el que importa. Si un nombre de índice o constraint ya estuviera ocupado, el
guard `ORA-00955` del script se lo tragaría **en silencio** y ese objeto no se crearía. Si
devuelve algo, **parar** y avisar.

### Qué crea

20 tablas, 24 índices, 22 constraints, 8 triggers (sólo estampan `UPDATED_AT` /
`UPDATED_BY`), 6 vistas, 1 función y 1 procedimiento. Todo sobre esas 20 tablas.

Los datos que inserta son semillas de la línea base: 4 roles, 5 permisos, 20 menús, el
usuario `admin`, 2 jobs y 2 plantillas de notificación. **Ningún dato histórico** — las
tablas nacen vacías.

### Qué NO toca

Se auditó el script: se extrajo cada `INSERT`, `UPDATE`, `DELETE`, `MERGE`, `ALTER` y
`CREATE`, incluidos los que van dentro de `EXECUTE IMMEDIATE`, y se comparó el objeto
destino contra la lista de las 20 tablas.

**98 de 98 destinos pertenecen al portal.** Y el script **no contiene ningún `DROP`** —
tampoco `TRUNCATE`, `GRANT`, `REVOKE`, `ALTER USER`, `ALTER SYSTEM`, `ALTER DATABASE`,
`CREATE USER` ni acceso por database link. Verificable con:

```bash
grep -niE "\bDROP\b|\bTRUNCATE\b" 01_ddl_portal_en_biometrico.sql   # 0 líneas
```

Los 11 `LIKE 'SERV_MED_%'` del archivo están todos dentro de `SELECT`, en los bloques
PRE-CHECK y VERIFICACIÓN.

Las únicas cuatro sentencias que modifican datos:

```sql
DELETE FROM SERV_MED_MENU_ROLE
 WHERE MENU_ID = (SELECT ID FROM SERV_MED_MENU WHERE CODE = 'EXPEDIENTE_GENERAL');
UPDATE SERV_MED_SEC_USER SET PASSWORD_HASH = NULL WHERE USERNAME = '68958027838';
UPDATE SERV_MED_FS_FILE  SET VERSION = 1              WHERE VERSION     IS NULL;
UPDATE SERV_MED_FS_FILE  SET DATE_UPLOAD = CREATED_AT WHERE DATE_UPLOAD IS NULL;
```

Las cuatro tablas las crea el mismo script, vacías, así que sólo pueden afectar filas que él
acaba de insertar.

Es **idempotente**: cada `CREATE` va dentro de un bloque que ignora `ORA-00955` ("el objeto
ya existe"), así que se puede volver a correr sin romper nada.

### Revisión estática del archivo (24-sep-2026)

Antes de entregarlo se revisó el `.sql` sin ejecutarlo. Resultados:

| Qué se revisó | Resultado |
|---|---|
| Bloques PL/SQL | 64 bloques en 88 tramos separados por `/`; `BEGIN`/`END` balanceados |
| Literales `q'[…]'` | 21 aperturas / 21 cierres |
| Nombres de objeto | 85 distintos, el más largo de **30** caracteres |
| Versión de Oracle que exige | **12.1** (columnas `IDENTITY`). `BIOMETRICO` es **19c**, alcanza de sobra |
| `DROP` · `TRUNCATE` · `GRANT` · `REVOKE` · `ALTER USER/SYSTEM/DATABASE` · `CREATE USER/TABLESPACE/LINK` · database links | **0 de cada uno** |

> Uno de los scripts de origen se llama `00_init_oracle21c.sql`. Es sólo el nombre del
> archivo, heredado del ambiente donde se escribió: **no exige Oracle 21c**.

### Una sola precaución: la codificación del archivo

El `.sql` está en **UTF-8** y contiene 13 literales con acentos que se **insertan como
datos** — por ejemplo `'Ver menú'`, `'Política por defecto para cargas'`,
`'Purgar auditoría antigua'`, `'Recuperación de contraseña'`.

**En SQL Developer: `Tools > Preferences > Environment > Encoding` debe estar en UTF-8**
antes de abrir el archivo. Si no, esos textos entran con caracteres corruptos.

Esto **no se probó en QA**, porque allí esas filas ya existían de antes y los `MERGE`
reportaron `0 rows merged`: nunca llegaron a escribirse. En producción se insertan por
primera vez.

No es grave —son etiquetas visibles en la aplicación, no claves ni identificadores— y se
comprueba y corrige después con esto:

```sql
-- Deben leerse con acentos correctos: "Ver menú", "Purgar auditoría antigua", etc.
SELECT CODE, NAME FROM SERV_MED_SEC_PERMISSION WHERE CODE = 'MENU_VIEW';
SELECT CODE, NAME FROM SERV_MED_JOB_CATALOG    WHERE CODE = 'JOB_PURGE_AUDIT';
SELECT CODE, SUBJECT FROM SERV_MED_NOTIF_TEMPLATE ORDER BY CODE;
SELECT POLICY_CODE, DESCRIPTION FROM SERV_MED_FS_FILE_POLICY;
```

Si salieron mal, se arreglan con `UPDATE` sobre esas cuatro tablas; no hace falta volver a
correr nada.

### Después

1. `COMMIT;` — los `CREATE` son DDL autoconfirmado, pero los `MERGE`/`INSERT` de las
   semillas necesitan confirmación explícita.
2. Correr el bloque **VERIFICACIÓN** del final: **20 tablas** y **cero objetos `INVALID`**.

---

## Paso 2 — el RBAC

`02_rbac_produccion.local.sql` sale de QA, del estado real que se usa hoy. Hace falta porque
el DDL trae la línea base pero **no incluye `ROLE_MEDICO_ANALISTA`**, y sin ese rol
`/analisis/**` responde **403 a todos**: la aplicación lo exige con
`hasRole("MEDICO_ANALISTA")` y la autoridad es el CODE tal cual, así que el CODE tiene que
ser exactamente `ROLE_MEDICO_ANALISTA`.

Trae 1 rol, 74 asignaciones menú-rol, 7 usuarios y 12 asignaciones usuario-rol. Todo por
`MERGE` e `INSERT … WHERE NOT EXISTS`, o sea idempotente, y resuelto por `CODE` y `USERNAME`
en lugar de por ID, porque los `IDENTITY` no coinciden entre ambientes. **No borra nada.**

`PASSWORD_HASH` va en `NULL` a propósito: con `portal.auth.strategy=LEGACY_PHP` la
contraseña la valida ORDS, el hash local no es fuente de verdad, y así no viajan hashes en
un archivo que se copia.

Al terminar, su propia verificación debe dar:

```
5 roles · 20 menús · 74 menú-rol · 7 usuarios · 12 usuario-rol

ADM 18 · ENFERMERO 9 · ROLE_ADMIN 18 · ROLE_MEDICO_ANALISTA 19 · USER 10
```

Idéntico a QA, con una diferencia esperada: QA muestra 6 roles y 13 asignaciones
usuario-rol porque arrastra un `MEDICO_ANALISTA` inactivo, sin el prefijo `ROLE_`, que fue
un primer intento y no se pudo borrar desde la pantalla. Producción nace sin él.

---

## Cuidado importante con las tablas que ya existen

`BIOMETRICO` ya tiene **64 tablas `SERV_MED_*`** con datos productivos. De esas:

- **53 son del PHP legacy** — el expediente clínico (`SERV_MED_ABDOMEN`,
  `SERV_MED_DIAGNOSTICO`, `SERV_MED_EXPLORACION_FISICA`, `SERV_MED_CAT_INDICE_IDC10` con las
  909 claves ICD, `SERV_MED_DET_ANT_LABORALES` con 51,835 filas…).

- **11 son de este mismo proyecto, pero del lado de ORDS**, no de la conexión JDBC. Se
  crearon en agosto de 2026 para Accidentes, Antidoping, Causas de consulta, Restricciones,
  Maternidad y Predio/Cuenta:

  ```
  SERV_MED_ACCIDENTE              SERV_MED_ANTIDOPING_INVENTARIO
  SERV_MED_ACCIDENTE_SEGUIMIENTO  SERV_MED_ANTIDOPING_RESULTADO
  SERV_MED_CAT_CAUSA_CONSULTA     SERV_MED_ANTIDOPING_SELECCION
  SERV_MED_CUENTA_PREDIO          SERV_MED_MATERNIDAD_SEGUIMIENTO
  SERV_MED_PREDIO                 SERV_MED_RESTRICCION_ASIGNADA
  SERV_MED_RESULTADO_EXAMEN_HIST
  ```

  **Tienen datos reales** y ninguno de estos scripts las toca. Son fáciles de confundir con
  las nuevas porque también son "del portal": la diferencia es la vía de acceso, no el dueño.
  Se reconocen en el diccionario porque son las únicas `SERV_MED_*` sin estadísticas
  recolectadas (`NUM_ROWS` y `LAST_ANALYZED` en blanco).

> **Nunca hacer una limpieza con comodín.** Un
> `DROP … WHERE table_name LIKE 'SERV_MED_%'` borraría las 64, es decir el servicio médico
> completo. Cualquier borrado va por nombre explícito, que es como está escrito el
> `99_rollback`.

---

## Si hay que deshacerlo

`99_rollback_portal_en_biometrico.sql` borra los 20 objetos **por nombre explícito**, nunca
por comodín, y trae una salvaguarda: **no borra tablas que tengan filas**. Si una tiene
datos, la reporta y la deja intacta, así que una corrida por accidente después de la
migración histórica no destruye nada. Para forzarlo hay que cambiar `v_forzar` de `'N'` a
`'S'` a mano.

Su `PASO 0` es de solo lectura y lista cada tabla con su conteo de filas antes de tocar
nada. La verificación final comprueba tres cosas: que no quede ninguna de las 20 del portal,
que las 64 preexistentes sigan ahí, y que las 11 del portal vía ORDS aparezcan una por una.

---

## Capacidad: qué hay que provisionar

Medido el 24-sep-2026 con `medir-crecimiento-tablas.sql`. El detalle está en
`proyeccion-crecimiento.md`; lo esencial:

| | Hoy | A 5 años |
|---|---|---|
| **Filesystem** (los PDF) | **14 GB** | **~207 GB** |
| Oracle, las 20 tablas del portal | 225 MB | ~3.8 GB |
| Oracle, las 64 del servicio médico | 35.8 MB | ~700 MB |

**El filesystem crece 54 veces más rápido que la base.** Un PDF promedio pesa 0.66 MB y la
fila que lo describe 0.33 KB. Como orden de magnitud para la conversación con
infraestructura: **250 GB de filesystem y 5 GB de tablespace** para cinco años.

Dos advertencias sobre esa cifra:

- El escenario de 207 GB asume el modelo real del negocio —examen inicial al candidato,
  examen de ingreso y exámenes periódicos— pero con **dos supuestos sin medir**: cuántos
  candidatos se examinan al mes y cada cuánto es el periódico. Los dos multiplican directo.
- `SERV_MED_FS_FILE_POLICY` viene con `RETENTION_DAYS = 3650` y **no hay proceso de purga**,
  así que hoy el crecimiento es acumulativo puro. Definir retención es la palanca que más
  mueve el número.

## Lo que falta fuera de la base

- **El directorio de archivos.** Los PDF **no van a Oracle**: van al filesystem
  (`portal.files.root`). Esa carpeta tiene que existir y ser escribible por el usuario del
  Tomcat, con el espacio de la tabla de arriba.
- **Las variables de entorno** del Tomcat de producción: `SPRING_DATASOURCE_URL`,
  `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` y `ORACLE_TNS_ADMIN`.
- **ORDS** va por su lado: no vive en el esquema del portal.

---

## Referencias

| Documento | Para qué |
|---|---|
| `docs/que-crea-el-ddl-en-biometrico.md` | La validación del DDL en detalle |
| `docs/fase1-inventario-resultado.md` | El inventario de producción que originó todo esto |
| `docs/seguimiento-migracion-produccion.html` | El tablero de la migración |

Los originales viven en `src/main/resources/db/sql/prod/` (los `.sql` de instalación) y en
`docs/` (la proyección y el medidor). Esta carpeta es una copia para entrega: si se corrige
algo, corregir el original y volver a copiar, para que el arreglo no se quede sólo aquí.
