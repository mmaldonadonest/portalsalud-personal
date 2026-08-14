# Normalización de encoding en `MED_TAG.CONTENT` — reporte y plan

**Fecha:** 13 de agosto de 2026
**Alcance:** tabla `MED_TAG` (Oracle, destino del ETL de `servicioMedico.tags`)
**Estado:** diagnóstico completo y verificado — **fix no ejecutado todavía**, pendiente de luz verde

---

## 1. Qué se encontró

Revisando `MED_TAG` en SQL Developer se detectaron valores con acentos y `ñ` corruptos (ej. `"3 aÃ±os"` en vez de `"3 años"`, `"presiÃ³n hidrÃ¡ulica"` en vez de `"presión hidráulica"`).

## 2. Causa raíz confirmada

**El origen es previo a nuestro ETL — no lo introdujo la migración de hoy.** Se verificó consultando directamente `servicioMedico.tags` en MariaDB (antes de cualquier paso nuestro) y las mismas filas ya traían la misma corrupción exacta. Es un problema heredado del sistema legacy (captura o inserción con un cliente/conexión mal configurado, probablemente hace años), igual en naturaleza al de los ~96 nombres de archivo con encoding roto ya documentados en la migración de `files`.

**Mecanismo exacto**: los bytes UTF-8 originales se reinterpretaron como **Windows-1252** (no Latin-1/ISO-8859-1 estricto) en algún punto de la captura original, y se volvieron a guardar como si esa reinterpretación fuera el texto real. Ejemplo: `ñ` (UTF-8 = bytes `C3 B1`) reinterpretado byte a byte bajo cp1252 da como resultado los dos caracteres `Ã` + `±`, que es exactamente lo que se ve almacenado.

Un detalle adicional que costó encontrar: 5 posiciones de cp1252 (`0x81`, `0x8D`, `0x8F`, `0x90`, `0x9D`) están oficialmente "sin definir" — el corruptor original las trató como Latin-1 puro (código = byte), lo que en pantalla se ve como un carácter invisible pegado a la `Ã` (ej. `Á` → `Ã` + un carácter de control invisible). Esto explica casos como `"ÃREA"` en vez de `"ÁREA"`. El algoritmo de reversión tuvo que replicar ese mismo mapeo (cp1252 + estos 5 huecos rellenados como Latin-1) para poder revertir el 100% de los casos, no solo la mayoría.

## 3. Alcance real (verificado, no estimado)

| Métrica | Valor |
|---|---|
| Filas de `MED_TAG` con `CONTENT` no vacío | 400,293 |
| Filas con la marca de corrupción (`Ã`) | 5,453 (1.36%) |
| **Reversibles de forma segura y verificada** | **5,453 (100%)** |

"Verificado" significa: para cada fila se aplicó la reversión y luego se decodificó el resultado como UTF-8 en **modo estricto** (rechaza cualquier secuencia inválida) — solo se cuenta como reversible si ese *round-trip* produce texto válido. Cero falsos positivos por diseño: una fila que ya está correcta, o cuya corrupción no siga este patrón exacto, simplemente no se toca.

### Desglose por grupo de campo (`TAG_GROUP`)

| Grupo | Filas afectadas |
|---|---|
| HISTORIA_LABORAL | 3,724 |
| PRETEST | 1,104 |
| CONTACTO_EMERGENCIA | 439 |
| CLINICO_MISC | 170 |
| EXAMEN_FISICO | 16 |

### Top de campos (`TYPE`) más afectados

| Campo | Filas |
|---|---|
| `antiguedad1` | 1,222 |
| `antiguedad2` | 916 |
| `antiguedad3` | 589 |
| `antiguedad4` | 214 |
| `contactar_aPRETEST` | 167 |
| `comentariosPRETEST` | 164 |
| `contactoEmerparentesco1` | 159 |
| `ciruOBS` | 147 |
| `descripcion1` | 142 |
| `apePRETEST` / `apmPRETEST` | 133 c/u |

*(lista completa de 5,453 filas con id/campo/original/corregido disponible en el CSV de trabajo si se necesita para auditoría — no se incluye aquí completa por tamaño)*

## 4. Muestra real (original → corregido)

| id | campo | antes | después |
|---|---|---|---|
| 199 | antiguedad1 | `3 aÃ±os` | `3 años` |
| 204 | observaciones1 | `se trabaja con maquinas de presiÃ³n hidrÃ¡ulica` | `se trabaja con maquinas de presión hidráulica` |
| 572 | giro1 | `Servicios sociales pÃºblicos` | `Servicios sociales públicos` |
| 718 | descripcion1 | `DesempeÃ±ar funciones en el Ã¡rea clÃ­nica` | `Desempeñar funciones en el área clínica` |

## 5. Plan de ejecución

1. **Este reporte** — evidencia y alcance antes de tocar datos (hecho, este documento).
2. **Ejecutar el `UPDATE` controlado** contra `MED_TAG` en Oracle local: para cada fila candidata, recalcular el fix, re-validar el *round-trip* estricto en el momento de la ejecución (no confiar en el escaneo previo) y actualizar solo si valida — mismo criterio de seguridad que el diagnóstico, no un `REPLACE` ciego de caracteres.
3. **Verificación post-fix**: re-correr el escaneo. Debe dar 0 filas candidatas restantes (o solo casos genuinamente nuevos que aparezcan por otra causa, no por este patrón).
4. **Spot-check manual**: releer 5-10 filas al azar de la tabla en SQL Developer para confirmar visualmente.
5. **No aplica a MariaDB origen** — el arreglo es solo en Oracle (`MED_TAG`), el dato de MariaDB queda como está (es la copia de trabajo, no el sistema de producción).

### Fuera de alcance de este plan (mencionar, no ejecutar todavía)

- Los ~96 nombres de archivo con encoding roto en `APP_FS_FILE.ORIGINAL_NAME` (documentados en la migración de `files`, decisión previa: corregir a mano). La técnica de este documento (cp1252 + relleno Latin-1 en los 5 huecos) probablemente los resuelve también — evaluar si conviene aplicarla ahí en vez de corrección manual, en un pase aparte.
- `MED_TAG.NSS` y `MED_TAG.TYPE` no se revisaron (son NSS numérico y nombres de campo en inglés/código, sin acentos por diseño — riesgo de corrupción ahí es nulo).
