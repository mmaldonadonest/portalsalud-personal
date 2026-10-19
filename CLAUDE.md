# Portal Salud

WAR de Spring Boot 3.4.3 · `java.version` 21, pero el JDK instalado es **22** (`C:/oracle/jdk22/jdk-22.0.2`) y por eso **los tests no corren**. Thymeleaf + tema estático en `src/main/resources/static/`. Sin Node, sin build de front.

Despliegue: Tomcat 11 externo en `http://localhost:9999/portal-salud`. Proyecto aparte `etl/` (JAR de consola) para la migración del legacy.

## Reglas que no se negocian

1. **Nunca ejecutar nada contra una base de datos** — ni lecturas, ni ORDS, ni el portal. Se entrega el `.sql` y lo corre el usuario.
2. **Nunca operar en git/GitHub sin permiso explícito.** Nada de `git add -A` a ciegas: ya se publicó una contraseña de producción así.
3. **Cuidado con las configuraciones.** Ningún placeholder que el sistema pueda tomar como valor válido; un `setenv.bat` mal puesto bloqueó la cuenta `BIOMETRICO` en producción.
4. **En local manda el WAR**: nada de variables de entorno ni properties externos. En producción las escribe el usuario a mano.
5. Los defaults fallan hacia lo inofensivo (`spring.profiles.active=local`).

## Dos bases, decir siempre a cuál va cada `.sql`

- **Base del portal** (JDBC): tablas `SERV_MED_*`, 20 en total. Viven **dentro de `BIOMETRICO`**, no en esquema propio.
- **ORDS** (`http://10.249.249.3/biows/ords/security`): los WS del legacy. `php-old/` es la fuente de verdad al migrar casos de uso.

Binarios: metadatos en `SERV_MED_FS_FILE` + archivo en disco (`portal.files.root`), nunca BLOB.

## Estilo

Español sin tildes en identificadores y SQL; con tildes en texto de usuario y documentos. Comentarios que expliquen el *porqué*.
