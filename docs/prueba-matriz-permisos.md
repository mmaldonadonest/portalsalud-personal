# Prueba de humo — Matriz de permisos (sub-tarea 8.4)

Pendiente de ejecutar. Ver detalle de la implementación en `docs/plan-tareas-concretas.html` (Bloque 8) y memoria `matriz-permisos-use-case`. Requiere reconstruir/redesplegar el WAR antes de probar (no hay SQL que aplicar, es 100% Java).

NSS de prueba sugerido: `68958027838` (o cualquier otro conocido, ver "Datos de prueba" en `docs/plan-tareas-concretas.html`).

## Con el usuario actual (`68958027838`, ya tiene acceso clínico) — confirmar que NO hay regresión

| # | Acción | Resultado esperado |
|---|---|---|
| 1 | Buscar el NSS y abrir Examen Médico | Carga normal, incluida la tarjeta "Restricciones médicas" |
| 2 | Abrir Consulta/Expediente | Carga normal |
| 3 | Abrir Incapacidades | Carga normal |
| 4 | Abrir Pretest | Carga normal |
| 5 | Abrir Antidoping | Carga normal |
| 6 | Abrir Accidentes | Carga normal |
| 7 | Abrir Maternidad | Carga normal |
| 8 | Abrir `/consumibles`, `/causas-consulta`, dashboard (`/home`) | Carga normal (rutas administrativas, sin filtro) |

Si cualquiera de 1-7 falla con 403, hay una regresión.

## Con un segundo usuario/rol SIN los `id_menu` clínicos — confirmar el bloqueo real

Requiere identificar o crear (vía `info/registra_rol_usuario`) un usuario cuyo rol en ORDS no tenga registrados los `id_menu` clínicos (6,7,8,9,10,11,12,13,14).

| # | Acción | Resultado esperado |
|---|---|---|
| 9 | Login con ese usuario, buscar un NSS | Ficha se ve, pero el menú lateral no muestra módulos clínicos (ya pasa hoy) |
| 10 | Forzar en el navegador `POST /api/nss/examen` (o cualquier ruta clínica) para ese NSS | **403** con el mensaje "No tienes permiso para ver esta información clínica." |
| 11 | Repetir con `/api/nss/maternidad`, `/api/nss/accidentes`, etc. | 403 en todas |
| 12 | `/consumibles` o `/causas-consulta` con ese mismo usuario | Sigue abierto (no es clínico) |

## Hueco conocido (esperado, no es bug)

| # | Acción | Resultado esperado |
|---|---|---|
| 13 | Con el usuario del punto 9, intentar ver/subir un adjunto (Laboratorio, Nota médica, etc.) | **No bloquea** — hueco documentado en sub-tarea 8.3, no protegido en este corte (`/api/nss/archivos`, `/api/nss/consulta/file`) |

## Notas

- Las pruebas 1-8 son las más importantes para confirmar que no se rompió nada existente.
- Las pruebas 9-12 requieren un segundo usuario/rol de prueba que hoy no se conoce — quedan pendientes hasta que exista.
- La prueba 13 no es un bug: es un alcance explícitamente fuera de este corte (ver `docs/plan-tareas-concretas.html` sub-tarea 8.3).
