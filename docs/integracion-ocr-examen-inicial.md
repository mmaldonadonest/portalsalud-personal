# Integración OCR ⇄ Portal Salud: examen médico inicial

**Estado:** contrato acordado el 5-oct-2026 con las respuestas de OCR; el portal arranca su construcción. Versión 2 (la del 3-oct era la propuesta inicial).
**Para:** equipo del sistema OCR y Portal Salud.
**Qué cambió respecto a la v1:** la solicitud lleva la ficha completa del candidato (sección 1), catálogos de dictamen y restricciones (nueva sección), `APTO_RESTRINGIDO` = Apto inclusión, y la lista de lo que falta de OCR para probar en QA.

## Qué se necesita

1. **OCR → Portal**: cuando un candidato debe pasar examen médico inicial, OCR avisa al portal. El portal muestra el examen como *pendiente* para esa persona y el médico lo captura en el módulo "Examen médico inicial".
2. **Portal → OCR**: cuando el médico finaliza el examen, el portal avisa a OCR el dictamen (apto / apto condicionado / apto restringido / no apto) y las restricciones, con la referencia que OCR mandó. El diagnóstico (ICD) se agregará cuando Jurídico lo apruebe.

## Recomendación: API REST de entrada + webhook de salida, con consulta de respaldo

| Dirección | Mecanismo | Por qué |
|---|---|---|
| OCR → Portal | `POST` a una API del portal que crea una **solicitud de examen** | OCR ya sabe cuándo pedirlo; una llamada síncrona con respuesta inmediata (id de solicitud) es lo más simple de probar y auditar |
| Portal → OCR | **Webhook**: el portal hace `POST` a una URL de OCR al finalizar el examen, con reintentos | OCR se entera al momento sin estar preguntando |
| Respaldo | `GET` de la solicitud en el portal, para que OCR consulte el estado si un webhook se perdió | Las notificaciones push fallan (red, OCR caído); con la consulta nunca se pierde un resultado |

No se recomienda que OCR solo haga *polling* (preguntar cada N minutos): genera carga inútil y retrasa el aviso.

## Contrato propuesto

### 0. Dónde

| Ambiente | Base de la API del portal | Estado |
|---|---|---|
| Producción | `https://health.onestcloud.mx/api/integracion/ocr/examen-inicial/` | Publicada por nginx en la raíz (sin `/portal-salud`), solo HTTPS |
| QA | por definir; se entrega junto con la API key de QA | |

Todas las rutas de abajo se leen relativas a esa base. Ninguna existe todavía: se construyen en el portal a partir de este contrato.


### 1. OCR crea la solicitud

`POST {base}/solicitudes`

```json
{
  "referenciaOcr": "OCR-2026-000123",
  "nss": "30048315698",
  "curp": "MACM830923HMCLLG01",
  "rfc": "MACM830923H17",
  "nombre": "MIGUEL", "apellidoPaterno": "MALDONADO", "apellidoMaterno": "CLEMENTE",
  "fechaNacimiento": "1983-09-23",
  "sexo": "M",
  "estadoCivil": "Soltero",
  "escolaridad": "Preparatoria",
  "profesionOficio": "Técnico en electrónica",
  "domicilio": "Gabino Barrera 12, San Rafael Chamapa, Naucalpan",
  "telefono": "5558211385",
  "empresa": "Onest México S.A. de C.V.",
  "cuenta": "DESARROLLO DE OPERACIONES",
  "agencia": "AIFA",
  "puestoSolicitado": "Auxiliar de almacén",
  "fechaCita": "2026-10-06",
  "callbackUrl": "https://ocr.ejemplo/api/examenes/resultado"
}
```

Con la solicitud como ficha del candidato (no está en el biométrico), estos datos son los que el portal muestra y los que salen en el expediente impreso (FT-SO-04: ficha de identificación y encabezado).

| Campo | Obligatorio | Para qué |
|---|---|---|
| `referenciaOcr`, `nss` | Sí | Llave de la solicitud y del examen |
| `nombre`, `apellidoPaterno`, `apellidoMaterno` | Sí | Ficha, impreso, notificación |
| `fechaNacimiento`, `sexo` (`M`/`F`) | Sí | Edad y sexo en el formato; el examen tiene secciones por sexo (gineco-obstétricos) |
| `puestoSolicitado`, `cuenta` | Sí | Encabezado del formato y del dictamen |
| `curp`, `rfc` | Si los tienen | Ficha e impreso |
| `estadoCivil`, `escolaridad`, `profesionOficio` | Si los tienen | Ficha de identificación del FT-SO-04 |
| `domicilio`, `telefono` | Si los tienen | Ficha de identificación |
| `empresa`, `agencia` | Si los tienen | Razón social y sitio/predio en el encabezado |
| `fechaCita` | Recomendado | Aviso "examen inicial pendiente (cita …)" y caducidad |
| `callbackUrl` | Opcional | Si no viene, se usa la configurada por sistema |

