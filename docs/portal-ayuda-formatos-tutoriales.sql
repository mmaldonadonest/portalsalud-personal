-- =============================================================================
-- BASE: PORTAL (JDBC, dentro de BIOMETRICO), tabla SERV_MED_AYUDA_ARTICULO (ya existe).
-- Ayuda del portal EMLE (21-oct-2026): articulos de lo nuevo (formatos FT-SO, firmas digitalizadas,
-- pruebas especiales, pases) y actualizacion de los que cambiaron. Los articulos con enlace
-- "...?tutorial=<id>" muestran el boton «Muestrame como», que abre el tutorial guiado
-- (src/main/resources/static/js/ayuda-tutoriales.js).
--
-- Mismo metodo que src/main/resources/db/sql/app_domain/ayuda.sql: procedimiento temporal que hace
-- MERGE por CLAVE (si el articulo existe lo REEMPLAZA; si no, lo crea) y al final se borra.
-- OJO: si un administrador edito en el portal alguno de los articulos que aqui se actualizan
-- (buscar-nss, consulta-alta, examen-medico, dictamen-significado, antidoping, ayuda-como-usar),
-- su texto se reemplaza por este.
-- Abrir el archivo como UTF-8 (lleva acentos). Lo corre el usuario.
-- Despues: reiniciar el portal o Administracion > Ayuda > Reindexar (si Meilisearch esta activo).
-- La ayuda sigue APAGADA en produccion (portal.ayuda.visible=false) hasta que se apruebe en local.
--
-- ORDEN (22-oct-2026: las tablas de ayuda NO existen todavia en la base del portal, ORA-00942):
--   1. src/main/resources/db/sql/app_domain/ayuda.sql   (tablas + 28 articulos base + menu AYUDA_ADMIN)
--   2. este archivo                                     (nuevos y actualizados)
-- =============================================================================
SET DEFINE OFF;

CREATE OR REPLACE PROCEDURE PR_SERV_MED_AYUDA_UP(
    p_clave VARCHAR2, p_modulo VARCHAR2, p_titulo VARCHAR2, p_pregunta VARCHAR2,
    p_palabras VARCHAR2, p_cuerpo CLOB, p_enlace VARCHAR2, p_orden NUMBER) IS
BEGIN
  MERGE INTO SERV_MED_AYUDA_ARTICULO t USING (SELECT p_clave clave FROM dual) s ON (t.CLAVE = s.clave)
  WHEN MATCHED THEN UPDATE SET MODULO = p_modulo, TITULO = p_titulo, PREGUNTA = p_pregunta, PALABRAS_CLAVE = p_palabras,
       CUERPO = p_cuerpo, ENLACE = p_enlace, ORDEN = p_orden, UPDATED_AT = SYSTIMESTAMP, UPDATED_BY = 'SCRIPT'
  WHEN NOT MATCHED THEN INSERT (CLAVE, MODULO, TITULO, PREGUNTA, PALABRAS_CLAVE, CUERPO, ENLACE, ORDEN, CREATED_BY, UPDATED_BY)
       VALUES (p_clave, p_modulo, p_titulo, p_pregunta, p_palabras, p_cuerpo, p_enlace, p_orden, 'SCRIPT', 'SCRIPT');
END;
/

