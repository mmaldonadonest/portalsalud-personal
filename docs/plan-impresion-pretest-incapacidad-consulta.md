# Plan — Impresión PDF en Pre-Test, Incapacidades y Consulta Médica

**Estado:** **EJECUTADO el 14-sep-2026** (reutilizando la mecánica del examen; verificado con Playwright). Ver `docs/contexto-sesion-11-sep-2026.md` §0a para lo construido y las decisiones tomadas por defecto (§4 de este documento). Si Salud Ocupacional entrega un layout/clave FT-SO, se adapta pasando la clave al fragmento `formato` y ajustando la plantilla correspondiente.
**Análisis original (14-sep, mañana):**
**Pregunta del usuario:** ¿estos 3 módulos deben o pueden imprimir? ¿hay plantilla y datos suficientes?

---

## 1. Respuesta corta

| Módulo | ¿Plantilla en el legacy? | ¿Datos suficientes? | ¿Tiene firma? | ¿Puede imprimir? |
|---|---|---|---|---|
| **Pre-Test** | No hay PDF, **sí hay la vista completa** del PHP (`view/content/pretest.html`, 705 líneas) que sirve de contenido | **Sí, completo** — 27 campos + 15 preguntas + síntomas COVID + comentarios | **Sí** (`drawdataUrl` en `SERV_MED_TAG`) | **Sí, es el mejor candidato** |
| **Incapacidad** | No | **Sí** — 21 campos del WS `consulta_incapacidad` | **Sí, y con datos reales** (6 de 7 registros del NSS de prueba traen firma) | Sí, pero **decidir qué documento es** (ver §4) |
| **Consulta médica** | No hay PDF; la vista `consultaMedica.html` del PHP da el orden de los campos | **Sí** — 18 campos (signos vitales, motivo, exploración, diagnóstico, tratamiento) + adjuntos | Campo sí; **0 de 3 NSS probados tienen firma real** (el histórico se guardó sin ella) | Sí |

**Ninguno de los tres imprimía en el PHP.** Verificado: en `php-old/pdf/` sólo hay `pdfGenerator.php` y `generatePDFEX.php`, ambos del examen médico; los únicos formatos oficiales que existen en todo el repo son **FT-SO-04** (examen), **FT-SO-11** (consentimiento) y **FT-SO-32** (aviso de privacidad). La librería `pdfmake` que aparece en `view/plugins/` viene con DataTables y no la usa ninguna pantalla del sistema médico.

Conclusión: **se puede**, con la misma mecánica ya probada en el examen (vista HTML + `@page` + `window.print()`, el PDF lo genera el navegador). Es funcionalidad nueva, no migración, así que el **contenido de cada documento hay que definirlo con el médico**.

---

## 2. Qué datos hay exactamente (verificado contra los WS, 14-sep-2026)

### Pre-Test — `SERV_MED_TAG` (base del portal), sufijo `PRETEST`
- **Identificación** (27 campos): nss, cuenta, puesto, agencia, apellidos, nombre, fecha de nacimiento, estado civil, IMSS, teléfonos (personal, casa), domicilio (calle, número, colonia, delegación), contacto de emergencia y a quién contactar.
- **Cuestionario de salud** (7): operado <6 meses, medicamentos, alergias, fracturas/hernias, embarazo, enfermedades, consumo de drogas — cada uno Sí/No + observaciones.
- **Síntomas COVID** (8): tos, fiebre, dolor de cabeza, dificultad respiratoria, dolor de garganta, escurrimiento nasal, ojos rojos, dolor muscular — Sí/No + observaciones.
- **Firma del paciente** + comentarios.
- Los 15 códigos de pregunta del portal Java (`LHOP`, `TALMED`, `SUAA`, …) **coinciden exactamente** con los del PHP, así que la vista del legacy sirve como plantilla de contenido sin traducción.
- Ventaja: los datos están en la **base del portal** (JDBC), no en ORDS → una sola lectura, sin dependencia del WS.

### Incapacidad — WS `Servcio/consulta_incapacidad`
folio, ramo, tipo, fecha de inicio, fecha de término, días autorizados, salario integrado, costo, imputable, estado de dictamen, goce de sueldo, complemento salarial, rubro, fecha de alta, ST2, alta, salario acumulado, URL de archivos, **firma digital**, fecha de registro.

### Consulta médica — WS `Servcio/conuslta_medica_usuario`
fecha, tipo de consulta, área de accidente, área involucrada, causa, peso, talla, IMC, FC, FR, TA, temperatura, motivo, exploración, diagnóstico, tratamiento, usuario que capturó, **firma digital**, y la relación con sus **adjuntos** (`SERV_MED_FS_FILE`).

En los tres casos la **ficha del empleado** (nombre, RFC, NSS, cuenta, empresa, puesto, edad, sexo) sale de `NssSearchService`, igual que en el expediente del examen.

---

## 3. Qué reutilizamos del examen (ya hecho y probado)

El documento del examen dejó infraestructura que sirve tal cual para los tres:

