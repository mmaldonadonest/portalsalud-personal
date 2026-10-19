# Carga histórica de MORBILIDAD — hallazgos del análisis

**Fecha:** 29-sep-2026 · **Estado:** en espera de respuestas de servicio médico
**Cuestionario entregado:** `docs/cuestionario-carga-historica.xlsx` (y `.docx`)
**Generador del cuestionario:** `docs/gen_xlsx.py`

> Nada de esto se ejecutó contra ninguna base. Todo fue lectura local de los archivos.

---

## 1. Qué hay en el origen

`C:\Users\Miguel\Downloads\MORBILIDAD\` — **1,072 archivos numerados**, 10 familias, **34 predios**, años **2017 a 2026**.

| # | Familia | Archivos | Años |
|---|---|---|---|
| 1 | REGISTRO DIARIO ATENCIONES | 111 | 2017–2026 |
| 2 | REPORTE EXAMEN NUEVO INGRESO | 112 | 2017–2026 |
| 3 | REPORTE EXAMEN PERIODICO | 107 | 2017–2026 |
| 4 | REPORTE INCAPACIDADES | 109 | 2017–2026 |
| 5 | REPORTE EXAMEN POS INCAPACIDAD | 106 | 2017–2026 |
| 6 | REPORTE ACCIDENTABILIDAD | 110 | 2017–2026 |
| 7 | REPORTE ANTIDOPING | 109 | 2017–2026 |
| 8 | REPORTE MATERNIDAD | 110 | 2017–2026 |
| 9 | REPORTE CONSUMIBLES | 104 | 2017–2026 |
| 10 | REPORTE MORBILIDAD (resumen mensual) | 91 | 2017–2025 |

Las 10 familias encajan casi 1:1 con los módulos y las pantallas `analisis-*` que ya existen en el portal.

**Volumen estimado** (no contado): ~2,000 atenciones por predio-año, 4,092 en el mayor → del orden de **200–250 mil atenciones** en la familia 1.

---

## 2. Formato de los archivos

**No son listados de registros: son matrices de "marca con 1".** Cada atributo se despliega en decenas de columnas y la celda lleva un `1`. Un renglón = una atención. Encabezado de **dos filas** (grupo combinado arriba, categoría abajo).

### Dos épocas estructurales

- **2017–2022:** 12 hojas mensuales (`ENE`…`DIC`) + `ACUMULADO` + hojas de gráficas.
- **2023–2026:** una sola hoja con todo el año, y un bloque `MES` de 12 columnas one-hot que sustituye a lo que antes daba la hoja.

`ACUMULADO` es la única hoja presente en los 10 años, pero es resumen, no detalle.

### Muestra medida (linaje MACRO I, familia 1)

| Año | Hoja | Fila hdr | Cols reales | Filas | Cambio |
|---|---|---|---|---|---|
| 2017 | ENE | 4 | 115 | 127 | — |
| 2018 | ENE | 4 | 181 | 206 | CUENTA 30→59, PUESTO 18→54 |
| 2019 | ENE | 4 | 182 | 297 | **CUENTA desaparece**, AGENCIA→73 |
| 2020 | ENE | 4 | 208 | 275 | vuelve CUENTA(59), PUESTO(73) |
| 2021 | ENE | 4 | 216 | 225 | **aparece PREDIO(11) y AREA(33)** |
| 2022 | ENE | 4 | 184 | 299 | PREDIO(16), AGENCIA cae a 2 |
| 2023 | M1 | 4 | 182 | 2,014 | **PREDIO → MES(12)** |
| 2024 | M1 | 4 | 187 | 2,021 | **SEXO → GENERO** |
| 2025 | MACRO 1 | 4 | 187 | 4,092 | — |
| 2026 | MACRO 1 | **3** | 187 | 1,735 | reaparece `N°` |

### Trampas técnicas verificadas

1. **La fila del encabezado se mueve:** 4 en nueve años, **3 en 2026**.
2. **2024 y 2025 declaran 16,336 columnas** por formato residual; el ancho real es 187. No confiar en `max_column`.
3. **Tres formatos:** `.xlsx` (2017–18), `.xlsm` con macros (2019–25), **`.xls` OLE2 binario** (2026 y AIFA 2025). Verificados los magic bytes: los `.xls` son binario real, no renombrados. `openpyxl` no los abre — hace falta **`xlrd`**.
4. **El año de la carpeta no siempre es el del archivo.** `MORBILIDAD 2018/TOLUCA/` contiene el de 2019; `MORBILIDAD 2019/SIGLO XXI/` el de 2020; `MORBILIDAD 2021/` guarda dos de 2022. **Tomar el año del contenido, nunca de la ruta.**
5. **El prefijo numérico no identifica la categoría.** En 2026 ZARA el 4 y el 5 están intercambiados; el `7` de 2022 ZARA es un pos-incapacidad; el `8` de 2022 ZARA es consumibles. **Clasificar por el texto del nombre, no por el número.**
6. **Las hojas de detalle traen renglones de etiqueta mezclados** (`TOTAL`, `ACUMULADO GENERAL`) en posiciones variables — en ENE 2017 los datos terminan en la fila 108 y las etiquetas están en la 123 y 125. Hay que excluirlos.
7. **Duplicados por copia:** `TOLUCA.xlsm` junto a `TOLUCA1.xlsm`, `SIGLO XXI (5).xlsm`, `MACROII (1).xlsm`, `8 REPORTE MATERNIDAD 2026 Z VALLEJO.xls.xlsx` (doble extensión). `2025/AIFA/` contiene un archivo de GERENCIA.
8. **Fórmulas rotas:** `#REF!` en el ACUMULADO de `5 POS INCAPACIDAD 2023 CPA`.

