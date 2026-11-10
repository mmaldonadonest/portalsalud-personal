/*
 * Tutoriales guiados de la ayuda del portal (sin IA, 21-oct-2026).
 *
 * Oscurece la pantalla, resalta el menu o boton REAL y explica cada paso en una burbuja. No guarda ni
 * consulta nada: solo senala. Cuando un paso necesita que el usuario haga algo (buscar un NSS, abrir un
 * modulo), espera a que aparezca lo siguiente antes de avanzar. Motor propio (sin librerias externas).
 *
 * Se abre con ayudaTutorial('id') desde el chat de ayuda, el centro de ayuda o un articulo cuyo enlace
 * trae ?tutorial=id. Si el tutorial es de otra pagina, primero navega a ella y arranca al cargar.
 */
(function () {
    'use strict';

    var CTX = (window.contextPath || '/').replace(/\/?$/, '/');

    function q(sel) {
        if (!sel) { return null; }
        var lista = Array.isArray(sel) ? sel : [sel];
        for (var i = 0; i < lista.length; i++) {
            var el = document.querySelector(lista[i]);
            // visible: con tamano y sin estar oculto por display:none de un ancestro (offsetParent es null
            // tambien en los elementos fijos, como el menu lateral: esos cuentan si tienen tamano)
            if (el && el.getClientRects().length && (el.offsetParent !== null || getComputedStyle(el).position === 'fixed')) { return el; }
        }
        return null;
    }

    // ------------------------------------------------------------------ tutoriales
    // Cada paso: el (selector o lista), titulo, texto; opcional: siNoEsta (que hacer si aun no aparece),
    // avanzaCon (selector que, al aparecer, avanza solo: el usuario hizo la accion).
    var MODULO = function (accion) { return '#nssModulesSub a.module[data-action="' + accion + '"]'; };
    // Cada tutorial puede llevar menu (CODE de SERV_MED_MENU; basta uno) o admin: el usuario solo ve en la
    // lista los que su rol puede abrir (window.ayudaPermisos lo pone fragments/ayuda-panel.html)
    var PRIMERO_NSS = 'Primero busca a la persona: escribe su NSS arriba y pulsa «Buscar». Aquí te espero.';
    // Los modulos estan dentro del grupo «Expediente General» del menu, que viene plegado: si el modulo aun
    // no se ve pero el grupo si, se senala el grupo (paso.alterno) con su propio texto
    var GRUPO = { alterno: '#nssModulesGroup > a.has-sub', textoAlterno: 'Abre «Expediente General» en el menú de la izquierda para ver los módulos de la persona.' };
    function conGrupo(paso) { return Object.assign({}, GRUPO, paso); }

    var TUTORIALES = {
        'portal-basico': {
            titulo: 'Conoce el portal: menú, ayuda y avisos',
            pagina: 'home',
            pasos: [
                { el: '.sidebar', titulo: 'El menú', texto: 'Aquí están los módulos que tu rol puede usar. Si te falta uno, pídelo al administrador: los menús se asignan por rol.' },
                { el: '#menuSidebar', titulo: 'Mostrar u ocultar el menú', texto: 'Este botón esconde el menú para tener más espacio, y lo vuelve a mostrar.' },
                { el: 'a[title="Ayuda del portal"]', titulo: 'Ayuda', texto: 'Desde aquí abres esta ayuda en cualquier pantalla: escribe tu duda o sigue un tutorial guiado.' },
                { el: '#notifDropdown', titulo: 'Avisos (campanita)', texto: 'El número indica avisos sin leer, por ejemplo una solicitud nueva de examen inicial. Pulsa para verlos.' }
            ]
        },
        'dashboard': {
            titulo: 'Leer el tablero de inicio',
            pagina: 'home',
            pasos: [
                { el: '#fFechaInicial', titulo: 'Periodo', texto: 'Elige la fecha inicial y la final del periodo que quieres ver.' },
                { el: '#fPredio', titulo: 'Predio', texto: 'Filtra por un predio o deja «Todos los predios».' },
                { el: '#btnActualizar', titulo: 'Actualizar', texto: 'Pulsa para recalcular las cifras con el periodo y el predio elegidos.' },
                { el: '#kpiCards', titulo: 'Totales', texto: 'Incapacidades, accidentes, consultas, exámenes y antidoping del periodo. Debajo de cada cifra ves cuánto viene del portal y cuánto de los Excel históricos.' },
                { el: '#chartTendencia', titulo: 'Tendencia por mes', texto: 'La evolución mes a mes. Puedes acercar, mover y regresar con los botones de la gráfica.' }
            ]
        },
        'buscar-nss': {
            titulo: 'Buscar a una persona por NSS',
            pagina: 'nss',
            pasos: [
                { el: '#nssInput', titulo: 'Escribe el NSS', texto: 'Escribe el número de seguridad social (11 dígitos) de la persona. Puedes pegarlo: se limpian los espacios.' },
                { el: '#nssSearchForm button[type="submit"]', titulo: 'Pulsa «Buscar»', texto: 'El portal busca a la persona en el biométrico y carga sus módulos.',
                    avanzaCon: '#nssModulesGroup' },
                { el: '#nssSearchResult', titulo: 'Datos de la persona', texto: 'Aquí ves sus datos generales para confirmar que es la persona correcta.', siNoEsta: PRIMERO_NSS },
                { el: '#nssModulesGroup', titulo: 'Sus módulos', texto: 'En el menú de la izquierda, «Expediente General» trae los módulos que tu rol puede usar: consultas, examen, antidoping… Ábrelo y pulsa uno.', siNoEsta: PRIMERO_NSS }
            ]
        },
        'consulta': {
            titulo: 'Capturar una consulta (pases y pruebas especiales)',
            pagina: 'nss', menu: ['CONSULTA_MEDICA'],
            pasos: [
                conGrupo({ el: MODULO('consulta-form'), titulo: 'Abre «Consulta Medica»', texto: 'Pulsa este módulo para abrir la nueva consulta.', siNoEsta: PRIMERO_NSS,
                    avanzaCon: '#consultaAltaForm' }),
                { el: '#tipoConsulta', titulo: 'Tipo de consulta', texto: '«Pase de Salida» abre el recuadro del pase (FT-SO-02; motivo y hora de salida obligatorios). «Pruebas Especiales» abre la carta de consentimiento (FT-SO-23).', siNoEsta: 'Abre el módulo «Consulta Medica».' },
                { el: ['#icdConsult'], titulo: 'Diagnóstico', texto: 'Es obligatorio en toda consulta: escribe al menos 3 letras y elige la clave del catálogo, o usa «Ver todo el catálogo».' },
                { el: '#medGenerar', titulo: 'Pase de medicamento (FT-SO-29)', texto: 'Márcalo si la persona necesita pase de medicamento. Puedes agregar varios medicamentos y marcar la vigencia como «Permanente».' },
                { el: '#firmaCanvas', titulo: 'Firma del paciente', texto: 'La persona firma aquí con el dedo o el mouse. Con firma, al guardar se abre el documento para imprimir.' },
                { el: '#consultaAltaForm button[onclick^="guardarConsulta"]', titulo: 'Guarda la consulta', texto: 'Al guardar aparecen los enlaces para imprimir los formatos que generaste. Después también los encuentras en «Archivo de Consultas» › detalle de la consulta.' }
            ]
        },
        'examen-formatos': {
            titulo: 'Imprimir el examen y sus formatos',
            pagina: 'nss', menu: ['EXAMEN_MEDICO'],
            pasos: [
                conGrupo({ el: MODULO('examen'), titulo: 'Abre «Examen Medico»', texto: 'Pulsa este módulo para abrir el examen de la persona.', siNoEsta: PRIMERO_NSS,
                    avanzaCon: '#examenAccordion' }),
                { el: '#examenTipoArriba', titulo: 'Tipo de examen', texto: 'El tipo define el formato que se imprime (FT-SO-04, 13, 16, 17, 18, 19 o 20). Si lo cambias, guárdalo aquí mismo.', siNoEsta: 'Abre el módulo «Examen Medico».' },
                { el: '[data-url*="examen/imprimir"]', titulo: 'Imprimir el examen', texto: 'Abre el expediente del examen en otra pestaña, con el encabezado 2026 y el consentimiento FT-SO-11, listo para imprimir o guardar como PDF.' },
                { el: 'select[name="SERV_MED_RESULTADO_EXAMEN.DICTAMEN"]', titulo: 'Dictamen y carta compromiso', texto: 'Con «Apto condicionado» o «Apto restringido», al guardar el examen se activa aquí el botón de la carta compromiso (FT-SO-25). La condición a cumplir son las observaciones.',
                    siNoEsta: 'Abre la sección «Resultado del examen» (o pulsa «Abrir todo») para seguir.' },
                { el: 'button[onclick^="exAbrirCertificacion"]', titulo: 'Dictamen de certificación (FT-SO-31)', texto: 'Solo si el cliente lo pide: marcas la aptitud para 10 tipos de trabajo, se expide con vigencia de 1 año y queda en el historial.',
                    siNoEsta: 'Abre la sección «Resultado del examen» para ver este botón.' },
                { el: '#examenGuardarBtn', titulo: 'Guardar el examen', texto: 'Guarda lo capturado. Quien guarda el examen es quien sale en «Elaboró» en los impresos, con su firma si la registró.' }
            ]
        },
        'antidoping': {
            titulo: 'Antidoping y cadena de custodia',
            pagina: 'nss', menu: ['ANTIDOPING'],
            pasos: [
                conGrupo({ el: MODULO('antidoping'), titulo: 'Abre «Antidoping»', texto: 'Pulsa este módulo para ver las pruebas de la persona.', siNoEsta: PRIMERO_NSS,
                    avanzaCon: '#moduleContent button[onclick^="nuevoAntidoping"]' }),
                { el: '#moduleContent button[onclick^="nuevoAntidoping"]', titulo: 'Nueva prueba', texto: 'Para registrar una prueba pulsa «Nuevo registro».', siNoEsta: 'Abre el módulo «Antidoping».',
                    avanzaCon: '#antidopingCustodia' },
                { el: '[data-para="antidopingTipoPrueba"]', titulo: 'Tipo de prueba', texto: 'Antidoping o alcoholimetría. Primero eliges el resultado; si un antidoping sale positivo, se pide la sustancia. En alcoholimetría capturas el % de alcohol en la custodia.' },
                { el: '#antidopingCustodia', titulo: 'Cadena de custodia (FT-SO-14)', texto: 'Identificación, razón de la prueba, elementos a detectar (desmarca los que tu kit no detecta), tratamiento médico, hora, lugar y responsable. Lo vacío sale en blanco para llenarse a mano.' },
                { el: '#antidopingAltaForm button[onclick^="guardarAntidoping"]', titulo: 'Guarda la prueba', texto: 'Después, en la lista, la columna «Formato» trae el FT-SO-14 en todas las pruebas y el FT-SO-22 (aceptación a CECOSAMA) solo en las positivas.' }
            ]
        },
        'consulta-historial': {
            titulo: 'Ver consultas anteriores',
            pagina: 'nss', menu: ['ARCHIVO_CONSULTAS'],
            pasos: [
                conGrupo({ el: MODULO('expediente'), titulo: 'Abre «Archivo de Consultas»', texto: 'Aquí están todas las consultas de la persona.', siNoEsta: PRIMERO_NSS,
                    avanzaCon: '#moduleContent button[onclick^="exportarConsultasExcel"]' }),
                { el: '#moduleContent button[onclick^="verConsulta"]', titulo: 'Ver el detalle', texto: 'El botón de cada renglón abre la consulta completa; desde el detalle se reimprimen sus formatos (pase de salida, de medicamento, pruebas especiales).',
                    siNoEsta: 'Esta persona aún no tiene consultas registradas en el portal.' },
                { el: '#moduleContent button[onclick^="exportarConsultasExcel"]', titulo: 'Exportar a Excel', texto: 'Descarga la lista de consultas. Debajo está también el histórico que venía de los Excel anteriores.' }
            ]
        },
        'incapacidad': {
            titulo: 'Dar de alta una incapacidad',
            pagina: 'nss', menu: ['INCAPACIDADES'],
            pasos: [
                conGrupo({ el: MODULO('incapacidad-form'), titulo: 'Abre «Incapacidades»', texto: 'Pulsa este módulo para capturar una incapacidad nueva.', siNoEsta: PRIMERO_NSS,
                    avanzaCon: '#incapacidadAltaForm' }),
                { el: '#rubro', titulo: 'Rubro y folio', texto: 'Interna o IMSS, y el folio del certificado de incapacidad.' },
                { el: '#ramo', titulo: 'Ramo', texto: 'Enfermedad general, riesgo de trabajo (RT1, RT2), maternidad… según el certificado.' },
                { el: '#tipo', titulo: 'Inicial o subsecuente', texto: 'La subsecuente continúa una anterior y lleva su fecha de inicio.' },
                { el: '#fechaInicio', titulo: 'Fechas', texto: 'Primera fecha, inicio, término y alta, como vienen en el certificado.' },
                { el: '#diasAutorizados', titulo: 'Días y costo', texto: 'Con el salario diario y los días autorizados el portal calcula el costo.' },
                { el: '#firmaCanvas', titulo: 'Firma del trabajador', texto: 'La persona firma aquí con el dedo o el mouse.' },
                { el: '#incapacidadAltaForm button[onclick^="guardarIncapacidad"]', titulo: 'Guarda la incapacidad', texto: 'Después la encuentras en «Archivo Incapacidades».' }
            ]
        },
        'incapacidad-lista': {
            titulo: 'Consultar incapacidades y sacar el reporte por fechas',
            pagina: 'nss', menu: ['ARCHIVO_INCAPACIDADES'],
            pasos: [
                conGrupo({ el: MODULO('incapacidades'), titulo: 'Abre «Archivo Incapacidades»', texto: 'Lista de las incapacidades de la persona.', siNoEsta: PRIMERO_NSS,
                    avanzaCon: '#reporteFechaInicial' }),
                { el: '#moduleContent button[onclick^="verIncapacidad"]', titulo: 'Ver el detalle', texto: 'Abre la incapacidad completa; desde ahí se reimprime.', siNoEsta: 'Esta persona aún no tiene incapacidades registradas.' },
                { el: '#reporteFechaInicial', titulo: 'Reporte por rango de fechas', texto: 'Elige fecha inicial y final: el reporte trae las incapacidades de TODAS las personas en ese periodo, no solo de esta.' },
                { el: '#moduleContent button[onclick^="generarReporteIncapacidades"]', titulo: 'Generar', texto: 'Arma el reporte y lo puedes exportar a Excel.' }
            ]
        },
        'archivos': {
            titulo: 'Subir y descargar PDFs del expediente',
            pagina: 'nss',
            pasos: [
                conGrupo({ el: MODULO('archivos'), titulo: 'Abre un módulo de archivos', texto: '«E. Laboratorio», «Historico E.M», «Nota Medica» y «Nota Incapacidad» guardan PDFs de la persona. Abre uno.', siNoEsta: PRIMERO_NSS,
                    avanzaCon: '#archivoFiles' }),
                { el: '#archivoFiles', titulo: 'Elige el archivo', texto: 'Selecciona el PDF (o varios) desde tu equipo.' },
                { el: '#moduleContent button[onclick^="subirArchivo"]', titulo: 'Súbelo', texto: 'Queda en la lista de abajo; desde ahí se descarga cuando lo necesites.' }
            ]
        },
        'examen-inicial': {
            titulo: 'Capturar y finalizar el examen inicial',
            pagina: 'nss', menu: ['EXAMEN_INICIAL', 'EXAMEN_MEDICO'],
            pasos: [
                conGrupo({ el: MODULO('examen-inicial'), titulo: 'Abre «Examen Médico Inicial»', texto: 'El examen de ingreso (FT-SO-04), capturado por hojas.', siNoEsta: PRIMERO_NSS,
                    avanzaCon: '#exIni' }),
                { el: '#exIniStepper', titulo: 'Las 10 hojas', texto: 'Pulsa una hoja para ir directo a ella. Puedes capturar en el orden que quieras.' },
                { el: '#exIniGuardado', titulo: 'Se guarda solo', texto: 'Lo capturado se guarda como borrador mientras avanzas; aquí ves cuándo fue el último guardado.' },
                { el: '#exIniNext', titulo: 'Avanzar', texto: '«Siguiente» y «Anterior» recorren las hojas.' },
                { el: '#moduleContent button[onclick^="exIniChip"]', titulo: 'Resultado', texto: 'En la última hoja eliges el dictamen: apto, apto condicionado, apto restringido o no apto.', siNoEsta: 'Ve a la hoja 10 «Cierre».' },
                { el: '#exIniFinalizarBtn', titulo: 'Finalizar', texto: 'Cierra el examen: ya no se edita y, si vino de OCR, se le avisa el resultado.', siNoEsta: 'Ve a la hoja 10 «Cierre».' }
            ]
        },
        'restricciones': {
            titulo: 'Asignar restricciones médicas',
            pagina: 'nss', menu: ['EXAMEN_MEDICO'],
            pasos: [
                conGrupo({ el: MODULO('examen'), titulo: 'Abre «Examen Medico»', texto: 'Las restricciones se asignan dentro del examen.', siNoEsta: PRIMERO_NSS,
                    avanzaCon: '#examenAccordion' }),
                { el: '#examenExtraHeader3', titulo: 'Restricciones médicas', texto: 'Abre esta sección y elige las que apliquen (RES-01 a RES-16). Las activas se marcan en ámbar en el expediente.', siNoEsta: 'Abre el módulo «Examen Medico».' },
                { el: '#examenGuardarBtn', titulo: 'Guarda', texto: 'Las restricciones quedan con el examen.' }
            ]
        },
        'pretest': {
            titulo: 'Llenar el Pre-Test',
            pagina: 'nss', menu: ['PRETEST'],
            pasos: [
                conGrupo({ el: MODULO('pretest'), titulo: 'Abre «Pre-Test»', texto: 'El cuestionario de ingreso de la persona.', siNoEsta: PRIMERO_NSS,
                    avanzaCon: '#pretestForm' }),
                { el: '#pretestForm', titulo: 'Las secciones', texto: 'Datos personales, domicilio, contactos de emergencia, cuestionario de salud y síntomas. Llena lo que aplique.' },
                { el: '#pretestCondicion', titulo: 'Condición', texto: 'Anota aquí cualquier condición de salud relevante.' },
                { el: '#firmaCanvas', titulo: 'Firma', texto: 'La persona firma con el dedo o el mouse.' },
                { el: '#moduleContent button[onclick^="guardarPretest"]', titulo: 'Guarda el Pre-Test', texto: 'Quien guarda queda como «Elaboró» en el impreso.' }
            ]
        },
        'accidentes': {
            titulo: 'Registrar un accidente de trabajo',
            pagina: 'nss', menu: ['ACCIDENTES'],
            pasos: [
                conGrupo({ el: MODULO('accidentes'), titulo: 'Abre «Accidentes»', texto: 'Lista de accidentes de la persona y su seguimiento.', siNoEsta: PRIMERO_NSS,
                    avanzaCon: '#moduleContent button[onclick^="nuevoAccidente"]' }),
                { el: '#moduleContent button[onclick^="nuevoAccidente"]', titulo: 'Nuevo registro', texto: 'Pulsa para capturar un accidente.', avanzaCon: '#accidenteAltaForm' },
                { el: '#accidenteTipoRiesgo', titulo: 'Tipo de riesgo y causa', texto: 'Laboral o trayecto, y la causa (caída, accidente vial, agresión…).' },
                { el: '#accidenteStatusCalificacion', titulo: 'Calificación', texto: 'Pendiente, calificado, improcedente… Se actualiza después en el seguimiento.' },
                { el: '#accidenteDias', titulo: 'Días y costo', texto: 'Los días de incapacidad y el costo se capturan; el portal no los calcula.' },
                { el: '#accidenteAltaForm button[onclick^="guardarAccidente"]', titulo: 'Guarda el accidente', texto: 'Después, en la lista, el botón de cada renglón abre el seguimiento del caso con sus evidencias.' }
            ]
        },
        'maternidad': {
            titulo: 'Registrar el seguimiento de maternidad',
            pagina: 'nss', menu: ['MATERNIDAD'],
            pasos: [
                conGrupo({ el: MODULO('maternidad'), titulo: 'Abre «Maternidad»', texto: 'Seguimiento del embarazo de la trabajadora.', siNoEsta: PRIMERO_NSS,
                    avanzaCon: '#moduleContent button[onclick^="nuevoMaternidad"]' }),
                { el: '#moduleContent button[onclick^="nuevoMaternidad"]', titulo: 'Nuevo registro', texto: 'Pulsa para capturar una revisión.', avanzaCon: '#maternidadAltaForm',
                    siNoEsta: 'Solo se registra en personas de sexo femenino.' },
                { el: '#maternidadSemanasGestacion', titulo: 'Semanas y fecha probable de parto', texto: 'Semanas de gestación y fecha probable de parto.' },
                { el: '#maternidadEstatus', titulo: 'Estatus laboral', texto: 'Elige de la lista en qué etapa está: laborando embarazada, incapacidad, lactancia, aborto, baja o término.' },
                { el: '#maternidadRestricciones', titulo: 'Restricciones', texto: 'Las tareas que no debe realizar durante el embarazo.' },
                { el: '#maternidadAltaForm button[onclick^="guardarMaternidad"]', titulo: 'Guarda', texto: 'Cada revisión queda en la lista.' }
            ]
        },
        'antidoping-seleccion': {
            titulo: 'Selección aleatoria para antidoping',
            pagina: 'antidoping-seleccion', menu: ['ANTIDOPING_SELECCION'],
            pasos: [
                { el: '#poolNss', titulo: 'Lista de candidatos', texto: 'Pega los NSS de los candidatos, uno por línea o separados por coma.' },
                { el: '#cantidadSeleccionar', titulo: 'Cuántos', texto: 'Cuántas personas debe elegir el portal al azar.' },
                { el: '#btnSeleccionar', titulo: 'Seleccionar', texto: 'El portal elige al azar y guarda la selección en el historial.' },
                { el: '#tablaHistorial', titulo: 'Historial', texto: 'Todas las selecciones anteriores, con fecha, tamaño de la lista y quién la hizo.' },
                { el: '#btnLimpiar', titulo: 'Empezar de nuevo', texto: 'Limpia la pantalla para una selección nueva.' }
            ]
        },
        'consumibles': {
            titulo: 'Registrar consumibles de antidoping',
            pagina: 'consumibles', menu: ['CONSUMIBLES'],
            pasos: [
                { el: '#predio', titulo: 'Predio y periodo', texto: 'Predio, año y mes del inventario.' },
                { el: '#cantidadInicial', titulo: 'Cantidades', texto: 'Cantidad inicial, lo que se entregó en el mes y lo que se consumió.' },
                { el: '#formConsumible button[type="submit"]', titulo: 'Guardar', texto: 'Registra el inventario del mes.' },
                { el: '#filtroPredio', titulo: 'Consultar', texto: 'Filtra la tabla por predio para ver su inventario mes a mes.' }
            ]
        },
        'causas-consulta': {
            titulo: 'Administrar el catálogo de causas de consulta',
            pagina: 'causas-consulta', menu: ['CAUSAS_CONSULTA'],
            pasos: [
                { el: '#nombre', titulo: 'Causa nueva', texto: 'Escribe el nombre de la causa que falta en el catálogo.' },
                { el: '#formCausa button[type="submit"]', titulo: 'Guardar', texto: 'La causa aparece desde ese momento en el alta de consulta.' },
                { el: '#filasCausas', titulo: 'Catálogo', texto: 'Desde cada renglón desactivas una causa que ya no se use; no se borra para no perder el histórico.' },
                { el: '#verInactivas', titulo: 'Ver inactivas', texto: 'Muestra también las desactivadas, para volver a activarlas.' }
            ]
        },
        'carga-excel': {
            titulo: 'Cargar un Excel de Servicio Médico',
            pagina: 'carga-excel', menu: ['CARGA_EXCEL'],
            pasos: [
                { el: '#selTipo', titulo: 'Tipo de archivo', texto: 'Elige qué vas a cargar (Atenciones, Incapacidades...). Si el archivo es de otro tipo, el portal lo rechaza sin cargar nada.' },
                { el: '#dropzone', titulo: 'El archivo', texto: 'Arrastra o selecciona el Excel (.xlsx). El orden de las columnas no importa; los encabezados deben ser los de la plantilla.' },
                { el: '#inputCorrecciones', titulo: 'Correcciones de NSS (opcional)', texto: 'Si Servicio Médico encontró a mano el NSS de renglones que llegaron sin él, sube aquí ese Excel. Solo se aplica si el predio y la fecha del renglón coinciden.' },
                { el: '#btnValidar', titulo: 'Validar', texto: 'Revisa catálogos y NSS contra el biométrico SIN escribir nada. Con archivos grandes tarda un par de minutos.' },
                { el: '#resCal', titulo: 'Resultado', texto: 'Cuántos renglones entran y cuántos se rechazan. «Excel de errores» trae cada rechazo con su renglón y motivo, para corregirlo en el original.',
                    siNoEsta: 'Primero valida un archivo.' },
                { el: '#tabs', titulo: 'Pestañas del resultado', texto: '«No entran»: renglones que no se cargarán, corrígelos y vuelve a validar. «Entran con ajuste»: se cargan, revisa el ajuste. «Resumen del archivo»: avisos generales con qué hacer. «Lo que ya estaba cargado»: lo que esta carga sustituye de ese predio y mes.',
                    siNoEsta: 'Aparece después de validar.' },
                { el: '#btnCargar', titulo: 'Cargar', texto: 'Escribe en la base los renglones válidos. Queda en el historial con tu usuario y una copia del archivo original.',
                    siNoEsta: 'Aparece cuando el archivo ya se validó y tiene renglones válidos.' }
            ]
        },
        'bandeja': {
            titulo: 'Bandeja de exámenes iniciales',
            pagina: 'examen-inicial/bandeja', menu: ['EXAMEN_INICIAL_BANDEJA'],
            pasos: [
                { el: '#bandejaFiltros', titulo: 'Filtros', texto: 'Filtra las solicitudes por estatus: pendientes, en captura, finalizadas o canceladas.' },
                { el: '#bandejaResult', titulo: 'Solicitudes', texto: 'Cada solicitud llega de OCR. Tómala para capturar su examen inicial; al finalizarlo, OCR recibe el resultado.',
                    siNoEsta: 'No hay solicitudes con este filtro.' }
            ]
        },
        'admin-roles': {
            titulo: 'Administrar roles y sus menús',
            pagina: 'admin/roles', admin: true,
            pasos: [
                { el: '#formRol', titulo: 'Crear un rol', texto: 'Código (por ejemplo SUPERVISOR), nombre y descripción.' },
                { el: '#filasRoles', titulo: 'Roles', texto: 'La lista de roles. Desde cada renglón entras a sus menús.' },
                { el: ['#filasRoles a[href*="/menus"]', '#filasRoles button'], titulo: 'Menús del rol', texto: 'Marca los módulos que el rol puede ver (por ejemplo «Ayuda»). Lo que no esté marcado no aparece y su ruta responde 403.' }
            ]
        },
        'admin-usuarios': {
            titulo: 'Asignar el rol de un usuario',
            pagina: 'admin/usuarios', admin: true,
            pasos: [
                { el: '#nss', titulo: 'Busca al usuario', texto: 'Escribe el NSS del empleado (es su usuario del portal).' },
                { el: '#formBuscar button[type="submit"]', titulo: 'Buscar', texto: 'Aparece su rol actual y puedes cambiarlo. El cambio aplica en su siguiente inicio de sesión.' }
            ]
        },
        'admin-firmas': {
            titulo: 'Administrar firmas digitalizadas',
            pagina: 'admin/firmas', admin: true,
            pasos: [
                { el: '#usuarioFirma', titulo: 'Cargar una firma existente', texto: 'NSS de empleado de quien firma (su usuario del portal), no el del paciente.' },
                { el: '#imagenFirma', titulo: 'Imagen', texto: 'La imagen de su firma. Se guarda cifrada.' }
            ]
        },
        'mi-firma': {
            titulo: 'Registrar mi firma digitalizada',
            pagina: 'perfil/firma',
            pasos: [
                { el: '.aviso-texto', titulo: 'Aviso de privacidad', texto: 'Léelo: explica para qué se usa tu firma. Para registrarla hay que aceptarlo.', siNoEsta: 'El aviso de privacidad todavía no está publicado; no se pueden registrar firmas.' },
                { el: '#opcionDibujo', titulo: 'Opción a: dibujarla', texto: 'Recomendado: firma en el recuadro con el dedo, una pluma de tableta o el mouse. «Limpiar» borra el trazo.' },
                { el: '#opcionImagen', titulo: 'Opción b: subir una imagen', texto: 'O sube una foto o escaneo de tu firma en papel blanco (PNG o JPG, hasta 1 MB). PDF no. Usa una de las dos opciones.' },
                { el: '#aceptoRegistro', titulo: 'Acepta el aviso', texto: 'Marca la casilla para autorizar el uso de tu firma en los formatos del Servicio Médico.' },
                { el: '#formRegistrarFirma button[type="submit"]', titulo: 'Guarda tu firma', texto: 'Queda vigente y desde ese momento sale en «Elaboró» de los documentos que tú captures. Si registras otra, sustituye a la anterior.' }
            ]
        }
    };

    // ------------------------------------------------------------------ motor
    var estado = null;   // { id, i, timer }
    var capa, marco, globo;

    function crearUI() {
        if (capa) { return; }
        var css = document.createElement('style');
        css.textContent =
            '.tut-capa{position:fixed;inset:0;z-index:2000;pointer-events:none}' +
            '.tut-marco{position:fixed;border-radius:8px;box-shadow:0 0 0 9999px rgba(15,23,42,.55);outline:3px solid #FDE047;transition:all .2s;pointer-events:none;z-index:2001}' +
            '.tut-globo{position:fixed;z-index:2002;width:340px;max-width:calc(100vw - 24px);background:#fff;color:#111;border-radius:10px;' +
            'box-shadow:0 10px 30px rgba(0,0,0,.25);padding:12px 14px;font:14px/1.45 system-ui,Segoe UI,Roboto,sans-serif;pointer-events:auto}' +
            '.tut-globo .tut-p{font-size:12px;color:#475467}.tut-globo h5{margin:2px 0 6px;font-size:15px;color:#003E75}' +
            '.tut-globo .tut-espera{margin-top:8px;padding:6px 8px;border-radius:6px;background:#FFFBEB;color:#92400E;font-size:13px}' +
            '.tut-globo .tut-b{display:flex;gap:6px;justify-content:flex-end;margin-top:10px}' +
            '.tut-globo button{border:0;border-radius:6px;padding:5px 12px;font-weight:600;cursor:pointer}' +
            '.tut-globo .tut-sig{background:#0057A8;color:#fff}.tut-globo .tut-ant{background:#EFF6FF;color:#003E75}.tut-globo .tut-sal{background:none;color:#475467;margin-right:auto}';
        document.head.appendChild(css);
        capa = document.createElement('div'); capa.className = 'tut-capa';
        marco = document.createElement('div'); marco.className = 'tut-marco';
        globo = document.createElement('div'); globo.className = 'tut-globo'; globo.setAttribute('role', 'dialog');
        capa.appendChild(marco);
        document.body.appendChild(capa);
        document.body.appendChild(globo);
        document.addEventListener('keydown', function (e) { if (estado && e.key === 'Escape') { terminar(); } });
    }

    function esc(s) { return String(s || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;'); }

    function pintar() {
        if (!estado) { return; }
        var t = TUTORIALES[estado.id];
        var paso = t.pasos[estado.i];
        var el = q(paso.el);
        var texto = paso.texto;
        if (!el && paso.alterno && q(paso.alterno)) { el = q(paso.alterno); texto = paso.textoAlterno; }
        // el usuario ya hizo la accion de este paso: avanza solo
        if (paso.avanzaCon && q(paso.avanzaCon) && estado.i < t.pasos.length - 1) { ir(estado.i + 1); return; }
        // Si pulso «Siguiente» sin hacer la accion de un paso anterior (p. ej. «Nuevo registro»), lo de este
        // paso aun no existe: se resalta lo ultimo que SI esta en pantalla y se le dice que falta, en vez de
        // dejar solo el texto sin nada senalado (reporte del usuario 25-oct)
        var aviso = null;
        for (var j = estado.i - 1; !el && j >= 0; j--) {
            var previo = q(t.pasos[j].el) || q(t.pasos[j].alterno);
            if (previo) { el = previo; aviso = 'Para ver este paso, primero: «' + t.pasos[j].titulo + '» (resaltado).'; }
        }
        // falta buscar a la persona: se senala el buscador de NSS
        if (!el && paso.siNoEsta === PRIMERO_NSS && q('#nssInput')) { el = q('#nssInput'); aviso = PRIMERO_NSS; }
        var ultimo = estado.i === t.pasos.length - 1;
        var espera = !el;
        if (espera) { aviso = paso.siNoEsta || 'Esperando a que aparezca en la pantalla…'; }
        var html = '<div class="tut-p">' + esc(t.titulo) + ' · paso ' + (estado.i + 1) + ' de ' + t.pasos.length + '</div>'
            + '<h5>' + esc(paso.titulo) + '</h5><div>' + esc(texto) + '</div>'
            + (aviso ? '<div class="tut-espera">' + esc(aviso) + '</div>' : '')
            + '<div class="tut-b"><button type="button" class="tut-sal">Salir</button>'
            + (estado.i > 0 ? '<button type="button" class="tut-ant">Anterior</button>' : '')
            + '<button type="button" class="tut-sig">' + (ultimo ? 'Terminar' : 'Siguiente') + '</button></div>';
        if (globo.dataset.html !== html) {
            globo.innerHTML = html;
            globo.dataset.html = html;
            globo.querySelector('.tut-sal').onclick = terminar;
            var ant = globo.querySelector('.tut-ant'); if (ant) { ant.onclick = function () { ir(estado.i - 1); }; }
            globo.querySelector('.tut-sig').onclick = function () { if (ultimo) { terminar(); } else { ir(estado.i + 1); } };
        }
        if (espera) {
            marco.style.display = 'none';
            capa.style.background = 'rgba(15,23,42,.35)';
            globo.style.left = Math.max(12, (window.innerWidth - globo.offsetWidth) / 2) + 'px';
            globo.style.top = '90px';
            return;
        }
        capa.style.background = 'none';
        marco.style.display = '';
        var r = el.getBoundingClientRect();
        // fuera de la pantalla (aparecio despues o el usuario se desplazo): se trae a la vista
        if (r.bottom < 0 || r.top > window.innerHeight) {
            el.scrollIntoView({ block: 'center' });
            r = el.getBoundingClientRect();
        }
        var m = 6;
        marco.style.left = (r.left - m) + 'px'; marco.style.top = (r.top - m) + 'px';
        marco.style.width = (r.width + 2 * m) + 'px'; marco.style.height = (r.height + 2 * m) + 'px';
        var gw = globo.offsetWidth, gh = globo.offsetHeight;
        var top = r.bottom + 14;
        if (top + gh > window.innerHeight - 8) { top = r.top - gh - 14; }
        // el globo siempre dentro de la pantalla (si el elemento es muy alto, encima de el)
        top = Math.min(Math.max(8, top), window.innerHeight - gh - 8);
        var left = Math.min(Math.max(12, r.left), window.innerWidth - gw - 12);
        globo.style.left = left + 'px'; globo.style.top = top + 'px';
    }

    function ir(i) {
        var t = TUTORIALES[estado.id];
        estado.i = Math.max(0, Math.min(i, t.pasos.length - 1));
        globo.dataset.html = '';
        var el = q(t.pasos[estado.i].el);
        if (el) { el.scrollIntoView({ block: 'center', behavior: 'smooth' }); }
        pintar();
    }

    function terminar() {
        if (!estado) { return; }
        clearInterval(estado.timer);
        estado = null;
        capa.style.display = 'none';
        globo.style.display = 'none';
    }

    function arrancar(id) {
        crearUI();
        if (estado) { clearInterval(estado.timer); }
        estado = { id: id, i: 0, timer: setInterval(pintar, 300) };
        capa.style.display = ''; globo.style.display = '';
        ir(0);
    }

    function enSuPagina(t) {
        var actual = location.pathname.replace(/\/+$/, '');
        return actual.endsWith('/' + t.pagina);
    }

    /** Abre un tutorial; si es de otra pagina, navega a ella y arranca al cargar. */
    window.ayudaTutorial = function (id) {
        var t = TUTORIALES[id];
        if (!t) { return; }
        var panel = document.getElementById('ayudaPanel');
        if (panel && window.bootstrap) { var oc = bootstrap.Offcanvas.getInstance(panel); if (oc) { oc.hide(); } }
        if (!enSuPagina(t)) {
            location.href = CTX + t.pagina + '?tutorial=' + encodeURIComponent(id);
            return;
        }
        setTimeout(function () { arrancar(id); }, 350);
    };

    /** Lista para pintar los botones de tutoriales (chat y centro de ayuda). */
    window.ayudaTutoriales = function () {
        var permisos = window.ayudaPermisos || {};
        var puede = function (tut) {
            if (tut.admin) { return !!permisos.admin; }
            if (!tut.menu || !permisos.menus) { return true; } // sin menus locales (fuente ORDS): todos
            return tut.menu.some(function (c) { return permisos.menus.indexOf(c) >= 0; });
        };
        return Object.keys(TUTORIALES).filter(function (k) { return puede(TUTORIALES[k]); })
            .map(function (k) { return { id: k, titulo: TUTORIALES[k].titulo }; });
    };

    document.addEventListener('DOMContentLoaded', function () {
        var id = new URLSearchParams(location.search).get('tutorial');
        if (id && TUTORIALES[id] && enSuPagina(TUTORIALES[id])) { setTimeout(function () { arrancar(id); }, 600); }
    });
})();
