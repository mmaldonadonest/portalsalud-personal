# Archivos de MORBILIDAD 2026 que no se cargaron porque aún no tienen gráfica

**Carpeta:** `C:\Users\Miguel\Downloads\MORBILIDAD\MORBILIDAD 2026\`
**Fecha:** 7-oct-2026

De los 132 archivos de la carpeta 2026, se cargaron 108 (atenciones, exámenes, incapacidades, accidentes, antidoping, maternidad y consumibles), que son los que alimentan las gráficas que hoy existen en el menú **Análisis**. Estos **24** no entraron.

## Por qué no se cargaron

Los 20 reportes de seguimiento (NOM-035, seguimiento médico especial, CAPA y carnet de crónicos) **todavía no tienen gráfica en el portal**, y además su Excel es distinto a los demás:

1. **Están hechos de otra forma.** Lo cargado tiene un renglón por cada consulta, incapacidad o accidente. Estos tienen un renglón por persona y una columna por mes (en control / sin control / baja, citas). Hace falta un cargador distinto.
2. **Vienen repetidos.** Cada predio guarda su copia del mismo reporte (se ve abajo: el mismo "GCIA" en M2, Mercurio, Tultipark y Zara). Antes de cargar hay que quitar duplicados; si no, se cuenta todo varias veces.
3. **No hay dónde verlos.** En Análisis no existe todavía la gráfica del carnet, de CAPA ni de NOM-035. Cuando se construyan, se cargan: las tablas destino ya están creadas (`SERV_MED_BITACORA_ESTADO_MES`, `SERV_MED_BITACORA_NOMENCL`).

Los otros 4 (manifiestos de residuos y productividad) no son datos clínicos por persona y quedaron fuera del alcance.

## Reportes de seguimiento — sin gráfica aún (20)

### NOM-035 riesgo psicosocial (7)

| Predio | Archivo |
|---|---|
| Atizapán | `ATIZAPARK\NOM035 Y AP 2026 ATIZAPAN.xlsx` |
| Macro 2 | `M2\NOM035 Y AP 2026 GERENCIA MACRO 2.xlsx` |
| Mercurio | `MERCURIO\NOM-035-MERCURIO  2026.xlsx` |
| Toluca | `TOLUCA\NOM035 Y AP 2026 TOLUCA.xlsx` |
| Tultipark | `TULTIPARK\NOM035 Y AP 2026 TULTIPARK 3.xlsx` |
| World Park | `WP\NOM035 Y AP 2026 WP.xlsx` |
| Z Vallejo | `ZARA\NOM035 Y AP 2026 Z VALLEJO.xlsx` |

### Seguimiento médico especial (7)

| Predio | Archivo |
|---|---|
| Macro 1 | `M1\SEGUIMIENTO MEDICO ESPECIAL- M1- 2026.xlsx` |
| Macro 2 | `M2\SEGUIMIENTO MEDICO ESPECIAL GCIA 2026 MACRO 2.xlsx` |
| Mercurio | `MERCURIO\SEGUIMIENTO MEDICO ESPECIAL GCIA 2026.xlsx` |
| Toluca | `TOLUCA\SEGUIMIENTO MEDICO ESPECIAL TOLUCA 2026.xlsx` |
| Tultipark | `TULTIPARK\SEGUIMIENTO MEDICO ESPECIAL GCIA 2026.xlsx` |
| World Park | `WP\SEGUIMIENTO MEDICO ESPECIAL 2026 WP.xlsx` |
| Z Vallejo | `ZARA\SEGUIMIENTO MEDICO ESPECIAL GCIA 2026.xlsx` |

### CAPA / adicciones (4)

| Predio | Archivo |
|---|---|
| Macro 2 | `M2\1 CONTROL CAPA MACRO2 2026.xlsm` |
| Mercurio | `MERCURIO\1 CONTROL CAPA 2026.xlsm` |
| Toluca | `TOLUCA\CONTROL CAPA TOLUCA 2026.xlsm` |
| World Park | `WP\CONTROL CAPA 2025 WORLD PARK.xlsm` |

### Carnet de crónico-degenerativos (2)

| Predio | Archivo |
|---|---|
| Siglo XXI | `SIGLO\CARNET 2026_PREDIO_ SIGLO XXI.xlsx` |
| World Park | `WP\KILOTON CARNET 2026.xlsm` |

## Fuera de alcance (4)

| Tipo | Archivo |
|---|---|
| Manifiestos de residuos | `MERCURIO\CONTROL MANIFIESTOS 2026 PREDIO.xlsm` |
| Manifiestos de residuos | `SIGLO\CONTROL MANIFIESTOS 2026 AIFA.xlsm` |
| Manifiestos de residuos | `SIGLO\CONTROL MANIFIESTOS 2026 GENERAL.xlsm` |
| Productividad | `TOLUCA\productividad Toluca 26.xlsx` |

## Qué hace falta para cargar los 20 de seguimiento

1. Que el servicio médico responda: cuál copia manda cuando el mismo reporte está en varios predios, qué significa cada clave de estado mensual, y qué años interesan.
2. Un cargador persona × mes con deduplicación por contenido, lectura de la hoja NOMENCLATURA y limpieza de filas vacías con fórmulas.
3. Las gráficas de seguimiento en Análisis (grupo nuevo).

Sin base nueva: las tablas ya existen.
