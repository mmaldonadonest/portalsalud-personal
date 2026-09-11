/* ==========================================================================
   Portal Salud Ocupacional — comportamiento común a todas las vistas
   ========================================================================== */
'use strict';

/* ---------- Menú lateral en pantallas pequeñas ---------- */
(function menuLateral() {
  const sb = document.getElementById('sb');
  const boton = document.getElementById('bg');
  if (!sb || !boton) return;

  boton.addEventListener('click', () => {
    const abierto = sb.classList.toggle('on');
    boton.setAttribute('aria-expanded', String(abierto));
  });

  document.addEventListener('click', (evento) => {
    if (window.innerWidth > 960) return;
    if (sb.contains(evento.target) || boton.contains(evento.target)) return;
    sb.classList.remove('on');
    boton.setAttribute('aria-expanded', 'false');
  });
})();

/* ---------- Avisos temporales ---------- */
function aviso(titulo, mensaje, esError) {
  const contenedor = document.getElementById('toast');
  if (!contenedor) return;

  const elemento = document.createElement('div');
  elemento.className = 'tst' + (esError ? ' err' : '');
  elemento.innerHTML = `
    <svg class="ic" viewBox="0 0 24 24" aria-hidden="true">
      ${esError
        ? '<circle cx="12" cy="12" r="9"/><path d="M12 8v5M12 16h.01"/>'
        : '<path d="M20 6 9 17l-5-5"/>'}
    </svg>
    <div><b></b><p></p></div>`;
  elemento.querySelector('b').textContent = titulo;
  elemento.querySelector('p').textContent = mensaje;

  contenedor.appendChild(elemento);
  setTimeout(() => {
    elemento.style.opacity = '0';
    elemento.style.transform = 'translateX(22px)';
    setTimeout(() => elemento.remove(), 260);
  }, 4000);
}

/* ---------- Diálogos ---------- */
let focoPrevio = null;

function abrirDialogo(contenido) {
  const capa = document.getElementById('ov');
  const dialogo = document.getElementById('mdl');
  if (!capa || !dialogo) return;

  focoPrevio = document.activeElement;
  dialogo.innerHTML = contenido;
  capa.classList.add('on');
  setTimeout(() => dialogo.querySelector('input,select,textarea,button')?.focus(), 70);
}

function cerrarDialogo() {
  document.getElementById('ov')?.classList.remove('on');
  focoPrevio?.focus();
}

document.getElementById('ov')?.addEventListener('click', (evento) => {
  if (evento.target.id === 'ov') cerrarDialogo();
});

document.addEventListener('keydown', (evento) => {
  if (evento.key !== 'Escape') return;
  cerrarDialogo();
  const sb = document.getElementById('sb');
  if (sb?.classList.contains('on')) {
    sb.classList.remove('on');
    document.getElementById('bg')?.setAttribute('aria-expanded', 'false');
  }
});

/* ---------- Llamadas a la API ---------- */
async function pedir(ruta, opciones = {}) {
  const respuesta = await fetch(ruta, {
    headers: { 'Content-Type': 'application/json' },
    credentials: 'same-origin',
    ...opciones
  });

  if (!respuesta.ok) {
    const detalle = await respuesta.json().catch(() => ({}));
    throw new Error(detalle.message || 'No fue posible completar la operación');
  }
  return respuesta.status === 204 ? null : respuesta.json();
}

/* ---------- Formato ---------- */
const formatoEntero = new Intl.NumberFormat('es-MX');
const formatoMoneda = new Intl.NumberFormat('es-MX', {
  style: 'currency', currency: 'MXN', maximumFractionDigits: 0
});

const numero = (valor) => formatoEntero.format(valor ?? 0);
const moneda = (valor) => formatoMoneda.format(valor ?? 0);
