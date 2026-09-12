# Consulta — criterio de fecha para el reporte de incapacidades

**Estado:** RESUELTO el 11 de septiembre de 2026 — **Opción A: manda la fecha de inicio** (la del certificado).
**Aplicación:** `docs/ords-incapacidades-criterio-fecha-inicio.sql` (WS `_cta`, lo aplica el usuario) +
`DashboardIncapacidadesService` agrupa por inicio con fallback a registro (ya en código).
**Fecha de la consulta:** 10 de septiembre de 2026
**Bloqueaba:** gráfica "Evolución mensual de indicadores" del Dashboard Ejecutivo (`/analisis/ejecutivo`)

---

## La consulta (texto para reenviar)

Estamos construyendo el tablero de Salud Ocupacional y necesitamos definir un criterio de negocio
que hoy no está documentado.

Cada incapacidad tiene dos fechas distintas:

- **Fecha de inicio**: cuando la incapacidad empieza, según el certificado
- **Fecha de registro**: cuando se captura en el sistema

Normalmente coinciden o están cerca, pero no siempre. El caso que nos bloquea:

> Una incapacidad **inicia el 20 de diciembre de 2023** y se **captura el 5 de enero de 2024**.

**La pregunta:** cuando alguien consulta el reporte del ejercicio **2024**, ¿esa incapacidad debe
aparecer?

**Opción A — No, es de 2023.** Manda la fecha de inicio. Los días de incapacidad se contarían en el
mes en que realmente ocurrieron. Consecuencia: los totales de 2024 bajan, porque se excluyen las que
vienen arrastrando de diciembre; y un reporte de 2023 emitido en su momento no coincidiría con el
mismo reporte consultado hoy, porque después se capturaron casos con inicio en 2023.

**Opción B — Sí, se capturó en 2024.** Manda la fecha de registro. Los días se contarían en el mes de
captura. Consecuencia: los totales de cada ejercicio quedan fijos y no cambian de forma retroactiva,
pero un caso que inició en diciembre aparece como si fuera de enero.

**Por qué importa:** hoy el sistema mezcla los dos criterios — filtra el periodo por fecha de registro
pero grafica por fecha de inicio. El resultado es que al consultar 2024 la gráfica muestra meses de
2023, y las cifras de las tarjetas no cuadran con la gráfica. Necesitamos un solo criterio para ambos.

Si existe algún lineamiento del IMSS o contable que ya defina esto, con eso nos alineamos.

---

## Detalle técnico (no enviar, para nosotros)

### Dónde está el desajuste

| | Campo |
|---|---|
| El WS `consulta_incapacidades_fecha_cta` filtra por | `TBL_SERV_INCAPACIDAD_MEDICA.FECHA_REGISTRO` |
| `DashboardIncapacidadesService` agrupa la tendencia por | `fechaInicio` |

Ninguna de las dos está mal por separado; el problema es usarlas a la vez.

### Es el único dashboard con este problema

Verificado el 10-sep-2026 — los otros tres filtran y agrupan por el mismo campo:

| Dashboard | Filtra por | Agrupa por | |
|---|---|---|---|
| Incapacidades | `fecha_registro` | `fecha_inicio` | inconsistente |
| Accidentes | `fecha_accidente` | `fecha_accidente` | ok |
| Exámenes | `fecha_registro` | `fecha_registro` | ok |
| Consultas | `fecha` | `fecha` | ok |

### Qué se ve afectado

Solo lo que se alimenta de la tendencia de incapacidades:

- Serie "Días de incapacidad" de la gráfica **Evolución mensual** — es la razón de que el eje X
  arranque en `2023-08` con el filtro puesto en 2024
- Pastilla de tendencia de la KPI **Días de incapacidad**
- Pastilla de tendencia de la KPI **Personas incapacitadas**
- Insight "Los días de incapacidad de {mes} subieron {x}% vs. {mes anterior}"

**No** se afectan: los valores de las 6 tarjetas KPI, Composición de días perdidos, Costo por ramo,
Ranking de predios, Top 8 causas ni Resultado de exámenes.

### Qué implica cada opción en código

- **Opción A (fecha de inicio) — ELEGIDA:** el filtro tiene que cambiar en el WS, no en Java:
  filtrar en Java sobre lo que el WS ya entregó por registro dejaría fuera las incapacidades
  registradas en 2025 con inicio en 2024. `FECHA_INICIO` es texto con formatos mezclados
  (263 ISO, 11 `dd/mm/yyyy hh:mi:ss`, 17 nulas de 291), por eso el SQL convierte según formato y
  cae a `fecha_registro` cuando es nula. Cambian los totales: en 2024, 51 de 221 filas tienen
  inicio fuera del año. ~2 h.
- **Opción B (fecha de registro):** cambiar el `mesDe(fila.fechaInicio())` por
  `mesDe(fila.fechaRegistro())`. Los totales no se mueven. ~0.5 h.

Alternativa si el negocio no se decide: pedir al WS que acepte **por cuál de las dos fechas filtrar**,
y exponerlo como un selector en la pantalla. Cuesta más, pero deja de ser una decisión nuestra.

---

## Hallazgo aparte, encontrado al investigar esto

El handler productivo `consulta_incapacidades_fecha` tiene depuración olvidada:

```sql
insert into bug values (kfechafin);
insert into bug values (kfechaini);
```

**Cada llamada inserta dos renglones en una tabla llamada `bug`.** El dashboard lo invoca en cada
carga de pantalla, así que esa tabla lleva tiempo creciendo sin que nadie la lea. Conviene quitar
las dos líneas y revisar qué tan grande está. No es urgente ni rompe nada, pero es una escritura en
cada llamada de lectura.
