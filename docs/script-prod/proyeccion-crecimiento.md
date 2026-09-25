# Proyección de crecimiento — Portal Salud

**Medido el 24-sep-2026** en `BIOMETRICO@PDBPRD` (64 tablas del servicio médico) y en
`ONEWMS_QA` (las 20 del portal, ya cargadas con la migración histórica).
Script: `docs/medir-crecimiento-tablas.sql` (solo lectura).

---

## Resumen para quien decide

| | Hoy | A 5 años, escenario C | A 5 años, escenario D |
|---|---|---|---|
| **Filesystem** (los PDF) | **14 GB** | ~71 GB | **~207 GB** |
| **Oracle** (tablas del portal) | 225 MB | ~1.3 GB | ~3.8 GB |
| Oracle (las 64 del servicio médico) | 35.8 MB | ~290 MB | ~700 MB |

**Lo único que hay que provisionar en serio es disco de archivos, no tablespace.** Un PDF
promedio pesa 0.66 MB y la fila que lo describe pesa 0.33 KB: el filesystem crece **54
veces más rápido** que la base.

El escenario a usar es el **D**, que es el modelo real del negocio (examen inicial al
candidato + examen de ingreso + periódicos). Está **preliminar**: faltan dos datos que
nadie ha medido —cuántos candidatos se examinan al mes y cada cuánto es el periódico— y
ambos multiplican directo. Ver la sección 3.D.

Como orden de magnitud para la conversación con infraestructura: **250 GB de filesystem y
5 GB de tablespace** para cinco años, o menos si se define retención.

Dato que pone todo en contexto: **las 20 tablas del portal (225 MB) van a pesar 6 veces
más que las 64 tablas del servicio médico que ya existen (35.8 MB)**, y eso sólo por los
550 mil tags históricos que trae la migración.

---

## 1. La unidad de costo: un examen médico

Casi todo el volumen cuelga de una sola cosa: cada examen médico genera una fila en cada
una de ~53 tablas clínicas, más sus tags, más sus PDF.

Con 3,213 exámenes medidos, el costo por examen es:

| Componente | Por examen | Cómo se obtuvo |
|---|---|---|
| 53 tablas clínicas (Oracle) | 11 KB | 35.8 MB ÷ 3,213 |
| 171 tags (Oracle) | 68 KB | 215 MB ÷ 3,213 |
| Metadatos de 6.7 archivos (Oracle) | 2 KB | 7 MB ÷ 21,677 archivos |
| **Total Oracle** | **~82 KB** | |
| **6.7 archivos en filesystem** | **~4.4 MB** | 6.7 × 0.66 MB promedio |

Multiplicadores medidos en la migración: **171 tags** y **6.7 archivos** por examen.

---

## 2. El ritmo histórico, y por qué no sirve para proyectar

Las 53 tablas clínicas **no tienen columna de fecha** (el `tags` de MariaDB tampoco la
tenía), así que el ritmo no se puede medir directo. Se deriva comparando el conteo real de
hoy contra el `NUM_ROWS` que quedó congelado la última vez que se recolectaron
estadísticas:

| Periodo | Exámenes | Ritmo |
|---|---|---|
| 05/2024 → 07/2025 | 2,346 → 3,056 | **~51 / mes** |
| 07/2025 → 09/2026 | 3,056 → 3,213 | **~11 / mes** |

> Es una estimación, no una medición: `NUM_ROWS` sale de un muestreo y la fecha de
> recolección es un proxy del momento. Sirve para el orden de magnitud.

El ritmo cayó cinco veces. Y aquí está lo importante:

- La empresa tiene **33,907 empleados** y da de alta **~217 al mes** (652 en 90 días).
- El sistema legacy registró **3,213 exámenes en total**: el **9.5%** de la plantilla.
- A 11 exámenes/mes contra 217 altas/mes, el legacy captura **un 5% de lo que pasa**.

**Proyectar con el ritmo histórico sería proyectar el desuso del sistema viejo, no el uso
del nuevo.** Si el portal se adopta de verdad, el volumen no sube un poco: sube 20 veces.

---

## 3. Escenarios a 1, 3 y 5 años

Partiendo de los 14 GB y 225 MB que ya trae la migración histórica:

### A. Ritmo actual (11 exámenes/mes) — el piso

| | 1 año | 3 años | 5 años |
|---|---|---|---|
| Filesystem | 14.6 GB | 15.8 GB | 16.9 GB |
| Oracle (portal) | 236 MB | 258 MB | 279 MB |

### B. Ritmo del legacy en su mejor momento (51/mes)

| | 1 año | 3 años | 5 años |
|---|---|---|---|
| Filesystem | 16.7 GB | 22.1 GB | 27.5 GB |
| Oracle (portal) | 275 MB | 376 MB | 476 MB |

### C. Adopción plena: un examen por cada alta (217/mes) — **el que hay que provisionar**

| | 1 año | 3 años | 5 años |
|---|---|---|---|
| **Filesystem** | **25.5 GB** | **48.5 GB** | **71.4 GB** |
| Oracle (portal) | 439 MB | 868 MB | 1.3 GB |

### D. El modelo real del negocio — **PRELIMINAR, faltan dos datos** (24-sep-2026)

Negocio confirmó que no es un examen por persona, sino **tres momentos distintos**:

1. **Examen inicial al candidato** — antes de contratarlo.
2. **Examen de ingreso** — ya contratado.
3. **Exámenes periódicos** — con una periodicidad que todavía no está definida.

