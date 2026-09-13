# Plan — Descargar el documento inmediatamente al firmar (examen, consulta, incapacidad)

**Estado:** EN EJECUCIÓN (opción A). Pedido el 11-sep-2026. **12-sep: hecho para el examen médico** (paso 4 en `guardarExamen()`); consulta e incapacidad siguen pendientes de decisión de contenido.
**Petición:** "el examen médico y todo lo que tenga firma al final descargue el documento inmediatamente".

## 1. Qué hay hoy (verificado contra el código)

| | PHP legacy (`php-old`) | Portal Java |
|---|---|---|
| Formularios con firma | 3: consulta médica (`consultMedic.php`), incapacidad (`incap.php`), examen médico (`Medico`) — canvas `#draw-dataUrl` → campo `FIRMA` | Los mismos 3: `consulta-form.html`, `incapacidad-form.html`, `examen-shell.html` — canvas `#firmaCanvas` → hidden `firma` (data URL PNG) |
| Qué pasa al guardar con firma | Solo un `Swal.fire("success")`. **No descarga nada.** | Igual: mensaje de éxito. |
| Documento imprimible | Solo el examen: menú "Imprimir expediente" → `pdf/pdfGenerator.php` (HTML + `window.print`). Consulta e incapacidad **no tienen** documento. | Solo el examen: "Imprimir expediente" (hecho 11-sep, `/api/nss/examen/imprimir`). |
| La firma en el documento | El PDF del examen **no** pinta la firma capturada: pone una imagen fija `files/firmaDoc.png` y el texto fijo "Dr. Mauricio Cerón Solana CP 5154089 / 7237048". | La vista de impresión trae líneas de firma en blanco. |
| La firma guardada ¿se puede leer? | Sí | Sí: examen `FIRMA_DIGITAL`, consulta `ConsultaDetalleDto.firmaDigital`, incapacidad `firma_digital` (los 3 WS la devuelven; `'sin firma'` / `'sin datos'` cuando no hay). |

Conclusión: **no es una migración, es funcionalidad nueva** — ni el PHP lo hacía. Sí es posible, y ya está la mitad del camino (vista de impresión del examen, firma en los 3 WS).

## 2. Dos formas de "descargar inmediatamente"

| | A — Vista de impresión + diálogo del navegador | B — PDF generado en servidor |
|---|---|---|
| Qué ve el usuario | Al guardar se abre una pestaña con el documento y aparece el diálogo de imprimir ya abierto: elige "Guardar como PDF" y listo (1 clic). | Al guardar se descarga `examen_<nss>_<fecha>.pdf` directo a la carpeta de descargas, sin diálogo. |
| Librerías | Ninguna (es lo que ya hace el examen). | `openpdf 2.0.3` o `itext html2pdf 3.0.5` — están en el repositorio Maven local, se pueden agregar offline. Hay que validar licencia: iText 7 es AGPL (uso comercial requiere licencia), OpenPDF es LGPL/MPL (ok). |
| Fidelidad | La misma vista de siempre; el PDF depende del navegador (Chrome/Edge lo hacen bien). | Layout propio en Java; hay que rehacer el documento en la librería (o render HTML→PDF con html2pdf, que respeta bastante CSS). |
| Riesgo | Bloqueador de pop-ups: el `window.open` ocurre después de un `fetch` asíncrono; algunos navegadores lo bloquean. Mitigación: abrir la pestaña **en el clic** de Guardar y navegarla al terminar, y además dejar un botón "Descargar documento" en el mensaje de éxito. | El PDF se puede además **guardar en `APP_FS_FILE`** como adjunto del expediente (queda evidencia del documento firmado, descargable después desde Historico E.M / detalle de consulta). |
| Esfuerzo | ~7 h | ~11 h (A + 4 h de librería/render) |

**Recomendación:** hacer **A ahora** (cubre "descarga inmediata" con lo que ya existe) dejando el documento preparado para B: si después quieren el archivo sin diálogo o guardarlo como evidencia en el expediente, B se monta encima sin rehacer nada.

## 3. Pasos (opción A)

1. **Firma en el documento** (0.5 h) — **Actualizado 11-sep noche:** la vista de impresión del examen ya es el port 1:1 de `pdfGenerator.php` (`pages/examen-documento.html` + `ExamenDocumentoService`), y ya pinta la firma capturada del trabajador (`FIRMA_DIGITAL`) en "Firma del Trabajador" como lo hacía el PHP. Queda solo decidir si el bloque "Realizó" sigue con la firma fija `firmaDoc.png` + texto fijo del Dr. Cerón, o toma nombre/firma del médico logueado.
2. **Documento de consulta médica** (2 h) — Nueva vista `/api/nss/consulta/imprimir?nss=&id=` con los datos de `ConsultaDetalleDto` (fecha, tipo, área, causa, signos vitales, motivo, exploración, diagnóstico, tratamiento, firma). Sin precedente en PHP: **confirmar con el médico qué debe llevar**.
3. **Documento de incapacidad** (2 h) — Nueva vista `/api/nss/incapacidad/imprimir?nss=&id=` (folio, ramo, tipo, inicio/término, días, costo, dictamen, rubro, firma). Mismo aviso: sin precedente.
4. **Descarga inmediata** (1 h) — **Examen: HECHO 12-sep** (`nss-search.html` → `guardarExamen()`: si hay firma, abre la pestaña en el clic, la navega a `/api/nss/examen/imprimir?nss=&auto=1` cuando el WS responde "Registro aceptado", la cierra si falla; enlace "Descargar expediente (PDF)" junto al resultado por si el navegador bloquea pop-ups; verificado con Playwright: pestaña abierta, firma del trabajador pintada, `window.print` disparado). Para consulta e incapacidad, en los 3 formularios: en el clic de Guardar abrir la pestaña vacía, y al recibir el "Registro aceptado" navegarla a la vista con `?auto=1`; la vista con `auto=1` lanza `window.print()` al cargar. Botón "Descargar documento" en el mensaje de éxito por si el navegador bloqueó la pestaña.
5. **Auditoría** — Las 3 vistas con `@Auditado(accion="export")` (el examen ya lo tiene).
6. **Verificación** (1 h) — Playwright: guardar con firma en cada formulario → pestaña abierta → PDF A4 con la firma visible; revisar en Chrome y Edge el pop-up.

## 4. Decisiones que necesito antes de ejecutar

- **A o B** (y si B: OpenPDF por licencia, salvo que ya tengan licencia de iText).
- Contenido del documento de **consulta** y de **incapacidad** (no existen en el PHP; propongo los campos de arriba).
- Nombre/cédula del médico: tomarlo del usuario logueado (hoy solo tenemos NSS y nombre; la **cédula profesional no está en ningún catálogo** — habría que capturarla en el perfil o en un catálogo de médicos) o dejarlo fijo como el PHP.
- Si el examen se firma en **cada guardado parcial** (el acordeón guarda por secciones), ¿se descarga en cada uno o solo cuando se guarda la sección de Cierre / dictamen? Propongo solo al cerrar.

## 5. Qué NO requiere

Ni SQL en la base del portal ni cambios en ORDS: la firma ya viaja y ya se devuelve en los 3 WS. Si en B se guarda el PDF como adjunto, se usa `APP_FS_FILE` tal cual.
