# Examen inicial y lista de bloqueados — preguntas para cerrar el requerimiento

**Para:** Servicio Médico y Recursos Humanos · **Fecha:** 25-sep-2026

Revisamos el formato **FT-SO-04 Examen Médico Inicial (rev. 04)** contra lo que el sistema
captura hoy. La buena noticia: **es el mismo examen**. De los 296 campos del formato, el
portal ya captura 292, y el tipo "Admisión" ya existe internamente. No hay que construir un
módulo nuevo.

Quedan estas preguntas para cerrar el alcance. Están ordenadas por lo que más cambia el
trabajo; las marcadas **⬤ crítica** deciden si algo es viable o no.

---

## 1. La lista de bloqueados

### ⬤ 1.1 · ¿Al candidato se le da de alta en el sistema **antes** del examen médico, o sólo cuando se le contrata?

Es la pregunta que decide todo lo demás. El examen se guarda asociado al NSS de la persona.

- Si el candidato **ya existe** cuando se le hace el examen → su "no apto" queda guardado y
  la lista de bloqueados funciona.
- Si se da de alta **sólo al contratar** → el examen del que no pasó nunca llega a
  guardarse, y la lista nacería vacía por más pantalla que construyamos. Habría que resolver
  primero dónde se registra a ese candidato.

*Dato relacionado:* hay **33,907 personas** registradas y sólo **3,584 vigentes**. Las demás
tienen estatus 1 (28,155), 99 (2,154) o 100 (12). **¿Qué significa cada uno?** Si alguno es
"candidato" o "rechazado", la respuesta a esta pregunta ya está ahí.

### ⬤ 1.2 · "Apto condicionado", ¿bloquea la contratación o sólo obliga a registrar restricciones?

Por definición, *apto condicionado* significa que **sí puede trabajar, con límites** — y para
eso el sistema ya tiene el módulo de Restricciones Médicas (no cargar peso, no trabajo en
alturas, etc.).

- Si **bloquea** → va en la misma lista que "no apto".
- Si **no bloquea** → la lista es sólo de no aptos, y lo de apto condicionado es otra cosa:
  un control de que a esa persona **sí se le hayan capturado sus restricciones**. Podemos
  hacer esa segunda pantalla, pero es un trabajo distinto.

### 1.3 · ¿Qué hace que alguien entre a la lista, y por cuánto tiempo?

- ¿Cualquier "no apto", o sólo el del examen de admisión?
- ¿Queda bloqueado para siempre, o vence después de cierto tiempo?
- Si la persona se vuelve a examinar y sale apto, ¿se le quita el bloqueo automáticamente?

### 1.4 · ¿Quién la consulta y cómo?

- ¿RH al momento de contratar, o Servicio Médico?
- ¿Se usa buscando **una persona** (por NSS o CURP), o revisando **la lista completa**?
  Suponemos lo primero, pero cambia el diseño de la pantalla.
- ¿Debe tener permiso propio, o basta con el rol que hoy ve los dashboards?

### 1.5 · ¿Qué se guarda de cada bloqueado?

Nuestra propuesta es registrar **sólo el dictamen** — no apto / apto condicionado, la fecha y
el tipo de examen — **y no el diagnóstico ni el padecimiento**. El dictamen ya es un veredicto
de aptitud laboral y evita manejar información clínica en una lista de consulta de RH.
**¿Están de acuerdo?**

> Nota: una lista que condiciona contrataciones a partir de información médica conviene que
> tenga visto bueno de RH o jurídico, y que quede por escrito quién puede consultarla.

---

## 2. El examen

### 2.1 · De los cinco tipos, ¿cuáles se usan de verdad?

El sistema ya los contempla: **Admisión · Periódico · Cambio de rol · Post incapacidad ·
Especial**. Vamos a poner el selector en el formulario. ¿Los usan los cinco, sobra alguno, o
falta alguno?

### 2.2 · Confirmar los dos campos nuevos: **cintura** y **cadera**

Están en la revisión 04 del formato y no existen en ningún sistema hoy. ¿Se capturan siempre
o sólo en ciertos casos? ¿Llevan alguna validación o rango esperado?

### 2.3 · Los antecedentes laborales: ¿se llenan completos?

El formato pide hasta **4 empleos anteriores** con 10 datos cada uno (empresa, giro, puesto,
turno, antigüedad, salida, descripción, riesgos, EPP, observaciones). Hoy el sistema **sólo
guarda el resumen**: edad al empezar a trabajar, cuántos trabajos y si tiene pensión.

Habilitar la tabla completa es la tarea más grande del plan. **¿Se llena en la práctica, o en
el examen de admisión se captura sólo el resumen?** Si en la práctica se llena, la hacemos; si
no, nos la ahorramos.

### 2.4 · ¿Hay algo del formato en papel que hoy **no** se esté capturando y se extrañe?

Detectamos tres bloques que el sistema imprime pero no deja capturar: el detalle de los 4
empleos, parte de la exploración física (oximetría, Romberg, voz, tráquea, detalle de oídos y
lentes por ojo) y unos misceláneos. **¿Los usan?** Si alguno no se usa, lo dejamos fuera.

---

## 3. Un aviso sobre el histórico

Hay que decirlo con claridad para que nadie se lleve una sorpresa:

**Hoy el sistema guarda un solo examen por persona, y cada examen nuevo sobrescribe al
anterior.** No es una decisión del portal: así está construido desde el sistema original.

- De las **3,213 personas** con examen en el sistema, sólo existe **la última versión**. Lo
  anterior ya no está y no se puede recuperar.
- Desde el **17 de agosto de 2026** sí se guarda una bitácora con el dictamen y la fecha de
  cada examen. Es lo que alimentará la lista de bloqueados, y **hoy tiene unos 24 registros
  reales**: crece sólo hacia adelante.

**¿Con eso basta, o necesitan poder abrir el examen completo de hace dos años?** Si lo
segundo, es un trabajo considerablemente mayor y lo planteamos aparte.

---

## 4. Dos datos para planear la infraestructura

No son del examen inicial, pero los necesitamos para dimensionar el servidor y evitar
quedarnos cortos de espacio.

### 4.1 · ¿Cuántos candidatos se examinan al mes?

Sabemos que se dan de alta unas **217 personas al mes**, pero no cuántos candidatos se
examinan para llegar a esa cifra. ¿Se examinan 2 por cada contratado? ¿4?

### 4.2 · ¿Cada cuánto es el examen periódico?

¿Anual, cada dos años, o depende del puesto de riesgo? Si depende del puesto, ¿cuánta gente
entra en cada categoría?

*Por qué importa:* cada examen genera en promedio **4.4 MB** de documentos escaneados. Con la
respuesta a estas dos preguntas sabemos si hay que pedir 70 GB o 250 GB de espacio para los
próximos cinco años. Hoy llevamos 14 GB.

---

## Lo que podemos avanzar sin esperar respuestas

El selector de tipo de examen y los campos de cintura y cadera no dependen de ninguna de
estas preguntas. Si están de acuerdo, arrancamos por ahí.
