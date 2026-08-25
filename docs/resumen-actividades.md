# Portal Salud — Resumen ejecutivo de actividades

Trabajo entregado a partir del análisis de `salud-ocupacional-v2` (13 de agosto) hasta hoy.

**Periodo:** 13–24 de agosto de 2026 · **30 de 33 tareas cerradas** · **204h (25.5 días) de desarrollo**

---

## Dashboard ejecutivo de KPIs

- Backend: Incapacidades, Accidentabilidad, Morbilidad/Consulta, Antidoping, Examen + historial, Resumen General — 44h
- UI con gráficas (ApexCharts) + desgloses por Género, Edad y Cuenta — 20h

## Expediente clínico

- Trabajos previos en Examen, fuente IMSS/interna en Incapacidades, CUENTA visible en expediente — 16h
- Contactos de emergencia en Examen — 8h
- Catálogo de diagnósticos normalizado (ICD/CIE obligatorio + diagnósticos secundarios) — 6h
- Catálogo de restricciones médicas (16 códigos fijos + historial de asignaciones por NSS) — 11h
- Causa de consulta médica — catálogo cerrado de 23 causas + "Otro", administrable — 9h

## Antidoping

- Backend Oracle/ORDS + módulo Java — 20h
- Consumibles (inventario por predio/mes) — 12h
- Selección aleatoria de personal + historial persistido — 11h

## Accidentes de trabajo

- Backend Oracle/ORDS + módulo Java — 20h
- Seguimiento de casos (historial, cierre, evidencias) — 9h

## Maternidad y reglas de NSS

- Maternidad — seguimiento completo con validación de sexo — 8h
- Reglas de NSS — persona asociada a varios predios (multipredio) — 6h

## Datos y catálogos técnicos

- Migración histórica local (21,448 archivos + 550,560 registros EAV)
- Numeración de carta dental y diccionario de siglas del Pretest, confirmados fieles al sistema real

---

## Pendiente

- Matriz de permisos y confidencialidad — código construido, falta confirmar prueba de humo — 18h
- Generación de ambiente de QA (server web, JDK, WAR, dominio) — sin iniciar — 5h
- Migración histórica — ETL Oracle, a cargo de equipo externo

---

**Hecho:** 204h (25.5 días) · **Pendiente:** 23h (2.875 días) · **Suma total:** 227h (28.375 días)

Detalle completo: `docs/plan-tareas-concretas.html`