### Calidad del marcado — alta

Renglones con exactamente una marca por bloque:

```
2026 M1 (1,636 atenciones)          2017 ENE (103 atenciones)
  RANGO DE EDAD   99.8%               EDAD              99.0%
  GENERO          99.9%               SEXO              99.0%
  CUENTA          99.9%               CUENTA            99.0%
  RAMO            99.9%               RAMO              99.0%
  CAUSA           99.8%               CAUSA             98.1%
```

`CAUSAS MUSCULO ESQUELETICAS` sale en 9–16%, pero es correcto: solo se llena en riesgo de trabajo.

**Verificado que los resúmenes cuadran con el detalle:** ENE 2017 MACRO I tiene 103 renglones numerados y `ACUMULADO` dice 103. Los `ACUMULADO` son confiables.

---

## 3. Lo estable (la buena noticia)

| Dimensión | Estabilidad |
|---|---|
| **Rango de edad** | Los **mismos 5 buckets los 10 años**. Solo `+ 55` → `55 +` en 2024 |
| **Género** | `FEM` / `MASC`, idéntico los 10 años |
| **Ramo** | 4 valores estables (`ENF GRAL`, `SEGUIMIENTO`, `PASE SALIDA`, `RT`) |
| **Modelo conceptual** | Los mismos 12–13 grupos los 10 años, mismo diseño one-hot |

---

## 4. La deriva de causas — 45 etiquetas, solo 6 en los 10 años

Las seis estables: `ACIDO PEPTICA`, `ALERGIA / INTOXICACION`, `CERVICAL / DORSAL / LUMBAR`, `CURACION`, `INYECCION`, `TENSION ARTERIAL (DETEC/CONTROL)`.

**En 2019–2020 el catálogo pasó de nombrar síntomas a nombrar aparatos.** Ésa es la frontera que parte la historia en dos. De 2020 en adelante solo difieren 7–8 etiquetas.

