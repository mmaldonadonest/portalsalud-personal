/*
 * PortalHtml (11-nov-2026, Aikido CWE-79 "Using document write methods can lead to XSS attacks").
 *
 * Las pantallas arman HTML con datos (tablas, avisos, listas). En lugar de asignarlo a innerHTML directo,
 * pasan por PortalHtml.poner(elemento, html): el HTML se interpreta en un <template> (inerte: no ejecuta
 * scripts ni carga nada), se le quitan las etiquetas y atributos que pueden ejecutar codigo (script, iframe,
 * on*, href/src con javascript:) y solo entonces se coloca. Asi, aunque algun dato llegue sin escapar, no
 * puede inyectar codigo. Los datos se siguen escapando en cada pantalla (escapeHtml/escapar): esto es la
 * segunda barrera, en un solo lugar.
 */
(function () {
    'use strict';
    var PROHIBIDAS = 'script,iframe,frame,frameset,object,embed,applet,base,link,meta,style,noscript';
    var URL_PELIGROSA = /^(javascript|vbscript|data:text\/html)/;
    var ATRIBUTOS_URL = ['href', 'src', 'xlink:href', 'action', 'formaction', 'srcdoc'];

    function limpiar(raiz) {
        raiz.querySelectorAll(PROHIBIDAS).forEach(function (n) { n.remove(); });
        raiz.querySelectorAll('*').forEach(function (el) {
            Array.prototype.slice.call(el.attributes).forEach(function (a) {
                var nombre = a.name.toLowerCase();
                var valor = String(a.value || '').replace(/[\s\u0000-\u001f]+/g, '').toLowerCase();
                if (nombre.indexOf('on') === 0 || (ATRIBUTOS_URL.indexOf(nombre) >= 0 && URL_PELIGROSA.test(valor))) {
                    el.removeAttribute(a.name);
                }
            });
        });
        return raiz;
    }

    /** Fragmento limpio a partir de un texto HTML (respeta <tr>, <td>, <option>... gracias al <template>). */
    function fragmento(html) {
        var t = document.createElement('template');
        t.innerHTML = html == null ? '' : String(html);   // template: contenido inerte, se limpia antes de usarse
        return limpiar(t.content);
    }

    window.PortalHtml = {
        /** Reemplaza el contenido del elemento por el HTML ya limpio. Devuelve el elemento. */
        poner: function (el, html) {
            if (el) {
                el.replaceChildren(fragmento(html));
            }
            return el;
        },
        /** Texto plano (sin interpretar HTML). */
        texto: function (el, txt) {
            if (el) {
                el.textContent = txt == null ? '' : String(txt);
            }
            return el;
        },
        /** Escapa un texto para concatenarlo dentro de HTML. */
        esc: function (s) {
            return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
                .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
        }
    };
})();
