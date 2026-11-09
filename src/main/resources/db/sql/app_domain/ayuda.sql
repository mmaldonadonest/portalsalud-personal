-- =============================================================================
-- Centro de ayuda / chat de ayuda (sin IA). BASE DEL PORTAL (SERV_MED_* dentro de BIOMETRICO), NO ORDS.
--
-- Contenido de referencia en Oracle (respaldo, permisos, admin desde el portal); el buscador
-- (Meilisearch en Docker, docker/ayuda/) es un indice DERIVADO que se reconstruye desde aqui.
-- Si Meilisearch no esta, el portal busca con LIKE sobre estas tablas.
--
--   SERV_MED_AYUDA_ARTICULO  un articulo por pregunta real del usuario, en Markdown.
--                            MODULO = CODE de SERV_MED_MENU (filtra por lo que el usuario ve) o GENERAL.
--   SERV_MED_AYUDA_PREGUNTA  lo que preguntan, cuantos resultados hubo y si les sirvio: de ahi
--                            sale que le falta a la ayuda.
-- Idempotente: las tablas se crean si no existen y los articulos se MERGE por CLAVE (re-correr
-- actualiza el contenido sin duplicar ni tocar lo que edito el admin... OJO: el MERGE SI pisa
-- el cuerpo; si el servicio medico ya edito un articulo en el portal, no re-correr la parte 2).
-- =============================================================================
SET SERVEROUTPUT ON;
SET DEFINE OFF;

-- ---------------------------------------------------------------------------
-- 1. Tablas
-- ---------------------------------------------------------------------------
BEGIN
  EXECUTE IMMEDIATE '
  CREATE TABLE SERV_MED_AYUDA_ARTICULO (
    ID             NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    CLAVE          VARCHAR2(60)   NOT NULL,
    MODULO         VARCHAR2(40)   DEFAULT ''GENERAL'' NOT NULL,
    TITULO         VARCHAR2(200)  NOT NULL,
    PREGUNTA       VARCHAR2(400),
    PALABRAS_CLAVE VARCHAR2(1000),
    CUERPO         CLOB           NOT NULL,
    ENLACE         VARCHAR2(300),
    ORDEN          NUMBER(5)      DEFAULT 100 NOT NULL,
    ACTIVO         CHAR(1)        DEFAULT ''S'' NOT NULL,
    CREATED_AT     TIMESTAMP(6)   DEFAULT SYSTIMESTAMP NOT NULL,
    CREATED_BY     VARCHAR2(100),
    UPDATED_AT     TIMESTAMP(6)   DEFAULT SYSTIMESTAMP NOT NULL,
    UPDATED_BY     VARCHAR2(100),
    CONSTRAINT UK_SERV_MED_AYUDA_ART_CLAVE UNIQUE (CLAVE),
    CONSTRAINT CK_SERV_MED_AYUDA_ART_ACTIVO CHECK (ACTIVO IN (''S'',''N''))
  )';
  DBMS_OUTPUT.PUT_LINE('SERV_MED_AYUDA_ARTICULO creada');
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; ELSE DBMS_OUTPUT.PUT_LINE('SERV_MED_AYUDA_ARTICULO ya existia'); END IF; END;
/

BEGIN
  EXECUTE IMMEDIATE '
  CREATE TABLE SERV_MED_AYUDA_PREGUNTA (
    ID           NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    USUARIO      VARCHAR2(100),
    PREGUNTA     VARCHAR2(500)  NOT NULL,
    RESULTADOS   NUMBER(5)      DEFAULT 0 NOT NULL,
    MOTOR        VARCHAR2(10),
    ARTICULO_ID  NUMBER,
    UTIL         CHAR(1),
    CREATED_AT   TIMESTAMP(6)   DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT CK_SERV_MED_AYUDA_PREG_UTIL CHECK (UTIL IS NULL OR UTIL IN (''S'',''N''))
  )';
  EXECUTE IMMEDIATE 'CREATE INDEX IX_SERV_MED_AYUDA_PREG_FECHA ON SERV_MED_AYUDA_PREGUNTA (CREATED_AT)';
  DBMS_OUTPUT.PUT_LINE('SERV_MED_AYUDA_PREGUNTA creada');
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; ELSE DBMS_OUTPUT.PUT_LINE('SERV_MED_AYUDA_PREGUNTA ya existia'); END IF; END;
/

