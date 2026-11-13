/*
 * Sesion del portal (13-nov-2026). Tres cosas, en todas las pantallas del layout app:
 *
 * 1) Sesion muerta -> aviso y login. Tras un reinicio del Tomcat (o al vencer) el servidor manda al
 *    login cualquier GET, y un guardado (POST) lo rechaza con 403 por token CSRF. Las pantallas
 *    mostraban ese 403 como «No tienes permiso» (caso Carga de Excel, 13-nov) y el usuario creia que
 *    le faltaban roles.
 * 2) Sesion viva pero token viejo (la persona volvio a entrar en otra pestana): se pide el token
 *    nuevo a api/sesion y se repite el guardado UNA vez, sin que el usuario se entere.
 * 3) Inactividad: 2 minutos antes de que venza la sesion pregunta «¿Necesitas mas tiempo?».
 *    Si -> extiende. No, o nadie contesta -> cierra la sesion y manda al login.
 *    La actividad se comparte entre pestanas (localStorage): trabajar en una no cierra la otra.
 *    Ojo: la campanita consulta al servidor cada 45 s, asi que el servidor por si solo nunca
 *    venceria una pestana abierta; por eso la inactividad se mide aqui, por teclado/raton.
 *
 * El 403 del filtro de permisos (ClinicalAccessFilter) trae su propio texto y se deja pasar tal cual:
 * ese si es falta de permiso.
 */