```
ETIQUETA DE CAUSA                         17  18  19  20  21  22  23  24  25  26   años
ACIDO PEPTICA                              X   X   X   X   X   X   X   X   X   X   10
ALERGIA / INTOXICACION                     X   X   X   X   X   X   X   X   X   X   10
ALGIA/CONT MP                              X   X   X   X   X   X   X   X   X   ·   9
ALGIA/CONT MT                              X   X   X   X   X   X   X   X   X   ·   9
BUCAL                                      X   X   ·   ·   ·   ·   ·   ·   ·   ·   2
BUCODENTAL                                 ·   ·   X   X   X   X   X   X   X   X   8
CEFALEA                                    X   X   X   X   X   X   X   X   X   ·   9
CERVICAL / DORSAL / LUMBAR                 X   X   X   X   X   X   X   X   X   X   10
CIRCULATORIO                               ·   ·   ·   X   X   X   X   X   X   X   7
CONTROL PESO                               ·   X   X   X   X   ·   ·   ·   ·   ·   4
CURACION                                   X   X   X   X   X   X   X   X   X   X   10
DERMATOLOGICA                              X   X   X   ·   ·   ·   ·   ·   ·   ·   3
DERMATOLOGICO                              ·   ·   ·   X   X   X   X   X   X   X   7
DIABETES MELLITUS (DETEC/CONTROL)          X   X   X   X   X   ·   ·   ·   ·   ·   5
DIGESTIVO                                  ·   ·   X   X   X   X   X   X   X   X   8
DISMENORREA                                X   X   X   ·   ·   ·   ·   ·   ·   ·   3
EMBARAZO (DETECCION/CONTROL )              X   X   ·   ·   ·   ·   ·   ·   ·   ·   2
EMBARAZO (DETECCION/CONTROL)               ·   ·   X   X   X   X   X   X   X   X   8
ENDOCRINO                                  ·   ·   ·   X   X   X   X   X   X   X   7
FISIOTERAPIA                               ·   ·   ·   ·   ·   ·   ·   ·   X   X   2
GASTROINTESTINAL                           X   X   ·   ·   ·   ·   ·   ·   ·   ·   2
GENITOURINARIO                             ·   ·   X   X   X   X   X   X   X   X   8
GINECOLOGICO                               ·   ·   ·   X   X   X   X   X   X   X   7
GRAL - ALGIA / MIALGIA /CONTUSION          ·   ·   ·   ·   ·   ·   ·   ·   ·   X   1
INYECCION                                  X   X   X   X   X   X   X   X   X   X   10
MP - ALGIA / MIALGIA /CONTUSION            ·   ·   ·   ·   ·   ·   ·   ·   ·   X   1
MT - ALGIA / MIALGIA /CONTUSION            ·   ·   ·   ·   ·   ·   ·   ·   ·   X   1
NAZAL                                      X   ·   ·   ·   ·   ·   ·   ·   ·   ·   1
NEUROLOGICO                                ·   ·   ·   X   X   X   X   X   ·   X   6
NOM 035                                    ·   ·   ·   ·   ·   ·   ·   ·   ·   X   1
OFTALMICO                                  ·   ·   ·   ·   ·   ·   ·   ·   ·   X   1
OFTALMOLOGICA                              X   X   X   X   X   X   X   X   X   ·   9
OTICA                                      X   X   ·   ·   ·   ·   ·   ·   ·   ·   2
OTICO                                      ·   ·   X   X   X   X   X   X   X   X   8
POLICONTUNDIDO                             X   ·   ·   ·   ·   ·   ·   ·   ·   ·   1
PROMOCION SALUD (VAC/ANTICONCEP)           X   X   X   X   X   X   X   X   X   ·   9
PSICOSOMATICA                              X   X   ·   ·   ·   ·   ·   ·   ·   ·   2
PSICOSOMATICO                              ·   ·   X   X   X   X   X   X   X   X   8
RESPIRATORIO                               ·   ·   X   X   X   X   X   X   X   X   8
SEGUIMIENTO A LA SALUD Y/O TRAMITES        X   X   X   X   X   X   X   X   X   ·   9
TENSION ARTERIAL (DETEC/CONTROL)           X   X   X   X   X   X   X   X   X   X   10
TOMA DE GLUCOSA                            ·   ·   ·   ·   ·   X   X   X   X   X   5
VACUNA / METODO PF / SEG SALUD             ·   ·   ·   ·   ·   ·   ·   ·   ·   X   1
VIAS RESPIRATORIAS                         X   X   ·   ·   ·   ·   ·   ·   ·   ·   2
VIAS URINARIAS                             X   X   ·   ·   ·   ·   ·   ·   ·   ·   2
```

