package com.onest.app.catalog.file.etl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Runner del ETL de {@code servicioMedico.tags} (MariaDB legacy) -> {@code SERV_MED_TAG}
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
        Set<Long> yaMigrados = writer.sourceIdsExistentes();
        Stats stats = new Stats();
        List<LegacyTagRow> buffer = new ArrayList<>();
        for (LegacyTagRow row : reader.findByIds(ids)) {
            acumular(row, yaMigrados, buffer, stats);
        }
        vaciar(buffer, stats);
        stats.log("sample");
    }

    private void runFull() {
        long minId = reader.minId();
        long maxId = reader.maxId();
        log.info("[etl-tags] modo=full, rango de ids [{}, {}], lote={}", minId, maxId, batchSize);

        // UNA consulta en vez de medio millon. Ver la nota de latencia en MedTagWriter.
        Set<Long> yaMigrados = writer.sourceIdsExistentes();
        log.info("[etl-tags] ya migrados en el destino: {}", yaMigrados.size());

        Stats stats = new Stats();
        List<LegacyTagRow> buffer = new ArrayList<>(batchSize);
        for (long from = minId; from <= maxId; from += batchSize) {
            long to = Math.min(from + batchSize - 1, maxId);
            for (LegacyTagRow row : reader.findByIdRange(from, to)) {
                acumular(row, yaMigrados, buffer, stats);
            }
            log.info("[etl-tags] lote [{}, {}] listo - acumulado: {}", from, to, stats);
        }
        vaciar(buffer, stats);
        stats.log("full");
    }

    /**
     * Clasifica una fila y la deja lista para el siguiente lote.
     *
     * <p>La comprobacion de "ya migrado" sale del {@code Set} en memoria, no de una consulta.
     * Es el cambio que convierte una corrida de 30 horas en uno de minutos: ver la nota de
     * latencia en {@link MedTagWriter}.
     */
    private void acumular(LegacyTagRow row, Set<Long> yaMigrados, List<LegacyTagRow> buffer, Stats stats) {
        if (yaMigrados.contains(row.id())) {
            stats.yaExistia.incrementAndGet();
            return;
        }
        if (row.type() == null || row.type().isBlank()) {
            log.warn("[etl-tags] id={} sin type (huerfano) - se omite", row.id());
            stats.sinType.incrementAndGet();
            return;
        }
        buffer.add(row);
        if (buffer.size() >= batchSize) {
            vaciar(buffer, stats);
        }
    }

    /** Manda el lote acumulado en un solo viaje y lo limpia. */
    private void vaciar(List<LegacyTagRow> buffer, Stats stats) {
        if (buffer.isEmpty()) {
            return;
        }
        stats.migrado.addAndGet(writer.insertBatch(buffer, CREATED_BY));
        buffer.clear();
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