-- Menu de administracion (solo rol ADMIN lo ve; la ruta /admin/** la protege SecurityConfiguration)
MERGE INTO SERV_MED_MENU t USING (SELECT 'AYUDA_ADMIN' code, 'Ayuda (artículos)' title, 'ri-question-answer-line' icon, 95 order_no FROM dual) s
  ON (t.CODE = s.code) WHEN NOT MATCHED THEN INSERT (CODE, TITLE, ICON, ORDER_NO) VALUES (s.code, s.title, s.icon, s.order_no);
COMMIT;

-- ---------------------------------------------------------------------------
-- 2. Articulos iniciales (uno por pregunta real). Markdown sencillo: #, **, listas, [texto](ruta).
--    Las rutas son relativas al portal (sin /portal-salud): el front antepone contextPath.
-- ---------------------------------------------------------------------------
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
-- ===== GENERAL =====
PR_SERV_MED_AYUDA_UP('entrar-portal', 'GENERAL', 'Cómo entrar al portal', '¿Cómo inicio sesión? ¿Con qué usuario entro?',
 'login, acceso, contraseña, usuario, entrar, iniciar sesion, launcher',
 'Entra con tu **NSS** y tu **contraseña** del portal (la misma que usabas en el sistema anterior). Si llegas desde el **launcher** de Onest, entras directo sin contraseña.

**Lo que ves después de entrar** (qué módulos aparecen en tu menú) depende del **rol** que tengas asignado.

**Si no puedes entrar**
- Revisa que el NSS no lleve espacios y que la contraseña sea la del portal, no la de tu correo.
- Si entras pero "Expediente General" sale vacío, tu usuario todavía **no tiene rol**: pídelo a sistemas (Administración → Usuarios).
- La sesión dura media jornada; si te sale "sesión expirada", vuelve a entrar y repite lo último que hacías.', 'login', 10);

PR_SERV_MED_AYUDA_UP('buscar-nss', 'GENERAL', 'Buscar a una persona por NSS', '¿Cómo busco a un trabajador? ¿Dónde empiezo?',
 'nss, buscar, busqueda, trabajador, empleado, expediente, ficha, candidato, multipredio',
 'Todo empieza en **Búsqueda NSS** (menú lateral). Escribe el NSS de 10 u 11 dígitos y busca.

- Si existe, aparece su **ficha** (nombre, puesto, cuenta, edad…) y en el menú lateral se activa **Expediente General** con los módulos que tu rol puede usar.
- Si es un **candidato** (todavía no es empleado), ves una ficha reducida con CURP y estado del proceso. Si además OCR mandó una solicitud, ves el aviso de **Examen médico inicial pendiente**.
- Si el NSS tiene **más de un predio/cuenta**, sale un aviso listando cada asociación: no es error, es una persona con varias relaciones laborales.
- Si no existe, verás "sin registro".

Puedes pegar el NSS con espacios: se recortan solos. Para abrir directo a una persona usa `nss?nss=<NSS>`.', 'nss', 11);

PR_SERV_MED_AYUDA_UP('menu-por-rol', 'GENERAL', 'No veo un módulo en mi menú', '¿Por qué no me aparece un módulo? ¿Cómo pido acceso?',
 'rol, permisos, menu, modulo, no aparece, acceso, 403',
 'Los módulos del menú (y los de Expediente General) se asignan **por rol** en Administración → Roles → Menús. Si no ves un módulo:

1. Confirma con sistemas qué rol tienes (Administración → Usuarios).
2. Pide que agreguen el módulo a tu rol. El cambio se ve al volver a entrar.

Si escribes la ruta a mano y te responde **403**, es el mismo motivo: tu rol no tiene ese módulo.', NULL, 12);

PR_SERV_MED_AYUDA_UP('campanita', 'GENERAL', 'La campanita de notificaciones', '¿Qué es la campana del encabezado? ¿Cómo activo los avisos de escritorio?',
 'campanita, notificacion, notificaciones, aviso, escritorio, pendiente, alerta',
 'La **campana** del encabezado avisa cuando hay algo que atender, por ejemplo un **examen médico inicial pendiente** que mandó OCR. Se actualiza sola cada 45 segundos.

- Clic en un aviso: te lleva directo a la pantalla (por ejemplo al examen inicial de esa persona) y lo marca como leído.
- **Avisos de escritorio**: dentro de la campana, "Activar avisos de escritorio" y acepta el permiso del navegador. Así te enteras aunque tengas otra ventana al frente.
- "Marcar todas como leídas" limpia el contador.', NULL, 13);

PR_SERV_MED_AYUDA_UP('firma-paciente', 'GENERAL', 'Firma del paciente en pantalla', '¿Cómo capturo la firma? ¿Se puede borrar?',
 'firma, firmar, canvas, paciente, trabajador, dibujar, borrar firma',
 'En los formularios con firma (consulta, incapacidad, examen, pre-test) el trabajador firma **con el dedo o el mouse** en el recuadro.

- **Limpiar** borra el trazo para volver a firmar.
- La firma se guarda junto con el registro y sale en el documento impreso.
- En el examen médico, si ya había una firma guardada se muestra; firmar de nuevo la reemplaza.', NULL, 14);

PR_SERV_MED_AYUDA_UP('diagnostico-icd', 'GENERAL', 'Capturar el diagnóstico (catálogo ICD)', '¿Cómo pongo el diagnóstico? No me deja escribirlo.',
 'icd, cie, diagnostico, catalogo, clave, buscar diagnostico',
 'El diagnóstico **no es texto libre**: se elige del catálogo ICD/CIE.

1. Escribe al menos **3 letras** del padecimiento o la clave en el buscador (por ejemplo "fiebre" o "A96").
2. Elige el renglón correcto de la lista; se agrega al cuadro de diagnóstico con su clave.
3. Puedes agregar más de uno; la misma clave no se repite.
4. "Ver todo" abre el catálogo completo para recorrerlo.

Es obligatorio en Consulta médica y al finalizar el Examen médico inicial.', NULL, 15);

-- ===== ARCHIVOS =====
PR_SERV_MED_AYUDA_UP('archivos-pdf', 'EXAMEN_MEDICO', 'Subir y descargar PDFs (E. Laboratorio, Histórico E.M, Nota Médica, Nota Incapacidad)', '¿Dónde guardo un PDF del trabajador? ¿Cómo lo descargo o lo borro?',
 'pdf, archivo, subir, descargar, borrar, laboratorio, historico, nota medica, nota incapacidad, adjunto, documento',
 'Los cuatro módulos funcionan igual; solo cambia la **categoría** donde queda el archivo:

- **E. Laboratorio**: estudios de laboratorio.
- **Histórico E.M**: exámenes médicos anteriores.
- **Nota Médica** y **Nota Incapacidad**: notas escaneadas.

**Subir**: elige uno o varios PDF y da clic en *Subir*; verás el resultado junto al botón y el archivo en la lista.
**Descargar / borrar**: cada renglón tiene su botón. Borrar pide confirmación y no se puede deshacer.

Solo se aceptan **PDF** de hasta 25 MB. Los archivos del sistema anterior ya están migrados y se ven aquí.', NULL, 20);

-- ===== CONSULTAS =====
PR_SERV_MED_AYUDA_UP('consulta-alta', 'EXPEDIENTE', 'Dar de alta una consulta médica', '¿Cómo registro una consulta? ¿Qué campos son obligatorios?',
 'consulta, consulta medica, alta, nueva consulta, registrar, signos vitales, imc, causa, tratamiento',
 'Busca el NSS y abre **Expediente General → Consulta Médica**.

**Qué captura**
- Tipo de consulta y, si aplica, área anatómica del accidente.
- **Causa**: catálogo (lo administra el servicio médico en *Causas de consulta*).
- Peso y talla: el **IMC se calcula solo**. FC, FR, TA, temperatura.
- Motivo, exploración y tratamiento (texto libre).
- **Diagnóstico** del catálogo ICD (obligatorio, ver "Capturar el diagnóstico").
- Firma del paciente y PDFs adjuntos (opcional).

Al guardar, el formulario se limpia (incluida la firma) para capturar la siguiente. La consulta aparece en **Archivo de Consultas**.', NULL, 30);

PR_SERV_MED_AYUDA_UP('consulta-historial', 'EXPEDIENTE', 'Ver el historial de consultas', '¿Dónde veo las consultas anteriores? ¿Cómo exporto a Excel?',
 'archivo de consultas, historial, consultas anteriores, exportar, excel, accidente, ver detalle',
 '**Expediente General → Archivo de Consultas** muestra todas las consultas del NSS.

- Buscador de texto arriba de la tabla y botón **Exportar a Excel**.
- Las consultas de **accidente/emergencia** se resaltan en amarillo con la etiqueta *Accidente*.
- El botón **Ver** abre el detalle completo, con la firma y los adjuntos si los tiene.', NULL, 31);

-- ===== INCAPACIDADES =====
PR_SERV_MED_AYUDA_UP('incapacidad-alta', 'INCAPACIDAD', 'Dar de alta una incapacidad', '¿Cómo registro una incapacidad del IMSS o interna? ¿Cómo se calcula el costo?',
 'incapacidad, incapacidades, imss, interna, st2, st-2, ramo, rubro, inicial, subsecuente, dias, salario, costo, goce de sueldo',
 'Busca el NSS y abre **Expediente General → Incapacidades** (esta es el alta; la lista está en *Archivo Incapacidades*).

- **Rubro**: Interna / IMSS. **Ramo**: Enfermedad General, Profesional, RT1, RT2, Maternidad.
- **Tipo**: Inicial o Subsecuente (la subsecuente lleva fecha de inicio subsecuente).
- Fechas: primera, inicio subsecuente, término, alta.
- **Salario diario × días autorizados = costo**, se calcula solo.
- ST2, estado del dictamen, alta, goce de sueldo.
- Firma del paciente y PDFs adjuntos.

El término no puede ser anterior al inicio. Al guardar aparece en Archivo Incapacidades.', NULL, 40);

PR_SERV_MED_AYUDA_UP('incapacidad-lista', 'INCAPACIDAD', 'Consultar incapacidades y reporte por fechas', '¿Dónde veo las incapacidades? ¿Cómo saco el reporte por rango de fechas?',
 'archivo incapacidades, lista, filtro, ramo, rubro, reporte, rango de fechas, exportar, excel',
 '**Expediente General → Archivo Incapacidades** lista todas las del NSS, con filtros por **Ramo** y **Rubro** y buscador de texto. *Exportar a Excel* respeta los filtros.

El **reporte por rango de fechas** (todas las personas) está en el mismo módulo: elige fecha inicial y final y exporta.', NULL, 41);

-- ===== EXAMEN MEDICO =====
PR_SERV_MED_AYUDA_UP('examen-medico', 'EXAMEN_MEDICO', 'Capturar el examen médico (periódico)', '¿Cómo lleno el examen médico? ¿Por qué se guardan solo algunas secciones?',
 'examen medico, examen, periodico, acordeon, secciones, guardar examen, heredofamiliares, exploracion, laboratorio, dictamen',
 '**Expediente General → Examen Médico** es el módulo más grande: un acordeón por grupo y, dentro, pestañas por sección. Cada pestaña se carga la primera vez que la abres y **no se pierde** al cambiar de pestaña.

**Guardar examen** manda **solo las secciones que abriste** en esta sesión: así no se sobrescriben con vacío las que no tocaste. *Abrir todo* carga todas las secciones de una vez.

Secciones con su propio botón de guardar: antecedentes laborales, contactos de emergencia (hasta 3), diagnósticos secundarios (hasta 3, con buscador ICD) y restricciones médicas.

Al final: firma del paciente. *Imprimir* abre el expediente completo en otra pestaña.', NULL, 50);

PR_SERV_MED_AYUDA_UP('restricciones-medicas', 'EXAMEN_MEDICO', 'Restricciones médicas (RES-01 a RES-16)', '¿Cómo registro una restricción? ¿Qué significa la franja ámbar?',
 'restriccion, restricciones, res-01, res-16, alturas, cargar peso, limite, vigencia, ambar',
 'Dentro del Examen Médico, sección **Restricciones médicas**: catálogo fijo **RES-01 a RES-16** (no realizar trabajo en alturas, no cargar más de X kg, etc.).

- Marca la restricción, define valor límite, unidad, vigencia y médico responsable, y guarda la sección.
- Las **activas** se muestran con una **franja ámbar** en la ficha para que salten a la vista.
- Las restricciones se registran, no se borran: una nueva vigencia deja constancia de la anterior.
- Cuando el dictamen es **Apto restringido**, estas restricciones viajan a OCR en el aviso del examen inicial.', NULL, 51);

PR_SERV_MED_AYUDA_UP('dictamen-significado', 'EXAMEN_MEDICO', 'Qué significa cada dictamen', '¿Cuál es la diferencia entre apto condicionado y apto restringido?',
 'dictamen, apto, no apto, apto condicionado, apto restringido, apto inclusion, resultado del examen',
 '| Dictamen | Significado |
|---|---|
| **Apto** | Sin hallazgos. |
| **Apto condicionado** | Puede trabajar; tiene una enfermedad **controlable** (obesidad, hipertensión, diabetes…) con seguimiento y carta compromiso. |
| **Apto restringido** | Puede trabajar con **restricciones permanentes** (padecimiento incurable o limitación física); se capturan en Restricciones médicas. En OCR se llama *Apto inclusión*. |
| **No apto** | No apto para ingreso. Un nuevo examen inicial puede cambiarlo. |', NULL, 52);

-- ===== EXAMEN INICIAL =====
PR_SERV_MED_AYUDA_UP('examen-inicial-que-es', 'EXAMEN_INICIAL', 'Qué es el examen médico inicial y cuándo se usa', '¿Cuál es la diferencia entre examen inicial y examen médico?',
 'examen inicial, inicial, nuevo ingreso, ingreso, candidato, ft-so-04, admision',
 'El **Examen médico inicial** (formato FT-SO-04) es el de **nuevo ingreso**: se aplica a candidatos antes de contratarlos, normalmente a partir de una solicitud que manda **OCR**.

- Se captura en una pantalla propia, por hojas, con **borrador que se guarda solo**.
- Al finalizar se guarda como examen tipo **Admisión** en el mismo expediente que el examen médico, con su dictamen, y se avisa a OCR.
- Los siguientes exámenes de esa persona (ya empleada) van en **Examen Médico**, no aquí.

Se llega desde la campanita, desde el aviso en la ficha del NSS, o desde **Exámenes → Exámenes iniciales** (bandeja).', 'examen-inicial/bandeja', 60);

PR_SERV_MED_AYUDA_UP('examen-inicial-captura', 'EXAMEN_INICIAL', 'Capturar el examen inicial más rápido', '¿Cómo funcionan "Todo normal", los Sí/No y el odontograma?',
 'todo normal, si no, switch, borrador, autoguardado, odontograma, dientes, exploracion, imc, hojas, stepper',
 'El examen inicial tiene **10 hojas** (ficha, laborales, heredofamiliares, no patológicos, patológicos, padecimiento, exploración, columna y piel, boca y dientes, cierre).

- **Se guarda solo** al cambiar de hoja y mientras capturas; arriba ves la hora del último guardado. Puedes cerrar y seguir después.
- **Sí / No** con botones. El switch **Todo normal** del bloque pone *No* en todo y limpia las observaciones; marca *Sí* solo donde haya hallazgo.
- **Exploración**: peso y talla calculan el IMC; "Todo normal" llena con *Normal* lo que esté vacío.
- **Dientes**: elige el hallazgo (caries, amalgama, endodoncia, prótesis, ausente) y da clic en los dientes; clic de nuevo lo quita. Sano = sin marca.
- **Cierre**: diagnóstico ICD, dictamen con 4 botones y firma.', NULL, 61);

PR_SERV_MED_AYUDA_UP('examen-inicial-finalizar', 'EXAMEN_INICIAL', 'Finalizar el examen inicial', '¿Qué pasa al finalizar? ¿Por qué no me deja finalizar?',
 'finalizar, dictamen, bloqueado, candado, no se puede finalizar, lo esta atendiendo, tomar examen, expediente, imprimir',
 'Al **Finalizar** el examen completo se guarda como *Admisión*, se abre el expediente impreso en otra pestaña y, si vino de OCR, se le avisa el dictamen.

Antes de finalizar necesitas: **dictamen** elegido, **diagnóstico ICD** y, de preferencia, firma.

**Si no te deja finalizar**
- *"Examen inicial bloqueado"*: la persona ya es empleada y tiene examen vigente; sus siguientes exámenes van en Examen Médico. Solo se vuelve a aplicar un inicial si el último fue **No apto**.
- *"Lo está atendiendo <usuario>"*: otro médico abrió primero ese examen. Puedes revisarlo, pero para finalizarlo usa **Tomar examen** (con eso queda a tu nombre).', NULL, 62);

PR_SERV_MED_AYUDA_UP('examen-inicial-bandeja', 'EXAMEN_INICIAL_BANDEJA', 'Bandeja de exámenes iniciales', '¿Dónde veo los exámenes iniciales pendientes? ¿Quién está atendiendo cada uno?',
 'bandeja, pendientes, en captura, finalizados, atiende, tomar, filtros, fechas, examenes iniciales',
 '**Exámenes → Exámenes iniciales** lista todas las solicitudes (hoy llegan de OCR) con filtros por estatus, NSS, nombre y fechas.

- Contadores de **pendientes** y **en captura** arriba.
- Columna **Atiende**: quién abrió el examen y desde cuándo (el tuyo en azul).
- **Abrir examen** te lleva directo a la captura; **Tomar** te asigna uno que tenía otro usuario; **Documento** abre el expediente de los finalizados.
- Las solicitudes caducan solas a los 30 días de la cita si nadie las atiende.', 'examen-inicial/bandeja', 63);

PR_SERV_MED_AYUDA_UP('examen-inicial-ocr', 'EXAMEN_INICIAL', 'Cómo llegan las solicitudes de OCR y qué recibe OCR', '¿Qué manda OCR? ¿Cómo se entera OCR del resultado?',
 'ocr, solicitud, webhook, integracion, candidato, aviso, dictamen, reenviar',
 'OCR manda al portal una **solicitud** con los datos del candidato (NSS, nombre, CURP, puesto, cuenta, cita). El portal:

1. Crea el aviso en la **campanita** y el banner en la ficha del NSS.
2. Muestra la ficha del candidato con los datos de la solicitud (aunque no esté en el biométrico).
3. Al **finalizar** el examen, manda a OCR el dictamen (Apto / Apto condicionado / Apto restringido = *Apto inclusión* / No apto) con las restricciones activas. Si OCR no responde, reintenta solo.

Sistemas puede ver cada intento y reenviar en **Administración → Integración OCR**.', NULL, 64);

-- ===== PRE-TEST / ANTIDOPING / ACCIDENTES / MATERNIDAD =====
PR_SERV_MED_AYUDA_UP('pretest', 'PRETEST', 'Pre-Test (cuestionario de ingreso)', '¿Qué es el Pre-Test y cómo se llena?',
 'pretest, pre-test, cuestionario, ingreso, checklist, sintomas, domicilio, contactos',
 '**Expediente General → Pre-Test**: datos personales, domicilio, contactos de emergencia, cuestionario de salud (Sí/No con observación) y síntomas. Termina con la firma del paciente y se guarda completo con *Guardar*.', NULL, 70);

PR_SERV_MED_AYUDA_UP('antidoping', 'ANTIDOPING', 'Registrar antidoping o alcoholemia', '¿Cómo registro una prueba de antidoping? ¿Y la alcoholemia?',
 'antidoping, alcoholemia, prueba, sustancia, thc, coc, amf, positivo, negativo, resultado',
 '**Expediente General → Antidoping** lista las pruebas del NSS; *Nuevo registro* captura:

- **Tipo**: Antidoping o Alcoholemia.
- **Sustancia** (solo Antidoping): AMF, THC, COC, etc.
- **Resultado**: Negativo / Positivo (los positivos se resaltan en rojo) y conclusión.

Para elegir a quién le toca la prueba usa **Exámenes → Antidoping - Selección** (selección aleatoria a partir de una lista pegada).', NULL, 71);

PR_SERV_MED_AYUDA_UP('antidoping-seleccion', 'ANTIDOPING_SELECCION', 'Selección aleatoria para antidoping', '¿Cómo elijo al azar a quién se le aplica antidoping?',
 'seleccion aleatoria, sorteo, antidoping, lista, pegar, historial, consumibles',
 '**Exámenes → Antidoping - Selección**: pega la lista del personal (una persona por renglón), indica cuántos se eligen y genera la selección. Cada selección queda en el **historial**; *Limpiar* reinicia el flujo.

Los **Consumibles** de antidoping (tiras, kits) se llevan en **Exámenes → Consumibles**.', 'antidoping-seleccion', 72);

PR_SERV_MED_AYUDA_UP('accidentes', 'ACCIDENTES', 'Accidentes de trabajo y su seguimiento', '¿Cómo registro un accidente? ¿Cómo lo doy seguimiento o lo cierro?',
 'accidente, accidentes, riesgo de trabajo, trayecto, laboral, causa rt, sdi, calificacion, seguimiento, evidencia, abierto, cerrado',
 '**Expediente General → Accidentes** lista los casos del NSS; *Nuevo registro* captura tipo de riesgo (laboral / trayecto), causa RT del catálogo, SDI/costo y estatus de calificación.

Cada caso tiene un **badge Abierto/Cerrado** y un botón de **seguimiento**: ahí agregas avances y evidencias (PDF) después del alta. El estado se calcula a partir del seguimiento; no se edita a mano.', NULL, 73);

PR_SERV_MED_AYUDA_UP('maternidad', 'MATERNIDAD', 'Seguimiento de maternidad', '¿Cómo registro un chequeo de embarazo? ¿Por qué el botón está deshabilitado?',
 'maternidad, embarazo, chequeo, semanas de gestacion, fecha probable de parto, revision, mujer, deshabilitado',
 '**Expediente General → Maternidad** es el historial de chequeos de embarazo; *Nuevo chequeo* captura semanas de gestación, fecha probable de parto, próxima revisión, restricciones laborales y estatus. Ningún campo es obligatorio: un chequeo temprano puede no tener fechas aún.

Solo está disponible para **NSS de mujer**; si el NSS corresponde a un hombre el botón sale deshabilitado con un aviso.', NULL, 74);

-- ===== EXAMENES (standalone) / DASHBOARD =====
PR_SERV_MED_AYUDA_UP('causas-consulta', 'CAUSAS_CONSULTA', 'Catálogo de causas de consulta', '¿Cómo agrego o desactivo una causa de consulta?',
 'causas, causa de consulta, catalogo, activar, desactivar, otro',
 '**Exámenes → Causas de consulta** administra el catálogo que se usa en Consulta médica (23 causas + *Otro*). Puedes **activar/desactivar** causas; las inactivas dejan de aparecer en el formulario pero no se pierden de las consultas ya guardadas.', 'causas-consulta', 80);

PR_SERV_MED_AYUDA_UP('consumibles', 'CONSUMIBLES', 'Consumibles de antidoping', '¿Dónde llevo el inventario de kits de antidoping?',
 'consumibles, inventario, kits, tiras, entradas, salidas, existencia',
 '**Exámenes → Consumibles**: entradas y salidas de los consumibles de antidoping con existencia actual. Cada movimiento queda registrado con fecha y usuario; el resumen alimenta el Dashboard.', 'consumibles', 81);

-- 2-nov-2026: tambien en docs/script-prod/carga-excel-pantalla-01-menu-y-ayuda.sql (para no re-sembrar todo)
PR_SERV_MED_AYUDA_UP('carga-excel', 'CARGA_EXCEL', 'Cargar un Excel de Servicio Médico', '¿Cómo cargo un Excel de atenciones, exámenes, incapacidades u otro layout?',
 'excel, carga, cargar, layout, atenciones, examen, incapacidades, accidentes, antidoping, maternidad, consumibles, errores, nss, correcciones',
 '**Carga de datos → Carga de Excel** recibe los 10 layouts por NSS (Atenciones, exámenes, incapacidades, accidentes, antidoping, maternidad y consumibles). El layout se reconoce por los encabezados: el orden de las columnas no importa y las columnas de más se ignoran (la pantalla las avisa).

1. Sube el **.xlsx** y, si hace falta, el Excel de **correcciones de NSS**.
2. **Validar** revisa catálogos y NSS contra el biométrico **sin escribir nada**. Con archivos grandes tarda un par de minutos.
3. Revisa el resultado: cuántos renglones entran, el **Excel de errores** (renglón, columna y motivo) y lo que se **sustituye**: lo que llega de un predio para un mes reemplaza lo ya cargado de ese predio y mes.
4. **Cargar en la base** escribe los renglones válidos. Queda en el historial con tu usuario y una copia del archivo original.

Si el mismo archivo ya se había cargado, no se vuelve a cargar; «Cargar de nuevo» lo fuerza.', 'carga-excel', 82);

PR_SERV_MED_AYUDA_UP('dashboard', 'GENERAL', 'Dashboard de inicio', '¿Qué muestran las gráficas del inicio?',
 'dashboard, inicio, kpi, graficas, indicadores, consultas por mes, genero, edad',
 'La pantalla de inicio muestra indicadores del servicio médico: consultas por periodo, desglose por género, edad y cuenta, consumibles y, para el rol de análisis, el **Análisis Ejecutivo** con morbilidad y exámenes. Los datos salen de lo que se captura en el portal; no hay nada que configurar.', 'home', 82);

-- ===== ADMINISTRACION =====
PR_SERV_MED_AYUDA_UP('admin-roles-menus', 'GENERAL', 'Administrar roles, menús y usuarios', '¿Cómo creo un rol? ¿Cómo le asigno módulos? ¿Cómo doy de alta un usuario?',
 'administracion, roles, menus por rol, usuarios, asignar, alta de usuario, permisos, admin',
 'Solo para el rol **ADMIN** (menú *Administración*):

- **Roles**: crea el rol y, con *Asignar menús*, marca qué módulos ve (Expediente General, Exámenes, bandeja, etc.).
- **Usuarios (rol)**: busca al usuario por NSS y asígnale su rol. Sin rol, entra pero no ve módulos.
- **Integración OCR**: solicitudes, intentos del aviso a OCR y *Reenviar*.
- **Ayuda (artículos)**: este mismo contenido; se puede editar y agregar preguntas nuevas.

Los cambios de rol se ven cuando el usuario vuelve a entrar.', 'admin/roles', 90);

PR_SERV_MED_AYUDA_UP('ayuda-como-usar', 'GENERAL', 'Cómo usar esta ayuda', '¿Cómo pregunto? ¿Qué hago si no encuentra mi duda?',
 'ayuda, chat, pregunta, buscar ayuda, manual, no encuentro',
 'Escribe tu duda como la dirías en voz alta ("cómo doy de alta una incapacidad del imss", "qué es apto condicionado", "dónde veo los exámenes pendientes"). No importan acentos ni errores de dedo.

- Te muestro los artículos que mejor coinciden; **Leer** abre el texto completo y **Abrir** te lleva a la pantalla.
- Marca **¿Te sirvió?** al final: con eso mejoramos la ayuda.
- Si no encuentro nada, tu pregunta queda registrada para que el servicio médico la documente.

Nunca escribas aquí datos de un paciente (NSS, CURP, diagnósticos): esta ayuda es sobre **cómo usar el portal**.', 'ayuda', 1);
COMMIT;
END;
/

DROP PROCEDURE PR_SERV_MED_AYUDA_UP;

-- ---------------------------------------------------------------------------
-- 3. Verificacion
-- ---------------------------------------------------------------------------
SELECT MODULO, COUNT(*) articulos FROM SERV_MED_AYUDA_ARTICULO WHERE ACTIVO = 'S' GROUP BY MODULO ORDER BY MODULO;
SELECT COUNT(*) total FROM SERV_MED_AYUDA_ARTICULO;
