# Tablas donde quedaron los Excel de MORBILIDAD (bitácora de atención)

**Base:** la del portal — esquema `BIOMETRICO`, tablas `SERV_MED_BITACORA_*`. No es ORDS ni toca el legacy.
**Carga a producción:** 30-sep / 1-oct-2026 (166 archivos+hojas, 24,236 eventos). Alimentan las gráficas del menú **Análisis**.
**Origen:** `C:\Users\Miguel\Downloads\MORBILIDAD\MORBILIDAD 2026\` — **solo la carpeta 2026** (132 archivos, enero–junio; el más reciente es del 3-jul-2026). Las carpetas 2017–2025 existen en el origen pero no se cargaron.

## Las cuatro tablas

| Tabla | Qué guarda | Un renglón por… | En prod |
|---|---|---|---:|
| `SERV_MED_BITACORA_LOTE` | La bitácora de la carga y el **cuadre**: archivo, hoja, familia, predio, año, filas leídas / insertadas / descartadas, el total que el propio Excel declara en su hoja ACUMULADO y si cuadró (S/N), estado, mensajes de descarte y el `CHECKSUM_SHA256` del archivo | archivo + hoja procesada | 166 |
| `SERV_MED_BITACORA_EVENTO` | **El dato principal**: cada atención, examen, incapacidad, accidente, antidoping o seguimiento de maternidad, ya transpuesto (la "marca con 1" del Excel resuelta a un valor). Columnas fijas comunes a todas las familias: fecha, año, mes, predio, cuenta, área, puesto, agencia, nombre (tal cual y normalizado), género, rango de edad, NSS (siempre vacío: el Excel no lo trae) | evento por persona | 24,236 |
| `SERV_MED_BITACORA_ATRIBUTO` | Lo que cambia de familia a familia, como clave → valor (mismo patrón EAV que `SERV_MED_TAG`): causa, ramo, diagnóstico, tratamiento, dictamen, días, costo, fechas de inicio/fin, resultado, desenlace… con el valor en texto y, cuando aplica, numérico o fecha | atributo de un evento | 118,515 |
| `SERV_MED_BITACORA_METRICA` | Conteos que **no** cuelgan de una persona: totales mensuales o anuales que el Excel ya traía calculados (productividad, carnet, CAPA, consumibles…) con predio, año, mes, concepto y subconcepto | cifra del Excel | 4,742 |

Relaciones: `EVENTO.LOTE_ID → LOTE`, `ATRIBUTO.EVENTO_ID → EVENTO`, `METRICA.LOTE_ID → LOTE`.

## Familias de eventos (columna `FAMILIA`)

| Familia | De qué hoja/archivo viene | Atributos típicos en `ATRIBUTO` |
|---|---|---|
| `ATENCION` | Consultas de la bitácora de atención | CAUSA, RAMO, DIAGNOSTICO, TRATAMIENTO, LESION_ME |
| `EXAMEN_INGRESO` | Exámenes de ingreso | DICTAMEN (15 apto · 45 condicionado · 60 restringido/inclusión · 30 no apto) |
| `EXAMEN_PERIODICO` | Exámenes periódicos | DICTAMEN |
| `EXAMEN_POS_INCAP` | Exámenes post incapacidad | DICTAMEN |
| `INCAPACIDAD` | Incapacidades | RAMO (EG / MAT / RT1 / RT2), TIPO (IMSS / INTERNA), DIAS, COSTO, FECHA_INICIO, FECHA_FIN |
| `ACCIDENTE` | Accidentes | TIPO (RT1 laboral / RT2 trayecto / INCIDENTE / ENF_PROFESIONAL) |
| `ANTIDOPING` | Pruebas antidoping | RESULTADO, DESENLACE (MONITOREO / BAJA / NO_CONTRATADO) |
| `MATERNIDAD` | Seguimiento de embarazo | ESTADO, DIAS |
| `CONSUMIBLE` | Inventario de medicamento (lote, caducidad, costo) | **No son eventos por persona**: se cargan como `METRICA` |

Agregar una familia nueva no requiere DDL: son renglones nuevos en `ATRIBUTO`.

## Consultas útiles (solo lectura)

```sql
-- Cuánto hay por tabla
SELECT 'EVENTO' t, COUNT(*) n FROM SERV_MED_BITACORA_EVENTO
UNION ALL SELECT 'ATRIBUTO', COUNT(*) FROM SERV_MED_BITACORA_ATRIBUTO
UNION ALL SELECT 'METRICA',  COUNT(*) FROM SERV_MED_BITACORA_METRICA
UNION ALL SELECT 'LOTE',     COUNT(*) FROM SERV_MED_BITACORA_LOTE;

-- Eventos por familia y mes (lo que ve la gráfica)
SELECT FAMILIA, ANIO, MES, COUNT(*) FROM SERV_MED_BITACORA_EVENTO
 GROUP BY FAMILIA, ANIO, MES ORDER BY FAMILIA, ANIO, MES;

-- Cuadre de cada archivo contra su propio ACUMULADO
SELECT ARCHIVO, HOJA, FAMILIA, FILAS_INSERTADAS, ACUMULADO_ESPERADO, CUADRA, ESTADO
  FROM SERV_MED_BITACORA_LOTE ORDER BY FAMILIA, ARCHIVO;

-- "Cuántos por causa" (patrón de todas las gráficas)
SELECT a.VALOR causa, COUNT(*) FROM SERV_MED_BITACORA_EVENTO e
  JOIN SERV_MED_BITACORA_ATRIBUTO a ON a.EVENTO_ID = e.ID AND a.NOMBRE = 'CAUSA'
 WHERE e.FAMILIA = 'ATENCION' GROUP BY a.VALOR ORDER BY 2 DESC;
```

## Dónde está todo

| Qué | Archivo |
|---|---|
| DDL (idempotente, aplicado en prod el 29-sep) | `docs/script-prod/excel-00-ddl-completo.sql` |
| Reporte verificable (cada cifra con su consulta) | `docs/carga-excel-2026-reporte-verificable.md` |
| Scripts de cuadre | `docs/script-prod/excel-04-que-se-cargo.sql`, `excel-06-cuadre.sql`, `excel-07-pantalla-vs-tabla.sql` |
| Cargador (proyecto aparte, consola) | `excel-loader/` (`CargaService`, `MapeoFamilia`, `BitacoraRepository`) |
| Hallazgos y decisiones de la carga | `docs/carga-historica-morbilidad-hallazgos.md` |

## Pendientes conocidos

- **Solo 2026, y solo hasta junio**: las carpetas 2017–2025 no entraron en esta carga; y de 2026 faltan julio–septiembre (pedir los Excel al corte).
- **La recarga no detecta archivos actualizados**: el cargador decide por archivo+hoja y no compara el `CHECKSUM_SHA256` (la columna sí se llena). Corregirlo antes de volver a cargar.
- **Incapacidades y accidentes están sin cuadrar** (sus archivos no traen hoja ACUMULADO); no significa que estén mal, pero no hay con qué confirmarlo.
- `NSS` viene vacío en todos los eventos: el Excel solo trae nombre. Si algún día se quiere ligar con el expediente, se resolvería por nombre normalizado (`NOMBRE_NORM`).

## No confundir con

`SERV_MED_IMPORT_LOTE` / `SERV_MED_IMPORT_FILA`: son del botón **"Importar Excel"** de la pantalla del portal (cargas manuales del usuario), no de esta carga histórica.
