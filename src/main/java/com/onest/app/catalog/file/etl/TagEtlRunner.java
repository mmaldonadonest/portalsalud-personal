package com.onest.app.catalog.file.etl;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Runner del ETL de {@code servicioMedico.tags} (MariaDB legacy) -> {@code MED_TAG}
 * (Oracle). Landing EAV fiel, sin normalizar (ver tags-salud.sql notas 2 y 3).
 *
 * <p>Activar con {@code --spring.profiles.active=local,etl --etl.tags.mode=sample|full}.
 * Independiente de {@code etl.files.mode} - se pueden correr por separado.</p>
 */
@Component
@Profile("etl")
public class TagEtlRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(TagEtlRunner.class);
    private static final String CREATED_BY = "ETL_LEGACY";

    private final LegacyTagReader reader;
    private final MedTagWriter writer;
    private final String mode;
    private final int batchSize;

    public TagEtlRunner(LegacyTagReader reader, MedTagWriter writer,
                         @Value("${etl.tags.mode:none}") String mode,
                         @Value("${etl.tags.batch-size:500}") int batchSize) {
        this.reader = reader;
        this.writer = writer;
        this.mode = mode;
        this.batchSize = batchSize;
    }

    @Override
    public void run(String... args) {
        switch (mode) {
            case "sample" -> runSample();
            case "full" -> runFull();
            default -> log.info("[etl-tags] etl.tags.mode={} - no se ejecuta nada. Usar 'sample' o 'full'.", mode);
        }
    }

    private void runSample() {
        List<Long> ids = reader.sampleIds();
        log.info("[etl-tags] modo=sample, {} ids seleccionados", ids.size());
        Stats stats = new Stats();
        for (LegacyTagRow row : reader.findByIds(ids)) {
            procesar(row, stats);
        }
        stats.log("sample");
    }

    private void runFull() {
        long minId = reader.minId();
        long maxId = reader.maxId();
        log.info("[etl-tags] modo=full, rango de ids [{}, {}], lote={}", minId, maxId, batchSize);
        Stats stats = new Stats();
        for (long from = minId; from <= maxId; from += batchSize) {
            long to = Math.min(from + batchSize - 1, maxId);
            for (LegacyTagRow row : reader.findByIdRange(from, to)) {
                procesar(row, stats);
            }
            log.info("[etl-tags] lote [{}, {}] listo - acumulado: {}", from, to, stats);
        }
        stats.log("full");
    }

    private void procesar(LegacyTagRow row, Stats stats) {
        if (writer.existsBySourceId(row.id())) {
            stats.yaExistia.incrementAndGet();
            return;
        }
        if (row.type() == null || row.type().isBlank()) {
            log.warn("[etl-tags] id={} sin type (huerfano) - se omite", row.id());
            stats.sinType.incrementAndGet();
            return;
        }
        writer.insert(row.nss(), row.type(), row.content(), row.id(), CREATED_BY);
        stats.migrado.incrementAndGet();
    }

    private static final class Stats {
        final AtomicInteger migrado = new AtomicInteger();
        final AtomicInteger yaExistia = new AtomicInteger();
        final AtomicInteger sinType = new AtomicInteger();

        void log(String modo) {
            log.info("[etl-tags] modo={} TERMINADO - {}", modo, this);
        }

        @Override
        public String toString() {
            return "migrado=" + migrado + " ya_existia=" + yaExistia + " sin_type=" + sinType;
        }
    }
}
