# 02 — Arquitectura Propuesta

## 1. Principios rectores

Derivados directamente de las reglas de desarrollo obligatorias del proyecto:

- El Excel es fuente de reglas y catálogos, no de estructura de datos. Nada se modela por posición de celda.
- Cero hardcoding de predios, años, causas médicas o permisos — todo vive en catálogos administrables en base de datos.
- La lógica de negocio y de seguridad vive en el backend / base de datos, nunca solo en el frontend.
- El modelo soporta múltiples años de forma nativa (columna `anio` + `mes` en cada registro/periodo, jamás una tabla por año).
- Arquitectura preparada para integraciones futuras (Oracle, RH, Power BI, n8n, SSO/Entra) sin necesidad de rediseño.

## 2. Arquitectura lógica

```
Excel / Captura Web / Integraciones futuras
            │
            ▼
      Validación (Zod schemas compartidos)
            │
            ▼
      API (Next.js Route Handlers)
            │
            ▼
   Capa de negocio (services/ — domain logic)
            │
            ▼
   Base de datos (PostgreSQL vía Prisma)
            │
            ▼
   Capa analítica (agregaciones, detección de anomalías, insights)
            │
            ▼
      Dashboard (React Server/Client Components + Recharts)
            │
            ▼
      Reportes (PDF / Excel / CSV)
```

Cada flecha es un límite de capa: el frontend nunca llama a Prisma directamente, siempre pasa por `services/`, que valida permisos y reglas de negocio antes de tocar la base de datos.

## 3. Stack tecnológico

| Capa | Tecnología | Justificación |
|---|---|---|
| Frontend | Next.js 14+ (App Router), React, TypeScript | SSR para dashboards con datos sensibles, RSC reduce JS al cliente |
| UI | Tailwind CSS + shadcn/ui | Componentes accesibles, consistentes, sin apariencia de plantilla genérica |
| Gráficas | Recharts | Estable, declarativo, suficiente para KPIs/tendencias/rankings |
| Backend | Next.js Route Handlers (`app/api/**`) | Evita separar repos en el MVP; toda lógica sensible vive en `services/`, no en componentes — permite extraer a microservicio después sin reescribir reglas de negocio |
| Base de datos | PostgreSQL (vía Supabase) | Relacional robusto, soporta particionado futuro por año/predio, RLS disponible si se necesita defensa en profundidad |
| ORM | Prisma | Migraciones versionadas, tipado end-to-end, buen soporte de constraints/índices |
| Autenticación | Supabase Auth (email/password + preparado para SSO/OIDC) | Acelera MVP; el modelo de roles/permisos es propio (no depende de Supabase para RBAC) |
| Reportes | `@react-pdf/renderer` o Puppeteer (PDF), `exceljs` (Excel), nativo (CSV) | Generación server-side, no expone lógica de reporte al cliente |
| Importador | `exceljs` / `xlsx` (SheetJS) en el backend | Parsing server-side dentro de una transacción |

## 4. Arquitectura de carpetas

```
src/
  app/                    → rutas (páginas + API route handlers), solo presentación y wiring
  components/             → componentes de UI genéricos y reutilizables (KpiCard, TrendChart, etc.)
  features/               → módulos funcionales (morbilidad, incapacidades, accidentabilidad, ...)
    <feature>/
      components/         → UI específica del módulo
      hooks/               → hooks específicos del módulo
  domain/                 → entidades, tipos de dominio, reglas puras (sin dependencias de framework)
  services/                → casos de uso / lógica de negocio (orquesta repositories + domain + validations)
  repositories/            → acceso a datos vía Prisma, un repo por agregado
  analytics/               → cálculo de KPIs, comparativos, detección de anomalías, insights
  validations/              → esquemas Zod compartidos entre frontend y backend
  lib/                       → utilidades transversales (auth, logger, formatters)
  types/                      → tipos TypeScript compartidos
  hooks/                        → hooks genéricos (usePredioFilter, useExport, etc.)
```

Regla dura: **ningún componente React llama a Prisma o contiene reglas de negocio**. Un componente llama a un hook o a una función de `services/` expuesta vía API route; toda validación de permisos y de datos ocurre en `services/` y se repite en el backend aunque ya se haya validado en el formulario (la validación de frontend es UX, no seguridad).

## 5. Autenticación y autorización

