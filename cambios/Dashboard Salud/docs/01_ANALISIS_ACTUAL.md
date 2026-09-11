# 01 — Análisis del Excel Actual: `10 REPORTE MORBILIDAD 2026 GERENCIA.xlsm`

> Análisis técnico realizado con Python/openpyxl sobre el archivo original. Este documento es la base factual para el modelo de datos normalizado (`03_MODELO_DATOS.md`). Todo valor de catálogo aquí listado proviene de datos reales observados en el archivo, no de suposiciones.

## 1. Resumen general del libro

- **13 hojas**: ACUMULADO, ATEN, CAUSAS, MUSC-ESQU, EM, INCAP, INC IMSS, INC INTER, ACCID, ANTDPG, ABAST ANTDP, ANTALCOH, MATER.
- **Contiene macros VBA** (`xl/vbaProject.bin`) — no decompiladas, solo se confirma su existencia. No se identificó lógica VBA crítica de negocio a preservar; se asume que las macros son de utilidad (formato, impresión) y no fuente de reglas de negocio.
- **Sin Excel Tables (`ListObjects`)** en ninguna hoja: todo son rangos planos con fórmulas y celdas combinadas (merges).
- **Sin validaciones de datos (`data_validations`)** en ninguna hoja: no existen listas desplegables nativas. Todos los "catálogos" son implícitos — aparecen como encabezados de columna fijos o texto repetido en una columna de captura.
- **Un defined name corrupto** (`_xleta.SUM` → `#NAME?`) sin valor utilizable; se descarta.

## 2. Hallazgo arquitectónico crítico: el archivo es un consolidador, no una fuente primaria

El archivo **no contiene datos primarios propios**. Es ~95% fórmulas de vínculo externo (`='[N]Hoja'!Celda`) que jalan de **8 libros satélite** ubicados en una carpeta de red local:

