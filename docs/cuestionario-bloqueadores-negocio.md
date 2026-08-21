# Cuestionario de decisiones pendientes — Salud Ocupacional

**Estado: las 7 preguntas quedaron respondidas el 20 de agosto de 2026**, mediante una propuesta formal de ONEST Logistics (`docs/ONEST_Proyecto_Salud_Ocupacional_Decisiones.pdf`). Este documento se conserva como registro de las preguntas originales y su resolución — no quedan preguntas abiertas de la lista original.

Ninguna de las 7 decisiones tiene código construido todavía: definen el modelo de datos/workflow para poder empezar a programar. **Actualizado de nuevo el mismo día:** `docs/entregable-liberacion-stoppers-salud.html` redujo y dimensionó las 3 que habían quedado sin estimar — protocolo antidoping → selección aleatoria (sin protocolo clínico complejo), flujo de accidentes → solo seguimiento (sin motor de investigación), reglas de NSS → confirmado con casos de prueba — 1 día cada una (se construyen sobre módulos ya existentes). Ya no queda ninguna sin dimensionar. Solo 1 (matriz de permisos) sigue — según el documento original — sin **aprobación formal** de las áreas responsables antes de pasar a producción.

Fuente: `docs/ONEST_Proyecto_Salud_Ocupacional_Decisiones.pdf` + `docs/entregable-liberacion-stoppers-salud.html` (20 de agosto de 2026) / `docs/checklist-bloqueadores-negocio.html` / `docs/plan-tareas-concretas.html`.

---

## ✓ Ya respondida antes de esta propuesta

**Migración histórica** — respondida 20 de agosto: **SÍ** entra en el alcance. La ejecuta un equipo externo con ETL en Oracle (no este equipo de desarrollo) — especificación técnica completa en `docs/etl-migracion-historica-especificacion.md`.

---

## ⚠ Decisión revertida el mismo día

**Causa de consulta médica** — el Product Owner había respondido primero que **NO** debía existir catálogo cerrado (texto libre). Horas después, la propuesta formal (§2 del PDF) exigió exactamente lo contrario, y esa es la decisión que queda vigente:

> **Catálogo normalizado de 23 causas** (enfermedad general, dolor de cabeza, problema gastrointestinal, problema respiratorio, dolor muscular, dolor de espalda, lesión musculoesquelética, caída, golpe/contusión, herida/cortadura, quemadura, problema ocular, reacción alérgica, mareo, presión arterial, accidente de trabajo, accidente en trayecto, accidente no laboral, seguimiento médico, examen médico, maternidad/embarazo, curación, valoración preventiva) **+ opción "Otro" con descripción libre**. Administrable (altas, bajas, activación/desactivación, histórico, control de duplicados).
>
> Habilita: top de causas, morbilidad por predio/cuenta, tendencias mensuales, comparativos, recurrencias, dashboard ejecutivo.
>
> **Pendiente sin definir:** el mapeo de los valores de texto libre ya guardados hoy en `CAUSA` hacia el catálogo nuevo — no viene especificado en el documento; sugerido consultarlo con Médico Jefe / PO antes de programar la migración de datos.

**Contestó:** ONEST Logistics (propuesta formal) **Fecha:** 20 de agosto de 2026 · **Costo:** 1.25 días de desarrollo (sin contar migración de datos históricos) — re-estimado 20 de agosto anclado a Consumibles (dominio nuevo completo en 12h), asumiendo que ORDS se publica en minutos · **Estado:** pendiente de validación médica

---

## Médico Jefe

### 1. Catálogo de diagnósticos: ¿normalizar contra ICD?

**Contexto:** el catálogo ICD (`Servcio/indice`) ya existe y ya está conectado en Consulta y Examen.

**Respuesta:** el diagnóstico principal debe seleccionarse **obligatoriamente** del catálogo ICD/CIE. El texto libre queda como información clínica complementaria. Se agrega un campo nuevo de **diagnósticos secundarios**.

**Desbloquea:** reportes de diagnóstico normalizados. **Costo:** 0.75 día de desarrollo — no es dominio nuevo (el catálogo ICD ya está integrado), solo cambia la captura + un campo EAV nuevo.

