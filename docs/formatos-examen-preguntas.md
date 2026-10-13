# Exámenes médicos periódico, cambio de puesto, egreso, post incapacidad y mantenimiento — preguntas que detienen el trabajo

**Para:** Servicio Médico y Salud Ocupacional
**Formatos revisados:** FT-SO-17 (periódico), FT-SO-16 (cambio de puesto), FT-SO-18 (egreso), FT-SO-13 (post incapacidad), FT-SO-19 (inicial mantenimiento), FT-SO-20 (periódico mantenimiento) — todos revisión 05
**Fecha:** 7-oct-2026

---

## En resumen

Los seis formatos son **el mismo examen médico que el portal ya captura** (el de admisión, FT-SO-04), con otro acomodo y entre 2 y 6 campos propios cada uno. El portal ya guarda prácticamente todo el contenido clínico; lo que falta es **imprimir cada formato con su cara** y capturar esos pocos campos.

Para hacerlo bien necesitamos **9 respuestas**. Las tres primeras detienen a los seis formatos; las demás, a uno cada una. Contesten debajo de cada pregunta, en este mismo documento.

| | |
|---|---|
| **Lo que ya podemos hacer sin respuesta** | El selector del tipo de examen, cintura/cadera/SpO2, y las plantillas de impresión con los campos que ya existen. |
| **Lo que está detenido** | Las 9 preguntas de abajo. Marcamos con ⛔ las que detienen todo. |

---

## Preguntas

### ⛔ P1. ¿Qué significa cada casilla del "RESULTADO DEL EXAMEN"?

Los seis formatos tienen cuatro casillas: **APTO 15 · APTO 45 · CLAVE 60 · CLAVE 30** (en el FT-SO-19 la segunda dice "APTO CONDICIONADO 45"; en el 16 y el 18 dice "APTO 1945", que suponemos es error de captura).

El portal guarda cuatro resultados: **Apto · Apto condicionado · Apto restringido (inclusión) · No apto**. Necesitamos la equivalencia exacta para marcar la casilla correcta al imprimir.

Nuestra suposición (confírmenla o corríjanla):

| Casilla del formato | ¿Es…? |
|---|---|
| APTO 15 | Apto |
| APTO 45 / APTO CONDICIONADO 45 | Apto condicionado |
| CLAVE 60 | Apto restringido (inclusión) |
| CLAVE 30 | No apto |

¿Qué significan los números 15, 45, 60 y 30? ¿Son claves de un catálogo de RH / IMSS o días de vigencia? Si son vigencia, ¿el portal debe calcular la fecha del próximo examen con ellos?

**Respuesta:**

### ⛔ P2. ¿Quién firma cada examen?

Los seis formatos traen impreso **"DR. MAURICIO CERÓN SOLANA — CP: 5154089"** como firma del médico, y la firma del trabajador bajo la leyenda "Yo certifico que los datos…".

- ¿La firma del médico es **siempre** la del Dr. Cerón (fija, aunque capture otro médico o enfermero), o debe salir **el nombre y cédula de quien aplicó el examen**?
- Si es quien aplicó: ¿tienen la cédula de cada médico/enfermero del servicio? El portal hoy no la guarda.
- ¿La firma del médico se dibuja en pantalla o basta el nombre impreso?

**Respuesta:**

### ⛔ P3. Cuando una persona tiene varios exámenes, ¿necesitan volver a imprimir uno anterior?

Ejemplo: alguien con examen de admisión (2024), periódico (2025) y periódico (2026). ¿Necesitan reimprimir el de 2024 tal cual se hizo, o solo el más reciente?

Importa porque el sistema anterior **guarda un solo examen por persona** (el nuevo pisa al anterior). El portal ya conserva un historial con fecha, tipo y resultado de cada examen finalizado; conservar **el contenido completo** de cada uno para reimprimirlo es posible, pero hay que decidirlo antes de construir los formatos.

**Respuesta:**

### P4. Periódico (FT-SO-17): ¿cada cuánto se aplica y de dónde sale "puesto inicial"?

- ¿Cada cuántos meses toca el examen periódico? ¿Es igual para todos los puestos? (Sirve para avisar en el portal a quién le toca.)
- "PUESTO INICIAL": ¿es el puesto con el que ingresó (lo tomaríamos del examen de admisión) o lo escribe el médico cada vez?
- "ESTUDIOS REALIZADOS Y RESULTADOS": ¿es texto libre que escribe el médico, o la lista de laboratorios/estudios que ya están subidos en el expediente (E. Laboratorio)?

