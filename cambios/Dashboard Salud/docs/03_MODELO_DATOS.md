# 03 — Modelo de Datos Normalizado

> Basado en los catálogos y relaciones reales identificados en `01_ANALISIS_ACTUAL.md`. Todas las tablas incluyen `id` (PK), `created_at`, `updated_at`, `created_by`, `updated_by` salvo que se indique lo contrario. Se omiten estas columnas comunes en los diagramas de detalle por brevedad, pero aplican a toda tabla operativa.

## 1. Principios del modelo

- **Un modelo de hechos por dominio, no una tabla por hoja de Excel.** ATEN + CAUSAS + MUSC-ESQU se funden en `medical_attentions` con dimensiones de causa/lesión. INC IMSS + INC INTER + MATER + accidentes se relacionan bajo `disabilities` con `disability_type` como dimensión, en vez de tres tablas paralelas.
- **Catálogos normalizados, no enums de código.** Predio, causa, cuenta, área, puesto, tipo de examen, etc. son tablas propias con PK numérica, administrables desde UI, nunca un `CHECK` o enum fijo en el esquema (salvo los pocos catálogos verdaderamente cerrados y estables, ver §3).
- **El periodo (año + mes) es un atributo del registro**, nunca una tabla por año. Se usa una tabla `periods` para representar el mes calendario y permitir marcarlo como "cerrado" (control de edición retroactiva).
- **Soft delete** (`deleted_at timestamptz null`) en catálogos y en registros operativos con relevancia legal/auditable; hard delete solo en datos verdaderamente transitorios (ej. borradores de importación no confirmados).
- **Nada de agregados persistidos** salvo que se documente una razón de rendimiento — los totales (`SUM` por predio/mes) se calculan en la capa analítica, no se guardan como columnas duplicadas, evitando el problema de "totales inconsistentes" ya observado en el Excel origen.

## 2. Diagrama entidad-relación (alto nivel)

```
predios ──┬────────────────────────────────────────────┐
          │                                              │
cuentas ──┤                                              │
          │                                              │
agencias ─┤                                              │
          ▼                                              ▼
     employees ──── employee_demographics          medical_attentions ──── attention_causes (catálogo)
          │                                              │
          │                                        musculoskeletal_injuries ──── injury_types (catálogo)
          │
          ├──── medical_exams ──┬── medical_exam_types (catálogo)
          │                     └── medical_exam_results (catálogo)
          │
          ├──── disabilities ──┬── disability_types (catálogo)
          │                    └── disability_costs (1:1 opcional, costo del caso)
          │
          ├──── accidents ──┬── accident_types (catálogo: laboral/trayecto)
          │                 └── accident_causes (catálogo: vial/agresión/caída/...)
          │
          ├──── drug_tests ──┬── drug_test_types (catálogo: antidoping/alcoholemia)
          │                  └── drug_test_results (catálogo: negativo/positivo)
          │
          └──── maternity_cases

drug_test_inventory ──── drug_test_batches (lotes con caducidad)

periods (año+mes, control de cierre)
audit_logs (bitácora transversal, referencia polimórfica a cualquier tabla)
attachments (adjuntos, referencia polimórfica)

users ──── roles ──── permissions
users ──── user_predio_access ──── predios (alcance por predio para Gerencia de Predio)
```

## 3. Catálogos maestros (administrables desde UI, sección Administración)

Todos con forma común: `id, codigo (único, opcional), nombre, activo (bool), orden (int, para UI), created_at, updated_at`.

