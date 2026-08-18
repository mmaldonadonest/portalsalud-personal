# Bloqueo: Examen Médico no puede entrar a "Resumen General"

**Fecha:** 17 de agosto de 2026
**Estado: RESUELTO el mismo día** — ver `docs/ords-examen-historial.sql`. Tabla `SERV_MED_RESULTADO_EXAMEN_HIST` aplicada + edición aditiva de `PR_SERVICIO_MED_EXAMEN2`, probada sin regresión (portal Java, PHP legacy y WS directo, los 3 confirmados funcionando). El resto de este documento queda como registro del diagnóstico original — sigue siendo válido como referencia de *por qué* se hizo el cambio. Único punto abierto: falta construir el WS agregado `consulta_examen_fecha` + el lado Java (trabajo mecánico, mismo patrón ya usado 4 veces) — el bloqueo *estructural* ya no existe.

## Resumen (léelo y ya)

`SERV_MED_RESULTADO_EXAMEN` (donde vive el resultado APTO/NO_APTO de cada examen) **no tiene columna de fecha** y **guarda una sola fila por NSS**. Cada examen nuevo sobreescribe al anterior.

**Consecuencia real: el historial de exámenes previos ya no existe.** No es que falte un WS — el dato que necesitaríamos ya no está en ningún lado.

```sql
DESC SERV_MED_RESULTADO_EXAMEN;
-- NSS, OBSERVACIONES, APTO, NO_APTO, APTO_CONDICIONADO, APTO_RESTRINGIDO, FIRMA_DIGITAL
-- (cero columnas de fecha)
```

```sql
-- El propio handler de escritura (Servcio/Medico) confirma el problema:
UPDATE SERV_MED_RESULTADO_EXAMEN SET ... WHERE NSS = KNNS;
-- upsert por NSS, sin fecha en ningún lado del INSERT/UPDATE
```

## Por qué esto bloquea "Resumen General"

"Resumen General" combina 4 dominios: Incapacidades, Accidentabilidad, Consulta y Examen. Los primeros 3 ya tienen WS agregado (`consulta_incapacidades_fecha`, `consulta_accidentes_fecha`, `consulta_medica_fecha`) y están verificados. Examen no puede tener uno — no hay fecha que filtrar ni historial que sumar.

## Qué se necesitaría para arreglarlo (no trivial)

| # | Actividad | Riesgo |
|---|---|---|
| 1 | Diseñar tabla de historial nueva (con fecha, sin sobreescribir) | Bajo — solo diseño |
| 2 | Aplicar la tabla en Oracle (DDL) | Bajo — tabla nueva, aditiva |
| 3 | Modificar el handler `Servcio/Medico` (PL/SQL de escritura) para insertar también ahí | **Alto — toca el guardado de Examen, ya en producción y verificado** |
| 4 | Probar que no rompe el guardado actual de Examen | **Alto — regresión posible en un flujo ya validado** |
| 5 | Construir el WS agregado nuevo sobre la tabla de historial | Bajo — mismo patrón ya usado 4 veces |
| 6 | Aplicar y probar ese WS | Bajo |
| 7 | Construir el lado Java (cliente/servicio/dashboard) | Bajo — mismo patrón ya usado 4 veces |
| 8 | Aceptar que no hay datos retroactivos | N/A — limitación permanente, no se puede resolver |

**El riesgo real está concentrado en los pasos 3 y 4.** Todo lo demás es trabajo mecánico ya probado.

## Alternativa sin tocar nada de esto

"Resumen General" se puede construir HOY con las 3 piezas que ya existen (Incapacidades + Accidentabilidad + Consulta), dejando Examen fuera explícitamente. Cero riesgo, cero WS nuevo — es solo combinar 3 endpoints que ya están verificados.
