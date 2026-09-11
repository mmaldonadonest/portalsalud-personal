# 08 — Arquitectura del Backend Java

> Decisiones estructurales del backend. Este documento es la referencia que evita el "código regado": define dónde va cada cosa y por qué.

## 1. Stack

| Capa | Tecnología | Versión |
|---|---|---|
| Lenguaje | Java | 21 (LTS) |
| Framework | Spring Boot | 3.3.x |
| Persistencia | Spring Data JPA + Hibernate | incluida |
| Base de datos | PostgreSQL | 16 |
| Migraciones | Flyway | incluida |
| Seguridad | Spring Security + JWT (jjwt) | 0.12.x |
| Vistas | Thymeleaf + HTML/JS | incluida |
| Build | Maven | 3.9+ |
| Utilidades | Lombok, MapStruct | 1.18 / 1.6 |
| Pruebas | JUnit 5, Testcontainers, AssertJ | incluidas |

Java 21 y no 17: permite `record`, pattern matching y virtual threads, que simplifican los DTOs y las consultas analíticas.

## 2. Organización: por módulo funcional, no por capa técnica

**Descartado** — agrupar por tipo de clase produce paquetes de 40 archivos sin relación entre sí:

```
controller/   ← 16 controladores mezclados
service/      ← 16 servicios mezclados
repository/   ← 30 repositorios mezclados
```

**Adoptado** — cada módulo contiene todo lo suyo y se puede entender de forma aislada:

```
mx.saludocupacional.portal
├── PortalApplication.java
│
├── shared/                          # transversal, sin dependencias de módulos
│   ├── config/                      # SecurityConfig, JpaConfig, WebConfig, OpenApiConfig
│   ├── exception/                   # ApiException, GlobalExceptionHandler, ErrorResponse
│   ├── audit/                       # AuditLog, AuditService, @Auditable, AuditAspect
│   ├── domain/                      # BaseEntity, SoftDeletable, Period
│   └── util/                        # DateUtils, NumberUtils
│
├── security/                        # autenticación y autorización
│   ├── domain/                      # User, Role, Permission, UserPredioAccess
│   ├── repository/
│   ├── service/                     # AuthService, JwtService, PermissionService
│   ├── web/                         # AuthController, dto/
│   └── PredioScopeFilter.java       # acota consultas por predio del usuario
│
├── catalog/                         # los 17 catálogos maestros
│   ├── domain/                      # Predio, Cuenta, Area, Puesto, AttentionCause…
│   ├── repository/
│   ├── service/                     # CatalogService genérico + PredioService
│   └── web/                         # CatalogController, dto/
│
├── employee/                        # entidad central de personas
│   ├── domain/ repository/ service/ web/
│
├── morbidity/                       # atenciones médicas y causas
│   ├── domain/                      # MedicalAttention
│   ├── repository/ service/ web/
│
├── disability/                      # incapacidades (IMSS, internas, todas)
│   ├── domain/                      # Disability, DisabilityCost
│   ├── repository/ service/ web/
│
├── accident/                        # accidentabilidad → genera incapacidad
│   ├── domain/ repository/ service/ web/
│
├── medicalexam/                     # exámenes médicos
│   ├── domain/ repository/ service/ web/
│
├── drugtest/                        # antidoping, alcoholimetría e inventario
│   ├── domain/                      # DrugTest, DrugTestBatch, InventoryMovement
│   ├── repository/ service/ web/
│
├── maternity/                       # maternidad → genera incapacidad
│   ├── domain/ repository/ service/ web/
│
├── analytics/                       # capa analítica ÚNICA
│   ├── dto/                         # KpiSummary, TrendSeries, PredioRanking, Insight
│   ├── repository/                  # consultas nativas agregadas
│   ├── service/                     # AnalyticsService, InsightEngine, RiskEvaluator
│   └── web/                         # DashboardController (JSON)
│
├── report/                          # PDF, Excel, CSV — consume analytics
│   ├── service/ web/
│
├── importer/                        # migración desde Excel
│   ├── domain/                      # ImportBatch, ImportBatchRow
│   ├── service/                     # ExcelParser, ImportValidator, ImportService
│   └── web/
│
└── ui/                              # controladores Thymeleaf (devuelven vistas)
    └── DashboardViewController, ModuleViewController…
```

### Reglas de dependencia

```
ui ──────────┐
web ─────────┼──→ service ──→ repository ──→ domain
report ──────┤        │
analytics ───┘        └──→ shared
```

- `domain` no depende de nada del framework salvo anotaciones JPA.
- `repository` solo conoce `domain`.
- `service` orquesta; es el único lugar con `@Transactional`.
- `web` solo traduce HTTP ↔ DTO; jamás contiene reglas de negocio.
- Un módulo puede llamar al `service` de otro, nunca a su `repository`.
- `analytics` lee de todos los módulos, pero ninguno depende de `analytics`.

## 3. Por qué un solo AnalyticsService

El requisito era explícito: dashboard y reportes nunca deben calcular con fórmulas distintas.

