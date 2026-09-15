# Skin ONEST — tarjetas de "Elija aplicación" del launcher SSO

Complemento de `onest-skin-guide.md`. El skin ya está aplicado en `sso.onestcloud.mx/sso/app/home`;
lo que no convenció al usuario final son las **tarjetas blancas** de la lista de aplicaciones
(bloque blanco sobre fondo gris claro, logo genérico repetido, poco contraste con la marca).

Tres alternativas, todas con los **mismos tokens** de `onest-skin.css` (`--onest-blue-*`,
`--onest-green-*`, `--onest-gray-*`, `--onest-radius-*`). Mock renderizado con las tres:
`onest-skin-launcher-tarjetas-mock.html` (abrir en el navegador) / `onest-skin-launcher-tarjetas-mock.png`.

| Opción | Qué es | Cuándo conviene |
|---|---|---|
| **A — Mosaico azul marca** | Tarjeta con degradado azul corporativo, texto blanco, ícono en cuadro translúcido, píldora verde de ambiente, botón "Abrir aplicación" que se vuelve blanco al pasar el mouse | Pocas apps (3–8). Es la que más "marca" tiene y elimina el blanco por completo. **Recomendada** |
| **B — Lista compacta** | Filas con banda azul a la izquierda, ícono en cuadro azul claro, clave en monoespaciado, píldora de ambiente y botón "Abrir" sólido | Muchas apps (10+) o si quieren ver todo sin scroll. Sigue habiendo blanco, pero con acento de color y hover azul claro |
| **C — Panel azul marino** | Un panel con el mismo degradado del sidebar y tarjetas translúcidas ("cristal"); ícono en cuadro blanco, borde verde al pasar el mouse | Si quieren que la página completa se sienta "oscura/corporativa" como el sidebar |

En las tres: el **ícono** debe ser distinto por app (hoy es el logo Onest repetido, que no ayuda a
distinguir). Puede ser un Remixicon por clave de app (`ri-graduation-cap-line`, `ri-file-scan-line`,
`ri-dashboard-line`, `ri-stethoscope-line`) o un campo `icono` en el catálogo de apps del SSO.
La píldora de ambiente usa **verde = Producción**, **ámbar = QA**, gris = otro.

## Markup común

Cada app es un `<a>` (toda la tarjeta es clicable, no sólo el link "Abrir aplicación"):

```html
<div class="apps-a">   <!-- o apps-b / apps-c -->
  <a class="app-a" href="/sso/app/open/OCR_APP">
    <div class="top">
      <div class="ico"><i class="ri-file-scan-line"></i></div>
      <span class="amb">Producción</span>          <!-- .amb.qa para QA -->
    </div>
    <div>
      <div class="nom">OCR Scan</div>
      <div class="clave">OCR_APP</div>
    </div>
    <span class="abrir">Abrir aplicación <i class="ri-arrow-right-up-line"></i></span>
  </a>
</div>
```

## CSS — Opción A (recomendada)

```css
.apps-a{display:grid;grid-template-columns:repeat(auto-fill,minmax(300px,1fr));gap:18px}
.app-a{position:relative;display:flex;flex-direction:column;gap:14px;padding:22px;border-radius:var(--onest-radius-lg);color:#fff;text-decoration:none;
  background:linear-gradient(135deg,var(--onest-blue-900) 0%,var(--onest-blue-700) 60%,var(--onest-blue-500) 100%);
  box-shadow:0 6px 18px rgba(0,62,117,.18);transition:transform .15s ease,box-shadow .15s ease}
.app-a:hover{transform:translateY(-3px);box-shadow:0 12px 26px rgba(0,62,117,.28);color:#fff}
.app-a .top{display:flex;justify-content:space-between;align-items:flex-start}
.app-a .ico{width:56px;height:56px;border-radius:14px;background:rgba(255,255,255,.16);display:grid;place-items:center;font-size:26px}
.app-a .amb{font-size:10px;font-weight:700;letter-spacing:.12em;text-transform:uppercase;padding:5px 10px;border-radius:999px;background:var(--onest-green-500);color:#fff}
.app-a .amb.qa{background:#F59E0B}
.app-a .nom{font-size:17px;font-weight:700;letter-spacing:-.01em}
.app-a .clave{font-size:12px;opacity:.78;margin-top:2px;font-family:ui-monospace,Menlo,monospace}
.app-a .abrir{margin-top:auto;display:inline-flex;align-items:center;gap:8px;font-size:13px;font-weight:600;background:rgba(255,255,255,.14);border:1px solid rgba(255,255,255,.28);padding:8px 14px;border-radius:var(--onest-radius-sm);width:max-content}
.app-a:hover .abrir{background:#fff;color:var(--onest-blue-900)}
```

