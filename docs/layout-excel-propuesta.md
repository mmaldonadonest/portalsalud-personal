# Layout propuesto para los Excel del servicio médico

**Para:** Servicio Médico y Salud Ocupacional
**Fecha:** 8-oct-2026
**Plantillas:** carpeta `docs/layout-excel/`, un archivo por reporte (1 ATENCIONES … 9 CONSUMIBLES MATERIAL FIJO). Cada archivo trae la pestaña de captura primero y, a la derecha, CATALOGOS y LEEME. `docs/layout-excel-propuesta.xlsx` es la versión consolidada de referencia.

## Criterio

Una columna se queda si existe hoy en el Excel del servicio médico (revisado contra los archivos de AIFA 2026), no existe en la base ni se puede calcular, y la usa alguna gráfica de Análisis. Excepciones: **ÁREA**, **DIAGNÓSTICO** y **TRATAMIENTO** (atenciones) se quedan aunque hoy ninguna gráfica los use, porque los traen los Excel y no se pueden recuperar de la base.

## Reglas

1. Una fila = un evento. Nada de "marca con 1" ni columnas por mes, cuenta o área.
2. Una hoja por tipo de evento, todos los predios juntos.
3. **NSS, PREDIO y ÁREA obligatorios** en todas las hojas por persona. Nombre, género, edad, cuenta, puesto y agencia salen del biométrico. PREDIO se captura porque el biométrico da el predio actual (no donde se atendió) y los candidatos no existen ahí. Candidatos: en EXAMEN NUEVO INGRESO van nombre, género, edad, cuenta y puesto solicitado; en ANTIDOPING, nombre, género y edad opcionales.
4. Catálogos cerrados: solo valores de la lista desplegable.
5. Fechas dd/mm/aaaa.
6. Para pegar datos: **Pegado especial → Valores** (Ctrl+Alt+V, V). Pegar normal borra la lista desplegable de esas celdas.
7. Lo pegado no se valida en Excel: la carga marca para revisión los valores fuera de catálogo. En diagnóstico, tratamiento y descripción de medicamento la lista solo sugiere: se permite un valor nuevo.

## Hojas

Los nombres de columna son los mismos de los Excel actuales del servicio médico.

