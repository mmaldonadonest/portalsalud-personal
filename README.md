# Portal Salud

Portal del Servicio Médico (Salud Ocupacional) de Onest. Se entrega como un **WAR autosuficiente**:
la configuración de producción, incluida la conexión a la base, viaja dentro del archivo. El
servidor solo pone Java, Tomcat, el `tnsnames.ora` y la carpeta de archivos.

---

## 1. Stack tecnológico

| Capa | Tecnología | Versión |
|---|---|---|
| Lenguaje | Java (compilado con `release 21`) | 21 |
| Framework | Spring Boot (WAR, sin Tomcat embebido en producción) | 3.4.3 |
| Núcleo | Spring Framework | 6.2.3 |
| Seguridad | Spring Security + validación de JWT del SSO (Nimbus JOSE) | 6.4.3 / 9.37.3 |
| Vistas | Thymeleaf (server-side, sin Node ni build de front) | 3.1.3 |
| Persistencia | Spring Data JPA + Hibernate (`ddl-auto=validate`) | 6.6.8 |
| Pool de conexiones | HikariCP | 5.1.0 |
| Driver de base | Oracle JDBC `ojdbc11` | 23.5 |
| Base de datos | Oracle 19c, PDB `PDBPRD`, esquema `BIOMETRICO` (tablas `SERV_MED_*`) | 19c |
| Web services legacy | ORDS `http://10.249.249.3/biows/ords/security` | — |
| Tareas programadas | Quartz | 2.3.2 |
| Excel | Apache POI | 5.3.0 |
| Servidor de aplicaciones | Apache Tomcat (Jakarta Servlet 6.0) | 10.1 o 11 |
| Build | Maven | 3.9+ |

Los archivos (PDF, imágenes) **no van en la base**: los metadatos se guardan en `SERV_MED_FS_FILE`
y el binario en disco, bajo `portal.files.root`.

---

## 2. Requisitos del servidor