Los teléfonos de emergencia **no** se piden: los captura el médico en la hoja 1 del examen.

#### Cómo responde el portal a esa petición

La respuesta es inmediata (síncrona). OCR debe guardar `solicitudId` y tratar cada código así:

| HTTP | Cuándo | Qué hace OCR |
|---|---|---|
| **201 Created** | Solicitud nueva aceptada | Guarda `solicitudId`; espera el webhook |
| **200 OK** | La misma `referenciaOcr` ya existía (reintento de OCR). **No se crea otra** | Trata como éxito; lee el `estatus` actual |
| **400 Bad Request** | Falta un dato obligatorio o viene mal | Corrige y reenvía; no reintentar igual |
| **401 Unauthorized** | `X-Api-Key` ausente o incorrecta | Revisar la llave del ambiente |
| **409 Conflict** | El NSS ya tiene examen médico vigente como empleado (candado: el inicial es solo de nuevo ingreso) | No es candidato nuevo: usar el dictamen vigente o escalar a Servicio Médico. No reintentar |
| **503 Service Unavailable** | El portal no puede atender (base caída) | Reintentar más tarde (5, 15, 60 min) |

**201 Created** — solicitud nueva:

```json
{
  "solicitudId": 418,
  "referenciaOcr": "OCR-2026-000123",
  "nss": "30048315698",
  "estatus": "PENDIENTE",
  "recibido": "2026-10-05T10:40:12-06:00",
  "caduca": "2026-11-04",
  "consulta": "https://health.onestcloud.mx/api/integracion/ocr/examen-inicial/solicitudes/418"
}
```

**200 OK** — la referencia ya existía; mismo cuerpo con el estatus actual (y el dictamen si ya terminó):

```json
{
  "solicitudId": 418,
  "referenciaOcr": "OCR-2026-000123",
  "nss": "30048315698",
  "estatus": "FINALIZADO",
  "recibido": "2026-10-05T10:40:12-06:00",
  "fechaExamen": "2026-10-06T11:20:00-06:00",
  "dictamen": "APTO",
  "consulta": "https://health.onestcloud.mx/api/integracion/ocr/examen-inicial/solicitudes/418"
}
```

**400 Bad Request** — datos inválidos (lista todos los problemas, no solo el primero):

```json
{
  "error": "SOLICITUD_INVALIDA",
  "detalles": [
    "nss: obligatorio",
    "fechaNacimiento: formato esperado yyyy-MM-dd",
    "sexo: valores permitidos M o F"
  ]
}
```

**401 Unauthorized** — llave incorrecta:

```json
{ "error": "NO_AUTORIZADO" }
```

**409 Conflict** — el NSS ya es empleado con examen vigente:

```json
{
  "error": "EXAMEN_VIGENTE",
  "mensaje": "El NSS 30048315698 ya tiene examen médico con dictamen Apto del 12/08/2026; el examen inicial solo aplica a nuevo ingreso",
  "dictamenVigente": "APTO",
  "fechaExamen": "2026-08-12"
}
```

**503 Service Unavailable** — el portal no puede atender ahora:

```json
{ "error": "NO_DISPONIBLE", "reintentarEn": 300 }
```

Reglas: `referenciaOcr` es única por sistema (repetirla devuelve la misma solicitud con `200`, nunca duplica); `nss` siempre obligatorio (OCR lo confirmó); `callbackUrl` opcional (si no viene se usa la configurada por sistema); todas las fechas en ISO (`yyyy-MM-dd` o con hora y zona).

Al aceptar la solicitud el portal, además: crea la notificación en la campanita de los médicos ("Examen inicial pendiente: NOMBRE, cita dd/mm"), muestra el aviso en la ficha al buscar ese NSS y deja la solicitud lista para que el médico abra el examen con un clic.

#### Cancelación (OCR → Portal)

`POST {base}/solicitudes/{id}/cancelar`

```json
{ "motivo": "No se presentó a la cita" }
```

Respuesta `200`:

