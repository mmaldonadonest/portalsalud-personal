# Carga histórica MORBILIDAD 2026 — reporte verificable

**Fecha de la carga:** 30 de septiembre de 2026
**Destino:** usuario `BIOMETRICO`, base `PDBPRD`, servidor `onestdb`, Oracle 19
**Alcance:** los Excel del servicio médico del ejercicio 2026

Cada afirmación de este documento lleva la consulta que la produce. Todas son de
**solo lectura** y se corren contra la base del portal (JDBC), donde viven las tablas
`SERV_MED_BITACORA_*`. Ninguna modifica datos.

---

## 1. Qué quedó cargado

| Tabla | Renglones |
|---|---:|
| `SERV_MED_BITACORA_EVENTO` | 24,236 |
| `SERV_MED_BITACORA_ATRIBUTO` | 118,515 |
| `SERV_MED_BITACORA_METRICA` | 4,742 |
| `SERV_MED_BITACORA_LOTE` | 166 |
| `SERV_MED_BITACORA_ESTADO_MES` | 0 |
| `SERV_MED_BITACORA_NOMENCL` | 0 |

Las dos últimas en cero es lo correcto: corresponden a familias no incluidas en este alcance.

```sql
SELECT 'EVENTO'     t, COUNT(*) n FROM SERV_MED_BITACORA_EVENTO
UNION ALL SELECT 'ATRIBUTO',   COUNT(*) FROM SERV_MED_BITACORA_ATRIBUTO
UNION ALL SELECT 'METRICA',    COUNT(*) FROM SERV_MED_BITACORA_METRICA
UNION ALL SELECT 'LOTE',       COUNT(*) FROM SERV_MED_BITACORA_LOTE
UNION ALL SELECT 'ESTADO_MES', COUNT(*) FROM SERV_MED_BITACORA_ESTADO_MES
UNION ALL SELECT 'NOMENCL',    COUNT(*) FROM SERV_MED_BITACORA_NOMENCL;
```

**Proceso:** 178 hojas detectadas, 166 procesadas, 12 saltadas por pertenecer a familias
fuera de alcance (carnet, CAPA, NOM-035, seguimiento especial). **0 hojas con error.**

**Control cruzado:** la misma carga se corrió antes en QA (`ONEWMS_QA`) y produjo
**exactamente los mismos cuatro conteos**. Dos bases independientes, mismo resultado.

---

## 2. Cobertura temporal — hallazgo principal

Los datos cargados **terminan en junio de 2026**.

| Familia | Ene | Feb | Mar | Abr | May | Jun | Jul | Ago | Sep |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| ATENCION | 1,917 | 1,742 | 2,147 | 2,138 | 2,351 | 2,245 | 4 | 0 | 0 |
| EXAMEN_INGRESO | 681 | 705 | 1,417 | 1,217 | 905 | 2,121 | 2 | 0 | 0 |
| INCAPACIDAD | 370 | 279 | 279 | 223 | 302 | 256 | 4 | 0 | 0 |
| ANTIDOPING | 89 | 127 | 123 | 132 | 190 | 124 | 34 | 1 | 0 |
| EXAMEN_PERIODICO | 135 | 421 | 218 | 197 | 185 | 109 | 3 | 0 | 0 |
| EXAMEN_POS_INCAP | 52 | 41 | 44 | 39 | 41 | 58 | 0 | 0 | 0 |
| ACCIDENTE | 52 | 16 | 32 | 28 | 44 | 32 | 0 | 0 | 0 |
| MATERNIDAD | 13 | 10 | 0 | 5 | 0 | 0 | 0 | 0 | 0 |

**No es un defecto de la carga: es la antigüedad del origen.** El archivo de Excel más
reciente de la carpeta de origen fue modificado el **3 de julio de 2026 a las 04:13**.
Distribución de los 132 archivos por mes de última modificación:

| Mes | Archivos |
|---|---:|
| 2026-02 | 3 |
| 2026-04 | 3 |
| 2026-05 | 10 |
| 2026-06 | 43 |
| 2026-07 | 73 |

**Implicación:** cualquier tablero construido sobre estos datos mostrará julio, agosto y
septiembre vacíos. Para cerrar la brecha hay que obtener los Excel al corte de septiembre
y recargar — ver el riesgo del punto 6 antes de intentarlo.

