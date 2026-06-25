# portalsalud-personal

## Migración Thymeleaf del dashboard

### Rutas creadas
- `/login`: vista `auth/login.html` con layout de autenticación.
- `/home`: vista `pages/home.html` con layout principal autenticado.
- `/`: redirección a `/home`.

### Decisión de layout
- `templates/layouts/app.html`: layout base para pantallas del dashboard con CSS/JS comunes del theme, sidebar, header, footer y scripts por página.
- `templates/layouts/auth.html`: layout liviano para autenticación con assets mínimos del theme.

### Fragmentos reutilizables
- `templates/fragments/menu.html`
- `templates/fragments/header.html`
- `templates/fragments/footer.html`
- `templates/fragments/scripts.html`

### Assets inventariados desde `analytics.html`
#### CSS
- `/theme/lib/remixicon/fonts/remixicon.css`
- `/theme/lib/jqvmap/jqvmap.min.css`
- `/theme/lib/apexcharts/apexcharts.css`
- `/theme/assets/css/style.min.css`

#### JS
- `/theme/lib/jquery/jquery.min.js`
- `/theme/lib/bootstrap/js/bootstrap.bundle.min.js`
- `/theme/lib/perfect-scrollbar/perfect-scrollbar.min.js`
- `/theme/lib/jqvmap/jquery.vmap.min.js`
- `/theme/lib/jqvmap/maps/jquery.vmap.world.js`
- `/theme/lib/apexcharts/apexcharts.min.js`
- `/theme/assets/js/script.js`
- `/theme/assets/js/db.data.js`
- `/theme/assets/js/db.analytics.js`

### Observaciones de assets
- El theme completo se copió de `src/main/webapp/theme/**` a `src/main/resources/static/theme/**` para mantener disponibles las dependencias y las páginas HTML estáticas referenciadas por el menú.
- Las vistas migradas ya no usan rutas relativas `../`; ahora resuelven assets mediante rutas compatibles con Spring/Thymeleaf.
- Los enlaces vacíos del layout y de las páginas migradas se reemplazaron por rutas válidas o `javascript:void(0)`.
