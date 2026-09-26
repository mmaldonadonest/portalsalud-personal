# Examen inicial y lista de bloqueados — cuestionario

**Para:** Servicio Médico y Recursos Humanos · **25-sep-2026**

Revisamos el formato **FT-SO-04 rev. 04** contra lo que el sistema ya captura: **es el mismo
examen**. De sus 296 campos, el portal ya guarda 292, y "Admisión" ya existe como tipo. No
hay que construir un módulo nuevo.

Sólo necesitamos estas respuestas para cerrar el alcance. **Se contestan marcando.**

---

## A · Lista de bloqueados

**A1.** Cuando se le hace el examen a un candidato, ¿ya está dado de alta en el sistema con su NSS?

- [ ] Sí, se da de alta **antes** del examen
- [ ] No, se da de alta **sólo si se le contrata**
- [ ] Depende: ________________

> Si la respuesta es la segunda, el "no apto" de quien no se contrató no se guarda en ningún
> lado y la lista nacería vacía. Es la pregunta que decide si esto es viable.

**A2.** ¿Qué significa cada estatus de empleado? El 0 ya sabemos que es vigente.

| Estatus | Personas | Significa |
|---|---:|---|
| 1 | 28,155 | ________________ |
| 99 | 2,154 | ________________ |
| 100 | 12 | ________________ |

**A3.** ¿Quién entra a la lista de bloqueados?

- [ ] Sólo los **no aptos**
- [ ] No aptos **y** aptos condicionados
- [ ] Otro: ________________

**A4.** ¿"Apto condicionado" impide contratar?

- [ ] Sí, no se contrata
- [ ] No, se contrata con restricciones

**A5.** ¿Cuánto dura el bloqueo?

- [ ] Permanente
- [ ] ______ meses
- [ ] Hasta que un examen nuevo salga apto

**A6.** Si se vuelve a examinar y sale apto, ¿se desbloquea solo?

- [ ] Sí, automático
- [ ] No, lo tiene que autorizar: ________________

**A7.** ¿Quién consulta la lista?

- [ ] RH
- [ ] Servicio Médico
- [ ] Ambos

**A8.** ¿Cómo la van a usar?

- [ ] Buscando **una persona** (por NSS o CURP)
- [ ] Revisando **la lista completa**
- [ ] Las dos

**A9.** ¿De acuerdo con que la lista muestre sólo el **dictamen y la fecha**, sin diagnóstico ni padecimiento?

- [ ] Sí
- [ ] No, también debe mostrar: ________________

---

## B · El examen

**B1.** ¿Cuáles tipos de examen se usan? Marcar todos los que apliquen.

- [ ] Admisión
- [ ] Periódico
- [ ] Cambio de rol
- [ ] Post incapacidad
- [ ] Especial
- [ ] Falta uno: ________________

**B2.** Cintura y cadera (son nuevos en la rev. 04, no existen en el sistema). ¿Se miden?

- [ ] Siempre
- [ ] Sólo en: ________________
- [ ] No se usan

**B3.** Antecedentes laborales: el formato pide hasta 4 empleos anteriores con 10 datos cada uno. **En la práctica, ¿cuántos se llenan?**

- [ ] Los 4 completos
- [ ] Normalmente ______ empleos
- [ ] Sólo el resumen (edad al empezar a trabajar y cuántos trabajos ha tenido)

> Habilitar la tabla completa es la tarea más grande del plan. Si no se llena, nos la ahorramos.

**B4.** Estos campos están en el formato pero hoy no se pueden capturar. **Marcar los que sí usan:**

- [ ] Oximetría (SpO2)
- [ ] Romberg
- [ ] Voz clara y fuerte
- [ ] Tráquea
- [ ] Oído derecho e izquierdo por separado (agudeza, conducto, membrana timpánica)
- [ ] Lentes por ojo
- [ ] Observaciones generales de exploración
- [ ] CCA
- [ ] Fecha de influenza
- [ ] Cirugía (observaciones)

---

## C · Exámenes anteriores

**C1.** Hoy se guarda **un solo examen por persona**: cada examen nuevo sobrescribe al anterior. Desde el 17-ago-2026 sí queda registrado el dictamen y la fecha de cada uno.

- [ ] Con el dictamen y la fecha nos basta
- [ ] Necesitamos poder **abrir el examen completo** de años anteriores

> La segunda opción es un trabajo mucho mayor y habría que planearlo aparte. Además, de las
> 3,213 personas ya examinadas sólo existe la última versión: lo anterior no se puede recuperar.

---

## D · Para dimensionar el servidor

**D1.** ¿Cuántos exámenes de **candidatos** se hacen al mes? ______

> Sabemos que se contratan ~217 personas al mes, pero no cuántos se examinan para llegar a esa cifra.

**D2.** ¿Cada cuánto es el examen **periódico**?

- [ ] Anual
- [ ] Cada 2 años
- [ ] Depende del puesto: ________________

> Cada examen genera ~4.4 MB de documentos. Con D1 y D2 sabemos si pedir 70 GB o 250 GB para
> los próximos 5 años. Hoy llevamos 14 GB.

---

### Mientras tanto

El selector de tipo de examen y los campos de cintura y cadera no dependen de ninguna de
estas respuestas. Si están de acuerdo, avanzamos con eso.
