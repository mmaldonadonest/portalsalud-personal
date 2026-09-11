# Skin ONEST — guía para aplicar en otro sitio con la misma plantilla

Portal Salud usa un theme de admin dashboard (Bootstrap 5 + jQuery) comprado/genérico, con una capa de marca ONEST encima. Esta guía es para replicar esa capa de marca en otro sitio (portal SSO launcher) que usa la **misma plantilla base**.

## Resumen: dos capas, no una

| Capa | Archivo(s) | Dónde aplica | Se puede apagar |
|---|---|---|---|
| **Marca base** (azul corporativo) | `onest-brand.css` | Todo el sitio, incluido login | No — siempre activa |
| **Skin completo** (ONEST Digital Design System v1.0) | `onest-skin.css` + `onest-skin.js` | Solo páginas internas (layout con sidebar) | Sí — toggle "Skin Mode" en el header, default = Onest |

El login **no** trae el skin completo — solo `onest-brand.css` (botones/links azules) + su propio `login.css` (mecánica del carrusel, sin colores de marca).

## Archivos a copiar

```
static/css/onest-brand.css   → azul corporativo: botones, links, tablas, focus rings
static/css/onest-skin.css    → Design System completo: paleta, tipografía, sidebar, cards, badges
static/js/onest-skin.js      → toggle Light/Dark/Onest + persistencia en localStorage
static/css/login.css         → si el launcher tiene pantalla de login con el carrusel (opcional)
theme/assets/img/logo-onest-smartlogistics.png
theme/assets/img/favicon.png
```

## Wiring — orden importa

En el `<head>` de la página, en este orden (después del CSS del theme base, antes de `pageStyles`/CSS específico de página):

```html
<link rel="stylesheet" href="/theme/assets/css/style.min.css">   <!-- theme base -->
<link rel="stylesheet" href="/css/onest-brand.css">              <!-- azul corporativo, siempre -->
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Montserrat:wght@600;700&family=Inter:wght@400;500;600&display=swap">
<link rel="stylesheet" href="/css/onest-skin.css">                <!-- Design System completo -->
```

Al final del `<body>`, después de jQuery y Bootstrap, después del `script.js` propio del theme:

```html
<script src="/theme/assets/js/script.js"></script>   <!-- Light/Dark del theme, sin tocar -->
<script src="/js/onest-skin.js"></script>             <!-- agrega el 3er modo "Onest" -->
```

`onest-skin.js` depende de que exista en el header un dropdown "Skin Mode" con esta estructura exacta (el JS busca `#skinMode .nav-link[data-mode="..."]`):

```html
<nav id="skinMode" class="nav nav-skin">
    <a href="javascript:void(0)" class="nav-link" data-mode="light">Light</a>
    <a href="javascript:void(0)" class="nav-link" data-mode="dark">Dark</a>
    <a href="javascript:void(0)" class="nav-link active" data-mode="onest">Onest</a>
</nav>
```

El botón "Onest" nace `.active` en el HTML — es el default, para que no haya parpadeo visible antes de que el JS corra.

## Cómo se activa

Todo `onest-skin.css` vive bajo el selector `html[data-brand="onest"]` — si ese atributo no está en el `<html>`, el archivo no cambia nada (inerte). `onest-skin.js` lo pone al cargar la página (salvo que el usuario haya elegido explícitamente Light/Dark antes — se guarda en `localStorage['brand-skin']`: `'onest'` | `'none'` | ausente = primera visita).

No existe una variante oscura del skin Onest — elegir "Onest" siempre limpia cualquier `data-skin="dark"` que haya quedado puesto.

## Paleta (tokens en `onest-skin.css`, reutilizables tal cual)

```css
--onest-blue-900: #003E75;   --onest-blue-700: #00549E;   --onest-blue-500: #1677BD;
--onest-blue-300: #4FA8E0;   --onest-blue-100: #EAF3FA;
--onest-green-700: #2B9138;  --onest-green-500: #39B54A;  --onest-green-100: #EAF7EC;
--onest-gray-950: #20252B;   --onest-gray-700: #475467;   --onest-gray-500: #667085;
--onest-gray-300: #D0D5DD;   --onest-gray-200: #E1E6EB;   --onest-gray-100: #F2F4F7;  --onest-gray-50: #F8FAFC;
--onest-radius-sm: 6px;      --onest-radius-md: 8px;      --onest-radius-lg: 12px;
```

Tipografía: **Montserrat** (600/700) para títulos y branding, **Inter** (400/500/600) para el resto del texto. Azul = color dominante (botones, links, activo). Verde = SOLO acento de éxito/confirmación, nunca relleno. Rojo/ámbar de Bootstrap se conservan sin tocar para error/advertencia (un error debe seguir viéndose rojo, no se convierte a marca).

## Qué NO copiar (específico de Portal Salud, no del skin)

- El `<meta name="app-context-path">` y el `contextPath` en JS — es plomería de despliegue de Portal Salud (nginx + context path de Tomcat), no tiene relación con el skin visual.
- Las reglas de `onest-skin.css` específicas de módulos de Portal Salud (`.examen-accordion`, `.examen-tabs`, `#nssModulesSub .nav-sub-link.active`, `.ficha-identificacion`) — son de pantallas que no existen en el launcher. El resto del archivo (sidebar, header, cards, botones, badges, footer) sí es genérico de la plantilla y aplica igual.

## Caveat conocido

Como `data-brand="onest"` lo pone `onest-skin.js` al final del `<body>`, hay un instante (imperceptible en la práctica, pero real) entre que el CSS carga y el JS corre donde la página se ve con los colores default del theme antes de "saltar" a los colores ONEST. No se resolvió con un script inline en `<head>` porque nunca generó una queja visible — si en el launcher sí se nota, la solución es un `<script>` inline y bloqueante en el `<head>` que lea `localStorage` y ponga `data-brand` antes de que el CSS pinte.
