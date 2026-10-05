# Integración OCR ⇄ Portal Salud: examen médico inicial

**Estado:** contrato acordado el 5-oct-2026 con las respuestas de OCR; el portal arranca su construcción. Versión 2 (la del 3-oct era la propuesta inicial).
**Para:** equipo del sistema OCR y Portal Salud.
**Qué cambió respecto a la v1:** la solicitud admite la ficha del candidato con solo 7 campos obligatorios (sección 1), catálogos de dictamen y restricciones (nueva sección), `APTO_RESTRINGIDO` = Apto inclusión, y la lista de lo que falta de OCR para probar en QA.

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
| `nss` | **Sí** | Llave del examen y de todo el expediente |
| `nombre`, `apellidoPaterno`, `apellidoMaterno` | **Sí** | Ficha, impreso, notificación |
| `curp` | **Sí** | Identificación del candidato |
| `puestoSolicitado`, `cuenta` | **Sí** | Encabezado del formato y del dictamen |
| `referenciaOcr` | Sí (llave de OCR) | Idempotencia y para devolverla en el webhook; si OCR no la manda, el portal usa el NSS como referencia y no puede distinguir reenvíos |
| `fechaNacimiento`, `sexo` (`M`/`F`) | Opcional | Edad y sexo del formato; si no vienen, el médico los captura en la hoja 1 (la CURP ya trae ambos y el portal los deriva de ahí) |
| `rfc`, `estadoCivil`, `escolaridad`, `profesionOficio`, `domicilio`, `telefono` | Opcional | Ficha de identificación del FT-SO-04; lo que falte lo captura el médico |
| `empresa`, `agencia` | Opcional | Razón social y sitio/predio en el encabezado |
| `fechaCita` | Opcional (recomendado) | Aviso "examen inicial pendiente (cita …)" y caducidad; sin ella, caduca a 30 días del alta |
| `callbackUrl` | Opcional | Si no viene, se usa la configurada por sistema |

Acordado el 5-oct-2026: **obligatorios solo NSS, nombre y dos apellidos, CURP, puesto y cuenta**; todo lo demás puede no venir. Los teléfonos de emergencia **no** se piden: los captura el médico en la hoja 1 del examen.

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
    "curp: obligatorio",
    "apellidoMaterno: obligatorio",
    "fechaNacimiento: formato esperado yyyy-MM-dd"
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
| `APTO` | Apto para el puesto, sin hallazgos que atender | Apto |
| `APTO_CONDICIONADO` | Puede trabajar, pero tiene una enfermedad **controlable** que debe mantener bajo control (obesidad, hipertensión, diabetes, etc.). Firma carta compromiso (FT-SO-25) y se le da seguimiento | Apto condicionado |
| `APTO_RESTRINGIDO` | Puede trabajar con **restricciones permanentes**: padecimiento incurable o limitación física (p.ej. problemas para caminar). Es el caso de **inclusión**; viaja con la lista de restricciones (RES-xx) | **Apto inclusión** |
| `NO_APTO` | No apto para el puesto | No apto |

Definiciones del Servicio Médico (5-oct-2026): *condicionado* = algo que se controla y se vigila; *restringido / inclusión* = algo que no se cura y limita qué puede hacer, por eso lleva restricciones explícitas.

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

## Seguridad (autenticación en los dos sentidos)

### OCR → Portal: API key

Toda llamada de OCR al portal (`POST solicitudes`, `GET`, `cancelar`) lleva el encabezado:

```
X-Api-Key: <llave emitida por el portal para OCR>
```

- La llave la **emite el portal**, una por ambiente (QA y producción), y se entrega a OCR por medio seguro (no por correo abierto ni chat).
- Sin encabezado o con llave incorrecta → `401` `{ "error": "NO_AUTORIZADO" }`. Tres fallos seguidos desde la misma IP quedan en bitácora.
- Solo HTTPS. En nginx, `/api/integracion/` acepta únicamente las IPs de OCR (lista que OCR entrega).
- La llave se puede rotar: el portal acepta la nueva y la anterior durante 7 días para cambiarla sin corte.

