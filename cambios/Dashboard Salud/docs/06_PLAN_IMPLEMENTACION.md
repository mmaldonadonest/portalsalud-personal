# 06 — Plan de Implementación por Fases

Cada fase produce un entregable verificable antes de avanzar a la siguiente. No se construyen pantallas sin la arquitectura y el modelo de datos aprobados primero (fases 1-4 ya completadas con este set de documentos).

## Fase 1 — Discovery ✅
Análisis completo del Excel. Entregable: `01_ANALISIS_ACTUAL.md`.

## Fase 2 — Arquitectura ✅
Entregable: `02_ARQUITECTURA_PROPUESTA.md`.

## Fase 3 — Datos ✅ (diseño) / pendiente (migraciones)
Entregable de diseño: `03_MODELO_DATOS.md`. Pendiente: traducir a schema Prisma + migraciones ejecutables (se hace al iniciar Fase 5).

## Fase 4 — UX ✅ (spec) / pendiente (mockups visuales)
Entregables: `04_MAPA_MODULOS.md`, `05_DASHBOARD_KPIS.md`. Pendiente en esta misma ronda: mockups HTML navegables del Dashboard Ejecutivo y 1-2 pantallas clave (Incapacidades), solicitados explícitamente por el usuario.

## Fase 5 — Skeleton
- Inicializar proyecto Next.js + TypeScript + Tailwind + shadcn/ui.
- Configurar Prisma + PostgreSQL (Supabase), generar schema desde `03_MODELO_DATOS.md`, primera migración.
- Autenticación (Supabase Auth) + modelo de roles/permisos base (los 6 roles del brief).
- Layout general: sidebar, header, `<DashboardFilter />`, estados genéricos (`loading/empty/error/unauthorized`).
- Seed de catálogos maestros con los valores reales extraídos del Excel (predios, cuentas, causas, etc. — nunca placeholders inventados).

**Criterio de salida**: login funcional, sidebar navegable con permisos aplicados, catálogos poblados y consultables.

## Fase 6 — Dashboard MVP
Orden: Dashboard Ejecutivo → Atenciones → Causas → Incapacidades → Accidentabilidad.
Cada uno con datos reales sembrados (aunque sea un subconjunto de meses) para validar que las gráficas y KPIs son correctos antes de construir los módulos secundarios.

**Criterio de salida**: Dashboard Ejecutivo funcional con datos reales, drill-down operativo a al menos un nivel.

## Fase 7 — Captura
CRUD completo (nuevo/consultar/editar/cancelar-inactivar/exportar) para cada módulo, en este orden de prioridad: Incapacidades → Atenciones → Exámenes Médicos → Accidentabilidad → Antidoping (pruebas + inventario) → Alcoholimetría → Maternidad. Validaciones de duplicados y de periodo cerrado incluidas desde el primer módulo, no añadidas después.

## Fase 8 — Importación
Módulo de importación Excel (`.xlsx/.xlsm/.csv`): parser, preview, validación de formato/catálogos/duplicados, corrección o rechazo por fila, importación transaccional, resultado con indicador de calidad de datos, auditoría. Se prueba explícitamente contra el archivo `10 REPORTE MORBILIDAD 2026 GERENCIA.xlsm` real y, si están disponibles, contra 1-2 de los libros satélite identificados en §2 de `01_ANALISIS_ACTUAL.md`.

## Fase 9 — Reportes
Generador de reportes (Ejecutivo, por predio, morbilidad, incapacidades, accidentabilidad, exámenes médicos, antidoping, maternidad) en PDF/Excel/CSV, server-side.

## Fase 10 — QA
Unit testing (services/analytics), integration testing (API routes + DB), testing de permisos (cada rol contra cada endpoint), testing responsive (1920x1080, 1366x768, tablet), testing de importación (casos válidos, con errores, con duplicados).

## Documentación viva a mantener en `/docs`
`README.md, ARCHITECTURE.md, DATABASE_SCHEMA.md, ER_DIAGRAM.md, API.md, SECURITY.md, DEPLOYMENT.md, USER_ROLES.md, DATA_DICTIONARY.md, EXCEL_MIGRATION.md` — se derivan y expanden de los 6 documentos de diseño una vez iniciada la construcción (Fase 5 en adelante), no se escriben por adelantado como documentos separados duplicando este contenido.

## Próximo paso inmediato
Construir los mockups HTML navegables (Dashboard Ejecutivo + Incapacidades) para validar visualmente el diseño antes de tocar código de producción, luego presentar el conjunto completo de documentos + mockups para aprobación del usuario antes de iniciar Fase 5.
