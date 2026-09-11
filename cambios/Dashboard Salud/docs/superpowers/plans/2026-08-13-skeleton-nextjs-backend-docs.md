# Skeleton Next.js + Documentación de Backend — Plan de Implementación

> **Para ejecutores agénticos:** SUB-SKILL REQUERIDA: usar superpowers:subagent-driven-development (recomendado) o superpowers:executing-plans para implementar este plan tarea por tarea. Los pasos usan sintaxis de checkbox (`- [ ]`) para seguimiento.

**Objetivo:** Dejar un proyecto Next.js real y funcional (Fase 5 del plan maestro: skeleton) más un set completo de documentación de backend, para que un segundo desarrollador pueda construir el backend (API routes, Prisma, auth, servicios) sin más contexto que estos documentos y el código ya andando.

**Arquitectura:** Next.js 14 App Router + TypeScript + Tailwind + shadcn/ui en el frontend; capas `domain/services/repositories/analytics` para lógica de negocio; PostgreSQL vía Prisma como única fuente de verdad; autenticación propia (JWT + bcrypt, sin librerías de terceros) para no depender de ningún proveedor SaaS, ya que el despliegue será en servidor propio.

**Tech Stack:** Next.js 14, React 18, TypeScript, Tailwind CSS, shadcn/ui, Recharts, Prisma, PostgreSQL, bcrypt, jsonwebtoken, Zod, Docker Compose.

## Global Constraints

- Node.js v22.x, npm (ya verificados disponibles en el entorno).
- Sin `any` en TypeScript salvo justificación explícita en comentario.
- Ningún predio/año/causa médica/permiso hardcodeado — todo catálogo vive en base de datos, sembrado (seed) desde los valores reales extraídos en `docs/01_ANALISIS_ACTUAL.md`.
- Toda tabla operativa incluye `created_at`, `updated_at`, `created_by`, `updated_by`, y `deleted_at` (soft delete) salvo catálogos verdaderamente inmutables.
- El modelo de datos debe corresponder exactamente al descrito en `docs/03_MODELO_DATOS.md` (incluida la regla de evento único `origen_tipo`/`origen_id` en `disabilities`).
- Ningún commit de credenciales reales; usar `.env.example` con placeholders.
- Sin Supabase ni ningún SaaS de auth/DB — todo debe funcionar contra un PostgreSQL propio vía `DATABASE_URL`.

---

## File Structure

```
package.json, tsconfig.json, next.config.mjs, tailwind.config.ts, postcss.config.mjs, .eslintrc.json
.env.example
docker-compose.yml
prisma/
  schema.prisma
  seed.ts
src/
  app/
    layout.tsx
    page.tsx                        → redirige a /dashboard
    globals.css
    (auth)/login/page.tsx
    dashboard/page.tsx
    api/
      health/route.ts
  components/
    ui/                              → shadcn/ui (generado por su CLI)
  lib/
    prisma.ts                        → cliente Prisma singleton
    auth/
      hash.ts                        → bcrypt helpers
      jwt.ts                         → firmar/verificar JWT
      session.ts                     → leer sesión desde cookie en server components/route handlers
  types/
    next-auth.d.ts (si aplica) — omitido, auth propia
docs/
  API.md
  DATA_DICTIONARY.md
  SECURITY.md
  ARCHITECTURE.md (actualiza 02_ARQUITECTURA_PROPUESTA.md con detalles de conexión)
  BACKEND_HANDOFF.md                → documento maestro de bienvenida para el desarrollador de backend
```

**Interfaces entre tareas:**
- Tarea 1 produce el proyecto Next.js base (`package.json`, estructura `src/app`) que todas las tareas siguientes asumen que existe.
- Tarea 2 produce `prisma/schema.prisma` — fuente de verdad de tipos que Tarea 3 (seed) y Tarea 6 (API.md) referencian por nombre exacto de modelo/campo.
- Tarea 4 produce `src/lib/auth/*` — funciones `hashPassword(plain): Promise<string>`, `verifyPassword(plain, hash): Promise<boolean>`, `signSessionToken(payload): string`, `verifySessionToken(token): SessionPayload | null` que Tarea 5 (rutas de auth) consume.

---

### Task 1: Inicializar proyecto Next.js con TypeScript, Tailwind y shadcn/ui

**Files:**
- Create: proyecto completo vía scaffolding (`package.json`, `tsconfig.json`, `next.config.mjs`, `tailwind.config.ts`, `src/app/layout.tsx`, `src/app/page.tsx`, `src/app/globals.css`)
- Create: `.gitignore`, `.env.example`

**Interfaces:**
- Produces: proyecto ejecutable con `npm run dev` sirviendo en `localhost:3000`, alias de import `@/*` → `src/*` configurado en `tsconfig.json`.

- [ ] **Step 1: Crear el proyecto Next.js**

```bash
cd "C:/Users/josea/OneDrive/Escritorio/Dashboard Salud"
npx create-next-app@latest . --typescript --tailwind --eslint --app --src-dir --import-alias "@/*" --no-turbopack --use-npm
```
Cuando pregunte si el directorio no está vacío (ya existe `docs/` y `.claude/`), confirmar que sí se debe continuar en el directorio actual.

- [ ] **Step 2: Verificar que el proyecto corre**

Run: `npm run dev` (en background, o revisar con curl tras unos segundos)
Expected: servidor escuchando en `http://localhost:3000`, página por defecto de Next.js visible.

Detener el servidor de dev antes de continuar.

- [ ] **Step 3: Inicializar shadcn/ui**

```bash
npx shadcn@latest init -d
```
Esto crea `components.json` y `src/lib/utils.ts`. Usar los valores por defecto (estilo "new-york" o "default", color base "slate" — se ajustará después con los tokens de la skill ui-ux-pro-max en una tarea posterior de theming, fuera de este plan).

- [ ] **Step 4: Agregar componentes base de shadcn que se usarán en todo el proyecto**

```bash
npx shadcn@latest add button card table select badge input label separator sheet dropdown-menu avatar
```

- [ ] **Step 5: Crear `.env.example`**

Contenido de `.env.example`:
```
# PostgreSQL — servidor propio, no Supabase
DATABASE_URL="postgresql://usuario:password@localhost:5432/salud_ocupacional?schema=public"

# JWT / sesiones (auth propia)
JWT_SECRET="reemplazar-por-un-secreto-largo-y-aleatorio-en-produccion"
JWT_EXPIRES_IN="7d"

# App
NEXT_PUBLIC_APP_URL="http://localhost:3000"
```

- [ ] **Step 6: Commit**

```bash
git init
git add -A
git commit -m "chore: scaffold Next.js project with TypeScript, Tailwind, shadcn/ui"
```

---

### Task 2: Schema Prisma completo (25 modelos según `docs/03_MODELO_DATOS.md`)

**Files:**
- Create: `prisma/schema.prisma`
- Modify: `.env.example` (ya tiene `DATABASE_URL`, no requiere cambio)

**Interfaces:**
- Consumes: nombres de catálogos y entidades exactamente como se documentaron en `docs/03_MODELO_DATOS.md` §3-§7.
- Produces: modelos Prisma `Predio`, `PredioAlias`, `Cuenta`, `Agencia`, `Area`, `Puesto`, `AttentionCause`, `InjuryType`, `MedicalExamType`, `MedicalExamResult`, `DisabilityType`, `AccidentType`, `AccidentCause`, `AccidentStatus`, `DrugTestType`, `DrugTestResult`, `DrugTestStatus`, `AgeRange`, `RiskThreshold`, `Employee`, `MedicalAttention`, `MedicalExam`, `Disability`, `DisabilityCost`, `Accident`, `DrugTest`, `DrugTestBatch`, `DrugTestInventoryMovement`, `MaternityCase`, `Period`, `User`, `Role`, `Permission`, `RolePermission`, `UserPredioAccess`, `AuditLog`, `Attachment`, `ImportBatch`, `ImportBatchRow` que Task 3 (seed) y `docs/API.md`/`docs/DATA_DICTIONARY.md` referencian por estos nombres exactos.

- [ ] **Step 1: Instalar Prisma**

```bash
npm install prisma --save-dev
npm install @prisma/client
npx prisma init --datasource-provider postgresql
```
Esto sobrescribe `prisma/schema.prisma` con un template y crea `.env` (que debe permanecer en `.gitignore`, ya generado por `create-next-app`).

- [ ] **Step 2: Escribir el schema completo**

Reemplazar el contenido de `prisma/schema.prisma`:

```prisma
generator client {
  provider = "prisma-client-js"
}

datasource db {
  provider = "postgresql"
  url      = env("DATABASE_URL")
}

// ============ CATÁLOGOS MAESTROS ============

model Predio {
  id        Int      @id @default(autoincrement())
  codigo    String?  @unique
  nombre    String   @unique
  activo    Boolean  @default(true)
  orden     Int      @default(0)
  createdAt DateTime @default(now()) @map("created_at")
  updatedAt DateTime @updatedAt @map("updated_at")

  aliases         PredioAlias[]
  employees       Employee[]
  attentions      MedicalAttention[]
  exams           MedicalExam[]
  disabilities    Disability[]
  accidents       Accident[]
  drugTests       DrugTest[]
  drugTestBatches DrugTestBatch[]
  maternityCases  MaternityCase[]
  userAccess      UserPredioAccess[]

  @@map("predios")
}

model PredioAlias {
  id       Int    @id @default(autoincrement())
  predioId Int    @map("predio_id")
  alias    String

  predio Predio @relation(fields: [predioId], references: [id], onDelete: Cascade)

  @@unique([predioId, alias])
  @@map("predio_aliases")
}

model Cuenta {
  id        Int      @id @default(autoincrement())
  codigo    String?  @unique
  nombre    String   @unique
  activo    Boolean  @default(true)
  orden     Int      @default(0)
  createdAt DateTime @default(now()) @map("created_at")
  updatedAt DateTime @updatedAt @map("updated_at")

  employees      Employee[]
  attentions     MedicalAttention[]
  exams          MedicalExam[]
  disabilities   Disability[]
  accidents      Accident[]
  drugTests      DrugTest[]
  maternityCases MaternityCase[]

  @@map("cuentas")
}

model Agencia {
  id        Int      @id @default(autoincrement())
  codigo    String?  @unique
  nombre    String   @unique
  activo    Boolean  @default(true)
  orden     Int      @default(0)
  createdAt DateTime @default(now()) @map("created_at")
  updatedAt DateTime @updatedAt @map("updated_at")

  drugTests DrugTest[]

  @@map("agencias")
}

model Area {
  id        Int      @id @default(autoincrement())
  codigo    String?  @unique
  nombre    String   @unique
  activo    Boolean  @default(true)
  orden     Int      @default(0)
  createdAt DateTime @default(now()) @map("created_at")
  updatedAt DateTime @updatedAt @map("updated_at")

  employees    Employee[]
  disabilities Disability[]
  accidents    Accident[]
  drugTests    DrugTest[]

  @@map("areas")
}

model Puesto {
  id        Int      @id @default(autoincrement())
  codigo    String?  @unique
  nombre    String   @unique
  activo    Boolean  @default(true)
  orden     Int      @default(0)
  createdAt DateTime @default(now()) @map("created_at")
  updatedAt DateTime @updatedAt @map("updated_at")

  employees Employee[]
  accidents Accident[]
  drugTests DrugTest[]

  @@map("puestos")
}

model AttentionCause {
  id        Int      @id @default(autoincrement())
  codigo    String?  @unique
  nombre    String   @unique
  categoria String?
  activo    Boolean  @default(true)
  orden     Int      @default(0)
  createdAt DateTime @default(now()) @map("created_at")
  updatedAt DateTime @updatedAt @map("updated_at")

  attentions MedicalAttention[]

  @@map("attention_causes")
}

model InjuryType {
  id            Int      @id @default(autoincrement())
  codigo        String?  @unique
  nombre        String   @unique
  regionCorporal String? @map("region_corporal")
  activo        Boolean  @default(true)
  orden         Int      @default(0)
  createdAt     DateTime @default(now()) @map("created_at")
  updatedAt     DateTime @updatedAt @map("updated_at")

  attentions MedicalAttention[]

  @@map("injury_types")
}

model MedicalExamType {
  id        Int      @id @default(autoincrement())
  codigo    String?  @unique
  nombre    String   @unique
  activo    Boolean  @default(true)
  orden     Int      @default(0)
  createdAt DateTime @default(now()) @map("created_at")
  updatedAt DateTime @updatedAt @map("updated_at")

  exams MedicalExam[]

  @@map("medical_exam_types")
}

model MedicalExamResult {
  id        Int      @id @default(autoincrement())
  codigo    String?  @unique
  nombre    String   @unique
  activo    Boolean  @default(true)
  orden     Int      @default(0)
  createdAt DateTime @default(now()) @map("created_at")
  updatedAt DateTime @updatedAt @map("updated_at")

  exams MedicalExam[]

  @@map("medical_exam_results")
}

model DisabilityType {
  id        Int      @id @default(autoincrement())
  codigo    String?  @unique
  nombre    String   @unique
  activo    Boolean  @default(true)
  orden     Int      @default(0)
  createdAt DateTime @default(now()) @map("created_at")
  updatedAt DateTime @updatedAt @map("updated_at")

  disabilities Disability[]

  @@map("disability_types")
}

model AccidentType {
  id        Int      @id @default(autoincrement())
  codigo    String?  @unique
  nombre    String   @unique
  activo    Boolean  @default(true)
  orden     Int      @default(0)
  createdAt DateTime @default(now()) @map("created_at")
  updatedAt DateTime @updatedAt @map("updated_at")

  accidents Accident[]

  @@map("accident_types")
}

model AccidentCause {
  id        Int      @id @default(autoincrement())
  codigo    String?  @unique
  nombre    String   @unique
  activo    Boolean  @default(true)
  orden     Int      @default(0)
  createdAt DateTime @default(now()) @map("created_at")
  updatedAt DateTime @updatedAt @map("updated_at")

  accidents Accident[]

  @@map("accident_causes")
}

model AccidentStatus {
  id        Int      @id @default(autoincrement())
  codigo    String?  @unique
  nombre    String   @unique
  activo    Boolean  @default(true)
  orden     Int      @default(0)
  createdAt DateTime @default(now()) @map("created_at")
  updatedAt DateTime @updatedAt @map("updated_at")

  accidents Accident[]

  @@map("accident_statuses")
}

model DrugTestType {
  id        Int      @id @default(autoincrement())
  codigo    String?  @unique
  nombre    String   @unique
  activo    Boolean  @default(true)
  orden     Int      @default(0)
  createdAt DateTime @default(now()) @map("created_at")
  updatedAt DateTime @updatedAt @map("updated_at")

  drugTests DrugTest[]

  @@map("drug_test_types")
}

model DrugTestResult {
  id        Int      @id @default(autoincrement())
  codigo    String?  @unique
  nombre    String   @unique
  activo    Boolean  @default(true)
  orden     Int      @default(0)
  createdAt DateTime @default(now()) @map("created_at")
  updatedAt DateTime @updatedAt @map("updated_at")

  drugTests DrugTest[]

  @@map("drug_test_results")
}

model DrugTestStatus {
  id        Int      @id @default(autoincrement())
  codigo    String?  @unique
  nombre    String   @unique
  activo    Boolean  @default(true)
  orden     Int      @default(0)
  createdAt DateTime @default(now()) @map("created_at")
  updatedAt DateTime @updatedAt @map("updated_at")

  drugTests DrugTest[]

  @@map("drug_test_statuses")
}

model AgeRange {
  id       Int    @id @default(autoincrement())
  nombre   String @unique
  edadMin  Int    @map("edad_min")
  edadMax  Int?   @map("edad_max")
  orden    Int    @default(0)

  @@map("age_ranges")
}

model RiskThreshold {
  id          Int      @id @default(autoincrement())
  clave       String   @unique // ej: "morbilidad_predio_medio", "antidoping_caducidad_dias_30"
  descripcion String
  valorNumero Decimal? @map("valor_numero")
  createdAt   DateTime @default(now()) @map("created_at")
  updatedAt   DateTime @updatedAt @map("updated_at")

  @@map("risk_thresholds")
}

// ============ ENTIDADES OPERATIVAS ============

model Employee {
  id              Int       @id @default(autoincrement())
  predioId        Int       @map("predio_id")
  numeroEmpleado  String?   @unique @map("numero_empleado")
  nombre          String
  genero          String?
  fechaNacimiento DateTime? @map("fecha_nacimiento") @db.Date
  puestoId        Int?      @map("puesto_id")
  areaId          Int?      @map("area_id")
  cuentaId        Int?      @map("cuenta_id")
  agenciaId       Int?      @map("agencia_id")
  fechaIngreso    DateTime? @map("fecha_ingreso") @db.Date
  activo          Boolean   @default(true)
  codigoExternoRh String?   @map("codigo_externo_rh")
  createdAt       DateTime  @default(now()) @map("created_at")
  updatedAt       DateTime  @updatedAt @map("updated_at")
  deletedAt       DateTime? @map("deleted_at")

  predio Predio  @relation(fields: [predioId], references: [id], onDelete: Restrict)
  puesto Puesto? @relation(fields: [puestoId], references: [id], onDelete: SetNull)
  area   Area?   @relation(fields: [areaId], references: [id], onDelete: SetNull)
  cuenta Cuenta? @relation(fields: [cuentaId], references: [id], onDelete: SetNull)

  attentions     MedicalAttention[]
  exams          MedicalExam[]
  disabilities   Disability[]
  accidents      Accident[]
  drugTests      DrugTest[]
  maternityCases MaternityCase[]

  @@index([predioId])
  @@map("employees")
}

model Period {
  id      Int     @id @default(autoincrement())
  anio    Int
  mes     Int // 1-12
  cerrado Boolean @default(false)
  fechaCierre DateTime? @map("fecha_cierre")

  attentions     MedicalAttention[]
  exams          MedicalExam[]
  disabilities   Disability[]
  accidents      Accident[]
  drugTests      DrugTest[]
  maternityCases MaternityCase[]

  @@unique([anio, mes])
  @@map("periods")
}

model MedicalAttention {
  id                Int       @id @default(autoincrement())
  employeeId        Int?      @map("employee_id")
  predioId          Int       @map("predio_id")
  cuentaId          Int?      @map("cuenta_id")
  periodId          Int       @map("period_id")
  fechaAtencion     DateTime  @map("fecha_atencion") @db.Date
  attentionCauseId  Int?      @map("attention_cause_id")
  injuryTypeId      Int?      @map("injury_type_id")
  esPersonalInclusion Boolean @default(false) @map("es_personal_inclusion")
  observaciones     String?
  createdAt         DateTime  @default(now()) @map("created_at")
  updatedAt         DateTime  @updatedAt @map("updated_at")
  createdBy         Int?      @map("created_by")
  updatedBy         Int?      @map("updated_by")
  deletedAt         DateTime? @map("deleted_at")

  employee       Employee?       @relation(fields: [employeeId], references: [id], onDelete: SetNull)
  predio         Predio          @relation(fields: [predioId], references: [id], onDelete: Restrict)
  cuenta         Cuenta?         @relation(fields: [cuentaId], references: [id], onDelete: SetNull)
  period         Period          @relation(fields: [periodId], references: [id], onDelete: Restrict)
  attentionCause AttentionCause? @relation(fields: [attentionCauseId], references: [id], onDelete: SetNull)
  injuryType     InjuryType?     @relation(fields: [injuryTypeId], references: [id], onDelete: SetNull)

  @@index([predioId, periodId])
  @@map("medical_attentions")
}

model MedicalExam {
  id                  Int       @id @default(autoincrement())
  employeeId          Int       @map("employee_id")
  predioId            Int       @map("predio_id")
  cuentaId            Int?      @map("cuenta_id")
  periodId            Int       @map("period_id")
  fechaExamen         DateTime  @map("fecha_examen") @db.Date
  medicalExamTypeId   Int       @map("medical_exam_type_id")
  medicalExamResultId Int       @map("medical_exam_result_id")
  observaciones       String?
  createdAt           DateTime  @default(now()) @map("created_at")
  updatedAt           DateTime  @updatedAt @map("updated_at")
  createdBy           Int?      @map("created_by")
  updatedBy           Int?      @map("updated_by")
  deletedAt           DateTime? @map("deleted_at")

  employee   Employee           @relation(fields: [employeeId], references: [id], onDelete: Restrict)
  predio     Predio             @relation(fields: [predioId], references: [id], onDelete: Restrict)
  cuenta     Cuenta?            @relation(fields: [cuentaId], references: [id], onDelete: SetNull)
  period     Period             @relation(fields: [periodId], references: [id], onDelete: Restrict)
  examType   MedicalExamType    @relation(fields: [medicalExamTypeId], references: [id], onDelete: Restrict)
  examResult MedicalExamResult  @relation(fields: [medicalExamResultId], references: [id], onDelete: Restrict)

  @@index([predioId, periodId])
  @@map("medical_exams")
}

model Disability {
  id                  Int       @id @default(autoincrement())
  employeeId          Int       @map("employee_id")
  predioId            Int       @map("predio_id")
  cuentaId            Int?      @map("cuenta_id")
  areaId              Int?      @map("area_id")
  periodId            Int       @map("period_id")
  disabilityTypeId    Int       @map("disability_type_id")
  fechaInicio         DateTime  @map("fecha_inicio") @db.Date
  fechaFin            DateTime? @map("fecha_fin") @db.Date
  diasIncapacidad     Int       @map("dias_incapacidad")
  folioImss           String?   @map("folio_imss")
  esInterna           Boolean   @default(false) @map("es_interna")
  origenTipo          String    @default("manual") @map("origen_tipo") // manual | accidente | maternidad
  origenId             Int?      @map("origen_id")
  createdAt           DateTime  @default(now()) @map("created_at")
  updatedAt           DateTime  @updatedAt @map("updated_at")
  createdBy           Int?      @map("created_by")
  updatedBy           Int?      @map("updated_by")
  deletedAt           DateTime? @map("deleted_at")

  employee       Employee       @relation(fields: [employeeId], references: [id], onDelete: Restrict)
  predio         Predio         @relation(fields: [predioId], references: [id], onDelete: Restrict)
  cuenta         Cuenta?        @relation(fields: [cuentaId], references: [id], onDelete: SetNull)
  area           Area?          @relation(fields: [areaId], references: [id], onDelete: SetNull)
  period         Period         @relation(fields: [periodId], references: [id], onDelete: Restrict)
  disabilityType DisabilityType @relation(fields: [disabilityTypeId], references: [id], onDelete: Restrict)
  cost           DisabilityCost?
  accidentOrigin Accident?      @relation("AccidentToDisability")
  maternityOrigin MaternityCase? @relation("MaternityToDisability")

  @@index([predioId, periodId])
  @@check(diasIncapacidad >= 0, name: "dias_incapacidad_no_negativo")
  @@map("disabilities")
}

model DisabilityCost {
  id           Int      @id @default(autoincrement())
  disabilityId Int      @unique @map("disability_id")
  monto        Decimal
  moneda       String   @default("MXN")
  fechaRegistro DateTime @default(now()) @map("fecha_registro")

  disability Disability @relation(fields: [disabilityId], references: [id], onDelete: Cascade)

  @@map("disability_costs")
}

model Accident {
  id                Int       @id @default(autoincrement())
  employeeId        Int       @map("employee_id")
  predioId          Int       @map("predio_id")
  cuentaId          Int?      @map("cuenta_id")
  areaId            Int?      @map("area_id")
  puestoId          Int?      @map("puesto_id")
  periodId          Int       @map("period_id")
  fechaAccidente    DateTime  @map("fecha_accidente") @db.Date
  accidentTypeId    Int       @map("accident_type_id")
  accidentCauseId   Int       @map("accident_cause_id")
  accidentStatusId  Int       @map("accident_status_id")
  genero            String?
  generaIncapacidad Boolean   @default(false) @map("genera_incapacidad")
  diasPerdidos      Int?      @map("dias_perdidos")
  costoCalificado   Decimal?  @map("costo_calificado")
  costoImprocedente Decimal?  @map("costo_improcedente")
  disabilityId      Int?      @unique @map("disability_id")
  createdAt         DateTime  @default(now()) @map("created_at")
  updatedAt         DateTime  @updatedAt @map("updated_at")
  createdBy         Int?      @map("created_by")
  updatedBy         Int?      @map("updated_by")
  deletedAt         DateTime? @map("deleted_at")

  employee       Employee       @relation(fields: [employeeId], references: [id], onDelete: Restrict)
  predio         Predio         @relation(fields: [predioId], references: [id], onDelete: Restrict)
  cuenta         Cuenta?        @relation(fields: [cuentaId], references: [id], onDelete: SetNull)
  area           Area?          @relation(fields: [areaId], references: [id], onDelete: SetNull)
  puesto         Puesto?        @relation(fields: [puestoId], references: [id], onDelete: SetNull)
  period         Period         @relation(fields: [periodId], references: [id], onDelete: Restrict)
  accidentType   AccidentType   @relation(fields: [accidentTypeId], references: [id], onDelete: Restrict)
  accidentCause  AccidentCause  @relation(fields: [accidentCauseId], references: [id], onDelete: Restrict)
  accidentStatus AccidentStatus @relation(fields: [accidentStatusId], references: [id], onDelete: Restrict)
  disability     Disability?    @relation("AccidentToDisability", fields: [disabilityId], references: [id], onDelete: SetNull)

  @@index([predioId, periodId])
  @@map("accidents")
}

model DrugTest {
  id                Int       @id @default(autoincrement())
  employeeId        Int       @map("employee_id")
  predioId          Int       @map("predio_id")
  cuentaId          Int?      @map("cuenta_id")
  areaId            Int?      @map("area_id")
  puestoId          Int?      @map("puesto_id")
  agenciaId         Int?      @map("agencia_id")
  periodId          Int       @map("period_id")
  fechaPrueba       DateTime  @map("fecha_prueba") @db.Date
  drugTestTypeId    Int       @map("drug_test_type_id")
  drugTestResultId  Int?      @map("drug_test_result_id")
  drugTestStatusId  Int?      @map("drug_test_status_id")
  batchId           Int?      @map("batch_id")
  createdAt         DateTime  @default(now()) @map("created_at")
  updatedAt         DateTime  @updatedAt @map("updated_at")
  createdBy         Int?      @map("created_by")
  updatedBy         Int?      @map("updated_by")
  deletedAt         DateTime? @map("deleted_at")

  employee   Employee        @relation(fields: [employeeId], references: [id], onDelete: Restrict)
  predio     Predio          @relation(fields: [predioId], references: [id], onDelete: Restrict)
  cuenta     Cuenta?         @relation(fields: [cuentaId], references: [id], onDelete: SetNull)
  area       Area?           @relation(fields: [areaId], references: [id], onDelete: SetNull)
  puesto     Puesto?         @relation(fields: [puestoId], references: [id], onDelete: SetNull)
  agencia    Agencia?        @relation(fields: [agenciaId], references: [id], onDelete: SetNull)
  period     Period          @relation(fields: [periodId], references: [id], onDelete: Restrict)
  testType   DrugTestType    @relation(fields: [drugTestTypeId], references: [id], onDelete: Restrict)
  result     DrugTestResult? @relation(fields: [drugTestResultId], references: [id], onDelete: SetNull)
  status     DrugTestStatus? @relation(fields: [drugTestStatusId], references: [id], onDelete: SetNull)
  batch      DrugTestBatch?  @relation(fields: [batchId], references: [id], onDelete: SetNull)

  @@index([predioId, periodId])
  @@map("drug_tests")
}

model DrugTestBatch {
  id               Int      @id @default(autoincrement())
  lote             String   @unique
  fechaCaducidad   DateTime @map("fecha_caducidad") @db.Date
  cantidadInicial  Int      @map("cantidad_inicial")
  predioId         Int      @map("predio_id")
  activo           Boolean  @default(true)
  createdAt        DateTime @default(now()) @map("created_at")
  updatedAt        DateTime @updatedAt @map("updated_at")

  predio    Predio                      @relation(fields: [predioId], references: [id], onDelete: Restrict)
  movements DrugTestInventoryMovement[]
  drugTests DrugTest[]

  @@map("drug_test_batches")
}

model DrugTestInventoryMovement {
  id          Int      @id @default(autoincrement())
  batchId     Int      @map("batch_id")
  tipo        String   // entrada | consumo
  cantidad    Int
  fecha       DateTime @db.Date
  predioId    Int      @map("predio_id")
  referencia  String?
  createdAt   DateTime @default(now()) @map("created_at")
  createdBy   Int?     @map("created_by")

  batch DrugTestBatch @relation(fields: [batchId], references: [id], onDelete: Restrict)

  @@map("drug_test_inventory_movements")
}

model MaternityCase {
  id                    Int       @id @default(autoincrement())
  employeeId            Int       @map("employee_id")
  predioId              Int       @map("predio_id")
  cuentaId              Int?      @map("cuenta_id")
  periodId              Int       @map("period_id")
  fechaInicioIncapacidad DateTime @map("fecha_inicio_incapacidad") @db.Date
  fechaProbableParto    DateTime? @map("fecha_probable_parto") @db.Date
  diasIncapacidad       Int       @map("dias_incapacidad")
  estatus               String?
  disabilityId          Int?      @unique @map("disability_id")
  createdAt             DateTime  @default(now()) @map("created_at")
  updatedAt             DateTime  @updatedAt @map("updated_at")
  deletedAt             DateTime? @map("deleted_at")

  employee   Employee    @relation(fields: [employeeId], references: [id], onDelete: Restrict)
  predio     Predio      @relation(fields: [predioId], references: [id], onDelete: Restrict)
  cuenta     Cuenta?     @relation(fields: [cuentaId], references: [id], onDelete: SetNull)
  period     Period      @relation(fields: [periodId], references: [id], onDelete: Restrict)
  disability Disability? @relation("MaternityToDisability", fields: [disabilityId], references: [id], onDelete: SetNull)

  @@map("maternity_cases")
}

// ============ USUARIOS, ROLES, PERMISOS ============

model User {
  id            Int       @id @default(autoincrement())
  email         String    @unique
  passwordHash  String    @map("password_hash")
  nombre        String
  roleId        Int       @map("role_id")
  activo        Boolean   @default(true)
  ultimoAcceso  DateTime? @map("ultimo_acceso")
  createdAt     DateTime  @default(now()) @map("created_at")
  updatedAt     DateTime  @updatedAt @map("updated_at")
  deletedAt     DateTime? @map("deleted_at")

  role         Role               @relation(fields: [roleId], references: [id], onDelete: Restrict)
  predioAccess UserPredioAccess[]
  auditLogs    AuditLog[]

  @@map("users")
}

model Role {
  id          Int      @id @default(autoincrement())
  nombre      String   @unique // Administrador, Salud Ocupacional, Gerencia Salud, Gerencia de Predio, Dirección, Consulta
  descripcion String?

  users       User[]
  permissions RolePermission[]

  @@map("roles")
}

model Permission {
  id               Int    @id @default(autoincrement())
  codigo           String @unique // ej: "disabilities.write", "reports.export"
  descripcion      String?
  nivelSensibilidad String @default("operativo") @map("nivel_sensibilidad") // operativo | agregado

  roles RolePermission[]

  @@map("permissions")
}

model RolePermission {
  roleId       Int @map("role_id")
  permissionId Int @map("permission_id")

  role       Role       @relation(fields: [roleId], references: [id], onDelete: Cascade)
  permission Permission @relation(fields: [permissionId], references: [id], onDelete: Cascade)

  @@id([roleId, permissionId])
  @@map("role_permissions")
}

model UserPredioAccess {
  userId   Int @map("user_id")
  predioId Int @map("predio_id")

  user   User   @relation(fields: [userId], references: [id], onDelete: Cascade)
  predio Predio @relation(fields: [predioId], references: [id], onDelete: Cascade)

  @@id([userId, predioId])
  @@map("user_predio_access")
}

// ============ TRANSVERSALES ============

model AuditLog {
  id             Int      @id @default(autoincrement())
  userId         Int?     @map("user_id")
  accion         String   // create | update | delete | export | import | login
  modulo         String
  entidad        String
  entidadId      Int?     @map("entidad_id")
  valorAnterior  Json?    @map("valor_anterior")
  valorNuevo     Json?    @map("valor_nuevo")
  contieneDatosSensibles Boolean @default(false) @map("contiene_datos_sensibles")
  ipAddress      String?  @map("ip_address")
  fechaHora      DateTime @default(now()) @map("fecha_hora")

  user User? @relation(fields: [userId], references: [id], onDelete: SetNull)

  @@index([entidad, entidadId])
  @@index([userId])
  @@map("audit_logs")
}

model Attachment {
  id           Int      @id @default(autoincrement())
  entidad      String
  entidadId    Int      @map("entidad_id")
  nombreArchivo String  @map("nombre_archivo")
  urlStorage   String   @map("url_storage")
  tipoMime     String   @map("tipo_mime")
  tamanoBytes  Int      @map("tamano_bytes")
  subidoPor    Int?     @map("subido_por")
  createdAt    DateTime @default(now()) @map("created_at")

  @@index([entidad, entidadId])
  @@map("attachments")
}

model ImportBatch {
  id                 Int      @id @default(autoincrement())
  archivoNombre      String   @map("archivo_nombre")
  usuarioId          Int      @map("usuario_id")
  fechaImportacion   DateTime @default(now()) @map("fecha_importacion")
  estado             String   @default("preview") // preview | confirmado | rechazado
  registrosTotales   Int      @default(0) @map("registros_totales")
  registrosCorrectos Int      @default(0) @map("registros_correctos")
  registrosAdvertencia Int    @default(0) @map("registros_advertencia")
  registrosRechazados Int     @default(0) @map("registros_rechazados")
  detalleErrores     Json?    @map("detalle_errores")

  rows ImportBatchRow[]

  @@map("import_batches")
}

model ImportBatchRow {
  id             Int    @id @default(autoincrement())
  importBatchId  Int    @map("import_batch_id")
  filaOrigen     Int    @map("fila_origen")
  entidadDestino String @map("entidad_destino")
  payload        Json
  estado         String @default("pendiente") // pendiente | importado | rechazado | duplicado
  motivoRechazo  String? @map("motivo_rechazo")

  importBatch ImportBatch @relation(fields: [importBatchId], references: [id], onDelete: Cascade)

  @@map("import_batch_rows")
}
```

