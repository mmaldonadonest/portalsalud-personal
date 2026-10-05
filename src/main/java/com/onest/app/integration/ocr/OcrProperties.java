package com.onest.app.integration.ocr;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Integracion con OCR (docs/integracion-ocr-examen-inicial.md). Las llaves las mete Maven al
 * WAR por ambiente (-Pqa / -Pprod); vacias = la API rechaza todo con 401.
 *
 * @param apiKey        la que OCR manda en X-Api-Key
 * @param hmacSecret    con la que el portal firma el webhook (X-Firma)
 * @param callbackUrl   webhook de OCR por defecto (si la solicitud no trae callbackUrl)
 * @param caducidadDias dias tras la cita (o el alta) para cancelar sola una solicitud pendiente
 * @param publicBaseUrl base publica del portal (p.ej. https://health.onestcloud.mx) para el documentoUrl del webhook
 */
@ConfigurationProperties(prefix = "portal.integracion.ocr")
public record OcrProperties(String apiKey, String hmacSecret, String callbackUrl, Integer caducidadDias, String publicBaseUrl) {

    public boolean habilitada() {
        return apiKey != null && !apiKey.isBlank();
    }

    public int caducidad() {
        return caducidadDias == null || caducidadDias <= 0 ? 30 : caducidadDias;
    }
}