- **Encabezado y pie repetidos por hoja** con logo, "SISTEMA MEDICO ONEST", fecha/hora de impresión y NSS, vía `thead`/`tfoot` de una tabla contenedora (sin la URL que mete el navegador).
- **`@page { size: A4; margin: 0 }`** + saltos de página por fila (`<tr class="hoja">`).
- **`?auto=1`** para abrir con el diálogo de imprimir/guardar como PDF ya lanzado.
- **Descarga al guardar**: el patrón de `guardarExamen()` (abrir la pestaña en el clic para que no la bloquee el navegador, navegarla al recibir "Registro aceptado", enlace de respaldo).
- **`@Auditado(accion="export")`** para que cada impresión quede en la bitácora.
- Las imágenes `Imagen1.png` (logo) y `firmaDoc.png` ya están en `static/img/examen/`.

Lo sensato es **extraer eso a un fragmento común** (`fragments/documento-print.html`) y que los cuatro documentos lo usen, en vez de copiar el CSS tres veces.

---

## 4. Decisiones que se necesitan antes de construir

1. **¿Qué documento es cada uno?** Ninguno existe en papel hoy:
   - *Pre-Test*: es una **declaración firmada del candidato** (salud + COVID). Propongo: encabezado tipo FT-SO con clave de control **por asignar**, ficha, las 15 preguntas con su respuesta y observaciones, comentarios y firma del paciente.
   - *Consulta médica*: es una **nota médica**. Su contenido está normado (NOM-004-SSA3, expediente clínico) — conviene que **el médico valide** qué debe incluir y si lleva su firma y cédula, antes de fijar el formato.
   - *Incapacidad*: **ojo**, el documento oficial de incapacidad lo expide el IMSS (ST-2/ST-7, etc.); lo que aquí se imprimiría es una **constancia interna de control** (folio, ramo, días, costo, dictamen). Hay que confirmar con negocio que eso es lo que quieren y cómo debe llamarse, para no dar la impresión de un documento oficial del instituto.
2. **Clave de control y "Revisión"** de cada formato (FT-SO-xx). El examen usa FT-SO-04/11/32; si estos documentos van a ser oficiales, Salud Ocupacional debe asignarles clave, o se imprimen sin ella.
3. **Bloque de firma del médico**: hoy el examen usa la firma fija del Dr. Cerón (heredada del PHP). Para estos tres, ¿firma fija, o nombre y cédula del médico logueado? (La **cédula profesional no existe en ningún catálogo** del sistema; habría que capturarla.)
4. **Cuándo se abre**: ¿sólo al guardar (como el examen), sólo con botón en el detalle, o ambos? Propongo **ambos**: botón "Imprimir" en el detalle + apertura automática al guardar cuando hay firma.

---

## 5. Plan de trabajo propuesto

| # | Paso | Detalle | Est. |
|---|---|---|---|
| 1 | Fragmento común de impresión | Extraer del examen el CSS `@media print`, encabezado/pie por hoja, `?auto=1`; dejarlo reutilizable | 1.5 h |
| 2 | Documento de **Pre-Test** | `GET /api/nss/pretest/imprimir?nss=` → vista con ficha + 15 preguntas + COVID + comentarios + firma; datos desde `SERV_MED_TAG` (sin WS) | 2.5 h |
| 3 | Documento de **Consulta médica** | `GET /api/nss/consulta/imprimir?nss=&id=` → nota médica con signos vitales, motivo, exploración, diagnóstico, tratamiento, adjuntos y firma | 2 h |
| 4 | Documento de **Incapacidad** | `GET /api/nss/incapacidad/imprimir?nss=&id=` → constancia con folio, ramo, tipo, periodo, días, dictamen, costo y firma | 2 h |
| 5 | Botón "Imprimir" en los detalles | Modal de consulta y de incapacidad + pantalla de Pre-Test | 0.5 h |
| 6 | Descarga al guardar | Mismo patrón de `guardarExamen()` en los 3 formularios (pestaña en el clic + enlace de respaldo) | 1 h |
| 7 | Auditoría | `@Auditado(accion="export")` en los 3 endpoints | incluido |
| 8 | Verificación | Playwright: guardar con firma → pestaña → PDF A4 con firma visible; revisar paginación en Chrome y Edge | 1 h |
| | **Total** | | **~10.5 h** (≈1.3 jornadas) |

Anclado al precedente real: el documento del examen (518 variables, 12 hojas) llevó ~4 h porque se portó con script desde el PHP; estos tres son mucho más chicos (18–50 campos) pero se escriben desde cero y necesitan definición de contenido.

**No requiere SQL ni cambios en ORDS**: los tres leen de fuentes que ya consume el portal (WS existentes + `SERV_MED_TAG` + `SERV_MED_FS_FILE`).

---

## 6. Riesgos y hallazgos a considerar

- **Consultas sin firma**: en los 3 NSS probados, ninguna consulta médica histórica trae `firma_digital` (llega `"sin datos"`). El documento saldría con la línea de firma en blanco hasta que se capture desde el portal. No es bloqueante, pero conviene que negocio lo sepa.
- **Incapacidades sí tienen firma** en registros recientes (6 de 7 en el NSS 30048315698), pero 0 de 12 en otro NSS — la cobertura es despareja.
- **Diagnóstico ICD**: las consultas históricas tampoco traen clave CIE-10 (mismo hallazgo del módulo Musculoesqueléticas), así que ese renglón del documento saldrá vacío en el histórico.
- El catálogo ICD del WS sólo tiene 909 claves (capítulos A y B) — si la nota médica debe llevar diagnóstico codificado, este hueco la afecta igual.
