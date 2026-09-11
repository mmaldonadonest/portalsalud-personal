# Despliegue — Portal Salud Ocupacional

## URL activa

**https://temporary-brisk-mesa-gas0rev.vercel.app**

**Reclamar para conservarlo** (expira si no se reclama):
https://vercel.com/claim-deployment?code=1fadab83-fc9f-40b4-b193-8e569e062066

Es un despliegue anónimo, creado sin iniciar sesión en Vercel. Al abrir el enlace de reclamo con tu cuenta, el proyecto pasa a ser tuyo y deja de expirar.

## Archivo único

Todo el portal vive en **un solo archivo**:

```
public/index.html    → 104 KB · estilos, marcado, datos y lógica
vercel.json          → cabeceras de seguridad y caché
```

Sin build, sin dependencias locales, sin servidor. Se puede abrir con doble clic, enviar por correo o subir a cualquier hosting estático. Las únicas dependencias externas son Chart.js (CDN) y las fuentes Fira (Google Fonts).

## Comandos

```bash
# Ver en local
npx serve public -l 4173

# Redesplegar (anónimo)
npx vercel deploy --temporary

# Tras reclamarlo e iniciar sesión
npx vercel deploy --prod
```

## Contenido

16 módulos y 33 gráficos con datos reales de `10 REPORTE MORBILIDAD 2026 GERENCIA.xlsm` (captura enero–junio 2026):

| Grupo | Módulos |
|---|---|
| Ejecutivo | Dashboard Ejecutivo, Vista por Predio |
| Morbilidad | Atenciones, Causas, Musculoesqueléticas |
| Clínico | Exámenes Médicos, Incapacidades, Accidentabilidad |
| Control | Antidoping, Inventario, Maternidad |
| Sistema | Empleados, Importar, Reportes, Auditoría, Administración |

## Verificaciones realizadas

- 16 vistas cargan sin errores de consola
- 33 gráficos renderizan, todos con `aria-label`
- Contraste AA superado en toda la paleta (mínimo 4.76:1, texto principal 18.72:1)
- Sin scroll horizontal en 375 / 768 / 1024 px
- Skip link, focus visible, navegación por teclado, `aria-current` en el menú
- Trampa de foco en modales y cierre con Escape
- `prefers-reduced-motion` respetado (CSS y Chart.js)

## Nota de privacidad

Publica **datos agregados**: totales por predio, causas, costos y días de incapacidad. No contiene nombres reales, números de empleado ni diagnósticos individuales — el único registro de persona es un ejemplo ficticio en Empleados.

Al conectar el backend, la aplicación debe quedar detrás de autenticación antes de exponer datos operativos identificables.

## Conectar el backend

Sustituir las constantes `D` y `T` del bloque `<script>` por llamadas a:

| Endpoint | Alimenta |
|---|---|
| `GET /api/dashboard/summary` | KPIs |
| `GET /api/dashboard/trends` | Series mensuales |
| `GET /api/dashboard/predios` | Ranking con semáforo |
| `GET /api/analytics/insights` | Textos del Análisis Ejecutivo |

Las cifras que deben devolver están documentadas en `docs/07_DATOS_REALES_2026.md`.

**Importante**: los umbrales del semáforo (crítico ≥280 días, alto ≥180, medio ≥60) y de caducidad (30/60/90 días) están hoy en el código del prototipo. Deben migrar a la tabla `risk_thresholds` y quedar configurables desde Administración.
