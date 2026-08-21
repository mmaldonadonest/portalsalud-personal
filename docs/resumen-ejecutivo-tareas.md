# Resumen ejecutivo — lista de tareas (horas)

Solo tarea, tipo, horas y estado — fuente lista para pasar a un plan de trabajo. Detalle completo (WS, bloqueadores, hallazgos) en `docs/plan-tareas-concretas.html`. Actualizado con la propuesta formal de decisiones de negocio (`docs/ONEST_Proyecto_Salud_Ocupacional_Decisiones.pdf`) y el entregable de liberación de stoppers (`docs/entregable-liberacion-stoppers-salud.html`): ya no queda ninguna tarea Bloqueada ni sin dimensionar.

*Portal Salud · Salud Ocupacional — 20 de agosto de 2026*

| # | Tarea | Tipo | Horas | Estado |
|---|---|---|---|---|
| 1 | Cerrar «trabajos previos» en Examen | Programación | 8h | ✅ Hecho |
| 2 | Fuente IMSS/interna en Incapacidades | Programación | 4h | ✅ Hecho |
| 3 | Contactos de emergencia en Examen | Programación | 8h | ✅ Hecho |
| 4 | Mostrar CUENTA en el expediente | Programación | 4h | ✅ Hecho |
| 5 | Dashboard KPIs — backend Incapacidades | Programación | 8h | ✅ Hecho |
| 6 | Dashboard KPIs — backend Accidentabilidad | Programación | 4h | ✅ Hecho |
| 7 | Dashboard KPIs — backend Morbilidad/Consulta | Programación | 4h | ✅ Hecho |
| 8 | Dashboard KPIs — backend Antidoping | Programación | 4h | ✅ Hecho |
| 9 | Dashboard KPIs — historial de Examen | Programación | 8h | ✅ Hecho |
| 10 | Dashboard KPIs — backend Examen | Programación | 8h | ✅ Hecho |
| 11 | Dashboard KPIs — Resumen General | Programación | 4h | ✅ Hecho |
| 12 | Dashboard KPIs — UI (gráficas) | Programación | 4h | ✅ Hecho |
| 13 | Consumibles (antidoping) — backend + módulo Java | Programación | 12h | ✅ Hecho |
| 14 | Dashboard — desglose por Género | Programación | 8h | ✅ Hecho |
| 15 | Dashboard — desglose por Edad | Programación | 8h | ✅ Hecho |
| 16 | Dashboard — desglose por Cuenta | Programación | 8h | ✅ Hecho |
| 17 | Antidoping — backend Oracle/ORDS | Programación | 8h | ✅ Hecho |
| 18 | Antidoping — módulo Java | Programación | 12h | ✅ Hecho |
| 19 | Antidoping — selección aleatoria | Programación | 8h | ⏳ Pendiente |
| 20 | Accidentes de trabajo — backend Oracle/ORDS | Programación | 8h | ✅ Hecho |
| 21 | Accidentes de trabajo — módulo Java | Programación | 12h | ✅ Hecho |
| 22 | Accidentes — seguimiento de casos | Programación | 8h | ⏳ Pendiente |
| 23 | Maternidad — seguimiento completo | Programación | 8h | ⏳ Pendiente |
| 24 | Catálogo de diagnósticos normalizado | Programación | 6h | ✅ Hecho |
| 25 | Reglas de NSS (persona multipredio) | Programación | 8h | ⏳ Pendiente |
| 26 | Catálogo de restricciones médicas | Programación | 11h | ⏳ Pendiente |
| 27 | Matriz de permisos y confidencialidad | Programación | 18h | ⏳ Pendiente |
| 28 | Migración histórica — local | Programación | — | ✅ Hecho |
| 29 | Migración histórica — con ETL Oracle (equipo externo) | Proceso lógico | — | ⏳ Pendiente |
| 30 | Numeración de carta dental | Proceso lógico | — | ✅ Hecho |
| 31 | Diccionario de siglas del Pretest | Programación | — | ✅ Hecho |
| 32 | Generación de ambiente de QA (server web, JDK, WAR, dominio) | Programación | 5h | ⏳ Pendiente |
| 33 | Causa de consulta médica — catálogo cerrado (revertido) | Programación | 10h | ⏳ Pendiente |

## ✓ Ya todo dimensionado

`docs/entregable-liberacion-stoppers-salud.html` (20 de agosto) redujo y dimensionó las 3 tareas que quedaban sin estimar: Antidoping → selección aleatoria (sin protocolo clínico complejo), Accidentes → solo seguimiento (sin motor de investigación), Reglas de NSS → confirmado con casos de prueba concretos. 8h cada una — se construyen sobre módulos ya existentes, no son dominio nuevo. Ya no queda ninguna fila sin estimar.

## Totales

- **Pendiente:** 76h — todo dimensionado (9.5 días)
- **Bloqueada:** 0h (0 días)
- **Hecho:** 150h (18.75 días)
- **Suma total:** 226h ÷ 8h/día = **28.25 días**

## Avance real vs. estimado

18 días de trabajo estimado (144h) se completaron en solo **7 días reales de calendario** (13–20 de agosto) — ritmo ≈2.6× más rápido que el estimado por jornada, gracias a 1 desarrollador Java senior + este agente de IA trabajando en paralelo.

```
Día 1 (13 ago, llega salud-ocupacional-v2)     7 días reales     Día 18 (estimado Hecho)
[████████████████████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░]
 0%                                38.9%                                          100%
```

---
Detalle completo: `docs/plan-tareas-concretas.html`