## CSS — Opción B (lista compacta)

```css
.apps-b{display:flex;flex-direction:column;gap:10px;max-width:760px}
.app-b{display:grid;grid-template-columns:52px 1fr auto auto;gap:16px;align-items:center;padding:14px 18px;border-radius:var(--onest-radius-md);text-decoration:none;color:inherit;
  background:#fff;border:1px solid var(--onest-gray-200);border-left:5px solid var(--onest-blue-700);transition:background .15s ease,border-color .15s ease}
.app-b:hover{background:var(--onest-blue-100);border-color:var(--onest-blue-300);border-left-color:var(--onest-green-500);color:inherit}
.app-b .ico{width:52px;height:52px;border-radius:12px;background:var(--onest-blue-100);color:var(--onest-blue-700);display:grid;place-items:center;font-size:24px}
.app-b .nom{font-size:15px;font-weight:600}
.app-b .clave{font-size:12px;color:var(--onest-gray-500);font-family:ui-monospace,Menlo,monospace}
.app-b .amb{font-size:10px;font-weight:700;letter-spacing:.12em;text-transform:uppercase;padding:5px 10px;border-radius:999px;background:var(--onest-green-100);color:var(--onest-green-700)}
.app-b .btn{font-size:13px;font-weight:600;color:#fff;background:var(--onest-blue-700);padding:9px 16px;border-radius:var(--onest-radius-sm)}
.app-b:hover .btn{background:var(--onest-green-700)}
```

Markup de la fila: `<a class="app-b"><div class="ico">…</div><div><div class="nom">…</div><div class="clave">…</div></div><span class="amb">Producción</span><span class="btn">Abrir</span></a>`.

## CSS — Opción C (panel azul marino)

```css
.panel-c{background:linear-gradient(0deg,#050d1a 0%,var(--onest-blue-900) 100%);border-radius:var(--onest-radius-lg);padding:28px;color:#fff}
.apps-c{display:grid;grid-template-columns:repeat(auto-fill,minmax(280px,1fr));gap:16px}
.app-c{display:flex;flex-direction:column;gap:12px;padding:20px;border-radius:var(--onest-radius-lg);text-decoration:none;color:#fff;
  background:rgba(255,255,255,.07);border:1px solid rgba(255,255,255,.14);backdrop-filter:blur(6px);transition:background .15s ease,border-color .15s ease,transform .15s ease}
.app-c:hover{background:rgba(255,255,255,.13);border-color:var(--onest-green-500);transform:translateY(-2px);color:#fff}
.app-c .top{display:flex;justify-content:space-between;align-items:center}
.app-c .ico{width:48px;height:48px;border-radius:12px;background:#fff;color:var(--onest-blue-700);display:grid;place-items:center;font-size:22px}
.app-c .amb{font-size:10px;font-weight:700;letter-spacing:.12em;text-transform:uppercase;padding:4px 10px;border-radius:999px;border:1px solid var(--onest-green-500);color:#9be0a5}
.app-c .nom{font-size:16px;font-weight:700}
.app-c .clave{font-size:12px;opacity:.7;font-family:ui-monospace,Menlo,monospace}
.app-c .abrir{font-size:13px;font-weight:600;color:#9be0a5;display:flex;align-items:center;gap:6px;margin-top:4px}
```

El panel envuelve título + subtítulo + rejilla: `<div class="panel-c"><h4>Elija aplicación</h4><p>…</p><div class="apps-c">…</div></div>`.

## Notas para el SSO

- Si el launcher usa `<a class="btn">` dentro de las tarjetas, aplicar también la regla de
  `onest-skin.css` `html[data-brand="onest"] a.btn { color: var(--bs-btn-color) }` (sin ella el
  texto del botón toma el azul de link y se pierde sobre fondos de color).
- Las tres opciones son responsivas (`auto-fill` / columnas que envuelven); en móvil la A y la C
  quedan a una columna, la B mantiene la fila.
- Nada de esto depende de JS; es sólo CSS + cambiar el markup de la tarjeta.