```sql
SELECT FAMILIA, ANIO,
       SUM(CASE WHEN MES = 1 THEN 1 ELSE 0 END) ene,
       SUM(CASE WHEN MES = 2 THEN 1 ELSE 0 END) feb,
       SUM(CASE WHEN MES = 3 THEN 1 ELSE 0 END) mar,
       SUM(CASE WHEN MES = 4 THEN 1 ELSE 0 END) abr,
       SUM(CASE WHEN MES = 5 THEN 1 ELSE 0 END) may,
       SUM(CASE WHEN MES = 6 THEN 1 ELSE 0 END) jun,
       SUM(CASE WHEN MES = 7 THEN 1 ELSE 0 END) jul,
       SUM(CASE WHEN MES = 8 THEN 1 ELSE 0 END) ago,
       SUM(CASE WHEN MES = 9 THEN 1 ELSE 0 END) sep,
       SUM(CASE WHEN MES IS NULL THEN 1 ELSE 0 END) sin_mes,
       COUNT(*) total
  FROM SERV_MED_BITACORA_EVENTO
 GROUP BY FAMILIA, ANIO
 ORDER BY FAMILIA, ANIO;
```

**Nota sobre 2025:** 321 de los 24,236 eventos tienen año 2025. No es un error de carga:
son casos que los propios Excel marcan como `ANTES 2025` o `2025 PENDIENTE` — principalmente
incapacidades que iniciaron en 2025 y seguían abiertas. Servicio médico confirmó el
30-sep-2026 que no deben contarse como 2026.

---

## 3. Cuadre contra el propio Excel

Cada archivo de Excel trae una hoja `ACUMULADO` con los totales que el servicio médico
declara. El cargador contó los renglones y comparó contra ese total, hoja por hoja.

| Situación | Hojas | Renglones |
|---|---:|---:|
| Cuadra exacto con el ACUMULADO | 78 | 19,233 |
| **No cuadra** | **4** | **2,149** |
| Sin hoja ACUMULADO: no hay contra qué comparar | 76 | 4,398 |
| El Excel no lleva total (su ACUMULADO está en cero) | 8 | 628 |

**Resultado: 21,382 renglones de evento verificados contra la propia fuente — el 88.2% de
los 24,236 cargados.** La discrepancia total en esos 21,382 es de **5 renglones en valor
absoluto (0.023%)**, repartidos así:

| Predio | Familia | Se leyeron | Dice el Excel | Dif. |
|---|---|---:|---:|---:|
| AIFA | EXAMEN_INGRESO | 1,681 | 1,683 | −2 |
| MERCURIO | ANTIDOPING | 462 | 463 | −1 |
| SMO | EXAMEN_PERIODICO | 1 | 2 | −1 |
| TOLUCA | EXAMEN_POS_INCAP | 5 | 4 | +1 |

**Pendiente de resolver:** no se ha determinado cuál de los dos números tiene razón en cada
caso. Hay que abrir los cuatro archivos. Una diferencia de ±1 o ±2 es compatible tanto con
un `ACUMULADO` desactualizado como con un error de lectura; con solo cuatro casos, vale la
pena agotarlos.

**Renglones descartados: 0 en las nueve familias.** Ningún renglón fue leído y luego
desechado por el cargador.

```sql
-- Tablero de cuadre
SELECT NVL(CUADRA,'-') cuadra, COUNT(*) hojas, SUM(FILAS_LEIDAS) renglones
  FROM SERV_MED_BITACORA_LOTE GROUP BY NVL(CUADRA,'-');
--   S = cuadra · N = no cuadra · Z = el Excel no lleva total · nulo = sin ACUMULADO

-- Las que no cuadran, con su diferencia
SELECT PREDIO, FAMILIA, HOJA, FILAS_LEIDAS, ACUMULADO_ESPERADO,
       FILAS_LEIDAS - ACUMULADO_ESPERADO diferencia, ARCHIVO
  FROM SERV_MED_BITACORA_LOTE WHERE CUADRA = 'N';

-- Descartes: tiene que dar 0 en todas
SELECT FAMILIA, SUM(FILAS_LEIDAS) leidas, SUM(FILAS_INSERTADAS) insertadas,
       SUM(FILAS_DESCARTADAS) descartadas
  FROM SERV_MED_BITACORA_LOTE GROUP BY FAMILIA ORDER BY 2 DESC;
```

> Al leer la consulta de descartes: en la familia `CONSUMIBLE`, `INSERTADAS` cuenta métricas
> y no eventos, así que ser distinto de `LEIDAS` es correcto ahí y solo ahí.

---

## 4. Lo que no se pudo verificar

**2,854 eventos (11.8%) no tienen contra qué compararse**, porque sus archivos no traen hoja
`ACUMULADO` o la traen en cero.

| Familia | Renglones sin verificar | % de la familia |
|---|---:|---:|
| INCAPACIDAD | 1,929 | **100%** |
| ACCIDENTE | 225 | **100%** |
| EXAMEN_INGRESO | 36 | 0.5% |
| MATERNIDAD | 19 | 27% |
| ANTIDOPING | 17 | 2% |
| CONSUMIBLE | 2,172 | 100% (no son eventos) |