| Requisito | Detalle |
|---|---|
| Java | JDK o JRE **21 o superior** |
| Tomcat | **10.1.x u 11.x**. Tomcat 9 o anteriores **no** sirven: son `javax`, el portal es `jakarta` |
| `tnsnames.ora` | Por defecto en **`/mnt/data/onedev/apps/exec/tomcat/bin/oracle_config/tnsnames.ora`**, con el alias `PDBPRD` (ver abajo). La carpeta se puede cambiar: ver [Rutas configurables](#rutas-configurables-por-servidor) |
| Carpeta de archivos | Por defecto **`/mnt/data/onedev/apps/exec/filessalud`**: debe existir y el usuario de Tomcat debe poder escribir en ella. Se puede cambiar: ver [Rutas configurables](#rutas-configurables-por-servidor) |
| Red | Salida a Oracle `10.249.249.35:1521`, ORDS `10.249.249.3:80` y HTTPS a `sso.onestcloud.mx`, `intranet.onestcloud.mx` y `ai-docs.onestcloud.mx` (OCR). Opcional: Meilisearch en `127.0.0.1:7700` para el buscador de ayuda; si no está, la ayuda busca en Oracle |

Entrada que debe tener el `tnsnames.ora` (es la base a la que se conecta el portal):

```
PDBPRD =
  (DESCRIPTION =
    (ADDRESS = (PROTOCOL = TCP)(HOST = 10.249.249.35)(PORT = 1521))
    (CONNECT_DATA = (SERVICE_NAME = pdbprd.subnetdb.vcnocionest.oraclevcn.com))
  )
```

**No hace falta** `setenv.sh`, `setenv.bat`, `CATALINA_OPTS` ni ninguna variable de entorno: con
las rutas por defecto, el WAR arranca solo. **Si el Tomcat tiene un `setenv` viejo del portal**
(con `SPRING_DATASOURCE_*` o `spring.profiles.active`), **hay que quitarlo**: Spring Boot deja que
una variable de entorno gane sobre lo que trae el WAR, así que un `SPRING_DATASOURCE_URL` olvidado
cambiaría a qué base se conecta el portal.

### Rutas configurables por servidor

Las dos rutas del servidor traen su valor por defecto dentro del WAR y cada servidor las puede
reescribir **sin rearmar el WAR**:

| Llave | Qué es | Valor por defecto (en el WAR) |
|---|---|---|
| `portal.oracle.tns-admin` | Carpeta donde está el `tnsnames.ora` | `/mnt/data/onedev/apps/exec/tomcat/bin/oracle_config` |
| `portal.files.root` | Carpeta de los archivos del expediente | `/mnt/data/onedev/apps/exec/filessalud` |

Hay tres formas. Si se usan varias, **gana la primera de la lista**:

**1. XML del contexto de Tomcat (recomendada).** Solo afecta al portal. Archivo
`$CATALINA_BASE/conf/Catalina/localhost/portal-salud.xml`. Se crea si no existe; su nombre debe
coincidir con el del WAR, y `localhost` es el nombre del host en `server.xml` (si el host se llama
distinto, va en la carpeta de ese host). Este archivo no se borra al redesplegar el WAR:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Context>
    <Parameter name="portal.oracle.tns-admin" value="/ruta/a/oracle_config" override="false"/>
    <Parameter name="portal.files.root"       value="/ruta/a/filessalud"     override="false"/>
</Context>
```

Se pone solo la llave que cambia; la otra se queda con su valor por defecto. Tomcat vuelve a
desplegar el portal al detectar el cambio de ese archivo, o al reiniciar.

**2. `bin/setenv.sh`.** Afecta a **todas** las aplicaciones del Tomcat. Usar solo si no se puede
usar el XML:

```sh
CATALINA_OPTS="$CATALINA_OPTS -Dportal.oracle.tns-admin=/ruta/a/oracle_config -Dportal.files.root=/ruta/a/filessalud"
```

Ahí solo deben ir estas dos rutas. Nada de `spring.datasource.*` ni `spring.profiles.active`.

**3. Valor por defecto del WAR** (`application-prod.properties`), si no se definió nada arriba.

**Cómo comprobar qué tomó:** el bloque `BASE DE DATOS ACTIVA` del log de arranque dice de dónde
salió cada dato y qué fuentes se leyeron, en orden de prioridad. Ejemplo con la carpeta del TNS
reescrita en el XML:

```
  Perfil(es) Spring : prod  (de: WAR, application.properties)
  URL configurada   : jdbc:oracle:thin:@PDBPRD  (de: WAR, application-prod.properties)
  Usuario config.   : biometrico  (de: WAR, application-prod.properties)
  TNS_ADMIN          : /ruta/a/oracle_config  (de: XML del contexto de Tomcat)
  Archivos (root)    : /mnt/data/onedev/apps/exec/filessalud  (de: WAR, application-prod.properties)
  --- Configuracion leida de (si una llave esta en varias, gana la primera) ---
  1. XML del contexto de Tomcat                : portal.oracle.tns-admin
  2. setenv, -D en CATALINA_OPTS               : nada del portal
  3. setenv, variable de entorno               : nada del portal
  4. WAR, application-prod.properties          : 14 llaves
  5. WAR, application.properties               : 50 llaves
```

En las fuentes del servidor (1 a 3) se listan solo los **nombres** de las llaves del portal, nunca
sus valores. Si en la 2 o la 3 aparece algo como `SPRING_DATASOURCE_URL` o
`spring.profiles.active`, el servidor tiene un `setenv` viejo que está cambiando la configuración
del WAR, y hay que quitarlo.

---

## 3. Build con Maven

### Archivo de configuración de producción

Maven toma los valores de producción de **`config/produccion/portal-salud.local.properties`**
(versionado en este repositorio privado). Estas son las llaves que debe tener:

| Llave | Qué es | Valor en producción |
|---|---|---|
| `prod.datasource.url` | URL JDBC | `jdbc:oracle:thin:@PDBPRD` (alias del `tnsnames.ora`) |
| `prod.tns.admin` | Carpeta del `tnsnames.ora`: valor por defecto de `portal.oracle.tns-admin` (cada servidor la puede reescribir) | `/mnt/data/onedev/apps/exec/tomcat/bin/oracle_config` |
| `prod.datasource.username` | Usuario de la base | `biometrico` |
| `prod.datasource.password` | Contraseña de la base | *(solo en ese archivo; no escribirla en documentos ni mensajes)* |
| `ocr.apikey` / `ocr.hmac.secret` | Llaves de la integración con OCR | *(solo en ese archivo)* |
| `ocr.callback.url` | Webhook de OCR | `https://ai-docs.onestcloud.mx/api/v1/integracion/portal/examen-inicial/dictamen` |
| `meili.key` | Llave del buscador del centro de ayuda (opcional) | *(solo en ese archivo)* |

Si la contraseña de `biometrico` cambia, se actualiza **solo ahí** y se vuelve a generar el WAR.

### Comando

Desde la raíz del proyecto, con `JAVA_HOME` apuntando a un JDK 21 o superior:

```sh
mvn -Pprod -DskipTests clean package
```

Windows (máquina de desarrollo, JDK 22):

```bat
set "JAVA_HOME=C:\oracle\jdk22\jdk-22.0.2"
mvn -Pprod -DskipTests clean package
```

Resultado: **`target/portal-salud.war`**.

- **Siempre con `-Pprod`.** Sin ese perfil el WAR sale con el perfil `local` y la conexión a
  producción vacía: no se conecta a nada productivo. Es a propósito, para que un WAR armado sin
  pensar falle hacia lo inofensivo.
- **`-DskipTests`:** las pruebas automáticas no corren con el JDK 22 de la máquina de desarrollo.
- **Otros perfiles:** `-Pqa` arma el WAR de QA con `config/produccion/portal-salud-qa.local.properties`.
  Sin perfil, el WAR es para pruebas locales.

### Verificar el WAR antes de entregarlo

El WAR debe traer `spring.profiles.active=prod` en `WEB-INF/classes/application.properties`, y la
URL, el usuario, la carpeta del TNS (`portal.oracle.tns-admin`) y la de archivos en
`WEB-INF/classes/application-prod.properties`. Con PowerShell:

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$z = [IO.Compression.ZipFile]::OpenRead((Resolve-Path "target\portal-salud.war"))
foreach ($n in "WEB-INF/classes/application.properties", "WEB-INF/classes/application-prod.properties") {
  $r = New-Object IO.StreamReader($z.GetEntry($n).Open()); $t = $r.ReadToEnd(); $r.Close()
  "== $n"; ($t -split "`n") | Where-Object { $_ -match "^(spring.profiles.active|spring.datasource.url|spring.datasource.username|portal.oracle.tns-admin|portal.files.root)" }
}
$z.Dispose()
```

---

## 4. Despliegue

1. Detener Tomcat.
2. Borrar el despliegue anterior: `webapps/portal-salud.war`, la carpeta `webapps/portal-salud/` y
   `work/Catalina/localhost/portal-salud/`.
3. Copiar `target/portal-salud.war` a `webapps/` **sin cambiarle el nombre**: Tomcat arma la ruta
   con el nombre del archivo y el portal queda en `/portal-salud`.
4. Iniciar Tomcat y revisar `logs/catalina.out` (o `catalina.<fecha>.log`).

### Cómo saber que conectó a la base correcta

Al arrancar, el log imprime un bloque **`BASE DE DATOS ACTIVA`** con la URL JDBC, el usuario y
`DB_NAME / PDB` que realmente tomó. Debe decir `PDBPRD` y `BIOMETRICO`. El mismo bloque dice de
qué fuente salió cada dato (ver [Rutas configurables](#rutas-configurables-por-servidor)). Después, la app responde en
`http://<servidor>:<puerto>/portal-salud/login`.

### Protección de la cuenta BIOMETRICO

Si la contraseña es incorrecta, el arranque falla con **un solo intento de login** (Hikari con
`minimum-idle=0` e `initialization-fail-timeout=1`, en `application.properties`). Así un arranque
fallido no bloquea la cuenta, que es lo que pasó el 27-sep-2026 con los 10 intentos por defecto.

---

## 5. Probar el WAR en un Tomcat de desarrollo (Windows)

La máquina de desarrollo no tiene el `tnsnames.ora` en la ruta Linux, así que el WAR de producción
no conecta tal cual. El Tomcat local (`C:\tomcat\apache-tomcat-11.0.21`) sirve la carpeta que
Maven deja junto al WAR, `target/portal-salud/`, mediante `conf/Catalina/localhost/portal-salud.xml`.
Para probar **sin tocar el WAR**, se ajusta solo esa copia, en
`target/portal-salud/WEB-INF/classes/application-prod.properties`:

- `spring.datasource.url=jdbc:oracle:thin:@//10.249.249.35:1521/pdbprd.subnetdb.vcnocionest.oraclevcn.com`
  (conexión directa a la misma base; con ella el driver no usa el `tnsnames.ora`, así que la carpeta del TNS no estorba)
- agregar `portal.tareas-programadas.activas=false` (para que el Tomcat local no ejecute las tareas programadas de producción)

Ese ajuste se pierde al volver a desplegar, que es lo que se quiere: el `.war` de `target/` nunca
lleva cambios locales.

---

## Otros documentos

- `docs/conexiones-ambientes.md`: bases y ambientes.
- `docs/paso-a-produccion-dos-bases.md`: qué `.sql` va a la base del portal y cuál a ORDS.
- Cargadores de una sola vez (`etl/`: migración del histórico del sistema anterior; `excel-loader/`:
  carga de los Excel): no están en `main`, para que nadie los ejecute por error y sobrescriba
  datos. Su código está en la rama `archivo/cargadores`.