> Esto sale del **linaje MACRO I**. Otros predios pueden traer causas que MACRO I nunca usó: **45 es un piso, no el total.**

### Tipo A — renombres 1:1 (se pueden proponer con evidencia; uno arranca el año en que el otro se apaga)

| Hasta | Desde | Corte |
|---|---|---|
| `VIAS RESPIRATORIAS` | `RESPIRATORIO` | 2019 |
| `GASTROINTESTINAL` | `DIGESTIVO` | 2019 |
| `VIAS URINARIAS` | `GENITOURINARIO` | 2019 |
| `BUCAL` | `BUCODENTAL` | 2019 |
| `OTICA` | `OTICO` | 2019 |
| `PSICOSOMATICA` | `PSICOSOMATICO` | 2019 |
| `DERMATOLOGICA` | `DERMATOLOGICO` | 2020 |
| `OFTALMOLOGICA` | `OFTALMICO` | 2026 |
| `ALGIA/CONT MT` | `MT - ALGIA / MIALGIA /CONTUSION` | 2026 |
| `ALGIA/CONT MP` | `MP - ALGIA / MIALGIA /CONTUSION` | 2026 |

Y uno que solo es espacio en blanco: `EMBARAZO (DETECCION/CONTROL )` vs `EMBARAZO (DETECCION/CONTROL)`. Se arregla normalizando.

### Tipo B — fusiones y divisiones (solo servicio médico puede decidir)

`CEFALEA`→? · `DIABETES MELLITUS`→`ENDOCRINO` y/o `TOMA DE GLUCOSA` (división 1→2) · `DISMENORREA`→`GINECOLOGICO` · `CONTROL PESO`→? · `NAZAL`→? · `SEGUIMIENTO SALUD` + `PROMOCION SALUD`→`VACUNA / METODO PF / SEG SALUD` · `POLICONTUNDIDO` está en `CAUSA` (2017) **y** en `CAUSAS MUSCULO ESQUELETICAS` los 10 años → riesgo de doble conteo.

### Tipo C — nuevas sin historia

`NOM 035` (2026), `FISIOTERAPIA` (2025–26), `TOMA DE GLUCOSA` (2022+). Necesitan nota en la gráfica: una barra que arranca en 2025 no significa que la actividad empezó ese año.

---

## 5. El lado del portal

### El origen de las gráficas NO coincide con los layouts

`DashboardConsultaService.resumen()` llama a un **WS de ORDS en vivo** (`expedienteService.reportePorFecha`) y **agrega en memoria** con `Map`s. **No hay tabla de hechos.** El javadoc del DTO dice literalmente *"KPIs de consultas medicas (Morbilidad)"* — el dashboard se diseñó contra este mismo modelo.

### Dos frenos en el código (no en el Excel)

1. **`.filter(f -> f.nss() != null && !f.nss().isBlank())`** — descarta toda fila sin NSS. **El Excel no tiene NSS en ningún año**, solo `NOMBRE COMPLETO`. Tal cual está, tiraría el 100% de lo histórico.
2. **`rangoEdad(int edad)`** — el servicio **calcula** el bucket desde la edad; el Excel **trae el bucket** y no la edad. Van en direcciones opuestas. Además el servicio produce `"18-25"` y el Excel dice `"18 - 25"` (con espacios): sin normalizar, las series salen duplicadas.

### Mapeo `ConsultaReporteDto` ← Excel

| Campo | Excel | |
|---|---|---|
| `fechaConsulta` | `FECHA` | ✅ |
| `nombre` | `NOMBRE COMPLETO` | ✅ |
| `genero` | `SEXO` / `GENERO` | ✅ mapear FEM/MASC |
| `cuenta` | `CUENTA` | ✅ |
| `causa` | `CAUSA` | ✅ |
| `diagnostico` | `DIAGNOSTICO` | ✅ texto libre, no CIE-10 |
| `edad` | solo el bucket | ⚠️ dirección opuesta |
| `tipoConsulta` | `RAMO` | ⚠️ `esAccidente()` busca "accidente"/"emergencia"; el Excel dice `RT` → **cero accidentes históricos** sin mapeo |
| `areaAccidente` | `AREA`, solo desde 2021 | ⚠️ |
| `nss`, `idConsulta`, `rfc`, `curp` | — | ❌ |

