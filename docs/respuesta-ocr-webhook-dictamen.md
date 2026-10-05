# Respuesta de Portal Salud a "Webhook de entrada: Portal Salud → OCR (dictamen del examen inicial)"

**Fecha:** 5-oct-2026
**Para:** equipo OCR
**Referencia:** `webhook-portal-a-ocr-dictamen.md` (propuesta de OCR) y `integracion-ocr-examen-inicial.md` v2 (contrato del portal).

Revisamos su propuesta contra lo que el portal ya tiene construido y probado en QA. Adoptamos su firma y sus nombres de campo; quedan **cinco acciones de su lado** para que el primer webhook real no falle.

## Lo que el portal ya ajustó a su propuesta (nada que hacer de su lado)

- `X-Firma` = HMAC-SHA256 del **cuerpo crudo**, hex minúsculas, con el secreto compartido. Tal como la definieron.
- El cuerpo lleva `estatus: "DICTAMINADO"` y `fechaDictamen` (`yyyy-MM-dd`), además de los campos del contrato del portal.
- Se mandan dos encabezados **opcionales** más, por si quieren rechazar reenvíos viejos: `X-Firma-Tiempo` (epoch UTC) y `X-Firma-V2` (`sha256=` + HMAC de `tiempo + "." + cuerpo`). Pueden ignorarlos.

Cuerpo que recibirán:

```json
{
  "evento": "EXAMEN_INICIAL_FINALIZADO",
  "solicitudId": 418,
  "referenciaOcr": "OCR-2026-000123",
  "nss": "30048315698",
  "curp": "MACM830923HMCLLG01",
  "estatus": "DICTAMINADO",
  "dictamen": "APTO_CONDICIONADO",
  "fechaDictamen": "2026-10-06",
  "fechaExamen": "2026-10-06T11:20:00-06:00",
  "restricciones": [ { "codigo": "RES-03", "descripcion": "No realizar trabajo en alturas" } ],
  "documentoUrl": "https://health.onestcloud.mx/api/integracion/ocr/examen-inicial/solicitudes/418/documento"
}
```

Los campos que no usen (`evento`, `nss`, `curp`, `fechaExamen`, `restricciones`, `documentoUrl`) simplemente se ignoran.

## Acciones para OCR

### 1. Agregar `APTO_CONDICIONADO` al catálogo de dictamen

Su catálogo tiene tres valores; el portal maneja **cuatro**. Sin este, cada candidato con una enfermedad controlable (obesidad, hipertensión, diabetes…) les regresará 409/422 y el aviso se quedará en reintentos.

| Valor que manda el portal | Significado | En OCR |
|---|---|---|
| `APTO` | Apto, sin hallazgos | Apto |
| `APTO_CONDICIONADO` | Puede trabajar; enfermedad **controlable** con seguimiento (firma carta compromiso) | Apto condicionado |
| `APTO_RESTRINGIDO` | Puede trabajar con **restricciones permanentes** (padecimiento incurable, limitación física); viaja con `restricciones[]` | **Apto inclusión** |
| `NO_APTO` | No apto | No apto |

### 2. `referenciaOcr` no debe ser el NSS

En su ejemplo la referencia es el NSS (`"56190333254"`). Para el portal `referenciaOcr` es la **llave de idempotencia**: repetirla devuelve la misma solicitud sin crear otra. Si un candidato sale **no apto** y lo vuelven a mandar con el mismo NSS como referencia, el portal regresará `200` con la solicitud anterior **ya finalizada** y no abrirá una nueva.

Usen el id de su expediente/proceso (único por intento), p. ej. `OCR-2026-000123`. El NSS va en su propio campo `nss`.

### 3. Aceptar `solicitudId` numérico

El portal devuelve `"solicitudId": 418` (número) en el `201` y lo repite igual en el webhook. Su receptor debe aceptarlo como número (o como texto numérico), no con el formato `"SOL-abc123"` del ejemplo.

### 4. Validar `X-Firma` como la definieron

HMAC-SHA256 del cuerpo exacto recibido (bytes UTF-8, sin reformatear), comparación en tiempo constante, `401` si no coincide. Responder `200 {"recibido": true}` solo si lo procesaron; cualquier otra respuesta hace que el portal reintente (1, 5, 15, 60 min, 6 h) con el mismo `solicitudId`, así que el receptor debe ser idempotente.

### 5. Entregar al portal

- URL final de su endpoint en **QA** y en **producción** (la que anunciarán como `callbackUrl`, o la que el portal configurará por defecto).
- IPs de salida de OCR, para la lista permitida en nginx.
- Confirmar que el secreto HMAC y la API key llegarán por medio seguro: **el secreto que circuló por chat se rota antes de producción**; para QA pueden usar el actual.

## Pendiente que es de OCR (no bloquea al portal)

Qué hace OCR con el dictamen: solo guardarlo, o además avanzar la etapa `MEDICAL_EXAM` del expediente. El portal manda lo mismo en cualquiera de los dos casos.

## Secuencia

1. OCR aplica 1–4 y entrega 5 para QA.
2. Prueba punta a punta en QA: OCR crea solicitud → portal muestra pendiente → médico captura y finaliza → OCR recibe el dictamen con firma válida. Prueba del respaldo: apagan su receptor, el portal reintenta, y consultan `GET /solicitudes/{id}`.
3. Llaves de producción nuevas, IPs en nginx, firewall en ambos sentidos, y repetir en producción.
