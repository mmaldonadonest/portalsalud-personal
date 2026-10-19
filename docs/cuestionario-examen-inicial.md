# Examen inicial y lista de bloqueados — cuestionario

**Para:** Servicio Médico y Recursos Humanos · **25-sep-2026**

Revisamos el formato **FT-SO-04 rev. 04** contra lo que el sistema ya captura: **es el mismo
examen**. De sus 296 campos, el portal ya guarda 292, y "Admisión" ya existe como tipo. No
hay que construir un módulo nuevo.

Sólo necesitamos estas 16 respuestas para cerrar el alcance.

---

## A · Lista de bloqueados

**1.** ¿Al candidato se le da de alta en el sistema **antes** del examen, o sólo cuando se le contrata?

**2.** ¿Qué significan los estatus de empleado **1** (28,155 personas), **99** (2,154) y **100** (12)? El 0 ya sabemos que es vigente.

**3.** ¿Quién entra a la lista: sólo los **no aptos**, o también los **aptos condicionados**?

**4.** ¿"Apto condicionado" impide contratar, o se contrata con restricciones?

**5.** ¿Cuánto dura el bloqueo: es permanente, vence a los X meses, o hasta que un examen nuevo salga apto?

**6.** Si se vuelve a examinar y sale apto, ¿se desbloquea automáticamente o alguien lo autoriza?

**7.** ¿Quién consulta la lista: RH, Servicio Médico, o ambos?

**8.** ¿La van a usar buscando **una persona** (por NSS o CURP), o revisando **la lista completa**?

**9.** ¿Basta con que muestre el **dictamen y la fecha**, sin diagnóstico ni padecimiento?

## B · El examen

**10.** ¿Cuáles de estos tipos de examen se usan? Admisión · Periódico · Cambio de rol · Post incapacidad · Especial. ¿Falta alguno?

**11.** **Cintura y cadera** son nuevos en la rev. 04 y no existen en el sistema. ¿Se miden siempre, o sólo en ciertos casos?

**12.** Antecedentes laborales: el formato pide hasta **4 empleos anteriores** con 10 datos cada uno. En la práctica, ¿se llenan los 4, o sólo el resumen (edad al empezar a trabajar y cuántos trabajos)?

**13.** Estos campos están en el formato pero hoy no se pueden capturar. ¿Cuáles usan?
Oximetría (SpO2) · Romberg · Voz clara y fuerte · Tráquea · Oído derecho e izquierdo por separado · Lentes por ojo · Observaciones generales · CCA · Fecha de influenza · Cirugía (observaciones)

## C · Exámenes anteriores

**14.** Hoy se guarda **un solo examen por persona** y cada uno sobrescribe al anterior; desde el 17-ago-2026 sí queda el dictamen y la fecha de cada uno. ¿Con eso basta, o necesitan **abrir el examen completo** de años anteriores?

## D · Para dimensionar el servidor

**15.** ¿Cuántos exámenes de **candidatos** se hacen al mes? Se contratan ~217 personas al mes, pero no sabemos cuántas se examinan para llegar a esa cifra.

**16.** ¿Cada cuánto es el examen **periódico**: anual, cada 2 años, o depende del puesto?

---

## Por qué preguntamos, en corto

**La 1 decide si la lista de bloqueados es viable.** Si al candidato no contratado no se le
da de alta, su "no apto" no se guarda en ningún lado y la lista nacería vacía. La respuesta a
la 2 probablemente resuelva también ésta.

**La 4 separa dos cosas distintas.** "Apto condicionado" significa que sí puede trabajar con
límites, y para eso el sistema ya tiene el módulo de Restricciones Médicas. Si no bloquea, la
lista es sólo de no aptos.

**La 12 es la tarea más grande del plan.** Si en la práctica sólo se captura el resumen, nos
la ahorramos completa.

**La 14 cambia el orden de magnitud.** Abrir exámenes completos de años anteriores es un
trabajo mucho mayor y habría que planearlo aparte. Además, de las 3,213 personas ya
examinadas sólo existe la última versión: lo anterior no se puede recuperar.

**La 15 y la 16 definen cuánto espacio pedir.** Cada examen genera ~4.4 MB de documentos. Con
esas dos respuestas sabemos si son 70 GB o 250 GB para los próximos cinco años; hoy llevamos 14 GB.

---

**Mientras tanto:** el selector de tipo de examen y los campos de cintura y cadera no
dependen de ninguna de estas respuestas. Si están de acuerdo, avanzamos con eso.
