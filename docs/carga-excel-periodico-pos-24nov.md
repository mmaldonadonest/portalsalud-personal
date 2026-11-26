# Carga de Excel: periódico y pos incapacidad 2026 (24-nov-2026)

Referencia de lo que se encontró en cada archivo, lo que se corrigió (en el Excel o en el portal) y cómo quedó.
WAR instalado en producción: `target/portal-salud.war` 24-nov 17:21,
sha256 `1167D2B21EE9D5516187A96DE854F4994C9AF4870432D2C717B3B3F1E43C8070`.

## 3 EXAMEN PERIODICO 2026.xlsx

| # | Hallazgo | Corrección | Dónde |
|---|---|---|---|
| 1 | Columnas corridas desde DIAGNOSTICO: la N repetía el STATUS en los 1,893 renglones y la Q traía datos sin encabezado (se habría perdido) | SM las acomodó (versión 15:49) | Excel |
| 2 | Encabezado «FECHA DE ATENCION»; el portal solo aceptaba «FECHA EXAMEN» → rechazo | Se aceptan los dos como sinónimos; el oficial es «FECHA DE ATENCION» | Portal (`LayoutNss`) |
| 3 | El rechazo decía que faltaban TODAS las columnas: comparaba contra el título de la fila 1 | Compara contra la fila más parecida al encabezado y dice la columna exacta | Portal (`CargaNssService`) |
| 4 | Fecha como número de serie de Excel (46053) por no tener formato de fecha → 1,893 rechazos | SM le dio formato de fecha (versión 16:18) y además el portal acepta el número (años 2000-2100) | Excel + portal (`ValidadorNss`) |

Resultado: 1,893 de 1,893 renglones válidos, fechas 2-ene a 30-oct-2026.
Para revisar, no detienen: 2 duplicados idénticos (renglones 544/547 NSS 92997611501 13-may; 1294/1295 NSS
92119245634 6-mar), errata «SE NEVIA A LAPI» (3), 180 NSS sin el cero inicial (el portal lo completa).

## 4 EXAMEN POS INCAPACIDAD 2026.xlsx

| # | Hallazgo | Corrección | Dónde |
|---|---|---|---|
| 1 | También trae «FECHA DE ATENCION». Con el cambio solo en periódico (WAR 16:32, NO instalado) se reconocía como PERIÓDICO y se habría cargado sin CAUSA | Pos incapacidad acepta los dos nombres igual que periódico | Portal (`LayoutNss`) |

Resultado en producción (24-nov 17:26): 425 leídos, 425 entran, 0 rechazados.
No se guardan (el Excel dice «SIN CARGA AL SISTEMA»): INICIA INCAPACIDAD, DIAS, ULTIMO DIA DE INCAPACIDAD, INICIA LABORES.
Para revisar, no detienen: 2 duplicados (NSS 92977532305 20-mar; 92997722167 23-may) y 4 renglones con la fecha de
atención antes del inicio de la incapacidad: 133 (14-mar vs 7-abr), 134 (14-mar vs 2-abr), 290 (3-mar vs 20-mar),
305 (11-mar vs 22-mar).

## Efecto en gráficas

Ninguno por el nombre de la columna: con cualquiera de los dos se guarda en la misma `FECHA`. Los exámenes de Excel se
ven en Análisis › Clínico › Exámenes Médicos sumados con ingreso y portal (sin filtro por tipo; propuesta pendiente).
PDF para SM: `C:\miguel\evidencias-examenes-sm-24nov\examenes-donde-se-ven-sm.pdf`.
