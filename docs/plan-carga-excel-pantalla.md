# Plan: carga de Excel por pantalla

2-nov-2026. Planeado y programado el mismo día; sin commit.

## Estado al 2-nov-2026

| Fase | Estado |
|---|---|
| 0 | Hecha. El paquete no trae rutas, contraseñas ni datos reales (solo el NSS del renglón de ejemplo de la plantilla). El rol de Servicio Médico no se necesita en código: se asigna el menú CARGA_EXCEL en Administración › Roles › Menús |
| 1 | Hecha. `com.onest.app.importer.nss` (9 clases). Normalizador de la consola se sustituyó por el mismo criterio dentro de CatalogosNss |
| 2 | Hecha. Un solo hilo (`carga-excel`) = candado; trabajos en memoria 8 h; original en SERV_MED_FS_FILE con BUSINESS_KEY `XLS_LOTE:id`; usuario en SERV_MED_XLS_LOTE.CREATED_BY (columna existente) |
| 3 | Hecha. `/carga-excel`, `/api/carga-excel`, grupo «Carga de datos» del menú, `/analisis/importar` redirige, prototipo borrado, tutorial `carga-excel` |
| 4 | Parcial. Sin base: Primer1 da 15,696 / 1,170 igual que la consola; columnas movidas, extra, encabezados raros, sin ÁREA y archivo ajeno se comportan bien. **Falta en navegador (lo hace el usuario):** validar con biométrico, cargar, TOLUCA 2025, 3 roles |
| 5 | SQL listo: `docs/script-prod/carga-excel-pantalla-01-menu-y-ayuda.sql` (base del portal). WAR en `target/`. Sin commit |

Objetivo: que MEDICO_ANALISTA y Servicio Médico carguen desde el portal los 10 layouts de Excel con la misma
lógica del cargador de consola (`excel-loader/`, paquete `nss`). Esta pantalla **reemplaza** el prototipo genérico de
Análisis › Importar Excel. La consola se queda para cargas masivas.

## Decisiones tomadas

| Tema | Decisión |
|---|---|
| Alcance | Los 10 layouts desde el inicio |
| Prototipo actual (`SERV_MED_IMPORT_*`) | Se reemplaza; las tablas no se borran (inofensivas) |
| Quién carga | MEDICO_ANALISTA y Servicio Médico |
| Consola | Se queda |
| Repo | El paquete `nss` entra al repo (lo revisa Aikido) |
| Columnas | Por nombre de encabezado: mover o agregar extras no afecta; quitar una obligatoria rechaza el archivo |

## Flujo de la pantalla

1. Subir el Excel y, si hace falta, el Excel de correcciones de NSS.
2. **Validar** (no escribe datos): layout reconocido, % aceptado, rechazos por renglón y columna, avisos,
   columnas ignoradas. Botón para descargar el Excel de errores.
3. **Confirmar**: vuelve a procesar el mismo archivo guardado (mismo checksum) y escribe en `SERV_MED_XLS_*`.
   Reprocesar evita guardar renglones intermedios en una tabla de staging.
4. Historial de lotes con su estado y el archivo original.

## Tareas

### Fase 0. Preparación

| # | Tarea | Listo cuando |
|---|---|---|
| 0.1 | Revisar el paquete `nss` antes de que entre al repo: sin rutas locales, contraseñas, NSS reales ni datos de Primer1 en comentarios o pruebas | Revisión sin hallazgos |
| 0.2 | Identificar el rol real de Servicio Médico (RBAC local y launcher, app 13) y cómo se le da el menú | Nombre del rol confirmado |

### Fase 1. Llevar la lógica al portal

