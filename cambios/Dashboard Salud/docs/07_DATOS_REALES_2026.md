# 07 — Datos Reales Extraídos del Excel (Ejercicio 2026)

> Valores extraídos con openpyxl (`data_only=True`, valores cacheados) del archivo `10 REPORTE MORBILIDAD 2026 GERENCIA.xlsm`. Estos son los números que el dashboard `docs/mockups/dashboard.html` muestra hoy, y los que el backend debe reproducir vía `/api/dashboard/*` una vez migrados los datos a PostgreSQL.
>
> **Estado de captura**: el archivo tiene datos de **enero a junio de 2026**. Julio–diciembre están en cero (sin captura), salvo arrastres de días de incapacidad y maternidad que se extienden a meses posteriores por la duración de los casos.

## 1. Resumen ejecutivo (hoja ACUMULADO)

| Indicador | ENE | FEB | MZO | ABR | MAY | JUN | Total |
|---|---|---|---|---|---|---|---|
| Atenciones generales | 1,932 | 1,746 | 2,156 | 2,122 | 2,362 | 2,295 | **12,613** |
| Examen médico ingreso | 699 | 722 | 1,418 | 1,241 | 880 | 2,169 | **7,129** |
| Examen médico periódico | 136 | 424 | 212 | 203 | 182 | 109 | **1,266** |
| Examen médico pos-incapacidad | 54 | 41 | 42 | 41 | 41 | 57 | **276** |
| Personas con incapacidad enf. general | 245 | 131 | 247 | 222 | 235 | 326 | **1,406** |
| Días incapacitantes enf. general | 1,194 | 1,073 | 1,308 | 1,123 | 1,072 | 1,301 | **7,259** |
| Personas embarazadas | 42 | 10 | 8 | 4 | 6 | 0 | **70** |
| Personas accidentadas (RT1+RT2) | 73 | 16 | 32 | 27 | 45 | 32 | **225** |
| Días por accidente de trabajo | 314 | 186 | 114 | 144 | 301 | 329 | **1,454** |
| Días por accidente de trayecto | 630 | 433 | 442 | 337 | 316 | 380 | **2,600** |
| Incapacidades internas (personas) | 23 | 36 | 29 | 15 | 19 | 20 | **142** |
| Incapacidades internas (días) | 49 | 66 | 94 | 65 | 114 | 110 | **504** |
| Antidoping aplicados | 91 | 129 | 125 | 133 | 192 | 123 | **793** |
| Kits suministrados | 166 | 411 | 0 | 62 | 82 | 183 | **1,553** (año completo) |

## 2. Incapacidades consolidadas (hoja INCAP)

| Concepto | Valor |
|---|---|
| Personas incapacitadas IMSS | 1,790 |
| Personas incapacitadas internas | 142 |
| Días enfermedad general | 7,259 |
| Días maternidad | 1,515 |
| Días RT1 (accidente laboral) | 1,454 |
| Días RT2 (accidente trayecto) | 2,600 |
| Días internas | 504 |
| **Total días acumulados** | **13,332** |
| **Horas no trabajadas** (días × 8) | **106,656** |

### Costos por tipo (valor único anual)

| Tipo | Monto MXN |
|---|---|
| Enfermedad general | $1,653,355.90 |
| Accidente trayecto | $892,922.08 |
| Maternidad | $504,831.24 |
| Accidente laboral | $486,885.71 |
| Interna | $209,516.55 |
| **Total** | **$3,747,511.48** |

### Días de incapacidad por mes y causa

| Mes | Enf. general | Maternidad | RT1 | RT2 | Interna | Total |
|---|---|---|---|---|---|---|
| ENE | 1,094 | 82 | 302 | 569 | 48 | 2,095 |
| FEB | 1,073 | 128 | 186 | 433 | 66 | 1,886 |
| MZO | 1,308 | 256 | 114 | 442 | 94 | 2,214 |
| ABR | 1,123 | 185 | 144 | 337 | 65 | 1,854 |
| MYO | 1,072 | 245 | 301 | 316 | 114 | 2,048 |
| JUN | 1,301 | 274 | 329 | 380 | 110 | 2,394 |