- [ ] **Step 3: Formatear y validar el schema**

```bash
npx prisma format
npx prisma validate
```
Expected: "The schema at prisma/schema.prisma is valid 🚀" — si hay error de relación ambigua o tipo, corregirlo antes de continuar (los errores de Prisma indican la línea exacta).

- [ ] **Step 4: Commit**

```bash
git add prisma/schema.prisma .env.example
git commit -m "feat: add complete Prisma schema (25 models) per docs/03_MODELO_DATOS.md"
```

---

### Task 3: Docker Compose para PostgreSQL local + primera migración + seed de catálogos reales

**Files:**
- Create: `docker-compose.yml`
- Create: `prisma/seed.ts`
- Modify: `package.json` (agregar script `prisma.seed`)

**Interfaces:**
- Consumes: modelos de catálogo definidos en Task 2 (`Predio`, `PredioAlias`, `Cuenta`, `Agencia`, `Area`, `Puesto`, `AttentionCause`, `InjuryType`, `MedicalExamType`, `MedicalExamResult`, `DisabilityType`, `AccidentType`, `AccidentCause`, `AccidentStatus`, `DrugTestType`, `DrugTestResult`, `DrugTestStatus`, `AgeRange`, `Role`, `Permission`, `RolePermission`, `User`).
- Produces: base de datos poblada localmente, ejecutable con `docker compose up -d && npx prisma migrate dev && npm run prisma:seed`.

