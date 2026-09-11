# Portal Salud Ocupacional — Backend

Backend en Java del Portal Integral de Salud Ocupacional. Sustituye el proceso
basado en `10 REPORTE MORBILIDAD 2026 GERENCIA.xlsm` y sus ocho libros satélite.

## Requisitos

| Herramienta | Versión | Descarga |
|---|---|---|
| JDK | 21 | [Eclipse Temurin](https://adoptium.net/temurin/releases/?version=21) |
| Maven | 3.9 o superior | [maven.apache.org](https://maven.apache.org/download.cgi) |
| Docker Desktop | reciente | [docker.com](https://www.docker.com/products/docker-desktop/) |

Instalación rápida en Windows:

```powershell
winget install EclipseAdoptium.Temurin.21.JDK
winget install Apache.Maven
```

Cierra y vuelve a abrir la terminal, y confirma:

```bash
java -version
mvn -version
```

## Arranque

```bash
docker compose up -d
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Abre `http://localhost:8080`. Flyway crea el esquema y carga los catálogos en el
primer arranque.

| Correo | Contraseña |
|---|---|
| `admin@saludocupacional.mx` | `CambiarEnProduccion123!` |

Cámbiala en el primer ingreso mediante `POST /api/auth/password`.

## Pruebas

```bash
mvn test
```

Levantan su propio PostgreSQL con Testcontainers, de modo que no tocan la base de
desarrollo. Requieren Docker en ejecución.

## Qué contiene

**Ciento cincuenta y seis clases** organizadas por módulo funcional, más siete
plantillas y tres migraciones. El detalle de la organización y sus razones está
en [`../docs/08_ARQUITECTURA_JAVA.md`](../docs/08_ARQUITECTURA_JAVA.md).

```
mx.saludocupacional.portal
├── shared/        auditoría, errores, periodos, umbrales
├── security/      usuarios, roles, permisos, autenticación
├── catalog/       diecisiete catálogos maestros
├── employee/      colaboradores
├── morbidity/     atenciones médicas
├── disability/    incapacidades
├── accident/      accidentabilidad
├── medicalexam/   exámenes médicos
├── drugtest/      antidoping e inventario
├── maternity/     maternidad
├── analytics/     capa analítica única
├── importer/      importación desde Excel
├── report/        reportes
├── audit/         consulta de bitácora
└── ui/            vistas con Thymeleaf
```

## Reglas que sostienen el diseño

**Un hecho se captura una sola vez.** Un accidente que incapacita crea su
incapacidad en la misma transacción, enlazada por `origenTipo` y `origenId`. El
módulo de incapacidades rechaza los tipos que pertenecen a otro módulo, y dar de
baja el accidente arrastra su incapacidad. Lo mismo aplica a maternidad.

**La existencia no se edita, se deriva.** El disponible de un lote de antidoping
es su cantidad inicial más el saldo de sus movimientos. Registrar una prueba
descuenta una unidad automáticamente.

**Ningún umbral vive en el código.** Los días que definen un predio en riesgo
crítico, la proximidad de caducidad que dispara una alerta y la variación que
genera un hallazgo se administran desde la tabla `risk_thresholds`.

**Una sola fuente de cifras.** Dashboard y reportes consultan el mismo
`AnalyticsService`, de modo que la pantalla y el archivo descargado nunca
difieren.

## API

| Recurso | Rutas |
|---|---|
| Autenticación | `POST /api/auth/login`, `/logout`, `/password` · `GET /api/auth/perfil` |
| Catálogos | `GET /api/catalogos` y sus rutas por catálogo |
| Colaboradores | `/api/empleados` |
| Atenciones | `/api/atenciones` |
| Incapacidades | `/api/incapacidades` |
| Accidentabilidad | `/api/accidentes` |
| Exámenes | `/api/examenes` |
| Antidoping | `/api/antidoping/pruebas`, `/inventario`, `/inventario/alertas` |
| Maternidad | `/api/maternidad` |
| Dashboard | `/api/dashboard/resumen`, `/tendencia`, `/predios`, `/causas`, `/insights` |
| Importación | `POST /api/importaciones/analizar`, `/{id}/confirmar` |
| Reportes | `GET /api/reportes/ejecutivo.xlsx`, `/predios.csv` |
| Auditoría | `GET /api/auditoria` |

La documentación interactiva queda en `http://localhost:8080/swagger-ui.html`.

## Estado

| Fase | Contenido | Estado |
|---|---|---|
| 1 | Esqueleto, migraciones, catálogos con datos reales | Completa |
| 2 | Seguridad: usuarios, roles, permisos, autenticación | Completa |
| 3 | Colaboradores y administración de catálogos | Completa |
| 4 | Morbilidad, incapacidades, accidentes, exámenes | Completa |
| 5 | Antidoping, inventario, maternidad | Completa |
| 6 | Capa analítica y endpoints del dashboard | Completa |
| 7 | Vistas con Thymeleaf | Dashboard listo; faltan las vistas de captura |
| 8 | Importador de Excel | Lector y flujo completos; falta mapear más hojas |
| 9 | Reportes | Excel y CSV listos; falta el PDF |
| 10 | Pruebas y despliegue | Pruebas de las reglas críticas y Dockerfile listos |

## Lo que sigue

1. **Vistas de captura**: el dashboard ya se sirve con Thymeleaf; faltan las
   pantallas de alta y edición de cada módulo, que consumirán la API existente.
2. **Más hojas en el importador**: el lector y el flujo de dos tiempos están
   completos, y hoy mapea la hoja de atenciones. Añadir otra hoja consiste en
   escribir su método de análisis siguiendo el mismo patrón.
3. **Reporte en PDF**: la dependencia ya está declarada; el servicio genera
   Excel y CSV desde la capa analítica, y el PDF seguiría el mismo camino.

## Variables de entorno en producción

Ningún secreto vive en el repositorio.

| Variable | Descripción |
|---|---|
| `DB_URL` | Cadena de conexión a PostgreSQL |
| `DB_USER` | Usuario de base de datos |
| `DB_PASSWORD` | Contraseña de base de datos |
| `JWT_SECRET` | Clave de firma, mínimo treinta y dos caracteres |
| `CORS_ORIGINS` | Orígenes permitidos, separados por coma |

## Despliegue con contenedor

```bash
docker build -t portal-salud .
docker run -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://tu-servidor:5432/salud_ocupacional \
  -e DB_USER=... -e DB_PASSWORD=... -e JWT_SECRET=... \
  portal-salud
```