## 3. Atenciones por predio (hoja ATEN)

| Predio | ENE | FEB | MAR | ABR | MAY | JUN | Total | Inclusión |
|---|---|---|---|---|---|---|---|---|
| AIFA | 543 | 316 | 357 | 556 | 693 | 587 | **3,052** | 24 |
| MACRO II | 311 | 369 | 471 | 492 | 423 | 387 | **2,453** | 358 |
| TOLUCA | 226 | 233 | 350 | 265 | 324 | 317 | **1,715** | 0 |
| MACRO I | 277 | 209 | 367 | 235 | 272 | 275 | **1,635** | 19 |
| WORLD PARK | 137 | 153 | 168 | 192 | 226 | 229 | **1,105** | 6 |
| Z VALLEJO | 150 | 134 | 113 | 104 | 120 | 117 | **738** | 4 |
| TULTIPARK | 93 | 102 | 107 | 75 | 116 | 111 | **604** | 72 |
| MERCURIO | 98 | 119 | 110 | 80 | 72 | 80 | **559** | 0 |
| ATIZAPAN | 36 | 31 | 40 | 43 | 41 | 47 | **238** | 1 |
| CHARCON | 22 | 23 | 27 | 24 | 26 | 96 | **218** | 0 |
| SIGLO XXI | 20 | 39 | 29 | 40 | 25 | 26 | **179** | 0 |
| MIKELS | 9 | 10 | 10 | 11 | 7 | 12 | **59** | 0 |
| U TEPALCAPA | 7 | 8 | 6 | 5 | 17 | 11 | **54** | 0 |
| SMO | 3 | 0 | 1 | 0 | 0 | 0 | **4** | 0 |
| FLORA / FORANEO / SIN DATO | — | — | — | — | — | — | **0** | 0 |
| **TOTAL** | 1,932 | 1,746 | 2,156 | 2,122 | 2,362 | 2,295 | **12,613** | **484** |

## 4. Causas de atención (hoja CAUSAS) — 26 catalogadas

| Causa | Total | | Causa | Total |
|---|---|---|---|---|
| Vacuna / método PF / seg. salud | 1,982 | | Circulatorio | 311 |
| Digestivo | 1,809 | | General – algia/mialgia/contusión | 296 |
| Respiratorio | 1,775 | | Bucodental | 272 |
| Neurológico | 1,401 | | Oftálmico | 231 |
| Tensión arterial (detec./control) | 991 | | Psicosomático | 171 |
| MT – algia/mialgia/contusión | 503 | | Ácido péptica | 166 |
| MP – algia/mialgia/contusión | 482 | | Alergia / intoxicación | 162 |
| Fisioterapia | 409 | | Inyección | 148 |
| Ginecológico | 320 | | Toma de glucosa | 137 |
| Curación | 313 | | Dermatológico | 132 |
| Cervical / dorsal / lumbar | 313 | | Genitourinario | 108 |
| | | | Embarazo (detección/control) | 69 |
| | | | Ótico | 66 |
| | | | Endocrino | 26 |
| | | | NOM 035 | 20 |

**Total acumulado**: 12,613 (coincide con ATEN).

## 5. Lesiones musculoesqueléticas (hoja MUSC-ESQU)

| Tipo | Total |
|---|---|
| Algia/contusión MT | 512 |
| Cervicalgia / dorsalgia / lumbalgia | 494 |
| Algia/contusión MP | 485 |
| Contusión/trauma cabeza | 46 |
| Policontundido | 18 |
| Contusión/trauma tronco | 17 |
| Esguince MP | 10 |
| Esguince MT | 6 |
| Luxación MT | 5 |
| Luxación MP | 1 |
| Amputaciones MT/MP · fracturas MT/MP/cabeza-tronco | 0 |
| **Total** | **1,594** |

## 6. Exámenes médicos por resultado (hoja EM, bloque ingreso)

