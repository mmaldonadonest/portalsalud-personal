# Qué crea el DDL del Portal Salud en BIOMETRICO

**Script:** `src/main/resources/db/sql/prod/01_ddl_portal_en_biometrico.sql`
**Base:** `BIOMETRICO@PDBPRD` · **Ejecuta:** el DBA, en SQL Developer, como `BIOMETRICO`
**Fecha del inventario:** 24-sep-2026

El portal Java necesita 20 tablas propias para funcionar. No tendrá esquema aparte: viven
dentro de `BIOMETRICO`, con prefijo `SERV_MED_`. Este documento dice exactamente qué toca
el script y qué no.

---

## 1. Qué crea

| Qué | Cuánto | Alcance |
|---|---|---|
| Tablas | 20 | las del portal, ninguna más |
| Índices | 24 con nombre + los de las PK | todos sobre esas 20 |
| Constraints | 22 con nombre + las PK del sistema | todas sobre esas 20 |
| Triggers | 8 | uno por tabla; sólo estampan `UPDATED_AT` / `UPDATED_BY` |
| Vistas | 6 | leen sólo `SEC_USER`, `FS_FILE`, `TAG` y `TAG_MIG_LOG` |
| Función | 1 | `SERV_MED_FN_TAG_GROUP`: lógica pura, no accede a ninguna tabla |
| Procedimiento | 1 | `SERV_MED_SP_LOG_SQL_EXEC`: sólo inserta en `SERV_MED_AUDIT_SQL_EXECUTION` |

### Las 20 tablas

```
Seguridad y menús (RBAC del portal)
  SERV_MED_SEC_USER              SERV_MED_SEC_ROLE
  SERV_MED_SEC_USER_ROLE         SERV_MED_SEC_PERMISSION
  SERV_MED_SEC_ROLE_PERMISSION   SERV_MED_MENU
  SERV_MED_MENU_ROLE

Auditoría
  SERV_MED_AUD_EVENT             SERV_MED_AUDIT_SQL_EXECUTION

Archivos (metadatos; el binario va al filesystem, NO a la base)
  SERV_MED_FS_FILE               SERV_MED_FS_FILE_VERSION
  SERV_MED_FS_FILE_ACCESS_LOG    SERV_MED_FS_FILE_POLICY
  SERV_MED_FS_FILE_ORPHAN

Importador de Excel (staging)
  SERV_MED_IMPORT_LOTE           SERV_MED_IMPORT_FILA

Infraestructura
  SERV_MED_JOB_CATALOG           SERV_MED_NOTIF_TEMPLATE

Histórico del legacy (EAV)
  SERV_MED_TAG                   SERV_MED_TAG_MIG_LOG
```

### Los datos que inserta

Son **semillas de la línea base**, nada más:

- 4 roles (`ROLE_ADMIN`, `USER`, `ADM`, `ENFERMERO`) y 5 permisos
- 20 menús y las asignaciones menú-rol de `USER` / `ADM` / `ENFERMERO`
- el usuario `admin`, 1 política de archivos, 2 jobs y 2 plantillas de notificación

**Ningún dato histórico.** Las tablas nacen vacías; cargar la migración es un paso aparte.

---

## 2. Qué NO toca

Se auditó el script mecánicamente: se extrajo cada `INSERT`, `UPDATE`, `DELETE`, `MERGE`,
`ALTER TABLE`, `CREATE …` y `COMMENT` —incluidos los que van dentro de
`EXECUTE IMMEDIATE`— y se comparó el objeto destino contra la lista de las 20 tablas.

**Resultado: 98 de 98 destinos de escritura pertenecen al esquema del portal.** Ninguna
sentencia tiene como destino una tabla, índice u objeto ajeno.

Verbos que **no aparecen** en el script:

`DROP TABLE` · `TRUNCATE` · `GRANT` · `REVOKE` · `ALTER USER` · `ALTER SYSTEM` ·
`ALTER DATABASE` · `CREATE USER` · `CREATE TABLESPACE` · acceso por database link

Los únicos borrados y modificaciones del script, completos:

```sql
DELETE FROM SERV_MED_MENU_ROLE
 WHERE MENU_ID = (SELECT ID FROM SERV_MED_MENU WHERE CODE = 'EXPEDIENTE_GENERAL');
UPDATE SERV_MED_SEC_USER SET PASSWORD_HASH = NULL WHERE USERNAME = '68958027838';
UPDATE SERV_MED_FS_FILE  SET VERSION = 1              WHERE VERSION     IS NULL;
UPDATE SERV_MED_FS_FILE  SET DATE_UPLOAD = CREATED_AT WHERE DATE_UPLOAD IS NULL;
```

Las cuatro tablas las crea el mismo script, vacías, así que sólo pueden afectar filas que
él acaba de insertar.

---

## 3. Lo que ya existe en BIOMETRICO y se queda igual

Al 24-sep-2026 el esquema tiene **64 tablas `SERV_MED_*`** y **10 `APP_*`**. Ninguna
choca por nombre con las 20 del portal. Después de instalar habrá 84 `SERV_MED_*`.

De esas 64:

- **53 son del PHP legacy** — el expediente clínico: `SERV_MED_ABDOMEN`,
  `SERV_MED_DIAGNOSTICO`, `SERV_MED_EXPLORACION_FISICA`, `SERV_MED_RESULTADO_EXAMEN`,
  `SERV_MED_CAT_INDICE_IDC10` (909 claves ICD), `SERV_MED_DET_ANT_LABORALES` (37,770
  filas)… casi todas con 3,056–3,069 filas, un renglón por examen.

- **11 son del portal, pero del lado de ORDS.** Se crearon en agosto de 2026 para
  Accidentes, Antidoping, Causas de consulta, Restricciones, Maternidad y Predio/Cuenta.
  El portal las consume por HTTP a través de los WS de ORDS, **no** por la conexión JDBC,
  y por eso no están entre las 20:

  ```
  SERV_MED_ACCIDENTE              SERV_MED_ANTIDOPING_INVENTARIO
  SERV_MED_ACCIDENTE_SEGUIMIENTO  SERV_MED_ANTIDOPING_RESULTADO
  SERV_MED_CAT_CAUSA_CONSULTA     SERV_MED_ANTIDOPING_SELECCION
  SERV_MED_CUENTA_PREDIO          SERV_MED_MATERNIDAD_SEGUIMIENTO
  SERV_MED_PREDIO                 SERV_MED_RESTRICCION_ASIGNADA
  SERV_MED_RESULTADO_EXAMEN_HIST
  ```

  **Tienen datos reales.** Son fáciles de confundir con las nuestras porque también son
  "del portal": la diferencia es la vía de acceso, no el dueño. Se reconocen en el
  diccionario porque son las únicas `SERV_MED_*` sin estadísticas recolectadas
  (`NUM_ROWS` y `LAST_ANALYZED` en blanco).

> **Nunca hacer una limpieza con comodín.** Un
> `DROP … WHERE table_name LIKE 'SERV_MED_%'` borraría las 64, es decir el servicio médico
> completo. Cualquier borrado va por nombre explícito.

---

## 4. Antes de ejecutar

1. **Respaldo del esquema** (`expdp`). El DDL es autoconfirmado: `ROLLBACK` no sirve. El
   respaldo es lo único que devuelve el estado anterior.

2. **Correr sólo el bloque PRE-CHECK** (está al inicio del mismo archivo, es de solo
   lectura) y revisar la salida antes de seguir:

   | Bloque | Qué debe dar en BIOMETRICO |
   |---|---|
   | (a) tablas del portal que ya existen | **vacío** |
   | (b) choque de índices / constraints | **vacío** |
   | (c) inventario | **64** `SERV_MED_*` y **10** `APP_*` |
   | (d) privilegios | `CREATE TABLE` + rol `RESOURCE` + `CREATE ANY VIEW` |

   El bloque (b) es el que importa: si un nombre de índice o constraint ya estuviera
   ocupado, el guard `ORA-00955` del script se lo tragaría **en silencio** y ese objeto no
   se crearía. Si devuelve algo, parar.

3. Ejecutar con **Run Script (F5)**, no sentencia por sentencia: hay bloques PL/SQL
   delimitados por `/`. Y tener abierto el panel de **DBMS Output** para ver los
   `[OK]` / `[SKIP]`.

El script es **idempotente**: cada `CREATE` va dentro de un bloque que ignora
`ORA-00955` ("el objeto ya existe"), así que se puede volver a correr sin romper nada.

---

## 5. Después de ejecutar

1. `COMMIT;` — los `CREATE` son DDL autoconfirmado, pero los `MERGE` / `INSERT` de las
   semillas (roles y menús) necesitan confirmación explícita.

2. Correr el bloque **VERIFICACIÓN** del final del archivo: deben aparecer **las 20
   tablas** y **cero objetos `INVALID`**.

3. Sembrar el RBAC real. El DDL sólo trae la línea base y **no incluye
   `ROLE_MEDICO_ANALISTA`**, sin el cual `/analisis/**` responde 403 a todos. Ese rol,
   junto con los usuarios y las asignaciones reales, se extrae de QA con
   `src/main/resources/db/sql/05_exportar_rbac_desde_qa.sql`, que es de solo lectura y
   genera los `INSERT` para pegar aquí.

4. Arrancar el WAR con perfil `prod`. Con `ddl-auto=validate`, si faltara algo no arranca
   y el log dice `Schema-validation: missing table` con el nombre exacto.

También hace falta, fuera de la base: que exista y sea escribible el directorio de
`portal.files.root` en el servidor (los binarios de los archivos **no** van a Oracle), y
las variables de entorno `SPRING_DATASOURCE_*` y `ORACLE_TNS_ADMIN` en el Tomcat.

---

## 6. Si hay que deshacerlo

`src/main/resources/db/sql/prod/02_rollback_portal_en_biometrico.sql`

Borra los 20 objetos **por nombre explícito**, nunca por comodín, y trae una salvaguarda:
**no borra tablas que tengan filas**. Si una tiene datos, la reporta y la deja intacta —
así una corrida por accidente después de la migración histórica no destruye nada. Para
forzarlo hay que cambiar `v_forzar` de `'N'` a `'S'` a mano.

Su verificación final comprueba tres cosas: que no quede ninguna de las 20 del portal, que
las 64 preexistentes sigan ahí, y que las 11 del portal vía ORDS aparezcan una por una.
