/* ==========================================================================
   Gráficas del dashboard ejecutivo
   Los datos llegan desde el servidor en un bloque JSON, sin peticiones extra.
   ========================================================================== */
'use strict';

(function graficasDashboard() {
  const bloque = document.getElementById('datosGraficas');
  if (!bloque || typeof Chart === 'undefined') return;

  let datos;
  try {
    datos = JSON.parse(bloque.textContent);
  } catch (error) {
    return;
  }

  const COLORES = {
    azul: '#1D4ED8', azulClaro: '#60A5FA', celeste: '#93C5FD',
    ambar: '#B45309', ambarClaro: '#F59E0B', verde: '#047857',
    rojo: '#B91C1C', violeta: '#6D28D9', turquesa: '#0E7490',
    rosa: '#BE185D', acero: '#64748B'
  };
  const PALETA = [COLORES.azul, COLORES.azulClaro, COLORES.turquesa, COLORES.verde,
                  COLORES.ambarClaro, COLORES.ambar, COLORES.violeta, COLORES.rosa,
                  COLORES.rojo, COLORES.acero];

  Object.assign(Chart.defaults, { maintainAspectRatio: false, color: '#334155' });
  Chart.defaults.font.family = "'Fira Sans', system-ui, sans-serif";
  Chart.defaults.font.size = 11;
  Object.assign(Chart.defaults.plugins.legend.labels,
    { usePointStyle: true, boxWidth: 7, padding: 13 });
  Object.assign(Chart.defaults.plugins.tooltip,
    { backgroundColor: '#0B1220', padding: 11, cornerRadius: 8, boxPadding: 5, usePointStyle: true });

  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    Chart.defaults.animation = false;
  }

  const ejeY = { grid: { color: '#EFF3F8', drawTicks: false }, border: { display: false },
                 ticks: { padding: 8 }, beginAtZero: true };
  const ejeX = { grid: { display: false }, border: { display: false }, ticks: { padding: 6 } };

  function dibujar(id, configuracion) {
    const lienzo = document.getElementById(id);
    if (lienzo) new Chart(lienzo, configuracion);
  }

  /* Evolución mensual: atenciones y días de incapacidad. */
  dibujar('cTendencia', {
    type: 'line',
    data: {
      labels: datos.meses,
      datasets: [
        {
          label: 'Atenciones',
          data: datos.serieAtenciones,
          borderColor: COLORES.azul,
          backgroundColor: 'rgba(29,78,216,.09)',
          fill: true, tension: .34, borderWidth: 2.5,
          pointRadius: 3.5, pointHoverRadius: 6,
          pointBackgroundColor: '#fff', pointBorderWidth: 2
        },
        {
          label: 'Días de incapacidad',
          data: datos.serieDias,
          borderColor: COLORES.ambar,
          tension: .34, borderWidth: 2.5,
          pointRadius: 3.5, pointHoverRadius: 6,
          pointBackgroundColor: '#fff', pointBorderWidth: 2
        }
      ]
    },
    options: {
      interaction: { mode: 'index', intersect: false },
      plugins: { legend: { position: 'top', align: 'end' } },
      scales: { y: ejeY, x: ejeX }
    }
  });

  /* Días perdidos por tipo de incapacidad. */
  if (datos.tipos?.length) {
    dibujar('cTipos', {
      type: 'doughnut',
      data: {
        labels: datos.tipos.map((t) => t.categoria),
        datasets: [{
          data: datos.tipos.map((t) => t.valor),
          backgroundColor: PALETA,
          borderWidth: 3, borderColor: '#fff', hoverOffset: 9
        }]
      },
      options: {
        cutout: '63%',
        plugins: {
          legend: { position: 'bottom' },
          tooltip: {
            callbacks: {
              label: (contexto) => ` ${contexto.label}: ${contexto.raw.toLocaleString('es-MX')} días`
            }
          }
        }
      }
    });
  }

  /* Principales causas de atención. */
  if (datos.causas?.length) {
    dibujar('cCausas', {
      type: 'bar',
      data: {
        labels: datos.causas.map((c) => c.categoria),
        datasets: [{
          data: datos.causas.map((c) => c.valor),
          backgroundColor: datos.causas.map((_, i) => i < 3 ? COLORES.azul
                                                    : i < 6 ? COLORES.azulClaro : COLORES.celeste),
          borderRadius: 5, barThickness: 15
        }]
      },
      options: {
        indexAxis: 'y',
        plugins: { legend: { display: false } },
        scales: { x: ejeY, y: { ...ejeX, ticks: { font: { size: 10 } } } }
      }
    });
  }

  /* Mecanismos de accidente. */
  if (datos.accidentes?.length) {
    dibujar('cAccidentes', {
      type: 'bar',
      data: {
        labels: datos.accidentes.map((a) => a.categoria),
        datasets: [{
          data: datos.accidentes.map((a) => a.valor),
          backgroundColor: [COLORES.rojo, COLORES.ambar, COLORES.ambarClaro, COLORES.violeta,
                            COLORES.acero, COLORES.turquesa, COLORES.rosa],
          borderRadius: 5, barThickness: 20
        }]
      },
      options: {
        indexAxis: 'y',
        plugins: { legend: { display: false } },
        scales: { x: ejeY, y: ejeX }
      }
    });
  }
})();