| Resultado | Total | % |
|---|---|---|
| Condicionado | 4,094 | 57.4% |
| Apto | 1,580 | 22.2% |
| No apto | 1,332 | 18.7% |
| Inclusión | 123 | 1.7% |
| **Total** | **7,129** | 100% |

Por tipo de examen: EMNI 4,082 · Pretest 3,047.

Predios con mayor volumen: AIFA 1,683 · Z Vallejo 1,405 · Macro II 1,071 · Macro I 870 · World Park 746.

## 7. Accidentabilidad (hoja ACCID)

| Concepto | Valor |
|---|---|
| Accidentes totales | 225 |
| Laborales (RT1) | 102 |
| Trayecto (RT2) | 123 |
| Mujeres | 113 |
| Hombres | 112 |

### Causas de riesgo de trabajo

| Causa | Total |
|---|---|
| Caída | 76 |
| Accidente vial | 48 |
| Golpe | 43 |
| Ergonómica | 33 |
| Improcedente | 12 |
| Herida | 7 |
| Agresión | 6 |

### Por predio (laboral / trayecto)

AIFA 16/26 · Macro II 13/20 · Z Vallejo 12/16 · Macro I 12/14 · Toluca 11/4 · World Park 9/12 · Tultipark 9/6 · Siglo XXI 7/6 · Foráneo 5/3 · Mercurio 3/9 · Mikels 3/1 · Atizapán 2/0 · Charcón 0/3 · U Tepalcapa 0/3.

## 8. Antidoping y alcoholimetría

**Antidoping por predio** (total 793): Mercurio 428 · U Tepalcapa 67 · Macro I 63 · Charcón 57 · World Park 42 · Atizapán 33 · Toluca 32 · Macro II 26 · Z Vallejo 17 · AIFA 14 · Mikels 4 · Siglo XXI 4 · Foráneo 4 · Tultipark 2.

**Alcoholimetría** (total 187): Mercurio 167 · World Park 8 · Macro II 6 · AIFA 3 · Charcón 1 · Macro I 1 · Tultipark 1.

> Observación de negocio: Mercurio concentra el 54% de los antidoping y el 89% de las alcoholimetrías — conviene confirmar si responde a un programa específico de ese predio o a un sesgo de captura.

## 9. Días de incapacidad IMSS por predio

Macro II 295 · AIFA 293 · Z Vallejo 219 · Macro I 200 · Mercurio 160 · Toluca 155 · World Park 124 · Siglo XXI 82 · Tultipark 80 · Charcón 67 · Foráneo 44 · U Tepalcapa 40 · Mikels 18 · Atizapán 13 · Flora 0 · SMO 0.

## 10. Notas para el backend

1. **El semáforo de riesgo del ranking de predios** se calcula en el mockup con umbrales provisionales (crítico ≥70% del máximo de atenciones o ≥280 días; alto ≥40% o ≥180 días; medio ≥12% o ≥60 días). Estos valores deben migrar a la tabla `risk_thresholds` y ser configurables desde Administración, **nunca quedar en código**.
2. **Los meses sin captura (JUL–DIC) no deben graficarse como cero real**: el backend debe distinguir "sin datos" de "cero eventos" al responder `/api/dashboard/trends`, para que el frontend corte la serie en lugar de dibujar una caída a cero.
3. **Los costos son un valor único anual**, no mensual (así vienen en el Excel). Al migrar a `disability_costs`, hay que decidir con negocio si se prorratean por mes o se mantienen como total del ejercicio.
4. **`ACUMULADO!O15` (días de maternidad) muestra 0 pese a tener valores mensuales** que suman 1,515 — es un error de fórmula heredado en el Excel origen. El dashboard usa la suma real (1,515), no el total del archivo.
5. El total de exámenes médicos mostrado como KPI (8,671) es la suma de ingreso (7,129) + periódico (1,266) + pos-incapacidad (276). El desglose por resultado (apto/no apto/condicionado/inclusión) solo existe para el bloque de ingreso.