**Incapacidades y accidentes están sin verificar en su totalidad.** Incapacidades es la
segunda familia por volumen después de atenciones. Esto no significa que estén mal: significa
que no existe hoy un mecanismo que lo confirme.

```sql
SELECT FAMILIA, COUNT(*) hojas, SUM(FILAS_LEIDAS) renglones
  FROM SERV_MED_BITACORA_LOTE WHERE CUADRA IS NULL
 GROUP BY FAMILIA ORDER BY 3 DESC;
```

---

## 5. Calidad de la fecha

**64 eventos de 24,236 (0.26%) no tienen mes**, porque el Excel de origen no lo reporta. La
concentración no es pareja:

| Familia | Sin mes | Total | % |
|---|---:|---:|---:|
| MATERNIDAD | 42 | 70 | **60%** |
| ACCIDENTE | 21 | 225 | 9.3% |
| ANTIDOPING | 1 | 826 | 0.1% |

Maternidad concentra dos tercios de todo el problema de fecha del proyecto. Los 21 accidentes
sin mes son exactamente los 21 del ejercicio 2025.

**Una incapacidad quedó fechada en diciembre de 2026**, mes que aún no ocurre. Consistente con
un error de captura en el origen.

```sql
SELECT FAMILIA, COUNT(*) sin_mes FROM SERV_MED_BITACORA_EVENTO
 WHERE MES IS NULL GROUP BY FAMILIA;

SELECT * FROM SERV_MED_BITACORA_EVENTO
 WHERE ANIO = 2026 AND MES = 12;   -- fecha futura
```

---

## 6. Riesgo operativo abierto

**Una recarga con los Excel actualizados no cargaría los datos nuevos, y no avisaría.**

El cargador decide si una hoja ya fue procesada comparando `ARCHIVO` + `HOJA`. La tabla
`SERV_MED_BITACORA_LOTE` guarda un `CHECKSUM_SHA256` de cada archivo, **pero ese valor no se
consulta al decidir**. Consecuencia: si el Excel de un predio se actualiza con julio, agosto
y septiembre y se vuelve a correr la carga, esa hoja se salta por tener ya un lote en estado
`OK`. El proceso termina con código 0 y reporta éxito; los tres meses nuevos no entran.

Resolverlo requiere que el cargador compare el checksum guardado contra el del archivo y,
si difieren, borre el lote y lo vuelva a procesar. La columna ya existe.

**Hasta que eso se resuelva, actualizar los datos exige borrar manualmente los lotes de los
archivos modificados antes de volver a cargar.**

---

## 7. Transformaciones deliberadas

Los números del portal **no son idénticos a los del Excel**, por decisiones tomadas y
confirmadas. Se documentan aquí para que no se reporten como defectos:

| Transformación | Alcance | Origen de la decisión |
|---|---|---|
| Casos marcados `ANTES 2025` / `2025 PENDIENTE` contados como 2025, no 2026 | 265 renglones | Servicio médico, 30-sep-2026 |
| Años imposibles del Excel (2029, 2626, 2002) corregidos al año del archivo | ~15 renglones | Criterio del cargador |
| Personas contadas por nombre normalizado (mayúsculas, sin acentos, espacios simples) | Todas las familias | Los Excel no traen NSS |
| Cuatro predios renombrados a la grafía del catálogo del portal: `MACRO 1`→`MACRO I`, `MACRO 2`→`MACRO II`, `MKLS`→`MIKELS`, `UT`→`U TEPALCAPA` | 7,623 eventos | Corrección del 30-sep-2026 |
| Marca doble antidoping/alcoholemia resuelta solo como antidoping | — | Servicio médico, 30-sep-2026 |
| Etiquetas neutras (`N/A`, `NO APLICA`, `SIN DATO`) pierden contra cualquier valor real | Todas las familias | Servicio médico, 30-sep-2026 |

Si el portal reprodujera exactamente los totales del Excel, significaría que también heredó
sus errores de captura.

---

## 8. Por qué la pantalla no muestra el total del Excel

**Esta sección existe para evitar una conclusión equivocada.** Quien compare el total de un
Excel contra el número de una gráfica va a ver una diferencia, y el primer pensamiento
razonable es "la carga falló". No es el caso: la diferencia se explica completa, al renglón.

Las pantallas filtran por fecha — literalmente `FECHA >= inicio AND FECHA <= fin`. De ahí
salen dos restas, ambas correctas:

| Concepto | Eventos | Por qué |
|---|---:|---|
| Cargados en total | 24,236 | |
| Del ejercicio 2025 | 321 | Marcados `ANTES 2025` / `2025 PENDIENTE`. No salen cuando se pide 2026. |
| Sin fecha en el origen | 64 | El Excel no la reportó. No cumplen la comparación, así que **no entran en ninguna gráfica**. |

