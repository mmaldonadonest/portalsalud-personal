# Migración PHP → Java · Estado del Proyecto

**Proyecto:** Portal Salud — Migración sistema médico legacy (PHP → Java Spring Boot, MariaDB → Oracle 21c)
**Corte de estado:** 2026-07-07
**Fuentes:** desarrollo a la fecha · `plan/valoracion-agente-v2.json`

---

## Resumen de estado

| Indicador | Valor |
|---|---|
| **Unidades completas** | **8 de 11** (+ 1 parcial, 2 pendientes) |
| Avance aproximado | ~77% de las unidades del plan |
| Flujo principal | **Operativo end-to-end**: login → búsqueda NSS → menú por permisos → módulos clínicos |
| Pendiente mayor | U07 (examen médico, el más complejo) y U10 (QA/UAT) |

> **Funcional hoy:** login, búsqueda por NSS (WS ORDS), menú dinámico de módulos por permisos, y los
> módulos clínicos — consultas, incapacidades, archivos (laboratorio/históricos/notas) y pre-test —
> con alta, detalle, firma digital y adjuntos.

---

## Estado por caso de uso

| ID | Caso de uso | Estado | Notas |
|----|-------------|:---:|---|
| **U01** | Búsqueda por NSS (searchEmploye) | Completo | POST→HTML, flujos ORDS encadenados, página `/nss`, shortcut en header |
| **U02** | Menú dinámico de módulos + permisos | Completo | validateModules (2 llamadas ORDS) → submenú lateral |
| **U03** | Pre-Test y Checklist | Completo | Laboratorio (MED_FILE) + Pre-Test EAV ~55 campos (MED_TAG) |
| **U04** | Expediente general + consultas | Completo | Historial + detalle + alta + IMC + ICD + firma + adjuntos |
| **U05** | Incapacidades + archivo | Completo | Lista + detalle + alta |
| **U06** | Notas e históricos PDF | Completo | Histórico E.M / Nota médica / Nota incapacidad (módulo genérico por tipo) |
| **U07** | Examen médico + navegación in-page | Pendiente | **El más complejo** (estado local, heredofamiliares, viewTypeExped) |
| **U08** | Compatibilidad auth (JAVA/LEGACY_PHP/HYBRID) | Completo | ⚠ regla de éxito del login legacy por verificar vs WS real |
| **U09** | ETL files/tags → Oracle 21c | Parcial | DDL aplicado (QA+local) + base64→BLOB validado; escritura nueva OK. Falta carga masiva del legacy (requiere MariaDB) |
| **U10** | QA integral (contract / E2E / UAT) | Pendiente | Los tests `@MockBean` requieren JDK 21 |
| **U11** | Hardening / observabilidad | Completo | Permissions-Policy, actuator (health/info), correlation-id (X-Trace-Id) |

Leyenda: **Completo** = migrado y compilando · **Parcial** = base lista, falta un paso externo · **Pendiente** = no iniciado.

---

## Infraestructura transversal lograda

- Cliente ORDS (`biowsRestClient`) que tolera `text/html` con cuerpo JSON (como el `json_decode` del PHP).
- Tablas de aterrizaje **`MED_FILE`** (adjuntos BLOB) y **`MED_TAG`** (EAV) aplicadas en **QA y local**.
- Gestión de archivos: upload / download / delete + módulo genérico por tipo (laboratorio, examen_medico, nota_medica, nota_incapacidad).
- Componentes reutilizados entre módulos: firma digital (canvas), buscador + Exportar a Excel, modales de detalle, guardado genérico con adjuntos.
- Usuario de prueba sembrado (`68958027838` / `admin`, rol ROLE_ADMIN).
- Seguridad: selector de estrategia de auth + cabeceras de hardening + trazabilidad por request.

---

## Bloqueos / pendientes de confirmar

1. **`x-api-key` real** del ORDS (hoy `XXXXXX` placeholder) — sin él, las llamadas ORDS fallan (401/timeout).
2. **Conectividad de red** al WS ORDS `10.249.249.3` desde el entorno de despliegue.
3. **Origen MariaDB** para la carga masiva de U09 (datos legacy existentes). Los registros **nuevos** ya se guardan directo en Oracle.
4. **Verificación manual** end-to-end (pendiente de la prueba del equipo).
5. **Tests**: los `@MockBean` no corren en el entorno actual (JDK 22 + Mockito self-attach); usar **JDK 21** en CI.

---

## Restante y estimación

| Bloque | Estado | Estimación (plan) |
|---|---|---|
| **U07** Examen médico + navegación in-page | Pendiente | 1.5 – 2.5 sem (el más grande) |
| **U10** QA integral (contract/E2E/UAT) | Pendiente | 1.0 – 1.5 sem |
| **U09** carga masiva legacy | Parcial (falta MariaDB) | 0.5 sem (una vez con acceso al origen) |
| Afinamiento post-verificación | — | según hallazgos |

Los 6 módulos funcionales del expediente (U01–U06) y los transversales (U08/U11) ya están cerrados;
el esfuerzo restante se concentra en el examen médico (U07) y la puerta de calidad (U10).

---

*Documento de estado generado a partir del desarrollo a la fecha. Reemplaza al documento de estimación previo.*
