# 05 — Especificación de Dashboards y KPIs

## 1. Filtros globales (aplican a todo dashboard)

`Año | Mes/Periodo | Predio (todos o específico) | Cuenta | Tipo de indicador` — un cambio en cualquier filtro global recalcula todos los KPIs y gráficas de la página simultáneamente (estado compartido vía contexto/URL query params, no estado duplicado por componente).

## 2. Dashboard Ejecutivo (`/dashboard`)

**Encabezado**: "Morbilidad y Salud Ocupacional" · **Subtítulo**: "Dashboard Ejecutivo | Salud Ocupacional"

### Fila de KPIs
| KPI | Detalle mostrado | Fuente |
|---|---|---|
| Atenciones médicas | Total del periodo, variación vs. mes anterior, variación vs. mismo periodo año anterior | `medical_attentions` |
| Personas incapacitadas | Número, variación, % sobre headcount si existe | `disabilities` + `employees` |
| Días de incapacidad | Días acumulados, promedio por caso | `disabilities` |
| Accidentes | Total, laborales, trayecto | `accidents` |
| Exámenes médicos | Total, aptos, no aptos, condicionados | `medical_exams` |
| Costo de incapacidades | Periodo, acumulado anual, variación | `disability_costs` |

### Evolución mensual de atenciones
Línea de tendencia, eje X = ENE..DIC, eje Y = número de atenciones. Toggle: año actual / año anterior / comparativo / acumulado.

### Principales causas de atención
Ranking Top 10 con número, porcentaje, variación y predio de mayor incidencia. Drill-down a registros.

### Morbilidad por predio
Ranking descendente: predio, atenciones, incapacidades, días incapacitantes, accidentes, costo — con `<RiskIndicator />` (bajo/medio/alto/crítico) contra umbrales configurables.

### Análisis Ejecutivo (insights automáticos)
Sección de texto generado por la capa analítica, ej.: "Las atenciones médicas aumentaron 18% respecto al mes anterior.", "Macro II concentra 23% de las incapacidades registradas.". Reglas: solo se muestran insights calculados de datos reales del periodo filtrado; nunca texto genérico o inventado.

## 3. Vista Gerencial (`/dashboard/gerencial`)

Diseñada para responder directamente las 10 preguntas del brief funcional (§17), sin tablas operativas — solo KPIs, tendencia, ranking e insights, con densidad visual reducida respecto al Dashboard Ejecutivo (menos elementos por pantalla, tipografía mayor). No repite el detalle de `/dashboard`; es una capa de resumen aún más alta, pensada para lectura en <2 minutos.

## 4. Dashboard de Incapacidades (`/incapacidades`)

**KPIs**: total de casos, personas incapacitadas, días acumulados, horas no trabajadas, costo, promedio de días.
**Segmentación**: enfermedad general, maternidad, accidente laboral, accidente de trayecto, interna (gráfica de barras apiladas o donut).
**Tendencia mensual** + **ranking por predio**.

## 5. Dashboard de Accidentabilidad (`/accidentabilidad`)

**KPIs**: accidentes totales, laborales, trayecto, enfermedad profesional, días perdidos.
**Filtros adicionales**: predio, mes, cuenta, género, tipo de riesgo.
**Gráficas**: tendencia mensual, ranking por causa de RT (vial/agresión/caída/golpe/herida/ergonómica), distribución por status (pendiente/calificado/improcedente/baja inconcluso).

## 6. Dashboard de Exámenes Médicos (`/examenes-medicos`)

**KPIs**: total, aptos, no aptos, condicionados, inclusión.
**Segmentación**: ingreso / periódico / pos-incapacidad (tabs o filtro).
**Gráficas**: porcentajes de resultado, evolución mensual por tipo de examen.

## 7. Dashboard de Antidoping (`/antidoping/pruebas`)

**KPIs**: pruebas realizadas, por predio, por resultado (negativo/positivo).
**Distribuciones**: edad, género, agencia, cuenta.

### Inventario (`/antidoping/inventario`)
**KPIs**: existencia, entradas, consumo, disponible, por lote y caducidad.
**Alertas** (contra `risk_thresholds` configurable): caduca ≤90 días, ≤60 días, ≤30 días, caducado, stock mínimo — mostradas como banner/badge, nunca silenciosas.

## 8. Dashboard de Maternidad (`/maternidad`)

**KPIs**: casos activos, casos acumulados, días incapacitantes.
**Distribuciones**: edad, predio, cuenta, tendencia mensual.

## 9. Drill-down (aplica a todo KPI/gráfica relevante)

```
Dashboard → Incapacidades → Predio Macro II → Enfermedad general → Registros
```
Implementado con `<Breadcrumbs />` y query params encadenados; cada nivel es una URL navegable (compartible/bookmarkeable), no un modal que pierde estado al refrescar.

## 10. Comparativos disponibles (transversal a todos los dashboards)

Mes vs. mes anterior · Mes vs. mismo mes año anterior · YTD vs. YTD año anterior · Predio vs. promedio corporativo · Predio vs. predio · Año vs. año. Se exponen como un selector de "modo de comparación" reutilizado por `<TrendChart />` y `<KpiCard />`, no reimplementado por dashboard.

## 11. Calidad de datos (`/administracion/importar` y widget en Dashboard Ejecutivo)

Indicador "Calidad de Datos": registros procesados, correctos, con advertencia, rechazados — visible tanto en el resultado de cada importación como, agregado, en un widget opcional del Dashboard Ejecutivo para el periodo activo.