```json
{ "solicitudId": 418, "estatus": "CANCELADO", "motivo": "No se presentó a la cita", "cancelado": "2026-10-08T09:02:00-06:00" }
```

Si ya estaba `FINALIZADO` → `409` `{ "error": "YA_FINALIZADO" }` (el examen ya ocurrió y el dictamen ya se notificó). Sin llamada de OCR, la solicitud caduca sola a los 30 días de `fechaCita` (o del alta, si no hubo cita) y pasa a `CANCELADO` con motivo `CADUCIDAD`.

### 2. Portal notifica el resultado (webhook)

`POST {callbackUrl}` al finalizar el examen:

```json
{
  "evento": "EXAMEN_INICIAL_FINALIZADO",
  "solicitudId": 418,
  "referenciaOcr": "OCR-2026-000123",
  "nss": "30048315698",
  "fechaExamen": "2026-10-06T11:20:00-06:00",
  "dictamen": "APTO_CONDICIONADO",
  "diagnosticos": [ { "clave": "E11.9", "descripcion": "Diabetes mellitus tipo 2 sin complicaciones" } ],
  "restricciones": [ "RES-03" ],
  "medico": "Dr. Mauricio Cerón Solana",
  "documentoUrl": "https://health.onestcloud.mx/api/integracion/ocr/examen-inicial/solicitudes/418/documento"
}
```

OCR responde `2xx` para confirmar. Si no, el portal reintenta (1, 5, 15, 60 min, 6 h) y deja cada intento en bitácora; un administrador puede reenviar a mano.

### 3. Consulta de respaldo

`GET {base}/solicitudes/{id}` (o `GET {base}/solicitudes?referenciaOcr=`) → mismo cuerpo del webhook más `estatus` (`PENDIENTE` | `EN_CAPTURA` | `FINALIZADO` | `CANCELADO`).

## Catálogos que manda el portal (acordados 5-oct-2026)

### Dictamen

| Valor en el webhook | Significado | Equivalente en OCR |
|---|---|---|
| `APTO` | Apto para el puesto | Apto |
| `APTO_CONDICIONADO` | Apto con una condición a cumplir (carta compromiso) | Apto condicionado |
| `APTO_RESTRINGIDO` | Apto con restricciones médicas (ver lista) | **Apto inclusión** |
| `NO_APTO` | No apto | No apto |

El portal manda siempre el valor literal de la primera columna; OCR lo traduce con esta tabla.

### Restricciones médicas (`restricciones[]`, solo cuando el dictamen es `APTO_RESTRINGIDO`)

| Código | Descripción |
|---|---|
| RES-01 | No cargar peso |
| RES-02 | Límite de carga (lleva `valorLimite` y `unidad`, p.ej. 10 kg) |
| RES-03 | No realizar trabajo en alturas |
| RES-04 | No operar maquinaria |
| RES-05 | No conducir vehículos |
| RES-06 | Evitar movimientos repetitivos |
| RES-07 | Evitar exposición a ruido |
| RES-08 | Evitar exposición a sustancias |
| RES-09 | Evitar temperaturas extremas |
| RES-10 | Evitar esfuerzo físico intenso |
| RES-11 | Trabajo administrativo |
| RES-12 | Requiere pausas periódicas |
| RES-13 | Restricción de horario |
| RES-14 | Restricción temporal (lleva `fechaFin`) |
| RES-15 | Uso obligatorio de equipo específico |
| RES-16 | Otra restricción (lleva `descripcion`) |

Cada restricción viaja como `{ "codigo": "RES-02", "descripcion": "Límite de carga", "valorLimite": "10", "unidad": "kg", "temporalidad": "TEMPORAL", "fechaFin": "2026-12-31" }`; los campos vacíos no se mandan.

### Estatus de la solicitud (`GET`)

`PENDIENTE` → `EN_CAPTURA` (el médico abrió el examen) → `FINALIZADO` | `CANCELADO` (por OCR o por caducidad a 30 días).

## Seguridad

- **Llave por sistema** en encabezado (`X-Api-Key`) para las llamadas de OCR al portal, y **firma HMAC** del cuerpo (`X-Firma`) en el webhook del portal a OCR, para que cada lado verifique al otro. Las llaves se escriben a mano en el servidor (igual que el resto de la configuración de producción), nunca en el repositorio ni con valores por defecto.
- Solo HTTPS; en nginx, lista de IPs permitidas para `/api/integracion/`.
- Toda llamada queda en la bitácora del portal (`SERV_MED_AUD_EVENT`) con el cuerpo recibido o enviado.

