# Análisis: de dónde salen las gráficas y cómo se suman las dos fuentes

**Fecha:** 7-oct-2026

Las gráficas del menú **Análisis** no leen de una sola tabla. Cada pantalla junta **dos fuentes** y las suma:

1. **Histórico de Excel** — lo que el servicio médico llevaba en sus Excel de morbilidad, cargado a la base del portal (carpeta 2026, enero–junio).
2. **Lo que se captura en el portal** — consultas, incapacidades, exámenes, accidentes, antidoping, maternidad y consumibles que se registran día a día en las pantallas del portal. Esto viaja por los web services de ORDS y queda en las tablas del servicio médico.

Los eventos nuevos **no** caen en las tablas del histórico de Excel; caen en sus tablas de captura, y la gráfica los recoge de ahí. Por eso no hace falta "pasar" nada: lo capturado aparece en Análisis igual que lo histórico.

## Tablas por gráfica

| Gráfica (Análisis) | Histórico Excel (tabla y familia) | Captura en el portal (WS → tabla) |
|---|---|---|
| Morbilidad: consultas por causa, tipo, género, cuenta, predio, tendencia | `SERV_MED_BITACORA_EVENTO` + `_ATRIBUTO`, familia `ATENCION` | `Servcio/consulta_medica_fecha` → `TBL_SERV_CONSULTA_MEDICA` |
| Causas musculoesqueléticas | `ATENCION` (atributo `LESION_ME`) | mismo WS → `TBL_SERV_CONSULTA_MEDICA` |
| Exámenes médicos: dictamen por mes y por predio | familias `EXAMEN_INGRESO`, `EXAMEN_PERIODICO`, `EXAMEN_POS_INCAP` | `Servcio/consulta_examen_fecha_cta` → tablas `SERV_MED_*` del examen (`_RESULTADO_EXAMEN` y secciones) |
| Incapacidades: días, costo, ramo, tendencia | familia `INCAPACIDAD` | `Servcio/consulta_incapacidades_fecha_cta` → `TBL_SERV_INCAPACIDAD_MEDICA` |
| Accidentabilidad: tipo de riesgo, causa RT, estatus | familia `ACCIDENTE` | `Servcio/consulta_accidentes_fecha_cta` → `SERV_MED_ACCIDENTE` |
| Control antidoping: resultado, tipo, predio | familia `ANTIDOPING` | `Servcio/consulta_antidoping_fecha_cta` → `SERV_MED_ANTIDOPING_RESULTADO` |
| Maternidad: partos, semanas de gestación, tendencia | familia `MATERNIDAD` | `Servcio/consulta_maternidad_fecha` → `SERV_MED_MATERNIDAD_SEGUIMIENTO` |
| Inventario / consumo de medicamento | `SERV_MED_BITACORA_METRICA`, familia `CONSUMIBLE` | `Servcio/consulta_consumibles` → `SERV_MED_ANTIDOPING_INVENTARIO` |

En corto:

- **Histórico Excel (3 tablas):** `SERV_MED_BITACORA_EVENTO`, `SERV_MED_BITACORA_ATRIBUTO`, `SERV_MED_BITACORA_METRICA`. (`SERV_MED_BITACORA_LOTE` es solo la bitácora de la carga; no se grafica.)
- **Captura en el portal (7 tablas, a través de ORDS):** `TBL_SERV_CONSULTA_MEDICA`, `TBL_SERV_INCAPACIDAD_MEDICA`, las `SERV_MED_*` del examen, `SERV_MED_ACCIDENTE`, `SERV_MED_ANTIDOPING_RESULTADO`, `SERV_MED_MATERNIDAD_SEGUIMIENTO`, `SERV_MED_ANTIDOPING_INVENTARIO`.

## Cómo se suman

Cada pantalla de Análisis hace lo mismo, con el mismo filtro de fechas, predio y cuenta para las dos fuentes:

1. **Pide lo vigente a ORDS** (por ejemplo `consulta_medica_fecha` con el rango de fechas). ORDS devuelve pocos renglones y el portal los agrupa en memoria: cuántos por causa, por género, por mes…
2. **Pide lo histórico a la base del portal**, pero **ya agrupado**: las consultas sobre `SERV_MED_BITACORA_*` devuelven directamente "cuántos por causa", "cuántos por mes", etc. No se traen las 24 mil atenciones a memoria; Oracle entrega los conteos.
3. **Acumula los dos conteos sobre las mismas categorías.** Si ORDS dice 12 consultas digestivas y el histórico dice 1,430, la barra muestra 1,442. Lo mismo para cada mes de la tendencia y para los totales (KPI).

Detalles que conviene saber:

- **Si el histórico está vacío** (no se corrió la carga en ese ambiente), la gráfica muestra solo lo vigente. Si ORDS no responde, muestra solo el histórico. No se cae.
- **El filtro por NSS solo aplica al lado vigente.** Los Excel no traen NSS (solo nombre), así que al filtrar por una persona el histórico no se descarta: se cuenta por nombre normalizado.
- **Rango de edad y predio** se comparan por el mismo texto en las dos fuentes; por eso el cargador dejó los rangos ("26 - 35") y los nombres de predio tal como los produce el portal.
- **Renglones sin mes** (casos "heredados" sin fecha) entran en los totales pero no en la barra mensual; el KPI puede ser mayor que la suma de las barras, y es correcto.

## La frontera entre las dos fuentes

El histórico de Excel llega hasta **junio 2026**. A partir de la fecha en que el servicio médico capture **solo en el portal**, no se deben cargar más Excel de esos meses: la misma consulta estaría en las dos fuentes y se contaría dos veces.

Hay que fijar esa **fecha de corte** con el servicio médico (por ejemplo: "julio 2026 en adelante solo portal") y, si se piden los Excel de julio–septiembre, cargarlos únicamente si en esos meses aún no capturaban en el portal.

Mientras conviven, una persona presente en las dos fuentes en el mismo periodo cuenta dos veces en "personas atendidas" (ORDS la identifica por NSS, el histórico por nombre). Es una limitación conocida y desaparece con la fecha de corte.

## Lo que no entra en Análisis todavía

Los reportes de seguimiento (carnet de crónico-degenerativos, CAPA, NOM-035, seguimiento especial) no tienen gráfica ni se cargaron; sus tablas (`SERV_MED_BITACORA_ESTADO_MES`, `SERV_MED_BITACORA_NOMENCL`) están vacías. Decisión del 7-oct-2026: se grafica solo lo acordado en el análisis inicial. Lista de archivos: `docs/carga-excel-2026-archivos-sin-grafica.md`.