| Hoja | Columnas |
|---|---|
| ATENCIONES | NSS, PREDIO, AREA, FECHA, RAMO, CAUSA, CAUSAS MUSCULO ESQUELETICAS, DIAGNOSTICO, TRATAMIENTO |
| EXAMEN NUEVO INGRESO | NSS, NOMBRE, APELLIDO_PATERNO, APELLIDO_MATERNO, PUESTO SOLICITADO, GENERO, EDAD, CUENTA, PREDIO, AREA, FECHA EXAMEN, TIPO DE EXAMEN, STATUS, DIAGNOSTICO |
| EXAMEN PERIODICO | NSS, PREDIO, AREA, FECHA EXAMEN, STATUS, DIAGNOSTICO |
| EXAMEN POS INCAPACIDAD | NSS, PREDIO, AREA, FECHA EXAMEN, STATUS, DIAGNOSTICO, RAMO, INTERNA, FOLIO, INICIA INCAPACIDAD |
| INCAPACIDADES | NSS, PREDIO, AREA, FOLIO, RUBRO, RAMO, TIPO, INICIO DE INCAPACIDAD, TERMINO DE INCAPACIDAD, DIAS DE INCAPACIDAD, FECHA DE PRIMERA INCAPACIDAD, SDI, DIAS EG / MAT / RT1 / RT2 × (AÑO ANTERIOR PENDIENTE, ENE…DIC, AÑO SIGUIENTE ENERO, TOTAL), DIAS RT2 TOTAL, COSTO ENF GRAL, COSTO MATERNIDAD, COSTO LABORAL, COSTO TRAYECTO, COSTO TOTAL, DIAGNOSTICO, CAUSA, ALTA INGRESO A LA OPERACION, ST7 |
| ACCIDENTES | NSS, PREDIO, AREA, FECHA, TIPO DE RIESGO, DX IMSS, SDI, PAGO 60%, CALIFICADO LABORAL ST2 FECHA INICIO, CALIFICADO LABORAL ST2 FECHA ALTA, CALIFICADO LABORAL DIAS, CALIFICADO LABORAL COSTO, CALIFICADO TRAYECTO ST2 FECHA INICIO, CALIFICADO TRAYECTO ST2 FECHA ALTA, CALIFICADO TRAYECTO DIAS, CALIFICADO TRAYECTO COSTO, IMPROCEDENTE LABORAL ST2 FECHA INICIO, IMPROCEDENTE LABORAL ST2 FECHA ALTA, IMPROCEDENTE LABORAL DIAS IMSS, IMPROCEDENTE LABORAL DIAS PAGADOS, IMPROCEDENTE LABORAL COSTO, IMPROCEDENTE TRAYECTO ST2 FECHA INICIO, IMPROCEDENTE TRAYECTO ST2 FECHA ALTA, IMPROCEDENTE TRAYECTO DIAS, IMPROCEDENTE TRAYECTO DIAS PAGADOS, IMPROCEDENTE TRAYECTO COSTO, DESCRIPCION BREVE DEL ACCIDENTE, CAUSA DE RT, COSTO CALIFICADO, COSTO IMPROCEDENTE, STATUS, FECHA PARA CALIFICACION, STATUS DEL CASO |
| ANTIDOPING | NSS, NOMBRE, APELLIDO_PATERNO, APELLIDO_MATERNO, GENERO, EDAD, PREDIO, AREA, FECHA, TIPO DE PRUEBA, RESULTADO, DOPING, ALCOHOLEMIA %, CONCLUSION, STATUS |
| MATERNIDAD | NSS, PREDIO, AREA, FECHA, STATUS LABORAL, FUM, FECHA PROBABLE DE PARTO, FECHA PROBABLE DE INCAP PRENATAL, FECHA PROBABLE REINICIO DE LABORES, REINICIO DE LABORES (FECHA REAL), INCAPACIDAD INICIO, INCAPACIDAD TERMINO, INCAPACIDAD FOLIO, LACTANCIA INICIA, LACTANCIA TERMINA, REANUDA ACTIVIDADES EN HORARIO NORMAL, CORREO DE AVISO EMBARAZO, CORREO DE AVISO LACTANCIA, CORREO DE AVISO TERMINO DE LACTANCIA |
| CONSUMIBLES MEDICAMENTOS | PREDIO, MES, CATEGORIA, DESCRIPCION, LOTE, FECHA DE CADUCIDAD, CANTIDAD MES ANTERIOR, RECIBIDO EN EL MES, CONSUMO SEMANA 1, CONSUMO SEMANA 2, CONSUMO SEMANA 3, CONSUMO SEMANA 4, CONSUMO SEMANA 5, TOTAL x LOTE, CONSUMO ACUMULADO, RESTANTE PARA USO x LOTE, TOTAL EN EXISTENCIA PARA PROXIMO MES |
| CONSUMIBLES MATERIAL FIJO | PREDIO, EQUIPO, TIPO DE MATERIAL, FECHA DE RECEPCION, MOTIVO, CANTIDAD, FECHA DE BAJA, CANTIDAD TOTAL SURTIDA |

Los tres exámenes van en hojas separadas, como en los Excel actuales (2 NUEVO INGRESO, 3 PERIODICO, 4 POS INCAPACIDAD). En NUEVO INGRESO, nombre, apellido paterno y puesto solicitado son obligatorios: el candidato todavía no existe en el biométrico.

\* FOLIO si la incapacidad es IMSS (INTERNA = NO); INICIA INCAPACIDAD si es interna (INTERNA = SI), porque las internas no tienen folio. Reemplazan el bloque DATOS INCAPACIDAD (días, inicio, último día, inicia labores): esos datos salen de la hoja INCAPACIDADES.