## Qué guarda el portal

Tabla nueva en la base del portal: `SERV_MED_EXAMEN_SOLICITUD` (referencia OCR, NSS/CURP, datos del candidato, estatus, callback, fechas, id del borrador/examen, intentos de notificación y su resultado). El examen en sí sigue en el mismo lugar de siempre (WS + `SERV_MED_TAG`).

En pantalla: al buscar el NSS, la ficha muestra "**Examen inicial pendiente** (solicitud OCR-2026-000123, cita 6-oct)" con el botón para abrirlo; ese aviso sustituye al botón provisional de hoy.

## Respuestas de OCR (5-oct-2026) y lo que sigue pendiente de su lado

Recibido su documento (`integracion-ocr-portal-salud-responsabilidades.md`). Resumen: NSS siempre (+ CURP cuando la tengan); el candidato **no** está en el biométrico al pedir el examen; de vuelta dictamen + restricciones (ICD pendiente de Jurídico); webhook + consulta; cancelación desde OCR y caducidad a 30 días; `APTO_RESTRINGIDO` = Apto inclusión.

**Decisión del portal ante el biométrico:** la solicitud de OCR hace de ficha del candidato. Por eso el contrato de la solicitud (sección 1) creció a los datos de la ficha de identificación del FT-SO-04: fecha de nacimiento y sexo obligatorios; CURP, RFC, estado civil, escolaridad, profesión u oficio, domicilio, teléfono, empresa y agencia si los tienen. Cuando RH lo contrata, el biométrico tiene el mismo NSS y queda enlazado solo.

**Lo que el portal sigue esperando de OCR para probar en QA:**
1. URL del webhook en QA (y después en producción).
2. Responsable de la regla de firewall en ambos sentidos.
3. Secreto HMAC acordado (el portal emite la API key).
4. Volumen mensual estimado.

## Preguntas para el equipo OCR (contestadas el 5-oct; se conservan como referencia)

Marquen la casilla que aplique o completen el espacio. Las tres primeras deciden el diseño; con ellas contestadas el portal construye su lado completo sin depender del calendario de OCR.

### 1 · Identidad del candidato

El portal trabaja por NSS y busca a la persona en el biométrico. Si el candidato no existe ahí, el examen no se puede guardar.

¿Qué identificador manda OCR en la solicitud?

- [ ] NSS siempre
- [ ] CURP siempre
- [ ] A veces uno, a veces otro
- [ ] Otro: ________________

Cuando OCR pide el examen, ¿el candidato ya está dado de alta en el biométrico?

- [ ] Sí, siempre (se da de alta antes de pedir el examen)
- [ ] No, se da de alta hasta que se contrata
- [ ] Depende: ________________

### 2 · Qué necesita OCR de vuelta

- [ ] Solo el dictamen (apto / apto condicionado / apto restringido / no apto)
- [ ] Dictamen y diagnóstico (clave ICD y descripción)
- [ ] Dictamen, diagnóstico y restricciones médicas
- [ ] También el PDF del expediente

> Si va el diagnóstico, es dato de salud que sale a otro sistema: necesitamos confirmación de Jurídico de que el consentimiento (FT-SO-11) y el aviso de privacidad (FT-SO-32) lo cubren.
> - [ ] Jurídico ya lo revisó  - [ ] Pendiente

### 3 · Cómo recibe OCR el resultado

- [ ] OCR puede exponer una URL (webhook) para que el portal le avise
- [ ] OCR solo puede llamar hacia afuera (consultaría el estado en el portal)
- [ ] Ambas

URL del webhook en QA: ________________  en producción: ________________

### 4 · Ambientes y red

¿El portal productivo puede salir hacia OCR? (hoy solo habla con ORDS)

- [ ] Sí, misma red
- [ ] Hay que abrir regla de firewall: quién la pide ________________
- [ ] No se sabe todavía

Llaves (API key) de OCR para QA y producción: ¿quién las genera y por qué medio se entregan? ________________

### 5 · Cancelaciones

Si el candidato no se presenta al examen:

- [ ] OCR cancela la solicitud (llamada al portal)
- [ ] Caduca sola a los ______ días
- [ ] Se queda pendiente hasta que alguien la cierre en el portal

### 6 · Volumen

Solicitudes de examen inicial estimadas al mes: ______ (sirve para dimensionar reintentos y bitácora).

---

**Cualquier duda sobre una pregunta, con gusto la explicamos antes de que la contesten.**