`C:\Users\Salud AIFA\SERVICIO MEDICO\GERENCIA\GERENCIA REPORTES\8 MORBILIDAD GERENCIA\`

| Índice | Archivo fuente | Alimenta a hojas |
|---|---|---|
| `[1]` | `5 REPORTE INCAPACIDADES 2026 GERENCIA.xls` | INCAP, INC IMSS, INC INTER |
| `[2]` | `1 REGISTRO DIARIO ATENCIONES 2026 GERENCIA.xls` | ATEN, CAUSAS, MUSC-ESQU |
| `[3]` | `2 REPORTE EXAMEN NUEVO INGRESO 2026 GERENCIA.xls` | EM (bloque ingreso) |
| `[4]` | `3 REPORTE EXAMEN PERIODICO 2026 GERENCIA.xls` | EM (bloque periódico) |
| `[5]` | `4 REPORTE EXAMEN POS INCAPACIDAD 2026 GERENCIA.xlsx` | EM (bloque pos-incapacidad) |
| `[6]` | `6 REPORTE ACCIDENTABILIDAD 2026 GERENCIA.xls` | ACCID |
| `[7]` | `7 REPORTE ANTIDOPING 2026 GERENCIA.xlsx` | ANTDPG, ABAST ANTDP, ANTALCOH |
| `[8]` | `8 REPORTE MATERNIDAD 2025 GERENCIA.xls.xlsx` | MATER |

**Implicación de diseño**: para la plataforma nueva, la fuente de verdad no debe modelarse como "un Excel" sino como un ecosistema de 9 archivos (el gerencial + 8 satélites) que hoy alimentan un reporte consolidado por fórmulas de vínculo. La plataforma reemplaza este ecosistema completo mediante captura directa en base de datos; el importador Excel (fase 8) debe poder ingerir tanto el archivo gerencial consolidado como, idealmente, los libros satélite si el usuario aún los produce durante la transición.

**Riesgo**: los valores cacheados en el `.xlsm` pueden estar desactualizados si los vínculos no se han refrescado. El importador debe advertir la fecha de última recalculación y no asumir que el valor cacheado es el estado actual.

## 3. Hoja por hoja

### 3.1 ACUMULADO (A1:P25) — Resumen ejecutivo mensual
Reporte gerencial consolidado, estructura de bloques verticales (no tabular por predio). Columnas: `ACTIVIDAD | ENE..DIC | TOTAL`. Filas = métricas: atenciones generales, examen médico (total/ingreso/periódico/pos-incapacidad), enfermedad general, personas incapacitadas, días incapacitantes, maternidad, accidentabilidad, etc. Las fórmulas de esta hoja referencian **internamente** a las demás hojas del mismo libro (`=ATEN!B23`, `=EM!B24`, `=INCAP!D9`), es decir, es la capa de consolidación de más alto nivel dentro del propio archivo gerencial.

### 3.2 ATEN — Atenciones por predio (A1:AC23)
Predio (17 filas) × 12 meses + total + columna de "atenciones a personal de inclusión". Todas las celdas jalan de `[2]ACUMULADO!<misma col/fila>` en el libro de registro diario de atenciones.

### 3.3 CAUSAS — Atenciones por causa médica (A1:AB23)
Predio × 27 causas médicas + acumulado anual. Jala de un bloque de columnas distinto (`EU:FT`) dentro de la misma hoja `ACUMULADO` del libro `[2]`.

### 3.4 MUSC-ESQU — Lesiones músculo-esqueléticas (A1:Q23)
Predio × 15 tipos de lesión + total. Jala de otro bloque de columnas (`FW:GK`) del mismo libro `[2]`. **Nota de calidad**: filas de totales con rangos de suma inconsistentes (ver §5).

### 3.5 EM — Exámenes médicos (A1:FC146) — hoja compleja multi-bloque
Contiene **4 bloques apilados verticalmente**, cada uno alimentado por un libro fuente distinto:
- Bloque Nuevo Ingreso (filas ~4-34) ← `[3]`
- Bloque Periódico (filas ~55-80) ← `[4]`
- Bloque Pos-Incapacidad (filas ~78-103) ← `[5]`
- Bloque adicional (filas ~101-126)

Cada bloque: predio × 12 meses + total, con desglose adicional por **STATUS** (APTO/NO APTO/CONDICIONADO/INCLUSION) y por **TIPO DE EXAMEN** (EMNI/PRETEST). Esta es la hoja que mapea directamente a los "tipos de examen" y "resultados" pedidos en el brief funcional.

### 3.6 INCAP — Incapacidades (resumen) (A1:R22)
Estructura de 3 niveles: MES (13 valores: `DE 2025, ENE..DIC, A 2027` — periodo de arrastre) × DÍAS ACUMULADOS POR CAUSA (`IMSS | INTERNAS | EG | MATER | RT 1 | RT 2 | TOTAL`) × COSTO DE INCAPACIDADES (valor único anual, no mensual, por eso usa merge vertical) × COSTO TOTAL. Fórmula relevante: `HORAS = DÍAS × 8` (conversión estándar de jornada). Jala de `[1]ACUM GRAL`.

### 3.7 INC IMSS (A1:FG245) — hoja más rica en dimensiones
Predio × [MES (14) × CUENTA (~67) × ÁREA (~22) × 4 tipos de incapacidad (enfermedad general/maternidad/accidente laboral/accidente trayecto) × 14 meses de días subsidiados por tipo × COSTOS × PERSONAS INCAPACITADAS]. Jala de `[1]ACUM IMSS`. Es la candidata natural a tabla de hechos central para "incapacidades IMSS" con dimensiones predio/mes/cuenta/área/tipo.

### 3.8 INC INTER (A1:AF245) — Incapacidades internas
Predio × 14 meses (personas + días subsidiados) + columna de pago monetario único por predio. Jala de `[1]ACUM INT`.

### 3.9 ACCID (A1:EY158) — hoja con más dimensiones cruzadas
Predio × MES(14) × TIPO DE RIESGO (laboral/trayecto) × GÉNERO × CUENTA(~67) × ÁREA(~22) × PUESTO(~34) × CAUSAS DE RT (7: vial, agresión, caída, golpe, herida, ergonómica, improcedente) × COSTOS × STATUS (pendiente/calificado/improcedente/baja inconcluso). Jala de `[6]GENERAL` con un **offset de fila +1** entre hoja destino y fuente. **Nota de calidad**: columna de costos "improcedente" contiene valores negativos (ver §5).

### 3.10 ANTDPG (A1:FC26) — Control de antidoping
Predio × MES × RANGO DE EDAD (18-25/26-35/36-45/46-55/55+) × GÉNERO × AGENCIA (GLI/ORLAMEX/VALLE LERMA/ONEST vs OTRO) × CUENTA(~67) × ÁREA(~22, incluye "CLIENTE/EXTERNO") × PUESTO(~40) × TIPO DE PRUEBA (antidoping/alcoholemia) × RESULTADO (negativo/positivo) × STATUS (N/A/CAPA/no interesado-baja/no contratado/recaída) × SITUACIÓN (laborando/baja-no aplica). Jala de `[7]ACUM ANTID`.

### 3.11 ABAST ANTDP (A1:CU78) — Inventario de kits de antidoping
Estructura **transpuesta**: mes-bloque en fila, predio en columna (única hoja así). Cada mes ocupa un bloque de filas: cantidad inicial / entrega mensual / consumo mensual. Bloque secundario de acumulado mensual consolidado. **Nomenclatura de predios abreviada y distinta** al resto del libro (ver §5). **Mezcla de tipos de dato** en columnas de predio (cantidad numérica, folio de lote como texto, y valores que parecen fechas serializadas de Excel mal tipadas como número) — requiere limpieza dedicada antes de normalizar.

### 3.12 ANTALCOH (A1:B24) — Pruebas de alcoholimetría
La hoja más simple: predio × conteo de pruebas. Jala directo de la columna "ALCOHOLEMIA" del bloque TIPO DE PRUEBA en `[7]ACUM ANTID`.

### 3.13 MATER (A1:EQ25) — Maternidad
Predio × MES(14) × EDAD × CUENTA(~67) × ÁREA(~22) × PUESTO. Jala de `[8]ACUMULADO`. **Nota de calidad**: el encabezado de la dimensión PUESTO no está resuelto a texto literal, es una fórmula sin evaluar en el archivo original (ver §5).

## 4. Catálogos consolidados encontrados

Todos los valores siguientes fueron observados directamente en el archivo. Se preservan como catálogos administrables en la plataforma — **ninguno debe quedar hardcodeado en código**.

### 4.1 Predios (17 + "sin dato")
```
AIFA, ATIZAPAN, CHARCON, FLORA, MACRO I, MACRO II, MERCURIO, MIKELS,
SIGLO XXI, SMO, TOLUCA, TULTIPARK, U TEPALCAPA, WORLD PARK, Z VALLEJO,
FORANEO, SIN DATO
```
**Inconsistencia detectada**: en `ABAST ANTDP` los mismos predios aparecen abreviados: `M1→MACRO I, M2→MACRO II, TULTI→TULTIPARK, UT→U TEPALCAPA, WP→WORLD PARK, Z VALL→Z VALLEJO, FOR→FORANEO`. Requiere tabla de mapeo de alias al construir el catálogo maestro `predios`.

### 4.2 Meses / periodos
Formato estándar `ENE..DIC`, con 3 abreviaturas inconsistentes según hoja: `MAR` vs `MZO`, `MAY` vs `MYO` (solo en INCAP), `AGO` vs `AGS` (solo en ACUMULADO). Varias hojas usan un eje extendido de 14 periodos: `2025 PENDIENTE, ENE..DIC, 2027 ENERO, TOTAL` — arrastre de casos entre ejercicios fiscales. En el modelo normalizado, el año/mes es un atributo de cada registro (nunca una tabla por año — ver `03_MODELO_DATOS.md`), y los "pendientes" de año anterior se modelan como registros con su propio periodo real, no como una columna especial.

### 4.3 Cuentas / clientes (56 + total, catálogo compartido)
```
COMEDOR, MANTENIMIENTO, PREVENCION, SERV GRAL, STAFF, MIKELS, SIGLO XXI, SMO,
UNILEVER TEPALCAPA, FLORA, CLARINS, FISCAL, MILWAUKEE, PLANETA, PUIG,
WOLTER KLAUWER, BSD, 47 BRAND, 5.11 TACTICAL, ADIDDAS, ADOLFO DGUEZ,
COMBIBLOCK, CORTE FIEL, CYTIE, ENVIO PACK, FANDELI, JBL, MADDEN, MARTI,
PIAGUI, RHEEM, ROYAL CANIN, SUBURBIA, UNILEVER CIVAC, UNILEVER POP, SEARS,
CARTERS, HISENSE, P&G MERCURIO, AVANTE, HABERS, U LERMA, ALKA, RICHS,
TARKETT, DIAGEO, CANCUN, CBI, IRAPUATO, JIUTEPEC, MARTI GDL, TEMPE,
ECOMMERCE, CROSS DOCK, ZARA CADENAS, ZARA ALMACEN, WILSON, S/D
```
Idéntico en INC IMSS, ACCID, ANTDPG y MATER. "S/D" (sin dato) se mapea a NULL/desconocido, no es una cuenta real.

### 4.4 Áreas (19 + total)
```
STAFF, CALIDAD, CAPACITACION, COMEDOR, DESPACHO, EMBARQUES, INVENTARIOS,
LOCKERS, MTTO, OPERACION, PATIO, PREVENCION / SEG TRANSPORTE,
REC HUMANOS / RECLUTAMIENTO, RECUPERADO, SALUD OCUP, SEG E HIG,
SERV GRAL / LIMPIEZA, SISTEMAS, TRAFICO
```
(corregido typo "RECUPERADCO"→"RECUPERADO"). En ANTDPG y ACCID se añade `CLIENTE / EXTERNO` como valor adicional.

### 4.5 Puestos (~36 + total, catálogo más granular en ANTDPG)
```
INCLUSION, CLIENTE / EXTERNO, ADMON, ANALISTA, ATN CLIENTES, AUDITOR,
AUTOMATISTA, AUX ALMACEN / AYUDANTE GRAL, BECARIA, CALIDAD, CAPTURISTA,
CHOFER, COCINA, COORDINADOR, COSTURA, DIRECCION, DOCUMENTADOR, EMBARQUES,
FACTURISTA, GERENTE, INVENTARIOS, JEFATURA, LAVADOR TARIMA,
LIMPIEZA / INTENDENCIA, MANIOBRISTA, MANTENIMIENTO, MAQUILA,
MEDICO / ENFERMERA, MESA CONTROL, MONITORISTA / PREVENCION / VIGILANCIA,
MONTACARGISTA, PATINERO, PLANEADOR TR2 / TRACKER, SEG E HIG, SUPERVISOR,
SURTIDOR, TASKER
```
(corregido typo "DIRECCCION"→"DIRECCION").

### 4.6 Causas médicas de atención (26, catálogo del brief funcional confirmado en el archivo)
```
ACIDO PEPTICA, ALERGIA / INTOXICACION, BUCODENTAL, CIRCULATORIO, CURACION,
DERMATOLOGICO, DIGESTIVO, EMBARAZO (DETECCION/CONTROL), ENDOCRINO,
GENITOURINARIO, GINECOLOGICO, INYECCION, NEUROLOGICO, OFTALMICO, OTICO,
PSICOSOMATICO, RESPIRATORIO, TENSION ARTERIAL (DETEC/CONTROL),
TOMA DE GLUCOSA, VACUNA / METODO PF / SEG SALUD, FISIOTERAPIA, NOM 035,
MT - ALGIA / MIALGIA / CONTUSION, MP - ALGIA / MIALGIA / CONTUSION,
GRAL - ALGIA / MIALGIA / CONTUSION, CERVICAL / DORSAL / LUMBAR
```

### 4.7 Tipos de lesión músculo-esquelética (15)
```
AMPUTACION MT, AMPUTACION MP, ALGIA/CONTUSION MT, ALGIA/CONTUSION MP,
CERVICALGIA/DORSALGIA/LUMBALGIA, CONTUSION/TRAUMA CABEZA,
CONTUSION/TRAUMA TRONCO, ESGUINCE MT, ESGUINCE MP, LUXACION MT,
LUXACION MP, FX MT, FX MP, FX CABEZA/TRONCO, POLICONTUNDIDO
```
(MT = miembro torácico, MP = miembro pélvico — nomenclatura clínica estándar).

### 4.8 Causas de riesgo de trabajo / accidente (7)
```
ACCID VIAL, AGRESION, CAIDA, GOLPE, HERIDA, ERGONOMICA, IMPROCEDENTE
```

### 4.9 Catálogos de estado (contextuales)
- **Resultado de examen médico**: APTO, NO APTO, CONDICIONADO, INCLUSION
- **Tipo de examen**: NUEVO INGRESO (EMNI), PERIODICO, POS INCAPACIDAD, PRETEST
- **Status de accidente**: PENDIENTE, CALIFICADO, IMPROCEDENTE, BAJA INCONCLUSO
- **Status de antidoping**: N/A, CAPA, NO INTERESADO/BAJA, NO CONTRATADO, RECAIDA
- **Situación laboral**: LABORANDO, BAJA/NO APLICA
- **Resultado de prueba**: NEGATIVO, POSITIVO
- **Tipo de prueba**: ANTIDOPING, ALCOHOLEMIA
- **Tipo de riesgo**: LABORAL, TRAYECTO
- **Género**: FEMENINO, MASCULINO
- **Rango de edad**: 18-25, 26-35, 36-45, 46-55, 55+
- **Agencia**: GLI, ORLAMEX, VALLE LERMA, ONEST, OTRO
- **Tipo de incapacidad**: ENFERMEDAD GENERAL, MATERNIDAD, ACCIDENTE LABORAL, ACCIDENTE TRAYECTO, INTERNA

## 5. Problemas de calidad de datos y riesgos de migración

1. **Fuente no primaria**: el archivo gerencial es un consolidador de 8 libros satélite vía fórmulas de vínculo externo. Los valores son cacheados y pueden estar desactualizados si no se refrescaron antes de exportar. El importador de la plataforma debe tratar estos valores como snapshot, no como tiempo real.
2. **Rangos declarados muy sobredimensionados** respecto a datos reales (ej. INC IMSS declarada hasta fila 245, datos reales solo ~23 filas). Cualquier parser debe acotar por detección de fin de datos, no confiar en `max_row`/`max_column`.
3. **Nomenclatura de predios inconsistente entre hojas** (abreviada en ABAST ANTDP vs completa en el resto). Requiere tabla de mapeo de alias al poblar el catálogo maestro de predios.
4. **Abreviaturas de mes inconsistentes** (MAR/MZO, MAY/MYO, AGO/AGS) según hoja.
5. **Eje de 14 "meses" mezcla periodo actual con arrastre de año anterior/siguiente** en varias hojas — deben separarse como atributo de periodo real (año+mes), no tratarse como categorías de un catálogo de "mes".
6. **Encabezados dinámicos no resueltos a texto** en MATER (columna PUESTO): el archivo original tiene fórmula sin evaluar en la celda de encabezado; solo el valor cacheado (no la fórmula) da el nombre real.
7. **Mezcla de tipos de dato en la misma columna semántica** en ABAST ANTDP (cantidad/folio de lote/posible fecha serializada mal tipada) — requiere limpieza dedicada fila por fila antes de tipar en el esquema normalizado.
8. **Valores negativos en costos de accidentes "improcedentes"** (ACCID) — deben confirmarse con negocio como ajustes legítimos antes de sumarse en reportes de costo.
9. **Offset de fila +1 entre hoja destino y fuente** en ACCID — relevante solo si se reconstruye la lógica de refresco del Excel; irrelevante si se migra a datos planos.
10. **Filas de totales con rangos de suma inconsistentes** en MUSC-ESQU (`SUM` de 17 filas en unas columnas, 15 en otras) — posible bug heredado que subestima totales; debe validarse contra valores cacheados antes de confiar en el total como fuente de verdad histórica.
11. **Valor cajón de sastre "SIN DATO"/"S/D"** en predio y cuenta — debe mapearse a NULL o fila "desconocido", nunca tratarse como categoría de negocio real.
12. **Sin validaciones nativas ni tablas estructuradas** en ninguna hoja: toda la integridad referencial actual es implícita por convención de encabezado. Debe reconstruirse por completo en el modelo destino (constraints, FKs, catálogos formales).

## 6. KPIs identificados (confirmados en el archivo, insumo para `05_DASHBOARD_KPIS.md`)

- Total de atenciones (mensual/acumulado, por predio, por causa)
- Exámenes médicos: total, por tipo (ingreso/periódico/pos-incapacidad), por resultado (apto/no apto/condicionado/inclusión)
- Personas incapacitadas y días incapacitantes, por tipo (enfermedad general/maternidad/accidente laboral/accidente trayecto/interna)
- Horas no trabajadas (= días × 8)
- Costo de incapacidades (mensual/acumulado anual, por tipo)
- Accidentes: total, por tipo de riesgo (laboral/trayecto), por causa de RT, por status, con costos calificados/improcedentes
- Pruebas de antidoping y alcoholemia: total, por resultado, por predio/cuenta/área/puesto/agencia/edad/género
- Inventario de kits de antidoping: existencia, entregas, consumo, por lote/caducidad
- Casos de maternidad: total, días incapacitantes, por edad/predio/cuenta

## 7. Relaciones identificadas entre hojas

- Todas las hojas de detalle (ATEN, CAUSAS, MUSC-ESQU, EM, INC IMSS, INC INTER, ACCID, ANTDPG, ABAST ANTDP, ANTALCOH, MATER) comparten la dimensión **predio** como eje principal de fila.
- INCAP es un resumen que agrega INC IMSS + INC INTER + maternidad + accidentes en una vista de "incapacidades" transversal.
- ACUMULADO consolida internamente ATEN, EM, INCAP y otras hojas del mismo libro — es el reporte gerencial de más alto nivel.
- ANTALCOH es en realidad un subconjunto de ANTDPG (columna "alcoholemia" del bloque tipo de prueba), no una fuente independiente.
- CAUSAS y MUSC-ESQU son ambas desagregaciones de ATEN por un eje adicional (causa médica / tipo de lesión).

Estas relaciones confirman el diseño de modelo de hechos único de "atención médica" con dimensiones (predio, causa, tipo de lesión) en vez de tablas separadas duplicando la relación predio-mes, y de manera análoga para incapacidades (tabla única con tipo como dimensión, en vez de INC IMSS / INC INTER / MATER separadas).
