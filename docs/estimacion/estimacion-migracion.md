# Migración PHP → Java · Estimación Ejecutiva

**Proyecto:** Portal Salud — Migración sistema médico legacy (PHP → Java Spring Boot, MariaDB → Oracle 21c)
**Fecha:** 2026-07-01
**Escenario base:** Tiempo completo (~6–8 h/día útiles) · **limitado por esperas externas**
**Fuentes:** `docs/Contexto.txt`, `plan/valoracion-agente-v2.json`, `plan/plan.csv`

---

## Resumen

| Indicador | Valor |
|---|---|
| **Total calendario (tiempo completo)** | **~3 semanas** (limitado por esperas externas, no por codificación) |
| Esfuerzo humano acumulado | ~100 h (≈ 14–15 días útiles a 6–8 h/día) |
| Escenario parcial (2–4 h/día) | 4 – 6 semanas · mismas ~100 h |
| Baseline solo humano (sin agente) | 9 – 12 semanas · 301 h |
| Unidades de trabajo | 11 (U01–U11) · 20 endpoints REST |
| Estado actual | **U01 iniciada** (flujo NSS `searchEmploye` + WS ORDS ya implementado) |

> **Hallazgo que destraba el programa:** el riesgo #1 del proyecto era *"API externa NSS no
> documentada — Step 0 bloqueante"*. Ya quedó resuelto: `app.php` documenta **todos** los WS ORDS
> (`http://10.249.249.3/biows/ords/security/...`) y **todos los módulos usan el mismo patrón**.
> Esto reduce el riesgo de U03–U08.

> **Por qué ~3 semanas y no menos:** a tiempo completo la codificación deja de ser el cuello de
> botella. El piso de ~3 semanas lo fija la **ruta crítica externa**: acceso de red al WS ORDS,
> validación de negocio, y E2E/UAT con usuarios clave. Reducir esas esperas es lo único que baja
> el calendario por debajo de 3 semanas.

---

## Desglose por caso de uso

*(Orden de la tabla = orden de ejecución recomendado. "Días útiles" = esfuerzo a 6–8 h/día)*

| ID | Caso de uso | Endpoints PHP | Complejidad | Semáforo | Días útiles ~ | ~h humano | Estado |
|----|-------------|---------------|:---:|:---:|:---:|:---:|---|
| **U01** | Flujo entrada NSS (searchEmploye + validateModules) | `searchEmploye`, `validateModules` | Media | ROJO | ~1 d | ~8 h | ~40% hecho |
| **U02** | Menú dinámico módulos 2..11 + permisos | render menú, `verificarPermisoExa` | Alta | ROJO | ~1.5 d | ~10 h | Pendiente |
| **U09** | Migración datos files/tags → Oracle 21c (ETL) | DDL + ETL + dedup | Media | ROJO | ~1 d | ~8 h | Pendiente |
| **U04** | Expediente general + consultas | `expediente`, `expedienteShowConsults`, `expedienteArch`, `consultaMedica` | Alta | ROJO | ~1.5 d | ~10 h | Pendiente |
| **U05** | Incapacidades + archivo | `incapacidades`, `viewNotaIncap`, `expFileNotaIncp` | Alta | ROJO | ~1.5 d | ~10 h | Pendiente |
| **U06** | Notas e históricos PDF | `viexExpExMedPdf`, `vieNotaMedPdf`, `expeFileNotaMedPdf`, `expedienteFilePrint` | Alta | ROJO | ~1.5 d | ~10 h | Pendiente |
| **U07** | Examen médico + navegación in-page (`irAContenido`) | `viewExpHeroFam`, `viewTypeExped` | **Muy Alta** | ROJO | **~2 d** | ~13 h | Pendiente |
| **U03** | Pre-Test y Checklist | `pretest`, `checklist` | Media | AMARILLO | ~1 d | ~8 h | Pendiente (bug) |
| **U08** | Compatibilidad auth JAVA/LEGACY_PHP/HYBRID | auth strategy + adapter | Media | ROJO | ~1 d | ~8 h | Pendiente |
| **U11** | Hardening (headers, CSS, observabilidad) | transversal | Baja | AMARILLO | ~0.5 d | ~5 h | Pendiente |
| **U10** | QA integral (contract tests, E2E, UAT) | transversal | Alta | ROJO | ~1.5 d | ~10 h | Pendiente |
| | | | | | **Σ ~14–15 días útiles** | **~100 h** | |

---

## Cómo se llega a ~3 semanas

Los ~14–15 días útiles de esfuerzo, a tiempo completo, caben en ~3 semanas hábiles. La compresión
adicional respecto al escenario parcial (4–6 sem) viene de:

1. **Dedicación completa** (6–8 h/día útiles en lugar de 2–4 h/día).
2. **Aceleración por agente:** la codificación se absorbe; la ruta crítica pasa a *paridad
   funcional + datos + E2E/UAT*.

> El calendario **no baja de ~3 semanas** solo con más horas de codificación: las esperas externas
> (red ORDS, validación de negocio, UAT) son el factor limitante.

| Semana | Foco | Entregable |
|---|---|---|
| 1 | U01 · U02 · U09 · U04 | Flujo NSS + menú + Oracle/ETL + expediente core |
| 2 | U05 · U06 · U07 | Incapacidades + PDFs + examen médico (lo más complejo) |
| 3 | U03 · U08 · U11 · U10 + cutover | Pre-Test + auth + hardening + QA/UAT + estabilización/Go-Live |

---

## Escenarios de total

| Escenario | Calendario | Esfuerzo humano | Cuándo aplicarlo |
|---|---|---|---|
| **Tiempo completo (recomendado)** | **~3 sem** | ~100 h | 6–8 h/día útiles; piso por esperas externas |
| Parcial (agente + humano) | 4–6 sem | ~100 h | Dedicación compartida con otras tareas (2–4 h/día) |
| Solo humano (baseline) | 9–12 sem | 301 h | Sin asistencia de agente |

---

## Supuestos y riesgos que mueven la aguja

**A favor (−tiempo):**

- Contrato ORDS ya conocido (`app.php`).
- U01 iniciada (flujo NSS implementado).
- Infra Java + login ya operativos.

**En contra (+tiempo — y son el piso del calendario a tiempo completo):**

- **Esperas externas** (ruta crítica): acceso de red al WS ORDS (`10.249.249.3`), validación de
  negocio y UAT con usuarios clave.
- **U07** es el bloque crítico (estado local + navegación in-page sin request).
- **Latencias 6–7 s** en 4 endpoints PDF → requieren tuning específico Java+Oracle.
- **ETL `tags`**: bug de duplicación masiva + encoding mixto (latin1/utf8) + `files.url` = base64
  de PDF (decisión BLOB).
- **`x-api-key` real** del WS ORDS (hoy enmascarado como `XXXXXX`) — necesario para pruebas E2E.

---

## KPIs acordados

| Métrica | Target |
|---|---|
| Paridad funcional módulos admin | ≥ 95% |
| Defectos críticos abiertos al cutover | 0 |
| Latencia p95 searchNSS | ≤ 500 ms |
| Latencia p95 validateModules | ≤ 300 ms |
| Latencia p95 PDF/históricos | ≤ 2500 ms |
| Latencia p95 viewTypeExped | ≤ 2000 ms |
| Cobertura de tests | ≥ 70% |
| UAT pass rate | ≥ 95% |

---

*Documento generado a partir de los artefactos de planeación del repositorio. El escenario base
asume dedicación de tiempo completo (~6–8 h/día útiles) con asistencia de agente para la
codificación; el calendario de ~3 semanas está limitado por la ruta crítica externa.*