| Tabla | Origen en el Excel | Notas |
|---|---|---|
| `predios` | Predio (17 valores + alias) | Incluye tabla `predio_aliases (predio_id, alias)` para resolver las abreviaturas detectadas en ABAST ANTDP (M1, M2, TULTI, UT, WP, Z VALL, FOR) durante importación |
| `cuentas` | Cuenta/cliente (56 valores) | |
| `agencias` | Agencia (GLI, ORLAMEX, VALLE LERMA, ONEST, OTRO) | |
| `areas` | Área (19 valores) | |
| `puestos` | Puesto (~36 valores) | |
| `attention_causes` | Causas médicas (26 valores) | Campo `categoria` opcional para agrupar en dashboards (ej. "respiratorio", "musculoesquelético") |
| `injury_types` | Tipos de lesión músculo-esquelética (15 valores) | Campo `region_corporal` (MT/MP/cabeza/tronco) derivado de la nomenclatura observada |
| `medical_exam_types` | Nuevo ingreso, periódico, pos-incapacidad, pretest | |
| `medical_exam_results` | Apto, no apto, condicionado, inclusión | |
| `disability_types` | Enfermedad general, maternidad, accidente laboral, accidente trayecto, interna | |
| `accident_types` | Laboral, trayecto | |
| `accident_causes` | Vial, agresión, caída, golpe, herida, ergonómica, improcedente | |
| `accident_statuses` | Pendiente, calificado, improcedente, baja inconcluso | |
| `drug_test_types` | Antidoping, alcoholemia | |
| `drug_test_results` | Negativo, positivo | |
| `drug_test_statuses` | N/A, CAPA, no interesado/baja, no contratado, recaída | |
| `age_ranges` | 18-25, 26-35, 36-45, 46-55, 55+ | Rango numérico (`edad_min`, `edad_max nullable`) para poder recalcular si cambian los cortes |
| `risk_thresholds` | (no existe en Excel — nuevo) | Umbrales configurables del semáforo de riesgo por predio y de alertas de caducidad de antidoping, en vez de hardcodear "≤30/60/90 días" |

## 4. Entidades operativas principales

### `employees`
```
id, predio_id FK, numero_empleado (único, nullable si aún no hay integración RH),
nombre, genero, fecha_nacimiento (para derivar edad_range dinámicamente, no almacenar edad fija),
puesto_id FK, area_id FK, cuenta_id FK, agencia_id FK nullable,
fecha_ingreso, activo, codigo_externo_rh nullable (para integración futura),
deleted_at
```
Nota: la edad se **calcula** a partir de `fecha_nacimiento` en el momento de la consulta (dimensión derivada), nunca se almacena como rango fijo — evita que un colaborador quede "congelado" en un rango de edad incorrecto con el paso del tiempo.

### `medical_attentions` (fusiona ATEN + CAUSAS + MUSC-ESQU)
```
id, employee_id FK nullable (permite captura agregada sin identificar persona, ver nota),
predio_id FK, period_id FK (año+mes), fecha_atencion,
attention_cause_id FK nullable, injury_type_id FK nullable,
es_personal_inclusion (bool),
observaciones,
deleted_at
```
Nota: `attention_cause_id` y `injury_type_id` son mutuamente informativos según si la atención es una causa médica general o una lesión musculoesquelética — se valida en `services/` que al menos uno esté presente. Se permite `employee_id` nulo para dar soporte a captura agregada por predio/mes (compatibilidad con el nivel de detalle que tenía el Excel), pero el diseño de captura nueva debe preferir siempre registrar el empleado cuando esté disponible, habilitando drill-down real a nivel individual.

### `medical_exams`
```
id, employee_id FK, predio_id FK, period_id FK, fecha_examen,
medical_exam_type_id FK, medical_exam_result_id FK,
observaciones, deleted_at
```

### `disabilities` (fusiona INCAP + INC IMSS + INC INTER + parte de MATER)
```
id, employee_id FK, predio_id FK, cuenta_id FK nullable, area_id FK nullable,
period_id FK (mes de inicio),
disability_type_id FK, fecha_inicio, fecha_fin nullable,
dias_incapacidad (int, o calculado de fecha_fin - fecha_inicio si ambas existen),
horas_no_trabajadas (calculado: dias * 8, no almacenado — ver §1),
folio_imss nullable, es_interna (bool),
origen_tipo (enum: manual | accidente | maternidad) default 'manual',
origen_id (FK nullable, apunta a accidents.id o maternity_cases.id según origen_tipo),
deleted_at
```
**Regla de no-duplicación (evento único)**: cuando una incapacidad es consecuencia de un accidente o de un caso de maternidad, `origen_tipo`/`origen_id` la vinculan a su hecho generador — la captura ocurre una sola vez en el módulo origen (`accidents` o `maternity_cases`) y el caso de incapacidad se crea o enlaza automáticamente desde ahí, nunca se vuelve a teclear el folio/fechas/días en un formulario de incapacidades independiente. Los servicios de captura de `accidents` y `maternity_cases` son los únicos puntos de entrada autorizados para crear una `disability` con ese origen; la UI de "Nuevo caso" en `/incapacidades` solo se usa para `origen_tipo = manual` (enfermedad general, IMSS, interna sin accidente asociado).

