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
            // visible: con tamano y sin estar oculto por display:none de un ancestro
            if (el && el.getClientRects().length && el.offsetParent !== null) { return el; }
        }
        return null;
    }

    // ------------------------------------------------------------------ tutoriales
    // Cada paso: el (selector o lista), titulo, texto; opcional: siNoEsta (que hacer si aun no aparece),
    // avanzaCon (selector que, al aparecer, avanza solo: el usuario hizo la accion).
    var MODULO = function (accion) { return '#nssModulesSub a.module[data-action="' + accion + '"]'; };
    var PRIMERO_NSS = 'Primero busca a la persona: escribe su NSS arriba y pulsa «Buscar». Aquí te espero.';
    // Los modulos estan dentro del grupo «Expediente General» del menu, que viene plegado: si el modulo aun
    // no se ve pero el grupo si, se senala el grupo (paso.alterno) con su propio texto
    var GRUPO = { alterno: '#nssModulesGroup > a.has-sub', textoAlterno: 'Abre «Expediente General» en el menú de la izquierda para ver los módulos de la persona.' };
    function conGrupo(paso) { return Object.assign({}, GRUPO, paso); }

    var TUTORIALES = {
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
            pagina: 'nss',
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
            pagina: 'nss',
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
            pagina: 'nss',
            pasos: [
                conGrupo({ el: MODULO('antidoping'), titulo: 'Abre «Antidoping»', texto: 'Pulsa este módulo para ver las pruebas de la persona.', siNoEsta: PRIMERO_NSS,
                    avanzaCon: '#moduleContent button[onclick^="nuevoAntidoping"]' }),
                { el: '#moduleContent button[onclick^="nuevoAntidoping"]', titulo: 'Nueva prueba', texto: 'Para registrar una prueba pulsa «Nuevo registro».', siNoEsta: 'Abre el módulo «Antidoping».',
                    avanzaCon: '#antidopingCustodia' },
                { el: '#antidopingTipoPrueba', titulo: 'Tipo de prueba', texto: 'Antidoping o alcoholemia. En antidoping eliges la sustancia; en alcoholemia capturas el % de alcohol en la custodia.' },
                { el: '#antidopingCustodia', titulo: 'Cadena de custodia (FT-SO-14)', texto: 'Identificación, razón de la prueba, elementos a detectar (desmarca los que tu kit no detecta), tratamiento médico, hora, lugar y responsable. Lo vacío sale en blanco para llenarse a mano.' },
                { el: '#antidopingAltaForm button[onclick^="guardarAntidoping"]', titulo: 'Guarda la prueba', texto: 'Después, en la lista, la columna «Formato» trae el FT-SO-14 en todas las pruebas y el FT-SO-22 (aceptación a CECOSAMA) solo en las positivas.' }
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
        var ultimo = estado.i === t.pasos.length - 1;
        var espera = !el;
        var html = '<div class="tut-p">' + esc(t.titulo) + ' · paso ' + (estado.i + 1) + ' de ' + t.pasos.length + '</div>'
            + '<h5>' + esc(paso.titulo) + '</h5><div>' + esc(texto) + '</div>'
            + (espera ? '<div class="tut-espera">' + esc(paso.siNoEsta || 'Esperando a que aparezca en la pantalla…') + '</div>' : '')
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
        return Object.keys(TUTORIALES).map(function (k) { return { id: k, titulo: TUTORIALES[k].titulo }; });
    };

    document.addEventListener('DOMContentLoaded', function () {
        var id = new URLSearchParams(location.search).get('tutorial');
        if (id && TUTORIALES[id] && enSuPagina(TUTORIALES[id])) { setTimeout(function () { arrancar(id); }, 600); }
    });
})();