**Respuesta:**

### P5. Cambio de puesto (FT-SO-16): ¿quién lo pide y qué es "motivo del cambio"?

- ¿Quién dispara este examen: RH avisa al servicio médico, o el médico lo decide? (¿Debería llegar como aviso al portal, como hoy llegan los de OCR?)
- "MOTIVO DEL CAMBIO": ¿texto libre o un catálogo (promoción, reubicación, restricción médica, solicitud del trabajador…)? Si es catálogo, pásennos la lista.
- ¿Es un examen completo o solo se revisan las secciones que afecta el nuevo puesto?

**Respuesta:**

### P6. Egreso (FT-SO-18): "motivo de salida" y "estudios durante su periodo laboral"

- "MOTIVO DE SALIDA": ¿catálogo (renuncia, término de contrato, despido, jubilación…) o texto libre?
- "ESTUDIOS REALIZADOS DURANTE SU PERIODO LABORAL Y RESULTADOS": ¿quieren que el portal **liste automáticamente** los estudios y exámenes que la persona tuvo mientras trabajó (ya los tiene), o lo escribe el médico?
- "ANTIGÜEDAD": ¿la calculamos de la fecha de ingreso que trae el biométrico, o la escribe el médico?
- ¿El resultado del examen de egreso usa las mismas cuatro casillas? (Un egreso "no apto" no tiene mucho sentido; ¿o aquí significa "con lesiones atribuibles al trabajo"?)

**Respuesta:**

### P7. Post incapacidad (FT-SO-13): ¿qué incapacidad y de dónde salen diagnóstico y tratamiento?

- El formato pide **días, tipo, diagnóstico y tratamiento** de la incapacidad. El portal guarda la incapacidad (ramo, tipo, fechas, días) pero **no** su diagnóstico ni tratamiento. ¿Los capturamos en el módulo de Incapacidades (cuando se da de alta) o en este examen?
- Si la persona tiene varias incapacidades, ¿el examen se refiere siempre a la **más reciente**, o el médico elige cuál?
- ¿Este examen lo pide RH al reincorporarse la persona, o el médico lo programa?

**Respuesta:**

### P8. Mantenimiento (FT-SO-19 y FT-SO-20): ¿en qué se diferencian del inicial y el periódico normales?

- ¿A quién se le aplica "mantenimiento"? ¿Es por puesto (técnicos de mantenimiento), por área, o por empresa/cuenta? ¿Cómo sabe el portal cuál imprimir?
- El FT-SO-19 trae **"DESCRIPCIÓN DE PUESTO"** y "ANTIGÜEDAD" que el 04 no tiene. ¿Hay más diferencias de contenido clínico (secciones o preguntas que solo van en mantenimiento), o es solo el encabezado?
- Para un candidato de mantenimiento que llega por OCR, ¿el examen inicial que ya construimos se imprime como FT-SO-19 en vez de FT-SO-04?

**Respuesta:**

### P9. Datos de la ficha que el sistema no trae: departamento, turno, escolaridad/profesión

Los formatos piden **TURNO** y **DEPARTAMENTO** (y el 04 escolaridad/profesión). El biométrico no los entrega al portal.

- ¿Los captura el médico en cada examen, o deberían venir de RH (y entonces hay que pedirlos al sistema de RH)?
- Si los captura el médico, ¿turno es un catálogo (matutino, vespertino, nocturno, mixto)?

**Respuesta:**

---

## Lo que haremos mientras responden

1. Selector del tipo de examen (admisión, periódico, cambio de puesto, egreso, post incapacidad, mantenimiento inicial, mantenimiento periódico) y los campos cintura / cadera / SpO2: ya construidos, se verifican en el examen periódico.
2. Plantilla de impresión del **FT-SO-17 periódico** con todo lo que ya existe; las casillas del resultado quedan en blanco hasta P1 y la firma como está hoy hasta P2.
3. "Imprimir expediente" elegirá la plantilla según el tipo de examen.

Con P1, P2 y P3 respondidas, los seis formatos se cierran uno tras otro; sin ellas, cada plantilla saldría con el resultado y la firma en duda.