(function () {
    'use strict';
    if (!window.fetch || window.__portalSesion) { return; }
    window.__portalSesion = true;

    var ORIG = window.fetch.bind(window);
    var MSG_PERMISO = 'No tienes permiso para ver esta información clínica.';
    var CLAVE = 'portal-salud.sesion.actividad';
    var AVISO_SEG = 120;          // el aviso aparece 2 minutos antes de vencer
    var muerta = false;

    function base() { return (typeof contextPath === 'string' && contextPath) ? contextPath : '/'; }
    function meta(n) { var m = document.querySelector('meta[name="' + n + '"]'); return m ? m.content : null; }
    function esPropio(url) {
        try { return new URL(url, window.location.href).origin === window.location.origin; } catch (e) { return false; }
    }
    function esLogin(url) {
        try { return /\/login$/.test(new URL(url, window.location.href).pathname); } catch (e) { return false; }
    }

    // ---------- estado de la sesion en el servidor
    // {datos} viva | null muerta | {error:true} no se pudo saber (red, 500): en ese caso NO se saca a nadie
    async function consultarSesion() {
        try {
            var r = await ORIG(base() + 'api/sesion', { headers: { 'Accept': 'application/json' }, cache: 'no-store', credentials: 'same-origin' });
            var tipo = r.headers.get('content-type') || '';
            if (r.ok && !esLogin(r.url) && tipo.indexOf('json') >= 0) { return await r.json(); }
            if (r.status === 401 || r.status === 403 || esLogin(r.url)) { return null; }
            return { error: true };
        } catch (e) {
            return { error: true };
        }
    }
    function ponerToken(s) {
        var m = document.querySelector('meta[name="_csrf"]');
        if (m) { m.content = s.csrfToken; }
        document.querySelectorAll('input[type="hidden"][name="' + (s.csrfParam || '_csrf') + '"]').forEach(function (i) { i.value = s.csrfToken; });
    }
    function conTokenNuevo(init, s) {
        var nuevo = Object.assign({}, init);
        var h = init.headers;
        if (h instanceof Headers) {
            h = new Headers(h);
            if (h.has(s.csrfHeader)) { h.set(s.csrfHeader, s.csrfToken); }
        } else if (Array.isArray(h)) {
            h = h.map(function (par) { return String(par[0]).toLowerCase() === s.csrfHeader.toLowerCase() ? [par[0], s.csrfToken] : par; });
        } else if (h && typeof h === 'object') {
            h = Object.assign({}, h);
            Object.keys(h).forEach(function (k) { if (k.toLowerCase() === s.csrfHeader.toLowerCase()) { h[k] = s.csrfToken; } });
        }
        if (h) { nuevo.headers = h; }
        var p = s.csrfParam || '_csrf';
        if ((init.body instanceof FormData || init.body instanceof URLSearchParams) && init.body.has(p)) {
            init.body.set(p, s.csrfToken);
        }
        return nuevo;
    }

    // ---------- aviso (una sola caja para «por vencer» y «termino»)
    var caja = null;
    function armarCaja() {
        if (caja) { return caja; }
        caja = document.createElement('div');
        caja.id = 'sesionAviso';
        caja.setAttribute('role', 'alertdialog');
        caja.setAttribute('aria-modal', 'true');
        caja.style.cssText = 'position:fixed;inset:0;z-index:20000;background:rgba(15,23,42,.55);display:none;align-items:center;justify-content:center;padding:16px;';
        caja.innerHTML =
            '<div style="background:#fff;color:#1f2937;border-radius:10px;max-width:420px;width:100%;padding:22px 24px;box-shadow:0 10px 30px rgba(0,0,0,.25);font-size:.95rem;">' +
            '<h5 class="mb-2" id="sesionTitulo" style="font-weight:600;"></h5>' +
            '<p class="mb-3" id="sesionTexto"></p>' +
            '<div class="d-flex gap-2 justify-content-end" id="sesionBotones"></div>' +
            '</div>';
        document.body.appendChild(caja);
        return caja;
    }
    function boton(texto, clase, accion) {
        var b = document.createElement('button');
        b.type = 'button';
        b.className = 'btn ' + clase;
        b.textContent = texto;
        b.addEventListener('click', accion);
        return b;
    }
    function mostrar(titulo, texto, botones) {
        var c = armarCaja();
        c.querySelector('#sesionTitulo').textContent = titulo;
        c.querySelector('#sesionTexto').textContent = texto;
        var cont = c.querySelector('#sesionBotones');
        cont.innerHTML = '';
        botones.forEach(function (b) { cont.appendChild(b); });
        c.style.display = 'flex';
        var ultimo = botones[botones.length - 1];
        if (ultimo) { ultimo.focus(); }
    }
    function ocultar() { if (caja) { caja.style.display = 'none'; } }
    function avisoVisible() { return !!caja && caja.style.display === 'flex'; }

    function irAlLogin(motivo) { window.location.href = base() + 'login?' + motivo; }

    // Sesion muerta: se avisa y el boton lleva al login. No se redirige solo para no tirar sin aviso
    // lo que la persona estaba escribiendo en la pantalla.
    function sesionTerminada() {
        if (muerta) { return; }
        muerta = true;
        detenerReloj();
        mostrar('Tu sesión terminó',
            'Para seguir trabajando vuelve a iniciar sesión. Lo que no se haya guardado en esta pantalla se pierde.',
            [boton('Iniciar sesión', 'btn-primary', function () { irAlLogin('sesion'); })]);
    }

    // ---------- fetch envuelto
    window.fetch = async function (input, init) {
        var r = await ORIG(input, init);
        try {
            var url = (typeof input === 'string' || input instanceof URL) ? String(input) : (input && input.url);
            if (!url || !esPropio(url) || muerta) { return r; }
            // GET con sesion muerta: Spring Security lo manda al login y fetch sigue la redireccion
            if (r.redirected && esLogin(r.url)) { sesionTerminada(); return r; }
            var metodo = String((init && init.method) || (input && input.method) || 'GET').toUpperCase();
            if (r.status !== 403 || metodo === 'GET' || metodo === 'HEAD') { return r; }
            var texto = '';
            try { texto = (await r.clone().text()).trim(); } catch (e) { /* sin cuerpo */ }
            if (texto === MSG_PERMISO) { return r; }                 // falta de permiso real
            var s = await consultarSesion();
            if (s === null) { sesionTerminada(); return r; }
            if (s.error || !s.csrfToken) { return r; }
            ponerToken(s);
            // repetir una vez con el token nuevo (solo si la peticion venia como (url, init): un Request
            // ya consumido no se puede reenviar)
            if (typeof input === 'string' || input instanceof URL) {
                return await ORIG(input, conTokenNuevo(init || {}, s));
            }
        } catch (e) { /* ante cualquier duda, la respuesta original */ }
        return r;
    };

    // ---------- inactividad
    var segundos = parseInt(meta('sesion-segundos') || '0', 10);
    if (!(segundos > AVISO_SEG + 60)) { return; }   // sin dato o sesion muy corta: sin aviso
    var local = Date.now();
    var escrito = 0;

    function ultima() {
        var compartida = 0;
        try { compartida = parseInt(localStorage.getItem(CLAVE) || '0', 10) || 0; } catch (e) { /* sin storage */ }
        return Math.max(local, compartida);
    }
    function actividad() {
        if (avisoVisible() || muerta) { return; }    // con el aviso arriba solo cuenta contestar
        local = Date.now();
        if (local - escrito > 15000) {
            escrito = local;
            try { localStorage.setItem(CLAVE, String(local)); } catch (e) { /* sin storage */ }
        }
    }
    function marcarAhora() {
        local = Date.now(); escrito = local;
        try { localStorage.setItem(CLAVE, String(local)); } catch (e) { /* sin storage */ }
    }
    ['mousedown', 'keydown', 'touchstart', 'wheel', 'mousemove'].forEach(function (ev) {
        document.addEventListener(ev, actividad, { passive: true, capture: true });
    });
    document.addEventListener('scroll', actividad, { passive: true, capture: true });
    marcarAhora();

    async function masTiempo() {
        marcarAhora();
        ocultar();
        var s = await consultarSesion();                 // esta misma llamada extiende la sesion
        if (s === null) { sesionTerminada(); return; }
        if (s && s.csrfToken) { ponerToken(s); }
    }
    async function cerrar(motivo) {
        if (muerta) { return; }
        muerta = true;
        detenerReloj();
        try {
            var h = {}; var t = meta('_csrf'); var n = meta('_csrf_header');
            if (t && n) { h[n] = t; }
            await ORIG(base() + 'logout', { method: 'POST', headers: h, credentials: 'same-origin' });
        } catch (e) { /* se manda al login de todos modos */ }
        irAlLogin(motivo);
    }
    function mmss(seg) { seg = Math.max(0, Math.ceil(seg)); return Math.floor(seg / 60) + ':' + String(seg % 60).padStart(2, '0'); }

    var reloj = setInterval(revisar, 1000);
    function detenerReloj() { if (reloj) { clearInterval(reloj); reloj = null; } }
    function revisar() {
        if (muerta) { return; }
        var inactivo = (Date.now() - ultima()) / 1000;
        var resta = segundos - inactivo;
        if (resta <= 0) { cerrar('expired'); return; }
        if (resta <= AVISO_SEG) {
            var texto = 'Por inactividad, tu sesión se cerrará en ' + mmss(resta) + '. ¿Necesitas más tiempo?';
            if (!avisoVisible()) {
                mostrar('Tu sesión está por vencer', texto, [
                    boton('No, cerrar sesión', 'btn-outline-secondary', function () { cerrar('logout'); }),
                    boton('Sí, necesito más tiempo', 'btn-primary', masTiempo)
                ]);
            } else {
                caja.querySelector('#sesionTexto').textContent = texto;
            }
        } else if (avisoVisible()) {
            ocultar();                                   // contestaron Si en otra pestana
        }
    }
}());
