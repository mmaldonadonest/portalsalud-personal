# Examen médico inicial — lo que falta y lo que necesitamos preguntar

**Para:** Servicio Médico y Recursos Humanos
**Formato revisado:** FT-SO-04 rev. 04

---

## En resumen

Comparamos su formato contra lo que el sistema ya guarda hoy: **es el mismo examen**. Casi
todos sus campos ya se capturan y "Admisión" ya existe como tipo de examen. **No hay que
construir un módulo nuevo**, sólo completar lo que falta.

Lo que falta son dos grupos muy distintos:

| | |
|---|---|
| **Lo que ya podemos hacer** | No depende de ninguna respuesta suya. Son campos que faltan y se agregan. |
| **Lo que está detenido** | No podemos decidirlo nosotros. Son las 7 preguntas de abajo. |

---

## Lo que ya podemos hacer (no necesita respuesta)

| Qué | Detalle |
|---|---|
| **Cintura y cadera** | Son nuevos en la rev. 04 y hoy no existen en el sistema |
| **Selector de tipo de examen** | Para elegir entre Admisión, Periódico, Cambio de rol, Post incapacidad y Especial |

Si están de acuerdo, avanzamos con eso mientras responden lo demás.

---

## Lo que está detenido

Siete preguntas. Las tres primeras deciden si una funcionalidad completa es viable o no;
las otras definen cuánto trabajo es.

### 1 · El candidato que no se contrata, ¿queda registrado?

Cuando alguien va a examen médico de admisión y sale **no apto**, ¿esa persona queda
registrada en el sistema, o sólo se registra a quien sí se contrata?

- [ ] Queda registrado desde que se le hace el examen
- [ ] Sólo se registra si se le contrata

> **Por qué importa:** si sólo se registra a quien se contrata, el "no apto" no se guarda en
> ningún lado y **la lista de bloqueados nacería vacía**. Es la pregunta que decide si esa
> funcionalidad se puede hacer.

### 2 · ¿Quién entra a la lista de bloqueados?

- [ ] Sólo los **no aptos**
- [ ] También los **aptos condicionados**

> **Por qué importa:** "apto condicionado" significa que sí puede trabajar, con límites — y para
> eso el sistema ya tiene el módulo de Restricciones Médicas. Si no impide contratar, la lista
> es solamente de no aptos y es más simple.

### 3 · ¿Cuánto dura el bloqueo?

- [ ] Es permanente
- [ ] Vence a los ______ meses
- [ ] Hasta que un examen nuevo salga apto

Y si se vuelve a examinar y sale apto:

- [ ] Se desbloquea solo
- [ ] Alguien tiene que autorizarlo

### 4 · Antecedentes laborales: ¿se llenan los cuatro empleos?

El formato pide hasta **4 empleos anteriores con 10 datos cada uno** (empresa, puesto, turno,
antigüedad, riesgos, equipo de protección…). En la práctica:

- [ ] Sí, se llenan los 4 con todo su detalle
- [ ] Sólo el resumen: edad al empezar a trabajar y cuántos trabajos ha tenido
- [ ] A veces uno o dos, depende del candidato

> **Por qué importa:** es la tarea más grande de todo el pendiente. Si en la práctica sólo se
> captura el resumen, nos la ahorramos completa.

### 5 · ¿Necesitan ver exámenes de años anteriores?

Hoy se guarda **un examen por persona** y cada uno reemplaza al anterior. Desde agosto sí queda
registrado el dictamen y la fecha de cada examen que se hace.

- [ ] Con el dictamen y la fecha es suficiente
- [ ] Necesitamos poder **abrir el examen completo** de años anteriores

> **Por qué importa:** guardar el examen completo de cada año es un trabajo mucho mayor y habría
> que planearlo aparte. Además, de las personas ya examinadas sólo existe la última versión: lo
> anterior ya no se puede recuperar.

### 6 · ¿Qué tipos de examen usan?

Marque los que apliquen:

- [ ] Admisión
- [ ] Periódico
- [ ] Cambio de rol
- [ ] Post incapacidad
- [ ] Especial
- [ ] Otro: ______________________

### 7 · ¿Cuáles de estos campos usan de verdad?

Están en el formato pero hoy el sistema no los captura. Marque sólo los que sí se llenan:

- [ ] Oximetría (SpO2)
- [ ] Romberg
- [ ] Voz clara y fuerte
- [ ] Tráquea
- [ ] Oído derecho e izquierdo **por separado**
- [ ] Lentes **por ojo**
- [ ] Observaciones generales
- [ ] CCA
- [ ] Fecha de influenza
- [ ] Cirugía (observaciones)

> **Por qué importa:** cada campo que marquen se construye; los que no, se quedan fuera y
> nadie los extraña.

---

## Dos datos más, sin prisa

No detienen nada, pero nos dicen cuánto espacio pedirle al servidor. Cada examen genera unos
4.4 MB de documentos escaneados.

**8 ·** ¿Cuántos exámenes de **candidatos** se hacen al mes, aproximadamente? _____________
*(Sabemos que se contratan ~217 personas al mes, pero no cuántas se examinan para llegar a esa
cifra.)*

**9 ·** El examen **periódico**, ¿cada cuánto se hace?

- [ ] Cada año
- [ ] Cada 2 años
- [ ] Depende del puesto

---

**Cualquier duda sobre alguna pregunta, con gusto la explicamos antes de que la contesten.**