- [ ] **Step 1: Crear `docker-compose.yml`**

```yaml
services:
  db:
    image: postgres:16-alpine
    restart: unless-stopped
    environment:
      POSTGRES_USER: salud_admin
      POSTGRES_PASSWORD: salud_dev_password
      POSTGRES_DB: salud_ocupacional
    ports:
      - "5432:5432"
    volumes:
      - db_data:/var/lib/postgresql/data

volumes:
  db_data:
```

- [ ] **Step 2: Levantar el contenedor y correr la primera migración**

```bash
docker compose up -d
```
Actualizar `.env` (no `.env.example`) local con:
```
DATABASE_URL="postgresql://salud_admin:salud_dev_password@localhost:5432/salud_ocupacional?schema=public"
```

```bash
npx prisma migrate dev --name init
```
Expected: se crea `prisma/migrations/<timestamp>_init/migration.sql` y las tablas quedan creadas en el contenedor.

- [ ] **Step 3: Instalar dependencias del seed**

```bash
npm install -D tsx bcryptjs
npm install -D @types/bcryptjs
```

- [ ] **Step 4: Escribir `prisma/seed.ts`**

```typescript
import { PrismaClient } from '@prisma/client'
import bcrypt from 'bcryptjs'

const prisma = new PrismaClient()

const PREDIOS: { nombre: string; aliases?: string[] }[] = [
  { nombre: 'AIFA' },
  { nombre: 'ATIZAPAN' },
  { nombre: 'CHARCON' },
  { nombre: 'FLORA' },
  { nombre: 'MACRO I', aliases: ['M1'] },
  { nombre: 'MACRO II', aliases: ['M2'] },
  { nombre: 'MERCURIO' },
  { nombre: 'MIKELS' },
  { nombre: 'SIGLO XXI', aliases: ['SIGLO'] },
  { nombre: 'SMO' },
  { nombre: 'TOLUCA' },
  { nombre: 'TULTIPARK', aliases: ['TULTI'] },
  { nombre: 'U TEPALCAPA', aliases: ['UT'] },
  { nombre: 'WORLD PARK', aliases: ['WP'] },
  { nombre: 'Z VALLEJO', aliases: ['Z VALL'] },
  { nombre: 'FORANEO', aliases: ['FOR'] },
  { nombre: 'SIN DATO' },
]

const CUENTAS = [
  'COMEDOR', 'MANTENIMIENTO', 'PREVENCION', 'SERV GRAL', 'STAFF', 'MIKELS', 'SIGLO XXI', 'SMO',
  'UNILEVER TEPALCAPA', 'FLORA', 'CLARINS', 'FISCAL', 'MILWAUKEE', 'PLANETA', 'PUIG',
  'WOLTER KLAUWER', 'BSD', '47 BRAND', '5.11 TACTICAL', 'ADIDDAS', 'ADOLFO DGUEZ',
  'COMBIBLOCK', 'CORTE FIEL', 'CYTIE', 'ENVIO PACK', 'FANDELI', 'JBL', 'MADDEN', 'MARTI',
  'PIAGUI', 'RHEEM', 'ROYAL CANIN', 'SUBURBIA', 'UNILEVER CIVAC', 'UNILEVER POP', 'SEARS',
  'CARTERS', 'HISENSE', 'P&G MERCURIO', 'AVANTE', 'HABERS', 'U LERMA', 'ALKA', 'RICHS',
  'TARKETT', 'DIAGEO', 'CANCUN', 'CBI', 'IRAPUATO', 'JIUTEPEC', 'MARTI GDL', 'TEMPE',
  'ECOMMERCE', 'CROSS DOCK', 'ZARA CADENAS', 'ZARA ALMACEN', 'WILSON', 'S/D',
]

const AREAS = [
  'STAFF', 'CALIDAD', 'CAPACITACION', 'COMEDOR', 'DESPACHO', 'EMBARQUES', 'INVENTARIOS',
  'LOCKERS', 'MTTO', 'OPERACION', 'PATIO', 'PREVENCION / SEG TRANSPORTE',
  'REC HUMANOS / RECLUTAMIENTO', 'RECUPERADO', 'SALUD OCUP', 'SEG E HIG',
  'SERV GRAL / LIMPIEZA', 'SISTEMAS', 'TRAFICO', 'CLIENTE / EXTERNO',
]

const PUESTOS = [
  'INCLUSION', 'CLIENTE / EXTERNO', 'ADMON', 'ANALISTA', 'ATN CLIENTES', 'AUDITOR',
  'AUTOMATISTA', 'AUX ALMACEN / AYUDANTE GRAL', 'BECARIA', 'CALIDAD', 'CAPTURISTA',
  'CHOFER', 'COCINA', 'COORDINADOR', 'COSTURA', 'DIRECCION', 'DOCUMENTADOR', 'EMBARQUES',
  'FACTURISTA', 'GERENTE', 'INVENTARIOS', 'JEFATURA', 'LAVADOR TARIMA',
  'LIMPIEZA / INTENDENCIA', 'MANIOBRISTA', 'MANTENIMIENTO', 'MAQUILA',
  'MEDICO / ENFERMERA', 'MESA CONTROL', 'MONITORISTA / PREVENCION / VIGILANCIA',
  'MONTACARGISTA', 'PATINERO', 'PLANEADOR TR2 / TRACKER', 'SEG E HIG', 'SUPERVISOR',
  'SURTIDOR', 'TASKER',
]

const AGENCIAS = ['GLI', 'ORLAMEX', 'VALLE LERMA', 'ONEST', 'OTRO']

const ATTENTION_CAUSES = [
  'ACIDO PEPTICA', 'ALERGIA / INTOXICACION', 'BUCODENTAL', 'CIRCULATORIO', 'CURACION',
  'DERMATOLOGICO', 'DIGESTIVO', 'EMBARAZO (DETECCION/CONTROL)', 'ENDOCRINO',
  'GENITOURINARIO', 'GINECOLOGICO', 'INYECCION', 'NEUROLOGICO', 'OFTALMICO', 'OTICO',
  'PSICOSOMATICO', 'RESPIRATORIO', 'TENSION ARTERIAL (DETEC/CONTROL)',
  'TOMA DE GLUCOSA', 'VACUNA / METODO PF / SEG SALUD', 'FISIOTERAPIA', 'NOM 035',
  'MT - ALGIA / MIALGIA / CONTUSION', 'MP - ALGIA / MIALGIA / CONTUSION',
  'GRAL - ALGIA / MIALGIA / CONTUSION', 'CERVICAL / DORSAL / LUMBAR',
]

const INJURY_TYPES: { nombre: string; region: string }[] = [
  { nombre: 'AMPUTACION MT', region: 'MT' },
  { nombre: 'AMPUTACION MP', region: 'MP' },
  { nombre: 'ALGIA/CONTUSION MT', region: 'MT' },
  { nombre: 'ALGIA/CONTUSION MP', region: 'MP' },
  { nombre: 'CERVICALGIA/DORSALGIA/LUMBALGIA', region: 'TRONCO' },
  { nombre: 'CONTUSION/TRAUMA CABEZA', region: 'CABEZA' },
  { nombre: 'CONTUSION/TRAUMA TRONCO', region: 'TRONCO' },
  { nombre: 'ESGUINCE MT', region: 'MT' },
  { nombre: 'ESGUINCE MP', region: 'MP' },
  { nombre: 'LUXACION MT', region: 'MT' },
  { nombre: 'LUXACION MP', region: 'MP' },
  { nombre: 'FX MT', region: 'MT' },
  { nombre: 'FX MP', region: 'MP' },
  { nombre: 'FX CABEZA/TRONCO', region: 'CABEZA' },
  { nombre: 'POLICONTUNDIDO', region: 'MULTIPLE' },
]

const MEDICAL_EXAM_TYPES = ['NUEVO INGRESO', 'PERIODICO', 'POS INCAPACIDAD', 'PRETEST']
const MEDICAL_EXAM_RESULTS = ['APTO', 'NO APTO', 'CONDICIONADO', 'INCLUSION']
const DISABILITY_TYPES = ['ENFERMEDAD GENERAL', 'MATERNIDAD', 'ACCIDENTE LABORAL', 'ACCIDENTE TRAYECTO', 'INTERNA']
const ACCIDENT_TYPES = ['LABORAL', 'TRAYECTO']
const ACCIDENT_CAUSES = ['ACCID VIAL', 'AGRESION', 'CAIDA', 'GOLPE', 'HERIDA', 'ERGONOMICA', 'IMPROCEDENTE']
const ACCIDENT_STATUSES = ['PENDIENTE', 'CALIFICADO', 'IMPROCEDENTE', 'BAJA INCONCLUSO']
const DRUG_TEST_TYPES = ['ANTIDOPING', 'ALCOHOLEMIA']
const DRUG_TEST_RESULTS = ['NEGATIVO', 'POSITIVO']
const DRUG_TEST_STATUSES = ['N/A', 'CAPA', 'NO INTERESADO/BAJA', 'NO CONTRATADO', 'RECAIDA']

const AGE_RANGES: { nombre: string; min: number; max: number | null }[] = [
  { nombre: '18-25', min: 18, max: 25 },
  { nombre: '26-35', min: 26, max: 35 },
  { nombre: '36-45', min: 36, max: 45 },
  { nombre: '46-55', min: 46, max: 55 },
  { nombre: '55+', min: 56, max: null },
]

const ROLES = ['SUPER_ADMIN', 'SALUD_OCUPACIONAL', 'GERENTE_SALUD', 'GERENTE_PREDIO', 'DIRECCION', 'CONSULTA']

const PERMISSIONS: { codigo: string; nivel: string }[] = [
  { codigo: 'attention.read', nivel: 'operativo' },
  { codigo: 'attention.create', nivel: 'operativo' },
  { codigo: 'attention.update', nivel: 'operativo' },
  { codigo: 'disability.read', nivel: 'operativo' },
  { codigo: 'disability.create', nivel: 'operativo' },
  { codigo: 'accident.read', nivel: 'operativo' },
  { codigo: 'accident.create', nivel: 'operativo' },
  { codigo: 'exam.read', nivel: 'operativo' },
  { codigo: 'exam.create', nivel: 'operativo' },
  { codigo: 'drugtest.read', nivel: 'operativo' },
  { codigo: 'drugtest.create', nivel: 'operativo' },
  { codigo: 'inventory.manage', nivel: 'operativo' },
  { codigo: 'maternity.read', nivel: 'operativo' },
  { codigo: 'maternity.create', nivel: 'operativo' },
  { codigo: 'dashboard.executive', nivel: 'agregado' },
  { codigo: 'employee.medical.read', nivel: 'operativo' },
  { codigo: 'reports.export', nivel: 'agregado' },
  { codigo: 'admin.catalogs', nivel: 'operativo' },
  { codigo: 'admin.users', nivel: 'operativo' },
  { codigo: 'admin.permissions', nivel: 'operativo' },
  { codigo: 'audit.read', nivel: 'operativo' },
]

async function main() {
  const predioMap = new Map<string, number>()
  for (const p of PREDIOS) {
    const created = await prisma.predio.upsert({
      where: { nombre: p.nombre },
      update: {},
      create: { nombre: p.nombre },
    })
    predioMap.set(p.nombre, created.id)
    if (p.aliases) {
      for (const alias of p.aliases) {
        await prisma.predioAlias.upsert({
          where: { predioId_alias: { predioId: created.id, alias } },
          update: {},
          create: { predioId: created.id, alias },
        })
      }
    }
  }

  for (const nombre of CUENTAS) {
    await prisma.cuenta.upsert({ where: { nombre }, update: {}, create: { nombre } })
  }
  for (const nombre of AGENCIAS) {
    await prisma.agencia.upsert({ where: { nombre }, update: {}, create: { nombre } })
  }
  for (const nombre of AREAS) {
    await prisma.area.upsert({ where: { nombre }, update: {}, create: { nombre } })
  }
  for (const nombre of PUESTOS) {
    await prisma.puesto.upsert({ where: { nombre }, update: {}, create: { nombre } })
  }
  for (const nombre of ATTENTION_CAUSES) {
    await prisma.attentionCause.upsert({ where: { nombre }, update: {}, create: { nombre } })
  }
  for (const it of INJURY_TYPES) {
    await prisma.injuryType.upsert({
      where: { nombre: it.nombre },
      update: {},
      create: { nombre: it.nombre, regionCorporal: it.region },
    })
  }
  for (const nombre of MEDICAL_EXAM_TYPES) {
    await prisma.medicalExamType.upsert({ where: { nombre }, update: {}, create: { nombre } })
  }
  for (const nombre of MEDICAL_EXAM_RESULTS) {
    await prisma.medicalExamResult.upsert({ where: { nombre }, update: {}, create: { nombre } })
  }
  for (const nombre of DISABILITY_TYPES) {
    await prisma.disabilityType.upsert({ where: { nombre }, update: {}, create: { nombre } })
  }
  for (const nombre of ACCIDENT_TYPES) {
    await prisma.accidentType.upsert({ where: { nombre }, update: {}, create: { nombre } })
  }
  for (const nombre of ACCIDENT_CAUSES) {
    await prisma.accidentCause.upsert({ where: { nombre }, update: {}, create: { nombre } })
  }
  for (const nombre of ACCIDENT_STATUSES) {
    await prisma.accidentStatus.upsert({ where: { nombre }, update: {}, create: { nombre } })
  }
  for (const nombre of DRUG_TEST_TYPES) {
    await prisma.drugTestType.upsert({ where: { nombre }, update: {}, create: { nombre } })
  }
  for (const nombre of DRUG_TEST_RESULTS) {
    await prisma.drugTestResult.upsert({ where: { nombre }, update: {}, create: { nombre } })
  }
  for (const nombre of DRUG_TEST_STATUSES) {
    await prisma.drugTestStatus.upsert({ where: { nombre }, update: {}, create: { nombre } })
  }
  for (const ar of AGE_RANGES) {
    await prisma.ageRange.upsert({
      where: { nombre: ar.nombre },
      update: {},
      create: { nombre: ar.nombre, edadMin: ar.min, edadMax: ar.max ?? undefined },
    })
  }

  const roleMap = new Map<string, number>()
  for (const nombre of ROLES) {
    const r = await prisma.role.upsert({ where: { nombre }, update: {}, create: { nombre } })
    roleMap.set(nombre, r.id)
  }

  const permMap = new Map<string, number>()
  for (const p of PERMISSIONS) {
    const created = await prisma.permission.upsert({
      where: { codigo: p.codigo },
      update: {},
      create: { codigo: p.codigo, nivelSensibilidad: p.nivel },
    })
    permMap.set(p.codigo, created.id)
  }

  const superAdminId = roleMap.get('SUPER_ADMIN')!
  for (const permId of permMap.values()) {
    await prisma.rolePermission.upsert({
      where: { roleId_permissionId: { roleId: superAdminId, permissionId: permId } },
      update: {},
      create: { roleId: superAdminId, permissionId: permId },
    })
  }

  const adminPasswordHash = await bcrypt.hash('CambiarEnProduccion123!', 10)
  await prisma.user.upsert({
    where: { email: 'admin@saludocupacional.local' },
    update: {},
    create: {
      email: 'admin@saludocupacional.local',
      passwordHash: adminPasswordHash,
      nombre: 'Administrador del Sistema',
      roleId: superAdminId,
    },
  })

  console.log('Seed completado: catálogos reales del Excel + roles + permisos + usuario admin.')
}

main()
  .catch((e) => {
    console.error(e)
    process.exit(1)
  })
  .finally(async () => {
    await prisma.$disconnect()
  })
```