```
                  AnalyticsService
                         │
        ┌────────────────┼────────────────┐
        ▼                ▼                ▼
  DashboardController  ReportService   (futuro: API externa / IA)
```

Si el PDF y la pantalla muestran cifras diferentes, es porque alguien duplicó una consulta. Con una sola fuente eso no puede ocurrir.

## 4. Regla de evento único

Un accidente que incapacita **no se captura dos veces**. `AccidentService` crea el `Accident` y su `Disability` en la misma transacción:

```java
@Transactional
public AccidentResponse create(CreateAccidentRequest req, Long userId) {
    Accident accident = accidentRepository.save(mapper.toEntity(req));
    if (req.generaIncapacidad()) {
        Disability d = disabilityService.createFromAccident(accident, req.diasIncapacidad(), userId);
        accident.setDisability(d);
    }
    auditService.record(AuditAction.CREATE, "Accident", accident.getId(), null, accident, userId);
    return mapper.toResponse(accident);
}
```

Lo mismo aplica a maternidad. `DisabilityService.create()` público solo acepta `origenTipo = MANUAL`; los otros orígenes entran por métodos dedicados que solo el módulo dueño invoca.

## 5. Convenciones que evitan el desorden

| Regla | Detalle |
|---|---|
| Un archivo, una responsabilidad | Si una clase supera ~200 líneas, se divide |
| Nombres explícitos | `DisabilityService`, no `DisSvc` ni `Helper` |
| DTOs como `record` | Inmutables, sin Lombok, sin setters |
| Sin lógica en entidades JPA | Las entidades son datos; el comportamiento vive en servicios |
| Sin `Optional` como parámetro | Solo como retorno de repositorios |
| Constructor injection | Nunca `@Autowired` en campos |
| Excepciones tipadas | `ResourceNotFoundException`, `BusinessRuleException`, no `RuntimeException` genérica |
| Un `GlobalExceptionHandler` | Todo error HTTP sale del mismo lugar, con formato uniforme |
| Migraciones versionadas | `V1__catalogos.sql`, `V2__operativas.sql`… nunca `ddl-auto: update` |

## 6. Thymeleaf: fragmentos, no páginas monolíticas

El HTML de 104 KB se descompone:

```
templates/
├── layout/
│   ├── base.html               # <html>, <head>, estructura
│   ├── sidebar.html            # menú (fragmento)
│   └── topbar.html             # búsqueda y alertas (fragmento)
├── fragments/
│   ├── kpi-card.html           # th:fragment="kpi(color, icono, label, valor, delta)"
│   ├── panel.html              # th:fragment="panel(titulo, subtitulo, contenido)"
│   ├── data-table.html
│   └── modal-form.html
└── views/
    ├── dashboard.html
    ├── predio.html
    ├── atenciones.html
    └── …
static/
├── css/portal.css              # tokens y componentes
└── js/
    ├── charts.js               # configuración común de Chart.js
    └── modules/dashboard.js    # lógica por vista
```

Cada tarjeta de KPI se declara una vez y se invoca:

```html
<div th:replace="~{fragments/kpi-card :: kpi(
  color=${'#1D4ED8'}, icono='shield',
  label='Atenciones médicas', valor=${kpis.atenciones},
  delta=${kpis.variacionAtenciones})}"></div>
```

## 7. Perfiles y configuración

```
application.yml           # común
application-dev.yml       # PostgreSQL local en Docker, logs SQL visibles
application-test.yml      # Testcontainers
application-prod.yml      # variables de entorno, sin secretos en el repo
```

Ningún secreto en el repositorio. `JWT_SECRET`, `DB_PASSWORD` y demás llegan por variable de entorno.

## 8. Fases de construcción

| Fase | Contenido | Entregable verificable |
|---|---|---|
| 1 | Esqueleto, Docker Compose, Flyway con catálogos, seed | `mvn spring-boot:run` arranca y `/api/health` responde |
| 2 | Seguridad: usuarios, roles, permisos, JWT, alcance por predio | Login devuelve token; endpoint protegido rechaza sin permiso |
| 3 | Catálogos y empleados (CRUD completo) | 17 predios y 56 cuentas consultables por API |
| 4 | Morbilidad, incapacidades, accidentes, exámenes | Alta de accidente con incapacidad en una sola transacción |
| 5 | Antidoping, inventario, maternidad | Registrar prueba descuenta stock del lote |
| 6 | AnalyticsService y endpoints del dashboard | `/api/dashboard/summary` devuelve las cifras reales de 2026 |
| 7 | Vistas Thymeleaf conectadas a la API | Dashboard funcionando servido por Spring |
| 8 | Importador de Excel | Carga del .xlsm con previsualización y confirmación |
| 9 | Reportes PDF/Excel/CSV | Reporte ejecutivo generado desde AnalyticsService |
| 10 | Auditoría, pruebas de integración, Docker de producción | Bitácora completa y suite verde |

Cada fase deja el sistema funcionando. No se avanza con una fase a medias.
