# Traspaso: correcciones del examen inicial EMNI

**Fecha:** 6 de noviembre de 2026
**Fuente:** `docs/09102026/ultimos/CORRECION EMNI.pptx`
**Estado:** programado en local; falta probar con Tomcat y guardado real

## Situación

- Los cambios están en el árbol de trabajo de `C:\miguel\portalsalud-personal`.
- No hay commit, no hay WAR nuevo y no se ejecutó nada en ninguna BD.
- No requiere SQL ni cambios en el WS. El biométrico no se modifica.
- Compila. La pantalla se generó sin BD y pasaron 30 de 30 pruebas de botones y campos en una copia estática.
- **Falta probar:** autoguardado, finalizar, impreso, expediente y Examen médico.
- **Ojo con git:** varios de estos archivos ya tenían cambios sin commit de otro trabajo. No usar `git checkout` para revertir ni `git add -A`.

## Decisiones confirmadas por el usuario

| Tema | Decisión |
|---|---|
| Conflicto con el cambio #13 | Manda el PPTX: en Cráneo, Sí verde y No rojo |
| Observaciones de la hoja 5 | Salen en el impreso |
| Agregado faltante | Solo avisa; no impide guardar |
| Genitales | Se elige a mano; solo uno |
| Teléfono del trabajador | Solo en Portal Salud; no tocar el biométrico |

## Qué se cambió

| Área | Cambio | Cómo quedó |
|---|---|---|
| Hoja 1 | Teléfono actualizado | Campo `telefonoSalud`; al finalizar se guarda en el tag `TELSALUD` |
| Expediente | Teléfono cortado | Usa `TELSALUD` si existe; el texto puede ocupar más de una línea |
| Impreso | Teléfono | `TELSALUD` reemplaza `telefono` |
| No patológicos | Bloques y tipos de campo | Vivienda, Higiene personal, Alimentación, Actividad física y recreación, Otros |
| Inmunizaciones | Colores y orden | Sí verde, No rojo; fechas junto a Influenza y Td |
| Inmunizaciones | Fecha de Td | Tag nuevo `fechaTD`; también sale en el impreso |
| Gineco-obstétricos | Tipos de campo | Números en Menarca, IVSA, G, P, A y C; fechas en FUM y FUP |
| Hoja 5 | Observaciones | Tags `APOBCONG`, `APOBGENI`, `APOBINFA`, `APOBDERM`, `APOBAUDI`, `APOBRESP`, `APOBOLFA`, `APOBTRAU` |
| Exploración | Sí/No | La selección se guarda en el borrador como `OKNO.<campo>`; escribir no la cambia |
| Exploración | Aviso | Sí o No con texto vacío marca borde rojo y no bloquea |
| Genitales | Solo uno | Elegir uno limpia el otro; se quitó "Todo normal" de esa sección |

La tabla `SERV_MED_ANT_PER_PATOLOGICOS` no tiene columnas de observaciones, por eso van en `SERV_MED_TAG`.

## Archivos modificados

- `src/main/java/com/onest/app/catalog/examen/service/ExamenService.java`
- `src/main/java/com/onest/app/catalog/examen/service/ExamenDocumentoService.java`
- `src/main/java/com/onest/app/catalog/exameninicial/service/ExamenInicialService.java`
- `src/main/java/com/onest/app/catalog/nss/web/NssSearchController.java`
- `src/main/resources/templates/fragments/examen-inicial-shell.html`
- `src/main/resources/templates/fragments/formato-emle.html`
- `src/main/resources/templates/fragments/nss-result.html`
- `src/main/resources/templates/pages/nss-search.html`

El detalle exacto de cada reemplazo y la prueba estática están en:

- `C:\Users\Miguel\AppData\Local\Temp\claude\7edbfeea33d5cc26\85451392-fb59-445e-9482-5061c3db82a8\scratchpad\aplica_emni.py`
- `C:\Users\Miguel\AppData\Local\Temp\claude\7edbfeea33d5cc26\85451392-fb59-445e-9482-5061c3db82a8\scratchpad\prueba_emni.py`

## Decisiones técnicas por revisar

- **Higiene personal y Alimentación:** ~~son subtítulos, como marca el PPTX~~. DECIDIDO 9-nov: son subtítulo de su bloque **y** siguen como campo de captura, como en producción (se quitó `SOLO_SUBTITULO`).
- **Inmunizaciones (9-nov):** la cuadrícula de 7 columnas se salía del recuadro; quedan 5 columnas y COVID con su fecha en un tercer renglón.
- **Caja de la hoja 5:** queda siempre visible, igual que en las demás secciones de esa hoja.
- **Examen médico:** comparte el catálogo, así que también muestra esa caja y su "Todo normal" ahora escribe NEGADOS en esas 8 observaciones. Esto cubre parte del #10, pero el #10 pedía mostrarla solo con Sí.
- **Agudeza visual:** el botón dice Sí, pero el texto guardado sigue siendo "OK".
- **Teléfono:** se guarda al finalizar, así que un examen bloqueado no puede actualizarlo.
- **Campos sin anotación de número:** No. habitantes sigue como texto, como marca el PPTX. No. comidas al día también.
- **Datos anteriores:** los valores que no son número o fecha se conservan hasta que alguien los edite.

## Examen inicial bloqueado

El bloqueo reportado **no es por permisos**. Es la regla `ExamenInicialService.candado`:

- Si el trabajador ya es empleado y tiene un examen con dictamen, no se puede finalizar.
- Solo se permite si el último examen fue un inicial No apto.
- Lo corregido se queda en borrador. El impreso no lee borradores, por eso la corrección no aparece.

**Decisión del usuario (6-nov):** NO se reabre un inicial finalizado y el candado no se quita, para mantener el control. La regla se queda como está y se verificó que las correcciones EMNI no la tocaron: el método `candado` no aparece en el diff y `finalizar` lo vuelve a evaluar en el servidor.

## Siguientes pasos

1. Pedir autorización antes de probar con guardado real. El NSS de prueba `30048315698` escribe en la BD y ensucia indicadores.
2. Probar autoguardado, reapertura, finalizar, impreso FT-SO-04 y expediente.
3. Revisar Examen médico, por el catálogo compartido.
4. ~~Resolver la regla para reabrir un inicial finalizado.~~ Decidido: no se reabre, el candado se queda.
5. Pasar al usuario el texto del control de cambios y capturarlo solo cuando lo valide.

## Control de cambios

Archivo: `C:\miguel\control-cambios-29oct\control-de-cambios-portal-salud-29oct.xlsx`

**Texto propuesto para el #13:**

> Cráneo: Sí en verde y No en rojo. Inmunizaciones: Sí en verde y No en rojo.

**Filas nuevas por redactar:**

- Teléfono del trabajador en Portal Salud.
- Teléfono completo en el expediente.
- Tipos de campo en No patológicos.
- Orden y fecha de Td en Inmunizaciones.
- Tipos de campo en Gineco-obstétricos.
- Observaciones de la hoja 5.
- Elección de Sí/No en Exploración.
- Genitales con una sola opción.

El #14, dividir Toxicológico, no venía en el PPTX y sigue pendiente.

## Reglas vigentes

- No ejecutar nada en ninguna BD, ni lecturas. Entregar `.sql` y lo corre el usuario.
- No hacer commit, push ni `git add` sin permiso para esa tarea.
- No modificar el proyecto Registro de Empleados.