En EXAMEN POS INCAPACIDAD, la columna CAUSA del Excel actual (EG / MAT / RT 1 / RT 2 / INTERNA) se separa en **RAMO** (EG, MAT, RT1, RT2) e **INTERNA** (SI/NO): INTERNA no es un ramo sino quién paga, y juntas producían renglones con dos marcas (14 en 2026).

**RAMO** de ATENCIONES e INCAPACIDADES tiene catálogos distintos, como en los Excel: en ATENCIONES es el tipo de consulta (ENF GRAL, SEGUIMIENTO SALUD, PASE SALIDA, RT); en INCAPACIDADES es el ramo IMSS (EG, MAT, RT1, RT2).


## Catálogos de referencia: DIAGNOSTICO y TRATAMIENTO

DIAGNOSTICO y TRATAMIENTO siguen siendo **texto libre** (sin lista desplegable): los Excel no traen catálogo y hoy se capturan 1,691 diagnósticos y 1,792 tratamientos distintos. En la hoja CATALOGOS, las columnas **DIAGNOSTICO (ref)** y **TRATAMIENTO (ref)** (encabezado gris) listan esos valores, del más al menos usado, solo como **guía** para escribirlos igual (p. ej. "LUMBALGIA" en lugar de "ALGIA LUMBAR" o "DOLOR LUMBAR"). Salen de ATENCIONES 2026 de todos los predios.


## Incapacidades: días subsidiados por ramo y mes

Decisión 9-oct: **se capturan** las 60 columnas (4 ramos × año anterior pendiente, ENE…DIC, enero del año siguiente y total), como en el Excel actual. **Lo que capturan es lo que manda**: el portal no las recalcula. Se pueden calcular desde inicio y término (coinciden 94 % en EG, 85 % MAT, 91 % RT1, 92 % RT2 en 2026; las diferencias son errores de captura), pero se respeta lo que reporta el servicio médico.

## Accidentes: qué cambia respecto al Excel actual

- Decisión 9-oct: **se capturan todos los bloques del Excel** (CALIFICADO y IMPROCEDENTE, laboral y trayecto: fechas ST2, días, días pagados y costos; y los totales de COSTO). **No se calcula nada de dinero ni de días**: puede haber reglas de pago que no conocemos (p. ej. los 3 casos de Toluca que no siguen la regla de días − 3). Lo que suben es lo que manda.
- **CAUSA DE RT** (las 7 opciones con marca) va como catálogo y **DESCRIPCION BREVE DEL ACCIDENTE** como texto libre aparte.
- Se capturan los dos status del Excel: **STATUS** y **STATUS DEL CASO**.
- No se capturan OBSERVACIONES ni DOCUMENTO SUBIDO EN FZ.
- Hallazgos del Excel actual: no trae fecha del accidente (solo mes reportado); en renglones vacíos las fórmulas dejan −3 días y −581.40 de costo.


## Consumibles: medicamentos y material fijo por separado

Como en el Excel actual (pestañas MEDICAMENTOS y MATERIAL FIJO), se manejan distinto:

- **Medicamentos** tienen lote y caducidad y se consumen: un renglón por **predio + mes + medicamento + lote**, con cantidad del mes anterior, recibido, consumo por semana (1 a 5) y los totales que hoy reportan (total por lote, acumulado, restante y existencia para el próximo mes), que se capturan y mandan. Las categorías (VÍA ORAL, INYECTABLE, TÓPICO, OFTÁLMICO, ÓTICO, ANTIBIÓTICO INYECTABLE / ORAL, OTROS, ESPECÍFICO, MATERIAL, URGENCIA) dejan de ser renglones de título y pasan a columna. Los días para caducar se calculan a la fecha de consulta. La lista de 143 medicamentos de AIFA queda en CATALOGOS como referencia.
- **Material fijo** (equipo) no caduca: se recibe y se da de baja. Un renglón por **recepción**, con MOTIVO (INICIAL, CAMBIO, DAÑO, AUMENTO) en lugar de los tres ciclos en columnas. **CANTIDAD TOTAL SURTIDA** la declara el usuario en cada recepción (es su responsabilidad, no se calcula); manda la del renglón más reciente del equipo. Catálogo de 18 equipos tomado del Excel.