- [ ] **Step 5: Registrar el script de seed en `package.json`**

Agregar a `package.json`:
```json
{
  "prisma": {
    "seed": "tsx prisma/seed.ts"
  }
}
```
Y en `"scripts"`:
```json
"prisma:seed": "tsx prisma/seed.ts"
```

- [ ] **Step 6: Ejecutar el seed y verificar**

```bash
npx prisma db seed
```
Expected: log final "Seed completado: catálogos reales del Excel + roles + permisos + usuario admin." sin errores.

Verificar con:
```bash
npx prisma studio
```
Abrir `Predio` y confirmar 17 registros; abrir `User` y confirmar el usuario `admin@saludocupacional.local`. Cerrar Prisma Studio (Ctrl+C) tras verificar.

- [ ] **Step 7: Commit**

```bash
git add docker-compose.yml prisma/seed.ts prisma/migrations package.json package-lock.json
git commit -m "feat: add Docker Compose Postgres, first migration, and catalog seed with real Excel values"
```

---

### Task 4: Auth propia (bcrypt + JWT) y cliente Prisma singleton

**Files:**
- Create: `src/lib/prisma.ts`
- Create: `src/lib/auth/hash.ts`
- Create: `src/lib/auth/jwt.ts`
- Create: `src/lib/auth/session.ts`
- Test: `src/lib/auth/__tests__/hash.test.ts`
- Test: `src/lib/auth/__tests__/jwt.test.ts`

**Interfaces:**
- Consumes: `DATABASE_URL`, `JWT_SECRET`, `JWT_EXPIRES_IN` de `.env`.
- Produces: `hashPassword(plain: string): Promise<string>`, `verifyPassword(plain: string, hash: string): Promise<boolean>`, `signSessionToken(payload: SessionPayload): string`, `verifySessionToken(token: string): SessionPayload | null`, `getSession(): Promise<SessionPayload | null>` — consumidos por Task 5 (rutas de login) y por cualquier API route futura que el backend construya.

- [ ] **Step 1: Instalar dependencias**

```bash
npm install jsonwebtoken
npm install -D @types/jsonwebtoken vitest
```

- [ ] **Step 2: Crear `src/lib/prisma.ts`**

```typescript
import { PrismaClient } from '@prisma/client'

const globalForPrisma = globalThis as unknown as { prisma?: PrismaClient }

export const prisma = globalForPrisma.prisma ?? new PrismaClient()

if (process.env.NODE_ENV !== 'production') {
  globalForPrisma.prisma = prisma
}
```

- [ ] **Step 3: Escribir el test de hash (falla primero)**

Crear `src/lib/auth/__tests__/hash.test.ts`:
```typescript
import { describe, it, expect } from 'vitest'
import { hashPassword, verifyPassword } from '../hash'

describe('hash', () => {
  it('hashes a password and verifies it correctly', async () => {
    const hash = await hashPassword('CambiarEnProduccion123!')
    expect(hash).not.toBe('CambiarEnProduccion123!')
    expect(await verifyPassword('CambiarEnProduccion123!', hash)).toBe(true)
  })

  it('rejects an incorrect password', async () => {
    const hash = await hashPassword('CambiarEnProduccion123!')
    expect(await verifyPassword('otra-clave', hash)).toBe(false)
  })
})
```

- [ ] **Step 4: Correr el test y confirmar que falla**

Run: `npx vitest run src/lib/auth/__tests__/hash.test.ts`
Expected: FAIL — "Cannot find module '../hash'"

- [ ] **Step 5: Implementar `src/lib/auth/hash.ts`**

```typescript
import bcrypt from 'bcryptjs'

const SALT_ROUNDS = 10

export async function hashPassword(plain: string): Promise<string> {
  return bcrypt.hash(plain, SALT_ROUNDS)
}

export async function verifyPassword(plain: string, hash: string): Promise<boolean> {
  return bcrypt.compare(plain, hash)
}
```

- [ ] **Step 6: Correr el test y confirmar que pasa**

Run: `npx vitest run src/lib/auth/__tests__/hash.test.ts`
Expected: PASS (2 tests)

- [ ] **Step 7: Escribir el test de JWT (falla primero)**

Crear `src/lib/auth/__tests__/jwt.test.ts`:
```typescript
import { describe, it, expect, beforeAll } from 'vitest'

beforeAll(() => {
  process.env.JWT_SECRET = 'test-secret-not-for-production'
  process.env.JWT_EXPIRES_IN = '7d'
})

describe('jwt', () => {
  it('signs and verifies a session token', async () => {
    const { signSessionToken, verifySessionToken } = await import('../jwt')
    const payload = { userId: 1, roleId: 2, email: 'a@b.com' }
    const token = signSessionToken(payload)
    const decoded = verifySessionToken(token)
    expect(decoded?.userId).toBe(1)
    expect(decoded?.roleId).toBe(2)
    expect(decoded?.email).toBe('a@b.com')
  })

  it('returns null for an invalid token', async () => {
    const { verifySessionToken } = await import('../jwt')
    expect(verifySessionToken('token-invalido')).toBeNull()
  })
})
```

- [ ] **Step 8: Correr el test y confirmar que falla**

Run: `npx vitest run src/lib/auth/__tests__/jwt.test.ts`
Expected: FAIL — "Cannot find module '../jwt'"

- [ ] **Step 9: Implementar `src/lib/auth/jwt.ts`**

```typescript
import jwt from 'jsonwebtoken'

export interface SessionPayload {
  userId: number
  roleId: number
  email: string
}

function getSecret(): string {
  const secret = process.env.JWT_SECRET
  if (!secret) throw new Error('JWT_SECRET no está configurado')
  return secret
}

export function signSessionToken(payload: SessionPayload): string {
  return jwt.sign(payload, getSecret(), {
    expiresIn: process.env.JWT_EXPIRES_IN ?? '7d',
  })
}

export function verifySessionToken(token: string): SessionPayload | null {
  try {
    return jwt.verify(token, getSecret()) as SessionPayload
  } catch {
    return null
  }
}
```

- [ ] **Step 10: Correr el test y confirmar que pasa**

Run: `npx vitest run src/lib/auth/__tests__/jwt.test.ts`
Expected: PASS (2 tests)

- [ ] **Step 11: Implementar `src/lib/auth/session.ts` (helper para Server Components y Route Handlers)**

```typescript
import { cookies } from 'next/headers'
import { verifySessionToken, type SessionPayload } from './jwt'

const SESSION_COOKIE_NAME = 'session_token'

export async function getSession(): Promise<SessionPayload | null> {
  const cookieStore = await cookies()
  const token = cookieStore.get(SESSION_COOKIE_NAME)?.value
  if (!token) return null
  return verifySessionToken(token)
}

export { SESSION_COOKIE_NAME }
```

- [ ] **Step 12: Commit**

```bash
git add src/lib/prisma.ts src/lib/auth package.json package-lock.json
git commit -m "feat: add own auth (bcrypt + JWT) and Prisma singleton client"
```

---

### Task 5: Ruta de login funcional (`/login` + `/api/auth/login`) y middleware de protección de rutas

**Files:**
- Create: `src/app/(auth)/login/page.tsx`
- Create: `src/app/api/auth/login/route.ts`
- Create: `src/app/api/auth/logout/route.ts`
- Create: `src/middleware.ts`

**Interfaces:**
- Consumes: `prisma` (Task 4), `hashPassword`/`verifyPassword` (Task 4), `signSessionToken` (Task 4), `SESSION_COOKIE_NAME` (Task 4).
- Produces: usuario autenticado puede iniciar sesión con `admin@saludocupacional.local` / `CambiarEnProduccion123!` y ser redirigido a `/dashboard`; rutas fuera de `/login` y `/api/auth/*` quedan protegidas por el middleware si no hay cookie de sesión válida.

- [ ] **Step 1: Crear el route handler de login**

