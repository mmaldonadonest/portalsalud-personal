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

    @Scheduled(cron = "0 15 1 * * *")
    public void caducar() {
        try {
            service.caducarVencidas();
        } catch (RuntimeException ex) {
            log.warn("[ocr] caducidad no ejecutada: {}", ex.getMessage());
        }
    }
}