**Contestó:** ONEST Logistics (propuesta formal) **Fecha:** 20 de agosto de 2026 **Estado:** pendiente de validación del Médico Jefe antes de producción

### 2. Catálogo de restricciones médicas

**Contexto:** ningún reporte real trae hoy un catálogo de restricciones (ej. «no cargar más de X kg»). No existía en ningún lado.

**Respuesta:** catálogo administrable de 16 restricciones:

| Código | Restricción |
|---|---|
| RES-01 | No cargar peso |
| RES-02 | Límite de carga |
| RES-03 | No realizar trabajo en alturas |
| RES-04 | No operar maquinaria |
| RES-05 | No conducir vehículos |
| RES-06 | Evitar movimientos repetitivos |
| RES-07 | Evitar exposición a ruido |
| RES-08 | Evitar exposición a sustancias |
| RES-09 | Evitar temperaturas extremas |
| RES-10 | Evitar esfuerzo físico intenso |
| RES-11 | Trabajo administrativo |
| RES-12 | Requiere pausas periódicas |
| RES-13 | Restricción de horario |
| RES-14 | Restricción temporal |
| RES-15 | Uso obligatorio de equipo específico |
| RES-16 | Otra restricción |

Cada restricción incluye: tipo, descripción, valor límite, unidad, fecha de inicio, fecha de fin, fecha de revaloración, temporalidad, observaciones, médico responsable y estatus.

**Desbloquea:** restricciones en el dictamen de examen médico. **Costo:** 1.4 días de desarrollo — anclado a Consumibles (dominio nuevo completo en 12h), asumiendo que ORDS se publica en minutos.

**Contestó:** ONEST Logistics (propuesta formal) **Fecha:** 20 de agosto de 2026 **Estado:** pendiente de validación del Médico Jefe antes de producción

---

## Gerente de Salud Ocupacional

### 3. Antidoping — selección aleatoria

**Contexto:** la captura básica (alta y consulta de resultados) ya está construida y en funcionamiento.

**Respuesta — dimensionada 20 de agosto, sustituye al workflow clínico anterior** (`docs/entregable-liberacion-stoppers-salud.html` §2): alcance reducido y aprobado, sin protocolo clínico complejo:

```
Selección aleatoria → Aplicación → Captura de resultado → Historial
```

Registra persona, fecha de aplicación y resultado, con historial/trazabilidad completos y sin permitir editar/borrar registros pasados.

**Desbloquea:** reglas de validación/flujo. **Costo:** 1 día de desarrollo — se construye sobre la captura ya existente, no es dominio nuevo. Sin aprobación pendiente — al descartar el protocolo clínico complejo, ya no aplica la reserva de autorización formal de la versión anterior.

**Contestó:** ONEST Logistics (`docs/entregable-liberacion-stoppers-salud.html`) **Fecha:** 20 de agosto de 2026

### 4. Accidentes — seguimiento de casos

**Contexto:** la captura básica (alta y consulta) ya está construida y en funcionamiento.

**Respuesta — dimensionada 20 de agosto, sustituye al workflow de investigación anterior** (`docs/entregable-liberacion-stoppers-salud.html` §3): alcance reducido y aprobado, solo seguimiento:

```
Registro del accidente → Seguimiento → Actualización → Evidencias/observaciones → Cierre
```

Explícitamente fuera de alcance: motor de investigación de accidentes, workflow independiente de investigación, expediente adicional, automatización de metodologías de causa raíz.

**Desbloquea:** reglas de proceso. **Costo:** 1 día de desarrollo — se construye sobre el alta ya existente, no es dominio nuevo.

**Contestó:** ONEST Logistics (`docs/entregable-liberacion-stoppers-salud.html`) **Fecha:** 20 de agosto de 2026

### 5. Maternidad: ¿qué nivel de seguimiento se espera? *(también involucra a Médico Jefe)*

**Contexto:** hoy solo existe un campo «¿está embarazada?» Sí/No + tiempo de embarazo.

**Respuesta:** evolucionar hacia un módulo de seguimiento de Salud Ocupacional (sin convertirlo en un expediente obstétrico completo), con: fecha de registro, semanas de gestación, fecha probable de parto, restricciones laborales, próxima revisión, observaciones, estatus, incapacidad y reincorporación.

