package com.onest.app.integration.ocr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Caduca cada madrugada las solicitudes pendientes vencidas (30 dias tras la cita o el alta). */
@Component
public class OcrCaducidadJob {

    private static final Logger log = LoggerFactory.getLogger(OcrCaducidadJob.class);
    private final SolicitudExamenService service;

    public OcrCaducidadJob(SolicitudExamenService service) {
        this.service = service;
    }

    /** Ver OcrWebhookService.tareasActivas: apagado solo en copias de prueba contra produccion. */
    @org.springframework.beans.factory.annotation.Value("${portal.tareas-programadas.activas:true}")
    private boolean tareasActivas;

    @Scheduled(cron = "0 15 1 * * *")
    public void caducar() {
        if (!tareasActivas) {
            return;
        }
        try {
            service.caducarVencidas();
        } catch (RuntimeException ex) {
            log.warn("[ocr] caducidad no ejecutada: {}", ex.getMessage());
        }
    }
}