Crear `src/app/api/auth/login/route.ts`:
```typescript
import { NextRequest, NextResponse } from 'next/server'
import { prisma } from '@/lib/prisma'
import { verifyPassword } from '@/lib/auth/hash'
import { signSessionToken } from '@/lib/auth/jwt'
import { SESSION_COOKIE_NAME } from '@/lib/auth/session'

export async function POST(request: NextRequest) {
  const body = await request.json().catch(() => null)
  const email = typeof body?.email === 'string' ? body.email : null
  const password = typeof body?.password === 'string' ? body.password : null

  if (!email || !password) {
    return NextResponse.json({ error: 'Email y contraseña son requeridos' }, { status: 400 })
  }

  const user = await prisma.user.findUnique({ where: { email }, include: { role: true } })

  if (!user || !user.activo || !(await verifyPassword(password, user.passwordHash))) {
    return NextResponse.json({ error: 'Credenciales inválidas' }, { status: 401 })
  }

  const token = signSessionToken({ userId: user.id, roleId: user.roleId, email: user.email })

  await prisma.user.update({ where: { id: user.id }, data: { ultimoAcceso: new Date() } })

  const response = NextResponse.json({
    user: { id: user.id, email: user.email, nombre: user.nombre, role: user.role.nombre },
  })
  response.cookies.set(SESSION_COOKIE_NAME, token, {
    httpOnly: true,
    secure: process.env.NODE_ENV === 'production',
    sameSite: 'lax',
    path: '/',
    maxAge: 60 * 60 * 24 * 7,
  })
  return response
}
```

- [ ] **Step 2: Crear el route handler de logout**

Crear `src/app/api/auth/logout/route.ts`:
```typescript
import { NextResponse } from 'next/server'
import { SESSION_COOKIE_NAME } from '@/lib/auth/session'

export async function POST() {
  const response = NextResponse.json({ ok: true })
  response.cookies.delete(SESSION_COOKIE_NAME)
  return response
}
```

- [ ] **Step 3: Crear la página de login**

Crear `src/app/(auth)/login/page.tsx`:
```tsx
'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'

export default function LoginPage() {
  const router = useRouter()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setLoading(true)
    setError(null)
    const res = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password }),
    })
    setLoading(false)
    if (!res.ok) {
      const data = await res.json().catch(() => ({}))
      setError(data.error ?? 'No se pudo iniciar sesión')
      return
    }
    router.push('/dashboard')
    router.refresh()
  }

  return (
    <main style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
      <form onSubmit={handleSubmit} style={{ width: 360, display: 'flex', flexDirection: 'column', gap: 12 }}>
        <h1>Portal Salud Ocupacional</h1>
        <input
          type="email"
          placeholder="Correo"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />
        <input
          type="password"
          placeholder="Contraseña"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />
        {error && <p style={{ color: 'red' }}>{error}</p>}
        <button type="submit" disabled={loading}>
          {loading ? 'Entrando…' : 'Entrar'}
        </button>
      </form>
    </main>
  )
}
```
Nota para el desarrollador de backend/frontend: este formulario usa estilos inline mínimos a propósito — el theming real (paleta, tipografía Lexend/Source Sans 3, componentes shadcn/ui) se aplica en una tarea de UI posterior fuera de este plan, documentada en `docs/BACKEND_HANDOFF.md`.

- [ ] **Step 4: Crear el middleware de protección de rutas**

Crear `src/middleware.ts`:
```typescript
import { NextRequest, NextResponse } from 'next/server'

const PUBLIC_PATHS = ['/login', '/api/auth/login', '/api/auth/logout', '/api/health']

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl
  const isPublic = PUBLIC_PATHS.some((p) => pathname === p || pathname.startsWith(p + '/'))
  if (isPublic) return NextResponse.next()

  const token = request.cookies.get('session_token')?.value
  if (!token) {
    const loginUrl = new URL('/login', request.url)
    return NextResponse.redirect(loginUrl)
  }
  return NextResponse.next()
}

export const config = {
  matcher: ['/((?!_next/static|_next/image|favicon.ico).*)'],
}
```
Nota: el middleware de Next.js corre en Edge Runtime y no puede usar `jsonwebtoken` (depende de Node `crypto`) — por eso aquí solo verifica que la cookie exista, no la valida criptográficamente. La validación real del token ocurre en cada Route Handler vía `getSession()` (Task 4), que sí corre en Node runtime. Este matiz debe quedar explícito en `docs/SECURITY.md` (Task 8) para que el desarrollador de backend no asuma protección completa a nivel de middleware.

- [ ] **Step 5: Crear el endpoint de salud usado por el middleware y por Docker healthchecks**

Crear `src/app/api/health/route.ts`:
```typescript
import { NextResponse } from 'next/server'

export async function GET() {
  return NextResponse.json({ status: 'ok', timestamp: new Date().toISOString() })
}
```

- [ ] **Step 6: Probar el flujo manualmente**

```bash
npm run dev
```
En otra terminal:
```bash
curl -i -X POST http://localhost:3000/api/auth/login -H "Content-Type: application/json" -d "{\"email\":\"admin@saludocupacional.local\",\"password\":\"CambiarEnProduccion123!\"}"
```
Expected: `200 OK`, cuerpo JSON con `user`, header `Set-Cookie: session_token=...`.

```bash
curl -i http://localhost:3000/dashboard
```
Expected sin cookie: `307` redirect a `/login`. Detener el servidor de dev tras verificar.

- [ ] **Step 7: Commit**

```bash
git add src/app/api/auth src/app/\(auth\) src/middleware.ts
git commit -m "feat: implement login/logout routes and route-protection middleware"
```

---

### Task 6: Página `/dashboard` mínima conectada a datos reales (prueba end-to-end del stack)

**Files:**
- Create: `src/app/dashboard/page.tsx`
- Create: `src/app/dashboard/layout.tsx`
- Modify: `src/app/page.tsx` (redirige a `/dashboard`)

**Interfaces:**
- Consumes: `prisma` (Task 4), `getSession` (Task 4), modelos `MedicalAttention`, `Disability`, `Accident`, `MedicalExam` (Task 2).
- Produces: página server-rendered que demuestra el pipeline completo `DB → Prisma → Server Component → HTML`, sirviendo de plantilla de referencia para que el backend developer replique el patrón en las demás vistas del `demo.html`.

- [ ] **Step 1: Crear el layout del área autenticada**

Crear `src/app/dashboard/layout.tsx`:
```tsx
import { redirect } from 'next/navigation'
import { getSession } from '@/lib/auth/session'

export default async function DashboardLayout({ children }: { children: React.ReactNode }) {
  const session = await getSession()
  if (!session) redirect('/login')
  return <>{children}</>
}
```

- [ ] **Step 2: Crear la página de dashboard con conteos reales**

Crear `src/app/dashboard/page.tsx`:
```tsx
import { prisma } from '@/lib/prisma'

export default async function DashboardPage() {
  const [attentionsCount, disabilitiesCount, accidentsCount, examsCount] = await Promise.all([
    prisma.medicalAttention.count({ where: { deletedAt: null } }),
    prisma.disability.count({ where: { deletedAt: null } }),
    prisma.accident.count({ where: { deletedAt: null } }),
    prisma.medicalExam.count({ where: { deletedAt: null } }),
  ])

  return (
    <main style={{ padding: 32 }}>
      <h1>Dashboard Ejecutivo</h1>
      <p>Datos reales desde PostgreSQL vía Prisma (catálogos sembrados, sin registros operativos aún).</p>
      <ul>
        <li>Atenciones médicas: {attentionsCount}</li>
        <li>Incapacidades: {disabilitiesCount}</li>
        <li>Accidentes: {accidentsCount}</li>
        <li>Exámenes médicos: {examsCount}</li>
      </ul>
    </main>
  )
}
```

- [ ] **Step 3: Redirigir la raíz a `/dashboard`**

Reemplazar el contenido de `src/app/page.tsx`:
```tsx
import { redirect } from 'next/navigation'

export default function RootPage() {
  redirect('/dashboard')
}
```

- [ ] **Step 4: Probar manualmente el flujo completo**

```bash
npm run dev
```
En el navegador: ir a `http://localhost:3000/` → debe redirigir a `/login` (sin sesión) → iniciar sesión con las credenciales del seed → debe redirigir a `/dashboard` → debe mostrarse "Atenciones médicas: 0", "Incapacidades: 0", etc. (0 porque el seed solo carga catálogos, no registros operativos — correcto y esperado).

Detener el servidor tras verificar.

- [ ] **Step 5: Commit**

```bash
git add src/app/dashboard src/app/page.tsx
git commit -m "feat: add authenticated dashboard page wired to real Prisma counts"
```

---

### Task 7: `docs/DATA_DICTIONARY.md` — diccionario de datos completo

**Files:**
- Create: `docs/DATA_DICTIONARY.md`

**Interfaces:**
- Consumes: `prisma/schema.prisma` (Task 2) como fuente exacta de nombres de tabla/columna, y `docs/01_ANALISIS_ACTUAL.md` §4 como fuente de origen de cada catálogo.

- [ ] **Step 1: Escribir el diccionario de datos**

Crear `docs/DATA_DICTIONARY.md` con esta estructura (una sección por tabla, generada a partir del schema de Task 2 — el contenido real debe listar cada campo de cada uno de los 25 modelos con: nombre de columna en BD, tipo Prisma, nullable/default, y de qué hoja/catálogo del Excel proviene según `docs/01_ANALISIS_ACTUAL.md`):

```markdown
# Diccionario de Datos — Portal Salud Ocupacional

> Generado a partir de `prisma/schema.prisma`. Cada tabla lista: columna, tipo, nullable, default, origen (hoja del Excel o "nueva — no existía en el Excel").

## Catálogos maestros

### predios
| Columna | Tipo | Nullable | Default | Origen |
|---|---|---|---|---|
| id | Int (PK, autoincrement) | No | — | nueva |
| codigo | String | Sí | — | nueva (para integración futura con RH/Oracle) |
| nombre | String (único) | No | — | ATEN, CAUSAS, MUSC-ESQU, EM, INC IMSS, INC INTER, ACCID, ANTDPG, ABAST ANTDP, ANTALCOH, MATER — columna A "predio" en todas |
| activo | Boolean | No | true | nueva |
| orden | Int | No | 0 | nueva |
| created_at / updated_at | DateTime | No | now() / auto | nueva |

### predio_aliases
| Columna | Tipo | Nullable | Default | Origen |
|---|---|---|---|---|
| id | Int (PK) | No | — | nueva |
| predio_id | Int (FK → predios) | No | — | nueva |
| alias | String | No | — | ABAST ANTDP (nomenclatura abreviada: M1, M2, TULTI, UT, WP, Z VALL, FOR — ver `01_ANALISIS_ACTUAL.md` §5.3) |

[... continuar con el mismo formato de tabla para: cuentas, agencias, areas, puestos, attention_causes, injury_types, medical_exam_types, medical_exam_results, disability_types, accident_types, accident_causes, accident_statuses, drug_test_types, drug_test_results, drug_test_statuses, age_ranges, risk_thresholds, employees, periods, medical_attentions, medical_exams, disabilities, disability_costs, accidents, drug_tests, drug_test_batches, drug_test_inventory_movements, maternity_cases, users, roles, permissions, role_permissions, user_predio_access, audit_logs, attachments, import_batches, import_batch_rows —
usando exactamente los nombres de columna `@map(...)` del schema de Task 2 y citando la hoja de origen correspondiente según docs/01_ANALISIS_ACTUAL.md §3 para cada tabla operativa, o "nueva — no existía en el Excel, requerida para [razón]" para tablas como periods, users, audit_logs, import_batches, disability_costs, drug_test_inventory_movements.]

## Reglas de integridad no expresables solo en el schema

- `disabilities.origen_tipo` solo acepta los valores `manual`, `accidente`, `maternidad` — validado en la capa `services/`, no como enum de base de datos (se documenta la razón: permitir agregar un cuarto origen sin migración de schema).
- `medical_attentions`: se requiere `attention_cause_id` O `injury_type_id` no nulo (al menos uno) — validado en `services/`, no expresable como CHECK simple porque depende de lógica de negocio evolutiva.
- `accidents.dias_perdidos` es una copia de lectura rápida; el valor de verdad vive en `disabilities.dias_incapacidad` del registro enlazado vía `accidents.disability_id` — ver `docs/03_MODELO_DATOS.md` §4 nota de `accidents`.
```

