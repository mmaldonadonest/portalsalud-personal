# Preguntas para consolidar la carga de los Excel de morbilidad

**Para:** Servicio Médico y Salud Ocupacional
**Fecha:** 9-oct-2026
**Relacionado:** plantilla `docs/layout-excel-propuesta.xlsx` y revisión de los Excel de 2026

Contesten debajo de cada pregunta. Las marcadas con ⛔ detienen la carga o la plantilla.

---

## A. Plantilla nueva (captura hacia adelante)

**A1. Equivalencia del STATUS de exámenes.** RESUELTA (9-oct): APTO (15) = APTO; CONDICIONADO (45) = APTO_CONDICIONADO, enfermedad controlable con seguimiento (obesidad, hipertensión…); INCLUSIÓN (60) = APTO_RESTRINGIDO, padecimiento incurable o limitación física (p. ej. problemas para caminar); NO APTO (30) = NO_APTO.

**A2. Costo de incapacidades internas.** RESUELTA (9-oct): el COSTO se captura directo en la plantilla, junto con SDI o PAGO (60%); ya no se calcula.

**A3. SDI o PAGO 60 %.** RESUELTA (9-oct): se capturan SDI, PAGO (60%) y COSTO; no se pierde el costo aunque falte el SDI.

**A4. Ramo de incapacidades: RT1 o RT 1.** Hoy se escribe de las dos formas (96 % sin espacio). ¿Usamos RT1 / RT2?

**Respuesta:**

**A5. Fecha del accidente.** El Excel de accidentes no trae fecha del accidente, solo el mes reportado. ¿Se puede capturar la fecha real, o tomamos la fecha de inicio del ST2?

**Respuesta:**

**A6. Días pagados en accidentes improcedentes.** El Excel calcula días pagados = días IMSS − 3. ¿Siempre se descuentan 3 días? (En Toluca hay 3 casos capturados a mano que no cumplen la regla.)

**Respuesta:**

**A7. Los dos STATUS de accidentes.** ¿Qué diferencia hay entre STATUS (pendiente / calificado / improcedente / baja inconcluso) y el segundo STATUS (pendiente / concluido / baja)? ¿Se necesitan los dos?

**Respuesta:**

**A8. Catálogo de ÁREA.** Tomamos 19 áreas del Excel de incapacidades de AIFA (STAFF, CALIDAD, CAPACITACIÓN, COMEDOR, DESPACHO, EMBARQUES, INVENTARIOS, LOCKERS, MTTO, OPERACIÓN, PATIO, PREVENCIÓN / SEG TRANSPORTE, REC HUMANOS / RECLUTAMIENTO, RECUPERADO, SALUD OCUP, SEG E HIG, SERV GRAL / LIMPIEZA, SISTEMAS, TRÁFICO). ¿Es la lista completa y vigente para todos los predios?

**Respuesta:**

**A9. Status de maternidad.** ¿El catálogo es LABORANDO EMBARAZADA, INCAPACIDAD, LACTANCIA, LABORANDO, TÉRMINO, BAJA, ABORTO? Hoy también escriben "LABORANDO EMB".

**Respuesta:**

**A10. Status de antidoping.** En 2026 son LABORANDO y BAJA / NO CONTRATADO. ¿Hay algún otro valor que usen?

**Respuesta:**

**A11. Examen de nuevo ingreso.** ¿Necesitan capturar ESTADO CIVIL y ESCOLARIDAD? (Escolaridad dice "SIN DATO" en 2 de cada 3 renglones.) ¿Y RESULTADOS PENDIENTES? ¿Qué significa "SDPA"?

**Respuesta:**

**A12. Diagnóstico y tratamiento.** Se quedan como texto libre, con la lista de valores actuales solo como referencia. ¿Les sirve así, o prefieren un catálogo corto?

**Respuesta:**

**A13. Consumibles.** ¿Necesitan lote y fecha de caducidad para su control? (Ninguna gráfica los usa.)

**Respuesta:**

---

## B. Carga de 2026

**B1. ⛔ Fecha de corte entre Excel y portal.** ¿Desde qué fecha capturan solo en el portal? Los Excel de esos meses ya no se cargan, para no contar dos veces.

**Respuesta:**

**B2. Excel de julio a septiembre 2026.** ¿Nos los pueden enviar? Lo cargado llega hasta junio.

**Respuesta:**

**B3. Folios de incapacidad repetidos (Toluca).** El folio UW713238 aparece en dos personas distintas (Mondragón Reyes Abigail y Rosales Romero Guillermo, ambos 26/01/2026) y el PG629693 dos veces para González González Roberto (01/06/2026). ¿Cuál es el correcto en el primero, y el segundo es renglón duplicado?

**Respuesta:**

**B4. Folios de maternidad sin su incapacidad.** 11 de los 28 folios de maternidad 2026 no están en el Excel de incapacidades 2026 (recontado 9-oct; antes se reportaron 12):

- 6 empezaron en 2025: MT297081 (AIFA), MT309382, MT298814, MT299676 (M1), MR293082, MR296018 (TOLUCA).
- 5 empezaron en 2026 y faltan en el Excel de incapacidades del predio: MT319806 (MACRO 2), MT344122 (AIFA), MR290050 (TOLUCA), MR297149 (SIGLO XXI), TM344302 (VALLEJO).

Archivo y celda de cada uno en `docs/plan-carga-excel-layout.xlsx`, pestaña FOLIOS MATERNIDAD. ¿Nos pasan esas incapacidades (inicio, término, días), o capturamos las fechas en la hoja de maternidad? ¿Por qué las 5 de 2026 no están en incapacidades?

**Respuesta:**

**B5. Exámenes pos incapacidad sin incapacidad.** De 277 exámenes pos incapacidad de 2026, 44 no tienen ninguna incapacidad con ese nombre en el Excel de incapacidades. Solo 5 empezaron en 2025; los otros 39 empezaron en 2026 (p. ej. 6 de AIFA en enero). ¿Falta capturar esas incapacidades, son de otro predio, o el nombre está escrito distinto?

**Respuesta:**

**B6. Identificar a las personas de 2026.** Los Excel no traen NSS. ¿Aceptan que lo cargado de 2026 se ligue por nombre contra el biométrico, revisando a mano los que no coincidan?

**Respuesta:**

**B7. Nombres de predio.** En 2026 un predio aparece de varias formas en hojas y carpetas (MKLS / MIKELS, UT / U TEPALCAPA, M1 / MACRO 1, WP / WORLD PARK, ZARA / Z VALLEJO). ¿Nos confirman la lista oficial y a cuál corresponde cada abreviatura?

**Respuesta:**