### `disability_costs`
```
id, disability_id FK único, monto, moneda (default MXN), fecha_registro
```
Tabla separada para aislar el dato monetario (sensible/auditable) del caso clínico, permitiendo permisos distintos sobre el costo vs. la información médica.

### `accidents`
```
id, employee_id FK, predio_id FK, cuenta_id FK nullable, area_id FK nullable, puesto_id FK nullable,
period_id FK, fecha_accidente,
accident_type_id FK, accident_cause_id FK, accident_status_id FK,
genero (redundante de employee pero capturable si el empleado no está identificado),
genera_incapacidad (bool),
dias_perdidos nullable,
costo_calificado nullable, costo_improcedente nullable,
deleted_at
```
Cuando `genera_incapacidad = true`, el formulario de captura de accidente incluye los campos mínimos de la incapacidad asociada (fecha_inicio, dias, folio_imss si aplica) y el `service` de accidentes crea en la misma transacción el registro `disabilities` con `origen_tipo = 'accidente'` y `origen_id = accidents.id` — el usuario nunca abre por separado el formulario de "Nuevo caso" de incapacidades para este escenario. `dias_perdidos` en `accidents` es una copia de lectura rápida (denormalización documentada por rendimiento del ranking de accidentabilidad); el valor de verdad vive en `disabilities.dias_incapacidad` del registro enlazado.

### `drug_tests`
```
id, employee_id FK, predio_id FK, cuenta_id FK nullable, area_id FK nullable, puesto_id FK nullable,
agencia_id FK nullable, period_id FK, fecha_prueba,
drug_test_type_id FK, drug_test_result_id FK nullable, drug_test_status_id FK nullable,
deleted_at
```

### `drug_test_inventory` / `drug_test_batches`
```
drug_test_batches: id, lote, fecha_caducidad, cantidad_inicial, predio_id FK, activo
drug_test_inventory_movements: id, batch_id FK, tipo (entrada|consumo), cantidad, fecha, predio_id FK, referencia nullable
```
El stock disponible se calcula (`SUM` de movimientos por lote), no se persiste — se recalcula bajo demanda o se cachea con job programado si el volumen lo justifica (documentar si se activa ese caso).

### `maternity_cases`
```
id, employee_id FK, predio_id FK, cuenta_id FK nullable, period_id FK,
fecha_inicio_incapacidad, fecha_probable_parto, dias_incapacidad, estatus,
deleted_at
```
Se mantiene como tabla propia (distinta de una fila más de `disabilities`) porque captura atributos exclusivos del caso clínico de maternidad (`fecha_probable_parto`, `estatus` de seguimiento) que no aplican a otros tipos de incapacidad, siguiendo el mismo eje de captura que ya tenía el Excel. Sigue el mismo patrón de no-duplicación que `accidents`: al registrarse el caso, el `service` de maternidad crea en la misma transacción el registro `disabilities` correspondiente con `origen_tipo = 'maternidad'` y `origen_id = maternity_cases.id` — nunca se captura el mismo caso dos veces. Todo KPI y reporte de "incapacidades" (incluida la segmentación por tipo del Dashboard de Incapacidades) lee siempre de `disabilities`, nunca suma `maternity_cases` por separado, evitando doble conteo.

## 5. Entidades transversales

### `periods`
```
id, anio (int), mes (int, 1-12), cerrado (bool), fecha_cierre nullable
```
Único por `(anio, mes)`. Toda tabla operativa referencia `period_id` en vez de columnas `anio`/`mes` sueltas, centralizando el control de "periodo cerrado" (no editable sin permiso elevado).

### `users`, `roles`, `permissions`
```
users: id, email (único), nombre, role_id FK, activo, ultimo_acceso, deleted_at
roles: id, nombre (Administrador, Salud Ocupacional, Gerencia Salud, Gerencia de Predio, Dirección, Consulta), descripcion
permissions: id, codigo (ej. "disabilities.write", "reports.export"), descripcion, nivel_sensibilidad (operativo|agregado)
role_permissions: role_id FK, permission_id FK
user_predio_access: user_id FK, predio_id FK   -- acota el alcance de roles tipo "Gerencia de Predio"
```