Escribir el archivo completo con las 25 tablas documentadas siguiendo el mismo patrón. Verificar cruzando cada tabla contra `prisma/schema.prisma` para que ningún nombre de columna quede desincronizado.

- [ ] **Step 2: Commit**

```bash
git add docs/DATA_DICTIONARY.md
git commit -m "docs: add complete data dictionary cross-referencing Prisma schema and Excel source"
```

---

### Task 8: `docs/API.md` — contrato de API para el backend

**Files:**
- Create: `docs/API.md`

**Interfaces:**
- Consumes: rutas ya implementadas (`/api/auth/login`, `/api/auth/logout`, `/api/health`) de Tasks 5-6, y el listado de endpoints objetivo de `docs/04_MAPA_MODULOS.md` + el prompt maestro del usuario (`/api/employees`, `/api/attentions`, `/api/disabilities`, `/api/accidents`, `/api/medical-exams`, `/api/drug-tests`, `/api/alcohol-tests`, `/api/maternity`, `/api/inventory`, `/api/dashboard/summary`, `/api/dashboard/trends`, `/api/dashboard/predios`, `/api/analytics/insights`, `/api/reports`, `/api/imports`).

- [ ] **Step 1: Escribir `docs/API.md`**

```markdown
# API.md — Contrato de API del Portal Salud Ocupacional

## Convenciones generales

- Todas las rutas viven bajo `src/app/api/**/route.ts` (Next.js Route Handlers).
- Toda respuesta de error usa el formato: `{ "error": "mensaje legible" }` con el código HTTP correspondiente (400 validación, 401 no autenticado, 403 sin permiso, 404 no encontrado, 409 conflicto/duplicado, 500 error interno).
- Toda ruta que no sea `/api/auth/*` ni `/api/health` requiere sesión válida (cookie `session_token`, ver `src/lib/auth/session.ts`). Verificar con `const session = await getSession(); if (!session) return NextResponse.json({ error: 'No autenticado' }, { status: 401 })` al inicio de cada handler.
- Toda ruta de escritura (`POST`/`PATCH`/`DELETE`) debe verificar el permiso específico contra `role_permissions` antes de tocar Prisma — nunca confiar en que el frontend ya ocultó el botón. Patrón sugerido: `requirePermission(session, 'disability.create')` como helper en `src/lib/auth/permissions.ts` (a implementar por el backend developer, no incluido en el skeleton).
- Toda escritura relevante genera un registro en `audit_logs` (usuario, acción, módulo, entidad, entidad_id, valor_anterior, valor_nuevo) — ver `docs/SECURITY.md`.
- Filtros comunes en query params de los endpoints de lectura: `anio`, `mes`, `predioId`, `cuentaId`, `page`, `pageSize` (paginación server-side obligatoria, nunca traer toda la tabla).

## Ya implementado en el skeleton

### `POST /api/auth/login`
Request: `{ "email": string, "password": string }`
Response 200: `{ "user": { "id": number, "email": string, "nombre": string, "role": string } }` + cookie `session_token` (httpOnly).
Response 401: `{ "error": "Credenciales inválidas" }`
Implementación: `src/app/api/auth/login/route.ts`.

### `POST /api/auth/logout`
Response 200: `{ "ok": true }`, limpia la cookie de sesión.
Implementación: `src/app/api/auth/logout/route.ts`.

### `GET /api/health`
Response 200: `{ "status": "ok", "timestamp": ISO8601 }`. Sin autenticación, usado por Docker healthcheck.
Implementación: `src/app/api/health/route.ts`.

## Pendiente de implementar por el backend developer

Para cada uno de los siguientes, el patrón a seguir es el mismo que `/api/auth/login`: Route Handler en `src/app/api/<recurso>/route.ts`, validación con Zod, llamada a un `service` en `src/services/<recurso>Service.ts` (no a Prisma directo desde el route handler), respuesta JSON tipada.

### `GET/POST /api/employees`
- `GET`: lista paginada de `Employee`, filtrable por `predioId`, `cuentaId`, `activo`. Requiere permiso `employee.medical.read` para incluir campos médicos relacionados; sin ese permiso, devolver solo datos básicos (nombre, predio, puesto).
- `POST`: crea un `Employee`. Requiere permiso `admin.users` o equivalente de RH (definir en `docs/SECURITY.md` al implementar). Body: campos de `docs/DATA_DICTIONARY.md#employees` excepto `id`, `createdAt`, `updatedAt`.

### `GET/POST /api/attentions`
- `GET`: lista de `MedicalAttention` filtrable por `predioId`, `anio`, `mes`, `attentionCauseId`. Requiere `attention.read`.
- `POST`: crea una `MedicalAttention`. Requiere `attention.create`. Validar que exista `attentionCauseId` o `injuryTypeId` (regla de negocio, ver `docs/DATA_DICTIONARY.md`). Debe resolver `periodId` a partir de la fecha enviada (buscar o crear el `Period` correspondiente vía `services/periodService.ts`).

### `GET/POST /api/disabilities`
- `GET`: lista de `Disability` filtrable por `predioId`, `anio`, `mes`, `disabilityTypeId`, `origenTipo`. Requiere `disability.read`.
- `POST`: crea una `Disability` con `origenTipo = 'manual'` únicamente. Requiere `disability.create`. **Nunca** debe permitir crear una `Disability` con `origenTipo = 'accidente'` o `'maternidad'` directamente — esas se generan solo desde `POST /api/accidents` y `POST /api/maternity` respectivamente (ver `docs/03_MODELO_DATOS.md` §4 y §8, regla de evento único).

### `GET/POST /api/accidents`
- `POST`: si el body incluye `generaIncapacidad: true` junto con los campos de incapacidad (`fechaInicio`, `diasIncapacidad`, `folioImss` opcional), el `service` debe crear `Accident` y `Disability` (`origenTipo: 'accidente'`, `origenId: accident.id`) en una única transacción Prisma (`prisma.$transaction`). Ver ejemplo de flujo completo en el prompt maestro §42 del usuario, replicado en `docs/BACKEND_HANDOFF.md`.

### `GET/POST /api/medical-exams`
Análogo a `/api/attentions`. Requiere `exam.read` / `exam.create`.

### `GET/POST /api/drug-tests`
- `POST`: al registrar una prueba con `batchId`, el `service` debe crear en la misma transacción un `DrugTestInventoryMovement` de tipo `consumo` por 1 unidad sobre ese lote — nunca editar `DrugTestBatch` directamente (ver `docs/03_MODELO_DATOS.md` §4, `drug_test_inventory_movements`).

### `GET/POST /api/alcohol-tests`
Filtra `DrugTest` donde `drugTestTypeId` corresponda al tipo "ALCOHOLEMIA" — no es una tabla separada (confirmado en `docs/01_ANALISIS_ACTUAL.md` §7: ANTALCOH es subconjunto de ANTDPG).

### `GET/POST /api/maternity`
Igual patrón que `/api/accidents`: crea `MaternityCase` + `Disability` (`origenTipo: 'maternidad'`) en una transacción.

### `GET/POST /api/inventory`
`GET`: existencia calculada por lote (`cantidadInicial - SUM(movimientos tipo consumo) + SUM(movimientos tipo entrada)`), nunca un campo de stock persistido. `POST`: registra un movimiento (`entrada`, `consumo` manual, o ajuste), nunca escribe un número de stock directo.

### `GET /api/dashboard/summary`
Query params: `anio`, `mes`, `predioId` (opcional). Devuelve los 6 KPIs del Dashboard Ejecutivo (`docs/05_DASHBOARD_KPIS.md` §2) ya agregados server-side. Debe usar el mismo `analyticsService` que consumirán `/api/reports` (ver `docs/02_ARQUITECTURA_PROPUESTA.md` §6 — dashboard y reportes comparten servicio, nunca fórmulas duplicadas).

### `GET /api/dashboard/trends`
Query params: `anio`, `predioId`, `indicador` (atenciones|incapacidades|dias_perdidos|accidentes|examenes). Devuelve serie de 12 meses.

### `GET /api/dashboard/predios`
Ranking de predios con semáforo de riesgo (calculado contra `risk_thresholds`, nunca hardcodeado).

### `GET /api/analytics/insights`
Devuelve la lista de insights de texto generados por reglas estadísticas (`docs/06_PLAN_IMPLEMENTACION.md` Fase 16). Debe ser consumible también por un futuro asistente de IA sin exponer acceso directo a la base de datos (principio del prompt maestro §49).

### `GET /api/reports`
Query params: `tipo` (ejecutivo|morbilidad|incapacidades|accidentabilidad|examenes|antidoping|maternidad|predio), `formato` (pdf|excel|csv), más los mismos filtros que `/api/dashboard/summary`. Debe reusar `analyticsService`.

### `POST /api/imports`
Recibe un archivo (`multipart/form-data`), lo parsea server-side, crea un `ImportBatch` + `ImportBatchRow` por fila detectada, devuelve preview sin escribir en las tablas operativas hasta una confirmación explícita en una segunda llamada (`POST /api/imports/:id/confirm`). Ver flujo completo en `docs/06_PLAN_IMPLEMENTACION.md` Fase 8.
```

- [ ] **Step 2: Commit**

```bash
git add docs/API.md
git commit -m "docs: add API contract for backend developer (implemented + pending endpoints)"
```

---

### Task 9: `docs/SECURITY.md` — guía de seguridad y RBAC

**Files:**
- Create: `docs/SECURITY.md`

**Interfaces:**
- Consumes: modelos `User`, `Role`, `Permission`, `RolePermission`, `UserPredioAccess`, `AuditLog` (Task 2), funciones de `src/lib/auth/*` (Task 4).

- [ ] **Step 1: Escribir `docs/SECURITY.md`**

```markdown
# SECURITY.md — Autenticación, RBAC y Auditoría

## Autenticación (ya implementada en el skeleton)

- Contraseñas: hash con bcrypt (`src/lib/auth/hash.ts`, 10 salt rounds). Nunca se almacena ni se loguea la contraseña en texto plano — verificar que ningún `console.log` ni `audit_logs` capture el campo `password`.
- Sesión: JWT firmado con `JWT_SECRET` (`src/lib/auth/jwt.ts`), almacenado en cookie `session_token` con flags `httpOnly`, `secure` (en producción), `sameSite: lax`. El JWT expira en `JWT_EXPIRES_IN` (default 7 días) — no hay refresh token en el MVP; al expirar, el usuario debe volver a iniciar sesión.
- **Importante — límite del middleware**: `src/middleware.ts` corre en Edge Runtime, que no soporta el módulo `crypto` de Node que usa `jsonwebtoken`. Por eso el middleware solo verifica que la cookie *exista*, no que sea válida. La validación criptográfica real ocurre en cada Route Handler vía `getSession()` (Node runtime). **Todo Route Handler que no sea público debe llamar `getSession()` y devolver 401 si es `null`** — no asumir que pasar el middleware significa estar autenticado.

## RBAC (a completar por el backend developer)

Roles sembrados en `prisma/seed.ts` (Task 3): `SUPER_ADMIN`, `SALUD_OCUPACIONAL`, `GERENTE_SALUD`, `GERENTE_PREDIO`, `DIRECCION`, `CONSULTA`. Solo `SUPER_ADMIN` tiene todos los permisos asignados en el seed — los demás roles deben recibir su set de `role_permissions` según la matriz de responsabilidades (a definir con negocio, borrador sugerido):

| Rol | Permisos sugeridos |
|---|---|
| SALUD_OCUPACIONAL | todos los `*.read` y `*.create` operativos, `inventory.manage` |
| GERENTE_SALUD | todos los `*.read`, `dashboard.executive`, `reports.export` — sin `*.create` |
| GERENTE_PREDIO | igual que GERENTE_SALUD pero acotado por `user_predio_access` (ver abajo) |
| DIRECCION | `dashboard.executive`, `reports.export` únicamente — sin acceso a `employee.medical.read` (solo agregados) |
| CONSULTA | solo lectura agregada, sin `employee.medical.read` |

**Patrón de verificación de permiso** (implementar como `src/lib/auth/permissions.ts`, no incluido en el skeleton):
```typescript
export async function requirePermission(session: SessionPayload, codigo: string) {
  const hasPermission = await prisma.rolePermission.findFirst({
    where: { roleId: session.roleId, permission: { codigo } },
  })
  if (!hasPermission) throw new ForbiddenError(codigo)
}
```
Llamar esto al inicio de cada Route Handler de escritura, y en los de lectura que expongan datos médicos individuales.

**Alcance por predio** (`user_predio_access`): para roles `GERENTE_PREDIO`, todo query de lectura debe añadir `WHERE predio_id IN (SELECT predio_id FROM user_predio_access WHERE user_id = :sessionUserId)`. Implementar como un helper `scopePredioFilter(session, baseWhere)` reutilizado por todos los `repositories/`, no reescrito por endpoint.

## Separación de sensibilidad (información ejecutiva vs. operativa)

- Todo `Permission.nivelSensibilidad = 'agregado'` (ej. `dashboard.executive`, `reports.export`) debe resolver contra las vistas SQL de solo agregados (`vw_ejecutivo_agregado` y equivalentes, ver `docs/03_MODELO_DATOS.md` §7), nunca contra las tablas operativas con `employee_id` expuesto.
- El permiso `employee.medical.read` es el único que habilita ver diagnósticos/causas médicas a nivel de un empleado identificado (ej. en `/employees/[id]` — Perfil de Empleado). Roles `DIRECCION` y `CONSULTA` no deben tenerlo por defecto.

## Auditoría

Cada escritura relevante (create/update/delete/export/import/login) debe insertar en `audit_logs`:
```typescript
await prisma.auditLog.create({
  data: {
    userId: session.userId,
    accion: 'create',
    modulo: 'disabilities',
    entidad: 'Disability',
    entidadId: newDisability.id,
    valorNuevo: newDisability, // JSON — nunca incluir campos de contraseña
    contieneDatosSensibles: true, // true si el registro incluye diagnóstico/causa médica identificable
  },
})
```
**Regla dura**: si el módulo es `medical_attentions`, `medical_exams`, o cualquier tabla con observaciones clínicas, marcar `contieneDatosSensibles: true` y considerar omitir el campo `observaciones` de `valorAnterior`/`valorNuevo` (guardar solo que cambió, no el contenido) — ver `docs/03_MODELO_DATOS.md` §5 nota de `audit_logs`.

## Checklist de seguridad antes de exponer un nuevo endpoint

1. ¿Verifica `getSession()` y devuelve 401 si no hay sesión?
2. ¿Verifica el permiso específico con `requirePermission()` y devuelve 403 si falta?
3. ¿Si es un rol acotado por predio, aplica `scopePredioFilter()`?
4. ¿Valida el body/query con un schema Zod antes de tocar Prisma?
5. ¿Registra en `audit_logs` si es una escritura?
6. ¿Evita loguear contraseñas, tokens, o datos médicos innecesarios en `console.log`?
7. ¿Usa `prisma.$transaction` si toca más de una tabla relacionada (ver regla de evento único)?
```

- [ ] **Step 2: Commit**

```bash
git add docs/SECURITY.md
git commit -m "docs: add security and RBAC guide for backend developer"
```

---

### Task 10: `docs/BACKEND_HANDOFF.md` — documento maestro de bienvenida

**Files:**
- Create: `docs/BACKEND_HANDOFF.md`

**Interfaces:**
- Consumes: todos los documentos anteriores (`01`-`06`, `API.md`, `DATA_DICTIONARY.md`, `SECURITY.md`, `02_ARQUITECTURA_PROPUESTA.md`), el schema de Task 2, y `docs/mockups/demo.html` como referencia visual.

- [ ] **Step 1: Escribir el documento de handoff**

```markdown
# BACKEND_HANDOFF.md — Guía de arranque para el desarrollador de backend

Bienvenido. Este documento es el punto de entrada único: léelo primero, de aquí te remite a cada documento específico según lo que necesites.

## 1. Qué es este proyecto

Portal Integral de Salud Ocupacional — reemplaza el proceso actual basado en el Excel `10 REPORTE MORBILIDAD 2026 GERENCIA.xlsm` (y los 8 libros satélite que lo alimentan) por una plataforma web con una sola base de datos, un solo modelo de usuarios/permisos, y una sola capa analítica compartida entre dashboard y reportes.

## 2. Cómo arrancar en tu máquina

```bash
git clone <este repositorio>
cd "Dashboard Salud"
npm install
cp .env.example .env
# editar .env: DATABASE_URL debe apuntar a tu Postgres (local vía Docker o el servidor propio ya definido)
docker compose up -d          # levanta Postgres local si no usas el servidor propio todavía
npx prisma migrate dev        # aplica las migraciones
npx prisma db seed            # siembra catálogos reales + usuario admin
npm run dev                   # http://localhost:3000
```
Login de prueba: `admin@saludocupacional.local` / `CambiarEnProduccion123!` — **cambiar en cuanto exista un flujo de gestión de usuarios real**, esta contraseña está en el seed solo para desarrollo.

## 3. Qué ya existe (no lo reconstruyas)

- Proyecto Next.js 14 + TypeScript + Tailwind + shadcn/ui funcionando (`npm run dev`).
- Schema Prisma completo con los 25 modelos del modelo de datos (`prisma/schema.prisma`), migrado y sembrado con los catálogos reales (17 predios, 56 cuentas, 26 causas médicas, etc. — ver `docs/DATA_DICTIONARY.md`).
- Autenticación propia funcionando: login, logout, sesión JWT en cookie httpOnly, middleware de protección de rutas (`src/middleware.ts`), helper `getSession()` (`src/lib/auth/session.ts`).
- Página `/dashboard` de referencia que consulta Prisma en un Server Component — úsala como plantilla del patrón a replicar.
- Docker Compose para Postgres local (`docker-compose.yml`).

## 4. Qué falta construir (tu trabajo)

En orden sugerido (ver `docs/06_PLAN_IMPLEMENTACION.md` Fases 6-17 para el detalle completo):

1. **RBAC real**: implementar `src/lib/auth/permissions.ts` con `requirePermission()` y `scopePredioFilter()` — ver `docs/SECURITY.md` §RBAC.
2. **Services y repositories** por módulo (`src/services/`, `src/repositories/`) — nunca llames a Prisma directo desde un Route Handler ni desde un componente React; ver la capa descrita en `docs/02_ARQUITECTURA_PROPUESTA.md` §4.
3. **Los endpoints listados como "pendiente" en `docs/API.md`** — empieza por `/api/attentions` y `/api/disabilities` (son la base de Morbilidad e Incapacidades, los módulos más centrales).
4. **La regla de evento único**: cuando implementes `/api/accidents` y `/api/maternity`, la creación de la `Disability` asociada debe ir en la misma transacción Prisma (`prisma.$transaction`) — está documentado con código de ejemplo en `docs/API.md` y la razón de negocio en `docs/03_MODELO_DATOS.md` §4 y §8. Esta es la regla más importante de todo el sistema: **un hecho real nunca se captura dos veces**.
5. **Capa analítica** (`src/analytics/`): cálculo de KPIs, comparativos, insights automáticos, detección de anomalías — debe ser el único lugar que calcula estos números, consumido tanto por `/api/dashboard/*` como por `/api/reports` (nunca dos fórmulas distintas para dashboard y PDF — ver `docs/02_ARQUITECTURA_PROPUESTA.md` §6).
6. **Importador Excel** (`/api/imports`) — el patrón de `ImportBatch`/`ImportBatchRow` ya está en el schema; falta el parser y las validaciones. Ver `docs/01_ANALISIS_ACTUAL.md` §5 para los problemas de calidad de datos reales que el parser debe manejar (nomenclatura de predios inconsistente, abreviaturas de mes, etc.).
7. **Reportes PDF/Excel/CSV** — reusar la capa analítica del punto 5.

## 5. Mapa de documentos (cuál leer para qué)

| Necesito... | Leer |
|---|---|
| Entender qué hay en el Excel origen y sus problemas de calidad | `docs/01_ANALISIS_ACTUAL.md` |
| Entender la arquitectura de capas y decisiones de stack | `docs/02_ARQUITECTURA_PROPUESTA.md` |
| Saber el nombre exacto de cada tabla/columna y de dónde viene | `docs/DATA_DICTIONARY.md` + `prisma/schema.prisma` |
| Entender las relaciones y reglas de negocio del modelo (evento único, etc.) | `docs/03_MODELO_DATOS.md` |
| Saber qué endpoint construir y su forma exacta de request/response | `docs/API.md` |
| Implementar permisos, auditoría, protección de datos sensibles | `docs/SECURITY.md` |
| Saber qué pantalla corresponde a qué endpoint | `docs/04_MAPA_MODULOS.md` + `docs/05_DASHBOARD_KPIS.md` |
| Ver cómo se ve la UI final (referencia visual) | `docs/mockups/demo.html` (ábrelo en el navegador) |
| Saber el orden de fases completo del proyecto | `docs/06_PLAN_IMPLEMENTACION.md` |

## 6. Reglas que no debes romper

Copiadas del prompt maestro del proyecto — son la base de todas las decisiones de diseño ya tomadas:

- Un solo portal, una sola autenticación, una sola base de datos, un solo modelo de usuarios, un solo catálogo de predios/cuentas, una sola capa analítica, una sola bitácora.
- Un evento real (accidente, caso de maternidad) existe una sola vez — nunca se duplica captura entre módulos relacionados.
- Nada de predios/años/causas médicas/permisos hardcodeados en código — todo vive en catálogos de base de datos ya sembrados.
- Nunca validar permisos solo en el frontend.
- Dashboard y reportes deben compartir el mismo servicio analítico, nunca fórmulas duplicadas.
- Antes de crear una tabla o endpoint nuevo, pregúntate: ¿este dato ya existe? ¿debo relacionarlo en vez de duplicarlo?

## 7. Contacto / dueño del diseño funcional

José Alemán (usuario de este proyecto) — dueño de las reglas de negocio y prioridades. Ante cualquier ambigüedad no cubierta en estos documentos, es la persona a consultar antes de asumir.
```

- [ ] **Step 2: Commit**

```bash
git add docs/BACKEND_HANDOFF.md
git commit -m "docs: add master handoff guide for backend developer"
```

---

### Task 11: Verificación final end-to-end

**Files:** ninguno nuevo — solo verificación.

**Interfaces:** consume todo lo construido en Tasks 1-10.

- [ ] **Step 1: Levantar el stack completo desde cero (simular la máquina del backend developer)**

```bash
docker compose down -v
docker compose up -d
npx prisma migrate reset --force
npm run dev
```

- [ ] **Step 2: Verificar checklist manual**

- `http://localhost:3000/` redirige a `/login`.
- Login con `admin@saludocupacional.local` / `CambiarEnProduccion123!` funciona y redirige a `/dashboard`.
- `/dashboard` muestra conteos en 0 (correcto, sin datos operativos aún).
- `curl http://localhost:3000/api/health` devuelve `{"status":"ok",...}` sin autenticación.
- `npx prisma studio` muestra los 17 predios, 56 cuentas, 6 roles, 21 permisos sembrados.

- [ ] **Step 3: Correr toda la suite de tests**

```bash
npx vitest run
```
Expected: todos los tests de `src/lib/auth/__tests__/` en verde.

- [ ] **Step 4: Correr el linter y el type-check**

```bash
npm run lint
npx tsc --noEmit
```
Expected: sin errores. Si `tsc` marca errores en el schema generado de Prisma, correr `npx prisma generate` primero.

- [ ] **Step 5: Confirmar que todos los documentos de `docs/` están commiteados**

```bash
git status
git log --oneline -15
```
Expected: working tree limpio, historial con los commits de las Tasks 1-10.

- [ ] **Step 6: Commit final (si quedó algo pendiente)**

```bash
git add -A
git commit -m "chore: final verification pass for Fase 5 skeleton + backend docs"
```