BEGIN
-- ===== GENERAL: actualizados =====
PR_SERV_MED_AYUDA_UP('buscar-nss', 'GENERAL', 'Buscar a una persona por NSS', '¿Cómo busco a un trabajador? ¿Dónde empiezo?',
 'nss, buscar, busqueda, trabajador, empleado, expediente, ficha, candidato, multipredio, tutorial',
 'Todo empieza en **Búsqueda NSS** (menú lateral). Escribe el NSS de 10 u 11 dígitos y pulsa **Buscar**.

- Si existe, aparece su **ficha** (nombre, puesto, cuenta, edad…) y en el menú lateral se activa **Expediente General** con los módulos que tu rol puede usar.
- Si es un **candidato** (todavía no es empleado), ves una ficha reducida con CURP y estado del proceso.
- Si el NSS tiene **más de un predio o cuenta**, sale un aviso con cada asociación: no es error.
- Si no existe, verás "sin registro".

Puedes pegar el NSS con espacios: se recortan solos. Con **Muéstrame cómo** te lo señalo en la pantalla.', 'nss?tutorial=buscar-nss', 11);

PR_SERV_MED_AYUDA_UP('ayuda-como-usar', 'GENERAL', 'Cómo usar esta ayuda', '¿Cómo pregunto? ¿Qué hago si no encuentra mi duda? ¿Qué es un tutorial guiado?',
 'ayuda, chat, pregunta, buscar ayuda, manual, no encuentro, tutorial, guia, paso a paso, muestrame',
 'Escribe tu duda como la dirías en voz alta ("cómo imprimo el pase de salida", "qué es apto condicionado", "cómo registro mi firma"). No importan acentos ni errores de dedo.

- Te muestro los artículos que mejor coinciden; **Leer** abre el texto completo.
- **Muéstrame cómo** abre un **tutorial guiado**: oscurece la pantalla y te señala el menú o botón real, paso a paso. Espera a que hagas cada acción y **no guarda nada**.
- **Abrir** te lleva a la pantalla.
- Marca **¿Te sirvió?** al final: con eso mejoramos la ayuda. Si no encuentro nada, tu pregunta queda registrada para documentarla.

Nunca escribas aquí datos de un paciente (NSS, CURP, diagnósticos): esta ayuda es sobre **cómo usar el portal**.', 'ayuda', 1);

-- ===== GENERAL: nuevos =====
PR_SERV_MED_AYUDA_UP('formatos-imprimir', 'GENERAL', 'Formatos para imprimir: dónde sale cada uno', '¿Dónde imprimo la carta compromiso, el pase de salida o la cadena de custodia? ¿Qué formatos tiene el portal?',
 'formatos, imprimir, impresion, ft-so, ft-so-25, ft-so-31, ft-so-22, ft-so-14, ft-so-02, ft-so-29, ft-so-23, carta compromiso, pase, custodia, cecosama, encabezado 2026',
 'Todos los formatos salen con el **encabezado 2026**, la **firma del médico responsable** con su cédula y, como segunda firma, **quien elaboró** el documento.

| Formato | Dónde sale (Expediente General ›) |
|---|---|
| Examen médico (FT-SO-04, 13, 16, 17, 18, 19, 20) y consentimiento FT-SO-11 | Examen Medico › Imprimir expediente |
| **FT-SO-25** Carta compromiso | Examen Medico › Resultado del examen (dictamen condicionado o restringido) |
| **FT-SO-31** Dictamen de certificación | Examen Medico › Resultado del examen › Dictamen de certificación |
| **FT-SO-22** Aceptación a CECOSAMA | Antidoping › columna Formato (solo positivas) |
| **FT-SO-14** Cadena de custodia | Antidoping › columna Formato (todas las pruebas) |
| **FT-SO-02** Pase de salida | Consulta Medica, tipo «Pase de Salida» |
| **FT-SO-29** Pase de medicamento | Consulta Medica, casilla «Generar pase de medicamento» |
| **FT-SO-23** Pruebas especiales | Consulta Medica, tipo «Pruebas Especiales» |

Los de Consulta también se reimprimen en **Archivo de Consultas** › detalle de la consulta.', NULL, 15);

PR_SERV_MED_AYUDA_UP('mi-firma-digitalizada', 'GENERAL', 'Registrar mi firma digitalizada', '¿Cómo pongo mi firma en los formatos? ¿Cómo registro mi firma? ¿Cómo la quito?',
 'firma, firma digitalizada, mi firma, registrar firma, elaboro, segunda firma, aviso de privacidad, revocar, dibujar firma, subir firma',
 'Cada persona registra **su propia** firma en **Mi firma digitalizada** (menú de tu foto, arriba a la derecha). No necesita rol especial.

1. Lee el **aviso de privacidad**.
2. Elige **una** opción: **dibujarla** en el recuadro (recomendado) o **subir una imagen** PNG o JPG de tu firma en papel blanco (hasta 1 MB; PDF no).
3. Marca «He leído el aviso y autorizo…» y pulsa **Guardar mi firma**. Queda **Vigente**.

**Dónde sale:** en «Elaboró» de los documentos que **tú captures o guardes** desde ese momento. Los documentos anteriores conservan a su autor.

- Si registras otra firma, **sustituye** a la anterior.
- **Retirar mi permiso** la apaga al instante; vuelve la línea para firmar a mano.
- Si el aviso de privacidad cambia, hay que aceptarlo de nuevo para que tu firma siga saliendo.
- Tu NSS debe estar activo en el biométrico; si no, tu firma no sale.
- Si no registras tu firma, los formatos salen con tu nombre y una línea para firmar a mano.', 'perfil/firma?tutorial=mi-firma', 16);

PR_SERV_MED_AYUDA_UP('firmas-administracion', 'GENERAL', 'Firmas digitalizadas: administración', '¿Cómo cargo la firma escaneada de alguien? ¿Cómo desactivo una firma? ¿Hay que activarla por cada paciente?',
 'firmas, administracion, cargar firma, firma escaneada, pendiente, activa tu firma, desactivar firma, firma del medico, cedula',
 'Solo para el rol **ADMIN**: **Administración › Firmas digitalizadas**.

- **Cargar una firma existente**: escribe el **NSS de quien firma** (el médico o la enfermera, **no** el del paciente) y sube su imagen. Queda **pendiente**: esa persona ve el botón **«Activa tu firma»** y la activa aceptando el aviso. Nadie la deja vigente por otra persona.
- **Desactivar**: apaga una firma vigente o pendiente (por ejemplo, alguien que dejó de trabajar ahí). No se borra: los documentos anteriores la conservan.
- La firma del médico responsable se carga **una sola vez** y sale en los formatos de **todos** los trabajadores.
- Las firmas se guardan **cifradas** y nunca se pueden descargar sueltas. Una revisión diaria apaga las de quien ya no es empleado activo.', 'admin/firmas', 92);

-- ===== CONSULTA (EXPEDIENTE): actualizado y nuevos =====
PR_SERV_MED_AYUDA_UP('consulta-alta', 'EXPEDIENTE', 'Dar de alta una consulta médica', '¿Cómo registro una consulta? ¿Qué campos son obligatorios?',
 'consulta, consulta medica, alta, nueva consulta, registrar, signos vitales, imc, causa, tratamiento, tipo de consulta, pase de salida, pruebas especiales, pase de medicamento',
 'Busca el NSS y abre **Expediente General › Consulta Médica**.

**Qué captura**
- **Tipo de consulta**. Dos tipos abren campos propios: **Pase de Salida** (formato FT-SO-02) y **Pruebas Especiales** (carta FT-SO-23).
- **Causa** del catálogo, peso y talla (el **IMC se calcula solo**), FC, FR, TA, temperatura.
- Motivo, exploración y tratamiento (texto libre).
- **Diagnóstico** del catálogo ICD: **obligatorio** en toda consulta.
- Casilla **Generar pase de medicamento** (FT-SO-29), opcional en cualquier consulta.
- Firma del paciente y PDFs adjuntos (opcional).

Al guardar salen los enlaces para imprimir los formatos que generaste y el formulario se limpia. La consulta aparece en **Archivo de Consultas**, donde también se reimprimen.', 'nss?tutorial=consulta', 30);

PR_SERV_MED_AYUDA_UP('pase-salida', 'EXPEDIENTE', 'Pase de salida (FT-SO-02)', '¿Cómo hago un pase de salida? ¿Qué es obligatorio?',
 'pase de salida, ft-so-02, salida, entrada, regresa, cita programada, envio, incapacidad, checador',
 'En **Consulta Médica** elige el tipo **«Pase de Salida»**: aparece el recuadro del pase con tres grupos.

- **Motivo y cita**: cita programada o envío (atención o seguimiento) y fecha de cita. **El motivo es obligatorio.**
- **Horario**: hora de salida (se llena con la hora actual; **obligatoria**), hora de entrada y si regresa.
- **Incapacidad** (si aplica): días, inicia y termina.
- Observaciones del pase.

Al guardar la consulta con firma se abre el **FT-SO-02** para imprimir. También está en Archivo de Consultas › detalle › «Pase de salida (FT-SO-02)».', 'nss?tutorial=consulta', 32);

PR_SERV_MED_AYUDA_UP('pase-medicamento', 'EXPEDIENTE', 'Pase de medicamento (FT-SO-29)', '¿Cómo hago un pase de medicamento? ¿Puedo poner varios medicamentos?',
 'pase de medicamento, ft-so-29, medicamento, gramaje, duracion, frecuencia, vigencia, permanente',
 'En cualquier **Consulta Médica** marca **«Generar pase de medicamento»**.

- **Varios medicamentos**: uno por renglón (medicamento, gramaje, duración en días y frecuencia). «Agregar otro medicamento» suma renglones. Al menos uno es obligatorio.
- **Vigencia**: fecha hasta la que se autoriza ingresar el medicamento, o marca **Permanente**.
- El padecimiento y la fecha salen del diagnóstico y la fecha de la consulta.

Al guardar aparece el enlace **Pase de medicamento (FT-SO-29)**; también en Archivo de Consultas › detalle.', 'nss?tutorial=consulta', 33);

PR_SERV_MED_AYUDA_UP('pruebas-especiales', 'EXPEDIENTE', 'Pruebas especiales (FT-SO-23)', '¿Cómo registro una prueba rápida de VIH? ¿Dónde está la carta de consentimiento?',
 'pruebas especiales, ft-so-23, vih, prueba rapida, anticuerpos, consentimiento, sospechoso, negativo',
 'En **Consulta Médica** elige el tipo **«Pruebas Especiales»**: aparece la carta de consentimiento.

- **Prueba rápida de anticuerpos contra** (de inicio «VIH»): obligatoria.
- ¿Autoriza? Sí o No, fecha (hoy) y lugar de realización.
- **Resultado**: Negativo, Sospechoso o Pendiente (para escribirlo a mano). Se guarda, se ve en el detalle de la consulta y sale en la carta.

Al guardar aparece el enlace **Pruebas especiales (FT-SO-23)**; también en Archivo de Consultas › detalle.', 'nss?tutorial=consulta', 34);

-- ===== EXAMEN MEDICO: actualizados y nuevos =====
PR_SERV_MED_AYUDA_UP('examen-medico', 'EXAMEN_MEDICO', 'Capturar el examen médico', '¿Cómo lleno el examen médico? ¿Cómo cambio el tipo de examen? ¿Cómo lo imprimo?',
 'examen medico, examen, periodico, acordeon, secciones, guardar examen, tipo de examen, trabajos previos, imprimir, encabezado 2026, elaboro',
 '**Expediente General › Examen Médico** es un acordeón por grupo y, dentro, pestañas por sección.

- **Tipo de examen** arriba: define el formato que se imprime (FT-SO-04, 13, 16, 17, 18, 19 o 20). Si lo cambias, guárdalo ahí mismo.
- **Guardar examen** manda **solo las secciones que abriste**; *Abrir todo* carga todas.
- **Trabajos previos** en tarjetas, paso a paso, con los datos que ya existían precargados.
- **Imprimir expediente** abre el examen con el **encabezado 2026** y el consentimiento **FT-SO-11** (con «Aptitud laboral»).

Quien **guarda** el examen sale en **«Elaboró»** de los impresos, con su firma si la registró. Desde el Resultado del examen salen la **carta compromiso (FT-SO-25)** y el **dictamen de certificación (FT-SO-31)**.', 'nss?tutorial=examen-formatos', 50);

PR_SERV_MED_AYUDA_UP('dictamen-significado', 'EXAMEN_MEDICO', 'Qué significa cada dictamen', '¿Cuál es la diferencia entre apto condicionado y apto restringido?',
 'dictamen, apto, no apto, apto condicionado, apto restringido, apto inclusion, resultado del examen, observaciones',
 '| Dictamen | Significado |
|---|---|
| **Apto** | Sin hallazgos. |
| **Apto condicionado** | Puede trabajar; tiene una enfermedad **controlable** (obesidad, hipertensión, diabetes…) con seguimiento y **carta compromiso (FT-SO-25)**. |
| **Apto restringido** | Puede trabajar con **restricciones permanentes**; se capturan en Restricciones médicas. En OCR se llama *Apto inclusión*. |
| **No apto** | No apto para ingreso. Un nuevo examen inicial puede cambiarlo. |

En **Observaciones** va la condición a cumplir. Si cambias de condicionado o restringido a otro dictamen, el portal pregunta si borra esas observaciones.', NULL, 52);

PR_SERV_MED_AYUDA_UP('carta-compromiso', 'EXAMEN_MEDICO', 'Carta compromiso (FT-SO-25)', '¿Cómo imprimo la carta compromiso? ¿Por qué el botón está deshabilitado?',
 'carta compromiso, ft-so-25, condicionado, restringido, condicion a cumplir, deshabilitado, boton',
 'Sale solo con dictamen **Apto condicionado** o **Apto restringido**.

1. En **Examen Médico › Resultado del examen** elige el dictamen y escribe en **Observaciones** la condición a cumplir.
2. **Guarda el examen.** El botón **Imprimir carta compromiso (FT-SO-25)** se activa hasta que el dictamen está guardado (por eso puede verse deshabilitado).
3. Si el dictamen guardado no aplica, el motivo sale junto al botón.

La carta marca el tipo de examen y su fecha, y lleva la firma del médico responsable.', 'nss?tutorial=examen-formatos', 53);

PR_SERV_MED_AYUDA_UP('dictamen-certificacion', 'EXAMEN_MEDICO', 'Dictamen de certificación de aptitud (FT-SO-31)', '¿Cómo hago el dictamen de certificación? ¿Dónde veo los anteriores?',
 'dictamen de certificacion, ft-so-31, aptitud, tipos de trabajo, alturas, espacios confinados, vigencia, historial, centro de trabajo, cliente',
 'Solo si el cliente lo pide. En **Examen Médico › Resultado del examen** pulsa **Dictamen de certificación (FT-SO-31)**.

- Marca la aptitud de los **10 tipos de trabajo** (Apto, No apto, Apto condicionado con especificación, No aplica). Todos son obligatorios.
- **Centro de trabajo**: CEDIS / cuenta (se prellena y se puede editar).
- **Expedir nuevo e imprimir**: se expide con fecha de hoy y **vigencia de 1 año**.
- Cada expedición queda en el **historial** («Dictámenes expedidos»); las anteriores no se modifican y se pueden volver a imprimir.', 'nss?tutorial=examen-formatos', 54);

-- ===== ANTIDOPING: actualizado y nuevo =====
PR_SERV_MED_AYUDA_UP('antidoping', 'ANTIDOPING', 'Registrar antidoping o alcoholemia', '¿Cómo registro una prueba de antidoping? ¿Y la alcoholemia?',
 'antidoping, alcoholemia, prueba, sustancia, thc, coc, amf, positivo, negativo, resultado, cadena de custodia, cecosama',
 '**Expediente General › Antidoping** lista las pruebas del NSS; **Nuevo registro** captura:

- **Tipo**: Antidoping o Alcoholemia. **Sustancia** (solo antidoping).
- **Resultado**: Negativo o Positivo (los positivos se resaltan en rojo) y conclusión.
- **Cadena de custodia (FT-SO-14)**: identificación, razón, elementos a detectar, % de alcohol, tratamiento, hora, lugar y responsable.

En la lista, la columna **Formato** trae el **FT-SO-14** en todas las pruebas y el **FT-SO-22** (aceptación a CECOSAMA) solo en las positivas.

Para elegir a quién le toca la prueba usa **Exámenes › Antidoping - Selección**.', 'nss?tutorial=antidoping', 71);

PR_SERV_MED_AYUDA_UP('cadena-custodia', 'ANTIDOPING', 'Cadena de custodia (FT-SO-14) y CECOSAMA (FT-SO-22)', '¿Cómo lleno la cadena de custodia? ¿Dónde imprimo la aceptación a CECOSAMA?',
 'cadena de custodia, ft-so-14, ft-so-22, cecosama, identificacion, elementos a detectar, bac, alcohol, responsable de la toma, lugar, positivo',
 'Al registrar la prueba en **Antidoping › Nuevo registro**, la sección **Cadena de custodia (FT-SO-14)** pide:

- Identificación (tipo, emitida por, número) y **razón** (aleatoria, periódico u otro).
- **Elementos a detectar**: las 7 casillas vienen marcadas; desmarca las que tu kit no detecta. En alcoholemia se captura el **% de alcohol**.
- Tratamiento médico, **hora** (se llena sola), **lugar** (recuerda el último) y **responsable** (tu nombre).

Lo que quede vacío sale en blanco para llenarse a mano. En la lista, columna **Formato**: **FT-SO-14** en todas; **FT-SO-22** (aceptación a CECOSAMA) solo en positivas.', 'nss?tutorial=antidoping', 72);

COMMIT;
END;
/

DROP PROCEDURE PR_SERV_MED_AYUDA_UP;

-- Verificacion: articulos de este script
SELECT CLAVE, MODULO, TITULO, ENLACE, ACTIVO, UPDATED_AT
  FROM SERV_MED_AYUDA_ARTICULO
 WHERE CLAVE IN ('buscar-nss', 'ayuda-como-usar', 'formatos-imprimir', 'mi-firma-digitalizada', 'firmas-administracion',
                 'consulta-alta', 'pase-salida', 'pase-medicamento', 'pruebas-especiales', 'examen-medico',
                 'dictamen-significado', 'carta-compromiso', 'dictamen-certificacion', 'antidoping', 'cadena-custodia')
 ORDER BY ORDEN;