### `audit_logs`
```
id, user_id FK, accion (create|update|delete|export|import|login),
modulo, entidad, entidad_id, valor_anterior (jsonb nullable), valor_nuevo (jsonb nullable),
ip_address nullable, fecha_hora
```
Nunca almacena datos médicos sensibles en texto libre innecesario — `valor_anterior`/`valor_nuevo` se limitan a los campos que cambiaron, no un dump completo si contiene observaciones clínicas extensas (se trunca/omite ese campo específico y se marca `contiene_datos_sensibles: true`).

### `attachments`
```
id, entidad, entidad_id (referencia polimórfica), nombre_archivo, url_storage, tipo_mime, tamano_bytes, subido_por FK
```

### `import_batches` (soporte al importador Excel, fase 8)
```
id, archivo_nombre, usuario_id FK, fecha_importacion, estado (preview|confirmado|rechazado),
registros_totales, registros_correctos, registros_advertencia, registros_rechazados,
detalle_errores (jsonb)
```
```
import_batch_rows: id, import_batch_id FK, fila_origen (int), entidad_destino, payload (jsonb),
estado (pendiente|importado|rechazado|duplicado), motivo_rechazo nullable
```

## 6. Índices y constraints clave

- `UNIQUE (anio, mes)` en `periods`.
- `UNIQUE (predio_id, alias)` en `predio_aliases`.
- Índices compuestos `(predio_id, period_id)` en cada tabla de hechos (`medical_attentions`, `disabilities`, `accidents`, `drug_tests`, `medical_exams`) — patrón de consulta dominante en todos los dashboards (filtro por predio + periodo).
- `CHECK (dias_incapacidad >= 0)` y constraints equivalentes contra los valores negativos inválidos observados en costos de accidentes (`costo_improcedente` permite negativo solo si se confirma con negocio que representa un ajuste; de lo contrario se agrega `CHECK (costo_improcedente >= 0)`).
- FK con `ON DELETE RESTRICT` en catálogos referenciados por tablas de hechos (no se puede borrar un predio con atenciones asociadas; se usa `activo = false` en su lugar — soft delete de catálogo).

## 7. Vistas analíticas (capa de solo lectura para dashboards ejecutivos)

No son tablas nuevas de captura, sino vistas SQL (o materializadas si el volumen lo requiere, documentado si se activa):
- `vw_kpi_mensual_predio`: agregados por predio+mes de atenciones/incapacidades/accidentes/exámenes, consumida por el Dashboard Ejecutivo.
- `vw_morbilidad_por_predio`: ranking con semáforo de riesgo (calculado contra `risk_thresholds`).
- `vw_ejecutivo_agregado`: vista sin campos identificables de empleado, para roles Dirección/Gerencia Salud, cumpliendo la separación de §23 del brief funcional (información ejecutiva vs. operativa).

## 8. Regla de evento único (no-duplicación) — resumen transversal

Un hecho real (un accidente, un caso de maternidad) existe una sola vez en su tabla de origen. Cuando ese hecho genera una incapacidad, el registro en `disabilities` se crea automáticamente desde el `service` del módulo origen (`accidents` o `maternity_cases`) dentro de la misma transacción, enlazado vía `origen_tipo`/`origen_id` — nunca se vuelve a capturar manualmente en `/incapacidades`. Todo KPI, ranking, tendencia y reporte de incapacidades lee siempre de `disabilities` como fuente única de días/costo, evitando doble conteo entre módulos. Este patrón se replica para cualquier relación evento→consecuencia que se agregue en el futuro (ej. si se modela una lesión musculoesquelética que deriva en incapacidad).

## 9. Puntos pendientes de validación con negocio (no inventados, marcados explícitamente)

1. Si los valores negativos en `costo_improcedente` de accidentes son ajustes legítimos o error de captura histórico (definirá si se agrega el `CHECK` o no).
2. Si la plataforma debe permitir captura sin `employee_id` (compatibilidad con el nivel de agregación actual del Excel) de forma permanente, o solo durante la migración inicial mientras se completa el catálogo de empleados.