- **Autenticación**: Supabase Auth con sesiones JWT en cookies httpOnly. Preparado para añadir proveedor SSO (Entra ID) sin cambiar el modelo de permisos.
- **Autorización (RBAC configurable)**: modelo `users` → `roles` → `permissions`, con una tabla adicional de **alcance por predio** (`user_predio_access`) para el rol "Gerencia de Predio". Cada request a un `service` verifica: (1) el usuario está autenticado, (2) el rol tiene el permiso requerido, (3) si el permiso está acotado por predio, el predio solicitado está en su alcance. Esta verificación vive en middleware de `services/`, nunca solo en la UI (un usuario sin permiso que llegue a la URL de la API recibe 403 aunque el botón esté oculto en el frontend).
- **Sensibilidad de datos**: cada tabla operativa (con datos potencialmente identificables de colaboradores) tiene un flag de sensibilidad a nivel de campo/vista; las vistas "ejecutivas" solo exponen agregados. Se implementa mediante vistas de base de datos separadas (`vw_*_agregado` de solo lectura) que el rol Dirección/Gerencia Salud consulta, mientras Salud Ocupacional accede a las tablas operativas completas.

## 6. Capa analítica

Vive en `src/analytics/`, separada de `services/` para poder evolucionar (hoy: reglas estadísticas determinísticas; mañana: modelos más sofisticados) sin tocar los casos de uso que la consumen:
- Cálculo de variaciones (mes vs mes anterior, YTD vs YTD, predio vs promedio corporativo).
- Generación de insights basados en umbrales configurables (ej. "variación > 15% respecto al mes anterior" → insight de texto).
- Detección de anomalías por reglas estadísticas explicables (ej. desviación estándar respecto al promedio móvil de 3 meses), no ML en el MVP.
- Todos los umbrales (semáforo de riesgo, alertas de caducidad, % de variación relevante) se leen de una tabla de configuración administrable, no están hardcodeados.

## 7. Flujo de datos: captura vs. importación vs. lectura

- **Captura manual**: formulario → validación Zod (cliente, UX) → API route → validación Zod (servidor, autoritativa) → `service` (valida permisos + reglas) → `repository` → Prisma → PostgreSQL, dentro de una transacción cuando hay escrituras relacionadas (ej. crear un caso de incapacidad y su registro de auditoría).
- **Importación Excel**: archivo → parser server-side → normalización a filas candidatas → validación por fila (formato, catálogos conocidos, duplicados) → preview al usuario → confirmación → escritura transaccional con rollback total si falla cualquier fila crítica → registro de auditoría de la importación completa.
- **Lectura para dashboards**: API de agregación (`/api/dashboard/*`) que ejecuta consultas agregadas en PostgreSQL (nunca trae filas crudas al cliente para agregarlas en JS) → responde JSON ya resumido → el cliente solo renderiza.

## 8. Manejo de errores y estados

Cada pantalla contempla explícitamente `loading | empty | error | success | unauthorized` mediante componentes genéricos (`<LoadingState />`, `<EmptyState />`, `<ErrorState />`) reutilizados en todos los módulos — nunca una pantalla en blanco sin explicación.

## 9. Estrategia de despliegue

- **Entorno**: Vercel (frontend + API routes) + Supabase (PostgreSQL + Auth + Storage para adjuntos) para el MVP — despliegue rápido, HTTPS por defecto, preview deployments por PR.
- **Migraciones**: Prisma Migrate, versionadas en el repo, aplicadas en CI antes del despliegue a producción.
- **Entornos**: `local` (Docker Postgres) → `staging` (proyecto Supabase separado) → `production`. Nunca se prueba una migración directo en producción.
- **Variables sensibles**: gestionadas como secretos de entorno (Vercel/Supabase), nunca en el repo.
- **Preparado para migrar a infraestructura propia** (self-hosted Postgres, contenedor Docker del backend) si el volumen o requisitos de cumplimiento lo exigen más adelante — no hay acoplamiento fuerte a servicios propietarios de Supabase más allá de Auth/Storage, que tienen alternativas estándar (NextAuth + S3-compatible) si se requiere migrar.

## 10. Preparación para integraciones futuras (no se implementan en el MVP)

- **API REST versionada** (`/api/v1/...`) para que Power BI o n8n consuman datos agregados sin acoplarse a la UI.
- **Modelo de `employees` desacoplado** de RH: hoy captura mínima (nombre/identificador/predio/puesto), preparado para sincronizarse desde un ERP/RH vía un job de integración futuro sin cambiar el esquema de las tablas de hechos (que referencian `employee_id`, no datos de RH embebidos).
- **Autenticación preparada para SSO/Entra ID** vía el mismo proveedor de Auth (OIDC), sin cambiar el modelo de roles.
- **Catálogo de predios/cuentas con campo `codigo_externo`** opcional, para mapear contra IDs de Oracle/ERP cuando exista esa integración.