## Qué gráfica usa cada columna

| Hoja | Columna | Gráfica |
|---|---|---|
| ATENCIONES | RAMO | Morbilidad: tipo de consulta |
| | CAUSA | Morbilidad: causas |
| | CAUSAS MUSCULO ESQUELETICAS | Musculoesquelético |
| EXAMEN NUEVO INGRESO / PERIODICO / POS INCAPACIDAD | STATUS | Exámenes: dictamen por mes y predio |
| | FOLIO, INICIA INCAPACIDAD | No es gráfica: liga el examen pos incapacidad con su incapacidad (folio IMSS; fecha de inicio solo para internas) |
| INCAPACIDADES | RAMO | Incapacidades por ramo |
| | DIAS DE INCAPACIDAD | Días de incapacidad |
| | COSTO ENF GRAL / MATERNIDAD / LABORAL / TRAYECTO / TOTAL | Costo de incapacidades: se captura, los montos no se calculan |
| | RUBRO | IMSS / interna |
| ACCIDENTES | TIPO DE RIESGO, CAUSA DE RT, STATUS | Accidentabilidad |
| | COSTO CALIFICADO, COSTO IMPROCEDENTE | Costo |
| ANTIDOPING | TIPO DE PRUEBA, RESULTADO, DOPING, CONCLUSION | Control antidoping |
| MATERNIDAD | STATUS LABORAL | Maternidad por estatus |
| | FUM | Semanas de gestación y partos por mes (fecha probable de parto = FUM + 280 días) |
| | REINICIO DE LABORES (FECHA REAL) | Reincorporadas |
| CONSUMIBLES MEDICAMENTOS / MATERIAL FIJO | PREDIO, MES, DESCRIPCION / EQUIPO, cantidades | Inventario |
| Todas | NSS + FECHA | Liga con el biométrico y tendencia por mes |
| Todas | AREA | Hoy ninguna; se captura porque no existe en el biométrico y la traen todos los Excel |
| Atenciones | TRATAMIENTO | Hoy ninguna; se captura porque lo trae el Excel y no existe en la base |
| Atenciones, exámenes, incapacidades, accidentes | DIAGNOSTICO | Hoy ninguna; se captura porque lo traen esos Excel y no existe en la base (en accidentes se llama DX IMSS) |

## Qué se quitó de los Excel actuales

| Motivo | Columnas |
|---|---|
| Sale del biométrico con el NSS | Nombre, rango de edad, género, agencia, cuenta, puesto, estado civil, escolaridad, fecha de ingreso |
| Se calcula | Mes reportado, costos por ramo, PAGO 60 %, días subsidiados por mes, días acumulados, edad gestacional, fecha probable de parto, consumo acumulado, existencia |
| Ninguna gráfica lo usa | Folio, tipo de incapacidad, alta ingreso, ST7, resultados pendientes, observaciones, fecha para calificación, documento en FZ, fechas probables de incapacidad y reinicio, periodo de lactancia, correo de aviso, lote, caducidad |

Si alguna de las columnas quitadas por "ninguna gráfica la usa" se necesita para control interno del servicio médico, se puede regresar; no afecta las gráficas.

## Pendientes de confirmar con el servicio médico

2. Catálogos: tomados tal cual de los encabezados de los Excel de AIFA 2026 (área, ramo, causa, músculo esquelético, causa RT, estatus, sustancia, conclusión). Cualquier valor nuevo se acuerda antes de usarlo.
3. Candidatos de examen de ingreso: se capturan nombre, apellidos y puesto solicitado en EXAMEN NUEVO INGRESO; cuando entren al biométrico, el NSS los liga.