**Desbloquea:** módulo de Maternidad. **Costo:** 1 día de desarrollo (8h) — anclado al precedente real más parecido en este repo, Contactos de emergencia (EAV sobre `MED_TAG`, arreglo de 3 registros, tomó 8h completas), asumiendo el mismo patrón EAV en vez de tabla Oracle dedicada: spike 1h + servicio/fragment 5h + ajuste por 2 campos de referencia extra (restricciones, incapacidad) 1h + prueba de humo 1h. Si más adelante se pide reportería/dashboard propio de Maternidad, se justificaría tabla nueva y el costo subiría.

**Contestó:** ONEST Logistics (propuesta formal) **Fecha:** 20 de agosto de 2026 **Estado:** pendiente de validación del Médico Jefe / Gerencia de Salud Ocupacional antes de producción

---

## RRHH

### 6. Reglas de NSS: ¿único o repetible entre predios?

**Contexto:** sin confirmar si un mismo NSS puede repetirse cuando un empleado trabaja en más de un predio/empresa del grupo.

**Respuesta:** el NSS se asocia a la **persona**, no a una asignación de predio. Una persona puede mantener varias relaciones laborales (empresa/cuenta/predio/puesto) sin duplicar su expediente. No es un simple ajuste de "único vs. repetible" — es un modelo de datos nuevo (`PERSONA` → múltiples `Relaciones laborales`). **Dimensionada 20 de agosto** (`docs/entregable-liberacion-stoppers-salud.html` §4) con requerimientos concretos (ajustar `NssSearchService`, evitar bloqueo al asociar a otro predio, recuperar relaciones existentes) y 5 casos de prueba de regresión.

**Desbloquea:** ajuste en la búsqueda por NSS. **Costo:** 1 día de desarrollo — reusa el mismo JOIN ya construido para CUENTA/dashboard, no es una entidad nueva.

**Contestó:** ONEST Logistics (`docs/entregable-liberacion-stoppers-salud.html`) **Fecha:** 20 de agosto de 2026

---

## Admin IT + Médico Jefe

### 7. Matriz de permisos y confidencialidad

**Contexto:** sin definir el corte de acceso por rol (médico / gerente / auditor).

**Respuesta:** RBAC + alcance organizacional — **Rol + Empresa + Cuenta + Predio + Tipo de información**. Los datos clínicos deben tener permisos independientes de los técnicos/administrativos: un administrador de TI no obtiene acceso automático al contenido clínico solo por administrar el sistema.

**Desbloquea:** matriz de permisos — toca varios servicios. **Costo:** 2.25 días de desarrollo, la más costosa de la lista — esta no depende de ORDS (es Spring Security del lado Java, wiring en ~10 paquetes existentes), por eso baja menos que las demás.

**Contestó:** ONEST Logistics (propuesta formal) **Fecha:** 20 de agosto de 2026 **Estado:** el documento señala explícitamente «Requiere aprobación formal» antes de producción.

---

## Resumen de seguimiento

| # | Pregunta | Responsable | Estado |
|---|---|---|---|
| — | Causa de consulta médica | Product Owner → revertido por propuesta formal | ⚠️ Revertida 20 ago — vuelve a ser catálogo cerrado |
| — | Migración histórica | Equipo externo | ✅ Respondida 20 ago |
| 1 | Catálogo de diagnósticos normalizado | Médico Jefe | ✅ Respondida 20 ago — pendiente validación formal |
| 2 | Catálogo de restricciones médicas | Médico Jefe | ✅ Respondida 20 ago — pendiente validación formal |
| 3 | Antidoping — selección aleatoria | Gerente SO | ✅ Respondida y dimensionada 20 ago — 1 día |
| 4 | Accidentes — seguimiento de casos | Gerente SO | ✅ Respondida y dimensionada 20 ago — 1 día |
| 5 | Maternidad — nivel de seguimiento | Gerente SO / Médico Jefe | ✅ Respondida 20 ago — pendiente validación formal |
| 6 | Reglas de NSS | RRHH | ✅ Respondida y dimensionada 20 ago — 1 día |
| 7 | Matriz de permisos y confidencialidad | Admin IT + Médico Jefe | ✅ Respondida 20 ago — pendiente aprobación formal |
