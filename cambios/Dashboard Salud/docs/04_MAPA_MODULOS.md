# 04 — Mapa de Módulos y Navegación

## 1. Sitemap

```
/                                   → redirige a /dashboard
/login                              → autenticación

/dashboard                          → Dashboard Ejecutivo (vista consolidada)
/dashboard/gerencial                → Vista Gerencial (10 preguntas clave, sin tablas operativas)

/morbilidad/atenciones               → listado + captura + KPIs de atenciones
/morbilidad/atenciones/[id]           → detalle / drill-down
/morbilidad/causas                    → ranking y análisis por causa
/morbilidad/musculoesqueleticas       → análisis de lesiones MT/MP

/examenes-medicos                     → dashboard + listado
/examenes-medicos/nuevo                → captura
/examenes-medicos/[id]                  → detalle

/incapacidades                          → dashboard general (todos los tipos)
/incapacidades/imss                      → detalle IMSS
/incapacidades/internas                   → detalle internas
/incapacidades/[id]                        → detalle de caso + costo

/accidentabilidad                           → dashboard + listado
/accidentabilidad/[id]                       → detalle de caso

/antidoping/pruebas                           → dashboard + listado + captura
/antidoping/inventario                         → control de lotes + alertas de caducidad
/antidoping/inventario/movimientos              → entradas/consumos

/alcoholimetria                                  → dashboard + listado

/maternidad                                       → dashboard + listado + captura

/reportes                                          → generador de reportes (PDF/Excel/CSV)

/administracion/predios                             → CRUD catálogo
/administracion/cuentas                              → CRUD catálogo
/administracion/catalogos                             → CRUD de todos los catálogos secundarios (causas, puestos, áreas, etc.)
/administracion/umbrales                               → configuración de semáforo de riesgo y alertas
/administracion/usuarios                                → gestión de usuarios
/administracion/permisos                                 → gestión de roles y permisos
/administracion/importar                                  → importador Excel (preview, validación, confirmación)

/auditoria                                                 → bitácora (solo roles autorizados)

/perfil                                                     → datos de la cuenta propia
```

## 2. Sidebar (estructura de navegación)

```
Inicio

Dashboard Ejecutivo
Vista Gerencial

Morbilidad
  Atenciones
  Causas
  Musculoesqueléticas

Exámenes Médicos

Incapacidades
  General
  IMSS
  Internas

Accidentabilidad

Antidoping
  Pruebas
  Inventario

Alcoholimetría

Maternidad

Reportes

Administración          (visible solo con permiso administrativo)
  Predios
  Cuentas
  Catálogos
  Umbrales de Riesgo
  Usuarios
  Permisos
  Importar Información

Auditoría                (visible solo con permiso de auditoría)
```

La visibilidad de cada ítem se resuelve dinámicamente contra los permisos del usuario (no hay listas hardcodeadas de roles en el componente de sidebar — consulta `role_permissions` vía el contexto de sesión).

## 3. User flows principales

### Flujo: Gerencia consulta el estado de morbilidad
```
Login → Dashboard Ejecutivo (KPIs + tendencia + top causas + ranking predios)
  → clic en predio con semáforo "Alto" → Morbilidad / Atenciones filtrado por ese predio
  → clic en causa top 1 → drill-down a registros de esa causa en ese predio/periodo
  → breadcrumb: Dashboard > Atenciones > Predio X > Causa Y > Registros
```

### Flujo: Salud Ocupacional captura una incapacidad
```
Login → Incapacidades → Nuevo
  → formulario (empleado, predio, tipo, fechas, folio IMSS si aplica)
  → validación en vivo (empleado existe, fechas coherentes, periodo no cerrado)
  → guardar → registro de auditoría automático → redirección a detalle del caso
```

### Flujo: Administrador importa datos históricos desde Excel
```
Administración > Importar → subir archivo (.xlsx/.xlsm/.csv)
  → sistema detecta hoja/estructura → preview de filas detectadas
  → sistema marca filas con error/duplicado
  → usuario corrige o excluye filas marcadas
  → confirmar importación → transacción → resultado (correctos/advertencia/rechazados) → auditoría
```

### Flujo: Control de vencimiento de antidoping
```
Antidoping > Inventario → alerta visible si algún lote caduca ≤30/60/90 días (umbral configurado en Administración > Umbrales)
  → clic en alerta → detalle de lote → registrar movimiento de consumo o baja
```

## 4. Componentes reutilizables (`src/components/`)

| Componente | Responsabilidad |
|---|---|
| `<KpiCard />` | Tarjeta de indicador con valor, variación y tendencia |
| `<DashboardFilter />` | Barra de filtros globales (año, mes/periodo, predio, cuenta, tipo de indicador) |
| `<TrendChart />` | Línea de tendencia mensual, soporta comparativo año actual/anterior |
| `<RankingChart />` | Ranking horizontal (causas, predios) con top N configurable |
| `<DataTable />` | Tabla con paginación server-side, ordenamiento, exportación |
| `<ExportButton />` | Disparador de exportación PDF/Excel/CSV, respeta permisos de sensibilidad |
| `<PeriodSelector />` | Selector de año/mes, consulta `periods` para marcar meses cerrados |
| `<PredioSelector />` | Selector de predio(s), consulta catálogo, respeta `user_predio_access` |
| `<StatusBadge />` | Badge de estado (apto/no apto, pendiente/calificado, etc.) |
| `<RiskIndicator />` | Semáforo de riesgo (bajo/medio/alto/crítico) contra `risk_thresholds` |
| `<EmptyState />` | Estado vacío estandarizado |
| `<LoadingState />` | Estado de carga estandarizado (skeleton) |
| `<ErrorState />` | Estado de error estandarizado, con reintento |
| `<UnauthorizedState />` | Estado de acceso denegado (403) |
| `<Breadcrumbs />` | Navegación de drill-down |

Cada módulo funcional (`src/features/<modulo>/`) consume estos componentes genéricos y solo añade la composición y lógica específica del dominio — nunca reimplementa una tabla, filtro o tarjeta de KPI desde cero.