Ejemplo:

```bash
curl -X POST https://health.onestcloud.mx/api/integracion/ocr/examen-inicial/solicitudes   -H "Content-Type: application/json"   -H "X-Api-Key: 9f3c…(llave de producción)"   -d @solicitud.json
```

### Portal → OCR: firma HMAC del webhook

El webhook que el portal manda a OCR lleva dos encabezados para que OCR verifique que viene del portal y que no es un reenvío viejo:

```
X-Firma-Tiempo: 1759680012                 (epoch en segundos, UTC)
X-Firma: sha256=3a9f…                      (HMAC-SHA256 en hexadecimal)
```

Cálculo: `HMAC_SHA256( secreto, X-Firma-Tiempo + "." + cuerpoJsonTalCual )`, en minúsculas hexadecimales. El cuerpo se firma **byte a byte como se envía** (UTF-8, sin reformatear).

OCR debe: (1) recalcular la firma con el mismo secreto y compararla en tiempo constante; (2) rechazar si `X-Firma-Tiempo` difiere más de 5 minutos del reloj de OCR (evita reenvíos); (3) responder `2xx` solo si lo procesó. Cualquier otra respuesta hace que el portal reintente (1, 5, 15, 60 min, 6 h) con el mismo `solicitudId`, así que el receptor debe ser **idempotente** por `solicitudId`.

El **secreto HMAC** lo genera el portal junto con la API key (uno por ambiente) y se entrega por el mismo medio seguro. Ninguna de las dos llaves vive en el repositorio ni tiene valor por defecto; en el portal van dentro del WAR de cada ambiente, en OCR donde ellos guarden sus secretos.

### Bitácora

Toda llamada en ambos sentidos queda en `SERV_MED_AUD_EVENT` del portal: fecha, IP, referencia, código de respuesta y cuerpo (sin la llave).

## Qué guarda el portal

Tabla nueva en la base del portal: `SERV_MED_EXAMEN_SOLICITUD` (referencia OCR, NSS/CURP, datos del candidato, estatus, callback, fechas, id del borrador/examen, intentos de notificación y su resultado). El examen en sí sigue en el mismo lugar de siempre (WS + `SERV_MED_TAG`).

En pantalla: al buscar el NSS, la ficha muestra "**Examen inicial pendiente** (solicitud OCR-2026-000123, cita 6-oct)" con el botón para abrirlo; ese aviso sustituye al botón provisional de hoy.

## Respuestas de OCR (5-oct-2026) y lo que sigue pendiente de su lado

Recibido su documento (`integracion-ocr-portal-salud-responsabilidades.md`). Resumen: NSS siempre (+ CURP cuando la tengan); el candidato **no** está en el biométrico al pedir el examen; de vuelta dictamen + restricciones (ICD pendiente de Jurídico); webhook + consulta; cancelación desde OCR y caducidad a 30 días; `APTO_RESTRINGIDO` = Apto inclusión.

**¿Se puede aplicar el examen a un NSS que aún no es empleado? Sí.** El portal guarda el examen por NSS y no exige que la persona exista en el biométrico; lo único que hoy requiere "ser empleado" es la ficha en pantalla, y para los candidatos esa ficha sale de la solicitud de OCR. Al contratarse, el mismo NSS liga el examen con el biométrico sin recapturar. Condición única: el candidato debe tener **NSS real** (número del IMSS), que es la llave de todo el expediente.

**Decisión del portal ante el biométrico:** la solicitud de OCR hace de ficha del candidato. Por eso el contrato de la solicitud (sección 1) admite los datos de la ficha de identificación del FT-SO-04. Obligatorios: NSS, nombre y apellidos, CURP, puesto y cuenta; el resto (fecha de nacimiento, sexo, RFC, estado civil, escolaridad, profesión, domicilio, teléfono, empresa, agencia) es opcional y lo que falte lo captura el médico. Cuando RH lo contrata, el biométrico tiene el mismo NSS y queda enlazado solo.

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
