package com.onest.app.catalog.firma.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cada madrugada apaga las firmas vigentes de quien ya no es empleado activo (BIO_EMPLEADO.EMP_STATUS
 * distinto de 0). Al imprimir tambien se revisa, asi que esto solo deja el cambio en la bitacora y en
 * «Mi firma» sin esperar a que alguien imprima.
 */
@Component
public class FirmaVigenciaJob {

    private static final Logger log = LoggerFactory.getLogger(FirmaVigenciaJob.class);
    private final FirmaService service;

    /** Apagado solo en copias de prueba contra produccion (igual que OcrCaducidadJob). */
    @Value("${portal.tareas-programadas.activas:true}")
    private boolean tareasActivas;

    public FirmaVigenciaJob(FirmaService service) {
        this.service = service;
    }

    @Scheduled(cron = "0 40 1 * * *")
    public void revisar() {
        if (!tareasActivas) {
            return;
        }
        try {
            int n = service.revisarVigencias();
            if (n > 0) {
                log.info("[firmas] revision diaria: {} firma(s) desactivada(s) por baja en el biometrico", n);
            }
        } catch (RuntimeException ex) {
            log.warn("[firmas] revision diaria no ejecutada: {}", ex.getMessage());
        }
    }
}
