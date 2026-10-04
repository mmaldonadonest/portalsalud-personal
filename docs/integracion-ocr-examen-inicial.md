# Integración OCR ⇄ Portal Salud: examen médico inicial

**Estado:** propuesta (3-oct-2026), nada construido todavía.
**Para:** equipo del sistema OCR y Portal Salud.

## Qué se necesita

1. **OCR → Portal**: cuando un candidato debe pasar examen médico inicial, OCR avisa al portal. El portal muestra el examen como *pendiente* para esa persona y el médico lo captura en el módulo "Examen médico inicial".
2. **Portal → OCR**: cuando el médico finaliza el examen, el portal avisa a OCR el dictamen (apto / apto condicionado / apto restringido / no apto) y el diagnóstico, con la referencia que OCR mandó.

## Recomendación: API REST de entrada + webhook de salida, con consulta de respaldo

| Dirección | Mecanismo | Por qué |
|---|---|---|
| OCR → Portal | `POST` a una API del portal que crea una **solicitud de examen** | OCR ya sabe cuándo pedirlo; una llamada síncrona con respuesta inmediata (id de solicitud) es lo más simple de probar y auditar |
| Portal → OCR | **Webhook**: el portal hace `POST` a una URL de OCR al finalizar el examen, con reintentos | OCR se entera al momento sin estar preguntando |
| Respaldo | `GET` de la solicitud en el portal, para que OCR consulte el estado si un webhook se perdió | Las notificaciones push fallan (red, OCR caído); con la consulta nunca se pierde un resultado |

No se recomienda que OCR solo haga *polling* (preguntar cada N minutos): genera carga inútil y retrasa el aviso.

## Contrato propuesto

### 1. OCR crea la solicitud

`POST /portal-salud/api/integracion/ocr/examen-inicial/solicitudes`

```json
{
  "referenciaOcr": "OCR-2026-000123",
  "nss": "30048315698",
  "curp": "MACM830923HMCLLG01",
  "nombre": "MIGUEL", "apellidoPaterno": "MALDONADO", "apellidoMaterno": "CLEMENTE",
  "puestoSolicitado": "Auxiliar de almacén",
  "cuenta": "DESARROLLO DE OPERACIONES",
  "fechaCita": "2026-10-06",
  "callbackUrl": "https://ocr.ejemplo/api/examenes/resultado"
}
```

Respuesta `201`:

```json
{ "solicitudId": 418, "estatus": "PENDIENTE", "recibido": "2026-10-03T19:40:12-06:00" }
```

Reglas: `referenciaOcr` es única (repetirla devuelve la misma solicitud, no crea otra); `nss` o `curp` obligatorio; `callbackUrl` opcional (si no viene se usa la configurada por sistema).

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
  "documentoUrl": "https://portal/portal-salud/api/integracion/ocr/examen-inicial/solicitudes/418/documento"
}
```

OCR responde `2xx` para confirmar. Si no, el portal reintenta (1, 5, 15, 60 min, 6 h) y deja cada intento en bitácora; un administrador puede reenviar a mano.

### 3. Consulta de respaldo

`GET /portal-salud/api/integracion/ocr/examen-inicial/solicitudes/{id}` (o `?referenciaOcr=`) → mismo cuerpo del webhook más `estatus` (`PENDIENTE` | `EN_CAPTURA` | `FINALIZADO` | `CANCELADO`).

## Seguridad

- **Llave por sistema** en encabezado (`X-Api-Key`) para las llamadas de OCR al portal, y **firma HMAC** del cuerpo (`X-Firma`) en el webhook del portal a OCR, para que cada lado verifique al otro. Las llaves se escriben a mano en el servidor (igual que el resto de la configuración de producción), nunca en el repositorio ni con valores por defecto.
- Solo HTTPS; en nginx, lista de IPs permitidas para `/api/integracion/`.
- Toda llamada queda en la bitácora del portal (`SERV_MED_AUD_EVENT`) con el cuerpo recibido o enviado.

## Qué guarda el portal

Tabla nueva en la base del portal: `SERV_MED_EXAMEN_SOLICITUD` (referencia OCR, NSS/CURP, datos del candidato, estatus, callback, fechas, id del borrador/examen, intentos de notificación y su resultado). El examen en sí sigue en el mismo lugar de siempre (WS + `SERV_MED_TAG`).

En pantalla: al buscar el NSS, la ficha muestra "**Examen inicial pendiente** (solicitud OCR-2026-000123, cita 6-oct)" con el botón para abrirlo; ese aviso sustituye al botón provisional de hoy.

## Preguntas para el equipo OCR

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
