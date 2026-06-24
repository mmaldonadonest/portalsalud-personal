# README-BOOTSTRAP

Guía de arranque para una arquitectura base **Spring Boot 3 + Java 21 + Oracle 21c + Thymeleaf**, usando **Maven** y scripts SQL manuales (sin Flyway/Liquibase).

---

## 1) Objetivo

Este bootstrap crea una base técnica reusable para proyectos enterprise con:

- Java 21 (LTS)
- Spring Boot 3.x
- Maven (build + dependencias)
- Oracle Database 21c (metadata/transaccional)
- Thymeleaf (SSR)
- SCSS patrón 7-1
- Seguridad RBAC (auth + autorización)
- Auditoría
- Menú dinámico por rol
- Jobs programados (Quartz)
- Envío de correos
- Push desktop y mobile
- Gestión de archivos (metadata en BD + binario en filesystem)

> **Importante:** Este bootstrap **no** incluye épicas o lógica de negocio específica.

---

## 2) Pre-requisitos

- JDK 21 instalado y configurado en `JAVA_HOME`
- Maven 3.9+
- Oracle 21c disponible
- Acceso a SMTP (para módulo mail)
- (Opcional) credenciales FCM/APNs para push mobile

Verificar instalación:

```bash
java -version
mvn -version
```

---

## 3) Estructura mínima esperada

```text
src/main/java/<basePackage>/
  config/
  security/
  audit/
  menu/
  role/
  auth/
  notification/
  job/
  mail/
  file/
  controller/
  service/
  repository/
  domain/
  dto/
  mapper/
  exception/

src/main/resources/
  application.yml
  application-local.yml
  application-dev.yml
  application-test.yml
  application-prod.yml
  db/sql/
  templates/
    layout/base.html
    fragments/{head,header,sidebar-menu,footer,scripts}.html
    pages/{home,auth,error}/
  static/
    scss/
    css/
    js/
    img/
```

---

## 4) Maven (obligatorio)

Este proyecto usa Maven como estándar de build y dependencias.

### 4.1 Dependencias base recomendadas

- `spring-boot-starter-web`
- `spring-boot-starter-thymeleaf`
- `spring-boot-starter-data-jpa`
- `spring-boot-starter-validation`
- `spring-boot-starter-security`
- `spring-boot-starter-actuator`
- `spring-boot-starter-mail`
- `spring-boot-starter-quartz`
- `ojdbc11`
- `spring-boot-starter-test` (test)

### 4.2 Reglas

- **No usar** Flyway/Liquibase.
- Versionado de SQL por convención de nombres y manifiesto.
- Build reproducible con `mvn clean verify`.

---

## 5) Configuración inicial (`application.yml`)

Definir por ambiente:

- Datasource Oracle (`jdbc:oracle:thin:@//host:port/service`)
- JPA/Hibernate (`ddl-auto: none`, dialect Oracle)
- Mail SMTP
- Rutas filesystem para archivos
- Config de Quartz
- Config de seguridad y sesiones
- Actuator/observabilidad

---

## 6) Inicialización de base de datos (sin Flyway)

Los objetos se crean con scripts SQL manuales en:

- `src/main/resources/db/sql`

Orden recomendado:

1. `00_schema.sql`
2. `01_tables_*.sql`
3. `02_indexes.sql`
4. `03_constraints.sql`
5. `04_*_seed.sql`
6. `09_views.sql`
7. `10_procedures.sql`
8. `11_triggers_*.sql`

### Reglas de ejecución

- Idempotente (safe re-run en local/dev)
- `stopOnError=true`
- Prohibido auto-ejecutar en `prod`
- Registrar ejecución en tabla de auditoría SQL

---

## 7) Seguridad base (Auth + Autorización)

- Autenticación por sesión (SSR)
- Password hashing con BCrypt
- Bloqueo por intentos fallidos
- RBAC con tablas de `roles`, `permissions`, `role_permission`, `user_role`
- Protección CSRF
- Rutas públicas mínimas:
  - `/login`
  - `/error`
  - `/css/**`, `/js/**`, `/img/**`

---

## 8) Menú dinámico por rol

- Catálogo de menús jerárquico (padre/hijo)
- Visibilidad por permisos
- Ordenamiento configurable
- Resaltado de ruta activa
- Caché por usuario/rol

---

## 9) Auditoría

Registrar al menos:

- `createdAt`, `createdBy`, `updatedAt`, `updatedBy`
- Eventos de login (success/failure)
- Cambios administrativos relevantes
- Correlation ID por request

Retención sugerida: 365 días (ajustable por política).

---

## 10) Jobs (Quartz)

Casos base sugeridos:

- Reintento de correos
- Reintento de push
- Reconciliación de archivos huérfanos
- Verificación de checksums
- Limpieza de auditoría por retención

Buenas prácticas:

- Jobs idempotentes
- Misfire policy definida
- Evitar concurrencia no controlada por job
- Bitácora de ejecución

---

## 11) Servicio de correos

- SMTP + plantillas Thymeleaf
- Envío asíncrono
- Reintentos con backoff
- Estado de entrega (pendiente/enviado/error)

---

## 12) Push notifications (desktop + mobile)

- Desktop: Web Push (VAPID/FCM)
- Android: FCM
- iOS: APNs (directo o proveedor)
- Persistir subscriptions/tokens
- Soportar opt-in/opt-out
- Reintentos y dead-letter para fallos

---

## 13) Gestión de archivos (BD + filesystem)

### Modelo

- **BD:** metadatos del archivo
- **Filesystem:** contenido binario

### Flujo recomendado (consistencia)

1. Validar tipo/tamaño
2. Guardar en temporal
3. Calcular checksum SHA-256
4. Mover de forma atómica a ruta final
5. Persistir metadatos en BD
6. Emitir evento de dominio (opcional/outbox)

### Reglas

- Nombre físico por UUID (no usar nombre original)
- Sharding de carpetas por fecha/hash
- Protección path traversal
- Logs de acceso/descarga/borrado
- Job de reconciliación (huérfanos)
- Job de verificación de integridad por checksum

---

## 14) Comandos Maven útiles

Compilar + test:

```bash
mvn clean verify
```

Arrancar en local:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Generar artefacto:

```bash
mvn clean package -DskipTests
```

---

## 15) Checklist de aceptación

- [ ] Build exitoso con Java 21 y Maven
- [ ] Conexión Oracle 21c correcta
- [ ] Sin Flyway/Liquibase
- [ ] Scripts SQL base ejecutados en orden
- [ ] Login + RBAC operativo
- [ ] Menú dinámico por rol visible
- [ ] Auditoría registrando eventos clave
- [ ] Jobs Quartz ejecutándose
- [ ] Correo funcional
- [ ] Push desktop/mobile configurado
- [ ] Gestión de archivos BD+filesystem funcional
- [ ] Layout Thymeleaf (header/sidebar/footer) responsive

---

## 16) Convenciones recomendadas

- Branching: `feature/*`, `fix/*`, `hotfix/*`
- Commits: Conventional Commits (`feat:`, `fix:`, `chore:`)
- Calidad: ejecutar `mvn clean verify` antes de PR
- PR: incluir evidencia de checklist de aceptación

---

## 17) No objetivos de este bootstrap

- No incluye lógica de negocio del dominio final
- No incluye integraciones propietarias específicas
- No incluye datos sensibles ni secretos reales

---

## 18) Siguiente paso sugerido

Crear un módulo `home` + `auth` + `menu` + `file` mínimo funcional y dejar pipeline CI ejecutando:

```bash
mvn -B clean verify
```