Eso deja el escenario C corto: son **al menos 2 exámenes por persona contratada**, más el
ciclo periódico sobre toda la plantilla vigente.

Estimación gruesa, con los supuestos marcados como tales:

| Concepto | Exámenes/mes | Supuesto |
|---|---|---|
| Candidatos | ~217 o más | **DESCONOCIDO**: se asume 1 candidato por alta, pero son más (no todo candidato se contrata) |
| Ingresos | 217 | medido: 652 altas en 90 días |
| Periódicos | ~299 | **DESCONOCIDO**: se asume anual sobre los 3,584 vigentes (3,584 ÷ 12) |
| **Total** | **~733 / mes** | 3.4 veces el escenario C |

| | 1 año | 3 años | 5 años |
|---|---|---|---|
| **Filesystem** | **52.7 GB** | **130 GB** | **207 GB** |
| Oracle (portal) | 946 MB | 2.4 GB | 3.8 GB |

Con este modelo, **100 GB de filesystem no alcanzan**: habría que ir a 250 GB para cinco
años, o definir retención.

**No tomar estos números como plan todavía.** Dependen de dos cosas que nadie ha medido:

- **Cuántos candidatos se examinan al mes.** Sólo conocemos las altas (217). La relación
  candidatos/contratados puede ser 1.5:1 o 4:1, y multiplica directo.
- **Cada cuánto es el examen periódico.** Anual, bienal o por puesto de riesgo cambia el
  tercer renglón entre 150 y 300 al mes.

Con esos dos números, esta tabla se recalcula en minutos.

---

## 4. Lo que puede cambiar estas cifras

**Retención.** `SERV_MED_FS_FILE_POLICY` viene con `RETENTION_DAYS = 3650`: nada se borra
en 10 años. Es la política más cara posible y hoy nadie la aplica —no hay proceso de
purga—, así que en la práctica el crecimiento es acumulativo puro. Definir retención por
tipo de documento es la palanca que más mueve el número final.

**Tamaño de los PDF.** El promedio de 0.66 MB viene de los archivos del legacy, y el mayor
migrado pesa 2 MB. Los documentos que genera el portal (Examen, Pre-Test, Consulta,
Incapacidad) se imprimen desde el navegador, así que su peso depende de si llevan imágenes
y de la firma en base64. Conviene medirlos cuando ya haya volumen real.

**Las firmas digitales.** Hoy viven como base64 dentro de `SERV_MED_TAG.CONTENT` (CLOB).
Si alguna vez se mueven a `SERV_MED_FS_FILE` —está anotado como fase futura—, migran de
Oracle al filesystem: baja el tablespace y sube el disco.

---

## 5. Lo que estas mediciones dejaron claro de paso

- **QA no puede medir crecimiento.** Sus fechas en `SERV_MED_TAG` y `SERV_MED_FS_FILE`
  son del momento de la migración (08–09/2026), no las originales. El `tags` del legacy
  nunca tuvo columna de fecha, así que esa información no existe en ninguna parte.

- **Las 11 tablas del portal vía ORDS sólo tienen datos de prueba nuestros**: 4 accidentes,
  7 antidoping, 24 causas, 3 restricciones, 50 en `RESULTADO_EXAMEN_HIST`, todo de
  08–09/2026. No hay uso de negocio todavía, así que tampoco dan tasa.

- **`SERV_MED_PREDIO` tiene 17 filas**, que son los 17 predios del catálogo.

- Hay una inconsistencia menor en el legacy: nueve tablas
  (`CARDIOPATIAS`, `ENDOCRINAS`, `GENERALES`, `HEREDOFAMILIAR`, `MENTALES`, `NEFROPATIA`,
  `NEUMOPATICA`, `OTRAS`, `TOXICOLOGICO`) tienen 3,222 filas en vez de 3,213: nueve
  registros de más respecto al resto del expediente. No afecta la proyección, pero explica
  por qué los conteos no cuadran exactamente entre tablas.

- **`SERV_MED_DET_ANT_LABORALES` es la única tabla con volumen propio**: 51,835 filas,
  16.1 por examen (hasta 4 empleos anteriores por persona, con su detalle).

---

## 6. Qué falta para cerrar la proyección

1. **Los dos datos que faltan del escenario D** (lo demás ya está medido):

   - **Candidatos examinados por mes.** Sólo tenemos las altas (217/mes). Hace falta la
     relación candidatos/contratados, o directamente el volumen de exámenes iniciales.
   - **Periodicidad del examen periódico.** Anual, bienal, o distinta según el puesto de
     riesgo. Mueve el tercer renglón entre ~150 y ~300 exámenes al mes.

   Con esos dos números el escenario D deja de ser preliminar. Todo lo demás —el costo por
   examen, los multiplicadores, el peso promedio del PDF— ya está medido.

2. **Medir el directorio real en el servidor de producción**, que es lo único que no se ve
   desde SQL:

   ```bash
   du -sh /mnt/data/onedev/apps/exec/filessalud
   find /mnt/data/onedev/apps/exec/filessalud -type f | wc -l
   ```

3. **Decidir retención** por tipo de documento, antes de que haya volumen que purgar.

4. **Volver a correr este script a los 3 y 6 meses** de producción. Con el portal
   escribiendo, `SERV_MED_FS_FILE.CREATED_AT` sí va a dar fechas reales y la proyección
   deja de depender de estadísticas viejas.