| # | Tarea | Listo cuando |
|---|---|---|
| 1.1 | Copiar al portal (`com.onest.app.importer.nss`): LectorXlsx, LayoutNss, CampoNss, ValidadorNss, CatalogosNss, CorreccionesNss, BiometricoNss, CargaNssService, XlsNssRepository, ReporteNss. **No** van PrellenadoNss, `carga/`, `inventario/`, `layout/`, `transposicion/` | Compila dentro del portal |
| 1.2 | Quitar lo de consola: carpeta de entrada y argumentos se cambian por el archivo subido; correcciones como segundo archivo opcional | El servicio recibe archivos, no rutas |
| 1.3 | Usar el DataSource del portal; consultas al biométrico por lote (hallazgo de 99 ms por viaje, ver U09) | Validar Primer1 sin una consulta por renglón |
| 1.4 | Excel de errores para descargar (renglón, columna, valor, motivo), sin diagnóstico ni tratamiento | Descarga con los mismos rechazos que la consola |

### Fase 2. Proceso en segundo plano y lotes

| # | Tarea | Listo cuando |
|---|---|---|
| 2.1 | Validar y Confirmar corren en segundo plano; la pantalla consulta el avance (nginx corta peticiones largas) | Primer1 se procesa sin timeout |
| 2.2 | Candado: no dos cargas del mismo layout a la vez | El segundo intento recibe aviso claro |
| 2.3 | Guardar el original con `SERV_MED_FS_FILE` y ligarlo al lote de `SERV_MED_XLS_LOTE`, con el usuario que cargó | El historial abre el archivo de cada lote |
| 2.4 | Mantener lo que ya hace la consola: salto por checksum, reemplazo por predio y mes, duplicados contra el portal | Mismos conteos que la consola |

### Fase 3. Pantalla y permisos

| # | Tarea | Listo cuando |
|---|---|---|
| 3.1 | Reescribir `analisis-importar.html`: subir, validar, resultado, confirmar o descartar, historial | Flujo completo en local |
| 3.2 | Sacar la ruta de `/analisis/**` (solo MEDICO_ANALISTA) a una propia permitida a los dos roles, en SecurityConfiguration y en la matriz de permisos | Servicio Médico entra; otros roles reciben 403 |
| 3.3 | Menú para Servicio Médico en el RBAC local | Aparece solo a los dos roles |
| 3.4 | Retirar el código del prototipo genérico (`ImportValidador`, `ExcelLector`, staging) | Nada lo usa |
| 3.5 | Ayuda guiada de la pantalla nueva (tutoriales existentes) | Tutorial publicado |

### Fase 4. Pruebas en local

| # | Tarea | Listo cuando |
|---|---|---|
| 4.1 | Primer1 ATENCIONES: validar, con correcciones, y cargar | Mismo resultado que la consola (88.5% hoy) |
| 4.2 | Los otros 9 layouts con los archivos de TOLUCA 2025, **solo validar** | Mismos conteos que los lotes 21-30 del 12-oct |
| 4.3 | Casos de columnas: movidas, extra, obligatoria faltante, archivo que no es de ningún layout | Mensajes claros en cada caso |
| 4.4 | Probar con un usuario MEDICO_ANALISTA, uno de Servicio Médico y uno sin permiso | 2 entran, 1 recibe 403 |

### Fase 5. Entrega

| # | Tarea | Listo cuando |
|---|---|---|
| 5.1 | SQL para la **base del portal** (si 2.3 o 3.3 lo requieren): columnas de archivo y usuario en `SERV_MED_XLS_LOTE`, menú nuevo | El usuario lo corre |
| 5.2 | WAR en `target/`, control de cambios, commit con permiso | En pruebas |

## Riesgos

- **Dos copias del código** (portal y consola): un cambio de catálogo hay que hacerlo en las dos. El portal será la fuente
  y la consola se actualiza copiando.
- **Pendientes de Atenciones** (ceros en la vista, conteo de personas, respuestas de SM): no dependen de esta pantalla,
  siguen su propio camino.
- **Servicio Médico cargando**: el reemplazo por predio y mes borra lo anterior de ese mes. La pantalla debe decirlo antes
  de confirmar ("este archivo reemplaza N renglones de PREDIO, MES").