El javadoc de `diagnostico` ya dice *"null/vacio en **la carga historica**"* — alguien ya lo previó.

### El importador existe pero no llega a ningún lado

El paquete `com.onest.app.importer` valida y guarda en `SERV_MED_IMPORT_LOTE` / `SERV_MED_IMPORT_FILA`, pero su propio comentario dice: *«Todo queda en staging porque aún no hay layouts destino»*.

### Hallazgo que cierra un pendiente viejo

**Los reportes de examen (familias 2, 3 y 5) traen `APTO` / `APTO CONDICIONADO` / `NO APTO` desde 2017.** El dictamen que se creía no registrado sí existe — vive en estos Excel, no en el portal. Es el dato del que dependería la *blacklist*. Confirmado en los archivos de 2017; **no confirmado en los recientes** porque la lectura cortó a 90 columnas.

---

## 6. Decisiones ya tomadas por el usuario

- **Carga por batch**, no por interfaz. El importador actual se queda para cargas manuales sencillas.
- **Match por nombre aceptado**, sabiendo que se pierden casos. *Para las gráficas casi no importa:* todas las dimensiones (causa, género, edad, cuenta, predio, tendencia) salen del propio renglón. El nombre solo se necesita para `totalPersonas` (NSS distintos) y para ligar al expediente. **Se puede cargar sin resolver identidad y las gráficas quedan completas.**
- Los encabezados de 2024+ piden *"SIN ACENTOS"* y los 10 años piden *"empezando por apellido"* → el orden es consistente.

## 7. Lo que hay que construir

1. **Tabla de hechos de atenciones históricas** — ya transpuesta, con el origen (archivo + hoja + fila) para auditar cualquier cifra hasta su celda. **Guardar el valor crudo del Excel Y la columna canónica derivada**, para poder recalcular sin releer 1,072 archivos.
2. **Catálogo de equivalencias de causa** (§4).
3. **Tabla de control de carga** — archivo, checksum, fecha, filas; para que el batch sea reejecutable sin duplicar. `SERV_MED_IMPORT_LOTE` sirve de molde.
4. **Que `DashboardConsultaService` lea de las dos fuentes** — ORDS para lo vigente, la tabla para lo histórico. Ahí se resuelven los dos frenos.

Nada de esto toca los WS de ORDS ni el PHP.

---

## 8. Estado del cuestionario — PENDIENTE

`docs/cuestionario-carga-historica.xlsx`: 16 preguntas, **35 celdas con lista desplegable**, 15 listas, hoja `Listas` oculta, celdas sin contestar en rojo.

**Corrección importante:** el encabezado dice *"solo preguntas que impiden avanzar"* y **no es exacto**. Casi todas bloquean que las gráficas sean *correctas*, no que el desarrollo *arranque* — porque si la tabla guarda el crudo + banderas, casi todo es reversible.

- **Bloquea arrancar:** solo la **P1** (alcance: cuántos cargadores y de qué forma). Y aun así, *atenciones diarias* está dentro con seguridad.
- **Bloquea publicar, no construir:** P5, P6, P7 (canon de causas) · P3, P4, P11 (qué entra en los totales → banderas) · P12 (días a caballo del año → guardar fechas) · P14 (qué distingue RT1 de RT2).
- **No bloquea nada:** P2, P8, P10, P13, P15, P16.

**Decisión pendiente del usuario** — se ofrecieron tres opciones y no eligió:

1. *(recomendada)* agregar columna **"¿Cuándo se necesita?"** con Bloquea el inicio / Antes de publicar / Puede esperar;
2. recortar a las 8 que bloquean inicio o publicación;
3. dejarlo y solo corregir el encabezado.

**Al retomar: preguntar si ya contestaron y cuál de las tres opciones quiere.**