Las dos últimas cifras **se traslapan**: 50 de los 64 sin fecha son además del ejercicio 2025,
así que no se pueden restar una tras otra. El total exacto que queda visible lo calcula el
bloque E del script citado abajo, que cuadra al renglón contra los 24,236.

En sentido contrario, la pantalla puede mostrar **más**: a lo anterior le suma lo que haya en
ORDS, que es el portal en operación. Un número mayor tampoco es un defecto.

**Cómo verificarlo.** El script `docs/script-prod/excel-07-pantalla-vs-tabla.sql` calcula,
pantalla por pantalla y predio por predio, el número que debe aparecer, usando exactamente el
mismo filtro que aplica el código. Se ajusta el rango al que se eligió en la pantalla y se
comparan las dos cifras.

| Si la pantalla muestra… | Significa |
|---|---|
| Menos que `DEBE_MOSTRAR` | Defecto de la pantalla. Hay que investigarlo. |
| Más | Normalmente ORDS sumando sus propios registros. |
| `DEBE_MOSTRAR` es menor que el Excel | Está explicado arriba: fuera de rango o sin fecha. |

El bloque E de ese script cuadra el total al renglón: visible + fuera de rango + sin fecha
tiene que dar exactamente 24,236. **Ningún renglón se perdió en la carga.**

> Antecedente relevante: el 30 de septiembre se detectaron y corrigieron dos defectos de este
> tipo, donde el dato estaba correctamente cargado y la pantalla mostraba cero — cuatro
> predios escritos con otra grafía, y una serie mensual que se construía antes de incorporar
> el histórico. Ambos están resueltos. Es el motivo por el que existe este cuadre: un dato
> bien cargado no garantiza un dato bien mostrado.

---

## 9. Distribución por predio

15 predios con datos. Los cinco primeros concentran el 71%:

| Predio | Eventos |
|---|---:|
| AIFA | 5,480 |
| MACRO II | 4,062 |
| MACRO I | 3,050 |
| Z VALLEJO | 2,551 |
| TOLUCA | 2,473 |
| *(otros 10)* | 6,620 |

**Ningún evento quedó sin predio** (los 15 suman exactamente 24,236).

```sql
SELECT NVL(PREDIO,'(sin predio)') predio, COUNT(*) eventos
  FROM SERV_MED_BITACORA_EVENTO GROUP BY PREDIO ORDER BY 2 DESC;

-- Control: ningún predio fuera del catálogo de 17 sitios del portal.
-- Tiene que salir vacía.
SELECT e.PREDIO, COUNT(*) FROM SERV_MED_BITACORA_EVENTO e
 WHERE e.PREDIO NOT IN ('AIFA','ATIZAPAN','CHARCON','FLORA','MACRO I','MACRO II',
       'MERCURIO','MIKELS','SIGLO XXI','SMO','TOLUCA','TULTIPARK','U TEPALCAPA',
       'WORLD PARK','Z VALLEJO','FORANEO','SIN DATO')
 GROUP BY e.PREDIO;
```

---

## 10. Resumen para decidir

| | |
|---|---|
| Datos cargados y verificados en producción | **Sí** — 24,236 eventos, 88.2% cuadrado contra la propia fuente, 0.023% de discrepancia |
| Datos al día | **No** — terminan en junio; faltan julio, agosto y septiembre |
| La actualización mensual funciona hoy | **No** — se saltaría en silencio (punto 6) |
| Incapacidades y accidentes verificados | **No** — 2,154 renglones sin mecanismo de cuadre |

**Lo que requiere decisión del PM:**

1. Obtener los Excel al corte de septiembre. Sin esto los tableros muestran tres meses vacíos.
2. Autorizar la corrección del cargador para que compare el checksum. Sin esto, el punto 1
   no se puede ejecutar de forma segura.
3. Definir si se invierte en verificar incapacidades y accidentes, o se aceptan sin cuadre.
4. Resolver las cuatro hojas que no cuadran (cinco renglones en total).

---

### Scripts de verificación

Todos solo lectura, en `docs/script-prod/`:

| Archivo | Qué responde |
|---|---|
| `excel-04-que-se-cargo.sql` | Qué hay en cada tabla, por familia y por predio |
| `excel-06-cuadre.sql` | Todo el contenido de los puntos 2, 3, 4 y 5 de este documento |
| `excel-07-pantalla-vs-tabla.sql` | El punto 8: qué número debe mostrar cada pantalla |
| `excel-03-verificacion-carga.sql` | Verificación posterior a la carga (años, predios a fundir) |
